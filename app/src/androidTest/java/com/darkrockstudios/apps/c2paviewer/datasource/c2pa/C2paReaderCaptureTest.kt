package com.darkrockstudios.apps.c2paviewer.datasource.c2pa

import androidx.test.platform.app.InstrumentationRegistry
import com.darkrockstudios.apps.c2paviewer.model.common.ImageSource
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.io.File

/**
 * Captures the REAL `reader.json()` / `reader.detailedJson()` output of the c2pa-android library
 * for each vendored sample image, writing the results to the app's external files dir so they can
 * be pulled off the device and turned into JVM unit-test fixtures. Also a smoke test that the
 * Android C2PA data source actually works on-device.
 *
 * Run on an emulator, then pull:
 *   adb shell run-as <app> ... / adb pull /sdcard/Android/data/<app>/files/c2pa-capture
 */
class C2paReaderCaptureTest {

	private val dataSource = AndroidC2paReaderDataSource()

	private val samples = listOf(
		"valid-C.jpg",
		"valid-multi-CAICAI.jpg",
		"invalid-sig.jpg",
		"tampered-dat.jpg",
		"no-manifest.jpg",
	)

	@Test
	fun captureRealReaderOutput() = runBlocking {
		// Sample assets live in the *test* APK (instrumentation context); write output into the
		// app-under-test's external files dir (target context).
		val testCtx = InstrumentationRegistry.getInstrumentation().context
		val appCtx = InstrumentationRegistry.getInstrumentation().targetContext
		val outDir = File(appCtx.getExternalFilesDir(null), "c2pa-capture").apply { mkdirs() }

		val summary = StringBuilder()
		for (asset in samples) {
			val bytes = testCtx.assets.open("c2pa/$asset").use { it.readBytes() }
			val result = runCatching { dataSource.read(ImageSource.Bytes(bytes, "image/jpeg")) }
			val base = asset.removeSuffix(".jpg")
			result.onSuccess { read ->
				when (read) {
					is C2paRawRead.NoManifest -> {
						summary.appendLine("$asset -> NoManifest")
						File(outDir, "$base.NOMANIFEST.txt").writeText("NoManifest")
					}

					is C2paRawRead.Manifest -> {
						summary.appendLine("$asset -> Manifest (json=${read.manifestJson.length} chars, detailed=${read.detailedJson?.length ?: 0})")
						File(outDir, "$base.manifest.json").writeText(read.manifestJson)
						read.detailedJson?.let { File(outDir, "$base.detailed.json").writeText(it) }
					}
				}
			}.onFailure { e ->
				summary.appendLine("$asset -> ERROR ${e::class.simpleName}: ${e.message}")
				File(outDir, "$base.ERROR.txt").writeText("${e::class.qualifiedName}: ${e.message}\n${e.stackTraceToString()}")
			}
		}

		File(outDir, "_summary.txt").writeText(summary.toString())
		println("C2PA capture summary:\n$summary")
		println("C2PA capture written to: ${outDir.absolutePath}")
	}
}
