package com.acmecorp.acmeapp.feature.catalog

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.acmecorp.acmeapp.core.common.error.AppError
import com.acmecorp.acmeapp.feature.catalog.data.local.CatalogLocalDataSource
import com.acmecorp.acmeapp.feature.catalog.data.remote.CatalogRemoteDataSource
import com.acmecorp.acmeapp.feature.catalog.data.remote.dto.ProductDto
import com.acmecorp.acmeapp.feature.catalog.domain.model.Product
import com.acmecorp.acmeapp.feature.catalog.domain.repository.CatalogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class TestAppError(message: String) : AppError(message)

class FakeCatalogRemoteDataSource(
    var products: List<ProductDto> = emptyList(),
    var error: Throwable? = null,
) : CatalogRemoteDataSource {
    override suspend fun fetchProducts(): List<ProductDto> {
        error?.let { throw it }
        return products
    }
}

class FakeCatalogLocalDataSource(initial: List<Product> = emptyList()) : CatalogLocalDataSource {
    private val flow = MutableStateFlow(initial)
    val stored: List<Product> get() = flow.value
    override fun observeProducts(): Flow<List<Product>> = flow.asStateFlow()
    override suspend fun replaceAll(products: List<Product>) { flow.value = products }
}

class FakeCatalogRepository(products: List<Product> = emptyList()) : CatalogRepository {
    private val flow = MutableStateFlow(products)
    var refreshError: AppError? = null
    var refreshCount = 0
    override fun observeProducts(): Flow<List<Product>> = flow.asStateFlow()
    override suspend fun refresh(): Either<AppError, Unit> {
        refreshCount++
        return refreshError?.left() ?: Unit.right()
    }
}
