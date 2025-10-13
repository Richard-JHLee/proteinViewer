package com.avas.proteinviewer.rendering

import android.graphics.Color
import com.avas.proteinviewer.domain.model.SecondaryStructure

object ColorMaps {
    // CPK 색상 체계 (아이폰과 100% 동일한 구현)
    fun cpk(element: String): Int = when (element.uppercase()) {
        // 기본 원소들 (아이폰 PDB.swift atomicColor와 100% 동일)
        "H" -> Color.rgb(255, 255, 255)  // 흰색 (1.0, 1.0, 1.0)
        "C" -> Color.rgb(51, 51, 51)     // 진한 회색 (0.2, 0.2, 0.2)
        "N" -> Color.rgb(51, 51, 255)    // 파란색 (0.2, 0.2, 1.0)
        "O" -> Color.rgb(255, 51, 51)    // 빨간색 (1.0, 0.2, 0.2)
        "S" -> Color.rgb(255, 255, 51)   // 노란색 (1.0, 1.0, 0.2)
        "P" -> Color.rgb(255, 128, 0)    // 주황색 (1.0, 0.5, 0.0)
        
        // 추가 원소들
        "F" -> Color.rgb(200, 200, 200)  // 연한 회색
        "CL" -> Color.rgb(0, 255, 0)     // 초록
        "BR" -> Color.rgb(128, 0, 0)     // 진한 빨강
        "I" -> Color.rgb(128, 0, 128)    // 보라색
        "FE" -> Color.rgb(255, 128, 0)   // 주황색
        "ZN" -> Color.rgb(128, 128, 128) // 회색
        "CA" -> Color.rgb(0, 255, 0)     // 초록
        "MG" -> Color.rgb(0, 255, 255)   // 청록색
        "NA" -> Color.rgb(0, 0, 255)     // 파랑
        "K" -> Color.rgb(255, 0, 255)    // 자홍색
        "CU" -> Color.rgb(255, 128, 0)   // 주황색
        "MN" -> Color.rgb(128, 128, 128) // 회색
        "CO" -> Color.rgb(255, 128, 0)   // 주황색
        "NI" -> Color.rgb(128, 128, 128) // 회색
        "SE" -> Color.rgb(255, 200, 50)  // 노랑
        "MO" -> Color.rgb(128, 128, 128) // 회색
        "W" -> Color.rgb(128, 128, 128)  // 회색
        "V" -> Color.rgb(128, 128, 128)  // 회색
        "CR" -> Color.rgb(128, 128, 128) // 회색
        "TI" -> Color.rgb(128, 128, 128) // 회색
        "AL" -> Color.rgb(200, 200, 200) // 연한 회색
        "SI" -> Color.rgb(200, 200, 200) // 연한 회색
        "B" -> Color.rgb(255, 200, 50)   // 노랑
        "LI" -> Color.rgb(200, 200, 200) // 연한 회색
        "BE" -> Color.rgb(200, 200, 200) // 연한 회색
        "HE" -> Color.rgb(255, 200, 50)  // 노랑
        "NE" -> Color.rgb(255, 200, 50) // 노랑
        "AR" -> Color.rgb(255, 200, 50) // 노랑
        "KR" -> Color.rgb(255, 200, 50)  // 노랑
        "XE" -> Color.rgb(255, 200, 50)  // 노랑
        "RN" -> Color.rgb(255, 200, 50) // 노랑
        else -> Color.rgb(204, 0, 204) // 아이폰과 동일: 보라색 (0.8, 0.0, 0.8)
    }
    
    // Secondary 구조 기반 색상 (제안된 구조에 맞춤)
    fun secondaryStructure(secondaryStructure: SecondaryStructure): Int = when (secondaryStructure) {
        SecondaryStructure.HELIX -> Color.rgb(255, 128, 0)   // α-helix = 주황색
        SecondaryStructure.SHEET -> Color.rgb(255, 200, 50)  // β-sheet = 노랑색
        SecondaryStructure.COIL -> Color.rgb(48, 80, 248)    // coil/loop = 파랑색
        SecondaryStructure.UNKNOWN -> Color.rgb(200, 200, 200) // 알 수 없음 = 회색
    }
    
    // 체인별 색상 (아이폰과 동일한 구현)
    fun chainColor(chainId: String): Int = when (chainId.uppercase()) {
        "A" -> Color.rgb(0, 122, 255)    // 시스템 블루
        "B" -> Color.rgb(255, 149, 0)   // 시스템 오렌지
        "C" -> Color.rgb(52, 199, 89)   // 시스템 그린
        "D" -> Color.rgb(175, 82, 222)  // 시스템 퍼플
        "E" -> Color.rgb(255, 45, 85)   // 시스템 핑크
        "F" -> Color.rgb(90, 200, 250)  // 시스템 틸
        "G" -> Color.rgb(88, 86, 214)   // 시스템 인디고
        "H" -> Color.rgb(162, 132, 94)  // 시스템 브라운
        else -> Color.rgb(142, 142, 147) // 시스템 그레이
    }
    
    // 2차 구조별 색상 (아이폰과 동일한 구현)
    fun secondaryStructureColor(structure: String): Int = when (structure.uppercase()) {
        "H", "HELIX" -> Color.rgb(255, 59, 48)    // 시스템 레드 (α-helix)
        "S", "SHEET" -> Color.rgb(255, 204, 0)    // 시스템 옐로우 (β-sheet)
        "C", "COIL" -> Color.rgb(142, 142, 147)   // 시스템 그레이 (coil)
        "L", "LOOP" -> Color.rgb(142, 142, 147)   // 시스템 그레이 (loop)
        else -> Color.rgb(0, 122, 255)             // 시스템 블루 (unknown)
    }
}




