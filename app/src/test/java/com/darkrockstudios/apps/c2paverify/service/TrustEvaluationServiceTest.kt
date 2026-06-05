package com.darkrockstudios.apps.c2paverify.service

import com.darkrockstudios.apps.c2paverify.model.trust.TrustLevel
import com.darkrockstudios.apps.c2paverify.model.trust.TrustRule
import com.darkrockstudios.apps.c2paverify.model.trust.UserTrustRule
import com.darkrockstudios.apps.c2paverify.repository.C2paManifestParser
import com.darkrockstudios.apps.c2paverify.repository.UserTrustRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class TrustEvaluationServiceTest {

	private val parser = C2paManifestParser(Json { ignoreUnknownKeys = true; isLenient = true })

	// valid-C: signer "C2PA Test Signing Cert", reported signingCredential.untrusted.
	private val untrustedManifest by lazy {
		parser.parse(
			requireNotNull(javaClass.classLoader?.getResourceAsStream("c2pa/captured/valid-C.manifest.json"))
				.bufferedReader().use { it.readText() },
		)
	}

	private fun service(rule: UserTrustRule? = null): TrustEvaluationService {
		val repo = mockk<UserTrustRepository> { coEvery { ruleFor(any()) } returns rule }
		return TrustEvaluationService(repo)
	}

	@Test
	fun `c2pa untrusted signer with no user rule is UNTRUSTED`() = runTest {
		assertEquals(TrustLevel.UNTRUSTED, service().evaluate(untrustedManifest))
	}

	@Test
	fun `user ALLOW promotes an untrusted signer to TRUSTED`() = runTest {
		val rule = UserTrustRule("k", "C2PA Test Signing Cert", TrustRule.ALLOW, createdAt = 0)
		assertEquals(TrustLevel.TRUSTED, service(rule).evaluate(untrustedManifest))
	}

	@Test
	fun `user DENY forces UNTRUSTED`() = runTest {
		val rule = UserTrustRule("k", "C2PA Test Signing Cert", TrustRule.DENY, createdAt = 0)
		assertEquals(TrustLevel.UNTRUSTED, service(rule).evaluate(untrustedManifest))
	}

	// Signer reported revoked via a stapled OCSP response.
	private val revokedManifest by lazy {
		parser.parse(
			"""
			{ "active_manifest":"m1","manifests":{"m1":{"signature_info":{"issuer":"X"}}},
			  "validation_state":"Valid",
			  "validation_results":{"activeManifest":{"failure":[
			    {"code":"signingCredential.ocsp.revoked","explanation":"revoked"}]}} }
			""".trimIndent(),
		)
	}

	@Test
	fun `revoked certificate with no user rule is UNTRUSTED`() = runTest {
		assertEquals(TrustLevel.UNTRUSTED, service().evaluate(revokedManifest))
	}

	@Test
	fun `user ALLOW still wins over a revoked certificate`() = runTest {
		val rule = UserTrustRule("k", "X", TrustRule.ALLOW, createdAt = 0)
		assertEquals(TrustLevel.TRUSTED, service(rule).evaluate(revokedManifest))
	}
}
