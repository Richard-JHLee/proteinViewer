package com.avas.proteinviewer.presentation.ui.components

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.avas.proteinviewer.core.domain.model.Protein
import com.avas.proteinviewer.core.domain.model.ProteinCategory
import com.avas.proteinviewer.presentation.ui.design.DesignSystem
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProteinCardTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun proteinCard_displaysProteinName() {
        // Given
        val protein = createTestProtein("1INS", "Insulin")
        var clicked = false
        
        // When
        composeTestRule.setContent {
            DesignSystem.Material3Theme {
                ProteinCard(
                    protein = protein,
                    onClick = { clicked = true }
                )
            }
        }
        
        // Then
        composeTestRule.onNodeWithText("Insulin").assertIsDisplayed()
        composeTestRule.onNodeWithText("ID: 1INS").assertIsDisplayed()
    }
    
    @Test
    fun proteinCard_displaysProteinDescription() {
        // Given
        val protein = createTestProtein("1INS", "Insulin", "A hormone that regulates blood sugar")
        var clicked = false
        
        // When
        composeTestRule.setContent {
            DesignSystem.Material3Theme {
                ProteinCard(
                    protein = protein,
                    onClick = { clicked = true }
                )
            }
        }
        
        // Then
        composeTestRule.onNodeWithText("A hormone that regulates blood sugar").assertIsDisplayed()
    }
    
    @Test
    fun proteinCard_displaysProteinMetadata() {
        // Given
        val protein = createTestProtein("1INS", "Insulin")
        var clicked = false
        
        // When
        composeTestRule.setContent {
            DesignSystem.Material3Theme {
                ProteinCard(
                    protein = protein,
                    onClick = { clicked = true }
                )
            }
        }
        
        // Then
        composeTestRule.onNodeWithText("Category").assertIsDisplayed()
        composeTestRule.onNodeWithText("Resolution").assertIsDisplayed()
        composeTestRule.onNodeWithText("Method").assertIsDisplayed()
        composeTestRule.onNodeWithText("Molecular Weight").assertIsDisplayed()
        composeTestRule.onNodeWithText("Organism").assertIsDisplayed()
    }
    
    @Test
    fun proteinCard_showsFavoriteIcon() {
        // Given
        val protein = createTestProtein("1INS", "Insulin", isFavorite = true)
        var clicked = false
        var favoriteClicked = false
        
        // When
        composeTestRule.setContent {
            DesignSystem.Material3Theme {
                ProteinCard(
                    protein = protein,
                    onClick = { clicked = true },
                    onFavoriteClick = { favoriteClicked = true }
                )
            }
        }
        
        // Then
        composeTestRule.onNodeWithContentDescription("Remove from favorites").assertIsDisplayed()
    }
    
    @Test
    fun proteinCard_hidesFavoriteIconWhenNotShown() {
        // Given
        val protein = createTestProtein("1INS", "Insulin")
        var clicked = false
        
        // When
        composeTestRule.setContent {
            DesignSystem.Material3Theme {
                ProteinCard(
                    protein = protein,
                    onClick = { clicked = true },
                    showFavorite = false
                )
            }
        }
        
        // Then
        composeTestRule.onNodeWithContentDescription("Add to favorites").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Remove from favorites").assertDoesNotExist()
    }
    
    @Test
    fun proteinCard_callsOnClickWhenTapped() {
        // Given
        val protein = createTestProtein("1INS", "Insulin")
        var clicked = false
        
        // When
        composeTestRule.setContent {
            DesignSystem.Material3Theme {
                ProteinCard(
                    protein = protein,
                    onClick = { clicked = true }
                )
            }
        }
        
        composeTestRule.onNodeWithText("Insulin").performClick()
        
        // Then
        assertTrue(clicked)
    }
    
    @Test
    fun proteinCard_callsOnFavoriteClickWhenFavoriteTapped() {
        // Given
        val protein = createTestProtein("1INS", "Insulin")
        var clicked = false
        var favoriteClicked = false
        
        // When
        composeTestRule.setContent {
            DesignSystem.Material3Theme {
                ProteinCard(
                    protein = protein,
                    onClick = { clicked = true },
                    onFavoriteClick = { favoriteClicked = true }
                )
            }
        }
        
        composeTestRule.onNodeWithContentDescription("Add to favorites").performClick()
        
        // Then
        assertTrue(favoriteClicked)
        assertFalse(clicked) // 메인 카드 클릭은 발생하지 않아야 함
    }
    
    @Test
    fun proteinCard_hidesMetadataWhenDisabled() {
        // Given
        val protein = createTestProtein("1INS", "Insulin")
        var clicked = false
        
        // When
        composeTestRule.setContent {
            DesignSystem.Material3Theme {
                ProteinCard(
                    protein = protein,
                    onClick = { clicked = true },
                    showCategory = false,
                    showResolution = false,
                    showMethod = false
                )
            }
        }
        
        // Then
        composeTestRule.onNodeWithText("Category").assertDoesNotExist()
        composeTestRule.onNodeWithText("Resolution").assertDoesNotExist()
        composeTestRule.onNodeWithText("Method").assertDoesNotExist()
    }
    
    private fun createTestProtein(
        id: String,
        name: String,
        description: String = "Test protein",
        isFavorite: Boolean = false
    ): Protein {
        return Protein(
            id = id,
            name = name,
            description = description,
            organism = "Homo sapiens",
            molecularWeight = 1000f,
            resolution = 2.0f,
            experimentalMethod = "X-ray",
            depositionDate = "2023-01-01",
            spaceGroup = "P1",
            category = ProteinCategory.ENZYME,
            isFavorite = isFavorite,
            imagePath = null,
            structure = null
        )
    }
}


