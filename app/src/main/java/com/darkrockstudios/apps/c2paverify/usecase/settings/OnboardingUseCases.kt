package com.darkrockstudios.apps.c2paverify.usecase.settings

import com.darkrockstudios.apps.c2paverify.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow

/** Observes whether the one-time intro slideshow has been completed or skipped. */
class ObserveOnboardingSeenUseCase(private val settings: SettingsRepository) {
	operator fun invoke(): Flow<Boolean> = settings.onboardingSeen
}

/** Records that the intro slideshow has been completed or skipped. */
class MarkOnboardingSeenUseCase(private val settings: SettingsRepository) {
	suspend operator fun invoke() = settings.markOnboardingSeen()
}
