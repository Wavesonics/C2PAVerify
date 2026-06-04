package com.darkrockstudios.apps.c2paviewer.ui.trust

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.DateFormat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.darkrockstudios.apps.c2paviewer.R
import com.darkrockstudios.apps.c2paviewer.model.trust.TrustAnchorInfo
import com.darkrockstudios.apps.c2paviewer.ui.common.plus
import com.darkrockstudios.apps.c2paviewer.model.trust.UserTrustRule
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrustManagementScreen(
	onBack: () -> Unit,
	viewModel: TrustManagementViewModel = koinViewModel(),
) {
	val state by viewModel.state.collectAsStateWithLifecycle()

	Scaffold(
		topBar = {
			TopAppBar(
				title = { Text(stringResource(R.string.trust_title)) },
				navigationIcon = {
					IconButton(onClick = onBack) {
						Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
					}
				},
				actions = {
					if (state.refreshing) {
						CircularProgressIndicator(modifier = Modifier.padding(end = 16.dp).size(24.dp))
					} else {
						IconButton(onClick = viewModel::refresh) {
							Icon(Icons.Filled.Refresh, stringResource(R.string.trust_refresh))
						}
					}
				},
			)
		},
	) { innerPadding ->
		LazyColumn(
			modifier = Modifier.fillMaxSize().consumeWindowInsets(innerPadding),
			contentPadding = innerPadding + PaddingValues(16.dp),
			verticalArrangement = Arrangement.spacedBy(12.dp),
		) {
			item {
				val updated = state.lastUpdatedEpochMs
				Text(
					text = if (updated != null) {
						stringResource(R.string.trust_updated, DateFormat.getDateInstance().format(Date(updated)))
					} else {
						stringResource(R.string.trust_bundled)
					},
					style = MaterialTheme.typography.bodySmall,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
				)
				state.error?.let {
					Text(
						stringResource(R.string.trust_refresh_failed, it),
						style = MaterialTheme.typography.bodySmall,
						color = MaterialTheme.colorScheme.error,
					)
				}
			}
			item {
				Text(
					stringResource(R.string.trust_your_rules, state.rules.size),
					style = MaterialTheme.typography.titleMedium,
				)
			}
			if (state.rules.isEmpty()) {
				item {
					Text(
						stringResource(R.string.trust_your_rules_empty),
						style = MaterialTheme.typography.bodyMedium,
						color = MaterialTheme.colorScheme.onSurfaceVariant,
					)
				}
			} else {
				items(state.rules, key = { it.authorityKey }) { rule ->
					RuleRow(rule, onRemove = { viewModel.removeRule(rule.authorityKey) })
				}
			}

			item {
				Text(
					stringResource(R.string.trust_default_cas, state.anchors.size),
					style = MaterialTheme.typography.titleMedium,
					modifier = Modifier.padding(top = 8.dp),
				)
			}
			items(state.anchors, key = { it.subject }) { anchor -> AnchorRow(anchor) }
		}
	}
}

@Composable
private fun RuleRow(rule: UserTrustRule, onRemove: () -> Unit) {
	val allow = rule.rule.name == "ALLOW"
	Card(Modifier.fillMaxWidth()) {
		Row(
			modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.SpaceBetween,
		) {
			Column(Modifier.weight(1f)) {
				Text(rule.displayName, style = MaterialTheme.typography.bodyLarge)
				Text(
					stringResource(if (allow) R.string.rule_allow else R.string.rule_deny),
					style = MaterialTheme.typography.labelMedium,
					fontWeight = FontWeight.SemiBold,
					color = if (allow) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
				)
			}
			TextButton(onClick = onRemove) { Text(stringResource(R.string.remove)) }
		}
	}
}

@Composable
private fun AnchorRow(anchor: TrustAnchorInfo) {
	Card(Modifier.fillMaxWidth()) {
		Column(Modifier.padding(16.dp)) {
			Text(anchor.displayName, style = MaterialTheme.typography.bodyLarge)
			anchor.notAfterEpochMs?.let {
				Text(
					stringResource(R.string.trust_expires, dateFormat.format(Date(it))),
					style = MaterialTheme.typography.bodySmall,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
				)
			}
		}
	}
}

private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
