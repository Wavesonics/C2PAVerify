package com.darkrockstudios.apps.c2paviewer.ui.viewer

import android.content.Intent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.darkrockstudios.apps.c2paviewer.R
import com.darkrockstudios.apps.c2paviewer.model.share.ReportBadge
import com.darkrockstudios.apps.c2paviewer.model.share.ReportBadgeStyle
import com.darkrockstudios.apps.c2paviewer.model.share.ReportOverlay
import com.darkrockstudios.apps.c2paviewer.model.share.toneFor
import com.darkrockstudios.apps.c2paviewer.model.summary.C2paSummary
import com.darkrockstudios.apps.c2paviewer.model.summary.OverallStatus
import com.darkrockstudios.apps.c2paviewer.ui.inspection.InspectionUiState
import com.darkrockstudios.apps.c2paviewer.ui.inspection.InspectionViewModel
import com.darkrockstudios.apps.c2paviewer.ui.inspection.SummaryCard
import me.saket.telephoto.zoomable.coil3.ZoomableAsyncImage
import me.saket.telephoto.zoomable.rememberZoomableImageState
import org.koin.androidx.compose.koinViewModel
import kotlin.math.roundToInt

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

	// While the photo is zoomed in (inspecting), slide the summary card down to a peek so it's out
	// of the way; bring it back when the photo returns to its fit/unzoomed state.
	val zoomState = rememberZoomableImageState()
	val peeking by remember {
		derivedStateOf { (zoomState.zoomableState.zoomFraction ?: 0f) > 0.02f }
	}
	val peekProgress by animateFloatAsState(if (peeking) 1f else 0f, label = "summaryPeek")

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
		// innerPadding is intentionally not applied: the photo fills the screen edge-to-edge (under
		// the system bars). The chrome stays clear of the bars on its own — the top bar via the
		// Scaffold, and the summary card via navigationBarsPadding() below (which is why we must NOT
		// consume the nav-bar inset here, or the card would drop onto the gesture bar).
		Box(
			modifier = Modifier.fillMaxSize(),
			contentAlignment = Alignment.Center,
		) {
			if (imageUri != null) {
				ZoomableAsyncImage(
					model = imageUri,
					contentDescription = stringResource(R.string.selected_photo),
					state = zoomState,
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
				peekFraction = { peekProgress },
				modifier = Modifier
					.align(Alignment.BottomCenter)
					.navigationBarsPadding()
					.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp)
					.widthIn(max = 520.dp),
			)
		}
	}
}

@Composable
private fun InspectionOverlay(
	state: InspectionUiState,
	onOpenDetails: (() -> Unit)?,
	peekFraction: () -> Float,
	modifier: Modifier = Modifier,
) {
	when (state) {
		InspectionUiState.Idle -> Unit
		InspectionUiState.Loading -> CircularProgressIndicator(modifier = modifier)
		is InspectionUiState.Loaded -> {
			// Slide the card down by (its height − a small peek) as the photo zooms in. The peek
			// fraction is read inside offset { } (layout phase) so zoom changes don't recompose.
			val density = LocalDensity.current
			val peekVisiblePx = with(density) { 28.dp.toPx() }
			var cardHeight by remember { mutableIntStateOf(0) }
			SummaryCard(
				summary = state.result.summary,
				onViewDetails = onOpenDetails?.takeIf { state.result.manifest != null },
				modifier = modifier
					.onSizeChanged { cardHeight = it.height }
					.offset {
						val hideBy = (cardHeight - peekVisiblePx).coerceAtLeast(0f)
						IntOffset(x = 0, y = (peekFraction() * hideBy).roundToInt())
					},
			)
		}

		is InspectionUiState.Error -> Card(
			modifier = modifier.fillMaxWidth(),
			colors = CardDefaults.cardColors(
				containerColor = MaterialTheme.colorScheme.errorContainer,
				contentColor = MaterialTheme.colorScheme.onErrorContainer,
			),
		) {
			Text(
				text = stringResource(R.string.inspect_error, state.message),
				modifier = Modifier.padding(16.dp),
				style = MaterialTheme.typography.bodyMedium,
			)
		}
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
	val badges = buildList {
		if (summary.ai.isAiGenerated) {
			add(ReportBadge(stringResource(R.string.ai_badge), ReportBadgeStyle.AI))
		}
		if (summary.aiModified.isAiModified) {
			add(ReportBadge(stringResource(R.string.ai_modified_badge), ReportBadgeStyle.AI))
		}
		if (summary.capture.isCameraCapture) {
			add(ReportBadge(stringResource(R.string.capture_badge), ReportBadgeStyle.CAPTURE))
		}
		if (summary.revoked) {
			add(ReportBadge(stringResource(R.string.revoked_badge), ReportBadgeStyle.ALERT))
		}
	}
	return ReportOverlay(
		statusLabel = statusLabel,
		tone = toneFor(summary.status),
		details = details,
		badges = badges,
		watermark = stringResource(R.string.report_watermark),
	)
}
