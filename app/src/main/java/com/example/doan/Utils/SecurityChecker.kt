package com.example.doan.Utils

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.example.doan.BuildConfig
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

/**
 * ✅ SECURITY: Kiểm tra các mối đe dọa bảo mật
 */
object SecurityChecker {
    
    private const val TAG = "SecurityChecker"
    
    /**
     * Kiểm tra thiết bị có bị root không
     */
    fun isDeviceRooted(): Boolean {
        return checkRootMethod1() || checkRootMethod2() || checkRootMethod3() || checkRootMethod4()
    }
    
    // Method 1: Check for su binary
    private fun checkRootMethod1(): Boolean {
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/su/bin/su",
            "/su/bin",
            "/system/xbin/daemonsu",
            "/system/etc/init.d/99telecom",
            "/system/app/Superuser/Superuser.apk"
        )
        
        for (path in paths) {
            if (File(path).exists()) {
                if (BuildConfig.DEBUG) {
                    Log.w(TAG, "Root detected: Found su binary at $path")
                }
                return true
            }
        }
        return false
    }
    
    // Method 2: Check for root management apps
    private fun checkRootMethod2(): Boolean {
        val rootApps = arrayOf(
            "com.noshufou.android.su",
            "com.noshufou.android.su.elite",
            "eu.chainfire.supersu",
            "com.koushikdutta.superuser",
            "com.thirdparty.superuser",
            "com.yellowes.su",
            "com.topjohnwu.magisk",
            "com.kingroot.kinguser",
            "com.kingo.root",
            "com.smedialink.oneclickroot",
            "com.zhiqupk.root.global",
            "com.alephzain.framaroot"
        )
        
        // Note: Cần context để check, sẽ implement sau
        return false
    }
    
    // Method 3: Check for dangerous props
    private fun checkRootMethod3(): Boolean {
        val buildTags = Build.TAGS
        return buildTags != null && buildTags.contains("test-keys")
    }
    
    // Method 4: Try to execute su command
    private fun checkRootMethod4(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("/system/xbin/which", "su"))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val result = reader.readLine()
            reader.close()
            result != null
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Kiểm tra app có bị repackage với debuggable flag không
     */
    fun isAppDebuggable(context: Context): Boolean {
        return (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }
    
    /**
     * Kiểm tra có đang chạy trên emulator không
     */
    fun isEmulator(): Boolean {
        return (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk" == Build.PRODUCT
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu")
                || Build.PRODUCT.contains("sdk")
                || Build.PRODUCT.contains("emulator")
                || Build.PRODUCT.contains("simulator"))
    }
    
    /**
     * Kiểm tra app có bị hook bởi Frida/Xposed không
     */
    fun isHooked(): Boolean {
        return checkFrida() || checkXposed()
    }
    
    private fun checkFrida(): Boolean {
        // Check for Frida server
        val fridaPorts = arrayOf(27042, 27043)
        for (port in fridaPorts) {
            try {
                val socket = java.net.Socket("127.0.0.1", port)
                socket.close()
                return true
            } catch (e: Exception) {
                // Port not open, good
            }
        }
        
        // Check for Frida libraries
        try {
            val mapsFile = File("/proc/self/maps")
            if (mapsFile.exists()) {
                val content = mapsFile.readText()
                if (content.contains("frida") || content.contains("gadget")) {
                    return true
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
        
        return false
    }
    
    private fun checkXposed(): Boolean {
        // Check for Xposed framework
        try {
            Class.forName("de.robv.android.xposed.XposedBridge")
            return true
        } catch (e: ClassNotFoundException) {
            // Not found, good
        }
        
        // Check stack trace for Xposed
        try {
            val stackTrace = Thread.currentThread().stackTrace
            for (element in stackTrace) {
                if (element.className.contains("xposed") || 
                    element.className.contains("de.robv.android")) {
                    return true
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
        
        return false
    }
    
    /**
     * Kiểm tra signature của app có bị thay đổi không (anti-tampering)
     */
    fun isAppTampered(context: Context): Boolean {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNATURES
                )
            }
            
            // Trong production, so sánh với signature gốc
            // Đây chỉ là placeholder - cần thay bằng signature thực của app
            // val expectedSignature = "YOUR_APP_SIGNATURE_HASH"
            // val actualSignature = getSignatureHash(packageInfo)
            // return actualSignature != expectedSignature
            
            false
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Error checking app signature", e)
            }
            false
        }
    }
    
    /**
     * Kiểm tra tổng hợp và quyết định có cho phép app chạy không
     * Wrap trong try-catch để tránh crash
     */
    fun performSecurityCheck(context: Context): SecurityCheckResult {
        return try {
            val isRooted = try { isDeviceRooted() } catch (e: Exception) { false }
            val isDebuggable = try { isAppDebuggable(context) } catch (e: Exception) { false }
            val isEmulator = try { isEmulator() } catch (e: Exception) { false }
            val isHooked = try { isHooked() } catch (e: Exception) { false }
            val isTampered = try { isAppTampered(context) } catch (e: Exception) { false }
            
            // KHÔNG BLOCK APP - chỉ log warning
            val shouldBlock = false
            
            SecurityCheckResult(
                isRooted = isRooted,
                isDebuggable = isDebuggable,
                isEmulator = isEmulator,
                isHooked = isHooked,
                isTampered = isTampered,
                shouldBlockApp = shouldBlock,
                reason = when {
                    isRooted -> "Thiết bị đã bị root"
                    isDebuggable && !isEmulator -> "App đã bị chỉnh sửa"
                    isHooked -> "Phát hiện công cụ hook (Frida/Xposed)"
                    isTampered -> "App đã bị thay đổi signature"
                    else -> null
                }
            )
        } catch (e: Exception) {
            // Trả về kết quả an toàn nếu có lỗi
            SecurityCheckResult(
                isRooted = false,
                isDebuggable = false,
                isEmulator = false,
                isHooked = false,
                isTampered = false,
                shouldBlockApp = false,
                reason = null
            )
        }
    }
}

data class SecurityCheckResult(
    val isRooted: Boolean,
    val isDebuggable: Boolean,
    val isEmulator: Boolean,
    val isHooked: Boolean = false,
    val isTampered: Boolean = false,
    val shouldBlockApp: Boolean,
    val reason: String?
)
