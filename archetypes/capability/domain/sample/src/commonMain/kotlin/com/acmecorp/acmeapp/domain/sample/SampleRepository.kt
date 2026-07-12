package com.acmecorp.acmeapp.domain.sample

import arrow.core.Either
import com.acmecorp.acmeapp.core.common.error.AppError
import com.acmecorp.acmeapp.domain.sample.model.Item
import kotlinx.coroutines.flow.Flow

interface SampleRepository {
    fun observeItems(): Flow<List<Item>>

    suspend fun refresh(): Either<AppError, Unit>
}
