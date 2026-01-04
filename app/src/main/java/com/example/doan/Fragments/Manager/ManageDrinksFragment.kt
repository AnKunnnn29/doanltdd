package com.example.doan.Fragments.Manager

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.doan.Activities.AddEditCategoryActivity
import com.example.doan.Activities.AddEditDrinkActivity
import com.example.doan.Adapters.ManagerCategoryAdapter
import com.example.doan.Adapters.ManagerDrinkAdapter
import com.example.doan.Models.ApiResponse
import com.example.doan.Models.Category
import com.example.doan.Models.Drink
import com.example.doan.Network.RetrofitClient
import com.example.doan.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.tabs.TabLayout
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ManageDrinksFragment : Fragment() {

    private lateinit var rvDrinks: RecyclerView
    private lateinit var rvCategories: RecyclerView
    private lateinit var adapter: ManagerDrinkAdapter
    private lateinit var categoryAdapter: ManagerCategoryAdapter
    private lateinit var btnAdd: MaterialButton
    private lateinit var tvHeaderTitle: TextView
    private lateinit var editSearch: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyState: View
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var swipeRefreshCategories: SwipeRefreshLayout
    private lateinit var searchCard: MaterialCardView
    private lateinit var headerLayout: View
    private lateinit var tabLayout: TabLayout
    private lateinit var statsLayout: View
    
    private val drinkList = mutableListOf<Drink>()
    private val categoryList = mutableListOf<Category>()
    private var currentTab = 0 // 0 = Drinks, 1 = Categories

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_manage_drinks, container, false)

        initViews(view)
        setupTabs()
        setupDrinksRecyclerView()
        setupCategoriesRecyclerView()
        setupSearch()
        setupAddButton()
        setupSwipeRefresh()
        animateViewsIn()
        loadDrinks()

        return view
    }

    private fun initViews(view: View) {
        rvDrinks = view.findViewById(R.id.rv_drinks)
        rvCategories = view.findViewById(R.id.rv_categories)
        btnAdd = view.findViewById(R.id.btn_add_drink)
        tvHeaderTitle = view.findViewById(R.id.tv_header_title)
        editSearch = view.findViewById(R.id.edit_search)
        progressBar = view.findViewById(R.id.progress_bar)
        emptyState = view.findViewById(R.id.empty_state)
        swipeRefresh = view.findViewById(R.id.swipe_refresh)
        swipeRefreshCategories = view.findViewById(R.id.swipe_refresh_categories)
        searchCard = view.findViewById(R.id.search_card)
        headerLayout = view.findViewById(R.id.header_layout)
        tabLayout = view.findViewById(R.id.tab_layout)
        statsLayout = view.findViewById(R.id.stats_layout)
    }

    private fun setupTabs() {
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentTab = tab?.position ?: 0
                switchTab(currentTab)
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun switchTab(tabIndex: Int) {
        when (tabIndex) {
            0 -> {
                // Drinks tab
                swipeRefresh.visibility = View.VISIBLE
                swipeRefreshCategories.visibility = View.GONE
                statsLayout.visibility = View.VISIBLE
                btnAdd.text = "Thêm"
                editSearch.hint = "Tìm kiếm món..."
                loadDrinks()
            }
            1 -> {
                // Categories tab
                swipeRefresh.visibility = View.GONE
                swipeRefreshCategories.visibility = View.VISIBLE
                statsLayout.visibility = View.GONE
                btnAdd.text = "Thêm"
                editSearch.hint = "Tìm kiếm danh mục..."
                loadCategories()
            }
        }
    }

    private fun setupDrinksRecyclerView() {
        rvDrinks.layoutManager = LinearLayoutManager(context)
        rvDrinks.layoutAnimation = AnimationUtils.loadLayoutAnimation(context, R.anim.layout_animation_fall_down)
        adapter = ManagerDrinkAdapter(requireContext(), drinkList, object : ManagerDrinkAdapter.OnDrinkActionListener {
            override fun onEditClick(drink: Drink) {
                openEditDrink(drink)
            }
            override fun onDeleteClick(drink: Drink) {
                showDeleteDrinkDialog(drink)
            }
        })
        rvDrinks.adapter = adapter
    }

    private fun setupCategoriesRecyclerView() {
        rvCategories.layoutManager = LinearLayoutManager(context)
        rvCategories.layoutAnimation = AnimationUtils.loadLayoutAnimation(context, R.anim.layout_animation_fall_down)
        categoryAdapter = ManagerCategoryAdapter(requireContext(), categoryList, object : ManagerCategoryAdapter.OnCategoryActionListener {
            override fun onEditClick(category: Category) {
                openEditCategory(category)
            }
            override fun onDeleteClick(category: Category) {
                showDeleteCategoryDialog(category)
            }
        })
        rvCategories.adapter = categoryAdapter
    }

    private fun setupSearch() {
        editSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (currentTab == 0) {
                    adapter.filter(s.toString())
                } else {
                    categoryAdapter.filter(s.toString())
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupAddButton() {
        btnAdd.setOnClickListener {
            if (currentTab == 0) {
                openAddDrink()
            } else {
                openAddCategory()
            }
        }
    }

    private fun setupSwipeRefresh() {
        swipeRefresh.setColorSchemeResources(R.color.wine_primary)
        swipeRefresh.setOnRefreshListener { loadDrinks() }
        
        swipeRefreshCategories.setColorSchemeResources(R.color.wine_primary)
        swipeRefreshCategories.setOnRefreshListener { loadCategories() }
    }

    private fun animateViewsIn() {
        headerLayout.alpha = 0f
        searchCard.alpha = 0f
        headerLayout.translationY = -30f
        searchCard.translationY = -30f

        headerLayout.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(350)
            .setInterpolator(DecelerateInterpolator())
            .start()

        searchCard.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(350)
            .setStartDelay(100)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    override fun onResume() {
        super.onResume()
        if (currentTab == 0) {
            loadDrinks()
        } else {
            loadCategories()
        }
    }

    private fun loadDrinks() {
        showLoading(true)
        RetrofitClient.getInstance(requireContext()).apiService.getDrinks()
            .enqueue(object : Callback<ApiResponse<List<Drink>>> {
                override fun onResponse(
                    call: Call<ApiResponse<List<Drink>>>,
                    response: Response<ApiResponse<List<Drink>>>
                ) {
                    showLoading(false)
                    swipeRefresh.isRefreshing = false
                    if (response.isSuccessful && response.body()?.success == true) {
                        response.body()?.data?.let { drinks ->
                            drinkList.clear()
                            drinkList.addAll(drinks)
                            adapter.updateList(drinkList)
                            updateEmptyState()
                            rvDrinks.scheduleLayoutAnimation()
                        }
                    } else {
                        Toast.makeText(context, "Không thể tải danh sách đồ uống", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<List<Drink>>>, t: Throwable) {
                    showLoading(false)
                    swipeRefresh.isRefreshing = false
                    Toast.makeText(context, "Lỗi kết nối: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
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
                    swipeRefreshCategories.isRefreshing = false
                    if (response.isSuccessful && response.body()?.success == true) {
                        response.body()?.data?.let { categories ->
                            categoryList.clear()
                            categoryList.addAll(categories)
                            categoryAdapter.updateList(categoryList)
                            updateEmptyState()
                            rvCategories.scheduleLayoutAnimation()
                        }
                    } else {
                        Toast.makeText(context, "Không thể tải danh sách danh mục", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<List<Category>>>, t: Throwable) {
                    showLoading(false)
                    swipeRefreshCategories.isRefreshing = false
                    Toast.makeText(context, "Lỗi kết nối: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun openAddDrink() {
        val intent = Intent(requireContext(), AddEditDrinkActivity::class.java)
        startActivity(intent)
    }

    private fun openEditDrink(drink: Drink) {
        val intent = Intent(requireContext(), AddEditDrinkActivity::class.java)
        intent.putExtra("DRINK_ID", drink.id)
        intent.putExtra("DRINK_DATA", drink)
        startActivity(intent)
    }

    private fun openAddCategory() {
        val intent = Intent(requireContext(), AddEditCategoryActivity::class.java)
        startActivity(intent)
    }

    private fun openEditCategory(category: Category) {
        val intent = Intent(requireContext(), AddEditCategoryActivity::class.java)
        intent.putExtra("CATEGORY_ID", category.id)
        intent.putExtra("CATEGORY_NAME", category.name)
        intent.putExtra("CATEGORY_DESCRIPTION", category.description)
        intent.putExtra("CATEGORY_IMAGE", category.image)
        startActivity(intent)
    }

    private fun showDeleteDrinkDialog(drink: Drink) {
        AlertDialog.Builder(requireContext())
            .setTitle("Xác nhận xóa")
            .setMessage("Bạn có chắc chắn muốn xóa món '${drink.name}'?")
            .setPositiveButton("Xóa") { _, _ ->
                deleteDrink(drink)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun showDeleteCategoryDialog(category: Category) {
        AlertDialog.Builder(requireContext())
            .setTitle("Xác nhận xóa")
            .setMessage("Bạn có chắc chắn muốn xóa danh mục '${category.name}'?")
            .setPositiveButton("Xóa") { _, _ ->
                deleteCategory(category)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun deleteDrink(drink: Drink) {
        showLoading(true)
        RetrofitClient.getInstance(requireContext()).apiService.deleteDrink(drink.id.toLong())
            .enqueue(object : Callback<ApiResponse<Void>> {
                override fun onResponse(
                    call: Call<ApiResponse<Void>>,
                    response: Response<ApiResponse<Void>>
                ) {
                    showLoading(false)
                    if (response.isSuccessful) {
                        Toast.makeText(context, "Đã xóa món '${drink.name}'", Toast.LENGTH_SHORT).show()
                        loadDrinks() // Reload list
                    } else {
                        Toast.makeText(context, "Không thể xóa món này", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<Void>>, t: Throwable) {
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
        if (currentTab == 0) {
            rvDrinks.visibility = if (show) View.GONE else View.VISIBLE
        } else {
            rvCategories.visibility = if (show) View.GONE else View.VISIBLE
        }
    }

    private fun updateEmptyState() {
        val isEmpty = if (currentTab == 0) drinkList.isEmpty() else categoryList.isEmpty()
        if (isEmpty) {
            emptyState.visibility = View.VISIBLE
        } else {
            emptyState.visibility = View.GONE
        }
    }
}
