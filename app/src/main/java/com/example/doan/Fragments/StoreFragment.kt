package com.example.doan.Fragments

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.doan.Adapters.StoreAdapter
import com.example.doan.Models.ApiResponse
import com.example.doan.Models.Branch
import com.example.doan.Models.DistanceMatrixResponse
import com.example.doan.Models.Element
import com.example.doan.Models.Store
import com.example.doan.Network.RetrofitClient
import com.example.doan.Network.RetrofitClientMaps
import com.example.doan.R
import com.example.doan.Utils.DataCache
import com.example.doan.Utils.LoadingDialog
import com.example.doan.Utils.LocationHelper
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.util.Locale

class StoreFragment : Fragment(), OnMapReadyCallback {

    private lateinit var searchView: SearchView
    private lateinit var recyclerBranches: RecyclerView
    private lateinit var cardBranchInfo: MaterialCardView
    private lateinit var tvBranchName: TextView
    private lateinit var tvBranchAddress: TextView
    private lateinit var tvDistance: TextView
    private lateinit var btnSetDefaultBranch: MaterialButton
    private lateinit var progressBar: ProgressBar
    private lateinit var loadingDialog: LoadingDialog
    
    // Zoom controls
    private lateinit var btnZoomIn: ImageButton
    private lateinit var btnZoomOut: ImageButton
    
    // Location button
    private lateinit var btnMyLocation: ImageButton
    
    // Nearest store suggestion
    private lateinit var cardNearestStore: MaterialCardView
    private lateinit var tvNearestStoreName: TextView
    private lateinit var tvNearestStoreDistance: TextView
    private lateinit var tvNearestStoreDuration: TextView
    private lateinit var btnSelectNearestStore: MaterialButton
    
    // Scroll view và map container để auto scroll
    private lateinit var nestedScrollView: androidx.core.widget.NestedScrollView
    private lateinit var cardMapContainer: androidx.cardview.widget.CardView
    
    private lateinit var storeAdapter: StoreAdapter
    private lateinit var locationHelper: LocationHelper

    private var map: GoogleMap? = null
    private var userLocation: LatLng? = null
    private var branches: List<Branch> = emptyList()
    private var stores: List<Store> = emptyList()
    private var selectedBranch: Branch? = null
    private var nearestBranch: Branch? = null
    
    // Cache cho địa chỉ đã geocode
    private val geocodeCache = mutableMapOf<String, LatLng?>()
    
    // Cache cho khoảng cách và thời gian
    private data class StoreDistanceInfo(val distanceText: String, val durationText: String, val distanceValue: Int)
    private val distanceCache = mutableMapOf<Int, StoreDistanceInfo>()
    
