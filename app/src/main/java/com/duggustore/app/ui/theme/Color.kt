package com.duggustore.app.ui.theme

import androidx.compose.ui.graphics.Color

// ── Brand ────────────────────────────────────────────────────────────────
// Teal leads (logo, category tiles, primary actions), orange is the shopping
// accent (cart, add-to-cart), coral marks discounts and destructive steps.
val Teal = Color(0xFF2BB3AC)
val TealDark = Color(0xFF1E8F89)
val TealLight = Color(0xFF6ACFC9)
val TealSurface = Color(0xFFE6F6F5)

val Orange = Color(0xFFF5A623)
val OrangeDark = Color(0xFFE08E0B)
val OrangeSurface = Color(0xFFFFF4E0)

val Coral = Color(0xFFEF6C6C)
val CoralDark = Color(0xFFD94F4F)
val CoralSurface = Color(0xFFFDECEC)

// ── Neutrals ─────────────────────────────────────────────────────────────
// The header and the bottom bar are white, so the page is too — the app reads
// as one surface instead of white bands top and bottom around a grey middle.
// Cards separate themselves with their shadow rather than with a tint.
val Background = Color(0xFFFFFFFF)
val SurfaceWhite = Color(0xFFFFFFFF)
val SurfaceMuted = Color(0xFFF1F3F6)
val TextPrimary = Color(0xFF16202C)
val TextSecondary = Color(0xFF6B7785)
val TextLight = Color(0xFFA3ACB9)
val BorderGray = Color(0xFFE8EBEF)

// ── Status ───────────────────────────────────────────────────────────────
val SuccessGreen = Color(0xFF22C55E)
val WarningYellow = Color(0xFFF59E0B)
val InfoBlue = Color(0xFF3B82F6)
val PendingYellow = Color(0xFFFBBF24)
val DeliveredGreen = Color(0xFF22C55E)
val StarYellow = Color(0xFFFFB020)

// ── Category tiles ───────────────────────────────────────────────────────
// Cycled in order for whatever categories the database returns, and reused
// for the offer cards so the two rails read as one family.
val CategoryColors = listOf(
    Teal,
    Color(0xFF4A90D9),
    Color(0xFFC96A9B),
    Color(0xFF8B6FD1),
    Color(0xFFE8894A),
    Color(0xFF3FAE7A),
    Color(0xFFD9646E),
    Color(0xFF5BB8D4)
)

// ── Compatibility aliases ────────────────────────────────────────────────
// The screens written before the redesign refer to these names. Keeping them
// pointed at the new palette re-skins those screens without touching each file.
val PrimaryGreen = Teal
val PrimaryGreenLight = TealLight
val PrimaryGreenDark = TealDark
val AccentOrange = Orange
val AccentRed = Coral

val CategoryGrocery = Teal
val CategoryVeggies = Color(0xFF3FAE7A)
val CategoryFruits = Color(0xFFE8894A)
val CategorySnacks = Color(0xFFEAB308)
val CategoryChocolate = Color(0xFF92400E)
val CategoryBread = Color(0xFFD97706)
val CategoryShampoo = Color(0xFFC96A9B)
val CategoryCleaning = Color(0xFF5BB8D4)
val CategoryBabyCare = Color(0xFFF472B6)
val CategoryColdDrinks = Color(0xFF4A90D9)
val CategoryMeat = Color(0xFFD9646E)
val CategoryDairy = Color(0xFF8B6FD1)
val CategoryFrozen = Color(0xFF0EA5E9)
