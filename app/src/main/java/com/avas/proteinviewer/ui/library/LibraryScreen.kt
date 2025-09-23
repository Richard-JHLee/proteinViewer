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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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

    val proteinCategories = getProteinCategories()
    val allFilteredProteins = getFilteredProteins(proteinCategories, searchText, selectedCategory, showingFavoritesOnly)
    
    // ViewModel에서 실제 API 데이터 가져오기
    val categoryProteinCounts by viewModel.categoryProteinCounts.collectAsState()

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
            // Search Bar
            SearchBar(
                searchText = searchText,
                onSearchTextChange = { searchText = it },
                onSearch = {
                    // 검색 실행
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Category Chips
            if (searchText.isEmpty()) {
                CategoryChipsRow(
                    categories = proteinCategories,
                    selectedCategory = selectedCategory,
                    onCategorySelect = { category ->
                        selectedCategory = category
                        // ViewModel에 선택된 카테고리 알림
                        if (category != null) {
                            viewModel.selectCategory(category.name)
                        }
                    },
                    showingFavoritesOnly = showingFavoritesOnly,
                    onFavoritesToggle = { showingFavoritesOnly = it }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Results Count
            ResultsCountText(
                searchText = searchText,
                selectedCategory = selectedCategory,
                allFilteredProteins = allFilteredProteins,
                categoryProteinCounts = categoryProteinCounts
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Main Content
            if (searchText.isNotEmpty()) {
                // 검색 결과 표시
                SearchResultsView(
                    proteins = allFilteredProteins,
                    proteinCategories = proteinCategories,
                    onProteinClick = { protein ->
                        selectedProtein = protein
                        isProteinLoading = true
                        proteinLoadingProgress = "Loading ${protein.pdbId}..."
                        showingInfoSheet = true
                    }
                )
            } else if (selectedCategory == null) {
                // All Categories - 카테고리 선택 인터페이스
                CategorySelectionView(
                    categories = proteinCategories,
                    categoryProteinCounts = categoryProteinCounts,
                    onCategorySelect = { category ->
                        selectedCategory = category
                        // ViewModel에 선택된 카테고리 알림
                        if (category != null) {
                            viewModel.selectCategory(category.name)
                        }
                    }
                )
            } else {
                // Single Category - 단일 카테고리 리스트
                SingleCategoryView(
                    category = selectedCategory!!,
                    proteins = allFilteredProteins,
                    proteinCategories = proteinCategories,
                    onBackClick = { selectedCategory = null },
                    onProteinClick = { protein ->
                        selectedProtein = protein
                        isProteinLoading = true
                        proteinLoadingProgress = "Loading ${protein.pdbId}..."
                        showingInfoSheet = true
                    }
                )
            }
        }
    }

    // Loading Overlay
    if (isProteinLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = proteinLoadingProgress,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchBar(
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    onSearch: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF2F2F7) // iOS systemGray6 equivalent
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            TextField(
                value = searchText,
                onValueChange = onSearchTextChange,
                placeholder = { 
                    Text("Search proteins...")
                },
                modifier = Modifier.weight(1f),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                singleLine = true
            )
            if (searchText.isNotEmpty()) {
                IconButton(onClick = { onSearchTextChange("") }) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
            containerColor = if (isSelected) color.copy(alpha = 0.15f) else Color(0xFFF2F2F7)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = if (isSelected) color else color.copy(alpha = 0.7f)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSelected) color else color.copy(alpha = 0.8f),
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun ResultsCountText(
    searchText: String,
    selectedCategory: ProteinCategory?,
    allFilteredProteins: List<ProteinInfo>,
    categoryProteinCounts: Map<String, Int>
) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val countText = when {
            searchText.isNotEmpty() -> {
                "Search results: ${allFilteredProteins.size}"
            }
            selectedCategory == null -> {
                val totalCount = categoryProteinCounts.values.sum()
                "Total: ${totalCount} proteins across all categories"
            }
            else -> {
                val categoryTotal = categoryProteinCounts[selectedCategory.name] ?: 0
                "Showing ${allFilteredProteins.size} of ${categoryTotal} proteins"
            }
        }
        
        Text(
            text = countText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun CategorySelectionView(
    categories: List<ProteinCategory>,
    categoryProteinCounts: Map<String, Int>,
    onCategorySelect: (ProteinCategory) -> Unit
) {
    Column(
        modifier = Modifier.padding(16.dp),
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

        // Category Grid - 2열 그리드 (아이폰과 동일)
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 400.dp)
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
    onProteinClick: (ProteinInfo) -> Unit
) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Selected Category Header
        SelectedCategoryHeader(
            category = category,
            proteinCount = proteins.size,
            onBackClick = onBackClick
        )

        // Protein List
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
}

@Composable
private fun SelectedCategoryHeader(
    category: ProteinCategory,
    proteinCount: Int,
    onBackClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
        }
        
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = category.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${proteinCount} proteins",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SearchResultsView(
    proteins: List<ProteinInfo>,
    proteinCategories: List<ProteinCategory>,
    onProteinClick: (ProteinInfo) -> Unit
) {
    LazyColumn(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onProteinClick(protein) },
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF2F2F7) // iOS systemGray6 equivalent
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Protein Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background((category?.color ?: Color.Gray).copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    category?.icon ?: Icons.Default.Science,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = category?.color ?: Color.Gray
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Protein Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "${protein.pdbId} - ${protein.name}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = protein.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Play Button
            IconButton(onClick = { onProteinClick(protein) }) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Load",
                    tint = MaterialTheme.colorScheme.primary
                )
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
            ProteinInfo("1LYZ", "Lysozyme", "Antibacterial enzyme", "Enzymes"),
            ProteinInfo("1TIM", "Triosephosphate Isomerase", "Glycolytic enzyme", "Enzymes"),
            ProteinInfo("1AKE", "Adenylate Kinase", "Energy metabolism enzyme", "Enzymes")
        )
    )
    
    val structural = ProteinCategory(
        name = "Structural",
        icon = Icons.Default.Apartment, // building.2 equivalent
        color = Color(0xFFFF9800), // Orange
        description = "Proteins that form cellular and tissue structures",
        proteins = listOf(
            ProteinInfo("1TUB", "Tubulin", "Cytoskeletal protein", "Structural"),
            ProteinInfo("1PGA", "Protein G", "Immunoglobulin-binding protein", "Structural")
        )
    )
    
    val defense = ProteinCategory(
        name = "Defense",
        icon = Icons.Default.Security, // shield equivalent
        color = Color(0xFFF44336), // Red
        description = "Proteins that protect the body from external threats",
        proteins = listOf(
            ProteinInfo("1IGY", "Immunoglobulin", "Antibody protein", "Defense")
        )
    )
    
    val transport = ProteinCategory(
        name = "Transport",
        icon = Icons.Default.LocalShipping, // car equivalent
        color = Color(0xFF4CAF50), // Green
        description = "Proteins that transport substances",
        proteins = listOf(
            ProteinInfo("1HHO", "Hemoglobin", "Oxygen transport protein", "Transport"),
            ProteinInfo("1UBQ", "Ubiquitin", "Protein degradation marker", "Transport")
        )
    )
    
    val hormones = ProteinCategory(
        name = "Hormones",
        icon = Icons.Default.Radio, // antenna.radiowaves.left.and.right equivalent
        color = Color(0xFF9C27B0), // Purple
        description = "Proteins responsible for signal transmission",
        proteins = listOf(
            ProteinInfo("1INS", "Insulin", "Glucose regulation hormone", "Hormones")
        )
    )
    
    val storage = ProteinCategory(
        name = "Storage",
        icon = Icons.Default.Archive, // archivebox equivalent
        color = Color(0xFF795548), // Brown
        description = "Proteins that store nutrients",
        proteins = listOf(
            ProteinInfo("1CRN", "Crotin", "Storage protein", "Storage")
        )
    )
    
    val receptors = ProteinCategory(
        name = "Receptors",
        icon = Icons.Default.Wifi, // wifi equivalent
        color = Color(0xFF00BCD4), // Cyan
        description = "Receptor proteins that receive signals",
        proteins = listOf(
            ProteinInfo("1F88", "G-protein coupled receptor", "Signal receptor", "Receptors")
        )
    )
    
    val membrane = ProteinCategory(
        name = "Membrane",
        icon = Icons.Default.BubbleChart, // bubble.left.and.bubble.right equivalent
        color = Color(0xFF4DB6AC), // Mint
        description = "Proteins that compose and regulate cell membranes",
        proteins = listOf(
            ProteinInfo("1K4C", "Membrane protein", "Cell membrane protein", "Membrane")
        )
    )
    
    val motor = ProteinCategory(
        name = "Motor",
        icon = Icons.Default.Settings, // gear equivalent
        color = Color(0xFF3F51B5), // Indigo
        description = "Proteins that create movement within cells",
        proteins = listOf(
            ProteinInfo("1ATP", "ATP Synthase", "Energy production enzyme", "Motor")
        )
    )
    
    val signaling = ProteinCategory(
        name = "Signaling",
        icon = Icons.Default.Share, // network equivalent
        color = Color(0xFFE91E63), // Pink
        description = "Proteins that mediate intercellular communication",
        proteins = listOf(
            ProteinInfo("1GFL", "Signaling protein", "Cell signaling", "Signaling")
        )
    )
    
    val chaperones = ProteinCategory(
        name = "Chaperones",
        icon = Icons.Default.Build, // wrench.and.screwdriver equivalent
        color = Color(0xFFF57C00), // Dark Orange (더 어두운 주황색)
        description = "Proteins that assist in protein folding",
        proteins = listOf(
            ProteinInfo("1HRO", "HSP70", "Heat shock protein", "Chaperones")
        )
    )
    
    val metabolic = ProteinCategory(
        name = "Metabolic",
        icon = Icons.Default.Refresh, // arrow.triangle.2.circlepath equivalent
        color = Color(0xFF009688), // Teal
        description = "Proteins involved in metabolic processes",
        proteins = listOf(
            ProteinInfo("1TIM", "Metabolic enzyme", "Metabolic process protein", "Metabolic")
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

