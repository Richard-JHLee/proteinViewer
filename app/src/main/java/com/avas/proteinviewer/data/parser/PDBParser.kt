package com.avas.proteinviewer.data.parser

import com.avas.proteinviewer.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

object PDBParser {
    
    private val standardResidues = setOf(
        "ALA", "ARG", "ASN", "ASP", "CYS", "GLN", "GLU", "GLY", "HIS", "ILE",
        "LEU", "LYS", "MET", "PHE", "PRO", "SER", "THR", "TRP", "TYR", "VAL"
    )
    
    private val backboneAtoms = setOf("CA", "C", "N", "O", "P", "O5'", "C5'", "C4'", "C3'", "O3'")
    
    suspend fun parse(pdbText: String): PDBStructure = withContext(Dispatchers.Default) {
        if (pdbText.trim().isEmpty()) {
            throw PDBParseException("Empty PDB content")
        }
        
        val lines = pdbText.lines()
        val atoms = mutableListOf<Atom>()
        val secondaryStructureMap = mutableMapOf<String, SecondaryStructure>()
        val annotations = mutableListOf<com.avas.proteinviewer.domain.model.Annotation>()
        
        // Parse header information
        parseHeaderInformation(lines, annotations)
        
        // Parse secondary structure
        parseSecondaryStructure(lines, secondaryStructureMap)
        
        // Parse atoms
        parseAtoms(lines, secondaryStructureMap, atoms)
        
        if (atoms.isEmpty()) {
            throw PDBParseException("No valid atoms found")
        }
        
        // Generate bonds
        val bonds = generateBonds(atoms)
        
        // iOS와 동일한 방식으로 단순화: 복잡한 포켓 분석 제거
        android.util.Log.d("PDBParser", "iOS 방식 적용: 백본이 아니고 리간드가 아닌 모든 원자를 Pocket으로 처리")
        val pocketCount = atoms.count { it.isPocket }
        val backboneCount = atoms.count { it.isBackbone }
        val ligandCount = atoms.count { it.isLigand }
        android.util.Log.d("PDBParser", "총 ${atoms.size}개 원자 중:")
        android.util.Log.d("PDBParser", "- Backbone: ${backboneCount}개")
        android.util.Log.d("PDBParser", "- Ligand: ${ligandCount}개") 
        android.util.Log.d("PDBParser", "- Pocket: ${pocketCount}개")
        
        // Calculate properties
        val boundingBox = calculateBoundingBox(atoms)
        val centerOfMass = calculateCenterOfMass(atoms)
        
        // Add calculated annotations
        addCalculatedAnnotations(atoms, annotations)
        
        PDBStructure(
            atoms = atoms,
            bonds = bonds,
            annotations = annotations,
            boundingBoxMin = boundingBox.first,
            boundingBoxMax = boundingBox.second,
            centerOfMass = centerOfMass
        )
    }
    
    private fun parseHeaderInformation(lines: List<String>, annotations: MutableList<com.avas.proteinviewer.domain.model.Annotation>) {
        for (line in lines) {
            when {
                line.startsWith("HEADER") && line.length > 50 -> {
                    val classification = line.substring(10, 50).trim()
                    val date = line.substring(50, 59).trim()
                    if (classification.isNotEmpty()) {
                        annotations.add(Annotation(
                            AnnotationType.FUNCTION,
                            classification,
                            "Protein classification"
                        ))
                    }
                    if (date.isNotEmpty()) {
                        annotations.add(Annotation(
                            AnnotationType.DEPOSITION_DATE,
                            date,
                            "Date when structure was deposited"
                        ))
                    }
                }
                line.startsWith("REMARK   2 RESOLUTION.") -> {
                    val resolution = line.substring(23).trim().split(" ")[0]
                    annotations.add(Annotation(
                        AnnotationType.RESOLUTION,
                        "$resolution Å",
                        "X-ray diffraction resolution"
                    ))
                }
                line.startsWith("EXPDTA") -> {
                    val method = line.substring(10).trim()
                    annotations.add(Annotation(
                        AnnotationType.EXPERIMENTAL_METHOD,
                        method,
                        "Experimental technique"
                    ))
                }
                line.startsWith("SOURCE") && line.contains("ORGANISM_SCIENTIFIC:") -> {
                    val organism = line.substring(line.indexOf("ORGANISM_SCIENTIFIC:") + 20).trim()
                    annotations.add(Annotation(
                        AnnotationType.ORGANISM,
                        organism,
                        "Source organism"
                    ))
                }
            }
        }
    }
    
    private fun parseSecondaryStructure(lines: List<String>, map: MutableMap<String, SecondaryStructure>) {
        for (line in lines) {
            try {
                when {
                    line.startsWith("HELIX") && line.length >= 38 -> {
                        val chain = line.substring(19, 20).trim()
                        val start = line.substring(21, 25).trim().toIntOrNull() ?: continue
                        val end = line.substring(33, 37).trim().toIntOrNull() ?: continue
                        for (i in start..end) {
                            map["${chain}_$i"] = SecondaryStructure.HELIX
                        }
                    }
                    line.startsWith("SHEET") && line.length >= 38 -> {
                        val chain = line.substring(21, 22).trim()
                        val start = line.substring(22, 26).trim().toIntOrNull() ?: continue
                        val end = line.substring(33, 37).trim().toIntOrNull() ?: continue
                        for (i in start..end) {
                            map["${chain}_$i"] = SecondaryStructure.SHEET
                        }
                    }
                }
            } catch (e: Exception) {
                // Skip malformed lines
            }
        }
    }
    
