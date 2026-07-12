package com.acmecorp.acmeapp.feature.catalog

import com.acmecorp.acmeapp.core.network.createHttpClient
import com.acmecorp.acmeapp.feature.catalog.data.remote.CatalogRemoteDataSourceImpl
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CatalogRemoteDataSourceImplTest {
    @Test
    fun parsesProductsFromJson() = runTest {
        val json = """
            {"products":[
              {"id":1,"title":"Phone","price":9.99,"description":"d"},
              {"id":2,"title":"Tablet","price":5.0}
            ]}
        """.trimIndent()
        val engine = MockEngine {
            respond(json, headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val client = createHttpClient(engine, "https://example.com/")

        val products = CatalogRemoteDataSourceImpl(client).fetchProducts()

        assertEquals(2, products.size)
        assertEquals("Phone", products[0].title)
        assertEquals("", products[1].description) // default applied
    }
}
