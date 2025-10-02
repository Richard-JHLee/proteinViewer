package com.avas.proteinviewer.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.hilt.navigation.compose.hiltViewModel
import com.avas.proteinviewer.viewmodel.ProteinViewModel
import com.avas.proteinviewer.ui.protein.ProteinInfoBottomSheet
import android.util.Log

// iOS 스타일 Protein Library Screen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onNavigateBack: () -> Unit,
    viewModel: ProteinViewModel = hiltViewModel()
) {
    Log.d("LibraryScreen", "LibraryScreen composable called")
    var searchText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<ProteinCategory?>(null) }
    var showingFavoritesOnly by remember { mutableStateOf(false) }
    var selectedProtein by remember { mutableStateOf<ProteinInfo?>(null) }
    var isProteinLoading by remember { mutableStateOf(false) }
    var proteinLoadingProgress by remember { mutableStateOf("") }
    var showingInfoSheet by remember { mutableStateOf(false) }
    var showingLoadingPopup by remember { mutableStateOf(false) }

    // ViewModel에서 실제 API 데이터 가져오기
    val categoryProteinCounts by viewModel.categoryProteinCounts.collectAsState()
    val categoryProteins by viewModel.categoryProteins.collectAsState()
    val isLoadingCategoryCounts by viewModel.isLoadingCategoryCounts.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    
    // iOS와 동일: 실제 API 데이터 사용
    val proteinCategories = getProteinCategories()
    val allFilteredProteins = if (searchText.isNotEmpty()) {
        // 검색 중일 때는 빈 리스트 (SearchResultsView에서 searchResults 사용)
        emptyList()
    } else if (selectedCategory != null) {
        // 선택된 카테고리의 실제 API 데이터 사용
        val apiProteins = categoryProteins[selectedCategory?.name] ?: emptyList()
        Log.d("LibraryScreen", "Selected category: ${selectedCategory?.name}, API proteins count: ${apiProteins.size}")
        if (apiProteins.isNotEmpty()) {
            Log.d("LibraryScreen", "Using API data: ${apiProteins.take(3).map { "${it.pdbId}: ${it.name}" }}")
            apiProteins
        } else {
            Log.d("LibraryScreen", "API data empty, falling back to sample data")
            // API 데이터가 없으면 샘플 데이터 사용
            getFilteredProteins(proteinCategories, searchText, selectedCategory, showingFavoritesOnly)
        }
    } else {
        // 전체 데이터
        getFilteredProteins(proteinCategories, searchText, selectedCategory, showingFavoritesOnly)
    }
    
    // 로딩 상태 변화 감지하여 팝업 제어
    LaunchedEffect(isLoadingCategoryCounts) {
        showingLoadingPopup = isLoadingCategoryCounts
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Protein Library") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Bar with Load Data Button
            SearchBarWithLoadData(
                searchText = searchText,
                onSearchTextChange = { searchText = it },
                onLoadData = {
                    // Load Data 버튼 클릭 시 아이폰과 동일한 동작
                    // showingLoadingPopup은 LaunchedEffect에서 자동으로 제어됨
                    viewModel.refreshAllCategoryCounts()
                },
                onPDBIdSearch = { pdbId ->
                    // PDB ID Search 버튼 클릭 시
                    android.util.Log.d("LibraryScreen", "PDB ID Search button clicked with: $pdbId")
                    viewModel.searchProteinByPDBId(pdbId)
                }
            )

            // Search Results Header (iOS와 동일한 동적 헤더)
            if (searchText.isNotEmpty()) {
                SearchResultsHeader(
                    searchText = searchText,
                    resultCount = searchResults.size,
                    onClearSearch = { searchText = "" }
                )
            }

            // Category Chips (검색 중일 때는 숨김 - iOS와 동일)
            if (searchText.isEmpty()) {
                CategoryChipsRow(
                    categories = proteinCategories,
                    selectedCategory = selectedCategory,
                    onCategorySelect = { category ->
                        selectedCategory = category
                        // ViewModel에 선택된 카테고리 알림 및 실제 단백질 데이터 로드
                        if (category != null) {
                            viewModel.selectCategory(category.displayName)
                            viewModel.loadCategoryProteins(category.displayName)
                        }
                    },
                    showingFavoritesOnly = showingFavoritesOnly,
                    onFavoritesToggle = { showingFavoritesOnly = it }
                )
                
                // iOS와 동일한 단백질 개수 설명 (패딩 줄임)
                if (selectedCategory == null) {
                    val totalCount = categoryProteinCounts.values.sum()
                    Text(
                        text = "Showing ${allFilteredProteins.size} of $totalCount proteins",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF666666),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                } else {
                    val categoryCount = categoryProteinCounts[selectedCategory?.displayName] ?: 0
                    val displayedCount = allFilteredProteins.size
                    Text(
                        text = "Showing $displayedCount of $categoryCount ${selectedCategory?.displayName?.lowercase()} proteins",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF666666),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            // Content Area
            Box(modifier = Modifier.weight(1f)) {
                when {
                    searchText.isNotEmpty() -> {
                // 검색 결과 표시
                SearchResultsView(
                            searchResults = searchResults,
                    onProteinClick = { protein ->
                        selectedProtein = protein
                        showingInfoSheet = true
                    }
                )
                    }
                    selectedCategory != null -> {
                        // 선택된 카테고리의 단백질 표시
                    SingleCategoryView(
                        category = selectedCategory!!,
                            proteins = allFilteredProteins,
                            isLoading = isLoadingCategoryCounts,
                            isLoadingMore = isLoadingMore,
                            hasMore = true, // API에서 더 많은 데이터가 있는지 확인
                        onProteinClick = { protein ->
                            selectedProtein = protein
                            showingInfoSheet = true
                        },
                            onLoadMore = {
                                // 더 많은 단백질 로드
                                viewModel.loadMoreCategoryProteins(selectedCategory!!.displayName)
                            }
                        )
                    }
                    else -> {
                        // 전체 단백질 표시 (그리드 레이아웃)
                        AllProteinsGridView(
                            proteins = allFilteredProteins,
                            onProteinClick = { protein ->
                                selectedProtein = protein
                                showingInfoSheet = true
                            }
                        )
                    }
                }
            }
        }
    }

    // Loading Popup (iOS와 동일한 스타일)
    if (showingLoadingPopup) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.padding(32.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Loading protein data...",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }

    // Protein Info Sheet (iOS와 동일한 모달)
    if (showingInfoSheet && selectedProtein != null) {
        ProteinInfoBottomSheet(
            protein = selectedProtein!!,
            onDismiss = { 
                showingInfoSheet = false
                selectedProtein = null
            },
            onView3D = {
                // 3D 뷰어로 이동
                showingInfoSheet = false
                selectedProtein = null
                // TODO: 3D 뷰어로 이동하는 로직 추가
            }
        )
    }
}

// iOS 스타일 카테고리 칩들
@Composable
private fun CategoryChipsRow(
    categories: List<ProteinCategory>,
    selectedCategory: ProteinCategory?,
    onCategorySelect: (ProteinCategory?) -> Unit,
    showingFavoritesOnly: Boolean,
    onFavoritesToggle: (Boolean) -> Unit
) {
        LazyRow(
        modifier = Modifier.padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
        // All 카테고리
            item {
                CategoryChip(
                text = "All",
                    isSelected = selectedCategory == null,
                onClick = { onCategorySelect(null) },
                count = null // 전체 개수는 별도로 표시
                )
            }
            
        // Favorites 토글
        item {
                CategoryChip(
                text = "Favorites",
                isSelected = showingFavoritesOnly,
                onClick = { onFavoritesToggle(!showingFavoritesOnly) },
                count = null,
                icon = Icons.Default.Favorite
            )
        }
        
        // 각 카테고리
        items(categories) { category ->
            CategoryChip(
                text = category.displayName,
                isSelected = selectedCategory == category,
                onClick = { onCategorySelect(category) },
                count = null, // 실제 카운트는 ViewModel에서 가져옴
                icon = getCategoryIcon(category.displayName),
                color = getCategoryColor(category.displayName)
            )
        }
    }
}

// iOS 스타일 카테고리 칩
@Composable
private fun CategoryChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    count: Int?,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) color else MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp, 
            if (isSelected) color else MaterialTheme.colorScheme.outline
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            icon?.let {
            Icon(
                    imageVector = it,
                contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (isSelected) Color.White else color
            )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
            count?.let {
                Text(
                    text = "($it)",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

// 단일 카테고리 뷰 (iOS와 동일)
@Composable
private fun SingleCategoryView(
    category: ProteinCategory,
    proteins: List<ProteinInfo>,
    isLoading: Boolean,
    isLoadingMore: Boolean,
    hasMore: Boolean,
    onProteinClick: (ProteinInfo) -> Unit,
    onLoadMore: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
        items(proteins) { protein ->
            ProteinRowCard(
                protein = protein,
                onClick = { onProteinClick(protein) }
            )
        }
        
        // Load More 버튼
        if (hasMore) {
            item {
                LoadMoreButton(
                    isLoading = isLoadingMore,
                    onClick = onLoadMore
                )
            }
        }
    }
}

// 단백질 행 카드 (iOS와 동일한 스타일)
@Composable
private fun ProteinRowCard(
    protein: ProteinInfo,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
                modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 카테고리 아이콘
            val category = determineCategoryFromProtein(protein)
                Icon(
                imageVector = getCategoryIcon(category),
                    contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        getCategoryColor(category).copy(alpha = 0.1f),
                        CircleShape
                    )
                    .padding(8.dp),
                tint = getCategoryColor(category)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // 단백질 정보
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = protein.pdbId,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = protein.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = protein.organism ?: "Unknown organism",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 해상도 정보
            protein.resolution?.let { resolution ->
            Text(
                    text = "${String.format("%.2f", resolution)} Å",
                style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            }
        }
    }
}

// Load More 버튼
@Composable
private fun LoadMoreButton(
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        enabled = !isLoading
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text("Load More Proteins")
    }
}

