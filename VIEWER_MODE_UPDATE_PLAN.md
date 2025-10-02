# 🎯 Viewer Mode 수정 계획

## 📱 **아이폰 Viewer Mode 구조**

### **1. 전체 화면 3D 뷰어**
- TopBar 없음 (완전 전체 화면)
- 3D 뷰어가 화면 전체를 차지
- 자동 회전 옵션

### **2. 하단 컨트롤 바**
```
┌─────────────────────────────────────────────┐
│ [Rendering] [Options] [Colors]              │  ← 48pt 높이
└─────────────────────────────────────────────┘
```

**버튼:**
- **Rendering Style**: 렌더링 방식 선택
- **Options**: 추가 옵션
- **Colors**: 색상 모드 선택

**스타일:**
- `.ultraThinMaterial` 배경
- 각 버튼: 아이콘 + 텍스트 (세로 배치)
- 선택 시: 색상 변경 (Rendering=빨강, Options=주황, Colors=녹색)

### **3. 2차 패널 (슬라이드 업)**

**Rendering Style 패널:**
```
┌─────────────────────────────────────────────┐
│ [Ribbon] [Spheres] [Sticks] [Cartoon]      │
│ [Surface]                                    │
└─────────────────────────────────────────────┘
```

**Color Mode 패널:**
```
┌─────────────────────────────────────────────┐
│ [Element] [Chain] [Uniform]                 │
│ [Secondary Structure]                        │
└─────────────────────────────────────────────┘
```

### **4. 우상단 버튼**
```
┌─────────────────────────────────────────────┐
│                              [←] [🔄] [⚙️]    │
└─────────────────────────────────────────────┘
```

- **← Back**: Info Mode로 돌아가기
- **🔄 Reset**: 카메라 리셋
- **⚙️ Settings**: 설정

---

## 🔧 **안드로이드 수정 계획**

### **수정 1: TopBar 제거**
```kotlin
Scaffold(
    // topBar 제거! (전체 화면)
    bottomBar = {
        // 아이폰 스타일 컨트롤 바
    }
)
```

### **수정 2: 하단 컨트롤 바 재구성**
```kotlin
bottomBar = {
    Column {
        // 2차 패널 (조건부)
        if (showSecondaryPanel) {
            when (selectedPanel) {
                Panel.RENDERING -> RenderingStylePanel()
                Panel.COLOR -> ColorModePanel()
                Panel.OPTIONS -> OptionsPanel()
            }
        }
        
        // 메인 컨트롤 바
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                .padding(8.dp)
        ) {
            ControlButton(
                icon = Icons.Default.Brush,
                label = "Rendering",
                selected = selectedPanel == Panel.RENDERING,
                color = Color(0xFFF44336) // 빨강
            )
            ControlButton(
                icon = Icons.Default.MoreVert,
                label = "Options",
                selected = selectedPanel == Panel.OPTIONS,
                color = Color(0xFFFF9800) // 주황
            )
            ControlButton(
                icon = Icons.Default.Palette,
                label = "Colors",
                selected = selectedPanel == Panel.COLOR,
                color = Color(0xFF4CAF50) // 녹색
            )
        }
    }
}
```

### **수정 3: 우상단 버튼 추가**
```kotlin
Box(modifier = Modifier.fillMaxSize()) {
    // 3D 뷰어
    ProteinCanvas3DView(...)
    
    // 우상단 버튼들
    Row(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(16.dp)
    ) {
        IconButton(onClick = onBackToInfo) {
            Icon(Icons.Default.ArrowBack, "Back")
        }
        IconButton(onClick = onResetCamera) {
            Icon(Icons.Default.Refresh, "Reset")
        }
        IconButton(onClick = onSettings) {
            Icon(Icons.Default.Settings, "Settings")
        }
    }
}
```

---

## ✅ **구현 순서**

1. ✅ TopBar 제거
2. ✅ 우상단 버튼 추가 (Back, Reset, Settings)
3. ✅ 하단 컨트롤 바 재구성 (3버튼)
4. ✅ 2차 패널 구현 (Rendering, Color, Options)
5. ✅ 블러 효과 배경
6. ✅ 버튼 선택 시 색상 변경

**바로 구현 시작합니다!**

