package com.acmecorp.acmeapp.feature.catalog.data.remote

import com.acmecorp.acmeapp.feature.catalog.data.remote.dto.ProductDto

interface CatalogRemoteDataSource {
    suspend fun fetchProducts(): List<ProductDto>
}