// 검색 결과 뷰
@Composable
private fun SearchResultsView(
    searchResults: List<ProteinInfo>,
    onProteinClick: (ProteinInfo) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(searchResults) { protein ->
            ProteinRowCard(
                protein = protein,
                onClick = { onProteinClick(protein) }
            )
        }
    }
}

// 전체 단백질 그리드 뷰
@Composable
private fun AllProteinsGridView(
    proteins: List<ProteinInfo>,
    onProteinClick: (ProteinInfo) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(proteins) { protein ->
            ProteinGridCard(
                protein = protein,
                onClick = { onProteinClick(protein) }
            )
        }
    }
}

// 단백질 그리드 카드
@Composable
private fun ProteinGridCard(
    protein: ProteinInfo,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val category = determineCategoryFromProtein(protein)
            Icon(
                imageVector = getCategoryIcon(category),
                contentDescription = null,
                    modifier = Modifier
                    .size(32.dp)
                        .background(
                        getCategoryColor(category).copy(alpha = 0.1f),
                        CircleShape
                    )
                    .padding(6.dp),
                tint = getCategoryColor(category)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = protein.pdbId,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = protein.name,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

// 검색 바와 로드 데이터 버튼
@Composable
private fun SearchBarWithLoadData(
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    onLoadData: () -> Unit,
    onPDBIdSearch: (String) -> Unit
) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = searchText,
            onValueChange = onSearchTextChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search proteins...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchText.isNotEmpty()) {
                    IconButton(onClick = { onSearchTextChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onLoadData,
                modifier = Modifier.weight(1f)
            ) {
                Text("Load Data")
            }
            
            Button(
                onClick = { onPDBIdSearch(searchText) },
                modifier = Modifier.weight(1f),
                enabled = searchText.isNotEmpty()
            ) {
                Text("Search PDB")
            }
        }
    }
}

