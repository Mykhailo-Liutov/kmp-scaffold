package com.acmecorp.acmeapp.feature.catalog.data.local

import androidx.room.Room
import androidx.room.RoomDatabase
import com.acmecorp.acmeapp.core.common.DispatcherProvider
import com.acmecorp.acmeapp.core.database.buildDefault
import platform.Foundation.NSHomeDirectory

fun catalogDatabase(dispatchers: DispatcherProvider): CatalogDatabase {
    val dbFilePath = NSHomeDirectory() + "/" + CATALOG_DB_FILE
    val builder: RoomDatabase.Builder<CatalogDatabase> =
        Room.databaseBuilder<CatalogDatabase>(name = dbFilePath)
    return builder.buildDefault(dispatchers.io)
}
