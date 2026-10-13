package com.chuparustom.app
import android.content.Intent
import android.os.Bundle
import androidx.activity.*
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

class LockActivity : ComponentActivity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val s = Store(this)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) { override fun handleOnBackPressed() = home() })
        setContent { ChupaTheme { Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            val t = rememberTick(s); t
            val need = s.fbSteps; val done = s.fbProgress; val ok = done >= need
            Column(Modifier.fillMaxSize().padding(28.dp), Arrangement.Center, Alignment.CenterHorizontally) {
                Text(if (ok) "\uD83D\uDD13 You earned it!" else "\uD83D\uDD12 Facebook is locked", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(16.dp))
                LinearProgressIndicator({ (done.toFloat() / need).coerceIn(0f, 1f) }, Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Text(if (ok) "$done / $need steps done" else "$done / $need steps - ${need - done} to go. Go for a walk!", textAlign = TextAlign.Center)
                Spacer(Modifier.height(24.dp))
                if (ok) Button(onClick = {
                    s.startFbSession()
                    packageManager.getLaunchIntentForPackage(s.fbPkg.ifEmpty { "com.facebook.katana" })?.let { startActivity(it) }
                    finish()
                }) { Text("Start ${s.fbMin}-minute session") }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { home() }) { Text("Not now") }
            } } } }
    }
    private fun home() {
        startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); finish()
    }
}
