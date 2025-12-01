package com.example.doan.Fragments.Manager

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.doan.Adapters.ManagerCategoryAdapter
import com.example.doan.Models.ApiResponse
import com.example.doan.Models.Category
import com.example.doan.Network.RetrofitClient
import com.example.doan.R
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ManageCategoriesFragment : Fragment() {

    private lateinit var rvCategories: RecyclerView
    private lateinit var adapter: ManagerCategoryAdapter
    private val categoryList = mutableListOf<Category>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_manage_categories, container, false)

        rvCategories = view.findViewById(R.id.rv_categories)
        rvCategories.layoutManager = LinearLayoutManager(context)

        adapter = ManagerCategoryAdapter(requireContext(), categoryList, object : ManagerCategoryAdapter.OnCategoryActionListener {
            override fun onEditClick(category: Category) {
                Toast.makeText(context, "Chỉnh sửa: ${category.name}", Toast.LENGTH_SHORT).show()
            }

            override fun onDeleteClick(category: Category) {
                Toast.makeText(context, "Xóa: ${category.name}", Toast.LENGTH_SHORT).show()
            }
        })
        rvCategories.adapter = adapter

        loadCategories()

        return view
    }

    private fun loadCategories() {
        RetrofitClient.getInstance(requireContext()).apiService.getCategories()
            .enqueue(object : Callback<ApiResponse<List<Category>>> {
                override fun onResponse(
                    call: Call<ApiResponse<List<Category>>>,
                    response: Response<ApiResponse<List<Category>>>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        response.body()?.data?.let { categories ->
                            categoryList.clear()
                            categoryList.addAll(categories)
                            adapter.updateList(categoryList)
                        }
                    } else {
                        Toast.makeText(context, "Không thể tải danh sách danh mục", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<List<Category>>>, t: Throwable) {
                    Toast.makeText(context, "Lỗi kết nối: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
}
