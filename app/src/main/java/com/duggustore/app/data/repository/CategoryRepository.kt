package com.duggustore.app.data.repository

import com.duggustore.app.data.model.Category
import com.duggustore.app.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.from

class CategoryRepository {

    suspend fun getAllCategories(): Result<List<Category>> {
        return try {
            val categories = SupabaseClient.client.from("categories")
                .select()
                .decodeList<Category>()
            Result.success(categories)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createCategory(category: Category): Result<Unit> {
        return try {
            SupabaseClient.client.from("categories").insert(category)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateCategory(category: Category): Result<Unit> {
        return try {
            SupabaseClient.client.from("categories")
                .update(category) { io.github.jan.supabase.postgrest.query.eq("id", category.id) }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteCategory(id: String): Result<Unit> {
        return try {
            SupabaseClient.client.from("categories")
                .delete { io.github.jan.supabase.postgrest.query.eq("id", id) }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
