package com.example.doan.Network;

import android.content.Context;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.concurrent.TimeUnit;

public class RetrofitClient {

    // CHÚ Ý: Thay đổi BASE_URL này khi triển khai hoặc đổi mạng
    // Để test trên emulator: http://10.0.2.2:8080/api/
    // Để test trên thiết bị thật: http://YOUR_IP:8080/api/ (ví dụ: http://192.168.1.100:8080/api/)
    private static final String BASE_URL = "http://10.0.2.2:8080/api/";

    private static RetrofitClient instance;
    private ApiService apiService;
    private Context context;

    private RetrofitClient(Context context) {
        this.context = context.getApplicationContext();

        // Logging interceptor
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        // Auth interceptor để tự động thêm JWT token
        AuthInterceptor authInterceptor = new AuthInterceptor(this.context);

        // OkHttp client với interceptors
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .addInterceptor(authInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        // Retrofit instance
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(ApiService.class);
    }

    public static synchronized RetrofitClient getInstance(Context context) {
        if (instance == null) {
            instance = new RetrofitClient(context);
        }
        return instance;
    }

    public ApiService getApiService() {
        return apiService;
    }

    // Helper method để lấy base URL cho loading images
    public static String getBaseUrl() {
        return BASE_URL.replace("/api/", "");
    }
}