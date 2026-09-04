package com.duggustore.app.data.repository

import com.duggustore.app.data.model.WalletTransaction
import com.duggustore.app.data.remote.SessionManager
import com.duggustore.app.data.remote.SupabaseService
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

/** The balance is never stored — it's always the sum of this ledger. */
fun List<WalletTransaction>.walletBalance(): Int =
    sumOf { if (it.type == "CREDIT") it.amount else -it.amount }

class WalletRepository {
    private fun token(): String? = SessionManager.getAccessToken()

    suspend fun getTransactions(userId: String): Result<List<WalletTransaction>> {
        return try {
            val list = SupabaseService.select("wallet_transactions", token(), mapOf("user_id" to userId))
            Result.success(
                list.map { json.decodeFromString(WalletTransaction.serializer(), it.toString()) }
                    .sortedByDescending { it.createdAt }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Spends the customer's own balance toward an order's total. RLS only
     * allows a user to insert a DEBIT row for themselves — every CREDIT is
     * granted server-side, so this can never manufacture free money.
     */
    suspend fun debit(userId: String, amount: Int, title: String): Result<Unit> {
        return try {
            val body = buildJsonObject {
                put("user_id", userId)
                put("title", title)
                put("amount", amount)
                put("type", "DEBIT")
            }.toString()
            SupabaseService.insert("wallet_transactions", body, token())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
