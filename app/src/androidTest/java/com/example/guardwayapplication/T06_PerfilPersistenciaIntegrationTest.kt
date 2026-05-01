package com.example.guardwayapplication

// ============================================================
// T-06 — Persistência de Dados e Atualização de Perfil de Utilizador
// Índice de Risco: 20 | Tipo: Integração (RF02 / RF04)
// Ferramenta: AndroidJUnitRunner + androidx.test
// Precondição: Servidor de arquivos e BD ativos; usuário previamente cadastrado
// ============================================================

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
import java.io.File

// ---------------------------------------------------------------
// Entidade de domínio: UsuarioPerfil
// ---------------------------------------------------------------

/**
 * Representa o perfil de um usuário do GuardWay.
 * statusVerificacao pode ser: "Pendente", "Em análise", "Verificado", "Rejeitado".
 */
data class UsuarioPerfil(
    val id: Int,
    val nome: String,
    val email: String,
    val cpf: String,
    val statusVerificacao: String,
    val reputacao: Int           // RF04 — sistema de boa reputação
)

// ---------------------------------------------------------------
// Helper de banco de dados — tabela de usuários
// ---------------------------------------------------------------

private const val DB_PERFIL_NAME = "guardway_perfil_test.db"

class PerfilDbHelper(context: android.content.Context) :
    SQLiteOpenHelper(context, DB_PERFIL_NAME, null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS usuarios (
                id                   INTEGER PRIMARY KEY AUTOINCREMENT,
                nome                 TEXT    NOT NULL,
                email                TEXT    NOT NULL UNIQUE,
                cpf                  TEXT    NOT NULL,
                status_verificacao   TEXT    NOT NULL DEFAULT 'Pendente',
                reputacao            INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS documentos_upload (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                usuario_id  INTEGER NOT NULL,
                caminho     TEXT    NOT NULL,
                tipo        TEXT    NOT NULL,
                timestamp   INTEGER NOT NULL,
                FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS documentos_upload")
        db.execSQL("DROP TABLE IF EXISTS usuarios")
        onCreate(db)
    }

    fun inserirUsuario(nome: String, email: String, cpf: String): Long {
        val cv = ContentValues().apply {
            put("nome", nome)
            put("email", email)
            put("cpf", cpf)
            put("status_verificacao", "Pendente")
            put("reputacao", 0)
        }
        return writableDatabase.insert("usuarios", null, cv)
    }

    fun buscarPorId(id: Int): UsuarioPerfil? {
        val cursor = readableDatabase.query(
            "usuarios",
            null, "id = ?", arrayOf(id.toString()),
            null, null, null
        )
        return cursor.use {
            if (it.moveToFirst()) {
                UsuarioPerfil(
                    id                 = it.getInt(it.getColumnIndexOrThrow("id")),
                    nome               = it.getString(it.getColumnIndexOrThrow("nome")),
                    email              = it.getString(it.getColumnIndexOrThrow("email")),
                    cpf                = it.getString(it.getColumnIndexOrThrow("cpf")),
                    statusVerificacao  = it.getString(it.getColumnIndexOrThrow("status_verificacao")),
                    reputacao          = it.getInt(it.getColumnIndexOrThrow("reputacao"))
                )
            } else null
        }
    }

    fun atualizarStatus(usuarioId: Int, novoStatus: String): Int {
        val cv = ContentValues().apply { put("status_verificacao", novoStatus) }
        return writableDatabase.update("usuarios", cv, "id = ?", arrayOf(usuarioId.toString()))
    }

    fun atualizarReputacao(usuarioId: Int, delta: Int): Int {
        writableDatabase.execSQL(
            "UPDATE usuarios SET reputacao = reputacao + ? WHERE id = ?",
            arrayOf(delta, usuarioId)
        )
        return buscarPorId(usuarioId)?.reputacao ?: -1
    }

    fun registrarUploadDocumento(usuarioId: Int, caminho: String, tipo: String): Long {
        val cv = ContentValues().apply {
            put("usuario_id", usuarioId)
            put("caminho", caminho)
            put("tipo", tipo)
            put("timestamp", System.currentTimeMillis())
        }
        return writableDatabase.insert("documentos_upload", null, cv)
    }

    fun contarDocumentosDoUsuario(usuarioId: Int): Int {
        val cursor = readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM documentos_upload WHERE usuario_id = ?",
            arrayOf(usuarioId.toString())
        )
        return cursor.use { if (it.moveToFirst()) it.getInt(0) else 0 }
    }
}

