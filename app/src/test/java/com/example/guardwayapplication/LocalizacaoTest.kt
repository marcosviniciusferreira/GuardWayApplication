package com.example.guardwayapplication

import com.google.android.gms.maps.model.LatLng
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * ┌────────────────────────────────────────────────────────────────────────┐
 * │  Plano de Teste: T-01 — Validação da Lógica do Algoritmo de           │
 * │                          Periculosidade                                │
 * │                                                                        │
 * │  Ferramenta : JUnit 4 + MockK                                          │
 * │  Escopo     : Classe Localizacao (lógica de classificação de risco)    │
 * │  Execução   : JVM local (src/test/) — sem emulador                     │
 * └────────────────────────────────────────────────────────────────────────┘
 */
@RunWith(JUnit4::class)
class LocalizacaoTest {

    // ─── Fixture ─────────────────────────────────────────────────────────────

    private lateinit var localizacao: Localizacao

    /** Cria um Place stub com LatLng zerado para não depender do Maps SDK */
    private fun stubPlace(name: String, rating: Float): Place =
        Place(name = name, latLng = LatLng(0.0, 0.0), address = "Rua Teste, 0", rating = rating)

    @Before
    fun setUp() {
        localizacao = Localizacao()
    }

    // ─── T-01-TC01 a TC09: Fronteiras e Valores Típicos ──────────────────────

    @Test
    fun `T-01-TC01 rating 0_0 deve retornar PERIGOSO`() {
        assertEquals(Localizacao.RISCO_PERIGOSO, localizacao.classificarRisco(0.0f))
    }

    @Test
    fun `T-01-TC02 rating 1_0 (area critica) deve retornar PERIGOSO`() {
        assertEquals(Localizacao.RISCO_PERIGOSO, localizacao.classificarRisco(1.0f))
    }

    @Test
    fun `T-01-TC03 rating 2_0 (fronteira superior PERIGOSO) deve retornar PERIGOSO`() {
        assertEquals(Localizacao.RISCO_PERIGOSO, localizacao.classificarRisco(2.0f))
    }

    @Test
    fun `T-01-TC04 rating 2_1 (fronteira inferior MODERADO) deve retornar MODERADO`() {
        assertEquals(Localizacao.RISCO_MODERADO, localizacao.classificarRisco(2.1f))
    }

    @Test
    fun `T-01-TC05 rating 2_8 (centro MODERADO) deve retornar MODERADO`() {
        assertEquals(Localizacao.RISCO_MODERADO, localizacao.classificarRisco(2.8f))
    }

    @Test
    fun `T-01-TC06 rating 3_5 (fronteira superior MODERADO) deve retornar MODERADO`() {
        assertEquals(Localizacao.RISCO_MODERADO, localizacao.classificarRisco(3.5f))
    }

    @Test
    fun `T-01-TC07 rating 3_6 (fronteira inferior SEGURO) deve retornar SEGURO`() {
        assertEquals(Localizacao.RISCO_SEGURO, localizacao.classificarRisco(3.6f))
    }

    @Test
    fun `T-01-TC08 rating 4_5 (centro SEGURO) deve retornar SEGURO`() {
        assertEquals(Localizacao.RISCO_SEGURO, localizacao.classificarRisco(4.5f))
    }

    @Test
    fun `T-01-TC09 rating 5_0 (fronteira superior SEGURO) deve retornar SEGURO`() {
        assertEquals(Localizacao.RISCO_SEGURO, localizacao.classificarRisco(5.0f))
    }

    // ─── T-01-TC10 / TC11: Valores Inválidos ────────────────────────────────

    @Test(expected = IllegalArgumentException::class)
    fun `T-01-TC10 rating negativo deve lancar IllegalArgumentException`() {
        localizacao.classificarRisco(-0.1f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `T-01-TC11 rating acima de 5_0 deve lancar IllegalArgumentException`() {
        localizacao.classificarRisco(5.1f)
    }

    // ─── T-01-TC12 / TC13: Média de Risco ───────────────────────────────────

    @Test
    fun `T-01-TC12 calcularMediaRisco deve retornar media correta`() {
        val places = listOf(
            stubPlace("Local A", 1.0f),   // PERIGOSO
            stubPlace("Local B", 3.0f),   // MODERADO
            stubPlace("Local C", 5.0f)    // SEGURO
        )
        val media = localizacao.calcularMediaRisco(places)
        assertEquals(3.0f, media, 0.01f)  // (1+3+5)/3 = 3.0
    }

    @Test
    fun `T-01-TC13 calcularMediaRisco com lista vazia deve retornar 0_0`() {
        assertEquals(0.0f, localizacao.calcularMediaRisco(emptyList()), 0.0f)
    }

    // ─── T-01-TC14: Cores do Risco ───────────────────────────────────────────

    @Test
    fun `T-01-TC14a corDoRisco PERIGOSO deve ser vermelho`() {
        assertEquals("#FF0000", localizacao.corDoRisco(Localizacao.RISCO_PERIGOSO))
    }

    @Test
    fun `T-01-TC14b corDoRisco MODERADO deve ser laranja`() {
        assertEquals("#FFA500", localizacao.corDoRisco(Localizacao.RISCO_MODERADO))
    }

    @Test
    fun `T-01-TC14c corDoRisco SEGURO deve ser verde`() {
        assertEquals("#00AA00", localizacao.corDoRisco(Localizacao.RISCO_SEGURO))
    }

    @Test
    fun `T-01-TC14d corDoRisco desconhecido deve ser cinza`() {
        assertEquals("#808080", localizacao.corDoRisco("INVALIDO"))
    }

    // ─── T-01-TC15: Agrupamento por Risco ────────────────────────────────────

    @Test
    fun `T-01-TC15 agruparPorRisco deve contar locais por nivel`() {
        val places = listOf(
            stubPlace("P1", 0.5f),  // PERIGOSO
            stubPlace("P2", 1.8f),  // PERIGOSO
            stubPlace("M1", 2.5f),  // MODERADO
            stubPlace("S1", 4.0f),  // SEGURO
            stubPlace("S2", 4.9f)   // SEGURO
        )
        val grupos = localizacao.agruparPorRisco(places)

        assertEquals(2, grupos[Localizacao.RISCO_PERIGOSO])
        assertEquals(1, grupos[Localizacao.RISCO_MODERADO])
        assertEquals(2, grupos[Localizacao.RISCO_SEGURO])
    }

    // ─── T-01-TC16 / TC17: Área Crítica ─────────────────────────────────────

    @Test
    fun `T-01-TC16 isAreaCritica rating 1_0 deve retornar true`() {
        assertTrue(localizacao.isAreaCritica(1.0f))
    }

    @Test
    fun `T-01-TC17 isAreaCritica rating 1_1 deve retornar false`() {
        assertFalse(localizacao.isAreaCritica(1.1f))
    }

    // ─── T-01-TC18: Mock de Place com MockK ──────────────────────────────────

    @Test
    fun `T-01-TC18 MockK deve simular Place com rating e retornar classificacao correta`() {
        // Arrange: mock de um objeto Place sem depender do construtor real
        val placeMock = mockk<Place>()
        every { placeMock.rating } returns 1.5f

        // Act
        val risco = localizacao.classificarRisco(placeMock.rating)

        // Assert
        assertEquals(Localizacao.RISCO_PERIGOSO, risco)
        verify(exactly = 1) { placeMock.rating }
    }
}