package com.avas.proteinviewer.data.repository

import com.avas.proteinviewer.data.api.PDBApiService
import com.avas.proteinviewer.data.api.PDBFileService
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

@OptIn(ExperimentalCoroutinesApi::class)
class ProteinRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var apiService: PDBApiService
    private lateinit var fileService: PDBFileService
    private lateinit var repository: ProteinRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()

        val client = OkHttpClient.Builder().build()
        val baseUrl = server.url("/")

        apiService = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(PDBApiService::class.java)

        fileService = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .build()
            .create(PDBFileService::class.java)

        repository = ProteinRepository(apiService, fileService)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun loadProteinStructure_success() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "text/plain")
                .setBody(loadResource("pdb_1crn_sample.pdb"))
        )

        val result = repository.loadProteinStructure("1CRN")

        assertThat(result.isSuccess).isTrue()
        val structure = result.getOrThrow()
        assertThat(structure.atomCount).isEqualTo(74)
        assertThat(structure.chainCount).isEqualTo(1)
    }

    @Test
    fun getProteinMetadata_success() = runTest {
        val entryJson = """
            {
              "rcsb_id": "1CRN",
              "struct": { "title": "WATER STRUCTURE OF A HYDROPHOBIC PROTEIN AT ATOMIC RESOLUTION" }
            }
        """.trimIndent()

        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody(entryJson)
        )

        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody(loadResource("polymer_entity_1crn.json"))
        )

        val result = repository.getProteinMetadata("1CRN")

        assertThat(result.isSuccess).isTrue()
        val metadata = result.getOrThrow()
        assertThat(metadata.pdbId).isEqualTo("1CRN")
        assertThat(metadata.sequenceLength).isEqualTo(46)
        assertThat(metadata.sequence).contains("TTCCPSIV")
        assertThat(metadata.formulaWeight).isWithin(0.001).of(4.738)
        assertThat(metadata.annotations.map { it.id }).contains("PF00321")
    }

    private fun loadResource(name: String): String {
        val stream = checkNotNull(javaClass.classLoader?.getResourceAsStream(name)) {
            "Missing test resource: $name"
        }
        return stream.bufferedReader().use { it.readText() }
    }
}
