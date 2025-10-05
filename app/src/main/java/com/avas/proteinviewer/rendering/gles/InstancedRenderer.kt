package com.avas.proteinviewer.rendering.gles

import android.opengl.GLES30
import android.util.Log
import com.avas.proteinviewer.domain.model.Atom
import com.avas.proteinviewer.domain.model.ColorMode
import com.avas.proteinviewer.rendering.ColorMaps
import android.graphics.Color
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sin
import kotlin.math.cos

/**
 * GPU 인스턴싱을 사용한 고성능 렌더러
 * 동일한 메쉬를 여러 번 그릴 때 성능을 크게 향상시킴
 */
class InstancedRenderer {
    
    companion object {
        private const val TAG = "InstancedRenderer"
    }
    
    // 인스턴싱용 셰이더
    private val INSTANCED_VERT_SHADER = """#version 300 es
layout (location = 0) in vec3 aPosition;
layout (location = 1) in vec3 aNormal;
layout (location = 2) in vec3 aInstancePosition;
layout (location = 3) in vec3 aInstanceColor;
layout (location = 4) in float aInstanceScale;

uniform mat4 uMvp;
uniform mat4 uModel;
uniform vec3 uLightPos;
uniform vec3 uViewPos;

out vec3 vColor;
out vec3 vNormal;
out vec3 vFragPos;

void main() {
    // 인스턴스 변환 적용
    vec3 worldPos = aPosition * aInstanceScale + aInstancePosition;
    gl_Position = uMvp * vec4(worldPos, 1.0);
    
    vColor = aInstanceColor;
    vNormal = aNormal;
    vFragPos = worldPos;
}"""

    private val INSTANCED_FRAG_SHADER = """#version 300 es
precision mediump float;

in vec3 vColor;
in vec3 vNormal;
in vec3 vFragPos;

uniform vec3 uLightPos;
uniform vec3 uViewPos;
uniform float uTransparency;

out vec4 fragColor;

void main() {
    // 간단한 Phong 조명 모델
    vec3 norm = normalize(vNormal);
    vec3 lightDir = normalize(uLightPos - vFragPos);
    vec3 viewDir = normalize(uViewPos - vFragPos);
    vec3 reflectDir = reflect(-lightDir, norm);
    
    float ambient = 0.3;
    float diffuse = max(dot(norm, lightDir), 0.0);
    float specular = pow(max(dot(viewDir, reflectDir), 0.0), 32.0);
    
    vec3 result = (ambient + diffuse + specular) * vColor;
    fragColor = vec4(result, uTransparency);
}"""
    
    private var program = 0
    private var aPositionHandle = 0
    private var aNormalHandle = 0
    private var aInstancePositionHandle = 0
    private var aInstanceColorHandle = 0
    private var aInstanceScaleHandle = 0
    private var uMvpHandle = 0
    private var uModelHandle = 0
    private var uLightPosHandle = 0
    private var uViewPosHandle = 0
    private var uTransparencyHandle = 0
    
    private var vao = 0
    private var meshVbo = 0
    private var instanceVbo = 0
    private var indexVbo = 0
    
    private var instanceCount = 0
    private var indexCount = 0
    
    fun initialize() {
        try {
            program = createProgram(INSTANCED_VERT_SHADER, INSTANCED_FRAG_SHADER)
            setupHandles()
            setupBuffers()
            Log.d(TAG, "InstancedRenderer initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize InstancedRenderer", e)
        }
    }
    
    private fun setupHandles() {
        aPositionHandle = GLES30.glGetAttribLocation(program, "aPosition")
        aNormalHandle = GLES30.glGetAttribLocation(program, "aNormal")
        aInstancePositionHandle = GLES30.glGetAttribLocation(program, "aInstancePosition")
        aInstanceColorHandle = GLES30.glGetAttribLocation(program, "aInstanceColor")
        aInstanceScaleHandle = GLES30.glGetAttribLocation(program, "aInstanceScale")
        
        uMvpHandle = GLES30.glGetUniformLocation(program, "uMvp")
        uModelHandle = GLES30.glGetUniformLocation(program, "uModel")
        uLightPosHandle = GLES30.glGetUniformLocation(program, "uLightPos")
        uViewPosHandle = GLES30.glGetUniformLocation(program, "uViewPos")
        uTransparencyHandle = GLES30.glGetUniformLocation(program, "uTransparency")
    }
    
