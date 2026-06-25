package com.example.projeto_viagens.data.repository

import com.example.projeto_viagens.data.local.RoteiroDao
import com.example.projeto_viagens.data.local.RoteiroEntity
import com.example.projeto_viagens.data.local.TripEntity
import com.example.projeto_viagens.data.remote.GeminiApiService
import com.example.projeto_viagens.data.remote.GeminiConfig
import com.example.projeto_viagens.data.remote.GeminiContent
import com.example.projeto_viagens.data.remote.GeminiGenerationConfig
import com.example.projeto_viagens.data.remote.GeminiPart
import com.example.projeto_viagens.data.remote.GeminiRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Resultado da geração do roteiro: sucesso (com texto) ou erro (com mensagem).
 */
sealed class RoteiroResult {
    data class Success(val conteudo: String) : RoteiroResult()
    data class Error(val message: String) : RoteiroResult()
}

/**
 * Repository responsável por:
 *  - montar o prompt a partir dos dados da viagem + interesses;
 *  - chamar a API do Gemini via Retrofit;
 *  - persistir o roteiro gerado no Room.
 *
 * Segue a separação de camadas recomendada (UI -> ViewModel -> Repository ->
 * fontes de dados remota/local).
 */
class RoteiroRepository(
    private val api: GeminiApiService,
    private val roteiroDao: RoteiroDao
) {

    /** Observa o roteiro mais recente persistido para a viagem. */
    fun observeRoteiro(tripId: Int): Flow<RoteiroEntity?> =
        roteiroDao.getLatestRoteiroByTrip(tripId)

    /**
     * Gera um roteiro turístico para a [trip] considerando os [interesses]
     * informados pelo usuário, persiste no banco e devolve o resultado.
     */
    suspend fun gerarRoteiro(trip: TripEntity, interesses: String): RoteiroResult =
        withContext(Dispatchers.IO) {
            if (!GeminiConfig.hasValidKey()) {
                return@withContext RoteiroResult.Error(
                    "API key do Gemini não configurada. Edite GeminiConfig.API_KEY."
                )
            }

            val prompt = montarPrompt(trip, interesses)

            try {
                val response = api.generateContent(
                    model = GeminiConfig.MODEL,
                    apiKey = GeminiConfig.API_KEY,
                    request = GeminiRequest(
                        contents = listOf(
                            GeminiContent(parts = listOf(GeminiPart(text = prompt)))
                        ),
                        generationConfig = GeminiGenerationConfig(temperature = 0.7)
                    )
                )

                val blockReason = response.promptFeedback?.blockReason
                if (blockReason != null) {
                    return@withContext RoteiroResult.Error(
                        "A solicitação foi bloqueada pela API ($blockReason)."
                    )
                }

                val texto = response.candidates
                    ?.firstOrNull()
                    ?.content
                    ?.parts
                    ?.firstOrNull()
                    ?.text
                    ?.trim()

                if (texto.isNullOrBlank()) {
                    return@withContext RoteiroResult.Error(
                        "A IA não retornou um roteiro. Tente novamente."
                    )
                }

                // Persiste o roteiro vinculado à viagem
                roteiroDao.insertRoteiro(
                    RoteiroEntity(
                        tripId = trip.id,
                        interesses = interesses,
                        conteudo = texto
                    )
                )
                roteiroDao.deleteOldRoteiros(trip.id)

                RoteiroResult.Success(texto)
            } catch (e: retrofit2.HttpException) {
                val code = e.code()
                val msg = when (code) {
                    400 -> "Requisição inválida (verifique a API key)."
                    403 -> "Acesso negado. API key inválida ou sem permissão."
                    429 -> "Limite de uso atingido. Tente novamente em instantes."
                    in 500..599 -> "Erro no servidor do Gemini ($code)."
                    else -> "Erro HTTP $code ao gerar o roteiro."
                }
                RoteiroResult.Error(msg)
            } catch (e: java.io.IOException) {
                RoteiroResult.Error("Sem conexão com a internet. Verifique sua rede.")
            } catch (e: Exception) {
                RoteiroResult.Error("Erro inesperado: ${e.message}")
            }
        }

    /** Monta o prompt textual enviado ao modelo. */
    private fun montarPrompt(trip: TripEntity, interesses: String): String {
        val interessesTexto = if (interesses.isBlank()) {
            "passeios variados e pontos turísticos populares"
        } else {
            interesses
        }

        return """
            Você é um guia de viagens especialista. Crie um roteiro turístico
            personalizado, prático e organizado por dias.

            Dados da viagem:
            - Destino: ${trip.destination}
            - Período: de ${trip.startDate} até ${trip.endDate}
            - Tipo de viagem: ${trip.type}
            - Orçamento aproximado: R$ ${"%.2f".format(trip.budget)}
            - Interesses do viajante: $interessesTexto

            Regras para a resposta:
            - Responda em português do Brasil.
            - Organize o roteiro por dia (Dia 1, Dia 2, ...), respeitando o período.
            - Para cada dia, sugira atividades de manhã, tarde e noite.
            - Inclua sugestões compatíveis com o orçamento e o tipo de viagem.
            - Seja objetivo e use texto corrido com títulos por dia (sem tabelas).
        """.trimIndent()
    }
}
