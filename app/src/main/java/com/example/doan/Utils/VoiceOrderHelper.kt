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
import com.example.doan.Models.Product
import java.util.Locale

/**
 * Voice Order Helper
 * Xu ly nhan dien giong noi va phan tich cau lenh dat hang
 */
class VoiceOrderHelper(
    private val context: Context,
    private val listener: VoiceOrderListener
) {
    
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private val handler = Handler(Looper.getMainLooper())
    private var timeoutRunnable: Runnable? = null
    private var lastPartialResult: String? = null  // Luu partial result cuoi cung
    
    interface VoiceOrderListener {
        fun onListeningStarted()
        fun onListeningEnded()
        fun onSpeechResult(text: String)
        fun onOrderParsed(result: VoiceOrderResult)
        fun onError(message: String)
    }
    
    data class VoiceOrderResult(
        val product: Product?,
        val similarProducts: List<Product>,  // Danh sách sản phẩm tương tự
        val quantity: Int,
        val sizeName: String,
        val originalText: String,
        val confidence: Float
    )
    
    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            listener.onError("Thiet bi khong ho tro nhan dien giong noi")
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
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "vi-VN")
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "vi-VN")
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 3000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
            }
            
            speechRecognizer?.startListening(intent)
            isListening = true
            lastPartialResult = null  // Reset partial result
            listener.onListeningStarted()
            
            // Set timeout 10 giay
            startTimeout()
            
            Log.d(TAG, "Started listening...")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting speech recognition", e)
            listener.onError("Khong the bat dau nhan dien giong noi: ${e.message}")
            isListening = false
        }
    }
    
    private fun startTimeout() {
        cancelTimeout()
        timeoutRunnable = Runnable {
            if (isListening) {
                Log.d(TAG, "Timeout - checking for partial results")
                
                // Neu co partial result, xu ly no thay vi bao loi
                if (!lastPartialResult.isNullOrEmpty()) {
                    Log.d(TAG, "Using last partial result: $lastPartialResult")
                    isListening = false
                    
                    val orderResult = parseVoiceOrder(lastPartialResult!!, 0.7f)
                    listener.onOrderParsed(orderResult)
                    listener.onListeningEnded()
                } else {
                    stopListening()
                    listener.onError("Het thoi gian cho - vui long thu lai")
                }
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
            Log.e(TAG, "Error stopping speech recognition", e)
        }
        speechRecognizer = null
    }


    private fun createRecognitionListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            Log.d(TAG, "Ready for speech")
        }
        
        override fun onBeginningOfSpeech() {
            Log.d(TAG, "Beginning of speech")
            cancelTimeout() // Reset timeout khi bat dau noi
            startTimeout()
        }
        
        override fun onRmsChanged(rmsdB: Float) {}
        
        override fun onBufferReceived(buffer: ByteArray?) {}
        
        override fun onEndOfSpeech() {
            Log.d(TAG, "End of speech")
            cancelTimeout()
        }
        
        override fun onError(error: Int) {
            cancelTimeout()
            isListening = false
            
            val errorMessage = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Loi ghi am"
                SpeechRecognizer.ERROR_CLIENT -> "Loi client - thu lai"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Chua cap quyen microphone"
                SpeechRecognizer.ERROR_NETWORK -> "Loi mang - kiem tra ket noi internet"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Het thoi gian ket noi mang"
                SpeechRecognizer.ERROR_NO_MATCH -> "Khong nhan dien duoc - thu noi ro hon"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Dang ban - thu lai"
                SpeechRecognizer.ERROR_SERVER -> "Loi server Google"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Khong nghe thay giong noi"
                else -> "Loi khong xac dinh ($error)"
            }
            
            Log.e(TAG, "Speech recognition error: $errorMessage ($error)")
            listener.onError(errorMessage)
            listener.onListeningEnded()
        }
        
        override fun onResults(results: Bundle?) {
            cancelTimeout()
            isListening = false
            
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val confidences = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
            
            Log.d(TAG, "Got results: $matches")
            
            if (!matches.isNullOrEmpty()) {
                val bestMatch = matches[0]
                val confidence = confidences?.getOrNull(0) ?: 0.5f
                
                Log.d(TAG, "Speech result: $bestMatch (confidence: $confidence)")
                listener.onSpeechResult(bestMatch)
                
                // Parse order tu text
                val orderResult = parseVoiceOrder(bestMatch, confidence)
                listener.onOrderParsed(orderResult)
            } else {
                listener.onError("Khong nhan duoc ket qua")
            }
            
            listener.onListeningEnded()
        }
        
        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                val text = matches[0]
                Log.d(TAG, "Partial result: $text")
                lastPartialResult = text  // Luu lai partial result
                listener.onSpeechResult(text)
                
                // Reset timeout moi khi co partial result
                cancelTimeout()
                startTimeout()
            }
        }
        
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }
    
    fun parseVoiceOrder(text: String, confidence: Float): VoiceOrderResult {
        val lowerText = text.lowercase(Locale.getDefault())
        
        val quantity = extractQuantity(lowerText)
        val sizeName = extractSize(lowerText)
        val (product, similarProducts) = findProductsByVoice(lowerText)
        
        Log.d(TAG, "Parsed: quantity=$quantity, size=$sizeName, product=${product?.name}, similar=${similarProducts.size}")
        
        return VoiceOrderResult(
            product = product,
            similarProducts = similarProducts,
            quantity = quantity,
            sizeName = sizeName,
            originalText = text,
            confidence = confidence
        )
    }


    private fun extractQuantity(text: String): Int {
        val numberRegex = Regex("(\\d+)")
        val numberMatch = numberRegex.find(text)
        if (numberMatch != null) {
            return numberMatch.value.toIntOrNull() ?: 1
        }
        
        val wordNumbers = mapOf(
            "mot" to 1, "một" to 1,
            "hai" to 2,
            "ba" to 3,
            "bon" to 4, "bốn" to 4,
            "nam" to 5, "năm" to 5,
            "sau" to 6, "sáu" to 6,
            "bay" to 7, "bảy" to 7,
            "tam" to 8, "tám" to 8,
            "chin" to 9, "chín" to 9,
            "muoi" to 10, "mười" to 10
        )
        
        for ((word, number) in wordNumbers) {
            if (text.contains(word)) {
                return number
            }
        }
        
        return 1
    }
    
    private fun extractSize(text: String): String {
        val sizePatterns = listOf(
            "size l" to "L", "sai l" to "L", "xai l" to "L",
            "size m" to "M", "sai m" to "M", "xai m" to "M",
            "size s" to "S", "sai s" to "S", "xai s" to "S",
            "co lon" to "L", "cỡ lớn" to "L", "lon" to "L", "lớn" to "L",
            "co vua" to "M", "cỡ vừa" to "M", "vua" to "M", "vừa" to "M",
            "co nho" to "S", "cỡ nhỏ" to "S", "nho" to "S", "nhỏ" to "S"
        )
        
        for ((pattern, size) in sizePatterns) {
            if (text.contains(pattern)) {
                return size
            }
        }
        
        return "M"
    }
    
    /**
     * Tìm sản phẩm và danh sách sản phẩm tương tự
     * @return Pair(bestMatch, similarProducts)
     */
    private fun findProductsByVoice(text: String): Pair<Product?, List<Product>> {
        val products = DataCache.products ?: return Pair(null, emptyList())
        
        // Loại bỏ các từ không cần thiết
        val cleanText = text
            .replace(Regex("cho toi|cho tôi|dat|đặt|them|thêm|mua|ly|coc|cốc|size [sml]|sai [sml]|co \\w+|cỡ \\w+|\\d+|một|hai|ba|bốn|năm|sáu|bảy|tám|chín|mười"), "")
            .trim()
        
        Log.d(TAG, "Clean text for search: '$cleanText'")
        
        // Tính điểm cho tất cả sản phẩm
        val scoredProducts = mutableListOf<Pair<Product, Double>>()
        
        for (product in products) {
            val productName = product.name?.lowercase(Locale.getDefault()) ?: continue
            
            // Kiểm tra exact match
            if (cleanText.contains(productName) || productName.contains(cleanText)) {
                scoredProducts.add(Pair(product, 1.0))
                continue
            }
            
            // Tính điểm similarity
            val productWords = productName.split(" ").filter { it.length > 1 }
            val textWords = cleanText.split(" ").filter { it.length > 1 }
            
            var matchCount = 0
            for (pWord in productWords) {
                for (tWord in textWords) {
                    if (pWord.contains(tWord) || tWord.contains(pWord) || 
                        levenshteinDistance(pWord, tWord) <= 2) {
                        matchCount++
                        break
                    }
                }
            }
            
            val score = if (productWords.isNotEmpty()) {
                matchCount.toDouble() / productWords.size
            } else 0.0
            
            if (score > 0.2) {
                scoredProducts.add(Pair(product, score))
            }
        }
        
        // Sắp xếp theo điểm giảm dần
        val sortedProducts = scoredProducts.sortedByDescending { it.second }
        
        // Best match là sản phẩm có điểm cao nhất (>= 0.3)
        val bestMatch = sortedProducts.firstOrNull { it.second >= 0.3 }?.first
        
        // Similar products là top 5 sản phẩm (không bao gồm best match)
        val similarProducts = sortedProducts
            .map { it.first }
            .filter { it != bestMatch }
            .take(5)
        
        Log.d(TAG, "Best match: ${bestMatch?.name}, similar: ${similarProducts.map { it.name }}")
        return Pair(bestMatch, similarProducts)
    }
    
    private fun calculateSimilarity(s1: String, s2: String): Double {
        val words1 = s1.split(" ").filter { it.length > 1 }
        val words2 = s2.split(" ").filter { it.length > 1 }
        
        if (words1.isEmpty() || words2.isEmpty()) return 0.0
        
        var matchCount = 0
        for (word1 in words1) {
            for (word2 in words2) {
                if (word1.contains(word2) || word2.contains(word1) || 
                    levenshteinDistance(word1, word2) <= 2) {
                    matchCount++
                    break
                }
            }
        }
        
        return matchCount.toDouble() / maxOf(words1.size, words2.size)
    }
    
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j
        
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        
        return dp[s1.length][s2.length]
    }
    
    fun destroy() {
        stopListening()
    }
    
    companion object {
        private const val TAG = "VoiceOrderHelper"
        private const val TIMEOUT_MS = 10000L // 10 giay
    }
}
