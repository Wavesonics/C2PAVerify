package com.darkrockstudios.apps.c2paviewer.di

import com.darkrockstudios.apps.c2paviewer.datasource.c2pa.AndroidC2paReaderDataSource
import com.darkrockstudios.apps.c2paviewer.datasource.c2pa.C2paReaderDataSource
import com.darkrockstudios.apps.c2paviewer.datasource.image.ImageBytesDataSource
import com.darkrockstudios.apps.c2paviewer.datasource.trustlist.TrustAnchorParser
import com.darkrockstudios.apps.c2paviewer.datasource.trustlist.TrustListAssetDataSource
import com.darkrockstudios.apps.c2paviewer.repository.C2paManifestParser
import com.darkrockstudios.apps.c2paviewer.repository.C2paManifestRepository
import com.darkrockstudios.apps.c2paviewer.repository.ImageRepository
import com.darkrockstudios.apps.c2paviewer.repository.TrustListRepository
import com.darkrockstudios.apps.c2paviewer.repository.UserTrustRepository
import com.darkrockstudios.apps.c2paviewer.service.TrustEvaluationService
import com.darkrockstudios.apps.c2paviewer.ui.inspection.InspectionViewModel
import com.darkrockstudios.apps.c2paviewer.ui.trust.TrustManagementViewModel
import com.darkrockstudios.apps.c2paviewer.usecase.inspect.InspectPhotoUseCase
import com.darkrockstudios.apps.c2paviewer.usecase.trust.ClearAuthorityRuleUseCase
import com.darkrockstudios.apps.c2paviewer.usecase.trust.GetTrustAnchorsUseCase
import com.darkrockstudios.apps.c2paviewer.usecase.trust.ObserveUserTrustRulesUseCase
import com.darkrockstudios.apps.c2paviewer.usecase.trust.SetAuthorityRuleUseCase
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * One Koin module per architectural layer. The DI scope mirrors statefulness:
 *  - stateless items (data sources, use cases) are registered as `factory`
 *  - stateful items (repositories, services) are registered as `single`
 *
 * Components are added here as each build-order step lands.
 */

// LAYER 1 — data sources (stateless → factory)
val dataSourceModule = module {
	factoryOf(::AndroidC2paReaderDataSource) bind C2paReaderDataSource::class
	factory { C2paManifestParser(get()) }
	factory { ImageBytesDataSource(androidContext()) }
	factory { TrustListAssetDataSource(androidContext()) }
	factoryOf(::TrustAnchorParser)
}

// LAYER 2 — repositories (stateful → single)
val repositoryModule = module {
	singleOf(::C2paManifestRepository)
	singleOf(::ImageRepository)
	singleOf(::TrustListRepository)
	singleOf(::UserTrustRepository)
}

// LAYER 3 — services (stateful → single)
val serviceModule = module {
	singleOf(::TrustEvaluationService)
}

// LAYER 4 — use cases (stateless → factory)
val useCaseModule = module {
	factoryOf(::InspectPhotoUseCase)
	factoryOf(::GetTrustAnchorsUseCase)
	factoryOf(::ObserveUserTrustRulesUseCase)
	factoryOf(::SetAuthorityRuleUseCase)
	factoryOf(::ClearAuthorityRuleUseCase)
}

// Presentation — ViewModels
val viewModelModule = module {
	viewModelOf(::InspectionViewModel)
	viewModelOf(::TrustManagementViewModel)
}
