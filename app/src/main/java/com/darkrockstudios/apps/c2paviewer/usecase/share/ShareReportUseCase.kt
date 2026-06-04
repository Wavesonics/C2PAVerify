package com.darkrockstudios.apps.c2paviewer.usecase.share

import com.darkrockstudios.apps.c2paviewer.model.share.ReportOverlay
import com.darkrockstudios.apps.c2paviewer.repository.ImageRepository
import com.darkrockstudios.apps.c2paviewer.repository.ReportRepository

/**
 * Builds a shareable verification report for an already-inspected photo: loads the image bytes and
 * renders the [overlay] onto them. Stateless use case combining the image + report repositories.
 *
 * @return a `content://` URI string the caller can drop into a share intent.
 */
class ShareReportUseCase(
	private val imageRepository: ImageRepository,
	private val reportRepository: ReportRepository,
) {
	suspend operator fun invoke(imageUri: String, overlay: ReportOverlay): String {
		val image = imageRepository.load(imageUri)
		return reportRepository.renderShareable(image, overlay)
	}
}
