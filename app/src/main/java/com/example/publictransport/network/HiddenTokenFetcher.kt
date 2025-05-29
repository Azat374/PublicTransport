package com.example.publictransport.network
// HiddenTokenFetcher.kt

import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.publictransport.network.TokenProvider

@Composable
fun HiddenTokenFetcher() {
    // Чтобы выполнить только один раз
    var loaded by remember { mutableStateOf(false) }
    if (!loaded) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    // Интерфейс для обратного вызова из JS
                    addJavascriptInterface(object {
                        @JavascriptInterface
                        fun sendToken(token: String) {
                            // Обновляем TokenProvider
                            TokenProvider.updateToken(token)
                        }
                    }, "AndroidBridge")

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView, url: String) {
                            // Когда страница загрузилась, читаем токен
                            view.evaluateJavascript(
                                // JS: вызов нашего интерфейса
                                "AndroidBridge.sendToken(window.CDU_REST_API.token);",
                                null
                            )
                        }
                    }
                    loadUrl("https://citybus.tha.kz/")
                }
            },
            modifier = Modifier  // без размера — невидимый
        )
        loaded = true
    }
}
