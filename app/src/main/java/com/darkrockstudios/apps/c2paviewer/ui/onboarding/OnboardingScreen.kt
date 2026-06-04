package com.darkrockstudios.apps.c2paviewer.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import com.darkrockstudios.apps.c2paviewer.R
import kotlinx.coroutines.launch

private data class OnboardingPage(
	val icon: ImageVector,
	val titleRes: Int,
	val bodyRes: Int,
)

private val onboardingPages = listOf(
	OnboardingPage(Icons.Filled.Info, R.string.onboarding_1_title, R.string.onboarding_1_body),
	OnboardingPage(Icons.Filled.CheckCircle, R.string.onboarding_2_title, R.string.onboarding_2_body),
	OnboardingPage(Icons.Filled.Share, R.string.onboarding_3_title, R.string.onboarding_3_body),
)

/**
 * One-time intro slideshow shown above the app on first launch (and when replayed). An opaque
 * [Surface] so it fully covers whatever is behind it; [onFinish] is called on Skip or "Get started".
 */
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
	val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
	val scope = rememberCoroutineScope()
	val lastPage = pagerState.currentPage == onboardingPages.lastIndex

	Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
		Column(
			modifier = Modifier
				.fillMaxSize()
				.statusBarsPadding()
				.navigationBarsPadding()
				.padding(24.dp),
		) {
			Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
				TextButton(onClick = onFinish) { Text(stringResource(R.string.onboarding_skip)) }
			}

			HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
				OnboardingSlide(onboardingPages[page])
			}

			PageIndicator(count = onboardingPages.size, current = pagerState.currentPage)
			Spacer(Modifier.size(24.dp))

			Button(
				onClick = {
					if (lastPage) onFinish()
					else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
				},
				modifier = Modifier
					.align(Alignment.CenterHorizontally)
					.widthIn(max = 400.dp)
					.fillMaxWidth(),
			) {
				Text(stringResource(if (lastPage) R.string.onboarding_done else R.string.onboarding_next))
			}
		}
	}
}

@Composable
private fun OnboardingSlide(page: OnboardingPage) {
	Column(
		modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.Center,
	) {
		Icon(
			imageVector = page.icon,
			contentDescription = null,
			modifier = Modifier.size(96.dp),
			tint = MaterialTheme.colorScheme.primary,
		)
		Spacer(Modifier.size(32.dp))
		Text(
			text = stringResource(page.titleRes),
			style = MaterialTheme.typography.headlineSmall,
			textAlign = TextAlign.Center,
		)
		Spacer(Modifier.size(16.dp))
		Text(
			text = stringResource(page.bodyRes),
			style = MaterialTheme.typography.bodyLarge,
			color = MaterialTheme.colorScheme.onSurfaceVariant,
			textAlign = TextAlign.Center,
		)
	}
}

@Composable
private fun PageIndicator(count: Int, current: Int) {
	Row(
		modifier = Modifier.fillMaxWidth(),
		horizontalArrangement = Arrangement.Center,
	) {
		repeat(count) { index ->
			val active = index == current
			Box(
				modifier = Modifier
					.padding(horizontal = 4.dp)
					.size(if (active) 10.dp else 8.dp)
					.clip(CircleShape)
					.background(
						if (active) MaterialTheme.colorScheme.primary
						else MaterialTheme.colorScheme.outlineVariant,
					),
			)
		}
	}
}
