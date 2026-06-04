package com.darkrockstudios.apps.c2paviewer.ui.inspection

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.darkrockstudios.apps.c2paviewer.ui.deepdive.DeepDiveScreen
import com.darkrockstudios.apps.c2paviewer.ui.viewer.ViewerScreen
import kotlinx.coroutines.launch

/**
 * Adaptive container for a single inspection: the photo viewer (list/primary pane) and the
 * deep-dive (detail pane). On expanded widths (tablets/foldables) both panes show side-by-side;
 * on compact widths the deep-dive is a separate pane reached via "View details", and
 * predictive back collapses it back to the photo. Both panes share one [InspectionViewModel].
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun InspectionScaffold(
	imageUri: String?,
	viewModel: InspectionViewModel,
	onExit: () -> Unit,
) {
	val navigator = rememberListDetailPaneScaffoldNavigator<Unit>()
	val scope = rememberCoroutineScope()

	NavigableListDetailPaneScaffold(
		navigator = navigator,
		listPane = {
			AnimatedPane {
				ViewerScreen(
					imageUri = imageUri,
					viewModel = viewModel,
					onBack = onExit,
					onOpenDetails = {
						scope.launch { navigator.navigateTo(ListDetailPaneScaffoldRole.Detail) }
					},
				)
			}
		},
		detailPane = {
			AnimatedPane {
				DeepDiveScreen(
					viewModel = viewModel,
					onBack = { scope.launch { navigator.navigateBack() } },
				)
			}
		},
	)
}
