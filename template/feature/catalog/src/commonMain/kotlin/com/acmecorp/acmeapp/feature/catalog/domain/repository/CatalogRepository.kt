package com.acmecorp.acmeapp.feature.catalog.domain.repository

import arrow.core.Either
import com.acmecorp.acmeapp.core.common.error.AppError
import com.acmecorp.acmeapp.feature.catalog.domain.model.Product
import kotlinx.coroutines.flow.Flow

interface CatalogRepository {
    fun observeProducts(): Flow<List<Product>>

    suspend fun refresh(): Either<AppError, Unit>
}
