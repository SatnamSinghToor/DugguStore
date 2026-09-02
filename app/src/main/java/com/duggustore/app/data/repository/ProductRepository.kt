package com.duggustore.app.data.repository

import com.duggustore.app.data.model.Product
import com.duggustore.app.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.eq

class ProductRepository {

    suspend fun getAllProducts(): Result<List<Product>> {
        return try {
            val products = SupabaseClient.client.from("products")
                .select()
                .decodeList<Product>()
            Result.success(products)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getProductsByCategory(categoryId: String): Result<List<Product>> {
        return try {
            val products = SupabaseClient.client.from("products")
                .select { eq("category_id", categoryId) }
                .decodeList<Product>()
            Result.success(products)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getProductsBySeller(sellerId: String): Result<List<Product>> {
        return try {
            val products = SupabaseClient.client.from("products")
                .select { eq("seller_id", sellerId) }
                .decodeList<Product>()
            Result.success(products)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getProduct(id: String): Result<Product?> {
        return try {
            val product = SupabaseClient.client.from("products")
                .select { eq("id", id) }
                .decodeSingle<Product>()
            Result.success(product)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createProduct(product: Product): Result<Unit> {
        return try {
            SupabaseClient.client.from("products").insert(product)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProduct(product: Product): Result<Unit> {
        return try {
            SupabaseClient.client.from("products")
                .update(product) { eq("id", product.id) }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteProduct(id: String): Result<Unit> {
        return try {
            SupabaseClient.client.from("products")
                .delete { eq("id", id) }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchProducts(query: String): Result<List<Product>> {
        return try {
            val products = SupabaseClient.client.from("products")
                .select()
                .decodeList<Product>()
            Result.success(products.filter {
                it.name.contains(query, ignoreCase = true) ||
                it.description.contains(query, ignoreCase = true)
            })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
