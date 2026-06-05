package com.darkrockstudios.apps.c2paverify.model.trust

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthorityKeyTest {

	@Test
	fun `anchor key round-trips the subject`() {
		val subject = "CN=Some CA, O=Example, C=US"
		val key = anchorAuthorityKey(subject)
		assertEquals(subject, anchorSubjectOf(key))
	}

	@Test
	fun `per-signer key is not treated as an anchor key`() {
		val signerKey = authorityKeyOf("CN=Signer", "0123456789")
		assertNull(anchorSubjectOf(signerKey!!))
	}

	@Test
	fun `anchor and per-signer keys do not collide`() {
		// A signer whose issuer string equals a CA subject still produces a distinct key.
		val subject = "CN=Acme CA"
		assertEquals(true, anchorAuthorityKey(subject) != authorityKeyOf(subject, ""))
	}
}
