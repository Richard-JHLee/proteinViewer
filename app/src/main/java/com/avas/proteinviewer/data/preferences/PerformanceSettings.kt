package com.avas.proteinviewer.data.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 성능 최적화 설정을 관리하는 클래스
 * SharedPreferences를 사용하여 설정을 저장하고 Flow로 변경사항을 관찰
 */
@Singleton
class PerformanceSettings @Inject constructor(
    context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "performance_settings",
        Context.MODE_PRIVATE
    )
    
    // 성능 최적화 활성화 여부
    private val _enableOptimization = MutableStateFlow(
        prefs.getBoolean(KEY_ENABLE_OPTIMIZATION, true)
    )
    val enableOptimization: StateFlow<Boolean> = _enableOptimization.asStateFlow()
    
    // 최대 원자 수 제한 (1000-10000, 기본값: 5000)
    private val _maxAtomsLimit = MutableStateFlow(
        prefs.getInt(KEY_MAX_ATOMS_LIMIT, 5000)
    )
    val maxAtomsLimit: StateFlow<Int> = _maxAtomsLimit.asStateFlow()
    
    // 샘플링 비율 (0.05-0.5, 기본값: 0.25 = 25%)
    private val _samplingRatio = MutableStateFlow(
        prefs.getFloat(KEY_SAMPLING_RATIO, 0.25f)
    )
    val samplingRatio: StateFlow<Float> = _samplingRatio.asStateFlow()
    
    /**
     * 성능 최적화 활성화 여부 설정
     */
    fun setEnableOptimization(enabled: Boolean) {
        _enableOptimization.value = enabled
        prefs.edit().putBoolean(KEY_ENABLE_OPTIMIZATION, enabled).apply()
    }
    
    /**
     * 최대 원자 수 제한 설정
     * @param limit 1000-10000 범위의 원자 수
     */
    fun setMaxAtomsLimit(limit: Int) {
        val validLimit = limit.coerceIn(1000, 10000)
        _maxAtomsLimit.value = validLimit
        prefs.edit().putInt(KEY_MAX_ATOMS_LIMIT, validLimit).apply()
    }
    
    /**
     * 샘플링 비율 설정
     * @param ratio 0.05-0.5 범위의 비율 (5%-50%)
     */
    fun setSamplingRatio(ratio: Float) {
        val validRatio = ratio.coerceIn(0.05f, 0.5f)
        _samplingRatio.value = validRatio
        prefs.edit().putFloat(KEY_SAMPLING_RATIO, validRatio).apply()
    }
    
    /**
     * 현재 설정 값 가져오기 (Flow가 아닌 즉시 값)
     */
    fun getEnableOptimization(): Boolean = _enableOptimization.value
    fun getMaxAtomsLimit(): Int = _maxAtomsLimit.value
    fun getSamplingRatio(): Float = _samplingRatio.value
    
    /**
     * 설정을 기본값으로 초기화
     */
    fun resetToDefaults() {
        setEnableOptimization(true)
        setMaxAtomsLimit(5000)
        setSamplingRatio(0.25f)
    }
    
    companion object {
        private const val KEY_ENABLE_OPTIMIZATION = "enable_optimization"
        private const val KEY_MAX_ATOMS_LIMIT = "max_atoms_limit"
        private const val KEY_SAMPLING_RATIO = "sampling_ratio"
    }
}

