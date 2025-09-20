package com.avas.proteinviewer.navigation

import android.net.Uri
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeepLinkHandler @Inject constructor() {
    fun handleDeepLink(uri: Uri): DeepLinkResult {
        return when {
            uri.scheme == "https" && uri.host == "proteinviewer.app" -> {
                val proteinId = uri.pathSegments.getOrNull(1)
                if (proteinId != null) {
                    DeepLinkResult.LoadProtein(proteinId)
                } else {
                    DeepLinkResult.Error("Invalid protein ID in URL")
                }
            }
            uri.scheme == "proteinviewer" && uri.host == "protein" -> {
                val proteinId = uri.pathSegments.getOrNull(0)
                if (proteinId != null) {
                    DeepLinkResult.LoadProtein(proteinId)
                } else {
                    DeepLinkResult.Error("Invalid protein ID in custom scheme")
                }
            }
            else -> DeepLinkResult.Error("Unsupported deep link: ${uri.toString()}")
        }
    }
    
    fun createProteinDeepLink(proteinId: String): Uri {
        return Uri.Builder()
            .scheme("proteinviewer")
            .authority("protein")
            .appendPath(proteinId)
            .build()
    }
    
    fun createWebDeepLink(proteinId: String): Uri {
        return Uri.Builder()
            .scheme("https")
            .authority("proteinviewer.app")
            .appendPath("protein")
            .appendPath(proteinId)
            .build()
    }
}

sealed class DeepLinkResult {
    data class LoadProtein(val proteinId: String) : DeepLinkResult()
    data class Error(val message: String) : DeepLinkResult()
}
