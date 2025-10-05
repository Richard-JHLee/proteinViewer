package com.avas.proteinviewer.rendering.gles

import android.util.Log
import com.avas.proteinviewer.domain.model.Atom
import com.avas.proteinviewer.domain.model.RenderStyle
import kotlin.math.sqrt

/**
 * Level of Detail (LOD) 관리자
 * 단백질 복잡도와 성능에 따라 렌더링 품질을 동적으로 조절
 */
class LODManager {
    
    companion object {
        private const val TAG = "LODManager"
        
        // LOD 레벨 정의
        const val LOD_ULTRA = 0    // 최고 품질
        const val LOD_HIGH = 1     // 고품질
        const val LOD_MEDIUM = 2   // 중품질
        const val LOD_LOW = 3      // 저품질
        const val LOD_ULTRA_LOW = 4 // 최저 품질
    }
    
    data class LODSettings(
        val level: Int,
        val maxAtoms: Int,
        val sphereSegments: Int,
        val tubeSegments: Int,
        val ribbonSegments: Int,
        val surfaceQuality: Float,
        val enableInstancing: Boolean,
        val enableShadows: Boolean,
        val enableReflections: Boolean
    )
    
    // 렌더링 스타일별 LOD 설정
    private val lodSettings = mapOf(
        LOD_ULTRA to LODSettings(
            level = LOD_ULTRA,
            maxAtoms = Int.MAX_VALUE,
            sphereSegments = 32,
            tubeSegments = 16,
            ribbonSegments = 12,
            surfaceQuality = 1.0f,
            enableInstancing = true,
            enableShadows = true,
            enableReflections = true
        ),
        LOD_HIGH to LODSettings(
            level = LOD_HIGH,
            maxAtoms = 50000,
            sphereSegments = 24,
            tubeSegments = 12,
            ribbonSegments = 10,
            surfaceQuality = 0.8f,
            enableInstancing = true,
            enableShadows = true,
            enableReflections = false
        ),
        LOD_MEDIUM to LODSettings(
            level = LOD_MEDIUM,
            maxAtoms = 25000,
            sphereSegments = 16,
            tubeSegments = 8,
            ribbonSegments = 8,
            surfaceQuality = 0.6f,
            enableInstancing = true,
            enableShadows = false,
            enableReflections = false
        ),
        LOD_LOW to LODSettings(
            level = LOD_LOW,
            maxAtoms = 10000,
            sphereSegments = 12,
            tubeSegments = 6,
            ribbonSegments = 6,
            surfaceQuality = 0.4f,
            enableInstancing = false,
            enableShadows = false,
            enableReflections = false
        ),
        LOD_ULTRA_LOW to LODSettings(
            level = LOD_ULTRA_LOW,
            maxAtoms = 5000,
            sphereSegments = 8,
            tubeSegments = 4,
            ribbonSegments = 4,
            surfaceQuality = 0.2f,
            enableInstancing = false,
            enableShadows = false,
            enableReflections = false
        )
    )
    
    private var currentLOD = LOD_MEDIUM
    private var performanceMetrics = PerformanceMetrics()
    
    data class PerformanceMetrics(
        var frameTime: Float = 16.67f, // 60 FPS 기준
        var drawCalls: Int = 0,
        var vertexCount: Int = 0,
        var memoryUsage: Long = 0,
        var lastUpdateTime: Long = System.currentTimeMillis()
    )
    
    /**
     * 단백질 복잡도에 따른 LOD 레벨 결정
     */
    fun determineLODLevel(atoms: List<Atom>, renderStyle: RenderStyle): Int {
        val atomCount = atoms.size
        val complexity = calculateComplexity(atoms)
        
        // 복잡도 기반 LOD 결정
        val lodLevel = when {
            complexity > 0.9f -> LOD_ULTRA_LOW
            complexity > 0.7f -> LOD_LOW
            complexity > 0.5f -> LOD_MEDIUM
            complexity > 0.3f -> LOD_HIGH
            else -> LOD_ULTRA
        }
        
        // 렌더링 스타일별 조정
        val adjustedLOD = adjustLODForStyle(lodLevel, renderStyle)
        
        // 성능 메트릭 기반 동적 조정
        val finalLOD = adjustLODForPerformance(adjustedLOD)
        
        Log.d(TAG, "LOD determined: atoms=$atomCount, complexity=$complexity, style=$renderStyle, finalLOD=$finalLOD")
        return finalLOD
    }
    
    /**
     * 단백질 복잡도 계산
     */
    private fun calculateComplexity(atoms: List<Atom>): Float {
        val atomCount = atoms.size
        val chainCount = atoms.map { it.chain }.distinct().size
        val residueCount = atoms.map { it.residueNumber }.distinct().size
        
        // 복잡도 공식: 원자 수, 체인 수, 잔기 수를 종합
        val atomComplexity = (atomCount / 10000f).coerceAtMost(1.0f)
        val chainComplexity = (chainCount / 10f).coerceAtMost(1.0f)
        val residueComplexity = (residueCount / 1000f).coerceAtMost(1.0f)
        
        return (atomComplexity * 0.6f + chainComplexity * 0.2f + residueComplexity * 0.2f)
    }
    
