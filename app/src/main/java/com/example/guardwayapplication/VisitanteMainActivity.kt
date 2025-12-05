package com.example.guardwayapplication

import ApiService
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
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
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.location.LocationServices
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.button.MaterialButton
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.material.navigation.NavigationView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Locale

class VisitanteMainActivity : AppCompatActivity(), OnMapReadyCallback, OnMapDataFound, NavigationView.OnNavigationItemSelectedListener {

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
        private const val DEFAULT_ZOOM = 15f
        // O LocationRequest é do pacote com.google.android.gms.location, corrigindo o erro de importação.
        private const val LOCATION_PRIORITY = LocationRequest.PRIORITY_HIGH_ACCURACY
        private const val BASE_URL = "http://192.168.1.9/"

        private const val DANGER_THRESHOLD = 5
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var googleMap: GoogleMap? = null

    // --- Componentes do Drawer e Toolbar ---
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toolbar: Toolbar
    private lateinit var navView: NavigationView
    private lateinit var btnUserProfile: ImageButton

    // --- Referências para a UI do Bottom Sheet ---
    private lateinit var bottomSheet: LinearLayout
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>
    private lateinit var tvAddressTitle: TextView
    private lateinit var btnPerigoStatus: MaterialButton
    private lateinit var btnEmergencyCall: MaterialButton
    // ---------------------------------------------

    lateinit var apiService: ApiService

    private var currentLatitude: Double? = null
    private var currentLongitude: Double? = null
    private var currentCEP: String? = null
    private var currentFullAddress: String? = null

