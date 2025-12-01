package com.example.doan.Activities;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.doan.Models.ApiResponse;
import com.example.doan.Models.Category;
import com.example.doan.Models.Product;
import com.example.doan.Network.RetrofitClient;
import com.example.doan.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddProductActivity extends AppCompatActivity {

    private static final String TAG = "AddProductActivity";

    private ImageView productImageView;
    private EditText nameInput, priceInput, descriptionInput;
    private Spinner categorySpinner; // Thay đổi từ EditText thành Spinner
    private Uri selectedImageUri;

    private ActivityResultLauncher<Intent> galleryLauncher;
    private List<Category> categoryList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_product);

        productImageView = findViewById(R.id.product_image_view);
        Button selectImageButton = findViewById(R.id.btn_select_image);
        Button saveButton = findViewById(R.id.btn_save_product);

        nameInput = findViewById(R.id.edit_text_product_name);
        priceInput = findViewById(R.id.edit_text_price);
        descriptionInput = findViewById(R.id.edit_text_description);
        categorySpinner = findViewById(R.id.spinner_category); // Ánh xạ Spinner

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();
                        productImageView.setImageURI(selectedImageUri);
                        Toast.makeText(this, "Đã chọn ảnh.", Toast.LENGTH_SHORT).show();
                    }
                });

        selectImageButton.setOnClickListener(v -> showImagePickerDialog());
        saveButton.setOnClickListener(v -> saveProduct());

        // Tải danh sách Category cho Spinner
        loadCategories();
    }

    private void loadCategories() {
        RetrofitClient.getInstance(this).getApiService().getCategories().enqueue(new Callback<ApiResponse<List<Category>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<Category>>> call, @NonNull Response<ApiResponse<List<Category>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    categoryList = response.body().getData();
                    List<String> categoryNames = new ArrayList<>();
                    for (Category category : categoryList) {
                        categoryNames.add(category.getName());
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(AddProductActivity.this,
                            android.R.layout.simple_spinner_item, categoryNames);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    categorySpinner.setAdapter(adapter);
                } else {
                    Toast.makeText(AddProductActivity.this, "Không thể tải danh mục.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<List<Category>>> call, @NonNull Throwable t) {
                Log.e(TAG, "Lỗi tải danh mục: " + t.getMessage());
                Toast.makeText(AddProductActivity.this, "Lỗi kết nối.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showImagePickerDialog() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(galleryIntent);
    }

    private void saveProduct() {
        String name = nameInput.getText().toString().trim();
        String priceStr = priceInput.getText().toString().trim();
        String description = descriptionInput.getText().toString().trim();
        
        // Lấy category được chọn từ Spinner
        int selectedCategoryIndex = categorySpinner.getSelectedItemPosition();
        
        if (selectedImageUri == null || name.isEmpty() || priceStr.isEmpty() || description.isEmpty() || selectedCategoryIndex == -1) {
            Toast.makeText(this, "Vui lòng điền đủ thông tin và chọn ảnh!", Toast.LENGTH_LONG).show();
            return;
        }

        Category selectedCategory = categoryList.get(selectedCategoryIndex);
        String categoryName = selectedCategory.getName();
        // Có thể gửi categoryId lên server nếu API hỗ trợ, ở đây ta vẫn gửi name như code cũ yêu cầu
        // nhưng tốt hơn là nên cập nhật API để nhận ID. Hiện tại giữ nguyên gửi name để tương thích.

        double price;
        try {
            price = Double.parseDouble(priceStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Giá phải là số hợp lệ.", Toast.LENGTH_SHORT).show();
            return;
        }

        uploadProduct(name, price, description, categoryName, selectedCategory.getId());
    }

    private void uploadProduct(String name, double price, String description, String categoryName, int categoryId) {
        String filePath = getRealPathFromURI(this, selectedImageUri);

        if (filePath == null) {
            Toast.makeText(this, "Không thể lấy đường dẫn ảnh hợp lệ.", Toast.LENGTH_LONG).show();
            return;
        }

        File file = new File(filePath);

        RequestBody fileReqBody = RequestBody.create(MediaType.parse(getContentResolver().getType(selectedImageUri)), file);
        MultipartBody.Part imagePart = MultipartBody.Part.createFormData("image", file.getName(), fileReqBody);

        RequestBody namePart = RequestBody.create(MediaType.parse("text/plain"), name);
        RequestBody pricePart = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(price));
        RequestBody descriptionPart = RequestBody.create(MediaType.parse("text/plain"), description);
        // Gửi cả category name (để tương thích code cũ)
        RequestBody categoryPart = RequestBody.create(MediaType.parse("text/plain"), categoryName);
        
        // Nếu backend đã update để nhận category_id, bạn có thể thêm Part này:
        // RequestBody categoryIdPart = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(categoryId));

        RetrofitClient.getInstance(this).getApiService().addProduct(
                imagePart,
                namePart,
                pricePart,
                descriptionPart,
                categoryPart
        ).enqueue(new Callback<Product>() {
            @Override
            public void onResponse(@NonNull Call<Product> call, @NonNull Response<Product> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(AddProductActivity.this, "Thêm sản phẩm THÀNH CÔNG!", Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    Toast.makeText(AddProductActivity.this, "Lỗi Server: " + response.code(), Toast.LENGTH_LONG).show();
                    try {
                        Log.e(TAG, "Lỗi phản hồi: " + response.errorBody().string());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<Product> call, @NonNull Throwable t) {
                Toast.makeText(AddProductActivity.this, "Lỗi kết nối API: " + t.getMessage(), Toast.LENGTH_LONG).show();
                Log.e(TAG, "Lỗi kết nối: " + t.getMessage());
            }
        });
    }

    private String getRealPathFromURI(Context context, Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Images.Media.DATA };
            cursor = context.getContentResolver().query(contentUri, proj, null, null, null);
            if (cursor == null) return contentUri.getPath();
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi lấy RealPath: " + e.getMessage());
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}