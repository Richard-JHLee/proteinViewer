package com.avas.proteinviewer.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.avas.proteinviewer.domain.model.MenuItemType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuDetailScreen(
    menuItem: MenuItemType,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(menuItem.title) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            when (menuItem) {
                MenuItemType.ABOUT -> AboutView()
                MenuItemType.USER_GUIDE -> UserGuideView()
                MenuItemType.FEATURES -> FeaturesView()
                MenuItemType.SETTINGS -> SettingsView()
                MenuItemType.HELP -> HelpView()
                MenuItemType.PRIVACY -> PrivacyView()
                MenuItemType.TERMS -> TermsView()
                MenuItemType.LICENSE -> LicenseView()
            }
        }
    }
}

@Composable
private fun AboutView() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Icon and Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = "App Icon",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "ProteinViewer",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Version 1.0.0",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Divider()
        
        // App Description
        Text(
            text = "Professional 3D Protein Structure Visualization",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        
        Text(
            text = "ProteinViewer is a powerful tool for visualizing and analyzing protein structures in 3D. Built with modern Android technologies and OpenGL ES 3.0 for high-performance rendering.",
            style = MaterialTheme.typography.bodyMedium
        )
        
        // Features
        Text(
            text = "Key Features:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        
        FeatureItem("3D Protein Visualization", "Interactive 3D rendering with multiple styles")
        FeatureItem("Advanced Analysis", "Detailed protein structure analysis")
        FeatureItem("Cross-Platform", "Consistent experience across devices")
        FeatureItem("Educational Tools", "Perfect for learning and research")
        
        // Contact Information
        Text(
            text = "Contact Information:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        
        Text(
            text = "For support, feedback, or questions, please contact us through the app store or visit our website.",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun UserGuideView() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "User Guide",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        GuideSection(
            title = "Getting Started",
            content = "1. Enter a PDB ID in the search field\n2. Tap 'Load Protein' to download the structure\n3. Use the 3D viewer to explore the protein\n4. Switch between different rendering styles"
        )
        
        GuideSection(
            title = "Navigation",
            content = "• Drag to rotate the 3D model\n• Pinch to zoom in/out\n• Use the style buttons to change rendering\n• Tap on elements to highlight them"
        )
        
        GuideSection(
            title = "Rendering Styles",
            content = "• Ribbon: Shows protein backbone structure\n• Spheres: Shows individual atoms\n• Sticks: Shows bonds between atoms\n• Cartoon: Simplified representation"
        )
        
        GuideSection(
            title = "Color Schemes",
            content = "• Chain: Different colors for each chain\n• Element: Colors based on atom types\n• Secondary Structure: Colors for α-helix, β-sheet, etc.\n• Uniform: Single color for all elements"
        )
    }
}

@Composable
private fun FeaturesView() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Key Features",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        FeatureCard(
            icon = Icons.Default.Visibility,
            title = "3D Visualization",
            description = "High-quality 3D rendering with multiple visualization styles including ribbon, spheres, sticks, and cartoon representations."
        )
        
        FeatureCard(
            icon = Icons.Default.Analytics,
            title = "Structure Analysis",
            description = "Detailed analysis of protein structures including secondary structure elements, binding sites, and molecular properties."
        )
        
        FeatureCard(
            icon = Icons.Default.Palette,
            title = "Color Schemes",
            description = "Multiple color schemes to highlight different aspects of protein structures including chain-based, element-based, and secondary structure coloring."
        )
        
        FeatureCard(
            icon = Icons.Default.TouchApp,
            title = "Interactive Controls",
            description = "Intuitive touch controls for rotating, zooming, and exploring protein structures with smooth animations."
        )
        
        FeatureCard(
            icon = Icons.Default.Storage,
            title = "Offline Support",
            description = "Download and store protein structures locally for offline viewing and analysis."
        )
        
        FeatureCard(
            icon = Icons.Default.School,
            title = "Educational Tools",
            description = "Perfect for students, researchers, and educators with detailed information panels and analysis tools."
        )
    }
}

