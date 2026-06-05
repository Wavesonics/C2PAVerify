package com.darkrockstudios.apps.c2paverify.datasource.image

import android.content.Context
import androidx.core.net.toUri
import com.darkrockstudios.apps.c2paverify.model.common.ImageSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Reads image bytes from a content URI via [Context.getContentResolver], on [Dispatchers.IO].
 * Bundled examples use the `file:///android_asset/...` scheme (the same one Coil understands for
 * display) and are read through the [android.content.res.AssetManager]. Android-specific; isolates
 * `android.*`/URI handling out of the KMP-clean upper layers.
 */
class ImageBytesDataSource(private val context: Context) {

	suspend fun read(uriString: String): ImageSource.Bytes = withContext(Dispatchers.IO) {
		if (uriString.startsWith(ASSET_URI_PREFIX)) {
			val assetPath = uriString.removePrefix(ASSET_URI_PREFIX)
			val bytes = context.assets.open(assetPath).use { it.readBytes() }
			ImageSource.Bytes(bytes = bytes, mimeType = mimeTypeFor(assetPath))
		} else {
			val uri = uriString.toUri()
			val resolver = context.contentResolver
			val mimeType = resolver.getType(uri)
			val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
				?: throw IOException("Unable to open image stream for $uriString")
			ImageSource.Bytes(bytes = bytes, mimeType = mimeType)
		}
	}

	private fun mimeTypeFor(path: String): String? = when {
		path.endsWith(".jpg", ignoreCase = true) || path.endsWith(".jpeg", ignoreCase = true) -> "image/jpeg"
		path.endsWith(".png", ignoreCase = true) -> "image/png"
		path.endsWith(".webp", ignoreCase = true) -> "image/webp"
		else -> null
	}

	private companion object {
		const val ASSET_URI_PREFIX = "file:///android_asset/"
	}
}
