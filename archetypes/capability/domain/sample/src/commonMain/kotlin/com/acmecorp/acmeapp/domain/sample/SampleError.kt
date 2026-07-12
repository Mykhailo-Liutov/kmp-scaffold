package com.acmecorp.acmeapp.domain.sample

import com.acmecorp.acmeapp.core.common.error.AppError

/** Sample-capability failure carried by the `Left` of `Either<AppError, …>`. */
class SampleError(cause: Throwable?) : AppError("Sample operation failed.", cause)
