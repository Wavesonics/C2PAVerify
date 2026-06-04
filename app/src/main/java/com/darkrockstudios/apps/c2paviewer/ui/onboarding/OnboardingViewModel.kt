package com.darkrockstudios.apps.c2paviewer.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.darkrockstudios.apps.c2paviewer.usecase.settings.MarkOnboardingSeenUseCase
import com.darkrockstudios.apps.c2paviewer.usecase.settings.ObserveOnboardingSeenUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Drives the one-time intro slideshow. The slideshow shows when it hasn't been seen yet, or when
 * the user explicitly replays it from the landing screen. [showOnboarding] is `null` until the
 * persisted flag has loaded (so we don't flash the slideshow on a cold start).
 */
class OnboardingViewModel(
	observeOnboardingSeen: ObserveOnboardingSeenUseCase,
	private val markOnboardingSeen: MarkOnboardingSeenUseCase,
) : ViewModel() {

	private val replayRequested = MutableStateFlow(false)

	val showOnboarding: StateFlow<Boolean?> =
		combine(observeOnboardingSeen(), replayRequested) { seen, replay -> replay || !seen }
			.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

	/** User finished or skipped the slideshow: persist the flag and clear any replay request. */
	fun finish() {
		replayRequested.value = false
		viewModelScope.launch { markOnboardingSeen() }
	}

	/** Re-open the slideshow on demand (e.g. from a "How it works" affordance). */
	fun replay() {
		replayRequested.value = true
	}
}
