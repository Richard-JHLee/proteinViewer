package com.avas.proteinviewer.presentation.viewmodel

import com.avas.proteinviewer.core.domain.model.Protein
import com.avas.proteinviewer.core.domain.model.ProteinCategory
import com.avas.proteinviewer.core.domain.usecase.LoadProteinUseCase
import com.avas.proteinviewer.core.domain.usecase.SearchProteinsUseCase
import com.avas.proteinviewer.core.domain.usecase.ToggleFavoriteUseCase
import com.avas.proteinviewer.presentation.state.ProteinViewerState
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProteinViewModelTest {
    
    private lateinit var loadProteinUseCase: LoadProteinUseCase
    private lateinit var searchProteinsUseCase: SearchProteinsUseCase
    private lateinit var toggleFavoriteUseCase: ToggleFavoriteUseCase
    private lateinit var viewModel: ProteinViewModel
    
    private val testDispatcher = StandardTestDispatcher()
    
    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        
        loadProteinUseCase = mockk()
        searchProteinsUseCase = mockk()
        toggleFavoriteUseCase = mockk()
        
        viewModel = ProteinViewModel(
            loadProteinUseCase = loadProteinUseCase,
            searchProteinsUseCase = searchProteinsUseCase,
            toggleFavoriteUseCase = toggleFavoriteUseCase
        )
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `loadProtein with success updates state correctly`() = runTest {
        // Given
        val proteinId = "1INS"
        val protein = createTestProtein(proteinId, "Insulin")
        coEvery { loadProteinUseCase(proteinId) } returns Result.success(protein)
        
        // When
        viewModel.loadProtein(proteinId)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(protein, state.currentProtein)
        assertNull(state.error)
    }
    
    @Test
    fun `loadProtein with failure updates error state`() = runTest {
        // Given
        val proteinId = "1INS"
        val errorMessage = "Network error"
        coEvery { loadProteinUseCase(proteinId) } returns Result.failure(Exception(errorMessage))
        
        // When
        viewModel.loadProtein(proteinId)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.currentProtein)
        assertEquals(errorMessage, state.error)
    }
    
    @Test
    fun `searchProteins with success updates search results`() = runTest {
        // Given
        val query = "insulin"
        val searchResults = listOf(
            createTestProtein("1INS", "Insulin"),
            createTestProtein("2INS", "Insulin variant")
        )
        coEvery { searchProteinsUseCase(query) } returns Result.success(searchResults)
        
        // When
        viewModel.searchProteins(query)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isSearching)
        assertEquals(searchResults, state.searchResults)
        assertNull(state.searchError)
    }
    
    @Test
    fun `searchProteins with failure updates search error`() = runTest {
        // Given
        val query = "insulin"
        val errorMessage = "Search failed"
        coEvery { searchProteinsUseCase(query) } returns Result.failure(Exception(errorMessage))
        
        // When
        viewModel.searchProteins(query)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isSearching)
        assertTrue(state.searchResults.isEmpty())
        assertEquals(errorMessage, state.searchError)
    }
    
    @Test
    fun `toggleFavorite with success updates protein favorite status`() = runTest {
        // Given
        val proteinId = "1INS"
        val protein = createTestProtein(proteinId, "Insulin")
        viewModel.loadProtein(proteinId)
        advanceUntilIdle()
        
        coEvery { toggleFavoriteUseCase(proteinId) } returns Result.success(true)
        
        // When
        viewModel.toggleFavorite(proteinId)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertTrue(state.currentProtein?.isFavorite == true)
    }
    
    @Test
    fun `clearError resets error states`() = runTest {
        // Given
        val proteinId = "1INS"
        coEvery { loadProteinUseCase(proteinId) } returns Result.failure(Exception("Error"))
        viewModel.loadProtein(proteinId)
        advanceUntilIdle()
        
        // When
        viewModel.clearError()
        
        // Then
        val state = viewModel.uiState.value
        assertNull(state.error)
        assertNull(state.searchError)
    }
    
    @Test
    fun `clearSearchResults resets search results`() = runTest {
        // Given
        val query = "insulin"
        val searchResults = listOf(createTestProtein("1INS", "Insulin"))
        coEvery { searchProteinsUseCase(query) } returns Result.success(searchResults)
        viewModel.searchProteins(query)
        advanceUntilIdle()
        
        // When
        viewModel.clearSearchResults()
        
        // Then
        val state = viewModel.uiState.value
        assertTrue(state.searchResults.isEmpty())
    }
    
    private fun createTestProtein(id: String, name: String): Protein {
        return Protein(
            id = id,
            name = name,
            description = "Test protein",
            organism = "Homo sapiens",
            molecularWeight = 1000f,
            resolution = 2.0f,
            experimentalMethod = "X-ray",
            depositionDate = "2023-01-01",
            spaceGroup = "P1",
            category = ProteinCategory.ENZYME,
            isFavorite = false,
            imagePath = null,
            structure = null
        )
    }
}


