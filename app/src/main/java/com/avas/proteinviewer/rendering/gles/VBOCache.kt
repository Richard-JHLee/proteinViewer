package com.avas.proteinviewer.rendering.gles

import android.opengl.GLES30
import android.util.Log
import android.util.LruCache
import com.avas.proteinviewer.domain.model.Atom
import com.avas.proteinviewer.domain.model.ColorMode
import com.avas.proteinviewer.domain.model.RenderStyle
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * VBO 캐싱 시스템으로 메모리 사용량 최적화
 * 동일한 렌더링 파라미터에 대해 VBO를 재사용
 */
class VBOCache {
    
    companion object {
        private const val TAG = "VBOCache"
        private const val MAX_CACHE_SIZE = 20 // 최대 캐시 크기
    }
    
    // VBO 캐시 (LRU 방식)
    private val vboCache = LruCache<String, CachedVBO>(MAX_CACHE_SIZE)
    
    // 메쉬 캐시 (구체, 실린더 등)
    private val meshCache = LruCache<String, MeshData>(10)
    
    data class CachedVBO(
        val vertexVbo: Int,
        val colorVbo: Int,
        val normalVbo: Int,
        val indexVbo: Int,
        val vertexCount: Int,
        val indexCount: Int,
        val lastUsed: Long = System.currentTimeMillis()
    )
    
    data class MeshData(
        val vertices: FloatArray,
        val normals: FloatArray,
        val indices: IntArray
    )
    
    /**
     * 캐시 키 생성
     */
    private fun generateCacheKey(
        atoms: List<Atom>,
        renderStyle: RenderStyle,
        colorMode: ColorMode,
        lodLevel: Int
    ): String {
        val atomCount = atoms.size
        val firstAtom = atoms.firstOrNull()
        val lastAtom = atoms.lastOrNull()
        
        return "${renderStyle.name}_${colorMode.name}_${lodLevel}_${atomCount}_${firstAtom?.id}_${lastAtom?.id}"
    }
    
    /**
     * VBO 캐시에서 데이터 가져오기
     */
    fun getCachedVBO(
        atoms: List<Atom>,
        renderStyle: RenderStyle,
        colorMode: ColorMode,
        lodLevel: Int
    ): CachedVBO? {
        val key = generateCacheKey(atoms, renderStyle, colorMode, lodLevel)
        val cached = vboCache.get(key)
        
        if (cached != null) {
            Log.d(TAG, "VBO cache hit: $key")
            return cached
        }
        
        Log.d(TAG, "VBO cache miss: $key")
        return null
    }
    
    /**
     * VBO 캐시에 데이터 저장
     */
    fun putCachedVBO(
        atoms: List<Atom>,
        renderStyle: RenderStyle,
        colorMode: ColorMode,
        lodLevel: Int,
        vbo: CachedVBO
    ) {
        val key = generateCacheKey(atoms, renderStyle, colorMode, lodLevel)
        vboCache.put(key, vbo)
        Log.d(TAG, "VBO cached: $key")
    }
    
    /**
     * 메쉬 캐시에서 데이터 가져오기
     */
    fun getCachedMesh(meshType: String, radius: Float, segments: Int): MeshData? {
        val key = "${meshType}_${radius}_${segments}"
        val cached = meshCache.get(key)
        
        if (cached != null) {
            Log.d(TAG, "Mesh cache hit: $key")
            return cached
        }
        
        Log.d(TAG, "Mesh cache miss: $key")
        return null
    }
    
    /**
     * 메쉬 캐시에 데이터 저장
     */
    fun putCachedMesh(meshType: String, radius: Float, segments: Int, mesh: MeshData) {
        val key = "${meshType}_${radius}_${segments}"
        meshCache.put(key, mesh)
        Log.d(TAG, "Mesh cached: $key")
    }
    
    /**
     * VBO 생성 (캐시된 데이터 사용)
     */
    fun createVBO(
        vertices: FloatArray,
        colors: FloatArray,
        normals: FloatArray,
        indices: IntArray
    ): CachedVBO {
        val buffers = IntArray(4)
        GLES30.glGenBuffers(4, buffers, 0)
        
        val vertexVbo = buffers[0]
        val colorVbo = buffers[1]
        val normalVbo = buffers[2]
        val indexVbo = buffers[3]
        
        // 정점 데이터 업로드
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vertexVbo)
        val vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        vertexBuffer.put(vertices)
        vertexBuffer.flip()
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vertexBuffer.capacity() * 4, vertexBuffer, GLES30.GL_STATIC_DRAW)
        
        // 색상 데이터 업로드
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, colorVbo)
        val colorBuffer = ByteBuffer.allocateDirect(colors.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        colorBuffer.put(colors)
        colorBuffer.flip()
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, colorBuffer.capacity() * 4, colorBuffer, GLES30.GL_STATIC_DRAW)
        
        // 노멀 데이터 업로드
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, normalVbo)
        val normalBuffer = ByteBuffer.allocateDirect(normals.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        normalBuffer.put(normals)
        normalBuffer.flip()
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, normalBuffer.capacity() * 4, normalBuffer, GLES30.GL_STATIC_DRAW)
        
        // 인덱스 데이터 업로드
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, indexVbo)
        val indexBuffer = ByteBuffer.allocateDirect(indices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asIntBuffer()
        indexBuffer.put(indices)
        indexBuffer.flip()
        GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, indexBuffer.capacity() * 4, indexBuffer, GLES30.GL_STATIC_DRAW)
        
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, 0)
        
        return CachedVBO(
            vertexVbo = vertexVbo,
            colorVbo = colorVbo,
            normalVbo = normalVbo,
            indexVbo = indexVbo,
            vertexCount = vertices.size / 3,
            indexCount = indices.size
        )
    }
    
    /**
     * VBO 삭제
     */
    fun deleteVBO(vbo: CachedVBO) {
        val buffers = intArrayOf(vbo.vertexVbo, vbo.colorVbo, vbo.normalVbo, vbo.indexVbo)
        GLES30.glDeleteBuffers(4, buffers, 0)
        Log.d(TAG, "VBO deleted: ${vbo.vertexVbo}, ${vbo.colorVbo}, ${vbo.normalVbo}, ${vbo.indexVbo}")
    }
    
    /**
     * 캐시 정리 (오래된 VBO 삭제)
     */
    fun cleanup() {
        val currentTime = System.currentTimeMillis()
        val maxAge = 30000L // 30초
        
        val iterator = vboCache.snapshot().iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (currentTime - entry.value.lastUsed > maxAge) {
                deleteVBO(entry.value)
                vboCache.remove(entry.key)
            }
        }
        
        Log.d(TAG, "Cache cleanup completed. VBO cache size: ${vboCache.size()}, Mesh cache size: ${meshCache.size()}")
    }
    
    /**
     * 모든 캐시 정리
     */
    fun clearAll() {
        val vboSnapshot = vboCache.snapshot()
        for (entry in vboSnapshot) {
            deleteVBO(entry.value)
        }
        vboCache.evictAll()
        meshCache.evictAll()
        Log.d(TAG, "All caches cleared")
    }
    
    /**
     * 캐시 통계
     */
    fun getCacheStats(): String {
        return "VBO Cache: ${vboCache.size()}/$MAX_CACHE_SIZE, Mesh Cache: ${meshCache.size()}/10"
    }
}
