package com.acmecorp.acmeapp.domain.sample.usecase

import arrow.core.Either
import com.acmecorp.acmeapp.core.common.error.AppError
import com.acmecorp.acmeapp.domain.sample.SampleRepository

class RefreshItemsUseCase(private val repository: SampleRepository) {
    suspend operator fun invoke(): Either<AppError, Unit> = repository.refresh()
}
