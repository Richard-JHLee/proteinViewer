package com.avas.proteinviewer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.avas.proteinviewer.data.model.PDBStructure
import com.avas.proteinviewer.data.model.ProteinMetadata
import com.avas.proteinviewer.data.repository.ProteinRepository
import com.avas.proteinviewer.data.local.SampleData
import com.avas.proteinviewer.ui.state.AppState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProteinViewModel @Inject constructor(
    private val repository: ProteinRepository
) : ViewModel() {
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _loadingProgress = MutableStateFlow("")
    val loadingProgress: StateFlow<String> = _loadingProgress.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _currentProteinId = MutableStateFlow("")
    val currentProteinId: StateFlow<String> = _currentProteinId.asStateFlow()
    
    private val _currentProteinName = MutableStateFlow("")
    val currentProteinName: StateFlow<String> = _currentProteinName.asStateFlow()
    
    private val _structure = MutableStateFlow<PDBStructure?>(null)
    val structure: StateFlow<PDBStructure?> = _structure.asStateFlow()

    private val _metadata = MutableStateFlow<ProteinMetadata?>(null)
    val metadata: StateFlow<ProteinMetadata?> = _metadata.asStateFlow()
    
    private val _appState = MutableStateFlow(AppState())
    val appState: StateFlow<AppState> = _appState.asStateFlow()
    
    fun saveState(state: AppState) {
        _appState.value = state
    }
    
    fun restoreState(): AppState {
        return _appState.value
    }
    
    fun onConfigurationChanged(newState: AppState) {
        _appState.value = newState
        // 필요한 경우 데이터 다시 로드
        if (newState.currentProteinId != null && _structure.value == null) {
            loadSelectedProtein(newState.currentProteinId)
        }
    }
    
    fun updateSelectedTab(tab: Int) {
        _appState.value = _appState.value.copy(selectedTab = tab)
    }
    
    fun updateRenderMode(mode: com.avas.proteinviewer.ui.state.RenderMode) {
        _appState.value = _appState.value.copy(renderMode = mode)
    }
    
    fun updateColorMode(mode: com.avas.proteinviewer.ui.state.ColorMode) {
        _appState.value = _appState.value.copy(colorMode = mode)
    }
    
    fun toggleBonds() {
        _appState.value = _appState.value.copy(showBonds = !_appState.value.showBonds)
    }
    
    fun toggleDarkTheme() {
        _appState.value = _appState.value.copy(isDarkTheme = !_appState.value.isDarkTheme)
    }
    
    fun loadDefaultProtein() {
        viewModelScope.launch {
            _isLoading.value = true
            _loadingProgress.value = "Loading default protein..."
            _error.value = null
            
            try {
                // Try to load from network first
                val structureResult = repository.loadProteinStructure("1CRN")
                
                if (structureResult.isSuccess) {
                    val loadedStructure = structureResult.getOrThrow()
                    val nameResult = repository.getProteinName("1CRN")
                    val proteinName = nameResult.getOrElse { "Crambin" }
                    val metadataResult = repository.getProteinMetadata("1CRN")
                    
                    _structure.value = loadedStructure
                    _currentProteinId.value = "1CRN"
                    _currentProteinName.value = proteinName
                    _metadata.value = metadataResult.getOrNull()
                    _isLoading.value = false
                    _loadingProgress.value = ""
                } else {
                    // Fall back to sample data if network fails
                    _loadingProgress.value = "Network unavailable, loading sample data..."
                    val sampleStructure = SampleData.getSampleStructure()
                    
                    if (sampleStructure != null) {
                        _structure.value = sampleStructure
                        _currentProteinId.value = "1CRN"
                        _currentProteinName.value = "Crambin (Sample Data)"
                        _metadata.value = SampleData.getSampleMetadata()
                        _isLoading.value = false
                        _loadingProgress.value = ""
                        _error.value = "Network unavailable. Showing sample data."
                    } else {
                        _error.value = "Failed to load protein and no sample data available."
                        _isLoading.value = false
                        _loadingProgress.value = ""
                    }
                }
                
            } catch (e: Exception) {
                // Final fallback to sample data
                _loadingProgress.value = "Loading sample data..."
                val sampleStructure = SampleData.getSampleStructure()
                
                if (sampleStructure != null) {
                    _structure.value = sampleStructure
                    _currentProteinId.value = "1CRN"
                    _currentProteinName.value = "Crambin (Sample Data)"
                    _metadata.value = SampleData.getSampleMetadata()
                    _isLoading.value = false
                    _loadingProgress.value = ""
                    _error.value = "Network error. Showing sample data."
                } else {
                    _error.value = "Failed to load protein: ${e.message}"
                    _isLoading.value = false
                    _loadingProgress.value = ""
                }
            }
        }
    }
    
    fun loadSelectedProtein(pdbId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _loadingProgress.value = "Loading protein $pdbId..."
            _error.value = null
            
            try {
                // Load protein structure
                val structureResult = repository.loadProteinStructure(pdbId)
                
                if (structureResult.isFailure) {
                    val error = structureResult.exceptionOrNull()
                    _error.value = when (error) {
                        is com.avas.proteinviewer.data.error.PDBError -> error.userFriendlyMessage
                        else -> "Failed to load protein: ${error?.message}"
                    }
                    _isLoading.value = false
                    _loadingProgress.value = ""
                    return@launch
                }
                
                val loadedStructure = structureResult.getOrThrow()
                
                // Load protein name
                val nameResult = repository.getProteinName(pdbId)
                val proteinName = nameResult.getOrElse { "Protein $pdbId" }
                val metadataResult = repository.getProteinMetadata(pdbId)
                
                _structure.value = loadedStructure
                _currentProteinId.value = pdbId.uppercase()
                _currentProteinName.value = proteinName
                _metadata.value = metadataResult.getOrNull()
                _isLoading.value = false
                _loadingProgress.value = ""
                
            } catch (e: Exception) {
                _error.value = "Failed to load protein $pdbId: ${e.message}"
                _isLoading.value = false
                _loadingProgress.value = ""
            }
        }
    }
}