    // Permission launcher
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        
        if (fineLocationGranted || coarseLocationGranted) {
            checkGPSAndGetLocation()
        } else {
            Toast.makeText(context, "Cần quyền vị trí để tìm quán gần nhất", Toast.LENGTH_SHORT).show()
        }
    }
    
    // GPS settings launcher
    private val gpsSettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            getCurrentLocation()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!Places.isInitialized()) {
            Places.initialize(requireContext(), getString(R.string.google_maps_key))
        }
        locationHelper = LocationHelper(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return try {
            val view = inflater.inflate(R.layout.fragment_store, container, false)

            loadingDialog = LoadingDialog(requireContext())
            
            initViews(view)
            setupMap()
            setupListeners()
            setupRecyclerView()
            
            // Hiển thị prompt yêu cầu bật vị trí nếu chưa có quyền
            if (!locationHelper.hasLocationPermission()) {
                showLocationPermissionPrompt()
            }
            
            view
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreateView: ${e.message}")
            e.printStackTrace()
            Toast.makeText(context, "Lỗi tải trang cửa hàng", Toast.LENGTH_SHORT).show()
            inflater.inflate(R.layout.fragment_store, container, false)
        }
    }
    
    /**
     * Hiển thị dialog gợi ý bật vị trí để tìm quán gần nhất
     */
    private fun showLocationPermissionPrompt() {
        try {
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("📍 Bật định vị")
                .setMessage("Bật định vị để tìm quán gần bạn nhất và xem khoảng cách, thời gian di chuyển.")
                .setPositiveButton("Bật ngay") { _, _ ->
                    requestLocationAndFindNearest()
                }
                .setNegativeButton("Để sau", null)
                .show()
        } catch (e: Exception) {
            Log.e(TAG, "Error showing location prompt", e)
        }
    }

    private fun initViews(view: View) {
        searchView = view.findViewById(R.id.search_location)
        recyclerBranches = view.findViewById(R.id.recycler_branches)
        cardBranchInfo = view.findViewById(R.id.card_branch_info)
        tvBranchName = view.findViewById(R.id.tv_branch_name)
        tvBranchAddress = view.findViewById(R.id.tv_branch_address)
        tvDistance = view.findViewById(R.id.tv_distance)
        btnSetDefaultBranch = view.findViewById(R.id.btn_set_default_branch)
        progressBar = view.findViewById(R.id.progress_bar)
        
        // Init zoom buttons
        btnZoomIn = view.findViewById(R.id.btn_zoom_in)
        btnZoomOut = view.findViewById(R.id.btn_zoom_out)
        
        // Init location button
        btnMyLocation = view.findViewById(R.id.btn_my_location)
        
        // Init nearest store card (có thể null nếu chưa thêm vào layout)
        cardNearestStore = view.findViewById(R.id.card_nearest_store) ?: run {
            // Tạo view động nếu chưa có trong layout
            MaterialCardView(requireContext()).apply { visibility = View.GONE }
        }
        tvNearestStoreName = view.findViewById(R.id.tv_nearest_store_name) ?: TextView(requireContext())
        tvNearestStoreDistance = view.findViewById(R.id.tv_nearest_store_distance) ?: TextView(requireContext())
        tvNearestStoreDuration = view.findViewById(R.id.tv_nearest_store_duration) ?: TextView(requireContext())
        btnSelectNearestStore = view.findViewById(R.id.btn_select_nearest_store) ?: MaterialButton(requireContext())
        
        // Init scroll view và map container
        nestedScrollView = view.findViewById(R.id.nested_scroll_view)
        cardMapContainer = view.findViewById(R.id.card_map_container)
    }
    
    private fun setupRecyclerView() {
        recyclerBranches.layoutManager = LinearLayoutManager(context)
        // Pass click listener to adapter
        storeAdapter = StoreAdapter(requireContext(), stores) { clickedStore ->
            // Find the branch corresponding to the clicked store
            val clickedBranch = branches.find { it.id == clickedStore.id }
            if (clickedBranch != null) {
                onBranchListClick(clickedBranch)
            }
        }
        recyclerBranches.adapter = storeAdapter
    }

    private fun setupMap() {
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
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

        btnSetDefaultBranch.setOnClickListener {
            selectedBranch?.let { branch ->
                saveDefaultBranch(branch)
                Toast.makeText(context, "Đã chọn ${branch.branchName} làm chi nhánh mặc định", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Map Zoom Listeners
        btnZoomIn.setOnClickListener {
            map?.animateCamera(CameraUpdateFactory.zoomIn())
        }
        
        btnZoomOut.setOnClickListener {
            map?.animateCamera(CameraUpdateFactory.zoomOut())
        }
        
        // My Location Button
        btnMyLocation.setOnClickListener {
            requestLocationAndFindNearest()
        }
        
        // Nearest store selection
        btnSelectNearestStore.setOnClickListener {
            nearestBranch?.let { branch ->
                saveDefaultBranch(branch)
                Toast.makeText(context, "Đã chọn ${branch.branchName} làm chi nhánh mặc định", Toast.LENGTH_SHORT).show()
                onBranchListClick(branch)
            }
        }
    }
    
    /**
     * Yêu cầu quyền vị trí và tìm quán gần nhất
     */
    private fun requestLocationAndFindNearest() {
        if (locationHelper.hasLocationPermission()) {
            checkGPSAndGetLocation()
        } else {
            // Yêu cầu quyền
            locationPermissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }
    
    /**
     * Kiểm tra GPS và lấy vị trí
     */
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
    
    /**
     * Lấy vị trí hiện tại của user
     */
    private fun getCurrentLocation() {
        loadingDialog.show("Đang xác định vị trí của bạn...")
        
        locationHelper.getCurrentLocation(
            onSuccess = { location ->
                if (!isAdded) return@getCurrentLocation
                
                userLocation = LatLng(location.latitude, location.longitude)
                Log.d(TAG, "Got user location: ${location.latitude}, ${location.longitude}")
                
                loadingDialog.setMessage("Đang tìm quán gần nhất...")
                
                // Cập nhật map và tìm quán gần nhất
                displayBranchesOnMap()
                findNearestStore()
            },
            onError = { error ->
                if (!isAdded) return@getCurrentLocation
                loadingDialog.dismiss()
                
                // Hiển thị dialog cho phép retry
                showLocationErrorDialog(error)
            }
        )
    }
    
    /**
     * Hiển thị dialog lỗi với option retry
     */
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
    
    /**
     * Tìm quán gần nhất dựa trên vị trí user
     */
    private fun findNearestStore() {
        val userLoc = userLocation ?: run {
            loadingDialog.dismiss()
            return
        }
        
        if (branches.isEmpty()) {
            loadingDialog.dismiss()
            return
        }
        
        // Tính khoảng cách đến tất cả các quán bằng Distance Matrix API
        calculateAllDistances(userLoc)
    }
    
    /**
     * Tính khoảng cách đến tất cả các quán
     */
    private fun calculateAllDistances(userLoc: LatLng) {
        val originStr = "${userLoc.latitude},${userLoc.longitude}"
        
        // Tạo danh sách destinations
        val destinations = branches.mapNotNull { branch ->
            geocodeCache[branch.address ?: ""]?.let { loc ->
                "${loc.latitude},${loc.longitude}"
            }
        }
        
        if (destinations.isEmpty()) {
            loadingDialog.dismiss()
            Toast.makeText(context, "Không thể xác định vị trí các cửa hàng", Toast.LENGTH_SHORT).show()
            return
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
                    loadingDialog.dismiss()
                    
                    if (response.isSuccessful && response.body()?.status == "OK") {
                        val elements = response.body()?.rows?.getOrNull(0)?.elements ?: return
                        
                        var minDistance = Int.MAX_VALUE
                        var nearestIndex = -1
                        
                        // Tìm quán gần nhất
                        elements.forEachIndexed { index, element ->
                            if (element.status == "OK") {
                                val distanceValue = element.distance.value
                                
                                // Cache khoảng cách
                                branches.getOrNull(index)?.id?.let { branchId ->
                                    distanceCache[branchId] = StoreDistanceInfo(
                                        element.distance.text,
                                        element.duration.text,
                                        distanceValue
                                    )
                                }
                                
                                if (distanceValue < minDistance) {
                                    minDistance = distanceValue
                                    nearestIndex = index
                                }
                            }
                        }
                        
                        // Hiển thị quán gần nhất
                        if (nearestIndex >= 0 && nearestIndex < branches.size) {
                            nearestBranch = branches[nearestIndex]
                            showNearestStoreCard(nearestBranch!!, elements[nearestIndex])
                            
                            // Zoom đến quán gần nhất
                            geocodeCache[nearestBranch!!.address ?: ""]?.let { loc ->
                                map?.animateCamera(CameraUpdateFactory.newLatLngZoom(loc, 15f))
                            }
                        }
                        
                        // Cập nhật adapter với thông tin khoảng cách
                        updateStoreListWithDistances()
                    } else {
                        Toast.makeText(context, "Không thể tính khoảng cách", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<DistanceMatrixResponse>, t: Throwable) {
                    if (!isAdded) return
                    loadingDialog.dismiss()
                    Log.e(TAG, "Error calculating distances", t)
                    Toast.makeText(context, "Lỗi kết nối", Toast.LENGTH_SHORT).show()
                }
            })
    }
    
    /**
     * Hiển thị card gợi ý quán gần nhất
     */
    private fun showNearestStoreCard(branch: Branch, element: Element) {
        try {
            cardNearestStore.visibility = View.VISIBLE
            tvNearestStoreName.text = "📍 ${branch.branchName}"
            tvNearestStoreDistance.text = "Khoảng cách: ${element.distance.text}"
            tvNearestStoreDuration.text = "Thời gian: ${element.duration.text}"
        } catch (e: Exception) {
            Log.e(TAG, "Error showing nearest store card", e)
        }
    }
    
    /**
     * Cập nhật danh sách store với thông tin khoảng cách
     */
    private fun updateStoreListWithDistances() {
        // Sắp xếp stores theo khoảng cách
        val sortedStores = stores.sortedBy { store ->
            distanceCache[store.id]?.distanceValue ?: Int.MAX_VALUE
        }
        
        // Cập nhật adapter
        storeAdapter.updateStoresWithDistance(sortedStores, distanceCache.mapValues { 
            "${it.value.distanceText} - ${it.value.durationText}" 
        })
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        locationHelper.stopLocationUpdates()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        
        // Disable default zoom controls since we added custom ones
        map?.uiSettings?.isZoomControlsEnabled = false
        
        val hcmc = LatLng(10.7769, 106.7009)
        map?.moveCamera(CameraUpdateFactory.newLatLngZoom(hcmc, 12f))

        map?.setOnMarkerClickListener { marker ->
            val tag = marker.tag
            if (tag is Branch) {
                onBranchMarkerClick(tag)
            }
            false
        }
        
        map?.setOnMapClickListener {
             cardBranchInfo.visibility = View.GONE
             selectedBranch = null
        }

        loadBranches()
        
        // Tự động yêu cầu vị trí khi mở màn hình (nếu đã có quyền)
        if (locationHelper.hasLocationPermission()) {
            // Delay một chút để map load xong
            view?.postDelayed({
                if (isAdded && userLocation == null) {
                    requestLocationAndFindNearest()
                }
            }, 1500)
        }
    }

    private fun loadBranches() {
        // Kiểm tra cache trước
        val cachedStores = DataCache.stores
        if (!cachedStores.isNullOrEmpty()) {
            stores = cachedStores
            storeAdapter.updateStores(stores)
            
            branches = stores.map { store ->
                Branch(
                    id = store.id,
                    branchName = store.storeName,
                    address = store.address
                )
            }
            DataCache.branches = branches
            
            // Geocode nếu chưa có trong cache
            if (geocodeCache.isEmpty()) {
                loadingDialog.show("Đang xác định vị trí...")
                geocodeAllBranchesAsync()
            } else {
                displayBranchesOnMap()
            }
            return
        }
        
        loadingDialog.show("Đang tải cửa hàng...")
        
        val ctx = context ?: return
        
        RetrofitClient.getInstance(ctx).apiService.getStores().enqueue(object : Callback<ApiResponse<List<Store>>> {
            override fun onResponse(call: Call<ApiResponse<List<Store>>>, response: Response<ApiResponse<List<Store>>>) {
                if (!isAdded) return
                
                if (response.isSuccessful && response.body()?.success == true) {
                    stores = response.body()?.data ?: emptyList()
                    
                    storeAdapter.updateStores(stores)
                    
                    branches = stores.map { store ->
                        Branch(
                            id = store.id,
                            branchName = store.storeName,
                            address = store.address
                        )
                    }
                    DataCache.branches = branches
                    DataCache.stores = stores
                    
                    if (branches.isEmpty()) {
                        loadingDialog.dismiss()
                        Toast.makeText(context, "Không có cửa hàng nào", Toast.LENGTH_SHORT).show()
                    } else {
                        loadingDialog.setMessage("Đang xác định vị trí...")
                        geocodeAllBranchesAsync()
                    }
                } else {
                    loadingDialog.dismiss()
                    Log.e(TAG, "API response not successful: ${response.code()}")
                    Toast.makeText(context, "Không thể tải danh sách cửa hàng", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse<List<Store>>>, t: Throwable) {
                if (!isAdded) return
                loadingDialog.dismiss()
                Log.e(TAG, "Error fetching stores", t)
                Toast.makeText(context, "Lỗi kết nối server: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
    
    private fun geocodeAllBranchesAsync() {
        val ctx = context ?: return
        
        viewLifecycleOwner.lifecycleScope.launch {
            loadingDialog.setMessage("Đang xác định vị trí...")
            
            // Geocode tất cả địa chỉ trên IO thread
            withContext(Dispatchers.IO) {
                for (branch in branches) {
                    val address = branch.address ?: continue
                    if (!geocodeCache.containsKey(address)) {
                        geocodeCache[address] = getLatLngFromAddressSync(ctx, address)
                    }
                }
            }
            
            // Cập nhật UI trên main thread
            if (isAdded) {
                loadingDialog.dismiss()
                displayBranchesOnMap()
            }
        }
    }
    
    private fun getLatLngFromAddressSync(ctx: Context, strAddress: String): LatLng? {
        if (strAddress.isBlank()) return null
        
        try {
            val coder = Geocoder(ctx, Locale.getDefault())
            val addressList = coder.getFromLocationName(strAddress, 1)
            if (!addressList.isNullOrEmpty()) {
                val location = addressList[0]
                return LatLng(location.latitude, location.longitude)
            }
        } catch (e: IOException) {
            Log.e(TAG, "Geocode error for: $strAddress", e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error geocoding: $strAddress", e)
        }
        return null
    }

    private fun displayBranchesOnMap() {
        map?.let { googleMap ->
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
                // Sử dụng cache thay vì geocode lại
                val branchLocation = geocodeCache[branch.address ?: ""]
                
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
                }
            }
            
            if (hasBranches) {
                try {
                    userLocation?.let { boundsBuilder.include(it) }
                    val bounds = boundsBuilder.build()
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
                } catch (e: Exception) {
                    Log.e(TAG, "Error animating camera: ${e.message}")
                }
            } else if (userLocation != null) {
                 googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation!!, 15f))
            } else {
                val hcmc = LatLng(10.7769, 106.7009)
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(hcmc, 12f))
            }
        }
    }

    private fun searchAddress(address: String) {
        val ctx = context ?: return
        loadingDialog.show("Đang tìm kiếm...")
        
        viewLifecycleOwner.lifecycleScope.launch {
            val location = withContext(Dispatchers.IO) {
                getLatLngFromAddressSync(ctx, address)
            }
            
            if (!isAdded) return@launch
            loadingDialog.dismiss()
            
            if (location != null) {
                userLocation = location
                geocodeCache[address] = location
                displayBranchesOnMap()
                recalculateDistances()
            } else {
                Toast.makeText(context, "Không tìm thấy địa chỉ này", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getLatLngFromAddress(strAddress: String): LatLng? {
        // Kiểm tra cache trước
        if (geocodeCache.containsKey(strAddress)) {
            return geocodeCache[strAddress]
        }
        return null
    }

    private fun onBranchListClick(branch: Branch) {
        // Lấy từ cache
        val branchLocation = geocodeCache[branch.address ?: ""]
        if (branchLocation != null) {
            map?.animateCamera(CameraUpdateFactory.newLatLngZoom(branchLocation, 16f))
            onBranchMarkerClick(branch)
            // Scroll xuống bản đồ
            scrollToMap()
        } else {
            // Nếu chưa có trong cache, geocode async
            val ctx = context ?: return
            loadingDialog.show("Đang xác định vị trí...")
            viewLifecycleOwner.lifecycleScope.launch {
                val location = withContext(Dispatchers.IO) {
                    getLatLngFromAddressSync(ctx, branch.address ?: "")
                }
                
                if (!isAdded) return@launch
                loadingDialog.dismiss()
                
                if (location != null) {
                    geocodeCache[branch.address ?: ""] = location
                    map?.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 16f))
                    onBranchMarkerClick(branch)
                    // Scroll xuống bản đồ
                    scrollToMap()
                } else {
                    Toast.makeText(context, "Không xác định được vị trí cửa hàng này", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    /**
     * Scroll xuống phần bản đồ với animation mượt
     */
    private fun scrollToMap() {
        try {
            // Delay một chút để map animation hoàn thành
            nestedScrollView.postDelayed({
                // Scroll đến vị trí của map container
                nestedScrollView.smoothScrollTo(0, cardMapContainer.top - 50)
            }, 300)
        } catch (e: Exception) {
            Log.e(TAG, "Error scrolling to map", e)
        }
    }

    private fun onBranchMarkerClick(branch: Branch) {
        selectedBranch = branch
        tvBranchName.text = branch.branchName
        tvBranchAddress.text = branch.address
        
        if (userLocation != null) {
            val branchLoc = geocodeCache[branch.address ?: ""]
            if (branchLoc != null) {
                calculateDistanceWithMatrixApi(userLocation!!, branchLoc)
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
        selectedBranch?.let { branch ->
            onBranchMarkerClick(branch)
        }
    }

    private fun calculateDistanceWithMatrixApi(origin: LatLng, dest: LatLng) {
        val originStr = "${origin.latitude},${origin.longitude}"
        val destStr = "${dest.latitude},${dest.longitude}"
        val key = getString(R.string.google_maps_key)

        RetrofitClientMaps.instance.getDistance(originStr, destStr, key)
            .enqueue(object : Callback<DistanceMatrixResponse> {
                override fun onResponse(
                    call: Call<DistanceMatrixResponse>,
                    response: Response<DistanceMatrixResponse>
                ) {
                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body?.status == "OK" && body.rows.isNotEmpty()) {
                            val element = body.rows[0].elements.getOrNull(0)
                            if (element?.status == "OK") {
                                val distanceText = element.distance.text
                                val durationText = element.duration.text
                                
                                tvDistance.text = "Khoảng cách: $distanceText ($durationText)"
                                tvDistance.visibility = View.VISIBLE
                                
                                // Update address text with more info
                                tvBranchAddress.text = "${selectedBranch?.address}\n(Cách bạn $distanceText - Đi khoảng $durationText)"
                            } else {
                                tvDistance.text = "Không thể tính khoảng cách"
                            }
                        }
                    } else {
                        Log.e(TAG, "Matrix API Error: ${response.code()}")
                        tvDistance.text = "Lỗi tính khoảng cách"
                    }
                }

                override fun onFailure(call: Call<DistanceMatrixResponse>, t: Throwable) {
                    Log.e(TAG, "Matrix API Failure", t)
                    tvDistance.text = "Lỗi kết nối"
                }
            })
    }

    private fun saveDefaultBranch(branch: Branch) {
        val prefs = requireContext().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putLong("DEFAULT_BRANCH_ID", branch.id?.toLong() ?: -1)
            putString("DEFAULT_BRANCH_NAME", branch.branchName)
            apply()
        }
    }

    companion object {
        private const val TAG = "StoreFragment"
    }
}
