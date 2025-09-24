package com.avas.proteinviewer.util

import java.util.Locale

/**
 * Language Detection Utility
 * iPhone LanguageHelper와 동일한 기능을 제공하는 Android 버전
 */
object LanguageHelper {
    
    /**
     * 현재 시스템 언어가 한국어인지 확인
     * iPhone의 isKorean과 동일한 로직
     */
    val isKorean: Boolean
        get() {
            val currentLocale = Locale.getDefault()
            val languageCode = currentLocale.language.lowercase()
            val country = currentLocale.country.lowercase()
            
            // 다양한 한국어 로케일 형식 지원
            return languageCode == "ko" || 
                   currentLocale.toString().startsWith("ko") ||
                   currentLocale.toString().contains("ko_KR") ||
                   currentLocale.toString().contains("ko-KR") ||
                   (languageCode == "ko" && country == "kr")
        }
    
    /**
     * 현재 시스템 언어 코드 반환
     */
    val currentLanguageCode: String
        get() = Locale.getDefault().language ?: "en"
    
    /**
     * 현재 시스템 로케일 식별자 반환
     */
    val currentLocaleIdentifier: String
        get() = Locale.getDefault().toString()
    
    /**
     * 다국어 텍스트를 위한 헬퍼 함수
     * iPhone의 localizedText와 동일한 기능
     * 
     * @param korean 한국어 텍스트
     * @param english 영어 텍스트
     * @return 현재 언어에 맞는 텍스트
     */
    fun localizedText(korean: String, english: String): String {
        return if (isKorean) korean else english
    }
}

/**
 * String Extension for localization
 * iPhone의 String extension과 동일한 기능
 */
fun String.localized(english: String): String {
    return if (LanguageHelper.isKorean) this else english
}

