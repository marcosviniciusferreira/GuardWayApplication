package com.example.guardwayapplication

import ApiService
import Ocorrencia
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.android.gms.common.api.Status
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*
import com.google.gson.Gson // Importa o GSON para logs de debug

class OccurrenceFormActivity : AppCompatActivity() {

    private lateinit var apiService: ApiService
    private lateinit var spnTipoOcorrencia: Spinner
    private lateinit var txtDescricao: EditText

    // VARIÁVEIS NOVAS PARA OS CAMPOS DE DATA E HORA SEPARADOS
    private lateinit var txtDataSelecionada: EditText
    private lateinit var txtHoraSelecionada: EditText

    private lateinit var btnSalvar: Button
    private lateinit var btnVoltar: Button
    private lateinit var textFormTitle: TextView
    private lateinit var autocompleteFragment: AutocompleteSupportFragment

    // Objeto Calendar para armazenar a data e hora selecionadas
    private val calendar = Calendar.getInstance()

    // Tipos de ocorrências padronizados
    private val tiposOcorrencia = arrayOf(
        "Selecione o Tipo", // Padrão
        "Furto ou Roubo",
        "Vandalismo",
        "Agressão",
        "Invasão",
        "Atividade Suspeita",
        "Assédio",
        "Outros (Relacionados à Segurança)"
    )

    private var enderecoSelecionado: String = ""
    private var latSelecionada: Double = 0.0
    private var lngSelecionada: Double = 0.0
    private var cepSelecionado: String = ""

    // TAG para Logs de Erro de API/Dados
    private val API_LOG_TAG = "API_PAYLOAD"

    private var isEditing: Boolean = false
    private var ocorrenciaId: Int? = null

    private var LOGGED_IN_USER_ID = 0 // Inicializado como 0, será preenchido pelo SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_occurrence_form)

        getUserIdFromSharedPreferences()

