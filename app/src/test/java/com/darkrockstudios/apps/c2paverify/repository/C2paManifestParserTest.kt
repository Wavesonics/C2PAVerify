package com.darkrockstudios.apps.c2paverify.repository

import com.darkrockstudios.apps.c2paverify.model.c2pa.ManifestValidationState
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Parses REAL c2pa-android `reader.json()` output captured on-device (see
 * `app/src/test/resources/c2pa/captured`, produced by C2paReaderCaptureTest). Runs on the JVM,
 * no device/native lib needed.
 */
class C2paManifestParserTest {

	private val parser = C2paManifestParser(Json { ignoreUnknownKeys = true; isLenient = true })

	private fun fixture(name: String): String =
		requireNotNull(javaClass.classLoader?.getResourceAsStream("c2pa/captured/$name")) {
			"Missing fixture c2pa/captured/$name"
		}.bufferedReader().use { it.readText() }

	@Test
	fun `valid manifest parses as VALID with signer info and actions`() {
		val data = parser.parse(fixture("valid-C.manifest.json"))

		assertEquals(ManifestValidationState.VALID, data.validationState)
		val active = data.activeManifest
		assertNotNull(active); active!!
		assertEquals("C.jpg", active.title)
		assertEquals("image/jpeg", active.format)
		assertEquals("C2PA Test Signing Cert", active.signature?.issuer)
		assertEquals("C2PA Signer", active.signature?.commonName)
		assertEquals("Ps256", active.signature?.algorithm)
		// c2pa-android emits the v2 actions label.
		assertTrue(active.assertions.any { it.label == "c2pa.actions.v2" })
		// No trust list configured yet → signer reported untrusted, but integrity is intact.
		assertTrue(data.signerUntrusted)
		assertTrue(data.integrityFailures.isEmpty())
	}

	@Test
	fun `invalid signature parses as INVALID with an integrity failure`() {
		val data = parser.parse(fixture("invalid-sig.manifest.json"))

		assertEquals(ManifestValidationState.INVALID, data.validationState)
		assertTrue(
			"expected a claimSignature.mismatch integrity failure",
			data.integrityFailures.any { it.code == "claimSignature.mismatch" },
		)
	}

	@Test
	fun `tampered data hash parses as INVALID`() {
		val data = parser.parse(fixture("tampered-dat.manifest.json"))
		assertEquals(ManifestValidationState.INVALID, data.validationState)
		assertTrue(data.integrityFailures.isNotEmpty())
	}

	@Test
	fun `multi-ingredient manifest exposes its ingredients`() {
		val data = parser.parse(fixture("valid-multi-CAICAI.manifest.json"))
		val active = data.activeManifest
		assertNotNull(active)
		assertTrue("expected at least one ingredient", active!!.ingredients.isNotEmpty())
		assertFalse(active.assertions.isEmpty())
	}
}
