package com.hellosweety.app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Locale

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private lateinit var web: WebView
    private var speech: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private val micRequest = 44
    private val voiceRequest = 45

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        web = WebView(this)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.webChromeClient = WebChromeClient()
        web.webViewClient = WebViewClient()
        web.addJavascriptInterface(SweetyBridge(), "Sweety")
        setContentView(web)
        web.loadUrl("file:///android_asset/index.html")
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), micRequest)
        }
    }

    inner class SweetyBridge {
        @JavascriptInterface fun startListening() { runOnUiThread { beginSpeech() } }
        @JavascriptInterface fun stopListening() { runOnUiThread { speech?.cancel(); speech?.destroy(); speech = null } }
        @JavascriptInterface fun speak(text: String) { runOnUiThread { tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "sweety") } }
        @JavascriptInterface fun openApp(which: String) { runOnUiThread { launchApp(which) } }
        @JavascriptInterface fun searchWeb(query: String) { runOnUiThread {
            val i = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + Uri.encode(query)))
            startActivity(i)
        } }
    }

    private fun beginSpeech() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), micRequest); return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "Is phone par speech recognition available nahi hai", Toast.LENGTH_LONG).show(); return
        }
        speech?.destroy()
        speech = SpeechRecognizer.createSpeechRecognizer(this)
        speech?.setRecognitionListener(object : android.speech.RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {}
            override fun onResults(results: Bundle?) {
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: return
                web.evaluateJavascript("onHeard(${org.json.JSONObject.quote(text)})", null)
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Hello Sweety ko command dein")
        }
        speech?.startListening(intent)
    }

    private fun launchApp(which: String) {
        val pkg = when (which.lowercase(Locale.ROOT)) {
            "youtube" -> "com.google.android.youtube"
            "whatsapp" -> "com.whatsapp"
            else -> null
        }
        val launch = pkg?.let { packageManager.getLaunchIntentForPackage(it) }
        if (launch != null) startActivity(launch)
        else {
            val url = if (which == "youtube") "https://www.youtube.com" else "https://www.whatsapp.com"
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale("hi", "IN")
            // Actual voice depends on the Hindi voices installed on this Android phone.
        }
    }

    override fun onDestroy() {
        speech?.destroy(); tts?.shutdown()
        super.onDestroy()
    }
}
