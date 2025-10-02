package com.avas.proteinviewer.rendering

import android.graphics.Color

object ColorMaps {
    // 매우 간단한 CPK 일부 (필요시 확장)
    fun cpk(element: String): Int = when (element.uppercase()) {
        "H" -> Color.rgb(255,255,255)
        "C" -> Color.rgb(50,50,50)
        "N" -> Color.rgb(48,80,248)
        "O" -> Color.rgb(255,13,13)
        "S" -> Color.rgb(255,200,50)
        "P" -> Color.rgb(255,128,0)
        "F" -> Color.rgb(200,200,200)
        "CL" -> Color.rgb(0,255,0)
        "BR" -> Color.rgb(128,0,0)
        "I" -> Color.rgb(128,0,128)
        "FE" -> Color.rgb(255,128,0)
        "ZN" -> Color.rgb(128,128,128)
        "CA" -> Color.rgb(0,255,0)
        "MG" -> Color.rgb(0,255,255)
        else -> Color.rgb(200,200,200)
    }
}




