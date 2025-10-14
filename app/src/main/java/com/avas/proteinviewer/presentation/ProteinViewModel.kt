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
    val showProteinDetail: Boolean = false, // 아이폰 showingInfoSheet와 동일
    val selectedProteinForDetail: ProteinInfo? = null, // Detail 모달에 표시할 단백질
    val selectedInfoTab: InfoTab = InfoTab.OVERVIEW,
    val viewMode: ViewMode = ViewMode.INFO,
    val previousViewMode: ViewMode? = null, // 이전 모드 추적
    // 아이폰 ProteinDatabase와 동일한 카테고리 관련 상태
    val selectedCategory: ProteinCategory? = null,
    val categoryProteinCounts: Map<ProteinCategory, Int> = emptyMap(),
    val categoryDataSource: Map<ProteinCategory, com.avas.proteinviewer.data.repository.ProteinDatabase.DataSource> = emptyMap(),
    val isLoadingCategoryCounts: Boolean = false,
    val showingLoadingPopup: Boolean = false,
    val currentPage: Int = 1,
    val isLoadingMore: Boolean = false,
    val hasMoreData: Boolean = false,
    val favorites: Set<String> = emptySet(),
    val searchText: String = "",
    val allProteinsByCategory: Map<ProteinCategory, List<ProteinInfo>> = emptyMap(),
    val loadedCategories: Set<ProteinCategory> = emptySet(),
    val isLoadingInfoTab: Boolean = false,
    // Disease Association & Research Status (아이폰과 동일)
    val diseaseAssociations: List<DiseaseAssociation> = emptyList(),
    val diseaseSummary: DiseaseSummary? = null,
    val isDiseaseLoading: Boolean = false,
    val diseaseError: String? = null,
    val researchStatus: ResearchStatus? = null,
    val researchSummary: ResearchSummary? = null,
    val isResearchLoading: Boolean = false,
    val researchError: String? = null,
    // Function Details (아이폰과 동일)
    val functionDetails: FunctionDetails? = null,
    val isFunctionLoading: Boolean = false,
    val functionError: String? = null,
    val showFunctionDetails: Boolean = false,
    // Structure Levels (아이폰과 동일)
    val showPrimaryStructure: Boolean = false,
    val primaryStructureData: PrimaryStructureData? = null,
    val isPrimaryStructureLoading: Boolean = false,
    val primaryStructureError: String? = null,
    val showSecondaryStructure: Boolean = false,
    val secondaryStructureData: List<SecondaryStructureData> = emptyList(),
    val isSecondaryStructureLoading: Boolean = false,
    val secondaryStructureError: String? = null,
    val showTertiaryStructure: Boolean = false,
    val tertiaryStructureData: TertiaryStructureData? = null,
    val isTertiaryStructureLoading: Boolean = false,
    val tertiaryStructureError: String? = null,
    val showQuaternaryStructure: Boolean = false,
    val quaternaryStructureData: QuaternaryStructureData? = null,
    val isQuaternaryStructureLoading: Boolean = false,
    val quaternaryStructureError: String? = null,
    // Related Proteins (아이폰과 동일)
    val showRelatedProteins: Boolean = false,
    val relatedProteins: List<RelatedProtein> = emptyList(),
    val isRelatedProteinsLoading: Boolean = false,
    val relatedProteinsError: String? = null,
    // Experimental Details (아이폰과 동일)
    val experimentalDetails: ExperimentalDetails? = null,
    val isExperimentalDetailsLoading: Boolean = false,
    // Research Detail (아이폰과 동일)
    val showResearchDetail: Boolean = false,
    val researchDetailType: ResearchDetailType? = null
)

enum class InfoTab {
    OVERVIEW, CHAINS, RESIDUES, LIGANDS, POCKETS, SEQUENCE, ANNOTATIONS
}

