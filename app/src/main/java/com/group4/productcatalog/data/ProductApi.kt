package com.group4.productcatalog.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ProductApi {

    @GET("products")
    suspend fun getProducts(
        @Query("limit") limit: Int = PAGE_SIZE,
        @Query("skip") skip: Int = 0,
        @Query("sortBy") sortBy: String? = null,
        @Query("order") order: String? = null
    ): ProductsResponse

    @GET("products/search")
    suspend fun searchProducts(
        @Query("q") query: String,
        @Query("limit") limit: Int = PAGE_SIZE,
        @Query("skip") skip: Int = 0,
        @Query("sortBy") sortBy: String? = null,
        @Query("order") order: String? = null
    ): ProductsResponse

    @GET("products/category/{category}")
    suspend fun getByCategory(
        @Path("category") category: String,
        @Query("limit") limit: Int = PAGE_SIZE,
        @Query("skip") skip: Int = 0,
        @Query("sortBy") sortBy: String? = null,
        @Query("order") order: String? = null
    ): ProductsResponse

    @GET("products/category-list")
    suspend fun getCategories(): List<String>

    @GET("products/{id}")
    suspend fun getProduct(@Path("id") id: Int): Product

    companion object {
        const val PAGE_SIZE = 20
    }
}

object RetrofitClient {
    val api: ProductApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://dummyjson.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ProductApi::class.java)
    }
}