    /**
     * 렌더링 스타일별 LOD 조정
     */
    private fun adjustLODForStyle(baseLOD: Int, renderStyle: RenderStyle): Int {
        return when (renderStyle) {
            RenderStyle.SPHERES -> {
                // Spheres는 가장 무거우므로 LOD를 한 단계 낮춤
                (baseLOD + 1).coerceAtMost(LOD_ULTRA_LOW)
            }
            RenderStyle.SURFACE -> {
                // Surface는 매우 무거우므로 LOD를 두 단계 낮춤
                (baseLOD + 2).coerceAtMost(LOD_ULTRA_LOW)
            }
            RenderStyle.STICKS -> {
                // Sticks는 중간 정도
                baseLOD
            }
            RenderStyle.RIBBON, RenderStyle.CARTOON -> {
                // Ribbon/Cartoon은 상대적으로 가벼움
                (baseLOD - 1).coerceAtLeast(LOD_ULTRA)
            }
        }
    }
    
    /**
     * 성능 메트릭 기반 LOD 동적 조정
     */
    private fun adjustLODForPerformance(baseLOD: Int): Int {
        
        // 프레임 시간이 너무 길면 LOD를 낮춤
        if (performanceMetrics.frameTime > 33.33f) { // 30 FPS 미만
            val adjustedLOD = (baseLOD + 1).coerceAtMost(LOD_ULTRA_LOW)
            Log.d(TAG, "Performance adjustment: frameTime=${performanceMetrics.frameTime}ms, LOD $baseLOD -> $adjustedLOD")
            return adjustedLOD
        }
        
        // 프레임 시간이 좋으면 LOD를 높일 수 있음
        if (performanceMetrics.frameTime < 16.67f && baseLOD > LOD_ULTRA) { // 60 FPS 이상
            val adjustedLOD = (baseLOD - 1).coerceAtLeast(LOD_ULTRA)
            Log.d(TAG, "Performance adjustment: frameTime=${performanceMetrics.frameTime}ms, LOD $baseLOD -> $adjustedLOD")
            return adjustedLOD
        }
        
        return baseLOD
    }
    
    /**
     * 현재 LOD 설정 가져오기
     */
    fun getCurrentLODSettings(): LODSettings {
        return lodSettings[currentLOD] ?: lodSettings[LOD_MEDIUM]!!
    }
    
    /**
     * LOD 레벨 설정
     */
    fun setLODLevel(level: Int) {
        if (level in LOD_ULTRA..LOD_ULTRA_LOW) {
            currentLOD = level
            Log.d(TAG, "LOD level set to: $level")
        }
    }
    
    /**
     * 성능 메트릭 업데이트
     */
    fun updatePerformanceMetrics(
        frameTime: Float,
        drawCalls: Int,
        vertexCount: Int,
        memoryUsage: Long
    ) {
        performanceMetrics = PerformanceMetrics(
            frameTime = frameTime,
            drawCalls = drawCalls,
            vertexCount = vertexCount,
            memoryUsage = memoryUsage,
            lastUpdateTime = System.currentTimeMillis()
        )
    }
    
    /**
     * 원자 리스트를 LOD에 맞게 필터링
     */
    fun filterAtomsForLOD(atoms: List<Atom>, renderStyle: RenderStyle): List<Atom> {
        val settings = getCurrentLODSettings()
        
        if (atoms.size <= settings.maxAtoms) {
            return atoms
        }
        
        // 원자 수가 제한을 초과하면 샘플링
        val step = atoms.size / settings.maxAtoms
        val filteredAtoms = atoms.filterIndexed { index, _ -> index % step == 0 }
        
        Log.d(TAG, "Atoms filtered: ${atoms.size} -> ${filteredAtoms.size} (LOD ${settings.level})")
        return filteredAtoms
    }
    
    /**
     * 렌더링 스타일별 최적화된 세그먼트 수 가져오기
     */
    fun getOptimalSegments(renderStyle: RenderStyle): Int {
        val settings = getCurrentLODSettings()
        return when (renderStyle) {
            RenderStyle.SPHERES -> settings.sphereSegments
            RenderStyle.RIBBON, RenderStyle.CARTOON -> settings.tubeSegments
            RenderStyle.STICKS -> settings.ribbonSegments
            RenderStyle.SURFACE -> (settings.sphereSegments * settings.surfaceQuality).toInt()
        }
    }
    
    /**
     * 인스턴싱 사용 여부 확인
     */
    fun shouldUseInstancing(): Boolean {
        return getCurrentLODSettings().enableInstancing
    }
    
    /**
     * 그림자 렌더링 사용 여부 확인
     */
    fun shouldRenderShadows(): Boolean {
        return getCurrentLODSettings().enableShadows
    }
    
    /**
     * 반사 렌더링 사용 여부 확인
     */
    fun shouldRenderReflections(): Boolean {
        return getCurrentLODSettings().enableReflections
    }
    
    /**
     * LOD 통계 정보
     */
    fun getLODStats(): String {
        val settings = getCurrentLODSettings()
        return "LOD Level: ${settings.level}, Max Atoms: ${settings.maxAtoms}, " +
                "Sphere Segments: ${settings.sphereSegments}, Tube Segments: ${settings.tubeSegments}, " +
                "Instancing: ${settings.enableInstancing}, Shadows: ${settings.enableShadows}"
    }
}