// 검색 결과 헤더
@Composable
private fun SearchResultsHeader(
    searchText: String,
    resultCount: Int,
    onClearSearch: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
            text = "Search results for \"$searchText\" ($resultCount found)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        
        TextButton(onClick = onClearSearch) {
            Text("Clear")
        }
    }
}

// 단백질 카테고리 enum
enum class ProteinCategory(val displayName: String) {
    ENZYMES("Enzymes"),
    ANTIBODIES("Antibodies"),
    MEMBRANE_PROTEINS("Membrane Proteins"),
    TRANSPORTERS("Transporters"),
    HORMONES("Hormones"),
    STORAGE_PROTEINS("Storage Proteins"),
    RECEPTORS("Receptors"),
    MOTOR_PROTEINS("Motor Proteins"),
    SIGNALING_PROTEINS("Signaling Proteins"),
    CHAPERONES("Chaperones"),
    METABOLIC_PROTEINS("Metabolic Proteins"),
    STRUCTURAL_PROTEINS("Structural Proteins")
}

// 단백질 정보 데이터 클래스
data class ProteinInfo(
    val pdbId: String,
    val name: String,
    val organism: String?,
    val resolution: Double?,
    val experimentalMethod: String?,
    val molecularWeight: Double?,
    val description: String? = null
)

