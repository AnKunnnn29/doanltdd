package com.example.doan.Utils

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.widget.Toast

/**
 * Voice Search Helper - Don gian hoa cho tim kiem
 */
class VoiceSearchHelper(
    private val context: Context,
    private val onResult: (String) -> Unit
) {
    
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private val handler = Handler(Looper.getMainLooper())
    private var timeoutRunnable: Runnable? = null
    private var lastPartialResult: String? = null
    
    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Toast.makeText(context, "Thiet bi khong ho tro nhan dien giong noi", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (isListening) {
            stopListening()
        }
        
        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer?.setRecognitionListener(createRecognitionListener())
            
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "vi-VN")
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }
            
            speechRecognizer?.startListening(intent)
            isListening = true
            lastPartialResult = null
            startTimeout()
            
            Log.d(TAG, "Voice search started")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting voice search", e)
            Toast.makeText(context, "Khong the bat dau tim kiem giong noi", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun startTimeout() {
        cancelTimeout()
        timeoutRunnable = Runnable {
            if (isListening) {
                Log.d(TAG, "Timeout")
                if (!lastPartialResult.isNullOrEmpty()) {
                    onResult(lastPartialResult!!)
                }
                stopListening()
            }
        }
        handler.postDelayed(timeoutRunnable!!, TIMEOUT_MS)
    }
    
    private fun cancelTimeout() {
        timeoutRunnable?.let { handler.removeCallbacks(it) }
        timeoutRunnable = null
    }
    
    fun stopListening() {
        cancelTimeout()
        isListening = false
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping", e)
        }
        speechRecognizer = null
    }
    
    private fun createRecognitionListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {
            cancelTimeout()
            startTimeout()
        }
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {
            cancelTimeout()
        }
        
        override fun onError(error: Int) {
            cancelTimeout()
            isListening = false
            
            // Neu co partial result, su dung no
            if (!lastPartialResult.isNullOrEmpty()) {
                onResult(lastPartialResult!!)
            } else {
                val msg = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "Khong nhan dien duoc"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Khong nghe thay giong noi"
                    else -> "Loi tim kiem giong noi"
                }
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        }
        
        override fun onResults(results: Bundle?) {
            cancelTimeout()
            isListening = false
            
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                onResult(matches[0])
            }
        }
        
        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                lastPartialResult = matches[0]
                cancelTimeout()
                startTimeout()
            }
        }
        
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }
    
    fun destroy() {
        stopListening()
    }
    
    companion object {
        private const val TAG = "VoiceSearchHelper"
        private const val TIMEOUT_MS = 5000L
    }
}
