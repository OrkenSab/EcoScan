package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Light Theme Colors (based on "белая тема.png" - exact background match)
val LightEcoGreen = Color(0xFF22C55E)        // Scan text / Leaf / Brackets
val LightEcoGreenDark = Color(0xFF15803D)
val LightBrandNavy = Color(0xFF1E252D)       // Eco text / Headline
val LightBackground = Color(0xFFFFFFFF)      // Exact match to white icon canvas
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFF4F6F5)
val LightTextPrimary = Color(0xFF1E252D)
val LightTextSecondary = Color(0xFF5C6874)
val LightOutline = Color(0xFFE2E8F0)

// Backward compatible aliases
val EcoGreenPrimary = LightEcoGreen
val EcoGreenDark = LightEcoGreenDark
val EcoGreenLight = Color(0xFF4ADE80)
val EcoBackground = LightBackground
val EcoSurface = LightSurface

// Dark Theme Colors (based on "темная тема.png" - exact background match #0B0D12)
val DarkEcoMint = Color(0xFF4ADE80)          // Scan text / Mint Leaf
val DarkBrandWhite = Color(0xFFFFFFFF)       // Eco text / Brackets
val DarkBackground = Color(0xFF0B0D12)       // Exact match to dark icon canvas (srgb 11, 13, 18)
val DarkSurface = Color(0xFF14171E)          // Elevated card surface
val DarkSurfaceVariant = Color(0xFF1D222B)   // Secondary card surface
val DarkTextPrimary = Color(0xFFFFFFFF)
val DarkTextSecondary = Color(0xFFA3B1BF)
val DarkOutline = Color(0xFF262C36)

// Severity Colors (adapted for readability in both modes)
val SeverityLow = Color(0xFF22C55E)          // 1-3
val SeverityMedium = Color(0xFFF59E0B)       // 4-6
val SeverityHigh = Color(0xFFEF4444)         // 7-8
val SeverityCritical = Color(0xFF991B1B)     // 9-10

// Category Impact Colors
val HarmNatureColor = Color(0xFF10B981)
val HarmHealthColor = Color(0xFFF97316)
val HarmEconomyColor = Color(0xFF3B82F6)
