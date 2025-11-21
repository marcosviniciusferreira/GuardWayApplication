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


// <--- MUDANÇA 1: Implementar OnMapReadyCallback
class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
        private const val DEFAULT_ZOOM = 15f // <--- Adicionado para centralizar a câmera
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var googleMap: GoogleMap? = null

    private var places = mutableListOf(
        Place("", LatLng(0.0, 0.0), "", 0.0f)
    )

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

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment
        // <--- MUDANÇA 3: Iniciar o carregamento do mapa, associando esta activity como callback
        mapFragment.getMapAsync(this)
    }

    // <--- MUDANÇA 4: Método do OnMapReadyCallback. O mapa só está pronto para uso aqui.
    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap // Armazena a referência do mapa
        addMarkers() // Chama para adicionar todos os lugares conhecidos (Parque Ibirapuera)
        // O getLastKnownLocation() se encarregará de adicionar e atualizar os marcadores quando a permissão for concedida.
    }

    // Função para adicionar marcadores
    private fun addMarkers() {
        val map = this.googleMap ?: return // Verifica se o mapa está pronto
        map.clear() // Limpa os marcadores existentes antes de adicionar os novos

        var userLatLng: LatLng? = null // Variável para armazenar a localização do usuário, se existir

        places.forEach { place ->
            val marker = map.addMarker( // Usa a variável 'map'
                MarkerOptions()
                    .title(place.name)
                    .snippet(place.address)
                    .position(place.latLng)
                // TODO: Certifique-se de que BitmapHelper e R.drawable.ic_android_black_24dp estão definidos corretamente.
                // O BitmapHelper não está definido neste código, mas vamos supor que exista.
                // Caso contrário, use o padrão: .icon(BitmapDescriptorFactory.defaultMarker())
                // .icon(BitmapHelper.vectorToBitmap(this, R.drawable.ic_android_black_24dp, ContextCompat.getColor(this, R.color.black)))
            )
            // Se for o marcador do usuário, guarda a posição para centralizar a câmera
            if (place.name == "Sua Localização") {
                userLatLng = place.latLng
            }
        }

        // <--- MUDANÇA 8: Centraliza a câmera no último marcador adicionado ou em uma posição padrão
        if (userLatLng != null) {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng!!, DEFAULT_ZOOM))
        } else if (places.isNotEmpty()) {
            // Se não encontrou o usuário, centraliza no primeiro item da lista, por exemplo
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(places.first().latLng, DEFAULT_ZOOM))
        }
    }

    private fun requestLocationPermission() {
        // 1. Verifica se a permissão de localização (precisa) já foi concedida.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {

            // 2. A permissão NÃO está concedida, então o app a solicita ao usuário.
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSION_REQUEST_CODE
            )
        } else {
            // 3. A permissão JÁ está concedida, então o app pode prosseguir.
            getLastKnownLocation()
        }
    }

    // <--- MUDANÇA 9: Tratamento do resultado da solicitação de permissão
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permissão concedida.
                getLastKnownLocation()
            } else {
                // Permissão negada. Lidar com o caso (ex: mostrar uma mensagem)
                Log.d("Location", "Permissão de localização negada pelo usuário.")
            }
        }
    }


    private fun getLastKnownLocation() {
        // Bloco de verificação de permissão de segurança (necessário antes de qualquer chamada de localização).
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Se a permissão não foi concedida, retorna (embora isso não deva acontecer se requestLocationPermission for chamada corretamente).
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    val userLatLng = LatLng(location.latitude, location.longitude)

                    // Encontra e remove um possível marcador de localização do usuário antigo
                    places.removeAll { it.name == "Sua Localização" } // <--- MUDANÇA 10: Remove a localização anterior antes de adicionar a nova

                    val userPlace = Place(
                        name = "Sua Localização",
                        latLng = userLatLng,
                        address = "Você está aqui!",
                        rating = 5.0f
                    )

                    // 4. Adiciona o novo lugar (a localização do usuário) à lista mutável 'places'.
                    places.add(userPlace)

                    Log.d("Location", "Localização do usuário adicionada: $userLatLng")

                    // <--- MUDANÇA 11: CHAMA addMarkers() para ATUALIZAR o mapa.
                    // Isso garante que o marcador só seja adicionado DEPOIS que a localização for conhecida.
                    addMarkers()

                } else {
                    Log.d("Location", "A última localização conhecida é nula.")
                }
            }
    }
}

data class Place(
    val name: String,
    val latLng: LatLng,
    val address: String,
    val rating: Float
)