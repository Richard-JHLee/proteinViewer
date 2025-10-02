package com.avas.proteinviewer.rendering.gles

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Log
import com.avas.proteinviewer.domain.model.PDBStructure
import com.avas.proteinviewer.domain.model.Atom
import com.avas.proteinviewer.domain.model.SecondaryStructure
import com.avas.proteinviewer.domain.model.RenderStyle
import com.avas.proteinviewer.domain.model.ColorMode
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.*

/**
 * OpenGL ES 3.0 Ribbon 렌더러 (제대로 된 버전)
 * Catmull-Rom 스플라인 + 튜브 메쉬로 폭이 있는 Ribbon 생성
 */
class ProperRibbonRenderer : GLSurfaceView.Renderer {

    private val camera = ArcballCamera()

    private val projectionMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    private var program = 0
    private var aPositionHandle = 0
    private var aColorHandle = 0
    private var aNormalHandle = 0
    private var uMvpHandle = 0

    private var vertexVbo = 0
    private var colorVbo = 0
    private var normalVbo = 0
    private var indexVbo = 0
    private var vao = 0

    private var indexCount = 0
    private val ribbonRadius = 0.4f
    private val tubeSegments = 8 // 원통의 분할 수

    private var buffersReady = false
    private var pendingStructure: PDBStructure? = null
    private var currentStructure: PDBStructure? = null
    private var currentRenderStyle: RenderStyle = RenderStyle.RIBBON
    private var currentColorMode: ColorMode = ColorMode.CHAIN
    private var currentHighlightedChains: Set<String> = emptySet()

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        Log.d(TAG, "onSurfaceCreated - Proper Ribbon Renderer")
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
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
        aNormalHandle = GLES30.glGetAttribLocation(program, "aNormal")
        uMvpHandle = GLES30.glGetUniformLocation(program, "uMvp")

        val buffers = IntArray(4)
        GLES30.glGenBuffers(4, buffers, 0)
        vertexVbo = buffers[0]
        colorVbo = buffers[1]
        normalVbo = buffers[2]
        indexVbo = buffers[3]

        val vaos = IntArray(1)
        GLES30.glGenVertexArrays(1, vaos, 0)
        vao = vaos[0]

        buffersReady = vertexVbo != 0 && colorVbo != 0 && normalVbo != 0 && indexVbo != 0 && vao != 0
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
        if (indexCount == 0 || program == 0 || !buffersReady) return

        GLES30.glUseProgram(program)
        val viewMatrix = camera.viewMatrix()
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

