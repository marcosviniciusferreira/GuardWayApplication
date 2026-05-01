package com.example.guardwayapplication

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class T03_AlertSyncIntegrationTest {

    private lateinit var usuario: UsuarioValidador

    @Before
    fun setUp() {
        // Se a sua classe Usuario pede parâmetros, você deve passá-los aqui
        // Exemplo baseado nos erros do seu print:
        usuario = UsuarioValidador()
    }

    @Test
    fun testEmailPadraoValido() {
        assertTrue(usuario.validarEmail("joao.silva@empresa.com"))
    }

    @Test
    fun testSenhaValidaCompleta() {
        assertTrue(usuario.validarSenha("Guard@2024"))
    }

}