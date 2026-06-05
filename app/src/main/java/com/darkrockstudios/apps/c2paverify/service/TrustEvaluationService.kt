package com.darkrockstudios.apps.c2paverify.service

import com.darkrockstudios.apps.c2paverify.model.c2pa.C2paManifestData
import com.darkrockstudios.apps.c2paverify.model.trust.TrustLevel
import com.darkrockstudios.apps.c2paverify.model.trust.TrustRule
import com.darkrockstudios.apps.c2paverify.model.trust.authorityKeyOf
import com.darkrockstudios.apps.c2paverify.repository.UserTrustRepository

/**
 * Resolves the [TrustLevel] of a manifest's signer by combining the user's allow/deny list with
 * c2pa's own `signingCredential.*` verdict (which reflects the configured trust list).
 *
 * Precedence: user DENY > user ALLOW > revoked certificate > c2pa trust verdict. (Integrity
 * failures are handled separately in `SummaryFactory`, which marks them TAMPERED_INVALID
 * regardless of trust.)
 */
class TrustEvaluationService(private val userTrust: UserTrustRepository) {

	suspend fun evaluate(manifest: C2paManifestData): TrustLevel {
		val signature = manifest.activeManifest?.signature
		val key = authorityKeyOf(signature?.issuer, signature?.certSerialNumber)
		val userRule = key?.let { userTrust.ruleFor(it) }

		return when (userRule?.rule) {
			TrustRule.DENY -> TrustLevel.UNTRUSTED
			TrustRule.ALLOW -> TrustLevel.TRUSTED
			null -> when {
				manifest.signerRevoked -> TrustLevel.UNTRUSTED
				manifest.signerTrusted -> TrustLevel.TRUSTED
				manifest.signerUntrusted -> TrustLevel.UNTRUSTED
				else -> TrustLevel.UNKNOWN
			}
		}
	}
}
