package com.example.doan.Fragments.Manager

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.doan.Adapters.StoreAdapter
import com.example.doan.Models.ApiResponse
import com.example.doan.Models.Store
import com.example.doan.Network.RetrofitClient
import com.example.doan.R
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ManageStoresFragment : Fragment() {

    private lateinit var rvStores: RecyclerView
    private lateinit var adapter: StoreAdapter
    private val storeList = mutableListOf<Store>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_manage_stores, container, false)

        rvStores = view.findViewById(R.id.rv_stores)
        rvStores.layoutManager = LinearLayoutManager(context)

        adapter = StoreAdapter(requireContext(), storeList)
        rvStores.adapter = adapter

        loadStores()

        return view
    }

    private fun loadStores() {
        RetrofitClient.getInstance(requireContext()).apiService.getStores()
            .enqueue(object : Callback<ApiResponse<List<Store>>> {
                override fun onResponse(
                    call: Call<ApiResponse<List<Store>>>,
                    response: Response<ApiResponse<List<Store>>>
                ) {
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

                override fun onFailure(call: Call<ApiResponse<List<Store>>>, t: Throwable) {
                    Toast.makeText(context, "Lỗi kết nối: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
}
