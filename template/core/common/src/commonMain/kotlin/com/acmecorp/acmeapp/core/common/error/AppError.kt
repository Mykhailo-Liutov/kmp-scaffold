package com.acmecorp.acmeapp.core.common.error

/**
 * Base for domain errors carried by the `Left` of `Either<AppError, T>`. Not a [Throwable] —
 * errors are returned, not thrown, across module boundaries. Each capability subclasses this
 * in its own `domain` module (e.g. `BillingError`).
 */
abstract class AppError(
    val message: String? = null,
    val cause: Throwable? = null,
)
