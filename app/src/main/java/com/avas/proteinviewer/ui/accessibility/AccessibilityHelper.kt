package com.avas.proteinviewer.ui.accessibility

import com.avas.proteinviewer.data.model.PDBStructure
import com.avas.proteinviewer.data.model.Atom
import com.avas.proteinviewer.data.model.Bond

object AccessibilityHelper {
    fun getProteinStructureDescription(structure: PDBStructure?): String {
        return structure?.let {
            "Protein structure with ${it.atoms.size} atoms, ${it.bonds.size} bonds, " +
            "${it.chains.size} chains, and ${it.residues.size} residues"
        } ?: "No protein structure loaded"
    }
    
    fun getAtomDescription(atom: Atom): String {
        return "Atom ${atom.element} at position ${atom.position.x}, ${atom.position.y}, ${atom.position.z}"
    }
    
    fun getBondDescription(bond: Bond): String {
        return "Bond between atoms ${bond.atomA} and ${bond.atomB}"
    }
    
    fun getChainDescription(chain: String, residueCount: Int): String {
        return "Chain $chain with $residueCount residues"
    }
    
    fun getResidueDescription(residueName: String, residueNumber: Int, chain: String): String {
        return "Residue $residueName $residueNumber in chain $chain"
    }
    
    fun getStatCardDescription(value: String, label: String): String {
        return "$label: $value"
    }
    
    fun getTabDescription(tabName: String, isSelected: Boolean): String {
        return if (isSelected) {
            "$tabName tab, currently selected"
        } else {
            "$tabName tab, tap to select"
        }
    }
    
    fun getNavigationItemDescription(itemName: String, isSelected: Boolean): String {
        return if (isSelected) {
            "$itemName, currently selected"
        } else {
            "$itemName, tap to navigate"
        }
    }
    
    fun getButtonDescription(buttonName: String, isEnabled: Boolean): String {
        return if (isEnabled) {
            "$buttonName button, tap to activate"
        } else {
            "$buttonName button, currently disabled"
        }
    }
    
    fun getLoadingDescription(progress: String?): String {
        return if (progress != null) {
            "Loading protein structure, $progress"
        } else {
            "Loading protein structure, please wait"
        }
    }
    
    fun getErrorDescription(error: String?): String {
        return "Error loading protein structure: ${error ?: "Unknown error"}"
    }
}
