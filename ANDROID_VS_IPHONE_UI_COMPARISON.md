# 📊 Android vs iPhone UI 비교 분석

## ❌ **주요 차이점 (수정 필요)**

### **1. Info Mode 헤더 구조**

| 항목 | iPhone | Android | 일치 여부 |
|-----|--------|---------|----------|
| **헤더 높이** | 44pt | TopAppBar (기본 높이) | ⚠️ 수정 필요 |
| **배경** | `.ultraThinMaterial` | Solid color | ❌ 불일치 |
| **Padding** | `horizontal: 16, top: 8, bottom: 12` | TopAppBar 기본 | ❌ 불일치 |
| **햄버거 메뉴** | `line.3.horizontal` | `Icons.Default.Menu` | ✅ 일치 |
| **단백질 ID 폰트** | `.title3` + `.semibold` | `.titleMedium` + `.SemiBold` | ⚠️ 크기 차이 |
| **단백질 이름 폰트** | `.callout` + `.secondary` | `.bodySmall` + `.onSurfaceVariant` | ⚠️ 크기 차이 |
| **Library 아이콘** | `books.vertical` | `Icons.Default.LibraryBooks` | ✅ 일치 |
| **Viewer 아이콘** | `eye` | `Icons.Default.Visibility` | ✅ 일치 |

**수정 사항:**
1. TopAppBar를 커스텀 헤더로 교체
2. `.ultraThinMaterial` 효과 적용 (투명 블러)
3. 정확한 padding 값 적용
4. 폰트 크기를 아이폰과 동일하게 조정

---

### **2. Focus/Clear 인디케이터**

| 항목 | iPhone | Android | 일치 여부 |
|-----|--------|---------|----------|
| **위치** | 헤더 바로 아래 (고정 영역) | 헤더 내부 | ❌ 불일치 |
| **Focus 배지** | 녹색 배지 + "Focused: Chain A" | 없음 | ❌ 미구현 |
| **Clear 버튼** | 빨간색 배지 + "Clear" | 빨간색 버튼 + "Clear" | ⚠️ 스타일 차이 |
| **레이아웃** | HStack (좌측: Focus, 우측: Clear) | 우측 정렬만 | ❌ 불일치 |
| **배경** | `.ultraThinMaterial` | Transparent | ❌ 불일치 |

**수정 사항:**
1. 별도의 고정 영역 추가 (헤더 아래)
2. Focus 인디케이터 추가 (녹색 배지)
3. Clear 버튼을 배지 스타일로 변경
4. HStack 레이아웃으로 변경

---

### **3. 3D Structure Preview**

| 항목 | iPhone | Android | 일치 여부 |
|-----|--------|---------|----------|
| **제목 폰트** | `.title3` + `.semibold` | `.titleMedium` + `.SemiBold` | ⚠️ 크기 차이 |
| **3D 뷰어 높이** | **220pt 고정** | `weight(0.35f)` (35% 가변) | ❌ **불일치** |
| **배경색** | `.systemGray6.opacity(0.3)` | `.surface` | ❌ 불일치 |
| **모서리** | `cornerRadius(12)` | `.medium` (default) | ⚠️ 차이 가능 |
| **Padding** | `horizontal: 16` | `16.dp` | ✅ 일치 |

**수정 사항:**
1. **3D 뷰어 높이를 220dp 고정으로 변경** ⭐ 중요!
2. 배경색을 연한 회색으로 변경
3. cornerRadius를 12dp로 명시

---

### **4. 탭바 (하단 고정)**

| 항목 | iPhone | Android | 일치 여부 |
|-----|--------|---------|----------|
| **위치** | `.toolbar(.bottomBar)` | 탭 내부 (스크롤 영역) | ❌ **불일치** |
| **스크롤** | 가로 스크롤 가능 | InfoPanel 내부 | ❌ 불일치 |
| **선택 스타일** | 파란색 배경 + 흰색 텍스트 | 다름 | ❌ 불일치 |
| **탭 목록** | 7개 탭 | 7개 탭 | ✅ 일치 |
| **배경** | Toolbar 영역 | InfoPanel 내부 | ❌ 불일치 |

**수정 사항:**
1. **탭바를 화면 하단으로 이동** ⭐ 중요!
2. 가로 스크롤 가능하도록 변경
3. 선택 시 파란색 배경 + 흰색 텍스트
4. Scaffold의 bottomBar로 구현

---

### **5. 탭 컨텐츠 영역**

| 항목 | iPhone | Android | 일치 여부 |
|-----|--------|---------|----------|
| **스크롤** | 세로 스크롤 | 세로 스크롤 | ✅ 일치 |
| **Padding** | `horizontal: 16, top: 6` | InfoPanel 내부 | ⚠️ 차이 가능 |
| **배경** | `.systemBackground` | 동일 | ✅ 일치 |

