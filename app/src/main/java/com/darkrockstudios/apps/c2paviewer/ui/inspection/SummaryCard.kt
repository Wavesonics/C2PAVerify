package com.darkrockstudios.apps.c2paviewer.ui.inspection

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.darkrockstudios.apps.c2paviewer.R
import com.darkrockstudios.apps.c2paviewer.model.summary.C2paSummary
import com.darkrockstudios.apps.c2paviewer.model.summary.OverallStatus

/**
 * Up-front summary of an inspection, presented as a bold colour-coded banner: a status icon +
 * label on a tonal container matching the verdict, then signer/generator details and an
 * AI-generated chip.
 */
@Composable
fun SummaryCard(
	summary: C2paSummary,
	modifier: Modifier = Modifier,
	onViewDetails: (() -> Unit)? = null,
) {
	val visuals = statusVisuals(summary.status)
	Card(
		modifier = modifier.fillMaxWidth(),
		colors = CardDefaults.cardColors(
			containerColor = visuals.container,
			contentColor = visuals.onContainer,
		),
	) {
		Column(
			modifier = Modifier.padding(16.dp),
			verticalArrangement = Arrangement.spacedBy(10.dp),
		) {
			Row(
				verticalAlignment = Alignment.CenterVertically,
				horizontalArrangement = Arrangement.spacedBy(12.dp),
			) {
				Icon(visuals.icon, contentDescription = null, modifier = Modifier.size(28.dp))
				Text(
					text = visuals.label,
					style = MaterialTheme.typography.titleMedium,
					fontWeight = FontWeight.SemiBold,
				)
			}

			if (summary.status == OverallStatus.NO_MANIFEST) {
				Text(
					text = stringResource(R.string.status_no_manifest_body),
					style = MaterialTheme.typography.bodyMedium,
				)
			} else {
				summary.signerName?.let {
					Text(stringResource(R.string.summary_signer, it), style = MaterialTheme.typography.bodyMedium)
				}
				summary.claimGenerator?.let {
					Text(stringResource(R.string.summary_generator, it), style = MaterialTheme.typography.bodySmall)
				}
				summary.signedTime?.let {
					Text(stringResource(R.string.summary_signed_time, it), style = MaterialTheme.typography.bodySmall)
				}
				Row(
					verticalAlignment = Alignment.CenterVertically,
					horizontalArrangement = Arrangement.spacedBy(8.dp),
				) {
					if (summary.ai.isAiGenerated) {
						AssistChip(
							onClick = {},
							label = { Text(stringResource(R.string.ai_badge)) },
							leadingIcon = {
								Icon(
									painter = painterResource(R.drawable.ic_auto_awesome),
									contentDescription = null,
									modifier = Modifier.size(AssistChipDefaults.IconSize),
								)
							},
						)
					}
					if (onViewDetails != null) {
						TextButton(onClick = onViewDetails) {
							Text(stringResource(R.string.view_details))
						}
					}
				}
			}
		}
	}
}

private data class StatusVisuals(
	val label: String,
	val container: Color,
	val onContainer: Color,
	val icon: ImageVector,
)

/** Semantic, theme-aware colours for the trust verdict (M3 has no standard success/warning role). */
@Composable
private fun statusVisuals(status: OverallStatus): StatusVisuals {
	val dark = isSystemInDarkTheme()
	return when (status) {
		OverallStatus.SIGNED_TRUSTED -> StatusVisuals(
			label = stringResource(R.string.status_trusted),
			container = if (dark) Color(0xFF14352A) else Color(0xFFD7F2E1),
			onContainer = if (dark) Color(0xFFA6F4C5) else Color(0xFF0B5132),
			icon = Icons.Filled.CheckCircle,
		)

		OverallStatus.SIGNED_UNTRUSTED -> StatusVisuals(
			label = stringResource(R.string.status_untrusted),
			container = if (dark) Color(0xFF3A3016) else Color(0xFFFFF0C2),
			onContainer = if (dark) Color(0xFFF6D77A) else Color(0xFF5A4500),
			icon = Icons.Filled.Warning,
		)

		OverallStatus.TAMPERED_INVALID -> StatusVisuals(
			label = stringResource(R.string.status_tampered),
			container = MaterialTheme.colorScheme.errorContainer,
			onContainer = MaterialTheme.colorScheme.onErrorContainer,
			icon = Icons.Filled.Warning,
		)

		OverallStatus.NO_MANIFEST -> StatusVisuals(
			label = stringResource(R.string.status_no_manifest),
			container = MaterialTheme.colorScheme.surfaceVariant,
			onContainer = MaterialTheme.colorScheme.onSurfaceVariant,
			icon = Icons.Filled.Info,
		)
	}
}
