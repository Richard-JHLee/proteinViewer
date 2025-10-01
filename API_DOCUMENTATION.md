# API Documentation

## Core Domain Models

### Protein
The central domain model representing a protein structure.

```kotlin
data class Protein(
    val id: String,                    // PDB ID (e.g., "1INS")
    val name: String,                  // Protein name
    val description: String,           // Protein description
    val organism: String,              // Source organism
    val molecularWeight: Float,        // Molecular weight in Da
    val resolution: Float,             // Resolution in Ångströms
    val experimentalMethod: String,    // Experimental method
    val depositionDate: String,        // Deposition date
    val spaceGroup: String,            // Crystallographic space group
    val category: ProteinCategory,     // Protein category
    val isFavorite: Boolean,           // User favorite status
    val imagePath: String?,            // Local image path
    val structure: ProteinStructure?   // 3D structure data
)
```

### Atom
Represents an individual atom in the protein structure.

```kotlin
data class Atom(
    val serial: Int,                   // Atom serial number
    val name: String,                  // Atom name (e.g., "CA", "N")
    val element: String,               // Element symbol (e.g., "C", "N", "O")
    val position: Vector3,             // 3D coordinates
    val occupancy: Float,              // Occupancy factor
    val tempFactor: Float,             // Temperature factor
    val chainId: String,               // Chain identifier
    val residueId: String,             // Residue identifier
    val residueName: String,           // Residue name (e.g., "ALA", "GLY")
    val residueSequence: Int,          // Residue sequence number
    val isBackbone: Boolean,           // Is backbone atom
    val isLigand: Boolean,             // Is ligand atom
    val isPocket: Boolean              // Is binding pocket atom
)
```

### Bond
Represents a chemical bond between atoms.

```kotlin
data class Bond(
    val atom1Serial: Int,              // First atom serial number
    val atom2Serial: Int,              // Second atom serial number
    val distance: Float,               // Bond distance in Ångströms
    val bondType: BondType,            // Type of bond
    val bondOrder: BondOrder,          // Bond order
    val isAromatic: Boolean,           // Is aromatic bond
    val isHydrogenBond: Boolean,       // Is hydrogen bond
    val isDisulfideBond: Boolean       // Is disulfide bond
)
```

## Use Cases

### SearchProteinsUseCase
Searches for proteins by query string.

```kotlin
class SearchProteinsUseCase @Inject constructor(
    private val repository: ProteinRepository
) {
    suspend operator fun invoke(query: String): Result<List<Protein>>
}
```

**Parameters:**
- `query: String` - Search query string

**Returns:**
- `Result<List<Protein>>` - Success with protein list or failure with error

**Example:**
```kotlin
val useCase = SearchProteinsUseCase(repository)
val result = useCase("insulin")
result.onSuccess { proteins ->
    // Handle successful search
}.onFailure { error ->
    // Handle search error
}
```

### LoadProteinUseCase
Loads a specific protein by ID.

```kotlin
class LoadProteinUseCase @Inject constructor(
    private val repository: ProteinRepository
) {
    suspend operator fun invoke(proteinId: String): Result<Protein>
}
```

**Parameters:**
- `proteinId: String` - PDB ID of the protein

**Returns:**
- `Result<Protein>` - Success with protein data or failure with error

### ToggleFavoriteUseCase
Toggles the favorite status of a protein.

```kotlin
class ToggleFavoriteUseCase @Inject constructor(
    private val repository: ProteinRepository
) {
    suspend operator fun invoke(proteinId: String): Result<Boolean>
}
```

**Parameters:**
- `proteinId: String` - PDB ID of the protein

**Returns:**
- `Result<Boolean>` - Success with new favorite status or failure with error

## Repository Interface

### ProteinRepository
Main repository interface for protein data operations.

