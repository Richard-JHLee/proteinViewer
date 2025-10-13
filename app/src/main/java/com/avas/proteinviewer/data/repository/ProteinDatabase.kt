package com.avas.proteinviewer.data.repository

import com.avas.proteinviewer.data.api.PDBAPIService
import com.avas.proteinviewer.domain.model.ProteinCategory
import com.avas.proteinviewer.domain.model.ProteinInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProteinDatabase @Inject constructor(
    private val apiService: PDBAPIService
) {
    
    // 상태 관리
    private val _proteins = MutableStateFlow<List<ProteinInfo>>(emptyList())
    val proteins: StateFlow<List<ProteinInfo>> = _proteins.asStateFlow()
    
    private val _categoryTotalCounts = MutableStateFlow<Map<ProteinCategory, Int>>(emptyMap())
    val categoryTotalCounts: StateFlow<Map<ProteinCategory, Int>> = _categoryTotalCounts.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _favorites = MutableStateFlow<Set<String>>(emptySet())
    val favorites: StateFlow<Set<String>> = _favorites.asStateFlow()
    
    // 샘플 데이터 상태 추적 (카테고리별로 샘플 데이터인지 실제 API 데이터인지)
    private val _categoryDataSource = MutableStateFlow<Map<ProteinCategory, DataSource>>(emptyMap())
    val categoryDataSource: StateFlow<Map<ProteinCategory, DataSource>> = _categoryDataSource.asStateFlow()
    
    // 페이지네이션 상태 관리
    private val categoryPages = mutableMapOf<ProteinCategory, Int>()
    private val categoryHasMore = mutableMapOf<ProteinCategory, Boolean>()
    private val loadedCategories = mutableSetOf<ProteinCategory>()
    
    enum class DataSource {
        SAMPLE,  // 샘플 데이터
        API      // 실제 API 데이터
    }
    
    private val itemsPerPage = 30
    
    init {
        // 초기화 시 기본 샘플 데이터를 먼저 로드
        loadBasicSampleData()
        
        // API에서 실제 데이터 로드 시도 (비동기)
        kotlinx.coroutines.GlobalScope.launch {
            loadAllCategoryCounts()
        }
    }
    
    /**
     * 기본 샘플 데이터 로드 (iPhone 앱과 동일한 방식)
     */
    private fun loadBasicSampleData() {
        android.util.Log.d("ProteinDatabase", "🔄 Starting to load basic sample data...")
        val allSamples = mutableListOf<ProteinInfo>()
        val sampleSources = mutableMapOf<ProteinCategory, DataSource>()
        
        for (category in ProteinCategory.values()) {
            val samples = apiService.getSampleProteins(category)
            android.util.Log.d("ProteinDatabase", "📦 Category ${category.displayName}: ${samples.size} samples")
            allSamples.addAll(samples)
            sampleSources[category] = DataSource.SAMPLE // 모든 카테고리를 샘플 데이터로 표시
        }
        
        _proteins.value = allSamples
        _categoryDataSource.value = sampleSources
        android.util.Log.d("ProteinDatabase", "✅ Loaded ${allSamples.size} basic sample proteins for all categories")
    }
    
    /**
     * 모든 카테고리의 실제 API 총 개수 로드 (iPhone 앱과 동일한 방식)
     */
    suspend fun loadAllCategoryCounts() = withContext(Dispatchers.IO) {
        android.util.Log.d("ProteinDatabase", "🔍 모든 카테고리 실제 API 개수 로딩 시작...")
        
        val counts = mutableMapOf<ProteinCategory, Int>()
        
        for (category in ProteinCategory.values()) {
            try {
                // 각 카테고리에서 실제 API 데이터 개수 확인 (빠른 검색)
                val (_, totalCount) = apiService.searchProteinsByCategory(category, limit = 100)
                counts[category] = totalCount
                android.util.Log.d("ProteinDatabase", "✅ ${category.displayName}: 실제 ${totalCount}개 단백질 확인")
                
                // API 부하 방지를 위한 짧은 지연
                kotlinx.coroutines.delay(200) // 0.2초
                
            } catch (e: Exception) {
                android.util.Log.e("ProteinDatabase", "❌ ${category.displayName} 개수 확인 실패: ${e.message}")
                // 실패 시 샘플 데이터 개수 사용
                val sampleCount = apiService.getSampleProteins(category).size
                counts[category] = sampleCount
            }
        }
        
        _categoryTotalCounts.value = counts
        
        android.util.Log.d("ProteinDatabase", "🎉 모든 카테고리 개수 로드 완료!")
        for ((category, count) in counts) {
            android.util.Log.d("ProteinDatabase", "📊 ${category.displayName}: ${count}개")
        }
    }
    
    /**
     * 특정 카테고리의 단백질 로드
     */
    suspend fun loadProteins(category: ProteinCategory, refresh: Boolean = false) = withContext(Dispatchers.IO) {
        _isLoading.value = true
        _errorMessage.value = null
        
        try {
            android.util.Log.d("ProteinDatabase", "🔍 ${category.displayName} 카테고리 실제 데이터 로딩 시작...")
            
            // 기존 샘플 데이터 제거
            if (refresh) {
                val filteredProteins = _proteins.value.filter { it.category != category }
                _proteins.value = filteredProteins
                categoryPages[category] = 0
                categoryHasMore[category] = true
            }
            
            // 첫 페이지 API 데이터 로드
            val currentPage = categoryPages[category] ?: 0
            val skip = currentPage * itemsPerPage
            val limit = itemsPerPage
            
            android.util.Log.d("ProteinDatabase", "📡 API 호출: skip=$skip, limit=$limit")
            val newProteins = apiService.searchProteinsByCategory(category, limit, skip).first
            
            if (newProteins.isNotEmpty()) {
                // PDB ID를 ProteinInfo로 변환 (간단한 변환)
                val proteinInfos = newProteins.map { pdbId ->
                    ProteinInfo.createSample(
                        id = pdbId,
                        name = pdbId,
                        category = category,
                        description = "PDB ID: $pdbId",
                        keywords = emptyList()
                    )
                }
                
                val currentProteins = _proteins.value.toMutableList()
                currentProteins.addAll(proteinInfos)
                _proteins.value = currentProteins
                
                categoryPages[category] = currentPage + 1
                categoryHasMore[category] = newProteins.size >= limit
                loadedCategories.add(category)
                
                // 실제 API 데이터로 표시
                val currentSources = _categoryDataSource.value.toMutableMap()
                currentSources[category] = DataSource.API
                _categoryDataSource.value = currentSources
                
                android.util.Log.d("ProteinDatabase", "✅ ${category.displayName}: ${proteinInfos.size}개 실제 단백질 로드 완료")
            } else {
                android.util.Log.w("ProteinDatabase", "⚠️ ${category.displayName} 실제 데이터 없음, 샘플 데이터 유지")
                // 샘플 데이터 복원
                val sampleProteins = apiService.getSampleProteins(category)
                val currentProteins = _proteins.value.toMutableList()
                currentProteins.addAll(sampleProteins)
                _proteins.value = currentProteins
                categoryHasMore[category] = true
                
                // 샘플 데이터로 표시
                val currentSources = _categoryDataSource.value.toMutableMap()
                currentSources[category] = DataSource.SAMPLE
                _categoryDataSource.value = currentSources
            }
            
        } catch (e: Exception) {
            android.util.Log.e("ProteinDatabase", "❌ ${category.displayName} 로딩 실패: ${e.message}")
            
            // API 실패 시 샘플 데이터 사용
            val sampleProteins = apiService.getSampleProteins(category)
            val currentProteins = _proteins.value.toMutableList()
            currentProteins.addAll(sampleProteins)
            _proteins.value = currentProteins
            _errorMessage.value = "Using sample data for ${category.displayName} (API error: ${e.message})"
            categoryHasMore[category] = true
            
            // 샘플 데이터로 표시
            val currentSources = _categoryDataSource.value.toMutableMap()
            currentSources[category] = DataSource.SAMPLE
            _categoryDataSource.value = currentSources
        }
        
        _isLoading.value = false
    }
    
    /**
     * 특정 카테고리에 더 로드할 수 있는지 확인
     */
    fun hasMoreProteins(category: ProteinCategory): Boolean {
        val hasMoreFromState = categoryHasMore[category] ?: true
        val currentlyLoaded = _proteins.value.count { it.category == category }
        val totalAvailable = _categoryTotalCounts.value[category] ?: 0
        
        android.util.Log.d("ProteinDatabase", "🔍 ${category.displayName} hasMoreProteins 체크:")
        android.util.Log.d("ProteinDatabase", "   - categoryHasMore[${category.displayName}]: $hasMoreFromState")
        android.util.Log.d("ProteinDatabase", "   - 현재 로드된 개수: $currentlyLoaded")
        android.util.Log.d("ProteinDatabase", "   - 전체 사용 가능: $totalAvailable")
        
        // 샘플 데이터만 있는 경우 (보통 3-6개): API에서 더 많은 데이터가 있을 가능성이 높음
        if (currentlyLoaded <= 10 && totalAvailable > currentlyLoaded) {
            android.util.Log.d("ProteinDatabase", "   - 샘플 데이터 수준, API에서 더 로드 가능")
            return true
        }
        
        // 상태가 true이고, 현재 로드된 개수가 전체보다 적은 경우에만 true
        val result = hasMoreFromState && currentlyLoaded < totalAvailable
        android.util.Log.d("ProteinDatabase", "   - 최종 결과: $result")
        
        return result
    }
    
    /**
     * 카테고리별 단백질 필터링
     */
    fun proteinsByCategory(category: ProteinCategory): List<ProteinInfo> {
        return _proteins.value.filter { it.category == category }
    }
    
    /**
     * 검색 기능
     */
    fun searchProteins(query: String): List<ProteinInfo> {
        if (query.isEmpty()) {
            return _proteins.value
        }
        
        val lowercasedQuery = query.lowercase()
        return _proteins.value.filter { protein ->
            protein.name.lowercase().contains(lowercasedQuery) ||
            protein.id.lowercase().contains(lowercasedQuery) ||
            protein.description.lowercase().contains(lowercasedQuery) ||
            protein.keywords.any { keyword -> keyword.lowercase().contains(lowercasedQuery) }
        }
    }
    
    /**
     * 즐겨찾기 토글
     */
    fun toggleFavorite(proteinId: String) {
        val currentFavorites = _favorites.value.toMutableSet()
        if (currentFavorites.contains(proteinId)) {
            currentFavorites.remove(proteinId)
        } else {
            currentFavorites.add(proteinId)
        }
        _favorites.value = currentFavorites
    }
    
    /**
     * 에러 메시지 클리어
     */
    fun clearError() {
        _errorMessage.value = null
    }
}
