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

		val titlePaint = textPaint(titleSize, Typeface.create(Typeface.DEFAULT, Typeface.BOLD))
		val bodyPaint = textPaint(bodySize, Typeface.DEFAULT).apply { alpha = 230 }
		val markPaint = textPaint(markSize, Typeface.DEFAULT).apply { alpha = 150 }

		val contentWidth = width - 2 * margin - 2 * padding
		val lines = overlay.details.flatMap { wrap(it, bodyPaint, contentWidth) }

		// Measure panel height: status row + detail lines + optional AI badge + watermark.
		val titleHeight = lineHeight(titlePaint)
		val bodyLineHeight = lineHeight(bodyPaint) + lineGap
		val badgeHeight = if (overlay.isAiGenerated) bodySize * 2.2f + lineGap else 0f
		val markHeight = lineHeight(markPaint) + lineGap
		val panelHeight = padding * 2 + titleHeight + lines.size * bodyLineHeight + badgeHeight + markHeight

		val panel = RectF(margin, height - margin - panelHeight, width - margin, height - margin)
		val corner = unit * 0.8f
		canvas.drawRoundRect(panel, corner, corner, fillPaint(0xE6101418.toInt()))

		var y = panel.top + padding
		val x = panel.left + padding

		// Status row: a tone-coloured dot + bold label.
		val dotR = titleSize * 0.32f
		val baseline = y + titlePaint.textSize * 0.82f
		canvas.drawCircle(x + dotR, y + titleHeight / 2f, dotR, fillPaint(toneColor(overlay.tone)))
		canvas.drawText(overlay.statusLabel, x + dotR * 2 + padding * 0.5f, baseline, titlePaint)
		y += titleHeight + lineGap

		for (line in lines) {
			canvas.drawText(line, x, y + bodyPaint.textSize * 0.82f, bodyPaint)
			y += bodyLineHeight
		}

		if (overlay.isAiGenerated) {
			val badgePaint = textPaint(bodySize, Typeface.create(Typeface.DEFAULT, Typeface.BOLD))
			val label = "✨ ${overlay.aiLabel}"
			val textW = badgePaint.measureText(label)
			val badge = RectF(x, y, x + textW + padding * 1.6f, y + bodySize * 2.1f)
			canvas.drawRoundRect(badge, badge.height() / 2f, badge.height() / 2f, fillPaint(AI_COLOR))
			canvas.drawText(label, badge.left + padding * 0.8f, badge.centerY() + badgePaint.textSize * 0.35f, badgePaint)
			y += badgeHeight
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

	private companion object {
		const val MAX_EDGE_PX = 2048
		const val REPORT_DIR = "reports"
		const val REPORT_FILE = "c2pa-report.png"
		const val FILEPROVIDER_SUFFIX = ".fileprovider"
		val AI_COLOR = 0xFF7C4DFF.toInt()
	}
}
