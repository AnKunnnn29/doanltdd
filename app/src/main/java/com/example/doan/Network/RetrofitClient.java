package com.example.doan.Network;

import android.content.Context;
import com.example.doan.Utils.SessionManager;
import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.concurrent.TimeUnit;

public class RetrofitClient {

    private static final String BASE_URL = "http://10.0.2.2:8080/api/";

    private static RetrofitClient instance;
    private ApiService apiService;

    private RetrofitClient(Context context) {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        AuthInterceptor authInterceptor = new AuthInterceptor(context);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .addInterceptor(authInterceptor)
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();

        // Create Gson with lenient parsing
        com.google.gson.Gson gson = new com.google.gson.GsonBuilder()
                .setLenient()
                .create();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        apiService = retrofit.create(ApiService.class);
    }

    public static synchronized RetrofitClient getInstance(Context context) {
        if (instance == null) {
            instance = new RetrofitClient(context.getApplicationContext());
        }
        return instance;
    }

    public ApiService getApiService() {
        return apiService;
    }

    public static String getBaseUrl() {
        return BASE_URL;
    }

    private static class AuthInterceptor implements Interceptor {
        private SessionManager sessionManager;

        public AuthInterceptor(Context context) {
            this.sessionManager = new SessionManager(context);
        }

        @Override
        public Response intercept(Chain chain) throws IOException {
            Request originalRequest = chain.request();
            Request.Builder requestBuilder = originalRequest.newBuilder();
            
            // Don't add token for login/register endpoints
            String path = originalRequest.url().encodedPath();
            boolean isAuthEndpoint = path.contains("/auth/login") || 
                                    path.contains("/auth/register") ||
                                    path.contains("/auth/register-with-otp") ||
                                    path.contains("/auth/otp-verify");
            
            if (!isAuthEndpoint) {
                String token = sessionManager.getToken();
                if (token != null && !token.isEmpty()) {
                    requestBuilder.addHeader("Authorization", "Bearer " + token);
                }
            }
            
            return chain.proceed(requestBuilder.build());
        }
    }
}