@HiltViewModel
class ProteinViewModel @Inject constructor(
    private val repository: ProteinRepository,
    private val proteinDatabase: com.avas.proteinviewer.data.repository.ProteinDatabase,
    private val diseaseAPIService: com.avas.proteinviewer.data.api.DiseaseAPIService,
    private val researchAPIService: com.avas.proteinviewer.data.api.ResearchAPIService,
    private val functionAPIService: com.avas.proteinviewer.data.api.FunctionAPIService,
    private val structureAPIService: com.avas.proteinviewer.data.api.StructureAPIService,
    private val relatedProteinsAPIService: com.avas.proteinviewer.data.api.RelatedProteinsAPIService,
    private val experimentalDetailsAPIService: com.avas.proteinviewer.data.api.ExperimentalDetailsAPIService,
    private val savedStateHandle: androidx.lifecycle.SavedStateHandle
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
                // 초기 로딩 시에는 PDB 구조를 로드하지 않음 (3D 뷰어를 보여주지 않으므로)
                // val structure = repository.loadPDBStructure(defaultId) { progress ->
                //     _uiState.update { it.copy(loadingProgress = progress) }
                // }
                
                // PDB 구조 다운로드 및 파싱
                val structure = repository.loadPDBStructure(defaultId) { progress ->
                    _uiState.update { it.copy(loadingProgress = progress) }
                }
                
                // 기본 ProteinInfo 생성
                val proteinInfo = ProteinInfo(
                    id = defaultId,
                    name = "Crambin",
                    category = ProteinCategory.ENZYMES,
                    description = "Plant seed protein from Crambe abyssinica",
                    organism = "Crambe abyssinica",
                    resolution = 1.5f,
                    experimentalMethod = "X-RAY DIFFRACTION",
                    depositionDate = "1981-01-01",
                    molecularWeight = 5000f
                )
                
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loadingProgress = "",
                        structure = structure, // 3D 구조 로드 완료
                        selectedProtein = null, // ProteinDetail은 API에서 가져와야 함
                        currentProteinId = defaultId,
                        currentProteinName = proteinInfo.name,
                        currentProteinInfo = proteinInfo
                    )
                }
                
                android.util.Log.d("ProteinViewModel", "Default protein loaded successfully: $defaultId")
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

    // 아이폰과 동일: Protein Library에서 단백질 선택 시 Detail 모달만 표시
    fun selectProteinFromLibrary(proteinInfo: ProteinInfo) {
        android.util.Log.d("ProteinViewModel", "📋 단백질 선택: ${proteinInfo.id} - Detail 모달 표시")
        _uiState.update {
            it.copy(
                showProteinDetail = true,
                selectedProteinForDetail = proteinInfo
                // Protein Library는 닫지 않음 (아이폰과 동일)
            )
        }
    }
    
    // Detail 모달 닫기
    fun dismissProteinDetail() {
        _uiState.update {
            it.copy(
                showProteinDetail = false,
                selectedProteinForDetail = null
            )
        }
    }
    
    // 아이폰과 동일: Detail 모달에서 "View 3D" 버튼 클릭 시
    fun loadProteinFor3DViewing(proteinId: String) {
        viewModelScope.launch {
            // Detail 모달 닫기
            _uiState.update {
                it.copy(
                    showProteinDetail = false,
                    selectedProteinForDetail = null,
                    showProteinLibrary = false, // Protein Library도 닫기
                    isLoading = true,
                    loadingProgress = "Loading 3D structure for $proteinId..."
                )
            }
            
            try {
                // PDB 구조 다운로드 및 파싱
                val structure = repository.loadPDBStructure(proteinId) { progress ->
                    _uiState.update { it.copy(loadingProgress = progress) }
                }
                
                val detail = repository.getProteinDetail(proteinId).first()
                
                // API에서 ProteinInfo도 가져오기 (Info 탭에 표시될 상세 정보)
                val proteinInfo = try {
                    repository.searchProteinByID(proteinId)
                } catch (e: Exception) {
                    null
                }
                
                // 구조 로딩 완료 - 메인 화면으로 이동
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loadingProgress = "",
                        structure = structure, // 3D 구조 로드 완료
                        selectedProtein = detail,
                        currentProteinId = proteinId,
                        currentProteinName = detail.name,
                        currentProteinInfo = proteinInfo, // API 상세 정보 업데이트
                        viewMode = ViewMode.INFO // Info 모드로
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
    
    // 기존 selectProtein은 유지 (다른 곳에서 사용될 수 있음)
    fun selectProtein(proteinId: String) {
        loadProteinFor3DViewing(proteinId)
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
                selectedCategory = category,
                currentPage = 1, // 페이지 리셋
                hasMoreData = true // 더보기 활성화
            ) 
        }
        
        // 아이폰과 동일: 카테고리 선택 시 해당 카테고리의 단백질 로드
        if (category != null) {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, loadingProgress = "Loading ${category.displayName}...") }
                
                try {
                    // 아이폰과 동일: 카테고리별 API 검색 (페이지네이션 지원)
                    val limit = 30 // 아이폰과 동일: 30개씩
                    val skip = 0 // 첫 페이지
                    
                    android.util.Log.d("ProteinViewModel", "🔍 [${category.displayName}] 카테고리 검색 (limit: $limit, skip: $skip)")
                    
                    // Repository 인터페이스를 통한 2단계 검색
                    val (pdbIds, totalCount) = repository.searchProteinIdsByCategory(category, limit, skip)
                    android.util.Log.d("ProteinViewModel", "✅ PDB IDs 받음: ${pdbIds.size}개, 전체: $totalCount")
                    
                    if (pdbIds.isNotEmpty()) {
                        // Repository를 통해 상세 정보 가져오기
                        val proteins = repository.getProteinsByIds(pdbIds)
                        
                        // 데이터 소스를 API로 업데이트
                        val updatedDataSource = _uiState.value.categoryDataSource.toMutableMap()
                        updatedDataSource[category] = com.avas.proteinviewer.data.repository.ProteinDatabase.DataSource.API
                        
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                loadingProgress = "",
                                searchResults = proteins,
                                hasMoreData = pdbIds.size >= limit, // 30개 이상이면 더 있음
                                categoryDataSource = updatedDataSource
                            )
                        }
                        
                        android.util.Log.d("ProteinViewModel", "✅ [${category.displayName}] ${proteins.size}개 단백질 로드 완료 (API 데이터)")
                    } else {
                        // 결과 없음: 샘플 데이터 사용
                        val sampleProteins = repository.searchProteinsByCategory(category, 30)
                        
                        // 데이터 소스를 SAMPLE로 업데이트
                        val updatedDataSource = _uiState.value.categoryDataSource.toMutableMap()
                        updatedDataSource[category] = com.avas.proteinviewer.data.repository.ProteinDatabase.DataSource.SAMPLE
                        
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                loadingProgress = "",
                                searchResults = sampleProteins,
                                hasMoreData = false,
                                categoryDataSource = updatedDataSource
                            )
                        }
                        
                        android.util.Log.d("ProteinViewModel", "⚠️ [${category.displayName}] 결과 없음, 샘플 데이터 사용")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ProteinViewModel", "❌ [${category.displayName}] 로드 실패: ${e.message}")
                    
                    // 에러 시 샘플 데이터 사용
                    val sampleProteins = repository.searchProteinsByCategory(category, 30)
                    
                    // 데이터 소스를 SAMPLE로 업데이트
                    val updatedDataSource = _uiState.value.categoryDataSource.toMutableMap()
                    updatedDataSource[category] = com.avas.proteinviewer.data.repository.ProteinDatabase.DataSource.SAMPLE
                    
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            loadingProgress = "",
                            searchResults = sampleProteins,
                            hasMoreData = false,
                            error = e.message,
                            categoryDataSource = updatedDataSource
                        )
                    }
                    
                    android.util.Log.d("ProteinViewModel", "⚠️ [${category.displayName}] 에러로 인한 샘플 데이터 사용")
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
    
    // 아이폰과 동일한 Load More 기능
    fun loadMore() {
        val category = _uiState.value.selectedCategory ?: return
        val currentPage = _uiState.value.currentPage
        
        if (_uiState.value.isLoadingMore || !_uiState.value.hasMoreData) {
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            
            try {
                val limit = 30 // 아이폰과 동일: 30개씩
                val skip = currentPage * limit // 다음 페이지
                
                android.util.Log.d("ProteinViewModel", "🔄 Load More: [${category.displayName}] (page: ${currentPage + 1}, skip: $skip)")
                
                // Repository를 통한 2단계 검색
                val (pdbIds, totalCount) = repository.searchProteinIdsByCategory(category, limit, skip)
                android.util.Log.d("ProteinViewModel", "✅ Load More PDB IDs 받음: ${pdbIds.size}개")
                
                if (pdbIds.isNotEmpty()) {
                    val newProteins = repository.getProteinsByIds(pdbIds)
                    
                    _uiState.update {
                        it.copy(
                            isLoadingMore = false,
                            searchResults = it.searchResults + newProteins, // 기존 리스트에 추가
                            currentPage = currentPage + 1,
                            hasMoreData = pdbIds.size >= limit // 30개 이상이면 더 있음
                        )
                    }
                    
                    android.util.Log.d("ProteinViewModel", "✅ Load More 완료: ${newProteins.size}개 추가, 총 ${_uiState.value.searchResults.size}개")
                } else {
                    // 더 이상 결과 없음
                    _uiState.update {
                        it.copy(
                            isLoadingMore = false,
                            hasMoreData = false
                        )
                    }
                    android.util.Log.d("ProteinViewModel", "⚠️ Load More: 더 이상 데이터 없음")
                }
            } catch (e: Exception) {
                android.util.Log.e("ProteinViewModel", "❌ Load More 실패: ${e.message}")
                _uiState.update {
                    it.copy(
                        isLoadingMore = false,
                        error = e.message
                    )
                }
            }
        }
    }
    
    // 아이폰과 동일한 Favorites 토글 기능
    fun toggleFavorite(proteinId: String) {
        viewModelScope.launch {
            try {
                proteinDatabase.toggleFavorite(proteinId)
                android.util.Log.d("ProteinViewModel", "❤️ Favorite 토글: $proteinId")
            } catch (e: Exception) {
                android.util.Log.e("ProteinViewModel", "❌ Favorite 토글 실패: ${e.message}")
            }
        }
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
            // 데이터 소스 관찰 (샘플 데이터인지 실제 API 데이터인지)
            proteinDatabase.categoryDataSource.collect { dataSources ->
                _uiState.update { 
                    it.copy(categoryDataSource = dataSources)
                }
                android.util.Log.d("ProteinViewModel", "📍 데이터 소스 업데이트: $dataSources")
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
    
    /**
     * 아이폰과 동일한 검색 타입 감지
     */
    private fun detectSearchType(searchText: String): SearchType {
        val trimmed = searchText.trim()
        
        // PDB ID 검사 (4자리 영숫자)
        if (trimmed.length == 4 && trimmed.all { it.isLetterOrDigit() }) {
            return SearchType.PDB_ID(trimmed.uppercase())
        }
        
        // 일반 텍스트 검색
        return SearchType.TEXT_SEARCH(trimmed)
    }
    
    /**
     * 검색 타입 열거형 (아이폰과 동일)
     */
    private sealed class SearchType {
        data class PDB_ID(val id: String) : SearchType()
        data class TEXT_SEARCH(val text: String) : SearchType()
    }
    
    /**
     * 아이폰과 동일한 검색 기반 데이터 로드
     */
    fun performSearchBasedDataLoad(searchText: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(showingLoadingPopup = true, currentPage = 1) }
            
            try {
                val searchType = detectSearchType(searchText)
                val searchResults = when (searchType) {
                    is SearchType.PDB_ID -> {
                        android.util.Log.d("ProteinViewModel", "🔍 PDB ID 검색: ${searchType.id}")
                        repository.searchProteinByID(searchType.id)?.let { listOf(it) } ?: emptyList()
                    }
                    is SearchType.TEXT_SEARCH -> {
                        android.util.Log.d("ProteinViewModel", "🔍 텍스트 검색: ${searchType.text}")
                        repository.searchProteinsByText(searchType.text)
                    }
                }
                
                _uiState.update {
                    it.copy(
                        searchResults = searchResults,
                        selectedCategory = null, // 카테고리 필터 해제
                        showingLoadingPopup = false
                    )
                }
                
                android.util.Log.d("ProteinViewModel", "✅ 검색 완료: ${searchResults.size}개 단백질 로드")
                
            } catch (e: Exception) {
                android.util.Log.e("ProteinViewModel", "❌ 검색 실패: ${e.message}")
                _uiState.update {
                    it.copy(
                        error = "검색 실패: ${e.message}",
                        showingLoadingPopup = false
                    )
                }
            }
        }
    }
    
    /**
     * 아이폰과 동일한 검색 버튼 정보 계산
     */
    fun getSearchButtonInfo(searchText: String): SearchButtonInfo {
        val trimmed = searchText.trim()
        
        return when {
            trimmed.length == 4 && trimmed.all { it.isLetterOrDigit() } -> {
                SearchButtonInfo("PDB ID Search", android.graphics.Color.parseColor("#9C27B0"), "magnifyingglass")
            }
            trimmed.length >= 2 -> {
                SearchButtonInfo("Text Search", android.graphics.Color.parseColor("#4CAF50"), "magnifyingglass")
            }
            else -> {
                SearchButtonInfo("Load Data", android.graphics.Color.parseColor("#2196F3"), "arrow.clockwise")
            }
        }
    }
    
    /**
     * 검색 버튼 정보 데이터 클래스
     */
    data class SearchButtonInfo(
        val text: String,
        val color: Int,
        val icon: String
    )
    
    // MARK: - Disease Association & Research Status (아이폰과 동일)
    
    /**
     * 아이폰 DetailedInfoSectionView.loadExperimentalDetails()와 동일
     * Additional Information 섹션에 표시할 실험 상세 정보 로드
     */
    fun loadExperimentalDetails(proteinId: String) {
        viewModelScope.launch {
            android.util.Log.d("ProteinViewModel", "🔍 Loading experimental details for: $proteinId")
            
            _uiState.update {
                it.copy(isExperimentalDetailsLoading = true)
            }
            
            try {
                val details = experimentalDetailsAPIService.fetchExperimentalDetails(proteinId)
                
                _uiState.update {
                    it.copy(
                        experimentalDetails = details,
                        isExperimentalDetailsLoading = false
                    )
                }
                
                android.util.Log.d("ProteinViewModel", "✅ Experimental details loaded")
                
            } catch (e: Exception) {
                android.util.Log.e("ProteinViewModel", "❌ Experimental details failed: ${e.message}")
                
                _uiState.update {
                    it.copy(isExperimentalDetailsLoading = false)
                }
            }
        }
    }
    
    /**
     * 아이폰 DiseaseAssociationView.loadDiseaseAssociations()와 동일
     * Detail 모달이 열릴 때 호출
     */
    fun loadDiseaseAssociations(proteinId: String) {
        viewModelScope.launch {
            android.util.Log.d("ProteinViewModel", "🔍 Loading disease associations for: $proteinId")
            
            _uiState.update {
                it.copy(
                    isDiseaseLoading = true,
                    diseaseError = null
                )
            }
            
            try {
                val diseases = diseaseAPIService.fetchDiseaseAssociations(proteinId)
                val summary = diseaseAPIService.createDiseaseSummary(diseases)
                
                _uiState.update {
                    it.copy(
                        diseaseAssociations = diseases,
                        diseaseSummary = summary,
                        isDiseaseLoading = false
                    )
                }
                
                android.util.Log.d("ProteinViewModel", "✅ Disease associations loaded: ${diseases.size} diseases")
                
            } catch (e: Exception) {
                android.util.Log.e("ProteinViewModel", "❌ Disease associations failed: ${e.message}")
                
                _uiState.update {
                    it.copy(
                        isDiseaseLoading = false,
                        diseaseError = e.message ?: "Failed to load disease data"
                    )
                }
            }
        }
    }
    
    /**
     * 아이폰 ResearchStatusViewModel.loadResearchStatus()와 동일
     * Detail 모달이 열릴 때 호출
     */
    fun loadResearchStatus(proteinId: String) {
        viewModelScope.launch {
            // 항상 새로 로드 (캐시 없음)
            _uiState.update {
                it.copy(
                    isResearchLoading = true,
                    researchError = null,
                    researchStatus = null, // 이전 데이터 초기화
                    researchSummary = null
                )
            }
            
            try {
                val research = researchAPIService.fetchResearchStatus(proteinId)
                val summary = researchAPIService.createResearchSummary(research)
                
                _uiState.update {
                    it.copy(
                        researchStatus = research,
                        researchSummary = summary,
                        isResearchLoading = false
                    )
                }
                
                android.util.Log.d("ProteinViewModel", "✅ Research status loaded: ${research.publications} publications, ${research.clinicalTrials} trials")
                
            } catch (e: Exception) {
                android.util.Log.e("ProteinViewModel", "❌ Research status failed: ${e.message}")
                
                _uiState.update {
                    it.copy(
                        isResearchLoading = false,
                        researchError = e.message ?: "Failed to load research data"
                    )
                }
            }
        }
    }
    
    /**
     * 아이폰 FunctionDetailsView와 동일
     * Function Summary에서 "View Details" 선택 시 호출
     */
    fun loadFunctionDetails(proteinId: String, proteinDescription: String) {
        viewModelScope.launch {
            android.util.Log.d("ProteinViewModel", "🔍 Loading function details for: $proteinId")
            
            _uiState.update {
                it.copy(
                    isFunctionLoading = true,
                    functionError = null,
                    showFunctionDetails = true
                )
            }
            
            try {
                val details = functionAPIService.fetchFunctionDetails(proteinId, proteinDescription)
                
                _uiState.update {
                    it.copy(
                        functionDetails = details,
                        isFunctionLoading = false
                    )
                }
                
                android.util.Log.d("ProteinViewModel", "✅ Function details loaded: ${details.goTerms.size} GO terms, ${details.ecNumbers.size} EC numbers")
                
            } catch (e: Exception) {
                android.util.Log.e("ProteinViewModel", "❌ Function details failed: ${e.message}")
                
                _uiState.update {
                    it.copy(
                        isFunctionLoading = false,
                        functionError = e.message ?: "Failed to load function details"
                    )
                }
            }
        }
    }
    
    /**
     * Function Details 모달 닫기
     */
    fun dismissFunctionDetails() {
        _uiState.update {
            it.copy(
                showFunctionDetails = false,
                functionDetails = null,
                functionError = null
            )
        }
    }
    
    // MARK: - Structure Levels (아이폰과 동일)
    
    /**
     * Primary Structure 로드
     */
    fun loadPrimaryStructure(proteinId: String) {
        viewModelScope.launch {
            android.util.Log.d("ProteinViewModel", "🔍 Loading primary structure for: $proteinId")
            
            _uiState.update {
                it.copy(
                    isPrimaryStructureLoading = true,
                    primaryStructureError = null,
                    showPrimaryStructure = true
                )
            }
            
            try {
                val data = structureAPIService.fetchPrimaryStructure(proteinId)
                
                _uiState.update {
                    it.copy(
                        primaryStructureData = data,
                        isPrimaryStructureLoading = false
                    )
                }
                
                android.util.Log.d("ProteinViewModel", "✅ Primary structure loaded: ${data.chains.size} chains")
                
            } catch (e: Exception) {
                android.util.Log.e("ProteinViewModel", "❌ Primary structure failed: ${e.message}")
                
                _uiState.update {
                    it.copy(
                        isPrimaryStructureLoading = false,
                        primaryStructureError = e.message ?: "Failed to load primary structure"
                    )
                }
            }
        }
    }
    
    fun dismissPrimaryStructure() {
        _uiState.update {
            it.copy(
                showPrimaryStructure = false,
                primaryStructureData = null,
                primaryStructureError = null
            )
        }
    }
    
    /**
     * Secondary Structure 로드
     */
    fun loadSecondaryStructure(proteinId: String) {
        viewModelScope.launch {
            android.util.Log.d("ProteinViewModel", "🔍 Loading secondary structure for: $proteinId")
            
            _uiState.update {
                it.copy(
                    isSecondaryStructureLoading = true,
                    secondaryStructureError = null,
                    showSecondaryStructure = true
                )
            }
            
            try {
                val data = structureAPIService.fetchSecondaryStructure(proteinId)
                
                _uiState.update {
                    it.copy(
                        secondaryStructureData = data,
                        isSecondaryStructureLoading = false
                    )
                }
                
                android.util.Log.d("ProteinViewModel", "✅ Secondary structure loaded: ${data.size} elements")
                
            } catch (e: Exception) {
                android.util.Log.e("ProteinViewModel", "❌ Secondary structure failed: ${e.message}")
                
                _uiState.update {
                    it.copy(
                        isSecondaryStructureLoading = false,
                        secondaryStructureError = e.message ?: "Failed to load secondary structure"
                    )
                }
            }
        }
    }
    
    fun dismissSecondaryStructure() {
        _uiState.update {
            it.copy(
                showSecondaryStructure = false,
                secondaryStructureData = emptyList(),
                secondaryStructureError = null
            )
        }
    }
    
    /**
     * Tertiary Structure 로드
     */
    fun loadTertiaryStructure(proteinId: String) {
        viewModelScope.launch {
            android.util.Log.d("ProteinViewModel", "🔍 Loading tertiary structure for: $proteinId")
            
            _uiState.update {
                it.copy(
                    isTertiaryStructureLoading = true,
                    tertiaryStructureError = null,
                    showTertiaryStructure = true
                )
            }
            
            try {
                val data = structureAPIService.fetchTertiaryStructure(proteinId)
                
                _uiState.update {
                    it.copy(
                        tertiaryStructureData = data,
                        isTertiaryStructureLoading = false
                    )
                }
                
                android.util.Log.d("ProteinViewModel", "✅ Tertiary structure loaded")
                
            } catch (e: Exception) {
                android.util.Log.e("ProteinViewModel", "❌ Tertiary structure failed: ${e.message}")
                
                _uiState.update {
                    it.copy(
                        isTertiaryStructureLoading = false,
                        tertiaryStructureError = e.message ?: "Failed to load tertiary structure"
                    )
                }
            }
        }
    }
    
    fun dismissTertiaryStructure() {
        _uiState.update {
            it.copy(
                showTertiaryStructure = false,
                tertiaryStructureData = null,
                tertiaryStructureError = null
            )
        }
    }
    
    /**
     * Quaternary Structure 로드
     */
    fun loadQuaternaryStructure(proteinId: String) {
        viewModelScope.launch {
            android.util.Log.d("ProteinViewModel", "🔍 Loading quaternary structure for: $proteinId")
            
            _uiState.update {
                it.copy(
                    isQuaternaryStructureLoading = true,
                    quaternaryStructureError = null,
                    showQuaternaryStructure = true
                )
            }
            
            try {
                val data = structureAPIService.fetchQuaternaryStructure(proteinId)
                
                _uiState.update {
                    it.copy(
                        quaternaryStructureData = data,
                        isQuaternaryStructureLoading = false
                    )
                }
                
                android.util.Log.d("ProteinViewModel", "✅ Quaternary structure loaded")
                
            } catch (e: Exception) {
                android.util.Log.e("ProteinViewModel", "❌ Quaternary structure failed: ${e.message}")
                
                _uiState.update {
                    it.copy(
                        isQuaternaryStructureLoading = false,
                        quaternaryStructureError = e.message ?: "Failed to load quaternary structure"
                    )
                }
            }
        }
    }
    
    fun dismissQuaternaryStructure() {
        _uiState.update {
            it.copy(
                showQuaternaryStructure = false,
                quaternaryStructureData = null,
                quaternaryStructureError = null
            )
        }
    }
    
    /**
     * 아이폰 RelatedProteinsView와 동일
     * Additional Information의 "View Details" 클릭 시 호출
     */
    fun loadRelatedProteins(proteinId: String) {
        viewModelScope.launch {
            android.util.Log.d("ProteinViewModel", "🔍 Loading related proteins for: $proteinId")
            
            _uiState.update {
                it.copy(
                    isRelatedProteinsLoading = true,
                    relatedProteinsError = null,
                    showRelatedProteins = true
                )
            }
            
            try {
                // 실제 API 호출
                val category = _uiState.value.selectedProteinForDetail?.category ?: ProteinCategory.ENZYMES
                val relatedProteins = relatedProteinsAPIService.fetchRelatedProteins(proteinId, category)
                
                _uiState.update {
                    it.copy(
                        relatedProteins = relatedProteins,
                        isRelatedProteinsLoading = false
                    )
                }
                
                android.util.Log.d("ProteinViewModel", "✅ Related proteins loaded: ${relatedProteins.size} proteins")
                
            } catch (e: Exception) {
                android.util.Log.e("ProteinViewModel", "❌ Related proteins failed: ${e.message}")
                
                _uiState.update {
                    it.copy(
                        isRelatedProteinsLoading = false,
                        relatedProteinsError = e.message ?: "Failed to load related proteins"
                    )
                }
            }
        }
    }
    
    fun dismissRelatedProteins() {
        _uiState.update {
            it.copy(
                showRelatedProteins = false,
                relatedProteins = emptyList(),
                relatedProteinsError = null
            )
        }
    }
    
    // MARK: - Research Detail Functions (아이폰과 동일)
    
    fun showResearchDetail(researchType: ResearchDetailType) {
        _uiState.update {
            it.copy(
                showResearchDetail = true,
                researchDetailType = researchType
            )
        }
    }
    
    fun dismissResearchDetail() {
        _uiState.update {
            it.copy(
                showResearchDetail = false,
                researchDetailType = null
            )
        }
    }
}
