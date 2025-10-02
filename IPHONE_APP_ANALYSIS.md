# 📱 iPhone App 완전 분석 문서

## 🎯 **1단계: 앱 시작 시 초기 화면**

### **UI 구조:**
```
iPhoneContentView (body)
└── NavigationView
    └── ZStack
        └── if (structure == nil)
            └── VStack
                ├── if (isLoading)
                │   ├── ProgressView (파란색, 1.2배 크기)
                │   ├── Text("Loading protein structure...")
                │   └── Text(loadingProgress) // "Downloading PDB file..." 등
                └── else
                    ├── Image(systemName: "atom") // 60pt, 파란색
                    ├── Text("Loading...") // 큰 제목, 굵게
                    └── Text("Loading default protein structure...")
```

### **상태:**
- `viewModel.structure = nil`
- `viewModel.isLoading = false` (최초)
- `showingProteinLibrary = false`

### **자동 실행:**
```swift
.onAppear {
    if viewModel.structure == nil {
        viewModel.loadDefaultProtein()  // 자동 호출!
    }
}
```

---

## 🔄 **2단계: 기본 단백질 로딩 (API 호출)**

### **함수:** `loadDefaultProtein()`
**파일:** `ProteinViewModel.swift`

### **API 호출 순서:**

#### **2-1. 상태 설정**
```swift
isLoading = true
loadingProgress = "Loading default protein..."
error = nil
```

#### **2-2. PDB 파일 다운로드**
```swift
// API: https://files.rcsb.org/download/1CRN.pdb
let defaultPdbId = "1CRN"  // 기본 단백질
let url = URL(string: "https://files.rcsb.org/download/\(defaultPdbId).pdb")!

// HTTP 요청
var request = URLRequest(url: url)
request.timeoutInterval = 30.0
request.setValue("ProteinApp/1.0", forHTTPHeaderField: "User-Agent")

let (data, response) = try await URLSession.shared.data(for: request)
```

**UI 업데이트:** `loadingProgress = "Downloading PDB file..."`

#### **2-3. PDB 파일 파싱**
```swift
let pdbText = String(decoding: data, as: UTF8.self)
let loadedStructure = try PDBParser.parse(pdbText: pdbText)
```

**UI 업데이트:** `loadingProgress = "Parsing PDB structure..."`

#### **2-4. 단백질 이름 조회**
```swift
// API: https://data.rcsb.org/rest/v1/core/entry/1CRN
let actualProteinName = await fetchProteinNameFromPDB(pdbId: defaultPdbId)

private func fetchProteinNameFromPDB(pdbId: String) async -> String {
    let url = "https://data.rcsb.org/rest/v1/core/entry/\(pdbId)"
    // JSON에서 struct.title 추출
    // "CRYSTAL STRUCTURE OF" 등의 접두사 제거
    return cleanedTitle
}
```

#### **2-5. 상태 업데이트**
```swift
self.structure = loadedStructure
self.currentProteinId = defaultPdbId  // "1CRN"
self.currentProteinName = actualProteinName  // "Crambin"
self.isLoading = false
self.loadingProgress = ""
```

---

## 🖥️ **3단계: 메인 화면 UI (단백질 로딩 완료 후)**

### **UI 구조:**
```
iPhoneContentView (body)
└── NavigationView
    └── ZStack
        └── if (structure != nil)  // ✅ 이제 true
            └── ProteinSceneContainer
                ├── 헤더 (상단)
                │   ├── 햄버거 메뉴 (좌상단)
                │   ├── 단백질 정보 (중앙)
                │   │   ├── "1CRN" (제목)
                │   │   └── "Crambin" (부제)
                │   ├── 📚 Protein Library 버튼 (우상단)
                │   └── 👁️ Viewer 모드 버튼 (우상단)
                ├── 탭 메뉴 (헤더 아래)
                │   ├── Overview
                │   ├── Chains
                │   ├── Residues
                │   ├── Ligands
                │   ├── Pockets
                │   ├── Sequence
                │   └── Annotations
                ├── 3D 뷰어 (중앙, 50% 높이)
                │   └── ProteinSceneView (SceneKit)
                └── 정보 패널 (하단, 50% 높이)
                    └── 선택된 탭 내용 표시
```

