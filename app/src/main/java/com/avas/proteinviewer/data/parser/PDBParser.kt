package com.avas.proteinviewer.data.parser

import com.avas.proteinviewer.data.error.PDBParseError
import com.avas.proteinviewer.data.model.*
import kotlin.math.sqrt

object PDBParser {
    
    // 표준 아미노산 잔기들
    private val standardResidues = setOf(
        "ALA", "ARG", "ASN", "ASP", "CYS", "GLN", "GLU", "GLY", "HIS", "ILE",
        "LEU", "LYS", "MET", "PHE", "PRO", "SER", "THR", "TRP", "TYR", "VAL"
    )
    
    // 백본 원자들
    private val backboneAtoms = setOf("CA", "C", "N", "O", "P", "O5'", "C5'", "C4'", "C3'", "O3'")
    
    fun parse(pdbText: String): PDBStructure {
        if (pdbText.trim().isEmpty()) {
            throw PDBParseError.InvalidFormat("Empty PDB content")
        }
        
        val lines = pdbText.split('\n')
        val atoms = mutableListOf<Atom>()
        val secondaryStructureMap = mutableMapOf<String, SecondaryStructure>()
        val annotations = mutableListOf<com.avas.proteinviewer.data.model.Annotation>()
        
        // Parse header information and annotations
        val title = parseTitle(lines)
        parseHeaderInformation(lines, annotations)
        
        // First pass: Parse secondary structure information
        parseSecondaryStructure(lines, secondaryStructureMap)
        
        // Second pass: Parse atoms with better error handling
        parseAtoms(lines, secondaryStructureMap, atoms)
        
        if (atoms.isEmpty()) {
            throw PDBParseError.NoValidAtoms
        }
        
        // Generate bonds more efficiently
        val bonds = generateBonds(atoms)
        
        // Calculate structure properties
        val boundingBox = calculateBoundingBox(atoms)
        val centerOfMass = calculateCenterOfMass(atoms)
        
        // Add calculated annotations if not present
        addCalculatedAnnotations(atoms, annotations)
        
        return PDBStructure(
            atoms = atoms,
            bonds = bonds,
            annotations = annotations,
            boundingBox = boundingBox,
            centerOfMass = centerOfMass,
            title = title
        )
    }
    
    private fun parseTitle(lines: List<String>): String? {
        val titleLines = mutableListOf<String>()
        
        for (line in lines) {
            if (line.startsWith("TITLE")) {
                val titleContent = safeSubstring(line, 10, 80).trim()
                if (titleContent.isNotEmpty()) {
                    titleLines.add(titleContent)
                }
            }
        }
        
        if (titleLines.isEmpty()) {
            return null
        }
        
        val fullTitle = titleLines.joinToString(" ")
        val cleanTitle = fullTitle
            .replace(Regex("CRYSTAL STRUCTURE OF", RegexOption.IGNORE_CASE), "")
            .replace(Regex("X-RAY STRUCTURE OF", RegexOption.IGNORE_CASE), "")
            .replace(Regex("NMR STRUCTURE OF", RegexOption.IGNORE_CASE), "")
            .trim()
        
        return if (cleanTitle.isEmpty()) null else cleanTitle
    }
    
    private fun parseHeaderInformation(lines: List<String>, annotations: MutableList<com.avas.proteinviewer.data.model.Annotation>) {
        for (line in lines) {
            when {
                line.startsWith("HEADER") && line.length >= 50 -> {
                    val classification = safeSubstring(line, 10, 50).trim()
                    val depositDate = safeSubstring(line, 50, 59).trim()
                    
                    if (classification.isNotEmpty()) {
                        annotations.add(com.avas.proteinviewer.data.model.Annotation(
                            type = AnnotationType.FUNCTION,
                            value = classification,
                            description = "Protein classification"
                        ))
                    }
                    if (depositDate.isNotEmpty()) {
                        annotations.add(com.avas.proteinviewer.data.model.Annotation(
                            type = AnnotationType.DEPOSITION_DATE,
                            value = depositDate,
                            description = "Structure deposition date"
                        ))
                    }
                }
                line.startsWith("REMARK   2 RESOLUTION") -> {
                    val resolutionStr = safeSubstring(line, 23, 30).trim()
                    if (resolutionStr.isNotEmpty()) {
                        annotations.add(com.avas.proteinviewer.data.model.Annotation(
                            type = AnnotationType.RESOLUTION,
                            value = "$resolutionStr Å",
                            description = "X-ray diffraction resolution"
                        ))
                    }
                }
                line.startsWith("EXPDTA") -> {
                    val method = safeSubstring(line, 10, 70).trim()
                    if (method.isNotEmpty()) {
                        annotations.add(com.avas.proteinviewer.data.model.Annotation(
                            type = AnnotationType.EXPERIMENTAL_METHOD,
                            value = method,
                            description = "Structure determination method"
                        ))
                    }
                }
            }
        }
    }
    
