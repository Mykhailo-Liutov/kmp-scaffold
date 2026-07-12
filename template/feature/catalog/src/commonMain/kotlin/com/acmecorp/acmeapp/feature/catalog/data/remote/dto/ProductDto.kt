package com.acmecorp.acmeapp.feature.catalog.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ProductListResponse(
    val products: List<ProductDto>,
)

@Serializable
data class ProductDto(
    val id: Int,
    val title: String,
    val price: Double,
    val description: String = "",
)
