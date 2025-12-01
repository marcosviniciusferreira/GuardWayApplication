package com.example.guardwayapplication

import ApiService
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.view.ViewGroup
import android.widget.ImageView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class UserFormActivity : AppCompatActivity() {

    private lateinit var apiService: ApiService
    private lateinit var txtNome: EditText
    private lateinit var txtEmail: EditText
    private lateinit var txtCpf: EditText
    private lateinit var txtSenha: EditText
    private lateinit var btnSalvar: Button

    // private lateinit var btnVoltar: Button <--- REMOVIDA
    private lateinit var textFormTitle: TextView

    private lateinit var btnBackToolbar: ImageView

    private var isEditing: Boolean = false
    private var userId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_form)

        // Inicializar Views
        textFormTitle = findViewById(R.id.textFormTitle)
        txtNome = findViewById(R.id.txtNome)
        txtEmail = findViewById(R.id.txtEmail)
        txtCpf = findViewById(R.id.txtCpf)
        txtSenha = findViewById(R.id.txtSenha)
        btnSalvar = findViewById(R.id.btnSalvar)
        // btnVoltar = findViewById(R.id.btnVoltar) <--- REMOVIDA

        // INICIALIZAÇÃO E LISTENER DO BOTÃO VOLTAR DA TOOLBAR (MANTIDO)
        btnBackToolbar = findViewById(R.id.btn_back_toolbar)
        btnBackToolbar.setOnClickListener {
            finish() // Retorna à Activity anterior
        }

        // Configurar Retrofit
        val retrofit = Retrofit.Builder()
            .baseUrl("http://192.168.1.4/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiService = retrofit.create(ApiService::class.java)

        setupIntentData()

        // btnVoltar.setOnClickListener { finish() } <--- REMOVIDA

        btnSalvar.setOnClickListener {
            saveUser()
        }
    }

    private fun setupIntentData() {
        // Usa getParcelableExtra para buscar o objeto Usuario completo
        val usuarioParaEditar = intent.getParcelableExtra<Usuario>("USUARIO_EXTRA")

        if (usuarioParaEditar != null) {
            isEditing = true
            userId = usuarioParaEditar.USUARIO_ID

            // 1. Configura a UI para Edição
            textFormTitle.text = "Editar Conta"
            btnSalvar.text = "Atualizar"
            // btnVoltar.text = "Voltar" <--- REMOVIDA

            // 2. Preenche os campos com os dados do objeto Usuario
            txtNome.setText(usuarioParaEditar.USUARIO_NOME)
            txtEmail.setText(usuarioParaEditar.USUARIO_EMAIL)
            txtCpf.setText(usuarioParaEditar.USUARIO_CPF)

            txtSenha.setText("")

        } else {
            // Configura a UI para Criação (caso o Intent não contenha o objeto)
            isEditing = false
            textFormTitle.text = "Novo Usuário"
            btnSalvar.text = "Salvar"
            userId = null
        }
    }

    private fun saveUser() {
        val nome = txtNome.text.toString().trim()
        val email = txtEmail.text.toString().trim()
        val cpf = txtCpf.text.toString().trim()
        val senha = txtSenha.text.toString().trim()

        if (nome.isEmpty() || email.isEmpty() || cpf.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos obrigatórios.", Toast.LENGTH_SHORT).show()
            return
        }

        val userPayload = Usuario(
            USUARIO_ID = userId ?: 0,
            USUARIO_NOME = nome,
            USUARIO_EMAIL = email,
            USUARIO_CPF = cpf,
            USUARIO_SENHA = senha,
        )

        val call: Call<ApiService.SuccessResponse> = if (isEditing) {
            apiService.updateUsuario(userPayload)
        } else {
            apiService.createUsuario(userPayload)
        }

        call.enqueue(object : Callback<ApiService.SuccessResponse> {
            override fun onResponse(call: Call<ApiService.SuccessResponse>, response: Response<ApiService.SuccessResponse>) {
                val message = response.body()?.message ?: "Operação concluída."

                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(this@UserFormActivity, message, Toast.LENGTH_SHORT).show()

                    setResult(RESULT_OK)
                    finish()
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