```kotlin
interface ProteinRepository {
    // Search operations
    suspend fun searchProteins(query: String): Result<List<Protein>>
    suspend fun getProteinById(id: String): Result<Protein>
    suspend fun getProteinsByCategory(category: String): Result<List<Protein>>
    suspend fun getFavoriteProteins(): Result<List<Protein>>
    
    // Structure operations
    suspend fun loadProteinStructure(proteinId: String): Result<Protein>
    suspend fun downloadProteinFile(proteinId: String): Result<String>
    
    // Favorite operations
    suspend fun addToFavorites(proteinId: String): Result<Unit>
    suspend fun removeFromFavorites(proteinId: String): Result<Unit>
    suspend fun isFavorite(proteinId: String): Result<Boolean>
    
    // Cache operations
    suspend fun getCachedProteins(): Flow<List<Protein>>
    suspend fun cacheProtein(protein: Protein): Result<Unit>
    suspend fun clearCache(): Result<Unit>
    
    // Metadata operations
    suspend fun getProteinMetadata(proteinId: String): Result<ProteinMetadata>
    suspend fun getDiseaseInfo(proteinId: String): Result<DiseaseInfo>
    suspend fun getResearchStatus(proteinId: String): Result<ResearchStatusInfo>
}
```

## Rendering Engine

### RenderingEngine
Interface for 3D rendering operations.

```kotlin
interface RenderingEngine {
    // Initialization
    suspend fun initialize()
    suspend fun loadStructure(structure: ProteinStructure)
    
    // Rendering
    fun render()
    
    // Camera control
    fun setCameraPosition(position: Vector3)
    fun setCameraTarget(target: Vector3)
    fun setCameraUp(up: Vector3)
    fun zoom(factor: Float)
    fun rotate(deltaX: Float, deltaY: Float)
    fun translate(deltaX: Float, deltaY: Float)
    
    // Rendering settings
    fun setColorMode(mode: ColorMode)
    fun setSliceMode(enabled: Boolean)
    fun setAnimationEnabled(enabled: Boolean)
    fun setBackgroundColor(color: Color3)
    
    // Highlighting
    fun highlightAtoms(atomSerials: List<Int>)
    fun highlightBonds(bondIndices: List<Int>)
    fun clearHighlights()
    
    // Selection
    fun selectAtom(atomSerial: Int)
    fun selectBond(bondIndex: Int)
    fun clearSelection()
    
    // Cleanup
    fun dispose()
}
```

### ColorMode
Enumeration of available color modes.

```kotlin
enum class ColorMode {
    CHAIN,                    // Color by chain
    RESIDUE,                  // Color by residue type
    ELEMENT,                  // Color by element
    SECONDARY_STRUCTURE,      // Color by secondary structure
    HYDROPHOBICITY,           // Color by hydrophobicity
    CHARGE,                   // Color by charge
    UNIFORM                   // Uniform color
}
```

## AR System

### ARCoreManager
Manages ARCore session and tracking state.

```kotlin
@Singleton
class ARCoreManager @Inject constructor(
    private val context: Context
) {
    val isARSupported: StateFlow<Boolean>
    val isTracking: StateFlow<Boolean>
    val trackingState: StateFlow<TrackingState>
    
    suspend fun initialize(): ARCoreStatus
    fun createSession(): Session?
    fun startSession()
    fun pauseSession()
    fun stopSession()
    fun updateTrackingState()
    fun getSession(): Session?
    fun dispose()
}
```

### ARTrackingManager
Manages AR tracking and plane detection.

```kotlin
@Singleton
class ARTrackingManager @Inject constructor(
    private val context: Context
) {
    val detectedPlanes: StateFlow<List<Plane>>
    val anchors: StateFlow<List<Anchor>>
    val selectedPlane: StateFlow<Plane?>
    val proteinAnchor: StateFlow<Anchor?>
    
    fun updateTracking(frame: Frame, session: Session)
    fun selectPlaneAt(x: Float, y: Float, frame: Frame, session: Session): Plane?
    fun createProteinAnchor(x: Float, y: Float, frame: Frame, session: Session): Anchor?
    fun getAnchorPosition(anchor: Anchor): Vector3
    fun getPlaneCenter(plane: Plane): Vector3
    fun getPlaneSize(plane: Plane): Vector3
    fun isPlaneLargeEnough(plane: Plane, minSize: Float = 0.5f): Boolean
    fun getLargestPlane(): Plane?
    fun removeProteinAnchor()
    fun clearAllAnchors()
    fun clearSelectedPlane()
    fun reset()
}
```