// ---------------------------------------------------------------
// Serviço de verificação de perfil (simula a lógica de backend — RF02)
// ---------------------------------------------------------------

/**
 * VerificacaoService representa a lógica que processa documentos enviados
 * e atualiza o status do usuário no banco.
 *
 * Em produção, esta lógica estaria em um ViewModel ou Repository.
 */
class VerificacaoService(private val db: PerfilDbHelper) {

    /**
     * Processa o upload de um documento e altera o status para "Em análise".
     * @param usuarioId  ID do usuário que enviou
     * @param arquivo    Arquivo simulado (PDF ou JPG)
     * @return true se o fluxo ocorreu sem erros
     */
    fun processarUploadDocumento(usuarioId: Int, arquivo: File): Boolean {
        // 1. Validar extensão (PDF ou JPG)
        val extensaoValida = arquivo.extension.lowercase() in listOf("pdf", "jpg", "jpeg")
        if (!extensaoValida) return false

        // 2. Registrar o upload na tabela de documentos
        val uploadId = db.registrarUploadDocumento(
            usuarioId = usuarioId,
            caminho   = arquivo.absolutePath,
            tipo      = arquivo.extension.uppercase()
        )
        if (uploadId < 0) return false

        // 3. Atualizar status do usuário para "Em análise"
        val linhasAfetadas = db.atualizarStatus(usuarioId, "Em análise")
        return linhasAfetadas > 0
    }

    /**
     * Simula aprovação manual pelo administrador — muda status para "Verificado"
     * e incrementa reputação base (RF04).
     */
    fun aprovarVerificacao(usuarioId: Int): UsuarioPerfil? {
        db.atualizarStatus(usuarioId, "Verificado")
        db.atualizarReputacao(usuarioId, 10)  // +10 pontos de reputação base
        return db.buscarPorId(usuarioId)
    }
}

// ---------------------------------------------------------------
// Classe de Teste T-06
// ---------------------------------------------------------------

@RunWith(AndroidJUnit4::class)
class T06_PerfilPersistenciaIntegrationTest {

    private lateinit var db: PerfilDbHelper
    private lateinit var servico: VerificacaoService
    private lateinit var cacheDir: File
    private var usuarioIdTeste: Int = -1

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        context.deleteDatabase(DB_PERFIL_NAME)
        db       = PerfilDbHelper(context)
        servico  = VerificacaoService(db)
        cacheDir = context.cacheDir

