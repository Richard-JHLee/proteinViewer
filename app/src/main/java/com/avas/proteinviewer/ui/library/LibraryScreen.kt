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
import android.util.Log

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
                            viewModel.selectCategory(category.name)
                            viewModel.loadCategoryProteins(category.name)
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
                    val category = selectedCategory
                    val categoryTotal = categoryProteinCounts[category?.name] ?: 0
                    val displayedCount = allFilteredProteins.size
                    Text(
                        text = "Showing $displayedCount of $categoryTotal proteins",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF666666),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            // Category Details Section 제거 - SingleCategoryView에서 처리하여 중복 방지

            // Main Content
            if (searchText.isNotEmpty()) {
                // 검색 결과 표시
                SearchResultsView(
                    proteins = searchResults,
                    proteinCategories = proteinCategories,
                    onProteinClick = { protein ->
                        selectedProtein = protein
                        isProteinLoading = true
                        proteinLoadingProgress = "Loading ${protein.pdbId}..."
                        showingInfoSheet = true
                    }
                )
            } else if (selectedCategory == null) {
                // All Categories 화면: 전체 카테고리 합계 표시
                val totalCount = categoryProteinCounts.values.sum()
                Text(
                    text = "Total: $totalCount proteins across all categories",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Content
            if (selectedCategory == null) {
                // All Categories - 카테고리 선택 인터페이스
                CategorySelectionView(
                    categories = proteinCategories,
                    categoryProteinCounts = categoryProteinCounts,
                    onCategorySelect = { category ->
                        selectedCategory = category
                        // ViewModel에 선택된 카테고리 알림 및 실제 단백질 데이터 로드
                        if (category != null) {
                            viewModel.selectCategory(category.name)
                            viewModel.loadCategoryProteins(category.name)
                        }
                    }
                )
            } else {
                // Single Category - 단일 카테고리 리스트 (실제 API 데이터 사용)
                val actualProteins = categoryProteins[selectedCategory!!.name] ?: emptyList()
                SingleCategoryView(
                    category = selectedCategory!!,
                    proteins = actualProteins,
                    proteinCategories = proteinCategories,
                    onBackClick = { selectedCategory = null },
                    onProteinClick = { protein ->
                        selectedProtein = protein
                        isProteinLoading = true
                        proteinLoadingProgress = "Loading ${protein.pdbId}..."
                        showingInfoSheet = true
                    },
                    isLoadingMore = isLoadingMore,
                    onLoadMore = { viewModel.loadMoreProteins() },
                    shouldShowLoadMore = viewModel.shouldShowLoadMore()
                )
            }
        }
    }

    // Loading Overlay - 아이폰과 동일한 스타일
    if (showingLoadingPopup || isProteinLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = Color(0xFF2196F3) // 아이폰과 동일한 파란색
                    )
                    Text(
                        text = if (isProteinLoading) "Loading protein..." else "Loading proteins...",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black
                    )
                    Text(
                        text = if (isProteinLoading) proteinLoadingProgress else "Please wait",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchResultsHeader(
    searchText: String,
    resultCount: Int,
    onClearSearch: () -> Unit
) {
    val trimmed = searchText.trim()
    val isPDBID = trimmed.length == 4 && trimmed.all { it.isLetterOrDigit() }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 검색 결과 텍스트
        Text(
            text = when {
                resultCount == 0 -> {
                    if (isPDBID) "PDB ID '${trimmed.uppercase()}'를 찾을 수 없습니다"
                    else "'$searchText' 검색 결과가 없습니다"
                }
                else -> {
                    if (isPDBID) "PDB ID '${trimmed.uppercase()}' 검색 결과: ${resultCount}개"
                    else "'$searchText' 검색 결과: ${resultCount}개"
                }
            },
            style = MaterialTheme.typography.bodySmall,
            color = if (resultCount == 0) Color(0xFFFF9800) else MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        // 전체 보기 버튼
        TextButton(
            onClick = onClearSearch,
            colors = ButtonDefaults.textButtonColors(
                contentColor = Color(0xFF2196F3)
            )
        ) {
            Text(
                text = "전체 보기",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun SearchBarWithLoadData(
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    onLoadData: () -> Unit,
    onPDBIdSearch: (String) -> Unit
) {
    // 아이폰과 동일한 버튼 정보 계산
    val buttonInfo = remember(searchText) {
        val trimmed = searchText.trim()
        when {
            trimmed.length == 4 && trimmed.all { it.isLetterOrDigit() } -> {
                Triple("PDB ID Search", Color(0xFF9C27B0), Icons.Default.Search) // Purple
            }
            trimmed.length >= 2 -> {
                Triple("Text Search", Color(0xFF4CAF50), Icons.Default.Search) // Green
            }
            else -> {
                Triple("Load Data", Color(0xFF2196F3), Icons.Default.Refresh) // Blue
            }
        }
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Search Input Field - Material Design 표준 높이
        Card(
            modifier = Modifier
                .weight(1f)
                .height(48.dp), // Material Design 표준 높이
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF2F2F7) // iOS systemGray6 equivalent
            ),
            shape = RoundedCornerShape(24.dp) // 더 둥근 모서리로 Material Design 스타일
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp) // 아이콘 크기 약간 축소
                )
                Spacer(modifier = Modifier.width(8.dp))
                TextField(
                    value = searchText,
                    onValueChange = onSearchTextChange,
                    placeholder = { 
                        Text(
                            "Search proteins...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp), // TextField 높이 명시적 설정
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium
                )
                if (searchText.isNotEmpty()) {
                    IconButton(
                        onClick = { onSearchTextChange("") },
                        modifier = Modifier.size(24.dp) // IconButton 크기 축소
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
        
        // Load Data / PDB ID Search Button - Material Design 표준 높이
        Button(
            onClick = {
                val trimmed = searchText.trim()
                android.util.Log.d("LibraryScreen", "Button clicked with searchText: '$searchText', trimmed: '$trimmed'")
                when {
                    trimmed.length == 4 && trimmed.all { it.isLetterOrDigit() } -> {
                        android.util.Log.d("LibraryScreen", "Calling onPDBIdSearch with: $trimmed")
                        onPDBIdSearch(trimmed)
                    }
                    else -> {
                        android.util.Log.d("LibraryScreen", "Calling onLoadData")
                        onLoadData()
                    }
                }
            },
            modifier = Modifier.height(48.dp), // 검색 입력란과 동일한 높이
            colors = ButtonDefaults.buttonColors(
                containerColor = buttonInfo.second.copy(alpha = 0.1f)
            ),
            shape = RoundedCornerShape(24.dp), // 더 둥근 모서리
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    buttonInfo.third,
                    contentDescription = null,
                    tint = buttonInfo.second,
                    modifier = Modifier.size(18.dp) // 아이콘 크기 증가
                )
                Text(
                    text = buttonInfo.first,
                    color = buttonInfo.second,
                    style = MaterialTheme.typography.bodyMedium // 텍스트 스타일 개선
                )
            }
        }
    }
}

@Composable
private fun CategoryChipsRow(
    categories: List<ProteinCategory>,
    selectedCategory: ProteinCategory?,
    onCategorySelect: (ProteinCategory?) -> Unit,
    showingFavoritesOnly: Boolean,
    onFavoritesToggle: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        // Categories Header
        Text(
            text = "Categories",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // All Categories Chip
            item {
                CategoryChip(
                    title = "All Categories",
                    icon = Icons.Default.GridView,
                    color = Color(0xFF2196F3), // Blue
                    isSelected = selectedCategory == null,
                    onClick = { onCategorySelect(null) }
                )
            }
            
            // Category Chips
            items(categories) { category ->
                CategoryChip(
                    title = category.name,
                    icon = category.icon,
                    color = category.color,
                    isSelected = selectedCategory == category,
                    onClick = { onCategorySelect(category) }
                )
            }
            
            // Favorites Chip
            item {
                CategoryChip(
                    title = "♥",
                    icon = Icons.Default.Favorite,
                    color = Color(0xFFE91E63), // Pink
                    isSelected = showingFavoritesOnly,
                    onClick = { onFavoritesToggle(!showingFavoritesOnly) }
                )
            }
        }
    }
}

@Composable
private fun CategoryChip(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) color.copy(alpha = 0.2f) else Color(0xFFF2F2F7) // iOS와 동일한 선택 상태
        ),
        shape = RoundedCornerShape(25.dp), // iOS와 동일한 더 둥근 모서리
        elevation = if (isSelected) CardDefaults.cardElevation(defaultElevation = 4.dp) else CardDefaults.cardElevation(defaultElevation = 0.dp) // iOS와 동일한 선택 시 그림자
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp), // iOS와 동일한 패딩
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp) // iOS와 동일한 간격
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp), // iOS와 동일한 아이콘 크기
                tint = if (isSelected) color else color.copy(alpha = 0.7f)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSelected) color else color.copy(alpha = 0.8f),
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium // iOS와 동일한 폰트 굵기
            )
        }
    }
}