## UI Components

### ProteinCard
Reusable protein card component.

```kotlin
@Composable
fun ProteinCard(
    protein: Protein,
    onClick: () -> Unit,
    onFavoriteClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    showFavorite: Boolean = true,
    showCategory: Boolean = true,
    showResolution: Boolean = true,
    showMethod: Boolean = true
)
```

**Parameters:**
- `protein: Protein` - Protein data to display
- `onClick: () -> Unit` - Click callback
- `onFavoriteClick: (() -> Unit)?` - Favorite toggle callback
- `modifier: Modifier` - Compose modifier
- `showFavorite: Boolean` - Show favorite button
- `showCategory: Boolean` - Show category info
- `showResolution: Boolean` - Show resolution info
- `showMethod: Boolean` - Show experimental method

### SearchBar
Search input component.

```kotlin
@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search proteins...",
    enabled: Boolean = true,
    showClearButton: Boolean = true,
    showSearchButton: Boolean = false
)
```

### Protein3DViewer
3D protein visualization component.

```kotlin
@Composable
fun Protein3DViewer(
    protein: Protein?,
    isAnimating: Boolean,
    sliceMode: Boolean,
    colorMode: ColorMode,
    onPlayPause: () -> Unit,
    onSliceToggle: () -> Unit,
    onColorModeChange: (ColorMode) -> Unit,
    onResetView: () -> Unit,
    onToggleInfo: () -> Unit,
    modifier: Modifier = Modifier
)
```

## Error Handling

### Result Type
All repository operations return `Result<T>` for proper error handling.

```kotlin
// Success case
val result: Result<Protein> = repository.getProteinById("1INS")
result.onSuccess { protein ->
    // Handle success
}.onFailure { error ->
    // Handle error
}

// Or using getOrNull()
val protein = result.getOrNull()
if (protein != null) {
    // Use protein
}
```

### Common Error Types
- `NetworkException`: Network connectivity issues
- `ParseException`: Data parsing errors
- `CacheException`: Local storage errors
- `ARException`: AR functionality errors

## Testing

### Unit Tests
```kotlin
@Test
fun `searchProteins with valid query returns success`() = runTest {
    // Given
    val query = "insulin"
    val expectedProteins = listOf(createTestProtein())
    coEvery { repository.searchProteins(query) } returns Result.success(expectedProteins)
    
    // When
    val result = useCase(query)
    
    // Then
    assertTrue(result.isSuccess)
    assertEquals(expectedProteins, result.getOrNull())
}
```

### UI Tests
```kotlin
@Test
fun proteinCard_displaysProteinName() {
    // Given
    val protein = createTestProtein("1INS", "Insulin")
    
    // When
    composeTestRule.setContent {
        ProteinCard(protein = protein, onClick = {})
    }
    
    // Then
    composeTestRule.onNodeWithText("Insulin").assertIsDisplayed()
}
```

## Performance Considerations

### Memory Management
- Use `@Composable` functions efficiently
- Dispose of resources properly
- Use `remember` for expensive calculations
- Implement proper lifecycle management

### Rendering Optimization
- Use Level of Detail (LOD) for large structures
- Implement frustum culling
- Use instanced rendering for repeated elements
- Optimize shader programs

### AR Performance
- Limit the number of tracked planes
- Use efficient anchor management
- Optimize 3D model complexity
- Implement proper occlusion handling

## Security

### Data Protection
- All network requests use HTTPS
- Sensitive data is encrypted in local storage
- User preferences are stored securely
- No personal data is collected

### AR Safety
- AR features require user permission
- Camera access is properly managed
- AR sessions are cleaned up properly
- No persistent AR data storage


