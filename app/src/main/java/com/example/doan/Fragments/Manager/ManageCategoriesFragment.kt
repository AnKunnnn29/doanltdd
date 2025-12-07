package com.example.doan.Fragments.Manager

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.doan.Adapters.ManagerCategoryAdapter
import com.example.doan.Models.ApiResponse
import com.example.doan.Models.Category
import com.example.doan.Network.RetrofitClient
import com.example.doan.R
import com.google.android.material.button.MaterialButton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ManageCategoriesFragment : Fragment() {

    private lateinit var rvCategories: RecyclerView
    private lateinit var adapter: ManagerCategoryAdapter
    private lateinit var btnAddCategory: MaterialButton
    private lateinit var progressBar: ProgressBar
    private val categoryList = mutableListOf<Category>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_manage_categories, container, false)

        rvCategories = view.findViewById(R.id.rv_categories)
        btnAddCategory = view.findViewById(R.id.btn_add_category)
        progressBar = view.findViewById(R.id.progress_bar)
        
        rvCategories.layoutManager = LinearLayoutManager(context)

        adapter = ManagerCategoryAdapter(requireContext(), categoryList, object : ManagerCategoryAdapter.OnCategoryActionListener {
            override fun onEditClick(category: Category) {
                showEditCategoryDialog(category)
            }

            override fun onDeleteClick(category: Category) {
                showDeleteConfirmDialog(category)
            }
        })
        rvCategories.adapter = adapter

        btnAddCategory.setOnClickListener {
            showAddCategoryDialog()
        }

        loadCategories()

        return view
    }

    private fun loadCategories() {
        showLoading(true)
        RetrofitClient.getInstance(requireContext()).apiService.getCategories()
            .enqueue(object : Callback<ApiResponse<List<Category>>> {
                override fun onResponse(
                    call: Call<ApiResponse<List<Category>>>,
                    response: Response<ApiResponse<List<Category>>>
                ) {
                    showLoading(false)
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
                    showLoading(false)
                    Toast.makeText(context, "Lỗi kết nối: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun showAddCategoryDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_edit_category, null)
        val editName = dialogView.findViewById<EditText>(R.id.edit_category_name)
        val editDescription = dialogView.findViewById<EditText>(R.id.edit_category_description)
        val editImageUrl = dialogView.findViewById<EditText>(R.id.edit_category_image_url)

        AlertDialog.Builder(requireContext())
            .setTitle("Thêm Danh Mục Mới")
            .setView(dialogView)
            .setPositiveButton("Thêm") { _, _ ->
                val name = editName.text.toString().trim()
                val description = editDescription.text.toString().trim()
                val imageUrl = editImageUrl.text.toString().trim()

                if (name.isEmpty()) {
                    Toast.makeText(context, "Vui lòng nhập tên danh mục", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                addCategory(name, description, imageUrl)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun showEditCategoryDialog(category: Category) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_edit_category, null)
        val editName = dialogView.findViewById<EditText>(R.id.edit_category_name)
        val editDescription = dialogView.findViewById<EditText>(R.id.edit_category_description)
        val editImageUrl = dialogView.findViewById<EditText>(R.id.edit_category_image_url)

        editName.setText(category.name)
        editDescription.setText(category.description)
        editImageUrl.setText(category.image)

        AlertDialog.Builder(requireContext())
            .setTitle("Chỉnh Sửa Danh Mục")
            .setView(dialogView)
            .setPositiveButton("Lưu") { _, _ ->
                val name = editName.text.toString().trim()
                val description = editDescription.text.toString().trim()
                val imageUrl = editImageUrl.text.toString().trim()

                if (name.isEmpty()) {
                    Toast.makeText(context, "Vui lòng nhập tên danh mục", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                updateCategory(category.id, name, description, imageUrl)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun showDeleteConfirmDialog(category: Category) {
        AlertDialog.Builder(requireContext())
            .setTitle("Xác nhận xóa")
            .setMessage("Bạn có chắc chắn muốn xóa danh mục '${category.name}'?")
            .setPositiveButton("Xóa") { _, _ ->
                deleteCategory(category)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun addCategory(name: String, description: String, imageUrl: String) {
        showLoading(true)
        val categoryData = mapOf(
            "name" to name,
            "description" to description,
            "imageUrl" to imageUrl
        )

        RetrofitClient.getInstance(requireContext()).apiService.createCategory(categoryData)
            .enqueue(object : Callback<ApiResponse<Category>> {
                override fun onResponse(
                    call: Call<ApiResponse<Category>>,
                    response: Response<ApiResponse<Category>>
                ) {
                    showLoading(false)
                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(context, "Đã thêm danh mục mới", Toast.LENGTH_SHORT).show()
                        loadCategories()
                    } else {
                        Toast.makeText(context, "Không thể thêm danh mục", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<Category>>, t: Throwable) {
                    showLoading(false)
                    Toast.makeText(context, "Lỗi: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun updateCategory(id: Int, name: String, description: String, imageUrl: String) {
        showLoading(true)
        val categoryData = mapOf(
            "name" to name,
            "description" to description,
            "imageUrl" to imageUrl
        )

        RetrofitClient.getInstance(requireContext()).apiService.updateCategory(id.toLong(), categoryData)
            .enqueue(object : Callback<ApiResponse<Category>> {
                override fun onResponse(
                    call: Call<ApiResponse<Category>>,
                    response: Response<ApiResponse<Category>>
                ) {
                    showLoading(false)
                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(context, "Đã cập nhật danh mục", Toast.LENGTH_SHORT).show()
                        loadCategories()
                    } else {
                        Toast.makeText(context, "Không thể cập nhật danh mục", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<Category>>, t: Throwable) {
                    showLoading(false)
                    Toast.makeText(context, "Lỗi: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun deleteCategory(category: Category) {
        showLoading(true)
        RetrofitClient.getInstance(requireContext()).apiService.deleteCategory(category.id.toLong())
            .enqueue(object : Callback<ApiResponse<String>> {
                override fun onResponse(
                    call: Call<ApiResponse<String>>,
                    response: Response<ApiResponse<String>>
                ) {
                    showLoading(false)
                    if (response.isSuccessful) {
                        Toast.makeText(context, "Đã xóa danh mục '${category.name}'", Toast.LENGTH_SHORT).show()
                        loadCategories()
                    } else {
                        Toast.makeText(context, "Không thể xóa danh mục này", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<String>>, t: Throwable) {
                    showLoading(false)
                    Toast.makeText(context, "Lỗi: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }
}
