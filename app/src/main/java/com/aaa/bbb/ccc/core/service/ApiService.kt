package com.aaa.bbb.ccc.core.service
import com.aaa.bbb.ccc.data.model.PartAPI
import retrofit2.Response
import retrofit2.http.GET
interface ApiService {
    @GET("api/app/ST300_TherianOCMaker")
    suspend fun getAllData(): Response<Map<String, List<PartAPI>>>
}