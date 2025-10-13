package com.avas.proteinviewer.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.avas.proteinviewer.domain.model.*
import com.avas.proteinviewer.data.api.ResearchDetailAPIService
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResearchDetailScreen(
    protein: ProteinInfo,
    researchType: ResearchDetailType,
    onDismiss: () -> Unit
) {
    val apiService = remember { ResearchDetailAPIService() }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var publications by remember { mutableStateOf<List<ResearchPublication>>(emptyList()) }
    var clinicalTrials by remember { mutableStateOf<List<ClinicalTrial>>(emptyList()) }
    var activeStudies by remember { mutableStateOf<List<ActiveStudy>>(emptyList()) }
    
    LaunchedEffect(protein.id, researchType) {
        isLoading = true
        error = null
        
        try {
            when (researchType) {
                ResearchDetailType.PUBLICATIONS -> {
                    publications = apiService.fetchPublications(protein.id)
                }
                ResearchDetailType.CLINICAL_TRIALS -> {
                    clinicalTrials = apiService.fetchClinicalTrials(protein.id)
                }
                ResearchDetailType.ACTIVE_STUDIES -> {
                    activeStudies = apiService.fetchActiveStudies(protein.id)
                }
            }
        } catch (e: Exception) {
            error = e.message ?: "Failed to load research details"
        }
        
        isLoading = false
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (researchType) {
                            ResearchDetailType.PUBLICATIONS -> "Publications"
                            ResearchDetailType.CLINICAL_TRIALS -> "Clinical Trials"
                            ResearchDetailType.ACTIVE_STUDIES -> "Active Studies"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Header Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Protein Icon
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                Color(protein.category.color).copy(alpha = 0.2f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Science,
                            contentDescription = null,
                            tint = Color(protein.category.color),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = protein.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "PDB ${protein.id}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Count Badge
                    Surface(
                        color = Color(protein.category.color),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = when (researchType) {
                                ResearchDetailType.PUBLICATIONS -> "${publications.size}"
                                ResearchDetailType.CLINICAL_TRIALS -> "${clinicalTrials.size}"
                                ResearchDetailType.ACTIVE_STUDIES -> "${activeStudies.size}"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
            
            // Content Section
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(
                                color = Color(protein.category.color)
                            )
                            Text(
                                text = "Loading research data...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFFF9500),
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "Failed to load data",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = error!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                else -> {
                    val hasData = when (researchType) {
                        ResearchDetailType.PUBLICATIONS -> publications.isNotEmpty()
                        ResearchDetailType.CLINICAL_TRIALS -> clinicalTrials.isNotEmpty()
                        ResearchDetailType.ACTIVE_STUDIES -> activeStudies.isNotEmpty()
                    }
                    
                    if (hasData) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            when (researchType) {
                                ResearchDetailType.PUBLICATIONS -> {
                                    items(publications) { publication ->
                                        PublicationCard(publication = publication)
                                    }
                                }
                                ResearchDetailType.CLINICAL_TRIALS -> {
                                    items(clinicalTrials) { trial ->
                                        ClinicalTrialCard(trial = trial)
                                    }
                                }
                                ResearchDetailType.ACTIVE_STUDIES -> {
                                    items(activeStudies) { study ->
                                        ActiveStudyCard(study = study)
                                    }
                                }
                            }
                        }
                    } else {
                        // 데이터가 없을 때 메시지 표시
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Icon(
                                    imageVector = when (researchType) {
                                        ResearchDetailType.PUBLICATIONS -> Icons.AutoMirrored.Filled.MenuBook
                                        ResearchDetailType.CLINICAL_TRIALS -> Icons.Default.MedicalServices
                                        ResearchDetailType.ACTIVE_STUDIES -> Icons.Default.Science
                                    },
                                    contentDescription = null,
                                    tint = Color(protein.category.color),
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = when (researchType) {
                                        ResearchDetailType.PUBLICATIONS -> "No publications found"
                                        ResearchDetailType.CLINICAL_TRIALS -> "No clinical trials found"
                                        ResearchDetailType.ACTIVE_STUDIES -> "No active studies found"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "No research data available for this protein",
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

@Composable
private fun PublicationCard(publication: ResearchPublication) {
    var isAbstractExpanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = publication.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                InfoChip(
                    icon = Icons.Default.Person,
                    text = publication.displayAuthors,
                    color = Color(0xFF007AFF)
                )
                
                InfoChip(
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    text = publication.journal,
                    color = Color(0xFF34C759)
                )
                
                InfoChip(
                    icon = Icons.Default.CalendarToday,
                    text = publication.year.toString(),
                    color = Color(0xFFFF9500)
                )
            }
            
            publication.abstract?.let { abstract ->
                Column {
                    Text(
                        text = abstract,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (isAbstractExpanded) Int.MAX_VALUE else 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    if (abstract.length > 150) {
                        TextButton(
                            onClick = { isAbstractExpanded = !isAbstractExpanded },
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text = if (isAbstractExpanded) "접기" else "더보기",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF007AFF)
                            )
                            Icon(
                                imageVector = if (isAbstractExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = Color(0xFF007AFF),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                publication.doi?.let { doi ->
                    LinkChip(
                        icon = Icons.Default.Link,
                        text = "DOI",
                        url = "https://doi.org/$doi",
                        color = Color(0xFF9C27B0)
                    )
                }
                
                publication.pmid?.let { pmid ->
                    LinkChip(
                        icon = Icons.Default.Search,
                        text = "PubMed",
                        url = "https://pubmed.ncbi.nlm.nih.gov/$pmid/",
                        color = Color(0xFF34C759)
                    )
                }
            }
        }
    }
}

@Composable
private fun ClinicalTrialCard(trial: ClinicalTrial) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = trial.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusChip(
                    text = trial.status,
                    color = Color(trial.statusColor)
                )
                
                trial.phase?.let { phase ->
                    InfoChip(
                        icon = Icons.Default.Science,
                        text = phase,
                        color = Color(0xFF007AFF)
                    )
                }
                
                trial.enrollment?.let { enrollment ->
                    InfoChip(
                        icon = Icons.Default.People,
                        text = "$enrollment participants",
                        color = Color(0xFF34C759)
                    )
                }
            }
            
            trial.condition?.let { condition ->
                Text(
                    text = "Condition: $condition",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            trial.intervention?.let { intervention ->
                Text(
                    text = "Intervention: $intervention",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            trial.sponsor?.let { sponsor ->
                Text(
                    text = "Sponsor: $sponsor",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            LinkChip(
                icon = Icons.Default.Link,
                text = "View on ClinicalTrials.gov",
                url = "https://clinicaltrials.gov/study/${trial.nctId}",
                color = Color(0xFF007AFF)
            )
        }
    }
}

@Composable
private fun ActiveStudyCard(study: ActiveStudy) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TypeChip(
                    text = study.type,
                    color = Color(study.typeColor)
                )
                
                StatusChip(
                    text = study.status,
                    color = Color(0xFF8E8E93)
                )
            }
            
            Text(
                text = study.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            study.institution?.let { institution ->
                Text(
                    text = "Institution: $institution",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            study.principalInvestigator?.let { pi ->
                Text(
                    text = "Principal Investigator: $pi",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            study.description?.let { description ->
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// Helper Composables

@Composable
private fun InfoChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    color: Color
) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun StatusChip(
    text: String,
    color: Color
) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun TypeChip(
    text: String,
    color: Color
) {
    Surface(
        color = color,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun LinkChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    url: String,
    color: Color
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.clickable { 
            try {
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                context.startActivity(intent)
            } catch (e: Exception) {
                android.util.Log.e("LinkChip", "Failed to open URL: $url", e)
            }
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Medium
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(10.dp)
            )
        }
    }
}
