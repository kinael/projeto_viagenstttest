package com.example.projeto_viagens.data.remote

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Interface Retrofit para a API generateContent do Gemini.
 *
 * Endpoint final:
 * POST https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent
 * Header obrigatório: x-goog-api-key: <API_KEY>
 */
interface GeminiApiService {

    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Header("x-goog-api-key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}
