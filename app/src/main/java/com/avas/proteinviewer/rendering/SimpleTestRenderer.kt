package com.avas.proteinviewer.rendering

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import com.avas.proteinviewer.data.model.PDBStructure
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * 매우 간단한 테스트 렌더러 - 빨간 네모 문제 해결용
 */
class SimpleTestRenderer : GLSurfaceView.Renderer {
    
    private var program: Int = 0
    private var positionHandle: Int = 0
    private var colorHandle: Int = 0
    
    private var sphereVertexBuffer: FloatBuffer? = null
    private var sphereIndexBuffer: java.nio.ShortBuffer? = null
    private var sphereVertexCount: Int = 0
    
    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        // 파란색 배경 설정
        GLES20.glClearColor(0.0f, 0.5f, 1.0f, 1.0f)
        
        // 매우 간단한 셰이더
        val vertexShader = """
            attribute vec4 vPosition;
            attribute vec4 vColor;
            varying vec4 fColor;
            void main() {
                fColor = vColor;
                gl_Position = vPosition;
                gl_PointSize = 50.0;
            }
        """.trimIndent()
        
        val fragmentShader = """
            precision mediump float;
            varying vec4 fColor;
            void main() {
                gl_FragColor = fColor;
            }
        """.trimIndent()
        
        program = createProgram(vertexShader, fragmentShader)
        if (program != 0) {
            positionHandle = GLES20.glGetAttribLocation(program, "vPosition")
            colorHandle = GLES20.glGetAttribLocation(program, "vColor")
            android.util.Log.d("SimpleTestRenderer", "Shader program created successfully: $program")
        } else {
            android.util.Log.e("SimpleTestRenderer", "Failed to create shader program")
        }
        
