package com.example.doan.Fragments.Manager

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.doan.Activities.LiveChatActivity
import com.example.doan.Adapters.StoreWithManagersAdapter
import com.example.doan.Models.ApiResponse
import com.example.doan.Models.StoreWithManagers
import com.example.doan.Models.User
import com.example.doan.Network.RetrofitClient
import com.example.doan.R
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ManageStoresFragment : Fragment() {

    private lateinit var rvStores: RecyclerView
    private lateinit var adapter: StoreWithManagersAdapter
    private val storeList = mutableListOf<StoreWithManagers>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_manage_stores, container, false)

        rvStores = view.findViewById(R.id.rv_stores)
        rvStores.layoutManager = LinearLayoutManager(context)

        adapter = StoreWithManagersAdapter(
            requireContext(), 
            storeList,
            onContactClick = { user, store ->
                openLiveChat(user, store)
            }
        )
        rvStores.adapter = adapter

        loadStoresWithManagers()

        return view
    }
    
    private fun openLiveChat(user: User, store: StoreWithManagers) {
        val intent = Intent(requireContext(), LiveChatActivity::class.java).apply {
            putExtra("store_id", store.id.toLong())
            putExtra("store_name", store.storeName)
        }
        startActivity(intent)
        Toast.makeText(context, "Đang mở chat hỗ trợ tại ${store.storeName}", Toast.LENGTH_SHORT).show()
    }

    private fun loadStoresWithManagers() {
        RetrofitClient.getInstance(requireContext()).apiService.getStoresWithManagers()
            .enqueue(object : Callback<ApiResponse<List<StoreWithManagers>>> {
                override fun onResponse(
                    call: Call<ApiResponse<List<StoreWithManagers>>>,
                    response: Response<ApiResponse<List<StoreWithManagers>>>
                ) {
                    if (!isAdded) return
                    
                    if (response.isSuccessful && response.body()?.success == true) {
                        response.body()?.data?.let { stores ->
                            storeList.clear()
                            storeList.addAll(stores)
                            adapter.updateStores(storeList)
                        }
                    } else {
                        Toast.makeText(context, "Không thể tải danh sách cửa hàng", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<List<StoreWithManagers>>>, t: Throwable) {
                    if (!isAdded) return
                    Log.e("ManageStores", "Error: ${t.message}")
                    Toast.makeText(context, "Lỗi kết nối: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
}
