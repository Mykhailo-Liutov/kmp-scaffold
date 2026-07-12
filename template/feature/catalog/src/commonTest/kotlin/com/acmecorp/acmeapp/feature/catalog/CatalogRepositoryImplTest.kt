package com.acmecorp.acmeapp.feature.catalog

import com.acmecorp.acmeapp.feature.catalog.data.remote.dto.ProductDto
import com.acmecorp.acmeapp.feature.catalog.data.repository.CatalogRepositoryImpl
import com.acmecorp.acmeapp.feature.catalog.domain.model.Product
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CatalogRepositoryImplTest {
    @Test
    fun refreshFetchesRemoteAndStoresLocally() = runTest {
        val remote = FakeCatalogRemoteDataSource(products = listOf(ProductDto(1, "A", 1.0, "d")))
        val local = FakeCatalogLocalDataSource()
        val repo = CatalogRepositoryImpl(remote, local)

        repo.refresh()

        assertEquals(listOf(Product(1, "A", 1.0, "d")), local.stored)
    }

    @Test
    fun observeDelegatesToLocalCache() = runTest {
        val local = FakeCatalogLocalDataSource(listOf(Product(1, "A", 1.0, "d")))
        val repo = CatalogRepositoryImpl(FakeCatalogRemoteDataSource(), local)

        assertEquals(1, repo.observeProducts().first().size)
    }
}
