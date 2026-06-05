package com.darkrockstudios.apps.c2paviewer.datasource.report

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.content.FileProvider
import com.darkrockstudios.apps.c2paviewer.model.common.ImageSource
import com.darkrockstudios.apps.c2paviewer.model.share.ReportBadgeStyle
import com.darkrockstudios.apps.c2paviewer.model.share.ReportOverlay
import com.darkrockstudios.apps.c2paviewer.model.share.ReportTone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * Renders a shareable "verification report": the inspected photo with a [ReportOverlay] panel
 * painted across the bottom (status accent, signer/generator details, an AI badge and a brand
 * mark). Writes a PNG into the app's cache and hands back a `content://` [FileProvider] URI string
 * the share sheet can read.
 *
 * Android-only (`android.graphics` + `FileProvider`); isolates that out of the KMP-clean layers.
 * All decoding/encoding runs on [Dispatchers.IO].
 */
class ReportRendererDataSource(private val context: Context) {

	suspend fun render(image: ImageSource, overlay: ReportOverlay): String = withContext(Dispatchers.IO) {
		val photo = decodeScaled(image) ?: throw IOException("Unable to decode image for report")
		val canvasBitmap = try {
			drawReport(photo, overlay)
		} finally {
			photo.recycle()
		}
		val uri = try {
			writeToCache(canvasBitmap)
		} finally {
			canvasBitmap.recycle()
		}
		uri
	}

