package com.avas.proteinviewer.core.domain.usecase

import com.avas.proteinviewer.core.domain.model.Protein
import com.avas.proteinviewer.core.domain.model.ProteinCategory
import com.avas.proteinviewer.core.domain.repository.ProteinRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SearchProteinsUseCaseTest {
    
    private lateinit var repository: ProteinRepository
    private lateinit var useCase: SearchProteinsUseCase
    
    @Before
    fun setUp() {
        repository = mockk()
        useCase = SearchProteinsUseCase(repository)
    }
    
    @Test
    fun `searchProteins with valid query returns success`() = runTest {
        // Given
        val query = "insulin"
        val expectedProteins = listOf(
            createTestProtein("1INS", "Insulin"),
            createTestProtein("2INS", "Insulin variant")
        )
        coEvery { repository.searchProteins(query) } returns Result.success(expectedProteins)
        
        // When
        val result = useCase(query)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedProteins, result.getOrNull())
    }
    
    @Test
    fun `searchProteins with empty query returns empty list`() = runTest {
        // Given
        val query = ""
        
        // When
        val result = useCase(query)
        
        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.isEmpty() == true)
    }
    
    @Test
    fun `searchProteins with blank query returns empty list`() = runTest {
        // Given
        val query = "   "
        
        // When
        val result = useCase(query)
        
        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.isEmpty() == true)
    }
    
    @Test
    fun `searchProteins with repository failure returns failure`() = runTest {
        // Given
        val query = "insulin"
        val exception = Exception("Network error")
        coEvery { repository.searchProteins(query) } returns Result.failure(exception)
        
        // When
        val result = useCase(query)
        
        // Then
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }
    
    @Test
    fun `searchProteins trims query`() = runTest {
        // Given
        val query = "  insulin  "
        val trimmedQuery = "insulin"
        val expectedProteins = listOf(createTestProtein("1INS", "Insulin"))
        coEvery { repository.searchProteins(trimmedQuery) } returns Result.success(expectedProteins)
        
        // When
        val result = useCase(query)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedProteins, result.getOrNull())
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
import com.avas.proteinviewer.core.domain.repository.ProteinRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SearchProteinsUseCaseTest {
    
    private lateinit var repository: ProteinRepository
    private lateinit var useCase: SearchProteinsUseCase
    
    @Before
    fun setUp() {
        repository = mockk()
        useCase = SearchProteinsUseCase(repository)
    }
    
    @Test
    fun `searchProteins with valid query returns success`() = runTest {
        // Given
        val query = "insulin"
        val expectedProteins = listOf(
            createTestProtein("1INS", "Insulin"),
            createTestProtein("2INS", "Insulin variant")
        )
        coEvery { repository.searchProteins(query) } returns Result.success(expectedProteins)
        
        // When
        val result = useCase(query)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedProteins, result.getOrNull())
    }
    
    @Test
    fun `searchProteins with empty query returns empty list`() = runTest {
        // Given
        val query = ""
        
        // When
        val result = useCase(query)
        
        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.isEmpty() == true)
    }
    
    @Test
    fun `searchProteins with blank query returns empty list`() = runTest {
        // Given
        val query = "   "
        
        // When
        val result = useCase(query)
        
        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.isEmpty() == true)
    }
    
    @Test
    fun `searchProteins with repository failure returns failure`() = runTest {
        // Given
        val query = "insulin"
        val exception = Exception("Network error")
        coEvery { repository.searchProteins(query) } returns Result.failure(exception)
        
        // When
        val result = useCase(query)
        
        // Then
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }
    
    @Test
    fun `searchProteins trims query`() = runTest {
        // Given
        val query = "  insulin  "
        val trimmedQuery = "insulin"
        val expectedProteins = listOf(createTestProtein("1INS", "Insulin"))
        coEvery { repository.searchProteins(trimmedQuery) } returns Result.success(expectedProteins)
        
        // When
        val result = useCase(query)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedProteins, result.getOrNull())
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


