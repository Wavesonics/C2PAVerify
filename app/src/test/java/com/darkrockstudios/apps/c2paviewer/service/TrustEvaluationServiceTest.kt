package com.darkrockstudios.apps.c2paviewer.service

import com.darkrockstudios.apps.c2paviewer.model.trust.TrustLevel
import com.darkrockstudios.apps.c2paviewer.model.trust.TrustRule
import com.darkrockstudios.apps.c2paviewer.model.trust.UserTrustRule
import com.darkrockstudios.apps.c2paviewer.repository.C2paManifestParser
import com.darkrockstudios.apps.c2paviewer.repository.UserTrustRepository
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
}
