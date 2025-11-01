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
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.location.LocationServices


class MainActivity : AppCompatActivity() {

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
    }

    // Declare the variable, but initialize it later
    private lateinit var fusedLocationClient: FusedLocationProviderClient


    private var places = mutableListOf(
        //Place("Google", LatLng(-23.5868031, -46.684306), "Av. Brg. Faria Lima, 3477 - 18º Andar - Itaim Bibi, São Paulo - SP", 4.8f),
        Place("Parque Ibirapuera", LatLng(-23.5899619, -46.66747), "Av. República do Líbano, 1111 - Ibirapuera, São Paulo - SP", 4.9f)
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

        //Carregar o mapa:
        mapFragment.getMapAsync { googleMap ->
            addMarkers(googleMap)
        }


    }

    //Função para adicionar marcadores
    private fun addMarkers(googleMap: GoogleMap) {
        places.forEach { place ->
            googleMap.addMarker(
                MarkerOptions()
                    .title(place.name)
                    .snippet(place.address)
                    .position(place.latLng)
                    .icon(
                        BitmapHelper.vectorToBitmap(
                            this,
                            R.drawable.ic_android_black_24dp,
                            ContextCompat.getColor(this, R.color.black)
                        )
                    )
            )
        }
    }

    private fun requestLocationPermission() {
        // 1. Check if permission is already granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {

            // 2. Permission is NOT granted, request it
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSION_REQUEST_CODE // PERMISSION_REQUEST_CODE must be an Int constant defined elsewhere
            )
        } else {
            // 3. Permission is already granted, proceed to get location
            getLastKnownLocation()
        }
    }

    private fun getLastKnownLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Se a permissão não foi concedida, não podemos continuar.
            // A função requestLocationPermission() já deve ter sido chamada antes.
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location -> // 'it' se refere ao 'location'
                if (location != null) {
                    // Localização encontrada!

                    // 2. Correção: Acesso direto às propriedades 'latitude' e 'longitude'
                    val userLatLng = LatLng(location.latitude, location.longitude)

                    // 3. Crie um novo objeto Place com as coordenadas do usuário
                    val userPlace = Place(
                        name = "Sua Localização",
                        latLng = userLatLng,
                        address = "Você está aqui!",
                        rating = 5.0f // Defina uma avaliação padrão
                    )

                    // 4. Adicione o novo lugar à lista mutável
                    places.add(userPlace)

                    // Opcional: Atualize os marcadores no mapa para incluir o novo local.
                    // Para isso, você precisará de uma referência ao objeto GoogleMap.
                    // (Veja a explicação no passo 3 abaixo)

                    Log.d("Location", "Localização do usuário adicionada: $userLatLng")

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