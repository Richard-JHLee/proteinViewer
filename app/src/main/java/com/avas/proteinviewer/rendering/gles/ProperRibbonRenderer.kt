package com.avas.proteinviewer.rendering.gles

import android.graphics.Color
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Log
import com.avas.proteinviewer.domain.model.PDBStructure
import com.avas.proteinviewer.domain.model.Atom
import com.avas.proteinviewer.domain.model.SecondaryStructure
import com.avas.proteinviewer.domain.model.RenderStyle
import com.avas.proteinviewer.domain.model.ColorMode
import com.avas.proteinviewer.rendering.ColorMaps
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
    private val ribbonRadius = 0.2f  // 얇은 리본을 위해 더 작게
    private val tubeSegments = 8 // 원통의 분할 수

    private var buffersReady = false
    private var pendingStructure: PDBStructure? = null
    private var currentStructure: PDBStructure? = null
    
    // 구체 메쉬 캐시 (메모리 효율성)
    private val sphereMeshCache = mutableMapOf<Int, MeshData>()
    
    // 구체 메쉬를 캐시에서 가져오거나 생성 (매우 매끄러운 구체)
    private fun getCachedSphereMesh(radius: Float, segments: Int): MeshData {
        val key = (radius * 1000).toInt() * 1000 + segments // radius와 segments를 키로 사용
        
        return sphereMeshCache.getOrPut(key) {
            createSmoothSphereMesh(Vector3(0f, 0f, 0f), radius, segments)
        }
    }
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
        // 구조를 다시 업로드하여 렌더링 스타일 변경 적용
        uploadStructure(currentStructure)
    }
    
    fun updateColorMode(mode: ColorMode) {
        val oldMode = currentColorMode
        currentColorMode = mode
        Log.d(TAG, "Color mode changed from $oldMode to: $mode")
        // 구조를 다시 업로드하여 색상 변경 적용
        if (currentStructure != null) {
            uploadStructure(currentStructure)
            Log.d(TAG, "Structure re-uploaded for color mode change")
        } else {
            Log.w(TAG, "Cannot update color mode: currentStructure is null")
        }
    }
    
    fun updateHighlightedChains(highlightedChains: Set<String>) {
        currentHighlightedChains = highlightedChains
        Log.d(TAG, "Highlighted chains updated: $highlightedChains")
        // 구조를 다시 업로드하여 하이라이트 효과 적용
        uploadStructure(currentStructure)
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

        Log.d(TAG, "uploadStructure: currentRenderStyle=$currentRenderStyle")

        // 초기 구조 로드 시에만 카메라 설정
        if (currentStructure == null) {
            setupCamera(structure.atoms)
        }

        when (currentRenderStyle) {
            RenderStyle.RIBBON -> uploadRibbonStructure(structure)
            RenderStyle.SPHERES -> uploadSpheresStructure(structure)
            RenderStyle.STICKS -> uploadSticksStructure(structure)
            RenderStyle.CARTOON -> uploadCartoonStructure(structure)
            RenderStyle.SURFACE -> uploadSurfaceStructure(structure)
        }
    }
    
    private fun uploadSpheresStructure(structure: PDBStructure) {
        // 모든 원자 렌더링 (CA만이 아닌)
        val atoms = structure.atoms
        
        if (atoms.isEmpty()) {
            Log.w(TAG, "No atoms found for spheres rendering")
            indexCount = 0
            return
        }

        Log.d(TAG, "Found ${atoms.size} atoms for spheres rendering")
        Log.d(TAG, "Spheres rendering: ${atoms.size} spheres will be created")

        // 바운딩 박스 계산 (카메라 설정은 초기 로드 시에만)
        // setupCamera(atoms) // 버튼 선택 시 자동 확대 방지

        val allVertices = mutableListOf<Float>()
        val allColors = mutableListOf<Float>()
        val allNormals = mutableListOf<Float>()
        val allIndices = mutableListOf<Short>()
        
        var vertexOffset = 0

        atoms.forEach { atom ->
            // 원자 색상
            val atomColor = getAtomColor(atom)
            
            // 체인 하이라이트 상태 확인
            val chainKey = "chain:${atom.chain}"
            val isHighlighted = currentHighlightedChains.contains(chainKey)
            val hasAnyHighlight = currentHighlightedChains.isNotEmpty()
            
            // Highlight 효과 적용
            var finalColor = atomColor
            if (hasAnyHighlight) {
                if (isHighlighted) {
                    // Highlighted: 밝고 선명하게
                    finalColor = finalColor.map { (it * 1.4f).coerceAtMost(1.0f) }
                } else {
                    // Not highlighted: 매우 희미하게
                    finalColor = finalColor.map { it * 0.15f }
                }
            }
            
            // 구체 메쉬 생성 (캐시된 메쉬 사용)
            val radius = 0.8f // 이미지처럼 큰 구체로 조밀한 구조 형성
            val center = Vector3(atom.position.x, atom.position.y, atom.position.z)
            val cachedMesh = getCachedSphereMesh(radius, 16) // 간단한 구체 (색으로 칠하기)
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
            
            // 모든 정점에 색상 적용
            repeat(sphereMesh.vertices.size / 3) {
                allColors.addAll(finalColor)
            }
            
            // 인덱스 오프셋 적용
            sphereMesh.indices.forEach { index ->
                allIndices.add((index + vertexOffset).toShort())
            }
            
            vertexOffset += sphereMesh.vertices.size / 3
        }

        indexCount = allIndices.size

        // 버퍼에 업로드
        uploadToGPU(allVertices, allColors, allNormals, allIndices)

        Log.d(TAG, "Uploaded ${allVertices.size / 3} vertices, $indexCount indices for spheres")
    }

    private fun uploadSticksStructure(structure: PDBStructure) {
        // 모든 원자 렌더링 + 연결선
        val atoms = structure.atoms
        
        if (atoms.isEmpty()) {
            Log.w(TAG, "No atoms found for sticks rendering")
            indexCount = 0
            return
        }

        Log.d(TAG, "Found ${atoms.size} atoms for sticks rendering")

        // 바운딩 박스 계산 (카메라 설정은 초기 로드 시에만)
        // setupCamera(atoms) // 버튼 선택 시 자동 확대 방지

        val allVertices = mutableListOf<Float>()
        val allColors = mutableListOf<Float>()
        val allNormals = mutableListOf<Float>()
        val allIndices = mutableListOf<Short>()
        
        var vertexOffset = 0

        // 1. 원자들을 작은 구체로 렌더링
        atoms.forEach { atom ->
            // 원자 색상
            val atomColor = getAtomColor(atom)
            
            // 체인 하이라이트 상태 확인
            val chainKey = "chain:${atom.chain}"
            val isHighlighted = currentHighlightedChains.contains(chainKey)
            val hasAnyHighlight = currentHighlightedChains.isNotEmpty()
            
            // Highlight 효과 적용
            var finalColor = atomColor
            if (hasAnyHighlight) {
                if (isHighlighted) {
                    // Highlighted: 밝고 선명하게
                    finalColor = finalColor.map { (it * 1.4f).coerceAtMost(1.0f) }
                } else {
                    // Not highlighted: 매우 희미하게
                    finalColor = finalColor.map { it * 0.15f }
                }
            }
            
            // 구체 메쉬 생성 (캐시된 메쉬 사용)
            val radius = 0.5f // Sticks용 구체 크기 (Spheres보다 작게)
            val center = Vector3(atom.position.x, atom.position.y, atom.position.z)
            val cachedMesh = getCachedSphereMesh(radius, 16) // 간단한 구체 (색으로 칠하기)
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
            
            // 모든 정점에 색상 적용
            repeat(sphereMesh.vertices.size / 3) {
                allColors.addAll(finalColor)
            }
            
            // 인덱스 오프셋 적용
            sphereMesh.indices.forEach { index ->
                allIndices.add((index + vertexOffset).toShort())
            }
            
            vertexOffset += sphereMesh.vertices.size / 3
        }

        // 2. 연결선 렌더링 (간단한 거리 기반)
        val bondRadius = 0.05f
        val bondColor = listOf(0.5f, 0.5f, 0.5f, 1.0f) // 회색 연결선
        
        atoms.forEachIndexed { i, atom1 ->
            val pos1 = Vector3(atom1.position.x, atom1.position.y, atom1.position.z)
            
            // 근처 원자들과 연결선 생성
            for (j in i + 1 until atoms.size) {
                val atom2 = atoms[j]
                val pos2 = Vector3(atom2.position.x, atom2.position.y, atom2.position.z)
                
                // 거리 계산
                val distance = Math.sqrt(
                    ((pos1.x - pos2.x) * (pos1.x - pos2.x) + 
                     (pos1.y - pos2.y) * (pos1.y - pos2.y) + 
                     (pos1.z - pos2.z) * (pos1.z - pos2.z)).toDouble()
                ).toFloat()
                
                // 2.0Å 이내면 연결선 생성
                if (distance < 2.0f) {
                    val cylinderMesh = createCylinderMesh(pos1, pos2, bondRadius, 6)
                    
                    // 메쉬 데이터 추가
                    allVertices.addAll(cylinderMesh.vertices)
                    allNormals.addAll(cylinderMesh.normals)
                    
                    // 모든 정점에 회색 색상 적용
                    repeat(cylinderMesh.vertices.size / 3) {
                        allColors.addAll(bondColor)
                    }
                    
                    // 인덱스 오프셋 적용
                    cylinderMesh.indices.forEach { index ->
                        allIndices.add((index + vertexOffset).toShort())
                    }
                    
                    vertexOffset += cylinderMesh.vertices.size / 3
                }
            }
        }

        indexCount = allIndices.size

        // 버퍼에 업로드
        uploadToGPU(allVertices, allColors, allNormals, allIndices)

        Log.d(TAG, "Uploaded ${allVertices.size / 3} vertices, $indexCount indices for sticks")
    }

    private fun uploadCartoonStructure(structure: PDBStructure) {
        // CA 원자만 사용하되 Ribbon과 다른 스타일로 렌더링
        val caAtoms = structure.atoms.filter { atom ->
            atom.name.trim().equals("CA", ignoreCase = true)
        }
        
        if (caAtoms.isEmpty()) {
            Log.w(TAG, "No CA atoms found for cartoon rendering")
            indexCount = 0
            return
        }

        Log.d(TAG, "Found ${caAtoms.size} CA atoms for cartoon rendering")

        // 바운딩 박스 계산 및 카메라 설정
        setupCamera(caAtoms)

        val allVertices = mutableListOf<Float>()
        val allColors = mutableListOf<Float>()
        val allNormals = mutableListOf<Float>()
        val allIndices = mutableListOf<Short>()
        
        var vertexOffset = 0

        // 체인별로 그룹화
        val chainGroups = caAtoms.groupBy { it.chain }
        
        chainGroups.forEach { (chain, atoms) ->
            val sortedAtoms = atoms.sortedBy { it.residueNumber }
            
            if (sortedAtoms.size < 2) return@forEach
            
            // 체인 하이라이트 상태 확인
            val chainKey = "chain:$chain"
            val isHighlighted = currentHighlightedChains.contains(chainKey)
            val hasAnyHighlight = currentHighlightedChains.isNotEmpty()
            
            // 색상 모드에 따른 색상 결정 (첫 번째 원자 기준)
            val firstAtom = sortedAtoms.first()
            val atomColor = getAtomColor(firstAtom)
            val chainColor = listOf(atomColor[0], atomColor[1], atomColor[2])
            
            // Cartoon: 2차 구조에 따라 다른 굵기와 모양
            val splinePoints = generateCatmullRomSpline(sortedAtoms, numSegments = 8)
            
            // 2차 구조별로 다른 굵기 적용
            val cartoonMesh = createCartoonTubeMesh(splinePoints, tubeSegments)
            
            // 메쉬 데이터 추가
            allVertices.addAll(cartoonMesh.vertices)
            allNormals.addAll(cartoonMesh.normals)
            
            // 색상 모드에 따른 색상 적용 + Highlight 효과
            val verticesPerSplinePoint = (tubeSegments + 1)
            splinePoints.forEachIndexed { index, splinePoint ->
                var finalColor = chainColor
                
                // Uniform 모드가 아닐 때만 2차 구조 색상 블렌딩
                if (currentColorMode != ColorMode.UNIFORM) {
                    val structureColor = getSecondaryStructureColor(splinePoint.secondaryStructure)
                    finalColor = blendColors(chainColor, structureColor, alpha = 0.8f)
                }
                
                // Highlight 효과
                if (hasAnyHighlight) {
                    if (isHighlighted) {
                        finalColor = finalColor.map { (it * 1.4f).coerceAtMost(1.0f) }
                    } else {
                        finalColor = finalColor.map { it * 0.15f }
                    }
                }
                
                // 각 스플라인 포인트의 원형 단면 정점들에 색상 적용
                repeat(verticesPerSplinePoint) {
                    allColors.addAll(finalColor)
                }
            }
            
            // 인덱스 오프셋 적용
            cartoonMesh.indices.forEach { index ->
                allIndices.add((index + vertexOffset).toShort())
            }
            
            vertexOffset += cartoonMesh.vertices.size / 3
        }

        indexCount = allIndices.size

        // 버퍼에 업로드
        uploadToGPU(allVertices, allColors, allNormals, allIndices)

        Log.d(TAG, "Uploaded ${allVertices.size / 3} vertices, $indexCount indices for cartoon")
    }

    // 헬퍼 함수들
    private fun getAtomColor(atom: Atom): List<Float> {
        return when (currentColorMode) {
            ColorMode.ELEMENT -> {
                // Element 컬러링: CPK 색상 체계 적용 (아이폰과 동일)
                val elementColor = ColorMaps.cpk(atom.element)
                listOf(
                    Color.red(elementColor) / 255f,
                    Color.green(elementColor) / 255f,
                    Color.blue(elementColor) / 255f,
                    1.0f
                )
            }
            ColorMode.CHAIN -> {
                // Chain 컬러링: 체인별 고유 색상 적용 (아이폰과 동일)
                val chainColor = ColorMaps.chainColor(atom.chain)
                listOf(
                    Color.red(chainColor) / 255f,
                    Color.green(chainColor) / 255f,
                    Color.blue(chainColor) / 255f,
                    1.0f
                )
            }
            ColorMode.UNIFORM -> {
                // Uniform 컬러링: 단일 색상 (회색) - 프레젠테이션용
                val uniformColor = listOf(0.5f, 0.5f, 0.5f, 1.0f)
                // 디버깅용 로그 (처음 5개 원자만)
                if (atom.residueNumber <= 5) {
                    Log.d(TAG, "Uniform color applied: $uniformColor")
                }
                uniformColor
            }
            ColorMode.SECONDARY_STRUCTURE -> {
                // Secondary Structure 컬러링: 2차 구조별 색상 적용 (아이폰과 동일)
                val structureColor = ColorMaps.secondaryStructureColor(atom.secondaryStructure.name)
                val color = listOf(
                    Color.red(structureColor) / 255f,
                    Color.green(structureColor) / 255f,
                    Color.blue(structureColor) / 255f,
                    1.0f
                )
                // 디버깅용 로그 (처음 5개 원자만)
                if (atom.residueNumber <= 5) {
                    Log.d(TAG, "Secondary Structure: ${atom.secondaryStructure.name} -> Color: $color")
                }
                color
            }
        }
    }

    private fun getVanDerWaalsRadius(element: String): Float {
        return when (element.uppercase()) {
            "H" -> 1.20f
            "C" -> 1.70f
            "N" -> 1.55f
            "O" -> 1.52f
            "S" -> 1.80f
            "P" -> 1.80f
            "F" -> 1.47f
            "CL" -> 1.75f
            "BR" -> 1.85f
            "I" -> 1.98f
            else -> 1.50f // 기본값
        }
    }

    private fun createSphereMesh(center: Vector3, radius: Float, segments: Int): MeshData {
        val vertices = mutableListOf<Float>()
        val normals = mutableListOf<Float>()
        val indices = mutableListOf<Short>()
        
        val rings = segments / 2
        val ringVertices = segments + 1
        
        // 정점 생성 (아이폰과 동일한 방식)
        for (i in 0..rings) {
            val phi = Math.PI * i / rings
            val y = (Math.cos(phi) * radius).toFloat()
            val sinPhi = Math.sin(phi)
            
            for (j in 0..segments) {
                val theta = 2.0 * Math.PI * j / segments
                val x = (Math.cos(theta) * sinPhi * radius).toFloat()
                val z = (Math.sin(theta) * sinPhi * radius).toFloat()
                
                vertices.addAll(listOf(
                    center.x + x,
                    center.y + y,
                    center.z + z
                ))
                
                // 법선 벡터 (중심에서 정점으로의 단위 벡터)
                val normalLength = Math.sqrt((x*x + y*y + z*z).toDouble()).toFloat()
                normals.addAll(listOf(
                    x / normalLength,
                    y / normalLength,
                    z / normalLength
                ))
            }
        }
        
        // 인덱스 생성 (아이폰과 동일한 삼각형 패턴)
        for (i in 0 until rings) {
            for (j in 0 until segments) {
                val current = i * ringVertices + j
                val next = current + ringVertices
                val currentNext = if (j < segments) current + 1 else current - segments
                val nextNext = if (j < segments) next + 1 else next - segments
                
                // 첫 번째 삼각형
                indices.addAll(listOf(
                    current.toShort(),
                    currentNext.toShort(),
                    next.toShort()
                ))
                
                // 두 번째 삼각형
                indices.addAll(listOf(
                    currentNext.toShort(),
                    nextNext.toShort(),
                    next.toShort()
                ))
            }
        }
        
        return MeshData(vertices, normals, indices)
    }

    private fun createSmoothSphereMesh(center: Vector3, radius: Float, segments: Int): MeshData {
        val vertices = mutableListOf<Float>()
        val normals = mutableListOf<Float>()
        val indices = mutableListOf<Short>()
        
        // 극도로 간단한 팔면체 구체 (6개 정점, 8개 삼각형)
        val octahedronVertices = listOf(
            // 팔면체의 6개 정점
            Vector3(0f, radius, 0f),      // 위쪽 정점
            Vector3(radius, 0f, 0f),      // 오른쪽 정점
            Vector3(0f, 0f, radius),      // 앞쪽 정점
            Vector3(-radius, 0f, 0f),     // 왼쪽 정점
            Vector3(0f, 0f, -radius),     // 뒤쪽 정점
            Vector3(0f, -radius, 0f)      // 아래쪽 정점
        )
        
        // 정점 추가
        octahedronVertices.forEach { vertex ->
            vertices.addAll(listOf(
                center.x + vertex.x,
                center.y + vertex.y,
                center.z + vertex.z
            ))
            // 모든 법선을 동일하게 (무늬 제거)
            normals.addAll(listOf(0f, 0f, 1f))
        }
        
        // 팔면체 인덱스 (8개 삼각형)
        val octahedronIndices = listOf(
            // 위쪽 4개 삼각형
            0, 1, 2,  // 위-오른쪽-앞
            0, 2, 3,  // 위-앞-왼쪽
            0, 3, 4,  // 위-왼쪽-뒤
            0, 4, 1,  // 위-뒤-오른쪽
            // 아래쪽 4개 삼각형
            5, 2, 1,  // 아래-앞-오른쪽
            5, 3, 2,  // 아래-왼쪽-앞
            5, 4, 3,  // 아래-뒤-왼쪽
            5, 1, 4   // 아래-오른쪽-뒤
        )
        
        indices.addAll(octahedronIndices.map { it.toShort() })
        
        return MeshData(vertices, normals, indices)
    }

    private fun createCylinderMesh(start: Vector3, end: Vector3, radius: Float, segments: Int): MeshData {
        val vertices = mutableListOf<Float>()
        val normals = mutableListOf<Float>()
        val indices = mutableListOf<Short>()
        
        val direction = Vector3(
            end.x - start.x,
            end.y - start.y,
            end.z - start.z
        )
        val length = Math.sqrt((direction.x*direction.x + direction.y*direction.y + direction.z*direction.z).toDouble()).toFloat()
        
        // 정규화된 방향 벡터
        val normalizedDirection = Vector3(
            direction.x / length,
            direction.y / length,
            direction.z / length
        )
        
        // 원통의 중심축에 수직인 두 개의 벡터 찾기
        val up = Vector3(0f, 1f, 0f)
        val rightX = normalizedDirection.y * up.z - normalizedDirection.z * up.y
        val rightY = normalizedDirection.z * up.x - normalizedDirection.x * up.z
        val rightZ = normalizedDirection.x * up.y - normalizedDirection.y * up.x
        val rightLength = Math.sqrt((rightX*rightX + rightY*rightY + rightZ*rightZ).toDouble()).toFloat()
        val right = Vector3(rightX / rightLength, rightY / rightLength, rightZ / rightLength)
        
        val forward = Vector3(
            normalizedDirection.y * right.z - normalizedDirection.z * right.y,
            normalizedDirection.z * right.x - normalizedDirection.x * right.z,
            normalizedDirection.x * right.y - normalizedDirection.y * right.x
        )
        
        // 원통의 원형 단면 정점들 생성
        for (i in 0..segments) {
            val angle = 2.0 * Math.PI * i / segments
            val cos = Math.cos(angle).toFloat()
            val sin = Math.sin(angle).toFloat()
            
            val offset = Vector3(
                right.x * cos + forward.x * sin,
                right.y * cos + forward.y * sin,
                right.z * cos + forward.z * sin
            )
            
            // 시작점과 끝점
            vertices.addAll(listOf(
                start.x + offset.x * radius,
                start.y + offset.y * radius,
                start.z + offset.z * radius
            ))
            vertices.addAll(listOf(
                end.x + offset.x * radius,
                end.y + offset.y * radius,
                end.z + offset.z * radius
            ))
            
            // 법선 벡터
            normals.addAll(listOf(offset.x, offset.y, offset.z))
            normals.addAll(listOf(offset.x, offset.y, offset.z))
        }
        
        // 인덱스 생성
        for (i in 0 until segments) {
            val current = i * 2
            val next = ((i + 1) % (segments + 1)) * 2
            
            // 첫 번째 삼각형
            indices.addAll(listOf(
                current.toShort(),
                (current + 1).toShort(),
                next.toShort()
            ))
            
            // 두 번째 삼각형
            indices.addAll(listOf(
                (current + 1).toShort(),
                (next + 1).toShort(),
                next.toShort()
            ))
        }
        
        return MeshData(vertices, normals, indices)
    }

    private fun createCartoonTubeMesh(splinePoints: List<SplinePoint>, segments: Int): MeshData {
        val vertices = mutableListOf<Float>()
        val normals = mutableListOf<Float>()
        val indices = mutableListOf<Short>()
        
        var vertexOffset = 0
        
        for (i in 0 until splinePoints.size - 1) {
            val current = splinePoints[i]
            val next = splinePoints[i + 1]
            
            // 2차 구조에 따라 다른 굵기 적용
            val radius = when (current.secondaryStructure) {
                SecondaryStructure.HELIX -> ribbonRadius * 2.0f  // α-helix: 두꺼운 원통
                SecondaryStructure.SHEET -> ribbonRadius * 2.5f  // β-sheet: 넓은 화살표
                SecondaryStructure.COIL -> ribbonRadius * 1.8f    // coil: 굵은 튜브
                else -> ribbonRadius * 1.5f                     // 기본: 중간 굵기
            }
            
            // 원통 메쉬 생성
            val cylinderMesh = createCylinderMesh(current.position, next.position, radius, segments)
            
            // 메쉬 데이터 추가
            vertices.addAll(cylinderMesh.vertices)
            normals.addAll(cylinderMesh.normals)
            
            // 인덱스 오프셋 적용
            cylinderMesh.indices.forEach { index ->
                indices.add((index + vertexOffset).toShort())
            }
            
            vertexOffset += cylinderMesh.vertices.size / 3
        }
        
        return MeshData(vertices, normals, indices)
    }

    data class MeshData(
        val vertices: List<Float>,
        val normals: List<Float>,
        val indices: List<Short>
    )

    private fun uploadSurfaceStructure(structure: PDBStructure) {
        // Surface: 단일 매끄러운 표면 (개별 구체가 아닌 연결된 표면)
        val atoms = structure.atoms
        
        if (atoms.isEmpty()) {
            Log.w(TAG, "No atoms found for surface rendering")
            indexCount = 0
            return
        }

        Log.d(TAG, "Found ${atoms.size} atoms for surface rendering")

        // 바운딩 박스 계산 (카메라 설정은 초기 로드 시에만)
        // setupCamera(atoms) // 버튼 선택 시 자동 확대 방지

        val allVertices = mutableListOf<Float>()
        val allColors = mutableListOf<Float>()
        val allNormals = mutableListOf<Float>()
        val allIndices = mutableListOf<Short>()
        
        var vertexOffset = 0

        // Surface: 큰 구체들로 매끄러운 표면 효과 (개별 구체가 아닌 연결된 표면)
        atoms.forEach { atom ->
            // Surface는 단일 회색 색상 사용
            val surfaceColor = listOf(0.7f, 0.7f, 0.7f, 1.0f) // 회색
            
            // 체인 하이라이트 상태 확인
            val chainKey = "chain:${atom.chain}"
            val isHighlighted = currentHighlightedChains.contains(chainKey)
            val hasAnyHighlight = currentHighlightedChains.isNotEmpty()
            
            // Highlight 효과 적용
            var finalColor = surfaceColor
            if (hasAnyHighlight) {
                if (isHighlighted) {
                    // Highlighted: 밝고 선명하게
                    finalColor = finalColor.map { (it * 1.4f).coerceAtMost(1.0f) }
                } else {
                    // Not highlighted: 매우 희미하게
                    finalColor = finalColor.map { it * 0.15f }
                }
            }
            
            // 큰 구체로 매끄러운 표면 효과 (캐시된 메쉬 사용)
            val radius = getVanDerWaalsRadius(atom.element) * 1.2f // 약간 더 크게
            val center = Vector3(atom.position.x, atom.position.y, atom.position.z)
            val cachedMesh = getCachedSphereMesh(radius, 20) // 간단한 표면 (색으로 칠하기)
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
            
            // 모든 정점에 색상 적용
            repeat(sphereMesh.vertices.size / 3) {
                allColors.addAll(finalColor)
            }
            
            // 인덱스 오프셋 적용
            sphereMesh.indices.forEach { index ->
                allIndices.add((index + vertexOffset).toShort())
            }
            
            vertexOffset += sphereMesh.vertices.size / 3
        }

        indexCount = allIndices.size

        // 버퍼에 업로드
        uploadToGPU(allVertices, allColors, allNormals, allIndices)

        Log.d(TAG, "Uploaded ${allVertices.size / 3} vertices, $indexCount indices for surface")
    }

    private fun uploadRibbonStructure(structure: PDBStructure) {
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
            
            // 색상 모드에 따른 색상 결정 (첫 번째 원자 기준)
            val firstAtom = sortedAtoms.first()
            Log.d(TAG, "Ribbon: Getting color for chain $chain, atom ${firstAtom.residueNumber}, colorMode: $currentColorMode")
            val atomColor = getAtomColor(firstAtom)
            val chainColor = listOf(atomColor[0], atomColor[1], atomColor[2])
            Log.d(TAG, "Ribbon: Chain $chain color: $chainColor")
            
            // 메쉬 데이터 추가
            allVertices.addAll(mesh.vertices)
            allNormals.addAll(mesh.normals)
            
            // 색상 모드에 따른 색상 적용 + Highlight 효과
            val verticesPerSplinePoint = (tubeSegments + 1)
            splinePoints.forEachIndexed { index, splinePoint ->
                var finalColor = chainColor
                
                // Uniform 모드가 아닐 때만 2차 구조 색상 블렌딩
                if (currentColorMode != ColorMode.UNIFORM) {
                    val structureColor = getSecondaryStructureColor(splinePoint.secondaryStructure)
                    finalColor = blendColors(chainColor, structureColor, alpha = 0.6f)
                }
                
                // Highlight 효과 (iPhone과 동일)
                if (hasAnyHighlight) {
                    if (isHighlighted) {
                        // Highlighted: 밝고 선명하게 (saturation x1.4, brightness x1.3)
                        finalColor = finalColor.map { (it * 1.4f).coerceAtMost(1.0f) }
                    } else {
                        // Not highlighted: 매우 희미하게 (alpha = 0.15)
                        finalColor = finalColor.map { it * 0.15f }
                    }
                }
                
                // 각 스플라인 포인트의 원형 단면 정점들에 색상 적용
                repeat(verticesPerSplinePoint) {
                    allColors.addAll(finalColor)
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
        camera.configure(distance = distance * 2.5f, minDistance = boundingRadius * 0.5f + 1f, maxDistance = boundingRadius * 20f)
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
    // Material 효과 제거 - 단순한 색상 렌더링
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

