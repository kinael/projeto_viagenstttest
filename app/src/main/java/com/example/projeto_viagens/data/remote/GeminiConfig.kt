package com.example.projeto_viagens.data.remote

/**
 * Configuração de acesso à API do Gemini.
 *
 * IMPORTANTE: substitua [API_KEY] pela sua chave gerada no Google AI Studio
 * (https://aistudio.google.com/app/apikey).
 *
 * Em um app de produção a chave NÃO deveria ficar no código-fonte (e sim em
 * local.properties / BuildConfig ou em um backend). Para fins do trabalho
 * acadêmico, mantemos aqui de forma centralizada e fácil de trocar.
 */
object GeminiConfig {

    // 🔑 Cole sua API key do Gemini aqui:
    const val API_KEY = "chave"

    // Modelo solicitado na tarefa
    const val MODEL = "gemini-2.5-flash"

    const val BASE_URL = "https://generativelanguage.googleapis.com/"

    fun hasValidKey(): Boolean =
        API_KEY.isNotBlank() && API_KEY != "COLE_SUA_API_KEY_AQUI"
}
