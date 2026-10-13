package com.chuparustom.app
import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private val perm = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { StepService.startIfAllowed(this) }
    private fun askPerms() {
        val l = mutableListOf(Manifest.permission.ACTIVITY_RECOGNITION)
        if (Build.VERSION.SDK_INT >= 33) l += Manifest.permission.POST_NOTIFICATIONS
        perm.launch(l.toTypedArray())
    }
    override fun onStart() { super.onStart(); StepService.startIfAllowed(this) }
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val s = Store(this)
        setContent { ChupaTheme { Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { App(s) { askPerms() } } } }
    }
}

fun lockEnabled(c: Context): Boolean =
    (Settings.Secure.getString(c.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: "").contains(c.packageName + "/" + LockService::class.java.name)

@Composable fun App(s: Store, askPerms: () -> Unit) {
    var screen by rememberSaveable { mutableStateOf(if (s.onboarded) "dash" else "welcome") }
    val tick = rememberTick(s)
    val back = { screen = if (s.onboarded) "dash" else "pick" }
    when (screen) {
        "welcome" -> Welcome(s) { screen = "pick" }
        "pick" -> PickScreen(s, tick, { screen = it }) { s.onboarded = true; screen = "dash" }
        "smoke" -> SmokeCfg(s, askPerms, back)
        "social" -> SocialCfg(s, askPerms, back)
        else -> Dash(s, tick, { screen = "pick" }, { screen = it })
    }
}

@Composable fun Page(content: @Composable ColumnScope.() -> Unit) =
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)

