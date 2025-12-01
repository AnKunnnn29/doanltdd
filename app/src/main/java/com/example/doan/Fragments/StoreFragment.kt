package com.example.doan.Fragments

import android.os.Bundle
import android.util.Log
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

class StoreFragment : Fragment() {

    private lateinit var storesRecyclerView: RecyclerView
    private lateinit var storeAdapter: StoreAdapter
    private var storeList = mutableListOf<Store>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_store, container, false)

        storesRecyclerView = view.findViewById(R.id.stores_recycler_view)
        storesRecyclerView.layoutManager = LinearLayoutManager(context)

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
                    if (response.isSuccessful && response.body() != null) {
                        val apiResponse = response.body()!!
                        if (apiResponse.success && apiResponse.data != null) {
                            storeList = apiResponse.data!!.toMutableList()
                            storeAdapter = StoreAdapter(requireContext(), storeList)
                            storesRecyclerView.adapter = storeAdapter
                        } else {
                            val message = apiResponse.message ?: "Lỗi tải dữ liệu cửa hàng"
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Lỗi tải dữ liệu: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<List<Store>>>, t: Throwable) {
                    Log.e("StoreFragment", "Lỗi kết nối API: ${t.message}")
                    Toast.makeText(context, "Không thể kết nối đến máy chủ", Toast.LENGTH_LONG).show()
                }
            })
    }
}
