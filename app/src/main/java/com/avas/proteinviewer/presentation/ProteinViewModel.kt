package com.avas.proteinviewer.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.avas.proteinviewer.domain.model.*
import com.avas.proteinviewer.domain.repository.ProteinRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProteinUiState(
    val isLoading: Boolean = false,
    val loadingProgress: String = "",
    val error: String? = null,
    val searchResults: List<ProteinInfo> = emptyList(),
    val selectedProtein: ProteinDetail? = null,
    val structure: PDBStructure? = null,
    val currentProteinId: String = "",
    val currentProteinName: String = "",
    val currentProteinInfo: ProteinInfo? = null, // API로부터 받은 상세 정보
    val renderStyle: RenderStyle = RenderStyle.RIBBON,
    val colorMode: ColorMode = ColorMode.ELEMENT,
    val highlightedChains: Set<String> = emptySet(),
    val showSideMenu: Boolean = false,
    val showProteinLibrary: Boolean = false,
    val selectedInfoTab: InfoTab = InfoTab.OVERVIEW,
    val viewMode: ViewMode = ViewMode.INFO
)

enum class InfoTab {
    OVERVIEW, CHAINS, RESIDUES, LIGANDS, POCKETS, SEQUENCE, ANNOTATIONS
}

@HiltViewModel
class ProteinViewModel @Inject constructor(
    private val repository: ProteinRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProteinUiState())
    val uiState: StateFlow<ProteinUiState> = _uiState.asStateFlow()

    init {
        loadDefaultProtein()
    }

    fun loadDefaultProtein() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadingProgress = "Loading default protein...") }
            
            try {
                val defaultId = "1CRN"
                val structure = repository.loadPDBStructure(defaultId) { progress ->
                    _uiState.update { it.copy(loadingProgress = progress) }
                }
                
                val detail = repository.getProteinDetail(defaultId).first()
                
                // ProteinInfo 생성
                val proteinInfo = ProteinInfo(
                    id = detail.id,
                    name = detail.name,
                    description = detail.description,
                    organism = detail.organism,
                    resolution = detail.resolution,
                    experimentalMethod = detail.experimentalMethod,
                    depositionDate = detail.depositionDate,
                    molecularWeight = detail.molecularWeight
                )
                
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loadingProgress = "",
                        structure = structure,
                        selectedProtein = detail,
                        currentProteinId = defaultId,
                        currentProteinName = detail.name,
                        currentProteinInfo = proteinInfo
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loadingProgress = "",
                        error = e.message ?: "Failed to load protein"
                    )
                }
            }
        }
    }

    fun searchProteins(query: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            repository.searchProteins(query)
                .catch { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "Search failed"
                        )
                    }
                }
                .collect { results ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            searchResults = results,
                            showProteinLibrary = true
                        )
                    }
                }
        }
    }

    fun selectProtein(proteinId: String) {
        viewModelScope.launch {
            _uiState.update { 
                it.copy(
                    isLoading = true, 
                    loadingProgress = "Loading protein $proteinId...",
                    showProteinLibrary = false
                ) 
            }
            
            try {
                val structure = repository.loadPDBStructure(proteinId) { progress ->
                    _uiState.update { it.copy(loadingProgress = progress) }
                }
                
                val detail = repository.getProteinDetail(proteinId).first()
                
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loadingProgress = "",
                        structure = structure,
                        selectedProtein = detail,
                        currentProteinId = proteinId,
                        currentProteinName = detail.name
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loadingProgress = "",
                        error = e.message ?: "Failed to load protein"
                    )
                }
            }
        }
    }

    fun setRenderStyle(style: RenderStyle) {
        _uiState.update { it.copy(renderStyle = style) }
    }

    fun setColorMode(mode: ColorMode) {
        _uiState.update { it.copy(colorMode = mode) }
    }

    fun toggleChainHighlight(chain: String) {
        _uiState.update {
            val newHighlights = it.highlightedChains.toMutableSet()
            if (chain in newHighlights) {
                newHighlights.remove(chain)
            } else {
                newHighlights.add(chain)
            }
            it.copy(highlightedChains = newHighlights)
        }
    }
    
    fun clearHighlights() {
        _uiState.update { it.copy(highlightedChains = emptySet()) }
    }

    fun toggleSideMenu() {
        _uiState.update { it.copy(showSideMenu = !it.showSideMenu) }
    }

    fun toggleProteinLibrary() {
        _uiState.update { it.copy(showProteinLibrary = !it.showProteinLibrary) }
    }

    fun setInfoTab(tab: InfoTab) {
        _uiState.update { it.copy(selectedInfoTab = tab) }
    }
    
    fun setViewMode(mode: ViewMode) {
        _uiState.update { it.copy(viewMode = mode) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
