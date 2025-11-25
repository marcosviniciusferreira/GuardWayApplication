package com.example.guardwayapplication

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
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
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.tasks.CancellationTokenSource
import java.util.Locale

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
        private const val DEFAULT_ZOOM = 15f
        // Adicionado para usar no getCurrentLocation()
        // ATENÇÃO: Se não estiver definido, você precisará criá-lo.
        private const val LOCATION_PRIORITY = com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var googleMap: GoogleMap? = null

    // Váriaveis para armazenar as coordenadas solicitadas pelo usuário
    private var currentLatitude: Double? = null
    private var currentLongitude: Double? = null
    private var currentCEP: String? = null // Váriavel para armazenar o CEP encontrado

    // Removendo o item padrão para evitar que o mapa centralize em (0, 0)
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

        // 1. Inicialize o FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // 2. Chame a função para solicitar a permissão
        requestLocationPermission()

        val mapFragment =
            supportFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap // Armazena a referência do mapa
        // O mapa está pronto. Se a permissão já foi dada, a localização será carregada.
        // Se a localização já foi obtida (antes do mapa estar pronto), 'places' será atualizada.
        getUserLocation()
    }

    private fun getUserLocation() {
        val map = this.googleMap ?: return
        map.clear() // Limpa todos os marcadores existentes (incluindo o anterior do usuário, se houver)

        var userLatLng: LatLng? = null

        places.forEach { place ->
            // NOVO: Se o nome for "Sua Localização", não adicione o marcador customizado
            if (place.name == "Sua Localização") {
                userLatLng = place.latLng // Ainda armazena a localização para centralizar a câmera

                // Habilita o indicador de localização nativo do Google Maps (o ponto azul)
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED) {
                    map.isMyLocationEnabled = true
                }
                // Não chame map.addMarker() para esta localização!

            } else {
                // Se for qualquer outro lugar (marcador de destino, etc.), adicione o marcador
                map.addMarker(
                    MarkerOptions()
                        .title(place.name)
                        .snippet(place.address)
                        .position(place.latLng)
                )
            }
        }

        // Centraliza a câmera na localização do usuário ou em outro marcador
        if (userLatLng != null) {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng!!, DEFAULT_ZOOM))
        } else if (places.isNotEmpty()) {
            // Caso especial: centraliza em outro marcador, se houver
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
            // Permissão JÁ está concedida, solicita a localização atual.
            getCurrentLocation()
        }
    }

    // CORREÇÃO: Estrutura lógica do if/else/catch.
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permissão concedida.
                getCurrentLocation()
            } else {
                // Permissão negada.
                Log.d("Location", "Permissão de localização negada pelo usuário.")
                // Opcional: Centralizar o mapa em uma localização padrão (ex: São Paulo)
                if (googleMap != null) {
                    val saoPaulo = LatLng(-23.5505, -46.6333)
                    googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(saoPaulo, DEFAULT_ZOOM))
                }
            }
        }
    }


    private fun getCurrentLocation() {
        // 1. Verificação de Permissão (Obrigatória)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED) {
            // Se a permissão não foi concedida, retorna e a solicita (para garantir que não pare o app)
            requestLocationPermission()
            return
        }

        // 2. Cria o token de cancelamento
        val cancellationTokenSource = CancellationTokenSource()

        // 3. Solicita a localização atual
        fusedLocationClient.getCurrentLocation(
            LOCATION_PRIORITY,
            cancellationTokenSource.token
        )
            .addOnSuccessListener { location ->
                if (location != null) {
                    val userLatLng = LatLng(location.latitude, location.longitude)

                    // --- NOVA LÓGICA: Armazena e Processa Coordenadas ---
                    currentLatitude = location.latitude
                    currentLongitude = location.longitude

                    Log.d("Location", "Coordenadas salvas: $currentLatitude, $currentLongitude")
                    performReverseGeocoding(currentLatitude!!, currentLongitude!!)
                    // ---------------------------------------------------

                    // Remove e adiciona o novo marcador (para atualização do mapa)
                    places.removeAll { it.name == "Sua Localização" }
                    val userPlace = Place(
                        name = "Sua Localização",
                        latLng = userLatLng,
                        address = "Você está aqui!",
                        rating = 5.0f
                    )
                    places.add(userPlace)

                    Log.d("Location", "Localização atual OBTIDA com sucesso: $userLatLng")
                    getUserLocation() // Atualiza o mapa com o novo local e centraliza

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

    // Fallback: Tenta a última localização
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

                    // --- NOVA LÓGICA: Armazena e Processa Coordenadas ---
                    currentLatitude = location.latitude
                    currentLongitude = location.longitude

                    Log.d("Location", "Coordenadas salvas (Fallback): $currentLatitude, $currentLongitude")
                    performReverseGeocoding(currentLatitude!!, currentLongitude!!)
                    // ---------------------------------------------------

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
                }
            }
            .addOnFailureListener { e ->
                Log.e("Location", "Erro no fallback de localização: ${e.message}")
            }
    }

    private fun performReverseGeocoding(lat: Double, lon: Double) {
        // Verifica se a classe Geocoder está disponível no dispositivo
        if (!Geocoder.isPresent()) {
            Log.e("Geocoder", "Geocoder não está disponível neste dispositivo.")
            currentCEP = null
            return
        }

        // Tenta obter o CEP
        try {
            val geocoder = Geocoder(this, Locale("pt", "BR"))
            // Máximo de 1 resultado é suficiente
            val addresses = geocoder.getFromLocation(lat, lon, 1)

            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]

                // O método getPostalCode() retorna o CEP
                val postalCode = address.postalCode

                if (postalCode != null) {
                    currentCEP = postalCode
                    Log.i("Geocoder", "CEP encontrado: $currentCEP")
                    Log.i("Geocoder", "Endereço completo: ${address.getAddressLine(0)}")

                    // A PARTIR DAQUI, VOCÊ PODE INICIAR A CONSULTA AO SEU BANCO DE DADOS
                    // Exemplo:
                    // checkDatabaseForCEP(currentCEP!!, currentLatitude!!, currentLongitude!!)

                } else {
                    currentCEP = null
                    Log.w("Geocoder", "CEP não encontrado para estas coordenadas.")
                }
            } else {
                currentCEP = null
                Log.w("Geocoder", "Nenhum endereço encontrado para as coordenadas.")
            }
        } catch (e: Exception) {
            currentCEP = null
            Log.e("Geocoder", "Erro no Geocoding: ${e.message}")
        }
    }
}

data class Place(
    val name: String,
    val latLng: LatLng,
    val address: String,
    val rating: Float
)