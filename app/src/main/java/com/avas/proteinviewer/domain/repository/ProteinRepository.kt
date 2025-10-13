package com.avas.proteinviewer.domain.repository

import com.avas.proteinviewer.domain.model.PDBStructure
import com.avas.proteinviewer.domain.model.ProteinDetail
import com.avas.proteinviewer.domain.model.ProteinInfo
import com.avas.proteinviewer.domain.model.ProteinCategory
import kotlinx.coroutines.flow.Flow

interface ProteinRepository {
    fun searchProteins(query: String): Flow<List<ProteinInfo>>
    fun getProteinDetail(proteinId: String): Flow<ProteinDetail>
    suspend fun loadPDBStructure(proteinId: String, onProgress: (String) -> Unit): PDBStructure
    suspend fun getPDBContent(proteinId: String): String
    suspend fun searchProteinsByCategory(category: ProteinCategory, limit: Int = 100): List<ProteinInfo>
    suspend fun getCategoryCount(category: ProteinCategory): Int
    
    // 아이폰과 동일한 검색 함수들
    suspend fun searchProteinByID(pdbId: String): ProteinInfo?
    suspend fun searchProteinsByText(searchText: String, limit: Int = 100): List<ProteinInfo>
    
    // 아이폰과 동일한 2단계 검색 (PDB ID 목록 -> 상세 정보)
    suspend fun searchProteinIdsByCategory(category: ProteinCategory, limit: Int = 30, skip: Int = 0): Pair<List<String>, Int>
    suspend fun getProteinsByIds(pdbIds: List<String>): List<ProteinInfo>
}
