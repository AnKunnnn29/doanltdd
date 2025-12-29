package com.example.doan.Activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.doan.Models.ApiResponse
import com.example.doan.Models.GroupOrderDto
import com.example.doan.Models.JoinGroupOrderRequest
import com.example.doan.Network.RetrofitClient
import com.example.doan.R
import com.example.doan.Utils.LoadingDialog
import com.google.android.material.textfield.TextInputEditText
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class JoinGroupOrderActivity : AppCompatActivity() {

    private lateinit var etInviteCode: TextInputEditText
    private lateinit var btnJoin: Button
    private lateinit var btnCreateNew: Button
    private lateinit var loadingDialog: LoadingDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_join_group_order)

        loadingDialog = LoadingDialog(this)
        initViews()
        
        // Check if opened with invite code from deep link
        intent.getStringExtra("INVITE_CODE")?.let { code ->
            etInviteCode.setText(code)
            joinGroupOrder(code)
        }
    }

    private fun initViews() {
        findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar).setNavigationOnClickListener {
            finish()
        }

        etInviteCode = findViewById(R.id.et_invite_code)
        btnJoin = findViewById(R.id.btn_join)
        btnCreateNew = findViewById(R.id.btn_create_new)

        btnJoin.setOnClickListener {
            val code = etInviteCode.text.toString().trim().uppercase()
            if (code.length < 6) {
                Toast.makeText(this, "Mã mời phải có ít nhất 6 ký tự", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            joinGroupOrder(code)
        }

        btnCreateNew.setOnClickListener {
            startActivity(Intent(this, CreateGroupOrderActivity::class.java))
            finish()
        }
    }

    private fun joinGroupOrder(code: String) {
        loadingDialog.show("Đang tham gia...")

        val request = JoinGroupOrderRequest(inviteCode = code)
        RetrofitClient.getInstance(this).apiService.joinGroupOrder(request)
            .enqueue(object : Callback<ApiResponse<GroupOrderDto>> {
                override fun onResponse(
                    call: Call<ApiResponse<GroupOrderDto>>,
                    response: Response<ApiResponse<GroupOrderDto>>
                ) {
                    loadingDialog.dismiss()
                    if (response.isSuccessful && response.body()?.success == true) {
                        val groupOrder = response.body()?.data
                        Toast.makeText(this@JoinGroupOrderActivity, 
                            "Tham gia thành công!", Toast.LENGTH_SHORT).show()
                        
                        val intent = Intent(this@JoinGroupOrderActivity, GroupOrderActivity::class.java)
                        intent.putExtra("GROUP_ORDER_ID", groupOrder?.id)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this@JoinGroupOrderActivity,
                            response.body()?.message ?: "Mã mời không hợp lệ", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<GroupOrderDto>>, t: Throwable) {
                    loadingDialog.dismiss()
                    Toast.makeText(this@JoinGroupOrderActivity, 
                        "Lỗi kết nối: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
}
