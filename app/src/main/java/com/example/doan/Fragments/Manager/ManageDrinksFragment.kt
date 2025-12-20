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
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.doan.Activities.AddEditDrinkActivity
import com.example.doan.Adapters.ManagerDrinkAdapter
import com.example.doan.Models.ApiResponse
import com.example.doan.Models.Drink
import com.example.doan.Network.RetrofitClient
import com.example.doan.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ManageDrinksFragment : Fragment() {

    private lateinit var rvDrinks: RecyclerView
    private lateinit var adapter: ManagerDrinkAdapter
    private lateinit var btnAddDrink: MaterialButton
    private lateinit var editSearch: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyState: View
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var searchCard: MaterialCardView
    private lateinit var headerLayout: View
    private val drinkList = mutableListOf<Drink>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_manage_drinks, container, false)

        // Initialize views
        rvDrinks = view.findViewById(R.id.rv_drinks)
        btnAddDrink = view.findViewById(R.id.btn_add_drink)
        editSearch = view.findViewById(R.id.edit_search)
        progressBar = view.findViewById(R.id.progress_bar)
        emptyState = view.findViewById(R.id.empty_state)
        swipeRefresh = view.findViewById(R.id.swipe_refresh)
        searchCard = view.findViewById(R.id.search_card)
        headerLayout = view.findViewById(R.id.header_layout)

        // Setup RecyclerView with animation
        rvDrinks.layoutManager = LinearLayoutManager(context)
        rvDrinks.layoutAnimation = AnimationUtils.loadLayoutAnimation(context, R.anim.layout_animation_fall_down)
        adapter = ManagerDrinkAdapter(requireContext(), drinkList, object : ManagerDrinkAdapter.OnDrinkActionListener {
            override fun onEditClick(drink: Drink) {
                openEditDrink(drink)
            }

            override fun onDeleteClick(drink: Drink) {
                showDeleteConfirmDialog(drink)
            }
        })
        rvDrinks.adapter = adapter

        // Setup search
        editSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                adapter.filter(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Setup add button
        btnAddDrink.setOnClickListener {
            openAddDrink()
        }

        // Setup swipe refresh
        swipeRefresh.setColorSchemeResources(R.color.wine_primary)
        swipeRefresh.setOnRefreshListener {
            loadDrinks()
        }

        // Animate header and search
        animateViewsIn()

        loadDrinks()

        return view
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
        // Reload drinks when returning to this fragment
        loadDrinks()
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

    private fun showDeleteConfirmDialog(drink: Drink) {
        AlertDialog.Builder(requireContext())
            .setTitle("Xác nhận xóa")
            .setMessage("Bạn có chắc chắn muốn xóa món '${drink.name}'?")
            .setPositiveButton("Xóa") { _, _ ->
                deleteDrink(drink)
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

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        rvDrinks.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun updateEmptyState() {
        if (drinkList.isEmpty()) {
            emptyState.visibility = View.VISIBLE
            rvDrinks.visibility = View.GONE
        } else {
            emptyState.visibility = View.GONE
            rvDrinks.visibility = View.VISIBLE
        }
    }
}