    private fun parseAtoms(
        lines: List<String>,
        secondaryStructureMap: Map<String, SecondaryStructure>,
        atoms: MutableList<Atom>
    ) {
        var atomId = 0
        
        for (line in lines) {
            if (!line.startsWith("ATOM") && !line.startsWith("HETATM")) continue
            if (line.length < 54) continue
            
            try {
                val name = line.substring(12, 16).trim()
                val residueName = line.substring(17, 20).trim()
                val chain = line.substring(21, 22).trim().ifEmpty { "A" }
                val residueNumber = line.substring(22, 26).trim().toIntOrNull() ?: continue
                
                val x = line.substring(30, 38).trim().toFloatOrNull() ?: continue
                val y = line.substring(38, 46).trim().toFloatOrNull() ?: continue
                val z = line.substring(46, 54).trim().toFloatOrNull() ?: continue
                
                val occupancy = if (line.length > 60) {
                    line.substring(54, 60).trim().toFloatOrNull() ?: 1.0f
                } else 1.0f
                
                val tempFactor = if (line.length > 66) {
                    line.substring(60, 66).trim().toFloatOrNull() ?: 0.0f
                } else 0.0f
                
                val element = if (line.length > 77) {
                    line.substring(76, 78).trim().ifEmpty { 
                        name.take(1)
                    }
                } else {
                    name.take(1)
                }
                
                val key = "${chain}_${residueNumber}"
                val secondaryStructure = secondaryStructureMap[key] ?: SecondaryStructure.COIL
                
                val isBackbone = backboneAtoms.contains(name)
                val isLigand = line.startsWith("HETATM") || !standardResidues.contains(residueName)
                // iOS와 동일한 방식: 백본이 아니고 리간드가 아닌 모든 원자를 Pocket으로 간주
                val isPocket = !isBackbone && !isLigand
                
                atoms.add(Atom(
                    id = atomId++,
                    element = element,
                    name = name,
                    chain = chain,
                    residueName = residueName,
                    residueNumber = residueNumber,
                    position = Vector3(x, y, z),
                    secondaryStructure = secondaryStructure,
                    isBackbone = isBackbone,
                    isLigand = isLigand,
                    isPocket = isPocket,
                    occupancy = occupancy,
                    temperatureFactor = tempFactor
                ))
            } catch (e: Exception) {
                // Skip malformed atoms
            }
        }
    }
    
    private fun generateBonds(atoms: List<Atom>): List<Bond> {
        val bonds = mutableListOf<Bond>()
        val maxBondDistance = 1.7f // Å
        
        // Covalent radii for common elements
        val covalentRadii = mapOf(
            "C" to 0.77f, "N" to 0.75f, "O" to 0.73f,
            "S" to 1.02f, "P" to 1.06f, "H" to 0.37f
        )
        
        // Only compute bonds between nearby atoms (optimization)
        for (i in atoms.indices) {
            val atomA = atoms[i]
            
            for (j in (i + 1) until atoms.size) {
                val atomB = atoms[j]
                
                // Skip if atoms are too far apart (quick rejection)
                if (kotlin.math.abs(atomA.position.x - atomB.position.x) > maxBondDistance) continue
                if (kotlin.math.abs(atomA.position.y - atomB.position.y) > maxBondDistance) continue
                if (kotlin.math.abs(atomA.position.z - atomB.position.z) > maxBondDistance) continue
                
                val distance = (atomA.position - atomB.position).length()
                
                // Check if bond should exist based on distance
                val radiusA = covalentRadii[atomA.element] ?: 0.77f
                val radiusB = covalentRadii[atomB.element] ?: 0.77f
                val bondThreshold = (radiusA + radiusB) * 1.3f
                
                if (distance < bondThreshold) {
                    // Determine bond order (simplified)
                    val order = when {
                        distance < (radiusA + radiusB) * 0.9f -> BondOrder.TRIPLE
                        distance < (radiusA + radiusB) * 1.0f -> BondOrder.DOUBLE
                        else -> BondOrder.SINGLE
                    }
                    
                    bonds.add(Bond(
                        atomA = atomA.id,
                        atomB = atomB.id,
                        order = order,
                        distance = distance
                    ))
                }
            }
        }
        
        return bonds
    }
    
    private fun calculateBoundingBox(atoms: List<Atom>): Pair<Vector3, Vector3> {
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var minZ = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE
        var maxZ = Float.MIN_VALUE
        
        for (atom in atoms) {
            minX = minOf(minX, atom.position.x)
            minY = minOf(minY, atom.position.y)
            minZ = minOf(minZ, atom.position.z)
            maxX = maxOf(maxX, atom.position.x)
            maxY = maxOf(maxY, atom.position.y)
            maxZ = maxOf(maxZ, atom.position.z)
        }
        
        return Pair(
            Vector3(minX, minY, minZ),
            Vector3(maxX, maxY, maxZ)
        )
    }
    
    private fun calculateCenterOfMass(atoms: List<Atom>): Vector3 {
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
    
    // iOS와 동일한 방식으로 단순화: 복잡한 포켓 분석 함수 제거
    
    private fun addCalculatedAnnotations(atoms: List<Atom>, annotations: MutableList<com.avas.proteinviewer.domain.model.Annotation>) {
        // Add atom count
        val pocketCount = atoms.count { it.isPocket }
        annotations.add(Annotation(
            AnnotationType.MOLECULAR_WEIGHT,
            "${atoms.size} atoms, ${pocketCount} pocket atoms",
            "Total number of atoms and pocket atoms in structure"
        ))
    }
}

class PDBParseException(message: String) : Exception(message)
