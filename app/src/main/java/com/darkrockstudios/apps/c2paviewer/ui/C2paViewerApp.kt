package com.darkrockstudios.apps.c2paviewer.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.darkrockstudios.apps.c2paviewer.ui.deepdive.DeepDiveScreen
import com.darkrockstudios.apps.c2paviewer.ui.inspection.InspectionViewModel
import com.darkrockstudios.apps.c2paviewer.ui.navigation.DeepDive
import com.darkrockstudios.apps.c2paviewer.ui.navigation.Landing
import com.darkrockstudios.apps.c2paviewer.ui.navigation.Viewer
import com.darkrockstudios.apps.c2paviewer.ui.picker.PickerScreen
import com.darkrockstudios.apps.c2paviewer.ui.viewer.ViewerScreen
import kotlinx.coroutines.flow.StateFlow
import org.koin.androidx.compose.koinViewModel

/**
 * Root composable. Hosts navigation between the landing/picker and the photo viewer, and routes
 * images shared into the app (via [sharedImage]) straight to the viewer.
 */
@Composable
fun C2paViewerApp(sharedImage: StateFlow<String?>) {
	val navController = rememberNavController()
	// One inspection VM shared by the viewer and the deep-dive (activity-scoped owner).
	val inspectionViewModel: InspectionViewModel = koinViewModel()
	var currentImage by rememberSaveable { mutableStateOf<String?>(null) }

	val shared by sharedImage.collectAsStateWithLifecycle()
	LaunchedEffect(shared) {
		val uri = shared ?: return@LaunchedEffect
		currentImage = uri
		navController.navigate(Viewer) { launchSingleTop = true }
	}

	NavHost(navController = navController, startDestination = Landing) {
		composable<Landing> {
			PickerScreen(
				onImagePicked = { uri ->
					currentImage = uri
					navController.navigate(Viewer)
				},
			)
		}
		composable<Viewer> {
			ViewerScreen(
				imageUri = currentImage,
				viewModel = inspectionViewModel,
				onBack = { navController.popBackStack() },
				onOpenDetails = { navController.navigate(DeepDive) },
			)
		}
		composable<DeepDive> {
			DeepDiveScreen(
				viewModel = inspectionViewModel,
				onBack = { navController.popBackStack() },
			)
		}
	}
}
