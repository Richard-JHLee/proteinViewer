package com.avas.proteinviewer.domain.repository

import com.avas.proteinviewer.domain.model.PDBStructure
import com.avas.proteinviewer.domain.model.ProteinDetail
import com.avas.proteinviewer.domain.model.ProteinInfo
import kotlinx.coroutines.flow.Flow

interface ProteinRepository {
    fun searchProteins(query: String): Flow<List<ProteinInfo>>
    fun getProteinDetail(proteinId: String): Flow<ProteinDetail>
    suspend fun loadPDBStructure(proteinId: String, onProgress: (String) -> Unit): PDBStructure
}
