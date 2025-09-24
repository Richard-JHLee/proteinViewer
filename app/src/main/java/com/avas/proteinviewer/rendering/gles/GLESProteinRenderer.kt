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
import kotlin.collections.HashMap
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
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
    private var atomChains: List<String> = emptyList()
    private var atomPockets: List<String?> = emptyList()
    private var atomPositions: FloatArray = FloatArray(0)
    private var baseColors: FloatArray = FloatArray(0)
    private var highlightedChains: Set<String> = emptySet()
    private var focusedChain: String? = null
    private var highlightedPockets: Set<String> = emptySet()
    private var focusedPocket: String? = null

    private var bondProgram = 0
    private var bondMvpHandle = 0
    private var bondLightDirHandle = 0
    private var bondVao = 0
    private var bondVertexVbo = 0
    private var bondIndexBuffer = 0
    private var bondInstanceVbo = 0
    private var bondIndexCount = 0
    private var bondCount = 0
    private var bondAtomA = IntArray(0)
    private var bondAtomB = IntArray(0)
    private var bondTransforms: FloatArray = FloatArray(0)
    private var bondBaseColors: FloatArray = FloatArray(0)
    private var bondColors: FloatArray = FloatArray(0)
    private var bondInstanceBuffer: ByteBuffer? = null
    private val bondLightDir = floatArrayOf(0.5f, -1.0f, 0.3f)

    init {
        val len = sqrt(bondLightDir[0] * bondLightDir[0] + bondLightDir[1] * bondLightDir[1] + bondLightDir[2] * bondLightDir[2])
        if (len > 0f) {
            bondLightDir[0] /= len
            bondLightDir[1] /= len
            bondLightDir[2] /= len
        }
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        Log.d(TAG, "onSurfaceCreated")
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glEnable(0x8642)  // GL_PROGRAM_POINT_SIZE constant
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)
        GLES30.glClearColor(1f, 1f, 1f, 1f)

        try {
        program = createProgram(VERT_SHADER, FRAG_SHADER)
            bondProgram = createProgram(BOND_VERT_SHADER, BOND_FRAG_SHADER)
        } catch (ex: RuntimeException) {
            Log.e(TAG, "Failed to create shader program", ex)
            program = 0
            bondProgram = 0
            buffersReady = false
            return
        }

        aPositionHandle = GLES30.glGetAttribLocation(program, "aPosition")
        aColorHandle = GLES30.glGetAttribLocation(program, "aColor")
        uMvpHandle = GLES30.glGetUniformLocation(program, "uMvp")
        uPointSizeHandle = GLES30.glGetUniformLocation(program, "uPointSize")

        bondMvpHandle = GLES30.glGetUniformLocation(bondProgram, "uMvp")
        bondLightDirHandle = GLES30.glGetUniformLocation(bondProgram, "uLightDir")

        val buffers = IntArray(2)
        GLES30.glGenBuffers(2, buffers, 0)
        atomVbo = buffers[0]
        colorVbo = buffers[1]

        val vaos = IntArray(1)
        GLES30.glGenVertexArrays(1, vaos, 0)
        vao = vaos[0]

        setupBondBuffers()

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

        renderBonds()
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
            atomChains = emptyList()
            atomPockets = emptyList()
            atomPositions = FloatArray(0)
            baseColors = FloatArray(0)
            bondCount = 0
            bondAtomA = IntArray(0)
            bondAtomB = IntArray(0)
            bondTransforms = FloatArray(0)
            bondBaseColors = FloatArray(0)
            bondColors = FloatArray(0)
            bondInstanceBuffer = null
            uploadBondInstances()
            applySelectionState()
            return
        }

        val atoms = structure.atoms
        atomCount = atoms.size
        atomChains = atoms.map { it.chain }
        val atomElements = atoms.map { it.element }
        atomPockets = List(atomCount) { index ->
            atoms[index].takeIf { it.isPocket }?.residueName
        }
        val availablePockets = atomPockets.filterNotNull().toSet()
        highlightedPockets = highlightedPockets.intersect(availablePockets)
        if (focusedPocket != null && !availablePockets.contains(focusedPocket)) {
            focusedPocket = null
        }
        atomPositions = FloatArray(atomCount * 3)
        baseColors = FloatArray(atomCount * 3)

        atomElements.forEachIndexed { index, element ->
            val color = ColorMaps.cpk(element)
            baseColors[index * 3] = ((color shr 16) and 0xFF) / 255f
            baseColors[index * 3 + 1] = ((color shr 8) and 0xFF) / 255f
            baseColors[index * 3 + 2] = (color and 0xFF) / 255f
        }

        val idToIndex = HashMap<Int, Int>(atomCount)
        atoms.forEachIndexed { index, atom -> idToIndex[atom.id] = index }
        val bondList = structure.bonds.mapNotNull { bond ->
            val a = idToIndex[bond.atomA] ?: return@mapNotNull null
            val b = idToIndex[bond.atomB] ?: return@mapNotNull null
            if (a == b) null else intArrayOf(a, b)
        }
        bondCount = bondList.size
        bondAtomA = IntArray(bondCount)
        bondAtomB = IntArray(bondCount)
        for (i in 0 until bondCount) {
            val pair = bondList[i]
            bondAtomA[i] = pair[0]
            bondAtomB[i] = pair[1]
        }
        bondTransforms = FloatArray(bondCount * 16)
        bondBaseColors = FloatArray(bondCount * 3)
        bondColors = FloatArray(bondCount * 3)

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

        for (i in 0 until atomCount) {
            val atom = atoms[i]
            val px = atom.position.x - centerX
            val py = atom.position.y - centerY
            val pz = atom.position.z - centerZ

            positionBuffer.put(px)
            positionBuffer.put(py)
            positionBuffer.put(pz)

            val baseIndex = i * 3
            atomPositions[baseIndex] = px
            atomPositions[baseIndex + 1] = py
            atomPositions[baseIndex + 2] = pz

            val r = baseColors[baseIndex]
            val g = baseColors[baseIndex + 1]
            val b = baseColors[baseIndex + 2]
            colorBuffer.put(r)
            colorBuffer.put(g)
            colorBuffer.put(b)
        }
        recomputeBondTransforms()
        positionBuffer.flip()
        colorBuffer.flip()

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, atomVbo)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, positionBuffer.capacity() * 4, positionBuffer, GLES30.GL_STATIC_DRAW)

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, colorVbo)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, colorBuffer.capacity() * 4, colorBuffer, GLES30.GL_STATIC_DRAW)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)

        pointSize = clamp(6f, 36f, 500f / max(1f, boundingRadius))
        Log.d(TAG, "Uploaded ${atoms.size} atoms, pointSize=$pointSize")
        
        // Apply current highlight state to newly uploaded atoms
        applySelectionState()
    }

    fun setHighlights(highlighted: Set<String>, focused: String?) {
        highlightedChains = highlighted
        focusedChain = focused
        Log.d(TAG, "setHighlights: highlighted=$highlighted, focused=$focused")
        applySelectionState()
        if (focused != null) {
            camera.focusOnTarget(0f, 0f, 0f, 15f)
        }
    }

    fun setPocketHighlights(highlighted: Set<String>, focused: String?) {
        highlightedPockets = highlighted
        focusedPocket = focused
        Log.d(TAG, "setPocketHighlights: highlighted=$highlighted, focused=$focused")
        applySelectionState()
        focused?.let { focusOnPocket(it) }
    }

    private fun applySelectionState() {
        if (!buffersReady || atomCount == 0 || baseColors.isEmpty()) return

        val colorArray = FloatArray(baseColors.size)
        var index = 0
        var highlightedCount = 0
        val hasChainSelection = highlightedChains.isNotEmpty() || focusedChain != null
        val hasPocketSelection = highlightedPockets.isNotEmpty() || focusedPocket != null
        val hasSelection = hasChainSelection || hasPocketSelection

        for (i in 0 until atomCount) {
            val baseR = baseColors[index]
            val baseG = baseColors[index + 1]
            val baseB = baseColors[index + 2]
            val chain = atomChains.getOrNull(i)
            val pocket = atomPockets.getOrNull(i)

            val chainFocused = focusedChain != null && focusedChain == chain
            val chainHighlighted = highlightedChains.contains(chain)
            val pocketFocused = focusedPocket != null && focusedPocket == pocket
            val pocketHighlighted = pocket != null && highlightedPockets.contains(pocket)

            val isFocused = chainFocused || pocketFocused
            val isHighlighted = !isFocused && (chainHighlighted || pocketHighlighted)

            if (isFocused || isHighlighted) {
                highlightedCount++
            }

            val adjusted = when {
                isFocused -> brightenColor(baseR, baseG, baseB, FOCUSED_HIGHLIGHT_FACTOR, HIGHLIGHT_LIFT)
                isHighlighted -> brightenColor(baseR, baseG, baseB, HIGHLIGHT_FACTOR, HIGHLIGHT_LIFT)
                hasSelection -> dimColor(baseR, baseG, baseB, NON_HIGHLIGHT_DIM_FACTOR)
                else -> dimColor(baseR, baseG, baseB, DEFAULT_DIM_FACTOR)
            }

            colorArray[index] = adjusted[0]
            colorArray[index + 1] = adjusted[1]
            colorArray[index + 2] = adjusted[2]
            index += 3
        }

        val buffer = ByteBuffer.allocateDirect(colorArray.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        buffer.put(colorArray)
        buffer.flip()

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, colorVbo)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, colorArray.size * 4, buffer, GLES30.GL_STATIC_DRAW)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)

        Log.d(TAG, "Color update: highlightedCount=$highlightedCount")

        updateBondColors(hasSelection)
        uploadBondInstances()
    }

    private fun focusOnPocket(pocketId: String) {
        if (atomPositions.isEmpty() || atomPockets.isEmpty()) return

        var count = 0
        var sumX = 0f
        var sumY = 0f
        var sumZ = 0f

        for (i in 0 until atomCount) {
            if (atomPockets.getOrNull(i) == pocketId) {
                val baseIndex = i * 3
                val x = atomPositions[baseIndex]
                val y = atomPositions[baseIndex + 1]
                val z = atomPositions[baseIndex + 2]
                sumX += x
                sumY += y
                sumZ += z
                count++
            }
        }

        if (count == 0) {
            Log.d(TAG, "focusOnPocket: no atoms found for $pocketId")
            return
        }

        val centerX = sumX / count
        val centerY = sumY / count
        val centerZ = sumZ / count

        var maxRadius = 0f
        for (i in 0 until atomCount) {
            if (atomPockets.getOrNull(i) == pocketId) {
                val baseIndex = i * 3
                val dx = atomPositions[baseIndex] - centerX
                val dy = atomPositions[baseIndex + 1] - centerY
                val dz = atomPositions[baseIndex + 2] - centerZ
                val dist = sqrt(dx * dx + dy * dy + dz * dz)
                if (dist > maxRadius) {
                    maxRadius = dist
                }
            }
        }

        camera.focusOnTarget(centerX, centerY, centerZ, maxRadius.coerceAtLeast(5f))
    }

    private fun setupBondBuffers() {
        destroyBondResources()
        val segments = 24
        val vertexStride = 6
        val vertexCount = (segments + 1) * 2
        val vertexData = FloatArray(vertexCount * vertexStride)
        var vi = 0
        for (i in 0..segments) {
            val theta = (i.toFloat() / segments.toFloat()) * (Math.PI.toFloat() * 2f)
            val cx = kotlin.math.cos(theta)
            val sx = kotlin.math.sin(theta)
            // bottom vertex
            vertexData[vi++] = cx
            vertexData[vi++] = -0.5f
            vertexData[vi++] = sx
            vertexData[vi++] = cx
            vertexData[vi++] = 0f
            vertexData[vi++] = sx
            // top vertex
            vertexData[vi++] = cx
            vertexData[vi++] = 0.5f
            vertexData[vi++] = sx
            vertexData[vi++] = cx
            vertexData[vi++] = 0f
            vertexData[vi++] = sx
        }

        val indexData = ShortArray(segments * 6)
        var ii = 0
        for (i in 0 until segments) {
            val bottom0 = (i * 2).toShort()
            val top0 = (bottom0 + 1).toShort()
            val bottom1 = (((i + 1) % (segments + 1)) * 2).toShort()
            val top1 = (bottom1 + 1).toShort()

            indexData[ii++] = bottom0
            indexData[ii++] = bottom1
            indexData[ii++] = top0

            indexData[ii++] = top0
            indexData[ii++] = bottom1
            indexData[ii++] = top1
        }
        bondIndexCount = indexData.size

        val vertexBuffer = ByteBuffer.allocateDirect(vertexData.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        vertexBuffer.put(vertexData).flip()
        val indexBuffer = ByteBuffer.allocateDirect(indexData.size * 2).order(ByteOrder.nativeOrder()).asShortBuffer()
        indexBuffer.put(indexData).flip()

        val vaos = IntArray(1)
        GLES30.glGenVertexArrays(1, vaos, 0)
        bondVao = vaos[0]
        val buffers = IntArray(3)
        GLES30.glGenBuffers(3, buffers, 0)
        bondVertexVbo = buffers[0]
        bondIndexBuffer = buffers[1]
        bondInstanceVbo = buffers[2]

        GLES30.glBindVertexArray(bondVao)

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, bondVertexVbo)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vertexData.size * 4, vertexBuffer, GLES30.GL_STATIC_DRAW)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, vertexStride * 4, 0)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, vertexStride * 4, (3 * 4))

        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, bondIndexBuffer)
        GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, indexData.size * 2, indexBuffer, GLES30.GL_STATIC_DRAW)

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, bondInstanceVbo)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, 0, null, GLES30.GL_DYNAMIC_DRAW)
        val instanceStride = (16 + 3) * 4
        var offset = 0
        for (i in 0..3) {
            GLES30.glEnableVertexAttribArray(2 + i)
            GLES30.glVertexAttribPointer(2 + i, 4, GLES30.GL_FLOAT, false, instanceStride, offset)
            GLES30.glVertexAttribDivisor(2 + i, 1)
            offset += 16
        }
        GLES30.glEnableVertexAttribArray(6)
        GLES30.glVertexAttribPointer(6, 3, GLES30.GL_FLOAT, false, instanceStride, offset)
        GLES30.glVertexAttribDivisor(6, 1)

        GLES30.glBindVertexArray(0)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
    }

    private fun destroyBondResources() {
        if (bondInstanceVbo != 0) {
            GLES30.glDeleteBuffers(1, intArrayOf(bondInstanceVbo), 0)
            bondInstanceVbo = 0
        }
        if (bondVertexVbo != 0) {
            GLES30.glDeleteBuffers(1, intArrayOf(bondVertexVbo), 0)
            bondVertexVbo = 0
        }
        if (bondIndexBuffer != 0) {
            GLES30.glDeleteBuffers(1, intArrayOf(bondIndexBuffer), 0)
            bondIndexBuffer = 0
        }
        if (bondVao != 0) {
            GLES30.glDeleteVertexArrays(1, intArrayOf(bondVao), 0)
            bondVao = 0
        }
        bondInstanceBuffer = null
        bondIndexCount = 0
    }

    private fun recomputeBondTransforms() {
        if (bondCount == 0 || atomPositions.isEmpty()) return
        if (bondTransforms.size != bondCount * 16) {
            bondTransforms = FloatArray(bondCount * 16)
        }
        val radius = 0.12f
        for (i in 0 until bondCount) {
            val a = bondAtomA.getOrNull(i) ?: continue
            val b = bondAtomB.getOrNull(i) ?: continue
            val ax = atomPositions[a * 3]
            val ay = atomPositions[a * 3 + 1]
            val az = atomPositions[a * 3 + 2]
            val bx = atomPositions[b * 3]
            val by = atomPositions[b * 3 + 1]
            val bz = atomPositions[b * 3 + 2]
            var dirX = bx - ax
            var dirY = by - ay
            var dirZ = bz - az
            val length = sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ)
            if (length < 1e-4f) continue
            val invLen = 1f / length
            dirX *= invLen
            dirY *= invLen
            dirZ *= invLen

            var upX = 0f
            var upY = 1f
            var upZ = 0f
            if (kotlin.math.abs(dirX) < 1e-4f && kotlin.math.abs(dirZ) < 1e-4f) {
                upX = 1f
                upY = 0f
                upZ = 0f
            }
            var xAxisX = upY * dirZ - upZ * dirY
            var xAxisY = upZ * dirX - upX * dirZ
            var xAxisZ = upX * dirY - upY * dirX
            var xLen = sqrt(xAxisX * xAxisX + xAxisY * xAxisY + xAxisZ * xAxisZ)
            if (xLen < 1e-4f) {
                upX = 0f
                upY = 0f
                upZ = 1f
                xAxisX = upY * dirZ - upZ * dirY
                xAxisY = upZ * dirX - upX * dirZ
                xAxisZ = upX * dirY - upY * dirX
                xLen = sqrt(xAxisX * xAxisX + xAxisY * xAxisY + xAxisZ * xAxisZ)
            }
            xAxisX /= xLen
            xAxisY /= xLen
            xAxisZ /= xLen

            val yAxisX = dirX
            val yAxisY = dirY
            val yAxisZ = dirZ

            var zAxisX = yAxisY * xAxisZ - yAxisZ * xAxisY
            var zAxisY = yAxisZ * xAxisX - yAxisX * xAxisZ
            var zAxisZ = yAxisX * xAxisY - yAxisY * xAxisX

            val halfLength = length * 0.5f
            val offset = i * 16
            bondTransforms[offset] = xAxisX * radius
            bondTransforms[offset + 1] = xAxisY * radius
            bondTransforms[offset + 2] = xAxisZ * radius
            bondTransforms[offset + 3] = 0f

            bondTransforms[offset + 4] = yAxisX * halfLength
            bondTransforms[offset + 5] = yAxisY * halfLength
            bondTransforms[offset + 6] = yAxisZ * halfLength
            bondTransforms[offset + 7] = 0f

            var zLen = sqrt(zAxisX * zAxisX + zAxisY * zAxisY + zAxisZ * zAxisZ)
            if (zLen < 1e-4f) {
                zLen = 1f
            }
            val zScale = radius / zLen
            bondTransforms[offset + 8] = zAxisX * zScale
            bondTransforms[offset + 9] = zAxisY * zScale
            bondTransforms[offset + 10] = zAxisZ * zScale
            bondTransforms[offset + 11] = 0f

            bondTransforms[offset + 12] = (ax + bx) * 0.5f
            bondTransforms[offset + 13] = (ay + by) * 0.5f
            bondTransforms[offset + 14] = (az + bz) * 0.5f
            bondTransforms[offset + 15] = 1f
        }
    }

    private fun updateBondColors(hasSelection: Boolean) {
        if (bondCount == 0) return
        if (bondColors.size != bondCount * 3) {
            bondColors = FloatArray(bondCount * 3)
        }
        if (bondBaseColors.size != bondCount * 3) {
            bondBaseColors = FloatArray(bondCount * 3)
        }
        for (i in 0 until bondCount) {
            val a = bondAtomA.getOrNull(i) ?: continue
            val b = bondAtomB.getOrNull(i) ?: continue
            val baseIndex = i * 3
            val baseAIndex = a * 3
            val baseBIndex = b * 3
            val avgR = (baseColors[baseAIndex] + baseColors[baseBIndex]) * 0.5f
            val avgG = (baseColors[baseAIndex + 1] + baseColors[baseBIndex + 1]) * 0.5f
            val avgB = (baseColors[baseAIndex + 2] + baseColors[baseBIndex + 2]) * 0.5f
            bondBaseColors[baseIndex] = avgR
            bondBaseColors[baseIndex + 1] = avgG
            bondBaseColors[baseIndex + 2] = avgB

            val chainA = atomChains.getOrNull(a)
            val chainB = atomChains.getOrNull(b)
            val pocketA = atomPockets.getOrNull(a)
            val pocketB = atomPockets.getOrNull(b)

            val chainFocused = focusedChain != null && (focusedChain == chainA || focusedChain == chainB)
            val chainHighlighted = highlightedChains.contains(chainA) || highlightedChains.contains(chainB)
            val pocketFocused = focusedPocket != null && (focusedPocket == pocketA || focusedPocket == pocketB)
            val pocketHighlighted = (pocketA != null && highlightedPockets.contains(pocketA)) || (pocketB != null && highlightedPockets.contains(pocketB))

            val isFocused = chainFocused || pocketFocused
            val isHighlighted = !isFocused && (chainHighlighted || pocketHighlighted)

            val adjusted = when {
                isFocused -> brightenColor(avgR, avgG, avgB, FOCUSED_HIGHLIGHT_FACTOR, HIGHLIGHT_LIFT)
                isHighlighted -> brightenColor(avgR, avgG, avgB, HIGHLIGHT_FACTOR, HIGHLIGHT_LIFT)
                hasSelection -> dimColor(avgR, avgG, avgB, NON_HIGHLIGHT_DIM_FACTOR)
                else -> dimColor(avgR, avgG, avgB, DEFAULT_DIM_FACTOR)
            }

            bondColors[baseIndex] = adjusted[0]
            bondColors[baseIndex + 1] = adjusted[1]
            bondColors[baseIndex + 2] = adjusted[2]
        }
    }

    private fun uploadBondInstances() {
        if (bondCount == 0 || bondInstanceVbo == 0) return
        val floatsPerInstance = 16 + 3
        val requiredBytes = bondCount * floatsPerInstance * 4
        if (bondInstanceBuffer == null || bondInstanceBuffer!!.capacity() < requiredBytes) {
            bondInstanceBuffer = ByteBuffer.allocateDirect(requiredBytes).order(ByteOrder.nativeOrder())
        }
        val buffer = bondInstanceBuffer!!
        buffer.clear()
        for (i in 0 until bondCount) {
            val matrixOffset = i * 16
            for (j in 0 until 16) {
                buffer.putFloat(bondTransforms[matrixOffset + j])
            }
            val colorOffset = i * 3
            buffer.putFloat(bondColors[colorOffset])
            buffer.putFloat(bondColors[colorOffset + 1])
            buffer.putFloat(bondColors[colorOffset + 2])
        }
        buffer.flip()
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, bondInstanceVbo)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, buffer.capacity(), buffer, GLES30.GL_DYNAMIC_DRAW)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
    }

    private fun renderBonds() {
        if (bondCount == 0 || bondProgram == 0 || bondVao == 0) return
        GLES30.glUseProgram(bondProgram)
        GLES30.glUniformMatrix4fv(bondMvpHandle, 1, false, mvpMatrix, 0)
        GLES30.glUniform3fv(bondLightDirHandle, 1, bondLightDir, 0)
        GLES30.glBindVertexArray(bondVao)
        GLES30.glDrawElementsInstanced(GLES30.GL_TRIANGLES, bondIndexCount, GLES30.GL_UNSIGNED_SHORT, 0, bondCount)
        GLES30.glBindVertexArray(0)
    }


    companion object {
        private const val TAG = "GLESProteinRenderer"

        private const val DEFAULT_DIM_FACTOR = 0.35f
        private const val NON_HIGHLIGHT_DIM_FACTOR = 0.1f
        private const val HIGHLIGHT_FACTOR = 1.15f
        private const val FOCUSED_HIGHLIGHT_FACTOR = 1.25f
        private const val HIGHLIGHT_LIFT = 0.05f

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
    float r2 = dot(coord, coord);
    if (r2 > 1.0) {
        discard;
    }
    float dist = sqrt(r2);
    float diffuse = clamp(1.0 - dist * dist, 0.0, 1.0);
    vec3 base = vColor * (0.55 + 0.45 * diffuse);
    float rim = smoothstep(0.6, 1.0, dist);
    vec3 rimColor = vec3(1.0);
    vec3 color = mix(base, rimColor, rim * 0.35);
    float alpha = smoothstep(1.0, 0.65, dist);
    fragColor = vec4(color, alpha);
}"""

        private const val BOND_VERT_SHADER = """#version 300 es
