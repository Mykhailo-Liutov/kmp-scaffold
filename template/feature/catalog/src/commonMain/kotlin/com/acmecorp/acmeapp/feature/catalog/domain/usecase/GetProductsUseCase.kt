package com.acmecorp.acmeapp.feature.catalog.domain.usecase

import com.acmecorp.acmeapp.feature.catalog.domain.model.Product
import com.acmecorp.acmeapp.feature.catalog.domain.repository.CatalogRepository
import kotlinx.coroutines.flow.Flow

class GetProductsUseCase(private val repository: CatalogRepository) {
    operator fun invoke(): Flow<List<Product>> = repository.observeProducts()
}
