package com.example.doan.Activities

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.doan.Adapters.SpinVoucherAdapter
import com.example.doan.Models.*
import com.example.doan.Network.RetrofitClient
import com.example.doan.R
import com.example.doan.Views.SpinWheelView
import com.google.android.material.button.MaterialButton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SpinWheelActivity : AppCompatActivity() {

    private lateinit var spinWheel: SpinWheelView
    private lateinit var btnSpin: MaterialButton
    private lateinit var tvCurrentPoints: TextView
    private lateinit var tvSpinHint: TextView
    private lateinit var tvNoVouchers: TextView
    private lateinit var rvVouchers: RecyclerView
    private lateinit var voucherAdapter: SpinVoucherAdapter

    private var currentPoints = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_spin_wheel)

        initViews()
        setupRecyclerView()
        loadUserPoints()
    }

    private fun initViews() {
        findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }
        
        spinWheel = findViewById(R.id.spinWheel)
        btnSpin = findViewById(R.id.btnSpin)
        tvCurrentPoints = findViewById(R.id.tvCurrentPoints)
        tvSpinHint = findViewById(R.id.tvSpinHint)
        tvNoVouchers = findViewById(R.id.tvNoVouchers)
        rvVouchers = findViewById(R.id.rvVouchers)

        btnSpin.setOnClickListener {
            if (currentPoints >= 5) {
                spinWheel()
            } else {
                Toast.makeText(this, "Cần 5 điểm để quay", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupRecyclerView() {
        voucherAdapter = SpinVoucherAdapter { voucher ->
            copyVoucherCode(voucher.voucherCode ?: "")
        }
        rvVouchers.layoutManager = LinearLayoutManager(this)
        rvVouchers.adapter = voucherAdapter
    }

    private fun loadUserPoints() {
        RetrofitClient.getInstance(this).apiService.getUserPoints()
            .enqueue(object : Callback<ApiResponse<UserPointsDto>> {
                override fun onResponse(
                    call: Call<ApiResponse<UserPointsDto>>,
                    response: Response<ApiResponse<UserPointsDto>>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val data = response.body()?.data
                        if (data != null) {
                            currentPoints = data.currentPoints
                            updateUI(data)
                        }
                    }
                }

                override fun onFailure(call: Call<ApiResponse<UserPointsDto>>, t: Throwable) {
                    Toast.makeText(this@SpinWheelActivity, "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun updateUI(data: UserPointsDto) {
        tvCurrentPoints.text = "${data.currentPoints} điểm"
        btnSpin.isEnabled = data.canSpin
        
        if (data.canSpin) {
            tvSpinHint.text = "Bạn có thể quay!"
            btnSpin.alpha = 1f
        } else {
            tvSpinHint.text = "Cần ${5 - data.currentPoints} điểm nữa để quay"
            btnSpin.alpha = 0.5f
        }

        val vouchers = data.availableRewards ?: emptyList()
        if (vouchers.isEmpty()) {
            tvNoVouchers.visibility = View.VISIBLE
            rvVouchers.visibility = View.GONE
        } else {
            tvNoVouchers.visibility = View.GONE
            rvVouchers.visibility = View.VISIBLE
            voucherAdapter.updateVouchers(vouchers)
        }
    }

    private fun spinWheel() {
        btnSpin.isEnabled = false
        
        RetrofitClient.getInstance(this).apiService.spinWheel()
            .enqueue(object : Callback<ApiResponse<SpinWheelResponse>> {
                override fun onResponse(
                    call: Call<ApiResponse<SpinWheelResponse>>,
                    response: Response<ApiResponse<SpinWheelResponse>>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val result = response.body()?.data
                        if (result != null) {
                            spinWheel.spin(result.winIndex) {
                                showResultDialog(result)
                                currentPoints = result.remainingPoints
                                loadUserPoints()
                            }
                        }
                    } else {
                        btnSpin.isEnabled = currentPoints >= 5
                        val errorMsg = response.body()?.message ?: "Không thể quay"
                        Toast.makeText(this@SpinWheelActivity, errorMsg, Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<SpinWheelResponse>>, t: Throwable) {
                    btnSpin.isEnabled = currentPoints >= 5
                    Toast.makeText(this@SpinWheelActivity, "Lỗi kết nối", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun showResultDialog(result: SpinWheelResponse) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_spin_result, null)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tvResultTitle)
        val tvMessage = dialogView.findViewById<TextView>(R.id.tvResultMessage)
        val tvVoucherCode = dialogView.findViewById<TextView>(R.id.tvResultVoucherCode)
        val btnCopy = dialogView.findViewById<MaterialButton>(R.id.btnCopyCode)
        val btnClose = dialogView.findViewById<MaterialButton>(R.id.btnCloseResult)
        
        if (result.discountPercent == 0) {
            tvTitle.text = "😢 Tiếc quá!"
            tvMessage.text = "Chúc bạn may mắn lần sau!"
            tvVoucherCode.visibility = View.GONE
            btnCopy.visibility = View.GONE
        } else {
            tvTitle.text = "🎉 Chúc mừng!"
            tvMessage.text = "Bạn nhận được voucher giảm ${result.discountPercent}%"
            tvVoucherCode.visibility = View.VISIBLE
            tvVoucherCode.text = result.voucherCode ?: ""
            btnCopy.visibility = View.VISIBLE
            
            btnCopy.setOnClickListener {
                copyVoucherCode(result.voucherCode ?: "")
            }
        }
        
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()
        
        btnClose.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
    }

    private fun copyVoucherCode(code: String) {
        if (code.isEmpty()) return
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Voucher Code", code)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "✅ Đã copy mã: $code", Toast.LENGTH_LONG).show()
    }
}
