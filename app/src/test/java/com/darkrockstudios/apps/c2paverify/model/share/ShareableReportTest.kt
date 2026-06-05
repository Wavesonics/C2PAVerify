package com.darkrockstudios.apps.c2paverify.model.share

import com.darkrockstudios.apps.c2paverify.model.summary.OverallStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class ShareableReportTest {

	@Test
	fun `trusted maps to trusted tone`() {
		assertEquals(ReportTone.TRUSTED, toneFor(OverallStatus.SIGNED_TRUSTED))
	}

	@Test
	fun `untrusted signer maps to untrusted tone`() {
		assertEquals(ReportTone.UNTRUSTED, toneFor(OverallStatus.SIGNED_UNTRUSTED))
	}

	@Test
	fun `tampered maps to invalid tone`() {
		assertEquals(ReportTone.INVALID, toneFor(OverallStatus.TAMPERED_INVALID))
	}

	@Test
	fun `no manifest maps to neutral tone`() {
		assertEquals(ReportTone.NEUTRAL, toneFor(OverallStatus.NO_MANIFEST))
	}

	@Test
	fun `every status has a tone`() {
		// Guards against an unmapped status if OverallStatus grows.
		OverallStatus.entries.forEach { toneFor(it) }
	}
}
