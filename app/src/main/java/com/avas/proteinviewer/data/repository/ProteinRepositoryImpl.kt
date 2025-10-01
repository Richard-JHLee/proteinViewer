package com.avas.proteinviewer.data.repository

import com.avas.proteinviewer.data.parser.PDBParser
import com.avas.proteinviewer.domain.model.PDBStructure
import com.avas.proteinviewer.domain.model.ProteinDetail
import com.avas.proteinviewer.domain.model.ProteinInfo
import com.avas.proteinviewer.domain.repository.ProteinRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProteinRepositoryImpl @Inject constructor() : ProteinRepository {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    override fun searchProteins(query: String): Flow<List<ProteinInfo>> = flow {
        if (query.isEmpty()) {
            // Return default proteins
            emit(getDefaultProteins())
            return@flow
        }

        try {
            // Search using RCSB PDB Search API
            val searchUrl = "https://search.rcsb.org/rcsbsearch/v2/query"
            val requestBody = """
                {
                  "query": {
                    "type": "terminal",
                    "service": "text",
                    "parameters": {
                      "value": "$query"
                    }
                  },
                  "return_type": "entry",
                  "request_options": {
                    "results_content_type": ["experimental"],
                    "return_all_hits": false,
                    "pager": {
                      "start": 0,
                      "rows": 20
                    }
                  }
                }
            """.trimIndent()

            val request = Request.Builder()
                .url(searchUrl)
                .post(okhttp3.RequestBody.create(
                    "application/json".toMediaType(),
                    requestBody
                ))
                .build()

            val response = withContext(Dispatchers.IO) {
                httpClient.newCall(request).execute()
            }

            if (response.isSuccessful) {
                val jsonResponse = JSONObject(response.body?.string() ?: "{}")
                val results = jsonResponse.optJSONArray("result_set") ?: JSONArray()
                
                val proteins = mutableListOf<ProteinInfo>()
                for (i in 0 until results.length()) {
                    val result = results.getJSONObject(i)
                    val id = result.optString("identifier", "")
                    if (id.isNotEmpty()) {
                        // Fetch details for each protein
                        val detail = fetchProteinDetail(id)
                        proteins.add(ProteinInfo(
                            id = id,
                            name = detail?.name ?: id,
                            description = detail?.description ?: "Unknown protein",
                            organism = detail?.organism,
                            resolution = detail?.resolution,
                            experimentalMethod = detail?.experimentalMethod,
                            depositionDate = detail?.depositionDate,
                            molecularWeight = detail?.molecularWeight
                        ))
                    }
                }
                
                emit(proteins)
            } else {
                emit(getDefaultProteins())
            }
        } catch (e: Exception) {
            emit(getDefaultProteins())
        }
    }.flowOn(Dispatchers.IO)

    override fun getProteinDetail(proteinId: String): Flow<ProteinDetail> = flow {
        val detail = fetchProteinDetail(proteinId)
        if (detail != null) {
            emit(detail)
        } else {
            throw Exception("Protein not found: $proteinId")
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun loadPDBStructure(proteinId: String, onProgress: (String) -> Unit): PDBStructure {
        return withContext(Dispatchers.IO) {
            onProgress("Downloading PDB file...")
            
            val pdbUrl = "https://files.rcsb.org/download/${proteinId.uppercase()}.pdb"
            val request = Request.Builder()
                .url(pdbUrl)
                .header("User-Agent", "ProteinViewer/1.0")
                .build()

            val response = httpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                throw Exception("Failed to download PDB file: ${response.code}")
            }

            val pdbText = response.body?.string() ?: throw Exception("Empty PDB file")
            
            onProgress("Parsing PDB structure...")
            PDBParser.parse(pdbText)
        }
    }

    private suspend fun fetchProteinDetail(proteinId: String): ProteinDetail? {
        return withContext(Dispatchers.IO) {
            try {
                val url = "https://data.rcsb.org/rest/v1/core/entry/$proteinId"
                val request = Request.Builder().url(url).build()
                val response = httpClient.newCall(request).execute()

                if (response.isSuccessful) {
                    val json = JSONObject(response.body?.string() ?: "{}")
                    
                    // Struct 정보
                    val struct = json.optJSONObject("struct")
                    val title = struct?.optString("title") ?: proteinId
                    val description = struct?.optString("pdbx_descriptor") ?: "Unknown protein"
                    
                    // Entry 정보
                    val entryInfo = json.optJSONObject("rcsb_entry_info")
                    val molecularWeight = entryInfo?.optDouble("molecular_weight")
                    val depositionDate = entryInfo?.optString("deposition_date")
                    
                    // 실험 방법
                    val exptl = json.optJSONArray("exptl")
                    val experimentalMethod = if (exptl != null && exptl.length() > 0) {
                        (0 until exptl.length()).map { exptl.getJSONObject(it).optString("method") }.joinToString(", ")
                    } else null
                    
                    // Resolution
                    val refine = json.optJSONArray("refine")
                    val resolution = refine?.optJSONObject(0)?.optDouble("ls_d_res_high")
                    
                    // Organism (Source organism)
                    val organism = try {
                        val entitySrcGen = json.optJSONArray("entity_src_gen")
                        if (entitySrcGen != null && entitySrcGen.length() > 0) {
                            entitySrcGen.getJSONObject(0).optString("pdbx_gene_src_scientific_name")
                        } else {
                            val entitySrcNat = json.optJSONArray("entity_src_nat")
                            if (entitySrcNat != null && entitySrcNat.length() > 0) {
                                entitySrcNat.getJSONObject(0).optString("pdbx_organism_scientific")
                            } else {
                                "Unknown"
                            }
                        }
                    } catch (e: Exception) {
                        "Unknown"
                    }
                    
                    ProteinDetail(
                        id = proteinId,
                        name = title,
                        description = description,
                        organism = organism,
                        molecularWeight = molecularWeight,
                        resolution = resolution,
                        experimentalMethod = experimentalMethod,
                        depositionDate = depositionDate
                    )
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun getDefaultProteins(): List<ProteinInfo> {
        return listOf(
            ProteinInfo(
                id = "1CRN",
                name = "Crambin",
                description = "Small plant seed protein",
                organism = "Crambe abyssinica",
                resolution = 0.54,
                experimentalMethod = "X-RAY DIFFRACTION",
                molecularWeight = 4.7
            ),
            ProteinInfo(
                id = "1HHO",
                name = "Hemoglobin",
                description = "Oxygen transport protein",
                organism = "Homo sapiens",
                resolution = 2.1,
                experimentalMethod = "X-RAY DIFFRACTION",
                molecularWeight = 64.5
            ),
            ProteinInfo(
                id = "2LYZ",
                name = "Lysozyme",
                description = "Antibacterial enzyme",
                organism = "Gallus gallus",
                resolution = 1.5,
                experimentalMethod = "X-RAY DIFFRACTION",
                molecularWeight = 14.3
            ),
            ProteinInfo(
                id = "4INS",
                name = "Insulin",
                description = "Hormone regulating glucose metabolism",
                organism = "Homo sapiens",
                resolution = 1.9,
                experimentalMethod = "X-RAY DIFFRACTION",
                molecularWeight = 5.8
            ),
            ProteinInfo(
                id = "1RXZ",
                name = "Ribonuclease A",
                description = "RNA degradation enzyme",
                organism = "Bos taurus",
                resolution = 1.26,
                experimentalMethod = "X-RAY DIFFRACTION",
                molecularWeight = 13.7
            )
        )
    }
}
