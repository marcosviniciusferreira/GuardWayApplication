// =========================================================================
// LoginActivity.kt - Código Consolidado
// =========================================================================
package com.example.guardwayapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

// --- CLASSES DE DADOS NECESSÁRIAS PARA A API ---

// Definição da Data Class LoginResponse
data class LoginResponse(
    val usuarioId: Int,
    val usuarioNome: String,
    val usuarioEmail: String,
    val usuarioCpf: String
)
class LoginActivity : AppCompatActivity() {

    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)

        val loginButton: Button = findViewById(R.id.loginButton)

        loginButton.setOnClickListener {
            performLogin()
        }
    }

    private fun performLogin() {
        // Valores hardcoded para teste RÁPIDO, conforme solicitado
        val email = "andre@teste.com"
        val password = "123"

        /*
        // Se precisar voltar a ler dos campos de texto, use:
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Preencha e-mail e senha.", Toast.LENGTH_SHORT).show()
            return
        }
        */

        val retrofit = Retrofit.Builder()
            .baseUrl("http://192.168.1.15/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(ApiService::class.java)

        // Esta chamada agora deve ser resolvida corretamente:
        val call = apiService.login(email, password)

        call.enqueue(object : Callback<List<LoginResponse>> {

            override fun onResponse(
                call: Call<List<LoginResponse>>,
                response: Response<List<LoginResponse>>
            ) {
                if (response.isSuccessful && response.body() != null) {

                    val loginResponses = response.body()!!
                    if (loginResponses.isNotEmpty()) {
                        // Login bem-sucedido
                        Toast.makeText(this@LoginActivity, "Login realizado com sucesso!", Toast.LENGTH_SHORT).show()

                        // TODO: Salvar dados do usuário (ex: token ou id) no SharedPreferences antes de redirecionar

                        // Assumindo que MainActivity existe:
                        val intent = Intent(this@LoginActivity, MainActivity::class.java)
                        startActivity(intent)
                        finish()

                    } else {
                        Toast.makeText(this@LoginActivity, "Usuário ou senha inválidos", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(this@LoginActivity, "Erro no login: Status ${response.code()}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<List<LoginResponse>>, t: Throwable) {
                Toast.makeText(this@LoginActivity, "Erro de conexão: ${t.message}",
                    Toast.LENGTH_LONG).show()
            }
        })
    }


}