package com.avas.proteinviewer.presentation

import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
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
    favoriteIds: Set<String> = emptySet(),
    hasMoreResults: Boolean = false,
    isLoadingResults: Boolean = false,
    isLoadingMore: Boolean = false,
    loadingMessage: String? = null,
    onSearch: (String) -> Unit,
    onSearchBasedDataLoad: (String) -> Unit,
    onProteinClick: (String) -> Unit,
    onCategorySelect: (ProteinCategory?) -> Unit,
    onShowAllCategories: () -> Unit,
    onDismiss: () -> Unit,
    onToggleFavorite: (String) -> Unit = {},
    onLoadMore: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var showFavoritesOnly by remember { mutableStateOf(false) }
    var showCustomSearchDialog by remember { mutableStateOf(false) }
    val displayedProteins = remember(proteins, showFavoritesOnly, favoriteIds) {
        if (showFavoritesOnly) {
            proteins.filter { favoriteIds.contains(it.id) }
        } else {
            proteins
        }
    }
    
    Scaffold(
        topBar = {
            LibraryHeader(
                onDismiss = onDismiss,
                searchQuery = searchQuery,
                onSearchQueryChange = {
                    searchQuery = it
                    if (it.isNotEmpty()) {
                        showFavoritesOnly = false
                        onCategorySelect(null)
                    } else {
                        onShowAllCategories()
                    }
                    if (it.length >= 3 || it.isEmpty()) {
                        onSearch(it)
                    }
                },
                onClearSearch = {
                    searchQuery = ""
                    showFavoritesOnly = false
                    onSearch("")
                    onShowAllCategories()
                },
                onActionClick = {
                    showFavoritesOnly = false
                    val trimmed = searchQuery.trim()
                    if (trimmed.length >= 2) {
                        // 2자 이상: 검색어 기반 API 호출 (아이폰과 동일)
                        onSearchBasedDataLoad(searchQuery)
                    } else {
                        // 2자 미만: 기존 카테고리 개수 업데이트 (아이폰과 동일)
                        onSearch("")
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
            if (searchQuery.isNotEmpty()) {
                SearchStatusBanner(
                    query = searchQuery,
                    resultCount = displayedProteins.size,
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

                Spacer(modifier = Modifier.height(4.dp))

                if (showFavoritesOnly) {
                    if (displayedProteins.isEmpty()) {
                        FavoritesEmptyState(
                            onBrowseCategories = {
                                showFavoritesOnly = false
                            }
                        )
                    } else {
                        FavoritesSelectedHeader(count = displayedProteins.size)
                    }
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
            
            // Protein List - 카테고리/검색/즐겨찾기 결과 표시
            val shouldShowList = selectedCategory != null || searchQuery.isNotEmpty() || showFavoritesOnly
            if (shouldShowList) {
                if (displayedProteins.isEmpty()) {
                    val emptyMessage = when {
                        showFavoritesOnly -> "No saved proteins yet"
                        selectedCategory != null -> "No proteins found in this category"
                        else -> "No proteins found"
                    }
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = emptyMessage,
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
                                proteinCount = displayedProteins.size,
                                onBack = { onCategorySelect(null) }
                            )
                        }
                        
                        // 단백질 리스트
                        Box(modifier = Modifier.fillMaxSize()) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(displayedProteins) { protein ->
                                    ProteinCard(
                                        protein = protein,
                                        isFavorite = favoriteIds.contains(protein.id),
                                        onClick = { onProteinClick(protein.id) },
                                        onToggleFavorite = { onToggleFavorite(protein.id) }
                                    )
                                }

                                if (hasMoreResults) {
                                    item {
                                        LoadMoreSection(
                                            isLoading = isLoadingMore,
                                            onLoadMore = onLoadMore
                                        )
                                    }
                                }
                            }

                            if (isLoadingResults) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        CircularProgressIndicator()
                                        loadingMessage?.let {
                                            Text(
                                                text = it,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
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
@OptIn(ExperimentalMaterial3Api::class)
private fun LibraryHeader(
    onDismiss: () -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onActionClick: () -> Unit
) {
    val actionState = calculateSearchActionState(searchQuery)

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
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
            
            Text(
                text = "Browse curated protein categories",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
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
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
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
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )
        )

        FilledTonalButton(
            onClick = onActionClick,
            modifier = Modifier.height(48.dp),
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
            containerColor = Color(0xFF9C27B0).copy(alpha = 0.2f), // 아이폰과 동일한 보라색
            contentColor = Color(0xFF9C27B0)
        )
        trimmed.length >= 2 -> SearchActionState(
            label = "Text Search",
            icon = Icons.Default.Search,
            containerColor = Color(0xFF4CAF50).copy(alpha = 0.2f), // 아이폰과 동일한 초록색
            contentColor = Color(0xFF4CAF50)
        )
        else -> SearchActionState(
            label = "Load Data",
            icon = Icons.Default.CloudDownload,
            containerColor = Color(0xFF2196F3).copy(alpha = 0.2f), // 아이폰과 동일한 파란색
            contentColor = Color(0xFF2196F3)
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
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = protein.id,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = protein.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Toggle favorite",
                        tint = if (isFavorite) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = protein.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                protein.resolution?.let {
                    ProteinMetaChip(
                        label = "${String.format("%.2f", it)} Å",
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    )
                }

                protein.experimentalMethod?.let { method ->
                    ProteinMetaChip(
                        label = method,
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
                    )
                }
            }

            protein.organism?.let { organism ->
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Public,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = organism,
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
            // Total count text (아이폰과 동일 - All 버튼과 Choose a Category 사이)
            val totalCount = categoryCounts.values.sum()
            Text(
                text = "Total: $totalCount proteins across all categories",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            
            // All Categories - 카테고리 그리드 바로 표시 (iOS와 동일)
            Column(
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.padding(top = 8.dp)
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

@Composable
private fun FavoritesSelectedHeader(count: Int) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Favorites",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "$count saved protein${if (count == 1) "" else "s"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LoadMoreSection(
    isLoading: Boolean,
    onLoadMore: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilledTonalButton(
            onClick = onLoadMore,
            enabled = !isLoading,
            shape = RoundedCornerShape(20.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(if (isLoading) "Loading…" else "Load more")
        }
        Text(
            text = "Fetch additional proteins in this category",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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

@Composable
private fun ProteinMetaChip(
    label: String,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color,
        tonalElevation = 0.dp
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 6.dp)
        )
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
