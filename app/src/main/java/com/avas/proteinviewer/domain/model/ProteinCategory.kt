package com.avas.proteinviewer.domain.model

enum class ProteinCategory(
    val displayName: String,
    val description: String,
    val searchTerms: List<String>,
    val color: Long,
    val icon: String
) {
    ENZYMES(
        displayName = "Enzymes",
        description = "생화학 반응을 촉매하는 효소들",
        searchTerms = listOf("enzyme", "catalase", "lysozyme", "ATP synthase", "kinase", "phosphatase"),
        color = 0xFF2196F3, // Blue (아이폰과 동일)
        icon = "E"
    ),
    STRUCTURAL(
        displayName = "Structural",
        description = "세포와 조직의 구조를 유지하는 단백질들",
        searchTerms = listOf("structural", "collagen", "actin", "tubulin", "keratin", "elastin"),
        color = 0xFFFF9800, // Orange (아이폰과 동일)
        icon = "S"
    ),
    DEFENSE(
        displayName = "Defense",
        description = "면역과 방어 기능을 하는 단백질들",
        searchTerms = listOf("defense", "antibody", "immunoglobulin", "complement", "antimicrobial", "immune"),
        color = 0xFFF44336, // Red (아이폰과 동일)
        icon = "D"
    ),
    TRANSPORT(
        displayName = "Transport",
        description = "물질 운반을 담당하는 단백질들",
        searchTerms = listOf("transport", "hemoglobin", "myoglobin", "channel", "pump", "carrier"),
        color = 0xFF4CAF50, // Green (아이폰과 동일)
        icon = "T"
    ),
    HORMONES(
        displayName = "Hormones",
        description = "신호 전달을 담당하는 호르몬 단백질들",
        searchTerms = listOf("hormone", "insulin", "glucagon", "growth factor", "messenger", "signal"),
        color = 0xFF9C27B0, // Purple (아이폰과 동일)
        icon = "H"
    ),
    STORAGE(
        displayName = "Storage",
        description = "영양분과 이온을 저장하는 단백질들",
        searchTerms = listOf("storage", "ferritin", "casein", "ovalbumin", "seed storage", "reserve"),
        color = 0xFF795548, // Brown (아이폰과 동일)
        icon = "S"
    ),
    RECEPTORS(
        displayName = "Receptors",
        description = "화학적 신호를 받는 수용체 단백질들",
        searchTerms = listOf("receptor", "GPCR", "ion channel", "signal", "binding", "transmembrane"),
        color = 0xFF00BCD4, // Cyan (아이폰과 동일)
        icon = "R"
    ),
    MEMBRANE(
        displayName = "Membrane",
        description = "세포막을 구성하고 조절하는 단백질들",
        searchTerms = listOf("membrane", "transmembrane", "channel", "pump", "transporter", "lipid"),
        color = 0xFF4DB6AC, // Mint (아이폰과 동일)
        icon = "M"
    ),
    MOTOR(
        displayName = "Motor",
        description = "기계적 힘을 생성하는 모터 단백질들",
        searchTerms = listOf("motor", "myosin", "kinesin", "dynein", "muscle", "contraction"),
        color = 0xFF3F51B5, // Indigo (아이폰과 동일)
        icon = "M"
    ),
    SIGNALING(
        displayName = "Signaling",
        description = "세포 간 통신에 관여하는 신호 전달 단백질들",
        searchTerms = listOf("signaling", "kinase", "phosphatase", "cascade", "transduction", "communication"),
        color = 0xFFE91E63, // Pink (아이폰과 동일)
        icon = "S"
    ),
    CHAPERONES(
        displayName = "Chaperones",
        description = "단백질 접힘을 돕는 챠퍼론 단백질들",
        searchTerms = listOf("chaperone", "folding", "HSP", "heat shock", "protein folding", "assistant"),
        color = 0xFFFFC107, // Yellow (아이폰과 동일)
        icon = "C"
    ),
    METABOLIC(
        displayName = "Metabolic",
        description = "대사 과정에 관여하는 단백질들",
        searchTerms = listOf("metabolic", "metabolism", "glycolysis", "citric acid", "oxidative", "energy"),
        color = 0xFF009688, // Teal (아이폰과 동일)
        icon = "M"
    );

    companion object {
        fun fromDisplayName(displayName: String): ProteinCategory? {
            return values().find { it.displayName == displayName }
        }
    }
}