### **헤더 버튼 위치 (코드 확인):**
```swift
// 파일: ProteinSceneView.swift, line 1175-1241
HStack {
    // 1. 햄버거 메뉴 (좌상단) - iPhone만
    if horizontalSizeClass != .regular {
        Button(action: { showingSideMenu = true }) {
            Image(systemName: "line.3.horizontal")
        }
    }
    
    Spacer()
    
    // 2. 단백질 정보 (중앙)
    VStack(spacing: 4) {
        Text(proteinId)  // "1CRN"
        Text(proteinName)  // "Crambin"
    }
    
    Spacer()
    
    // 3. 📚 Protein Library 버튼 (우상단)
    if let onProteinLibraryTap = onProteinLibraryTap {
        Button(action: {
            // 하이라이트 초기화
            highlightedChains.removeAll()
            highlightedLigands.removeAll()
            highlightedPockets.removeAll()
            focusedElement = nil
            
            onProteinLibraryTap()  // ✅ 이 콜백 호출!
        }) {
            Image(systemName: "books.vertical")
        }
    }
    
    // 4. 👁️ Viewer 모드 버튼 (우상단)
    Button(action: { viewMode = .viewer }) {
        Image(systemName: "eye")
    }
}
```

---

## 📚 **4단계: Protein Library 버튼 클릭 → 화면 전환**

### **클릭 이벤트:**
```swift
// 파일: iPhoneContentView.swift, line 14-19
ProteinSceneContainer(
    structure: structure,
    proteinId: viewModel.currentProteinId,
    proteinName: viewModel.currentProteinName,
    onProteinLibraryTap: {
        showingProteinLibrary = true  // ✅ 상태 변경!
    }
)
```

### **화면 전환 방식:**
```swift
// 파일: iPhoneContentView.swift, line 108-127
.fullScreenCover(isPresented: $showingProteinLibrary) {
    NavigationView {
        ProteinLibraryView { selectedProteinId in
            // 단백질 선택 시 콜백
            showingProteinLibrary = false
            is3DStructureLoading = true
            structureLoadingProgress = "Loading 3D structure for \(selectedProteinId)..."
            viewModel.loadSelectedProtein(selectedProteinId)
            
            Task {
                try? await Task.sleep(nanoseconds: 3_000_000_000)  // 3초
                await MainActor.run {
                    is3DStructureLoading = false
                    structureLoadingProgress = ""
                }
            }
        }
    }
    .navigationViewStyle(.stack)
}
```

**전환 방식:** `fullScreenCover` = **전체 화면 모달**

---

## 🗂️ **5단계: Protein Library UI (카테고리 그리드)**

### **UI 구조:**
```
ProteinLibraryView (body)
└── VStack
    ├── 헤더
    │   ├── "Protein Library" (제목)
    │   └── ❌ 닫기 버튼 (우상단)
    ├── 검색 바
    │   └── TextField("Search proteins...")
    ├── if (searchText.isEmpty && selectedCategory == nil)  // ✅ 초기 상태
    │   └── VStack
    │       ├── 헤더 텍스트
    │       │   ├── "Choose a Category" (제목)
    │       │   └── "Explore proteins by their biological function" (부제)
    │       └── LazyVGrid (2열 그리드)
    │           └── ForEach(ProteinCategory.allCases) { category in
    │               CategorySelectionCard(
    │                   category: category,
    │                   proteinCount: categoryProteinCounts[category]
    │               )
    │           }
    └── .task {
        // 초기 로딩
        if database.proteins.isEmpty {
            await database.loadProteins()  // ✅ API 호출!
        }
    }
```

### **초기 상태:**
- `selectedCategory = nil`
- `showingFavoritesOnly = false`
- `searchText = ""`
- `database.proteins = []`

### **자동 API 호출:**
```swift
// 파일: ProteinLibrary.swift, line 4174-4179
.task {
    if database.proteins.isEmpty {
        print("🚀 Protein Library 초기 데이터 로딩 시작...")
        await database.loadProteins()  // ✅ 모든 카테고리 로딩!
        print("✅ 초기 로딩 완료: \(database.proteins.count)개 단백질")
    }
}
```

### **CategorySelectionCard UI:**
```
Card (142x180pt)
├── Circle (60x60pt, 카테고리 색상 10% 투명도 배경)
│   └── Image(systemName: category.icon) (카테고리 색상)
├── Text(category.rawValue) // "Enzymes" 등
├── Text("\(proteinCount) proteins") // "45000 proteins" 등
└── Text(category.description) // 3줄 제한
```

