핵심 기능

기본 구조: SwiftUI가 iPhone 전용 전체 화면과 iPad 사이드바 레이아웃을 분기해 SceneKit 기반 뷰어와 부가 화면을 연결함 (ProteinApp/Sources/App/ContentView.swift:47, ProteinApp/Sources/App/iPhoneContentView.swift:10, ProteinApp/Sources/App/iPadContentView.swift:4).
3D 뷰어: SceneKit SCNView를 UIViewRepresentable로 감싸 LOD 캐시, 자동 회전, 체인·리간드·포켓 하이라이트를 제공 (ProteinApp/Sources/App/ProteinSceneView.swift:13, ProteinApp/Sources/App/ProteinSceneView.swift:2901).
데이터 파서: mmCIF/PDB 텍스트를 직접 파싱하고 결합·주석·통계 정보를 계산하는 도메인 계층을 포함 (ProteinApp/Sources/App/PDB.swift:1).
라이브러리: PDBAPIService가 카테고리별 고급 검색·GraphQL 호출·페이지네이션을 담당하고 ProteinLibraryView가 필터·즐겨찾기·로드 모달을 제공 (ProteinApp/Sources/App/ProteinLibrary.swift:396, ProteinApp/Sources/App/ProteinLibrary.swift:3523).
부가 정보: PDB·UniProt 통합 상세 정보와 연구 현황(논문/임상시험) 카드를 각각 비동기로 로드 (ProteinApp/Sources/App/AdditionalInfoService.swift:166, ProteinApp/Sources/App/AdditionalInfoView.swift:10, ProteinApp/Sources/App/ResearchStatusModels.swift:43, ProteinApp/Sources/App/ResearchStatusView.swift:4).
사용자 환경: 즐겨찾기 로컬 저장과 간이 다국어 헬퍼로 사용자별 선호를 유지 (ProteinApp/Sources/App/FavoritesManager.swift:1, ProteinApp/Sources/App/LanguageHelper.swift:1).
Android 적용성

3D 렌더링은 SceneKit 전용 API(LOD 캐시, SCNTransaction, SCNAction 등)에 의존하므로 Android에서는 Filament/Sceneform/Rajawali 같은 엔진으로 재구현하고 하이라이트·재질·LOD 로직을 수동 이식해야 함 (ProteinApp/Sources/App/ProteinSceneView.swift:13, ProteinApp/Sources/App/ProteinSceneView.swift:2901).
UI는 SwiftUI NavigationView + UIViewRepresentable 구조라 Jetpack Compose의 NavHost와 AndroidView, 태블릿 대응에서는 adaptive layout을 새로 설계해야 함 (ProteinApp/Sources/App/iPhoneContentView.swift:10, ProteinApp/Sources/App/iPadContentView.swift:4).
네트워크/동시성은 URLSession async/await와 반복 재시도 로직을 Kotlin 코루틴 + Retrofit/OkHttp로 재작성하고, PubMed·ClinicalTrials API의 대기/백오프 처리를 suspend 기반으로 옮겨야 함 (ProteinApp/Sources/App/ProteinViewModel.swift:17, ProteinApp/Sources/App/ResearchStatusModels.swift:86).
PDB 파싱 및 수학 처리는 Swift simd와 컬렉션 확장에 기대고 있어 Kotlin 데이터 클래스와 벡터/행렬 라이브러리 또는 직접 구현으로 치환해야 함 (ProteinApp/Sources/App/PDB.swift:1).
로컬 상태와 시스템 통합(UserDefaults, Haptics, Orientation Lock)은 Android의 DataStore/SharedPreferences, Vibrator, Activity orientation 설정으로 대체 필요 (ProteinApp/Sources/App/FavoritesManager.swift:1, ProteinApp/Sources/App/iPhoneContentView.swift:128).
수동 다국어 분기와 태블릿 UI 차별화는 Android의 string 리소스와 qualifiers, WindowSizeClass 등을 활용해 다시 구성해야 함 (ProteinApp/Sources/App/LanguageHelper.swift:1, ProteinApp/Sources/App/iPadContentView.swift:4).
권장 전략

Kotlin 모듈로 PDB 구조·도메인 모델을 우선 이식하고 단위 테스트로 파서 정확도를 검증 (ProteinApp/Sources/App/PDB.swift:1).
선택한 Android 3D 엔진으로 렌더링 프로토타입을 만들고 체인 색상·하이라이트를 단계적으로 구현, LOD 캐시는 성능 측정 후 도입 (ProteinApp/Sources/App/ProteinSceneView.swift:40).
Jetpack Compose에서 뷰어·라이브러리·정보 화면 네비게이션을 구성하고 iPad 전용 UX는 Android 태블릿 레이아웃으로 재설계 (ProteinApp/Sources/App/ContentView.swift:54, ProteinApp/Sources/App/ProteinLibrary.swift:3523).
Retrofit/OkHttp + 코루틴으로 PDB·UniProt·PubMed 호출을 묶고 캐시/백오프 정책을 정의해 API 사용량 제한을 대비 (ProteinApp/Sources/App/ProteinViewModel.swift:39, ProteinApp/Sources/App/ResearchStatusModels.swift:114).
구체적으로 안드로이드 구현을 위해 단계별로 어떻게 개발해야 하는지 작성해 달라


