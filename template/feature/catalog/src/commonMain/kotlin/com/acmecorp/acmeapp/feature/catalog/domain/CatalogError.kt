package com.acmecorp.acmeapp.feature.catalog.domain

import com.acmecorp.acmeapp.core.common.error.AppError

/** Catalog failure carried by the `Left` of `Either<AppError, …>`. */
class CatalogError(cause: Throwable?) : AppError("Couldn't load the catalog. Check your connection.", cause)
