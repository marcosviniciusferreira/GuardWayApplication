package com.example.guardwayapplication

import ApiService
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputLayout
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
    private var toolbarLogin: Toolbar? = null
    private var btnBack: ImageView? = null
    private lateinit var createAccountButton: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        prefsManager = SharedPreferencesManager(this)

        // Toolbar só existe no layout portrait — no landscape o bloco é ignorado
        val toolbar: Toolbar? = findViewById(R.id.toolbar_login)
        if (toolbar != null) {
            toolbarLogin = toolbar
            setSupportActionBar(toolbarLogin)
            supportActionBar?.setDisplayShowTitleEnabled(false)
            toolbarLogin?.title = null

            btnBack = findViewById(R.id.btn_back_toolbar)
            btnBack?.setOnClickListener {
                onBackPressed()
            }
        }

        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        val loginButton: Button = findViewById(R.id.loginButton)
        createAccountButton = findViewById(R.id.createAccountButton)

        // tv_forgot_password só existe no layout portrait
        val forgotPasswordTextView: TextView? = findViewById(R.id.tv_forgot_password)
        forgotPasswordTextView?.setOnClickListener {
            Toast.makeText(this, "Abrindo tela de Esqueci Senha...", Toast.LENGTH_SHORT).show()
        }

        loginButton.setOnClickListener {
            performLogin()
        }

        createAccountButton.setOnClickListener {
            Toast.makeText(this, "Navegando para Criar Nova Conta...", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(this@LoginActivity, VisitanteMainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun performLogin() {
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()
        val validador = UsuarioValidador()

        if (!validador.validarEmail(email)) {
            val tilEmail: TextInputLayout = findViewById(R.id.til_email)
            tilEmail.error = "Email invalido"
            return
        }

        if (!validador.validarSenha(password)) {
            val tilPassword: TextInputLayout = findViewById(R.id.til_password)
            tilPassword.error = "Senha invalida"
            return
        }

        val retrofit = Retrofit.Builder()
            .baseUrl("http://192.168.1.16/")
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
                        finish()
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
                    this@LoginActivity,
                    "Erro de conexão: ${t.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }
}