package com.example.doan.Fragments.Manager

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.doan.Adapters.ManagerDrinkAdapter
import com.example.doan.Models.ApiResponse
import com.example.doan.Models.Drink
import com.example.doan.Network.RetrofitClient
import com.example.doan.R
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ManageDrinksFragment : Fragment() {

    private lateinit var rvDrinks: RecyclerView
    private lateinit var adapter: ManagerDrinkAdapter
    private val drinkList = mutableListOf<Drink>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_manage_drinks, container, false)

        rvDrinks = view.findViewById(R.id.rv_drinks)
        rvDrinks.layoutManager = LinearLayoutManager(context)

        adapter = ManagerDrinkAdapter(requireContext(), drinkList, object : ManagerDrinkAdapter.OnDrinkActionListener {
            override fun onEditClick(drink: Drink) {
                Toast.makeText(context, "Chỉnh sửa: ${drink.name}", Toast.LENGTH_SHORT).show()
            }

            override fun onDeleteClick(drink: Drink) {
                Toast.makeText(context, "Xóa: ${drink.name}", Toast.LENGTH_SHORT).show()
            }
        })
        rvDrinks.adapter = adapter

        loadDrinks()

        return view
    }

    private fun loadDrinks() {
        RetrofitClient.getInstance(requireContext()).apiService.getDrinks()
            .enqueue(object : Callback<ApiResponse<List<Drink>>> {
                override fun onResponse(
                    call: Call<ApiResponse<List<Drink>>>,
                    response: Response<ApiResponse<List<Drink>>>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        response.body()?.data?.let { drinks ->
                            drinkList.clear()
                            drinkList.addAll(drinks)
                            adapter.updateList(drinkList)
                        }
                    } else {
                        Toast.makeText(context, "Không thể tải danh sách đồ uống", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<List<Drink>>>, t: Throwable) {
                    Toast.makeText(context, "Lỗi kết nối: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
}
