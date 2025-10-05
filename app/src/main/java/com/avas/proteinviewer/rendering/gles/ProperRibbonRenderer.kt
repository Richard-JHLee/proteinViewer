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
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)
    
    private var onRenderingCompleteCallback: (() -> Unit)? = null
    private var pendingRenderingComplete = false // 렌더링 완료 콜백 대기 상태
    private var hasLoggedFirstFrame = false // 첫 번째 프레임 로그 출력 여부
    private var hasLoggedLigandsPockets = false // Ligands/Pockets 로그 출력 여부

    private var program = 0
    private var aPositionHandle = 0
    private var aColorHandle = 0
    private var aNormalHandle = 0
    private var uMvpHandle = 0
    private var uModelHandle = 0
    private var uTransparencyHandle = 0
    private var uLightPosHandle = 0
    private var uViewPosHandle = 0

    private var vertexVbo = 0
    private var colorVbo = 0
    private var normalVbo = 0
    private var indexVbo = 0
    private var vao = 0

    private var indexCount = 0
    private val ribbonRadius = 0.2f  // 얇은 리본을 위해 더 작게
    private var tubeSegments = 8 // 원통의 분할 수 (LOD에 따라 조정)
    

    private var buffersReady = false
    private var pendingStructure: PDBStructure? = null
    private var currentStructure: PDBStructure? = null
    
    // 구체 렌더러 인스턴스
    private val sphereRenderer = SphereRenderer()
    
    // 성능 최적화 컴포넌트 (선택적 사용)
    private val instancedRenderer = InstancedRenderer()
    private val vboCache = VBOCache()
    private val lodManager = LODManager()
    
    private var currentRenderStyle: RenderStyle = RenderStyle.RIBBON
    private var currentColorMode: ColorMode = ColorMode.ELEMENT
    private var currentHighlightedChains: Set<String> = emptySet()
    private var currentFocusedElement: String? = null
    private var isInfoMode: Boolean = false
    
    // Options values
    private var rotationEnabled: Boolean = false
    private var zoomLevel: Float = 1.0f
    private var transparency: Float = 1.0f
    private var ligandTransparency: Float = 0.3f // Ligands 색상 강도 (30% 강도로 진하게)
    private var pocketTransparency: Float = 0.3f // Pockets 색상 강도 (30% 강도로 진하게)
    private var atomSize: Float = 1.0f
    private var ribbonWidth: Float = 1.2f
    private var ribbonFlatness: Float = 0.5f
    
    // 복잡한 단백질을 위한 LOD (Level of Detail) 최적화
    private var lodLevel: Int = 1 // 1=고품질, 2=중품질, 3=저품질
    private var isComplexProtein: Boolean = false

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        Log.d(TAG, "onSurfaceCreated - Proper Ribbon Renderer")
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)
        GLES30.glClearColor(1f, 1f, 1f, 1f)

        try {
            program = createProgram(VERT_SHADER, FRAG_SHADER_FLAT)
            
            // 성능 최적화 컴포넌트 초기화 (선택적)
            try {
                instancedRenderer.initialize()
                Log.d(TAG, "Performance optimization components initialized")
            } catch (e: Exception) {
                Log.w(TAG, "Performance optimization components failed to initialize: ${e.message}")
            }
            
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
        uModelHandle = GLES30.glGetUniformLocation(program, "uModel")
        uTransparencyHandle = GLES30.glGetUniformLocation(program, "uTransparency")
        uLightPosHandle = GLES30.glGetUniformLocation(program, "uLightPos")
        uViewPosHandle = GLES30.glGetUniformLocation(program, "uViewPos")

        val buffers = IntArray(4)
        GLES30.glGenBuffers(4, buffers, 0)
        vertexVbo = buffers[0]
        colorVbo = buffers[1]
        normalVbo = buffers[2]
        indexVbo = buffers[3]

        val vaos = IntArray(1)
        GLES30.glGenVertexArrays(1, vaos, 0)
        vao = vaos[0]

        buffersReady = vertexVbo != 0 && colorVbo != 0 && indexVbo != 0 && vao != 0
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

        // 자동 회전 처리
        if (autoRotationEnabled) {
            val currentTime = System.currentTimeMillis()
            val deltaTime = (currentTime - lastFrameTime) / 1000.0f // 초 단위
            lastFrameTime = currentTime
            
            // Y축 주위로 자동 회전 (좌우 회전)
            val rotationDelta = rotationSpeed * deltaTime
            camera.orbit(rotationDelta * 50f, 0f) // 50f는 민감도 조정
        }

        GLES30.glUseProgram(program)
        val viewMatrix = camera.viewMatrix()
        
        // 모델 매트릭스 (구조물을 원점으로 이동 후 스케일 적용)
        android.opengl.Matrix.setIdentityM(modelMatrix, 0)
        
        // Info 모드에서만 구조물을 원점으로 이동 (카메라가 원점을 바라보므로)
        if (isInfoMode) {
            android.opengl.Matrix.translateM(modelMatrix, 0, -structureCenterX, -structureCenterY, -structureCenterZ)
        }
        
        // 스케일 적용
        android.opengl.Matrix.scaleM(modelMatrix, 0, structureScale, structureScale, structureScale)

        if (uModelHandle >= 0) {
            GLES30.glUniformMatrix4fv(uModelHandle, 1, false, modelMatrix, 0)
        }
        
        // 디버그 로그 (첫 번째 프레임에서만)
        if (indexCount > 0 && !hasLoggedFirstFrame) {
            Log.d(TAG, "Model matrix translation: (-$structureCenterX, -$structureCenterY, -$structureCenterZ)")
            Log.d(TAG, "Model matrix scale: $structureScale")
            hasLoggedFirstFrame = true
        }
        
        // MVP 매트릭스 계산 (Model * View * Projection)
        val tempMatrix = FloatArray(16)
        Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, tempMatrix, 0)

        GLES30.glBindVertexArray(vao)

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vertexVbo)
        GLES30.glEnableVertexAttribArray(aPositionHandle)
        GLES30.glVertexAttribPointer(aPositionHandle, 3, GLES30.GL_FLOAT, false, 0, 0)

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, colorVbo)
        GLES30.glEnableVertexAttribArray(aColorHandle)
        GLES30.glVertexAttribPointer(aColorHandle, 3, GLES30.GL_FLOAT, false, 0, 0)

        if (aNormalHandle >= 0 && normalVbo != 0) {
            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, normalVbo)
            GLES30.glEnableVertexAttribArray(aNormalHandle)
            GLES30.glVertexAttribPointer(aNormalHandle, 3, GLES30.GL_FLOAT, false, 0, 0)
        }

        GLES30.glUniformMatrix4fv(uMvpHandle, 1, false, mvpMatrix, 0)
        GLES30.glUniform1f(uTransparencyHandle, transparency)

        // 삼각형 메쉬로 Ribbon 렌더링
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, indexVbo)
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, indexCount, GLES30.GL_UNSIGNED_INT, 0)

        GLES30.glDisableVertexAttribArray(aPositionHandle)
        GLES30.glDisableVertexAttribArray(aColorHandle)
        if (aNormalHandle >= 0) {
            GLES30.glDisableVertexAttribArray(aNormalHandle)
        }
        GLES30.glBindVertexArray(0)
        
        // Ribbon 렌더링 후 Ligands와 Pockets를 작게 추가 렌더링
        renderLigandsAndPocketsForRibbon()
        
        // 렌더링 완료 콜백 호출 (구조 업데이트가 완료된 경우에만)
        if (pendingRenderingComplete) {
            pendingRenderingComplete = false
            onRenderingCompleteCallback?.let { callback ->
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    android.util.Log.d("ProperRibbonRenderer", "Rendering complete callback invoked")
                    callback()
                }
            }
        }
    }

    fun updateStructure(structure: PDBStructure?) {
        currentStructure = structure
        pendingStructure = structure
        if (!buffersReady) {
            indexCount = 0
            return
        }
        uploadStructure(structure)
        
        // 구조물 크기에 맞게 카메라 자동 조정
        if (structure != null) {
            adjustCameraForStructure(structure)
        }
    }
    
    private fun adjustCameraForStructure(structure: PDBStructure) {
        // 모든 원자의 위치를 분석하여 바운딩 박스 계산
        var minX = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var minY = Float.MAX_VALUE
        var maxY = Float.MIN_VALUE
        var minZ = Float.MAX_VALUE
        var maxZ = Float.MIN_VALUE
        
        structure.atoms.forEach { atom ->
            minX = minOf(minX, atom.position.x)
            maxX = maxOf(maxX, atom.position.x)
            minY = minOf(minY, atom.position.y)
            maxY = maxOf(maxY, atom.position.y)
            minZ = minOf(minZ, atom.position.z)
            maxZ = maxOf(maxZ, atom.position.z)
        }
        
        // 구조물의 중심점 계산
        val centerX = (minX + maxX) / 2f
        val centerY = (minY + maxY) / 2f
        val centerZ = (minZ + maxZ) / 2f
        
        // 구조물의 경계 반지름 계산 (GLESProteinRenderer 방식)
        val spanX = maxX - minX
        val spanY = maxY - minY
        val spanZ = maxZ - minZ
        val boundingRadius = maxOf(spanX, spanY, spanZ) * 0.5f
        
        // 카메라 타겟 설정 (Info 모드와 Viewer 모드 모두 구조물 중심을 바라봄)
        camera.setTarget(centerX, centerY, centerZ) // Info 모드와 Viewer 모드 모두 구조물 중심을 바라봄
        
        // 정확한 카메라 거리 계산 (GLESProteinRenderer와 동일한 방식)
        val distance = boundingRadius / Math.tan(Math.toRadians((camera.fovDeg * 0.5f).toDouble())).toFloat()
        val optimalDistance = distance * 1.4f
        
        // Info 모드와 zoomLevel을 고려한 최종 거리 설정
        // Info 모드에서도 적절한 거리로 설정하여 너무 가깝지 않도록 함
        val baseDistance = if (isInfoMode) optimalDistance * 1.5f else optimalDistance * 1.8f // Info 모드도 적절한 거리로
        // zoomLevel의 영향을 줄여서 갑작스러운 확대 방지
        val adjustedZoomLevel = 1.0f + (zoomLevel - 1.0f) * 0.3f // zoomLevel 영향 30%로 제한
        val finalDistance = baseDistance / adjustedZoomLevel
        
        camera.configure(
            distance = finalDistance,
            minDistance = boundingRadius * 0.3f + 1f,
            maxDistance = boundingRadius * 15f
        )
        
        // 카메라 초기 각도 설정 (구조물을 중앙에 잘 볼 수 있는 각도)
        camera.setInitialAngles(0f, 0f) // yaw: 0도, pitch: 0도 (정면에서 보기)
        
        // Info 모드에서만 적절한 스케일링으로 확대 (스타일별 조정)
        val scaleFactor = if (isInfoMode) {
            when (currentRenderStyle) {
                RenderStyle.SPHERES -> 1.5f // Spheres는 조금만 확대
                RenderStyle.STICKS -> 1.5f // Sticks도 조금만 확대
                RenderStyle.SURFACE -> 1.5f // Surface도 조금만 확대
                else -> 2f // Ribbon, Cartoon은 2배 확대
            }
        } else {
            1f // Viewer 모드: 기본 크기
        }
        
        // 구조물 중심점 저장 (나중에 모델 매트릭스에서 사용)
        structureCenterX = centerX
        structureCenterY = centerY
        structureCenterZ = centerZ
        
        // 스케일 팩터 적용
        structureScale = scaleFactor
        
        // 성능 최적화: 구조 바운딩 박스 로그 제거
    }
    
    private var structureScale = 1f // 구조물 스케일 팩터
    private var structureCenterX = 0f // 구조물 중심 X
    private var structureCenterY = 0f // 구조물 중심 Y  
    private var structureCenterZ = 0f // 구조물 중심 Z
    
    // 자동 회전 관련
    private var autoRotationEnabled = false
    private var rotationSpeed = 0.5f // 회전 속도 (도/프레임)
    private var lastFrameTime = System.currentTimeMillis()
    
    fun setInfoMode(isInfoMode: Boolean) {
        this.isInfoMode = isInfoMode
        Log.d(TAG, "Info mode set to: $isInfoMode")
        // Info 모드 변경 시 새로운 렌더링 수행 (색상 차이 반영을 위해)
        if (currentStructure != null) {
            adjustCameraForStructure(currentStructure!!)
            // Info 모드와 Viewer 모드의 색상 차이를 반영하기 위해 새로 렌더링
            // 성능 최적화: Info 모드 재렌더링 로그 제거
        }
    }
    
    fun updateRenderStyle(style: RenderStyle) {
        currentRenderStyle = style
        Log.d(TAG, "Render style changed to: $style")
        // 구조를 다시 업로드하여 렌더링 스타일 변경 적용
        uploadStructure(currentStructure)
        // 렌더 스타일 변경 후에도 카메라 설정을 다시 적용 (Info 모드 고려)
        if (currentStructure != null) {
            adjustCameraForStructure(currentStructure!!)
        }
    }
    
    fun updateOptions(
        rotationEnabled: Boolean = this.rotationEnabled,
        zoomLevel: Float = this.zoomLevel,
        transparency: Float = this.transparency,
        atomSize: Float = this.atomSize,
        ribbonWidth: Float = this.ribbonWidth,
        ribbonFlatness: Float = this.ribbonFlatness
    ) {
        this.rotationEnabled = rotationEnabled
        this.zoomLevel = zoomLevel
        this.transparency = transparency
        this.atomSize = atomSize
        this.ribbonWidth = ribbonWidth
        this.ribbonFlatness = ribbonFlatness
        
        // 자동 회전 설정 업데이트
        setAutoRotation(rotationEnabled)
        
        Log.d(TAG, "updateOptions: rotation=$rotationEnabled, zoom=$zoomLevel, transparency=$transparency, atomSize=$atomSize, ribbonWidth=$ribbonWidth, ribbonFlatness=$ribbonFlatness")
        
        // 구조가 있으면 다시 업로드
        currentStructure?.let { 
            uploadStructure(it)
        }
    }
    
    fun setAutoRotation(enabled: Boolean) {
        autoRotationEnabled = enabled
        Log.d(TAG, "Auto rotation ${if (enabled) "enabled" else "disabled"}")
    }
    
    fun setOnRenderingCompleteCallback(callback: (() -> Unit)?) {
        onRenderingCompleteCallback = callback
    }
    
    fun updateColorMode(mode: ColorMode) {
        val oldMode = currentColorMode
        currentColorMode = mode
        Log.d(TAG, "Color mode changed from $oldMode to: $mode")
        Log.d(TAG, "Current render style: $currentRenderStyle, isInfoMode: $isInfoMode")
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
        // 성능 최적화: 전체 구조 재업로드 대신 렌더링만 요청
        // requestRender()는 GLSurfaceView에서 사용 가능하므로 제거
    }
    
    fun updateFocusedElement(focusedElement: String?) {
        currentFocusedElement = focusedElement
        Log.d(TAG, "Focused element updated: $focusedElement")
        
        // Focus된 요소에 카메라 이동
        if (focusedElement != null) {
            moveCameraToFocusedElement(focusedElement)
        }
        
        // 성능 최적화: 전체 구조 재업로드 대신 렌더링만 요청
        // requestRender()는 GLSurfaceView에서 사용 가능하므로 제거
    }

    fun rotate(deltaX: Float, deltaY: Float) {
        camera.orbit(deltaX, deltaY)
        Log.d(TAG, "Camera rotated: deltaX=$deltaX, deltaY=$deltaY")
    }

    fun pan(deltaX: Float, deltaY: Float) {
        camera.pan(deltaX, deltaY)
        Log.d(TAG, "Camera panned: deltaX=$deltaX, deltaY=$deltaY")
    }

    fun zoom(scaleFactor: Float) {
        camera.zoom(scaleFactor)
        Log.d(TAG, "Camera zoomed: scaleFactor=$scaleFactor")
    }
    
    /**
     * Focus된 요소에 카메라를 이동시키는 함수
     */
    private fun moveCameraToFocusedElement(focusedElement: String) {
        currentStructure?.let { structure ->
            val targetPosition = when {
                // Chain focus
                focusedElement.startsWith("chain:") -> {
                    val chainId = focusedElement.removePrefix("chain:")
                    val chainAtoms = structure.atoms.filter { it.chain == chainId }
                    if (chainAtoms.isNotEmpty()) {
                        val center = chainAtoms.map { it.position }.reduce { acc, pos -> acc + pos } / chainAtoms.size.toFloat()
                        center
                    } else null
                }
                // Ligand focus
                focusedElement.startsWith("ligand:") -> {
                    val ligandName = focusedElement.removePrefix("ligand:")
                    val ligandAtoms = structure.atoms.filter { it.isLigand && it.residueName == ligandName }
                    if (ligandAtoms.isNotEmpty()) {
                        val center = ligandAtoms.map { it.position }.reduce { acc, pos -> acc + pos } / ligandAtoms.size.toFloat()
                        center
                    } else null
                }
                // Pocket focus
                focusedElement.startsWith("pocket:") -> {
                    val pocketName = focusedElement.removePrefix("pocket:")
                    val pocketAtoms = structure.atoms.filter { it.isPocket && it.residueName == pocketName }
                    if (pocketAtoms.isNotEmpty()) {
                        val center = pocketAtoms.map { it.position }.reduce { acc, pos -> acc + pos } / pocketAtoms.size.toFloat()
                        center
                    } else null
                }
                // Residue focus
                focusedElement.matches(Regex("\\d+")) -> {
                    val residueNumber = focusedElement.toIntOrNull()
                    if (residueNumber != null) {
                        val residueAtoms = structure.atoms.filter { it.residueNumber == residueNumber }
                        if (residueAtoms.isNotEmpty()) {
                            val center = residueAtoms.map { it.position }.reduce { acc, pos -> acc + pos } / residueAtoms.size.toFloat()
                            center
                        } else null
                    } else null
                }
                else -> null
            }
            
            if (targetPosition != null) {
                // 카메라를 해당 위치로 이동
                camera.setTarget(targetPosition.x, targetPosition.y, targetPosition.z)
                
                // 적절한 거리로 조정 (더 가까이) - 고정된 거리 사용
                val newDistance = 15f // 고정된 가까운 거리
                camera.configure(
                    distance = newDistance,
                    minDistance = 5f,
                    maxDistance = 100f
                )
                
                Log.d(TAG, "Camera moved to focused element: $focusedElement at position: (${targetPosition.x}, ${targetPosition.y}, ${targetPosition.z})")
            }
        }
    }

    private fun uploadStructure(structure: PDBStructure?) {
        if (structure == null || structure.atoms.isEmpty()) {
            indexCount = 0
            return
        }

        Log.d(TAG, "uploadStructure: currentRenderStyle=$currentRenderStyle")

        // 복잡한 단백질 감지 및 LOD 설정
        val atomCount = structure.atoms.size
        isComplexProtein = atomCount > 10000 // 1만개 이상 원자는 복잡한 단백질로 간주
        lodLevel = when {
            atomCount > 50000 -> 3 // 매우 복잡: 저품질
            atomCount > 20000 -> 2 // 복잡: 중품질
            else -> 1 // 일반: 고품질
        }
        
        Log.d(TAG, "Protein complexity: $atomCount atoms, LOD level: $lodLevel, isComplex: $isComplexProtein")
        
        // LOD에 따른 튜브 세그먼트 수 조정 (성능 최적화)
        tubeSegments = when (lodLevel) {
            3 -> 4 // 매우 복잡: 4개 세그먼트 (저품질)
            2 -> 6 // 복잡: 6개 세그먼트 (중품질)
            else -> 8 // 일반: 8개 세그먼트 (고품질)
        }
        Log.d(TAG, "LOD optimization: tubeSegments set to $tubeSegments")

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
        
        // 렌더링 완료 콜백 대기 상태 설정
        pendingRenderingComplete = true
    }
    
    private fun uploadSpheresStructure(structure: PDBStructure) {
        val atoms = structure.atoms
        
        if (atoms.isEmpty()) {
            Log.w(TAG, "No atoms found for spheres rendering")
            indexCount = 0
            return
        }

        Log.d(TAG, "Found ${atoms.size} atoms for spheres rendering")
        
        // LOD 매니저를 사용한 동적 최적화
        val currentLOD = lodManager.determineLODLevel(atoms, RenderStyle.SPHERES)
        val limitedAtoms = lodManager.filterAtomsForLOD(atoms, RenderStyle.SPHERES)
        val optimalSegments = lodManager.getOptimalSegments(RenderStyle.SPHERES)
        
        Log.d(TAG, "LOD optimization: ${atoms.size} -> ${limitedAtoms.size} atoms, segments: $optimalSegments")

        // Ligands와 Pockets를 작고 투명하게 렌더링하기 위해 분리
        val proteinAtoms = limitedAtoms.filter { !it.isLigand && !it.isPocket }
        val ligandAtoms = limitedAtoms.filter { it.isLigand }
        val pocketAtoms = limitedAtoms.filter { it.isPocket }
        
        // LOD 매니저에서 최적화된 세그먼트 수 사용
        val sphereSegments = optimalSegments
        
        // 메인 단백질 구조 렌더링
        val sphereData = if (proteinAtoms.isNotEmpty()) {
            sphereRenderer.createSphereRenderData(
                atoms = proteinAtoms,
                colorMode = currentColorMode,
                radius = 0.8f * atomSize, // atomSize 적용
                segments = sphereSegments  // LOD에 따른 세그먼트 수
            )
        } else {
            // 모든 원자가 Ligand/Pocket인 경우 기본 처리
            sphereRenderer.createSphereRenderData(
                atoms = limitedAtoms,
                colorMode = currentColorMode,
                radius = 0.8f * atomSize,
                segments = sphereSegments
            )
        }
        
        // Ligands를 작고 투명하게 추가 렌더링
        val ligandData = if (ligandAtoms.isNotEmpty()) {
            sphereRenderer.createSphereRenderData(
                atoms = ligandAtoms,
                colorMode = ColorMode.UNIFORM, // 주황색으로 통일
                radius = 0.8f, // 적절한 크기로 조정
                segments = max(4, sphereSegments / 2) // LOD에 따른 세그먼트 수
            )
        } else null
        
        // Pockets를 작고 투명하게 추가 렌더링
        val pocketData = if (pocketAtoms.isNotEmpty()) {
            sphereRenderer.createSphereRenderData(
                atoms = pocketAtoms,
                colorMode = ColorMode.UNIFORM, // 보라색으로 통일
                radius = 0.8f, // 적절한 크기로 조정
                segments = max(4, sphereSegments / 2) // LOD에 따른 세그먼트 수
            )
        } else null

        // 메인 구조와 Ligands/Pockets 데이터 합치기
        val combinedVertices = sphereData.vertices.toMutableList()
        val combinedColors = sphereData.colors.toMutableList()
        val combinedNormals = sphereData.normals.toMutableList()
        val combinedIndices = sphereData.indices.toMutableList()
        
        var indexOffset = sphereData.vertices.size / 3
        
        // Ligands 데이터 추가
        ligandData?.let { data ->
            combinedVertices.addAll(data.vertices)
            // Ligands는 주황색으로 설정 (투명도 적용)
            val orangeColors = FloatArray(data.colors.size) { i ->
                when (i % 3) {
                    0 -> 1.0f * ligandTransparency // R (투명도 적용)
                    1 -> 0.5f * ligandTransparency // G (투명도 적용)
                    2 -> 0.0f * ligandTransparency // B (투명도 적용)
                    else -> data.colors[i]
                }
            }
            combinedColors.addAll(orangeColors.toList())
            combinedNormals.addAll(data.normals)
            
            // 인덱스 오프셋 적용
            val adjustedIndices = data.indices.map { it + indexOffset }
            combinedIndices.addAll(adjustedIndices)
            indexOffset += data.vertices.size / 3
        }
        
        // Pockets 데이터 추가
        pocketData?.let { data ->
            combinedVertices.addAll(data.vertices)
            // Pockets는 보라색으로 설정 (투명도 적용)
            val purpleColors = FloatArray(data.colors.size) { i ->
                when (i % 3) {
                    0 -> 0.6f * pocketTransparency // R (투명도 적용)
                    1 -> 0.2f * pocketTransparency // G (투명도 적용)
                    2 -> 0.7f * pocketTransparency // B (투명도 적용)
                    else -> data.colors[i]
                }
            }
            combinedColors.addAll(purpleColors.toList())
            combinedNormals.addAll(data.normals)
            
            // 인덱스 오프셋 적용
            val adjustedIndices = data.indices.map { it + indexOffset }
            combinedIndices.addAll(adjustedIndices)
        }
        
        // Highlight 효과 적용 (메인 구조만)
        val highlightedColors = applyHighlightEffect(combinedColors, atoms)

        indexCount = combinedIndices.size

        // 버퍼에 업로드
        uploadToGPU(combinedVertices, highlightedColors.toList(), combinedNormals, combinedIndices)

        Log.d(TAG, "Uploaded ${combinedVertices.size / 3} vertices, $indexCount indices for spheres (including ligands and pockets)")
    }

    private fun uploadSticksStructure(structure: PDBStructure) {
        val atoms = structure.atoms
        
        if (atoms.isEmpty()) {
            Log.w(TAG, "No atoms found for sticks rendering")
            indexCount = 0
            return
        }

        Log.d(TAG, "Found ${atoms.size} atoms for sticks rendering")

        // SphereRenderer를 사용하여 구체 렌더링 데이터 생성
        val sphereData = sphereRenderer.createSphereRenderData(
            atoms = atoms,
            colorMode = currentColorMode,
            radius = 0.5f * atomSize, // Sticks용 구체 크기 (Spheres보다 작게) + atomSize 적용
            segments = 12  // 매끄러운 구체 (회전 시 각진 모서리 제거)
        )

        // Highlight 효과 적용
        val highlightedColors = applyHighlightEffect(sphereData.colors, atoms)

        val allVertices = mutableListOf<Float>()
        val allColors = mutableListOf<Float>()
        val allNormals = mutableListOf<Float>()
        val allIndices = mutableListOf<Int>()
        
        // 구체 데이터 추가
        allVertices.addAll(sphereData.vertices)
        allColors.addAll(highlightedColors)
        allNormals.addAll(sphereData.normals)
        allIndices.addAll(sphereData.indices)
        
        var vertexOffset = sphereData.vertices.size / 3

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
                        allIndices.add(index + vertexOffset)
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
        val allIndices = mutableListOf<Int>()
        
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
                var finalColor: List<Float>
                
                // Element 색상 모드일 때는 구조별 색상 사용 (리본의 시각적 특징)
                if (currentColorMode == ColorMode.ELEMENT) {
                    // Cartoon도 구조별 색상이 더 적절: α-helix(빨강), β-sheet(노랑), loop(파랑)
                    finalColor = getSecondaryStructureColor(splinePoint.secondaryStructure)
                    Log.d(TAG, "Cartoon Element: Chain $chain, spline point $index, structure ${splinePoint.secondaryStructure}, color: $finalColor")
                } else {
                    // 다른 색상 모드들은 첫 번째 원자 기준으로 색상 결정
                    val firstAtom = sortedAtoms.first()
                    val atomColor = getAtomColor(firstAtom)
                    finalColor = listOf(atomColor[0], atomColor[1], atomColor[2])
                    
                    // Uniform 모드가 아닐 때만 2차 구조 색상 블렌딩
                    if (currentColorMode != ColorMode.UNIFORM) {
                        val structureColor = getSecondaryStructureColor(splinePoint.secondaryStructure)
                        finalColor = blendColors(finalColor, structureColor, alpha = 0.8f)
                    }
                }
                
                // Highlight 효과
                if (isInfoMode && !hasAnyHighlight) {
                    // Info 모드에서도 Element 색상 모드는 원래 색상 유지 (구조별 색상이 중요)
                    if (currentColorMode != ColorMode.ELEMENT) {
                        finalColor = finalColor.map { it * 0.3f }
                    }
                } else if (hasAnyHighlight) {
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
                allIndices.add(index + vertexOffset)
            }
            
            vertexOffset += cartoonMesh.vertices.size / 3
        }

        indexCount = allIndices.size

        // 버퍼에 업로드
        uploadToGPU(allVertices, allColors, allNormals, allIndices)

        Log.d(TAG, "Uploaded ${allVertices.size / 3} vertices, $indexCount indices for cartoon")
    }





    private fun createCylinderMesh(start: Vector3, end: Vector3, radius: Float, segments: Int): MeshData {
        val vertices = mutableListOf<Float>()
        val normals = mutableListOf<Float>()
        val indices = mutableListOf<Int>()
        
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
                current,
                current + 1,
                next
            ))
            
            // 두 번째 삼각형
            indices.addAll(listOf(
                current + 1,
                next + 1,
                next
            ))
        }
        
        return MeshData(vertices, normals, indices)
    }

    private fun createCartoonTubeMesh(splinePoints: List<SplinePoint>, segments: Int): MeshData {
        val vertices = mutableListOf<Float>()
        val normals = mutableListOf<Float>()
        val indices = mutableListOf<Int>()
        
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
                indices.add(index + vertexOffset)
            }
            
            vertexOffset += cylinderMesh.vertices.size / 3
        }
        
        return MeshData(vertices, normals, indices)
    }

    /**
     * Highlight와 Focus 효과를 적용하는 함수
     */
    private fun applyHighlightEffect(colors: List<Float>, atoms: List<Atom>): List<Float> {
        val highlightedColors = mutableListOf<Float>()
        var colorIndex = 0
        
        atoms.forEach { atom ->
            val chainKey = "chain:${atom.chain}"
            val ligandKey = "ligand:${atom.residueName}"
            val pocketKey = "pocket:${atom.residueName}"
            
            val isHighlighted = currentHighlightedChains.contains(chainKey) || 
                               currentHighlightedChains.contains(ligandKey) ||
                               currentHighlightedChains.contains(pocketKey)
            
            val isFocused = when {
                atom.isLigand && currentFocusedElement == ligandKey -> true
                atom.isPocket && currentFocusedElement == pocketKey -> true
                currentFocusedElement == chainKey -> true
                else -> false
            }
            
            val hasAnyHighlight = currentHighlightedChains.isNotEmpty() || currentFocusedElement != null
            
            // 각 원자의 모든 정점에 대해 색상 적용
            val verticesPerAtom = colors.size / atoms.size / 3 // 각 원자당 정점 수 (RGB)
            repeat(verticesPerAtom) {
                if (colorIndex < colors.size) {
                    val originalColor = colors.subList(colorIndex, colorIndex + 3)
                    val baseColor = when {
                        isFocused -> {
                            // Focused: 매우 밝고 선명하게 (카메라가 해당 요소로 이동)
                            originalColor.map { (it * 2.0f).coerceAtMost(1.0f) }
                        }
                        hasAnyHighlight -> {
                            if (isHighlighted) {
                                // Highlighted: 밝고 선명하게
                                originalColor.map { (it * 1.4f).coerceAtMost(1.0f) }
                            } else {
                                // Not highlighted: 매우 희미하게
                                originalColor.map { it * 0.15f }
                            }
                        }
                        else -> originalColor
                    }
                    val adjustedColor = if (isInfoMode && !hasAnyHighlight && !isFocused) {
                        adjustInfoModeBaseColor(baseColor)
                    } else {
                        baseColor
                    }
                    highlightedColors.addAll(adjustedColor)
                    colorIndex += 3
                }
            }
        }
        
        return highlightedColors
    }

    private fun isAtomHighlighted(atom: Atom): Boolean {
        val chainKey = "chain:${atom.chain}"
        val ligandKey = "ligand:${atom.residueName}"
        val pocketKey = "pocket:${atom.residueName}"
        return currentHighlightedChains.contains(chainKey) ||
            currentHighlightedChains.contains(ligandKey) ||
            currentHighlightedChains.contains(pocketKey)
    }

    private fun isAtomFocused(atom: Atom): Boolean {
        val chainKey = "chain:${atom.chain}"
        val ligandKey = "ligand:${atom.residueName}"
        val pocketKey = "pocket:${atom.residueName}"
        return when {
            atom.isLigand && currentFocusedElement == ligandKey -> true
            atom.isPocket && currentFocusedElement == pocketKey -> true
            currentFocusedElement == chainKey -> true
            else -> false
        }
    }

    private fun createUniformColorList(size: Int, color: Triple<Float, Float, Float>): List<Float> {
        val (r, g, b) = color
        val list = FloatArray(size)
        for (i in list.indices step 3) {
            list[i] = r
            if (i + 1 < list.size) list[i + 1] = g
            if (i + 2 < list.size) list[i + 2] = b
        }
        return list.toList()
    }

    private fun scaleColorList(colors: List<Float>, scale: Float): List<Float> {
        if (scale == 1.0f) return colors
        return colors.map { value -> (value * scale).coerceAtMost(1.0f) }
    }

    private fun adjustInfoModeBaseColor(color: List<Float>): List<Float> {
        if (color.isEmpty()) return color
        val r = color[0]
        val g = color.getOrElse(1) { r }
        val b = color.getOrElse(2) { r }
        val maxChannel = maxOf(r, g, b)
        val targetBrightness = 0.45f
        val minLift = 0.2f
        val factor = if (maxChannel < targetBrightness) {
            targetBrightness / (maxChannel.coerceAtLeast(0.05f))
        } else {
            1.0f
        }
        val adjust = listOf(
            (r * factor + minLift).coerceAtMost(1.0f),
            (g * factor + minLift).coerceAtMost(1.0f),
            (b * factor + minLift).coerceAtMost(1.0f)
        )
        return adjust
    }

    data class MeshData(
        val vertices: List<Float>,
        val normals: List<Float>,
        val indices: List<Int>
    )

    private fun uploadSurfaceStructure(structure: PDBStructure) {
        val atoms = structure.atoms
        
        if (atoms.isEmpty()) {
            Log.w(TAG, "No atoms found for surface rendering")
            indexCount = 0
            return
        }

        Log.d(TAG, "Found ${atoms.size} atoms for surface rendering")

        // SphereRenderer를 사용하여 구체 렌더링 데이터 생성 (Surface는 Uniform 모드로)
        val sphereData = sphereRenderer.createSphereRenderData(
            atoms = atoms,
            colorMode = ColorMode.UNIFORM, // Surface는 항상 회색
            radius = 1.0f * atomSize, // Surface용 큰 구체 + atomSize 적용
            segments = 20  // 매끄러운 구체 (회전 시 각진 모서리 제거)
        )

        // Highlight 효과 적용
        val highlightedColors = applyHighlightEffect(sphereData.colors, atoms)

        indexCount = sphereData.indices.size

        // 버퍼에 업로드
        uploadToGPU(sphereData.vertices, highlightedColors, sphereData.normals, sphereData.indices)

        Log.d(TAG, "Uploaded ${sphereData.vertices.size / 3} vertices, $indexCount indices for surface")
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

        // 바운딩 박스 계산 및 카메라 설정 (모든 원자 기준으로 설정)
        setupCamera(structure.atoms)

        // 체인별로 그룹화
        val chainGroups = caAtoms.groupBy { it.chain }
        
        val allVertices = mutableListOf<Float>()
        val allColors = mutableListOf<Float>()
        val allNormals = mutableListOf<Float>()
        val allIndices = mutableListOf<Int>()
        
        var vertexOffset = 0

        chainGroups.forEach { (chain, atoms) ->
            val sortedAtoms = atoms.sortedBy { it.residueNumber }
            
            if (sortedAtoms.size < 2) return@forEach
            
            // Catmull-Rom 스플라인으로 부드러운 곡선 생성
            val splinePoints = generateCatmullRomSpline(sortedAtoms, numSegments = 10)
            
            // 튜브 메쉬 생성 (ribbonWidth 적용)
            val mesh = createTubeMesh(splinePoints, ribbonRadius * ribbonWidth, tubeSegments)
            
            // 체인 하이라이트 상태 확인 (iPhone과 동일)
            val chainKey = "chain:$chain"
            val isHighlighted = currentHighlightedChains.contains(chainKey)
            val hasAnyHighlight = currentHighlightedChains.isNotEmpty()
            
            // 메쉬 데이터 추가
            allVertices.addAll(mesh.vertices)
            allNormals.addAll(mesh.normals)
            
            // 색상 모드에 따른 색상 적용 + Highlight 효과
            val verticesPerSplinePoint = (tubeSegments + 1)
            splinePoints.forEachIndexed { index, splinePoint ->
                var finalColor: List<Float>
                
                // Element 색상 모드일 때는 구조별 색상 사용 (리본의 시각적 특징)
                if (currentColorMode == ColorMode.ELEMENT) {
                    // 리본은 구조별 색상이 더 적절: α-helix(빨강), β-sheet(노랑), loop(파랑)
                    finalColor = getSecondaryStructureColor(splinePoint.secondaryStructure)
                    Log.d(TAG, "Ribbon Element: Chain $chain, spline point $index, structure ${splinePoint.secondaryStructure}, color: $finalColor")
                } else {
                    // 다른 색상 모드들은 첫 번째 원자 기준으로 색상 결정
                    val firstAtom = sortedAtoms.first()
                    val atomColor = getAtomColor(firstAtom)
                    finalColor = listOf(atomColor[0], atomColor[1], atomColor[2])
                    
                    // Uniform 모드가 아닐 때만 2차 구조 색상 블렌딩
                    if (currentColorMode != ColorMode.UNIFORM) {
                        val structureColor = getSecondaryStructureColor(splinePoint.secondaryStructure)
                        finalColor = blendColors(finalColor, structureColor, alpha = 0.6f)
                    }
                }
                
                // Highlight 효과 (iPhone과 동일)
                if (isInfoMode && !hasAnyHighlight) {
                    // Info 모드에서도 Element 색상 모드는 원래 색상 유지 (구조별 색상이 중요)
                    if (currentColorMode != ColorMode.ELEMENT) {
                        finalColor = finalColor.map { it * 0.3f }
                    }
                } else if (hasAnyHighlight) {
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
                allIndices.add(index + vertexOffset)
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

    // Catmull-Rom 스플라인 곡선 생성 (Cα 원자만 사용)
    private fun generateCatmullRomSpline(
        atoms: List<Atom>,
        tension: Float = 0.5f,
        numSegments: Int = 10
    ): List<SplinePoint> {
        // Cα 원자만 필터링 (제안된 구조에 맞춤)
        val caAtoms = atoms.filter { it.name == "CA" }
        if (caAtoms.isEmpty()) {
            Log.w(TAG, "No Cα atoms found for spline generation")
            return emptyList()
        }
        
        val result = mutableListOf<SplinePoint>()

        for (i in 0 until caAtoms.size - 1) {
            val p0 = if (i > 0) caAtoms[i - 1].position else caAtoms[i].position
            val p1 = caAtoms[i].position
            val p2 = caAtoms[i + 1].position
            val p3 = if (i < caAtoms.size - 2) caAtoms[i + 2].position else caAtoms[i + 1].position

            for (j in 0..numSegments) {
                val t = j.toFloat() / numSegments
                val point = catmullRom(p0, p1, p2, p3, t, tension)
                result.add(SplinePoint(Vector3(point.x, point.y, point.z), caAtoms[i].secondaryStructure))
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
        val indices = mutableListOf<Int>()

        for (i in 0 until curve.size - 1) {
            val p1 = curve[i].position
            val p2 = curve[i + 1].position

            // 튜브 방향 벡터
            val direction = (p2 - p1).normalize()

            // 수직 벡터 계산
            val up = Vector3(0f, 1f, 0f)
            val right = direction.cross(up).normalize()
            val normal = direction.cross(right).normalize()

            // 타원형 단면 생성 (ribbonFlatness 적용)
            for (j in 0..segments) {
                val angle = (j.toFloat() / segments) * 2 * PI.toFloat()
                val cosAngle = cos(angle)
                val sinAngle = sin(angle)

                // ribbonFlatness로 타원형 만들기 (0.1=매우 납작, 1.0=원형)
                val horizontalRadius = radius
                val verticalRadius = radius * ribbonFlatness

                val offset = (right * cosAngle * horizontalRadius + normal * sinAngle * verticalRadius)
                val vertex = p1 + offset

                vertices.addAll(listOf(vertex.x, vertex.y, vertex.z))

                val n = offset.normalize()
                normals.addAll(listOf(n.x, n.y, n.z))
            }
        }

        // 인덱스 생성
        for (i in 0 until curve.size - 2) {
            for (j in 0 until segments) {
                val i0 = i * (segments + 1) + j
                val i1 = i0 + 1
                val i2 = i0 + segments + 1
                val i3 = i2 + 1

                indices.addAll(listOf(i0, i2, i1, i1, i2, i3))
            }
        }

        return Mesh(vertices, normals, indices)
    }

    private fun uploadToGPU(
        vertices: List<Float>,
        colors: List<Float>,
        normals: List<Float>,
        indices: List<Int>
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

        val indexBuffer = ByteBuffer.allocateDirect(indices.size * 4).order(ByteOrder.nativeOrder()).asIntBuffer()
        indices.forEach { indexBuffer.put(it) }
        indexBuffer.flip()

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vertexVbo)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vertexBuffer.capacity() * 4, vertexBuffer, GLES30.GL_STATIC_DRAW)

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, colorVbo)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, colorBuffer.capacity() * 4, colorBuffer, GLES30.GL_STATIC_DRAW)

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, normalVbo)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, normalBuffer.capacity() * 4, normalBuffer, GLES30.GL_STATIC_DRAW)

        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, indexVbo)
        GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, indexBuffer.capacity() * 4, indexBuffer, GLES30.GL_STATIC_DRAW)

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

    // 헬퍼 함수들
    private fun getAtomColor(atom: Atom): List<Float> {
        return when (currentColorMode) {
            ColorMode.ELEMENT -> {
                // Element 컬러링: CPK 색상 체계 적용 (아이폰과 동일)
                val elementColor = ColorMaps.cpk(atom.element)
                val color = listOf(
                    Color.red(elementColor) / 255f,
                    Color.green(elementColor) / 255f,
                    Color.blue(elementColor) / 255f,
                    1.0f
                )
                Log.d(TAG, "Element color for ${atom.element}: $color (RGB: ${Color.red(elementColor)}, ${Color.green(elementColor)}, ${Color.blue(elementColor)})")
                color
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
                listOf(0.5f, 0.5f, 0.5f, 1.0f)
            }
            ColorMode.SECONDARY_STRUCTURE -> {
                // Secondary Structure 컬러링: 2차 구조별 색상 적용 (제안된 구조에 맞춤)
                val structureColor = ColorMaps.secondaryStructure(atom.secondaryStructure)
                val color = listOf(
                    Color.red(structureColor) / 255f,
                    Color.green(structureColor) / 255f,
                    Color.blue(structureColor) / 255f,
                    1.0f
                )
                color
            }
        }
    }

    private fun getSecondaryStructureColor(structure: SecondaryStructure): List<Float> {
        // 제안된 구조에 맞춘 색상 매핑 사용
        val structureColor = ColorMaps.secondaryStructure(structure)
        return listOf(
            Color.red(structureColor) / 255f,
            Color.green(structureColor) / 255f,
            Color.blue(structureColor) / 255f
        )
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
        val indices: List<Int>
    )

    companion object {
        private const val TAG = "ProperRibbonRenderer"

        private const val VERT_SHADER = """#version 300 es
layout (location = 0) in vec3 aPosition;
layout (location = 1) in vec3 aColor;
layout (location = 2) in vec3 aNormal;
uniform mat4 uMvp;
uniform mat4 uModel;
out vec3 vColor;
out vec3 vNormal;
out vec3 vPosition;
void main() {
    gl_Position = uMvp * vec4(aPosition, 1.0);
    vColor = aColor;
    vNormal = mat3(uModel) * aNormal;
    vPosition = vec3(uModel * vec4(aPosition, 1.0));
}"""

        private const val FRAG_SHADER_FLAT = """#version 300 es
precision mediump float;
in vec3 vColor;
uniform float uTransparency;
out vec4 fragColor;
void main() {
    // 플랫 색상 렌더링 (Ribbon/Cartoon용)
    fragColor = vec4(vColor, uTransparency);
}"""

        private const val FRAG_SHADER_LIT = """#version 300 es
precision mediump float;
in vec3 vColor;
in vec3 vNormal;
in vec3 vPosition;
uniform float uTransparency;
uniform vec3 uLightPos;
uniform vec3 uViewPos;
out vec4 fragColor;
void main() {
    // 조명 위치가 (0,0,0)이면 플랫 색상 렌더링 (Ribbon/Cartoon용)
    if (length(uLightPos) < 0.1) {
        fragColor = vec4(vColor, uTransparency);
        return;
    }
    
    // 정규화된 노멀 벡터
    vec3 normal = normalize(vNormal);
    
    // 조명 벡터들
    vec3 lightDir = normalize(uLightPos - vPosition);
    vec3 viewDir = normalize(uViewPos - vPosition);
    vec3 reflectDir = reflect(-lightDir, normal);
    
    // 조명 계산 (Phong 모델)
    float ambient = 0.3; // 주변광
    float diffuse = max(dot(normal, lightDir), 0.0); // 확산광
    float specular = pow(max(dot(viewDir, reflectDir), 0.0), 32.0); // 반사광
    
    // 최종 색상 계산
    vec3 lighting = ambient + diffuse * 0.7 + specular * 0.3;
    vec3 finalColor = vColor * lighting;
    
    fragColor = vec4(finalColor, uTransparency);
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
    
    /**
     * Ribbon 렌더링에서 Ligands와 Pockets를 작게 추가 렌더링
     */
    private fun renderLigandsAndPocketsForRibbon() {
        currentStructure?.let { structure ->
            val ligandAtoms = structure.atoms.filter { it.isLigand }
            val pocketAtoms = structure.atoms.filter { it.isPocket }
            
            // 디버그: 매번 로그 출력 (임시)
            Log.d(TAG, "renderLigandsAndPocketsForRibbon: Ligands: ${ligandAtoms.size}, Pockets: ${pocketAtoms.size}")
            Log.d(TAG, "Total atoms: ${structure.atoms.size}")
            
            // 로그 최적화: 첫 번째 렌더링에서만 출력
            if (!hasLoggedLigandsPockets) {
                Log.d(TAG, "Rendering Ligands: ${ligandAtoms.size}, Pockets: ${pocketAtoms.size}")
                Log.d(TAG, "Structure center: ($structureCenterX, $structureCenterY, $structureCenterZ)")
                
                if (ligandAtoms.isNotEmpty()) {
                    val firstLigand = ligandAtoms.first()
                    Log.d(TAG, "First ligand position: (${firstLigand.position.x}, ${firstLigand.position.y}, ${firstLigand.position.z})")
                }
                
                if (pocketAtoms.isNotEmpty()) {
                    val firstPocket = pocketAtoms.first()
                    Log.d(TAG, "First pocket position: (${firstPocket.position.x}, ${firstPocket.position.y}, ${firstPocket.position.z})")
                }
                hasLoggedLigandsPockets = true
            }
            
            if (ligandAtoms.isNotEmpty() || pocketAtoms.isNotEmpty()) {
                Log.d(TAG, "Using sphereRenderer for Ribbon Ligands/Pockets")
                
                // OpenGL 상태 명시적으로 설정
                GLES30.glUseProgram(program)
                GLES30.glEnable(GLES30.GL_DEPTH_TEST)
                GLES30.glEnable(GLES30.GL_CULL_FACE)
                GLES30.glCullFace(GLES30.GL_BACK)
                
                // Spheres와 같은 방식으로 렌더링
                renderLigandsAndPocketsWithSphereRenderer(ligandAtoms, pocketAtoms)
            } else {
                Log.d(TAG, "No ligands or pockets to render")
            }
        }
    }
    
    /**
     * Ribbon에서 sphereRenderer를 사용하여 Ligands와 Pockets 렌더링
     */
    private fun renderLigandsAndPocketsWithSphereRenderer(ligandAtoms: List<Atom>, pocketAtoms: List<Atom>) {
        fun renderAtomBatch(
            atoms: List<Atom>,
            colorMode: ColorMode,
            radius: Float,
            segments: Int,
            alpha: Float,
            colorOverride: Triple<Float, Float, Float>? = null,
            colorScale: Float = 1.0f
        ) {
            if (atoms.isEmpty()) return
            val data = sphereRenderer.createSphereRenderData(
                atoms = atoms,
                colorMode = colorMode,
                radius = radius,
                segments = segments
            )
            val recolored = when {
                colorOverride != null -> data.copy(colors = createUniformColorList(data.colors.size, colorOverride))
                colorScale != 1.0f -> data.copy(colors = scaleColorList(data.colors, colorScale))
                else -> data
            }
            renderSphereData(recolored, alpha)
        }

        if (ligandAtoms.isNotEmpty()) {
            val ligandColorMode = if (currentColorMode == ColorMode.UNIFORM) ColorMode.UNIFORM else ColorMode.ELEMENT
            val focusedLigands = mutableListOf<Atom>()
            val highlightedLigands = mutableListOf<Atom>()
            val baseLigands = mutableListOf<Atom>()
            ligandAtoms.forEach { atom ->
                when {
                    isAtomFocused(atom) -> focusedLigands.add(atom)
                    isAtomHighlighted(atom) -> highlightedLigands.add(atom)
                    else -> baseLigands.add(atom)
                }
            }

            val hasFocus = currentFocusedElement != null
            val baseAlpha = if (hasFocus) 0.3f else 0.7f
            val ligandBaseRadius = 0.045f * atomSize

            renderAtomBatch(
                atoms = baseLigands,
                colorMode = ligandColorMode,
                radius = ligandBaseRadius,
                segments = 8,
                alpha = baseAlpha * transparency
            )

            renderAtomBatch(
                atoms = highlightedLigands,
                colorMode = ligandColorMode,
                radius = ligandBaseRadius * 1.3f,
                segments = 8,
                alpha = 0.9f * transparency,
                colorScale = 1.15f
            )

            renderAtomBatch(
                atoms = focusedLigands,
                colorMode = ligandColorMode,
                radius = ligandBaseRadius * 1.6f,
                segments = 8,
                alpha = 1.0f * transparency,
                colorScale = 1.2f
            )
        }

        if (pocketAtoms.isNotEmpty()) {
            val pocketColorMode = if (currentColorMode == ColorMode.UNIFORM) {
                ColorMode.UNIFORM
            } else {
                currentColorMode
            }
            val focusedPockets = mutableListOf<Atom>()
            val highlightedPockets = mutableListOf<Atom>()
            val basePockets = mutableListOf<Atom>()
            pocketAtoms.forEach { atom ->
                when {
                    isAtomFocused(atom) -> focusedPockets.add(atom)
                    isAtomHighlighted(atom) -> highlightedPockets.add(atom)
                    else -> basePockets.add(atom)
                }
            }

            val pocketBaseRadius = 0.3f * atomSize

            renderAtomBatch(
                atoms = basePockets,
                colorMode = pocketColorMode,
                radius = pocketBaseRadius,
                segments = 8,
                alpha = 0.4f * transparency
            )

            renderAtomBatch(
                atoms = highlightedPockets,
                colorMode = pocketColorMode,
                radius = pocketBaseRadius * 1.5f,
                segments = 8,
                alpha = 0.8f * transparency,
                colorScale = 1.15f
            )

            renderAtomBatch(
                atoms = focusedPockets,
                colorMode = pocketColorMode,
                radius = pocketBaseRadius * 2.0f,
                segments = 8,
                alpha = 1.0f * transparency,
                colorScale = 1.3f
            )
        }

        if (uTransparencyHandle >= 0) {
            GLES30.glUniform1f(uTransparencyHandle, transparency)
        }
    }
    
    /**
     * Sphere 데이터를 렌더링하는 함수
     */
    private fun renderSphereData(sphereData: SphereRenderData, alpha: Float = transparency) {
        if (uModelHandle >= 0) {
            GLES30.glUniformMatrix4fv(uModelHandle, 1, false, modelMatrix, 0)
        }

        if (uTransparencyHandle >= 0) {
            GLES30.glUniform1f(uTransparencyHandle, alpha)
        }

        // 임시 VBO 생성
        val tempVertexVbo = IntArray(1)
        val tempColorVbo = IntArray(1)
        val tempNormalVbo = IntArray(1)
        val tempIndexVbo = IntArray(1)
        
        GLES30.glGenBuffers(1, tempVertexVbo, 0)
        GLES30.glGenBuffers(1, tempColorVbo, 0)
        GLES30.glGenBuffers(1, tempNormalVbo, 0)
        GLES30.glGenBuffers(1, tempIndexVbo, 0)
        
        // 버텍스 데이터 업로드
        val vertexBuffer = ByteBuffer.allocateDirect(sphereData.vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        vertexBuffer.put(sphereData.vertices.toFloatArray())
        vertexBuffer.position(0)
        
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, tempVertexVbo[0])
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vertexBuffer.capacity() * 4, vertexBuffer, GLES30.GL_STATIC_DRAW)
        GLES30.glEnableVertexAttribArray(aPositionHandle)
        GLES30.glVertexAttribPointer(aPositionHandle, 3, GLES30.GL_FLOAT, false, 0, 0)
        
        // 컬러 데이터 업로드
        val colorBuffer = ByteBuffer.allocateDirect(sphereData.colors.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        colorBuffer.put(sphereData.colors.toFloatArray())
        colorBuffer.position(0)
        
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, tempColorVbo[0])
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, colorBuffer.capacity() * 4, colorBuffer, GLES30.GL_STATIC_DRAW)
        GLES30.glEnableVertexAttribArray(aColorHandle)
        GLES30.glVertexAttribPointer(aColorHandle, 3, GLES30.GL_FLOAT, false, 0, 0)
        
        // 노멀 데이터 업로드
        if (aNormalHandle >= 0) {
            val normalBuffer = ByteBuffer.allocateDirect(sphereData.normals.size * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
            normalBuffer.put(sphereData.normals.toFloatArray())
            normalBuffer.position(0)

            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, tempNormalVbo[0])
            GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, normalBuffer.capacity() * 4, normalBuffer, GLES30.GL_STATIC_DRAW)
            GLES30.glEnableVertexAttribArray(aNormalHandle)
            GLES30.glVertexAttribPointer(aNormalHandle, 3, GLES30.GL_FLOAT, false, 0, 0)
        }
        
        // 인덱스 데이터 업로드
        val indexBuffer = ByteBuffer.allocateDirect(sphereData.indices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asIntBuffer()
        indexBuffer.put(sphereData.indices.toIntArray())
        indexBuffer.position(0)
        
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, tempIndexVbo[0])
        GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, indexBuffer.capacity() * 4, indexBuffer, GLES30.GL_STATIC_DRAW)
        
        // 렌더링
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, sphereData.indices.size, GLES30.GL_UNSIGNED_INT, 0)
        
        // 정리
        GLES30.glDisableVertexAttribArray(aPositionHandle)
        GLES30.glDisableVertexAttribArray(aColorHandle)
        if (aNormalHandle >= 0) {
            GLES30.glDisableVertexAttribArray(aNormalHandle)
        }
        GLES30.glDeleteBuffers(1, tempVertexVbo, 0)
        GLES30.glDeleteBuffers(1, tempColorVbo, 0)
        GLES30.glDeleteBuffers(1, tempNormalVbo, 0)
        GLES30.glDeleteBuffers(1, tempIndexVbo, 0)
    }
    
    /**
     * 작은 구체들을 렌더링 (Ligands와 Pockets용)
     */
    private fun renderSmallSpheres(ligandAtoms: List<Atom>, pocketAtoms: List<Atom>) {
        // Ligands 렌더링 (각 원자의 고유한 색상 사용)
        if (ligandAtoms.isNotEmpty()) {
            renderSmallSpheresForAtomsWithHighlight(ligandAtoms, 1.0f, 10.0f) // 1.0 alpha, 10.0 크기
        }
        
        // Pockets 렌더링 (각 원자의 고유한 색상 사용)
        if (pocketAtoms.isNotEmpty()) {
            renderSmallSpheresForAtomsWithHighlight(pocketAtoms, 1.0f, 10.0f) // 1.0 alpha, 10.0 크기
        }
    }
    
    /**
     * 특정 원자들에 대해 작은 구체들을 렌더링 (highlight/focus 효과 적용)
     */
    private fun renderSmallSpheresForAtomsWithHighlight(atoms: List<Atom>, alpha: Float, radius: Float) {
        Log.d(TAG, "renderSmallSpheresForAtomsWithHighlight: ${atoms.size} atoms, alpha=$alpha, radius=$radius")
        if (atoms.isNotEmpty()) {
            atoms.forEachIndexed { index, atom ->
                if (index < 3) { // 처음 3개만 로그 출력
                    Log.d(TAG, "Rendering atom $index: element=${atom.element}, position=(${atom.position.x}, ${atom.position.y}, ${atom.position.z})")
                }
                val x = atom.position.x - structureCenterX
                val y = atom.position.y - structureCenterY
                val z = atom.position.z - structureCenterZ
                
                // Highlight/Focus 효과 적용
                val hasAnyHighlight = currentHighlightedChains.isNotEmpty() || currentFocusedElement != null
                val isHighlighted = when {
                    // Chain 기반 highlight
                    currentHighlightedChains.contains(atom.chain) -> true
                    currentFocusedElement == atom.chain -> true
                    // Residue 기반 focus
                    currentFocusedElement == atom.residueNumber.toString() -> true
                    // Ligand 특별 처리
                    atom.isLigand && currentFocusedElement?.startsWith("ligand:") == true -> true
                    atom.isLigand && currentHighlightedChains.any { it.startsWith("ligand:") } -> true
                    // Pocket 특별 처리
                    atom.isPocket && currentFocusedElement?.startsWith("pocket:") == true -> true
                    atom.isPocket && currentHighlightedChains.any { it.startsWith("pocket:") } -> true
                    else -> false
                }
                
                // 원자의 고유한 색상 가져오기
                val (baseR, baseG, baseB) = atom.atomicColor
                if (index < 3) { // 처음 3개만 로그 출력
                    Log.d(TAG, "Atom $index color: baseR=$baseR, baseG=$baseG, baseB=$baseB")
                }
                
                var colorTriplet = Triple(baseR, baseG, baseB)
                var finalAlpha = alpha

                if (hasAnyHighlight) {
                    if (isHighlighted) {
                        colorTriplet = Triple(
                            (baseR * 1.4f).coerceAtMost(1.0f),
                            (baseG * 1.4f).coerceAtMost(1.0f),
                            (baseB * 1.4f).coerceAtMost(1.0f)
                        )
                    } else {
                        colorTriplet = Triple(baseR * 0.15f, baseG * 0.15f, baseB * 0.15f)
                        finalAlpha = alpha * 0.15f
                    }
                }

                if (isInfoMode && !hasAnyHighlight && !isHighlighted) {
                    val adjusted = adjustInfoModeBaseColor(listOf(colorTriplet.first, colorTriplet.second, colorTriplet.third))
                    colorTriplet = Triple(adjusted[0], adjusted[1], adjusted[2])
                }

                val finalR = colorTriplet.first
                val finalG = colorTriplet.second
                val finalB = colorTriplet.third


                
                // 더 많은 점들로 구체 생성 (크기 적용)
                val spherePoints = mutableListOf<FloatArray>()
                
                // 중심점
                spherePoints.add(floatArrayOf(x, y, z))
                
                // X축 방향
                spherePoints.add(floatArrayOf(x + radius, y, z))
                spherePoints.add(floatArrayOf(x - radius, y, z))
                
                // Y축 방향
                spherePoints.add(floatArrayOf(x, y + radius, z))
                spherePoints.add(floatArrayOf(x, y - radius, z))
                
                // Z축 방향
                spherePoints.add(floatArrayOf(x, y, z + radius))
                spherePoints.add(floatArrayOf(x, y, z - radius))
                
                // 대각선 방향들 (더 많은 점들)
                val diagonal = radius * 0.7f
                spherePoints.add(floatArrayOf(x + diagonal, y + diagonal, z))
                spherePoints.add(floatArrayOf(x - diagonal, y - diagonal, z))
                spherePoints.add(floatArrayOf(x + diagonal, y - diagonal, z))
                spherePoints.add(floatArrayOf(x - diagonal, y + diagonal, z))
                spherePoints.add(floatArrayOf(x + diagonal, y, z + diagonal))
                spherePoints.add(floatArrayOf(x - diagonal, y, z - diagonal))
                spherePoints.add(floatArrayOf(x, y + diagonal, z + diagonal))
                spherePoints.add(floatArrayOf(x, y - diagonal, z - diagonal))
                
                // 추가 점들로 더 조밀하게
                val small = radius * 0.5f
                spherePoints.add(floatArrayOf(x + small, y + small, z + small))
                spherePoints.add(floatArrayOf(x - small, y - small, z - small))
                spherePoints.add(floatArrayOf(x + small, y - small, z + small))
                spherePoints.add(floatArrayOf(x - small, y + small, z - small))
                
                val tempVbo = IntArray(1)
                GLES30.glGenBuffers(1, tempVbo, 0)
                
                val vertexBuffer = ByteBuffer.allocateDirect(spherePoints.size * 3 * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer()
                
                val colorBuffer = ByteBuffer.allocateDirect(spherePoints.size * 3 * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer()
                
                spherePoints.forEach { point ->
                    vertexBuffer.put(point)
                    colorBuffer.put(finalR * finalAlpha)
                    colorBuffer.put(finalG * finalAlpha)
                    colorBuffer.put(finalB * finalAlpha)
                }
                
                vertexBuffer.position(0)
                colorBuffer.position(0)
                
                GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, tempVbo[0])
                GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vertexBuffer.capacity() * 4, vertexBuffer, GLES30.GL_STATIC_DRAW)
                GLES30.glEnableVertexAttribArray(aPositionHandle)
                GLES30.glVertexAttribPointer(aPositionHandle, 3, GLES30.GL_FLOAT, false, 0, 0)
                
                val tempColorVbo = IntArray(1)
                GLES30.glGenBuffers(1, tempColorVbo, 0)
                GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, tempColorVbo[0])
                GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, colorBuffer.capacity() * 4, colorBuffer, GLES30.GL_STATIC_DRAW)
                GLES30.glEnableVertexAttribArray(aColorHandle)
                GLES30.glVertexAttribPointer(aColorHandle, 3, GLES30.GL_FLOAT, false, 0, 0)
                
                GLES30.glDrawArrays(GLES30.GL_POINTS, 0, spherePoints.size)
                
                GLES30.glDisableVertexAttribArray(aPositionHandle)
                GLES30.glDisableVertexAttribArray(aColorHandle)
                GLES30.glDeleteBuffers(1, tempVbo, 0)
                GLES30.glDeleteBuffers(1, tempColorVbo, 0)
            }
        }
    }
    
    /**
     * 특정 원자들에 대해 작은 구체들을 렌더링 (기존 함수 - 호환성 유지)
     */
    private fun renderSmallSpheresForAtoms(atoms: List<Atom>, r: Float, g: Float, b: Float, alpha: Float, radius: Float) {
        // 간단한 구체 메쉬 생성 (8면체)
        val sphereVertices = mutableListOf<Float>()
        val sphereColors = mutableListOf<Float>()
        val sphereIndices = mutableListOf<Int>()
        
        atoms.forEach { atom ->
            val x = atom.position.x - structureCenterX
            val y = atom.position.y - structureCenterY
            val z = atom.position.z - structureCenterZ
            
            // 8면체 구체 생성 (간단한 형태)
            val spherePoints = listOf(
                floatArrayOf(x + radius, y, z), floatArrayOf(x - radius, y, z),
                floatArrayOf(x, y + radius, z), floatArrayOf(x, y - radius, z),
                floatArrayOf(x, y, z + radius), floatArrayOf(x, y, z - radius)
            )
            
            spherePoints.forEach { point ->
                sphereVertices.addAll(point.toList())
                sphereColors.addAll(listOf(r * alpha, g * alpha, b * alpha))
            }
        }
        
        // 작은 구체로 렌더링 (더 확실한 표시)
        if (sphereVertices.isNotEmpty()) {
            // 각 원자마다 작은 구체 렌더링
            atoms.forEach { atom ->
                val x = atom.position.x - structureCenterX
                val y = atom.position.y - structureCenterY
                val z = atom.position.z - structureCenterZ
                
                // 더 많은 점들로 구체 생성 (크기 적용)
                val spherePoints = mutableListOf<FloatArray>()
                
                // 중심점
                spherePoints.add(floatArrayOf(x, y, z))
                
                // X축 방향
                spherePoints.add(floatArrayOf(x + radius, y, z))
                spherePoints.add(floatArrayOf(x - radius, y, z))
                
                // Y축 방향
                spherePoints.add(floatArrayOf(x, y + radius, z))
                spherePoints.add(floatArrayOf(x, y - radius, z))
                
                // Z축 방향
                spherePoints.add(floatArrayOf(x, y, z + radius))
                spherePoints.add(floatArrayOf(x, y, z - radius))
                
                // 대각선 방향들 (더 많은 점들)
                val diagonal = radius * 0.7f
                spherePoints.add(floatArrayOf(x + diagonal, y + diagonal, z))
                spherePoints.add(floatArrayOf(x - diagonal, y - diagonal, z))
                spherePoints.add(floatArrayOf(x + diagonal, y - diagonal, z))
                spherePoints.add(floatArrayOf(x - diagonal, y + diagonal, z))
                spherePoints.add(floatArrayOf(x + diagonal, y, z + diagonal))
                spherePoints.add(floatArrayOf(x - diagonal, y, z - diagonal))
                spherePoints.add(floatArrayOf(x, y + diagonal, z + diagonal))
                spherePoints.add(floatArrayOf(x, y - diagonal, z - diagonal))
                
                // 추가 점들로 더 조밀하게
                val small = radius * 0.5f
                spherePoints.add(floatArrayOf(x + small, y + small, z + small))
                spherePoints.add(floatArrayOf(x - small, y - small, z - small))
                spherePoints.add(floatArrayOf(x + small, y - small, z + small))
                spherePoints.add(floatArrayOf(x - small, y + small, z - small))
                
                val tempVbo = IntArray(1)
                GLES30.glGenBuffers(1, tempVbo, 0)
                
                val vertexBuffer = ByteBuffer.allocateDirect(spherePoints.size * 3 * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer()
                
                val colorBuffer = ByteBuffer.allocateDirect(spherePoints.size * 3 * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer()
                
                spherePoints.forEach { point ->
                    vertexBuffer.put(point)
                    colorBuffer.put(r * alpha)
                    colorBuffer.put(g * alpha)
                    colorBuffer.put(b * alpha)
                }
                
                vertexBuffer.position(0)
                colorBuffer.position(0)
                
                GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, tempVbo[0])
                GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vertexBuffer.capacity() * 4, vertexBuffer, GLES30.GL_STATIC_DRAW)
                GLES30.glEnableVertexAttribArray(aPositionHandle)
                GLES30.glVertexAttribPointer(aPositionHandle, 3, GLES30.GL_FLOAT, false, 0, 0)
                
                val tempColorVbo = IntArray(1)
                GLES30.glGenBuffers(1, tempColorVbo, 0)
                GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, tempColorVbo[0])
                GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, colorBuffer.capacity() * 4, colorBuffer, GLES30.GL_STATIC_DRAW)
                GLES30.glEnableVertexAttribArray(aColorHandle)
                GLES30.glVertexAttribPointer(aColorHandle, 3, GLES30.GL_FLOAT, false, 0, 0)
                
                // 점 렌더링
                GLES30.glDrawArrays(GLES30.GL_POINTS, 0, spherePoints.size)
                
                // 정리
                GLES30.glDisableVertexAttribArray(aPositionHandle)
                GLES30.glDisableVertexAttribArray(aColorHandle)
                GLES30.glDeleteBuffers(1, tempVbo, 0)
                GLES30.glDeleteBuffers(1, tempColorVbo, 0)
            }
        }
    }
    
    
}
