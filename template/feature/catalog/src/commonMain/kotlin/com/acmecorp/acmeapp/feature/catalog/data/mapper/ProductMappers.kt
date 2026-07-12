package com.acmecorp.acmeapp.feature.catalog.data.mapper

import com.acmecorp.acmeapp.feature.catalog.data.local.entity.ProductEntity
import com.acmecorp.acmeapp.feature.catalog.data.remote.dto.ProductDto
import com.acmecorp.acmeapp.feature.catalog.domain.model.Product

fun ProductDto.toDomain() = Product(id = id, title = title, price = price, description = description)

fun ProductEntity.toDomain() = Product(id = id, title = title, price = price, description = description)

fun Product.toEntity() = ProductEntity(id = id, title = title, price = price, description = description)
