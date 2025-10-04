package com.avas.proteinviewer.presentation

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ManageSearch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.avas.proteinviewer.domain.model.ProteinInfo
import com.avas.proteinviewer.domain.model.ProteinCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProteinLibraryScreen(
    proteins: List<ProteinInfo>,
    selectedCategory: ProteinCategory?,
    showCategoryGrid: Boolean,
    categoryCounts: Map<String, Int>,
    onSearch: (String) -> Unit,
    onProteinClick: (String) -> Unit,
    onCategorySelect: (ProteinCategory?) -> Unit,
    onShowAllCategories: () -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var showFavoritesOnly by remember { mutableStateOf(false) }
    var showCustomSearchDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            LibraryHeader(
                onDismiss = onDismiss,
                searchQuery = searchQuery,
                onSearchQueryChange = {
                    searchQuery = it
                    if (it.length >= 3 || it.isEmpty()) {
                        onSearch(it)
                    }
                },
                onClearSearch = {
                    searchQuery = ""
                    onSearch("")
                },
                onActionClick = {
                    onSearch(searchQuery)
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (searchQuery.isNotEmpty()) {
                SearchStatusBanner(
                    query = searchQuery,
                    resultCount = proteins.size,
                    onReset = {
                        searchQuery = ""
                        onSearch("")
                        onCategorySelect(null)
                        onShowAllCategories()
                        showFavoritesOnly = false
                    }
                )
            }

            // Category Selection (iOS와 동일)
            if (searchQuery.isEmpty()) {
                CategoryFilterRow(
                    selectedCategory = selectedCategory,
                    showFavoritesOnly = showFavoritesOnly,
                    onAllCategories = {
                        showFavoritesOnly = false
                        onCategorySelect(null)
                        onShowAllCategories()
                    },
                    onCategorySelect = { category ->
                        showFavoritesOnly = false
                        onCategorySelect(category)
                    },
                    onToggleFavorites = {
                        showFavoritesOnly = !showFavoritesOnly
                        if (showFavoritesOnly) {
                            onCategorySelect(null)
                            onShowAllCategories()
                        }
                    },
                    onCustomSearch = {
                        showFavoritesOnly = false
                        showCustomSearchDialog = true
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (showFavoritesOnly) {
                    FavoritesEmptyState(
                        onBrowseCategories = {
                            showFavoritesOnly = false
                        }
                    )
                } else {
                    CategorySelectionSection(
                        selectedCategory = selectedCategory,
                        showCategoryGrid = showCategoryGrid,
                        categoryCounts = categoryCounts,
                        onCategorySelect = onCategorySelect,
                        onShowAllCategories = onShowAllCategories
                    )
                }
            }
            
            // Protein List - 카테고리 선택 시 또는 검색 결과가 있을 때 표시
            if (selectedCategory != null || searchQuery.isNotEmpty()) {
                if (proteins.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isEmpty()) 
                                "No proteins found in this category" 
                            else 
                                "No proteins found",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // 선택된 카테고리 헤더 (카테고리가 선택된 경우)
                        if (selectedCategory != null) {
                            SelectedCategoryView(
                                category = selectedCategory,
                                proteinCount = proteins.size,
                                onBack = { onCategorySelect(null) }
                            )
                        }
                        
                        // 단백질 리스트
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(proteins) { protein ->
                                ProteinCard(
                                    protein = protein,
                                    onClick = { onProteinClick(protein.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCustomSearchDialog) {
        AlertDialog(
            onDismissRequest = { showCustomSearchDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.ManageSearch,
                    contentDescription = null
                )
            },
            title = { Text("Custom Search") },
            text = {
                Text(
                    text = "Advanced search presets will be available soon. For now, use the text field above to search by keyword or PDB ID.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = { showCustomSearchDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun LibraryHeader(
    onDismiss: () -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onActionClick: () -> Unit
) {
    val actionState = remember(searchQuery) { calculateSearchActionState(searchQuery) }

    Surface(
        tonalElevation = 2.dp,
        shadowElevation = 4.dp,
        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Protein Library",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Browse curated protein categories",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                FilledTonalIconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close library"
                    )
                }
            }

            SearchBarSection(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                onClear = onClearSearch,
                actionState = actionState,
                onActionClick = onActionClick
            )
        }
    }
}

@Composable
private fun SearchBarSection(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    actionState: SearchActionState,
    onActionClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ) {
            TextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyMedium,
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = onClear) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                placeholder = {
                    Text(
                        text = "Search proteins (e.g. hemoglobin, 1HHO)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                colors = TextFieldDefaults.textFieldColors(
                    containerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )
        }

        FilledTonalButton(
            onClick = onActionClick,
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = actionState.containerColor,
                contentColor = actionState.contentColor
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = actionState.icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = actionState.label,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1
            )
        }
    }
}

private data class SearchActionState(
    val label: String,
    val icon: ImageVector,
    val containerColor: Color,
    val contentColor: Color
)

@Composable
private fun calculateSearchActionState(query: String): SearchActionState {
    val trimmed = query.trim()
    val isPdbId = trimmed.length == 4 && trimmed.all { it.isLetterOrDigit() }
    return when {
        isPdbId -> SearchActionState(
            label = "PDB ID Search",
            icon = Icons.Default.Search,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
        trimmed.length >= 2 -> SearchActionState(
            label = "Text Search",
            icon = Icons.Default.Search,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
        else -> SearchActionState(
            label = "Load Data",
            icon = Icons.Default.CloudDownload,
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        )
    }
}

@Composable
private fun SearchStatusBanner(
    query: String,
    resultCount: Int,
    onReset: () -> Unit
) {
    val trimmed = query.trim()
    val isPdbId = trimmed.length == 4 && trimmed.all { it.isLetterOrDigit() }
    val hasResults = resultCount > 0

    val containerColor = if (hasResults) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    } else {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
    }

    val message = if (hasResults) {
        if (isPdbId) {
            "${trimmed.uppercase()} • $resultCount result${if (resultCount == 1) "" else "s"}"
        } else {
            "\"$trimmed\" • $resultCount matches"
        }
    } else {
        if (isPdbId) {
            "No structure found for ${trimmed.uppercase()}"
        } else {
            "No results for \"$trimmed\""
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        color = containerColor,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (hasResults) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onErrorContainer
                    }
                )
                if (!hasResults) {
                    Text(
                        text = "Try a different keyword or reset",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                    )
                }
            }

            TextButton(onClick = onReset) {
                Text("View All")
            }
        }
    }
}

@Composable
private fun ProteinCard(
    protein: ProteinInfo,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = protein.id,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                protein.resolution?.let {
                    Text(
                        text = "${String.format("%.2f", it)} Å",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = protein.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = protein.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
            
            protein.organism?.let { organism ->
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Organism: ",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = organism,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            protein.experimentalMethod?.let { method ->
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Method: ",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = method,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// 카테고리 선택 섹션 (iOS와 동일)
@Composable
private fun CategorySelectionSection(
    selectedCategory: ProteinCategory?,
    showCategoryGrid: Boolean,
    categoryCounts: Map<String, Int>,
    onCategorySelect: (ProteinCategory?) -> Unit,
    onShowAllCategories: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        if (selectedCategory == null) {
            // All Categories - 카테고리 그리드 바로 표시 (iOS와 동일)
            Column(
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.padding(top = 20.dp)
            ) {
                // 헤더
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Choose a Category",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Text(
                        text = "Explore proteins by their biological function",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
                
                // 카테고리 그리드
                CategoryGridView(
                    categories = ProteinCategory.values().toList(),
                    categoryCounts = categoryCounts,
                    onCategorySelect = onCategorySelect
                )
            }
        }
    }
    
    // 로딩 팝업은 나중에 추가 (핵심 기능 우선)
}

@Composable
private fun CategoryFilterRow(
    selectedCategory: ProteinCategory?,
    showFavoritesOnly: Boolean,
    onAllCategories: () -> Unit,
    onCategorySelect: (ProteinCategory) -> Unit,
    onToggleFavorites: () -> Unit,
    onCustomSearch: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Categories",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Swipe for more",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                CategoryFilterChip(
                    label = "All",
                    selected = selectedCategory == null && !showFavoritesOnly,
                    color = MaterialTheme.colorScheme.primary,
                    onClick = onAllCategories
                ) {
                    Icon(
                        imageVector = Icons.Default.GridView,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            items(ProteinCategory.values()) { category ->
                CategoryFilterChip(
                    label = category.displayName,
                    selected = selectedCategory == category,
                    color = Color(category.color),
                    onClick = { onCategorySelect(category) }
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(Color(category.color).copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = category.displayName.take(1).uppercase(),
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(category.color),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            item {
                CategoryFilterChip(
                    label = "Favorites",
                    selected = showFavoritesOnly,
                    color = MaterialTheme.colorScheme.tertiary,
                    onClick = onToggleFavorites
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            item {
                CategoryFilterChip(
                    label = "Custom",
                    selected = false,
                    color = MaterialTheme.colorScheme.secondary,
                    onClick = onCustomSearch
                ) {
                    Icon(
                        imageVector = Icons.Default.ManageSearch,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryFilterChip(
    label: String,
    selected: Boolean,
    color: Color,
    onClick: () -> Unit,
    leadingContent: @Composable () -> Unit
) {
    val containerColor = if (selected) {
        color.copy(alpha = 0.18f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    }
    val borderColor = if (selected) color.copy(alpha = 0.7f) else Color.Transparent

    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        color = containerColor,
        border = if (borderColor == Color.Transparent) null else BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            leadingContent()
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun FavoritesEmptyState(
    onBrowseCategories: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        tonalElevation = 2.dp,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(36.dp)
            )
            Text(
                text = "No favorites yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Save proteins to quickly access them here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            FilledTonalButton(onClick = onBrowseCategories) {
                Text("Browse categories")
            }
        }
    }
}

// 카테고리 그리드 뷰 (iOS와 동일)
@Composable
private fun CategoryGridView(
    categories: List<ProteinCategory>,
    categoryCounts: Map<String, Int>,
    onCategorySelect: (ProteinCategory?) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(categories) { category ->
            CategoryCard(
                category = category,
                proteinCount = categoryCounts[category.displayName] ?: 0,
                onSelect = { onCategorySelect(category) }
            )
        }
    }
}

// 카테고리 카드 (iOS와 동일)
@Composable
private fun CategoryCard(
    category: ProteinCategory,
    proteinCount: Int,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 아이콘
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(
                        color = Color(category.color).copy(alpha = 0.1f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = category.icon.uppercase().take(1),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(category.color),
                    fontWeight = FontWeight.Bold
                )
            }
            
            // 카테고리 정보
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = category.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                
                Text(
                    text = "$proteinCount proteins",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // 설명
            Text(
                text = category.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}

// 선택된 카테고리 뷰
@Composable
private fun SelectedCategoryView(
    category: ProteinCategory,
    proteinCount: Int,
    onBack: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(category.color).copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = category.displayName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(category.color)
                )
                
                TextButton(onClick = onBack) {
                    Text("Back to Categories")
                }
            }
            
            Text(
                text = "$proteinCount proteins found",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = category.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
