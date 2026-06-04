package com.darkrockstudios.apps.c2paviewer.repository

import com.darkrockstudios.apps.c2paviewer.datasource.image.ImageBytesDataSource
import com.darkrockstudios.apps.c2paviewer.model.common.ImageSource

/**
 * Provides image bytes for inspection as a platform-neutral [ImageSource]. Thin seam over
 * [ImageBytesDataSource]; keeps `android.*`/URI handling in the data layer. (Coil/Telephoto load
 * the URI directly in the UI; this path is for feeding the C2PA reader.)
 */
class ImageRepository(private val imageBytes: ImageBytesDataSource) {
	suspend fun load(uriString: String): ImageSource = imageBytes.read(uriString)
}
