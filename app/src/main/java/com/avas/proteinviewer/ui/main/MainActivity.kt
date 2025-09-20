package com.avas.proteinviewer.ui.main

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.avas.proteinviewer.navigation.ProteinViewerNavigation
import com.avas.proteinviewer.navigation.DeepLinkHandler
import com.avas.proteinviewer.navigation.DeepLinkResult
import com.avas.proteinviewer.ui.state.AppState
import com.avas.proteinviewer.ui.theme.ProteinViewerTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Main Activity - equivalent to iPhone ContentView
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var deepLinkHandler: DeepLinkHandler
    
    private var savedState: AppState? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 저장된 상태 복원
        savedInstanceState?.let {
            savedState = it.getParcelable("app_state")
        }
        
        setContent {
            ProteinViewerTheme {
                ProteinViewerNavigation()
            }
        }
        
        handleDeepLink(intent)
    }
    
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // 현재 상태 저장 (실제 구현에서는 ViewModel에서 가져와야 함)
        // outState.putParcelable("app_state", currentAppState)
    }
    
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // 화면 회전 등 Configuration Changes 처리
        // ViewModel에 새로운 설정 전달
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleDeepLink(it) }
    }
    
    private fun handleDeepLink(intent: Intent) {
        val uri = intent.data
        if (uri != null) {
            val result = deepLinkHandler.handleDeepLink(uri)
            when (result) {
                is DeepLinkResult.LoadProtein -> {
                    // TODO: ViewModel에 protein ID 전달하여 로드
                    // viewModel.loadSelectedProtein(result.proteinId)
                    println("Deep Link: Loading protein ${result.proteinId}")
                }
                is DeepLinkResult.Error -> {
                    // TODO: 에러 처리
                    println("Deep Link Error: ${result.message}")
                }
            }
        }
    }
}