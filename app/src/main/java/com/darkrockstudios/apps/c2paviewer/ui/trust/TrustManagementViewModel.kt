package com.darkrockstudios.apps.c2paviewer.ui.trust

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.darkrockstudios.apps.c2paviewer.model.trust.TrustAnchorInfo
import com.darkrockstudios.apps.c2paviewer.model.trust.TrustRule
import com.darkrockstudios.apps.c2paviewer.model.trust.UserTrustRule
import com.darkrockstudios.apps.c2paviewer.model.trust.anchorAuthorityKey
import com.darkrockstudios.apps.c2paviewer.usecase.trust.ClearAuthorityRuleUseCase
import com.darkrockstudios.apps.c2paviewer.usecase.trust.GetTrustListUseCase
import com.darkrockstudios.apps.c2paviewer.usecase.trust.ObserveUserTrustRulesUseCase
import com.darkrockstudios.apps.c2paviewer.usecase.trust.RefreshTrustListUseCase
import com.darkrockstudios.apps.c2paviewer.usecase.trust.SetAuthorityRuleUseCase
import com.darkrockstudios.apps.c2paviewer.usecase.trust.TrustListView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TrustUiState(
	val loading: Boolean = true,
	val anchors: List<TrustAnchorInfo> = emptyList(),
	val rules: List<UserTrustRule> = emptyList(),
	val lastUpdatedEpochMs: Long? = null,
	val refreshing: Boolean = false,
	val error: String? = null,
)

class TrustManagementViewModel(
	private val getTrustList: GetTrustListUseCase,
	observeUserRules: ObserveUserTrustRulesUseCase,
	private val clearAuthorityRule: ClearAuthorityRuleUseCase,
	private val setAuthorityRule: SetAuthorityRuleUseCase,
	private val refreshTrustList: RefreshTrustListUseCase,
) : ViewModel() {

	private val listView = MutableStateFlow<TrustListView?>(null)
	private val refreshing = MutableStateFlow(false)
	private val error = MutableStateFlow<String?>(null)

	val state: StateFlow<TrustUiState> =
		combine(listView, observeUserRules(), refreshing, error) { view, rules, refreshing, error ->
			TrustUiState(
				loading = view == null,
				anchors = view?.anchors.orEmpty(),
				rules = rules,
				lastUpdatedEpochMs = view?.lastUpdatedEpochMs,
				refreshing = refreshing,
				error = error,
			)
		}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TrustUiState())

	init {
		viewModelScope.launch { listView.value = getTrustList() }
	}

	fun refresh() {
		viewModelScope.launch {
			refreshing.value = true
			error.value = null
			val result = refreshTrustList()
			if (result.isFailure) {
				error.value = result.exceptionOrNull()?.message ?: "Refresh failed"
			}
			listView.value = getTrustList()
			refreshing.value = false
		}
	}

	fun removeRule(authorityKey: String) {
		viewModelScope.launch { clearAuthorityRule(authorityKey) }
	}

	/** Add or remove a default CA from the user's dis-allow list. */
	fun setAnchorBlocked(anchor: TrustAnchorInfo, blocked: Boolean) {
		val key = anchorAuthorityKey(anchor.subject)
		viewModelScope.launch {
			if (blocked) {
				setAuthorityRule(key, anchor.displayName, TrustRule.DENY, System.currentTimeMillis())
			} else {
				clearAuthorityRule(key)
			}
		}
	}
}
