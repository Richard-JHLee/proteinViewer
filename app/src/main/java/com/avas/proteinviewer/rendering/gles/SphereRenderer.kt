package com.avas.proteinviewer.rendering.gles

import android.opengl.GLES30
import android.util.Log
import com.avas.proteinviewer.domain.model.Atom
import com.avas.proteinviewer.domain.model.Vector3
import com.avas.proteinviewer.domain.model.ColorMode
import com.avas.proteinviewer.rendering.ColorMaps
import android.graphics.Color

/**
 * 구체 렌더링 전용 클래스
 * Spheres, Sticks, Surface 스타일에서 사용
 */
class SphereRenderer {
    
    companion object {
        private const val TAG = "SphereRenderer"
    }
    
    // 구체 메쉬 캐시 (메모리 효율성)
    private val sphereMeshCache = mutableMapOf<Int, MeshData>()
    
    /**
     * 구체 메쉬를 캐시에서 가져오거나 생성
     */
    private fun getCachedSphereMesh(radius: Float, segments: Int): MeshData {
        val key = (radius * 1000).toInt() * 1000 + segments // radius와 segments를 키로 사용
        
        return sphereMeshCache.getOrPut(key) {
            createSphereMesh(Vector3(0f, 0f, 0f), radius, segments)
        }
    }
    
    /**
     * 구체 메쉬 생성 (구현 가이드에 따라)
     */
    private fun createSphereMesh(center: Vector3, radius: Float, segments: Int): MeshData {
        val vertices = mutableListOf<Float>()
        val normals = mutableListOf<Float>()
        val indices = mutableListOf<Int>()
        
        // 구체 생성 (구현 가이드에 따라)
        val rings = segments  // 세로 링 수
        val sectors = segments  // 가로 섹터 수
        val stride = sectors + 1  // 각 링의 정점 수 (seam을 위해 +1)
        
        // 정점 생성 (구 좌표계 적용)
        for (i in 0..rings) {
            val phi = Math.PI * i / rings  // 위도 (0 ~ π)
            val cosPhi = Math.cos(phi)
            val sinPhi = Math.sin(phi)
            
            for (j in 0..sectors) {
                val theta = 2.0 * Math.PI * j / sectors  // 경도 (0 ~ 2π)
                val cosTheta = Math.cos(theta)
                val sinTheta = Math.sin(theta)
                
                // 구 좌표계: x = sinφ * cosθ * radius, y = cosφ * radius, z = sinφ * sinθ * radius
                val x = (sinPhi * cosTheta * radius).toFloat()
                val y = (cosPhi * radius).toFloat()
                val z = (sinPhi * sinTheta * radius).toFloat()
                
                // 정점 위치 (center 더하기)
                vertices.addAll(listOf(
                    center.x + x,
                    center.y + y,
                    center.z + z
                ))
                
                // 모든 정점에 동일한 노멀 벡터 적용 (무늬 제거)
                normals.addAll(listOf(0f, 0f, 1f))
            }
        }
        
        // 인덱스 생성 (stride = sectors + 1)
        val totalVertices = (rings + 1) * (sectors + 1)
        
        for (i in 0 until rings) {
            for (j in 0 until sectors) {
                val base = i * stride + j
                val nextBase = base + stride
                
                // 경계 처리: 마지막 섹터에서는 첫 번째 정점으로 연결
                val nextJ = if (j == sectors - 1) 0 else j + 1
                val baseNext = i * stride + nextJ
                val nextBaseNext = baseNext + stride
                
                // 정점 범위 검증
                if (base < totalVertices && baseNext < totalVertices && 
                    nextBase < totalVertices && nextBaseNext < totalVertices) {
                    
                    // 두 개의 삼각형
                    // 첫 번째 삼각형: (base, baseNext, nextBase)
                    indices.addAll(listOf(
                        base,
                        baseNext,
                        nextBase
                    ))
                    
                    // 두 번째 삼각형: (baseNext, nextBaseNext, nextBase)
                    indices.addAll(listOf(
                        baseNext,
                        nextBaseNext,
                        nextBase
                    ))
                } else {
                    Log.w(TAG, "Index out of bounds: base=$base, baseNext=$baseNext, nextBase=$nextBase, nextBaseNext=$nextBaseNext, totalVertices=$totalVertices")
                }
            }
        }
        
        // 디버깅: 배열 길이 확인
        val totalTriangles = rings * sectors * 2
        Log.d(TAG, "Sphere mesh: $totalVertices vertices, $totalTriangles triangles, ${vertices.size/3} actual vertices")
        
        return MeshData(vertices, normals, indices)
    }
    
