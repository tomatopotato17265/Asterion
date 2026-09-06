package dev.tomatopotato.asterion

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import asterion.shared.generated.resources.Res
import asterion.shared.generated.resources.inter_bold
import asterion.shared.generated.resources.inter_medium
import asterion.shared.generated.resources.inter_regular
import asterion.shared.generated.resources.inter_semibold
import org.jetbrains.compose.resources.Font

fun rememberInterFontFamily(): FontFamily = FontFamily(
    Font(Res.font.inter_regular, FontWeight.Normal),
    Font(Res.font.inter_medium, FontWeight.Medium),
    Font(Res.font.inter_semibold, FontWeight.SemiBold),
    Font(Res.font.inter_bold, FontWeight.Bold),
)

@Composable
fun AsterionTypography(): Typography {
    val inter = rememberInterFontFamily()
    val d = Typography()
    return Typography(
        displayLarge = d.displayLarge.copy(fontFamily = inter),
        displayMedium = d.displayMedium.copy(fontFamily = inter),
        displaySmall = d.displaySmall.copy(fontFamily = inter),
        headlineLarge = d.headlineLarge.copy(fontFamily = inter),
        headlineMedium = d.headlineMedium.copy(fontFamily = inter),
        headlineSmall = d.headlineSmall.copy(fontFamily = inter),
        titleLarge = d.titleLarge.copy(fontFamily = inter),
        titleMedium = d.titleMedium.copy(fontFamily = inter),
        titleSmall = d.titleSmall.copy(fontFamily = inter),
        bodyLarge = d.bodyLarge.copy(fontFamily = inter),
        bodyMedium = d.bodyMedium.copy(fontFamily = inter),
        bodySmall = d.bodySmall.copy(fontFamily = inter),
        labelLarge = d.labelLarge.copy(fontFamily = inter),
        labelMedium = d.labelMedium.copy(fontFamily = inter),
        labelSmall = d.labelSmall.copy(fontFamily = inter),
    )
}
