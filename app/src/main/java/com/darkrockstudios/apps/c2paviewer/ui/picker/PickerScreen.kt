package com.darkrockstudios.apps.c2paviewer.ui.picker

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.darkrockstudios.apps.c2paviewer.R

/**
 * Landing screen: an informative empty state plus the Android Photo Picker
 * ([ActivityResultContracts.PickVisualMedia], no storage permission required).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickerScreen(
	onImagePicked: (String) -> Unit,
	onOpenTrust: () -> Unit,
) {
	val pickMedia = rememberLauncherForActivityResult(
		ActivityResultContracts.PickVisualMedia(),
	) { uri -> if (uri != null) onImagePicked(uri.toString()) }

	Scaffold(
		topBar = {
			TopAppBar(
				title = { Text(stringResource(R.string.app_name)) },
				actions = {
					IconButton(onClick = onOpenTrust) {
						Icon(
							painter = painterResource(R.drawable.ic_shield),
							contentDescription = stringResource(R.string.trust_manage),
						)
					}
				},
			)
		},
	) { innerPadding ->
		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(innerPadding)
				.verticalScroll(rememberScrollState())
				.padding(horizontal = 24.dp, vertical = 32.dp),
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
		) {
			Icon(
				imageVector = Icons.Default.Search,
				contentDescription = null,
				modifier = Modifier.size(72.dp),
				tint = MaterialTheme.colorScheme.primary,
			)
			Text(
				text = stringResource(R.string.empty_title),
				style = MaterialTheme.typography.headlineSmall,
				textAlign = TextAlign.Center,
			)
			Text(
				text = stringResource(R.string.empty_body),
				style = MaterialTheme.typography.bodyMedium,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
				textAlign = TextAlign.Center,
			)
			Button(
				onClick = {
					pickMedia.launch(
						PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
					)
				},
			) {
				Text(stringResource(R.string.pick_photo))
			}
		}
	}
}
