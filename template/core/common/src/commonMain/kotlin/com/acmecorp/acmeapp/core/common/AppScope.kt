package com.acmecorp.acmeapp.core.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

/** Application-lifetime [CoroutineScope] for hot flows that outlive any single screen. */
class AppScope(dispatchers: DispatcherProvider) : CoroutineScope {
    override val coroutineContext = SupervisorJob() + dispatchers.default
}
