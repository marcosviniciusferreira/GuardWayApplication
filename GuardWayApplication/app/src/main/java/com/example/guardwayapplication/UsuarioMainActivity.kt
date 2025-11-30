package com.example.guardwayapplication

import ApiService
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.button.MaterialButton
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.location.Priority
import com.google.android.material.navigation.NavigationView
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place // Classe Place do Google Places
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.android.gms.common.api.Status
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Locale


class UsuarioMainActivity : AppCompatActivity(), OnMapReadyCallback, OnMapDataFound, NavigationView.OnNavigationItemSelectedListener {

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
        private const val DEFAULT_ZOOM = 15f
        private const val LOCATION_PRIORITY = Priority.PRIORITY_HIGH_ACCURACY
        private const val BASE_URL = "http://192.168.1.4/"

        private const val DANGER_THRESHOLD = 5
        private const val DEFAULT_LATITUDE = -23.5505 // São Paulo
        private const val DEFAULT_LONGITUDE = -46.6333 // São Paulo
        private const val DEFAULT_ADDRESS = "Localização Indisponível (São Paulo, SP)"
        // ⚠️ ATENÇÃO: Use a sua chave de API do Google Maps/Places
        private const val PLACES_API_KEY = "AIzaSyCuUyAV8yqeNBatJcxGUv-nJKC7OChYZLM"
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var googleMap: GoogleMap? = null

    // --- Componentes do Drawer e Toolbar ---
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toolbar: Toolbar
    private lateinit var navView: NavigationView
    private lateinit var btnUserProfile: ImageButton

    // --- SharedPreferences ---
    private lateinit var prefsManager: SharedPreferencesManager

    // --- Referências para a UI do Bottom Sheet ---
    private lateinit var bottomSheet: LinearLayout
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>
    private lateinit var tvAddressTitle: TextView
    private lateinit var btnPerigoStatus: MaterialButton
    private lateinit var btnEmergencyCall: MaterialButton
    private lateinit var btnVerRelatorioCompleto: MaterialButton
    private lateinit var btnCadastrarOcorrencia: MaterialButton

    // --- Referências para o Autocomplete (NOVO) ---
    private lateinit var autocompleteFragment: AutocompleteSupportFragment
    // ------------------------------------------------

    lateinit var apiService: ApiService

    private var currentLatitude: Double? = null
    private var currentLongitude: Double? = null
    private var currentCEP: String? = null
    private var currentFullAddress: String? = null

