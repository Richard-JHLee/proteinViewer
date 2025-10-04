package com.avas.proteinviewer.domain.model

enum class MenuItemType(val title: String, val icon: String, val description: String) {
    ABOUT(
        title = "About",
        icon = "info.circle",
        description = "App information and version"
    ),
    USER_GUIDE(
        title = "User Guide",
        icon = "book",
        description = "User guide"
    ),
    FEATURES(
        title = "Features",
        icon = "star",
        description = "Key features"
    ),
    SETTINGS(
        title = "Settings",
        icon = "gear",
        description = "App settings"
    ),
    HELP(
        title = "Help",
        icon = "questionmark.circle",
        description = "Help and FAQ"
    ),
    PRIVACY(
        title = "Privacy Policy",
        icon = "hand.raised",
        description = "Privacy Policy"
    ),
    TERMS(
        title = "Terms of Service",
        icon = "doc.text",
        description = "Terms of Service"
    ),
    LICENSE(
        title = "License",
        icon = "doc.plaintext",
        description = "License information"
    )
}
