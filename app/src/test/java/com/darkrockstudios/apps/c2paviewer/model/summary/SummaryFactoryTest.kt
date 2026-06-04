package com.darkrockstudios.apps.c2paviewer.model.summary

import com.darkrockstudios.apps.c2paviewer.model.trust.TrustLevel
import com.darkrockstudios.apps.c2paviewer.repository.C2paManifestParser
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SummaryFactoryTest {

	private val parser = C2paManifestParser(Json { ignoreUnknownKeys = true; isLenient = true })

	private fun fixture(name: String): String =
		requireNotNull(javaClass.classLoader?.getResourceAsStream("c2pa/captured/$name")) {
			"Missing fixture $name"
		}.bufferedReader().use { it.readText() }

	@Test
	fun `null manifest is NO_MANIFEST`() {
		val summary = SummaryFactory.buildSummary(manifest = null, trust = TrustLevel.UNKNOWN)
		assertEquals(OverallStatus.NO_MANIFEST, summary.status)
		assertFalse(summary.manifestPresent)
	}

	@Test
	fun `valid manifest maps to trusted or untrusted by trust level`() {
		val data = parser.parse(fixture("valid-C.manifest.json"))
		assertEquals(
			OverallStatus.SIGNED_UNTRUSTED,
			SummaryFactory.buildSummary(data, TrustLevel.UNTRUSTED).status,
		)
		assertEquals(
			OverallStatus.SIGNED_TRUSTED,
			SummaryFactory.buildSummary(data, TrustLevel.TRUSTED).status,
		)
	}

	@Test
	fun `integrity failure is TAMPERED_INVALID even if trusted`() {
		val data = parser.parse(fixture("invalid-sig.manifest.json"))
		assertEquals(
			OverallStatus.TAMPERED_INVALID,
			SummaryFactory.buildSummary(data, TrustLevel.TRUSTED).status,
		)
	}

	@Test
	fun `signer info is carried into the summary`() {
		val data = parser.parse(fixture("valid-C.manifest.json"))
		val summary = SummaryFactory.buildSummary(data, TrustLevel.UNTRUSTED)
		assertEquals("C2PA Test Signing Cert", summary.signerName)
		assertEquals("C2PA Signer", summary.signerCommonName)
	}

	@Test
	fun `detectAi flags trainedAlgorithmicMedia`() {
		val aiJson = """
			{
			  "active_manifest": "m1",
			  "manifests": {
			    "m1": {
			      "assertions": [
			        { "label": "c2pa.actions.v2", "data": { "actions": [
			          { "action": "c2pa.created",
			            "digitalSourceType": "http://cv.iptc.org/newscodes/digitalsourcetype/trainedAlgorithmicMedia" }
			        ] } }
			      ]
			    }
			  },
			  "validation_state": "Valid"
			}
		""".trimIndent()
		val ai = SummaryFactory.detectAi(parser.parse(aiJson))
		assertTrue(ai.isAiGenerated)
		assertTrue(ai.sourceTypes.any { it.contains("trainedAlgorithmicMedia") })
	}

	@Test
	fun `detectAi is false for an ordinary edited image`() {
		val data = parser.parse(fixture("valid-multi-CAICAI.manifest.json"))
		assertFalse(SummaryFactory.detectAi(data).isAiGenerated)
	}

	@Test
	fun `detectAi ignores AI only in ingredient provenance, not the asset's own claim`() {
		// Active manifest is a plain capture; only an *ingredient* manifest is AI-generated.
		// The asset itself is not AI-generated, so the headline flag must stay false.
		val json = """
			{
			  "active_manifest": "active",
			  "manifests": {
			    "active": {
			      "assertions": [
			        { "label": "c2pa.actions.v2", "data": { "actions": [
			          { "action": "c2pa.created",
			            "digitalSourceType": "http://cv.iptc.org/newscodes/digitalsourcetype/digitalCapture" }
			        ] } }
			      ]
			    },
			    "ingredient": {
			      "assertions": [
			        { "label": "c2pa.actions.v2", "data": { "actions": [
			          { "action": "c2pa.created",
			            "digitalSourceType": "http://cv.iptc.org/newscodes/digitalsourcetype/trainedAlgorithmicMedia" }
			        ] } }
			      ]
			    }
			  },
			  "validation_state": "Valid"
			}
		""".trimIndent()
		assertFalse(SummaryFactory.detectAi(parser.parse(json)).isAiGenerated)
	}
}
