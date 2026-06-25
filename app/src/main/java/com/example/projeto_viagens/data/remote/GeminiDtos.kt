package com.example.projeto_viagens.data.remote

/**
 * DTOs (Data Transfer Objects) que espelham o corpo de request/response da
 * API generateContent do Gemini.
 *
 * Request:
 * {
 *   "contents": [ { "parts": [ { "text": "..." } ] } ],
 *   "generationConfig": { "temperature": 0.7 }
 * }
 *
 * Response:
 * {
 *   "candidates": [ { "content": { "parts": [ { "text": "..." } ] } } ]
 * }
 */

// ---------- Request ----------

data class GeminiRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig? = null
)

data class GeminiContent(
    val parts: List<GeminiPart>
)

data class GeminiPart(
    val text: String
)

data class GeminiGenerationConfig(
    val temperature: Double = 0.7
)

// ---------- Response ----------

data class GeminiResponse(
    val candidates: List<GeminiCandidate>? = null,
    val promptFeedback: GeminiPromptFeedback? = null
)

data class GeminiCandidate(
    val content: GeminiContent? = null,
    val finishReason: String? = null
)

data class GeminiPromptFeedback(
    val blockReason: String? = null
)
