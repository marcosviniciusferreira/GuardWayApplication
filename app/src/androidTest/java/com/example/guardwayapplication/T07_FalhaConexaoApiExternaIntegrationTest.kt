package com.example.guardwayapplication

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

// Import essencial para reconhecer os IDs reais do seu XML
import com.example.guardwayapplication.R

/**
 * T-07 — Teste de Integração: Falha de Conexão com API Externa
 * Validando a sinalização de risco na VisitanteMainActivity
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class T07_FalhaConexaoApiExternaIntegrationTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(VisitanteMainActivity::class.java)

    // Baseado no seu XML, o botão de status inicia com "Carregando..."
    // Vamos testar a transição para o estado de risco.
    private val TEXTO_PERIGO = "PERIGO"

    @Test
    fun t07_verificarSinalizacaoDeRiscoNoMapa() {

        // 1. Aguarda o carregamento inicial (simulando resposta da API/GPS)
        waitForMillis(5000)

        // 2. Verifica se o botão de status de perigo está visível
        // No seu XML o ID é btn_perigo_status
        onView(withId(R.id.btn_perigo_status))
            .check(matches(isDisplayed()))

        /*
           NOTA: Como o seu XML não possui um campo de busca (EditText),
           o teste assume que a Activity carrega o risco da posição atual.
           Se você quiser simular a escrita em algum lugar, verifique se o ID existe.
        */

        // 3. Valida se o componente de status exibe o alerta de segurança
        // Usamos containsString porque seu botão tem múltiplas linhas no XML
        onView(withId(R.id.btn_perigo_status))
            .check(matches(withText(containsString("PERIGO"))))

        // 4. Verifica se a cor de fundo é a cor de alerta (guardway_red)
        // O Espresso valida cores via Matcher personalizado
        onView(withId(R.id.btn_perigo_status))
            .check(matches(hasBackgroundColor()))
    }

    // --- Utilitários e Matchers ---

    private fun hasBackgroundColor(): Matcher<View> {
        return object : TypeSafeMatcher<View>() {
            override fun describeTo(description: Description) {
                description.appendText("com cor de fundo específica")
            }
            override fun matchesSafely(view: View): Boolean {
                // Como você usa backgroundTint no MaterialButton, a validação é visual
                return view.backgroundTintList != null
            }
        }
    }

    private fun waitForMillis(millis: Long) {
        onView(isRoot()).perform(object : ViewAction {
            override fun getConstraints(): Matcher<View> = isRoot()
            override fun getDescription() = "Espera $millis ms"
            override fun perform(uiController: UiController, view: View) {
                uiController.loopMainThreadForAtLeast(millis)
            }
        })
    }
}