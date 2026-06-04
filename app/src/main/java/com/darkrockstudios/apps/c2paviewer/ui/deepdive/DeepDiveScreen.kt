package com.darkrockstudios.apps.c2paviewer.ui.deepdive

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.darkrockstudios.apps.c2paviewer.R
import com.darkrockstudios.apps.c2paviewer.model.c2pa.C2paManifestData
import com.darkrockstudios.apps.c2paviewer.model.c2pa.ValidationCategory
import com.darkrockstudios.apps.c2paviewer.ui.inspection.InspectionUiState
import com.darkrockstudios.apps.c2paviewer.ui.inspection.InspectionViewModel
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

private val prettyJson = Json { prettyPrint = true; prettyPrintIndent = "  " }

/**
 * Full manifest exploration: manifest metadata, signature/certificate, assertions, ingredients,
 * validation issues, and a raw-JSON explorer. Shares [InspectionViewModel] with the viewer.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeepDiveScreen(
	viewModel: InspectionViewModel,
	onBack: () -> Unit,
) {
	val state by viewModel.state.collectAsStateWithLifecycle()

	Scaffold(
		topBar = {
			TopAppBar(
				title = { Text(stringResource(R.string.details_title)) },
				navigationIcon = {
					IconButton(onClick = onBack) {
						Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
					}
				},
			)
		},
	) { innerPadding ->
		val manifest = (state as? InspectionUiState.Loaded)?.result?.manifest
		if (manifest == null) {
			Column(Modifier.fillMaxSize().padding(innerPadding).padding(24.dp)) {
				Text(stringResource(R.string.status_no_manifest_body))
			}
			return@Scaffold
		}

		LazyColumn(
			modifier = Modifier.fillMaxSize().padding(innerPadding),
			contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
			verticalArrangement = Arrangement.spacedBy(12.dp),
		) {
			item { ManifestSection(manifest) }
			item { SignatureSection(manifest) }
			item { AssertionsSection(manifest) }
			item { IngredientsSection(manifest) }
			item { ValidationSection(manifest) }
			item { RawJsonSection(manifest) }
		}
	}
}

@Composable
private fun ManifestSection(manifest: C2paManifestData) {
	val m = manifest.activeManifest
	SectionCard(stringResource(R.string.section_manifest)) {
		KeyValue(stringResource(R.string.label_title), m?.title)
		KeyValue(stringResource(R.string.label_format), m?.format)
		KeyValue(stringResource(R.string.label_generator), m?.claimGenerator)
		KeyValue(stringResource(R.string.label_instance), m?.instanceId)
	}
}

@Composable
private fun SignatureSection(manifest: C2paManifestData) {
	val sig = manifest.activeManifest?.signature
	val trust = when {
		manifest.signerTrusted -> stringResource(R.string.trust_trusted)
		manifest.signerUntrusted -> stringResource(R.string.trust_untrusted)
		else -> stringResource(R.string.trust_unknown)
	}
	SectionCard(stringResource(R.string.section_signature)) {
		KeyValue(stringResource(R.string.label_issuer), sig?.issuer)
		KeyValue(stringResource(R.string.label_common_name), sig?.commonName)
		KeyValue(stringResource(R.string.label_algorithm), sig?.algorithm)
		KeyValue(stringResource(R.string.label_serial), sig?.certSerialNumber)
		KeyValue(stringResource(R.string.label_signed_time), sig?.time)
		KeyValue(stringResource(R.string.label_trust), trust)
	}
}

@Composable
private fun AssertionsSection(manifest: C2paManifestData) {
	val assertions = manifest.activeManifest?.assertions.orEmpty()
	ExpandableCard(stringResource(R.string.section_assertions, assertions.size)) {
		if (assertions.isEmpty()) {
			Text(stringResource(R.string.empty_section), style = MaterialTheme.typography.bodySmall)
		}
		assertions.forEachIndexed { index, a ->
			if (index > 0) HorizontalDivider(Modifier.padding(vertical = 8.dp))
			Text(a.label, style = MaterialTheme.typography.titleSmall)
			a.kind?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
			Mono(prettyPrint(a.data))
		}
	}
}

@Composable
private fun IngredientsSection(manifest: C2paManifestData) {
	val ingredients = manifest.activeManifest?.ingredients.orEmpty()
	ExpandableCard(stringResource(R.string.section_ingredients, ingredients.size)) {
		if (ingredients.isEmpty()) {
			Text(stringResource(R.string.empty_section), style = MaterialTheme.typography.bodySmall)
		}
		ingredients.forEachIndexed { index, ing ->
			if (index > 0) HorizontalDivider(Modifier.padding(vertical = 8.dp))
			Text(ing.title ?: "—", style = MaterialTheme.typography.titleSmall)
			KeyValue(stringResource(R.string.label_format), ing.format)
			ing.relationship?.let { Text(stringResource(R.string.ingredient_relationship, it), style = MaterialTheme.typography.bodySmall) }
		}
	}
}

@Composable
private fun ValidationSection(manifest: C2paManifestData) {
	val issues = manifest.validationIssues
	ExpandableCard(stringResource(R.string.section_validation, issues.size)) {
		if (issues.isEmpty()) {
			Text(stringResource(R.string.empty_section), style = MaterialTheme.typography.bodySmall)
		}
		issues.forEach { issue ->
			val color = when (issue.category) {
				ValidationCategory.FAILURE -> MaterialTheme.colorScheme.error
				ValidationCategory.SUCCESS -> MaterialTheme.colorScheme.primary
				else -> MaterialTheme.colorScheme.onSurfaceVariant
			}
			Text("${issue.category.name} · ${issue.code}", style = MaterialTheme.typography.bodyMedium, color = color)
			issue.explanation?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
		}
	}
}

@Composable
private fun RawJsonSection(manifest: C2paManifestData) {
	ExpandableCard(stringResource(R.string.section_raw_json), initiallyExpanded = false) {
		Mono(remember(manifest) { prettyPrint(manifest.rawDetailedJson ?: manifest.rawManifestJson) })
	}
}

// --- reusable bits ---

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
	Card(Modifier.fillMaxWidth()) {
		Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
			Text(title, style = MaterialTheme.typography.titleMedium)
			content()
		}
	}
}

@Composable
private fun ExpandableCard(
	title: String,
	initiallyExpanded: Boolean = true,
	content: @Composable () -> Unit,
) {
	var expanded by rememberSaveable(title) { mutableStateOf(initiallyExpanded) }
	Card(Modifier.fillMaxWidth()) {
		Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
			Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
				Text(title, style = MaterialTheme.typography.titleMedium)
				TextButton(onClick = { expanded = !expanded }) {
					Text(stringResource(if (expanded) R.string.hide else R.string.show))
				}
			}
			if (expanded) content()
		}
	}
}

@Composable
private fun KeyValue(label: String, value: String?) {
	if (value.isNullOrBlank()) return
	Column {
		Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
		Text(value, style = MaterialTheme.typography.bodyMedium)
	}
}

@Composable
private fun Mono(text: String) {
	Text(
		text = text,
		fontFamily = FontFamily.Monospace,
		style = MaterialTheme.typography.bodySmall,
		modifier = Modifier.horizontalScroll(rememberScrollState()),
	)
}

private fun prettyPrint(raw: String): String =
	runCatching {
		prettyJson.encodeToString(JsonElement.serializer(), prettyJson.parseToJsonElement(raw))
	}.getOrDefault(raw)

private fun prettyPrint(element: JsonElement?): String =
	element?.let {
		runCatching { prettyJson.encodeToString(JsonElement.serializer(), it) }.getOrNull()
	} ?: "—"
