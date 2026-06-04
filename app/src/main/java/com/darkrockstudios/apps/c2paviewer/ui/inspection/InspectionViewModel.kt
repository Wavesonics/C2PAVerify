package com.darkrockstudios.apps.c2paviewer.ui.inspection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.darkrockstudios.apps.c2paviewer.model.summary.InspectionResult
import com.darkrockstudios.apps.c2paviewer.model.trust.TrustRule
import com.darkrockstudios.apps.c2paviewer.model.trust.authorityKeyOf
import com.darkrockstudios.apps.c2paviewer.usecase.inspect.InspectPhotoUseCase
import com.darkrockstudios.apps.c2paviewer.usecase.trust.ClearAuthorityRuleUseCase
import com.darkrockstudios.apps.c2paviewer.usecase.trust.SetAuthorityRuleUseCase
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface InspectionUiState {
	data object Idle : InspectionUiState
	data object Loading : InspectionUiState
	data class Loaded(val result: InspectionResult) : InspectionUiState
	data class Error(val message: String) : InspectionUiState
}

class InspectionViewModel(
	private val inspectPhoto: InspectPhotoUseCase,
	private val setAuthorityRule: SetAuthorityRuleUseCase,
	private val clearAuthorityRule: ClearAuthorityRuleUseCase,
) : ViewModel() {

	private val _state = MutableStateFlow<InspectionUiState>(InspectionUiState.Idle)
	val state: StateFlow<InspectionUiState> = _state.asStateFlow()

	private var lastInspectedUri: String? = null

	/** Inspects [uri]; no-ops if it's already loaded for the same URI. */
	fun inspect(uri: String) {
		if (uri == lastInspectedUri && _state.value is InspectionUiState.Loaded) return
		lastInspectedUri = uri
		viewModelScope.launch { runInspection(uri) }
	}

	/** Sets (or clears, when [rule] is null) the user trust rule for the current signer. */
	fun setSignerRule(rule: TrustRule?) {
		val loaded = _state.value as? InspectionUiState.Loaded ?: return
		val signature = loaded.result.manifest?.activeManifest?.signature ?: return
		val key = authorityKeyOf(signature.issuer, signature.certSerialNumber) ?: return
		val displayName = signature.commonName ?: signature.issuer ?: key
		viewModelScope.launch {
			if (rule == null) {
				clearAuthorityRule(key)
			} else {
				setAuthorityRule(key, displayName, rule, System.currentTimeMillis())
			}
			lastInspectedUri?.let { runInspection(it) }
		}
	}

	private suspend fun runInspection(uri: String) {
		_state.value = InspectionUiState.Loading
		_state.value = runCatching { inspectPhoto(uri) }
			.fold(
				onSuccess = { InspectionUiState.Loaded(it) },
				onFailure = {
					Napier.e(throwable = it) { "Inspection failed for $uri" }
					InspectionUiState.Error(it.message ?: "Inspection failed")
				},
			)
	}
}
