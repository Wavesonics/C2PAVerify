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

/** Window width buckets (derived from the WindowManager-backed [WindowSizeClass]). */
private enum class WidthBucket { COMPACT, MEDIUM, EXPANDED }

private fun WindowSizeClass.widthBucket(): WidthBucket = when {
	isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND) -> WidthBucket.EXPANDED
	isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND) -> WidthBucket.MEDIUM
	else -> WidthBucket.COMPACT
}

/**
 * Adaptive container for a single inspection. The photo is always the primary surface. Layout is
 * driven by Jetpack WindowManager via [currentWindowAdaptiveInfo] — width size-class buckets plus
 * fold posture:
 *  - **Open foldable** (vertical hinge): split exactly at the crease (~50/50), photo left.
 *  - **Expanded width** (≥840dp): photo fills, Content Credentials pinned to [DetailPaneWidth].
 *  - **Compact / Medium**: single pane; "View details" shows the deep-dive, back collapses it.
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
	// A vertical hinge means an open foldable (flat or half-open); split at the crease regardless
	// of width bucket.
	val verticalHinge = info.windowPosture.hingeList.firstOrNull { it.isVertical }
	val widthBucket = info.windowSizeClass.widthBucket()

	when {
		verticalHinge != null -> {
			val density = LocalDensity.current
			val photoWidth = with(density) { verticalHinge.bounds.left.toDp() }
			val creaseWidth = with(density) { verticalHinge.bounds.width.toDp() }
			Row(Modifier.fillMaxSize()) {
				Box(Modifier.width(photoWidth).fillMaxHeight()) { Photo(imageUri, viewModel, onExit) }
				Spacer(Modifier.width(creaseWidth).fillMaxHeight())
				Box(Modifier.weight(1f).fillMaxHeight()) { Credentials(viewModel) }
			}
		}

		widthBucket == WidthBucket.EXPANDED -> Row(Modifier.fillMaxSize()) {
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
