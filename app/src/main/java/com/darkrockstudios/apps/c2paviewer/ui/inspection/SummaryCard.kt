package com.darkrockstudios.apps.c2paviewer.ui.inspection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.darkrockstudios.apps.c2paviewer.R
import com.darkrockstudios.apps.c2paviewer.model.summary.C2paSummary
import com.darkrockstudios.apps.c2paviewer.model.summary.OverallStatus

/**
 * Up-front summary of an inspection: a colour-coded status, the signer/generator, and an
 * AI-generated chip when applicable.
 */
@Composable
fun SummaryCard(summary: C2paSummary, modifier: Modifier = Modifier) {
	Card(modifier = modifier.fillMaxWidth()) {
		Column(
			modifier = Modifier.padding(16.dp),
			verticalArrangement = Arrangement.spacedBy(8.dp),
		) {
			StatusRow(summary.status)

			if (summary.status == OverallStatus.NO_MANIFEST) {
				Text(
					text = stringResource(R.string.status_no_manifest_body),
					style = MaterialTheme.typography.bodyMedium,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
				)
			} else {
				summary.signerName?.let {
					Text(
						text = stringResource(R.string.summary_signer, it),
						style = MaterialTheme.typography.bodyMedium,
					)
				}
				summary.claimGenerator?.let {
					Text(
						text = stringResource(R.string.summary_generator, it),
						style = MaterialTheme.typography.bodySmall,
						color = MaterialTheme.colorScheme.onSurfaceVariant,
					)
				}
				summary.signedTime?.let {
					Text(
						text = stringResource(R.string.summary_signed_time, it),
						style = MaterialTheme.typography.bodySmall,
						color = MaterialTheme.colorScheme.onSurfaceVariant,
					)
				}
				if (summary.ai.isAiGenerated) {
					AssistChip(
						onClick = {},
						label = { Text(stringResource(R.string.ai_badge)) },
					)
				}
			}
		}
	}
}

@Composable
private fun StatusRow(status: OverallStatus) {
	val (label, color) = when (status) {
		OverallStatus.SIGNED_TRUSTED -> stringResource(R.string.status_trusted) to StatusGreen
		OverallStatus.SIGNED_UNTRUSTED -> stringResource(R.string.status_untrusted) to StatusAmber
		OverallStatus.TAMPERED_INVALID -> stringResource(R.string.status_tampered) to MaterialTheme.colorScheme.error
		OverallStatus.NO_MANIFEST -> stringResource(R.string.status_no_manifest) to MaterialTheme.colorScheme.outline
	}
	Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
		Surface(color = color, shape = CircleShape, modifier = Modifier.size(12.dp)) {}
		Text(text = label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
	}
}

private val StatusGreen = Color(0xFF2E7D32)
private val StatusAmber = Color(0xFFF9A825)
