package com.avas.proteinviewer.rendering

import android.content.Context
import android.view.Choreographer
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.avas.proteinviewer.data.model.FilamentStructure
import com.google.android.filament.*
import com.google.android.filament.utils.Utils
import com.google.android.filament.android.UiHelper
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.*

/**
 * 기본 Filament 3D 렌더러 - 정말 간단한 구현
 */
class BasicFilamentView(context: Context) : SurfaceView(context), SurfaceHolder.Callback {

    private lateinit var engine: Engine
    private lateinit var renderer: Renderer
    private lateinit var scene: Scene
    private lateinit var view: View
    private var swapChain: SwapChain? = null
    private var camera: Camera? = null
    // Delay EntityManager native access until after JNI libraries are loaded
    private val em by lazy { EntityManager.get() }
    private lateinit var uiHelper: UiHelper

    private val light = em.create()
    private var unlit: Material? = null
    private var structure: FilamentStructure? = null
    private val atomEntities = mutableListOf<Int>()

    private val scaleDetector = ScaleGestureDetector(context, ScaleListener())
    private val gestureDetector = GestureDetector(context, GestureListener())

    private var cameraTarget = FloatArray(3)
    private var cameraDistance = 50f
    private var azimuth = Math.toRadians(35.0)
    private var elevation = Math.toRadians(30.0)
    private var minDistance = 5f
    private var maxDistance = 500f
    private var currentPointerCount = 0
    private var isPanning = false

    init {
        // Ensure JNI libs are loaded before any native access
        Utils.init()
        Filament.init()
        holder.addCallback(this)
        isFocusable = true
        isClickable = true
        
        uiHelper = UiHelper(UiHelper.ContextErrorPolicy.DONT_CHECK).apply {
            renderCallback = SurfaceCallback()
        }
    }

    private inner class SurfaceCallback : UiHelper.RendererCallback {
        override fun onNativeWindowChanged(surface: android.view.Surface) {
            if (::engine.isInitialized) {
                swapChain = engine.createSwapChain(surface)
            }
        }

        override fun onDetachedFromSurface() {
            if (::engine.isInitialized) {
                swapChain?.let { engine.destroySwapChain(it) }
                swapChain = null
            }
        }

        override fun onResized(width: Int, height: Int) {
            if (::view.isInitialized) {
                view.viewport = Viewport(0, 0, width, height)
                camera?.setProjection(45.0, width.toDouble() / height, 0.1, 5000.0, Camera.Fov.VERTICAL)
            }
        }
    }

    override fun surfaceCreated(h: SurfaceHolder) {
        try {
            engine = Engine.create()
            renderer = engine.createRenderer()
            scene = engine.createScene()
            view = engine.createView().apply {
                this.scene = scene
            }

            camera = engine.createCamera(em.create()).also {
                it!!.setExposure(16.0f, 1.0f/125.0f, 100.0f)
                view.camera = it
                it.setProjection(45.0, 1.0, 0.1, 5000.0, Camera.Fov.VERTICAL)
            }

            // 간단한 라이트
            LightManager.Builder(LightManager.Type.DIRECTIONAL)
                .color(1.0f, 1.0f, 1.0f)
                .intensity(35_000.0f)
                .direction(0.5f, -1.0f, 0.3f)
                .castShadows(false)
                .build(engine, light)
            scene.addEntity(light)

            // 머티리얼 로드
            unlit = readUnlitMaterial("materials/unlit.filamat")

            uiHelper.attachTo(this)
            Choreographer.getInstance().postFrameCallback(frameCb)
        } catch (e: Exception) {
            android.util.Log.e("BasicFilamentView", "Error in surfaceCreated", e)
        }
    }

    override fun surfaceChanged(h: SurfaceHolder, format: Int, width: Int, height: Int) {
        // UiHelper가 처리
    }

    override fun surfaceDestroyed(h: SurfaceHolder) {
        Choreographer.getInstance().removeFrameCallback(frameCb)
        uiHelper.detach()

        atomEntities.forEach { entity ->
            engine.destroyEntity(entity)
        }
        unlit?.let { engine.destroyMaterial(it) }
        scene.removeEntity(light)
        engine.destroyEntity(light)

        swapChain?.let { engine.destroySwapChain(it) }
        camera?.let { engine.destroyCameraComponent(it.entity) }
        engine.destroyRenderer(renderer)
        engine.destroyView(view)
        engine.destroyScene(scene)
        engine.destroy()
    }

