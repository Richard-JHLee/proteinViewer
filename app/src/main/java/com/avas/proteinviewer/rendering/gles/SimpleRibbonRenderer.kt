package com.avas.proteinviewer.rendering.gles

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Log
import com.avas.proteinviewer.domain.model.PDBStructure
import com.avas.proteinviewer.domain.model.Atom
import com.avas.proteinviewer.rendering.ColorMaps
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.tan

/**
 * OpenGL ES 3.0 Ribbon 렌더러
 * 아이폰의 Ribbon 스타일을 모방하여 CA (alpha carbon) 원자들을 선으로 연결
 */
class SimpleRibbonRenderer : GLSurfaceView.Renderer {

    private val camera = ArcballCamera()

    private val projectionMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    private var program = 0
    private var aPositionHandle = 0
    private var aColorHandle = 0
    private var uMvpHandle = 0
    private var uLineWidthHandle = 0

    private var lineVbo = 0
    private var colorVbo = 0
    private var vao = 0

    private var lineVertexCount = 0
    private var lineWidth = 3.0f

    private var buffersReady = false
    private var pendingStructure: PDBStructure? = null

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        Log.d(TAG, "onSurfaceCreated - Ribbon Renderer")
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)
        GLES30.glClearColor(1f, 1f, 1f, 1f)
        GLES30.glLineWidth(lineWidth) // OpenGL ES 3.0에서는 lineWidth가 제한적일 수 있음

        try {
            program = createProgram(VERT_SHADER, FRAG_SHADER)
        } catch (ex: RuntimeException) {
            Log.e(TAG, "Failed to create shader program", ex)
            program = 0
            buffersReady = false
            return
        }

        aPositionHandle = GLES30.glGetAttribLocation(program, "aPosition")
        aColorHandle = GLES30.glGetAttribLocation(program, "aColor")
        uMvpHandle = GLES30.glGetUniformLocation(program, "uMvp")
        uLineWidthHandle = GLES30.glGetUniformLocation(program, "uLineWidth")

        val buffers = IntArray(2)
        GLES30.glGenBuffers(2, buffers, 0)
        lineVbo = buffers[0]
        colorVbo = buffers[1]

        val vaos = IntArray(1)
        GLES30.glGenVertexArrays(1, vaos, 0)
        vao = vaos[0]

        buffersReady = lineVbo != 0 && colorVbo != 0 && vao != 0
        pendingStructure?.let {
            uploadStructure(it)
            pendingStructure = null
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        val safeWidth = max(1, width)
        val safeHeight = max(1, height)
        GLES30.glViewport(0, 0, safeWidth, safeHeight)

        val aspect = safeWidth.toFloat() / safeHeight.toFloat()
        Matrix.perspectiveM(projectionMatrix, 0, camera.fovDeg, aspect, 0.1f, 2000f)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)
        if (lineVertexCount == 0 || program == 0 || !buffersReady) return

        GLES30.glUseProgram(program)
        val viewMatrix = camera.viewMatrix()
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

        GLES30.glBindVertexArray(vao)

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, lineVbo)
        GLES30.glEnableVertexAttribArray(aPositionHandle)
        GLES30.glVertexAttribPointer(aPositionHandle, 3, GLES30.GL_FLOAT, false, 0, 0)

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, colorVbo)
        GLES30.glEnableVertexAttribArray(aColorHandle)
        GLES30.glVertexAttribPointer(aColorHandle, 3, GLES30.GL_FLOAT, false, 0, 0)

        GLES30.glUniformMatrix4fv(uMvpHandle, 1, false, mvpMatrix, 0)
        GLES30.glUniform1f(uLineWidthHandle, lineWidth)

        // LINE_STRIP으로 CA 원자들을 연결 (임시 - 추후 삼각형 메쉬로 변경 예정)
        GLES30.glLineWidth(5.0f) // 더 두껍게
        GLES30.glDrawArrays(GLES30.GL_LINE_STRIP, 0, lineVertexCount)

        GLES30.glDisableVertexAttribArray(aPositionHandle)
        GLES30.glDisableVertexAttribArray(aColorHandle)
        GLES30.glBindVertexArray(0)
    }

    fun updateStructure(structure: PDBStructure?) {
        pendingStructure = structure
        if (!buffersReady) {
            lineVertexCount = 0
            return
        }
        uploadStructure(structure)
    }

    fun orbit(deltaX: Float, deltaY: Float) {
        camera.orbit(deltaX, deltaY)
    }

    fun pan(deltaX: Float, deltaY: Float) {
        camera.pan(deltaX, deltaY)
    }

    fun zoom(scaleFactor: Float) {
        camera.zoom(scaleFactor)
    }

    private fun uploadStructure(structure: PDBStructure?) {
        if (structure == null || structure.atoms.isEmpty()) {
            lineVertexCount = 0
            return
        }

        // CA (alpha carbon) 원자만 필터링
        val caAtoms = structure.atoms.filter { atom ->
            atom.name.trim().equals("CA", ignoreCase = true)
        }.sortedBy { it.residueNumber } // 순서대로 정렬

        if (caAtoms.isEmpty()) {
            Log.w(TAG, "No CA atoms found, using all atoms")
            // CA가 없으면 모든 원자 사용 (폴백)
            uploadAllAtoms(structure.atoms)
            return
        }

        Log.d(TAG, "Found ${caAtoms.size} CA atoms for ribbon rendering")

        // 체인별로 그룹화하여 선 그리기
        val chainGroups = caAtoms.groupBy { it.chain }
        var totalVertices = 0

        val positionList = mutableListOf<Float>()
        val colorList = mutableListOf<Float>()

        chainGroups.forEach { (chain, atoms) ->
            val sortedAtoms = atoms.sortedBy { it.residueNumber }

            // 바운딩 박스 계산 (카메라 설정용)
            var minX = Float.POSITIVE_INFINITY
            var minY = Float.POSITIVE_INFINITY
            var minZ = Float.POSITIVE_INFINITY
            var maxX = Float.NEGATIVE_INFINITY
            var maxY = Float.NEGATIVE_INFINITY
            var maxZ = Float.NEGATIVE_INFINITY

            sortedAtoms.forEach { atom ->
                val p = atom.position
                if (p.x < minX) minX = p.x
                if (p.y < minY) minY = p.y
                if (p.z < minZ) minZ = p.z
                if (p.x > maxX) maxX = p.x
                if (p.y > maxY) maxY = p.y
                if (p.z > maxZ) maxZ = p.z
            }

            val centerX = (minX + maxX) * 0.5f
            val centerY = (minY + maxY) * 0.5f
            val centerZ = (minZ + maxZ) * 0.5f

            val spanX = maxX - minX
            val spanY = maxY - minY
            val spanZ = maxZ - minZ
            val boundingRadius = max(spanX, max(spanY, spanZ)) * 0.5f

            camera.setTarget(0f, 0f, 0f)
            val distance = boundingRadius / tan(Math.toRadians((camera.fovDeg * 0.5f).toDouble())).toFloat()
            camera.configure(distance = distance * 1.4f, minDistance = boundingRadius * 0.3f + 1f, maxDistance = boundingRadius * 15f)

            // 체인 색상 결정
            val chainColor = getChainColor(chain)
            val r: Float = ((chainColor shr 16) and 0xFF) / 255f
            val g: Float = ((chainColor shr 8) and 0xFF) / 255f
            val b: Float = (chainColor and 0xFF) / 255f

            // 선 정점 생성
            sortedAtoms.forEach { atom ->
                positionList.add(atom.position.x - centerX)
                positionList.add(atom.position.y - centerY)
                positionList.add(atom.position.z - centerZ)

                colorList.add(r)
                colorList.add(g)
                colorList.add(b)

                totalVertices++
            }
        }

        lineVertexCount = totalVertices

        // 버퍼에 업로드
        val positionBuffer = ByteBuffer.allocateDirect(positionList.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        positionList.forEach { positionBuffer.put(it) }
        positionBuffer.flip()

        val colorBuffer = ByteBuffer.allocateDirect(colorList.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        colorList.forEach { colorBuffer.put(it) }
        colorBuffer.flip()

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, lineVbo)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, positionBuffer.capacity() * 4, positionBuffer, GLES30.GL_STATIC_DRAW)

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, colorVbo)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, colorBuffer.capacity() * 4, colorBuffer, GLES30.GL_STATIC_DRAW)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)

        Log.d(TAG, "Uploaded ${lineVertexCount} vertices for ribbon rendering")
    }

    private fun uploadAllAtoms(atoms: List<Atom>) {
        // 폴백: CA가 없을 경우 모든 원자 사용
        lineVertexCount = atoms.size

        var minX = Float.POSITIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        var minZ = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY
        var maxZ = Float.NEGATIVE_INFINITY

        atoms.forEach { atom ->
            val p = atom.position
            if (p.x < minX) minX = p.x
            if (p.y < minY) minY = p.y
            if (p.z < minZ) minZ = p.z
            if (p.x > maxX) maxX = p.x
            if (p.y > maxY) maxY = p.y
            if (p.z > maxZ) maxZ = p.z
        }

        val centerX = (minX + maxX) * 0.5f
        val centerY = (minY + maxY) * 0.5f
        val centerZ = (minZ + maxZ) * 0.5f

        val spanX = maxX - minX
        val spanY = maxY - minY
        val spanZ = maxZ - minZ
        val boundingRadius = max(spanX, max(spanY, spanZ)) * 0.5f

        camera.setTarget(0f, 0f, 0f)
        val distance = boundingRadius / tan(Math.toRadians((camera.fovDeg * 0.5f).toDouble())).toFloat()
        camera.configure(distance = distance * 1.4f, minDistance = boundingRadius * 0.3f + 1f, maxDistance = boundingRadius * 15f)

        val positionBuffer = ByteBuffer.allocateDirect(lineVertexCount * 3 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        val colorBuffer = ByteBuffer.allocateDirect(lineVertexCount * 3 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()

        atoms.forEach { atom ->
            positionBuffer.put(atom.position.x - centerX)
            positionBuffer.put(atom.position.y - centerY)
            positionBuffer.put(atom.position.z - centerZ)

            val color = ColorMaps.cpk(atom.element)
            val r: Float = ((color shr 16) and 0xFF) / 255f
            val g: Float = ((color shr 8) and 0xFF) / 255f
            val b: Float = (color and 0xFF) / 255f
            colorBuffer.put(r)
            colorBuffer.put(g)
            colorBuffer.put(b)
        }
        positionBuffer.flip()
        colorBuffer.flip()

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, lineVbo)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, positionBuffer.capacity() * 4, positionBuffer, GLES30.GL_STATIC_DRAW)

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, colorVbo)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, colorBuffer.capacity() * 4, colorBuffer, GLES30.GL_STATIC_DRAW)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)

        Log.d(TAG, "Uploaded ${atoms.size} fallback vertices")
    }

    private fun getChainColor(chain: String): Int {
        return when (chain.uppercase()) {
            "A" -> android.graphics.Color.rgb(48, 80, 248) // Blue
            "B" -> android.graphics.Color.rgb(255, 128, 0) // Orange
            "C" -> android.graphics.Color.rgb(0, 200, 100) // Green
            "D" -> android.graphics.Color.rgb(200, 50, 200) // Purple
            "E" -> android.graphics.Color.rgb(255, 100, 150) // Pink
            "F" -> android.graphics.Color.rgb(0, 200, 200) // Teal
            else -> android.graphics.Color.rgb(150, 150, 150) // Gray
        }
    }

    companion object {
        private const val TAG = "SimpleRibbonRenderer"

        private const val VERT_SHADER = """#version 300 es
layout (location = 0) in vec3 aPosition;
layout (location = 1) in vec3 aColor;
uniform mat4 uMvp;
uniform float uLineWidth;
out vec3 vColor;
void main() {
    gl_Position = uMvp * vec4(aPosition, 1.0);
    vColor = aColor;
}"""

        private const val FRAG_SHADER = """#version 300 es
precision mediump float;
in vec3 vColor;
out vec4 fragColor;
void main() {
    fragColor = vec4(vColor, 1.0);
}"""

        private fun createProgram(vertexSrc: String, fragmentSrc: String): Int {
            val vertexShader = compileShader(GLES30.GL_VERTEX_SHADER, vertexSrc)
            val fragmentShader = compileShader(GLES30.GL_FRAGMENT_SHADER, fragmentSrc)
            val program = GLES30.glCreateProgram()
            GLES30.glAttachShader(program, vertexShader)
            GLES30.glAttachShader(program, fragmentShader)
            GLES30.glLinkProgram(program)
            val linkStatus = IntArray(1)
            GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, linkStatus, 0)
            if (linkStatus[0] == 0) {
                val log = GLES30.glGetProgramInfoLog(program)
                GLES30.glDeleteProgram(program)
                throw RuntimeException("Unable to link shader program: $log")
            }
            GLES30.glDeleteShader(vertexShader)
            GLES30.glDeleteShader(fragmentShader)
            return program
        }

        private fun compileShader(type: Int, src: String): Int {
            val shader = GLES30.glCreateShader(type)
            GLES30.glShaderSource(shader, src)
            GLES30.glCompileShader(shader)
            val status = IntArray(1)
            GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, status, 0)
            if (status[0] == 0) {
                val log = GLES30.glGetShaderInfoLog(shader)
                GLES30.glDeleteShader(shader)
                throw RuntimeException("Shader compilation failed: $log")
            }
            return shader
        }
    }
}

