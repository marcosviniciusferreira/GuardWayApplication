package com.example.guardwayapplication

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * ┌────────────────────────────────────────────────────────────────────────┐
 * │  Plano de Teste: T-05 — Integridade de Dados e Máscaras de Input      │
 * │                          de Cadastro                                   │
 * │                                                                        │
 * │  Ferramenta : JUnit 4                                                  │
 * │  Escopo     : Classe Usuario (validação de campos e máscaras)          │
 * │  Execução   : JVM local (src/test/) — sem emulador                     │
 * └────────────────────────────────────────────────────────────────────────┘
 */
@RunWith(JUnit4::class)
class UsuarioValidadorTest {

    private lateinit var usuario: UsuarioValidador

    @Before
    fun setUp() {
        usuario = UsuarioValidador()
    }

    // ─── E-MAIL ──────────────────────────────────────────────────────────────

    @Test
    fun `T-05-TC01 email padrao valido`() {
        assertTrue(usuario.validarEmail("joao.silva@empresa.com"))
    }

    @Test
    fun `T-05-TC02 email com subdominio co_br deve ser valido (correcao PDCA)`() {
        assertTrue(usuario.validarEmail("entregador@empresa.co.br"))
    }

    @Test
    fun `T-05-TC03 email sem arroba deve ser invalido`() {
        assertFalse(usuario.validarEmail("joaosilvaempresa.com"))
    }

    @Test
    fun `T-05-TC04 email sem dominio deve ser invalido`() {
        assertFalse(usuario.validarEmail("joao@"))
    }

    @Test
    fun `T-05-TC05 email com espaco interno deve ser invalido`() {
        assertFalse(usuario.validarEmail("joao silva@empresa.com"))
    }

    @Test
    fun `T-05-TC06 email em branco deve ser invalido`() {
        assertFalse(usuario.validarEmail(""))
    }

    @Test
    fun `T-05-TC07 email com tag de mais valido (RFC 5322)`() {
        assertTrue(usuario.validarEmail("joao+guardway@dominio.com"))
    }

    // ─── SENHA ───────────────────────────────────────────────────────────────

    @Test
    fun `T-05-TC08 senha valida completa`() {
        assertTrue(usuario.validarSenha("Guard@2024"))
    }

    @Test
    fun `T-05-TC09 senha sem numero deve ser invalida`() {
        assertFalse(usuario.validarSenha("Guardway@"))
    }

    @Test
    fun `T-05-TC10 senha sem caractere especial deve ser invalida`() {
        assertFalse(usuario.validarSenha("Guardway2024"))
    }

    @Test
    fun `T-05-TC11 senha com 7 caracteres deve ser invalida`() {
        assertFalse(usuario.validarSenha("Grd@1ab"))
    }

    @Test
    fun `T-05-TC12 senha minima valida com 8 caracteres`() {
        assertTrue(usuario.validarSenha("Ab1!cdef"))
    }

    // ─── NOME ────────────────────────────────────────────────────────────────

    @Test
    fun `T-05-TC13 nome valido com acentuacao`() {
        assertTrue(usuario.validarNome("João da Silva"))
    }

    @Test
    fun `T-05-TC14 nome com 2 caracteres deve ser invalido`() {
        assertFalse(usuario.validarNome("Jo"))
    }

    @Test
    fun `T-05-TC15 nome com numero deve ser invalido`() {
        assertFalse(usuario.validarNome("Jo3o Silva"))
    }

    @Test
    fun `T-05-TC16 nome em branco deve ser invalido`() {
        assertFalse(usuario.validarNome(""))
    }

    // ─── TELEFONE / MÁSCARA ───────────────────────────────────────────────────

    @Test
    fun `T-05-TC17 aplicarMascaraTelefone deve formatar 11 digitos corretamente`() {
        val resultado = usuario.aplicarMascaraTelefone("11987654321")
        assertEquals("(11) 98765-4321", resultado)
    }

    @Test
    fun `T-05-TC18 aplicarMascaraTelefone deve filtrar caracteres nao numericos`() {
        val resultado = usuario.aplicarMascaraTelefone("11-98765-4321")
        assertEquals("(11) 98765-4321", resultado)
    }

    @Test
    fun `T-05-TC19 aplicarMascaraTelefone com 10 digitos deve retornar null`() {
        assertNull(usuario.aplicarMascaraTelefone("1198765432"))
    }

    @Test
    fun `T-05-TC20 validarTelefone formatado deve retornar true`() {
        assertTrue(usuario.validarTelefone("(11) 98765-4321"))
    }

    @Test
    fun `T-05-TC21 validarTelefone sem parenteses deve retornar false`() {
        assertFalse(usuario.validarTelefone("11 98765-4321"))
    }

    // ─── CADASTRO COMPLETO ───────────────────────────────────────────────────

    @Test
    fun `T-05-TC22 cadastro completamente valido deve retornar lista de erros vazia`() {
        val erros = usuario.validarCadastroCompleto(
            nome      = "Maria Souza",
            email     = "maria@guardway.com.br",
            senha     = "Segur@2024",
            telefone  = "(11) 91234-5678"
        )
        assertTrue("Esperava zero erros, mas encontrou: $erros", erros.isEmpty())
    }

    @Test
    fun `T-05-TC23 cadastro com email e senha invalidos deve retornar 2 erros`() {
        val erros = usuario.validarCadastroCompleto(
            nome      = "Carlos Oliveira",
            email     = "emailinvalido",
            senha     = "fraca",
            telefone  = "(21) 99876-5432"
        )
        assertEquals(2, erros.size)
    }
}