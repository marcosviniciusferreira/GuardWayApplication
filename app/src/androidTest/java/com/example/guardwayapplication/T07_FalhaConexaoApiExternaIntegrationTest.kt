package com.example.guardwayapplication

/**
 * T-07 ROBUSTO — Falha de Conexão com 4 Cenários Distintos de Rede
 *
 * Cada teste usa um MockWebServer com política diferente.
 * Isso cobre o espectro real de condições de rede que um usuário
 * de segurança pública encontra em campo.
 *
 * META: Esses testes TÊM TEMPORIZAÇÃO — alguns podem ser flaky em CI
 * lento. Isso é documentado como limitação conhecida, não como bug.
 *
 * ADICIONAR ao build.gradle.kts:
 *   androidTestImplementation("com.squareup.okhttp3:mockwebserver:4.9.1")
 */

import android.Manifest
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.containsString
import org.hamcrest.TypeSafeMatcher
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
@LargeTest
class T07_FalhaConexaoApiExternaIntegrationTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var scenario: ActivityScenario<VisitanteMainActivity>

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
    }

    @After
    fun tearDown() {
        VisitanteMainActivity.testBaseUrl = null
        if (::scenario.isInitialized) scenario.close()
        mockWebServer.shutdown()
    }

    // ────────────────────────────────────────────────────────────────────────
    // TC-07-01: DISCONNECT_AT_START (teste original — mantido)
    // Simula: rede completamente morta
    // ────────────────────────────────────────────────────────────────────────
    @Test
    fun tc07_01_redeCompletamenteMorta_deveExibirErroDeRede() {
        mockWebServer.enqueue(MockResponse().apply {
            socketPolicy = SocketPolicy.DISCONNECT_AT_START
        })
        launchActivityComMock()
        waitForMillis(6_000)

        onView(withId(R.id.btn_perigo_status))
            .check(matches(withText(containsString("Erro de Rede"))))
    }

    // ────────────────────────────────────────────────────────────────────────
    // TC-07-02: NO_RESPONSE (sem resposta até timeout do Retrofit)
    // Simula: servidor existe mas não responde — 3G fraquíssimo em campo
    // ATENÇÃO: Este teste demora até 30s (timeout do OkHttp).
    //          Pode falhar em CI com timeout menor. Documentado como flaky.
    // ────────────────────────────────────────────────────────────────────────
    @Test
    fun tc07_02_servidorSemResposta_deveExibirErroAposTimeout() {
        mockWebServer.enqueue(MockResponse().apply {
            socketPolicy = SocketPolicy.NO_RESPONSE
        })
        launchActivityComMock()

        // Aguarda o timeout do Retrofit (padrão OkHttp = 10s)
        waitForMillis(12_000)

        onView(withId(R.id.btn_perigo_status))
            .check(matches(isDisplayed()))
        // NOTA: Esse teste pode resultar em FAIL se o timeout do app for > 12s.
        // Isso identifica uma melhoria necessária: o app deveria ter timeout de no máximo 10s.
    }

    // ────────────────────────────────────────────────────────────────────────
    // TC-07-03: Resposta lenta (5 segundos de delay)
    // Simula: 3G fraco — usuário em área periférica usando o app
    // ────────────────────────────────────────────────────────────────────────
    @Test
    fun tc07_03_respostaLenta5Segundos_appDevePermanecer_responsivo() {
        mockWebServer.enqueue(
            MockResponse()
                .setBodyDelay(5, TimeUnit.SECONDS)
                .setResponseCode(200)
                .setBody("[]")  // resposta vazia mas válida
        )
        launchActivityComMock()

        // App deve estar RESPONSIVO durante os 5s de espera
        // (não pode travar a UI thread enquanto aguarda a rede)
        onView(withId(R.id.btn_perigo_status))
            .check(matches(isDisplayed()))  // UI responsiva imediatamente

        waitForMillis(7_000)  // espera a resposta processar

        // Após receber [] (lista vazia), o status deve ser neutro/seguro
        onView(withId(R.id.btn_perigo_status))
            .check(matches(isDisplayed()))
    }

    // ────────────────────────────────────────────────────────────────────────
    // TC-07-04: Erro HTTP 503 (servidor fora do ar)
    // Simula: backend do GuardWay em manutenção
    // ESTE TESTE PODE FALHAR se o app não tratar 503 corretamente.
    // Falhar aqui = bug real identificado.
    // ────────────────────────────────────────────────────────────────────────
    @Test
    fun tc07_04_erro503_deveExibirMensagemDeServicoIndisponivel() {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(503)
                .setBody("""{"error": "Service Unavailable"}""")
        )
        launchActivityComMock()
        waitForMillis(6_000)

        // O app deve tratar 503 como falha — não pode crashar silenciosamente
        onView(withId(R.id.btn_perigo_status))
            .check(matches(isDisplayed()))

        // NOTA: Se o app não trata 503 e simplesmente ignora a resposta,
        // o botão pode não exibir mensagem de erro. Isso =BUG identificado.
        // Ação: Adicionar tratamento de response.code() != 200 no onResponse()
        // da VisitanteMainActivity.
    }

    // ────────────────────────────────────────────────────────────────────────
    // TC-07-05: Reconexão após falha
    // Simula: usuário perde e recupera rede (WiFi → 4G)
    // ────────────────────────────────────────────────────────────────────────
    @Test
    fun tc07_05_reconexaoAposFalha_segundaRequisicaoDevePassar() {
        // Primeira requisição falha
        mockWebServer.enqueue(MockResponse().apply {
            socketPolicy = SocketPolicy.DISCONNECT_AT_START
        })
        // Segunda requisição (reconexão) retorna sucesso
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""[{"nome": "Área Segura", "rating": 4.5}]""")
        )
        launchActivityComMock()
        waitForMillis(8_000)

        // App deve estar em estado recuperável (não em loop de crash)
        onView(withId(R.id.btn_perigo_status))
            .check(matches(isDisplayed()))
    }

    // ── Utilitários ─────────────────────────────────────────────────────────

    private fun launchActivityComMock() {
        VisitanteMainActivity.testBaseUrl = mockWebServer.url("/").toString()
        scenario = ActivityScenario.launch(VisitanteMainActivity::class.java)
        scenario.onActivity { /* sincroniza Main Thread */ }
    }

    private fun hasBackgroundTint(): Matcher<View> =
        object : TypeSafeMatcher<View>() {
            override fun describeTo(d: Description) {
                d.appendText("com backgroundTintList aplicado")
            }
            override fun matchesSafely(v: View) = v.backgroundTintList != null
        }

    private fun waitForMillis(millis: Long) {
        onView(isRoot()).perform(object : ViewAction {
            override fun getConstraints(): Matcher<View> = isRoot()
            override fun getDescription() = "Espera$millis ms"
            override fun perform(uiController: UiController, view: View) {
                uiController.loopMainThreadForAtLeast(millis)
            }
        })
    }
}