---

## 🔍 **6단계: 카테고리 선택 → 단백질 리스트**

### **클릭 이벤트:**
```swift
// 파일: ProteinLibrary.swift, line 4036-4043
CategorySelectionCard(...) {
    provideHapticFeedback(style: .medium)  // 햅틱 피드백
    
    withAnimation(.spring(response: 0.3)) {
        selectedCategory = category  // ✅ 카테고리 설정!
        resetPagination()
    }
}
```

### **API 호출:**
```swift
// 파일: ProteinLibrary.swift, line 4147-4172
.onChange(of: selectedCategory) { newCategory in
    guard let newCategory = newCategory else { return }
    
    Task {
        await database.loadProteins(for: newCategory)  // ✅ API 호출!
    }
}
```

### **API 호출 로직:**
```swift
// 파일: ProteinLibrary.swift, line 2990-3198
func loadProteins(for category: ProteinCategory? = nil, refresh: Bool = false) async {
    await MainActor.run {
        isLoading = true
        errorMessage = nil
    }
    
    if let category = category {
        // 1. 샘플 데이터 먼저 로드 (즉시 표시)
        let samples = apiService.getSampleProteins(for: category)
        proteins = samples
        
        // 2. API에서 실제 데이터 로드 (30개씩 페이지네이션)
        let apiProteins = await apiService.fetchProteins(
            for: category,
            limit: 30,
            skip: 0
        )
        
        // 3. 샘플 + API 데이터 병합 (중복 제거)
        let combined = mergeSamplesWithAPIData(samples, apiProteins)
        proteins = combined
    }
    
    await MainActor.run {
        isLoading = false
    }
}
```

### **UI 전환:**
```swift
// 파일: ProteinLibrary.swift, line 4047-4059
if selectedCategory != nil {
    VStack(spacing: 16) {
        // 선택된 카테고리 헤더
        SelectedCategoryHeader(
            category: selectedCategory!,
            proteinCount: allFilteredProteins.count
        ) {
            // 뒤로 가기
            withAnimation(.spring(response: 0.3)) {
                selectedCategory = nil  // ✅ 카테고리 그리드로 돌아감
            }
        }
        
        // 단백질 리스트
        ForEach(displayedProteins) { protein in
            ProteinRowCard(...)
        }
        
        // "More Data" 버튼 (30개 더 로드)
        if hasMoreData {
            Button("More Data") {
                currentPage += 1
                // 다음 30개 로드...
            }
        }
    }
}
```

---

## 🧬 **7단계: 단백질 선택 → 3D 로딩 → 메인 화면**

### **클릭 이벤트:**
```swift
// 파일: ProteinLibrary.swift, line 4066-4086
ProteinRowCard(
    protein: protein,
    isFavorite: database.favorites.contains(protein.id)
) {
    provideHapticFeedback(style: .medium)
    
    // 상세보기 표시
    selectedProtein = protein
    showingInfoSheet = true  // ✅ 상세 정보 시트 표시
}
```

### **상세 정보 시트 → "View 3D Structure" 버튼:**
```swift
// 상세 정보 시트에서 "View 3D Structure" 버튼 클릭 시
Button("View 3D Structure") {
    // 콜백 호출
    onProteinSelected(protein.id)  // ✅ "1CRN" 등 전달
}
```

### **콜백 처리 (iPhoneContentView):**
```swift
// 파일: iPhoneContentView.swift, line 111-123
ProteinLibraryView { selectedProteinId in
    showingProteinLibrary = false  // ✅ 1. 라이브러리 닫기
    is3DStructureLoading = true    // ✅ 2. 로딩 오버레이 표시
    structureLoadingProgress = "Loading 3D structure for \(selectedProteinId)..."
    
    viewModel.loadSelectedProtein(selectedProteinId)  // ✅ 3. API 호출!
    
    Task {
        try? await Task.sleep(nanoseconds: 3_000_000_000)  // 3초 대기
        await MainActor.run {
            is3DStructureLoading = false
            structureLoadingProgress = ""
        }
    }
}
```