    private var places = mutableListOf<Place>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_visitante_main)

        // --- Configuração da Toolbar e Drawer ---
        drawerLayout = findViewById(R.id.drawer_layout)
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.title = null

        btnUserProfile = findViewById(R.id.btn_user_profile)
        btnUserProfile.setOnClickListener {
            navigateToLogin()
        }
        // -------------------------------------------------------------

        val toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navView = findViewById(R.id.nav_view)
        navView.setNavigationItemSelectedListener(this)
        // ------------------------------------------

        // --- Inicialização do Bottom Sheet e Componentes ---
        bottomSheet = findViewById(R.id.bottom_sheet)
        tvAddressTitle = findViewById(R.id.tv_address_title)
        btnPerigoStatus = findViewById(R.id.btn_perigo_status)
        btnEmergencyCall = findViewById(R.id.btn_emergency_call)
        // -------------------------------------

        // --- Aplicando Insets (Correção para a barra de navegação e visibilidade) ---
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            v.setPadding(systemBars.left, 0, systemBars.right, 0)

            val initialPaddingBottom = resources.getDimensionPixelSize(R.dimen.bottom_sheet_padding_base)

            bottomSheet.setPadding(
                bottomSheet.paddingLeft,
                bottomSheet.paddingTop,
                bottomSheet.paddingRight,
                initialPaddingBottom + systemBars.bottom
            )
            insets
        }
        // -------------------------------------------------------------

        // --- Inicialização do Retrofit ---
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiService = retrofit.create(ApiService::class.java)
        // ---------------------------------

        // Configura o comportamento do painel deslizante
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.isHideable = true
        val peekHeight = resources.getDimensionPixelSize(R.dimen.bottom_sheet_peek_height)
        bottomSheetBehavior.peekHeight = peekHeight
        bottomSheetBehavior.isDraggable = true
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        bottomSheet.visibility = View.VISIBLE


        // Listener do botão de emergência
        btnEmergencyCall.setOnClickListener {
            Toast.makeText(this, "Abrindo discador de emergência (190)...", Toast.LENGTH_SHORT).show()
            val numeroEmergencia = "tel:190"
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse(numeroEmergencia))
            startActivity(intent)
        }


        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        requestLocationPermission()

        val mapFragment =
            supportFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
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
    // --- FIM DOS MÉTODOS DE CONTROLE DE ESTADO ---


    // --- Métodos do Drawer Layout ---

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_login -> {
                navigateToLogin() // Leva para a tela de Login
            }
            R.id.nav_sobre_nos -> {
                // TODO: Implementar SobreNosActivity
                Toast.makeText(this, "Abrindo Sobre Nós...", Toast.LENGTH_SHORT).show()
            }
            R.id.nav_ajuda -> {
                // TODO: Implementar CentralAjudaActivity
                Toast.makeText(this, "Abrindo Central de Ajuda...", Toast.LENGTH_SHORT).show()
            }
            R.id.nav_emergencia -> {
                btnEmergencyCall.performClick()
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    /**
     * Lógica unificada de navegação para a tela de Login.
     */
    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        Toast.makeText(this, "Navegando para Login...", Toast.LENGTH_SHORT).show()
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    // --- Implementação do Mapa e Localização ---

    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap
        getUserLocation()
    }

    private fun getUserLocation() {
        val map = this.googleMap ?: return
        map.clear()

        var userLatLng: LatLng? = null
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED) {
            map.isMyLocationEnabled = true
        }

        places.forEach { place ->
            if (place.name == "Sua Localização") userLatLng = place.latLng
            else map.addMarker(MarkerOptions().title(place.name).position(place.latLng))
        }
        if (userLatLng != null) {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng!!, DEFAULT_ZOOM))
        } else if (places.isNotEmpty()) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(places.first().latLng, DEFAULT_ZOOM))
        }
    }

    private fun requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
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
                Log.d("Location", "Permissão de localização negada pelo usuário.")
                if (googleMap != null) {
                    val saoPaulo = LatLng(-23.5505, -46.6333)
                    googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(saoPaulo, DEFAULT_ZOOM))
                    onError("Permissão de localização negada.")
                }
            }
        }
    }


    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED) {
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
                    val userPlace = Place(
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
                getLastKnownLocationFallback()
            }
    }

    private fun getLastKnownLocationFallback() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED) {
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
                    val userPlace = Place(
                        name = "Sua Localização",
                        latLng = userLatLng,
                        address = "Você está aqui!",
                        rating = 5.0f
                    )
                    places.add(userPlace)
                    getUserLocation()
                } else {
                    onError("Não foi possível obter a localização atual ou de fallback.")
                }
            }
            .addOnFailureListener { e ->
                onError("Erro no fallback de localização.")
            }
    }

    private fun performReverseGeocoding(lat: Double, lon: Double) {
        if (!Geocoder.isPresent()) {
            currentCEP = null
            onError("Geocoder indisponível.")
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
            }
        } catch (e: Exception) {
            currentCEP = null
            onError("Erro ao decodificar endereço.")
        }
    }
    private fun getOcorrenciasByCep(cep: String) {

        setPerigoStatusLoading(true)

        apiService.getOcorrenciasPorCep(cep).enqueue(object : Callback<List<ApiService.OcorrenciaItem>> {

            override fun onResponse(call: Call<List<ApiService.OcorrenciaItem>>, response: Response<List<ApiService.OcorrenciaItem>>) {
                Log.d("API_OCORRENCIAS", "Status Code: ${response.code()}")
                Log.d("API_OCORRENCIAS", "isSuccessful: ${response.isSuccessful}")

                if (response.isSuccessful && response.body() != null) {
                    val ocorrencias = response.body()!!
                    val count = ocorrencias.size

                    Log.d("API_OCORRENCIAS", "Ocorrências contadas: $count")

                    // Limpa e redesenha os marcadores (Mantendo o marcador "Sua Localização")
                    places.removeAll { it.name != "Sua Localização" }
                    ocorrencias.forEach { item ->
                        try {
                            val latLng = LatLng(item.latitude, item.longitude)
                            val markerPlace = Place(
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

                    // Tenta obter o endereço da primeira ocorrência, se não, usa o endereço do Geocoder
                    val addressFromApi = ocorrencias.firstOrNull()?.endereco ?: currentFullAddress

                    // 🌟 CHAMADA CORRIGIDA: Envia os dados de volta para a UI
                    onOccurrenceDataReceived(
                        ApiService.OcorrenciaCepResponse(
                            status = statusText,
                            count = count,
                            address = addressFromApi
                        )
                    )
                } else {
                    Log.e("API_OCORRENCIAS", "Erro ou corpo vazio: ${response.errorBody()?.string()}")
                    // Se falhar, usa o valor padrão de 0
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

                // ⭐️ CORREÇÃO: Em caso de falha de rede, trata o estado ⭐️
                Log.e("API_FALHA", "Erro de Conexão: ${t.message}", t)
                Toast.makeText(this@VisitanteMainActivity, "Falha de conexão com o servidor.", Toast.LENGTH_SHORT).show()

                onOccurrenceDataReceived(
                    ApiService.OcorrenciaCepResponse(
                        "Erro de Rede", // Status para indicar falha
                        0,
                        currentFullAddress
                    )
                )
            }
        })
    }

    // --- Implementação da Interface OnMapDataFound ---

    override fun onAddressFound(address: String) {
        tvAddressTitle.text = address
        if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_HIDDEN) {
            bottomSheet.visibility = View.VISIBLE
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }
    }

    override fun onOccurrenceDataReceived(data: ApiService.OcorrenciaCepResponse) {

        // ⭐️ CORREÇÃO: Restaura o botão e aplica a cor final ⭐️
        setPerigoStatusLoading(false)

        val count = data.count
        val statusText = data.status

        val colorResId = when (statusText) {
            "PERIGOSO" -> R.color.guardway_red
            "Erro de Rede" -> android.R.color.darker_gray // Cinza para erro
            else -> android.R.color.holo_green_dark // Verde para Seguro/Default
        }
        val color = ContextCompat.getColor(this, colorResId)

        val buttonText = "$statusText\n$count ocorrência(s)"
        btnPerigoStatus.text = buttonText
        btnPerigoStatus.setBackgroundColor(color)

        if (data.address != null && data.address.isNotEmpty()) {
            tvAddressTitle.text = data.address
            currentFullAddress = data.address
        }

        if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_HIDDEN) {
            bottomSheet.visibility = View.VISIBLE
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }
    }

    override fun onError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        bottomSheet.visibility = View.GONE
    }
}

// data class usada para a localização no mapa.
data class Place(
    val name: String,
    val latLng: LatLng,
    val address: String,
    val rating: Float
)