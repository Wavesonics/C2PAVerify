package com.darkrockstudios.apps.c2paviewer.ui.viewer

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.darkrockstudios.apps.c2paviewer.R
import com.darkrockstudios.apps.c2paviewer.model.share.ReportOverlay
import com.darkrockstudios.apps.c2paviewer.model.share.toneFor
import com.darkrockstudios.apps.c2paviewer.model.summary.C2paSummary
import com.darkrockstudios.apps.c2paviewer.model.summary.OverallStatus
import com.darkrockstudios.apps.c2paviewer.ui.inspection.InspectionUiState
import com.darkrockstudios.apps.c2paviewer.ui.inspection.InspectionViewModel
import com.darkrockstudios.apps.c2paviewer.ui.inspection.SummaryCard
import me.saket.telephoto.zoomable.coil3.ZoomableAsyncImage
import org.koin.androidx.compose.koinViewModel

/**
 * Displays the selected/shared photo (Telephoto + Coil 3 zoom/pan/subsampling) and overlays the
 * C2PA summary card once inspection completes. Once loaded, a Share action renders the photo with a
 * verification overlay and hands it to the system share sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreen(
	imageUri: String?,
	onBack: () -> Unit,
	onOpenDetails: (() -> Unit)?,
	viewModel: InspectionViewModel = koinViewModel(),
) {
	LaunchedEffect(imageUri) {
		if (imageUri != null) viewModel.inspect(imageUri)
	}
	val state by viewModel.state.collectAsStateWithLifecycle()
	val sharing by viewModel.sharing.collectAsStateWithLifecycle()

	val context = LocalContext.current
	val chooserTitle = stringResource(R.string.share_report_chooser)
	LaunchedEffect(Unit) {
		viewModel.shareRequests.collect { uriString ->
			val send = Intent(Intent.ACTION_SEND).apply {
				type = "image/png"
				putExtra(Intent.EXTRA_STREAM, uriString.toUri())
				addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
			}
			context.startActivity(Intent.createChooser(send, chooserTitle))
		}
	}

	val loaded = state as? InspectionUiState.Loaded
	val overlay = loaded?.let { reportOverlayFor(it.result.summary) }

	Scaffold(
		topBar = {
			TopAppBar(
				title = { Text(stringResource(R.string.viewer_title)) },
				navigationIcon = {
					IconButton(onClick = onBack) {
						Icon(
							imageVector = Icons.AutoMirrored.Filled.ArrowBack,
							contentDescription = stringResource(R.string.back),
						)
					}
				},
				actions = {
					if (sharing) {
						CircularProgressIndicator(modifier = Modifier.padding(end = 16.dp).size(24.dp))
					} else if (overlay != null) {
						IconButton(onClick = { viewModel.shareReport(overlay) }) {
							Icon(Icons.Filled.Share, stringResource(R.string.share_report))
						}
					}
				},
			)
		},
	) { innerPadding ->
		Box(
			modifier = Modifier
				.fillMaxSize()
				.background(MaterialTheme.colorScheme.surfaceContainerHigh)
				.padding(innerPadding),
			contentAlignment = Alignment.Center,
		) {
			if (imageUri != null) {
				ZoomableAsyncImage(
					model = imageUri,
					contentDescription = stringResource(R.string.selected_photo),
					modifier = Modifier.fillMaxSize(),
				)
			} else {
				Text(
					text = stringResource(R.string.no_image),
					color = MaterialTheme.colorScheme.onSurfaceVariant,
				)
			}

			InspectionOverlay(
				state = state,
				onOpenDetails = onOpenDetails,
				modifier = Modifier
					.align(Alignment.BottomCenter)
					.navigationBarsPadding()
					.padding(16.dp)
					.widthIn(max = 520.dp),
			)
		}
	}
}

@Composable
private fun InspectionOverlay(
	state: InspectionUiState,
	onOpenDetails: (() -> Unit)?,
	modifier: Modifier = Modifier,
) {
	when (state) {
		InspectionUiState.Idle -> Unit
		InspectionUiState.Loading -> CircularProgressIndicator(modifier = modifier)
		is InspectionUiState.Loaded -> SummaryCard(
			summary = state.result.summary,
			onViewDetails = onOpenDetails?.takeIf { state.result.manifest != null },
			modifier = modifier,
		)

		is InspectionUiState.Error -> Text(
			text = stringResource(R.string.inspect_error, state.message),
			color = MaterialTheme.colorScheme.error,
			modifier = modifier,
		)
	}
}

/** Resolves a [C2paSummary] into a localised [ReportOverlay] for the renderer. */
@Composable
private fun reportOverlayFor(summary: C2paSummary): ReportOverlay {
	val statusLabel = when (summary.status) {
		OverallStatus.SIGNED_TRUSTED -> stringResource(R.string.status_trusted)
		OverallStatus.SIGNED_UNTRUSTED -> stringResource(R.string.status_untrusted)
		OverallStatus.TAMPERED_INVALID -> stringResource(R.string.status_tampered)
		OverallStatus.NO_MANIFEST -> stringResource(R.string.status_no_manifest)
	}
	val details = buildList {
		if (summary.status == OverallStatus.NO_MANIFEST) {
			add(stringResource(R.string.status_no_manifest_body))
		} else {
			summary.signerName?.let { add(stringResource(R.string.summary_signer, it)) }
			summary.claimGenerator?.let { add(stringResource(R.string.summary_generator, it)) }
			summary.signedTime?.let { add(stringResource(R.string.summary_signed_time, it)) }
		}
	}
	return ReportOverlay(
		statusLabel = statusLabel,
		tone = toneFor(summary.status),
		details = details,
		isAiGenerated = summary.ai.isAiGenerated,
		aiLabel = stringResource(R.string.ai_badge),
		watermark = stringResource(R.string.report_watermark),
	)
}
