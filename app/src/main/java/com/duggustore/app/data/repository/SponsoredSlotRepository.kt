package com.duggustore.app.data.repository

import com.duggustore.app.data.model.SponsoredSlot
import com.duggustore.app.data.remote.SessionManager
import com.duggustore.app.data.remote.SupabaseService
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Seller-paid placements on the home rail. Same live-window-in-RLS trick as
 * campaigns: a plain select only ever returns slots that are APPROVED and
 * currently running, so [getLiveSlots] needs no date filtering of its own.
 * Pricing and collecting payment both happen outside the app for now —
 * approving a request here is the only gate that puts it live.
 */
class SponsoredSlotRepository {
    private val json = Json { ignoreUnknownKeys = true }

    private fun token(): String? = SessionManager.getAccessToken()

    suspend fun getLiveSlots(): Result<List<SponsoredSlot>> {
        return try {
            val rows = SupabaseService.select("sponsored_slots", token())
            Result.success(rows.map { json.decodeFromJsonElement(SponsoredSlot.serializer(), it) })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** A seller's own requests, whatever their status — RLS lets them see only their own rows here. */
    suspend fun getMySlots(sellerId: String): Result<List<SponsoredSlot>> {
        return try {
            val rows = SupabaseService.select("sponsored_slots", token(), mapOf("seller_id" to sellerId))
            Result.success(rows.map { json.decodeFromJsonElement(SponsoredSlot.serializer(), it) })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** starts_at/ends_at are left unset here — they're only stamped once an admin approves the request, via review_sponsored_slot. */
    suspend fun requestSlot(sellerId: String, headline: String, message: String, durationDays: Int): Result<Unit> {
        return try {
            val body = buildJsonObject {
                put("seller_id", sellerId)
                put("headline", headline)
                put("message", message)
                put("duration_days", durationDays)
                put("status", "PENDING")
            }.toString()
            SupabaseService.insert("sponsored_slots", body, token())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Admin-only in practice — RLS only lets an admin session see every seller's requests. */
    suspend fun getAllSlots(): Result<List<SponsoredSlot>> {
        return try {
            val rows = SupabaseService.selectAll("sponsored_slots", token())
            Result.success(rows.map { json.decodeFromJsonElement(SponsoredSlot.serializer(), it) })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun reviewSlot(slotId: String, approve: Boolean, rejectionReason: String = ""): Result<Unit> {
        return try {
            val body = buildJsonObject {
                put("p_slot_id", slotId)
                put("p_approve", approve)
                put("p_rejection_reason", rejectionReason)
            }.toString()
            SupabaseService.rpc("review_sponsored_slot", body, token())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
