package com.darkrockstudios.apps.c2paviewer.ui.inspection

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import com.darkrockstudios.apps.c2paviewer.ui.deepdive.DeepDiveScreen
import com.darkrockstudios.apps.c2paviewer.ui.viewer.ViewerScreen

/** Max width of the Content Credentials panel on a (non-folding) large screen. */
private val DetailPaneWidth = 300.dp

/**
 * Adaptive container for a single inspection. The photo is always the primary surface:
 *  - **Foldable, open** (separating vertical hinge): split exactly at the crease (~50/50), photo
 *    left, credentials right.
 *  - **Expanded large screen** (tablet): photo fills the space, credentials pinned to [DetailPaneWidth].
 *  - **Compact** (phone): single pane; "View details" shows the deep-dive, back collapses it.
 *
 * Both panes share one [InspectionViewModel].
 */
@Composable
fun InspectionScaffold(
	imageUri: String?,
	viewModel: InspectionViewModel,
	onExit: () -> Unit,
) {
	val info = currentWindowAdaptiveInfo()
	// Split at the crease whenever a vertical hinge is present (open foldable), whether the device
	// is flat or half-folded — a flat-open fold reports the hinge as non-separating.
	val verticalHinge = info.windowPosture.hingeList.firstOrNull { it.isVertical }
	val expanded = info.windowSizeClass
		.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)

	when {
		verticalHinge != null -> {
			// Place the divider exactly on the hinge so each pane occupies one half of the fold.
			val density = LocalDensity.current
			val photoWidth = with(density) { verticalHinge.bounds.left.toDp() }
			val creaseWidth = with(density) { verticalHinge.bounds.width.toDp() }
			Row(Modifier.fillMaxSize()) {
				Box(Modifier.width(photoWidth).fillMaxHeight()) { Photo(imageUri, viewModel, onExit) }
				Spacer(Modifier.width(creaseWidth).fillMaxHeight())
				Box(Modifier.weight(1f).fillMaxHeight()) { Credentials(viewModel) }
			}
		}

		expanded -> Row(Modifier.fillMaxSize()) {
			Box(Modifier.weight(1f).fillMaxHeight()) { Photo(imageUri, viewModel, onExit) }
			Box(Modifier.width(DetailPaneWidth).fillMaxHeight()) { Credentials(viewModel) }
		}

		else -> {
			var showDetail by rememberSaveable { mutableStateOf(false) }
			BackHandler(enabled = showDetail) { showDetail = false }
			if (showDetail) {
				DeepDiveScreen(viewModel = viewModel, onBack = { showDetail = false }, showBack = true)
			} else {
				ViewerScreen(
					imageUri = imageUri,
					viewModel = viewModel,
					onBack = onExit,
					onOpenDetails = { showDetail = true },
				)
			}
		}
	}
}

@Composable
private fun Photo(imageUri: String?, viewModel: InspectionViewModel, onExit: () -> Unit) {
	// In two-pane mode the credentials are already visible, so no "View details" affordance.
	ViewerScreen(imageUri = imageUri, viewModel = viewModel, onBack = onExit, onOpenDetails = null)
}

@Composable
private fun Credentials(viewModel: InspectionViewModel) {
	DeepDiveScreen(viewModel = viewModel, onBack = {}, showBack = false)
}
