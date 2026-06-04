package com.darkrockstudios.apps.c2paviewer.usecase.trust

import com.darkrockstudios.apps.c2paviewer.model.trust.TrustAnchorInfo
import com.darkrockstudios.apps.c2paviewer.model.trust.TrustRule
import com.darkrockstudios.apps.c2paviewer.model.trust.UserTrustRule
import com.darkrockstudios.apps.c2paviewer.repository.TrustListRepository
import com.darkrockstudios.apps.c2paviewer.repository.UserTrustRepository
import kotlinx.coroutines.flow.Flow

/** The trusted signing CAs in the active trust list. */
class GetTrustAnchorsUseCase(private val trustList: TrustListRepository) {
	suspend operator fun invoke(): List<TrustAnchorInfo> = trustList.anchors()
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
