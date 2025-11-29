package com.example.guardwayapplication

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.gms.tasks.CancellationTokenSource
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Locale

// --- Interface para a resposta da API (Ajuste conforme sua API real) ---
// Deve ser definida em seu próprio arquivo Kotlin se for usada em outros lugares.
data class OcorrenciaCepResponse(
    val status: String, // Ex: "Seguro", "Perigoso"
    val count: Int,
    val address: String? = null // Endereço formatado (opcional)
)

// --- Interface para comunicação de dados do mapa para a UI ---
interface OnMapDataFound {
    fun onAddressFound(address: String)
    fun onOccurrenceDataReceived(data: OcorrenciaCepResponse)
    fun onError(message: String)
}

// Assumindo que você tem uma ApiService com este método:
// @GET("api/ocorrencias/cep/{cep}")
// fun getOcorrenciasPorCep(@Path("cep") cep: String): Call<OcorrenciaCepResponse>
interface ApiService {
    // Exemplo do método que você precisa adicionar (ajuste a URL)
    fun getOcorrenciasPorCep(cep: String): Call<OcorrenciaCepResponse>
}

class MainActivity : AppCompatActivity(), OnMapReadyCallback, OnMapDataFound {

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
        private const val DEFAULT_ZOOM = 15f
        private const val LOCATION_PRIORITY = com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
        private const val BASE_URL = "http://192.168.1.15/" // Base URL do seu servidor
        private const val DANGER_THRESHOLD = 5 // Se for maior que 5, é "PERIGOSO"
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var googleMap: GoogleMap? = null

    // --- Referências para a UI do Bottom Sheet ---
    private lateinit var bottomSheet: LinearLayout
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>
    private lateinit var tvAddressTitle: TextView
    private lateinit var btnPerigoStatus: MaterialButton
    private lateinit var btnEmergencyCall: MaterialButton
    private lateinit var fabAddOcorrencia: FloatingActionButton
    // ---------------------------------------------

    private lateinit var apiService: ApiService

    private var currentLatitude: Double? = null
    private var currentLongitude: Double? = null
    private var currentCEP: String? = null
    private var currentFullAddress: String? = null // Adicionado para armazenar o endereço completo

    private var places = mutableListOf<Place>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // --- Inicialização do Retrofit ---
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiService = retrofit.create(ApiService::class.java)
        // ---------------------------------

        // --- Inicialização do Bottom Sheet e Componentes ---
        bottomSheet = findViewById(R.id.bottom_sheet)
        tvAddressTitle = findViewById(R.id.tv_address_title)
        btnPerigoStatus = findViewById(R.id.btn_perigo_status)
        btnEmergencyCall = findViewById(R.id.btn_emergency_call)

        // Configura o comportamento do painel deslizante
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN // Começa escondido
        // -------------------------------------

        // Listener do botão de emergência (exemplo)
        btnEmergencyCall.setOnClickListener {
            Toast.makeText(this, "Acionando chamada de emergência...", Toast.LENGTH_SHORT).show()
            // Implementar a lógica real de chamada aqui
        }

        // Listener do FAB (leva para o formulário de adição de ocorrência)
        fabAddOcorrencia.setOnClickListener {
            val intent = Intent(this, OccurrenceFormActivity::class.java)
            // Se necessário, passe a localização atual para o formulário
            intent.putExtra("LATITUDE", currentLatitude)
            intent.putExtra("LONGITUDE", currentLongitude)
            intent.putExtra("ADDRESS", currentFullAddress)
            startActivity(intent)
        }


        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        requestLocationPermission()

