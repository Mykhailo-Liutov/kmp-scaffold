package com.acmecorp.acmeapp.feature.catalog

import app.cash.turbine.test
import com.acmecorp.acmeapp.feature.catalog.domain.model.Product
import com.acmecorp.acmeapp.feature.catalog.domain.usecase.GetProductsUseCase
import com.acmecorp.acmeapp.feature.catalog.domain.usecase.RefreshProductsUseCase
import com.acmecorp.acmeapp.feature.catalog.ui.list.CatalogListViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class CatalogListViewModelTest {
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun emitsCachedProductsAndFinishesRefresh() = runTest {
        val repo = FakeCatalogRepository(products = listOf(Product(1, "A", 1.0, "d")))
        val viewModel = CatalogListViewModel(GetProductsUseCase(repo), RefreshProductsUseCase(repo))

        viewModel.state.test {
            val state = awaitItem()
            assertEquals(1, state.products.size)
            assertEquals(false, state.isLoading)
            assertNull(state.errorMessage)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(1, repo.refreshCount)
    }

    @Test
    fun surfacesRefreshErrorInState() = runTest {
        val repo = FakeCatalogRepository().apply { refreshError = TestAppError("boom") }
        val viewModel = CatalogListViewModel(GetProductsUseCase(repo), RefreshProductsUseCase(repo))

        viewModel.state.test {
            assertEquals("boom", awaitItem().errorMessage)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
