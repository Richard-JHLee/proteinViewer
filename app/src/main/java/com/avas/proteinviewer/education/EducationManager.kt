package com.avas.proteinviewer.education

import com.avas.proteinviewer.data.model.PDBStructure
import com.avas.proteinviewer.data.model.SecondaryStructure

class EducationManager {
    
    fun getProteinExplanation(proteinId: String, structure: PDBStructure): ProteinExplanation {
        return when (proteinId.uppercase()) {
            "1CRN" -> ProteinExplanation(
                title = "Crambin",
                description = "Crambin is a small, water-soluble protein found in the seeds of the cabbage plant (Crambe abyssinica). It's a model protein for studying protein folding and stability.",
                structureInfo = "Contains 46 amino acids with a high content of cysteine residues forming disulfide bonds.",
                biologicalFunction = "Acts as a storage protein in seeds and has antimicrobial properties.",
                educationalNotes = listOf(
                    "Notice the compact globular structure",
                    "Disulfide bonds (yellow) provide structural stability",
                    "Hydrophobic core with hydrophilic surface",
                    "Excellent model for studying protein folding"
                )
            )
            "1HHO" -> ProteinExplanation(
                title = "Hemoglobin",
                description = "Hemoglobin is the oxygen-carrying protein in red blood cells. This structure shows the complete tetramer with four subunits.",
                structureInfo = "Tetrameric protein with 2 α-chains and 2 β-chains, each containing a heme group.",
                biologicalFunction = "Transports oxygen from lungs to tissues and carbon dioxide back to lungs.",
                educationalNotes = listOf(
                    "Four subunits work cooperatively",
                    "Heme groups (red) bind oxygen molecules",
                    "Conformational changes upon oxygen binding",
                    "Essential for aerobic respiration"
                )
            )
            "1INS" -> ProteinExplanation(
                title = "Insulin",
                description = "Insulin is a hormone that regulates blood glucose levels. This structure shows the mature insulin dimer.",
                structureInfo = "Two chains (A and B) connected by disulfide bonds, produced from proinsulin precursor.",
                biologicalFunction = "Promotes glucose uptake by cells and regulates carbohydrate metabolism.",
                educationalNotes = listOf(
                    "Two polypeptide chains",
                    "Disulfide bonds maintain structure",
                    "Binds to insulin receptor",
                    "Critical for diabetes treatment"
                )
            )
            "1LYZ" -> ProteinExplanation(
                title = "Lysozyme",
                description = "Lysozyme is an enzyme that breaks down bacterial cell walls by cleaving peptidoglycan bonds.",
                structureInfo = "Single polypeptide chain with both α-helix and β-sheet secondary structures.",
                biologicalFunction = "Antibacterial defense mechanism, found in tears, saliva, and egg whites.",
                educationalNotes = listOf(
                    "Active site cleaves peptidoglycan",
                    "Mixed α/β structure",
                    "Natural antibiotic",
                    "First enzyme structure solved by X-ray crystallography"
                )
            )
            else -> ProteinExplanation(
                title = "Protein $proteinId",
                description = "A protein structure from the Protein Data Bank.",
                structureInfo = "Contains ${structure.atomCount} atoms, ${structure.residueCount} residues, and ${structure.chainCount} chains.",
                biologicalFunction = "Function varies by protein type and structure.",
                educationalNotes = listOf(
                    "Study the overall shape",
                    "Look for secondary structure elements",
                    "Identify functional regions",
                    "Compare with other proteins"
                )
            )
        }
    }
    
    fun getSecondaryStructureExplanation(structure: SecondaryStructure): String {
        return when (structure) {
            SecondaryStructure.HELIX -> "α-Helix: A right-handed spiral structure stabilized by hydrogen bonds between backbone atoms. Each turn contains 3.6 amino acids."
            SecondaryStructure.SHEET -> "β-Sheet: Extended polypeptide chains arranged side-by-side, stabilized by hydrogen bonds between adjacent chains."
            SecondaryStructure.COIL -> "Coil/Loop: Flexible regions connecting secondary structure elements, often involved in protein function."
            SecondaryStructure.UNKNOWN -> "Unknown: Secondary structure not determined or not applicable (e.g., for ligands)."
        }
    }
    
    fun getColorSchemeExplanation(colorScheme: String): String {
        return when (colorScheme.uppercase()) {
            "ELEMENT" -> "Element coloring: Colors atoms by their chemical element (C=gray, N=blue, O=red, S=yellow, etc.)"
            "CHAIN" -> "Chain coloring: Different colors for each protein chain to show subunit organization"
            "UNIFORM" -> "Uniform coloring: Single color for all atoms to focus on structure shape"
            "SECONDARY_STRUCTURE" -> "Secondary structure coloring: Colors by structural elements (helix=red, sheet=blue, coil=yellow)"
            else -> "Color scheme information not available"
        }
    }
}

data class ProteinExplanation(
    val title: String,
    val description: String,
    val structureInfo: String,
    val biologicalFunction: String,
    val educationalNotes: List<String>
)
