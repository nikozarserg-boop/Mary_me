package org.example.animation.io

import io.ktor.client.HttpClient

import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import io.ktor.serialization.kotlinx.json.json

class BrushStoreService(

    private val baseUrl: String = "https://brushstore.example.com/api",
    private val httpClient: HttpClient = createHttpClient()
) {
    suspend fun getCategories(): List<BrushStoreCategory> = withContext(Dispatchers.IO) {
        httpClient.get("$baseUrl/categories").body()
    }
    
    suspend fun getBrushes(
        category: String? = null,
        query: String? = null,
        page: Int = 1,
        pageSize: Int = 20
    ): BrushStoreResponse = withContext(Dispatchers.IO) {
        httpClient.get("$baseUrl/brushes") {
            url {
                parameters.append("category", category ?: "")
                parameters.append("q", query ?: "")
                parameters.append("page", page.toString())
                parameters.append("pageSize", pageSize.toString())
            }
        }.body()
    }
    
    suspend fun downloadBrush(brushId: String): ByteArray = withContext(Dispatchers.IO) {
        httpClient.get("$baseUrl/brushes/$brushId/download").body()
    }
    
    suspend fun getPreviewImage(url: String): ByteArray = withContext(Dispatchers.IO) {
        httpClient.get(url).body()
    }
    
    fun close() {
        httpClient.close()
    }
    
    companion object {
        fun createHttpClient(): HttpClient {
            return HttpClient(createHttpClientEngine()) {
                install(ContentNegotiation) {
                    json(Json { ignoreUnknownKeys = true })
                }
            }
        }
    }
}