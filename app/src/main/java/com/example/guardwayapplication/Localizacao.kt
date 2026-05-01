package com.example.guardwayapplication

/**
 * Classe de domínio responsável pelo algoritmo de classificação de periculosidade.
 *
 * Regras de negócio (T-01):
 *   rating 0.0 – 2.0  → PERIGOSO  (vermelho)
 *   rating 2.1 – 3.5  → MODERADO  (amarelo)
 *   rating 3.6 – 5.0  → SEGURO    (verde)
 *
 * Cobertura de testes: LocalizacaoTest.kt
 */
class Localizacao {

    companion object {
        const val RISCO_PERIGOSO = "PERIGOSO"
        const val RISCO_MODERADO = "MODERADO"
        const val RISCO_SEGURO   = "SEGURO"

        const val LIMITE_PERIGOSO = 2.0f
        const val LIMITE_MODERADO = 3.5f
        const val RATING_MAXIMO   = 5.0f
        const val RATING_MINIMO   = 0.0f
    }

    /**
     * Classifica o nível de risco de um local com base no rating.
     *
     * @param rating valor entre 0.0 e 5.0
     * @return String com o nível de risco: "PERIGOSO", "MODERADO" ou "SEGURO"
     * @throws IllegalArgumentException se rating estiver fora do intervalo [0.0, 5.0]
     */
    fun classificarRisco(rating: Float): String {
        require(rating in RATING_MINIMO..RATING_MAXIMO) {
            "Rating inválido: $rating. Deve estar entre $RATING_MINIMO e $RATING_MAXIMO."
        }

        return when {
            rating <= LIMITE_PERIGOSO -> RISCO_PERIGOSO
            rating <= LIMITE_MODERADO -> RISCO_MODERADO
            else                       -> RISCO_SEGURO
        }
    }

    /**
     * Calcula a média ponderada de risco de uma lista de locais.
     * Retorna 0.0f se a lista estiver vazia.
     */
    fun calcularMediaRisco(places: List<Place>): Float {
        if (places.isEmpty()) return 0.0f
        return places.map { it.rating }.average().toFloat()
    }

    /**
     * Retorna a cor hexadecimal associada ao nível de risco para exibição no mapa.
     */
    fun corDoRisco(risco: String): String = when (risco) {
        RISCO_PERIGOSO -> "#FF0000"   // Vermelho
        RISCO_MODERADO -> "#FFA500"   // Laranja/Amarelo
        RISCO_SEGURO   -> "#00AA00"   // Verde
        else            -> "#808080"  // Cinza (desconhecido)
    }

    /**
     * Agrega os locais em grupos por nível de risco.
     * Retorna um mapa: { "PERIGOSO" -> 3, "MODERADO" -> 5, "SEGURO" -> 12 }
     */
    fun agruparPorRisco(places: List<Place>): Map<String, Int> {
        return places.groupBy { classificarRisco(it.rating) }
            .mapValues { it.value.size }
    }

    /**
     * Verifica se um local deve exibir alerta crítico
     * (rating <= 1.0 indica área crítica).
     */
    fun isAreaCritica(rating: Float): Boolean {
        require(rating in RATING_MINIMO..RATING_MAXIMO) {
            "Rating inválido: $rating."
        }
        return rating <= 1.0f
    }
}