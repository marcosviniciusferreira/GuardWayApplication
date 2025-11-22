package com.example.guardwayapplication

import retrofit2.http.GET

interface ApiService {
    @GET("http://192.168.1.15/meu_projeto_api/get_produtos.php/")
    fun getProdutos(): Call<List<Produto>>

}
