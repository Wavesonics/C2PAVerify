package com.darkrockstudios.apps.c2paviewer.ui.navigation

import kotlinx.serialization.Serializable

/** Type-safe navigation destinations. The deep-dive becomes a detail pane (step 6), not a route. */
@Serializable
object Landing

@Serializable
object Viewer

@Serializable
object DeepDive