---

### **6. 탭 로딩 상태**

| 항목 | iPhone | Android | 일치 여부 |
|-----|--------|---------|----------|
| **로딩 인디케이터** | ProgressView + 메시지 | 없음? | ❌ 미확인 |
| **메시지** | "Loading Overview..." | 없음? | ❌ 미확인 |

---

## 🎨 **Viewer Mode 비교**

### **1. 화면 구조**

| 항목 | iPhone | Android | 일치 여부 |
|-----|--------|---------|----------|
| **레이아웃** | 전체 화면 3D + 하단 패널 | ProteinViewerScreen | ⚠️ 미확인 |
| **하단 패널** | 슬라이드 업 패널 | 없음? | ❌ 미확인 |
| **패널 종류** | Rendering Style, Color Mode, More | 다름? | ❌ 미확인 |

**Android ProteinViewerScreen 확인 필요!**

---

## 🔧 **우선 수정 항목 (Info Mode)**

### **🔥 긴급 (구조적 변경)**

1. **3D 뷰어 높이를 220dp 고정**
   - 현재: `weight(0.35f)` (가변)
   - 변경: `height(220.dp)` (고정)

2. **탭바를 화면 하단으로 이동**
   - 현재: InfoPanel 내부
   - 변경: Scaffold `bottomBar`

3. **Focus/Clear 영역을 헤더 아래로 분리**
   - 현재: TopAppBar 내부
   - 변경: 별도 고정 영역

### **⚠️ 중요 (스타일 변경)**

4. **헤더 배경을 블러 효과로**
   - `.ultraThinMaterial` 효과

5. **3D 뷰어 배경색**
   - 연한 회색 (`.systemGray6.opacity(0.3)`)

6. **폰트 크기 조정**
   - 단백질 ID: `.title3` (더 크게)
   - 단백질 이름: `.callout`
   - 섹션 제목: `.title3`

### **📝 일반 (세부 조정)**

7. **Padding 값 정확히 맞추기**
   - 헤더: `horizontal: 16, top: 8, bottom: 12`
   - 3D 프리뷰: `horizontal: 16`
   - 탭 컨텐츠: `horizontal: 16, top: 6`

8. **Focus 인디케이터 추가**
   - 녹색 배지 + "Focused: Chain A"

9. **Clear 버튼 스타일**
   - 빨간색 배지 스타일로 변경

10. **탭 선택 스타일**
    - 파란색 배경 + 흰색 텍스트

---

## 📱 **안드로이드 코드 수정 계획**

### **Step 1: 헤더 구조 변경**
```kotlin
// TopAppBar 제거, 커스텀 헤더 구현
Column {
    // Custom Header (44.dp height)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)) // 블러 효과
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // 햄버거, 단백질 정보, Library, Viewer 버튼
    }
    
    // Focus/Clear 영역 (조건부)
    if (showFocusOrClear) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Focus 인디케이터, Spacer, Clear 버튼
        }
    }
}
```

### **Step 2: 3D 프리뷰 고정 높이**
```kotlin
Column(
    modifier = Modifier
        .fillMaxWidth()
        .height(220.dp) // 고정! (weight 제거)
        .padding(16.dp)
) {
    Text("3D Structure Preview", ...)
    Box(modifier = Modifier
        .fillMaxWidth()
        .weight(1f) // 제목 제외한 나머지
    ) {
        ProteinCanvas3DView(...)
    }
}
```

### **Step 3: 탭바 하단 이동**
```kotlin
Scaffold(
    bottomBar = {
        // 가로 스크롤 탭바
        ScrollableTabRow(
            selectedTabIndex = uiState.selectedInfoTab.ordinal,
            modifier = Modifier.fillMaxWidth()
        ) {
            InfoTab.values().forEach { tab ->
                Tab(
                    selected = uiState.selectedInfoTab == tab,
                    onClick = { viewModel.setInfoTab(tab) },
                    text = { Text(tab.displayName) }
                )
            }
        }
    }
) {
    // 3D 프리뷰 + 탭 컨텐츠
}
```

---

## ✅ **수정 완료 체크리스트**

- [ ] 헤더 구조 변경 (커스텀 구현)
- [ ] 3D 뷰어 220dp 고정 높이
- [ ] 탭바 하단 이동
- [ ] Focus/Clear 영역 분리
- [ ] 블러 효과 배경
- [ ] 폰트 크기 조정
- [ ] Padding 값 정확히 맞추기
- [ ] Focus 인디케이터 추가
- [ ] Clear 버튼 배지 스타일
- [ ] 탭 선택 스타일 (파란색 배경)
- [ ] Viewer Mode 확인 및 수정

**이제 이 계획대로 하나씩 수정하겠습니다!**

