package com.example.data.model

/**
 * Parsed and validated result shown in UI
 */
data class EcoAnalysisResult(
    val isTrash: Boolean,
    val pollutionScore: Int, // 0..10
    val trashTypes: List<String>,
    val summary: String,
    val harm: HarmDetails,
    val advice: String,
    val localImagePath: String? = null
) {
    val severityLevel: SeverityLevel
        get() = when (pollutionScore) {
            0 -> SeverityLevel.NONE
            in 1..3 -> SeverityLevel.LOW
            in 4..6 -> SeverityLevel.MEDIUM
            in 7..8 -> SeverityLevel.HIGH
            else -> SeverityLevel.CRITICAL
        }
}

data class HarmDetails(
    val nature: String,
    val health: String,
    val economy: String
)

enum class SeverityLevel(val title: String) {
    NONE("Чисто"),
    LOW("Слабое"),
    MEDIUM("Среднее"),
    HIGH("Высокое"),
    CRITICAL("Критическое")
}

/**
 * Groq Chat Completion DTOs
 */
data class GroqChatRequest(
    val model: String,
    val messages: List<GroqMessage>,
    val max_tokens: Int = 1500,
    val temperature: Double = 0.2
)

data class GroqMessage(
    val role: String,
    val content: Any // String or List<GroqContentPart>
)

data class GroqContentPart(
    val type: String,
    val text: String? = null,
    val image_url: GroqImageUrl? = null
)

data class GroqImageUrl(
    val url: String
)
