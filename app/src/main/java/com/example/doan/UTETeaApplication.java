package com.example.doan;

import android.app.Application;
import com.example.doan.Utils.CartManager;

public class UTETeaApplication extends Application {
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Initialize CartManager with context
        CartManager.getInstance().init(this);
    }
}
