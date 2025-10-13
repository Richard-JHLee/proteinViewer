package com.avas.proteinviewer.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.avas.proteinviewer.data.api.ResearchDetailAPIService
import com.avas.proteinviewer.domain.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class ResearchDetailViewModel @Inject constructor(
    private val researchDetailAPIService: ResearchDetailAPIService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ResearchDetailUiState())
    val uiState: StateFlow<ResearchDetailUiState> = _uiState.asStateFlow()
    
    fun loadResearchDetails(proteinId: String, researchType: ResearchDetailType) {
        android.util.Log.d("ResearchDetailViewModel", "🔍 loadResearchDetails called: $proteinId, $researchType")
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )
            
            try {
                when (researchType) {
                    ResearchDetailType.PUBLICATIONS -> {
                        android.util.Log.d("ResearchDetailViewModel", "📚 Fetching publications...")
                        val publications = researchDetailAPIService.fetchPublications(proteinId)
                        android.util.Log.d("ResearchDetailViewModel", "📚 Publications loaded: ${publications.size}")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            publications = publications
                        )
                    }
                    ResearchDetailType.CLINICAL_TRIALS -> {
                        android.util.Log.d("ResearchDetailViewModel", "🏥 Fetching clinical trials...")
                        val trials = researchDetailAPIService.fetchClinicalTrials(proteinId)
                        android.util.Log.d("ResearchDetailViewModel", "🏥 Clinical trials loaded: ${trials.size}")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            clinicalTrials = trials
                        )
                    }
                    ResearchDetailType.ACTIVE_STUDIES -> {
                        android.util.Log.d("ResearchDetailViewModel", "🔬 Fetching active studies...")
                        val studies = researchDetailAPIService.fetchActiveStudies(proteinId)
                        android.util.Log.d("ResearchDetailViewModel", "🔬 Active studies loaded: ${studies.size}")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            activeStudies = studies
                        )
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ResearchDetailViewModel", "❌ Error loading research details: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load research details"
                )
            }
        }
    }
}

data class ResearchDetailUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val publications: List<ResearchPublication> = emptyList(),
    val clinicalTrials: List<ClinicalTrial> = emptyList(),
    val activeStudies: List<ActiveStudy> = emptyList()
)