	private fun decodeScaled(image: ImageSource): Bitmap? {
		val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
		decodeInto(image, bounds)
		val opts = BitmapFactory.Options().apply {
			inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight)
			inPreferredConfig = Bitmap.Config.ARGB_8888
		}
		return decodeInto(image, opts)
	}

	private fun decodeInto(image: ImageSource, opts: BitmapFactory.Options): Bitmap? = when (image) {
		is ImageSource.Bytes -> BitmapFactory.decodeByteArray(image.bytes, 0, image.bytes.size, opts)
		is ImageSource.Path -> BitmapFactory.decodeFile(image.path, opts)
	}

	/** Largest power-of-two subsample that keeps the long edge at/under [MAX_EDGE_PX] (OOM guard). */
	private fun sampleSizeFor(width: Int, height: Int): Int {
		var sample = 1
		var longEdge = maxOf(width, height)
		while (longEdge / 2 >= MAX_EDGE_PX) {
			longEdge /= 2
			sample *= 2
		}
		return sample
	}

	private fun drawReport(photo: Bitmap, overlay: ReportOverlay): Bitmap {
		val width = photo.width
		val height = photo.height
		val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
		val canvas = Canvas(out)
		canvas.drawBitmap(photo, 0f, 0f, null)

		// Scale every dimension off the short edge so the panel reads the same on any resolution.
		val unit = minOf(width, height) / 28f
		val margin = unit
		val padding = unit * 1.1f
		val titleSize = unit * 1.7f
		val bodySize = unit * 1.15f
		val markSize = unit * 0.95f
		val lineGap = bodySize * 0.55f
		val sectionGap = bodySize * 0.8f

		val titlePaint = textPaint(titleSize, Typeface.create(Typeface.DEFAULT, Typeface.BOLD))
		val taglinePaint = textPaint(bodySize, Typeface.DEFAULT).apply { alpha = 200 }
		val bodyPaint = textPaint(bodySize, Typeface.DEFAULT).apply { alpha = 230 }
		val trustPaint = textPaint(bodySize, Typeface.create(Typeface.DEFAULT, Typeface.BOLD))
		val markPaint = textPaint(markSize, Typeface.DEFAULT).apply { alpha = 150 }
		val badgePaint = textPaint(bodySize, Typeface.create(Typeface.DEFAULT, Typeface.BOLD))

		val contentWidth = width - 2 * margin - 2 * padding

		// The hero accent: the origin badge colour, or the trust tone when the verdict leads.
		val accentColor = overlay.headlineStyle?.let { badgeColor(it) } ?: toneColor(overlay.tone)

		// Hero headline (+ tagline), wrapped to fit beside the accent dot.
		val dotR = titleSize * 0.32f
		val headlineInset = dotR * 2 + padding * 0.5f
		val headlineLines = wrap(overlay.headline, titlePaint, contentWidth - headlineInset)
		val taglineLines = overlay.tagline?.let { wrap(it, taglinePaint, contentWidth) }.orEmpty()

		// Secondary pills, laid into wrapping rows up front so we can size the panel.
		val pillHeight = bodySize * 2.1f
		val pillGap = padding * 0.5f
		val pills = overlay.badges.map { badge ->
			val text = "${badgeGlyph(badge.style)} ${badge.label}"
			BadgePill(badge.style, text, badgePaint.measureText(text) + padding * 1.6f)
		}
		val badgeRows = flowIntoRows(pills, contentWidth, pillGap)

		// Demoted trust line + signer details.
		val trustDotR = bodySize * 0.34f
		val trustInset = trustDotR * 2 + padding * 0.4f
		val trustLines = overlay.trustLabel?.let { wrap(it, trustPaint, contentWidth - trustInset) }.orEmpty()
		val detailLines = overlay.details.flatMap { wrap(it, bodyPaint, contentWidth) }

		// Measure the panel: hero + (badges) + (trust + details) + watermark.
		val titleLineH = lineHeight(titlePaint)
		val bodyLineH = lineHeight(bodyPaint)
		val trustLineH = lineHeight(trustPaint)
		val markLineH = lineHeight(markPaint)

		var contentH = 0f
		contentH += headlineLines.size * (titleLineH + lineGap)
		contentH += taglineLines.size * (bodyLineH + lineGap)
		if (badgeRows.isNotEmpty()) contentH += sectionGap + badgeRows.size * (pillHeight + lineGap)
		if (trustLines.isNotEmpty() || detailLines.isNotEmpty()) contentH += sectionGap
		contentH += trustLines.size * (trustLineH + lineGap)
		contentH += detailLines.size * (bodyLineH + lineGap)
		contentH += sectionGap + markLineH

		val panelHeight = padding * 2 + contentH
		val panel = RectF(margin, height - margin - panelHeight, width - margin, height - margin)
		val corner = unit * 0.8f
		canvas.drawRoundRect(panel, corner, corner, fillPaint(0xE6101418.toInt()))

		val x = panel.left + padding
		var y = panel.top + padding

		// Hero: accent dot + bold headline.
		headlineLines.forEachIndexed { i, line ->
			if (i == 0) canvas.drawCircle(x + dotR, y + titleLineH / 2f, dotR, fillPaint(accentColor))
			canvas.drawText(line, x + headlineInset, y + titlePaint.textSize * 0.82f, titlePaint)
			y += titleLineH + lineGap
		}
		for (line in taglineLines) {
			canvas.drawText(line, x + headlineInset, y + taglinePaint.textSize * 0.82f, taglinePaint)
			y += bodyLineH + lineGap
		}

		// Secondary pills.
		if (badgeRows.isNotEmpty()) {
			y += sectionGap
			for (row in badgeRows) {
				var bx = x
				for (pill in row) {
					val rect = RectF(bx, y, bx + pill.width, y + pillHeight)
					canvas.drawRoundRect(rect, pillHeight / 2f, pillHeight / 2f, fillPaint(badgeColor(pill.style)))
					canvas.drawText(pill.text, rect.left + padding * 0.8f, rect.centerY() + badgePaint.textSize * 0.35f, badgePaint)
					bx += pill.width + pillGap
				}
				y += pillHeight + lineGap
			}
		}

		// Demoted trust verdict + signer details.
		if (trustLines.isNotEmpty() || detailLines.isNotEmpty()) y += sectionGap
		trustLines.forEachIndexed { i, line ->
			if (i == 0) canvas.drawCircle(x + trustDotR, y + trustLineH / 2f, trustDotR, fillPaint(toneColor(overlay.tone)))
			canvas.drawText(line, x + trustInset, y + trustPaint.textSize * 0.82f, trustPaint)
			y += trustLineH + lineGap
		}
		for (line in detailLines) {
			canvas.drawText(line, x, y + bodyPaint.textSize * 0.82f, bodyPaint)
			y += bodyLineH + lineGap
		}

		// Brand mark, bottom-right of the panel.
		val markWidth = markPaint.measureText(overlay.watermark)
		canvas.drawText(
			overlay.watermark,
			panel.right - padding - markWidth,
			panel.bottom - padding,
			markPaint,
		)
		return out
	}

	private fun writeToCache(bitmap: Bitmap): String {
		val dir = File(context.cacheDir, REPORT_DIR).apply { mkdirs() }
		val file = File(dir, REPORT_FILE)
		file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
		val authority = "${context.packageName}$FILEPROVIDER_SUFFIX"
		return FileProvider.getUriForFile(context, authority, file).toString()
	}

	private fun textPaint(size: Float, typeface: Typeface) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		color = Color.WHITE
		textSize = size
		this.typeface = typeface
	}

	private fun fillPaint(argb: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		style = Paint.Style.FILL
		color = argb
	}

	private fun lineHeight(paint: Paint): Float = paint.fontMetrics.let { it.descent - it.ascent }

	/** Greedy word-wrap so long signer names don't overflow the panel. */
	private fun wrap(text: String, paint: Paint, maxWidth: Float): List<String> {
		if (paint.measureText(text) <= maxWidth) return listOf(text)
		val out = mutableListOf<String>()
		val current = StringBuilder()
		for (word in text.split(' ')) {
			val candidate = if (current.isEmpty()) word else "$current $word"
			if (paint.measureText(candidate) <= maxWidth || current.isEmpty()) {
				current.clear().append(candidate)
			} else {
				out.add(current.toString())
				current.clear().append(word)
			}
		}
		if (current.isNotEmpty()) out.add(current.toString())
		return out
	}

	private fun toneColor(tone: ReportTone): Int = when (tone) {
		ReportTone.TRUSTED -> 0xFF2E7D32.toInt()
		ReportTone.UNTRUSTED -> 0xFFF9A825.toInt()
		ReportTone.INVALID -> 0xFFE53935.toInt()
		ReportTone.NEUTRAL -> 0xFF9E9E9E.toInt()
	}

	/** A measured indicator pill ready to draw. */
	private data class BadgePill(val style: ReportBadgeStyle, val text: String, val width: Float)

	/** Greedy left-to-right flow of pills into rows no wider than [maxWidth]. */
	private fun flowIntoRows(pills: List<BadgePill>, maxWidth: Float, gap: Float): List<List<BadgePill>> {
		val rows = mutableListOf<MutableList<BadgePill>>()
		var rowWidth = 0f
		for (pill in pills) {
			if (rows.isEmpty() || rowWidth + pill.width > maxWidth) {
				rows.add(mutableListOf())
				rowWidth = 0f
			}
			rows.last().add(pill)
			rowWidth += pill.width + gap
		}
		return rows
	}

	private fun badgeGlyph(style: ReportBadgeStyle): String = when (style) {
		ReportBadgeStyle.AI -> "✨"
		ReportBadgeStyle.CAPTURE -> "📷"
		ReportBadgeStyle.SOFTWARE -> "🖌"
		ReportBadgeStyle.ENHANCED -> "🪄"
		ReportBadgeStyle.EDITED -> "✏️"
		ReportBadgeStyle.ALERT -> "⛔"
	}

	private fun badgeColor(style: ReportBadgeStyle): Int = when (style) {
		ReportBadgeStyle.AI -> AI_COLOR
		ReportBadgeStyle.CAPTURE -> CAPTURE_COLOR
		ReportBadgeStyle.SOFTWARE -> SOFTWARE_COLOR
		ReportBadgeStyle.ENHANCED -> ENHANCED_COLOR
		ReportBadgeStyle.EDITED -> EDITED_COLOR
		ReportBadgeStyle.ALERT -> ALERT_COLOR
	}

	private companion object {
		const val MAX_EDGE_PX = 2048
		const val REPORT_DIR = "reports"
		const val REPORT_FILE = "c2pa-report.png"
		const val FILEPROVIDER_SUFFIX = ".fileprovider"
		val AI_COLOR = 0xFF7C4DFF.toInt()
		val CAPTURE_COLOR = 0xFF1565C0.toInt()
		val SOFTWARE_COLOR = 0xFF00897B.toInt()
		val ENHANCED_COLOR = 0xFFEF6C00.toInt()
		val EDITED_COLOR = 0xFF5C6BC0.toInt()
		val ALERT_COLOR = 0xFFD32F2F.toInt()
	}
}
