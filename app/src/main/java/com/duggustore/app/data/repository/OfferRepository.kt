package com.duggustore.app.data.repository

import com.duggustore.app.data.model.Coupon
import com.duggustore.app.data.remote.SessionManager
import com.duggustore.app.data.remote.SupabaseService
import kotlinx.serialization.json.Json
import java.util.Calendar

/**
 * The offers shown on the home carousel.
 *
 * These are the store's real coupons rather than pictures of offers, so the code
 * on the card is one the customer can actually type into the cart.
 */
class OfferRepository {

    private val json = Json { ignoreUnknownKeys = true }

    private fun token(): String? = SessionManager.getAccessToken()

    suspend fun getOffers(): Result<List<Coupon>> {
        return try {
            val rows = SupabaseService.select(
                "coupons",
                token(),
                mapOf("is_active" to "true")
            )
            val coupons = rows.map { json.decodeFromJsonElement(Coupon.serializer(), it) }
            Result.success(rotateForToday(coupons))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Same offers, different one in front each day.
     *
     * A fixed order means whoever opens the app sees the same card at the top
     * every morning and stops reading it. Rotating by day of year moves a
     * different offer to the front without needing anything scheduled server
     * side, and keeps the order stable for the whole of that day.
     */
    private fun rotateForToday(coupons: List<Coupon>): List<Coupon> {
        if (coupons.size < 2) return coupons
        // Sorted first so the rotation starts from the same place on every
        // device, whatever order PostgREST happened to return.
        val stable = coupons.sortedBy { it.code }
        val shift = Calendar.getInstance().get(Calendar.DAY_OF_YEAR) % stable.size
        return stable.drop(shift) + stable.take(shift)
    }
}
