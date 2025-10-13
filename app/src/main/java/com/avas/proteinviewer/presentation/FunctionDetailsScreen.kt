package com.avas.proteinviewer.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.avas.proteinviewer.domain.model.FunctionDetails
import com.avas.proteinviewer.domain.model.ProteinInfo

/**
 * 아이폰 FunctionDetailsView와 완전히 동일
 * Function Summary의 "View Details" 클릭 시 표시되는 모달
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FunctionDetailsScreen(
    protein: ProteinInfo,
    uiState: ProteinUiState,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = Modifier.fillMaxHeight(0.95f),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Navigation Bar (아이폰과 동일)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(60.dp))
                    
                    Text(
                        text = "Function Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    TextButton(
                        onClick = onDismiss
                    ) {
                        Text(
                            text = "Done",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            HorizontalDivider()
            
            // Scrollable Content (아이폰과 동일)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when {
                    // 로딩 상태
                    uiState.isFunctionLoading -> {
                        FunctionLoadingView()
                    }
                    // 에러 상태
                    uiState.functionError != null -> {
                        FunctionErrorView(
                            error = uiState.functionError!!,
                            onRetry = { /* Retry logic handled by caller */ }
                        )
                    }
                    // 데이터 표시
                    uiState.functionDetails != null -> {
                        FunctionContentView(
                            protein = protein,
                            details = uiState.functionDetails!!
                        )
                    }
                    // Empty state
                    else -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No function data available",
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

// MARK: - Loading View

@Composable
private fun FunctionLoadingView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = "Loading function details...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// MARK: - Error View

@Composable
private fun FunctionErrorView(
    error: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = Color(0xFFFF9500),
                modifier = Modifier.size(64.dp)
            )
            
            Text(
                text = "Failed to load function details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Button(
                onClick = onRetry
            ) {
                Text("Retry")
            }
        }
    }
}

// MARK: - Content View

@Composable
private fun FunctionContentView(
    protein: ProteinInfo,
    details: FunctionDetails
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header (아이폰과 동일)
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Protein Function Information",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Text(
                text = "PDB ID: ${protein.id}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // Molecular Function (아이폰과 동일)
        FunctionInfoCard(
            icon = Icons.Default.Functions,
            title = "Molecular Function",
            tint = Color(0xFF007AFF) // Blue
        ) {
            Text(
                text = details.molecularFunction,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        
        // Biological Process (아이폰과 동일)
        FunctionInfoCard(
            icon = Icons.Default.AccountTree,
            title = "Biological Process",
            tint = Color(0xFF34C759) // Green
        ) {
            Text(
                text = details.biologicalProcess,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        
        // Cellular Component (아이폰과 동일)
        FunctionInfoCard(
            icon = Icons.Default.Domain,
            title = "Cellular Component",
            tint = Color(0xFFFF9500) // Orange
        ) {
            Text(
                text = details.cellularComponent,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        
        // GO Terms (아이폰과 동일)
        if (details.goTerms.isNotEmpty()) {
            FunctionInfoCard(
                icon = Icons.Default.Tag,
                title = "GO Terms",
                tint = Color(0xFF9C27B0) // Purple
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    details.goTerms.forEach { term ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = term.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            
                            Surface(
                                color = Color(term.categoryColor).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = term.categoryDisplayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(term.categoryColor),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // EC Numbers (아이폰과 동일)
        if (details.ecNumbers.isNotEmpty()) {
            FunctionInfoCard(
                icon = Icons.Default.Numbers,
                title = "EC Numbers",
                tint = Color(0xFFFF3B30) // Red
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    details.ecNumbers.forEach { ecNumber ->
                        Text(
                            text = ecNumber,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
        
        // Catalytic Activity (항상 표시, 데이터 없으면 메시지 표시)
        FunctionInfoCard(
            icon = Icons.Default.Science,
            title = "Catalytic Activity",
            tint = Color(0xFF00BCD4) // Cyan
        ) {
            Text(
                text = if (details.catalyticActivity != "Catalytic activity information not available") {
                    details.catalyticActivity
                } else {
                    "No catalytic activity information available"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (details.catalyticActivity != "Catalytic activity information not available") {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                fontStyle = if (details.catalyticActivity == "Catalytic activity information not available") {
                    androidx.compose.ui.text.font.FontStyle.Italic
                } else {
                    androidx.compose.ui.text.font.FontStyle.Normal
                }
            )
        }
        
        // Structure Information (확장: 더 많은 정보 표시)
        FunctionInfoCard(
            icon = Icons.Default.ViewInAr,
            title = "Structure Information",
            tint = Color(0xFF5856D6) // Indigo
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (details.resolution != null) {
                    StructureInfoRow(
                        label = "Resolution",
                        value = String.format("%.2f Å", details.resolution)
                    )
                }
                
                if (details.method != null) {
                    StructureInfoRow(
                        label = "Method",
                        value = details.method
                    )
                }
                
                if (details.depositionDate != null) {
                    StructureInfoRow(
                        label = "Deposition Date",
                        value = details.depositionDate
                    )
                }
                
                if (details.releaseDate != null) {
                    StructureInfoRow(
                        label = "Release Date",
                        value = details.releaseDate
                    )
                }
                
                if (details.numberOfChains != null) {
                    StructureInfoRow(
                        label = "Number of Chains",
                        value = "${details.numberOfChains}"
                    )
                }
                
                if (details.numberOfResidues != null) {
                    StructureInfoRow(
                        label = "Number of Residues",
                        value = "${details.numberOfResidues}"
                    )
                }
            }
        }
    }
}

// MARK: - Helper Composables

@Composable
private fun StructureInfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FunctionInfoCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    tint: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Title with icon
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(20.dp)
                )
                
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = tint
                )
            }
            
            // Content
            content()
        }
    }
}