@Composable fun Welcome(s: Store, next: () -> Unit) = Page {
    Spacer(Modifier.height(40.dp))
    Text("\uD83D\uDC4B Welcome to ChupaRustom", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
    Text("Managing your bad habits - one step at a time.")
    var n by remember { mutableStateOf(s.name) }
    OutlinedTextField(n, { n = it }, label = { Text("What should we call you?") }, modifier = Modifier.fillMaxWidth())
    Button({ s.name = n.trim(); next() }, Modifier.fillMaxWidth()) { Text("Let's go") }
}

val habits = listOf("\uD83D\uDEAC Smoking", "\uD83D\uDCF1 Social Networking", "\uD83D\uDE48 Masturbating", "\uD83D\uDD1E Watching Porn",
    "\uD83C\uDF19 Sleeping Late", "\u23F0 Waking up Late", "\uD83E\uDD5F Eating Singara")

@Composable fun PickScreen(s: Store, tick: Int, go: (String) -> Unit, done: () -> Unit) = Page {
    val ctx = LocalContext.current; tick
    Text("Which habits do you want to manage?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    habits.forEachIndexed { i, h ->
        val on = (i == 0 && s.smokeOn) || (i == 1 && s.fbOn)
        Card(Modifier.fillMaxWidth().clickable {
            when (i) { 0 -> go("smoke"); 1 -> go("social"); else -> Toast.makeText(ctx, "Coming soon", Toast.LENGTH_SHORT).show() }
        }) { Row(Modifier.padding(18.dp), Arrangement.SpaceBetween) {
            Text(h, style = MaterialTheme.typography.titleMedium)
            Text(if (on) "\u2705 Active" else if (i > 1) "Coming soon" else "")
        } }
    }
    if (s.smokeOn || s.fbOn) Button(done, Modifier.fillMaxWidth()) { Text("Go to dashboard") }
}

@Composable fun Slide(label: String, v: Int, lo: Int, hi: Int, step: Int, on: (Int) -> Unit) {
    Text(label, fontWeight = FontWeight.SemiBold)
    Slider(v.toFloat(), { on((it / step).toInt() * step) }, valueRange = lo.toFloat()..hi.toFloat(), steps = (hi - lo) / step - 1)
}

@Composable fun SmokeCfg(s: Store, askPerms: () -> Unit, back: () -> Unit) = Page {
    var n by remember { mutableIntStateOf(s.smokeSteps) }
    Text("How do you want to manage your smoking count?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Slide("I can smoke 1 cigarette after every $n steps", n, 500, 10000, 500) { n = it }
    Button({
        s.smokeSteps = n
        if (!s.smokeOn) { s.smokeOn = true; s.smokeMark = s.totalSteps; s.smokeNotified = false }
        askPerms(); back()
    }, Modifier.fillMaxWidth()) { Text("Save") }
    if (s.smokeOn) OutlinedButton({ s.smokeOn = false; back() }, Modifier.fillMaxWidth()) { Text("Stop managing smoking") }
    TextButton(back) { Text("Cancel") }
}

@Composable fun SocialCfg(s: Store, askPerms: () -> Unit, back: () -> Unit) = Page {
    val ctx = LocalContext.current
    var n by remember { mutableIntStateOf(s.fbSteps) }
    var m by remember { mutableIntStateOf(s.fbMin) }
    Text("How do you want to manage your social networking?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Slide("Walk $n steps...", n, 500, 10000, 500) { n = it }
    Slide("...to use Facebook for $m minutes", m, 5, 60, 5) { m = it }
    Text("To lock Facebook, turn on the \"ChupaRustom Facebook Lock\" accessibility service. On Android 13+, you may first need: Settings > Apps > ChupaRustom > \u22EE > Allow restricted settings.")
    OutlinedButton({ ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }, Modifier.fillMaxWidth()) { Text("Open accessibility settings") }
    Button({
        s.fbSteps = n; s.fbMin = m
        if (!s.fbOn) { s.fbOn = true; s.fbMark = s.totalSteps; s.fbEnd = 0; s.fbNotified = false }
        askPerms(); back()
    }, Modifier.fillMaxWidth()) { Text("Save") }
    if (s.fbOn) OutlinedButton({ s.fbOn = false; back() }, Modifier.fillMaxWidth()) { Text("Stop managing social networking") }
    TextButton(back) { Text("Cancel") }
}

@Composable fun Dash(s: Store, tick: Int, add: () -> Unit, edit: (String) -> Unit) = Page {
    val ctx = LocalContext.current; tick
    val now by produceState(System.currentTimeMillis()) { while (true) { delay(1000); value = System.currentTimeMillis() } }
    Text("Hi ${s.name.ifEmpty { "there" }} \uD83D\uDC4B", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp)) {
        Text("\uD83D\uDC63 Steps today: ${s.stepsToday()}", style = MaterialTheme.typography.titleMedium)
        Text("Total steps counted: ${s.totalSteps}")
    } }
    if (s.smokeOn) Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val need = s.smokeSteps; val done = s.smokeProgress
        Text("\uD83D\uDEAC Smoking", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        LinearProgressIndicator({ (done.toFloat() / need).coerceIn(0f, 1f) }, Modifier.fillMaxWidth())
        Text(if (done >= need) "\u2705 You're eligible for 1 cigarette" else "\uD83D\uDEB6 ${need - done} more steps to earn the next one")
        Text("Progress: $done / $need steps")
        Text("Today: ${s.smokedToday()}  |  Total: ${s.smokeTotal}  |  Smoked early: ${s.smokeEarly}")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button({ s.logSmoke() }) { Text("I smoked one") }
            TextButton({ edit("smoke") }) { Text("Edit") }
        }
    } }
    if (s.fbOn) Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val need = s.fbSteps; val done = s.fbProgress; val left = s.fbEnd - now
        Text("\uD83D\uDCF1 Social Networking (Facebook)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (left > 0) Text("\u23F3 Session running: ${left / 60000}m ${left / 1000 % 60}s left")
        else {
            LinearProgressIndicator({ (done.toFloat() / need).coerceIn(0f, 1f) }, Modifier.fillMaxWidth())
            Text(if (done >= need) "\uD83D\uDD13 Unlocked - open Facebook for ${s.fbMin} min" else "\uD83D\uDD12 Locked - ${need - done} more steps")
            Text("Progress: $done / $need steps")
        }
        Text("Sessions: ${s.fbSessions}  |  Time used: ${s.fbUsedMin} min")
        TextButton({ edit("social") }) { Text("Edit") }
    } }
    if (s.fbOn && !lockEnabled(ctx)) Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp)) {
        Text("\u26A0\uFE0F Facebook lock is OFF. Enable the accessibility service so Facebook can be locked.")
        TextButton({ ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }) { Text("Open accessibility settings") }
    } }
    OutlinedButton(add, Modifier.fillMaxWidth()) { Text("Add / manage habits") }
}
