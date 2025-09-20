package com.avas.proteinviewer.data.api.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RcsbEntryDto(
    @Json(name = "rcsb_id") val rcsbId: String? = null,
    @Json(name = "struct") val struct: RcsbStructDto? = null
)

@JsonClass(generateAdapter = true)
data class RcsbStructDto(
    @Json(name = "title") val title: String? = null
)
