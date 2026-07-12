package com.acmecorp.acmeapp.feature.catalog.data.local

import com.acmecorp.acmeapp.feature.catalog.domain.model.Product
import kotlinx.coroutines.flow.Flow

interface CatalogLocalDataSource {
    fun observeProducts(): Flow<List<Product>>
    suspend fun replaceAll(products: List<Product>)
}
