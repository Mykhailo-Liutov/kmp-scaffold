package com.acmecorp.acmeapp.feature.catalog.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.acmecorp.acmeapp.feature.catalog.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY title")
    fun observeAll(): Flow<List<ProductEntity>>

    @Upsert
    suspend fun upsertAll(items: List<ProductEntity>)

    @Query("DELETE FROM products")
    suspend fun clear()

    // Atomic: rows absent from the fresh remote result must not survive a refresh.
    @Transaction
    suspend fun replaceAll(items: List<ProductEntity>) {
        clear()
        upsertAll(items)
    }
}
