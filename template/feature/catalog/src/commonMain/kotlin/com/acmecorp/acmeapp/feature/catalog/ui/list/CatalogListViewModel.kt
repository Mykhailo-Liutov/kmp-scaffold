package com.acmecorp.acmeapp.feature.catalog.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acmecorp.acmeapp.feature.catalog.domain.usecase.GetProductsUseCase
import com.acmecorp.acmeapp.feature.catalog.domain.usecase.RefreshProductsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CatalogListViewModel(
    getProducts: GetProductsUseCase,
    private val refreshProducts: RefreshProductsUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(CatalogUiState(isLoading = true))
    val state: StateFlow<CatalogUiState> = _state.asStateFlow()

    init {
        getProducts()
            .onEach { products -> _state.update { it.copy(products = products) } }
            .launchIn(viewModelScope)
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            refreshProducts().fold(
                { error -> _state.update { it.copy(isLoading = false, errorMessage = error.message) } },
                { _state.update { it.copy(isLoading = false) } },
            )
        }
    }
}
