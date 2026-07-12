package com.acmecorp.acmeapp.data.sample

import arrow.core.Either
import arrow.core.right
import com.acmecorp.acmeapp.core.common.error.AppError
import com.acmecorp.acmeapp.domain.sample.SampleRepository
import com.acmecorp.acmeapp.domain.sample.model.Item
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Skeleton stub: in-memory data so the capability is exercisable. Back it with a real source. */
class InMemorySampleRepository : SampleRepository {
    private val items = MutableStateFlow<List<Item>>(emptyList())

    override fun observeItems(): Flow<List<Item>> = items.asStateFlow()

    override suspend fun refresh(): Either<AppError, Unit> = Unit.right()
}
