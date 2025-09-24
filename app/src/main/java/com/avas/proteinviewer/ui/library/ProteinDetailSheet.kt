package com.avas.proteinviewer.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
// ProteinInfo는 LibraryScreen.kt에 정의되어 있음

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProteinDetailSheet(
    protein: ProteinInfo,
    onDismiss: () -> Unit,
    onView3D: (String) -> Unit,
    onFavorite: (String) -> Unit
) {
    val listState = rememberLazyListState()
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header Section
            item {
                HeaderSectionView(protein = protein)
            }
            
            // Main Info Section
            item {
                MainInfoSectionView(protein = protein)
            }
            
            // Detailed Info Section
            item {
                DetailedInfoSectionView(protein = protein)
            }
            
            // Additional Info Section
            item {
                AdditionalInfoSectionView(protein = protein)
            }
            
            // Action Buttons
            item {
                ActionButtonsSectionView(
                    protein = protein,
            onView3D = { onView3D(protein.pdbId) },
            onFavorite = { onFavorite(protein.pdbId) }
                )
            }
        }
    }
}

@Composable
private fun HeaderSectionView(protein: ProteinInfo) {
    Column {
        // 네비게이션 바 (아이폰과 동일)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            NavigationButton(
                title = "Overview",
                icon = Icons.Default.Info,
                onClick = { /* TODO: 스크롤 기능 */ }
            )
            NavigationButton(
                title = "Function", 
                icon = Icons.Default.Functions,
                onClick = { /* TODO: 스크롤 기능 */ }
            )
            NavigationButton(
                title = "Structure",
                icon = Icons.Default.Category,
                onClick = { /* TODO: 스크롤 기능 */ }
            )
        }
        
        // 헤더 카드 (아이폰과 동일)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // 상단: 아이콘과 단백질 이름
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    GradientIcon(
                        icon = Icons.Default.Science,
                        base = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(64.dp)
                    )
                    
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = protein.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                // 하단: PDB ID와 카테고리 태그
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CapsuleTag(
                        text = "PDB ${protein.pdbId}",
                        foreground = MaterialTheme.colorScheme.primary,
                        background = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        icon = Icons.Default.QrCode
                    )
                    
                    CapsuleTag(
                        text = protein.categoryName,
                        foreground = Color.White,
                        background = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                        icon = Icons.Default.Label
                    )
                }
            }
        }
    }
}

@Composable
private fun NavigationButton(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp)),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun MainInfoSectionView(protein: ProteinInfo) {
    Column {
        // Function Summary (아이폰과 동일)
        InfoCard(
            icon = Icons.Default.Functions,
            title = "Function Summary",
            tint = MaterialTheme.colorScheme.primary
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = protein.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                TextButton(
                    onClick = { /* TODO: View Details 기능 */ }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "View Details",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
        
        // 핵심 포인트 (아이폰과 동일)
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricPill(
                title = "Structure",
                value = "1→4 단계",
                icon = Icons.Default.GridView
            )
            MetricPill(
                title = "Coloring",
                value = "Element/Chain/SS",
                icon = Icons.Default.Palette
            )
            MetricPill(
                title = "Interact",
                value = "Rotate/Zoom/Slice",
                icon = Icons.Default.TouchApp
            )
        }
        
        // External Resources (아이폰과 동일)
        InfoCard(
            icon = Icons.Default.Link,
            title = "External Resources",
            tint = Color(0xFF2196F3)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LinkRow(
                    title = "View on PDB Website",
                    subtitle = "rcsb.org/structure/${protein.pdbId}",
                    icon = Icons.Default.Language,
                    tint = Color(0xFF2196F3)
                ) { /* TODO: PDB 웹사이트 열기 */ }
                
                LinkRow(
                    title = "View on UniProt",
                    subtitle = "Protein sequence & function",
                    icon = Icons.Default.Storage,
                    tint = Color(0xFF4CAF50)
                ) { /* TODO: UniProt 링크 */ }
            }
        }
        
        // Disease Association (아이폰과 동일)
        InfoCard(
            icon = Icons.Default.LocalHospital,
            title = "Disease Association",
            tint = Color(0xFFFF9800)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "This protein is associated with several diseases:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // 질병 태그들
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(listOf("Cancer", "Diabetes", "Alzheimer's")) { disease ->
                        Surface(
                            modifier = Modifier.clip(RoundedCornerShape(16.dp)),
                            color = Color(0xFFFF9800).copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = disease,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFF9800)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailedInfoSectionView(protein: ProteinInfo) {
    Column {
        // Additional Information (아이폰과 동일)
        InfoCard(
            icon = Icons.Default.Info,
            title = "Additional Information",
            tint = Color.Gray
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                InfoRow(
                    title = "Structure Type",
                    value = "X-ray Crystallography",
                    icon = Icons.Default.Category
                )
                InfoRow(
                    title = "Resolution",
                    value = "2.5 Å",
                    icon = Icons.Default.ZoomIn
                )
                InfoRow(
                    title = "Organism",
                    value = "Homo sapiens",
                    icon = Icons.Default.Person
                )
                InfoRow(
                    title = "Expression",
                    value = "E. coli",
                    icon = Icons.Default.Eco
                )
                
                TextButton(
                    onClick = { /* TODO: View Details 기능 */ }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "View Details",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(
    title: String,
    value: String,
    icon: ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(100.dp)
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun AdditionalInfoSectionView(protein: ProteinInfo) {
    Column {
        // Keywords & Tags (아이폰과 동일)
        InfoCard(
            icon = Icons.Default.Key,
            title = "Keywords & Tags",
            tint = MaterialTheme.colorScheme.primary
        ) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(listOf("Enzyme", "Catalyst", "Metabolism", "Biochemistry", "Protein")) { keyword ->
                    Surface(
                        modifier = Modifier.clip(RoundedCornerShape(16.dp)),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = keyword,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionButtonsSectionView(
    protein: ProteinInfo,
    onView3D: () -> Unit,
    onFavorite: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 3D View Button
        Button(
            onClick = onView3D,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = Icons.Filled.Visibility,
                contentDescription = "View 3D",
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("View 3D Structure")
        }
        
        // Favorite Button
        OutlinedButton(
            onClick = onFavorite,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = "Add to Favorites",
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Favorite")
        }
    }
}
