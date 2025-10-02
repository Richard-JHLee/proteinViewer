# 📱 iPhone Info Mode & Viewer Mode 완전 UI 분석

## 🎯 **Info Mode UI 구조**

### **1. 헤더 (상단 고정)**

```
┌─────────────────────────────────────────────┐
│ ☰  (햄버거)    [1CRN]        📚  👁️     │  ← 44pt 높이
│              [Crambin]                       │
├─────────────────────────────────────────────┤
│ 🎯 Focused: Chain A        [Clear 버튼]     │  ← 조건부 표시
└─────────────────────────────────────────────┘
```

**구성 요소:**
- **좌측**: 햅버거 메뉴 (`line.3.horizontal`) - iPhone만
- **중앙**: 
  - 단백질 ID (`font: .title3`, `fontWeight: .semibold`)
  - 단백질 이름 (`font: .callout`, `color: .secondary`)
- **우측**: 
  - 📚 Library 버튼 (`books.vertical`)
  - 👁️ Viewer 모드 버튼 (`eye`)
- **하단 (조건부)**: 
  - Focus 인디케이터 (녹색 배지)
  - Clear 버튼 (빨간색 배지)

**스타일:**
- `padding(.horizontal, 16)`
- `padding(.top, 8)`
- `padding(.bottom, 12)`
- `background(.ultraThinMaterial)`

---

### **2. 3D Structure Preview (중간)**

```
┌─────────────────────────────────────────────┐
│ 3D Structure Preview                         │  ← .title3
├─────────────────────────────────────────────┤
│                                              │
│          [3D 뷰어 영역]                       │  ← 220pt 높이
│                                              │
└─────────────────────────────────────────────┘
```

**구성 요소:**
- **제목**: "3D Structure Preview" (`.title3`, `.semibold`)
- **3D 뷰어**: `ProteinSceneView`
  - `frame(height: 220)`
  - `padding(.horizontal, 16)`
  - `background(Color(.systemGray6).opacity(0.3))`
  - `cornerRadius(12)`
  - `isInfoMode: true` (작은 뷰어 모드)

---

### **3. 탭 컨텐츠 영역 (스크롤 가능)**

```
┌─────────────────────────────────────────────┐
│                                              │
│     [선택된 탭의 컨텐츠 표시]                   │
│     - Overview                               │
│     - Chains                                 │
│     - Residues                               │
│     - Ligands                                │
│     - Pockets                                │
│     - Sequence                               │
│     - Annotations                            │
│                                              │
└─────────────────────────────────────────────┘
```

**스타일:**
- `ScrollView` (세로 스크롤)
- `padding(.horizontal, 16)`
- `padding(.top, 6)`
- `background(Color(.systemBackground))`

---

### **4. 하단 탭바 (하단 고정)**

```
┌─────────────────────────────────────────────┐
│ [Overview] [Chains] [Residues] [Ligands]    │  ← 가로 스크롤
│ [Pockets] [Sequence] [Annotations]          │
└─────────────────────────────────────────────┘
```

**구성 요소:**
- `ScrollView(.horizontal)` - 탭이 많을 경우 스크롤
- `ToolbarItemGroup(placement: .bottomBar)`
- 각 탭 버튼:
  - 선택: 파란색 배경 + 흰색 텍스트
  - 미선택: 투명 배경 + 기본 텍스트

**탭 목록:**
1. **Overview** - 단백질 기본 정보
2. **Chains** - 체인 목록
3. **Residues** - 잔기 목록
4. **Ligands** - 리간드 목록
5. **Pockets** - 포켓 목록
6. **Sequence** - 아미노산 서열
7. **Annotations** - 주석 정보

---

## 🎨 **Viewer Mode UI 구조**

### **1. 전체 화면 3D 뷰어**

```
┌─────────────────────────────────────────────┐
│                                              │
│                                              │
│          [전체 화면 3D 뷰어]                   │
│                                              │
│                                              │
└─────────────────────────────────────────────┘
```

**구성 요소:**
- `ProteinSceneView`
  - `.frame(maxWidth: .infinity, maxHeight: .infinity)`
  - `isInfoMode: false` (전체 화면 모드)
  - `autoRotate: true` (자동 회전)

---

### **2. 하단 컨트롤 패널 (슬라이드 업)**

```
┌─────────────────────────────────────────────┐
│ [Rendering Style]  [Color Mode]  [More]     │  ← 패널 선택
├─────────────────────────────────────────────┤
│                                              │
│     [선택된 패널 내용]                         │
│     - Ribbon / Ball & Stick / Space Fill    │
│     - Element / Chain / Uniform             │
│                                              │
└─────────────────────────────────────────────┘
```

**패널 종류:**
1. **Rendering Style**: Ribbon, Ball & Stick, Space Fill, Cartoon
2. **Color Mode**: Element, Chain, Uniform, Secondary Structure
3. **More**: 추가 옵션

---

### **3. 우상단 버튼들**

```
┌─────────────────────────────────────────────┐
│                              [←] [🔄] [⚙️]    │
│                                              │
└─────────────────────────────────────────────┘
```

**버튼:**
- **← (Back)**: Info Mode로 돌아가기
- **🔄 (Reset)**: 카메라 리셋
- **⚙️ (Settings)**: 설정

---

## 📊 **Info Mode 탭별 상세 내용**

