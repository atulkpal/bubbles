package com.ashwathai.bubbles.ui.theme.luxury

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.ashwathai.bubbles.R

private val fontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

val CormorantFamily = FontFamily(
    Font(
        googleFont = GoogleFont("Cormorant Garamond"),
        fontProvider = fontProvider,
        weight = FontWeight.Bold
    ),
    Font(
        googleFont = GoogleFont("Cormorant Garamond"),
        fontProvider = fontProvider,
        weight = FontWeight.SemiBold
    ),
    Font(
        googleFont = GoogleFont("Cormorant Garamond"),
        fontProvider = fontProvider,
        weight = FontWeight.Medium
    )
)

val MontserratFamily = FontFamily(
    Font(
        googleFont = GoogleFont("Montserrat"),
        fontProvider = fontProvider,
        weight = FontWeight.Bold
    ),
    Font(
        googleFont = GoogleFont("Montserrat"),
        fontProvider = fontProvider,
        weight = FontWeight.SemiBold
    ),
    Font(
        googleFont = GoogleFont("Montserrat"),
        fontProvider = fontProvider,
        weight = FontWeight.Medium
    ),
    Font(
        googleFont = GoogleFont("Montserrat"),
        fontProvider = fontProvider,
        weight = FontWeight.Normal
    )
)

object LuxuryTypography {
    val DisplayLarge = TextStyle(
        fontFamily = CormorantFamily,
        fontWeight = FontWeight.Bold,
        fontSize = LuxuryText.DisplayLarge.sp,
        lineHeight = 62.sp,
        letterSpacing = (-0.5).sp
    )
    val DisplayMedium = TextStyle(
        fontFamily = CormorantFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = LuxuryText.DisplayMedium.sp,
        lineHeight = 48.sp,
        letterSpacing = (-0.25).sp
    )
    val DisplaySmall = TextStyle(
        fontFamily = CormorantFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = LuxuryText.DisplaySmall.sp,
        lineHeight = 40.sp
    )
    val HeadlineLarge = TextStyle(
        fontFamily = MontserratFamily,
        fontWeight = FontWeight.Bold,
        fontSize = LuxuryText.HeadlineLarge.sp,
        lineHeight = 32.sp
    )
    val HeadlineMedium = TextStyle(
        fontFamily = MontserratFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = LuxuryText.HeadlineMedium.sp,
        lineHeight = 28.sp
    )
    val HeadlineSmall = TextStyle(
        fontFamily = MontserratFamily,
        fontWeight = FontWeight.Medium,
        fontSize = LuxuryText.HeadlineSmall.sp,
        lineHeight = 24.sp
    )
    val BodyLarge = TextStyle(
        fontFamily = MontserratFamily,
        fontWeight = FontWeight.Normal,
        fontSize = LuxuryText.BodyLarge.sp,
        lineHeight = 24.sp
    )
    val BodyMedium = TextStyle(
        fontFamily = MontserratFamily,
        fontWeight = FontWeight.Normal,
        fontSize = LuxuryText.BodyMedium.sp,
        lineHeight = 20.sp
    )
    val BodySmall = TextStyle(
        fontFamily = MontserratFamily,
        fontWeight = FontWeight.Normal,
        fontSize = LuxuryText.BodySmall.sp,
        lineHeight = 16.sp
    )
    val LabelLarge = TextStyle(
        fontFamily = MontserratFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = LuxuryText.LabelLarge.sp,
        letterSpacing = 0.2.sp,
        lineHeight = 20.sp
    )
    val LabelMedium = TextStyle(
        fontFamily = MontserratFamily,
        fontWeight = FontWeight.Medium,
        fontSize = LuxuryText.LabelMedium.sp,
        letterSpacing = LuxuryText.UppercaseLabelSpacing.sp,
        lineHeight = 16.sp
    )
    val LabelSmall = TextStyle(
        fontFamily = MontserratFamily,
        fontWeight = FontWeight.Medium,
        fontSize = LuxuryText.LabelSmall.sp,
        letterSpacing = 0.8.sp,
        lineHeight = 14.sp
    )
}