// 카테고리별 색상
private fun getCategoryColor(category: String): Color {
    return when (category) {
        "Enzymes" -> Color(0xFF4CAF50)
        "Antibodies" -> Color(0xFF2196F3)
        "Membrane Proteins" -> Color(0xFF9C27B0)
        "Transporters" -> Color(0xFFFF9800)
        "Hormones" -> Color(0xFFE91E63)
        "Storage Proteins" -> Color(0xFF795548)
        "Receptors" -> Color(0xFF607D8B)
        "Motor Proteins" -> Color(0xFF3F51B5)
        "Signaling Proteins" -> Color(0xFF00BCD4)
        "Chaperones" -> Color(0xFF8BC34A)
        "Metabolic Proteins" -> Color(0xFFFFC107)
        "Structural Proteins" -> Color(0xFF9E9E9E)
        else -> Color(0xFF757575)
    }
}

// 카테고리별 아이콘
private fun getCategoryIcon(category: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (category) {
        "Enzymes" -> Icons.Default.Science
        "Antibodies" -> Icons.Default.MedicalServices
        "Membrane Proteins" -> Icons.Default.CellTower
        "Transporters" -> Icons.Default.LocalShipping
        "Hormones" -> Icons.Default.Favorite
        "Storage Proteins" -> Icons.Default.Storage
        "Receptors" -> Icons.Default.Radio
        "Motor Proteins" -> Icons.Default.DirectionsRun
        "Signaling Proteins" -> Icons.Default.SignalCellularAlt
        "Chaperones" -> Icons.Default.Support
        "Metabolic Proteins" -> Icons.Default.Timeline
        "Structural Proteins" -> Icons.Default.Architecture
        else -> Icons.Default.Category
    }
}

// 단백질에서 카테고리 결정
private fun determineCategoryFromProtein(protein: ProteinInfo): String {
    val name = protein.name.lowercase()
    val description = protein.description?.lowercase() ?: ""
    val organism = protein.organism?.lowercase() ?: ""
    
    val text = "$name $description $organism"
    
    return when {
        text.contains("enzyme") || text.contains("kinase") || text.contains("phosphatase") -> "Enzymes"
        text.contains("antibody") || text.contains("immunoglobulin") -> "Antibodies"
        text.contains("membrane") || text.contains("transmembrane") -> "Membrane Proteins"
        text.contains("transporter") || text.contains("channel") -> "Transporters"
        text.contains("hormone") || text.contains("insulin") -> "Hormones"
        text.contains("storage") || text.contains("ferritin") -> "Storage Proteins"
        text.contains("receptor") || text.contains("binding") -> "Receptors"
        text.contains("motor") || text.contains("myosin") || text.contains("kinesin") -> "Motor Proteins"
        text.contains("signaling") || text.contains("signal") -> "Signaling Proteins"
        text.contains("chaperone") || text.contains("heat shock") -> "Chaperones"
        text.contains("metabolic") || text.contains("metabolism") -> "Metabolic Proteins"
        text.contains("structural") || text.contains("collagen") -> "Structural Proteins"
        else -> "Structural Proteins"
    }
}

// 카테고리 목록 가져오기
private fun getProteinCategories(): List<ProteinCategory> {
    return ProteinCategory.values().toList()
}

// 필터링된 단백질 목록 가져오기 (샘플 데이터)
private fun getFilteredProteins(
    categories: List<ProteinCategory>,
    searchText: String,
    selectedCategory: ProteinCategory?,
    showingFavoritesOnly: Boolean
): List<ProteinInfo> {
    // 샘플 데이터 반환 (실제로는 ViewModel에서 가져옴)
    return listOf(
        ProteinInfo("1CRN", "Crambin", "Crambe abyssinica", 0.54, "X-ray", 4720.0, "Plant seed protein"),
        ProteinInfo("1HHO", "Hemoglobin", "Homo sapiens", 1.74, "X-ray", 64458.0, "Oxygen transport protein"),
        ProteinInfo("1INS", "Insulin", "Homo sapiens", 1.5, "X-ray", 5808.0, "Hormone regulating glucose metabolism")
    )
}