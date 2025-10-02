package com.avas.proteinviewer.presentation

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.avas.proteinviewer.domain.model.*
import kotlin.math.*

/**
 * iOS 스타일의 3D 렌더링 시스템
 * - Style별 다른 렌더링 방식
 * - ColorMode 완벽 지원
 * - Highlight/Focus 시스템
 */
class Protein3DRenderer(
    private val structure: PDBStructure,
    private val renderStyle: RenderStyle,
    private val colorMode: ColorMode,
    private val highlightedChains: Set<String> = emptySet(),
    private val transparency: Float = 0.7f,
    private val atomSize: Float = 1.0f
) {
    
    // 3D 변환 파라미터
    private var rotationX = 0f
    private var rotationY = 0f
    private var scale = 1f
    private var offsetX = 0f
    private var offsetY = 0f
    
    fun updateTransform(
        newRotationX: Float,
        newRotationY: Float,
        newScale: Float,
        newOffsetX: Float,
        newOffsetY: Float
    ) {
        rotationX = newRotationX
        rotationY = newRotationY
        scale = newScale
        offsetX = newOffsetX
        offsetY = newOffsetY
    }
    
    fun render(drawScope: DrawScope) {
        val centerX = drawScope.size.width / 2f
        val centerY = drawScope.size.height / 2f
        
        // 중심점 계산
        val com = structure.centerOfMass
        
        // 정규화 스케일
        val boundingSize = maxOf(
            structure.boundingBoxMax.x - structure.boundingBoxMin.x,
            structure.boundingBoxMax.y - structure.boundingBoxMin.y,
            structure.boundingBoxMax.z - structure.boundingBoxMin.z
        )
        
        val scaleFactor = (drawScope.size.minDimension * 0.4f * scale) / boundingSize
        
        when (renderStyle) {
            RenderStyle.SPHERES -> renderSpheres(drawScope, centerX, centerY, com, scaleFactor)
            RenderStyle.STICKS -> renderSticks(drawScope, centerX, centerY, com, scaleFactor)
            RenderStyle.RIBBON, RenderStyle.CARTOON -> renderRibbon(drawScope, centerX, centerY, com, scaleFactor)
            RenderStyle.SURFACE -> renderSurface(drawScope, centerX, centerY, com, scaleFactor)
        }
    }
    
    private fun renderSpheres(
        drawScope: DrawScope,
        centerX: Float,
        centerY: Float,
        com: Vector3,
        scaleFactor: Float
    ) {
        // Z-depth 기반 정렬 (뒤에서 앞으로)
        val sortedAtoms = structure.atoms.sortedBy { atom ->
            val (x, y, z) = rotatePoint(
                atom.position.x - com.x,
                atom.position.y - com.y,
                atom.position.z - com.z
            )
            -z // Z가 클수록 (앞쪽) 나중에 그림
        }
        
        sortedAtoms.forEach { atom ->
            val (screenX, screenY, depth) = project3DTo2D(
                atom.position.x - com.x,
                atom.position.y - com.y,
                atom.position.z - com.z,
                scaleFactor,
                centerX + offsetX,
                centerY + offsetY
            )
            
            val isHighlighted = highlightedChains.contains(atom.chain)
            
            // 디버그: 초기 상태 확인
            if (atom.id == 1) {
                android.util.Log.d("Protein3DRenderer", "highlightedChains: $highlightedChains")
                android.util.Log.d("Protein3DRenderer", "atom.chain: ${atom.chain}, isHighlighted: $isHighlighted")
            }
            
            // 투명도 계산 (iOS 방식)
            val baseOpacity = when {
                isHighlighted -> 0.9f
                highlightedChains.isEmpty() -> 0.5f // 아이폰과 동일: 0.5f
                else -> 0.15f // 아이폰과 동일: 0.15f (매우 희미)
            }
            
            val finalOpacity = baseOpacity * transparency
            
            // 반지름 계산
            val baseRadius = getAtomRadius(atom.element) * atomSize
            val perspectiveFactor = 1.0f / (1.0f + abs(depth) * 0.005f)
            val radius = (baseRadius * scaleFactor * perspectiveFactor).coerceAtLeast(2f)
            
            // 색상 결정
            val color = getAtomColor(atom, isHighlighted, finalOpacity)
            
            drawScope.drawCircle(
                color = color,
                radius = radius,
                center = Offset(screenX, screenY)
            )
        }
    }
    
    private fun renderSticks(
        drawScope: DrawScope,
        centerX: Float,
        centerY: Float,
        com: Vector3,
        scaleFactor: Float
    ) {
        // 본드 먼저 그리기
        structure.bonds.forEach { bond ->
            val atom1 = structure.atoms.find { it.id == bond.atomA } ?: return@forEach
            val atom2 = structure.atoms.find { it.id == bond.atomB } ?: return@forEach
            
            val (x1, y1, _) = project3DTo2D(
                atom1.position.x - com.x,
                atom1.position.y - com.y,
                atom1.position.z - com.z,
                scaleFactor,
                centerX + offsetX,
                centerY + offsetY
            )
            
            val (x2, y2, _) = project3DTo2D(
                atom2.position.x - com.x,
                atom2.position.y - com.y,
                atom2.position.z - com.z,
                scaleFactor,
                centerX + offsetX,
                centerY + offsetY
            )
            
            val isHighlighted = highlightedChains.contains(atom1.chain)
            // 아이폰과 동일: 초기 0.5f, 하이라이트 0.8f, 다른것 하이라이트시 0.15f
            val baseOpacity = if (isHighlighted) 0.8f else if (highlightedChains.isEmpty()) 0.5f else 0.15f
            val color = getAtomColor(atom1, isHighlighted, baseOpacity * transparency)
            
            drawScope.drawLine(
                color = color,
                start = Offset(x1, y1),
                end = Offset(x2, y2),
                strokeWidth = 2f
            )
        }
        
        // 원자를 작은 점으로
        structure.atoms.forEach { atom ->
            val (screenX, screenY, _) = project3DTo2D(
                atom.position.x - com.x,
                atom.position.y - com.y,
                atom.position.z - com.z,
                scaleFactor,
                centerX + offsetX,
                centerY + offsetY
            )
            
            val isHighlighted = highlightedChains.contains(atom.chain)
            // 아이폰과 동일
            val baseOpacity = if (isHighlighted) 0.9f else if (highlightedChains.isEmpty()) 0.5f else 0.15f
            val color = getAtomColor(atom, isHighlighted, baseOpacity * transparency)
            
            drawScope.drawCircle(
                color = color,
                radius = 3f,
                center = Offset(screenX, screenY)
            )
        }
    }
    
    private fun renderRibbon(
        drawScope: DrawScope,
        centerX: Float,
        centerY: Float,
        com: Vector3,
        scaleFactor: Float
    ) {
        // 체인별로 그룹화
        val chainGroups = structure.atoms.groupBy { it.chain }
        
        chainGroups.forEach { (chainId, chainAtoms) ->
            // CA (alpha carbon) 원자만 필터링하고 정렬
            val caAtoms = chainAtoms
                .filter { it.name == "CA" && it.isBackbone }
                .sortedBy { it.residueNumber }
            
            if (caAtoms.size < 3) return@forEach
            
            val isChainHighlighted = highlightedChains.contains(chainId)
            
            // Catmull-Rom 스플라인 생성 (iOS와 동일)
            val splinePoints = generateCatmullRomSpline(caAtoms, 5)
            
            // 리본을 실제 면(surface)으로 렌더링
            if (splinePoints.size < 2) return@forEach
            
            // 투명도 계산 (iOS 방식)
            val baseOpacity = when {
                isChainHighlighted -> 1.0f // 아이폰과 동일
                highlightedChains.isEmpty() -> 0.5f // 아이폰과 동일
                else -> 0.15f // 아이폰과 동일
            }
            
            // 리본의 폭 계산
            val getRibbonWidth = { atom: Atom ->
                when (atom.secondaryStructure.name) {
                    "HELIX" -> 30f
                    "SHEET" -> 40f
                    else -> 15f
                }
            }
            
            // 각 세그먼트를 사각형 면으로 그리기
            for (i in 0 until splinePoints.size - 1) {
                val atom1 = splinePoints[i]
                val atom2 = splinePoints[i + 1]
                
                // 3D to 2D 투영
                val (x1, y1, _) = project3DTo2D(
                    atom1.position.x - com.x,
                    atom1.position.y - com.y,
                    atom1.position.z - com.z,
                    scaleFactor,
                    centerX + offsetX,
                    centerY + offsetY
                )
                
                val (x2, y2, _) = project3DTo2D(
                    atom2.position.x - com.x,
                    atom2.position.y - com.y,
                    atom2.position.z - com.z,
                    scaleFactor,
                    centerX + offsetX,
                    centerY + offsetY
                )
                
                // 방향 벡터 계산
                val dx = x2 - x1
                val dy = y2 - y1
                val length = kotlin.math.sqrt(dx * dx + dy * dy)
                
                if (length < 0.1f) continue
                
                // 수직 벡터 (리본의 폭 방향)
                val perpX = -dy / length
                val perpY = dx / length
                
                val ribbonWidth = getRibbonWidth(atom1)
                val halfWidth = ribbonWidth / 2f
                
                // 리본의 네 꼭지점 계산
                val leftTop = Offset(x1 + perpX * halfWidth, y1 + perpY * halfWidth)
                val rightTop = Offset(x1 - perpX * halfWidth, y1 - perpY * halfWidth)
                val leftBottom = Offset(x2 + perpX * halfWidth, y2 + perpY * halfWidth)
                val rightBottom = Offset(x2 - perpX * halfWidth, y2 - perpY * halfWidth)
                
                val color = getRibbonColor(atom1, isChainHighlighted, baseOpacity * transparency)
                
                // 리본 면 그리기 (채워진 사각형)
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(leftTop.x, leftTop.y)
                    lineTo(rightTop.x, rightTop.y)
                    lineTo(rightBottom.x, rightBottom.y)
                    lineTo(leftBottom.x, leftBottom.y)
                    close()
                }
                
                // 면을 채움 (실제 리본)
                drawScope.drawPath(
                    path = path,
                    color = color
                )
                
                // 양쪽 가장자리 선 그리기 (면이 선처럼 보이도록)
                // 왼쪽 가장자리
                drawScope.drawLine(
                    color = color.copy(alpha = minOf(1f, color.alpha * 1.3f)), // 더 진하게
                    start = leftTop,
                    end = leftBottom,
                    strokeWidth = 2.5f
                )
                
                // 오른쪽 가장자리
                drawScope.drawLine(
                    color = color.copy(alpha = minOf(1f, color.alpha * 1.3f)), // 더 진하게
                    start = rightTop,
                    end = rightBottom,
                    strokeWidth = 2.5f
                )
            }
        }
        
        // 리간드 렌더링
        renderLigands(drawScope, centerX, centerY, com, scaleFactor)
        
        // 포켓 렌더링
        renderPockets(drawScope, centerX, centerY, com, scaleFactor)
    }
    
    private fun renderSurface(
        drawScope: DrawScope,
        centerX: Float,
        centerY: Float,
        com: Vector3,
        scaleFactor: Float
    ) {
        // Surface는 반투명 큰 구체로 표현
        structure.atoms.forEach { atom ->
            val (screenX, screenY, depth) = project3DTo2D(
                atom.position.x - com.x,
                atom.position.y - com.y,
                atom.position.z - com.z,
                scaleFactor,
                centerX + offsetX,
                centerY + offsetY
            )
            
            val isHighlighted = highlightedChains.contains(atom.chain)
            // 아이폰과 동일
            val baseOpacity = if (isHighlighted) 0.6f else if (highlightedChains.isEmpty()) 0.3f else 0.1f
            
            val baseRadius = getAtomRadius(atom.element) * atomSize * 1.5f
            val perspectiveFactor = 1.0f / (1.0f + abs(depth) * 0.005f)
            val radius = (baseRadius * scaleFactor * perspectiveFactor).coerceAtLeast(3f)
            
            val color = getAtomColor(atom, isHighlighted, baseOpacity * transparency)
            
            drawScope.drawCircle(
                color = color,
                radius = radius,
                center = Offset(screenX, screenY)
            )
        }
    }
    
    private fun renderLigands(
        drawScope: DrawScope,
        centerX: Float,
        centerY: Float,
        com: Vector3,
        scaleFactor: Float
    ) {
        // iOS 스타일: 리간드를 크고 눈에 띄게 렌더링 (ProteinSceneView.swift 3263-3268, 4270-4273)
        val ligandAtoms = structure.atoms.filter { it.isLigand }.sortedByDescending { it.position.z }
        
        ligandAtoms.forEach { atom ->
            val (screenX, screenY, depth) = project3DTo2D(
                atom.position.x - com.x,
                atom.position.y - com.y,
                atom.position.z - com.z,
                scaleFactor,
                centerX + offsetX,
                centerY + offsetY
            )
            
            // iOS처럼 리간드는 주황색으로 표시
            val ligandColor = Color(0xFFFF9500) // systemOrange
            
            // 크기를 크게 (iOS: baseRadius = 0.6)
            val baseRadius = 12f * scale
            val radius = (baseRadius * scaleFactor / 50f).coerceAtLeast(8f)
            
            // 외곽선 (검은색, 더 두껍게)
            drawScope.drawCircle(
                color = Color.Black.copy(alpha = 0.5f),
                radius = radius + 3f,
                center = Offset(screenX, screenY)
            )
            
            // 메인 리간드 구체
            drawScope.drawCircle(
                color = ligandColor.copy(alpha = 0.9f * transparency),
                radius = radius,
                center = Offset(screenX, screenY)
            )
            
            // 하이라이트 효과 (입체감)
            drawScope.drawCircle(
                color = Color.White.copy(alpha = 0.4f),
                radius = radius * 0.5f,
                center = Offset(screenX - radius * 0.2f, screenY - radius * 0.2f)
            )
        }
    }
    
    private fun renderPockets(
        drawScope: DrawScope,
        centerX: Float,
        centerY: Float,
        com: Vector3,
        scaleFactor: Float
    ) {
        // iOS 스타일: 포켓을 작고 반투명하게 렌더링 (ProteinSceneView.swift 3275-3280, 4271)
        val pocketAtoms = structure.atoms.filter { it.isPocket }.sortedByDescending { it.position.z }
        
        pocketAtoms.forEach { atom ->
            val (screenX, screenY, depth) = project3DTo2D(
                atom.position.x - com.x,
                atom.position.y - com.y,
                atom.position.z - com.z,
                scaleFactor,
                centerX + offsetX,
                centerY + offsetY
            )
            
            // iOS처럼 포켓은 청록색(cyan)으로 표시
            val pocketColor = Color(0xFF5AC8FA) // systemTeal/Cyan
            
            // 크기를 작게 (iOS: baseRadius = 0.6, 리본이 잘 보이도록)
            val baseRadius = 6f * scale
            val radius = (baseRadius * scaleFactor / 50f).coerceAtLeast(3f)
            
            // 외곽선 (검은색)
            drawScope.drawCircle(
                color = Color.Black.copy(alpha = 0.3f),
                radius = radius + 2f,
                center = Offset(screenX, screenY)
            )
            
            // 메인 포켓 구체
            drawScope.drawCircle(
                color = pocketColor.copy(alpha = 0.6f * transparency),
                radius = radius,
                center = Offset(screenX, screenY)
            )
        }
    }
    
    // Catmull-Rom 스플라인 생성 (iOS와 동일한 알고리즘)
    private fun generateCatmullRomSpline(caAtoms: List<Atom>, segmentsPerSpan: Int): List<Atom> {
        if (caAtoms.size < 4) return caAtoms
        
        val splinePoints = mutableListOf<Atom>()
        splinePoints.add(caAtoms[0])
        
        for (i in 0 until caAtoms.size - 3) {
            val p0 = caAtoms[i]
            val p1 = caAtoms[i + 1]
            val p2 = caAtoms[i + 2]
            val p3 = caAtoms[i + 3]
            
            for (j in 1..segmentsPerSpan) {
                val t = j.toFloat() / segmentsPerSpan
                val interpolatedPos = catmullRomInterpolation(
                    p0.position, p1.position, p2.position, p3.position, t
                )
                
                // 보간된 원자 생성 (p1의 속성 상속)
                val interpolatedAtom = p1.copy(position = interpolatedPos)
                splinePoints.add(interpolatedAtom)
            }
        }
        
        splinePoints.add(caAtoms.last())
        return splinePoints
    }
    
    private fun catmullRomInterpolation(
        p0: Vector3, p1: Vector3, p2: Vector3, p3: Vector3, t: Float
    ): Vector3 {
        val t2 = t * t
        val t3 = t2 * t
        
        val x = 0.5f * ((2 * p1.x) + (-p0.x + p2.x) * t + 
                (2 * p0.x - 5 * p1.x + 4 * p2.x - p3.x) * t2 + 
                (-p0.x + 3 * p1.x - 3 * p2.x + p3.x) * t3)
        
        val y = 0.5f * ((2 * p1.y) + (-p0.y + p2.y) * t + 
                (2 * p0.y - 5 * p1.y + 4 * p2.y - p3.y) * t2 + 
                (-p0.y + 3 * p1.y - 3 * p2.y + p3.y) * t3)
        
        val z = 0.5f * ((2 * p1.z) + (-p0.z + p2.z) * t + 
                (2 * p0.z - 5 * p1.z + 4 * p2.z - p3.z) * t2 + 
                (-p0.z + 3 * p1.z - 3 * p2.z + p3.z) * t3)
        
        return Vector3(x, y, z)
    }
    
    // 3D to 2D 투영 (원근 효과 포함)
    private fun project3DTo2D(
        x: Float, y: Float, z: Float,
        scale: Float,
        centerX: Float,
        centerY: Float
    ): Triple<Float, Float, Float> {
        val (x1, y1, z1) = rotatePoint(x, y, z)
        
        // 원근 투영
        val distance = 100f
        val factor = distance / (distance + z1)
        
        val screenX = centerX + x1 * scale * factor
        val screenY = centerY + y1 * scale * factor
        
        return Triple(screenX, screenY, z1)
    }
    
    private fun rotatePoint(x: Float, y: Float, z: Float): Triple<Float, Float, Float> {
        // Y축 회전
        val cosY = cos(rotationY * 0.01f)
        val sinY = sin(rotationY * 0.01f)
        val x1 = x * cosY - z * sinY
        val z1 = x * sinY + z * cosY
        
        // X축 회전
        val cosX = cos(rotationX * 0.01f)
        val sinX = sin(rotationX * 0.01f)
        val y1 = y * cosX - z1 * sinX
        val z2 = y * sinX + z1 * cosX
        
        return Triple(x1, y1, z2)
    }
    
    private fun getAtomColor(atom: Atom, isHighlighted: Boolean, opacity: Float): Color {
        val baseColor = when (colorMode) {
            ColorMode.ELEMENT -> {
                val (r, g, b) = atom.atomicColor
                Color(r, g, b)
            }
            ColorMode.CHAIN -> getChainColor(atom.chain)
            ColorMode.UNIFORM -> Color.Blue
            ColorMode.SECONDARY_STRUCTURE -> getSecondaryStructureColor(atom.secondaryStructure.name)
        }
        
        // iOS 방식: Highlight된 경우 채도와 밝기 증가
        return if (isHighlighted) {
            // 아이폰과 동일: 채도 1.4배, 밝기 1.3배 증가
            val hsv = FloatArray(3)
            val argb = (baseColor.alpha * 255).toInt() shl 24 or
                       ((baseColor.red * 255).toInt() shl 16) or
                       ((baseColor.green * 255).toInt() shl 8) or
                       (baseColor.blue * 255).toInt()
            android.graphics.Color.colorToHSV(argb, hsv)
            hsv[1] = (hsv[1] * 1.4f).coerceIn(0f, 1f) // Saturation 증가
            hsv[2] = (hsv[2] * 1.3f).coerceIn(0f, 1f) // Brightness 증가
            Color(android.graphics.Color.HSVToColor(hsv)).copy(alpha = opacity)
        } else {
            baseColor.copy(alpha = opacity * 0.7f)
        }
    }
    
    private fun getRibbonColor(atom: Atom, isHighlighted: Boolean, opacity: Float): Color {
        val baseColor = when (colorMode) {
            ColorMode.SECONDARY_STRUCTURE -> getSecondaryStructureColor(atom.secondaryStructure.name)
            ColorMode.CHAIN -> getChainColor(atom.chain)
            ColorMode.ELEMENT -> {
                val (r, g, b) = atom.atomicColor
                Color(r, g, b)
            }
            ColorMode.UNIFORM -> Color.Blue
        }
        
        // 아이폰과 동일: 하이라이트 시 채도와 밝기 증가
        return if (isHighlighted) {
            val hsv = FloatArray(3)
            val argb = (baseColor.alpha * 255).toInt() shl 24 or
                       ((baseColor.red * 255).toInt() shl 16) or
                       ((baseColor.green * 255).toInt() shl 8) or
                       (baseColor.blue * 255).toInt()
            android.graphics.Color.colorToHSV(argb, hsv)
            hsv[1] = (hsv[1] * 1.4f).coerceIn(0f, 1f) // Saturation 증가
            hsv[2] = (hsv[2] * 1.3f).coerceIn(0f, 1f) // Brightness 증가
            Color(android.graphics.Color.HSVToColor(hsv)).copy(alpha = opacity)
        } else {
            baseColor.copy(alpha = opacity)
        }
    }
    
    private fun getAtomRadius(element: String): Float {
        return when (element.uppercase()) {
            "H" -> 0.3f
            "C" -> 0.7f
            "N" -> 0.65f
            "O" -> 0.6f
            "S" -> 1.0f
            "P" -> 1.0f
            else -> 0.8f
        }
    }
    
    private fun getChainColor(chain: String): Color {
        // iOS 앱과 동일한 체인 색상 (ProteinSceneView.swift 4226-4237)
        return when (chain.uppercase()) {
            "A" -> Color(0xFF007AFF) // systemBlue
            "B" -> Color(0xFFFF9500) // systemOrange
            "C" -> Color(0xFF34C759) // systemGreen
            "D" -> Color(0xFFAF52DE) // systemPurple
            "E" -> Color(0xFFFF2D55) // systemPink
            "F" -> Color(0xFF5AC8FA) // systemTeal
            "G" -> Color(0xFF5856D6) // systemIndigo
            "H" -> Color(0xFFA2845E) // systemBrown
            else -> Color(0xFF8E8E93) // systemGray (기본값)
        }
    }
    
    private fun getSecondaryStructureColor(structureType: String): Color {
        // iOS 앱과 동일한 2차 구조 색상 (ProteinSceneView.swift 4210-4221)
        return when (structureType) {
            "HELIX" -> Color(0xFFFF3B30) // systemRed
            "SHEET" -> Color(0xFFFFCC00) // systemYellow
            "COIL" -> Color(0xFF8E8E93) // systemGray
            else -> Color(0xFF007AFF) // systemBlue (unknown)
        }
    }
}

