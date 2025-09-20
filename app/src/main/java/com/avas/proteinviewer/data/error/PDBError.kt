package com.avas.proteinviewer.data.error

sealed class PDBError : Exception() {
    data class InvalidPDBID(val id: String) : PDBError()
    data class StructureNotFound(val id: String) : PDBError()
    data class ServerError(val code: Int) : PDBError()
    object InvalidResponse : PDBError()
    object EmptyResponse : PDBError()
    object NetworkUnavailable : PDBError()
    object Timeout : PDBError()
    
    val userFriendlyMessage: String
        get() = when (this) {
            is InvalidPDBID -> "Invalid protein ID '$id'. Please check the ID and try again."
            is StructureNotFound -> "Protein structure '$id' not found in the database. Please try a different protein."
            is ServerError -> "Server error ($code). Please check your internet connection and try again."
            is InvalidResponse -> "Invalid response from server. Please try again."
            is EmptyResponse -> "No data received from server. Please try again."
            is NetworkUnavailable -> "No internet connection. Please check your network and try again."
            is Timeout -> "Request timed out. Please check your connection and try again."
        }
}

sealed class PDBParseError : Exception() {
    data class InvalidFormat(override val message: String) : PDBParseError()
    object NoValidAtoms : PDBParseError()
    data class CorruptedData(override val message: String) : PDBParseError()
}
