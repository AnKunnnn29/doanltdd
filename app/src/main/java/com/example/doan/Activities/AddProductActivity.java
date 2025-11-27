package com.example.doan.Activities;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.doan.Models.Product;
import com.example.doan.R;
import com.example.doan.Network.RetrofitClient;

import java.io.File;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import androidx.annotation.NonNull;

public class AddProductActivity extends AppCompatActivity {

    private static final String TAG = "AddProductActivity";

    private ImageView productImageView;
    private EditText nameInput, priceInput, descriptionInput, categoryInput;
    private Uri selectedImageUri;

    private ActivityResultLauncher<Intent> galleryLauncher;


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
        categoryInput = findViewById(R.id.edit_text_category);


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
    }

    private void showImagePickerDialog() {

        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(galleryIntent);
    }


    private void saveProduct() {

        String name = nameInput.getText().toString().trim();
        String priceStr = priceInput.getText().toString().trim();
        String description = descriptionInput.getText().toString().trim();
        String category = categoryInput.getText().toString().trim();

        if (selectedImageUri == null || name.isEmpty() || priceStr.isEmpty() || description.isEmpty() || category.isEmpty()) {
            Toast.makeText(this, "Vui lòng điền đủ thông tin và chọn ảnh!", Toast.LENGTH_LONG).show();
            return;
        }

        double price;
        try {
            price = Double.parseDouble(priceStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Giá phải là số hợp lệ.", Toast.LENGTH_SHORT).show();
            return;
        }


        uploadProduct(name, price, description, category);
    }

    private void uploadProduct(String name, double price, String description, String category) {

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
        RequestBody categoryPart = RequestBody.create(MediaType.parse("text/plain"), category);

        RequestBody managerIdPart = RequestBody.create(MediaType.parse("text/plain"), "1");


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
                    Log.e(TAG, "Lỗi phản hồi: " + response.errorBody().toString());
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
            if (cursor == null) return contentUri.getPath(); // Fallback
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