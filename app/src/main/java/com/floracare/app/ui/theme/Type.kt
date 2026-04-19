package com.floracare.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.floracare.app.R

private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

private val displaySerif = FontFamily(
    Font(GoogleFont("Fraunces"), provider, weight = FontWeight.Normal),
    Font(GoogleFont("Fraunces"), provider, weight = FontWeight.SemiBold),
    Font(GoogleFont("Fraunces"), provider, weight = FontWeight.Bold),
)

private val bodySans = FontFamily(
    Font(GoogleFont("Plus Jakarta Sans"), provider, weight = FontWeight.Normal),
    Font(GoogleFont("Plus Jakarta Sans"), provider, weight = FontWeight.Medium),
    Font(GoogleFont("Plus Jakarta Sans"), provider, weight = FontWeight.SemiBold),
)

val FloraCareTypography = Typography(
    displayLarge = TextStyle(fontFamily = displaySerif, fontWeight = FontWeight.SemiBold, fontSize = 48.sp, lineHeight = 54.sp, letterSpacing = (-0.5).sp),
    displayMedium = TextStyle(fontFamily = displaySerif, fontWeight = FontWeight.SemiBold, fontSize = 36.sp, lineHeight = 42.sp, letterSpacing = (-0.25).sp),
    displaySmall = TextStyle(fontFamily = displaySerif, fontWeight = FontWeight.Normal, fontSize = 28.sp, lineHeight = 34.sp),

    headlineLarge = TextStyle(fontFamily = displaySerif, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, lineHeight = 32.sp),
    headlineMedium = TextStyle(fontFamily = displaySerif, fontWeight = FontWeight.Normal, fontSize = 22.sp, lineHeight = 28.sp),
    headlineSmall = TextStyle(fontFamily = displaySerif, fontWeight = FontWeight.Normal, fontSize = 18.sp, lineHeight = 24.sp),

    titleLarge = TextStyle(fontFamily = bodySans, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp),
    titleMedium = TextStyle(fontFamily = bodySans, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 20.sp),
    titleSmall = TextStyle(fontFamily = bodySans, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp),

    bodyLarge = TextStyle(fontFamily = bodySans, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = bodySans, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = bodySans, fontSize = 12.sp, lineHeight = 16.sp),

    labelLarge = TextStyle(fontFamily = bodySans, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = 0.5.sp),
    labelMedium = TextStyle(fontFamily = bodySans, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.5.sp),
    labelSmall = TextStyle(fontFamily = bodySans, fontWeight = FontWeight.Medium, fontSize = 10.sp, lineHeight = 12.sp, letterSpacing = 0.5.sp),
)
