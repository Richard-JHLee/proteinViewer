package com.avas.proteinviewer.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.testing.TestNavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.avas.proteinviewer.data.model.PDBStructure
import com.avas.proteinviewer.data.model.Atom
import com.avas.proteinviewer.data.model.Bond
import com.avas.proteinviewer.ui.main.MainScreen
import com.avas.proteinviewer.viewmodel.ProteinViewModel
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class MainScreenTest {
    
    @get:Rule
    val hiltRule = HiltAndroidRule(this)
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun mainScreen_displaysLoadingWhenLoading() {
        // Given
        val mockStructure = createMockStructure()
        
        // When
        composeTestRule.setContent {
            MainScreen(
                onNavigateToSearch = {},
                onNavigateToLibrary = {},
                onNavigateToInfo = {},
                onNavigateToAbout = {},
                onNavigateToUserGuide = {},
                onNavigateToFeatures = {},
                onNavigateToSettings = {},
                onNavigateToHelp = {},
                onNavigateToPrivacy = {},
                onNavigateToTerms = {},
                onNavigateToLicense = {},
                shouldShowDrawer = false
            )
        }
        
        // Then
        composeTestRule.onNodeWithText("Loading protein structure...")
            .assertIsDisplayed()
    }
    
    @Test
    fun mainScreen_displaysProteinWhenLoaded() {
        // Given
        val mockStructure = createMockStructure()
        
        // When
        composeTestRule.setContent {
            MainScreen(
                onNavigateToSearch = {},
                onNavigateToLibrary = {},
                onNavigateToInfo = {},
                onNavigateToAbout = {},
                onNavigateToUserGuide = {},
                onNavigateToFeatures = {},
                onNavigateToSettings = {},
                onNavigateToHelp = {},
                onNavigateToPrivacy = {},
                onNavigateToTerms = {},
                onNavigateToLicense = {},
                shouldShowDrawer = false
            )
        }
        
        // Then
        composeTestRule.onNodeWithText("1CRN")
            .assertIsDisplayed()
    }
    
    @Test
    fun mainScreen_displaysErrorWhenError() {
        // Given
        val errorMessage = "Network error"
        
        // When
        composeTestRule.setContent {
            MainScreen(
                onNavigateToSearch = {},
                onNavigateToLibrary = {},
                onNavigateToInfo = {},
                onNavigateToAbout = {},
                onNavigateToUserGuide = {},
                onNavigateToFeatures = {},
                onNavigateToSettings = {},
                onNavigateToHelp = {},
                onNavigateToPrivacy = {},
                onNavigateToTerms = {},
                onNavigateToLicense = {},
                shouldShowDrawer = false
            )
        }
        
        // Then
        composeTestRule.onNodeWithText("Error loading protein")
            .assertIsDisplayed()
    }
    
    @Test
    fun mainScreen_navigationButtonsWork() {
        // Given
        var searchClicked = false
        var libraryClicked = false
        var infoClicked = false
        
        // When
        composeTestRule.setContent {
            MainScreen(
                onNavigateToSearch = { searchClicked = true },
                onNavigateToLibrary = { libraryClicked = true },
                onNavigateToInfo = { infoClicked = true },
                onNavigateToAbout = {},
                onNavigateToUserGuide = {},
                onNavigateToFeatures = {},
                onNavigateToSettings = {},
                onNavigateToHelp = {},
                onNavigateToPrivacy = {},
                onNavigateToTerms = {},
                onNavigateToLicense = {},
                shouldShowDrawer = false
            )
        }
        
        // Then
        composeTestRule.onNodeWithContentDescription("Menu")
            .performClick()
        
        composeTestRule.onNodeWithText("Search")
            .performClick()
        assertTrue(searchClicked)
        
        composeTestRule.onNodeWithText("Library")
            .performClick()
        assertTrue(libraryClicked)
        
        composeTestRule.onNodeWithText("Info")
            .performClick()
        assertTrue(infoClicked)
    }
    
    @Test
    fun mainScreen_controlBarButtonsWork() {
        // Given
        composeTestRule.setContent {
            MainScreen(
                onNavigateToSearch = {},
                onNavigateToLibrary = {},
                onNavigateToInfo = {},
                onNavigateToAbout = {},
                onNavigateToUserGuide = {},
                onNavigateToFeatures = {},
                onNavigateToSettings = {},
                onNavigateToHelp = {},
                onNavigateToPrivacy = {},
                onNavigateToTerms = {},
                onNavigateToLicense = {},
                shouldShowDrawer = false
            )
        }
        
        // When & Then
        composeTestRule.onNodeWithText("Spheres")
            .assertIsDisplayed()
            .performClick()
        
        composeTestRule.onNodeWithText("Sticks")
            .assertIsDisplayed()
            .performClick()
        
        composeTestRule.onNodeWithText("Cartoon")
            .assertIsDisplayed()
            .performClick()
        
        composeTestRule.onNodeWithText("Element")
            .assertIsDisplayed()
            .performClick()
        
        composeTestRule.onNodeWithText("Chain")
            .assertIsDisplayed()
            .performClick()
    }
    
    @Test
    fun mainScreen_animationControlsWork() {
        // Given
        composeTestRule.setContent {
            MainScreen(
                onNavigateToSearch = {},
                onNavigateToLibrary = {},
                onNavigateToInfo = {},
                onNavigateToAbout = {},
                onNavigateToUserGuide = {},
                onNavigateToFeatures = {},
                onNavigateToSettings = {},
                onNavigateToHelp = {},
                onNavigateToPrivacy = {},
                onNavigateToTerms = {},
                onNavigateToLicense = {},
                shouldShowDrawer = false
            )
        }
        
        // When & Then
        composeTestRule.onNodeWithContentDescription("Play")
            .performClick()
        
        composeTestRule.onNodeWithContentDescription("Pause")
            .performClick()
        
        composeTestRule.onNodeWithContentDescription("Next Step")
            .performClick()
        
        composeTestRule.onNodeWithContentDescription("Previous Step")
            .performClick()
    }
    
    private fun createMockStructure(): PDBStructure {
        return PDBStructure(
            atoms = listOf(
                Atom(0, "N", "ALA", "A", 1, 20.154f, 16.967f, 23.862f, 1.0f, 11.18f, "N"),
                Atom(1, "CA", "ALA", "A", 1, 19.030f, 16.038f, 23.456f, 1.0f, 10.35f, "C"),
                Atom(2, "C", "ALA", "A", 1, 17.680f, 16.647f, 23.456f, 1.0f, 9.93f, "C"),
                Atom(3, "O", "ALA", "A", 1, 17.380f, 17.456f, 24.456f, 1.0f, 9.93f, "O")
            ),
            bonds = listOf(
                Bond(0, 1, 1.5f),
                Bond(1, 2, 1.5f),
                Bond(2, 3, 1.2f)
            ),
            annotations = emptyList(),
            title = "Test Protein",
            boundingBox = null,
            centerOfMass = null
        )
    }
}