    private fun parseSecondaryStructure(lines: List<String>, secondaryStructureMap: MutableMap<String, SecondaryStructure>) {
        for (line in lines) {
            when {
                line.startsWith("HELIX") && line.length >= 37 -> {
                    parseHelixRecord(line, secondaryStructureMap)
                }
                line.startsWith("SHEET") && line.length >= 37 -> {
                    parseSheetRecord(line, secondaryStructureMap)
                }
            }
        }
    }
    
    private fun parseHelixRecord(line: String, secondaryStructureMap: MutableMap<String, SecondaryStructure>) {
        val chain = safeSubstring(line, 19, 20).trim()
        val startResStr = safeSubstring(line, 21, 25).trim()
        val endResStr = safeSubstring(line, 33, 37).trim()
        
        val startRes = startResStr.toIntOrNull() ?: return
        val endRes = endResStr.toIntOrNull() ?: return
        
        for (resNum in startRes..endRes) {
            secondaryStructureMap["${chain}_$resNum"] = SecondaryStructure.HELIX
        }
    }
    
    private fun parseSheetRecord(line: String, secondaryStructureMap: MutableMap<String, SecondaryStructure>) {
        val chain = safeSubstring(line, 21, 22).trim()
        val startResStr = safeSubstring(line, 22, 26).trim()
        val endResStr = safeSubstring(line, 33, 37).trim()
        
        val startRes = startResStr.toIntOrNull() ?: return
        val endRes = endResStr.toIntOrNull() ?: return
        
        for (resNum in startRes..endRes) {
            secondaryStructureMap["${chain}_$resNum"] = SecondaryStructure.SHEET
        }
    }
    
    private fun parseAtoms(
        lines: List<String>,
        secondaryStructureMap: Map<String, SecondaryStructure>,
        atoms: MutableList<Atom>
    ) {
        var atomIndex = 0
        
        for (line in lines) {
            if (!line.startsWith("ATOM") && !line.startsWith("HETATM")) continue
            if (line.length < 54) continue // 최소 좌표까지는 있어야 함
            
            // Parse atom information with safer extraction
            val atomName = safeSubstring(line, 12, 16).trim()
            val residueName = safeSubstring(line, 17, 20).trim()
            val chain = safeSubstring(line, 21, 22).trim()
            val residueNumberStr = safeSubstring(line, 22, 26).trim()
            
            // Parse coordinates with validation
            val xStr = safeSubstring(line, 30, 38).trim()
            val yStr = safeSubstring(line, 38, 46).trim()
            val zStr = safeSubstring(line, 46, 54).trim()
            
            val residueNumber = residueNumberStr.toIntOrNull() ?: continue
            val x = xStr.toFloatOrNull() ?: continue
            val y = yStr.toFloatOrNull() ?: continue
            val z = zStr.toFloatOrNull() ?: continue
            
            if (!x.isFinite() || !y.isFinite() || !z.isFinite()) continue
            
            // Parse optional fields
            val occupancy = safeSubstring(line, 54, 60).trim().toFloatOrNull() ?: 1.0f
            val tempFactor = safeSubstring(line, 60, 66).trim().toFloatOrNull() ?: 0.0f
            var element = safeSubstring(line, 76, 78).trim()
            
            // Guess element from atom name if not provided
            if (element.isEmpty()) {
                element = guessElement(atomName)
            }
            
            // Determine atom properties
            val isBackbone = backboneAtoms.contains(atomName)
            val isLigand = line.startsWith("HETATM") || !standardResidues.contains(residueName)
            val isPocket = !isBackbone && !isLigand
            
            // Get secondary structure
            val structureKey = "${chain}_$residueNumber"
            val secondaryStructure = secondaryStructureMap[structureKey] 
                ?: if (isLigand) SecondaryStructure.UNKNOWN else SecondaryStructure.COIL
            
            val atom = Atom(
                id = atomIndex,
                element = element.replaceFirstChar { it.uppercase() },
                name = atomName,
                chain = chain.ifEmpty { "A" }, // Default chain
                residueName = residueName,
                residueNumber = residueNumber,
                position = Vector3(x, y, z),
                secondaryStructure = secondaryStructure,
                isBackbone = isBackbone,
                isLigand = isLigand,
                isPocket = isPocket,
                occupancy = occupancy,
                temperatureFactor = tempFactor
            )
            
            atoms.add(atom)
            atomIndex++
        }
    }
    
    private fun generateBonds(atoms: List<Atom>): List<Bond> {
        if (atoms.size <= 1) return emptyList()
        
        val bonds = mutableListOf<Bond>()
        
        for (i in atoms.indices) {
            for (j in i + 1 until atoms.size) {
                val atomA = atoms[i]
                val atomB = atoms[j]
                
                val distance = calculateDistance(atomA.position, atomB.position)
                
                // Skip too close atoms (likely overlapping)
                if (distance <= 0.4f) continue
                
                val bondCutoff = (covalentRadius(atomA.element) + covalentRadius(atomB.element)) * 1.3f
                
                if (distance <= bondCutoff) {
                    bonds.add(Bond(
                        atomA = i,
                        atomB = j,
                        order = BondOrder.SINGLE,
                        distance = distance
                    ))
                }
            }
        }
        
        return bonds
    }
    
