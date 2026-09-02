package com.duggustore.app.data.repository

import com.duggustore.app.data.model.Category
import com.duggustore.app.data.remote.SupabaseService
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

class CategoryRepository {

    suspend fun getAllCategories(): Result<List<Category>> {
        return try {
            val list = SupabaseService.selectAll("categories")
            Result.success(list.map { json.decodeFromString(Category.serializer(), it.toString()) })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createCategory(category: Category): Result<Unit> {
        return try {
            SupabaseService.insert("categories", json.encodeToString(Category.serializer(), category))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateCategory(category: Category): Result<Unit> {
        return try {
            SupabaseService.update("categories", category.id, json.encodeToString(Category.serializer(), category))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteCategory(id: String): Result<Unit> {
        return try {
            SupabaseService.delete("categories", id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
