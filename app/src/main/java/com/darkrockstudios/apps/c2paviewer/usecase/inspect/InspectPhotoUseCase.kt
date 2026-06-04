package com.darkrockstudios.apps.c2paviewer.usecase.inspect

import com.darkrockstudios.apps.c2paviewer.model.c2pa.C2paManifestData
import com.darkrockstudios.apps.c2paviewer.model.summary.InspectionResult
import com.darkrockstudios.apps.c2paviewer.model.summary.SummaryFactory
import com.darkrockstudios.apps.c2paviewer.model.trust.TrustLevel
import com.darkrockstudios.apps.c2paviewer.repository.C2paManifestRepository
import com.darkrockstudios.apps.c2paviewer.repository.ImageRepository

/**
 * Orchestrates a full inspection: load image bytes, read+parse the C2PA manifest, derive the
 * trust level, and assemble the summary. Stateless use case combining two repositories and the
 * pure [SummaryFactory].
 *
 * NOTE: until the trust list is configured (step 4b), [TrustLevel] is derived directly from the
 * manifest's `signingCredential.*` codes, so a valid manifest with an unrecognized signer reads
 * as UNTRUSTED.
 */
class InspectPhotoUseCase(
	private val imageRepository: ImageRepository,
	private val manifestRepository: C2paManifestRepository,
) {
	suspend operator fun invoke(imageUri: String): InspectionResult {
		val image = imageRepository.load(imageUri)
		return when (val result = manifestRepository.inspect(image)) {
			is C2paManifestRepository.ManifestResult.NoManifest ->
				InspectionResult(
					summary = SummaryFactory.buildSummary(manifest = null, trust = TrustLevel.UNKNOWN),
					manifest = null,
				)

			is C2paManifestRepository.ManifestResult.Present ->
				InspectionResult(
					summary = SummaryFactory.buildSummary(result.data, deriveTrust(result.data)),
					manifest = result.data,
				)
		}
	}

	private fun deriveTrust(data: C2paManifestData): TrustLevel = when {
		data.signerTrusted -> TrustLevel.TRUSTED
		data.signerUntrusted -> TrustLevel.UNTRUSTED
		else -> TrustLevel.UNKNOWN
	}
}
