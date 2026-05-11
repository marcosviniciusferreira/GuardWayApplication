package com.example.guardwayapplication

/**
 * =====================================================================
 * T-08 — Acessibilidade: Validação de contentDescription nos Botões Principais
 * RF Associada : RF05
 * Tipo de Teste  : Ponta a Ponta (E2E) — Framework Espresso
 * =====================================================================
 */

import android.view.View
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.*
import org.hamcrest.TypeSafeMatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.core.app.ActivityScenario
import android.Manifest
import androidx.test.rule.GrantPermissionRule

// Import essencial para resolver as referências de ID (R.id)
import com.example.guardwayapplication.R

@RunWith(AndroidJUnit4::class)
@MediumTest
class T08_AcessibilidadeTest {

    // O teste inicia na tela de Login
    @get:Rule
    val activityRule = ActivityScenarioRule(LoginActivity::class.java)

    // Concede permissão de GPS automaticamente para evitar crash

    @get:Rule

    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(

        Manifest.permission.ACCESS_FINE_LOCATION,

        Manifest.permission.ACCESS_COARSE_LOCATION

    )

    @Test
    fun t08_botaoLoginTemContentDescription() {
        // ID corrigido conforme seu layout de login: loginButton
        onView(withId(R.id.loginButton))
            .check(matches(isDisplayed()))
            .check(matches(hasNonEmptyContentDescription()))
    }

    // ════════════════════════════════════════════════════════════════════════
    // TC-02 — Botão de Perfil/Status possui contentDescription
    // ════════════════════════════════════════════════════════════════════════

    @Test
    fun t08_botaoStatusPerigoTemContentDescription() {

    // Lança a VisitanteMainActivity diretamente, sem depender de login
    ActivityScenario.launch(VisitanteMainActivity::class.java).use {
    onView(withId(R.id.btn_perigo_status))
    .check(matches(isDisplayed()))
    .check(matches(hasNonEmptyContentDescription()))
    }
    }

    // ════════════════════════════════════════════════════════════════════════
    // TC-03 — Verificação de campos de entrada (Email e Senha)
    // ════════════════════════════════════════════════════════════════════════

    @Test
    fun t08_camposDeTextoTemHintOuContentDescription() {
        // ID corrigido conforme seu layout de login: emailEditText
        onView(withId(R.id.emailEditText))
            .check(
                matches(
                    anyOf(
                        hasNonEmptyContentDescription(),
                        withHint(not(isEmptyOrNullString()))
                    )
                )
            )

        // ID corrigido conforme seu layout de login: passwordEditText
        onView(withId(R.id.passwordEditText))
            .check(
                matches(
                    anyOf(
                        hasNonEmptyContentDescription(),
                        withHint(not(isEmptyOrNullString()))
                    )
                )
            )
    }

    // ════════════════════════════════════════════════════════════════════════
    // MATCHER PERSONALIZADO — Requisito fundamental para TalkBack
    // ════════════════════════════════════════════════════════════════════════

    private fun hasNonEmptyContentDescription(): Matcher<View> =
        object : TypeSafeMatcher<View>() {
            override fun describeTo(description: Description) {
                description.appendText("View com contentDescription não vazio")
            }

            override fun matchesSafely(view: View): Boolean {
                val cd = view.contentDescription
                return !cd.isNullOrBlank()
            }
        }

    // ════════════════════════════════════════════════════════════════════════
    // UTILITÁRIOS DE NAVEGAÇÃO
    // ════════════════════════════════════════════════════════════════════════

    private fun navigateToMapScreen() {
        // Preenche email para avançar da tela de login
        onView(withId(R.id.emailEditText))
            .perform(typeText("acessibilidade@guardway.com"), closeSoftKeyboard())

        onView(withId(R.id.loginButton)).perform(click())

        // Aguarda transição para carregar VisitanteMainActivity ou UsuarioMainActivity
        waitForMillis(3_000L)
    }

    private fun waitForMillis(millis: Long) {
        onView(isRoot()).perform(object : ViewAction {
            override fun getConstraints(): Matcher<View> = isRoot()
            override fun getDescription() = "Aguarda $millis ms"
            override fun perform(uiController: UiController, view: View) {
                uiController.loopMainThreadForAtLeast(millis)
            }
        })
    }
}