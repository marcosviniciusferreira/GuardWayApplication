package com.example.guardwayapplication

import ApiService
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
        btnBackToolbar = findViewById(R.id.btn_back_toolbar)

        btnBackToolbar.setOnClickListener {
            finish()
        }

        // Configurar Retrofit
        val retrofit = Retrofit.Builder()
            .baseUrl("http://192.168.1.13/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiService = retrofit.create(ApiService::class.java)

        setupIntentData()

        btnSalvar.setOnClickListener {
            saveUser()
        }
    }

    private fun setupIntentData() {
        // Correção do getParcelableExtra (compatibilidade com Android 13+)
        val usuarioParaEditar = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("USUARIO_EXTRA", Usuario::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra<Usuario>("USUARIO_EXTRA")
        }

        if (usuarioParaEditar != null) {
            isEditing = true
            userId = usuarioParaEditar.USUARIO_ID
            textFormTitle.text = "Editar Conta"
            btnSalvar.text = "Atualizar"
            txtNome.setText(usuarioParaEditar.USUARIO_NOME)
            txtEmail.setText(usuarioParaEditar.USUARIO_EMAIL)
            txtCpf.setText(usuarioParaEditar.USUARIO_CPF)
            txtSenha.setText("")
        } else {
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

        // As chamadas agora usam os nomes corretos definidos na ApiService
        val call: Call<ApiService.SuccessResponse> = if (isEditing) {
            apiService.updateUsuario(userPayload)
        } else {
            apiService.createUsuario(userPayload)
        }

        call.enqueue(object : Callback<ApiService.SuccessResponse> {
            override fun onResponse(call: Call<ApiService.SuccessResponse>, response: Response<ApiService.SuccessResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val message = response.body()?.message ?: "Sucesso!"
                    Toast.makeText(this@UserFormActivity, message, Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                } else {
                    val errorMsg = response.body()?.message ?: "Erro no servidor"
                    Toast.makeText(this@UserFormActivity, "Falha: ${response.code()}. $errorMsg", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<ApiService.SuccessResponse>, t: Throwable) {
                // Log para debug facilitado
                android.util.Log.e("API_FALHA", "Erro de Conexão: ${t.message}", t)
                Toast.makeText(this@UserFormActivity, "Falha de conexão: Verifique o IP e o Firewall", Toast.LENGTH_LONG).show()
            }
        })
    }
}
