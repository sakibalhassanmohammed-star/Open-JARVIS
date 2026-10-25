package com.openjarvis.assistant

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.Executors

private val Navy = Color(0xFF06101F)
private val Panel = Color(0xFF102238)
private val Cyan = Color(0xFF00D9FF)
private val Muted = Color(0xFFB8C8D9)

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        setContent { Dashboard() }
    }
    override fun onInit(status: Int) { if (status == TextToSpeech.SUCCESS) tts?.language = Locale.getDefault() }
    override fun onDestroy() { tts?.shutdown(); super.onDestroy() }
    fun speak(text: String) { tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis") }
}

@Composable
private fun Dashboard() {
    val ctx = LocalContext.current
    val activity = ctx as? MainActivity
    val prefs = remember { ctx.getSharedPreferences("jarvis_prefs", 0) }
    var name by remember { mutableStateOf(prefs.getString("name", "JARVIS") ?: "JARVIS") }
    var persona by remember { mutableStateOf(prefs.getString("persona", "Classic") ?: "Classic") }
    var model by remember { mutableStateOf(prefs.getString("model", "gemini-2.0-flash") ?: "gemini-2.0-flash") }
    var apiKey by remember { mutableStateOf("") }
    var prompt by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("STANDBY") }
    var response by remember { mutableStateOf("Systems ready. Choose a feature to begin.") }
    var voice by remember { mutableStateOf(true) }
    var memory by remember { mutableStateOf(true) }
    var confirm by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }

    val mic = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        status = if (granted) "MICROPHONE GRANTED" else "MICROPHONE DENIED"
    }
    val notification = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    MaterialTheme(colorScheme = darkColorScheme(primary = Cyan, background = Navy, surface = Panel, onSurface = Color.White)) {
        Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Navy, Color(0xFF0A1B2E), Navy))).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("O P E N   J . A . R . V . I . S", color = Cyan, style = MaterialTheme.typography.labelMedium)
            Text("Mobile AI Control Center", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("LOCAL DASHBOARD • ANDROID", color = Muted, style = MaterialTheme.typography.labelSmall)
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("CORE STATUS     $status", color = Cyan, fontWeight = FontWeight.Bold)
                    Text(name, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold)
                    Text(response, color = Muted)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                try {
                                    ctx.startActivity(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM).putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to $name"))
                                    status = "VOICE INPUT OPENED"
                                } catch (_: Exception) { status = "VOICE RECOGNIZER UNAVAILABLE" }
                            } else mic.launch(Manifest.permission.RECORD_AUDIO)
                        }) { Icon(Icons.Default.Mic, null); Text(" Speak") }
                        OutlinedButton(onClick = { activity?.speak("$name online."); status = "SPEAKING" }) {
                            Icon(Icons.Default.RecordVoiceOver, null); Text(" Test voice")
                        }
                    }
                }
            }
            PanelCard("01 • ASSISTANT CONFIGURATION", "Identity and local preferences") {
                OutlinedTextField(name, { name = it }, label = { Text("Assistant name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(persona, { persona = it }, label = { Text("Persona") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(model, { model = it }, label = { Text("Gemini model ID") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                ToggleRow("Voice responses", voice) { voice = it }
                ToggleRow("Local preference memory", memory) { memory = it }
                ToggleRow("Confirm before actions", confirm) { confirm = it }
                Button(onClick = {
                    prefs.edit().putString("name", name).putString("persona", persona).putString("model", model)
                        .putBoolean("voice", voice).putBoolean("memory", memory).putBoolean("confirm", confirm).apply()
                    status = "CONFIGURATION SAVED"; response = "Preferences saved on this device."
                }, modifier = Modifier.fillMaxWidth()) { Text("Save configuration") }
            }
            PanelCard("02 • GEMINI CONNECTION", "Key is kept in memory only; never committed to GitHub") {
                Text("Enter a Google AI Studio key and message to test the Gemini REST API.", color = Muted, style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(apiKey, { apiKey = it.trim() }, label = { Text("Google AI Studio API key") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(prompt, { prompt = it }, label = { Text("Message to Gemini") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                Button(enabled = apiKey.isNotBlank() && prompt.isNotBlank() && !busy, onClick = {
                    busy = true; status = "CONNECTING"; response = "Sending request to Gemini…"
                    Executors.newSingleThreadExecutor().execute {
                        val result = gemini(apiKey, model, prompt)
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            busy = false
                            status = if (result.startsWith("ERROR:")) "CONNECTION FAILED" else "GEMINI CONNECTED"
                            response = result.removePrefix("ERROR: ")
                            if (voice) activity?.speak(response)
                        }
                    }
                }, modifier = Modifier.fillMaxWidth()) { Text(if (busy) "Connecting…" else "Test connection / Send") }
            }
            PanelCard("03 • ANDROID PERMISSIONS", "Opens Android settings; you choose what to enable") {
                PermissionRow("Microphone", "Speech recognition") { mic.launch(Manifest.permission.RECORD_AUDIO) }
                PermissionRow("Floating overlay", "Draw over other apps") {
                    ctx.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${ctx.packageName}")))
                }
                PermissionRow("Accessibility service", "Optional Android accessibility access") {
                    ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
                if (Build.VERSION.SDK_INT >= 33) PermissionRow("Notifications", "Android 13+") { notification.launch(Manifest.permission.POST_NOTIFICATIONS) }
            }
            PanelCard("04 • AUTOMATION & PRIVACY", "Feature status in this starter") {
                InfoLine("Voice input", "System recognizer; not always listening")
                InfoLine("Voice output", "Android TextToSpeech")
                InfoLine("Accessibility", "Declaration only; no automated taps")
                InfoLine("Floating HUD", "Permission shortcut only; service not implemented")
                InfoLine("Screen capture / vision", "Not implemented")
                InfoLine("Wake phrase / background listening", "Not implemented")
                InfoLine("Long-term chat history", "Not implemented")
                InfoLine("Root controls", "Not included")
                Text("Accessibility permissions are powerful. Enable them only if you understand and want that access. This starter does not secretly monitor or control other apps.", color = Muted, style = MaterialTheme.typography.bodySmall)
            }
            PanelCard("05 • DIAGNOSTICS", "Runtime information") {
                InfoLine("Android version", Build.VERSION.RELEASE ?: "Unknown")
                InfoLine("Package", ctx.packageName)
                InfoLine("API key storage", "Memory only")
                InfoLine("Build method", "GitHub Actions")
                OutlinedButton(onClick = { status = "READY"; response = "Local dashboard check complete." }, modifier = Modifier.fillMaxWidth()) { Text("Run local check") }
            }
            Text("OPEN JARVIS • STARTER BUILD • REVIEW PERMISSIONS", color = Muted, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable private fun PanelCard(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = {
            Text(title, color = Cyan, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, color = Muted, style = MaterialTheme.typography.bodySmall)
            HorizontalDivider(color = Color(0xFF294057))
            content()
        })
    }
}
@Composable private fun ToggleRow(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f)); Switch(checked = value, onCheckedChange = onChange)
    }
}
@Composable private fun PermissionRow(title: String, subtitle: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.SemiBold); Text(subtitle, color = Muted, style = MaterialTheme.typography.bodySmall) }
        OutlinedButton(onClick = onClick) { Text("Open") }
    }
}
@Composable private fun InfoLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(label, color = Muted, modifier = Modifier.weight(0.9f), style = MaterialTheme.typography.bodySmall)
        Text(value, modifier = Modifier.weight(1.1f), style = MaterialTheme.typography.bodySmall)
    }
}
private fun gemini(key: String, model: String, prompt: String): String {
    var conn: HttpURLConnection? = null
    return try {
        val safeModel = model.trim().ifBlank { "gemini-2.0-flash" }
        conn = (URL("https://generativelanguage.googleapis.com/v1beta/models/$safeModel:generateContent?key=$key").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"; connectTimeout = 15000; readTimeout = 30000; doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
        }
        val body = JSONObject().put("contents", org.json.JSONArray().put(JSONObject().put("parts", org.json.JSONArray().put(JSONObject().put("text", prompt))))).toString()
        conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val data = stream.bufferedReader().use { it.readText() }
        if (code !in 200..299) "ERROR: HTTP $code" else {
            JSONObject(data).optJSONArray("candidates")?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)?.optString("text") ?: "Gemini responded without text."
        }
    } catch (e: Exception) { "ERROR: ${e.message ?: "Network request failed"}" }
    finally { conn?.disconnect() }
}