@Composable
private fun SettingsView() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "Performance Optimization",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        
        Text(
            text = "Adjust rendering performance for large protein structures. Lower settings provide faster rendering, while higher settings offer better quality.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        // Performance Settings Cards
        SettingsCard(
            title = "Enable Performance Optimization",
            description = "When enabled, limits the number of atoms in large proteins to improve performance.",
            isEnabled = true
        )
        
        SettingsCard(
            title = "Max Atoms Limit",
            description = "Maximum number of atoms to render. Lower values provide faster rendering.",
            value = "5000"
        )
        
        SettingsCard(
            title = "Sampling Ratio",
            description = "Ratio of atoms to sample from each chain. Lower values provide faster processing.",
            value = "25%"
        )
        
        // Performance Guide
        Text(
            text = "Performance Guide",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        
        PerformanceGuideItem(
            icon = Icons.Default.Speed,
            title = "Fast Rendering",
            description = "500-1000 atoms, 5-10% sampling",
            color = Color.Green
        )
        
        PerformanceGuideItem(
            icon = Icons.Default.Balance,
            title = "Balanced",
            description = "1500-2500 atoms, 10-20% sampling",
            color = Color(0xFFFF9800) // Orange color
        )
        
        PerformanceGuideItem(
            icon = Icons.Default.Star,
            title = "High Quality",
            description = "3000-5000 atoms, 20-50% sampling",
            color = Color.Blue
        )
    }
}

@Composable
private fun HelpView() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Help & FAQ",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        FAQItem(
            question = "The app runs slowly",
            answer = "Large protein structures may take longer to load. Try starting with smaller proteins or adjust performance settings."
        )
        
        FAQItem(
            question = "3D model doesn't rotate",
            answer = "Use drag gestures in viewer mode to rotate the model. Make sure you're in viewer mode, not info mode."
        )
        
        FAQItem(
            question = "Colors don't change",
            answer = "Select your desired color mode from Color Schemes and wait a moment for the changes to apply."
        )
        
        FAQItem(
            question = "Cannot load protein",
            answer = "Check your internet connection and ensure you've entered a valid PDB ID."
        )
        
        FAQItem(
            question = "Need additional help",
            answer = "If problems persist or you have other questions, please contact us through the app info page."
        )
    }
}

@Composable
private fun PrivacyView() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Privacy Policy",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        PrivacySection(
            title = "1. Information We Collect",
            content = "ProteinViewer does not collect personal information from users.\n\n• PDB ID: Public identifier for downloading protein structure data, not personal information.\n• App usage data: All data generated within the app is stored only on your device.\n• Network requests: Only API calls for downloading protein structure data are made."
        )
        
        PrivacySection(
            title = "2. How We Use Information",
            content = "• Protein structure visualization and 3D rendering\n• Educational and research data provision\n• Local data processing for app functionality improvement\n• In-app feature provision for user experience enhancement"
        )
        
        PrivacySection(
            title = "3. Information Protection & Security",
            content = "• All data is stored only on your device.\n• No personal information is transmitted to external servers.\n• Only public data is requested when making PDB API calls.\n• All local data is deleted when the app is uninstalled."
        )
        
        PrivacySection(
            title = "4. Third-Party Services",
            content = "• RCSB PDB (data.rcsb.org): Protein structure data provision\n• PDBe (www.ebi.ac.uk): Additional protein information provision\n• UniProt (rest.uniprot.org): Protein function information provision\n\nThese services are all public APIs and do not require personal information."
        )
        
        PrivacySection(
            title = "5. User Rights",
            content = "• Data deletion: All data can be deleted by uninstalling the app.\n• Data modification: Information entered within the app can be modified at any time.\n• Contact: For privacy-related inquiries, please use the app info page."
        )
        
        PrivacySection(
            title = "6. Policy Changes",
            content = "The privacy policy may be changed as needed, and changes will be announced within the app. Last updated: January 9, 2025"
        )
    }
}

