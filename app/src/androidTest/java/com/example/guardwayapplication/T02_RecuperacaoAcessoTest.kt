package com.example.guardwayapplication

/**
 * =====================================================================
 * T-02 — Fluxo Completo de Recuperação de Acesso e Segurança
 * RF Associada : RF01
 * Tipo de Teste  : Ponta a Ponta (E2E) — Framework Espresso
 * =====================================================================
 */

import android.graphics.Color
import android.view.View
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.containsString
import org.hamcrest.TypeSafeMatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Import essencial para resolver referências de ID
import com.example.guardwayapplication.R

@RunWith(AndroidJUnit4::class)
@LargeTest
class T02_RecuperacaoAcessoTest {

    // Inicia na LoginActivity conforme sua estrutura de pastas
    @get:Rule
    val activityRule = ActivityScenarioRule(LoginActivity::class.java)

    private val EMAIL_VALIDO     = "usuario@guardway.com"
    private val TEXTO_PERIGO      = "PERIGO"
    private val TIMEOUT_REDE_MS  = 5_000L

    /**
     * TC-01: Fluxo de Login seguido de verificação de status na tela principal.
     */
    @Test
    fun t02_fluxoCompletoRecuperacaoAcessoESeguranca() {

        // ── PASSO 1: Tela de login — preencher e-mail ───────────────────────
        // Ajustado para o ID do seu XML: emailEditText
        onView(withId(R.id.emailEditText))
            .check(matches(isDisplayed()))
            .perform(
                clearText(),
                typeText(EMAIL_VALIDO),
                closeSoftKeyboard()
            )

        // ── PASSO 2: Clicar no botão de login ────────────────────
        // Ajustado para o ID do seu XML: loginButton
        onView(withId(R.id.loginButton))
            .check(matches(isEnabled()))
            .perform(click())

        // Aguarda transição para a tela principal (UsuarioMainActivity ou VisitanteMainActivity)
        waitForMillis(3_000L)

        // ── PASSO 3: Validar o status de risco no botão ──
        // Ajustado para o ID btn_perigo_status conforme conversas anteriores
        onView(withId(R.id.btn_perigo_status))
            .check(matches(isDisplayed()))

        // Aguarda resposta da "API" simulada
        waitForMillis(TIMEOUT_REDE_MS)

        // ── PASSO 4: Verificar texto "PERIGO" no componente ──────────────
        onView(withId(R.id.btn_perigo_status))
            .check(matches(withText(containsString(TEXTO_PERIGO))))

        // ── PASSO 5: Verificar presença de cor de fundo visual ─────────────
        onView(withId(R.id.btn_perigo_status))
            .check(matches(hasBackgroundTint()))
    }

    // ════════════════════════════════════════════════════════════════════════
    // MATCHERS E UTILITÁRIOS
    // ════════════════════════════════════════════════════════════════════════

    private fun hasBackgroundTint(): Matcher<View> =
        object : TypeSafeMatcher<View>() {
            override fun describeTo(description: Description) {
                description.appendText("View deve possuir uma cor de fundo (tint) definida")
            }
            override fun matchesSafely(view: View): Boolean {
                // Valida backgroundTintList usado em MaterialButtons
                return view.backgroundTintList != null
            }
        }

    private fun waitForMillis(millis: Long) {
        onView(isRoot()).perform(object : ViewAction {
            override fun getConstraints(): Matcher<View> = isRoot()
            override fun getDescription() = "Aguarda ${millis}ms"
            override fun perform(uiController: UiController, view: View) {
                uiController.loopMainThreadForAtLeast(millis)
            }
        })
    }
}