        val mapFragment =
            supportFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap
        getUserLocation()
    }

    private fun getUserLocation() {
        val map = this.googleMap ?: return
        map.clear()

        var userLatLng: LatLng? = null

        places.forEach { place ->
            if (place.name == "Sua Localização") {
                userLatLng = place.latLng

                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED) {
                    map.isMyLocationEnabled = true
                }

            } else {
                map.addMarker(
                    MarkerOptions()
                        .title(place.name)
                        .snippet(place.address)
                        .position(place.latLng)
                )
            }
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

                    Log.d("Location", "Localização atual OBTIDA com sucesso: $userLatLng")
                    getUserLocation()

                } else {
                    Log.d("Location", "getCurrentLocation retornou nulo. Tentando fallback...")
                    getLastKnownLocationFallback()
                }
            }
            .addOnFailureListener { e ->
                Log.e("Location", "Erro ao obter localização atual: ${e.message}")
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
                    Log.d("Location", "Localização de fallback OBTIDA: $userLatLng")
                    getUserLocation()
                } else {
                    Log.d("Location", "Localização de fallback também é nula.")
                    onError("Não foi possível obter a localização atual ou de fallback.")
                }
            }
            .addOnFailureListener { e ->
                Log.e("Location", "Erro no fallback de localização: ${e.message}")
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

                currentFullAddress = fullAddress // Armazena o endereço completo
                onAddressFound(fullAddress) // Atualiza o título do Bottom Sheet

                if (postalCode != null) {
                    currentCEP = postalCode
                    Log.i("Geocoder", "CEP encontrado: $currentCEP. Buscando dados da API...")
                    getOcorrenciasByCep(postalCode) // Chamada para a API
                } else {
                    currentCEP = null
                    Log.w("Geocoder", "CEP não encontrado. Exibindo dados genéricos.")
                    // Se o CEP falhar, usa uma resposta padrão (0 ocorrências, Seguro)
                    onOccurrenceDataReceived(OcorrenciaCepResponse("Seguro", 0, currentFullAddress))
                }
            } else {
                currentCEP = null
                onError("Nenhum endereço encontrado.")
            }
        } catch (e: Exception) {
            currentCEP = null
            Log.e("Geocoder", "Erro no Geocoding: ${e.message}")
            onError("Erro ao decodificar endereço.")
        }
    }

    // --- Implementação da API ---
    private fun getOcorrenciasByCep(cep: String) {
        apiService.getOcorrenciasPorCep(cep).enqueue(object : Callback<OcorrenciaCepResponse> {
            override fun onResponse(call: Call<OcorrenciaCepResponse>, response: Response<OcorrenciaCepResponse>) {
                if (response.isSuccessful) {
                    response.body()?.let { data ->
                        onOccurrenceDataReceived(data)
                    } ?: onError("Resposta da API vazia.")
                } else {
                    Log.e("API Ocorrencias", "Erro ao buscar dados. Código: ${response.code()}")
                    // Tenta exibir a resposta padrão se a API falhar, mas o endereço foi encontrado
                    onOccurrenceDataReceived(OcorrenciaCepResponse("Seguro", 0, currentFullAddress))
                }
            }

            override fun onFailure(call: Call<OcorrenciaCepResponse>, t: Throwable) {
                Log.e("API Ocorrencias", "Falha de conexão: ${t.message}", t)
                onError("Falha de conexão com o servidor.")
            }
        })
    }

    // --- Implementação da Interface OnMapDataFound (Atualização da UI) ---

    override fun onAddressFound(address: String) {
        tvAddressTitle.text = address
        // Expande o painel para o estado 'recolhido' (parcialmente visível)
        if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_HIDDEN) {
            bottomSheet.visibility = View.VISIBLE
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }
    }

    // ESTA FUNÇÃO ESTAVA COMENTADA E FOI DESCOMENTADA
    override fun onOccurrenceDataReceived(data: OcorrenciaCepResponse) {
        val count = data.count
        val statusText = if (count > DANGER_THRESHOLD) "PERIGOSO" else "SEGURO"

        // Determina a cor com base no limite
        val colorResId = if (count > DANGER_THRESHOLD) R.color.black else R.color.white
        val color = ContextCompat.getColor(this, colorResId)

        // Atualiza o texto do botão de status
        val buttonText = "$statusText\n$count ocorrência(s)"
        btnPerigoStatus.text = buttonText
        btnPerigoStatus.setBackgroundColor(color)

        // Se o endereço da API for mais detalhado, use-o
        if (data.address != null) {
            tvAddressTitle.text = data.address
            currentFullAddress = data.address
        }

        // Garante que o painel esteja visível e no estado recolhido
        if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_HIDDEN) {
            bottomSheet.visibility = View.VISIBLE
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }
    }

    // ESTA FUNÇÃO ESTAVA COMENTADA E FOI DESCOMENTADA
    override fun onError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        // Esconde o painel em caso de erro grave (como falta de permissão ou geocoder)
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