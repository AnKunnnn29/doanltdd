package com.example.doan.Utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*

/**
 * Helper class để quản lý việc lấy vị trí GPS của user
 */
class LocationHelper(private val context: Context) {

    companion object {
        private const val TAG = "LocationHelper"
        const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        const val GPS_REQUEST_CODE = 1002
    }

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private var locationCallback: LocationCallback? = null
    private var onLocationReceived: ((Location) -> Unit)? = null
    private var onLocationError: ((String) -> Unit)? = null

    /**
     * Kiểm tra xem đã có quyền location chưa
     */
    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Yêu cầu quyền location
     */
    fun requestLocationPermission(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    /**
     * Kiểm tra GPS đã bật chưa và yêu cầu bật nếu cần
     */
    fun checkAndRequestGPS(
        activity: Activity,
        onGPSEnabled: () -> Unit,
        onGPSDisabled: () -> Unit
    ) {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            10000L
        ).build()

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
            .setAlwaysShow(true)

        val settingsClient = LocationServices.getSettingsClient(context)
        settingsClient.checkLocationSettings(builder.build())
            .addOnSuccessListener {
                // GPS đã bật
                onGPSEnabled()
            }
            .addOnFailureListener { exception ->
                if (exception is ResolvableApiException) {
                    try {
                        // Hiển thị dialog yêu cầu bật GPS
                        exception.startResolutionForResult(activity, GPS_REQUEST_CODE)
                    } catch (sendEx: IntentSender.SendIntentException) {
                        Log.e(TAG, "Error requesting GPS", sendEx)
                        onGPSDisabled()
                    }
                } else {
                    // Không thể resolve, hiển thị dialog thủ công
                    showEnableGPSDialog(activity)
                    onGPSDisabled()
                }
            }
    }

    /**
     * Hiển thị dialog yêu cầu bật GPS thủ công
     */
    private fun showEnableGPSDialog(activity: Activity) {
        AlertDialog.Builder(activity)
            .setTitle("Bật định vị")
            .setMessage("Vui lòng bật GPS để xác định vị trí của bạn và tìm quán gần nhất.")
            .setPositiveButton("Cài đặt") { _, _ ->
                activity.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            .setNegativeButton("Để sau", null)
            .show()
    }

    /**
     * Lấy vị trí hiện tại của user
     */
    fun getCurrentLocation(
        onSuccess: (Location) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!hasLocationPermission()) {
            onError("Chưa cấp quyền truy cập vị trí")
            return
        }

        this.onLocationReceived = onSuccess
        this.onLocationError = onError

        try {
            // Thử lấy last known location trước
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null && isLocationFresh(location)) {
                        Log.d(TAG, "Got last known location: ${location.latitude}, ${location.longitude}")
                        onSuccess(location)
                    } else {
                        // Nếu không có last location hoặc quá cũ, request location mới
                        Log.d(TAG, "Last location is null or stale, requesting new location...")
                        requestNewLocation()
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error getting last location", e)
                    requestNewLocation()
                }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException", e)
            onError("Không có quyền truy cập vị trí")
        }
    }
    
    /**
     * Kiểm tra location có còn mới không (trong vòng 5 phút)
     */
    private fun isLocationFresh(location: Location): Boolean {
        val fiveMinutesAgo = System.currentTimeMillis() - (5 * 60 * 1000)
        return location.time > fiveMinutesAgo
    }

    /**
     * Request vị trí mới từ GPS với timeout
     */
    private fun requestNewLocation() {
        // Sử dụng BALANCED_POWER_ACCURACY để nhanh hơn
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            5000L
        )
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(1000L)
            .setMaxUpdates(1)
            .setMaxUpdateDelayMillis(10000L)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation
                if (location != null) {
                    Log.d(TAG, "Got new location: ${location.latitude}, ${location.longitude}")
                    onLocationReceived?.invoke(location)
                } else {
                    Log.e(TAG, "Location result is null")
                    onLocationError?.invoke("Không thể xác định vị trí. Vui lòng thử lại.")
                }
                stopLocationUpdates()
            }
            
            override fun onLocationAvailability(availability: LocationAvailability) {
                if (!availability.isLocationAvailable) {
                    Log.e(TAG, "Location not available")
                    // Không gọi error ở đây vì có thể vẫn nhận được location
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
            
            // Timeout sau 15 giây nếu không nhận được location
            android.os.Handler(Looper.getMainLooper()).postDelayed({
                if (locationCallback != null) {
                    Log.e(TAG, "Location request timeout")
                    onLocationError?.invoke("Không thể xác định vị trí. Vui lòng kiểm tra GPS và thử lại.")
                    stopLocationUpdates()
                }
            }, 15000)
            
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException requesting location", e)
            onLocationError?.invoke("Không có quyền truy cập vị trí")
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting location", e)
            onLocationError?.invoke("Lỗi xác định vị trí: ${e.message}")
        }
    }

    /**
     * Dừng cập nhật vị trí
     */
    fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
            locationCallback = null
        }
    }

    /**
     * Tính khoảng cách giữa 2 điểm (đơn vị: km)
     */
    fun calculateDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0] / 1000f // Convert to km
    }
}