Android 이식 로드맵

사전 설계

Kotlin 멀티모듈 구조(도메인·데이터·UI) 정의, SceneKit 의존 기능을 Android 3D 엔진(예: Filament+Sceneform V1)으로 대체할 기획 수립
PDB/UniProt/PubMed 등 네트워크 사용량·레이트리밋 분석, OkHttp+Retrofit+Kotlinx.serialization 기반 API 설계 문서화
디자인 시스템(Compose Material3)과 태블릿 대응 레이아웃 전략을 UX팀과 합의
도메인/데이터 계층 포팅

PDBStructure·Atom·Bond 등 핵심 모델을 Kotlin data class로 변환하고 단위 테스트 준비
Swift PDBParser 로직을 Kotlin으로 이식하며 simd 연산을 Kotlinx-math/Vector API로 대체; 대용량 파일 파싱 스트림 처리 도입
캐시·즐겨찾기 로직을 DataStore Preferences로 구현, 멀티 스레드 안전성 검증
Retrofit 인터페이스 작성: PDB 다운로드, GraphQL, UniProt, PubMed, ClinicalTrials; 응답 DTO와 매퍼 구성
렌더링 엔진 구축

선택 엔진에서 기본 Scene 설정 → 카메라 컨트롤, 제스처(회전/핀치/슬라이스) 구현
LOD 전략: 구·실린더 메시를 재활용하도록 VertexCache 구성, 색상별 머티리얼 풀 관리로 SceneKit GeometryCache 대응
체인/리간드/포켓 하이라이트, 리본/카툰 표현 알고리즘 작성; 셰이더나 포스트 프로세싱 필요 시 GLSL/Material Definition 설계
자동회전, 투명도, 원자 크기 등 옵션을 Scene graph 노드 속성으로 노출하고 Compose 상태와 바인딩
UI 레이어(Compose)

Navigation Graph 설계: Home → Viewer → BottomSheet/Dialog(Info, Quiz, Search) 구조, 태블릿은 NavigationRail+TwoPane 적용
Viewer 화면은 AndroidView로 3D 뷰 삽입, ControlBar/SecondaryBar를 Compose로 재현
Protein Library: LazyColumn/Grid, 즐겨찾기 토글, 무한 스크롤; 뷰모델은 Paging 3 도입 고려
Additional Info · Research Status 시트를 Compose Card로 구현, 상태 흐름(StateFlow)와 스낵바 에러 처리 연결
다국어: string 리소스/플러럴스 작성, LocaleConfig + Compose 지원 확대
동시성·데이터 관리

ViewModel + StateFlow + Hilt DI 세팅, 각 기능별 UseCase 정의 (기본 단백질 로드, 선택 단백질 로드 등)
네트워크 재시도/백오프 정책, 다운로드 진행률 StateFlow 노출, OkHttp Cache로 기본 캐싱
PDB 파일 오프라인 캐시(Room or 파일 시스템) 및 구조 해석 결과 메모리 캐싱 전략 수립
검증 및 최적화

단위 테스트: 파서, 매퍼, ViewModel; Fake API로 유닛테스트 작성
계측 테스트: Compose UI, 3D 뷰 제스처 Espresso 확장 검토
프로파일링: 렌더링 FPS, 메모리, PDB 50k atom 로딩 시간 측정 → 필요시 LOD/가비지 전략 조정
접근성/컬러 대비, 다국어 UI, Orientation, 다중 창 모드 점검
배포 준비

Beta 빌드 위한 CI 파이프라인 구성(Gradle + GitHub Actions/Bitrise)
README/프로덕트 문서에 Android 전용 빌드·디버그 지침 추가
QA 플레잉리스트와 사용자 가이드(Android) 작성, 앱 스토어 소재 업데이트
실제 소스를 가지고 아이폰 처럼 개발하기 위해 어떻게 개발해야 하는지 단계별로 수행방법을 작성해 달라


이식 단계

환경 & 모듈 설계

