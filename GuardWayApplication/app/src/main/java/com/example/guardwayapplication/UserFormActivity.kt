package com.example.guardwayapplication

import ApiService
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.view.ViewGroup // Adicionada para o Dialog/FrameLayout, embora não usada neste arquivo, é bom ter se fosse usar o Dialog.
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
    private lateinit var txtSenha  : EditText
    private lateinit var btnSalvar: Button

    private lateinit var btnVoltar: Button
    private lateinit var textFormTitle: TextView

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
        txtSenha = findViewById(R.id.txtSenha) // CORREÇÃO: Já estava aqui e está correto.
        btnSalvar = findViewById(R.id.btnSalvar)
        btnVoltar = findViewById(R.id.btnVoltar)

        // Configurar Retrofit
        val retrofit = Retrofit.Builder()
            .baseUrl("http://192.168.1.4/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiService = retrofit.create(ApiService::class.java)

        setupIntentData()

        btnVoltar.setOnClickListener { finish() }

        btnSalvar.setOnClickListener {
            saveUser()
        }
    }

    private fun setupIntentData() {
        // Usa getParcelableExtra para buscar o objeto Usuario completo
        // Se a classe Usuario for Serializable, use getSerializableExtra("USUARIO_EXTRA") as? Usuario
        val usuarioParaEditar = intent.getParcelableExtra<Usuario>("USUARIO_EXTRA")

        if (usuarioParaEditar != null) {
            isEditing = true
            userId = usuarioParaEditar.USUARIO_ID // Pega o ID do objeto

            // 1. Configura a UI para Edição
            textFormTitle.text = "Editar Usuário (ID: $userId)"
            btnSalvar.text = "Atualizar"
            btnVoltar.text = "Voltar"

            // 2. Preenche os campos com os dados do objeto Usuario
            txtNome.setText(usuarioParaEditar.USUARIO_NOME)
            txtEmail.setText(usuarioParaEditar.USUARIO_EMAIL)
            txtCpf.setText(usuarioParaEditar.USUARIO_CPF)

            // **NUNCA** preencha o campo de senha com o valor real por segurança.
            txtSenha.setText("")

        } else {
            // Configura a UI para Criação (caso o Intent não contenha o objeto)
            isEditing = false
            textFormTitle.text = "Novo Usuário"
            btnSalvar.text = "Salvar"
            userId = null // Garante que o ID é nulo para criação
        }
    }

    private fun saveUser() {
        val nome = txtNome.text.toString().trim()
        val email = txtEmail.text.toString().trim()
        val cpf = txtCpf.text.toString().trim()
        // A senha só será usada se for preenchida, o backend deve tratar o campo vazio/nulo
        val senha = txtSenha.text.toString().trim()

        if (nome.isEmpty() || email.isEmpty() || cpf.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos obrigatórios.", Toast.LENGTH_SHORT).show()
            return
        }

        // Se estiver editando, e a senha não foi alterada, é comum enviar o hash antigo
        // ou um campo nulo para o backend ignorar. Como a senha aqui é sempre vazia na edição
        // a menos que o usuário digite algo, o backend deve aceitar a string vazia ""
        // e ignorá-la na atualização.

        // Cria o objeto de dados a ser enviado
        val userPayload = Usuario(
            USUARIO_ID = userId ?: 0, // Essencial: Envia o ID na edição
            USUARIO_NOME = nome,
            USUARIO_EMAIL = email,
            USUARIO_CPF = cpf,
            USUARIO_SENHA = senha,
        )

        // Escolhe a chamada de API correta
        val call: Call<ApiService.SuccessResponse> = if (isEditing) {
            // Se for edição, chama o método PUT/PATCH da API
            apiService.updateUsuario(userPayload)
        } else {
            // Se for criação, chama o método POST da API
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