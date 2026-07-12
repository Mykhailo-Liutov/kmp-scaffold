package com.acmecorp.acmeapp.core.database

import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlin.coroutines.CoroutineContext

fun <T : RoomDatabase> RoomDatabase.Builder<T>.buildDefault(queryContext: CoroutineContext): T =
    setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(queryContext)
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()
