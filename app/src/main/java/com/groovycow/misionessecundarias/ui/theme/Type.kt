package com.groovycow.misionessecundarias.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import com.groovycow.misionessecundarias.R

/** Fuente pixelada usada en títulos y subtítulos de la aplicación. */
val MinecraftFontFamily = FontFamily(Font(R.font.minecraft))

/** Fuente arcade usada en textos, etiquetas, campos y botones. */
val PressStartFontFamily = FontFamily(Font(R.font.press_start_2p))

private val MaterialTypography = Typography()

/** Tipografía global con identidad visual de videojuego. */
val Typography =
    Typography(
        displayLarge = MaterialTypography.displayLarge.copy(fontFamily = MinecraftFontFamily),
        displayMedium = MaterialTypography.displayMedium.copy(fontFamily = MinecraftFontFamily),
        displaySmall = MaterialTypography.displaySmall.copy(fontFamily = MinecraftFontFamily),
        headlineLarge = MaterialTypography.headlineLarge.copy(fontFamily = MinecraftFontFamily),
        headlineMedium = MaterialTypography.headlineMedium.copy(fontFamily = MinecraftFontFamily),
        headlineSmall = MaterialTypography.headlineSmall.copy(fontFamily = MinecraftFontFamily),
        titleLarge = MaterialTypography.titleLarge.copy(fontFamily = MinecraftFontFamily),
        titleMedium = MaterialTypography.titleMedium.copy(fontFamily = MinecraftFontFamily),
        titleSmall = MaterialTypography.titleSmall.copy(fontFamily = MinecraftFontFamily),
        bodyLarge =
            MaterialTypography.bodyLarge.copy(
                fontFamily = PressStartFontFamily,
                fontSize = 14.sp,
                lineHeight = 22.sp,
                letterSpacing = 0.sp,
            ),
        bodyMedium =
            MaterialTypography.bodyMedium.copy(
                fontFamily = PressStartFontFamily,
                fontSize = 12.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.sp,
            ),
        bodySmall =
            MaterialTypography.bodySmall.copy(
                fontFamily = PressStartFontFamily,
                fontSize = 10.sp,
                lineHeight = 17.sp,
                letterSpacing = 0.sp,
            ),
        labelLarge =
            MaterialTypography.labelLarge.copy(
                fontFamily = PressStartFontFamily,
                fontSize = 11.sp,
                lineHeight = 18.sp,
                letterSpacing = 0.sp,
            ),
        labelMedium =
            MaterialTypography.labelMedium.copy(
                fontFamily = PressStartFontFamily,
                fontSize = 10.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.sp,
            ),
        labelSmall =
            MaterialTypography.labelSmall.copy(
                fontFamily = PressStartFontFamily,
                fontSize = 9.sp,
                lineHeight = 14.sp,
                letterSpacing = 0.sp,
            ),
    )
