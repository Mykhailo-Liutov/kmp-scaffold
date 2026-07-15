package com.acmecorp.acmeapp.feature.catalog.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.acmecorp.acmeapp.core.common.DispatcherProvider
import com.acmecorp.acmeapp.core.database.buildDestructiveCache

fun catalogDatabase(context: Context, dispatchers: DispatcherProvider): CatalogDatabase {
    val dbFile = context.getDatabasePath(CATALOG_DB_FILE)
    val builder: RoomDatabase.Builder<CatalogDatabase> =
        Room.databaseBuilder(context.applicationContext, dbFile.absolutePath)
    // Remote-list cache, safe to drop; switch to buildDefault + migrations if this ever holds user data.
    return builder.buildDestructiveCache(dispatchers.io)
}
