package com.acmecorp.acmeapp.core.database

import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlin.coroutines.CoroutineContext

/** Safe default: a schema change without a matching migration fails at open instead of losing data. */
fun <T : RoomDatabase> RoomDatabase.Builder<T>.buildDefault(queryContext: CoroutineContext): T =
    setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(queryContext)
        .build()

/** For disposable caches ONLY: a schema change DROPS ALL TABLES instead of requiring a migration. */
fun <T : RoomDatabase> RoomDatabase.Builder<T>.buildDestructiveCache(queryContext: CoroutineContext): T =
    setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(queryContext)
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()
