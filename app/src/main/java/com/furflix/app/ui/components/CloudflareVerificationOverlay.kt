package com.furflix.app.ui.components

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.furflix.app.R
import com.furflix.app.data.remote.FurAffinityScraper
import kotlinx.coroutines.delay

/**
 * Handles Cloudflare challenges via WebView using the exact same User-Agent as OkHttp.
 * Starts silently (1dp) for a few seconds to let automatic JS challenges finish;
 * if interactive verification (e.g. Turnstile checkbox) is needed, opens a visible dialog.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CloudflareVerificationOverlay(
    onVerified: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var showVisibleDialog by remember { mutableStateOf(false) }
    var isResolved by remember { mutableStateOf(false) }
    val onVerifiedRef = rememberUpdatedState(onVerified)

    LaunchedEffect(Unit) {
        delay(4500)
        if (!isResolved) {
            showVisibleDialog = true
        }
    }

    val webViewContent: @Composable (Modifier) -> Unit = { mod ->
        AndroidView(
            modifier = mod,
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        userAgentString = FurAffinityScraper.USER_AGENT
                    }
                    val cm = CookieManager.getInstance()
                    cm.setAcceptCookie(true)
                    cm.setAcceptThirdPartyCookies(this, true)

                    fun pollCheck(view: WebView?, attempt: Int = 0) {
                        if (isResolved || view == null || attempt > 60) return
                        view.evaluateJavascript(
                            """
                            (function() {
                                var title = (document.title || '').toLowerCase();
                                var body = (document.body ? document.body.innerText : '').toLowerCase();
                                if (title.indexOf('just a moment') >= 0 ||
                                    title.indexOf('checking') >= 0 ||
                                    title.indexOf('attention required') >= 0 ||
                                    body.indexOf('checking your browser') >= 0 ||
                                    document.querySelector('#challenge-running, #cf-challenge-running, .cf-turnstile')) {
                                    return 'CHALLENGE';
                                }
                                if (document.querySelector('figure[id^="sid-"], a[href*="/browse/"], #my-username, .loggedin_user_avatar')) {
                                    return 'OK';
                                }
                                return 'WAIT';
                            })();
                            """.trimIndent()
                        ) { res ->
                            val state = res?.removeSurrounding("\"")?.trim() ?: ""
                            Log.d("FurCF", "CF poll #$attempt: $state")
                            if (state == "OK" && !isResolved) {
                                isResolved = true
                                cm.flush()
                                val c1 = cm.getCookie("https://furaffinity.net") ?: ""
                                val c2 = cm.getCookie("https://www.furaffinity.net") ?: ""
                                val merged = listOf(c1, c2).filter { it.isNotEmpty() }.joinToString("; ")
                                onVerifiedRef.value(merged)
                            } else if (!isResolved) {
                                Handler(Looper.getMainLooper()).postDelayed({
                                    pollCheck(view, attempt + 1)
                                }, 800)
                            }
                        }
                    }

                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = false

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            pollCheck(view)
                        }
                    }

                    loadUrl("${FurAffinityScraper.FA_BASE}/browse/")
                }
            }
        )
    }

    if (showVisibleDialog) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight(0.75f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.cf_dialog_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.action_cancel),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                HorizontalDivider()
                webViewContent(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }
        }
    } else {
        Box(modifier = Modifier.size(1.dp)) {
            webViewContent(Modifier.size(1.dp))
        }
    }
}
