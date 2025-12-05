package com.example.guardwayapplication

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

class SobreNosActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sobre_nos)

        // 1. Configuração da Toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Remover o título padrão do ActionBar (Se o título customizado já estiver no XML)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // 2. Listener para o botão de voltar customizado na Toolbar
        val btnBackToolbar = findViewById<ImageView>(R.id.btn_back_toolbar)
        btnBackToolbar.setOnClickListener {
            onBackPressed() // ou finish()
        }

    }
}