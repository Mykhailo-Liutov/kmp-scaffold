package com.acmecorp.acmeapp.feature.catalog.domain.usecase

import arrow.core.Either
import com.acmecorp.acmeapp.core.common.error.AppError
import com.acmecorp.acmeapp.feature.catalog.domain.repository.CatalogRepository

class RefreshProductsUseCase(private val repository: CatalogRepository) {
    suspend operator fun invoke(): Either<AppError, Unit> = repository.refresh()
}
