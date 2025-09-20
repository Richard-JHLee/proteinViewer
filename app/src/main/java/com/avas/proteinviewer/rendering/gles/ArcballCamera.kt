package com.avas.proteinviewer.rendering.gles

import android.opengl.Matrix
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Simple arcball/orbit camera matching SceneKit default behaviour.
 */
class ArcballCamera {
    private val viewMatrix = FloatArray(16)

    private var targetX = 0f
    private var targetY = 0f
    private var targetZ = 0f

    private var distance = 20f
    private var minDistance = 5f
    private var maxDistance = 200f

    private var yawRad = 0f    // horizontal rotation (around Y)
    private var pitchRad = 0f  // vertical rotation (around X)

    private val tmpForward = FloatArray(3)
    private val tmpRight = FloatArray(3)
    private val tmpUp = floatArrayOf(0f, 1f, 0f)

    /** field of view in degrees (used for pan scaling). */
    var fovDeg: Float = 60f

    fun configure(distance: Float, minDistance: Float, maxDistance: Float) {
        this.distance = distance
        this.minDistance = minDistance
        this.maxDistance = maxDistance
        clampDistance()
    }

    fun setTarget(x: Float, y: Float, z: Float) {
        targetX = x
        targetY = y
        targetZ = z
    }

    fun orbit(deltaYawDeg: Float, deltaPitchDeg: Float) {
        val sensitivity = 0.005f
        yawRad -= deltaYawDeg * sensitivity
        pitchRad += deltaPitchDeg * sensitivity
        val maxPitch = Math.toRadians(89.0).toFloat()
        val minPitch = Math.toRadians(-89.0).toFloat()
        pitchRad = pitchRad.coerceIn(minPitch, maxPitch)
    }

    fun zoom(scaleFactor: Float) {
        if (scaleFactor.isNaN() || scaleFactor == 0f) return
        distance /= scaleFactor
        clampDistance()
    }

    fun pan(deltaX: Float, deltaY: Float) {
        // derive camera axes
        val cosPitch = cos(pitchRad)
        val sinPitch = sin(pitchRad)
        val cosYaw = cos(yawRad)
        val sinYaw = sin(yawRad)

        // forward vector
        tmpForward[0] = sinYaw * cosPitch
        tmpForward[1] = sinPitch
        tmpForward[2] = cosPitch * cosYaw

        // right = forward x up
        tmpRight[0] = tmpForward[2]
        tmpRight[1] = 0f
        tmpRight[2] = -tmpForward[0]
        normalize(tmpRight)

        // true up = right x forward
        val upX = tmpRight[1] * tmpForward[2] - tmpRight[2] * tmpForward[1]
        val upY = tmpRight[2] * tmpForward[0] - tmpRight[0] * tmpForward[2]
        val upZ = tmpRight[0] * tmpForward[1] - tmpRight[1] * tmpForward[0]

        val panScale = distance * 0.0015f
        targetX += (tmpRight[0] * (-deltaX) + upX * deltaY) * panScale
        targetY += (tmpRight[1] * (-deltaX) + upY * deltaY) * panScale
        targetZ += (tmpRight[2] * (-deltaX) + upZ * deltaY) * panScale
    }

    fun viewMatrix(): FloatArray {
        val cosPitch = cos(pitchRad)
        val sinPitch = sin(pitchRad)
        val cosYaw = cos(yawRad)
        val sinYaw = sin(yawRad)

        val eyeX = targetX + distance * cosPitch * sinYaw
        val eyeY = targetY + distance * sinPitch
        val eyeZ = targetZ + distance * cosPitch * cosYaw

        Matrix.setLookAtM(
            viewMatrix,
            0,
            eyeX,
            eyeY,
            eyeZ,
            targetX,
            targetY,
            targetZ,
            0f,
            1f,
            0f
        )
        return viewMatrix
    }

    private fun clampDistance() {
        distance = min(max(distance, minDistance), maxDistance)
    }

    private fun normalize(v: FloatArray) {
        val length = kotlin.math.sqrt(v[0]*v[0] + v[1]*v[1] + v[2]*v[2])
        if (length > 0f) {
            v[0] /= length
            v[1] /= length
            v[2] /= length
        }
    }
}
