package com.darkrockstudios.apps.c2paverify.datasource.c2pa

import com.darkrockstudios.apps.c2paverify.model.common.ImageSource
import com.darkrockstudios.apps.c2paverify.model.trust.TrustMaterial
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.contentauth.c2pa.C2PAContext
import org.contentauth.c2pa.C2PAError
import org.contentauth.c2pa.C2PASettings
import org.contentauth.c2pa.DataStream
import org.contentauth.c2pa.FileStream
import org.contentauth.c2pa.Reader
import org.contentauth.c2pa.Stream
import org.contentauth.c2pa.settings.C2PASettingsDefinition
import org.contentauth.c2pa.settings.TrustSettings
import org.contentauth.c2pa.settings.VerifySettings
import java.io.File

/**
 * The ONLY file that touches the `org.contentauth.c2pa` library. All blocking JNI calls run on
 * [Dispatchers.IO]; library exceptions and the no-manifest case are mapped to [C2paRawRead] /
 * [C2paReadException] so nothing above the data layer sees c2pa types.
 *
 * When [TrustMaterial] is supplied, the reader is built from [C2PASettings] with trust
 * verification enabled, so the manifest JSON carries `signingCredential.trusted` / `.untrusted`.
 */
class AndroidC2paReaderDataSource : C2paReaderDataSource {

	override suspend fun read(image: ImageSource, trust: TrustMaterial?): C2paRawRead =
		withContext(Dispatchers.IO) {
			val format = image.mimeTypeOrDefault()
			buildStream(image).use { stream ->
				try {
					openAndExtract(format, stream, trust)
				} catch (e: C2PAError) {
					if (e.indicatesNoManifest()) {
						Napier.d(tag = TAG) { "No C2PA manifest present: ${e.message}" }
						C2paRawRead.NoManifest
					} else {
						throw C2paReadException("Failed to read C2PA data: ${e.message}", e)
					}
				}
			}
		}

	private fun openAndExtract(format: String, stream: Stream, trust: TrustMaterial?): C2paRawRead =
		if (trust != null && trust.hasAnchors) {
			// Settings may be freed once the context is built; the context must outlive the reader.
			val settings = C2PASettings.fromDefinition(buildDefinition(trust))
			val context = try {
				C2PAContext.fromSettings(settings)
			} finally {
				settings.close()
			}
			context.use { ctx ->
				Reader.fromContext(ctx).withStream(format, stream).use { extract(it) }
			}
		} else {
			Reader.fromStream(format, stream).use { extract(it) }
		}

	private fun extract(reader: Reader): C2paRawRead.Manifest {
		val manifestJson = reader.json()
		val detailedJson = runCatching { reader.detailedJson() }
			.onFailure { Napier.w(tag = TAG, throwable = it) { "detailedJson() failed" } }
			.getOrNull()
		return C2paRawRead.Manifest(
			manifestJson = manifestJson,
			detailedJson = detailedJson,
			certChainDer = emptyList(),
		)
	}

	private fun buildDefinition(trust: TrustMaterial) = C2PASettingsDefinition(
		trust = TrustSettings(
			verifyTrustList = true,
			trustAnchors = trust.trustAnchorsPem,
			allowedList = trust.allowedListPem,
			trustConfig = trust.trustConfig,
		),
		verify = VerifySettings(verifyTrust = true, verifyTimestampTrust = true),
	)

	private fun buildStream(image: ImageSource): Stream = when (image) {
		is ImageSource.Bytes -> DataStream(image.bytes)
		is ImageSource.Path -> FileStream(File(image.path), FileStream.Mode.READ)
	}

	private fun ImageSource.mimeTypeOrDefault(): String = when (this) {
		is ImageSource.Bytes -> mimeType
		is ImageSource.Path -> mimeType
	} ?: DEFAULT_MIME

	private companion object {
		const val TAG = "C2paReader"
		const val DEFAULT_MIME = "image/jpeg"
	}
}

/**
 * Best-effort detection of the "asset has no embedded manifest" case. The native layer surfaces it
 * as a [C2PAError] whose message mentions a missing manifest/JUMBF (the exact subtype varies, e.g.
 * `ManifestNotFound: no JUMBF data found`), so match on the message text of any subtype.
 */
private fun C2PAError.indicatesNoManifest(): Boolean {
	val raw = message?.lowercase() ?: return false
	val compact = raw.replace(" ", "").replace("_", "")
	return listOf(
		"manifestnotfound",
		"jumbfnotfound",
		"nomanifest",
		"noclaim",
		"claimmissing",
		"noembedded",
	).any { it in compact }
}