    // CORREÇÃO 1: Tipo da lista mudado de Place para LocalPlace
    private var places = mutableListOf<LocalPlace>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_usuario_main)

        initViews()
        initializeRetrofit()

        prefsManager = SharedPreferencesManager(this)

        // --- Configuração da Toolbar e Drawer ---
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.title = null

        btnUserProfile.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        val toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navView.setNavigationItemSelectedListener(this)
        setupDrawerHeader()
        // ------------------------------------------

        // Configura o Places Autocomplete (NOVO)
        setupPlacesAutocomplete()

        // --- Aplicando Insets ---
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            v.setPadding(systemBars.left, 0, systemBars.right, 0)

            val initialPaddingBottom =
                resources.getDimensionPixelSize(R.dimen.bottom_sheet_padding_base)

            bottomSheet.setPadding(
                bottomSheet.paddingLeft,
                bottomSheet.paddingTop,
                bottomSheet.paddingRight,
                initialPaddingBottom + systemBars.bottom
            )
            insets
        }
        // -------------------------------------------------------------

        // Configura o comportamento do painel deslizante
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.isHideable = true
        val peekHeight = resources.getDimensionPixelSize(R.dimen.bottom_sheet_peek_height)
        bottomSheetBehavior.peekHeight = peekHeight
        bottomSheetBehavior.isDraggable = true
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        bottomSheet.visibility = View.VISIBLE


        // Listener do botão de emergência
        btnEmergencyCall.setOnClickListener {
            Toast.makeText(this, "Abrindo discador de emergência (190)...", Toast.LENGTH_SHORT)
                .show()
            val numeroEmergencia = "tel:190"
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse(numeroEmergencia))
            startActivity(intent)
        }

        // Listener do botão Cadastrar Ocorrência
        btnCadastrarOcorrencia.setOnClickListener {
            val intent = Intent(this@UsuarioMainActivity, OccurrenceFormActivity::class.java)
            // Passa os dados de localização atuais para pré-preencher o formulário
            intent.putExtra("latitude", currentLatitude ?: 0.0)
            intent.putExtra("longitude", currentLongitude ?: 0.0)
            intent.putExtra("endereco", currentFullAddress)
            intent.putExtra("cep", currentCEP)

            startActivity(intent)
            Toast.makeText(this, "Navegando para Cadastro de Ocorrência...", Toast.LENGTH_SHORT)
                .show()
        }

        // Listener do botão Ver Relatório Completo
        btnVerRelatorioCompleto.setOnClickListener {
            navigateToRelatorioSeguranca()
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        requestLocationPermission()

        val mapFragment =
            supportFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Estado Inicial: "Avaliando..."
        tvAddressTitle.text = "Aguardando localização..."
        setPerigoStatusLoading(true)
    }

    /**
     * Centraliza a inicialização do Retrofit.
     */
    private fun initializeRetrofit() {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiService = retrofit.create(ApiService::class.java)
    }

    /**
     * Centraliza a inicialização de todas as views.
     */
    private fun initViews() {
        // Toolbar e Drawer
        drawerLayout = findViewById(R.id.drawer_layout)
        toolbar = findViewById(R.id.toolbar)
        btnUserProfile = findViewById(R.id.btn_user_profile)
        navView = findViewById(R.id.nav_view)

        // Bottom Sheet
        bottomSheet = findViewById(R.id.bottom_sheet)
        tvAddressTitle = findViewById(R.id.tv_address_title)
        btnPerigoStatus = findViewById(R.id.btn_perigo_status)
        btnEmergencyCall = findViewById(R.id.btn_emergency_call)
        btnCadastrarOcorrencia = findViewById(R.id.btn_cadastrar_ocorrencia)
        btnVerRelatorioCompleto = findViewById(R.id.btn_ver_relatorio_completo)
    }

    /**
     * Configura o Google Places Autocomplete para a pesquisa de endereço.
     */
    private fun setupPlacesAutocomplete() {
        // 1. Inicializa o Places API
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, PLACES_API_KEY)
        }

        // 2. Obtém a instância do fragmento
        autocompleteFragment = supportFragmentManager
            .findFragmentById(R.id.autocomplete_fragment) as AutocompleteSupportFragment

        // 3. Define os campos de dados que você quer receber
        // NOTE: Usa a classe Place.Field da biblioteca Google Places (Importada no topo)
        autocompleteFragment.setPlaceFields(
            listOf(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.ADDRESS,
                Place.Field.LAT_LNG,
                Place.Field.ADDRESS_COMPONENTS
            )
        )

        // 4. Define o listener de seleção
        autocompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                Log.i("Places", "Lugar selecionado: ${place.name}, ${place.address}")

                val latLng = place.latLng ?: return onError("Coordenadas inválidas para o endereço.")

                // Extrai o CEP
                val cep = place.addressComponents?.asList()
                    ?.find { it.types.contains("postal_code") }?.name ?: ""

                val address = place.address ?: place.name ?: "Endereço Desconhecido"

                // Atualiza o mapa: move a câmera para a localização pesquisada
                googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM))

                // Atualiza as variáveis globais
                currentLatitude = latLng.latitude
                currentLongitude = latLng.longitude
                currentCEP = cep
                currentFullAddress = address

                // Atualiza o Bottom Sheet
                onAddressFound(currentFullAddress!!)

                // Recarrega as ocorrências para o novo local
                if (currentCEP != null && currentCEP!!.isNotEmpty()) {
                    getOcorrenciasByCep(currentCEP!!)
                } else {
                    getOcorrenciasByCep("00000-000") // Fallback de CEP
                }

                Toast.makeText(this@UsuarioMainActivity, "Localização de pesquisa carregada.", Toast.LENGTH_SHORT).show()
            }

            override fun onError(status: Status) {
                Log.e("Places", "Ocorreu um erro no Autocomplete: $status")
                Toast.makeText(this@UsuarioMainActivity, "Erro ao buscar endereço: ${status.statusMessage}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    /**
     * Carrega os dados do SharedPreferences e os exibe no nav_header_usuario.
     */
    private fun setupDrawerHeader() {
        val headerView = navView.getHeaderView(0)

        val tvUserName = headerView.findViewById<TextView>(R.id.tv_user_name)
        val tvUserEmail = headerView.findViewById<TextView>(R.id.tv_user_email)
        val btnLogout = headerView.findViewById<Button>(R.id.btn_logout)

        // Carrega os dados salvos
        tvUserName?.text = prefsManager.getUserName() ?: "Usuário Guardway"
        tvUserEmail?.text = prefsManager.getUserEmail() ?: "email@guardway.com"

        // Listener do botão de Logout
        btnLogout?.setOnClickListener {
            performLogout()
        }
    }

    // --- MÉTODOS DE CONTROLE DE ESTADO DO BOTÃO DE PERIGO ---

    /**
     * Gerencia o estado visual do botão de status de perigo (Carregando, Seguro, Perigoso).
     */
    private fun setPerigoStatusLoading(isLoading: Boolean) {
        if (isLoading) {
            // Estado de Carregando
            btnPerigoStatus.text = "Avaliando..."
            // Usando uma cor neutra, por exemplo, cinza escuro
            btnPerigoStatus.setBackgroundColor(ContextCompat.getColor(this, android.R.color.darker_gray))
            btnPerigoStatus.isEnabled = false // Desativa para evitar cliques
        } else {
            // Estado normal (Será sobrescrito imediatamente por onOccurrenceDataReceived)
            btnPerigoStatus.isEnabled = true
        }
    }

    // --- MÉTODOS DE NAVEGAÇÃO DO DRAWER (e Botões) ---

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_minha_conta -> {
                // TODO: Implementar MinhaContaActivity
                Toast.makeText(this, "Navegando para Minha Conta...", Toast.LENGTH_SHORT).show()
            }

            R.id.nav_minhas_ocorrencias -> {
                // TODO: Implementar MinhasOcorrenciasActivity
                Toast.makeText(this, "Navegando para Minhas Ocorrências...", Toast.LENGTH_SHORT)
                    .show()
            }

            R.id.nav_relatorio_seguranca -> {
                navigateToRelatorioSeguranca()
            }

            R.id.nav_chamar_emergencia -> {
                btnEmergencyCall.performClick()
            }

            R.id.nav_sobre_nos -> {
                // TODO: Implementar SobreNosActivity
                Toast.makeText(this, "Abrindo Sobre Nós...", Toast.LENGTH_SHORT).show()
            }

            R.id.nav_ajuda -> {
                // TODO: Implementar CentralAjudaActivity
                Toast.makeText(this, "Abrindo Central de Ajuda...", Toast.LENGTH_SHORT).show()
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun navigateToRelatorioSeguranca() {
        val cepToPass = currentCEP ?: "00000-000"
        val addressToPass = currentFullAddress ?: "Localização em processamento"

        val intent = Intent(this, RelatorioSegurancaActivity::class.java)

        intent.putExtra("endereco", addressToPass)
        intent.putExtra("cep", cepToPass)

        startActivity(intent)

        if (currentCEP == null || currentFullAddress == null) {
            Toast.makeText(this, "Abrindo Relatório. Dados iniciais podem estar incompletos.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Abrindo Relatório de Segurança...", Toast.LENGTH_SHORT).show()
        }
    }


    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    /**
     * Limpa os dados de sessão do SharedPreferences e redireciona para a tela de Visitante.
     */
    private fun performLogout() {
        prefsManager.clearData()
        Toast.makeText(this, "Sessão encerrada.", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, VisitanteMainActivity::class.java)
        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    // --- MÉTODOS DE LOCALIZAÇÃO, MAPA E API ---

    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap
        getUserLocation()
    }

    private fun getUserLocation() {
        val map = this.googleMap ?: return

        // 1. Limpa todos os marcadores antigos
        map.clear()

        var userLatLng: LatLng? = null

        // 2. Tenta habilitar a camada 'My Location' (ponto azul)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            map.isMyLocationEnabled = true
        }

        // 3. Adiciona todos os locais à lista de places (incluindo a localização do usuário)
        places.forEach { place ->
            if (place.name == "Sua Localização") {
                userLatLng = place.latLng
            } else {
                // Adiciona ocorrências (ou outros lugares) no mapa
                map.addMarker(MarkerOptions().title(place.name).position(place.latLng))
            }
        }

        // 4. Move a câmera para a localização do usuário (ou padrão)
        val targetLatLng = userLatLng ?: LatLng(DEFAULT_LATITUDE, DEFAULT_LONGITUDE)

        if (userLatLng != null) {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng!!, DEFAULT_ZOOM))
        } else {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(targetLatLng, DEFAULT_ZOOM))
        }
    }

    private fun requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                PERMISSION_REQUEST_CODE
            )
        } else {
            getCurrentLocation()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation()
            } else {
                Log.d("Location", "Permissão de localização negada pelo usuário. Usando padrão.")
                onError("Permissão de localização negada. Exibindo localização padrão.")
                getUserLocation()
            }
        }
    }


    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestLocationPermission()
            return
        }
        val cancellationTokenSource = CancellationTokenSource()

        fusedLocationClient.getCurrentLocation(
            LOCATION_PRIORITY,
            cancellationTokenSource.token
        )
            .addOnSuccessListener { location ->
                if (location != null) {
                    val userLatLng = LatLng(location.latitude, location.longitude)

                    currentLatitude = location.latitude
                    currentLongitude = location.longitude

                    performReverseGeocoding(currentLatitude!!, currentLongitude!!)

                    places.removeAll { it.name == "Sua Localização" }
                    // CORREÇÃO 2: Criação da instância LocalPlace
                    val userPlace = LocalPlace(
                        name = "Sua Localização",
                        latLng = userLatLng,
                        address = "Você está aqui!",
                        rating = 5.0f
                    )
                    places.add(userPlace)
                    getUserLocation()

                } else {
                    getLastKnownLocationFallback()
                }
            }
            .addOnFailureListener { e ->
                Log.e("LOCATION", "getCurrentLocation falhou: ${e.message}")
                getLastKnownLocationFallback()
            }
    }

    private fun getLastKnownLocationFallback() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            onError("Permissão negada. Localização padrão.")
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    val userLatLng = LatLng(location.latitude, location.longitude)
                    currentLatitude = location.latitude
                    currentLongitude = location.longitude
                    performReverseGeocoding(currentLatitude!!, currentLongitude!!)

                    places.removeAll { it.name == "Sua Localização" }
                    // CORREÇÃO 3: Criação da instância LocalPlace
                    val userPlace = LocalPlace(
                        name = "Sua Localização",
                        latLng = userLatLng,
                        address = "Você está aqui!",
                        rating = 5.0f
                    )
                    places.add(userPlace)
                    getUserLocation()

                } else {
                    onError("Não foi possível obter a localização atual ou de fallback.")
                    currentFullAddress = DEFAULT_ADDRESS
                    onAddressFound(DEFAULT_ADDRESS)
                    getUserLocation()
                }
            }
            .addOnFailureListener { e ->
                Log.e("LOCATION", "getLastKnownLocationFallback falhou: ${e.message}")
                onError("Erro no fallback de localização.")
                currentFullAddress = DEFAULT_ADDRESS
                onAddressFound(DEFAULT_ADDRESS)
                getUserLocation()
            }
    }

    private fun performReverseGeocoding(lat: Double, lon: Double) {
        if (!Geocoder.isPresent()) {
            currentCEP = null
            onError("Geocoder indisponível.")
            onAddressFound("Geocoder indisponível. Coordenadas: $lat, $lon")
            return
        }
        try {
            val geocoder = Geocoder(this, Locale("pt", "BR"))
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(lat, lon, 1)

            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                val postalCode = address.postalCode
                val fullAddress = address.getAddressLine(0) ?: "Endereço Desconhecido"

                currentFullAddress = fullAddress
                onAddressFound(fullAddress)

                if (postalCode != null) {
                    currentCEP = postalCode
                    getOcorrenciasByCep(postalCode)
                } else {
                    currentCEP = null
                    onOccurrenceDataReceived(
                        ApiService.OcorrenciaCepResponse(
                            "Seguro",
                            0,
                            currentFullAddress
                        )
                    )
                }
            } else {
                currentCEP = null
                onError("Nenhum endereço encontrado.")
                onAddressFound("Nenhum endereço encontrado para: $lat, $lon")
            }
        } catch (e: Exception) {
            currentCEP = null
            onError("Erro ao decodificar endereço.")
            onAddressFound("Erro ao decodificar. Coordenadas: $lat, $lon")
        }
    }

    private fun getOcorrenciasByCep(cep: String) {

        setPerigoStatusLoading(true)

        apiService.getOcorrenciasPorCep(cep)
            .enqueue(object : Callback<List<ApiService.OcorrenciaItem>> {

                override fun onResponse(
                    call: Call<List<ApiService.OcorrenciaItem>>,
                    response: Response<List<ApiService.OcorrenciaItem>>
                ) {

                    if (response.isSuccessful && response.body() != null) {
                        val ocorrencias = response.body()!!
                        val count = ocorrencias.size

                        places.removeAll { it.name != "Sua Localização" }

                        ocorrencias.forEach { item ->
                            try {
                                val latLng = LatLng(item.latitude, item.longitude)
                                // CORREÇÃO 4: Criação da instância LocalPlace
                                val markerPlace = LocalPlace(
                                    name = "${item.tipo_ocorrencia} (#${item.id_ocorrencia})",
                                    latLng = latLng,
                                    address = item.endereco ?: "Ocorrência",
                                    rating = 0f
                                )
                                places.add(markerPlace)
                            } catch (e: Exception) {
                                Log.e("API_OCORRENCIAS", "Erro ao criar LatLng para ocorrência ${item.id_ocorrencia}: ${e.message}")
                            }
                        }

                        getUserLocation()

                        val statusText = if (count > DANGER_THRESHOLD) "PERIGOSO" else "SEGURO"
                        val addressFromApi = ocorrencias.firstOrNull()?.endereco ?: currentFullAddress

                        onOccurrenceDataReceived(
                            ApiService.OcorrenciaCepResponse(
                                status = statusText,
                                count = count,
                                address = addressFromApi
                            )
                        )
                    } else {
                        onOccurrenceDataReceived(
                            ApiService.OcorrenciaCepResponse(
                                "Seguro",
                                0,
                                currentFullAddress
                            )
                        )
                    }
                }

                override fun onFailure(call: Call<List<ApiService.OcorrenciaItem>>, t: Throwable) {

                    Toast.makeText(
                        this@UsuarioMainActivity,
                        "Falha de conexão: ${t.message}",
                        Toast.LENGTH_SHORT
                    ).show()

                    onOccurrenceDataReceived(
                        ApiService.OcorrenciaCepResponse(
                            "Erro de Rede",
                            0,
                            currentFullAddress
                        )
                    )
                }
            })
    }

    override fun onAddressFound(address: String) {
        tvAddressTitle.text = address
        bottomSheet.visibility = View.VISIBLE
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    override fun onOccurrenceDataReceived(data: ApiService.OcorrenciaCepResponse) {

        setPerigoStatusLoading(false)

        val count = data.count
        val statusText = data.status

        val colorResId = when (statusText) {
            "PERIGOSO" -> android.R.color.black
            "Erro de Rede", "Falha na Rede" -> android.R.color.darker_gray
            else -> android.R.color.holo_green_dark
        }
        val color = ContextCompat.getColor(this, colorResId)

        val buttonText = "$statusText\n$count ocorrência(s)"
        btnPerigoStatus.text = buttonText
        btnPerigoStatus.setBackgroundColor(color)

        if (data.address != null && data.address.isNotEmpty()) {
            tvAddressTitle.text = data.address
            currentFullAddress = data.address
        }

        bottomSheet.visibility = View.VISIBLE
        if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_HIDDEN) {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }
    }

    override fun onError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        Log.e("MAIN_ERROR", message)

        setPerigoStatusLoading(false)
        btnPerigoStatus.text = "ERRO\nSem Dados"
        btnPerigoStatus.setBackgroundColor(ContextCompat.getColor(this, android.R.color.darker_gray))
        tvAddressTitle.text = DEFAULT_ADDRESS

        bottomSheet.visibility = View.VISIBLE
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    // A classe customizada foi renomeada para LocalPlace para evitar conflito com a Place do Google Places
    data class LocalPlace(
        val name: String,
        val latLng: LatLng,
        val address: String,
        val rating: Float
    )
}