    private fun setupBuffers() {
        val vaos = IntArray(1)
        GLES30.glGenVertexArrays(1, vaos, 0)
        vao = vaos[0]
        
        val vbos = IntArray(3)
        GLES30.glGenBuffers(3, vbos, 0)
        meshVbo = vbos[0]
        instanceVbo = vbos[1]
        indexVbo = vbos[2]
        
        GLES30.glBindVertexArray(vao)
        
        // 메쉬 데이터 (위치, 노멀)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, meshVbo)
        GLES30.glEnableVertexAttribArray(aPositionHandle)
        GLES30.glVertexAttribPointer(aPositionHandle, 3, GLES30.GL_FLOAT, false, 6 * 4, 0)
        GLES30.glEnableVertexAttribArray(aNormalHandle)
        GLES30.glVertexAttribPointer(aNormalHandle, 3, GLES30.GL_FLOAT, false, 6 * 4, 3 * 4)
        
        // 인스턴스 데이터 (위치, 색상, 스케일)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, instanceVbo)
        GLES30.glEnableVertexAttribArray(aInstancePositionHandle)
        GLES30.glVertexAttribPointer(aInstancePositionHandle, 3, GLES30.GL_FLOAT, false, 7 * 4, 0)
        GLES30.glVertexAttribDivisor(aInstancePositionHandle, 1)
        
        GLES30.glEnableVertexAttribArray(aInstanceColorHandle)
        GLES30.glVertexAttribPointer(aInstanceColorHandle, 3, GLES30.GL_FLOAT, false, 7 * 4, 3 * 4)
        GLES30.glVertexAttribDivisor(aInstanceColorHandle, 1)
        
        GLES30.glEnableVertexAttribArray(aInstanceScaleHandle)
        GLES30.glVertexAttribPointer(aInstanceScaleHandle, 1, GLES30.GL_FLOAT, false, 7 * 4, 6 * 4)
        GLES30.glVertexAttribDivisor(aInstanceScaleHandle, 1)
        
        GLES30.glBindVertexArray(0)
    }
    
    /**
     * 구체 메쉬를 업로드하고 인스턴스 데이터를 설정
     */
    fun uploadSphereInstances(
        atoms: List<Atom>,
        colorMode: ColorMode,
        radius: Float,
        segments: Int
    ) {
        if (atoms.isEmpty()) {
            instanceCount = 0
            return
        }
        
        // 기본 구체 메쉬 생성 (한 번만)
        val sphereMesh = createSphereMesh(radius, segments)
        
        // 메쉬 데이터 업로드
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, meshVbo)
        val meshBuffer = ByteBuffer.allocateDirect(sphereMesh.vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        meshBuffer.put(sphereMesh.vertices.toFloatArray())
        meshBuffer.flip()
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, meshBuffer.capacity() * 4, meshBuffer, GLES30.GL_STATIC_DRAW)
        
