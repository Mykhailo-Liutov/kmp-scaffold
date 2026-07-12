package com.acmecorp.acmeapp.domain.sample.usecase

import com.acmecorp.acmeapp.domain.sample.SampleRepository
import com.acmecorp.acmeapp.domain.sample.model.Item
import kotlinx.coroutines.flow.Flow

class ObserveItemsUseCase(private val repository: SampleRepository) {
    operator fun invoke(): Flow<List<Item>> = repository.observeItems()
}
