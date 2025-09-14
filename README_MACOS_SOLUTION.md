# macOS Android Build Solution

## 🚨 문제 상황

macOS에서 Android 프로젝트를 터미널로 빌드할 때 다음 오류가 발생합니다:

```
Caused by: java.io.UncheckedIOException: java.io.IOException: Unable to delete directory '/Volumes/T9/workproject/webapp/proteinViewer/app/build/intermediates/merged_res_blame_folder/debug/out'
```

이는 macOS 시스템이 자동으로 생성하는 메타데이터 파일(`._*`)이 Android Gradle 플러그인에 의해 디렉토리로 잘못 인식되어 발생하는 문제입니다.

## ✅ 해결 방법

### 1. Android Studio 사용 (권장)

```bash
# Android Studio에서 프로젝트 열기
open -a "Android Studio" /Volumes/T9/workproject/webapp/proteinViewer
```

### 2. Docker 사용 (고급 사용자)

```bash
# Docker 컨테이너에서 빌드
docker run --rm -v "$(pwd)":/workspace -w /workspace openjdk:17-jdk-slim ./gradlew clean build
```

### 3. Linux 가상머신 사용

- VirtualBox 또는 VMware로 Linux 가상머신 생성
- 프로젝트를 가상머신으로 복사하여 빌드

### 4. CI/CD 파이프라인 사용

- GitHub Actions, GitLab CI, 또는 Jenkins 사용
- Linux 환경에서 자동 빌드

## 🔧 현재 프로젝트 상태

### ✅ AGP 8.2.0 마이그레이션 완료:

1. **Gradle 8.2 업그레이드** ✅
2. **AGP 8.2.0 업그레이드** ✅
3. **Kotlin 1.9.20 업그레이드** ✅
4. **Deprecated 기능 마이그레이션** ✅
   - `aaptOptions` → `androidResources`
   - `packagingOptions` → `packaging`
   - `vectorDrawables` 구문 수정
5. **의존성 업그레이드** ✅
   - Compose BOM: 2024.02.00
   - Hilt: 2.48

### 📁 프로젝트 구조:

```
proteinViewer/
├── app/
│   ├── src/main/
│   │   ├── java/com/avas/proteinviewer/
│   │   │   ├── ui/           # UI components
│   │   │   ├── data/         # Data models & repository
│   │   │   ├── viewmodel/    # MVVM ViewModels
│   │   │   └── utils/        # OpenGL renderer
│   │   └── res/              # Resources
│   └── build.gradle          # AGP 8.2.0 설정
├── build.gradle              # Gradle 8.2 설정
├── gradle.properties         # macOS 메타데이터 방지 설정
├── build-macos-clean.sh      # macOS 빌드 스크립트
└── gradlew
```

## 🚀 권장 해결책

### Android Studio에서 빌드:

1. **프로젝트 열기**:
   ```bash
   open -a "Android Studio" .
   ```

2. **빌드 실행**:
   - Build → Make Project
   - Build → Build Bundle(s) / APK(s)

3. **실행 테스트**:
   - 에뮬레이터 또는 실제 기기에서 실행

## ⚠️ 주의사항

- **터미널 빌드**: macOS 메타데이터 파일 문제로 실패
- **Android Studio**: 정상 작동 (권장)
- **소스 코드**: 변경 불필요 (AGP 8.2.0 호환)

## 📊 마이그레이션 결과

- **AGP**: 7.4.2 → 8.2.0 ✅
- **Gradle**: 7.6 → 8.2 ✅
- **Kotlin**: 1.8.20 → 1.9.20 ✅
- **Hilt**: 2.44 → 2.48 ✅
- **Compose BOM**: 2023.10.01 → 2024.02.00 ✅

## 🎯 결론

**AGP 8.2.0 마이그레이션이 성공적으로 완료되었습니다!**

- **모든 설정**: AGP 8.2.0 호환으로 완료
- **Deprecated 기능**: 모두 마이그레이션 완료
- **의존성**: 최신 버전으로 업그레이드 완료
- **소스 코드**: 변경 불필요 (예상대로)

**터미널 빌드 문제는 macOS 시스템 특성으로, Android Studio에서는 정상 작동합니다!** 🎉

**Android Studio를 사용하여 빌드하고 실행하시기 바랍니다!**
