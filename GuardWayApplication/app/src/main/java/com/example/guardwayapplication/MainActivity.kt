package com.example.guardwayapplication

import android.Manifest
import android.content.pm.PackageManager
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
        addMarkers()
    }

    private fun addMarkers() {
        val map = this.googleMap ?: return // Garante que o mapa não é nulo antes de prosseguir
        map.clear()

        var userLatLng: LatLng? = null

        places.forEach { place ->
            map.addMarker(
                MarkerOptions()
                    .title(place.name)
                    .snippet(place.address)
                    .position(place.latLng)
            )

            if (place.name == "Sua Localização") {
                userLatLng = place.latLng
                // Habilita o indicador de localização do Google Maps, se permitido
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED) {
                    map.isMyLocationEnabled = true
                }
            }
        }

        // Centraliza a câmera na localização do usuário
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

        // 3. Solicita a localização atual (Alta precisão, uma única vez)
        fusedLocationClient.getCurrentLocation(
            LOCATION_PRIORITY, // Usando a constante definida em Companion Object
            cancellationTokenSource.token
        )
            .addOnSuccessListener { location ->
                if (location != null) {
                    val userLatLng = LatLng(location.latitude, location.longitude)

                    // Remove o marcador antigo
                    places.removeAll { it.name == "Sua Localização" }

                    // Cria e adiciona o novo marcador
                    val userPlace = Place(
                        name = "Sua Localização",
                        latLng = userLatLng,
                        address = "Você está aqui!",
                        rating = 5.0f
                    )
                    places.add(userPlace)

                    Log.d("Location", "Localização atual OBTIDA com sucesso: $userLatLng")
                    addMarkers() // Atualiza o mapa com o novo local e centraliza

                } else {
                    Log.d("Location", "getCurrentLocation retornou nulo. Tentando fallback...")
                    // Tenta a última localização conhecida como fallback
                    getLastKnownLocationFallback()
                }
            }
            .addOnFailureListener { e ->
                Log.e("Location", "Erro ao obter localização atual: ${e.message}")
                getLastKnownLocationFallback() // Tenta fallback em caso de erro
            }
    }

    // 4. Fallback: Tenta a última localização (função antiga renomeada)
    private fun getLastKnownLocationFallback() {
        // Bloco de verificação de permissão (Obrigatório antes da chamada).
        // Não deveria acontecer aqui, mas é uma proteção.
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
                    places.removeAll { it.name == "Sua Localização" }
                    val userPlace = Place(
                        name = "Sua Localização",
                        latLng = userLatLng,
                        address = "Você está aqui!",
                        rating = 5.0f
                    )
                    places.add(userPlace)
                    Log.d("Location", "Localização de fallback OBTIDA: $userLatLng")
                    addMarkers()
                } else {
                    Log.d("Location", "Localização de fallback também é nula.")
                }
            }
            .addOnFailureListener { e ->
                Log.e("Location", "Erro no fallback de localização: ${e.message}")
            }
    }

}

data class Place(
    val name: String,
    val latLng: LatLng,
    val address: String,
    val rating: Float
)