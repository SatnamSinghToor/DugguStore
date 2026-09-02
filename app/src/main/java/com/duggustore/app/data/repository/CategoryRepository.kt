package com.duggustore.app.data.repository

import com.duggustore.app.data.model.Category
import com.duggustore.app.data.remote.SupabaseClient.client
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.eq

class CategoryRepository {

    suspend fun getAllCategories(): Result<List<Category>> {
        return try {
            val categories = client.from("categories")
                .select()
                .decodeList<Category>()
            Result.success(categories)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createCategory(category: Category): Result<Unit> {
        return try {
            client.from("categories").insert(category)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateCategory(category: Category): Result<Unit> {
        return try {
            client.from("categories")
                .update(category) { eq("id", category.id) }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteCategory(id: String): Result<Unit> {
        return try {
            client.from("categories")
                .delete { eq("id", id) }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
