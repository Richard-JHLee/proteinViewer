package com.avas.proteinviewer.data.converter

import com.avas.proteinviewer.data.model.PDBStructure
import com.avas.proteinviewer.data.model.FilamentStructure
import com.avas.proteinviewer.data.model.FilamentAtom
import com.avas.proteinviewer.data.model.FilamentBond
import com.avas.proteinviewer.data.model.FilamentSecondary
import com.avas.proteinviewer.data.model.FilamentLigand

object StructureConverter {
    fun convertPDBToStructure(pdbStructure: PDBStructure): FilamentStructure {
        // 원자 변환
        val atoms = pdbStructure.atoms.map { atom ->
            FilamentAtom(
                id = atom.id,
                chain = atom.chain,
                resName = atom.residueName,
                x = atom.position.x,
                y = atom.position.y,
                z = atom.position.z,
                element = atom.element
            )
        }
        
        // 결합 변환
        val bonds = pdbStructure.bonds.map { bond ->
            FilamentBond(
                a = bond.atomA,
                b = bond.atomB,
                order = when (bond.order) {
                    com.avas.proteinviewer.data.model.BondOrder.SINGLE -> 1
                    com.avas.proteinviewer.data.model.BondOrder.DOUBLE -> 2
                    com.avas.proteinviewer.data.model.BondOrder.TRIPLE -> 3
                    else -> 1
                }
            )
        }
        
        // 체인 정보 생성
        val chains = atoms.groupBy { it.chain }.mapValues { (_, chainAtoms) ->
            chainAtoms.map { it.id }.toIntArray()
        }
        
        // 이차구조 변환
        val secondary = pdbStructure.atoms.mapNotNull { atom ->
            when (atom.secondaryStructure) {
                com.avas.proteinviewer.data.model.SecondaryStructure.HELIX -> 
                    FilamentSecondary(
                        start = atom.residueNumber,
                        end = atom.residueNumber,
                        type = "helix"
                    )
                com.avas.proteinviewer.data.model.SecondaryStructure.SHEET -> 
                    FilamentSecondary(
                        start = atom.residueNumber,
                        end = atom.residueNumber,
                        type = "sheet"
                    )
                else -> null
            }
        }.distinctBy { "${it.start}_${it.end}" }
        
        // 리간드 정보 생성
        val ligands = atoms.filter { atom -> 
            pdbStructure.atoms.find { it.id == atom.id }?.isLigand == true
        }.groupBy { "${it.chain}_${it.resName}_${it.id}" }
            .map { (key, ligandAtoms) ->
                FilamentLigand(
                    name = ligandAtoms.first().resName,
                    atomIds = ligandAtoms.map { it.id }.toIntArray()
                )
            }
        
        return FilamentStructure(
            atoms = atoms,
            bonds = bonds,
            chains = chains,
            secondary = secondary,
            ligands = ligands
        )
    }
}
