package com.acmecorp.acmeapp.feature.catalog.domain.model

data class Product(
    val id: Int,
    val title: String,
    val price: Double,
    val description: String,
)