import com.avas.proteinviewer.core.domain.model.Protein
import com.avas.proteinviewer.core.domain.model.ProteinCategory
import com.avas.proteinviewer.core.domain.usecase.LoadProteinUseCase
import com.avas.proteinviewer.core.domain.usecase.SearchProteinsUseCase
import com.avas.proteinviewer.core.domain.usecase.ToggleFavoriteUseCase
import com.avas.proteinviewer.presentation.state.ProteinViewerState
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProteinViewModelTest {
    
    private lateinit var loadProteinUseCase: LoadProteinUseCase
    private lateinit var searchProteinsUseCase: SearchProteinsUseCase
    private lateinit var toggleFavoriteUseCase: ToggleFavoriteUseCase
    private lateinit var viewModel: ProteinViewModel
    
    private val testDispatcher = StandardTestDispatcher()
    
    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        
        loadProteinUseCase = mockk()
        searchProteinsUseCase = mockk()
        toggleFavoriteUseCase = mockk()
        
        viewModel = ProteinViewModel(
            loadProteinUseCase = loadProteinUseCase,
            searchProteinsUseCase = searchProteinsUseCase,
            toggleFavoriteUseCase = toggleFavoriteUseCase
        )
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `loadProtein with success updates state correctly`() = runTest {
        // Given
        val proteinId = "1INS"
        val protein = createTestProtein(proteinId, "Insulin")
        coEvery { loadProteinUseCase(proteinId) } returns Result.success(protein)
        
        // When
        viewModel.loadProtein(proteinId)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(protein, state.currentProtein)
        assertNull(state.error)
    }
    
    @Test
    fun `loadProtein with failure updates error state`() = runTest {
        // Given
        val proteinId = "1INS"
        val errorMessage = "Network error"
        coEvery { loadProteinUseCase(proteinId) } returns Result.failure(Exception(errorMessage))
        
        // When
        viewModel.loadProtein(proteinId)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.currentProtein)
        assertEquals(errorMessage, state.error)
    }
    
    @Test
    fun `searchProteins with success updates search results`() = runTest {
        // Given
        val query = "insulin"
        val searchResults = listOf(
            createTestProtein("1INS", "Insulin"),
            createTestProtein("2INS", "Insulin variant")
        )
        coEvery { searchProteinsUseCase(query) } returns Result.success(searchResults)
        
        // When
        viewModel.searchProteins(query)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isSearching)
        assertEquals(searchResults, state.searchResults)
        assertNull(state.searchError)
    }
    
    @Test
    fun `searchProteins with failure updates search error`() = runTest {
        // Given
        val query = "insulin"
        val errorMessage = "Search failed"
        coEvery { searchProteinsUseCase(query) } returns Result.failure(Exception(errorMessage))
        
        // When
        viewModel.searchProteins(query)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isSearching)
        assertTrue(state.searchResults.isEmpty())
        assertEquals(errorMessage, state.searchError)
    }
    
    @Test
    fun `toggleFavorite with success updates protein favorite status`() = runTest {
        // Given
        val proteinId = "1INS"
        val protein = createTestProtein(proteinId, "Insulin")
        viewModel.loadProtein(proteinId)
        advanceUntilIdle()
        
        coEvery { toggleFavoriteUseCase(proteinId) } returns Result.success(true)
        
        // When
        viewModel.toggleFavorite(proteinId)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertTrue(state.currentProtein?.isFavorite == true)
    }
    
    @Test
    fun `clearError resets error states`() = runTest {
        // Given
        val proteinId = "1INS"
        coEvery { loadProteinUseCase(proteinId) } returns Result.failure(Exception("Error"))
        viewModel.loadProtein(proteinId)
        advanceUntilIdle()
        
        // When
        viewModel.clearError()
        
        // Then
        val state = viewModel.uiState.value
        assertNull(state.error)
        assertNull(state.searchError)
    }
    
    @Test
    fun `clearSearchResults resets search results`() = runTest {
        // Given
        val query = "insulin"
        val searchResults = listOf(createTestProtein("1INS", "Insulin"))
        coEvery { searchProteinsUseCase(query) } returns Result.success(searchResults)
        viewModel.searchProteins(query)
        advanceUntilIdle()
        
        // When
        viewModel.clearSearchResults()
        
        // Then
        val state = viewModel.uiState.value
        assertTrue(state.searchResults.isEmpty())
    }
    
    private fun createTestProtein(id: String, name: String): Protein {
        return Protein(
            id = id,
            name = name,
            description = "Test protein",
            organism = "Homo sapiens",
            molecularWeight = 1000f,
            resolution = 2.0f,
            experimentalMethod = "X-ray",
            depositionDate = "2023-01-01",
            spaceGroup = "P1",
            category = ProteinCategory.ENZYME,
            isFavorite = false,
            imagePath = null,
            structure = null
        )
    }
}


