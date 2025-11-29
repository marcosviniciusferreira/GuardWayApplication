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
import android.widget.ImageButton // Import necessário para o novo botão
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

// --- Interface para comunicação de dados do mapa para a UI ---
interface OnMapDataFound {
    fun onAddressFound(address: String)
    fun onOccurrenceDataReceived(data: ApiService.OcorrenciaCepResponse)
    fun onError(message: String)
}

class MainActivity : AppCompatActivity(), OnMapReadyCallback, OnMapDataFound, NavigationView.OnNavigationItemSelectedListener {

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
        private const val DEFAULT_ZOOM = 15f
        private const val LOCATION_PRIORITY = com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
        private const val BASE_URL = "http://192.168.1.15/" // Base URL do seu servidor

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
        setContentView(R.layout.activity_main)

        // --- Configuração da Toolbar e Drawer ---
        drawerLayout = findViewById(R.id.drawer_layout)
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar) // Define a Toolbar como ActionBar
        supportActionBar?.setDisplayShowTitleEnabled(false) // Oculta o título padrão da Activity
        toolbar.title = null // Garante que o título da Toolbar esteja nulo


        // 🌟 INICIALIZAÇÃO DO BOTÃO DE PERFIL E LISTENER
        btnUserProfile = findViewById(R.id.btn_user_profile)
        btnUserProfile.setOnClickListener {
            navigateToLogin()
        }
        // -------------------------------------------------------------

        val toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            toolbar,
            R.string.navigation_drawer_open, // Certifique-se de que estas strings estão em strings.xml
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

            // 1. Aplica padding nas laterais (o topo é tratado pela Toolbar/AppBarLayout)
            v.setPadding(systemBars.left, 0, systemBars.right, 0)

            // 2. Aplica o padding inferior DENTRO do Bottom Sheet para compensar a barra de navegação.
            val initialPaddingBottom = resources.getDimensionPixelSize(R.dimen.bottom_sheet_padding_base)

            bottomSheet.setPadding(
                bottomSheet.paddingLeft,
                bottomSheet.paddingTop,
                bottomSheet.paddingRight,
                initialPaddingBottom + systemBars.bottom // Adiciona o insets inferior
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

        // Permite que o painel seja completamente ocultado arrastando para baixo
        bottomSheetBehavior.isHideable = true

        // Define a altura fixa para o estado recolhido (COLLAPSED)
        val peekHeight = resources.getDimensionPixelSize(R.dimen.bottom_sheet_peek_height)
        bottomSheetBehavior.peekHeight = peekHeight

        // Habilita o comportamento deslizante (Draggable)
        bottomSheetBehavior.isDraggable = true

        // 🌟 Define o estado inicial como EXPANDIDO (ABERTO por padrão)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        bottomSheet.visibility = View.VISIBLE // Garante visibilidade imediata


        // Listener do botão de emergência (AGORA COM ACTION_DIAL)
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

    // --- Métodos do Drawer Layout ---

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_login -> {
                navigateToLogin() // Usa o método refatorado
            }
            R.id.nav_sobre_nos -> {
                Toast.makeText(this, "Abrindo Sobre Nós...", Toast.LENGTH_SHORT).show()
            }
            R.id.nav_ajuda -> {
                Toast.makeText(this, "Abrindo Central de Ajuda...", Toast.LENGTH_SHORT).show()
            }
            R.id.nav_emergencia -> {
                // Reutiliza o clique do botão para centralizar a lógica de emergência
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
        // Assume que LoginActivity::class.java é uma referência válida para a sua tela de login.
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

    // --- Implementação do Mapa e Localização (Sem Alterações) ---

    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap
        getUserLocation()
    }

    private fun getUserLocation() {
        val map = this.googleMap ?: return
        map.clear()
        // ... (resto do código de getUserLocation)
        // Implementação omitida por brevidade, assumindo que está correta
        // ...

        var userLatLng: LatLng? = null
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED) {
            map.isMyLocationEnabled = true
        }
        // Lógica de marcadores
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
        // ... (resto do código de getCurrentLocation)
        // Implementação omitida por brevidade, assumindo que está correta
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

    // --- Implementação da API (Sem Alterações) ---
    private fun getOcorrenciasByCep(cep: String) {
        apiService.getOcorrenciasPorCep(cep).enqueue(object : Callback<ApiService.OcorrenciaCepResponse> {
            override fun onResponse(call: Call<ApiService.OcorrenciaCepResponse>, response: Response<ApiService.OcorrenciaCepResponse>) {
                if (response.isSuccessful) {
                    response.body()?.let { data ->
                        onOccurrenceDataReceived(data)
                    } ?: onError("Resposta da API vazia.")
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

            override fun onFailure(call: Call<ApiService.OcorrenciaCepResponse>, t: Throwable) {
                Toast.makeText(this@MainActivity, "Falha de conexão com o servidor.", Toast.LENGTH_SHORT).show()
                onOccurrenceDataReceived(
                    ApiService.OcorrenciaCepResponse(
                        "Seguro",
                        0,
                        currentFullAddress
                    )
                )
            }
        })
    }

    // --- Implementação da Interface OnMapDataFound (Ajustada para Estado Inicial EXPANDIDO) ---

    override fun onAddressFound(address: String) {
        tvAddressTitle.text = address
        // Já que o estado inicial é EXPANDED, não precisamos forçar o estado aqui.
        if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_HIDDEN) {
            // Caso o usuário tenha escondido e novos dados cheguem, podemos voltar ao estado COLLAPSED ou EXPANDED.
            // Manter COLLAPSED para não ser muito intrusivo
            bottomSheet.visibility = View.VISIBLE
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }
    }

    override fun onOccurrenceDataReceived(data: ApiService.OcorrenciaCepResponse) {
        val count = data.count
        val statusText = if (count > DANGER_THRESHOLD) "PERIGOSO" else "SEGURO"

        // Usa as cores corretas
        val colorResId = if (count > DANGER_THRESHOLD) R.color.black else android.R.color.holo_green_dark
        val color = ContextCompat.getColor(this, colorResId)

        val buttonText = "$statusText\n$count ocorrência(s)"
        btnPerigoStatus.text = buttonText
        btnPerigoStatus.setBackgroundColor(color)

        if (data.address != null && data.address.isNotEmpty()) {
            tvAddressTitle.text = data.address
            currentFullAddress = data.address
        }

        // Se o painel estiver escondido e novos dados chegarem, o ideal é reexibi-lo
        if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_HIDDEN) {
            bottomSheet.visibility = View.VISIBLE
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }
    }

    override fun onError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show() // Use LONG para erros críticos
        // Oculta o painel apenas se o erro for grave (ex: falha de permissão)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        bottomSheet.visibility = View.GONE
    }
}

data class Place(
    val name: String,
    val latLng: LatLng,
    val address: String,
    val rating: Float
)