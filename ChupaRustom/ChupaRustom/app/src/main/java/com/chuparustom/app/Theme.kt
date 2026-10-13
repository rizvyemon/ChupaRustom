package com.chuparustom.app
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import android.content.SharedPreferences

@Composable fun ChupaTheme(content: @Composable () -> Unit) = MaterialTheme(
    colorScheme = darkColorScheme(primary = Color(0xFFFF7A3D), onPrimary = Color.Black, secondary = Color(0xFFB36BFF),
        background = Color(0xFF14101F), surface = Color(0xFF211A33), onBackground = Color.White, onSurface = Color.White),
    content = content)

@Composable fun rememberTick(s: Store): Int {
    var t by remember { mutableIntStateOf(0) }
    DisposableEffect(Unit) {
        val l = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> t++ }
        s.sp.registerOnSharedPreferenceChangeListener(l)
        onDispose { s.sp.unregisterOnSharedPreferenceChangeListener(l) }
    }
    return t
}
