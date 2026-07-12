package com.acmecorp.acmeapp.feature.catalog.data.repository

import arrow.core.Either
import com.acmecorp.acmeapp.core.common.error.AppError
import com.acmecorp.acmeapp.core.common.error.catching
import com.acmecorp.acmeapp.feature.catalog.data.local.CatalogLocalDataSource
import com.acmecorp.acmeapp.feature.catalog.data.mapper.toDomain
import com.acmecorp.acmeapp.feature.catalog.data.remote.CatalogRemoteDataSource
import com.acmecorp.acmeapp.feature.catalog.domain.CatalogError
import com.acmecorp.acmeapp.feature.catalog.domain.model.Product
import com.acmecorp.acmeapp.feature.catalog.domain.repository.CatalogRepository
import kotlinx.coroutines.flow.Flow

class CatalogRepositoryImpl(
    private val remote: CatalogRemoteDataSource,
    private val local: CatalogLocalDataSource,
) : CatalogRepository {
    // UI observes the local cache; refresh fetches remote and upserts behind the catching() boundary.
    override fun observeProducts(): Flow<List<Product>> = local.observeProducts()

    override suspend fun refresh(): Either<AppError, Unit> = catching({ CatalogError(it) }) {
        val products = remote.fetchProducts().map { it.toDomain() }
        local.replaceAll(products)
    }
}