import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.avas.proteinviewer.core.domain.model.Protein
import com.avas.proteinviewer.core.domain.model.ProteinCategory
import com.avas.proteinviewer.presentation.ui.design.DesignSystem
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProteinCardTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun proteinCard_displaysProteinName() {
        // Given
        val protein = createTestProtein("1INS", "Insulin")
        var clicked = false
        
        // When
        composeTestRule.setContent {
            DesignSystem.Material3Theme {
                ProteinCard(
                    protein = protein,
                    onClick = { clicked = true }
                )
            }
        }
        
        // Then
        composeTestRule.onNodeWithText("Insulin").assertIsDisplayed()
        composeTestRule.onNodeWithText("ID: 1INS").assertIsDisplayed()
    }
    
    @Test
    fun proteinCard_displaysProteinDescription() {
        // Given
        val protein = createTestProtein("1INS", "Insulin", "A hormone that regulates blood sugar")
        var clicked = false
        
        // When
        composeTestRule.setContent {
            DesignSystem.Material3Theme {
                ProteinCard(
                    protein = protein,
                    onClick = { clicked = true }
                )
            }
        }
        
        // Then
        composeTestRule.onNodeWithText("A hormone that regulates blood sugar").assertIsDisplayed()
    }
    
    @Test
    fun proteinCard_displaysProteinMetadata() {
        // Given
        val protein = createTestProtein("1INS", "Insulin")
        var clicked = false
        
        // When
        composeTestRule.setContent {
            DesignSystem.Material3Theme {
                ProteinCard(
                    protein = protein,
                    onClick = { clicked = true }
                )
            }
        }
        
        // Then
        composeTestRule.onNodeWithText("Category").assertIsDisplayed()
        composeTestRule.onNodeWithText("Resolution").assertIsDisplayed()
        composeTestRule.onNodeWithText("Method").assertIsDisplayed()
        composeTestRule.onNodeWithText("Molecular Weight").assertIsDisplayed()
        composeTestRule.onNodeWithText("Organism").assertIsDisplayed()
    }
    
    @Test
    fun proteinCard_showsFavoriteIcon() {
        // Given
        val protein = createTestProtein("1INS", "Insulin", isFavorite = true)
        var clicked = false
        var favoriteClicked = false
        
        // When
        composeTestRule.setContent {
            DesignSystem.Material3Theme {
                ProteinCard(
                    protein = protein,
                    onClick = { clicked = true },
                    onFavoriteClick = { favoriteClicked = true }
                )
            }
        }
        
        // Then
        composeTestRule.onNodeWithContentDescription("Remove from favorites").assertIsDisplayed()
    }
    
    @Test
    fun proteinCard_hidesFavoriteIconWhenNotShown() {
        // Given
        val protein = createTestProtein("1INS", "Insulin")
        var clicked = false
        
        // When
        composeTestRule.setContent {
            DesignSystem.Material3Theme {
                ProteinCard(
                    protein = protein,
                    onClick = { clicked = true },
                    showFavorite = false
                )
            }
        }
        
        // Then
        composeTestRule.onNodeWithContentDescription("Add to favorites").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Remove from favorites").assertDoesNotExist()
    }
    
    @Test
    fun proteinCard_callsOnClickWhenTapped() {
        // Given
        val protein = createTestProtein("1INS", "Insulin")
        var clicked = false
        
        // When
        composeTestRule.setContent {
            DesignSystem.Material3Theme {
                ProteinCard(
                    protein = protein,
                    onClick = { clicked = true }
                )
            }
        }
        
        composeTestRule.onNodeWithText("Insulin").performClick()
        
        // Then
        assertTrue(clicked)
    }
    
    @Test
    fun proteinCard_callsOnFavoriteClickWhenFavoriteTapped() {
        // Given
        val protein = createTestProtein("1INS", "Insulin")
        var clicked = false
        var favoriteClicked = false
        
        // When
        composeTestRule.setContent {
            DesignSystem.Material3Theme {
                ProteinCard(
                    protein = protein,
                    onClick = { clicked = true },
                    onFavoriteClick = { favoriteClicked = true }
                )
            }
        }
        
        composeTestRule.onNodeWithContentDescription("Add to favorites").performClick()
        
        // Then
        assertTrue(favoriteClicked)
        assertFalse(clicked) // 메인 카드 클릭은 발생하지 않아야 함
    }
    
    @Test
    fun proteinCard_hidesMetadataWhenDisabled() {
        // Given
        val protein = createTestProtein("1INS", "Insulin")
        var clicked = false
        
        // When
        composeTestRule.setContent {
            DesignSystem.Material3Theme {
                ProteinCard(
                    protein = protein,
                    onClick = { clicked = true },
                    showCategory = false,
                    showResolution = false,
                    showMethod = false
                )
            }
        }
        
        // Then
        composeTestRule.onNodeWithText("Category").assertDoesNotExist()
        composeTestRule.onNodeWithText("Resolution").assertDoesNotExist()
        composeTestRule.onNodeWithText("Method").assertDoesNotExist()
    }
    
    private fun createTestProtein(
        id: String,
        name: String,
        description: String = "Test protein",
        isFavorite: Boolean = false
    ): Protein {
        return Protein(
            id = id,
            name = name,
            description = description,
            organism = "Homo sapiens",
            molecularWeight = 1000f,
            resolution = 2.0f,
            experimentalMethod = "X-ray",
            depositionDate = "2023-01-01",
            spaceGroup = "P1",
            category = ProteinCategory.ENZYME,
            isFavorite = isFavorite,
            imagePath = null,
            structure = null
        )
    }
}