@Composable
private fun CategorySelectionView(
    categories: List<ProteinCategory>,
    categoryProteinCounts: Map<String, Int>,
    onCategorySelect: (ProteinCategory) -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    
    // 디바이스 크기에 따라 동적으로 높이 계산
    // 전체 화면 높이에서 다른 요소들을 제외한 공간 계산
    // 상단바(56dp) + 검색바(48dp) + 카테고리칩(60dp) + 헤더(100dp) + 여백(32dp) = 약 296dp
    val reservedSpace = 296.dp
    val availableHeight = maxOf(screenHeight - reservedSpace, 300.dp) // 최소 300dp 보장
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Choose a Category",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Explore proteins by their biological function",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        // Category Grid - 2열 그리드 (디바이스 크기에 맞춰 동적 조정)
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = availableHeight),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(categories.size) { index ->
                val category = categories[index]
                CategorySelectionCard(
                    category = category,
                    proteinCount = categoryProteinCounts[category.name] ?: 0,
                    onSelect = { onCategorySelect(category) }
                )
            }
        }
    }
}

@Composable
private fun CategorySelectionCard(
    category: ProteinCategory,
    proteinCount: Int,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF2F2F7) // iOS systemGray6 equivalent
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(category.color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    category.icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = category.color
                )
            }

            // Category Info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "${proteinCount} proteins",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Description
            Text(
                text = category.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun SingleCategoryView(
    category: ProteinCategory,
    proteins: List<ProteinInfo>,
    proteinCategories: List<ProteinCategory>,
    onBackClick: () -> Unit,
    onProteinClick: (ProteinInfo) -> Unit,
    isLoadingMore: Boolean,
    onLoadMore: () -> Unit,
    shouldShowLoadMore: Boolean
) {
    Log.d("SingleCategoryView", "Rendering SingleCategoryView for ${category.name} with ${proteins.size} proteins")
    
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // Selected Category Header
        item {
            SelectedCategoryHeader(
                category = category,
                proteinCount = proteins.size,
                onBackClick = onBackClick
            )
        }
        
        // Protein List or Empty State
        if (proteins.isEmpty()) {
            item {
                // Empty State - 데이터가 로드되지 않았을 때
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Science,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = Color(0xFF9E9E9E)
                        )
                        Text(
                            text = "No proteins found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF666666)
                        )
                        Text(
                            text = "Try refreshing or check your connection",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF999999),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            // Protein List with spacing
            items(proteins) { protein ->
                Log.d("SingleCategoryView", "Rendering protein: ${protein.pdbId}")
                Column(
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    ProteinRowCard(
                        protein = protein,
                        categories = proteinCategories,
                        onProteinClick = onProteinClick
                    )
                }
            }
            
            // Load More 버튼 (iOS와 동일한 기능)
            item {
                LoadMoreButton(
                    isLoading = isLoadingMore,
                    onLoadMore = onLoadMore,
                    shouldShow = shouldShowLoadMore
                )
            }
        }
    }
}

