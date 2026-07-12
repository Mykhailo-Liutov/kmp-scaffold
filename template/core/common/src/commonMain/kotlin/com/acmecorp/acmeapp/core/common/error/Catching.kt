package com.acmecorp.acmeapp.core.common.error

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import kotlin.coroutines.cancellation.CancellationException

/**
 * The single boundary primitive that makes data access total: the only place a throwing SDK call
 * (Ktor, Room, …) is allowed, converting it to `Either<AppError, T>`. Above this, nothing throws.
 *
 * Rethrows [CancellationException] and fatal [Error]s (OOM/StackOverflow must not become a domain error);
 * everything else maps via [mapError].
 */
@Suppress("TooGenericExceptionCaught")
suspend fun <T> catching(mapError: (Throwable) -> AppError, block: suspend () -> T): Either<AppError, T> =
    try {
        block().right()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        if (e is Error) throw e
        mapError(e).left()
    }
