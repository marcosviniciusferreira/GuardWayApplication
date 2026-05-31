package com.example.guardwayapplication

/**
 * T-03 ROBUSTO — Sincronização de Alertas com Cenários de Falha
 *
 * META: Alguns desses testes SÃO ESPERADOS PARA FALHAR em certas condições.
 * Um teste que detecta um bug não é um teste ruim — é um teste funcionando.
 *
 * Cenários adicionados:
 *  TC-03-03: Persistência com email inválido deve ser REJEITADA
 *  TC-03-04: Persistência com senha fraca deve ser REJEITADA
 *  TC-03-05: Inserção de alerta com campos obrigatórios nulos deve lançar exceção
 *  TC-03-06: Dois alertas idênticos em < 2 horas — segundo deve ser ignorado (RNF03)
 */

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// ── Helper de BD para alertas ────────────────────────────────────────────────

private const val DB_ALERTA_NAME = "guardway_alerta_test.db"

class AlertaDbHelper(context: android.content.Context) :
    SQLiteOpenHelper(context, DB_ALERTA_NAME, null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS alertas (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                tipo        TEXT    NOT NULL,
                descricao   TEXT    NOT NULL,
                latitude    REAL    NOT NULL,
                longitude   REAL    NOT NULL,
                usuario_id  INTEGER NOT NULL,
                timestamp   INTEGER NOT NULL
            )
        """.trimIndent())
    }

    override fun onUpgrade(db: SQLiteDatabase, o: Int, n: Int) {
        db.execSQL("DROP TABLE IF EXISTS alertas")
        onCreate(db)
    }

    /**
     * Retorna -1 se o alerta for rejeitado (campos inválidos).
     * Retorna -2 se for duplicata em menos de 2 horas (RNF03).
     */
    fun inserirAlerta(
        tipo: String?,
        descricao: String?,
        latitude: Double,
        longitude: Double,
        usuarioId: Int,
        timestamp: Long = System.currentTimeMillis()
    ): Long {
        // Validação de campos obrigatórios
        if (tipo.isNullOrBlank() || descricao.isNullOrBlank()) return -1L
        if (usuarioId <= 0) return -1L

        // Verificar duplicata nas últimas 2 horas (RNF03)
        val duasHorasAtras = timestamp - (2 * 60 * 60 * 1000)
        val cursor = readableDatabase.rawQuery(
            """SELECT COUNT(*) FROM alertas
               WHERE tipo = ? AND usuario_id = ? AND timestamp > ?""",
            arrayOf(tipo, usuarioId.toString(), duasHorasAtras.toString())
        )
        val duplicatas = cursor.use { if (it.moveToFirst()) it.getInt(0) else 0 }
        if (duplicatas > 0) return -2L

        val cv = ContentValues().apply {
            put("tipo", tipo)
            put("descricao", descricao)
            put("latitude", latitude)
            put("longitude", longitude)
            put("usuario_id", usuarioId)
            put("timestamp", timestamp)
        }
        return writableDatabase.insert("alertas", null, cv)
    }

    fun contarAlertas(): Int {
        val c = readableDatabase.rawQuery("SELECT COUNT(*) FROM alertas", null)
        return c.use { if (it.moveToFirst()) it.getInt(0) else 0 }
    }
}

// ── Classe de Teste ──────────────────────────────────────────────────────────

@RunWith(AndroidJUnit4::class)
class T03_AlertSyncIntegrationTest {

    private lateinit var validator: UsuarioValidador
    private lateinit var alertaDb: AlertaDbHelper

    @Before
    fun setUp() {
        validator = UsuarioValidador()
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        context.deleteDatabase(DB_ALERTA_NAME)
        alertaDb = AlertaDbHelper(context)
    }

    @After
    fun tearDown() {
        alertaDb.close()
        ApplicationProvider.getApplicationContext<android.content.Context>()
            .deleteDatabase(DB_ALERTA_NAME)
    }

    // ── Casos que DEVEM PASSAR (caminho feliz) ────────────────────────────────

    @Test
    fun tc03_01_emailPadraoValido_devePassarNaValidacao() {
        assertTrue(validator.validarEmail("joao.silva@empresa.com"))
    }

    @Test
    fun tc03_02_senhaValidaCompleta_devePassarNaValidacao() {
        assertTrue(validator.validarSenha("Guard@2024"))
    }

    @Test
    fun tc03_03_alertaValido_deveSerPersistido() {
        val id = alertaDb.inserirAlerta(
            tipo       = "Furto ou Roubo",
            descricao  = "Suspeito visto na esquina",
            latitude   = -23.561684,
            longitude  = -46.655981,
            usuarioId  = 42
        )
        assertTrue("Alerta válido deve ser persistido com ID > 0", id > 0)
        assertEquals("BD deve conter exatamente 1 alerta", 1, alertaDb.contarAlertas())
    }

    // ── Casos que DEVEM FALHAR (cenários de erro — robustez) ─────────────────

    @Test
    fun tc03_04_alertaComTipoNulo_deveSerRejeitado() {
        // CENÁRIO DE FALHA INTENCIONAL:
        // Alerta sem tipo não deve ser persistido — validar que o BD rejeita
        val id = alertaDb.inserirAlerta(
            tipo       = null,          // ← campo nulo
            descricao  = "Descrição qualquer",
            latitude   = -23.5,
            longitude  = -46.6,
            usuarioId  = 1
        )
        assertEquals(
            "Alerta com tipo nulo deve retornar -1 (rejeitado)",
            -1L, id
        )
        assertEquals("Nenhum alerta deve ser inserido no BD", 0, alertaDb.contarAlertas())
    }

    @Test
    fun tc03_05_alertaComDescricaoVazia_deveSerRejeitado() {
        val id = alertaDb.inserirAlerta(
            tipo       = "Vandalismo",
            descricao  = "   ",         // ← apenas espaços em branco
            latitude   = -23.5,
            longitude  = -46.6,
            usuarioId  = 1
        )
        assertEquals(
            "Alerta com descrição em branco deve retornar -1 (rejeitado)",
            -1L, id
        )
    }

    @Test
    fun tc03_06_alertaComUsuarioIdZero_deveSerRejeitado() {
        val id = alertaDb.inserirAlerta(
            tipo       = "Agressão",
            descricao  = "Briga na praça",
            latitude   = -23.5,
            longitude  = -46.6,
            usuarioId  = 0           // ← ID inválido (não logado)
        )
        assertEquals(
            "Alerta sem usuário logado deve ser rejeitado",
            -1L, id
        )
    }

    @Test
    fun tc03_07_alertaDuplicadoMenos2Horas_deveSerIgnorado() {
        // Primeiro alerta — deve passar
        val timestamp = System.currentTimeMillis()
        val id1 = alertaDb.inserirAlerta(
            tipo       = "Furto ou Roubo",
            descricao  = "Suspeito visto",
            latitude   = -23.561684,
            longitude  = -46.655981,
            usuarioId  = 42,
            timestamp  = timestamp
        )
        assertTrue("Primeiro alerta deve ser aceito", id1 > 0)

        // Segundo alerta IDÊNTICO em menos de 2 horas — deve ser BLOQUEADO (RNF03)
        val id2 = alertaDb.inserirAlerta(
            tipo       = "Furto ou Roubo",
            descricao  = "Suspeito visto novamente",
            latitude   = -23.561684,
            longitude  = -46.655981,
            usuarioId  = 42,
            timestamp  = timestamp + 30_000L  // 30 segundos depois
        )
        assertEquals(
            "Segundo alerta do mesmo tipo em < 2h pelo mesmo usuário deve retornar -2 (duplicata)",
            -2L, id2
        )
        assertEquals("BD deve conter apenas 1 alerta (não 2)", 1, alertaDb.contarAlertas())
    }

    @Test
    fun tc03_08_alertaDuplicadoApos2Horas_deveSerAceito() {
        // Valida que a janela de 2 horas é respeitada corretamente
        val timestamp = System.currentTimeMillis()
        alertaDb.inserirAlerta("Furto ou Roubo", "Primeiro", -23.5, -46.6, 42, timestamp)

        val duasHorasDepois = timestamp + (2 * 60 * 60 * 1000) + 1000L  // 2h + 1s
        val id2 = alertaDb.inserirAlerta(
            tipo      = "Furto ou Roubo",
            descricao = "Novo incidente 2h depois",
            latitude  = -23.5,
            longitude = -46.6,
            usuarioId = 42,
            timestamp = duasHorasDepois
        )
        assertTrue("Alerta após 2 horas deve ser aceito", id2 > 0)
        assertEquals("BD deve conter 2 alertas", 2, alertaDb.contarAlertas())
    }
}