### **API 호출 (loadSelectedProtein):**
```swift
// 파일: ProteinViewModel.swift, line 107-175
func loadSelectedProtein(_ pdbId: String) {
    isLoading = true
    loadingProgress = "Initializing..."
    
    Task {
        // 1. PDB ID 정규화 (대문자, 4자리)
        let formattedPdbId = pdbId.uppercased()
        
        // 2. PDB 파일 다운로드
        let url = URL(string: "https://files.rcsb.org/download/\(formattedPdbId).pdb")!
        let (data, response) = try await URLSession.shared.data(for: request)
        
        // 3. 파싱
        let pdbText = String(decoding: data, as: UTF8.self)
        let loadedStructure = try PDBParser.parse(pdbText: pdbText)
        
        // 4. 이름 조회
        let actualProteinName = await fetchProteinNameFromPDB(pdbId: formattedPdbId)
        
        // 5. 상태 업데이트
        self.structure = loadedStructure
        self.currentProteinId = formattedPdbId
        self.currentProteinName = actualProteinName
        self.isLoading = false
    }
}
```

### **결과:**
- `viewModel.structure` 업데이트됨
- `iPhoneContentView`가 `ProteinSceneContainer`로 전환됨 (3단계 UI)
- 사용자는 새로운 단백질의 3D 구조 볼 수 있음

---

## 📊 **API 엔드포인트 정리**

| API | 용도 | 예시 |
|-----|------|------|
| `https://files.rcsb.org/download/{pdbId}.pdb` | PDB 파일 다운로드 | `1CRN.pdb` |
| `https://data.rcsb.org/rest/v1/core/entry/{pdbId}` | 단백질 이름 조회 | `1CRN` → "Crambin" |
| `https://search.rcsb.org/rcsbsearch/v2/query` | 카테고리별 단백질 검색 | `category=Enzymes` |

---

## 🎨 **UI 컴포넌트 정리**

### **카테고리 카드 (CategorySelectionCard):**
- **크기:** 142x180pt
- **배경:** 흰색, 모서리 둥글게 12pt
- **그림자:** 반경 4pt
- **내용:**
  - 아이콘 (60x60pt 원형 배경)
  - 카테고리 이름 (헤드라인)
  - 단백질 개수 (작은 텍스트)
  - 설명 (3줄 제한, 작은 텍스트)

### **단백질 로우 카드 (ProteinRowCard):**
- **배경:** 흰색, 모서리 둥글게 12pt
- **패딩:** 16pt
- **내용:**
  - 단백질 ID (굵게)
  - 단백질 이름 (부제)
  - 해상도 (작은 텍스트)
  - 실험 방법 (작은 텍스트)
  - 즐겨찾기 버튼 (⭐)

---

## 🔄 **상태 관리 정리**

### **iPhoneContentView:**
- `@ObservedObject var viewModel: ProteinViewModel`
- `@State private var showingProteinLibrary: Bool = false`
- `@State private var is3DStructureLoading = false`
- `@State private var structureLoadingProgress = ""`

### **ProteinViewModel:**
- `@Published var structure: PDBStructure?`
- `@Published var isLoading = false`
- `@Published var loadingProgress: String = ""`
- `@Published var error: String?`
- `@Published var currentProteinId: String = ""`
- `@Published var currentProteinName: String = ""`

### **ProteinLibraryView:**
- `@StateObject private var database = ProteinDatabase()`
- `@State private var searchText = ""`
- `@State private var selectedCategory: ProteinCategory? = nil`
- `@State private var showingFavoritesOnly = false`
- `@State private var currentPage = 1`

### **ProteinDatabase:**
- `@Published var proteins: [ProteinInfo] = []`
- `@Published var isLoading = false`
- `@Published var errorMessage: String?`
- `@Published var favorites: Set<String> = []`

---

## ✅ **핵심 포인트 정리**

1. **앱 시작 시**: 자동으로 기본 단백질(`1CRN`) 로드
2. **메인 화면**: 헤더 우상단에 📚 버튼 (Protein Library)
3. **Protein Library**: `fullScreenCover`로 전체 화면 모달 표시
4. **초기 상태**: `selectedCategory = nil` → 카테고리 그리드 바로 표시
5. **카테고리 선택**: API 호출 → 30개 단백질 리스트 표시
6. **단백질 선택**: 상세 정보 시트 → "View 3D Structure" → API 호출 → 메인 화면 업데이트
7. **페이지네이션**: "More Data" 버튼 → 30개씩 추가 로드

**이제 이 분석을 바탕으로 안드로이드에 동일하게 구현합니다!**

