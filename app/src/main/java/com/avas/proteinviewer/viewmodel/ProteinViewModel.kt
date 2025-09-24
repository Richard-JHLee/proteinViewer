package com.avas.proteinviewer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.avas.proteinviewer.data.model.PDBStructure
import com.avas.proteinviewer.data.model.ProteinMetadata
import com.avas.proteinviewer.data.repository.ProteinRepository
import com.avas.proteinviewer.data.local.SampleData
import com.avas.proteinviewer.ui.state.AppState
import com.avas.proteinviewer.ui.library.ProteinInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log
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
    
    private val _searchResults = MutableStateFlow<List<ProteinInfo>>(emptyList())
    val searchResults: StateFlow<List<ProteinInfo>> = _searchResults.asStateFlow()
    
    private val _appState = MutableStateFlow(AppState())
    val appState: StateFlow<AppState> = _appState.asStateFlow()
    
    // 현재 선택된 카테고리
    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()
    
    // Load More 기능을 위한 상태
    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()
    
    // 각 카테고리별 페이지네이션 상태
    private val categoryPages = mutableMapOf<String, Int>()
    private val categoryHasMore = mutableMapOf<String, Boolean>()
    
    // 카테고리별 단백질 개수 (PDB 실제 데이터 기반)
    private val _categoryProteinCounts = MutableStateFlow<Map<String, Int>>(
        mapOf(
            "Enzymes" to 45000,
            "Structural" to 32000,
            "Defense" to 18000,
            "Transport" to 25000,
            "Hormones" to 8000,
            "Storage" to 5000,
            "Receptors" to 15000,
            "Membrane" to 28000,
            "Motor" to 6000,
            "Signaling" to 12000,
            "Chaperones" to 4000,
            "Metabolic" to 38000
        )
    )
    val categoryProteinCounts: StateFlow<Map<String, Int>> = _categoryProteinCounts.asStateFlow()
    
    // 카테고리별 실제 단백질 데이터 (아이폰과 동일한 기능)
    private val _categoryProteins = MutableStateFlow<Map<String, List<ProteinInfo>>>(emptyMap())
    val categoryProteins: StateFlow<Map<String, List<ProteinInfo>>> = _categoryProteins.asStateFlow()
    
    // 카테고리 로딩 상태
    private val _isLoadingCategoryCounts = MutableStateFlow(true)
    val isLoadingCategoryCounts: StateFlow<Boolean> = _isLoadingCategoryCounts.asStateFlow()
    
    init {
        // ViewModel 초기화 시 모든 카테고리 개수 병렬 로드
        Log.d("ProteinViewModel", "ViewModel init - starting loadAllCategoryCounts()")
        loadAllCategoryCounts()
    }
    
    /**
     * Load Data 버튼 클릭 시 모든 카테고리 개수 새로고침 (아이폰과 동일한 기능)
     */
    fun refreshAllCategoryCounts() {
        Log.d("ProteinViewModel", "refreshAllCategoryCounts() called - Load Data button clicked")
        _isLoadingCategoryCounts.value = true
        loadAllCategoryCounts()
    }
    
    /**
     * 모든 카테고리의 단백질 개수를 병렬로 로드 (앱 시작 시 호출)
     */
    fun loadAllCategoryCounts() {
        Log.d("ProteinViewModel", "loadAllCategoryCounts() called")
        viewModelScope.launch {
            Log.d("ProteinViewModel", "Starting parallel loading of all category counts")
            
            // UI에서 사용하는 모든 카테고리 이름
            val allCategories = listOf(
                "Enzymes", "Structural", "Defense", "Transport", 
                "Hormones", "Storage", "Receptors", "Membrane", 
                "Motor", "Signaling", "Chaperones", "Metabolic"
            )
            
            // 병렬로 모든 카테고리 로드
            val jobs = allCategories.map { category ->
                launch {
                    try {
                        Log.d("ProteinViewModel", "Loading count for category: $category")
                        val repositoryCategory = mapUICategoryToRepository(category)
                        val response = repository.searchProteinsByCategory(repositoryCategory)
                        
                        if (response.isSuccess) {
                            val count = response.getOrNull() ?: 0
                            // 현재 카운트 맵을 업데이트
                            val currentCounts = _categoryProteinCounts.value.toMutableMap()
                            currentCounts[category] = count
                            _categoryProteinCounts.value = currentCounts
                            Log.d("ProteinViewModel", "✅ $category: $count proteins loaded")
                        } else {
                            Log.w("ProteinViewModel", "❌ Failed to load count for $category")
                            // 실패 시 샘플 데이터 개수 사용
                            val sampleCount = getSampleCountForCategory(category)
                            val currentCounts = _categoryProteinCounts.value.toMutableMap()
                            currentCounts[category] = sampleCount
                            _categoryProteinCounts.value = currentCounts
                            Log.d("ProteinViewModel", "📊 $category: Using sample count $sampleCount")
                        }
                        
                        // 아이폰과 동일: API 부하 방지를 위한 짧은 지연
                        kotlinx.coroutines.delay(200) // 0.2초
                    } catch (e: Exception) {
                        Log.e("ProteinViewModel", "Error loading count for $category: ${e.message}")
                        // 에러 시 샘플 데이터 개수 사용
                        val sampleCount = getSampleCountForCategory(category)
                        val currentCounts = _categoryProteinCounts.value.toMutableMap()
                        currentCounts[category] = sampleCount
                        _categoryProteinCounts.value = currentCounts
                        Log.d("ProteinViewModel", "📊 $category: Using sample count $sampleCount due to error")
                    }
                }
            }
            
            // 모든 작업 완료 대기
            jobs.forEach { it.join() }
            _isLoadingCategoryCounts.value = false
            Log.d("ProteinViewModel", "🎉 All category counts loaded successfully")
            Log.d("ProteinViewModel", "Final counts: ${_categoryProteinCounts.value}")
        }
    }
    
    /**
     * 카테고리 선택 및 해당 카테고리의 단백질 개수 로드
     */
    fun selectCategory(category: String) {
        Log.d("ProteinViewModel", "selectCategory called with: $category")
        _selectedCategory.value = category
        loadCategoryCount(category)
    }
    
    /**
     * UI 카테고리 이름을 Repository 매핑용 이름으로 변환
     */
    private fun mapUICategoryToRepository(category: String): String {
        val mapping = mapOf(
            "Enzymes" to "enzymes",
            "Structural" to "structural", 
            "Defense" to "defense",
            "Transport" to "transport",
            "Hormones" to "hormones",
            "Storage" to "storage",
            "Receptors" to "receptors",
            "Membrane" to "membrane",
            "Motor" to "motor",
            "Signaling" to "signaling",
            "Chaperones" to "chaperones",
            "Metabolic" to "metabolic"
        )
        return mapping[category] ?: category.lowercase()
    }
    
    /**
     * 카테고리 선택 시 실제 단백질 데이터 로드 (아이폰과 동일한 기능)
     */
    fun loadCategoryProteins(category: String) {
        Log.d("ProteinViewModel", "loadCategoryProteins() called for: $category")
        viewModelScope.launch {
            Log.d("ProteinViewModel", "Starting category proteins loading for: $category")
            
            // UI 카테고리 이름을 Repository 매핑용 이름으로 변환
            val repositoryCategory = mapUICategoryToRepository(category)
            Log.d("ProteinViewModel", "Processing category: $category -> $repositoryCategory")
            
            try {
                // 아이폰과 동일: 실제 단백질 데이터 로드 (30개씩)
                Log.d("ProteinViewModel", "$category: Loading actual protein data from API")
                
                val response = repository.searchProteinsByCategory(repositoryCategory, 30, 0)
                
                Log.d("ProteinViewModel", "$category: API response received - Success: ${response.isSuccess}")
                
                if (response.isSuccess) {
                    val proteins = response.getOrNull() ?: emptyList()
                    Log.d("ProteinViewModel", "$category: Retrieved ${proteins.size} proteins from API")
                    
                    // 단백질 정보 로그 출력
                    proteins.take(5).forEach { protein ->
                        Log.d("ProteinViewModel", "$category: Protein - PDB: ${protein.pdbId}, Name: ${protein.name}")
                    }
                    
                    // 현재 단백질 리스트를 업데이트
                    val currentMap = _categoryProteins.value.toMutableMap()
                    currentMap[category] = proteins
                    _categoryProteins.value = currentMap
                    
                    // 페이지네이션 상태 초기화
                    categoryPages[category] = 0
                    categoryHasMore[category] = proteins.size >= 30
                    
                    Log.d("ProteinViewModel", "$category: ${proteins.size}개 단백질 로드 완료")
                    Log.d("ProteinViewModel", "Current categoryProteins map: ${_categoryProteins.value}")
                } else {
                    val exception = response.exceptionOrNull()
                    Log.w("ProteinViewModel", "$category: 단백질 데이터 로드 실패 - ${exception?.message}")
                    
                    // 실패 시 빈 리스트로 설정
                    val currentMap = _categoryProteins.value.toMutableMap()
                    currentMap[category] = emptyList()
                    _categoryProteins.value = currentMap
                    
                    Log.d("ProteinViewModel", "$category: Set empty list due to failure")
                }
            } catch (e: Exception) {
                Log.e("ProteinViewModel", "$category: 예외 발생", e)
                
                // 예외 시 빈 리스트로 설정
                val currentMap = _categoryProteins.value.toMutableMap()
                currentMap[category] = emptyList()
                _categoryProteins.value = currentMap
                
                Log.d("ProteinViewModel", "$category: Set empty list due to exception")
            }
        }
    }

    /**
     * 선택된 카테고리의 단백질 개수만 로드
     */
    fun loadCategoryCount(category: String) {
        Log.d("ProteinViewModel", "loadCategoryCount() called for: $category")
        viewModelScope.launch {
            Log.d("ProteinViewModel", "Starting category count loading for: $category")
            
            // UI 카테고리 이름을 Repository 매핑용 이름으로 변환
            val repositoryCategory = mapUICategoryToRepository(category)
            Log.d("ProteinViewModel", "Processing category: $category -> $repositoryCategory")
            
            try {
                // 아이폰과 동일: 변환된 카테고리 이름을 Repository에 전달
                Log.d("ProteinViewModel", "$category: Searching category directly as '$repositoryCategory'")
                
                val response = repository.searchProteinsByCategory(repositoryCategory)
                
                if (response.isSuccess) {
                    val count = response.getOrNull() ?: 0
                    
                    // 현재 카운트 맵을 업데이트
                    val currentCounts = _categoryProteinCounts.value.toMutableMap()
                    currentCounts[category] = count
                    _categoryProteinCounts.value = currentCounts
                    Log.d("ProteinViewModel", "$category: Final count $count proteins")
                } else {
                    // 검색 실패 시 샘플 데이터 개수 사용
                    val sampleCount = getSampleCountForCategory(category)
                    val currentCounts = _categoryProteinCounts.value.toMutableMap()
                    currentCounts[category] = sampleCount
                    _categoryProteinCounts.value = currentCounts
                    Log.d("ProteinViewModel", "$category: All searches failed, using sample count $sampleCount")
                }
            } catch (e: Exception) {
                // 예외 발생 시 샘플 데이터 개수 사용
                val sampleCount = getSampleCountForCategory(category)
                val currentCounts = _categoryProteinCounts.value.toMutableMap()
                currentCounts[category] = sampleCount
                _categoryProteinCounts.value = currentCounts
                Log.d("ProteinViewModel", "$category: Exception, using sample count $sampleCount: ${e.message}")
            }
        }
    }
    
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

    fun updateHighlightedPockets(pockets: Set<String>) {
        _appState.value = _appState.value.copy(highlightedPockets = pockets)
    }

    fun updateFocusedPocket(pocket: String?) {
        _appState.value = _appState.value.copy(focusedPocket = pocket)
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
                    // PDB 파일에서 직접 제목 추출 (API 호출 제거)
                    val proteinName = loadedStructure.title ?: "Crambin"
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
    
    /**
     * PDB ID로 검색하여 단백질 리스트에 추가
     */
    fun searchProteinByPDBId(pdbId: String) {
        Log.d("ProteinViewModel", "searchProteinByPDBId called with PDB ID: $pdbId")
        viewModelScope.launch {
            _isLoading.value = true
            _loadingProgress.value = "Searching for protein $pdbId..."
            _error.value = null
            
            try {
                // Step 1: Search for PDB ID existence
                val searchResult = repository.searchByPDBId(pdbId)
                
                if (searchResult.isFailure) {
                    val error = searchResult.exceptionOrNull()
                    _error.value = when (error) {
                        is com.avas.proteinviewer.data.error.PDBError -> error.userFriendlyMessage
                        else -> "Failed to search protein: ${error?.message}"
                    }
                    _isLoading.value = false
                    _loadingProgress.value = ""
                    return@launch
                }
                
                val exists = searchResult.getOrThrow()
                if (!exists) {
                    _error.value = "Protein '$pdbId' not found in the database. Please try a different protein."
                    _isLoading.value = false
                    _loadingProgress.value = ""
                    return@launch
                }
                
                Log.d("ProteinViewModel", "PDB ID $pdbId found, adding to protein list...")
                _loadingProgress.value = "Adding protein $pdbId to list..."
                
                // Step 2: Create ProteinInfo for the found PDB ID
                val proteinInfo = createProteinInfoFromPDBId(pdbId)
                
                // Step 3: Add to search results (check for duplicates)
                val currentSearchResults = _searchResults.value.toMutableList()
                
                // Check if protein already exists in search results
                val alreadyExists = currentSearchResults.any { it.pdbId.equals(pdbId, ignoreCase = true) }
                
                if (!alreadyExists) {
                    currentSearchResults.add(proteinInfo)
                    _searchResults.value = currentSearchResults
                    Log.d("ProteinViewModel", "Successfully added protein $pdbId to search results")
                } else {
                    Log.d("ProteinViewModel", "Protein $pdbId already exists in search results, skipping duplicate")
                }
                
                _isLoading.value = false
                _loadingProgress.value = ""
                
            } catch (e: Exception) {
                Log.e("ProteinViewModel", "Error searching protein $pdbId", e)
                _error.value = "Failed to search protein $pdbId: ${e.message}"
                _isLoading.value = false
                _loadingProgress.value = ""
            }
        }
    }

    /**
     * PDB ID로부터 ProteinInfo 생성
     */
    private fun createProteinInfoFromPDBId(pdbId: String): ProteinInfo {
        return ProteinInfo(
            pdbId = pdbId.uppercase(),
            name = generateProteinName(pdbId, "Search"),
            description = generateProteinDescription(pdbId, "Search", 1.0),
            categoryName = "Search Results"
        )
    }

    /**
     * 단백질 이름 생성 (검색 결과용)
     */
    private fun generateProteinName(pdbId: String, category: String): String {
        return when {
            pdbId == "1AOR" -> "Aldehyde Ferredoxin Oxidoreductase - 1AOR"
            pdbId == "1CRN" -> "Crambin - 1CRN"
            pdbId == "1HHO" -> "Hemoglobin - 1HHO"
            pdbId == "1LYZ" -> "Lysozyme C - 1LYZ"
            pdbId == "1TIM" -> "Triosephosphate Isomerase - 1TIM"
            pdbId == "1AKE" -> "Adenylate Kinase - 1AKE"
            else -> "Protein $pdbId"
        }
    }

    /**
     * 단백질 설명 생성 (검색 결과용)
     */
    private fun generateProteinDescription(pdbId: String, category: String, score: Double): String {
        return when {
            pdbId == "1AOR" -> "Structure of a hyperthermophilic tungstopterin enzyme, aldehyde ferredoxin oxidoreductase | Method: X-RAY DIFFRACTION | Resolution: 2.30Å | Organism: Pyrococcus furiosus"
            pdbId == "1CRN" -> "Crambin protein | Method: X-RAY DIFFRACTION | Resolution: 0.54Å | Organism: Crambe abyssinica"
            pdbId == "1HHO" -> "Human deoxyhemoglobin | Method: X-RAY DIFFRACTION | Resolution: 1.74Å | Organism: Homo sapiens"
            pdbId == "1LYZ" -> "Lysozyme C enzyme | Method: X-RAY DIFFRACTION | Resolution: 1.65Å | Organism: Gallus gallus"
            pdbId == "1TIM" -> "Triosephosphate isomerase enzyme | Method: X-RAY DIFFRACTION | Resolution: 1.20Å | Organism: Gallus gallus"
            pdbId == "1AKE" -> "Adenylate kinase enzyme | Method: X-RAY DIFFRACTION | Resolution: 1.50Å | Organism: Escherichia coli"
            else -> "Protein from $category category | Method: X-RAY DIFFRACTION | Resolution: 2.00Å | Organism: Unknown"
        }
    }

    fun loadSelectedProtein(pdbId: String) {
        Log.d("ProteinViewModel", "loadSelectedProtein called with PDB ID: $pdbId")
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
                
                // PDB 파일에서 직접 제목 추출 (API 호출 제거)
                val proteinName = loadedStructure.title ?: "Protein $pdbId"
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
    
    /**
     * 카테고리별 단백질 개수 로드
     */
    fun loadCategoryCounts() {
        Log.d("ProteinViewModel", "loadCategoryCounts() called")
        viewModelScope.launch {
            Log.d("ProteinViewModel", "Starting category counts loading in coroutine")
            val counts = mutableMapOf<String, Int>()
            
                   // 카테고리별 검색어 정의 (아이폰과 정확히 동일)
                   val categorySearchTerms = mapOf(
                       "Enzymes" to listOf("lysozyme", "catalase", "peroxidase", "isomerase", "ribonuclease", "thioredoxin", "kinase", "phosphatase", "transferase", "hydrolase"),
                       "Structural" to listOf("structural protein", "structural", "protein", "collagen", "keratin", "elastin", "fibroin", "laminin", "actin", "tubulin", "titin", "spectrin", "dystrophin", "vimentin", "desmin", "lamin", "neurofilament", "cytoskeleton", "intermediate filament", "microtubule", "microfilament", "thick filament", "thin filament", "scaffold", "matrix", "filament", "fiber", "bundle", "network", "fibrin", "fibronectin", "tenascin", "osteopontin", "bone sialoprotein", "osteocalcin", "myosin", "tropomyosin", "troponin", "nebulin", "dystrophin", "utrophin"),
                       "Defense" to listOf("immunoglobulin", "antibody", "complement", "lysozyme", "defensin", "interferon", "interleukin", "cytokine", "antigen", "immune"),
                       "Transport" to listOf("hemoglobin", "myoglobin", "transferrin", "albumin", "transporter", "channel", "pump", "carrier", "receptor", "binding"),
                       "Hormones" to listOf("insulin", "hormone", "growth", "cytokine", "signaling", "receptor", "factor", "regulator", "activator", "inhibitor"),
                       "Storage" to listOf("ferritin", "albumin", "casein", "ovalbumin", "lactoferrin", "vitellogenin", "transferrin", "ceruloplasmin", "storage", "binding", "reserve", "depot", "accumulation", "sequestration", "retention", "metal", "iron", "calcium", "zinc"),
                       "Receptors" to listOf("receptor", "gpcr", "neurotransmitter", "agonist", "antagonist", "ligand", "binding", "membrane", "signaling", "activation"),
                       "Membrane" to listOf("membrane", "integral", "peripheral", "transmembrane", "lipid", "channel", "pore", "transporter", "pump", "barrier"),
                       "Motor" to listOf("motor", "kinesin", "dynein", "myosin", "movement", "transport", "cargo", "microtubule", "actin", "contraction"),
                       "Signaling" to listOf("signaling", "pathway", "cascade", "messenger", "factor", "protein", "transduction", "activation", "regulation", "response"),
                       "Chaperones" to listOf("chaperone", "chaperonin", "folding", "hsp", "shock", "protein", "assistance", "quality", "control", "refolding"),
                       "Metabolic" to listOf("metabolic", "metabolism", "pathway", "biosynthesis", "catabolism", "anabolism", "glycolysis", "citric", "fatty", "amino")
                   )
            
            for ((category, searchTerms) in categorySearchTerms) {
                Log.d("ProteinViewModel", "Processing category: $category")
                try {
                    // 아이폰과 동일: 상위 8개 검색어를 사용하여 더 포괄적인 검색
                    val topSearchTerms = searchTerms.take(8)
                    Log.d("ProteinViewModel", "$category: Using search terms: $topSearchTerms")
                    var maxCount = 0
                    
                    for (searchTerm in topSearchTerms) {
                        Log.d("ProteinViewModel", "$category: Searching with term: '$searchTerm'")
                        val response = repository.searchProteinsByCategory(searchTerm)
                        
                        if (response.isSuccess) {
                            val count = response.getOrNull() ?: 0
                            if (count > maxCount) {
                                maxCount = count
                            }
                            Log.d("ProteinViewModel", "$category with '$searchTerm': $count proteins")
                        }
                    }
                    
                    if (maxCount > 0) {
                        counts[category] = maxCount
                        Log.d("ProteinViewModel", "$category: Final count $maxCount proteins")
                    } else {
                        // 모든 검색어 실패 시 샘플 데이터 개수 사용
                        counts[category] = getSampleCountForCategory(category)
                        Log.d("ProteinViewModel", "$category: All searches failed, using sample count ${counts[category]}")
                    }
                } catch (e: Exception) {
                    // 예외 발생 시 샘플 데이터 개수 사용
                    counts[category] = getSampleCountForCategory(category)
                    Log.d("ProteinViewModel", "$category: Exception, using sample count ${counts[category]}: ${e.message}")
                }
            }
            
            _categoryProteinCounts.value = counts
        }
    }
    
    /**
     * Load More 기능 - iOS와 동일한 페이지네이션
     */
    fun loadMoreProteins() {
        val currentCategory = _selectedCategory.value
        if (currentCategory == null) {
            Log.w("ProteinViewModel", "Load More: No category selected")
            return
        }
        
        if (_isLoadingMore.value) {
            Log.w("ProteinViewModel", "Load More: Already loading, ignoring request")
            return
        }
        
        Log.d("ProteinViewModel", "Load More: Starting for category $currentCategory")
        _isLoadingMore.value = true
        
        viewModelScope.launch {
            try {
                val repositoryCategory = mapUICategoryToRepository(currentCategory)
                val currentPage = categoryPages[currentCategory] ?: 0
                val nextPage = currentPage + 1
                val skip = nextPage * 30
                
                Log.d("ProteinViewModel", "Load More: Loading page $nextPage (skip: $skip)")
                
                val response = repository.searchProteinsByCategory(repositoryCategory, 30, skip)
                
                if (response.isSuccess) {
                    val newProteins = response.getOrNull() ?: emptyList()
                    Log.d("ProteinViewModel", "Load More: Retrieved ${newProteins.size} new proteins")
                    
                    if (newProteins.isNotEmpty()) {
                        // 기존 단백질 리스트에 추가
                        val currentMap = _categoryProteins.value.toMutableMap()
                        val existingProteins = currentMap[currentCategory] ?: emptyList()
                        val updatedProteins = existingProteins + newProteins
                        currentMap[currentCategory] = updatedProteins
                        _categoryProteins.value = currentMap
                        
                        // 페이지 상태 업데이트
                        categoryPages[currentCategory] = nextPage
                        categoryHasMore[currentCategory] = newProteins.size >= 30
                        
                        Log.d("ProteinViewModel", "Load More: Updated total proteins to ${updatedProteins.size}")
                    } else {
                        // 더 이상 데이터가 없음
                        categoryHasMore[currentCategory] = false
                        Log.d("ProteinViewModel", "Load More: No more proteins available")
                    }
                } else {
                    Log.w("ProteinViewModel", "Load More: Failed to load more proteins")
                }
            } catch (e: Exception) {
                Log.e("ProteinViewModel", "Load More: Exception occurred", e)
            } finally {
                _isLoadingMore.value = false
            }
        }
    }
    
    /**
     * Load More 버튼 표시 여부 확인
     */
    fun shouldShowLoadMore(): Boolean {
        val currentCategory = _selectedCategory.value ?: return false
        val currentProteins = _categoryProteins.value[currentCategory] ?: emptyList()
        val totalCount = _categoryProteinCounts.value[currentCategory] ?: 0
        val hasMore = categoryHasMore[currentCategory] ?: true
        
        // iOS와 동일한 로직: 30개 미만이거나 더 가져올 수 있는 경우
        return currentProteins.size < totalCount && hasMore
    }
    
    /**
     * 카테고리별 샘플 데이터 개수 반환
     */
    private fun getSampleCountForCategory(category: String): Int {
        return when (category) {
            "Enzymes" -> 3
            "Structural" -> 2
            "Defense" -> 1
            "Transport" -> 2
            "Hormones" -> 1
            "Storage" -> 1
            "Receptors" -> 1
            "Membrane" -> 1
            "Motor" -> 1
            "Signaling" -> 1
            "Chaperones" -> 1
            "Metabolic" -> 1
            else -> 1
        }
    }
}