    /**
     * 원자 색상 결정
     */
    private fun getAtomColor(atom: Atom, colorMode: ColorMode): List<Float> {
        return when (colorMode) {
            ColorMode.ELEMENT -> {
                val elementColor = ColorMaps.cpk(atom.element)
                listOf(
                    Color.red(elementColor) / 255f,
                    Color.green(elementColor) / 255f,
                    Color.blue(elementColor) / 255f
                )
            }
            ColorMode.CHAIN -> {
                val chainColor = ColorMaps.chainColor(atom.chain)
                listOf(
                    Color.red(chainColor) / 255f,
                    Color.green(chainColor) / 255f,
                    Color.blue(chainColor) / 255f
                )
            }
            ColorMode.UNIFORM -> {
                listOf(0.5f, 0.5f, 0.5f) // 회색 (RGB만)
            }
            ColorMode.SECONDARY_STRUCTURE -> {
                val structureColor = ColorMaps.secondaryStructureColor(atom.secondaryStructure.name)
                listOf(
                    Color.red(structureColor) / 255f,
                    Color.green(structureColor) / 255f,
                    Color.blue(structureColor) / 255f
                )
            }
        }
    }
    
    /**
     * 구체 렌더링 데이터 생성
     */
    fun createSphereRenderData(
        atoms: List<Atom>,
        colorMode: ColorMode,
        radius: Float = 0.8f,
        segments: Int = 8
    ): SphereRenderData {
        val allVertices = mutableListOf<Float>()
        val allColors = mutableListOf<Float>()
        val allNormals = mutableListOf<Float>()
        val allIndices = mutableListOf<Int>()
        
        var vertexOffset = 0
        
        atoms.forEach { atom ->
            // 원자 색상
            val atomColor = getAtomColor(atom, colorMode)
            
            // 구체 메쉬 생성
            val center = Vector3(atom.position.x, atom.position.y, atom.position.z)
            val cachedMesh = getCachedSphereMesh(radius, segments)
            val sphereMesh = MeshData(
                cachedMesh.vertices.mapIndexed { index, value ->
                    when (index % 3) {
                        0 -> value + center.x
                        1 -> value + center.y
                        2 -> value + center.z
                        else -> value
                    }
                },
                cachedMesh.normals,
                cachedMesh.indices
            )
            
            // 메쉬 데이터 추가
            allVertices.addAll(sphereMesh.vertices)
            allNormals.addAll(sphereMesh.normals)
            
            // 모든 정점에 색상 적용 (구체 하나에 하나의 색상)
            repeat(sphereMesh.vertices.size / 3) {
                allColors.addAll(atomColor)
            }
            
            // 인덱스 오프셋 적용
            sphereMesh.indices.forEach { index ->
                allIndices.add(index + vertexOffset)
            }
            
            vertexOffset += sphereMesh.vertices.size / 3
        }
        
        return SphereRenderData(allVertices, allColors, allNormals, allIndices)
    }
}

/**
 * 구체 렌더링 데이터 클래스
 */
data class SphereRenderData(
    val vertices: List<Float>,
    val colors: List<Float>,
    val normals: List<Float>,
    val indices: List<Int>
)


/**
 * 메쉬 데이터 클래스
 */
data class MeshData(
    val vertices: List<Float>,
    val normals: List<Float>,
    val indices: List<Int>
)
