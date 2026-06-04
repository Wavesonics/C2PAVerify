package com.darkrockstudios.apps.c2paviewer.ui.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.darkrockstudios.apps.c2paviewer.R
import com.darkrockstudios.apps.c2paviewer.ui.inspection.InspectionUiState
import com.darkrockstudios.apps.c2paviewer.ui.inspection.InspectionViewModel
import com.darkrockstudios.apps.c2paviewer.ui.inspection.SummaryCard
import me.saket.telephoto.zoomable.coil3.ZoomableAsyncImage
import org.koin.androidx.compose.koinViewModel

/**
 * Displays the selected/shared photo (Telephoto + Coil 3 zoom/pan/subsampling) and overlays the
 * C2PA summary card once inspection completes.
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
