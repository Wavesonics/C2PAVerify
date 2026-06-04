package com.darkrockstudios.apps.c2paviewer.usecase.inspect

import com.darkrockstudios.apps.c2paviewer.model.summary.InspectionResult
import com.darkrockstudios.apps.c2paviewer.model.summary.SummaryFactory
import com.darkrockstudios.apps.c2paviewer.model.trust.TrustLevel
import com.darkrockstudios.apps.c2paviewer.repository.C2paManifestRepository
import com.darkrockstudios.apps.c2paviewer.repository.ImageRepository
import com.darkrockstudios.apps.c2paviewer.repository.TrustListRepository
import com.darkrockstudios.apps.c2paviewer.service.TrustEvaluationService

/**
 * Orchestrates a full inspection: load image bytes, read+parse the C2PA manifest against the
 * active trust list, derive the trust level (c2pa verdict + user allow/deny), and assemble the
 * summary. Stateless use case combining repositories + the trust service + the pure SummaryFactory.
 */
class InspectPhotoUseCase(
	private val imageRepository: ImageRepository,
	private val manifestRepository: C2paManifestRepository,
	private val trustListRepository: TrustListRepository,
	private val trustEvaluationService: TrustEvaluationService,
) {
	suspend operator fun invoke(imageUri: String): InspectionResult {
		val image = imageRepository.load(imageUri)
		val trust = trustListRepository.current()
		return when (val result = manifestRepository.inspect(image, trust)) {
			is C2paManifestRepository.ManifestResult.NoManifest ->
				InspectionResult(
					summary = SummaryFactory.buildSummary(manifest = null, trust = TrustLevel.UNKNOWN),
					manifest = null,
				)

			is C2paManifestRepository.ManifestResult.Present -> {
				val level = trustEvaluationService.evaluate(result.data)
				InspectionResult(
					summary = SummaryFactory.buildSummary(result.data, level),
					manifest = result.data,
				)
			}
		}
	}
}