        // Inicialização da API Places (Manter a chave hardcoded, mas atenção para segurança)
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, "AIzaSyCuUyAV8yqeNBatJcxGUv-nJKC7OChYZLM")
        }

        // Inicialização do Autocomplete Fragment
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

                // Tenta encontrar o CEP/postal_code
                cepSelecionado = place.addressComponents?.asList()
                    ?.find { it.types.contains("postal_code") }?.name ?: ""

                // Se o CEP não for encontrado, tenta usar o nome do lugar para o endereço
                if (enderecoSelecionado.isEmpty()) {
                    enderecoSelecionado = place.name ?: ""
                }

                Log.i("PlacesDetails", "Endereço: $enderecoSelecionado, Lat: $latSelecionada, Lng: $lngSelecionada, CEP: $cepSelecionado")
                Toast.makeText(this@OccurrenceFormActivity, "Endereço selecionado!", Toast.LENGTH_SHORT).show()
            }

            override fun onError(status: Status) {
                Log.e("Places", "Ocorreu um erro no Autocomplete: $status")
                Toast.makeText(this@OccurrenceFormActivity, "Erro ao buscar endereço.", Toast.LENGTH_SHORT).show()
                // Limpa os dados em caso de erro para forçar a nova validação
                enderecoSelecionado = ""
                latSelecionada = 0.0
                lngSelecionada = 0.0
                cepSelecionado = ""
            }
        })

        // Inicialização de Views
        textFormTitle = findViewById(R.id.textFormTitle)
        spnTipoOcorrencia = findViewById(R.id.spnTipoOcorrencia)
        txtDescricao = findViewById(R.id.txtDescricao)

        // NOVO: Inicialização dos campos de Data e Hora
        txtDataSelecionada = findViewById(R.id.txtDataSelecionada)
        txtHoraSelecionada = findViewById(R.id.txtHoraSelecionada)

        btnSalvar = findViewById(R.id.btnSalvar)
        btnVoltar = findViewById(R.id.btnVoltar)

        // Configuração do Spinner
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            tiposOcorrencia
        )
        spnTipoOcorrencia.adapter = adapter

        txtDataSelecionada.setOnClickListener { showDatePickerDialog() }
        txtHoraSelecionada.setOnClickListener { showTimePickerDialog() }

        val retrofit = Retrofit.Builder()
            // ATENÇÃO: Se estiver testando no emulador, o IP é 10.0.2.2. Se for celular, use o IP da sua máquina.
            .baseUrl("http://192.168.1.9/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiService = retrofit.create(ApiService::class.java)

        setupIntentData()

        btnVoltar.setOnClickListener { finish() }
        btnSalvar.setOnClickListener { saveOcorrencia() }

        // NOVO: Inicializa os campos com a data e hora atual SE não estiver em edição
        if (!isEditing) {
            updateDateLabel(calendar)
            updateTimeLabel(calendar)
        }
    }

    private fun getUserIdFromSharedPreferences() {
        val sharedPreferences = getSharedPreferences("GuardWayPrefs", Context.MODE_PRIVATE)
        // Obtém o ID. O valor padrão (0) é usado se a chave "USER_ID" não for encontrada.
        LOGGED_IN_USER_ID = sharedPreferences.getInt("user_id", 0)

        if (LOGGED_IN_USER_ID == 0) {
            Log.e(API_LOG_TAG, "LOGGED_IN_USER_ID não encontrado no SharedPreferences. Usando 0.")
        } else {
            Log.d(API_LOG_TAG, "LOGGED_IN_USER_ID carregado: $LOGGED_IN_USER_ID")
        }
    }

    // Função para exibir o seletor de Data
    private fun showDatePickerDialog() {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            calendar.set(selectedYear, selectedMonth, selectedDay)
            updateDateLabel(calendar)
        }, year, month, day).show()
    }

    // Função para exibir o seletor de Hora
    private fun showTimePickerDialog() {
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        TimePickerDialog(this, { _, selectedHour, selectedMinute ->
            calendar.set(Calendar.HOUR_OF_DAY, selectedHour)
            calendar.set(Calendar.MINUTE, selectedMinute)
            updateTimeLabel(calendar)
        }, hour, minute, true).show() // true para formato 24 horas
    }

    // Atualiza o texto do campo de data
    private fun updateDateLabel(cal: Calendar) {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        txtDataSelecionada.setText(dateFormat.format(cal.time))
    }

    // Atualiza o texto do campo de hora
    private fun updateTimeLabel(cal: Calendar) {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        txtHoraSelecionada.setText(timeFormat.format(cal.time))
    }

    private fun setupIntentData() {
        val ocorrenciaParaEditar = intent.getParcelableExtra<Ocorrencia>("OCORRENCIA_EXTRA")

        if (ocorrenciaParaEditar != null) {
            isEditing = true
            ocorrenciaId = ocorrenciaParaEditar.id_ocorrencia

            textFormTitle.text = "Editar Ocorrência (ID: $ocorrenciaId)"
            btnSalvar.text = "Atualizar Ocorrência"

            // Define o valor selecionado no Spinner
            val tipoOcorrenciaIndex = tiposOcorrencia.indexOf(ocorrenciaParaEditar.tipo_ocorrencia)
            if (tipoOcorrenciaIndex >= 0) {
                spnTipoOcorrencia.setSelection(tipoOcorrenciaIndex)
            }

            txtDescricao.setText(ocorrenciaParaEditar.descricao)

            // Parse e ajuste dos campos de Data e Hora para edição
            try {
                val fullDateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val date = fullDateTimeFormat.parse(ocorrenciaParaEditar.data_hora)
                if (date != null) {
                    calendar.time = date
                    updateDateLabel(calendar)
                    updateTimeLabel(calendar)
                }
            } catch (e: Exception) {
                Log.e("ParseError", "Erro ao fazer parse da data/hora: ${e.message}")
                Toast.makeText(this, "Data/Hora inválida na edição.", Toast.LENGTH_SHORT).show()
            }

            // Define dados de localização existentes
            // Usamos o endereço da própria ocorrência
            enderecoSelecionado = ocorrenciaParaEditar.endereco ?: "${ocorrenciaParaEditar.latitude}, ${ocorrenciaParaEditar.longitude}"
            // ⚠️ Cuidado: Se latitude e longitude são Strings na classe Ocorrencia, o toDoubleOrNull é essencial.
            // Vou assumir que sua classe Ocorrencia.kt tem latitude e longitude como String, mas o banco espera Double.
            latSelecionada = ocorrenciaParaEditar.latitude.toDoubleOrNull() ?: 0.0
            lngSelecionada = ocorrenciaParaEditar.longitude.toDoubleOrNull() ?: 0.0
            cepSelecionado = ocorrenciaParaEditar.CEP

            autocompleteFragment.setText(enderecoSelecionado)

        } else {
            isEditing = false
            textFormTitle.text = "Registrar Nova Ocorrência"
            btnSalvar.text = "Registrar"
            ocorrenciaId = null
            // Garante que a primeira opção ("Selecione o Tipo") esteja selecionada ao criar
            spnTipoOcorrencia.setSelection(0)

            // LÓGICA DE PREENCHIMENTO AUTOMÁTICO DO ENDEREÇO
            val initialLatitude = intent.getDoubleExtra("latitude", 0.0)
            val initialLongitude = intent.getDoubleExtra("longitude", 0.0)
            val initialAddress = intent.getStringExtra("endereco")
            val initialCep = intent.getStringExtra("cep")

            if (initialAddress != null && initialAddress.isNotEmpty() && initialLatitude != 0.0) {

                // 1. Define as variáveis de localização da Activity
                enderecoSelecionado = initialAddress
                latSelecionada = initialLatitude
                lngSelecionada = initialLongitude
                cepSelecionado = initialCep ?: "" // Usa o CEP enviado ou vazio

                // 2. Preenche o campo de busca com o endereço completo
                autocompleteFragment.setText(initialAddress)

                Log.d("IntentData", "Localização inicial carregada: $initialAddress")
                Toast.makeText(this, "Localização inicial do mapa preenchida.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveOcorrencia() {
        // Pega o item selecionado do Spinner
        val tipoOcorrencia = spnTipoOcorrencia.selectedItem.toString().trim()
        val descricao = txtDescricao.text.toString().trim()

        // NOVO: Formata a data e hora combinadas para o formato do backend (yyyy-MM-dd HH:mm:ss)
        val fullDateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val dataHora = fullDateTimeFormat.format(calendar.time)

        val endereco = enderecoSelecionado.trim()
        val cep = cepSelecionado.trim() // Variável CEP para validação


        if (tipoOcorrencia == tiposOcorrencia[0]) {
            Toast.makeText(this, "Selecione um Tipo de Ocorrência válido.", Toast.LENGTH_SHORT).show()
            return
        }

        // 2. Validação dos campos de Data e Hora
        if (txtDataSelecionada.text.isEmpty() || txtHoraSelecionada.text.isEmpty()) {
            Toast.makeText(this, "A Data e a Hora da ocorrência são obrigatórias.", Toast.LENGTH_SHORT).show()
            return
        }

        // 3. Validação de Endereço e Localização
        if (endereco.isEmpty() || latSelecionada == 0.0 || lngSelecionada == 0.0 || cep.isEmpty()) {
            Toast.makeText(this, "Selecione um endereço válido usando o campo de busca. CEP, Latitude e Longitude são obrigatórios.", Toast.LENGTH_LONG).show()

            // Log detalhado para depuração no Logcat
            Log.e(API_LOG_TAG, "Dados de Localização Incompletos! Endereço: $endereco, Lat: $latSelecionada, Lng: $lngSelecionada, CEP: $cep")

            return
        }

        // 4. Validação do ID do Usuário
        if (LOGGED_IN_USER_ID == 0) {
            Toast.makeText(this, "ID do usuário não encontrado. Faça login novamente.", Toast.LENGTH_LONG).show()
            return
        }

        // --- FIM DA VALIDAÇÃO REFORÇADA ---

        val ocorrenciaPayload = Ocorrencia(
            id_ocorrencia = ocorrenciaId,
            id_usuario = LOGGED_IN_USER_ID,
            tipo_ocorrencia = tipoOcorrencia,
            descricao = descricao,
            data_hora = dataHora,
            latitude = latSelecionada.toString(), // ⚠️ Certifique-se de que a classe Ocorrencia.kt aceita String aqui
            longitude = lngSelecionada.toString(), // ⚠️ E aqui
            CEP = cep,
            endereco = endereco // Campo Endereço
            // validada e caminho_arquivo usarão valores padrão da classe Ocorrencia
        )

        // DEBUG: Imprime o JSON Payload ANTES de enviar usando GSON (confirma o que será serializado)
        val gson = Gson()
        Log.d(API_LOG_TAG, "JSON Payload para API: ${gson.toJson(ocorrenciaPayload)}")

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
                    finish() // ✅ Termina a Activity APÓS SUCESSO
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Corpo do erro não disponível"
                    Log.e(API_LOG_TAG, "Code: ${response.code()}, Message: $message, Body: $errorBody")
                    Toast.makeText(this@OccurrenceFormActivity, "Falha na API: $message", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<ApiService.SuccessResponse>, t: Throwable) {
                Log.e(API_LOG_TAG, "Falha de conexão ou rede", t)
                Toast.makeText(this@OccurrenceFormActivity, "Falha de conexão: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }
}