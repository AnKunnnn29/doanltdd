package com.example.doan

import android.app.Application
import com.example.doan.Utils.CartManager

class UTETeaApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize CartManager with context
        CartManager.getInstance().init(this)
    }
}
