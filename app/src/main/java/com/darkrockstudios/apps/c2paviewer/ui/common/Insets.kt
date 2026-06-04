package com.darkrockstudios.apps.c2paviewer.ui.common

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalLayoutDirection

/**
 * Sums two [PaddingValues] respecting layout direction. Used to fold system-bar insets into a
 * scrollable list's `contentPadding` so the list scrolls edge-to-edge (under the bars) while its
 * first/last items still clear them.
 */
@Composable
@ReadOnlyComposable
operator fun PaddingValues.plus(other: PaddingValues): PaddingValues {
	val direction = LocalLayoutDirection.current
	return PaddingValues(
		start = calculateStartPadding(direction) + other.calculateStartPadding(direction),
		top = calculateTopPadding() + other.calculateTopPadding(),
		end = calculateEndPadding(direction) + other.calculateEndPadding(direction),
		bottom = calculateBottomPadding() + other.calculateBottomPadding(),
	)
}