        // 인덱스 데이터 업로드
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, indexVbo)
        val indexBuffer = ByteBuffer.allocateDirect(sphereMesh.indices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asIntBuffer()
        indexBuffer.put(sphereMesh.indices.toIntArray())
        indexBuffer.flip()
        GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, indexBuffer.capacity() * 4, indexBuffer, GLES30.GL_STATIC_DRAW)
        
        indexCount = sphereMesh.indices.size
        
        // 인스턴스 데이터 생성
        val instanceData = ByteBuffer.allocateDirect(atoms.size * 7 * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        
        atoms.forEach { atom ->
            // 위치
            instanceData.put(atom.position.x)
            instanceData.put(atom.position.y)
            instanceData.put(atom.position.z)
            
            // 색상
            val color = getAtomColor(atom, colorMode)
            instanceData.put(color[0])
            instanceData.put(color[1])
            instanceData.put(color[2])
            
            // 스케일 (원소에 따른 크기 조정)
            val scale = getAtomScale(atom.element)
            instanceData.put(scale)
        }
        instanceData.flip()
        
        // 인스턴스 데이터 업로드
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, instanceVbo)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, instanceData.capacity() * 4, instanceData, GLES30.GL_DYNAMIC_DRAW)
        
        instanceCount = atoms.size
        Log.d(TAG, "Uploaded $instanceCount sphere instances")
    }
    
    /**
     * 인스턴싱된 구체들을 렌더링
     */
    fun renderInstancedSpheres(
        mvpMatrix: FloatArray,
        modelMatrix: FloatArray,
        lightPos: FloatArray,
        viewPos: FloatArray,
        transparency: Float
    ) {
        if (instanceCount == 0 || program == 0) return
        
        GLES30.glUseProgram(program)
        
        // 유니폼 설정
        GLES30.glUniformMatrix4fv(uMvpHandle, 1, false, mvpMatrix, 0)
        GLES30.glUniformMatrix4fv(uModelHandle, 1, false, modelMatrix, 0)
        GLES30.glUniform3fv(uLightPosHandle, 1, lightPos, 0)
        GLES30.glUniform3fv(uViewPosHandle, 1, viewPos, 0)
        GLES30.glUniform1f(uTransparencyHandle, transparency)
        
        // 인스턴싱된 렌더링
        GLES30.glBindVertexArray(vao)
        GLES30.glDrawElementsInstanced(
            GLES30.GL_TRIANGLES,
            indexCount,
            GLES30.GL_UNSIGNED_INT,
            0,
            instanceCount
        )
        GLES30.glBindVertexArray(0)
    }
    
    private fun createSphereMesh(radius: Float, segments: Int): InstancedMeshData {
        val vertices = mutableListOf<Float>()
        val normals = mutableListOf<Float>()
        val indices = mutableListOf<Int>()
        
        val rings = segments
        val sectors = segments
        val stride = sectors + 1
        
        // 정점 생성
        for (i in 0..rings) {
            val phi = Math.PI * i / rings
            for (j in 0..sectors) {
                val theta = 2.0 * Math.PI * j / sectors
                
                val x = (radius * sin(phi) * cos(theta)).toFloat()
                val y = (radius * cos(phi)).toFloat()
                val z = (radius * sin(phi) * sin(theta)).toFloat()
                
                vertices.addAll(listOf(x, y, z))
                normals.addAll(listOf(x/radius, y/radius, z/radius))
            }
        }
        
        // 인덱스 생성
        for (i in 0 until rings) {
            for (j in 0 until sectors) {
                val base = i * stride + j
                val baseNext = (i + 1) * stride + j
                val nextBase = i * stride + (j + 1) % stride
                val nextBaseNext = (i + 1) * stride + (j + 1) % stride
                
                indices.addAll(listOf(base, baseNext, nextBase))
                indices.addAll(listOf(baseNext, nextBaseNext, nextBase))
            }
        }
        
        return InstancedMeshData(vertices, normals, indices)
    }
    
    private fun getAtomColor(atom: Atom, colorMode: ColorMode): List<Float> {
        return when (colorMode) {
            ColorMode.ELEMENT -> {
                val elementColor = ColorMaps.cpk(atom.element)
                listOf(
                    Color.red(elementColor) / 255f,
                    Color.green(elementColor) / 255f,
                    Color.blue(elementColor) / 255f
                )
            }
            ColorMode.CHAIN -> {
                val chainColor = ColorMaps.chainColor(atom.chain)
                listOf(
                    Color.red(chainColor) / 255f,
                    Color.green(chainColor) / 255f,
                    Color.blue(chainColor) / 255f
                )
            }
            ColorMode.UNIFORM -> {
                listOf(0.5f, 0.5f, 0.5f)
            }
            ColorMode.SECONDARY_STRUCTURE -> {
                val structureColor = ColorMaps.secondaryStructure(atom.secondaryStructure)
                listOf(
                    Color.red(structureColor) / 255f,
                    Color.green(structureColor) / 255f,
                    Color.blue(structureColor) / 255f
                )
            }
        }
    }
    
    private fun getAtomScale(element: String): Float {
        // 원소에 따른 크기 조정
        return when (element.uppercase()) {
            "H" -> 0.3f
            "C" -> 0.7f
            "N" -> 0.65f
            "O" -> 0.6f
            "S" -> 1.0f
            "P" -> 1.1f
            else -> 0.8f
        }
    }
    
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
    
    fun cleanup() {
        if (vao != 0) {
            GLES30.glDeleteVertexArrays(1, intArrayOf(vao), 0)
            vao = 0
        }
        if (meshVbo != 0) {
            GLES30.glDeleteBuffers(1, intArrayOf(meshVbo), 0)
            meshVbo = 0
        }
        if (instanceVbo != 0) {
            GLES30.glDeleteBuffers(1, intArrayOf(instanceVbo), 0)
            instanceVbo = 0
        }
        if (indexVbo != 0) {
            GLES30.glDeleteBuffers(1, intArrayOf(indexVbo), 0)
            indexVbo = 0
        }
        if (program != 0) {
            GLES30.glDeleteProgram(program)
            program = 0
        }
    }
}

data class InstancedMeshData(
    val vertices: List<Float>,
    val normals: List<Float>,
    val indices: List<Int>
)

