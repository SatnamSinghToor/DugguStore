package com.duggustore.app.data.repository

import com.duggustore.app.data.model.StoreNotification
import com.duggustore.app.data.remote.SessionManager
import com.duggustore.app.data.remote.SupabaseService
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.JsonPrimitive

private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

/** Raw DB row from the `notifications` table. */
@Serializable
data class DbNotification(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    val type: String = "ORDER",
    val title: String = "",
    val message: String = "",
    @SerialName("is_read") val isRead: Boolean = false,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("order_id") val orderId: String? = null
)

/**
 * Reads and manages the `notifications` table, which is populated server-side
 * by the `trg_order_status_notify` trigger whenever an order's status changes.
 *
 * The existing [StoreNotification] / [NotificationsScreen] contract is preserved:
 * DB rows are mapped into the same shape so no UI changes are required.
 */
class NotificationsRepository {

    private fun token(): String? = SessionManager.getAccessToken()

    /** Returns all notifications for [userId], newest first. */
    suspend fun getNotifications(userId: String): Result<List<DbNotification>> {
        return try {
            // PostgREST ordering: append order query param manually because
            // SupabaseService.select() only supports eq-filters.
            val rows = SupabaseService.select(
                table = "notifications",
                token = token(),
                params = mapOf("user_id" to userId),
                select = "*&order=created_at.desc"
            )
            Result.success(rows.map {
                json.decodeFromString(DbNotification.serializer(), it.toString())
            })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Marks a single notification row as read. */
    suspend fun markRead(notificationId: String): Result<Unit> {
        return try {
            val body = buildJsonObject { put("is_read", JsonPrimitive(true)) }.toString()
            SupabaseService.update("notifications", notificationId, body, token())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Marks every unread notification for [userId] as read in a single PATCH.
     * Uses the existing [SupabaseService.updateWhere] which filters by a single
     * column — we filter by user_id (the caller has already loaded only their own).
     */
    suspend fun markAllRead(userId: String): Result<Unit> {
        return try {
            val body = buildJsonObject { put("is_read", JsonPrimitive(true)) }.toString()
            SupabaseService.updateWhere("notifications", "user_id", userId, body, token())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ---- mapping to the shared StoreNotification model ----------------

    fun DbNotification.toStoreNotification(): StoreNotification {
        val kind = when {
            title.contains("confirmed", ignoreCase = true) -> StoreNotification.Kind.Confirmed
            title.contains("prepared", ignoreCase = true) ||
                title.contains("preparing", ignoreCase = true) -> StoreNotification.Kind.Preparing
            title.contains("pickup", ignoreCase = true) -> StoreNotification.Kind.ReadyForPickup
            title.contains("delivery", ignoreCase = true) ||
                title.contains("way", ignoreCase = true) -> StoreNotification.Kind.OutForDelivery
            title.contains("delivered", ignoreCase = true) -> StoreNotification.Kind.Delivered
            title.contains("cancelled", ignoreCase = true) -> StoreNotification.Kind.Cancelled
            else -> StoreNotification.Kind.Placed
        }
        return StoreNotification(
            id = id,
            title = title,
            body = message,
            timestamp = createdAt,
            orderId = orderId.orEmpty(),
            kind = kind
        )
    }
}
