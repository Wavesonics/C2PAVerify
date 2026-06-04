package com.darkrockstudios.apps.c2paviewer.datasource.image

import android.content.Context
import androidx.core.net.toUri
import com.darkrockstudios.apps.c2paviewer.model.common.ImageSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Reads image bytes from a content URI via [Context.getContentResolver], on [Dispatchers.IO].
 * Android-specific; isolates `android.*`/URI handling out of the KMP-clean upper layers.
 */
class ImageBytesDataSource(private val context: Context) {

	suspend fun read(uriString: String): ImageSource.Bytes = withContext(Dispatchers.IO) {
		val uri = uriString.toUri()
		val resolver = context.contentResolver
		val mimeType = resolver.getType(uri)
		val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
			?: throw IOException("Unable to open image stream for $uriString")
		ImageSource.Bytes(bytes = bytes, mimeType = mimeType)
	}
}
