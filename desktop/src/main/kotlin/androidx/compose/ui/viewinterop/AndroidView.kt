package androidx.compose.ui.viewinterop

import android.content.Context
import android.view.View
import android.webkit.WebView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.awt.Desktop
import java.net.URI

@Composable
fun <T : View> AndroidView(
    factory: (Context) -> T,
    modifier: Modifier = Modifier,
    update: (T) -> Unit = {},
) {
    val context = LocalContext.current
    val view = remember { factory(context) }
    SideEffect {
        update(view)
    }

    if (view is WebView) {
        DesktopWebViewHost(webView = view, modifier = modifier)
    } else {
        Box(modifier = modifier)
    }
}

@Composable
private fun DesktopWebViewHost(
    webView: WebView,
    modifier: Modifier = Modifier,
) {
    val currentUrl = webView.currentUrlState
    var cookieOrTokenInput by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Card(modifier = Modifier.fillMaxWidth(0.85f)) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "macOS Browser Authentication",
                    style = MaterialTheme.typography.titleMedium,
                )
                if (!currentUrl.isNullOrBlank()) {
                    Text(
                        text = "Target URL: $currentUrl",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = "1. Click 'Open in macOS Browser' to sign in, or use the key icon in the top bar.\n" +
                        "2. Copy your session cookie (e.g. sp_dc for Spotify, or SID/SAPISID for YouTube) or token.\n" +
                        "Tip: In DevTools > Application > Cookies, click the Value cell and press Cmd+A then Cmd+C (double-clicking stops at hyphens '-').",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            val target = currentUrl ?: return@Button
                            runCatching {
                                if (Desktop.isDesktopSupported()) {
                                    Desktop.getDesktop().browse(URI(target))
                                }
                            }.onFailure {
                                statusMessage = "Could not launch browser: ${it.message}"
                            }
                        },
                        enabled = !currentUrl.isNullOrBlank(),
                    ) {
                        Text("Open in macOS Browser")
                    }
                    OutlinedButton(
                        onClick = { webView.reload() },
                        enabled = !currentUrl.isNullOrBlank(),
                    ) {
                        Text("Retry Check")
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = cookieOrTokenInput,
                    onValueChange = { cookieOrTokenInput = it },
                    label = { Text("Paste cookie (sp_dc=... or raw value) or auth token") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Button(
                    onClick = {
                        webView.submitCookiesOrToken(cookieOrTokenInput, currentUrl)
                        statusMessage = "Applied credentials to session handler."
                    },
                    enabled = cookieOrTokenInput.isNotBlank(),
                ) {
                    Text("Apply Cookie / Token")
                }
                statusMessage?.let { msg ->
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}
