package com.acmecorp.acmeapp.feature.catalog.ui.list

import com.acmecorp.acmeapp.feature.catalog.domain.model.Product

// Plain screen state — loading and error are ordinary fields, no Result/UiState wrapper.
data class CatalogUiState(
    val products: List<Product> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)