## Core Domain Models

### Protein
The central domain model representing a protein structure.

```kotlin
data class Protein(
    val id: String,                    // PDB ID (e.g., "1INS")
    val name: String,                  // Protein name
    val description: String,           // Protein description
    val organism: String,              // Source organism
    val molecularWeight: Float,        // Molecular weight in Da
    val resolution: Float,             // Resolution in Ångströms
    val experimentalMethod: String,    // Experimental method
    val depositionDate: String,        // Deposition date
    val spaceGroup: String,            // Crystallographic space group
    val category: ProteinCategory,     // Protein category
    val isFavorite: Boolean,           // User favorite status
    val imagePath: String?,            // Local image path
    val structure: ProteinStructure?   // 3D structure data
)
```

### Atom
Represents an individual atom in the protein structure.

```kotlin
data class Atom(
    val serial: Int,                   // Atom serial number
    val name: String,                  // Atom name (e.g., "CA", "N")
    val element: String,               // Element symbol (e.g., "C", "N", "O")
    val position: Vector3,             // 3D coordinates
    val occupancy: Float,              // Occupancy factor
    val tempFactor: Float,             // Temperature factor
    val chainId: String,               // Chain identifier
    val residueId: String,             // Residue identifier
    val residueName: String,           // Residue name (e.g., "ALA", "GLY")
    val residueSequence: Int,          // Residue sequence number
    val isBackbone: Boolean,           // Is backbone atom
    val isLigand: Boolean,             // Is ligand atom
    val isPocket: Boolean              // Is binding pocket atom
)
```

### Bond
Represents a chemical bond between atoms.

```kotlin
data class Bond(
    val atom1Serial: Int,              // First atom serial number
    val atom2Serial: Int,              // Second atom serial number
    val distance: Float,               // Bond distance in Ångströms
    val bondType: BondType,            // Type of bond
    val bondOrder: BondOrder,          // Bond order
    val isAromatic: Boolean,           // Is aromatic bond
    val isHydrogenBond: Boolean,       // Is hydrogen bond
    val isDisulfideBond: Boolean       // Is disulfide bond
)
```

## Use Cases

### SearchProteinsUseCase
Searches for proteins by query string.

```kotlin
class SearchProteinsUseCase @Inject constructor(
    private val repository: ProteinRepository
) {
    suspend operator fun invoke(query: String): Result<List<Protein>>
}
```

**Parameters:**
- `query: String` - Search query string

**Returns:**
- `Result<List<Protein>>` - Success with protein list or failure with error

**Example:**
```kotlin
val useCase = SearchProteinsUseCase(repository)
val result = useCase("insulin")
result.onSuccess { proteins ->
    // Handle successful search
}.onFailure { error ->
    // Handle search error
}
```

### LoadProteinUseCase
Loads a specific protein by ID.

```kotlin
class LoadProteinUseCase @Inject constructor(
    private val repository: ProteinRepository
) {
    suspend operator fun invoke(proteinId: String): Result<Protein>
}
```

**Parameters:**
- `proteinId: String` - PDB ID of the protein

**Returns:**
- `Result<Protein>` - Success with protein data or failure with error

### ToggleFavoriteUseCase
Toggles the favorite status of a protein.

```kotlin
class ToggleFavoriteUseCase @Inject constructor(
    private val repository: ProteinRepository
) {
    suspend operator fun invoke(proteinId: String): Result<Boolean>
}
```

**Parameters:**
- `proteinId: String` - PDB ID of the protein

**Returns:**
- `Result<Boolean>` - Success with new favorite status or failure with error

## Repository Interface

### ProteinRepository
Main repository interface for protein data operations.

