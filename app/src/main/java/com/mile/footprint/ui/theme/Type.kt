package com.mile.footprint.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.mile.footprint.R

// Set of Material typography styles to start with
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )






    /* Other default text styles to override
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
    */
)

 val robotomonoFontFamily = FontFamily(
     Font(R.font.robotomono_light, FontWeight.Light),
     Font(R.font.robotomono_thin,FontWeight.Thin),
     Font(R.font.robotomono_italic,FontWeight.Black),
     Font(R.font.robotomono_regular,FontWeight.Normal),
     Font(R.font.robotomono_bold,FontWeight.Bold),
     Font(R.font.robotomono_semibold,FontWeight.SemiBold),
     Font(R.font.robotomono_medium,FontWeight.Medium)
 )