import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.testing.TestNavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.avas.proteinviewer.data.model.PDBStructure
import com.avas.proteinviewer.data.model.Atom
import com.avas.proteinviewer.data.model.Bond
import com.avas.proteinviewer.ui.main.MainScreen
import com.avas.proteinviewer.viewmodel.ProteinViewModel
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class MainScreenTest {
    
    @get:Rule
    val hiltRule = HiltAndroidRule(this)
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun mainScreen_displaysLoadingWhenLoading() {
        // Given
        val mockStructure = createMockStructure()
        
        // When
        composeTestRule.setContent {
            MainScreen(
                onNavigateToSearch = {},
                onNavigateToLibrary = {},
                onNavigateToInfo = {},
                onNavigateToAbout = {},
                onNavigateToUserGuide = {},
                onNavigateToFeatures = {},
                onNavigateToSettings = {},
                onNavigateToHelp = {},
                onNavigateToPrivacy = {},
                onNavigateToTerms = {},
                onNavigateToLicense = {},
                shouldShowDrawer = false
            )
        }
        
        // Then
        composeTestRule.onNodeWithText("Loading protein structure...")
            .assertIsDisplayed()
    }
    
    @Test
    fun mainScreen_displaysProteinWhenLoaded() {
        // Given
        val mockStructure = createMockStructure()
        
        // When
        composeTestRule.setContent {
            MainScreen(
                onNavigateToSearch = {},
                onNavigateToLibrary = {},
                onNavigateToInfo = {},
                onNavigateToAbout = {},
                onNavigateToUserGuide = {},
                onNavigateToFeatures = {},
                onNavigateToSettings = {},
                onNavigateToHelp = {},
                onNavigateToPrivacy = {},
                onNavigateToTerms = {},
                onNavigateToLicense = {},
                shouldShowDrawer = false
            )
        }
        
        // Then
        composeTestRule.onNodeWithText("1CRN")
            .assertIsDisplayed()
    }
    
    @Test
    fun mainScreen_displaysErrorWhenError() {
        // Given
        val errorMessage = "Network error"
        
        // When
        composeTestRule.setContent {
            MainScreen(
                onNavigateToSearch = {},
                onNavigateToLibrary = {},
                onNavigateToInfo = {},
                onNavigateToAbout = {},
                onNavigateToUserGuide = {},
                onNavigateToFeatures = {},
                onNavigateToSettings = {},
                onNavigateToHelp = {},
                onNavigateToPrivacy = {},
                onNavigateToTerms = {},
                onNavigateToLicense = {},
                shouldShowDrawer = false
            )
        }
        
        // Then
        composeTestRule.onNodeWithText("Error loading protein")
            .assertIsDisplayed()
    }
    
    @Test
    fun mainScreen_navigationButtonsWork() {
        // Given
        var searchClicked = false
        var libraryClicked = false
        var infoClicked = false
        
        // When
        composeTestRule.setContent {
            MainScreen(
                onNavigateToSearch = { searchClicked = true },
                onNavigateToLibrary = { libraryClicked = true },
                onNavigateToInfo = { infoClicked = true },
                onNavigateToAbout = {},
                onNavigateToUserGuide = {},
                onNavigateToFeatures = {},
                onNavigateToSettings = {},
                onNavigateToHelp = {},
                onNavigateToPrivacy = {},
                onNavigateToTerms = {},
                onNavigateToLicense = {},
                shouldShowDrawer = false
            )
        }
        
        // Then
        composeTestRule.onNodeWithContentDescription("Menu")
            .performClick()
        
        composeTestRule.onNodeWithText("Search")
            .performClick()
        assertTrue(searchClicked)
        
        composeTestRule.onNodeWithText("Library")
            .performClick()
        assertTrue(libraryClicked)
        
        composeTestRule.onNodeWithText("Info")
            .performClick()
        assertTrue(infoClicked)
    }
    
    @Test
    fun mainScreen_controlBarButtonsWork() {
        // Given
        composeTestRule.setContent {
            MainScreen(
                onNavigateToSearch = {},
                onNavigateToLibrary = {},
                onNavigateToInfo = {},
                onNavigateToAbout = {},
                onNavigateToUserGuide = {},
                onNavigateToFeatures = {},
                onNavigateToSettings = {},
                onNavigateToHelp = {},
                onNavigateToPrivacy = {},
                onNavigateToTerms = {},
                onNavigateToLicense = {},
                shouldShowDrawer = false
            )
        }
        
        // When & Then
        composeTestRule.onNodeWithText("Spheres")
            .assertIsDisplayed()
            .performClick()
        
        composeTestRule.onNodeWithText("Sticks")
            .assertIsDisplayed()
            .performClick()
        
        composeTestRule.onNodeWithText("Cartoon")
            .assertIsDisplayed()
            .performClick()
        
        composeTestRule.onNodeWithText("Element")
            .assertIsDisplayed()
            .performClick()
        
        composeTestRule.onNodeWithText("Chain")
            .assertIsDisplayed()
            .performClick()
    }
    
    @Test
    fun mainScreen_animationControlsWork() {
        // Given
        composeTestRule.setContent {
            MainScreen(
                onNavigateToSearch = {},
                onNavigateToLibrary = {},
                onNavigateToInfo = {},
                onNavigateToAbout = {},
                onNavigateToUserGuide = {},
                onNavigateToFeatures = {},
                onNavigateToSettings = {},
                onNavigateToHelp = {},
                onNavigateToPrivacy = {},
                onNavigateToTerms = {},
                onNavigateToLicense = {},
                shouldShowDrawer = false
            )
        }
        
        // When & Then
        composeTestRule.onNodeWithContentDescription("Play")
            .performClick()
        
        composeTestRule.onNodeWithContentDescription("Pause")
            .performClick()
        
        composeTestRule.onNodeWithContentDescription("Next Step")
            .performClick()
        
        composeTestRule.onNodeWithContentDescription("Previous Step")
            .performClick()
    }
    
    private fun createMockStructure(): PDBStructure {
        return PDBStructure(
            atoms = listOf(
                Atom(0, "N", "ALA", "A", 1, 20.154f, 16.967f, 23.862f, 1.0f, 11.18f, "N"),
                Atom(1, "CA", "ALA", "A", 1, 19.030f, 16.038f, 23.456f, 1.0f, 10.35f, "C"),
                Atom(2, "C", "ALA", "A", 1, 17.680f, 16.647f, 23.456f, 1.0f, 9.93f, "C"),
                Atom(3, "O", "ALA", "A", 1, 17.380f, 17.456f, 24.456f, 1.0f, 9.93f, "O")
            ),
            bonds = listOf(
                Bond(0, 1, 1.5f),
                Bond(1, 2, 1.5f),
                Bond(2, 3, 1.2f)
            ),
            annotations = emptyList(),
            title = "Test Protein",
            boundingBox = null,
            centerOfMass = null
        )
    }
}


