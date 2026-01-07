package com.example.doan.Fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SearchView
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.doan.Adapters.StoreAdapter
import com.example.doan.Models.*
import com.example.doan.Network.RetrofitClient
import com.example.doan.Network.RetrofitClientMaps
import com.example.doan.R
import com.example.doan.Utils.LoadingDialog
import com.example.doan.Utils.LocationHelper
import com.example.doan.Utils.TouchableMapFragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.atomic.AtomicInteger

class StoreFragment : Fragment(), OnMapReadyCallback {

    private var map: GoogleMap? = null
    private lateinit var searchView: SearchView
    private lateinit var recyclerStores: RecyclerView
    private lateinit var cardStoreInfo: MaterialCardView
    private lateinit var tvStoreName: TextView
    private lateinit var tvStoreAddress: TextView
    private lateinit var btnSetDefaultStore: Button
    private lateinit var btnZoomIn: ImageButton
    private lateinit var btnZoomOut: ImageButton
    private lateinit var btnMyLocation: ImageButton
    private lateinit var cardNearestStore: MaterialCardView
    private lateinit var tvNearestStoreName: TextView
    private lateinit var tvNearestStoreDistance: TextView
    private lateinit var tvNearestStoreDuration: TextView
    private lateinit var btnSelectNearestStore: MaterialButton
    private lateinit var nestedScrollView: NestedScrollView
    private lateinit var cardMapContainer: androidx.cardview.widget.CardView

    private var stores = mutableListOf<Store>()
    private lateinit var storeAdapter: StoreAdapter

    private val geocodeCache = mutableMapOf<String, LatLng>()
    private val markerMap = mutableMapOf<Int, Marker>()
    private var selectedStore: Store? = null
    private var nearestStore: Store? = null

    private lateinit var locationHelper: LocationHelper
    private var userLocation: LatLng? = null
    private lateinit var loadingDialog: LoadingDialog

    private val distanceCache = mutableMapOf<Int, StoreDistanceInfo>()

    companion object {
        private const val TAG = "StoreFragment"
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            checkGPSAndGetLocation()
        } else {
            Toast.makeText(context, "Quyền truy cập vị trí bị từ chối", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_store, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            initViews(view)
            setupRecyclerView()
            setupMap()
            setupListeners()

            locationHelper = LocationHelper(requireContext())
            loadingDialog = LoadingDialog(requireActivity())
            
            // Load stores after all views are initialized
            view.findViewById<ProgressBar>(R.id.progress_bar)?.visibility = View.VISIBLE
            loadStores()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onViewCreated", e)
            Toast.makeText(context, "Lỗi khởi tạo: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        // Only reload if adapter is already initialized and stores list is empty
        try {
            if (::storeAdapter.isInitialized && stores.isEmpty()) {
                view?.findViewById<ProgressBar>(R.id.progress_bar)?.visibility = View.VISIBLE
                loadStores()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onResume", e)
        }
    }

    private fun initViews(view: View) {
        try {
            searchView = view.findViewById(R.id.search_location)
            recyclerStores = view.findViewById(R.id.recycler_branches)
            cardStoreInfo = view.findViewById(R.id.card_branch_info)
            tvStoreName = view.findViewById(R.id.tv_branch_name)
            tvStoreAddress = view.findViewById(R.id.tv_branch_address)
            btnSetDefaultStore = view.findViewById(R.id.btn_set_default_branch)
            btnZoomIn = view.findViewById(R.id.btn_zoom_in)
            btnZoomOut = view.findViewById(R.id.btn_zoom_out)
            btnMyLocation = view.findViewById(R.id.btn_my_location)
            cardNearestStore = view.findViewById(R.id.card_nearest_store)
            tvNearestStoreName = view.findViewById(R.id.tv_nearest_store_name)
            tvNearestStoreDistance = view.findViewById(R.id.tv_nearest_store_distance)
            tvNearestStoreDuration = view.findViewById(R.id.tv_nearest_store_duration)
            btnSelectNearestStore = view.findViewById(R.id.btn_select_nearest_store)
            nestedScrollView = view.findViewById(R.id.nested_scroll_view)
            cardMapContainer = view.findViewById(R.id.card_map_container)
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing views", e)
            throw e
        }
    }

    private fun setupRecyclerView() {
        try {
            recyclerStores.layoutManager = LinearLayoutManager(requireContext())
            storeAdapter = StoreAdapter(requireContext(), stores) { clickedStore ->
                onStoreListClick(clickedStore)
            }
            recyclerStores.adapter = storeAdapter
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up RecyclerView", e)
        }
    }

    private fun setupMap() {
        try {
            val mapFragment = childFragmentManager.findFragmentById(R.id.map) as? TouchableMapFragment
            mapFragment?.getMapAsync(this)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up map", e)
        }
    }

    private fun setupListeners() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrEmpty()) {
                    searchAddress(query)
                    searchView.clearFocus()
                }
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })

