package com.darkrockstudios.apps.c2paviewer.ui.trust

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.darkrockstudios.apps.c2paviewer.model.trust.TrustAnchorInfo
import com.darkrockstudios.apps.c2paviewer.model.trust.UserTrustRule
import com.darkrockstudios.apps.c2paviewer.usecase.trust.ClearAuthorityRuleUseCase
import com.darkrockstudios.apps.c2paviewer.usecase.trust.GetTrustAnchorsUseCase
import com.darkrockstudios.apps.c2paviewer.usecase.trust.ObserveUserTrustRulesUseCase
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
)

class TrustManagementViewModel(
	private val getTrustAnchors: GetTrustAnchorsUseCase,
	observeUserRules: ObserveUserTrustRulesUseCase,
	private val clearAuthorityRule: ClearAuthorityRuleUseCase,
) : ViewModel() {

	private val anchors = MutableStateFlow<List<TrustAnchorInfo>?>(null)

	val state: StateFlow<TrustUiState> =
		combine(anchors, observeUserRules()) { anchors, rules ->
			TrustUiState(loading = anchors == null, anchors = anchors.orEmpty(), rules = rules)
		}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TrustUiState())

	init {
		viewModelScope.launch { anchors.value = getTrustAnchors() }
	}

	fun removeRule(authorityKey: String) {
		viewModelScope.launch { clearAuthorityRule(authorityKey) }
	}
}
