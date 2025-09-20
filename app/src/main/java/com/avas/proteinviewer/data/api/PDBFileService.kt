package com.avas.proteinviewer.data.api

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface PDBFileService {

    @GET("download/{pdbId}.pdb")
    suspend fun getProteinStructure(
        @Path("pdbId") pdbId: String
    ): Response<ResponseBody>
}
