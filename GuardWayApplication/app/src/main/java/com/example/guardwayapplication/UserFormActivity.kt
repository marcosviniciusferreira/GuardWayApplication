package com.example.guardwayapplication

import ApiService
import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class UserFormActivity : AppCompatActivity() {

    private lateinit var apiService: ApiService
    private lateinit var editNome: EditText
    private lateinit var editEmail: EditText
    private lateinit var editCpf: EditText
    private lateinit var btnSalvar: Button
    private lateinit var textFormTitle: TextView

    private var isEditing: Boolean = false
    private var userId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_form)

        // Inicializar Views
        textFormTitle = findViewById(R.id.textFormTitle)
        editNome = findViewById(R.id.editNome)
        editEmail = findViewById(R.id.editEmail)
        editCpf = findViewById(R.id.editCpf)
        btnSalvar = findViewById(R.id.btnSalvar)

        // Configurar Retrofit
        val retrofit = Retrofit.Builder()
            .baseUrl("http://192.168.1.15/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiService = retrofit.create(ApiService::class.java)

        setupIntentData()

        btnSalvar.setOnClickListener {
            saveUser()
        }
    }

    private fun setupIntentData() {
        // Verifica se o Intent contém um ID de usuário (sinaliza Edição)
        val idFromIntent = intent.getIntExtra("ID", -1)

        if (idFromIntent != -1) {
            isEditing = true
            userId = idFromIntent

            // 1. Configura a UI para Edição
            textFormTitle.text = "Editar Usuário (ID: $userId)"
            btnSalvar.text = "Atualizar"

            // 2. Preenche os campos com os dados passados pelo Intent
            editNome.setText(intent.getStringExtra("NOME"))
            editEmail.setText(intent.getStringExtra("EMAIL"))
            editCpf.setText(intent.getStringExtra("CPF"))

        } else {
            // Configura a UI para Criação
            isEditing = false
            textFormTitle.text = "Novo Usuário"
            btnSalvar.text = "Salvar"
        }
    }

    private fun saveUser() {
        val nome = editNome.text.toString().trim()
        val email = editEmail.text.toString().trim()
        val cpf = editCpf.text.toString().trim()

        if (nome.isEmpty() || email.isEmpty() || cpf.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos obrigatórios.", Toast.LENGTH_SHORT).show()
            return
        }

        // Cria o objeto de dados a ser enviado
        val userPayload = Usuario(
            USUARIO_ID = userId ?: 0, // Envia 0 se for novo, o ID se for edição
            USUARIO_NOME = nome,
            USUARIO_EMAIL = email,
            USUARIO_CPF = cpf,
            USUARIO_SENHA = ""
        )

        // Escolhe a chamada de API correta
        val call: Call<ApiService.SuccessResponse> = if (isEditing) {
            apiService.updateUsuario(userPayload)
        } else {
            apiService.createUsuario(userPayload)
        }

        // Executa a chamada
        call.enqueue(object : Callback<ApiService.SuccessResponse> {
            override fun onResponse(call: Call<ApiService.SuccessResponse>, response: Response<ApiService.SuccessResponse>) {
                val message = response.body()?.message ?: "Operação concluída."

                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(this@UserFormActivity, message, Toast.LENGTH_SHORT).show()

                    // Sinaliza para a Activity anterior (UserListActivity) recarregar a lista
                    setResult(RESULT_OK)
                    finish() // Fecha o formulário
                } else {
                    Toast.makeText(this@UserFormActivity, "Falha: ${response.code()}. $message", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<ApiService.SuccessResponse>, t: Throwable) {
                Toast.makeText(this@UserFormActivity, "Falha de conexão: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }
}