    private fun calculateBoundingBox(atoms: List<Atom>): BoundingBox {
        if (atoms.isEmpty()) {
            return BoundingBox(
                min = Vector3(0f, 0f, 0f),
                max = Vector3(0f, 0f, 0f)
            )
        }
        
        val firstPos = atoms.first().position
        var minX = firstPos.x
        var minY = firstPos.y
        var minZ = firstPos.z
        var maxX = firstPos.x
        var maxY = firstPos.y
        var maxZ = firstPos.z
        
        for (atom in atoms.drop(1)) {
            val pos = atom.position
            minX = minOf(minX, pos.x)
            minY = minOf(minY, pos.y)
            minZ = minOf(minZ, pos.z)
            maxX = maxOf(maxX, pos.x)
            maxY = maxOf(maxY, pos.y)
            maxZ = maxOf(maxZ, pos.z)
        }
        
        return BoundingBox(
            min = Vector3(minX, minY, minZ),
            max = Vector3(maxX, maxY, maxZ)
        )
    }
    
    private fun calculateCenterOfMass(atoms: List<Atom>): Vector3 {
        if (atoms.isEmpty()) return Vector3(0f, 0f, 0f)
        
        var sumX = 0f
        var sumY = 0f
        var sumZ = 0f
        
        for (atom in atoms) {
            sumX += atom.position.x
            sumY += atom.position.y
            sumZ += atom.position.z
        }
        
        val count = atoms.size.toFloat()
        return Vector3(sumX / count, sumY / count, sumZ / count)
    }
    
    private fun addCalculatedAnnotations(atoms: List<Atom>, annotations: MutableList<com.avas.proteinviewer.data.model.Annotation>) {
        // Add molecular weight if not present
        if (!annotations.any { it.type == AnnotationType.MOLECULAR_WEIGHT }) {
            val estimatedWeight = atoms.sumOf { atomicWeight(it.element).toDouble() }
            annotations.add(com.avas.proteinviewer.data.model.Annotation(
                type = AnnotationType.MOLECULAR_WEIGHT,
                value = "${estimatedWeight.toInt()} Da",
                description = "Calculated molecular weight"
            ))
        }
        
        // Add default values for missing annotations
        val defaultAnnotations = listOf(
            Triple(AnnotationType.RESOLUTION, "Unknown", "Resolution not specified"),
            Triple(AnnotationType.EXPERIMENTAL_METHOD, "Unknown", "Method not specified"),
            Triple(AnnotationType.ORGANISM, "Unknown", "Source organism not specified")
        )
        
        for ((type, value, description) in defaultAnnotations) {
            if (!annotations.any { it.type == type }) {
                annotations.add(com.avas.proteinviewer.data.model.Annotation(type = type, value = value, description = description))
            }
        }
    }
    
    // Utility functions
    private fun safeSubstring(string: String, start: Int, end: Int): String {
        val startIndex = maxOf(0, minOf(start, string.length))
        val endIndex = maxOf(startIndex, minOf(end, string.length))
        return string.substring(startIndex, endIndex)
    }
    
    private fun guessElement(atomName: String): String {
        val cleaned = atomName.trim()
        val letters = cleaned.filter { it.isLetter() }
        
        if (letters.length >= 2) {
            val twoChar = letters.substring(0, 2)
            if (listOf("CA", "MG", "FE", "ZN", "CU", "MN", "NI", "CO").contains(twoChar.uppercase())) {
                return twoChar
            }
        }
        
        return if (letters.isEmpty()) "C" else letters.substring(0, 1)
    }
    
    private fun calculateDistance(pos1: Vector3, pos2: Vector3): Float {
        val dx = pos1.x - pos2.x
        val dy = pos1.y - pos2.y
        val dz = pos1.z - pos2.z
        return sqrt(dx * dx + dy * dy + dz * dz)
    }
    
    private fun covalentRadius(element: String): Float = when (element.uppercase()) {
        "H" -> 0.31f
        "C" -> 0.76f
        "N" -> 0.71f
        "O" -> 0.66f
        "S" -> 1.05f
        "P" -> 1.07f
        "CA" -> 1.74f
        "MG" -> 1.30f
        "FE" -> 1.25f
        "ZN" -> 1.22f
        "CU" -> 1.28f
        "MN" -> 1.39f
        else -> 0.85f
    }
    
    private fun atomicWeight(element: String): Float = when (element.uppercase()) {
        "H" -> 1.008f
        "C" -> 12.01f
        "N" -> 14.01f
        "O" -> 16.00f
        "S" -> 32.07f
        "P" -> 30.97f
        "CA" -> 40.08f
        "MG" -> 24.31f
        "FE" -> 55.85f
        "ZN" -> 65.38f
        "CU" -> 63.55f
        "MN" -> 54.94f
        else -> 14.0f // Average approximation
    }
}