layout (location = 0) in vec3 aPosition;
layout (location = 1) in vec3 aNormal;
layout (location = 2) in mat4 aInstanceMatrix;
layout (location = 6) in vec3 aInstanceColor;
uniform mat4 uMvp;
uniform vec3 uLightDir;
out vec3 vColor;
out float vDiffuse;
void main() {
    vec4 worldPos = aInstanceMatrix * vec4(aPosition, 1.0);
    gl_Position = uMvp * worldPos;
    vec3 normal = mat3(aInstanceMatrix) * aNormal;
    vDiffuse = max(dot(normalize(normal), normalize(-uLightDir)), 0.0);
    vColor = aInstanceColor;
}"""

        private const val BOND_FRAG_SHADER = """#version 300 es
precision mediump float;
in vec3 vColor;
in float vDiffuse;
out vec4 fragColor;
void main() {
    float ambient = 0.35;
    float lighting = ambient + (0.65 * vDiffuse);
    fragColor = vec4(vColor * lighting, 1.0);
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

        private fun brightenColor(r: Float, g: Float, b: Float, factor: Float, lift: Float): FloatArray {
            return floatArrayOf(
                clamp(0f, 1f, r * factor + lift),
                clamp(0f, 1f, g * factor + lift),
                clamp(0f, 1f, b * factor + lift)
            )
        }

        private fun dimColor(r: Float, g: Float, b: Float, factor: Float): FloatArray {
            return floatArrayOf(r * factor, g * factor, b * factor)
        }
    }
}
