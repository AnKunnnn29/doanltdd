package com.example.doan.Activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.doan.Fragments.AccountFragment
import com.example.doan.R

class AccountActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account)

        
        if (savedInstanceState == null) {
             supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, AccountFragment()) // Sử dụng android.R.id.content làm container tạm
                .commit()
        }
    }
}
