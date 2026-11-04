package com.ourlist.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// ---------------------------------------------------------------- surfaces
/** Root background: obsidian ink black matching PayTrace. */
val Ink = Color(0xFF0F1115)

/** Base surface for cards, dialogs, and sheets. */
val Surface = Color(0xFF171A21)

/** Elevated surface for raised cards, headers, interactive items. */
val SurfaceRaised = Color(0xFF20242D)

/** Deepest surface, used by the floating bottom dock and segmented bar track. */
val SurfaceSunken = Color(0xFF08090C)

/** Selected pill highlight background. */
val SurfaceSelected = Color(0xFF28292E)

val SurfaceSnackbar = Color(0xFF293041)

// ---------------------------------------------------------------- gradients
val OverviewGradientStart = Color(0xFF381829) // Warm deep wine/plum
val OverviewGradientEnd = Color(0xFF171C29)   // Dark slate

val HeartGradientStart = Color(0xFF441324)    // Romantic deep ruby
val HeartGradientEnd = Color(0xFF1A1521)

// ---------------------------------------------------------------- accents (Couple / Brother App)
/** Romantic Rose Coral - primary accent, vibrant and crisp against dark ink. */
val Primary = Color(0xFFFF5376)

/** Soft rose label tint for content on top of dark gradients. */
val PrimarySoft = Color(0xFFFFB2C1)
val PrimaryContainer = Color(0xFF381822)

/** Signature Heart Red for the Heart tab and romantic bucket items. */
val HeartRed = Color(0xFFFF2E63)
val HeartRedSoft = Color(0xFFFF8FA3)

/** Success / Done / Memory achieved. */
val Income = Color(0xFF4CC38A)
val Success = Color(0xFF4CC38A)

/** Target date / Due soon / Attention. */
val Due = Color(0xFFFFB74D)

/** Someday / Review / Lavender accent. */
val Review = Color(0xFFB69CFF)

/** Danger / Delete / Conflict alert. */
val Danger = Color(0xFFFF4D6D)

// ---------------------------------------------------------------- controls
val SwitchTrackOff = Color(0xFF3A3F4B)
val SwitchKnob = Color(0xFFF7F8FB)

// ---------------------------------------------------------------- text & dividers
val TextPrimary = Color(0xFFF5F6FA)
val TextSecondary = Color(0xFFA9AFBD)
val TextTertiary = Color(0xFF7C8492)
val IconInactive = Color(0xFF8E95A5)
val Divider = Color(0xFF2A2F3A)

// Wish Type specific accent colors
val WishMustColor = Color(0xFFFF5376)     // 꼭 하고 싶다 (Rose Coral)
val WishSomedayColor = Color(0xFFB69CFF)  // 언젠간 하고 싶다 (Lavender)
val WishDateColor = Color(0xFFFFB74D)     // 구체적인 날에 하고 싶다 (Amber)

object OurListDimens {
    val MinTouchTarget = 48.dp
    val ScreenPadding = 16.dp
    val CardCorner = 18.dp
    val BottomDockHeight = 74.dp
}
