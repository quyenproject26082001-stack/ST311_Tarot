package com.tarot.card.meaning.core.service
import com.tarot.card.meaning.data.model.PartAPI
import retrofit2.Response
import retrofit2.http.GET
interface ApiService {
    @GET("api/app/ST300_TherianOCMaker")
    suspend fun getAllData(): Response<Map<String, List<PartAPI>>>
}