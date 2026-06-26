package com.example.projeto_viagens.data.remote

object GeminiConfig {

    const val API_KEY = "chave"

    const val MODEL = "gemini-2.5-flash"

    const val BASE_URL = "https://generativelanguage.googleapis.com/"

    fun hasValidKey(): Boolean =
        API_KEY.isNotBlank() && API_KEY != "COLE_SUA_API_KEY_AQUI"
}
