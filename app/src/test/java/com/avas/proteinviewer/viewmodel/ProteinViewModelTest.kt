package com.avas.proteinviewer.viewmodel

import com.avas.proteinviewer.data.model.PDBStructure
import com.avas.proteinviewer.data.model.Atom
import com.avas.proteinviewer.data.model.Bond
import com.avas.proteinviewer.data.repository.ProteinRepository
import com.avas.proteinviewer.ui.state.AppState
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@ExperimentalCoroutinesApi
class ProteinViewModelTest {
    
    private lateinit var viewModel: ProteinViewModel
    private lateinit var mockRepository: ProteinRepository
    private val testDispatcher = UnconfinedTestDispatcher()
    
    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockRepository = mockk()
        viewModel = ProteinViewModel(mockRepository)
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `loadDefaultProtein should load 1CRN protein`() = runTest {
        // Given
        val mockStructure = createMockStructure()
        coEvery { mockRepository.loadProteinStructure("1CRN") } returns Result.success(mockStructure)
        coEvery { mockRepository.getProteinMetadata("1CRN") } returns Result.success(null)
        
        // When
        viewModel.loadDefaultProtein()
        
        // Then
        val structure = viewModel.structure.value
        assertNotNull(structure)
        assertEquals("1CRN", viewModel.currentProteinId.value)
        assertEquals("Test Protein", viewModel.currentProteinName.value)
    }
    
    @Test
    fun `loadSelectedProtein should load specified protein`() = runTest {
        // Given
        val proteinId = "1ABC"
        val mockStructure = createMockStructure()
        coEvery { mockRepository.loadProteinStructure(proteinId) } returns Result.success(mockStructure)
        coEvery { mockRepository.getProteinMetadata(proteinId) } returns Result.success(null)
        
        // When
        viewModel.loadSelectedProtein(proteinId)
        
        // Then
        val structure = viewModel.structure.value
        assertNotNull(structure)
        assertEquals(proteinId, viewModel.currentProteinId.value)
    }
    
    @Test
    fun `searchProteinByPDBId should add protein to search results`() = runTest {
        // Given
        val pdbId = "1ABC"
        coEvery { mockRepository.searchByPDBId(pdbId) } returns Result.success(true)
        
        // When
        viewModel.searchProteinByPDBId(pdbId)
        
        // Then
        val searchResults = viewModel.searchResults.value
        assertTrue(searchResults.isNotEmpty())
        assertEquals(pdbId, searchResults.first().pdbId)
    }
    
    @Test
    fun `updateProteinStyle should update app state`() {
        // Given
        val newStyle = com.avas.proteinviewer.ui.protein.ProteinStyle.Sticks
        
        // When
        viewModel.updateProteinStyle(newStyle)
        
        // Then
        val appState = viewModel.appState.value
        assertEquals(newStyle, appState.proteinStyle)
    }
    
    @Test
    fun `updateColorMode should update app state`() {
        // Given
        val newColorMode = com.avas.proteinviewer.ui.state.ColorMode.CHAIN
        
        // When
        viewModel.updateColorMode(newColorMode)
        
        // Then
        val appState = viewModel.appState.value
        assertEquals(newColorMode, appState.colorMode)
    }
    
    @Test
    fun `startAnimation should set isAnimating to true`() {
        // When
        viewModel.startAnimation()
        
        // Then
        val appState = viewModel.appState.value
        assertTrue(appState.isAnimating)
    }
    
    @Test
    fun `pauseAnimation should set isAnimating to false`() {
        // Given
        viewModel.startAnimation()
        
        // When
        viewModel.pauseAnimation()
        
        // Then
        val appState = viewModel.appState.value
        assertFalse(appState.isAnimating)
    }
    
    @Test
    fun `nextAnimationStep should increment step`() {
        // Given
        val initialStep = viewModel.appState.value.animationStep
        
        // When
        viewModel.nextAnimationStep()
        
        // Then
        val appState = viewModel.appState.value
        assertEquals(initialStep + 1, appState.animationStep)
    }
    
    @Test
    fun `previousAnimationStep should decrement step`() {
        // Given
        viewModel.nextAnimationStep()
        val currentStep = viewModel.appState.value.animationStep
        
        // When
        viewModel.previousAnimationStep()
        
        // Then
        val appState = viewModel.appState.value
        assertEquals(currentStep - 1, appState.animationStep)
    }
    
    @Test
    fun `highlightChain should add chain to highlighted chains`() {
        // Given
        val chainId = "A"
        
        // When
        viewModel.highlightChain(chainId)
        
        // Then
        val appState = viewModel.appState.value
        assertTrue(appState.highlightedChains.contains(chainId))
    }
    