### **Overview 탭**

**표시 항목:**
1. **Basic Information**
   - PDB ID
   - Name
   - Classification
   - Organism
   - Deposition Date

2. **Structure Details**
   - Experimental Method
   - Resolution
   - R-factor
   - Space Group

3. **Statistics**
   - Total Atoms
   - Chains
   - Residues
   - Ligands
   - Water Molecules

4. **Molecular Weight**
   - Total Weight
   - Average Weight per Chain

### **Chains 탭**

**표시 항목:**
- Chain ID (A, B, C...)
- Residue Count
- Sequence Length
- Highlight 버튼 (체인 강조)
- Focus 버튼 (체인 포커스)

**인터랙션:**
- Highlight: 선택한 체인만 색상 강조
- Focus: 카메라가 선택한 체인으로 줌인

### **Residues 탭**

**표시 항목:**
- Residue Number
- Residue Name (3-letter code)
- Chain ID
- Secondary Structure Type
- Highlight 버튼

### **Ligands 탭**

**표시 항목:**
- Ligand Name
- Chemical Formula
- Molecular Weight
- Chain ID
- Residue Number
- Highlight 버튼

### **Pockets 탭**

**표시 항목:**
- Pocket ID
- Volume (Å³)
- Surface Area (Å²)
- Residues in Pocket
- Highlight 버튼

### **Sequence 탭**

**표시 항목:**
- Chain별 아미노산 서열
- 1-letter code로 표시
- 10개씩 블록으로 구분
- 컬러 코딩 (아미노산 타입별)

### **Annotations 탭**

**표시 항목:**
- HEADER 정보
- TITLE
- COMPND (Compound)
- SOURCE (Organism)
- KEYWDS (Keywords)
- EXPDTA (Experimental Data)
- AUTHOR
- REVDAT (Revision Date)
- JRNL (Journal Citation)

---

## 🎨 **UI 디자인 가이드**

### **색상 팔레트:**
- **Primary**: `.blue` (시스템 파란색)
- **Background**: `.systemBackground` (흰색/검은색)
- **Secondary Background**: `.systemGray6` (연한 회색)
- **Highlight**: `.green` (포커스 인디케이터)
- **Error/Clear**: `.red` (Clear 버튼)

### **타이포그래피:**
- **Title**: `.title3`, `.semibold`
- **Headline**: `.headline`
- **Body**: `.callout`, `.footnote`
- **Caption**: `.caption`

### **간격:**
- **Horizontal Padding**: 16pt
- **Vertical Spacing**: 12pt
- **Button Height**: 44pt (최소 터치 영역)
- **Corner Radius**: 12pt (카드), 16pt (배지)

### **애니메이션:**
- **Duration**: 0.3초 (일반)
- **Spring**: `response: 0.3` (탄성 효과)
- **Haptic**: `.light`, `.medium` (햅틱 피드백)

---

## ✅ **Info Mode vs Viewer Mode 차이점**

| 항목 | Info Mode | Viewer Mode |
|-----|----------|-------------|
| **화면 구성** | 헤더 + 3D 프리뷰 + 탭 + 하단 탭바 | 전체 화면 3D + 하단 패널 |
| **3D 뷰어 크기** | 220pt 고정 | 전체 화면 |
| **자동 회전** | 꺼짐 | 켜짐 (옵션) |
| **인터랙션** | 제한적 (줌 인/아웃만) | 전체 제스처 |
| **탭 표시** | 하단 고정 탭바 | 없음 |
| **컨트롤** | 탭별 정보 + 하이라이트 버튼 | Rendering Style + Color Mode |
| **목적** | 정보 열람 + 선택적 강조 | 3D 구조 자세히 보기 |

---

## 🔄 **모드 전환 플로우**

### **Info Mode → Viewer Mode**
1. 우상단 👁️ 버튼 클릭
2. 전체 화면으로 전환 (애니메이션)
3. 하단 컨트롤 패널 표시
4. 자동 회전 시작 (옵션)

### **Viewer Mode → Info Mode**
1. 우상단 ← 버튼 클릭
2. Info Mode로 돌아가기 (애니메이션)
3. 3D 프리뷰 + 탭 UI 표시
4. 마지막 선택 탭 유지

---

## 📱 **반응형 디자인**

### **iPhone (Compact)**
- 햄버거 메뉴 표시
- 탭바 가로 스크롤
- 3D 프리뷰 220pt

### **iPad (Regular)**
- 햄버거 메뉴 숨김 (사이드바 사용)
- 탭바 가로 스크롤 (선택적)
- 3D 프리뷰 더 큰 크기 (가능)

---

## 🎯 **핵심 포인트**

1. **고정 헤더**: 항상 상단에 단백질 정보 표시
2. **3D 프리뷰**: 220pt 고정 높이, 둥근 모서리
3. **하단 탭바**: 가로 스크롤, 선택 시 파란색 배경
4. **탭 컨텐츠**: 스크롤 가능, 16pt 패딩
5. **Viewer Mode**: 전체 화면, 하단 슬라이드 업 패널
6. **Focus/Highlight**: 녹색/빨간색 배지, Clear 버튼
7. **햅틱 피드백**: 모든 버튼 클릭 시

**이제 안드로이드 UI를 이 구조에 정확히 맞춰 수정하겠습니다!**

