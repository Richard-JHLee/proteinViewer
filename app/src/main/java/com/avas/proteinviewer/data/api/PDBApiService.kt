package com.avas.proteinviewer.data.api

import com.avas.proteinviewer.data.api.model.RcsbEntryDto
import com.avas.proteinviewer.data.api.model.RcsbPolymerEntityDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Body

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

    /**
     * PDB 검색 API (아이폰과 동일한 POST 방식)
     */
    @POST("rcsbsearch/v2/query")
    suspend fun searchProteins(
        @Body query: @JvmSuppressWildcards Map<String, Any>
    ): Response<@JvmSuppressWildcards Map<String, Any>>

    /**
     * PDB ID 검색 API (특정 PDB ID 존재 여부 확인)
     */
    @POST("rcsbsearch/v2/query")
    suspend fun searchByPDBId(
        @Body query: @JvmSuppressWildcards Map<String, Any>
    ): Response<@JvmSuppressWildcards Map<String, Any>>

    /**
     * PDB 메타데이터 API (struct.title 등 가져오기)
     */
    @GET("core/entry/{pdbId}")
    suspend fun getProteinMetadata(
        @Path("pdbId") pdbId: String
    ): Response<@JvmSuppressWildcards Map<String, Any>>
}
