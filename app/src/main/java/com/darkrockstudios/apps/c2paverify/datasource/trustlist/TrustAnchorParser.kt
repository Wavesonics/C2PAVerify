package com.darkrockstudios.apps.c2paverify.datasource.trustlist

import com.darkrockstudios.apps.c2paverify.model.trust.TrustAnchorInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

/**
 * Parses a PEM bundle of trust anchors into displayable [TrustAnchorInfo] entries using
 * `java.security` (Android/JVM crypto). A data source so the crypto stays out of the KMP-clean
 * upper layers.
 */
class TrustAnchorParser {

	suspend fun parse(pem: String): List<TrustAnchorInfo> = withContext(Dispatchers.IO) {
		val factory = CertificateFactory.getInstance("X.509")
		pemCertificateBlocks(pem).mapNotNull { block ->
			runCatching {
				val cert = factory.generateCertificate(block.byteInputStream()) as X509Certificate
				val subject = cert.subjectX500Principal.name
				TrustAnchorInfo(
					displayName = subject.commonNameOrOrg() ?: subject,
					subject = subject,
					notBeforeEpochMs = cert.notBefore?.time,
					notAfterEpochMs = cert.notAfter?.time,
				)
			}.getOrNull()
		}.sortedBy { it.displayName.lowercase() }
	}

	/**
	 * Returns [pem] with any certificate whose subject is in [excludedSubjects] removed, so a
	 * dis-allowed CA is no longer a trust anchor. Subjects are computed exactly as in [parse], so
	 * the values stored from a [TrustAnchorInfo.subject] match. Unparseable blocks are kept.
	 */
	suspend fun filterPem(pem: String, excludedSubjects: Set<String>): String =
		withContext(Dispatchers.IO) {
			if (excludedSubjects.isEmpty()) return@withContext pem
			val factory = CertificateFactory.getInstance("X.509")
			pemCertificateBlocks(pem).filter { block ->
				val subject = runCatching {
					(factory.generateCertificate(block.byteInputStream()) as X509Certificate).subjectX500Principal.name
				}.getOrNull()
				subject == null || subject !in excludedSubjects
			}.joinToString("\n")
		}

	private fun pemCertificateBlocks(pem: String): List<String> {
		val begin = "-----BEGIN CERTIFICATE-----"
		val end = "-----END CERTIFICATE-----"
		val blocks = mutableListOf<String>()
		var index = 0
		while (true) {
			val start = pem.indexOf(begin, index)
			if (start < 0) break
			val stop = pem.indexOf(end, start)
			if (stop < 0) break
			blocks.add(pem.substring(start, stop + end.length))
			index = stop + end.length
		}
		return blocks
	}

	/** Extracts CN= (or falls back to O=) from an RFC 2253 distinguished name. */
	private fun String.commonNameOrOrg(): String? =
		rdnValue("CN") ?: rdnValue("O")

	private fun String.rdnValue(key: String): String? =
		Regex("(?:^|,)\\s*$key=([^,]+)").find(this)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }
}
