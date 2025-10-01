# Protein Viewer Android App

A modern Android application for visualizing 3D protein structures with AR support, built using Clean Architecture principles and Jetpack Compose.

## 🧬 Features

### Core Features
- **3D Protein Visualization**: Interactive 3D rendering of protein structures using OpenGL ES
- **AR Mode**: Augmented Reality visualization using ARCore
- **Protein Search**: Search and browse protein databases
- **Educational Content**: Interactive learning tools and quizzes
- **Multilingual Support**: English, Korean, and Japanese

### Technical Features
- **Clean Architecture**: MVVM pattern with Use Cases and Repository pattern
- **Modern UI**: Jetpack Compose with Material 3 design
- **Dependency Injection**: Hilt for clean dependency management
- **3D Rendering**: Custom OpenGL ES renderer with Filament integration
- **AR Integration**: ARCore-based augmented reality
- **Offline Support**: Local caching and offline functionality

## 🏗️ Architecture

The app follows Clean Architecture principles with clear separation of concerns:

```
app/src/main/java/com/avas/proteinviewer/
├── core/                           # Core business logic
│   ├── domain/                     # Domain layer
│   │   ├── model/                  # Domain models
│   │   ├── repository/             # Repository interfaces
│   │   └── usecase/                # Use cases
│   └── data/                       # Data layer
│       ├── local/                  # Local data sources
│       ├── remote/                 # Remote data sources
│       └── repository/             # Repository implementations
├── presentation/                   # Presentation layer
│   ├── ui/                         # UI components
│   ├── viewmodel/                  # ViewModels
│   └── state/                      # UI state
├── rendering/                      # 3D rendering
│   ├── engine/                     # Rendering engine
│   └── models/                     # 3D models
├── ar/                            # AR functionality
│   ├── core/                      # AR core
│   ├── rendering/                 # AR rendering
│   └── tracking/                  # AR tracking
└── di/                           # Dependency injection
```

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog or later
- Android SDK 24+ (Android 7.0)
- Kotlin 1.9.10+
- ARCore supported device (for AR features)

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/your-username/protein-viewer-android.git
   cd protein-viewer-android
   ```

2. **Open in Android Studio**
   - Open Android Studio
   - Select "Open an existing project"
   - Navigate to the cloned directory

3. **Sync dependencies**
   - Android Studio will automatically sync Gradle
   - Wait for the sync to complete

4. **Build and run**
   - Connect an Android device or start an emulator
   - Click the "Run" button or use `Ctrl+R`

### Building from Command Line

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Run tests
./gradlew test
./gradlew connectedAndroidTest
```

## 📱 Usage

### Basic Usage

1. **Launch the app** - The main screen displays a 3D protein viewer
2. **Search proteins** - Use the search bar to find specific proteins
3. **Browse library** - Explore categorized protein collections
4. **View in 3D** - Interact with 3D protein structures
5. **Try AR mode** - Place proteins in your real environment

### Advanced Features

- **Color modes**: Switch between different coloring schemes
- **Slice mode**: Cut through protein structures
- **Animation**: Animate protein dynamics
- **Educational content**: Learn about protein structure and function

## 🛠️ Development

### Code Style

The project follows Kotlin coding conventions and Android best practices:

- Use `camelCase` for variables and functions
- Use `PascalCase` for classes and interfaces
- Use meaningful names for all identifiers
- Add KDoc comments for public APIs
- Follow the existing code formatting

### Testing

Run tests using the following commands:

```bash
# Unit tests
./gradlew test

# UI tests
./gradlew connectedAndroidTest

# All tests
./gradlew check
```

### Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📚 Dependencies

### Core Dependencies
- **AndroidX**: Core Android libraries
- **Jetpack Compose**: Modern UI toolkit
- **Hilt**: Dependency injection
- **Retrofit**: HTTP client
- **Room**: Local database
- **Coroutines**: Asynchronous programming

### 3D Rendering
- **OpenGL ES**: 3D graphics
- **Filament**: Google's PBR renderer
- **Custom Shaders**: Protein-specific rendering

### AR
- **ARCore**: Google's AR platform
- **Custom AR Renderer**: Protein AR visualization

## 🎨 Design System

The app uses a comprehensive design system with:

- **Colors**: Consistent color palette
- **Typography**: Material 3 typography scale
- **Spacing**: 8dp grid system
- **Shapes**: Rounded corner system
- **Elevation**: Material elevation system

## 🔧 Configuration

### Build Variants

- **Debug**: Development build with debugging enabled
- **Release**: Production build with optimizations

### ProGuard

The release build uses ProGuard for code obfuscation and optimization. Rules are defined in `proguard-rules.pro`.

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🤝 Support

For support and questions:

- Create an issue on GitHub
- Check the documentation
- Review the code comments

## 🙏 Acknowledgments

- **RCSB PDB**: Protein structure data
- **Google ARCore**: AR platform
- **Google Filament**: 3D rendering engine
- **Android team**: Modern Android development tools

## 📈 Roadmap

### Version 1.1
- [ ] Advanced protein analysis tools
- [ ] Cloud synchronization
- [ ] Social sharing features

### Version 1.2
- [ ] Machine learning integration
- [ ] Advanced AR features
- [ ] Performance optimizations

---

