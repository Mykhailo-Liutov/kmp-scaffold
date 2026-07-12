package com.acmecorp.acmeapp.feature.catalog.data.local

import com.acmecorp.acmeapp.feature.catalog.data.local.dao.ProductDao
import com.acmecorp.acmeapp.feature.catalog.data.mapper.toDomain
import com.acmecorp.acmeapp.feature.catalog.data.mapper.toEntity
import com.acmecorp.acmeapp.feature.catalog.domain.model.Product
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CatalogLocalDataSourceImpl(
    private val dao: ProductDao,
) : CatalogLocalDataSource {
    override fun observeProducts(): Flow<List<Product>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun replaceAll(products: List<Product>) {
        dao.upsertAll(products.map { it.toEntity() })
    }
}
