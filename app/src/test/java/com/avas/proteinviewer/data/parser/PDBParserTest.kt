package com.avas.proteinviewer.data.parser

import com.avas.proteinviewer.data.error.PDBParseError
import com.avas.proteinviewer.data.model.AnnotationType
import com.avas.proteinviewer.data.model.Vector3
import com.avas.proteinviewer.data.model.BoundingBox
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PDBParserTest {

    private fun loadResource(name: String): String {
        val stream = checkNotNull(javaClass.classLoader?.getResourceAsStream(name)) {
            "Missing test resource: $name"
        }
        return stream.bufferedReader().use { it.readText() }
    }

    @Test
    fun `parse sample PDB yields expected structural summary`() {
        val pdbText = loadResource("pdb_1crn_sample.pdb")

        val structure = PDBParser.parse(pdbText)

        println("atomCount=${structure.atomCount}, bondCount=${structure.bonds.size}")
        println("boundingBox=${structure.boundingBox}, centerOfMass=${structure.centerOfMass}")

        assertThat(structure.atomCount).isEqualTo(74)
        assertThat(structure.residueCount).isEqualTo(15)
        assertThat(structure.chainCount).isEqualTo(1)
        assertThat(structure.bonds).isNotEmpty()

        val expectedBoundingBox = BoundingBox(
            min = Vector3(0f, 0f, 0f),
            max = Vector3(88.5f, 1.5f, 0f)
        )
        assertThat(structure.boundingBox).isEqualTo(expectedBoundingBox)

        assertThat(structure.annotations.map { it.type })
            .containsAtLeast(AnnotationType.MOLECULAR_WEIGHT, AnnotationType.EXPERIMENTAL_METHOD, AnnotationType.FUNCTION)

        val firstAtom = structure.atoms.first()
        assertThat(firstAtom.element).isEqualTo("N")
        assertThat(firstAtom.chain).isEqualTo("A")
        assertThat(firstAtom.residueNumber).isEqualTo(1)
    }

    @Test(expected = PDBParseError.InvalidFormat::class)
    fun `empty file throws error`() {
        PDBParser.parse("   \n   ")
    }

    @Test(expected = PDBParseError.NoValidAtoms::class)
    fun `file without atom records throws error`() {
        val pdbText = """HEADER    TEST                                                  \nTITLE     NO ATOMS HERE                                         \nEND\n"""
        PDBParser.parse(pdbText)
    }
}