        btnSetDefaultStore.setOnClickListener {
            selectedStore?.let { store ->
                saveDefaultStore(store)
                Toast.makeText(context, "Đã chọn ${store.storeName} làm chi nhánh mặc định", Toast.LENGTH_SHORT).show()
            }
        }

        btnZoomIn.setOnClickListener {
            map?.animateCamera(CameraUpdateFactory.zoomIn())
        }

        btnZoomOut.setOnClickListener {
            map?.animateCamera(CameraUpdateFactory.zoomOut())
        }

        btnMyLocation.setOnClickListener {
            requestLocationAndFindNearest()
        }

        btnSelectNearestStore.setOnClickListener {
            nearestStore?.let { store ->
                saveDefaultStore(store)
                Toast.makeText(context, "Đã chọn ${store.storeName} làm chi nhánh mặc định", Toast.LENGTH_SHORT).show()
                onStoreListClick(store)
            }
        }
    }

    private fun requestLocationAndFindNearest() {
        if (locationHelper.hasLocationPermission()) {
            checkGPSAndGetLocation()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun checkGPSAndGetLocation() {
        val activity = activity ?: return

        locationHelper.checkAndRequestGPS(
            activity,
            onGPSEnabled = {
                getCurrentLocation()
            },
            onGPSDisabled = {
                Toast.makeText(context, "Vui lòng bật GPS để tìm quán gần nhất", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun getCurrentLocation() {
        loadingDialog.show("Đang xác định vị trí của bạn...")

        locationHelper.getCurrentLocation(
            onSuccess = { location ->
                if (!isAdded) return@getCurrentLocation

                userLocation = LatLng(location.latitude, location.longitude)
                Log.d(TAG, "Got user location: ${location.latitude}, ${location.longitude}")

                loadingDialog.setMessage("Đang tìm quán gần nhất...")

                displayStoresOnMap()
                findNearestStore()
            },
            onError = { error ->
                if (!isAdded) return@getCurrentLocation
                loadingDialog.dismiss()

                showLocationErrorDialog(error)
            }
        )
    }

    private fun showLocationErrorDialog(error: String) {
        try {
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Không xác định được vị trí")
                .setMessage("$error\n\nBạn có thể:\n• Kiểm tra GPS đã bật chưa\n• Ra ngoài trời để có tín hiệu tốt hơn\n• Nhập địa chỉ thủ công vào ô tìm kiếm")
                .setPositiveButton("Thử lại") { _, _ ->
                    requestLocationAndFindNearest()
                }
                .setNegativeButton("Nhập địa chỉ") { _, _ ->
                    searchView.requestFocus()
                }
                .show()
        } catch (e: Exception) {
            Log.e(TAG, "Error showing location error dialog", e)
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
        }
    }

    private fun findNearestStore() {
        val userLoc = userLocation ?: run {
            loadingDialog.dismiss()
            return
        }

        if (stores.isEmpty()) {
            loadingDialog.dismiss()
            return
        }

        calculateAllDistances(userLoc)
    }

    private fun handleAllDistancesCalculated() {
        if (!isAdded) return
        loadingDialog.dismiss()

        if (distanceCache.isEmpty()) {
            Toast.makeText(context, "Không thể tính khoảng cách đến cửa hàng nào", Toast.LENGTH_SHORT).show()
            return
        }

        val nearestEntry = distanceCache.minByOrNull { it.value.distanceValue }
        val nearestStoreId = nearestEntry?.key
        nearestStore = stores.find { it.id == nearestStoreId }

        if (nearestStore != null && nearestEntry != null) {
            showNearestStoreCard(nearestStore!!, nearestEntry.value)

            geocodeCache[nearestStore!!.address ?: ""]?.let { loc ->
                map?.animateCamera(CameraUpdateFactory.newLatLngZoom(loc, 15f))
            }
        }

        updateStoreListWithDistances()
    }

    private fun calculateAllDistances(userLoc: LatLng) {
        val originStr = "${userLoc.latitude},${userLoc.longitude}"

        val geocodedStores = stores.filter { geocodeCache.containsKey(it.address ?: "") }

        if (geocodedStores.isEmpty()) {
            loadingDialog.dismiss()
            Toast.makeText(context, "Không thể xác định vị trí các cửa hàng", Toast.LENGTH_SHORT).show()
            return
        }

        distanceCache.clear()

        val chunks = geocodedStores.chunked(25)
        val completedRequests = AtomicInteger(0)

        chunks.forEach { chunk ->
            val destinations = chunk.mapNotNull { store ->
                geocodeCache[store.address ?: ""]?.let { loc ->
                    "${loc.latitude},${loc.longitude}"
                }
            }

            if (destinations.isEmpty()) {
                if (completedRequests.incrementAndGet() == chunks.size) {
                    handleAllDistancesCalculated()
                }
                return@forEach
            }

            val destStr = destinations.joinToString("|")
            val key = getString(R.string.google_maps_key)

            RetrofitClientMaps.instance.getDistance(originStr, destStr, key)
                .enqueue(object : Callback<DistanceMatrixResponse> {
                    override fun onResponse(
                        call: Call<DistanceMatrixResponse>,
                        response: Response<DistanceMatrixResponse>
                    ) {
                        if (!isAdded) return

                        if (response.isSuccessful && response.body()?.status == "OK") {
                            val elements = response.body()?.rows?.getOrNull(0)?.elements ?: emptyList()

                            elements.forEachIndexed { index, element ->
                                if (element.status == "OK") {
                                    chunk.getOrNull(index)?.id?.let { storeId ->
                                        distanceCache[storeId] = StoreDistanceInfo(
                                            element.distance.text,
                                            element.duration.text,
                                            element.distance.value
                                        )
                                    }
                                }
                            }
                        } else {
                            Log.e(TAG, "Distance Matrix API error for chunk: ${response.body()?.status} - ${response.errorBody()?.string()}")
                        }

                        if (completedRequests.incrementAndGet() == chunks.size) {
                            handleAllDistancesCalculated()
                        }
                    }

                    override fun onFailure(call: Call<DistanceMatrixResponse>, t: Throwable) {
                        if (!isAdded) return
                        Log.e(TAG, "Error calculating distances for a chunk", t)
                        if (completedRequests.incrementAndGet() == chunks.size) {
                            handleAllDistancesCalculated()
                        }
                    }
                })
        }
    }

    private fun showNearestStoreCard(store: Store, distanceInfo: StoreDistanceInfo) {
        try {
            cardNearestStore.visibility = View.VISIBLE
            tvNearestStoreName.text = "📍 ${store.storeName}"
            tvNearestStoreDistance.text = "Khoảng cách: ${distanceInfo.distanceText}"
            tvNearestStoreDuration.text = "Thời gian: ${distanceInfo.durationText}"
        } catch (e: Exception) {
            Log.e(TAG, "Error showing nearest store card", e)
        }
    }

    private fun updateStoreListWithDistances() {
        val sortedStores = stores.sortedBy { store ->
            distanceCache[store.id ?:0]?.distanceValue ?: Int.MAX_VALUE
        }

        storeAdapter.updateStoresWithDistance(sortedStores.toMutableList(), distanceCache.mapValues { 
            "${it.value.distanceText} - ${it.value.durationText}" 
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::locationHelper.isInitialized) {
            locationHelper.stopLocationUpdates()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        try {
            map = googleMap

            // Enable all gestures for smooth map interaction
            map?.uiSettings?.apply {
                isZoomControlsEnabled = false
                isZoomGesturesEnabled = true
                isScrollGesturesEnabled = true
                isRotateGesturesEnabled = true
                isTiltGesturesEnabled = true
                isMapToolbarEnabled = false
            }

            val hcmc = LatLng(10.8231, 106.6297) // Ho Chi Minh City center
            map?.moveCamera(CameraUpdateFactory.newLatLngZoom(hcmc, 11f))

            map?.setOnMarkerClickListener { marker ->
                try {
                    val tag = marker.tag
                    if (tag is Store) {
                        onStoreMarkerClick(tag)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in marker click", e)
                }
                false
            }

            map?.setOnMapClickListener {
                try {
                     cardStoreInfo.visibility = View.GONE
                     selectedStore = null
                } catch (e: Exception) {
                    Log.e(TAG, "Error in map click", e)
                }
            }

            if (locationHelper.hasLocationPermission()) {
                view?.postDelayed({
                    try {
                        if (isAdded && userLocation == null) {
                            checkGPSAndGetLocation()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error checking GPS", e)
                    }
                }, 1000) 
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onMapReady", e)
        }
    }

    private fun loadStores() {
        try {
            if (!isAdded) return
            if (!::storeAdapter.isInitialized) {
                Log.e(TAG, "storeAdapter not initialized, skipping loadStores")
                return
            }
            
            Log.d(TAG, "Starting to load stores from: ${RetrofitClient.getBaseUrl()}")
            
            RetrofitClient.getInstance(requireContext()).apiService.getStores().enqueue(object : Callback<ApiResponse<List<Store>>> {
                override fun onResponse(call: Call<ApiResponse<List<Store>>>, response: Response<ApiResponse<List<Store>>>) {
                    try {
                        if (!isAdded) return
                        
                        Log.d(TAG, "Store API response received. Success: ${response.isSuccessful}, Code: ${response.code()}")
                        Log.d(TAG, "Response body: ${response.body()}")
                        
                        if (response.isSuccessful && response.body()?.success == true) {
                            val storeList = response.body()?.data ?: emptyList()
                            Log.d(TAG, "Loaded ${storeList.size} stores")
                            
                            stores.clear()
                            stores.addAll(storeList)
                            storeAdapter.notifyDataSetChanged()
                            
                            // Hide loading
                            view?.findViewById<ProgressBar>(R.id.progress_bar)?.visibility = View.GONE
                            
                            // Log first few stores for debugging
                            storeList.take(3).forEach { store ->
                                Log.d(TAG, "Store: ${store.storeName} at ${store.address} (${store.latitude}, ${store.longitude})")
                            }
                            
                            if (storeList.isEmpty()) {
                                Toast.makeText(context, "Không có chi nhánh nào", Toast.LENGTH_SHORT).show()
                            }
                            
                            geocodeAllStores()
                        } else {
                            // Hide loading
                            view?.findViewById<ProgressBar>(R.id.progress_bar)?.visibility = View.GONE
                            val errorMsg = response.body()?.message ?: "Unknown error"
                            Log.e(TAG, "Store API failed: $errorMsg")
                            Toast.makeText(context, "Không thể tải danh sách chi nhánh: $errorMsg", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing stores response", e)
                        view?.findViewById<ProgressBar>(R.id.progress_bar)?.visibility = View.GONE
                        Toast.makeText(context, "Lỗi xử lý dữ liệu: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<List<Store>>>, t: Throwable) {
                    try {
                        if (!isAdded) return
                        // Hide loading
                        view?.findViewById<ProgressBar>(R.id.progress_bar)?.visibility = View.GONE
                        Log.e(TAG, "Store API call failed", t)
                        Toast.makeText(context, "Lỗi kết nối: ${t.message}", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in onFailure", e)
                    }
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error in loadStores", e)
            view?.findViewById<ProgressBar>(R.id.progress_bar)?.visibility = View.GONE
            Toast.makeText(context, "Lỗi khởi tạo: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun geocodeAllStores() {
        try {
            if (!isAdded) return
            
            val key = getString(R.string.google_maps_key)
            var completedCount = 0
            
            if (stores.isEmpty()) {
                displayStoresOnMap()
                return
            }

            stores.forEach { store ->
                val currentAddress = store.address
                if (store.latitude != 0.0 && store.longitude != 0.0) {
                     geocodeCache[currentAddress ?: ""] = LatLng(store.latitude, store.longitude)
                     completedCount++
                     if (completedCount == stores.size) {
                         displayStoresOnMap()
                     }
                } else if (!currentAddress.isNullOrEmpty()) {
                    RetrofitClientMaps.instance.geocodeAddress(currentAddress, key)
                        .enqueue(object : Callback<GeocodingResponse> {
                            override fun onResponse(call: Call<GeocodingResponse>, response: Response<GeocodingResponse>) {
                                try {
                                    if (!isAdded) return
                                    
                                    if (response.isSuccessful && response.body()?.results?.isNotEmpty() == true) {
                                        val location = response.body()!!.results[0].geometry.location
                                        geocodeCache[currentAddress] = LatLng(location.lat, location.lng)
                                    }
                                    completedCount++
                                    if (completedCount == stores.size) {
                                        displayStoresOnMap()
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error processing geocoding response", e)
                                    completedCount++
                                    if (completedCount == stores.size) {
                                        displayStoresOnMap()
                                    }
                                }
                            }

                            override fun onFailure(call: Call<GeocodingResponse>, t: Throwable) {
                                try {
                                    if (!isAdded) return
                                    completedCount++
                                    if (completedCount == stores.size) {
                                        displayStoresOnMap()
                                    }
                                    Log.e(TAG, "Geocoding failed for address: $currentAddress", t)
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error in geocoding onFailure", e)
                                }
                            }
                        })
                } else {
                    // Skip geocoding if no valid key or address
                    completedCount++
                    if (completedCount == stores.size) {
                         displayStoresOnMap()
                     }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in geocodeAllStores", e)
            displayStoresOnMap() // Fallback to display stores without geocoding
        }
    }

    private fun displayStoresOnMap() {
        try {
            if (!isAdded || map == null) return
            
            Log.d(TAG, "Displaying ${stores.size} stores on map")
            
            map?.clear()
            markerMap.clear()

            userLocation?.let {
                map?.addMarker(
                    MarkerOptions()
                        .position(it)
                        .title("Vị trí của bạn")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                )
            }

            var markersAdded = 0
            stores.forEach { store ->
                // Try to use geocoded location first
                val latLng = geocodeCache[store.address ?: ""] ?: run {
                    // Fallback: use store's lat/lng if available
                    if (store.latitude != 0.0 && store.longitude != 0.0) {
                        LatLng(store.latitude, store.longitude)
                    } else {
                        null
                    }
                }
                
                latLng?.let { position ->
                    val marker = map?.addMarker(
                        MarkerOptions()
                            .position(position)
                            .title(store.storeName)
                            .snippet(store.address)
                    )
                    marker?.tag = store
                    store.id?.let { markerMap[it] = marker!! }
                    markersAdded++
                }
            }
            
            Log.d(TAG, "Added $markersAdded markers to map")
            
            // Zoom to show all markers if available
            if (markersAdded > 0) {
                val bounds = com.google.android.gms.maps.model.LatLngBounds.Builder()
                var hasPoints = false
                
                geocodeCache.values.forEach { 
                    bounds.include(it)
                    hasPoints = true
                }
                
                // Include stores with direct coordinates
                stores.forEach { store ->
                    if (store.latitude != 0.0 && store.longitude != 0.0) {
                        bounds.include(LatLng(store.latitude, store.longitude))
                        hasPoints = true
                    }
                }
                
                userLocation?.let { 
                    bounds.include(it)
                    hasPoints = true
                }
                
                if (hasPoints) {
                    try {
                        map?.animateCamera(
                            CameraUpdateFactory.newLatLngBounds(bounds.build(), 100)
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error animating camera", e)
                        // Fallback to default HCM view
                        val hcmc = LatLng(10.8231, 106.6297)
                        map?.animateCamera(CameraUpdateFactory.newLatLngZoom(hcmc, 11f))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in displayStoresOnMap", e)
        }
    }
    
    private fun searchAddress(address: String) {
        loadingDialog.show("Đang tìm kiếm...")
        
        // Tìm kiếm theo tên cửa hàng trước (không cần API key)
        val searchLower = address.lowercase().trim()
        val matchedStores = stores.filter { store ->
            store.storeName?.lowercase()?.contains(searchLower) == true ||
            store.address?.lowercase()?.contains(searchLower) == true
        }
        
        if (matchedStores.isNotEmpty()) {
            loadingDialog.dismiss()
            Log.d(TAG, "Found ${matchedStores.size} stores matching: $address")
            
            // Hiển thị cửa hàng đầu tiên tìm được
            val firstMatch = matchedStores.first()
            val latLng = geocodeCache[firstMatch.address ?: ""] ?: run {
                if (firstMatch.latitude != 0.0 && firstMatch.longitude != 0.0) {
                    LatLng(firstMatch.latitude, firstMatch.longitude)
                } else null
            }
            
            latLng?.let {
                map?.animateCamera(CameraUpdateFactory.newLatLngZoom(it, 15f))
                onStoreListClick(firstMatch)
            }
            
            // Cập nhật danh sách hiển thị các cửa hàng phù hợp
            storeAdapter.updateStores(matchedStores)
            Toast.makeText(context, "Tìm thấy ${matchedStores.size} chi nhánh", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Nếu không tìm thấy theo tên, thử tìm theo địa chỉ với Google Maps API
        val key = getString(R.string.google_maps_key)
        
        // Add Vietnam context to improve search results
        val searchQuery = if (!address.contains("Việt Nam") && !address.contains("Vietnam")) {
            "$address, Việt Nam"
        } else {
            address
        }
        
        Log.d(TAG, "Searching address via API: $searchQuery")
        
        RetrofitClientMaps.instance.geocodeAddress(searchQuery, key).enqueue(object : Callback<GeocodingResponse> {
            override fun onResponse(call: Call<GeocodingResponse>, response: Response<GeocodingResponse>) {
                if (!isAdded) return
                
                Log.d(TAG, "Geocoding response: ${response.code()}, body: ${response.body()}")
                
                if (response.isSuccessful && response.body()?.results?.isNotEmpty() == true) {
                    val location = response.body()!!.results[0].geometry.location
                    userLocation = LatLng(location.lat, location.lng)
                    Log.d(TAG, "Searched location: ${location.lat}, ${location.lng}")

                    loadingDialog.setMessage("Đang tìm quán gần nhất...")
                    displayStoresOnMap()
                    findNearestStore()
                } else {
                    loadingDialog.dismiss()
                    val status = response.body()?.status ?: "UNKNOWN"
                    Log.e(TAG, "Geocoding failed with status: $status")
                    
                    when (status) {
                        "ZERO_RESULTS" -> {
                            // Hiển thị lại tất cả cửa hàng
                            storeAdapter.updateStores(stores)
                            Toast.makeText(context, "Không tìm thấy '$address'. Hiển thị tất cả chi nhánh.", Toast.LENGTH_LONG).show()
                        }
                        "REQUEST_DENIED" -> {
                            // API key lỗi - hiển thị tất cả cửa hàng
                            storeAdapter.updateStores(stores)
                            Toast.makeText(context, "Không tìm thấy kết quả. Vui lòng thử tên chi nhánh.", Toast.LENGTH_LONG).show()
                        }
                        "OVER_QUERY_LIMIT" -> Toast.makeText(context, "Đã vượt quá giới hạn truy vấn. Vui lòng thử lại sau.", Toast.LENGTH_SHORT).show()
                        else -> {
                            storeAdapter.updateStores(stores)
                            Toast.makeText(context, "Không tìm thấy địa chỉ", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            override fun onFailure(call: Call<GeocodingResponse>, t: Throwable) {
                if (!isAdded) return
                loadingDialog.dismiss()
                Log.e(TAG, "Geocoding search failed", t)
                // Hiển thị lại tất cả cửa hàng khi lỗi
                storeAdapter.updateStores(stores)
                Toast.makeText(context, "Lỗi kết nối. Hiển thị tất cả chi nhánh.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun onStoreMarkerClick(store: Store) {
        selectedStore = store
        tvStoreName.text = store.storeName
        tvStoreAddress.text = store.address
        cardStoreInfo.visibility = View.VISIBLE

        store.id?.let { markerMap[it]?.position?.let { latLng -> map?.animateCamera(CameraUpdateFactory.newLatLng(latLng), 300, null) } }
        
        // Scroll list to show selected store
        val position = stores.indexOf(store)
        if (position >= 0) {
            (recyclerStores.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(position, 0)
        }
        
        // Scroll the main view up to show the map fully
        nestedScrollView.post {
            nestedScrollView.smoothScrollTo(0, cardMapContainer.top)
        }
    }

    private fun onStoreListClick(store: Store) {
         val marker = store.id?.let { markerMap[it] }
         if (marker != null) {
             onStoreMarkerClick(store)
         } else {
             // Fallback if marker not found (e.g. map not ready)
             selectedStore = store
             tvStoreName.text = store.storeName
             tvStoreAddress.text = store.address
             cardStoreInfo.visibility = View.VISIBLE
         }
    }

    private fun saveDefaultStore(store: Store) {
        val sharedPref = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putInt("default_store_id", store.id ?: 0)
            putString("default_store_name", store.storeName)
            apply()
        }
    }
}
