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
    val focusedElement: String? = null, // 아이폰과 동일: "Chain A" 등
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
        android.util.Log.d("ProteinViewModel", "ViewModel initialized")
        android.util.Log.d("ProteinViewModel", "Initial highlightedChains: ${_uiState.value.highlightedChains}")
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

    // Highlight functions for different element types
    fun toggleChainHighlight(chain: String) {
        _uiState.update {
            val newHighlights = it.highlightedChains.toMutableSet()
            val key = "chain:$chain"
            
            // 디버그: 현재 상태 확인
            android.util.Log.d("ProteinViewModel", "toggleChainHighlight - chain: $chain")
            android.util.Log.d("ProteinViewModel", "Before: $newHighlights")
            android.util.Log.d("ProteinViewModel", "Contains '$key': ${key in newHighlights}")
            
            // 올바른 토글 로직: contains가 true면 제거, false면 추가
            if (key in newHighlights) {
                newHighlights.remove(key)
                android.util.Log.d("ProteinViewModel", "Action: REMOVE")
            } else {
                newHighlights.add(key)
                android.util.Log.d("ProteinViewModel", "Action: ADD")
            }
            
            android.util.Log.d("ProteinViewModel", "After: $newHighlights")
            it.copy(highlightedChains = newHighlights)
        }
    }
    
    fun clearHighlights() {
        _uiState.update { it.copy(
            highlightedChains = emptySet(),
            focusedElement = null
        ) }
    }
    
    fun toggleLigandHighlight(ligandName: String) {
        _uiState.update {
            val newHighlights = it.highlightedChains.toMutableSet()
            if ("ligand:$ligandName" in newHighlights) {
                newHighlights.remove("ligand:$ligandName")
            } else {
                newHighlights.add("ligand:$ligandName")
            }
            it.copy(highlightedChains = newHighlights)
        }
    }
    
    fun togglePocketHighlight(pocketName: String) {
        _uiState.update {
            val newHighlights = it.highlightedChains.toMutableSet()
            if ("pocket:$pocketName" in newHighlights) {
                newHighlights.remove("pocket:$pocketName")
            } else {
                newHighlights.add("pocket:$pocketName")
            }
            it.copy(highlightedChains = newHighlights)
        }
    }
    
    // Focus functions for different element types
    fun toggleChainFocus(chain: String) {
        _uiState.update {
            val newFocus = if (it.focusedElement == "chain:$chain") null else "chain:$chain"
            it.copy(focusedElement = newFocus)
        }
    }
    
    fun toggleLigandFocus(ligandName: String) {
        _uiState.update {
            val newFocus = if (it.focusedElement == "ligand:$ligandName") null else "ligand:$ligandName"
            it.copy(focusedElement = newFocus)
        }
    }
    
    fun togglePocketFocus(pocketName: String) {
        _uiState.update {
            val newFocus = if (it.focusedElement == "pocket:$pocketName") null else "pocket:$pocketName"
            it.copy(focusedElement = newFocus)
        }
    }
    
    // Highlight All 기능 (iPhone과 동일)
    fun toggleHighlightAll() {
        _uiState.update { state ->
            val currentStructure = state.structure
            if (currentStructure == null) {
                android.util.Log.d("ProteinViewModel", "toggleHighlightAll - no structure loaded")
                return@update state
            }
            
            // 현재 모든 체인이 하이라이트되어 있는지 확인
            val allChains = currentStructure.chains.map { "chain:$it" }.toSet()
            val allHighlighted = allChains.isNotEmpty() && allChains.all { chain -> chain in state.highlightedChains }
            
            android.util.Log.d("ProteinViewModel", "toggleHighlightAll - allHighlighted: $allHighlighted, chains: ${currentStructure.chains}")
            
            if (allHighlighted) {
                // 모두 하이라이트되어 있으면 모두 해제
                state.copy(highlightedChains = emptySet())
            } else {
                // 일부 또는 전부 해제되어 있으면 모두 하이라이트
                state.copy(highlightedChains = allChains)
            }
        }
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
