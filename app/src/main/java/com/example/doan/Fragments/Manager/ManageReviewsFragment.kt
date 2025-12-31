package com.example.doan.Fragments.Manager

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.doan.Adapters.ReviewManagementAdapter
import com.example.doan.Models.ApiResponse
import com.example.doan.Models.PageResponse
import com.example.doan.Models.ReviewManagement
import com.example.doan.Network.RetrofitClient
import com.example.doan.R
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ManageReviewsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var chipGroup: ChipGroup
    private lateinit var chipAll: Chip
    private lateinit var chipBackup: Chip
    private lateinit var tvTotalReviews: TextView
    
    private lateinit var adapter: ReviewManagementAdapter
    private var currentPage = 0
    private var isLoading = false
    private var hasMoreData = true
    private var showBackupOnly = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_manage_reviews, container, false)
        
        initViews(view)
        setupRecyclerView()
        setupChipGroup()
        setupSwipeRefresh()
        loadReviews(refresh = true)
        
        return view
    }
    
    private fun initViews(view: View) {
        recyclerView = view.findViewById(R.id.rv_reviews)
        progressBar = view.findViewById(R.id.progress_bar)
        tvEmpty = view.findViewById(R.id.tv_empty)
        swipeRefresh = view.findViewById(R.id.swipe_refresh)
        chipGroup = view.findViewById(R.id.chip_group_filter)
        chipAll = view.findViewById(R.id.chip_all)
        chipBackup = view.findViewById(R.id.chip_backup)
        tvTotalReviews = view.findViewById(R.id.tv_total_reviews)
    }
    
    private fun setupRecyclerView() {
        adapter = ReviewManagementAdapter(
            onDeleteClick = { review -> showDeleteConfirmation(review) }
        )
        
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
        
        // Pagination
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                
                if (!isLoading && hasMoreData) {
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 5
                        && firstVisibleItemPosition >= 0) {
                        loadReviews(refresh = false)
                    }
                }
            }
        })
    }
    
    private fun setupChipGroup() {
        chipAll.isChecked = true
        
        chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            showBackupOnly = checkedIds.contains(R.id.chip_backup)
            loadReviews(refresh = true)
        }
    }
    
    private fun setupSwipeRefresh() {
        swipeRefresh.setColorSchemeResources(R.color.wine_primary)
        swipeRefresh.setOnRefreshListener {
            loadReviews(refresh = true)
        }
    }
    
    private fun loadReviews(refresh: Boolean) {
        if (isLoading) return
        
        if (refresh) {
            currentPage = 0
            hasMoreData = true
            adapter.clearData()
        }
        
        isLoading = true
        if (currentPage == 0) {
            progressBar.visibility = View.VISIBLE
        }
        
        val call = if (showBackupOnly) {
            RetrofitClient.getInstance(requireContext()).apiService
                .getBackupReviews(currentPage, 20)
        } else {
            RetrofitClient.getInstance(requireContext()).apiService
                .getManagerReviews(currentPage, 20)
        }
        
        call.enqueue(object : Callback<ApiResponse<PageResponse<ReviewManagement>>> {
            override fun onResponse(
                call: Call<ApiResponse<PageResponse<ReviewManagement>>>,
                response: Response<ApiResponse<PageResponse<ReviewManagement>>>
            ) {
                if (!isAdded) return
                
                isLoading = false
                progressBar.visibility = View.GONE
                swipeRefresh.isRefreshing = false
                
                if (response.isSuccessful && response.body()?.success == true) {
                    val pageData = response.body()?.data
                    val reviews = pageData?.content ?: emptyList()
                    
                    if (refresh) {
                        adapter.setData(reviews)
                    } else {
                        adapter.addData(reviews)
                    }
                    
                    hasMoreData = !(pageData?.isLast ?: true)
                    currentPage++
                    
                    // Update total count
                    val total = pageData?.totalElements ?: 0
                    tvTotalReviews.text = "Tổng: $total đánh giá"
                    
                    updateEmptyState()
                } else {
                    Toast.makeText(context, "Không thể tải đánh giá", Toast.LENGTH_SHORT).show()
                    updateEmptyState()
                }
            }

            override fun onFailure(call: Call<ApiResponse<PageResponse<ReviewManagement>>>, t: Throwable) {
                if (!isAdded) return
                
                isLoading = false
                progressBar.visibility = View.GONE
                swipeRefresh.isRefreshing = false
                
                Log.e(TAG, "Error loading reviews: ${t.message}")
                Toast.makeText(context, "Lỗi kết nối", Toast.LENGTH_SHORT).show()
                updateEmptyState()
            }
        })
    }
    
    private fun updateEmptyState() {
        if (adapter.itemCount == 0) {
            tvEmpty.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
            tvEmpty.text = if (showBackupOnly) {
                "Chưa có đánh giá backup nào"
            } else {
                "Chưa có đánh giá nào"
            }
        } else {
            tvEmpty.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }
    
    private fun showDeleteConfirmation(review: ReviewManagement) {
        // Không cho xóa backup reviews
        if (review.isFromDeletedUser) {
            Toast.makeText(context, "Không thể xóa đánh giá backup", Toast.LENGTH_SHORT).show()
            return
        }
        
        AlertDialog.Builder(requireContext())
            .setTitle("Xóa đánh giá")
            .setMessage("Bạn có chắc muốn xóa đánh giá này của ${review.userFullName}?")
            .setPositiveButton("Xóa") { _, _ ->
                deleteReview(review.id)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }
    
    private fun deleteReview(reviewId: Long) {
        progressBar.visibility = View.VISIBLE
        
        RetrofitClient.getInstance(requireContext()).apiService
            .deleteReviewByAdmin(reviewId)
            .enqueue(object : Callback<ApiResponse<String>> {
                override fun onResponse(
                    call: Call<ApiResponse<String>>,
                    response: Response<ApiResponse<String>>
                ) {
                    if (!isAdded) return
                    
                    progressBar.visibility = View.GONE
                    
                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(context, "Đã xóa đánh giá", Toast.LENGTH_SHORT).show()
                        loadReviews(refresh = true)
                    } else {
                        Toast.makeText(context, "Không thể xóa đánh giá", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<String>>, t: Throwable) {
                    if (!isAdded) return
                    
                    progressBar.visibility = View.GONE
                    Toast.makeText(context, "Lỗi kết nối", Toast.LENGTH_SHORT).show()
                }
            })
    }
    
    companion object {
        private const val TAG = "ManageReviewsFragment"
    }
}
