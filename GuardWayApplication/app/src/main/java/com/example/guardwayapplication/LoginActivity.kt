package com.example.guardwayapplication

import ApiService
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView // Adicionado para o botão de voltar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar // Import correto para Toolbar
import com.google.android.material.button.MaterialButton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

data class LoginResponse(
    val usuarioId: Int,
    val usuarioNome: String,
    val usuarioEmail: String,
    val usuarioCpf: String
)

class LoginActivity : AppCompatActivity() {

    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var prefsManager: SharedPreferencesManager
    private lateinit var toolbarLogin: Toolbar // CORRIGIDO: Deve ser Toolbar, não ImageView
    private lateinit var btnBack: ImageView // NOVO: Referência ao ícone de voltar
    private lateinit var createAccountButton: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Inicializa o gerenciador de SharedPreferences
        prefsManager = SharedPreferencesManager(this)

        // 1. Inicializa a Toolbar
        toolbarLogin = findViewById(R.id.toolbar_login) // CORRIGIDO: Usando o ID da Toolbar
        setSupportActionBar(toolbarLogin) // Configura a Toolbar como ActionBar

        // Desativa a exibição do título padrão da ActionBar
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // 2. Inicializa e configura o botão de Voltar (ImageView)
        btnBack = findViewById(R.id.btn_back_toolbar)

        // Listener para o ícone de voltar (ImageView)
        btnBack.setOnClickListener {
            // Chama a função padrão de volta do Android.
            onBackPressed()
        }

        // Seus Listeners anteriores estavam duplicados ou incorretos, foram removidos/corrigidos.

        // 3. Inicializa os campos e botões
        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        val loginButton: Button = findViewById(R.id.loginButton)
        createAccountButton = findViewById(R.id.createAccountButton)
        val forgotPasswordTextView: TextView = findViewById(R.id.tv_forgot_password)

        // Garante que o título interno da Toolbar esteja nulo (redundante, mas seguro)
        toolbarLogin.title = null

        // 4. Listeners de clique
        loginButton.setOnClickListener {
            performLogin()
        }

        createAccountButton.setOnClickListener {
            // TODO: Lógica de navegação para a tela de Cadastro
            Toast.makeText(this, "Navegando para Criar Nova Conta...", Toast.LENGTH_SHORT).show()
        }

        forgotPasswordTextView.setOnClickListener {
            // TODO: Lógica para a recuperação de senha
            Toast.makeText(this, "Abrindo tela de Esqueci Senha...", Toast.LENGTH_SHORT).show()
        }
    }

    // Sobrescreve a função de volta para definir a navegação padrão (opcional)
    override fun onBackPressed() {
        super.onBackPressed()
        // Navega para a Activity VisitanteMainActivity
        val intent = Intent(this@LoginActivity, VisitanteMainActivity::class.java)
        startActivity(intent)
        finish() // Fecha a tela de Login
    }

    private fun performLogin() {
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Preencha e-mail e senha para continuar.", Toast.LENGTH_SHORT)
                .show()
            return
        }

        val retrofit = Retrofit.Builder()
            .baseUrl("http://192.168.1.4/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(ApiService::class.java)

        val call = apiService.login(email, password)

        call.enqueue(object : Callback<List<LoginResponse>> {

            override fun onResponse(
                call: Call<List<LoginResponse>>,
                response: Response<List<LoginResponse>>
            ) {
                if (response.isSuccessful && response.body() != null) {

                    val loginResponses = response.body()!!
                    if (loginResponses.isNotEmpty()) {

                        val userData = loginResponses.first()

                        // 🌟 SALVA TODOS OS DADOS DO USUÁRIO NO SHARED PREFERENCES
                        prefsManager.saveUserData(
                            id = userData.usuarioId,
                            nome = userData.usuarioNome,
                            email = userData.usuarioEmail,
                            cpf = userData.usuarioCpf
                        )

                        Toast.makeText(
                            this@LoginActivity,
                            "Login sucesso! Bem-vindo, ${userData.usuarioNome}",
                            Toast.LENGTH_SHORT
                        ).show()

                        val intent = Intent(this@LoginActivity, UsuarioMainActivity::class.java)
                        startActivity(intent)
                        finish() // Fecha a tela de Login

                    } else {
                        Toast.makeText(
                            this@LoginActivity,
                            "Usuário ou senha inválidos",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        this@LoginActivity,
                        "Erro no login: Status ${response.code()}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onFailure(call: Call<List<LoginResponse>>, t: Throwable) {
                Toast.makeText(
                    this@LoginActivity, "Erro de conexão: ${t.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }
}