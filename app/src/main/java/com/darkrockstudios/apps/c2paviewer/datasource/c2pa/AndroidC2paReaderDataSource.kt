package com.darkrockstudios.apps.c2paviewer.datasource.c2pa

import com.darkrockstudios.apps.c2paviewer.model.common.ImageSource
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.contentauth.c2pa.C2PAError
import org.contentauth.c2pa.DataStream
import org.contentauth.c2pa.FileStream
import org.contentauth.c2pa.Reader
import org.contentauth.c2pa.Stream
import java.io.File

/**
 * The ONLY file that touches the `org.contentauth.c2pa` library. All blocking JNI calls run on
 * [Dispatchers.IO]; library exceptions and the no-manifest case are mapped to [C2paRawRead] /
 * [C2paReadException] so nothing above the data layer sees c2pa types.
 */
class AndroidC2paReaderDataSource : C2paReaderDataSource {

	override suspend fun read(image: ImageSource): C2paRawRead = withContext(Dispatchers.IO) {
		val format = image.mimeTypeOrDefault()
		buildStream(image).use { stream ->
			val reader = try {
				Reader.fromStream(format, stream)
			} catch (e: C2PAError) {
				if (e.indicatesNoManifest()) {
					Napier.d(tag = TAG) { "No C2PA manifest present: ${e.message}" }
					return@withContext C2paRawRead.NoManifest
				}
				throw C2paReadException("Failed to read C2PA data: ${e.message}", e)
			}

			reader.use { r ->
				val manifestJson = r.json()
				val detailedJson = runCatching { r.detailedJson() }
					.onFailure { Napier.w(tag = TAG, throwable = it) { "detailedJson() failed" } }
					.getOrNull()
				C2paRawRead.Manifest(
					manifestJson = manifestJson,
					detailedJson = detailedJson,
					// The 0.0.9 Reader API does not expose the raw signer cert chain; trust is
					// evaluated via c2pa settings (see TrustEvaluationService) instead.
					certChainDer = emptyList(),
				)
			}
		}
	}

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
 * Best-effort detection of the "asset has no embedded manifest" case, which the native layer
 * surfaces as a generic [C2PAError.Api]. Matched leniently; refined against real device output.
 */
private fun C2PAError.indicatesNoManifest(): Boolean {
	val raw = (this as? C2PAError.Api)?.message?.lowercase() ?: return false
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