        // 테스트 데이터 생성
        createTestData()
    }
    
    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        android.util.Log.d("SimpleTestRenderer", "Surface changed: ${width}x${height}")
    }
    
    override fun onDrawFrame(gl: GL10?) {
        // 화면 지우기
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        
        if (program == 0) return
        
        // 깊이 테스트 활성화
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        
        // 뒷면 컬링 비활성화 (구체가 보이도록)
        GLES20.glDisable(GLES20.GL_CULL_FACE)
        
        // 셰이더 프로그램 사용
        GLES20.glUseProgram(program)
        
        sphereVertexBuffer?.let { vertices ->
            sphereIndexBuffer?.let { indices ->
                // 위치 설정 (x,y,z) - stride = 7 floats = 28 bytes
                GLES20.glEnableVertexAttribArray(positionHandle)
                GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 28, vertices)
                
                // 색상 설정 (r,g,b,a) - 3번째 float부터 시작
                vertices.position(3)
                GLES20.glEnableVertexAttribArray(colorHandle)
                GLES20.glVertexAttribPointer(colorHandle, 4, GLES20.GL_FLOAT, false, 28, vertices)
                vertices.position(0)
                
                // 구체 메쉬 그리기
                GLES20.glDrawElements(GLES20.GL_TRIANGLES, sphereVertexCount, GLES20.GL_UNSIGNED_SHORT, indices)
                
                GLES20.glDisableVertexAttribArray(positionHandle)
                GLES20.glDisableVertexAttribArray(colorHandle)
                
                android.util.Log.d("SimpleTestRenderer", "Drew sphere mesh with $sphereVertexCount indices")
            }
        }
    }
    
    private fun createTestData() {
        // iPhone과 동일: 기본 테스트 구체 (나중에 실제 단백질 데이터로 교체)
        android.util.Log.d("SimpleTestRenderer", "Creating test sphere mesh...")
    }
    
    private fun createProgram(vertexSource: String, fragmentSource: String): Int {
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource)
        if (vertexShader == 0) return 0
        
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
        if (fragmentShader == 0) return 0
        
        val program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)
        
        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] != GLES20.GL_TRUE) {
            android.util.Log.e("SimpleTestRenderer", "Program link failed: ${GLES20.glGetProgramInfoLog(program)}")
            GLES20.glDeleteProgram(program)
            return 0
        }
        
        return program
    }
    
    private fun loadShader(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        
        val compiled = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
        if (compiled[0] == 0) {
            android.util.Log.e("SimpleTestRenderer", "Shader compilation failed: ${GLES20.glGetShaderInfoLog(shader)}")
            GLES20.glDeleteShader(shader)
            return 0
        }
        
        return shader
    }
    
    fun updateStructure(structure: PDBStructure?) {
        android.util.Log.d("SimpleTestRenderer", "Structure updated: ${structure?.atoms?.size ?: 0} atoms")
        structure?.let { 
            createProteinSpheres(it)
        }
    }
    
    private fun createProteinSpheres(structure: PDBStructure) {
        val sphereVertices = mutableListOf<Float>()
        val sphereIndices = mutableListOf<Short>()
        
        // 각 원자를 작은 구체로 변환
        structure.atoms.forEachIndexed { index, atom ->
            val baseIndex = (index * 6).toShort()
            val radius = 0.05f // 작은 구체
            
            // 원자 위치 (정규화)
            val center = calculateCenter(structure.atoms)
            val scale = 2.0f / calculateBoundingSize(structure.atoms, center)
            val x = (atom.position.x - center.x) * scale
            val y = (atom.position.y - center.y) * scale
            val z = (atom.position.z - center.z) * scale
            
            // 원소별 색상
            val color = getAtomColor(atom.element)
            
            // 8면체 정점 추가
            sphereVertices.addAll(listOf(
                x, y + radius, z, color[0], color[1], color[2], color[3],  // 위쪽
                x, y - radius, z, color[0], color[1], color[2], color[3],  // 아래쪽
                x + radius, y, z, color[0], color[1], color[2], color[3],  // 오른쪽
                x - radius, y, z, color[0], color[1], color[2], color[3],  // 왼쪽
                x, y, z + radius, color[0], color[1], color[2], color[3],  // 앞쪽
                x, y, z - radius, color[0], color[1], color[2], color[3]   // 뒤쪽
            ))
            
            // 8면체 인덱스 추가
            sphereIndices.addAll(listOf(
                (baseIndex + 0).toShort(), (baseIndex + 2).toShort(), (baseIndex + 4).toShort(),
                (baseIndex + 0).toShort(), (baseIndex + 4).toShort(), (baseIndex + 3).toShort(),
                (baseIndex + 0).toShort(), (baseIndex + 3).toShort(), (baseIndex + 5).toShort(),
                (baseIndex + 0).toShort(), (baseIndex + 5).toShort(), (baseIndex + 2).toShort(),
                (baseIndex + 1).toShort(), (baseIndex + 4).toShort(), (baseIndex + 2).toShort(),
                (baseIndex + 1).toShort(), (baseIndex + 3).toShort(), (baseIndex + 4).toShort(),
                (baseIndex + 1).toShort(), (baseIndex + 5).toShort(), (baseIndex + 3).toShort(),
                (baseIndex + 1).toShort(), (baseIndex + 2).toShort(), (baseIndex + 5).toShort()
            ))
        }
        
        val finalVertices = sphereVertices.toFloatArray()
        val finalIndices = sphereIndices.toShortArray()
        
        sphereVertexBuffer = ByteBuffer.allocateDirect(finalVertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(finalVertices)
            .apply { position(0) }
            
        sphereIndexBuffer = ByteBuffer.allocateDirect(finalIndices.size * 2)
            .order(ByteOrder.nativeOrder())
            .asShortBuffer()
            .put(finalIndices)
            .apply { position(0) }
            
        sphereVertexCount = finalIndices.size
        
        android.util.Log.d("SimpleTestRenderer", "Protein spheres created: ${structure.atoms.size} atoms, $sphereVertexCount indices")
    }
    
    private fun getAtomColor(element: String): FloatArray {
        return when (element.uppercase()) {
            "C" -> floatArrayOf(0.2f, 0.2f, 0.2f, 1f)   // Carbon - 검은색
            "N" -> floatArrayOf(0.2f, 0.2f, 1.0f, 1f)   // Nitrogen - 파란색
            "O" -> floatArrayOf(1.0f, 0.2f, 0.2f, 1f)   // Oxygen - 빨간색
            "S" -> floatArrayOf(1.0f, 1.0f, 0.2f, 1f)   // Sulfur - 노란색
            "P" -> floatArrayOf(1.0f, 0.5f, 0.0f, 1f)   // Phosphorus - 주황색
            "H" -> floatArrayOf(1.0f, 1.0f, 1.0f, 1f)   // Hydrogen - 흰색
            else -> floatArrayOf(0.8f, 0.0f, 0.8f, 1f)  // 기타 - 보라색
        }
    }
    
    private fun calculateCenter(atoms: List<com.avas.proteinviewer.data.model.Atom>): com.avas.proteinviewer.data.model.Vector3 {
        if (atoms.isEmpty()) return com.avas.proteinviewer.data.model.Vector3(0f, 0f, 0f)
        
        var sumX = 0f
        var sumY = 0f
        var sumZ = 0f
        
        for (atom in atoms) {
            sumX += atom.position.x
            sumY += atom.position.y
            sumZ += atom.position.z
        }
        
        return com.avas.proteinviewer.data.model.Vector3(
            sumX / atoms.size,
            sumY / atoms.size,
            sumZ / atoms.size
        )
    }
    
    private fun calculateBoundingSize(atoms: List<com.avas.proteinviewer.data.model.Atom>, center: com.avas.proteinviewer.data.model.Vector3): Float {
        if (atoms.isEmpty()) return 1f
        
        var maxDistance = 0f
        
        for (atom in atoms) {
            val dx = atom.position.x - center.x
            val dy = atom.position.y - center.y
            val dz = atom.position.z - center.z
            val distance = kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
            maxDistance = kotlin.math.max(maxDistance, distance)
        }
        
        return maxDistance * 2f
    }
    
    fun onTouchStart(x: Float, y: Float) {}
    fun onTouchMove(x: Float, y: Float) {}
    fun onTouchEnd() {}
    fun zoomIn() {}
    fun zoomOut() {}
}