**Made with ❤️ for the scientific community**


A modern Android application for visualizing 3D protein structures with AR support, built using Clean Architecture principles and Jetpack Compose.

## 🧬 Features

### Core Features
- **3D Protein Visualization**: Interactive 3D rendering of protein structures using OpenGL ES
- **AR Mode**: Augmented Reality visualization using ARCore
- **Protein Search**: Search and browse protein databases
- **Educational Content**: Interactive learning tools and quizzes
- **Multilingual Support**: English, Korean, and Japanese

### Technical Features
- **Clean Architecture**: MVVM pattern with Use Cases and Repository pattern
- **Modern UI**: Jetpack Compose with Material 3 design
- **Dependency Injection**: Hilt for clean dependency management
- **3D Rendering**: Custom OpenGL ES renderer with Filament integration
- **AR Integration**: ARCore-based augmented reality
- **Offline Support**: Local caching and offline functionality

## 🏗️ Architecture

The app follows Clean Architecture principles with clear separation of concerns:

```
app/src/main/java/com/avas/proteinviewer/
├── core/                           # Core business logic
│   ├── domain/                     # Domain layer
│   │   ├── model/                  # Domain models
│   │   ├── repository/             # Repository interfaces
│   │   └── usecase/                # Use cases
│   └── data/                       # Data layer
│       ├── local/                  # Local data sources
│       ├── remote/                 # Remote data sources
│       └── repository/             # Repository implementations
├── presentation/                   # Presentation layer
│   ├── ui/                         # UI components
│   ├── viewmodel/                  # ViewModels
│   └── state/                      # UI state
├── rendering/                      # 3D rendering
│   ├── engine/                     # Rendering engine
│   └── models/                     # 3D models
├── ar/                            # AR functionality
│   ├── core/                      # AR core
│   ├── rendering/                 # AR rendering
│   └── tracking/                  # AR tracking
└── di/                           # Dependency injection
```

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog or later
- Android SDK 24+ (Android 7.0)
- Kotlin 1.9.10+
- ARCore supported device (for AR features)

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/your-username/protein-viewer-android.git
   cd protein-viewer-android
   ```

2. **Open in Android Studio**
   - Open Android Studio
   - Select "Open an existing project"
   - Navigate to the cloned directory

3. **Sync dependencies**
   - Android Studio will automatically sync Gradle
   - Wait for the sync to complete

4. **Build and run**
   - Connect an Android device or start an emulator
   - Click the "Run" button or use `Ctrl+R`

### Building from Command Line

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Run tests
./gradlew test
./gradlew connectedAndroidTest
```

## 📱 Usage

### Basic Usage

1. **Launch the app** - The main screen displays a 3D protein viewer
2. **Search proteins** - Use the search bar to find specific proteins
3. **Browse library** - Explore categorized protein collections
4. **View in 3D** - Interact with 3D protein structures
5. **Try AR mode** - Place proteins in your real environment

### Advanced Features

- **Color modes**: Switch between different coloring schemes
- **Slice mode**: Cut through protein structures
- **Animation**: Animate protein dynamics
- **Educational content**: Learn about protein structure and function

## 🛠️ Development

### Code Style

The project follows Kotlin coding conventions and Android best practices:

- Use `camelCase` for variables and functions
- Use `PascalCase` for classes and interfaces
- Use meaningful names for all identifiers
- Add KDoc comments for public APIs
- Follow the existing code formatting

### Testing

Run tests using the following commands:

```bash
# Unit tests
./gradlew test

# UI tests
./gradlew connectedAndroidTest

# All tests
./gradlew check
```

### Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📚 Dependencies

### Core Dependencies
- **AndroidX**: Core Android libraries
- **Jetpack Compose**: Modern UI toolkit
- **Hilt**: Dependency injection
- **Retrofit**: HTTP client
- **Room**: Local database
- **Coroutines**: Asynchronous programming

### 3D Rendering
- **OpenGL ES**: 3D graphics
- **Filament**: Google's PBR renderer
- **Custom Shaders**: Protein-specific rendering

### AR
- **ARCore**: Google's AR platform
- **Custom AR Renderer**: Protein AR visualization

## 🎨 Design System

The app uses a comprehensive design system with:

- **Colors**: Consistent color palette
- **Typography**: Material 3 typography scale
- **Spacing**: 8dp grid system
- **Shapes**: Rounded corner system
- **Elevation**: Material elevation system

## 🔧 Configuration

### Build Variants

- **Debug**: Development build with debugging enabled
- **Release**: Production build with optimizations

### ProGuard

The release build uses ProGuard for code obfuscation and optimization. Rules are defined in `proguard-rules.pro`.

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🤝 Support

For support and questions:

- Create an issue on GitHub
- Check the documentation
- Review the code comments

## 🙏 Acknowledgments

- **RCSB PDB**: Protein structure data
- **Google ARCore**: AR platform
- **Google Filament**: 3D rendering engine
- **Android team**: Modern Android development tools

## 📈 Roadmap

### Version 1.1
- [ ] Advanced protein analysis tools
- [ ] Cloud synchronization
- [ ] Social sharing features

### Version 1.2
- [ ] Machine learning integration
- [ ] Advanced AR features
- [ ] Performance optimizations

---

**Made with ❤️ for the scientific community**