```kotlin
interface ProteinRepository {
    // Search operations
    suspend fun searchProteins(query: String): Result<List<Protein>>
    suspend fun getProteinById(id: String): Result<Protein>
    suspend fun getProteinsByCategory(category: String): Result<List<Protein>>
    suspend fun getFavoriteProteins(): Result<List<Protein>>
    
    // Structure operations
    suspend fun loadProteinStructure(proteinId: String): Result<Protein>
    suspend fun downloadProteinFile(proteinId: String): Result<String>
    
    // Favorite operations
    suspend fun addToFavorites(proteinId: String): Result<Unit>
    suspend fun removeFromFavorites(proteinId: String): Result<Unit>
    suspend fun isFavorite(proteinId: String): Result<Boolean>
    
    // Cache operations
    suspend fun getCachedProteins(): Flow<List<Protein>>
    suspend fun cacheProtein(protein: Protein): Result<Unit>
    suspend fun clearCache(): Result<Unit>
    
    // Metadata operations
    suspend fun getProteinMetadata(proteinId: String): Result<ProteinMetadata>
    suspend fun getDiseaseInfo(proteinId: String): Result<DiseaseInfo>
    suspend fun getResearchStatus(proteinId: String): Result<ResearchStatusInfo>
}
```

## Rendering Engine

### RenderingEngine
Interface for 3D rendering operations.

```kotlin
interface RenderingEngine {
    // Initialization
    suspend fun initialize()
    suspend fun loadStructure(structure: ProteinStructure)
    
    // Rendering
    fun render()
    
    // Camera control
    fun setCameraPosition(position: Vector3)
    fun setCameraTarget(target: Vector3)
    fun setCameraUp(up: Vector3)
    fun zoom(factor: Float)
    fun rotate(deltaX: Float, deltaY: Float)
    fun translate(deltaX: Float, deltaY: Float)
    
    // Rendering settings
    fun setColorMode(mode: ColorMode)
    fun setSliceMode(enabled: Boolean)
    fun setAnimationEnabled(enabled: Boolean)
    fun setBackgroundColor(color: Color3)
    
    // Highlighting
    fun highlightAtoms(atomSerials: List<Int>)
    fun highlightBonds(bondIndices: List<Int>)
    fun clearHighlights()
    
    // Selection
    fun selectAtom(atomSerial: Int)
    fun selectBond(bondIndex: Int)
    fun clearSelection()
    
    // Cleanup
    fun dispose()
}
```

### ColorMode
Enumeration of available color modes.

```kotlin
enum class ColorMode {
    CHAIN,                    // Color by chain
    RESIDUE,                  // Color by residue type
    ELEMENT,                  // Color by element
    SECONDARY_STRUCTURE,      // Color by secondary structure
    HYDROPHOBICITY,           // Color by hydrophobicity
    CHARGE,                   // Color by charge
    UNIFORM                   // Uniform color
}
```

## AR System

### ARCoreManager
Manages ARCore session and tracking state.

```kotlin
@Singleton
class ARCoreManager @Inject constructor(
    private val context: Context
) {
    val isARSupported: StateFlow<Boolean>
    val isTracking: StateFlow<Boolean>
    val trackingState: StateFlow<TrackingState>
    
    suspend fun initialize(): ARCoreStatus
    fun createSession(): Session?
    fun startSession()
    fun pauseSession()
    fun stopSession()
    fun updateTrackingState()
    fun getSession(): Session?
    fun dispose()
}
```

### ARTrackingManager
Manages AR tracking and plane detection.

```kotlin
@Singleton
class ARTrackingManager @Inject constructor(
    private val context: Context
) {
    val detectedPlanes: StateFlow<List<Plane>>
    val anchors: StateFlow<List<Anchor>>
    val selectedPlane: StateFlow<Plane?>
    val proteinAnchor: StateFlow<Anchor?>
    
    fun updateTracking(frame: Frame, session: Session)
    fun selectPlaneAt(x: Float, y: Float, frame: Frame, session: Session): Plane?
    fun createProteinAnchor(x: Float, y: Float, frame: Frame, session: Session): Anchor?
    fun getAnchorPosition(anchor: Anchor): Vector3
    fun getPlaneCenter(plane: Plane): Vector3
    fun getPlaneSize(plane: Plane): Vector3
    fun isPlaneLargeEnough(plane: Plane, minSize: Float = 0.5f): Boolean
    fun getLargestPlane(): Plane?
    fun removeProteinAnchor()
    fun clearAllAnchors()
    fun clearSelectedPlane()
    fun reset()
}
```