        GLES30.glBindVertexArray(vao)

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vertexVbo)
        GLES30.glEnableVertexAttribArray(aPositionHandle)
        GLES30.glVertexAttribPointer(aPositionHandle, 3, GLES30.GL_FLOAT, false, 0, 0)

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, colorVbo)
        GLES30.glEnableVertexAttribArray(aColorHandle)
        GLES30.glVertexAttribPointer(aColorHandle, 3, GLES30.GL_FLOAT, false, 0, 0)

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, normalVbo)
        GLES30.glEnableVertexAttribArray(aNormalHandle)
        GLES30.glVertexAttribPointer(aNormalHandle, 3, GLES30.GL_FLOAT, false, 0, 0)

        GLES30.glUniformMatrix4fv(uMvpHandle, 1, false, mvpMatrix, 0)

        // 삼각형 메쉬로 Ribbon 렌더링
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, indexVbo)
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, indexCount, GLES30.GL_UNSIGNED_SHORT, 0)

        GLES30.glDisableVertexAttribArray(aPositionHandle)
        GLES30.glDisableVertexAttribArray(aColorHandle)
        GLES30.glDisableVertexAttribArray(aNormalHandle)
        GLES30.glBindVertexArray(0)
    }

    fun updateStructure(structure: PDBStructure?) {
        currentStructure = structure
        pendingStructure = structure
        if (!buffersReady) {
            indexCount = 0
            return
        }
        uploadStructure(structure)
    }
    
    fun updateRenderStyle(style: RenderStyle) {
        currentRenderStyle = style
        Log.d(TAG, "Render style changed to: $style")
        // 현재는 Ribbon만 지원, 나중에 다른 스타일 구현 예정
        // TODO: Spheres, Sticks, Cartoon, Surface 구현
    }
    
    fun updateColorMode(mode: ColorMode) {
        val oldMode = currentColorMode
        currentColorMode = mode
        Log.d(TAG, "Color mode changed from $oldMode to: $mode")
        // Color mode가 변경되면 버퍼 재생성 필요
        currentStructure?.let {
            uploadStructure(it)
        }
    }
    
    fun updateHighlightedChains(highlightedChains: Set<String>) {
        currentHighlightedChains = highlightedChains
        Log.d(TAG, "Highlighted chains updated: $highlightedChains")
        // Highlight가 변경되면 버퍼 재생성 필요 (색상/투명도 변경)
        currentStructure?.let {
            uploadStructure(it)
        }
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
            indexCount = 0
            return
        }

        // CA (alpha carbon) 원자만 필터링
        val caAtoms = structure.atoms.filter { atom ->
            atom.name.trim().equals("CA", ignoreCase = true)
        }

        if (caAtoms.isEmpty()) {
            Log.w(TAG, "No CA atoms found")
            indexCount = 0
            return
        }

        Log.d(TAG, "Found ${caAtoms.size} CA atoms for ribbon rendering")

        // 바운딩 박스 계산 및 카메라 설정
        setupCamera(caAtoms)

        // 체인별로 그룹화
        val chainGroups = caAtoms.groupBy { it.chain }
        
        val allVertices = mutableListOf<Float>()
        val allColors = mutableListOf<Float>()
        val allNormals = mutableListOf<Float>()
        val allIndices = mutableListOf<Short>()
        
        var vertexOffset = 0

        chainGroups.forEach { (chain, atoms) ->
            val sortedAtoms = atoms.sortedBy { it.residueNumber }
            
            if (sortedAtoms.size < 2) return@forEach
            
            // Catmull-Rom 스플라인으로 부드러운 곡선 생성
            val splinePoints = generateCatmullRomSpline(sortedAtoms, numSegments = 10)
            
            // 튜브 메쉬 생성
            val mesh = createTubeMesh(splinePoints, ribbonRadius, tubeSegments)
            
            // 체인 하이라이트 상태 확인 (iPhone과 동일)
            val chainKey = "chain:$chain"
            val isHighlighted = currentHighlightedChains.contains(chainKey)
            val hasAnyHighlight = currentHighlightedChains.isNotEmpty()
            
            // 체인 색상
            val chainColor = getChainColor(chain)
            
            // 메쉬 데이터 추가
            allVertices.addAll(mesh.vertices)
            allNormals.addAll(mesh.normals)
            
            // 2차 구조에 따라 색상 블렌딩 + Highlight 효과
            val verticesPerSplinePoint = (tubeSegments + 1)
            splinePoints.forEachIndexed { index, splinePoint ->
                val structureColor = getSecondaryStructureColor(splinePoint.secondaryStructure)
                var blendedColor = blendColors(chainColor, structureColor, alpha = 0.6f)
                
                // Highlight 효과 (iPhone과 동일)
                if (hasAnyHighlight) {
                    if (isHighlighted) {
                        // Highlighted: 밝고 선명하게 (saturation x1.4, brightness x1.3)
                        blendedColor = blendedColor.map { (it * 1.4f).coerceAtMost(1.0f) }
                    } else {
                        // Not highlighted: 매우 희미하게 (alpha = 0.15)
                        blendedColor = blendedColor.map { it * 0.15f }
                    }
                }
                
                // 각 스플라인 포인트의 원형 단면 정점들에 색상 적용
                repeat(verticesPerSplinePoint) {
                    allColors.addAll(blendedColor)
                }
            }
            
            // 인덱스 오프셋 적용
            mesh.indices.forEach { index ->
                allIndices.add((index + vertexOffset).toShort())
            }
            
            vertexOffset += mesh.vertices.size / 3
        }

        indexCount = allIndices.size

        // 버퍼에 업로드
        uploadToGPU(allVertices, allColors, allNormals, allIndices)

        Log.d(TAG, "Uploaded ${allVertices.size / 3} vertices, $indexCount indices")
    }

    private fun setupCamera(atoms: List<Atom>) {
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

        camera.setTarget(centerX, centerY, centerZ)
        val distance = boundingRadius / tan(Math.toRadians((camera.fovDeg * 0.5f).toDouble())).toFloat()
        camera.configure(distance = distance * 1.4f, minDistance = boundingRadius * 0.3f + 1f, maxDistance = boundingRadius * 15f)
    }

    // Catmull-Rom 스플라인 곡선 생성
    private fun generateCatmullRomSpline(
        atoms: List<Atom>,
        tension: Float = 0.5f,
        numSegments: Int = 10
    ): List<SplinePoint> {
        val result = mutableListOf<SplinePoint>()

        for (i in 0 until atoms.size - 1) {
            val p0 = if (i > 0) atoms[i - 1].position else atoms[i].position
            val p1 = atoms[i].position
            val p2 = atoms[i + 1].position
            val p3 = if (i < atoms.size - 2) atoms[i + 2].position else atoms[i + 1].position

            for (j in 0..numSegments) {
                val t = j.toFloat() / numSegments
                val point = catmullRom(p0, p1, p2, p3, t, tension)
                result.add(SplinePoint(Vector3(point.x, point.y, point.z), atoms[i].secondaryStructure))
            }
        }

        return result
    }

    private fun catmullRom(
        p0: com.avas.proteinviewer.domain.model.Vector3,
        p1: com.avas.proteinviewer.domain.model.Vector3,
        p2: com.avas.proteinviewer.domain.model.Vector3,
        p3: com.avas.proteinviewer.domain.model.Vector3,
        t: Float,
        tension: Float
    ): com.avas.proteinviewer.domain.model.Vector3 {
        val t2 = t * t
        val t3 = t2 * t

        val v0x = (p2.x - p0.x) * tension
        val v0y = (p2.y - p0.y) * tension
        val v0z = (p2.z - p0.z) * tension

        val v1x = (p3.x - p1.x) * tension
        val v1y = (p3.y - p1.y) * tension
        val v1z = (p3.z - p1.z) * tension

        return com.avas.proteinviewer.domain.model.Vector3(
            x = (2 * p1.x - 2 * p2.x + v0x + v1x) * t3 +
                    (-3 * p1.x + 3 * p2.x - 2 * v0x - v1x) * t2 +
                    v0x * t + p1.x,

            y = (2 * p1.y - 2 * p2.y + v0y + v1y) * t3 +
                    (-3 * p1.y + 3 * p2.y - 2 * v0y - v1y) * t2 +
                    v0y * t + p1.y,

            z = (2 * p1.z - 2 * p2.z + v0z + v1z) * t3 +
                    (-3 * p1.z + 3 * p2.z - 2 * v0z - v1z) * t2 +
                    v0z * t + p1.z
        )
    }

    // 튜브 메쉬 생성 (Ribbon용)
    private fun createTubeMesh(
        curve: List<SplinePoint>,
        radius: Float,
        segments: Int
    ): Mesh {
        val vertices = mutableListOf<Float>()
        val normals = mutableListOf<Float>()
        val indices = mutableListOf<Short>()

        for (i in 0 until curve.size - 1) {
            val p1 = curve[i].position
            val p2 = curve[i + 1].position

            // 튜브 방향 벡터
            val direction = (p2 - p1).normalize()

            // 수직 벡터 계산
            val up = Vector3(0f, 1f, 0f)
            val right = direction.cross(up).normalize()
            val normal = direction.cross(right).normalize()

            // 원형 단면 생성
            for (j in 0..segments) {
                val angle = (j.toFloat() / segments) * 2 * PI.toFloat()
                val cosAngle = cos(angle)
                val sinAngle = sin(angle)

                val offset = (right * cosAngle + normal * sinAngle) * radius
                val vertex = p1 + offset

                vertices.addAll(listOf(vertex.x, vertex.y, vertex.z))

                val n = offset.normalize()
                normals.addAll(listOf(n.x, n.y, n.z))
            }
        }

        // 인덱스 생성
        for (i in 0 until curve.size - 2) {
            for (j in 0 until segments) {
                val i0 = (i * (segments + 1) + j).toShort()
                val i1 = (i0 + 1).toShort()
                val i2 = (i0 + segments + 1).toShort()
                val i3 = (i2 + 1).toShort()

                indices.addAll(listOf(i0, i2, i1, i1, i2, i3))
            }
        }

        return Mesh(vertices, normals, indices)
    }

    private fun uploadToGPU(
        vertices: List<Float>,
        colors: List<Float>,
        normals: List<Float>,
        indices: List<Short>
    ) {
        val vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        vertices.forEach { vertexBuffer.put(it) }
        vertexBuffer.flip()

        val colorBuffer = ByteBuffer.allocateDirect(colors.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        colors.forEach { colorBuffer.put(it) }
        colorBuffer.flip()

        val normalBuffer = ByteBuffer.allocateDirect(normals.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        normals.forEach { normalBuffer.put(it) }
        normalBuffer.flip()

        val indexBuffer = ByteBuffer.allocateDirect(indices.size * 2).order(ByteOrder.nativeOrder()).asShortBuffer()
        indices.forEach { indexBuffer.put(it) }
        indexBuffer.flip()

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vertexVbo)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vertexBuffer.capacity() * 4, vertexBuffer, GLES30.GL_STATIC_DRAW)

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, colorVbo)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, colorBuffer.capacity() * 4, colorBuffer, GLES30.GL_STATIC_DRAW)

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, normalVbo)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, normalBuffer.capacity() * 4, normalBuffer, GLES30.GL_STATIC_DRAW)

        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, indexVbo)
        GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, indexBuffer.capacity() * 2, indexBuffer, GLES30.GL_STATIC_DRAW)

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, 0)
    }

    private fun getChainColor(chain: String): List<Float> {
        return when (currentColorMode) {
            ColorMode.CHAIN -> {
                // Chain별 고유 색상
                when (chain.uppercase()) {
                    "A" -> listOf(0.2f, 0.3f, 1.0f) // Blue
                    "B" -> listOf(1.0f, 0.5f, 0.0f) // Orange
                    "C" -> listOf(0.0f, 0.8f, 0.4f) // Green
                    "D" -> listOf(0.8f, 0.2f, 0.8f) // Purple
                    "E" -> listOf(1.0f, 0.4f, 0.6f) // Pink
                    "F" -> listOf(0.0f, 0.8f, 0.8f) // Teal
                    else -> listOf(0.6f, 0.6f, 0.6f) // Gray
                }
            }
            ColorMode.UNIFORM -> {
                // 단일 색상 (파란색)
                listOf(0.3f, 0.5f, 1.0f)
            }
            ColorMode.ELEMENT -> {
                // Element 색상은 uploadStructure에서 atom별로 처리
                // 여기서는 기본 색상 반환
                listOf(0.6f, 0.6f, 0.6f)
            }
            ColorMode.SECONDARY_STRUCTURE -> {
                // Secondary Structure 색상은 getSecondaryStructureColor에서 처리
                listOf(0.6f, 0.6f, 0.6f)
            }
        }
    }

    private fun getSecondaryStructureColor(structure: SecondaryStructure): List<Float> {
        return when (structure) {
            SecondaryStructure.HELIX -> listOf(1.0f, 0.2f, 0.2f) // Red
            SecondaryStructure.SHEET -> listOf(1.0f, 1.0f, 0.2f) // Yellow
            SecondaryStructure.COIL -> listOf(0.6f, 0.6f, 0.6f) // Gray
            SecondaryStructure.UNKNOWN -> listOf(0.7f, 0.7f, 0.7f) // Light Gray
        }
    }

    private fun blendColors(chainColor: List<Float>, structureColor: List<Float>, alpha: Float = 0.7f): List<Float> {
        return listOf(
            chainColor[0] * alpha + structureColor[0] * (1 - alpha),
            chainColor[1] * alpha + structureColor[1] * (1 - alpha),
            chainColor[2] * alpha + structureColor[2] * (1 - alpha)
        )
    }

    // 데이터 클래스
    data class Vector3(val x: Float, val y: Float, val z: Float) {
        operator fun plus(other: Vector3) = Vector3(x + other.x, y + other.y, z + other.z)
        operator fun minus(other: Vector3) = Vector3(x - other.x, y - other.y, z - other.z)
        operator fun times(scalar: Float) = Vector3(x * scalar, y * scalar, z * scalar)

        fun length() = sqrt(x * x + y * y + z * z)
        fun normalize(): Vector3 {
            val len = length()
            return if (len > 0.0001f) this * (1f / len) else Vector3(0f, 1f, 0f)
        }
        fun cross(other: Vector3) = Vector3(
            y * other.z - z * other.y,
            z * other.x - x * other.z,
            x * other.y - y * other.x
        )
    }

    data class SplinePoint(
        val position: Vector3,
        val secondaryStructure: SecondaryStructure
    )

    data class Mesh(
        val vertices: List<Float>,
        val normals: List<Float>,
        val indices: List<Short>
    )

    companion object {
        private const val TAG = "ProperRibbonRenderer"

        private const val VERT_SHADER = """#version 300 es
layout (location = 0) in vec3 aPosition;
layout (location = 1) in vec3 aColor;
layout (location = 2) in vec3 aNormal;
uniform mat4 uMvp;
out vec3 vColor;
out vec3 vNormal;
void main() {
    gl_Position = uMvp * vec4(aPosition, 1.0);
    vColor = aColor;
    vNormal = aNormal;
}"""

        private const val FRAG_SHADER = """#version 300 es
precision mediump float;
in vec3 vColor;
in vec3 vNormal;
out vec4 fragColor;
void main() {
    // 간단한 조명 계산
    vec3 lightDir = normalize(vec3(1.0, 1.0, 1.0));
    float diffuse = max(dot(normalize(vNormal), lightDir), 0.3);
    fragColor = vec4(vColor * diffuse, 1.0);
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