    private val frameCb = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            swapChain?.let { renderer.render(view) }
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        currentPointerCount = event.pointerCount
        scaleDetector.onTouchEvent(event)
        isPanning = !scaleDetector.isInProgress && currentPointerCount >= 2
        gestureDetector.onTouchEvent(event)
        if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
            currentPointerCount = 0
            isPanning = false
        }
        return true
    }

    fun loadStructure(s: FilamentStructure) {
        structure = s
        renderAtoms()
    }

    private fun renderAtoms() {
        val s = structure ?: return
        clearCurrentRenderable()

        try {
            val bounds = calculateBounds(s)
            val centerX = (bounds.minX + bounds.maxX) * 0.5f
            val centerY = (bounds.minY + bounds.maxY) * 0.5f
            val centerZ = (bounds.minZ + bounds.maxZ) * 0.5f
            cameraTarget[0] = 0f
            cameraTarget[1] = 0f
            cameraTarget[2] = 0f
            val spanX = bounds.maxX - bounds.minX
            val spanY = bounds.maxY - bounds.minY
            val spanZ = bounds.maxZ - bounds.minZ
            val maxExtent = maxOf(spanX, spanY, spanZ)
            val radius = (maxExtent * 0.5f).coerceAtLeast(1f)
            val verticalFovRad = Math.toRadians(45.0) / 2.0
            val distanceForFit = (radius / tan(verticalFovRad)).toFloat()
            cameraDistance = max(distanceForFit * 1.2f, radius * 1.5f)
            cameraDistance = max(cameraDistance, 6f)
            minDistance = max(radius * 0.4f, 1f)
            maxDistance = max(radius * 25f, cameraDistance * 3f)

            s.atoms.forEach { atom ->
                val entity = em.create()
                
                // 간단한 구 메쉬
                val sphereMesh = createSimpleSphere(0.3f, 8)
                
                // 머티리얼 인스턴스
                val materialInstance = unlit?.createInstance()?.apply {
                    val color = getElementColor(atom.element)
                    setParameter("baseColor", color[0], color[1], color[2], 1.0f)
                }
                
                if (materialInstance != null) {
                    // 렌더러블 컴포넌트
                    RenderableManager.Builder(1)
                        .geometry(0, RenderableManager.PrimitiveType.TRIANGLES, 
                            sphereMesh.vertexBuffer, sphereMesh.indexBuffer)
                        .material(0, materialInstance)
                        .castShadows(false)
                        .receiveShadows(false)
                        .build(engine, entity)

                    // 위치 설정
                    val tm = engine.transformManager
                    tm.create(entity)
                    val transform = FloatArray(16)
                    android.opengl.Matrix.setIdentityM(transform, 0)
                    android.opengl.Matrix.translateM(
                        transform,
                        0,
                        atom.x - centerX,
                        atom.y - centerY,
                        atom.z - centerZ
                    )
                    tm.setTransform(tm.getInstance(entity), transform)

                    scene.addEntity(entity)
                    atomEntities += entity
                }
            }
            updateCamera()
        } catch (e: Exception) {
            android.util.Log.e("BasicFilamentView", "Error in renderAtoms", e)
        }
    }

    private fun calculateBounds(structure: FilamentStructure): Bounds {
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var minZ = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        var maxZ = -Float.MAX_VALUE

        structure.atoms.forEach { atom ->
            if (atom.x < minX) minX = atom.x
            if (atom.y < minY) minY = atom.y
            if (atom.z < minZ) minZ = atom.z
            if (atom.x > maxX) maxX = atom.x
            if (atom.y > maxY) maxY = atom.y
            if (atom.z > maxZ) maxZ = atom.z
        }

        return Bounds(minX, minY, minZ, maxX, maxY, maxZ)
    }

    private fun updateCamera() {
        val cam = camera ?: return
        val distance = cameraDistance.coerceIn(minDistance, maxDistance)
        val cosElevation = cos(elevation).toFloat()
        val sinElevation = sin(elevation).toFloat()
        val cosAzimuth = cos(azimuth).toFloat()
        val sinAzimuth = sin(azimuth).toFloat()

        val eyeX = cameraTarget[0] + distance * cosElevation * sinAzimuth
        val eyeY = cameraTarget[1] + distance * sinElevation
        val eyeZ = cameraTarget[2] + distance * cosElevation * cosAzimuth

        cam.lookAt(
            eyeX.toDouble(), eyeY.toDouble(), eyeZ.toDouble(),
            cameraTarget[0].toDouble(), cameraTarget[1].toDouble(), cameraTarget[2].toDouble(),
            0.0, 1.0, 0.0
        )
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            return true
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            cameraDistance /= detector.scaleFactor
            updateCamera()
            return true
        }
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        private var lastSpanX = 0f
        private var lastSpanY = 0f

        override fun onDown(e: MotionEvent): Boolean {
            lastSpanX = 0f
            lastSpanY = 0f
            return true
        }

        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            if (isPanning) {
                panCamera(-distanceX, distanceY)
            } else {
                orbitCamera(distanceX, distanceY)
            }
            updateCamera()
            return true
        }
    }

    private fun orbitCamera(deltaX: Float, deltaY: Float) {
        val sensitivity = 0.005
        azimuth -= deltaX * sensitivity
        elevation += deltaY * sensitivity
        val maxElevation = Math.toRadians(89.0)
        val minElevation = Math.toRadians(-89.0)
        if (elevation > maxElevation) elevation = maxElevation
        if (elevation < minElevation) elevation = minElevation
    }

    private fun panCamera(deltaX: Float, deltaY: Float) {
        val distance = cameraDistance.coerceIn(minDistance, maxDistance)
        val panScale = distance * 0.0015f

        val cosElevation = cos(elevation).toFloat()
        val sinElevation = sin(elevation).toFloat()
        val cosAzimuth = cos(azimuth).toFloat()
        val sinAzimuth = sin(azimuth).toFloat()

        val rightX = cosAzimuth
        val rightY = 0f
        val rightZ = -sinAzimuth

        val upX = -sinElevation * sinAzimuth
        val upY = cosElevation
        val upZ = -sinElevation * cosAzimuth

        cameraTarget[0] += (rightX * deltaX + upX * deltaY) * panScale
        cameraTarget[1] += (rightY * deltaX + upY * deltaY) * panScale
        cameraTarget[2] += (rightZ * deltaX + upZ * deltaY) * panScale
    }

    private fun createSimpleSphere(radius: Float, segments: Int): BasicSphereMesh {
        val verts = mutableListOf<Float>()
        val indices = mutableListOf<Short>()
        
        val stacks = segments
        val slices = segments * 2

        for (i in 0..stacks) {
            val v = i.toFloat() / stacks
            val phi = Math.PI * v
            val y = Math.cos(phi).toFloat() * radius
            val r = Math.sin(phi).toFloat() * radius
            for (j in 0..slices) {
                val u = j.toFloat() / slices
                val theta = 2.0 * Math.PI * u
                val x = (r * Math.cos(theta)).toFloat()
                val z = (r * Math.sin(theta)).toFloat()
                verts += listOf(x, y, z)
            }
        }

        for (i in 0 until stacks) {
            for (j in 0 until slices) {
                val a = i * (slices + 1) + j
                val b = a + slices + 1
                indices += a.toShort()
                indices += b.toShort()
                indices += (a + 1).toShort()
                indices += (a + 1).toShort()
                indices += b.toShort()
                indices += (b + 1).toShort()
            }
        }

        val vb = VertexBuffer.Builder()
            .bufferCount(1)
            .vertexCount(verts.size / 3)
            .attribute(VertexBuffer.VertexAttribute.POSITION, 0,
                VertexBuffer.AttributeType.FLOAT3, 0, 12)
            .build(engine)

        val vbBuf = ByteBuffer.allocateDirect(verts.size * 4).order(ByteOrder.nativeOrder())
        verts.forEach { vbBuf.putFloat(it) }
        vb.setBufferAt(engine, 0, vbBuf.flip())

        val ib = IndexBuffer.Builder()
            .indexCount(indices.size)
            .bufferType(IndexBuffer.Builder.IndexType.USHORT)
            .build(engine)

        val ibBuf = ByteBuffer.allocateDirect(indices.size * 2).order(ByteOrder.nativeOrder())
        indices.forEach { ibBuf.putShort(it) }
        ib.setBuffer(engine, ibBuf.flip())

        return BasicSphereMesh(vb, ib)
    }

    private fun getElementColor(element: String): FloatArray {
        return when (element.uppercase()) {
            "C" -> floatArrayOf(0.2f, 0.2f, 0.2f)   // Carbon - 검은색
            "N" -> floatArrayOf(0.2f, 0.2f, 1.0f)   // Nitrogen - 파란색
            "O" -> floatArrayOf(1.0f, 0.2f, 0.2f)   // Oxygen - 빨간색
            "S" -> floatArrayOf(1.0f, 1.0f, 0.2f)   // Sulfur - 노란색
            "P" -> floatArrayOf(1.0f, 0.5f, 0.0f)   // Phosphorus - 주황색
            "H" -> floatArrayOf(1.0f, 1.0f, 1.0f)   // Hydrogen - 흰색
            else -> floatArrayOf(0.8f, 0.0f, 0.8f)  // 기타 - 보라색
        }
    }

    private fun clearCurrentRenderable() {
        atomEntities.forEach { e ->
            scene.removeEntity(e)
            engine.destroyEntity(e)
        }
        atomEntities.clear()
    }

    private fun readUnlitMaterial(path: String): Material {
        val asset = context.assets.open(path)
        val bytes = asset.readBytes()
        asset.close()
        return Material.Builder().payload(ByteBuffer.wrap(bytes), bytes.size).build(engine)
    }
}

data class BasicSphereMesh(val vertexBuffer: VertexBuffer, val indexBuffer: IndexBuffer)

private data class Bounds(
    val minX: Float,
    val minY: Float,
    val minZ: Float,
    val maxX: Float,
    val maxY: Float,
    val maxZ: Float
)