        // Pré-condição: usuário cadastrado com status inicial "Pendente"
        val id = db.inserirUsuario(
            nome  = "André Arruda Seter",
            email = "andre.seter@guardway.com",
            cpf   = "12345678901"
        )
        usuarioIdTeste = id.toInt()
    }

    @After
    fun tearDown() {
        db.close()
        ApplicationProvider.getApplicationContext<android.content.Context>()
            .deleteDatabase(DB_PERFIL_NAME)
    }

    // ------------------------------------------------------------------
    // TC-06-01: Status inicial deve ser "Pendente" após cadastro
    // ------------------------------------------------------------------
    @Test
    fun tc06_01_usuarioCadastrado_statusInicialPendente() {
        val usuario = db.buscarPorId(usuarioIdTeste)
        assertNotNull("Usuário deve ser encontrado no banco", usuario)
        assertEquals("Status inicial deve ser 'Pendente'", "Pendente", usuario!!.statusVerificacao)
        assertEquals("Reputação inicial deve ser 0", 0, usuario.reputacao)
    }

    // ------------------------------------------------------------------
    // TC-06-02: Upload de documento PDF → status muda para "Em análise"
    //           (Resultado esperado do Plano de Testes T-06)
    // ------------------------------------------------------------------
    @Test
    fun tc06_02_uploadDocumentoPDF_statusEmAnalise() {
        // Simula arquivo PDF de documento de identidade
        val docPdf = File(cacheDir, "rg_andre_seter.pdf").apply { createNewFile() }

        val sucesso = servico.processarUploadDocumento(usuarioIdTeste, docPdf)

        assertTrue("Processamento do upload deve retornar sucesso", sucesso)

        val usuarioAtualizado = db.buscarPorId(usuarioIdTeste)
        assertNotNull(usuarioAtualizado)
        assertEquals(
            "Status deve mudar para 'Em análise' após upload de documento",
            "Em análise",
            usuarioAtualizado!!.statusVerificacao
        )
    }

    // ------------------------------------------------------------------
    // TC-06-03: Upload de documento JPG → status muda para "Em análise"
    // ------------------------------------------------------------------
    @Test
    fun tc06_03_uploadDocumentoJPG_statusEmAnalise() {
        val docJpg = File(cacheDir, "cnh_frente.jpg").apply { createNewFile() }

        val sucesso = servico.processarUploadDocumento(usuarioIdTeste, docJpg)

        assertTrue(sucesso)
        assertEquals(
            "Status deve mudar para 'Em análise' após upload de JPG",
            "Em análise",
            db.buscarPorId(usuarioIdTeste)!!.statusVerificacao
        )
    }

    // ------------------------------------------------------------------
    // TC-06-04: Extensão inválida (.txt) → status NÃO deve ser alterado
    // ------------------------------------------------------------------
    @Test
    fun tc06_04_extensaoInvalida_statusPermanecePendente() {
        val arquivoInvalido = File(cacheDir, "notas.txt").apply { createNewFile() }

        val sucesso = servico.processarUploadDocumento(usuarioIdTeste, arquivoInvalido)

        assertFalse("Upload com extensão inválida deve falhar", sucesso)
        assertEquals(
            "Status não deve mudar com arquivo inválido",
            "Pendente",
            db.buscarPorId(usuarioIdTeste)!!.statusVerificacao
        )
    }

    // ------------------------------------------------------------------
    // TC-06-05: Aprovação manual → status "Verificado" e reputação incrementada (RF04)
    // ------------------------------------------------------------------
    @Test
    fun tc06_05_aprovacaoManual_statusVerificadoEReputacaoIncrementada() {
        // Pré: simula que admin aprovou
        val usuarioVerificado = servico.aprovarVerificacao(usuarioIdTeste)

        assertNotNull(usuarioVerificado)
        assertEquals(
            "Status após aprovação deve ser 'Verificado'",
            "Verificado",
            usuarioVerificado!!.statusVerificacao
        )
        assertTrue(
            "Reputação deve ser maior que zero após verificação",
            usuarioVerificado.reputacao > 0
        )
    }

    // ------------------------------------------------------------------
    // TC-06-06: Persistência após múltiplas operações — dados íntegros no banco
    // ------------------------------------------------------------------
    @Test
    fun tc06_06_persistenciaMultiplasOperacoes_dadosIntegros() {
        val doc1 = File(cacheDir, "doc1.pdf").apply { createNewFile() }
        val doc2 = File(cacheDir, "doc2.jpg").apply { createNewFile() }

        servico.processarUploadDocumento(usuarioIdTeste, doc1)
        servico.processarUploadDocumento(usuarioIdTeste, doc2)

        val totalDocs = db.contarDocumentosDoUsuario(usuarioIdTeste)
        assertEquals("Dois documentos devem estar salvos no banco", 2, totalDocs)

        // Dados originais do perfil devem continuar íntegros
        val usuario = db.buscarPorId(usuarioIdTeste)!!
        assertEquals("Nome deve ser preservado", "André Arruda Seter", usuario.nome)
        assertEquals("CPF deve ser preservado", "12345678901", usuario.cpf)
    }
}