    @Test
    fun `unhighlightChain should remove chain from highlighted chains`() {
        // Given
        val chainId = "A"
        viewModel.highlightChain(chainId)
        
        // When
        viewModel.unhighlightChain(chainId)
        
        // Then
        val appState = viewModel.appState.value
        assertFalse(appState.highlightedChains.contains(chainId))
    }
    
    @Test
    fun `clearAllHighlights should clear all highlights`() {
        // Given
        viewModel.highlightChain("A")
        viewModel.highlightChain("B")
        
        // When
        viewModel.clearAllHighlights()
        
        // Then
        val appState = viewModel.appState.value
        assertTrue(appState.highlightedChains.isEmpty())
        assertTrue(appState.highlightedPockets.isEmpty())
        assertNull(appState.focusedChain)
        assertNull(appState.focusedPocket)
    }
    
    @Test
    fun `toggleARMode should toggle AR mode`() {
        // Given
        val initialARMode = viewModel.appState.value.arMode
        
        // When
        viewModel.toggleARMode()
        
        // Then
        val appState = viewModel.appState.value
        assertEquals(!initialARMode, appState.arMode)
    }
    
    @Test
    fun `updateARSettings should update AR settings`() {
        // Given
        val surfaceMode = true
        val cartoonMode = false
        val scale = 2.0f
        
        // When
        viewModel.updateARSettings(surfaceMode, cartoonMode, scale)
        
        // Then
        val appState = viewModel.appState.value
        assertEquals(surfaceMode, appState.arSurfaceMode)
        assertEquals(cartoonMode, appState.arCartoonMode)
        assertEquals(scale, appState.arScale)
    }
    
    private fun createMockStructure(): PDBStructure {
        return PDBStructure(
            atoms = listOf(
                Atom(0, "N", "ALA", "A", 1, 20.154f, 16.967f, 23.862f, 1.0f, 11.18f, "N"),
                Atom(1, "CA", "ALA", "A", 1, 19.030f, 16.038f, 23.456f, 1.0f, 10.35f, "C")
            ),
            bonds = listOf(
                Bond(0, 1, 1.5f)
            ),
            annotations = emptyList(),
            title = "Test Protein",
            boundingBox = null,
            centerOfMass = null
        )
    }
}


import com.avas.proteinviewer.data.model.PDBStructure
import com.avas.proteinviewer.data.model.Atom
import com.avas.proteinviewer.data.model.Bond
import com.avas.proteinviewer.data.repository.ProteinRepository
import com.avas.proteinviewer.ui.state.AppState
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@ExperimentalCoroutinesApi
class ProteinViewModelTest {
    
    private lateinit var viewModel: ProteinViewModel
    private lateinit var mockRepository: ProteinRepository
    private val testDispatcher = UnconfinedTestDispatcher()
    
    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockRepository = mockk()
        viewModel = ProteinViewModel(mockRepository)
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `loadDefaultProtein should load 1CRN protein`() = runTest {
        // Given
        val mockStructure = createMockStructure()
        coEvery { mockRepository.loadProteinStructure("1CRN") } returns Result.success(mockStructure)
        coEvery { mockRepository.getProteinMetadata("1CRN") } returns Result.success(null)
        
        // When
        viewModel.loadDefaultProtein()
        
        // Then
        val structure = viewModel.structure.value
        assertNotNull(structure)
        assertEquals("1CRN", viewModel.currentProteinId.value)
        assertEquals("Test Protein", viewModel.currentProteinName.value)
    }
    
    @Test
    fun `loadSelectedProtein should load specified protein`() = runTest {
        // Given
        val proteinId = "1ABC"
        val mockStructure = createMockStructure()
        coEvery { mockRepository.loadProteinStructure(proteinId) } returns Result.success(mockStructure)
        coEvery { mockRepository.getProteinMetadata(proteinId) } returns Result.success(null)
        
        // When
        viewModel.loadSelectedProtein(proteinId)
        
        // Then
        val structure = viewModel.structure.value
        assertNotNull(structure)
        assertEquals(proteinId, viewModel.currentProteinId.value)
    }
    
    @Test
    fun `searchProteinByPDBId should add protein to search results`() = runTest {
        // Given
        val pdbId = "1ABC"
        coEvery { mockRepository.searchByPDBId(pdbId) } returns Result.success(true)
        
        // When
        viewModel.searchProteinByPDBId(pdbId)
        
        // Then
        val searchResults = viewModel.searchResults.value
        assertTrue(searchResults.isNotEmpty())
        assertEquals(pdbId, searchResults.first().pdbId)
    }
    
