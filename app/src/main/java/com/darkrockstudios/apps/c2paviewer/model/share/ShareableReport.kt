package com.darkrockstudios.apps.c2paviewer.model.share

import com.darkrockstudios.apps.c2paviewer.model.summary.OverallStatus

/** Colour family for the status accent drawn on a shareable report. */
enum class ReportTone { TRUSTED, UNTRUSTED, INVALID, NEUTRAL }

/**
 * Everything the (Android) report renderer needs to draw the verification overlay onto a photo,
 * expressed as already-localised, platform-neutral values. The UI layer resolves string resources
 * and builds this; the renderer only paints. Keeping it pure leaves the upper layers KMP-clean.
 */
data class ReportOverlay(
	val statusLabel: String,
	val tone: ReportTone,
	/** Signer / generator / signed-time lines, in display order. May be empty. */
	val details: List<String>,
	val isAiGenerated: Boolean,
	val aiLabel: String,
	/** Brand mark, e.g. "Verified with C2PAViewer". */
	val watermark: String,
)

/** Maps the inspection verdict to a report accent tone. */
fun toneFor(status: OverallStatus): ReportTone = when (status) {
	OverallStatus.SIGNED_TRUSTED -> ReportTone.TRUSTED
	OverallStatus.SIGNED_UNTRUSTED -> ReportTone.UNTRUSTED
	OverallStatus.TAMPERED_INVALID -> ReportTone.INVALID
	OverallStatus.NO_MANIFEST -> ReportTone.NEUTRAL
}
