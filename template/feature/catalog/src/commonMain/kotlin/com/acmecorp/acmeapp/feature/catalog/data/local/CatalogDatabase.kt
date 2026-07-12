package com.acmecorp.acmeapp.feature.catalog.data.local

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.acmecorp.acmeapp.feature.catalog.data.local.dao.ProductDao
import com.acmecorp.acmeapp.feature.catalog.data.local.entity.ProductEntity

@Database(entities = [ProductEntity::class], version = 1)
@ConstructedBy(CatalogDatabaseConstructor::class)
abstract class CatalogDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
}

// Room generates the platform `actual` objects via KSP.
@Suppress("NO_ACTUAL_FOR_EXPECT", "KotlinNoActualForExpect")
expect object CatalogDatabaseConstructor : RoomDatabaseConstructor<CatalogDatabase> {
    override fun initialize(): CatalogDatabase
}

internal const val CATALOG_DB_FILE = "catalog.db"
