package com.avas.proteinviewer.rendering.gles

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Log
import com.avas.proteinviewer.data.model.PDBStructure
import com.avas.proteinviewer.rendering.ColorMaps
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.tan

/**
 * Minimal OpenGL ES 3.0 renderer for the protein viewer. Atoms are rendered as
 * coloured point sprites; the class is intentionally simple so we can evolve it
 * toward full SceneKit parity step by step.
 */
class GLESProteinRenderer : GLSurfaceView.Renderer {

    private val camera = ArcballCamera()

    private val projectionMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    private var program = 0
    private var aPositionHandle = 0
    private var aColorHandle = 0
    private var uMvpHandle = 0
    private var uPointSizeHandle = 0

    private var atomVbo = 0
    private var colorVbo = 0
    private var vao = 0

    private var atomCount = 0
    private var pointSize = 12f

    private var buffersReady = false
    private var pendingStructure: PDBStructure? = null

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        Log.d(TAG, "onSurfaceCreated")
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glEnable(0x8642)  // GL_PROGRAM_POINT_SIZE constant
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)
        GLES30.glClearColor(1f, 1f, 1f, 1f)

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
        uPointSizeHandle = GLES30.glGetUniformLocation(program, "uPointSize")

        val buffers = IntArray(2)
        GLES30.glGenBuffers(2, buffers, 0)
        atomVbo = buffers[0]
        colorVbo = buffers[1]

        val vaos = IntArray(1)
        GLES30.glGenVertexArrays(1, vaos, 0)
        vao = vaos[0]

        buffersReady = atomVbo != 0 && colorVbo != 0 && vao != 0
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
        if (atomCount == 0 || program == 0 || !buffersReady) return

        GLES30.glUseProgram(program)
        val viewMatrix = camera.viewMatrix()
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

        GLES30.glBindVertexArray(vao)

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, atomVbo)
        GLES30.glEnableVertexAttribArray(aPositionHandle)
        GLES30.glVertexAttribPointer(aPositionHandle, 3, GLES30.GL_FLOAT, false, 0, 0)

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, colorVbo)
        GLES30.glEnableVertexAttribArray(aColorHandle)
        GLES30.glVertexAttribPointer(aColorHandle, 3, GLES30.GL_FLOAT, false, 0, 0)

        GLES30.glUniformMatrix4fv(uMvpHandle, 1, false, mvpMatrix, 0)
        GLES30.glUniform1f(uPointSizeHandle, pointSize)

        GLES30.glDrawArrays(GLES30.GL_POINTS, 0, atomCount)

        GLES30.glDisableVertexAttribArray(aPositionHandle)
        GLES30.glDisableVertexAttribArray(aColorHandle)
        GLES30.glBindVertexArray(0)
    }

    fun updateStructure(structure: PDBStructure?) {
        pendingStructure = structure
        if (!buffersReady) {
            atomCount = 0
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
            atomCount = 0
            return
        }

        val atoms = structure.atoms
        atomCount = atoms.size

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

        val positionBuffer = ByteBuffer.allocateDirect(atomCount * 3 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        val colorBuffer = ByteBuffer.allocateDirect(atomCount * 3 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()

        atoms.forEach { atom ->
            positionBuffer.put(atom.position.x - centerX)
            positionBuffer.put(atom.position.y - centerY)
            positionBuffer.put(atom.position.z - centerZ)

            val color = ColorMaps.cpk(atom.element)
            val r = ((color shr 16) and 0xFF) / 255f
            val g = ((color shr 8) and 0xFF) / 255f
            val b = (color and 0xFF) / 255f
            colorBuffer.put(r)
            colorBuffer.put(g)
            colorBuffer.put(b)
        }
        positionBuffer.flip()
        colorBuffer.flip()

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, atomVbo)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, positionBuffer.capacity() * 4, positionBuffer, GLES30.GL_STATIC_DRAW)

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, colorVbo)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, colorBuffer.capacity() * 4, colorBuffer, GLES30.GL_STATIC_DRAW)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)

        pointSize = clamp(6f, 36f, 500f / max(1f, boundingRadius))
        Log.d(TAG, "Uploaded ${atoms.size} atoms, pointSize=$pointSize")
    }

    companion object {
        private const val TAG = "GLESProteinRenderer"

        private const val VERT_SHADER = """#version 300 es
layout (location = 0) in vec3 aPosition;
layout (location = 1) in vec3 aColor;
uniform mat4 uMvp;
uniform float uPointSize;
out vec3 vColor;
void main() {
    gl_Position = uMvp * vec4(aPosition, 1.0);
    gl_PointSize = uPointSize;
    vColor = aColor;
}"""

        private const val FRAG_SHADER = """#version 300 es
precision mediump float;
in vec3 vColor;
out vec4 fragColor;
void main() {
    vec2 coord = gl_PointCoord * 2.0 - 1.0;
    float r = dot(coord, coord);
    if (r > 1.0) {
        discard;
    }
    float alpha = smoothstep(1.0, 0.8, r);
    fragColor = vec4(vColor, alpha);
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

        private fun clamp(minValue: Float, maxValue: Float, value: Float): Float {
            return max(minValue, min(maxValue, value))
        }
    }
}
