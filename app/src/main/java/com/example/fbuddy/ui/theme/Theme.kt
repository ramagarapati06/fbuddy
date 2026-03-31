package com.example.fbuddy.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Teal        = Color(0xFF2E8B77)
val TealLight   = Color(0xFFE8F5F2)
val TealMid     = Color(0xFFA8D5CB)
val Amber       = Color(0xFFC47B2E)
val AmberLight  = Color(0xFFFBF3E8)
val Rose        = Color(0xFFC45858)
val RoseLight   = Color(0xFFFAF0F0)
val Green       = Color(0xFF3A8A5C)
val GreenLight  = Color(0xFFEDF7F2)
val BgSand      = Color(0xFFF5F2ED)
val White       = Color(0xFFFEFCF8)
val CardWhite   = Color(0xFFFFFFFF)
val Sand        = Color(0xFFEDE9E0)
val Sand2       = Color(0xFFE4DFD4)
val Ink         = Color(0xFF1C1A17)
val Ink2        = Color(0xFF4A4640)
val Ink3        = Color(0xFF8A857C)
val Ink4        = Color(0xFFB8B2A8)

private val FBuddyColorScheme = lightColorScheme(
    primary            = Teal,
    onPrimary          = Color.White,
    primaryContainer   = TealLight,
    onPrimaryContainer = Teal,
    secondary          = Amber,
    onSecondary        = Color.White,
    secondaryContainer = AmberLight,
    background         = BgSand,
    onBackground       = Ink,
    surface            = CardWhite,
    onSurface          = Ink,
    surfaceVariant     = Sand,
    onSurfaceVariant   = Ink2,
    error              = Rose,
    outline            = Sand2,
)

@Composable
fun FBuddyTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = FBuddyColorScheme, content = content)
}