iOS 구조를 기준으로 기능 영역을 도메인/데이터/UI 모듈로 나누고 Android 프로젝트 스켈레톤을 준비. ProteinApp/Sources/App/ContentView.swift:47에서 확인되는 iPhone vs iPad 분기 요구를 Android WindowSizeClass 대응 설계에 반영.
Hilt, Retrofit, Kotlinx.serialization, Jetpack Compose, Filament(또는 Sceneform v1) 의존성을 Gradle에 추가.
도메인 계층 포팅

PDBStructure와 연관 타입을 Kotlin data class로 변환하고 Swift의 계산 프로퍼티를 함수로 치환 (ProteinApp/Sources/App/PDB.swift:24).
PDBParser.parse 전체 흐름(헤더 파싱, 2차 구조 맵, 본드 생성 등)을 Kotlin으로 옮겨 단위 테스트 작성 (ProteinApp/Sources/App/PDB.swift:99).
ProteinViewModel의 기본/선택 단백질 로딩 로직을 UseCase 및 ViewModel 조합으로 재구성 (ProteinApp/Sources/App/ProteinViewModel.swift:17).
네트워크/데이터 소스 구현

ProteinViewModel이 호출하는 PDB 다운로드, 이름 조회 REST를 Retrofit API로 정의 (ProteinApp/Sources/App/ProteinViewModel.swift:213).
AdditionalInfoService.fetchAdditionalInfo와 동일한 PDB+UniProt 결합을 Repository 패턴으로 구현 (ProteinApp/Sources/App/AdditionalInfoService.swift:166).
ResearchStatusService의 PubMed/ClinicalTrials 호출과 백오프 로직을 코루틴으로 옮기고 fallback 처리 유지 (ProteinApp/Sources/App/ResearchStatusModels.swift:86).
3D 렌더링 엔진 구축

SceneKit 기반 기능(LOD 캐시, 재질 풀)을 Filament/Sceneform에서 재현: GeometryCache의 구/실린더 LOD 전략을 재사용 가능하게 설계 (ProteinApp/Sources/App/ProteinSceneView.swift:13).
ProteinSceneView 제공 속성(스타일, 색상 모드, 하이라이트, 자동 회전)을 Android 엔진 노드에 매핑하고, 체인별 노드 이름 규칙을 유지 (ProteinApp/Sources/App/ProteinSceneView.swift:2901).
리본/카툰 생성 로직을 커브/메시 생성으로 이식하고, 하이라이트 전환 시 트랜잭션 효과를 애니메이션으로 대체 (ProteinApp/Sources/App/ProteinSceneView.swift:3074).
UI/네비게이션 이식 (Jetpack Compose)

ContentView의 기기 분기를 Compose Navigation + WindowSizeClass로 구현, 태블릿은 NavigationRail 두 패널을 구성 (ProteinApp/Sources/App/ContentView.swift:54, ProteinApp/Sources/App/iPadContentView.swift:4).
iPhoneContentView의 상태/로딩/에러/풀스크린 커버를 Compose Scaffold와 ModalBottomSheet로 변환 (ProteinApp/Sources/App/iPhoneContentView.swift:10).
ProteinSceneContainer UI 제어 버튼과 하단 바를 Compose로 재작성, 3D 뷰는 AndroidView를 통해 삽입.
ProteinLibraryView의 검색·카테고리·즐겨찾기·무한 스크롤을 Paging 3 + LazyColumn/Grid로 구성 (ProteinApp/Sources/App/ProteinLibrary.swift:3523).
AdditionalInfoView, ResearchStatusView의 카드/에러/로딩 표현을 Compose Card 및 Snackbar로 재현 (ProteinApp/Sources/App/AdditionalInfoView.swift:10, ProteinApp/Sources/App/ResearchStatusView.swift:4).
다국어는 LanguageHelper 조건을 Android string 리소스 다국어로 대체 (ProteinApp/Sources/App/LanguageHelper.swift:1).
상태/저장 & 시스템 통합

즐겨찾기 관리 로직을 DataStore로 포팅하고 FavoritesManager 인터페이스를 유지 (ProteinApp/Sources/App/FavoritesManager.swift:1).
iOS에서 사용하던 햅틱·회전 잠금·상태바 제어는 Android Vibrator/Orientation API/WindowInsets로 대응 (ProteinApp/Sources/App/iPhoneContentView.swift:128).
단백질 파일·설명 데이터를 Room/파일 캐시로 저장해 오프라인 접근을 제공.
테스트/최적화/CI

파서와 리포지토리 단위 테스트, ViewModel 코루틴 테스트 작성; Compose UI는 Robolectric/계측 테스트로 핵심 화면 검증.
대형 PDB(>50k atom) 로딩 성능 측정, LOD·스레딩 최적화 반복.
GitHub Actions/Bitrise로 CI 파이프라인 구성, QA용 내부 배포 준비; 문서화(README, 개발 가이드)를 Android 버전으로 업데이트.




Auto context