@Composable
private fun TermsView() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Terms of Service",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        TermsSection(
            title = "Service Usage",
            content = "ProteinViewer is a paid app. Payment through the Google Play Store is required to download and use the app."
        )
        
        TermsSection(
            title = "Payment & Subscription",
            content = "• App purchases are processed through the Google Play Store.\n• One-time payment provides access to all app features.\n• No additional payments after app purchase.\n• Refunds are limited according to Google Play Store policy."
        )
        
        TermsSection(
            title = "Service Scope",
            content = "• 3D protein structure visualization\n• Advanced rendering options\n• Protein analysis tools\n• Offline data storage\n• Customer support service\n• All features are included with app purchase."
        )
        
        TermsSection(
            title = "Refund Policy",
            content = "• App purchase refunds are processed according to Google Play Store policy.\n• Please request refunds directly through the Google Play Store.\n• Google's refund policy: https://support.google.com/googleplay/answer/2479637\n• Developers have no refund processing authority; Google manages all refunds."
        )
        
        TermsSection(
            title = "App Updates & Support",
            content = "• App updates are provided free of charge.\n• New features can be used without additional payment.\n• 30-day advance notice will be given before app support discontinuation.\n• We are not responsible for data loss due to support discontinuation."
        )
        
        TermsSection(
            title = "Liability Limitation",
            content = "Developers are not responsible for damages caused by app usage. This applies equally to paid app usage."
        )
        
        TermsSection(
            title = "App Changes",
            content = "App features may be changed without prior notice. Major feature changes will be announced through app updates."
        )
        
        TermsSection(
            title = "Contact & Support",
            content = "For service-related inquiries, please contact us through the app info page. Last updated: January 9, 2025"
        )
    }
}

@Composable
private fun LicenseView() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "License Information",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "This app uses the following open source libraries and frameworks:",
            style = MaterialTheme.typography.bodyMedium
        )
        
        LicenseItem(
            name = "Android Jetpack Compose",
            license = "Apache License 2.0",
            description = "Modern Android UI toolkit"
        )
        
        LicenseItem(
            name = "OpenGL ES 3.0",
            license = "Khronos Group License",
            description = "3D graphics rendering"
        )
        
        LicenseItem(
            name = "Retrofit",
            license = "Apache License 2.0",
            description = "HTTP client for API calls"
        )
        
        LicenseItem(
            name = "OkHttp",
            license = "Apache License 2.0",
            description = "HTTP client library"
        )
        
        LicenseItem(
            name = "Hilt",
            license = "Apache License 2.0",
            description = "Dependency injection"
        )
        
        LicenseItem(
            name = "Kotlin Coroutines",
            license = "Apache License 2.0",
            description = "Asynchronous programming"
        )
        
        Divider()
        
        Text(
            text = "Protein Data",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        
        Text(
            text = "Protein structure data is provided by:\n• RCSB Protein Data Bank (rcsb.org)\n• PDBe (ebi.ac.uk)\n• UniProt (uniprot.org)\n\nAll protein data is publicly available and used in accordance with their respective terms of use.",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

// Helper Composable Functions

@Composable
private fun FeatureItem(title: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            Icons.Default.Check,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FeatureCard(
    icon: ImageVector,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun GuideSection(title: String, content: String) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    description: String,
    isEnabled: Boolean = false,
    value: String? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (isEnabled) {
                    Switch(
                        checked = true,
                        onCheckedChange = { }
                    )
                } else if (value != null) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun PerformanceGuideItem(
    icon: ImageVector,
    title: String,
    description: String,
    color: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = color
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FAQItem(question: String, answer: String) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = question,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = answer,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun PrivacySection(title: String, content: String) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun TermsSection(title: String, content: String) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun LicenseItem(name: String, license: String, description: String) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = license,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