## UI Components

### ProteinCard
Reusable protein card component.

```kotlin
@Composable
fun ProteinCard(
    protein: Protein,
    onClick: () -> Unit,
    onFavoriteClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    showFavorite: Boolean = true,
    showCategory: Boolean = true,
    showResolution: Boolean = true,
    showMethod: Boolean = true
)
```

**Parameters:**
- `protein: Protein` - Protein data to display
- `onClick: () -> Unit` - Click callback
- `onFavoriteClick: (() -> Unit)?` - Favorite toggle callback
- `modifier: Modifier` - Compose modifier
- `showFavorite: Boolean` - Show favorite button
- `showCategory: Boolean` - Show category info
- `showResolution: Boolean` - Show resolution info
- `showMethod: Boolean` - Show experimental method

### SearchBar
Search input component.

```kotlin
@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search proteins...",
    enabled: Boolean = true,
    showClearButton: Boolean = true,
    showSearchButton: Boolean = false
)
```

### Protein3DViewer
3D protein visualization component.

```kotlin
@Composable
fun Protein3DViewer(
    protein: Protein?,
    isAnimating: Boolean,
    sliceMode: Boolean,
    colorMode: ColorMode,
    onPlayPause: () -> Unit,
    onSliceToggle: () -> Unit,
    onColorModeChange: (ColorMode) -> Unit,
    onResetView: () -> Unit,
    onToggleInfo: () -> Unit,
    modifier: Modifier = Modifier
)
```

## Error Handling

### Result Type
All repository operations return `Result<T>` for proper error handling.

```kotlin
// Success case
val result: Result<Protein> = repository.getProteinById("1INS")
result.onSuccess { protein ->
    // Handle success
}.onFailure { error ->
    // Handle error
}

// Or using getOrNull()
val protein = result.getOrNull()
if (protein != null) {
    // Use protein
}
```

### Common Error Types
- `NetworkException`: Network connectivity issues
- `ParseException`: Data parsing errors
- `CacheException`: Local storage errors
- `ARException`: AR functionality errors

## Testing

### Unit Tests
```kotlin
@Test
fun `searchProteins with valid query returns success`() = runTest {
    // Given
    val query = "insulin"
    val expectedProteins = listOf(createTestProtein())
    coEvery { repository.searchProteins(query) } returns Result.success(expectedProteins)
    
    // When
    val result = useCase(query)
    
    // Then
    assertTrue(result.isSuccess)
    assertEquals(expectedProteins, result.getOrNull())
}
```

### UI Tests
```kotlin
@Test
fun proteinCard_displaysProteinName() {
    // Given
    val protein = createTestProtein("1INS", "Insulin")
    
    // When
    composeTestRule.setContent {
        ProteinCard(protein = protein, onClick = {})
    }
    
    // Then
    composeTestRule.onNodeWithText("Insulin").assertIsDisplayed()
}
```

## Performance Considerations

### Memory Management
- Use `@Composable` functions efficiently
- Dispose of resources properly
- Use `remember` for expensive calculations
- Implement proper lifecycle management

### Rendering Optimization
- Use Level of Detail (LOD) for large structures
- Implement frustum culling
- Use instanced rendering for repeated elements
- Optimize shader programs

### AR Performance
- Limit the number of tracked planes
- Use efficient anchor management
- Optimize 3D model complexity
- Implement proper occlusion handling

## Security

### Data Protection
- All network requests use HTTPS
- Sensitive data is encrypted in local storage
- User preferences are stored securely
- No personal data is collected

### AR Safety
- AR features require user permission
- Camera access is properly managed
- AR sessions are cleaned up properly
- No persistent AR data storage


