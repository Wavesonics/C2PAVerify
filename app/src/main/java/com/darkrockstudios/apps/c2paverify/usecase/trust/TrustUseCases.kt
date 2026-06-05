package com.darkrockstudios.apps.c2paverify.usecase.trust

import com.darkrockstudios.apps.c2paverify.model.trust.TrustAnchorInfo
import com.darkrockstudios.apps.c2paverify.model.trust.TrustRule
import com.darkrockstudios.apps.c2paverify.model.trust.UserTrustRule
import com.darkrockstudios.apps.c2paverify.repository.TrustListRepository
import com.darkrockstudios.apps.c2paverify.repository.UserTrustRepository
import kotlinx.coroutines.flow.Flow

/** Snapshot of the active trust list for display. */
data class TrustListView(
	val anchors: List<TrustAnchorInfo>,
	val lastUpdatedEpochMs: Long?,
)

/** The trusted signing CAs in the active trust list, plus when it was last refreshed. */
class GetTrustListUseCase(private val trustList: TrustListRepository) {
	suspend operator fun invoke(): TrustListView =
		TrustListView(trustList.anchors(), trustList.lastUpdatedEpochMs())
}

/** Fetches the latest official trust list from the network. */
class RefreshTrustListUseCase(private val trustList: TrustListRepository) {
	suspend operator fun invoke(): Result<Unit> = trustList.refresh()
}

/** Observe the user's allow/deny rules. */
class ObserveUserTrustRulesUseCase(private val userTrust: UserTrustRepository) {
	operator fun invoke(): Flow<List<UserTrustRule>> = userTrust.observeRules()
}

/** Create or update a user allow/deny rule for a signing authority. */
class SetAuthorityRuleUseCase(private val userTrust: UserTrustRepository) {
	suspend operator fun invoke(authorityKey: String, displayName: String, rule: TrustRule, createdAt: Long) {
		userTrust.setRule(
			UserTrustRule(
				authorityKey = authorityKey,
				displayName = displayName,
				rule = rule,
				createdAt = createdAt,
			),
		)
	}
}

/** Remove a user allow/deny rule. */
class ClearAuthorityRuleUseCase(private val userTrust: UserTrustRepository) {
	suspend operator fun invoke(authorityKey: String) = userTrust.clear(authorityKey)
}
