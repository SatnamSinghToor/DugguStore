package com.duggustore.app.data.repository

import com.duggustore.app.data.model.Product
import com.duggustore.app.data.remote.SupabaseService
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

class ProductRepository {

    suspend fun getAllProducts(): Result<List<Product>> {
        return try {
            val list = SupabaseService.selectAll("products")
            Result.success(list.map { json.decodeFromString(Product.serializer(), it.toString()) })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getProductsByCategory(categoryId: String): Result<List<Product>> {
        return try {
            val list = SupabaseService.select("products", params = mapOf("category_id" to categoryId))
            Result.success(list.map { json.decodeFromString(Product.serializer(), it.toString()) })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getProductsBySeller(sellerId: String): Result<List<Product>> {
        return try {
            val list = SupabaseService.select("products", params = mapOf("seller_id" to sellerId))
            Result.success(list.map { json.decodeFromString(Product.serializer(), it.toString()) })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getProduct(id: String): Result<Product?> {
        return try {
            val list = SupabaseService.select("products", params = mapOf("id" to id))
            val product = list.firstOrNull()?.let { json.decodeFromString(Product.serializer(), it.toString()) }
            Result.success(product)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createProduct(product: Product): Result<Unit> {
        return try {
            SupabaseService.insert("products", json.encodeToString(Product.serializer(), product))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProduct(product: Product): Result<Unit> {
        return try {
            SupabaseService.update("products", product.id, json.encodeToString(Product.serializer(), product))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteProduct(id: String): Result<Unit> {
        return try {
            SupabaseService.delete("products", id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchProducts(query: String): Result<List<Product>> {
        return try {
            val all = SupabaseService.selectAll("products")
            val products = all.map { json.decodeFromString(Product.serializer(), it.toString()) }
            Result.success(products.filter {
                it.name.contains(query, ignoreCase = true) || it.description.contains(query, ignoreCase = true)
            })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
