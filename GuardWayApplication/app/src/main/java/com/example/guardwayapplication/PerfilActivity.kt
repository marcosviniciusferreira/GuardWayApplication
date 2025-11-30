package com.example.guardwayapplication

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import com.google.android.material.button.MaterialButton
import android.widget.ImageView // Importe ImageView para o btn_back

// IMPORTANTE: Adicione este import para usar registerForActivityResult
import androidx.activity.result.contract.ActivityResultContracts


class PerfilActivity : AppCompatActivity() {

    private lateinit var prefsManager: SharedPreferencesManager

    // Registra o launcher para a Activity de Edição
    private val editLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // Se a edição foi concluída com sucesso (RESULT_OK), recarrega o nome.
            val tvName = findViewById<TextView>(R.id.tv_name)
            val userName = prefsManager.getUserName()
            tvName.text = if (userName.isNullOrEmpty()) "Usuário Desconhecido" else userName
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil)

        prefsManager = SharedPreferencesManager(this)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        // REMOVIDO: supportActionBar?.setDisplayHomeAsUpEnabled(true)
        // -> Esta linha cria a seta padrão do ActionBar, duplicando o botão.
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // --- BOTÃO DE VOLTAR CUSTOMIZADO (@id/btn_back) ---
        val btnBack = findViewById<ImageView>(R.id.btn_back)
        btnBack.setOnClickListener {
            onBackPressed() // Usa o ImageView customizado
        }

        // REMOVIDO: toolbar.setNavigationOnClickListener { onBackPressed() }
        // -> Este listener era para a seta padrão que removemos acima.

        // --- EXIBIR DADOS DO USUÁRIO ---
        val tvName = findViewById<TextView>(R.id.tv_name)
        val userName = prefsManager.getUserName()
        tvName.text = if (userName.isNullOrEmpty()) "Usuário Desconhecido" else userName
        // -------------------------------

        // --- CONFIGURAÇÃO DOS ITENS DE MENU ---

        // 1. Editar Perfil
        val btnEditarPerfil = findViewById<CardView>(R.id.btn_editar_perfil)
        btnEditarPerfil.findViewById<TextView>(R.id.tv_item_title).text = "Editar Perfil"

        // Implementação da Edição de Perfil
        btnEditarPerfil.setOnClickListener {
            // 1. Recuperar todos os dados do usuário logado do SharedPreferences
            val userId = prefsManager.getUserId()
            val nome = prefsManager.getUserName()
            val email = prefsManager.getUserEmail()
            val cpf = prefsManager.getUserCpf()

            // 2. Construir o objeto Usuario com os dados completos (incluindo CPF)
            val usuarioLogado = Usuario(
                USUARIO_ID = userId,
                USUARIO_NOME = nome ?: "Nome Inválido",
                USUARIO_EMAIL = email ?: "email@invalido.com",
                USUARIO_CPF = cpf ?: "000.000.000-00",
                USUARIO_SENHA = "",
            )

            // 3. Criar Intent e passar o objeto
            val intent = Intent(this, UserFormActivity::class.java)
            intent.putExtra("USUARIO_EXTRA", usuarioLogado)

            // 4. Inicia a Activity usando o launcher para esperar um resultado
            editLauncher.launch(intent)
        }

        // 2. Minhas Ocorrências
        val btnMinhasOcorrencias = findViewById<CardView>(R.id.btn_minhas_ocorrencias)
        btnMinhasOcorrencias.findViewById<TextView>(R.id.tv_item_title).text = "Minhas Ocorrências"

        // 3. Sobre o Guardway
        val btnSobreGuardway = findViewById<CardView>(R.id.btn_sobre_guardway)
        btnSobreGuardway.findViewById<TextView>(R.id.tv_item_title).text = "Sobre o Guardway"

        // 4. Ajuda e Suporte
        val btnAjudaSuporte = findViewById<CardView>(R.id.btn_ajuda_suporte)
        btnAjudaSuporte.findViewById<TextView>(R.id.tv_item_title).text = "Ajuda e Suporte"

        // 5. Termos de uso
        val btnTermosUso = findViewById<CardView>(R.id.btn_termos_uso)
        btnTermosUso.findViewById<TextView>(R.id.tv_item_title).text = "Termos de uso"

        // --- Listener para o botão Sair ---
        val btnLogout = findViewById<MaterialButton>(R.id.btn_logout)
        btnLogout.setOnClickListener {
            prefsManager.clearData()

            val intent = Intent(this, LoginActivity::class.java)
            // Garante que o usuário não volte para a tela de Perfil após o logout
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)

            finish()
        }
    }
}