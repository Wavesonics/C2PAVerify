package com.darkrockstudios.apps.c2paviewer

import android.app.Application
import com.darkrockstudios.apps.c2paviewer.di.appModules
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class C2paViewerApp : Application() {
	override fun onCreate() {
		super.onCreate()

		Napier.base(DebugAntilog())

		startKoin {
			androidLogger()
			androidContext(this@C2paViewerApp)
			modules(appModules)
		}
	}
}
