package com.darkrockstudios.apps.c2paviewer.repository

import com.darkrockstudios.apps.c2paviewer.datasource.report.ReportRendererDataSource
import com.darkrockstudios.apps.c2paviewer.model.common.ImageSource
import com.darkrockstudios.apps.c2paviewer.model.share.ReportOverlay

/**
 * Produces shareable verification reports. Thin seam over [ReportRendererDataSource]; keeps the
 * `android.graphics`/`FileProvider` work in the data layer so this stays KMP-clean.
 *
 * @return a `content://` URI string (FileProvider) for the rendered PNG.
 */
class ReportRepository(private val renderer: ReportRendererDataSource) {
	suspend fun renderShareable(image: ImageSource, overlay: ReportOverlay): String =
		renderer.render(image, overlay)
}
