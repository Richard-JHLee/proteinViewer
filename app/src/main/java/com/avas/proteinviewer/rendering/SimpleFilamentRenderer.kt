package com.avas.proteinviewer.rendering

import android.content.Context
import android.graphics.Color
import android.view.Choreographer
import android.view.MotionEvent
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
 * ValtoLibraries 방식의 간단한 Filament 렌더러
 */
class SimpleFilamentRenderer(context: Context) : SurfaceView(context), SurfaceHolder.Callback {

    private lateinit var engine: Engine
    private lateinit var renderer: Renderer
    private lateinit var scene: Scene
    private lateinit var view: View
    private var swapChain: SwapChain? = null
    private var camera: Camera? = null
    // Delay EntityManager native access until after JNI libs are loaded
    private val em by lazy { EntityManager.get() }
    private lateinit var uiHelper: UiHelper

    private val light = em.create()
    private var unlit: Material? = null
    private var structure: FilamentStructure? = null
    private var ready: Boolean = false
    private var renderedOnce: Boolean = false
    private var sharedSphere: SphereMesh? = null
    private var totalAtoms: Int = 0
    private var loadedAtoms: Int = 0
    private val initialAtomCount: Int = 2500
    private val batchSize: Int = 1000
    private val atomEntities = mutableListOf<Int>()

    init {
        try {
            // Load required JNI libraries before any Filament API calls that touch native code
            Utils.init()
            Filament.init()
            holder.addCallback(this)
            isFocusable = true
            isClickable = true
            
            uiHelper = UiHelper(UiHelper.ContextErrorPolicy.DONT_CHECK).apply {
                renderCallback = SurfaceCallback()
            }
        } catch (e: Exception) {
            android.util.Log.e("SimpleFilamentRenderer", "Error in init", e)
        }
    }

