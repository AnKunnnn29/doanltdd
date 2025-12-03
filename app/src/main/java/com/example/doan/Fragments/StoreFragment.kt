package com.example.doan.Fragments

import android.content.Context
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.example.doan.Models.ApiResponse
import com.example.doan.Models.Branch
import com.example.doan.Network.RetrofitClient
import com.example.doan.R
import com.example.doan.Utils.DataCache
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class StoreFragment : Fragment(), OnMapReadyCallback {

    private lateinit var etSearchAddress: EditText
    private lateinit var btnSearchAddress: ImageView
    private lateinit var cardBranchInfo: MaterialCardView
    private lateinit var tvBranchName: TextView
    private lateinit var tvBranchAddress: TextView
    private lateinit var tvDistance: TextView
    private lateinit var btnSetDefaultBranch: MaterialButton
    private lateinit var progressBar: ProgressBar

    private var map: GoogleMap? = null
    private var userLocation: LatLng? = null
    private var branches: List<Branch> = emptyList()
    private var selectedBranch: Branch? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return try {
            val view = inflater.inflate(R.layout.fragment_store, container, false)

            initViews(view)
            setupMap()
            setupListeners()
            
            view
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreateView: ${e.message}")
            e.printStackTrace()
            Toast.makeText(context, "Lỗi tải trang cửa hàng", Toast.LENGTH_SHORT).show()
            inflater.inflate(R.layout.fragment_store, container, false)
        }
    }

    private fun initViews(view: View) {
        etSearchAddress = view.findViewById(R.id.et_search_address)
        btnSearchAddress = view.findViewById(R.id.btn_search_address)
        cardBranchInfo = view.findViewById(R.id.card_branch_info)
        tvBranchName = view.findViewById(R.id.tv_branch_name)
        tvBranchAddress = view.findViewById(R.id.tv_branch_address)
        tvDistance = view.findViewById(R.id.tv_distance)
        btnSetDefaultBranch = view.findViewById(R.id.btn_set_default_branch)
        progressBar = view.findViewById(R.id.progress_bar)
    }

    private fun setupMap() {
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
    }

    private fun setupListeners() {
        btnSearchAddress.setOnClickListener {
            val address = etSearchAddress.text.toString().trim()
            if (address.isNotEmpty()) {
                searchAddress(address)
            }
        }

        etSearchAddress.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val address = etSearchAddress.text.toString().trim()
                if (address.isNotEmpty()) {
                    searchAddress(address)
                }
                true
            } else {
                false
            }
        }

        btnSetDefaultBranch.setOnClickListener {
            selectedBranch?.let { branch ->
                saveDefaultBranch(branch)
                Toast.makeText(context, "Đã chọn ${branch.branchName} làm chi nhánh mặc định", Toast.LENGTH_SHORT).show()
                
                // Optional: Navigate to Order or Home screen
                // requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation).selectedItemId = R.id.nav_order
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        
        // Move camera to a default location (e.g., Ho Chi Minh City)
        val hcmc = LatLng(10.7769, 106.7009)
        map?.moveCamera(CameraUpdateFactory.newLatLngZoom(hcmc, 12f))

        // Setup marker click listener
        map?.setOnMarkerClickListener { marker ->
            val tag = marker.tag
            if (tag is Branch) {
                onBranchMarkerClick(tag)
            }
            false // Allow default behavior (center map on marker)
        }
        
        map?.setOnMapClickListener {
             cardBranchInfo.visibility = View.GONE
             selectedBranch = null
        }

        loadBranches()
    }

    private fun loadBranches() {
        progressBar.visibility = View.VISIBLE
        // Use getStores() instead of getBranches() since backend doesn't have branches endpoint
        RetrofitClient.getInstance(requireContext()).apiService.getStores().enqueue(object : Callback<ApiResponse<List<com.example.doan.Models.Store>>> {
            override fun onResponse(call: Call<ApiResponse<List<com.example.doan.Models.Store>>>, response: Response<ApiResponse<List<com.example.doan.Models.Store>>>) {
                progressBar.visibility = View.GONE
                if (response.isSuccessful && response.body()?.success == true) {
                    val stores = response.body()?.data ?: emptyList()
                    
                    Log.d(TAG, "Loaded ${stores.size} stores from API")
                    stores.forEachIndexed { index, store ->
                        Log.d(TAG, "Store $index: ${store.storeName} at ${store.address}")
                    }
                    
                    // Convert Store to Branch for compatibility
                    branches = stores.map { store ->
                        Branch(
                            id = store.id,
                            branchName = store.storeName,
                            address = store.address
                        )
                    }
                    DataCache.branches = branches // Cache for other screens
                    
                    if (branches.isEmpty()) {
                        Toast.makeText(context, "Không có cửa hàng nào", Toast.LENGTH_SHORT).show()
                    } else {
                        displayBranchesOnMap()
                    }
                } else {
                    Log.e(TAG, "API response not successful: ${response.code()}")
                    Toast.makeText(context, "Không thể tải danh sách cửa hàng", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse<List<com.example.doan.Models.Store>>>, t: Throwable) {
                progressBar.visibility = View.GONE
                Log.e(TAG, "Error fetching stores", t)
                Toast.makeText(context, "Lỗi kết nối server: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun displayBranchesOnMap() {
        map?.let { googleMap ->
            // Keep user marker if exists
            googleMap.clear()
            userLocation?.let {
                googleMap.addMarker(
                    MarkerOptions()
                        .position(it)
                        .title("Vị trí của bạn")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                )
            }

            val boundsBuilder = LatLngBounds.Builder()
            var hasBranches = false

            for (branch in branches) {
                // Branch is converted from Store which has latitude/longitude
                // Try to get coordinates from geocoding the address
                val branchLocation = getLatLngFromAddress(branch.address ?: "")
                
                if (branchLocation != null) {
                    val marker = googleMap.addMarker(
                        MarkerOptions()
                            .position(branchLocation)
                            .title(branch.branchName)
                            .snippet(branch.address)
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                    )
                    marker?.tag = branch
                    boundsBuilder.include(branchLocation)
                    hasBranches = true
                    
                    Log.d(TAG, "Added marker for ${branch.branchName} at $branchLocation")
                } else {
                    Log.w(TAG, "Could not geocode address for ${branch.branchName}: ${branch.address}")
                }
            }
            
            if (hasBranches) {
                try {
                    val bounds = boundsBuilder.build()
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
                } catch (e: Exception) {
                    Log.e(TAG, "Error animating camera: ${e.message}")
                }
            } else {
                // No branches could be geocoded, show default location (HCMC)
                val hcmc = LatLng(10.7769, 106.7009)
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(hcmc, 12f))
                Toast.makeText(context, "Không thể hiển thị vị trí cửa hàng trên bản đồ", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun searchAddress(address: String) {
        progressBar.visibility = View.VISIBLE
        Thread {
            val location = getLatLngFromAddress(address)
            activity?.runOnUiThread {
                progressBar.visibility = View.GONE
                if (location != null) {
                    userLocation = location
                    map?.let { googleMap ->
                        // Clear previous markers but re-add branch markers
                        displayBranchesOnMap() 
                        
                        // Add/Update user marker
                        googleMap.addMarker(
                            MarkerOptions()
                                .position(location)
                                .title("Vị trí của bạn: $address")
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                        )?.showInfoWindow()

                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 14f))
                        
                        // Auto-calculate distances
                        recalculateDistances()
                    }
                } else {
                    Toast.makeText(context, "Không tìm thấy địa chỉ này", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun getLatLngFromAddress(strAddress: String): LatLng? {
        val coder = Geocoder(requireContext(), Locale.getDefault())
        try {
            val addressList = coder.getFromLocationName(strAddress, 1)
            if (!addressList.isNullOrEmpty()) {
                val location = addressList[0]
                return LatLng(location.latitude, location.longitude)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    private fun onBranchMarkerClick(branch: Branch) {
        selectedBranch = branch
        tvBranchName.text = branch.branchName
        tvBranchAddress.text = branch.address
        
        if (userLocation != null) {
            // Calculate distance
            // We need branch location again. In real app, store it in Branch object.
            val branchLoc = getLatLngFromAddress(branch.address ?: "")
            if (branchLoc != null) {
                val distance = calculateDistance(userLocation!!, branchLoc)
                tvBranchAddress.text = "${branch.address}\n(Cách bạn khoảng ${String.format("%.1f", distance)} km)"
                tvDistance.text = "Cách bạn: ${String.format("%.1f", distance)} km"
                tvDistance.visibility = View.VISIBLE
            } else {
                 tvDistance.visibility = View.GONE
            }
        } else {
            tvDistance.text = "Nhập địa chỉ để xem khoảng cách"
             tvDistance.visibility = View.VISIBLE
        }
        
        cardBranchInfo.visibility = View.VISIBLE
    }
    
    private fun recalculateDistances() {
        // Update UI if a branch is already selected
        selectedBranch?.let { branch ->
            onBranchMarkerClick(branch)
        }
    }

    private fun calculateDistance(start: LatLng, end: LatLng): Double {
        val earthRadius = 6371.0 // km
        val dLat = Math.toRadians(end.latitude - start.latitude)
        val dLon = Math.toRadians(end.longitude - start.longitude)
        val a = sin(dLat / 2).pow(2.0) +
                cos(Math.toRadians(start.latitude)) * cos(Math.toRadians(end.latitude)) *
                sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }

    private fun saveDefaultBranch(branch: Branch) {
        val prefs = requireContext().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putLong("DEFAULT_BRANCH_ID", branch.id?.toLong() ?: -1)
            putString("DEFAULT_BRANCH_NAME", branch.branchName)
            apply()
        }
        
        // Also update session or global state if needed
    }

    companion object {
        private const val TAG = "StoreFragment"
    }
}
