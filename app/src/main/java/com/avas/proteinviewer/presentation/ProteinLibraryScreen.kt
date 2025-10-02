package com.avas.proteinviewer.presentation

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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
    onSearch: (String) -> Unit,
    onProteinClick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<ProteinCategory?>(null) }
    var showCategoryGrid by remember { mutableStateOf(false) }
    
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
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Protein Library") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
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
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    if (it.length >= 3 || it.isEmpty()) {
                        onSearch(it)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search proteins (e.g., hemoglobin, 1HHO)") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                singleLine = true
            )
            
            // Category Selection (iOS와 동일)
            if (searchQuery.isEmpty()) {
                CategorySelectionSection(
                    selectedCategory = selectedCategory,
                    showCategoryGrid = showCategoryGrid,
                    categoryCounts = categoryCounts,
                    onCategorySelect = { category ->
                        selectedCategory = category
                        showCategoryGrid = false
                    },
                    onShowAllCategories = {
                        showCategoryGrid = true
                        selectedCategory = null
                    }
                )
            }
            
            // Protein List
            if (proteins.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isEmpty()) 
                            "Enter a search query to find proteins" 
                        else 
                            "No proteins found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
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
    onCategorySelect: (ProteinCategory) -> Unit,
    onShowAllCategories: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // All Categories 버튼
        Button(
            onClick = onShowAllCategories,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (showCategoryGrid) MaterialTheme.colorScheme.primary 
                               else MaterialTheme.colorScheme.surface
            )
        ) {
            Icon(
                Icons.Default.GridView,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text("All Categories")
        }
        
        // 카테고리 그리드 또는 선택된 카테고리 표시
        if (showCategoryGrid) {
            CategoryGridView(
                categories = ProteinCategory.values().toList(),
                categoryCounts = categoryCounts,
                onCategorySelect = onCategorySelect
            )
        } else if (selectedCategory != null) {
            SelectedCategoryView(
                category = selectedCategory,
                proteinCount = categoryCounts[selectedCategory.displayName] ?: 0,
                onBack = { onShowAllCategories() }
            )
        }
    }
}

// 카테고리 그리드 뷰 (iOS와 동일)
@Composable
private fun CategoryGridView(
    categories: List<ProteinCategory>,
    categoryCounts: Map<String, Int>,
    onCategorySelect: (ProteinCategory) -> Unit
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

