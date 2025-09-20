package com.avas.proteinviewer.data.api

import com.avas.proteinviewer.data.api.model.RcsbEntryDto
import com.avas.proteinviewer.data.api.model.RcsbPolymerEntityDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * PDB API 서비스 인터페이스
 */
interface PDBApiService {

    @GET("entry/{pdbId}")
    suspend fun getEntry(@Path("pdbId") pdbId: String): Response<RcsbEntryDto>

    @GET("polymer_entity/{pdbId}/{entityId}")
    suspend fun getPolymerEntity(
        @Path("pdbId") pdbId: String,
        @Path("entityId") entityId: String = "1"
    ): Response<RcsbPolymerEntityDto>
}
