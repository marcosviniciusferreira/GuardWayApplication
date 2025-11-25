package com.example.guardwayapplication

import ApiService
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*

class OccurrenceFormActivity : AppCompatActivity() {

    private lateinit var apiService: ApiService
    private lateinit var txtTipoOcorrencia: EditText
    private lateinit var txtDescricao: EditText
    private lateinit var txtDataHora: EditText
    private lateinit var btnSalvar: Button
    private lateinit var btnVoltar: Button
    private lateinit var textFormTitle: TextView
    private lateinit var autocompleteFragment: AutocompleteSupportFragment

    private var enderecoSelecionado: String = ""
    private var latSelecionada: Double = 0.0
    private var lngSelecionada: Double = 0.0
    private var cepSelecionado: String = ""

    private var isEditing: Boolean = false
    private var ocorrenciaId: Int? = null
    private val LOGGED_IN_USER_ID = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_occurrence_form)

        if (!Places.isInitialized()) {
            // Busca o valor da chave de API do arquivo de recursos strings.xml
            Places.initialize(applicationContext, getString(R.string.api_key))
        }

        autocompleteFragment =
            supportFragmentManager.findFragmentById(R.id.autocomplete_fragment)
                    as AutocompleteSupportFragment
        autocompleteFragment.setPlaceFields(
            listOf(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.ADDRESS,
                Place.Field.LAT_LNG,
                Place.Field.ADDRESS_COMPONENTS
            )
        )

        autocompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                Log.i("Places", "Lugar selecionado: ${place.name}, ${place.address}")

                enderecoSelecionado = place.address ?: ""
                latSelecionada = place.latLng?.latitude ?: 0.0
                lngSelecionada = place.latLng?.longitude ?: 0.0
                cepSelecionado = place.addressComponents?.asList()
                    ?.find { it.types.contains("postal_code") }?.name ?: ""

                Log.i("PlacesDetails", "Endereço: $enderecoSelecionado, Lat: $latSelecionada, Lng: $lngSelecionada, CEP: $cepSelecionado")
                Toast.makeText(this@OccurrenceFormActivity, "Endereço selecionado!", Toast.LENGTH_SHORT).show()
            }

            override fun onError(status: com.google.android.gms.common.api.Status) {
                // Esta mensagem é exibida por causa da chave de API incorreta.
                Log.e("Places", "Ocorreu um erro no Autocomplete: $status")
                Toast.makeText(this@OccurrenceFormActivity, "Erro ao buscar endereço.", Toast.LENGTH_SHORT).show()
            }
        })

        textFormTitle = findViewById(R.id.textFormTitle)
        txtTipoOcorrencia = findViewById(R.id.txtTipoOcorrencia)
        txtDescricao = findViewById(R.id.txtDescricao)
        txtDataHora = findViewById(R.id.txtDataHora)
        btnSalvar = findViewById(R.id.btnSalvar)
        btnVoltar = findViewById(R.id.btnVoltar)

        val retrofit = Retrofit.Builder()
            .baseUrl("http://192.168.1.15/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiService = retrofit.create(ApiService::class.java)

        setupIntentData()

        btnVoltar.setOnClickListener { finish() }
        btnSalvar.setOnClickListener { saveOcorrencia() }

        if (!isEditing) {
            val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            txtDataHora.setText(formatter.format(Date()))
        }
    }

    private fun setupIntentData() {
        val ocorrenciaParaEditar = intent.getParcelableExtra<Ocorrencia>("OCORRENCIA_EXTRA")

        if (ocorrenciaParaEditar != null) {
            isEditing = true
            ocorrenciaId = ocorrenciaParaEditar.id_ocorrencia

            textFormTitle.text = "Editar Ocorrência (ID: $ocorrenciaId)"
            btnSalvar.text = "Atualizar Ocorrência"

            txtTipoOcorrencia.setText(ocorrenciaParaEditar.tipo_ocorrencia)
            txtDescricao.setText(ocorrenciaParaEditar.descricao)
            txtDataHora.setText(ocorrenciaParaEditar.data_hora)

            enderecoSelecionado = ocorrenciaParaEditar.endereco ?: "${ocorrenciaParaEditar.latitude}, ${ocorrenciaParaEditar.longitude}"
            latSelecionada = ocorrenciaParaEditar.latitude.toDoubleOrNull() ?: 0.0
            lngSelecionada = ocorrenciaParaEditar.longitude.toDoubleOrNull() ?: 0.0
            cepSelecionado = ocorrenciaParaEditar.CEP

            autocompleteFragment.setText(enderecoSelecionado)

        } else {
            isEditing = false
            textFormTitle.text = "Registrar Nova Ocorrência"
            btnSalvar.text = "Registrar"
            ocorrenciaId = null
        }
    }

    private fun saveOcorrencia() {
        val tipoOcorrencia = txtTipoOcorrencia.text.toString().trim()
        val descricao = txtDescricao.text.toString().trim()
        val dataHora = txtDataHora.text.toString().trim()
        val endereco = enderecoSelecionado.trim()

        if (tipoOcorrencia.isEmpty() || endereco.isEmpty() || dataHora.isEmpty()) {
            Toast.makeText(this, "Todos os campos, incluindo o endereço, são obrigatórios.", Toast.LENGTH_SHORT).show()
            return
        }

        val ocorrenciaPayload = Ocorrencia(
            id_ocorrencia = ocorrenciaId,
            id_usuario = LOGGED_IN_USER_ID,
            tipo_ocorrencia = tipoOcorrencia,
            descricao = descricao,
            data_hora = dataHora,
            latitude = latSelecionada.toString(),
            longitude = lngSelecionada.toString(),
            CEP = cepSelecionado,
            endereco = enderecoSelecionado
        )

        val call: Call<ApiService.SuccessResponse> = if (isEditing) {
            apiService.updateOcorrencia(ocorrenciaPayload)
        } else {
            apiService.createOcorrencia(ocorrenciaPayload)
        }

        call.enqueue(object : Callback<ApiService.SuccessResponse> {
            override fun onResponse(call: Call<ApiService.SuccessResponse>, response: Response<ApiService.SuccessResponse>) {
                val message = response.body()?.message ?: "Operação concluída."
                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(this@OccurrenceFormActivity, message, Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Corpo do erro não disponível"
                    Log.e("API_ERROR", "Code: ${response.code()}, Message: $message, Body: $errorBody")
                    Toast.makeText(this@OccurrenceFormActivity, "Falha na API: $message", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<ApiService.SuccessResponse>, t: Throwable) {
                Log.e("API_FAILURE", "Erro de conexão", t)
                Toast.makeText(this@OccurrenceFormActivity, "Falha de conexão: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }
}