    private inner class SurfaceCallback : UiHelper.RendererCallback {
        override fun onNativeWindowChanged(surface: android.view.Surface) {
            try {
                if (::engine.isInitialized) {
                    swapChain = engine.createSwapChain(surface)
                }
            } catch (e: Exception) {
                android.util.Log.e("SimpleFilamentRenderer", "Error in onNativeWindowChanged", e)
            }
        }

        override fun onDetachedFromSurface() {
            try {
                if (::engine.isInitialized) {
                    swapChain?.let { engine.destroySwapChain(it) }
                    swapChain = null
                }
            } catch (e: Exception) {
                android.util.Log.e("SimpleFilamentRenderer", "Error in onDetachedFromSurface", e)
            }
        }

        override fun onResized(width: Int, height: Int) {
            try {
                if (::view.isInitialized) {
                    view.viewport = Viewport(0, 0, width, height)
                    camera?.setProjection(45.0, width.toDouble() / height, 0.1, 5000.0, Camera.Fov.VERTICAL)
                }
            } catch (e: Exception) {
                android.util.Log.e("SimpleFilamentRenderer", "Error in onResized", e)
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
            ready = true
            renderedOnce = false
            
            android.util.Log.d("SimpleFilamentRenderer", "Surface created successfully")
        } catch (e: Exception) {
            android.util.Log.e("SimpleFilamentRenderer", "Error in surfaceCreated", e)
        }
    }

    override fun surfaceChanged(h: SurfaceHolder, format: Int, width: Int, height: Int) {
        // UiHelper가 처리
    }

    override fun surfaceDestroyed(h: SurfaceHolder) {
        try {
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
            
            android.util.Log.d("SimpleFilamentRenderer", "Surface destroyed")
        } catch (e: Exception) {
            android.util.Log.e("SimpleFilamentRenderer", "Error in surfaceDestroyed", e)
        }
        ready = false
        renderedOnce = false
    }

    private val frameCb = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            try {
                swapChain?.let { renderer.render(view) }
                // If structure arrived before ready, render once after ready
                if (ready && !renderedOnce && structure != null) {
                    renderInitialAtoms()
                    renderedOnce = true
                }
                // Progressive loading of remaining atoms
                if (ready && structure != null && loadedAtoms < totalAtoms) {
                    val next = minOf(loadedAtoms + batchSize, totalAtoms)
                    ensureSharedSphere()
                    addAtomsRange(loadedAtoms, next)
                    loadedAtoms = next
                    if (loadedAtoms % 1000 == 0 || loadedAtoms == totalAtoms) {
                        android.util.Log.d("SimpleFilamentRenderer", "Progressive load: $loadedAtoms/$totalAtoms")
                    }
                }
                Choreographer.getInstance().postFrameCallback(this)
            } catch (e: Exception) {
                android.util.Log.e("SimpleFilamentRenderer", "Error in frame callback", e)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return true
    }

    fun loadStructure(s: FilamentStructure) {
        try {
            // Reset previous
            clearCurrentRenderable()
            structure = s
            totalAtoms = s.atoms.size
            loadedAtoms = 0
            if (ready) {
                // Render initial chunk, then progressive batches
                renderInitialAtoms()
                renderedOnce = true
                android.util.Log.d("SimpleFilamentRenderer", "Structure loaded with ${s.atoms.size} atoms (initial ${minOf(initialAtomCount, totalAtoms)} rendered)")
            } else {
                android.util.Log.d("SimpleFilamentRenderer", "Structure loaded with ${s.atoms.size} atoms (deferred until surface ready)")
            }
        } catch (e: Exception) {
            android.util.Log.e("SimpleFilamentRenderer", "Error in loadStructure", e)
        }
    }

    private fun renderInitialAtoms() {
        val s = structure ?: return
        try {
            ensureSharedSphere()
            val end = minOf(initialAtomCount, s.atoms.size)
            addAtomsRange(0, end)
            loadedAtoms = end
            android.util.Log.d("SimpleFilamentRenderer", "Initial rendered atoms: $loadedAtoms/$totalAtoms")
        } catch (e: Exception) {
            android.util.Log.e("SimpleFilamentRenderer", "Error in renderInitialAtoms", e)
        }
    }

    private fun ensureSharedSphere() {
        if (sharedSphere == null) {
            sharedSphere = createSimpleSphere(0.5f, 12)
        }
    }

    private fun addAtomsRange(start: Int, end: Int) {
        val s = structure ?: return
        if (start >= end) return
        val sphereMesh = sharedSphere ?: return
        for (i in start until end) {
            val atom = s.atoms[i]
            val entity = em.create()
            val materialInstance = unlit?.createInstance()?.apply {
                val color = getElementColor(atom.element)
                setParameter("baseColor", color[0], color[1], color[2], 1.0f)
            }
            if (materialInstance != null) {
                RenderableManager.Builder(1)
                    .geometry(0, RenderableManager.PrimitiveType.TRIANGLES, sphereMesh.vertexBuffer, sphereMesh.indexBuffer)
                    .material(0, materialInstance)
                    .castShadows(false)
                    .receiveShadows(false)
                    .build(engine, entity)

                val tm = engine.transformManager
                tm.create(entity)
                val transform = FloatArray(16)
                android.opengl.Matrix.setIdentityM(transform, 0)
                android.opengl.Matrix.translateM(transform, 0, atom.x, atom.y, atom.z)
                tm.setTransform(tm.getInstance(entity), transform)

                scene.addEntity(entity)
                atomEntities += entity
            }
        }
    }

    private fun createSimpleSphere(radius: Float, segments: Int): SphereMesh {
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

        return SphereMesh(vb, ib)
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
        try {
            atomEntities.forEach { e ->
                scene.removeEntity(e)
                engine.destroyEntity(e)
            }
            atomEntities.clear()
        } catch (e: Exception) {
            android.util.Log.e("SimpleFilamentRenderer", "Error in clearCurrentRenderable", e)
        }
    }

    private fun readUnlitMaterial(path: String): Material? {
        return try {
            val asset = context.assets.open(path)
            val bytes = asset.readBytes()
            asset.close()
            Material.Builder().payload(ByteBuffer.wrap(bytes), bytes.size).build(engine)
        } catch (e: Exception) {
            android.util.Log.e("SimpleFilamentRenderer", "Error reading material", e)
            null
        }
    }
}

data class SphereMesh(val vertexBuffer: VertexBuffer, val indexBuffer: IndexBuffer)
