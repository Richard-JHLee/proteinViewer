package com.avas.proteinviewer.data.repository

import com.avas.proteinviewer.data.api.PDBApiService
import com.avas.proteinviewer.data.api.PDBFileService
import com.avas.proteinviewer.data.error.PDBError
import com.avas.proteinviewer.data.model.PDBStructure
import com.avas.proteinviewer.data.model.ProteinAnnotation
import com.avas.proteinviewer.data.model.ProteinMetadata
import com.avas.proteinviewer.data.parser.PDBParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProteinRepository @Inject constructor(
    private val apiService: PDBApiService,
    private val fileService: PDBFileService
) {

    suspend fun loadProteinStructure(pdbId: String): Result<PDBStructure> {
        return withContext(Dispatchers.IO) {
            try {
                val formattedPdbId = pdbId.uppercase().trim()
                if (formattedPdbId.length != 4 || !formattedPdbId.all { it.isLetterOrDigit() }) {
                    return@withContext Result.failure(PDBError.InvalidPDBID(pdbId))
                }

                var response: retrofit2.Response<ResponseBody>? = null
                var lastException: Exception? = null

                repeat(3) { attempt ->
                    try {
                        response = fileService.getProteinStructure(formattedPdbId)
                        if (response?.isSuccessful == true) {
                            return@repeat
                        }
                    } catch (e: Exception) {
                        lastException = e
                        if (attempt < 2) {
                            delay(1000L * (attempt + 1))
                        }
                    }
                }

                if (response == null) {
                    return@withContext Result.failure(
                        when (lastException) {
                            is java.net.UnknownHostException -> PDBError.NetworkUnavailable
                            is java.net.SocketTimeoutException -> PDBError.Timeout
                            else -> PDBError.InvalidResponse
                        }
                    )
                }

                if (!response!!.isSuccessful) {
                    return@withContext Result.failure(
                        when (response!!.code()) {
                            404 -> PDBError.StructureNotFound(formattedPdbId)
                            500, 502, 503, 504 -> PDBError.ServerError(response!!.code())
                            else -> PDBError.ServerError(response!!.code())
                        }
                    )
                }

                val pdbText = response!!.body()?.string()
                if (pdbText.isNullOrEmpty()) {
                    return@withContext Result.failure(PDBError.EmptyResponse)
                }

                val pdbStructure = PDBParser.parse(pdbText)
                Result.success(pdbStructure)
            } catch (e: Exception) {
                Result.failure(
                    when (e) {
                        is PDBError -> e
                        is java.net.UnknownHostException -> PDBError.NetworkUnavailable
                        is java.net.SocketTimeoutException -> PDBError.Timeout
                        else -> PDBError.InvalidResponse
                    }
                )
            }
        }
    }

    suspend fun getProteinName(pdbId: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getEntry(pdbId.uppercase())

                if (!response.isSuccessful) {
                    return@withContext Result.success("Protein $pdbId")
                }

                val title = response.body()?.struct?.title
                if (title.isNullOrEmpty()) {
                    return@withContext Result.success("Protein $pdbId")
                }

                val cleanTitle = cleanEntryTitle(title)
                Result.success(if (cleanTitle.isEmpty()) "Protein $pdbId" else cleanTitle)
            } catch (e: Exception) {
                Result.success("Protein $pdbId")
            }
        }
    }

    suspend fun getProteinMetadata(
        pdbId: String,
        entityId: String = "1"
    ): Result<ProteinMetadata> {
        return withContext(Dispatchers.IO) {
            try {
                val entryResponse = apiService.getEntry(pdbId.uppercase())
                val entryTitle = entryResponse.body()?.struct?.title.orEmpty()

                val polymerResponse = apiService.getPolymerEntity(pdbId.uppercase(), entityId)
                if (!polymerResponse.isSuccessful) {
                    return@withContext Result.failure(PDBError.InvalidResponse)
                }

                val polymerEntity = polymerResponse.body()
                    ?: return@withContext Result.failure(PDBError.InvalidResponse)

                val entityPoly = polymerEntity.entityPoly
                val polymerInfo = polymerEntity.polymerEntity

                val metadata = ProteinMetadata(
                    pdbId = pdbId.uppercase(),
                    title = cleanEntryTitle(entryTitle).ifEmpty { "Protein ${pdbId.uppercase()}" },
                    sequence = entityPoly?.sequence.orEmpty(),
                    sequenceLength = entityPoly?.sequenceLength ?: 0,
                    polymerType = entityPoly?.polymerType,
                    description = polymerInfo?.description,
                    formulaWeight = polymerInfo?.formulaWeight,
                    sourceOrganism = polymerEntity.sourceOrganisms?.firstOrNull()?.scientificName,
                    annotations = polymerEntity.annotations
                        ?.map { ProteinAnnotation(id = it.annotationId, name = it.name, type = it.type) }
                        .orEmpty()
                )

                Result.success(metadata)
            } catch (e: Exception) {
                Result.failure(
                    when (e) {
                        is PDBError -> e
                        is java.net.UnknownHostException -> PDBError.NetworkUnavailable
                        is java.net.SocketTimeoutException -> PDBError.Timeout
                        else -> PDBError.InvalidResponse
                    }
                )
            }
        }
    }

    private fun cleanEntryTitle(title: String): String {
        return title
            .replace("CRYSTAL STRUCTURE OF", "", ignoreCase = true)
            .replace("X-RAY STRUCTURE OF", "", ignoreCase = true)
            .replace("NMR STRUCTURE OF", "", ignoreCase = true)
            .trim()
    }
}