    @Test
    fun `updateProteinStyle should update app state`() {
        // Given
        val newStyle = com.avas.proteinviewer.ui.protein.ProteinStyle.Sticks
        
        // When
        viewModel.updateProteinStyle(newStyle)
        
        // Then
        val appState = viewModel.appState.value
        assertEquals(newStyle, appState.proteinStyle)
    }
    
    @Test
    fun `updateColorMode should update app state`() {
        // Given
        val newColorMode = com.avas.proteinviewer.ui.state.ColorMode.CHAIN
        
        // When
        viewModel.updateColorMode(newColorMode)
        
        // Then
        val appState = viewModel.appState.value
        assertEquals(newColorMode, appState.colorMode)
    }
    
    @Test
    fun `startAnimation should set isAnimating to true`() {
        // When
        viewModel.startAnimation()
        
        // Then
        val appState = viewModel.appState.value
        assertTrue(appState.isAnimating)
    }
    
    @Test
    fun `pauseAnimation should set isAnimating to false`() {
        // Given
        viewModel.startAnimation()
        
        // When
        viewModel.pauseAnimation()
        
        // Then
        val appState = viewModel.appState.value
        assertFalse(appState.isAnimating)
    }
    
    @Test
    fun `nextAnimationStep should increment step`() {
        // Given
        val initialStep = viewModel.appState.value.animationStep
        
        // When
        viewModel.nextAnimationStep()
        
        // Then
        val appState = viewModel.appState.value
        assertEquals(initialStep + 1, appState.animationStep)
    }
    
    @Test
    fun `previousAnimationStep should decrement step`() {
        // Given
        viewModel.nextAnimationStep()
        val currentStep = viewModel.appState.value.animationStep
        
        // When
        viewModel.previousAnimationStep()
        
        // Then
        val appState = viewModel.appState.value
        assertEquals(currentStep - 1, appState.animationStep)
    }
    
    @Test
    fun `highlightChain should add chain to highlighted chains`() {
        // Given
        val chainId = "A"
        
        // When
        viewModel.highlightChain(chainId)
        
        // Then
        val appState = viewModel.appState.value
        assertTrue(appState.highlightedChains.contains(chainId))
    }
    
    @Test
    fun `unhighlightChain should remove chain from highlighted chains`() {
        // Given
        val chainId = "A"
        viewModel.highlightChain(chainId)
        
        // When
        viewModel.unhighlightChain(chainId)
        
        // Then
        val appState = viewModel.appState.value
        assertFalse(appState.highlightedChains.contains(chainId))
    }
    
    @Test
    fun `clearAllHighlights should clear all highlights`() {
        // Given
        viewModel.highlightChain("A")
        viewModel.highlightChain("B")
        
        // When
        viewModel.clearAllHighlights()
        
        // Then
        val appState = viewModel.appState.value
        assertTrue(appState.highlightedChains.isEmpty())
        assertTrue(appState.highlightedPockets.isEmpty())
        assertNull(appState.focusedChain)
        assertNull(appState.focusedPocket)
    }
    
    @Test
    fun `toggleARMode should toggle AR mode`() {
        // Given
        val initialARMode = viewModel.appState.value.arMode
        
        // When
        viewModel.toggleARMode()
        
        // Then
        val appState = viewModel.appState.value
        assertEquals(!initialARMode, appState.arMode)
    }
    
    @Test
    fun `updateARSettings should update AR settings`() {
        // Given
        val surfaceMode = true
        val cartoonMode = false
        val scale = 2.0f
        
        // When
        viewModel.updateARSettings(surfaceMode, cartoonMode, scale)
        
        // Then
        val appState = viewModel.appState.value
        assertEquals(surfaceMode, appState.arSurfaceMode)
        assertEquals(cartoonMode, appState.arCartoonMode)
        assertEquals(scale, appState.arScale)
    }
    
    private fun createMockStructure(): PDBStructure {
        return PDBStructure(
            atoms = listOf(
                Atom(0, "N", "ALA", "A", 1, 20.154f, 16.967f, 23.862f, 1.0f, 11.18f, "N"),
                Atom(1, "CA", "ALA", "A", 1, 19.030f, 16.038f, 23.456f, 1.0f, 10.35f, "C")
            ),
            bonds = listOf(
                Bond(0, 1, 1.5f)
            ),
            annotations = emptyList(),
            title = "Test Protein",
            boundingBox = null,
            centerOfMass = null
        )
    }
}


