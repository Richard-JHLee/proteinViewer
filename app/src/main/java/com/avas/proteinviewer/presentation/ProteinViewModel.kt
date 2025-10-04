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
    val previousViewMode: ViewMode? = null, // 이전 모드 추적
    // 아이폰 ProteinDatabase와 동일한 카테고리 관련 상태
    val selectedCategory: ProteinCategory? = null,
    val categoryProteinCounts: Map<ProteinCategory, Int> = emptyMap(),
    val isLoadingCategoryCounts: Boolean = false,
    val showingLoadingPopup: Boolean = false,
    val currentPage: Int = 1,
    val isLoadingMore: Boolean = false,
    val hasMoreData: Boolean = false,
    val favorites: Set<String> = emptySet(),
    val searchText: String = "",
    val allProteinsByCategory: Map<ProteinCategory, List<ProteinInfo>> = emptyMap(),
    val loadedCategories: Set<ProteinCategory> = emptySet(),
    val isLoadingInfoTab: Boolean = false
)

enum class InfoTab {
    OVERVIEW, CHAINS, RESIDUES, LIGANDS, POCKETS, SEQUENCE, ANNOTATIONS
}

@HiltViewModel
class ProteinViewModel @Inject constructor(
    private val repository: ProteinRepository,
    private val proteinDatabase: com.avas.proteinviewer.data.repository.ProteinDatabase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProteinUiState())
    val uiState: StateFlow<ProteinUiState> = _uiState.asStateFlow()

    init {
        android.util.Log.d("ProteinViewModel", "ViewModel initialized")
        android.util.Log.d("ProteinViewModel", "Initial highlightedChains: ${_uiState.value.highlightedChains}")
        loadDefaultProtein()
        loadCategoryCounts()
        observeProteinDatabase()
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
                    category = ProteinCategory.ENZYMES, // 기본값
                    description = detail.description,
                    organism = detail.organism,
                    resolution = detail.resolution?.toFloat(),
                    experimentalMethod = detail.experimentalMethod,
                    depositionDate = detail.depositionDate,
                    molecularWeight = detail.molecularWeight?.toFloat()
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
                
                // 구조 로딩 완료 - 즉시 Info 모드로 전환
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loadingProgress = "",
                        structure = structure,
                        selectedProtein = detail,
                        currentProteinId = proteinId,
                        currentProteinName = detail.name,
                        viewMode = ViewMode.INFO // Info 모드로 전환
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
    
    /**
     * 3D 렌더링 완료 시 호출되는 콜백
     * 현재는 InfoModeScreen에서 직접 관리하므로 빈 함수
     */
    fun onRenderingComplete() {
        // InfoModeScreen에서 3D 렌더링 상태를 직접 관리
        android.util.Log.d("ProteinViewModel", "3D rendering completed")
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
        // 로딩 시작
        _uiState.update { it.copy(isLoadingInfoTab = true) }
        
        // 탭 변경
        _uiState.update { it.copy(selectedInfoTab = tab) }
        
        // 로딩 시뮬레이션 (실제 데이터 처리 시간)
        viewModelScope.launch {
            kotlinx.coroutines.delay(500) // 0.5초 로딩
            _uiState.update { it.copy(isLoadingInfoTab = false) }
        }
    }
    
    fun setViewMode(mode: ViewMode) {
        val currentMode = _uiState.value.viewMode
        _uiState.update { 
            it.copy(
                viewMode = mode,
                previousViewMode = currentMode // 현재 모드를 이전 모드로 저장
            ) 
        }
        android.util.Log.d("ProteinViewModel", "View mode changed from $currentMode to $mode")
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    // 카테고리 관련 함수들
    fun selectCategory(category: ProteinCategory?) {
        _uiState.update { 
            it.copy(
                selectedCategory = category
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
                selectedCategory = null
            ) 
        }
    }
    
    fun hideCategoryGrid() {
        _uiState.update { it.copy() }
    }
    
    // 아이폰과 동일한 카테고리 카운트 로딩
    fun loadCategoryCounts() {
        viewModelScope.launch {
            _uiState.update { it.copy(showingLoadingPopup = true) }
            
            try {
                val categoryCounts = mutableMapOf<ProteinCategory, Int>()
                
                // 각 카테고리별로 API 호출하여 실제 개수 가져오기
                for (category in ProteinCategory.values()) {
                    try {
                        // 새로운 함수를 사용하여 실제 총 개수 가져오기
                        val totalCount = repository.getCategoryCount(category)
                        categoryCounts[category] = totalCount
                        
                        // API 부하 방지를 위한 짧은 지연 (아이폰과 동일: 0.2초)
                        kotlinx.coroutines.delay(200)
                        
                    } catch (e: Exception) {
                        // 실패 시 샘플 데이터 개수 사용 (아이폰과 동일)
                        categoryCounts[category] = getSampleProteinCount(category)
                    }
                }
                
                _uiState.update { 
                    it.copy(
                        categoryProteinCounts = categoryCounts,
                        showingLoadingPopup = false
                    ) 
                }
                
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        error = e.message ?: "Failed to load category counts",
                        showingLoadingPopup = false
                    ) 
                }
            }
        }
    }
    
    // 샘플 데이터 개수 반환 (아이폰과 동일)
    private fun getSampleProteinCount(category: ProteinCategory): Int {
        return when (category) {
            ProteinCategory.ENZYMES -> 45000
            ProteinCategory.STRUCTURAL -> 32000
            ProteinCategory.DEFENSE -> 18000
            ProteinCategory.TRANSPORT -> 25000
            ProteinCategory.HORMONES -> 8000
            ProteinCategory.STORAGE -> 5000
            ProteinCategory.RECEPTORS -> 15000
            ProteinCategory.MEMBRANE -> 12000
            ProteinCategory.MOTOR -> 6000
            ProteinCategory.SIGNALING -> 12000
            ProteinCategory.CHAPERONES -> 3000
            ProteinCategory.METABOLIC -> 38000
        }
    }
    
    
    /**
     * ProteinDatabase 상태 관찰 (iPhone 앱과 동일한 방식)
     */
    private fun observeProteinDatabase() {
        viewModelScope.launch {
            // 카테고리별 개수 관찰
            proteinDatabase.categoryTotalCounts.collect { categoryCounts ->
                _uiState.update { 
                    it.copy(categoryProteinCounts = categoryCounts)
                }
                android.util.Log.d("ProteinViewModel", "📊 카테고리 개수 업데이트: $categoryCounts")
            }
        }
        
        viewModelScope.launch {
            // 단백질 목록 관찰
            proteinDatabase.proteins.collect { proteins ->
                val proteinsByCategory = proteins.groupBy { protein -> protein.category }
                _uiState.update { 
                    it.copy(
                        allProteinsByCategory = proteinsByCategory,
                        searchResults = proteins
                    )
                }
                android.util.Log.d("ProteinViewModel", "📦 단백질 목록 업데이트: ${proteins.size}개")
            }
        }
        
        viewModelScope.launch {
            // 로딩 상태 관찰
            proteinDatabase.isLoading.collect { isLoading ->
                _uiState.update { 
                    it.copy(isLoadingCategoryCounts = isLoading)
                }
            }
        }
        
        viewModelScope.launch {
            // 에러 메시지 관찰
            proteinDatabase.errorMessage.collect { errorMessage ->
                _uiState.update { 
                    it.copy(error = errorMessage)
                }
            }
        }
        
        viewModelScope.launch {
            // 즐겨찾기 관찰
            proteinDatabase.favorites.collect { favorites ->
                _uiState.update { 
                    it.copy(favorites = favorites)
                }
            }
        }
    }
    
    /**
     * 특정 카테고리의 단백질 로드
     */
    fun loadCategoryProteins(category: ProteinCategory) {
        viewModelScope.launch {
            try {
                android.util.Log.d("ProteinViewModel", "🔄 ${category.displayName} 카테고리 단백질 로드...")
                proteinDatabase.loadProteins(category, refresh = true)
            } catch (e: Exception) {
                android.util.Log.e("ProteinViewModel", "❌ ${category.displayName} 카테고리 로드 실패: ${e.message}")
            }
        }
    }
    
    /**
     * 카테고리 개수 새로고침
     */
    fun refreshCategoryCounts() {
        viewModelScope.launch {
            _uiState.update { it.copy(showingLoadingPopup = true) }
            try {
                android.util.Log.d("ProteinViewModel", "🔄 카테고리 개수 새로고침...")
                proteinDatabase.loadAllCategoryCounts()
            } catch (e: Exception) {
                android.util.Log.e("ProteinViewModel", "❌ 카테고리 개수 새로고침 실패: ${e.message}")
            } finally {
                _uiState.update { it.copy(showingLoadingPopup = false) }
            }
        }
    }
}
