package com.example.guardwayapplication

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView

class CentralAjudaActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_central_ajuda)

        // 1. Configuração da Toolbar e botão Voltar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        val btnBackToolbar = findViewById<ImageView>(R.id.btn_back_toolbar)
        btnBackToolbar.setOnClickListener {
            finish() // Retorna à Activity anterior
        }

        // 2. Configuração dos botões de Contato
        val btnFalarEmail = findViewById<CardView>(R.id.btn_falar_email)
        val btnFalarWhatsapp = findViewById<CardView>(R.id.btn_falar_whatsapp)

        btnFalarEmail.setOnClickListener {
            abrirEmail()
        }

        btnFalarWhatsapp.setOnClickListener {
            abrirWhatsapp()
        }
    }

    private fun abrirEmail() {
        // Define o destinatário e o assunto padrão
        val destinatario = "suporte@guardway.com.br"
        val assunto = "Dúvida/Suporte - App GuardWay"

        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:") // Somente aplicativos de e-mail devem lidar com isso
            putExtra(Intent.EXTRA_EMAIL, arrayOf(destinatario))
            putExtra(Intent.EXTRA_SUBJECT, assunto)
        }

        // Verifica se há um aplicativo de e-mail para lidar com a Intent
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            Toast.makeText(this, "Nenhum aplicativo de e-mail encontrado.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun abrirWhatsapp() {
        val numeroSuporte = "+5511986079604"
        val mensagemPadrao = "Olá, preciso de ajuda com o aplicativo GuardWay."

        try {
            val url = "https://api.whatsapp.com/send?phone=$numeroSuporte&text=${Uri.encode(mensagemPadrao)}"
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Erro ao abrir WhatsApp. Verifique se o app está instalado.", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }
}