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
    val viewMode: ViewMode = ViewMode.INFO,
    // 카테고리 관련 상태 추가
    val selectedCategory: ProteinCategory? = null,
    val showCategoryGrid: Boolean = false,
    val categoryProteinCounts: Map<String, Int> = emptyMap(),
    val isLoadingCategoryCounts: Boolean = false
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
    
    // 카테고리 관련 함수들
    fun selectCategory(category: ProteinCategory?) {
        _uiState.update { 
            it.copy(
                selectedCategory = category,
                showCategoryGrid = false
            ) 
        }
        
        // 아이폰과 동일: 카테고리 선택 시 해당 카테고리의 단백질 로드
        if (category != null) {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true) }
                
                try {
                    // TODO: 카테고리별 단백질 검색 API 호출
                    // 현재는 일반 검색으로 대체
                    repository.searchProteins(category.displayName)
                        .catch { e ->
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    error = e.message ?: "Failed to load proteins"
                                )
                            }
                        }
                        .collect { proteins ->
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    searchResults = proteins.take(30) // 아이폰과 동일: 30개씩
                                )
                            }
                        }
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "Failed to load proteins"
                        )
                    }
                }
            }
        }
    }
    
    fun showAllCategories() {
        _uiState.update { 
            it.copy(
                selectedCategory = null,
                showCategoryGrid = true
            ) 
        }
    }
    
    fun hideCategoryGrid() {
        _uiState.update { it.copy(showCategoryGrid = false) }
    }
    
    fun loadCategoryCounts() {
        // 하드코딩된 카테고리별 protein count (iOS와 동일)
        val categoryCounts = mapOf(
            "Enzymes" to 45000,
            "Structural" to 32000,
            "Transport" to 25000,
            "Storage" to 5000,
            "Hormonal" to 8000,
            "Defense" to 18000,
            "Regulatory" to 12000,
            "Motor" to 6000,
            "Receptor" to 15000,
            "Signaling" to 12000,
            "Metabolic" to 38000,
            "Binding" to 22000
        )
        
        _uiState.update { 
            it.copy(
                categoryProteinCounts = categoryCounts,
                isLoadingCategoryCounts = false
            ) 
        }
    }
}