@Composable
private fun SelectedCategoryHeader(
    category: ProteinCategory,
    proteinCount: Int,
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // Back Button - iOS와 동일한 스타일
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onBackClick,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color(0xFF007AFF)
                ),
                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFF007AFF)
                    )
                    Text(
                        text = "Categories",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF007AFF)
                    )
                }
            }
        }
        
        // Category Detail Card - iOS와 동일한 스타일
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFE3F2FD) // Light blue background
            ),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Category Icon
                Icon(
                    category.icon,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = Color(0xFF1976D2) // Blue color
                )
                
                // Category Info
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = "${proteinCount} proteins found",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF666666)
                    )
                }
            }
        }
        
        // Category Description - iOS와 동일하게 카드 밖에 위치
        Text(
            text = category.description,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF666666),
            modifier = Modifier.padding(horizontal = 0.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun SearchResultsView(
    proteins: List<ProteinInfo>,
    proteinCategories: List<ProteinCategory>,
    onProteinClick: (ProteinInfo) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        items(proteins) { protein ->
            ProteinRowCard(
                protein = protein,
                categories = proteinCategories,
                onProteinClick = onProteinClick
            )
        }
    }
}

@Composable
private fun ProteinRowCard(
    protein: ProteinInfo,
    categories: List<ProteinCategory>,
    onProteinClick: (ProteinInfo) -> Unit
) {
    val category = findCategoryByName(categories, protein.categoryName)
    var isFavorite by remember { mutableStateOf(false) } // 즐겨찾기 상태
    
    // iOS와 동일한 카드 스타일
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onProteinClick(protein) },
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF2F2F7) // iOS와 동일한 배경색
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp) // iOS와 동일한 패딩
        ) {
            // Header Row (iOS와 동일한 레이아웃)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // PDB ID Badge (iOS와 동일한 스타일)
                Text(
                    text = protein.pdbId.uppercase(),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = category?.color ?: Color.Gray,
                    modifier = Modifier
                        .background(
                            (category?.color ?: Color.Gray).copy(alpha = 0.1f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Favorite Button (iOS와 동일한 하트 아이콘)
                IconButton(
                    onClick = { isFavorite = !isFavorite },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                        modifier = Modifier.size(16.dp),
                        tint = if (isFavorite) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Protein Name (iOS와 동일한 긴 이름 스타일)
            Text(
                text = protein.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 20.sp
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Description (iOS와 동일한 상세 설명 스타일)
            Text(
                text = protein.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Category Badge (iOS와 동일한 카테고리 표시)
            if (category != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        category.icon,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = category.color
                    )
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = category.color,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// Data Classes
data class ProteinCategory(
    val name: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color,
    val description: String,
    val proteins: List<ProteinInfo>
)

data class ProteinInfo(
    val pdbId: String,
    val name: String,
    val description: String,
    val categoryName: String
)

// Load More Button (iOS와 동일한 디자인)
@Composable
private fun LoadMoreButton(
    isLoading: Boolean,
    onLoadMore: () -> Unit,
    shouldShow: Boolean
) {
    if (!shouldShow) return
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Button(
            onClick = onLoadMore,
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFF2F2F7)
            ),
            shape = RoundedCornerShape(25.dp),
            border = ButtonDefaults.outlinedButtonBorder.copy(
                brush = SolidColor(Color(0xFF007AFF).copy(alpha = 0.3f))
            ),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFF007AFF)
                    )
                } else {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color(0xFF007AFF)
                    )
                }
                
                Text(
                    text = if (isLoading) "Loading..." else "Load More",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF007AFF),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// Helper Functions
private fun findCategoryByName(categories: List<ProteinCategory>, categoryName: String): ProteinCategory? {
    return categories.find { it.name == categoryName }
}

private fun getProteinCategories(): List<ProteinCategory> {
    val enzymes = ProteinCategory(
        name = "Enzymes",
        icon = Icons.Default.Build, // scissors equivalent
        color = Color(0xFF2196F3), // Blue
        description = "Proteins that catalyze biochemical reactions",
        proteins = listOf(
            ProteinInfo("1LYZ", "Lysozyme", "항균 작용을 하는 효소, 눈물과 침에 존재", "Enzymes"),
            ProteinInfo("1TIM", "Triosephosphate Isomerase", "포도당 대사 경로의 핵심 효소", "Enzymes"),
            ProteinInfo("1AKE", "Adenylate Kinase", "세포 내 에너지 전달 효소", "Enzymes")
        )
    )
    
    val structural = ProteinCategory(
        name = "Structural",
        icon = Icons.Default.Apartment, // building.2 equivalent
        color = Color(0xFFFF9800), // Orange
        description = "Proteins that form cellular and tissue structures",
        proteins = listOf(
            ProteinInfo("1TUB", "Tubulin", "세포골격의 미세소관을 구성하는 단백질", "Structural"),
            ProteinInfo("1PGA", "Protein G", "면역글로불린 결합 단백질", "Structural")
        )
    )
    
    val defense = ProteinCategory(
        name = "Defense",
        icon = Icons.Default.Security, // shield equivalent
        color = Color(0xFFF44336), // Red
        description = "Proteins that protect the body from external threats",
        proteins = listOf(
            ProteinInfo("1IGY", "Immunoglobulin", "병원체를 인식하고 중화하는 항체 단백질", "Defense")
        )
    )
    
    val transport = ProteinCategory(
        name = "Transport",
        icon = Icons.Default.LocalShipping, // car equivalent
        color = Color(0xFF4CAF50), // Green
        description = "Proteins that transport substances",
        proteins = listOf(
            ProteinInfo("1HHO", "Hemoglobin", "적혈구의 산소 운반 단백질", "Transport"),
            ProteinInfo("1UBQ", "Ubiquitin", "단백질 분해 신호를 전달하는 마커", "Transport")
        )
    )
    
    val hormones = ProteinCategory(
        name = "Hormones",
        icon = Icons.Default.Radio, // antenna.radiowaves.left.and.right equivalent
        color = Color(0xFF9C27B0), // Purple
        description = "Proteins responsible for signal transmission",
        proteins = listOf(
            ProteinInfo("1INS", "Insulin", "혈당 조절 호르몬", "Hormones")
        )
    )
    
    val storage = ProteinCategory(
        name = "Storage",
        icon = Icons.Default.Archive, // archivebox equivalent
        color = Color(0xFF795548), // Brown
        description = "Proteins that store nutrients",
        proteins = listOf(
            ProteinInfo("1CRN", "Crotin", "식물 종자에 저장되는 영양 단백질", "Storage")
        )
    )
    
    val receptors = ProteinCategory(
        name = "Receptors",
        icon = Icons.Default.Wifi, // wifi equivalent
        color = Color(0xFF00BCD4), // Cyan
        description = "Receptor proteins that receive signals",
        proteins = listOf(
            ProteinInfo("1F88", "G-protein coupled receptor", "세포막을 통과하는 신호 수용체", "Receptors")
        )
    )
    
    val membrane = ProteinCategory(
        name = "Membrane",
        icon = Icons.Default.BubbleChart, // bubble.left.and.bubble.right equivalent
        color = Color(0xFF4DB6AC), // Mint
        description = "Proteins that compose and regulate cell membranes",
        proteins = listOf(
            ProteinInfo("1K4C", "Membrane protein", "세포막을 구성하고 조절하는 단백질", "Membrane")
        )
    )
    
    val motor = ProteinCategory(
        name = "Motor",
        icon = Icons.Default.Settings, // gear equivalent
        color = Color(0xFF3F51B5), // Indigo
        description = "Proteins that create movement within cells",
        proteins = listOf(
            ProteinInfo("1ATP", "ATP Synthase", "세포 내 에너지 생산을 담당하는 효소", "Motor")
        )
    )
    
    val signaling = ProteinCategory(
        name = "Signaling",
        icon = Icons.Default.Share, // network equivalent
        color = Color(0xFFE91E63), // Pink
        description = "Proteins that mediate intercellular communication",
        proteins = listOf(
            ProteinInfo("1GFL", "Signaling protein", "세포 간 신호 전달을 매개하는 단백질", "Signaling")
        )
    )
    
    val chaperones = ProteinCategory(
        name = "Chaperones",
        icon = Icons.Default.Build, // wrench.and.screwdriver equivalent
        color = Color(0xFFF57C00), // Dark Orange (더 어두운 주황색)
        description = "Proteins that assist in protein folding",
        proteins = listOf(
            ProteinInfo("1HRO", "HSP70", "단백질 접힘을 도와주는 분자 샤페론", "Chaperones")
        )
    )
    
    val metabolic = ProteinCategory(
        name = "Metabolic",
        icon = Icons.Default.Refresh, // arrow.triangle.2.circlepath equivalent
        color = Color(0xFF009688), // Teal
        description = "Proteins involved in metabolic processes",
        proteins = listOf(
            ProteinInfo("1TIM", "Metabolic enzyme", "대사 과정에 관여하는 효소", "Metabolic")
        )
    )
    
    return listOf(enzymes, structural, defense, transport, hormones, storage, receptors, membrane, motor, signaling, chaperones, metabolic)
}

private fun getFilteredProteins(
    categories: List<ProteinCategory>,
    searchText: String,
    selectedCategory: ProteinCategory?,
    showingFavoritesOnly: Boolean
): List<ProteinInfo> {
    var proteins = categories.flatMap { it.proteins }
    
    if (searchText.isNotEmpty()) {
        proteins = proteins.filter { protein ->
            protein.name.contains(searchText, ignoreCase = true) ||
            protein.description.contains(searchText, ignoreCase = true) ||
            protein.pdbId.contains(searchText, ignoreCase = true)
        }
    }
    
    if (selectedCategory != null) {
        proteins = proteins.filter { it.categoryName == selectedCategory.name }
    }
    
    return proteins
}

