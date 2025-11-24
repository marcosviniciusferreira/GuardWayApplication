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
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

// ----------------------------------------------------
// PASSO 1: Definição da Data Class LoginResponse
// ESTA CLASSE DEVE REFLETIR EXATAMENTE o JSON que o seu servidor retorna.
// Exemplo: se o JSON retornar apenas um 'sucesso: true' e 'token: string'
data class LoginResponse(
    val usuarioId: Int,
    val usuarioNome: String,
    val usuarioEmail: String,
    val usuarioCpf: String
)
// ----------------------------------------------------


class LoginActivity : AppCompatActivity() {

    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // enableEdgeToEdge e ViewCompat não são estritamente necessários para o login,
        // mas se estiverem dando erro, verifique se foram importados corretamente
        // ou remova-os se não estiver usando-os ativamente.

        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        val loginButton: Button = findViewById(R.id.loginButton)

        loginButton.setOnClickListener {
            blockLogin()
        }
    }

    private fun blockLogin() {
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()

        // ⚠️ VERIFIQUE: A URL base deve terminar com uma barra e não ter espaços.
        val retrofit = Retrofit.Builder()
            .baseUrl("http://192.168.1.15/") // Removido o espaço extra
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(ApiService::class.java)

        val call = apiService.login(email, password)

        // PASSO 2: Implementação ÚNICA e correta do Callback
        call.enqueue(object : Callback<List<LoginResponse>> {

            override fun onResponse(
                call: Call<List<LoginResponse>>,
                response: Response<List<LoginResponse>>
            ) {
                // Acesso correto ao 'this' da Activity para Contexto (Toast e Intent)
                if (response.isSuccessful && response.body() != null) {
                    val loginResponses = response.body()!!
                    if (loginResponses.isNotEmpty()) {

                        // Redireciona
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