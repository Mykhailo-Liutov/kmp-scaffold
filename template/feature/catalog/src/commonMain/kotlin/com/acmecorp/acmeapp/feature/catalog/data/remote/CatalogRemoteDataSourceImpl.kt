package com.acmecorp.acmeapp.feature.catalog.data.remote

import com.acmecorp.acmeapp.feature.catalog.data.remote.dto.ProductDto
import com.acmecorp.acmeapp.feature.catalog.data.remote.dto.ProductListResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

class CatalogRemoteDataSourceImpl(
    private val client: HttpClient,
) : CatalogRemoteDataSource {
    override suspend fun fetchProducts(): List<ProductDto> =
        client.get("products").body<ProductListResponse>().products
}
