package com.darkrockstudios.apps.c2paverify.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertFalse
import org.junit.Test

/**
 * Enforces the project's layering rules (see AGENTS.md / PLAN.md):
 *
 *   ViewModel -> UseCase -> Service -> Repository -> DataSource
 *
 *  - The "KMP-clean" layers (model/repository/service/usecase) must not import `android.*`.
 *  - Stateful layers (repository, service) must not reference siblings.
 *  - Lower layers must not depend on upper layers.
 *  - ViewModels must not access data sources directly.
 */
class ArchitectureTest {

	private companion object {
		const val ROOT = "com.darkrockstudios.apps.c2paverify"
		const val MODEL = "$ROOT.model"
		const val DATASOURCE = "$ROOT.datasource"
		const val REPOSITORY = "$ROOT.repository"
		const val SERVICE = "$ROOT.service"
		const val USECASE = "$ROOT.usecase"
		const val UI = "$ROOT.ui"
	}

	private fun filesInPackage(prefix: String) =
		Konsist.scopeFromProject().files.filter { it.packagee?.name?.startsWith(prefix) == true }

	@Test
	fun `model, repository, service and usecase layers do not depend on Android`() {
		filesInPackage(MODEL).assertFalse(additionalMessage = "model must stay KMP-clean") { f ->
			f.imports.any { it.name.startsWith("android.") }
		}
		listOf(REPOSITORY, SERVICE, USECASE).forEach { layer ->
			filesInPackage(layer).assertFalse(additionalMessage = "$layer must stay KMP-clean") { f ->
				f.imports.any { it.name.startsWith("android.") }
			}
		}
	}

	@Test
	fun `repositories do not reference sibling repositories`() {
		filesInPackage(REPOSITORY).assertFalse { f ->
			f.imports.any { it.name.startsWith("$REPOSITORY.") }
		}
	}

	@Test
	fun `services do not reference sibling services`() {
		filesInPackage(SERVICE).assertFalse { f ->
			f.imports.any { it.name.startsWith("$SERVICE.") }
		}
	}

	@Test
	fun `lower layers do not depend on upper layers`() {
		// Repository may not see service / usecase / ui
		filesInPackage(REPOSITORY).assertFalse { f ->
			f.imports.any {
				it.name.startsWith("$SERVICE.") || it.name.startsWith("$USECASE.") || it.name.startsWith("$UI.")
			}
		}
		// Service may not see usecase / ui
		filesInPackage(SERVICE).assertFalse { f ->
			f.imports.any { it.name.startsWith("$USECASE.") || it.name.startsWith("$UI.") }
		}
		// UseCase may not see ui
		filesInPackage(USECASE).assertFalse { f ->
			f.imports.any { it.name.startsWith("$UI.") }
		}
	}

	@Test
	fun `view models do not access data sources directly`() {
		Konsist.scopeFromProject()
			.classes()
			.filter { it.name.endsWith("ViewModel") }
			.assertFalse { vm ->
				vm.containingFile.imports.any { it.name.startsWith("$DATASOURCE.") }
			}
	}
}
