package com.avas.proteinviewer.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.avas.proteinviewer.ui.search.SearchScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SearchScreenTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun searchScreen_displaysSearchButton() {
        // When
        composeTestRule.setContent {
            SearchScreen(
                onNavigateBack = {}
            )
        }
        
        // Then
        composeTestRule.onNodeWithText("Search by PDB ID or Text")
            .assertIsDisplayed()
    }
    
    @Test
    fun searchScreen_searchButtonOpensSearchSheet() {
        // Given
        composeTestRule.setContent {
            SearchScreen(
                onNavigateBack = {}
            )
        }
        
        // When
        composeTestRule.onNodeWithText("Search by PDB ID or Text")
            .performClick()
        
        // Then
        composeTestRule.onNodeWithText("Search Proteins")
            .assertIsDisplayed()
    }
    
    @Test
    fun searchScreen_backButtonWorks() {
        // Given
        var backClicked = false
        composeTestRule.setContent {
            SearchScreen(
                onNavigateBack = { backClicked = true }
            )
        }
        
        // When
        composeTestRule.onNodeWithContentDescription("Back")
            .performClick()
        
        // Then
        assertTrue(backClicked)
    }
    
    @Test
    fun searchScreen_displaysSearchResults() {
        // Given
        composeTestRule.setContent {
            SearchScreen(
                onNavigateBack = {}
            )
        }
        
        // When
        composeTestRule.onNodeWithText("Search by PDB ID or Text")
            .performClick()
        
        // Then
        composeTestRule.onNodeWithText("Search Proteins")
            .assertIsDisplayed()
        
        composeTestRule.onNodeWithText("PDB ID")
            .assertIsDisplayed()
        
        composeTestRule.onNodeWithText("Search")
            .assertIsDisplayed()
    }
}


import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.avas.proteinviewer.ui.search.SearchScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SearchScreenTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun searchScreen_displaysSearchButton() {
        // When
        composeTestRule.setContent {
            SearchScreen(
                onNavigateBack = {}
            )
        }
        
        // Then
        composeTestRule.onNodeWithText("Search by PDB ID or Text")
            .assertIsDisplayed()
    }
    
    @Test
    fun searchScreen_searchButtonOpensSearchSheet() {
        // Given
        composeTestRule.setContent {
            SearchScreen(
                onNavigateBack = {}
            )
        }
        
        // When
        composeTestRule.onNodeWithText("Search by PDB ID or Text")
            .performClick()
        
        // Then
        composeTestRule.onNodeWithText("Search Proteins")
            .assertIsDisplayed()
    }
    
    @Test
    fun searchScreen_backButtonWorks() {
        // Given
        var backClicked = false
        composeTestRule.setContent {
            SearchScreen(
                onNavigateBack = { backClicked = true }
            )
        }
        
        // When
        composeTestRule.onNodeWithContentDescription("Back")
            .performClick()
        
        // Then
        assertTrue(backClicked)
    }
    
    @Test
    fun searchScreen_displaysSearchResults() {
        // Given
        composeTestRule.setContent {
            SearchScreen(
                onNavigateBack = {}
            )
        }
        
        // When
        composeTestRule.onNodeWithText("Search by PDB ID or Text")
            .performClick()
        
        // Then
        composeTestRule.onNodeWithText("Search Proteins")
            .assertIsDisplayed()
        
        composeTestRule.onNodeWithText("PDB ID")
            .assertIsDisplayed()
        
        composeTestRule.onNodeWithText("Search")
            .assertIsDisplayed()
    }
}


