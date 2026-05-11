package com.example.guardwayapplication

import android.Manifest
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.containsString
import org.hamcrest.TypeSafeMatcher
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * T07 — Falha de Conexão com API Externa
 *
 * Estratégia: MockWebServer (OkHttp)
 *
 * Por que MockWebServer e não MockK / repositório fake?
 * -------------------------------------------------------
 * O projeto não usa injeção de dependência (Hilt/Koin/Dagger). O Retrofit e o
 * ApiService são instanciados diretamente dentro de onCreate() de
 * VisitanteMainActivity usando uma constante BASE_URL hardcoded.
 *
 * Isso impede trocar o colaborador de rede via MockK sem refatorar a Activity.
 * O MockWebServer resolve o problema na camada de transporte HTTP:
 *   1. Sobe um servidor HTTP real na loopback (localhost) antes de qualquer teste.
 *   2. O teste injeta a baseUrl do MockWebServer no campo `apiService` da Activity
 *      logo após ela ser criada (via `onActivity { }`).
 *   3. O MockWebServer é configurado para fechar a conexão abruptamente,
 *      simulando um timeout/erro de rede real.
 *   4. O Retrofit dispara onFailure() → a Activity exibe "Erro de Rede".
 *
 * Dependência necessária em app/build.gradle.kts (androidTestImplementation):
 *   androidTestImplementation("com.squareup.okhttp3:mockwebserver:4.9.1")
 *
 * Certifique-se de que a versão bate com a do okhttp3 já declarado no projeto (4.9.x).
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class T07_FalhaConexaoApiExternaIntegrationTest {



    // ── 1. MockWebServer ─────────────────────────────────────────────────────
    private lateinit var mockWebServer: MockWebServer
    private lateinit var scenario: ActivityScenario<VisitanteMainActivity>

    // ── 2. Regras ────────────────────────────────────────────────────────────
    //
    // IMPORTANTE: NÃO use ActivityScenarioRule como @get:Rule direto aqui,
    // pois precisamos iniciar o MockWebServer *antes* de a Activity ser criada.
    // O RuleChain garante a ordem: permissões → MockWebServer (setUp) → Activity.
    //

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    private val activityRule = ActivityScenarioRule(VisitanteMainActivity::class.java)

    @get:Rule
    val ruleChain: RuleChain = RuleChain
        .outerRule(permissionRule)
        .around(activityRule)

    // ── 3. setUp / tearDown ──────────────────────────────────────────────────

    @Before
    fun setUp() {
        // ── 1. Sobe o servidor na thread de instrumentação ────────────────
        mockWebServer = MockWebServer()
        mockWebServer.start()
        mockWebServer.enqueue(MockResponse().apply {
            socketPolicy = okhttp3.mockwebserver.SocketPolicy.DISCONNECT_AT_START
        })

        // ── 2. Injeta a URL do mock ANTES de lançar a Activity ───────────
        // testBaseUrl é lido pelo onCreate() no lugar de BASE_URL.
        VisitanteMainActivity.testBaseUrl = mockWebServer.url("/").toString()

        // ── 3. Lança a Activity SÓ AGORA ─────────────────────────────────
        // onCreate() vai ler testBaseUrl e criar o apiService apontando
        // para o MockWebServer desde o primeiro momento.
        scenario = ActivityScenario.launch(VisitanteMainActivity::class.java)
    }

    @After
    fun tearDown() {
        // Limpa para não vazar entre testes
        VisitanteMainActivity.testBaseUrl = null
        scenario.close()
        mockWebServer.shutdown()
    }

    @Test
    fun t07_quandoApifalha_deveExibirErroDeRede() {
        // Barreira de sincronização
        scenario.onActivity { /* aguarda Main Thread processar */ }

        waitForMillis(6000)

        onView(withId(R.id.btn_perigo_status))
            .check(matches(isDisplayed()))

        onView(withId(R.id.btn_perigo_status))
            .check(matches(withText(containsString("Erro de Rede"))))

        onView(withId(R.id.btn_perigo_status))
            .check(matches(hasBackgroundTint()))
    }

    // ── 5. Custom Matchers ───────────────────────────────────────────────────

    /**
     * Verifica que o botão possui um backgroundTintList não nulo,
     * indicando que a cor de estado (erro, perigo ou seguro) foi aplicada.
     */
    private fun hasBackgroundTint(): Matcher<View> {
        return object : TypeSafeMatcher<View>() {
            override fun describeTo(description: Description) {
                description.appendText("com backgroundTintList aplicado (não nulo)")
            }
            override fun matchesSafely(view: View): Boolean {
                return view.backgroundTintList != null
            }
        }
    }

    // ── 6. Utilitário ────────────────────────────────────────────────────────

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
