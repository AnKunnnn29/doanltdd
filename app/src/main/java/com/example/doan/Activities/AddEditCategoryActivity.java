package com.example.doan.Activities;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.doan.Models.ApiResponse;
import com.example.doan.Models.Category;
import com.example.doan.Network.RetrofitClient;
import com.example.doan.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddEditCategoryActivity extends AppCompatActivity {

    private static final String TAG = "AddEditCategoryActivity";
    private static final int CAMERA_PERMISSION_CODE = 100;

    private ImageView imgCategory;
    private TextInputEditText editName, editDescription;
    private MaterialButton btnSelectImage, btnSave, btnCancel;
    private ProgressBar progressBar;

    private Uri selectedImageUri = null;
    private String imageUrlFromWeb = null;
    private Category editingCategory = null;
    private boolean isEditMode = false;

    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_category);

        initViews();
        setupImageLaunchers();
        checkEditMode();
        setupListeners();
    }

    private void initViews() {
        imgCategory = findViewById(R.id.img_category);
        editName = findViewById(R.id.edit_name);
        editDescription = findViewById(R.id.edit_description);
        btnSelectImage = findViewById(R.id.btn_select_image);
        btnSave = findViewById(R.id.btn_save);
        btnCancel = findViewById(R.id.btn_cancel);
        progressBar = findViewById(R.id.progress_bar);
    }

    private void setupImageLaunchers() {
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();
                        imageUrlFromWeb = null;
                        displayImage();
                    }
                }
        );

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Bundle extras = result.getData().getExtras();
                        Bitmap imageBitmap = (Bitmap) extras.get("data");
                        selectedImageUri = saveBitmapToFile(imageBitmap);
                        imageUrlFromWeb = null;
                        displayImage();
                    }
                }
        );
    }

    private void checkEditMode() {
        Intent intent = getIntent();
        if (intent.hasExtra("category_id")) {
            isEditMode = true;
            setTitle("Chỉnh sửa danh mục");
            int categoryId = intent.getIntExtra("category_id", -1);
            loadCategoryData(categoryId);
        } else {
            setTitle("Thêm danh mục mới");
        }
    }

    private void loadCategoryData(int categoryId) {
        progressBar.setVisibility(View.VISIBLE);
        RetrofitClient.getInstance(this).getApiService()
                .getCategoryById(categoryId)
                .enqueue(new Callback<ApiResponse<Category>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<Category>> call,
                                           @NonNull Response<ApiResponse<Category>> response) {
                        progressBar.setVisibility(View.GONE);
                        if (response.isSuccessful() && response.body() != null) {
                            editingCategory = response.body().getData();
                            populateFields();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<Category>> call, @NonNull Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(AddEditCategoryActivity.this, "Không thể tải dữ liệu", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void populateFields() {
        if (editingCategory == null) return;

        editName.setText(editingCategory.getName());
        editDescription.setText(editingCategory.getDescription());

        if (editingCategory.getImage() != null) {
            imageUrlFromWeb = editingCategory.getImage();
            Glide.with(this)
                    .load(editingCategory.getImage())
                    .placeholder(R.drawable.ic_image_placeholder)
                    .into(imgCategory);
        }
    }

    private void setupListeners() {
        btnSelectImage.setOnClickListener(v -> showImageSourceDialog());
        btnSave.setOnClickListener(v -> saveCategory());
        btnCancel.setOnClickListener(v -> finish());
    }

    private void showImageSourceDialog() {
        String[] options = {"Chọn từ thư viện", "Chụp ảnh", "Nhập URL"};

        new MaterialAlertDialogBuilder(this)
                .setTitle("Chọn nguồn ảnh")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            openGallery();
                            break;
                        case 1:
                            openCamera();
                            break;
                        case 2:
                            showUrlInputDialog();
                            break;
                    }
                })
                .show();
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    private void openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_CODE);
        } else {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            cameraLauncher.launch(intent);
        }
    }

    private void showUrlInputDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_image_url, null);
        TextInputEditText editUrl = dialogView.findViewById(R.id.edit_url);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Nhập URL ảnh")
                .setView(dialogView)
                .setPositiveButton("OK", (dialog, which) -> {
                    String url = editUrl.getText().toString().trim();
                    if (!url.isEmpty()) {
                        selectedImageUri = null;
                        imageUrlFromWeb = url;
                        displayImage();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void displayImage() {
        if (selectedImageUri != null) {
            Glide.with(this)
                    .load(selectedImageUri)
                    .placeholder(R.drawable.ic_image_placeholder)
                    .into(imgCategory);
        } else if (imageUrlFromWeb != null) {
            Glide.with(this)
                    .load(imageUrlFromWeb)
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_broken_image)
                    .into(imgCategory);
        }
    }

    private Uri saveBitmapToFile(Bitmap bitmap) {
        try {
            File file = new File(getCacheDir(), "camera_image_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.close();
            return Uri.fromFile(file);
        } catch (IOException e) {
            Log.e(TAG, "Error saving bitmap: " + e.getMessage());
            return null;
        }
    }

    private void saveCategory() {
        if (!validateInputs()) return;

        String name = editName.getText().toString().trim();
        String description = editDescription.getText().toString().trim();

        progressBar.setVisibility(View.VISIBLE);
        btnSave.setEnabled(false);

        Category category = new Category();
        category.setName(name);
        category.setDescription(description);

        if (imageUrlFromWeb != null) {
            category.setImage(imageUrlFromWeb);
        }

        if (isEditMode) {
            category.setId(editingCategory.getId());
            updateCategory(category);
        } else {
            createCategory(category);
        }
    }

    private void createCategory(Category category) {
        RetrofitClient.getInstance(this).getApiService()
                .createCategory(category)
                .enqueue(new Callback<ApiResponse<Category>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<Category>> call,
                                           @NonNull Response<ApiResponse<Category>> response) {
                        progressBar.setVisibility(View.GONE);
                        btnSave.setEnabled(true);

                        if (response.isSuccessful() && response.body() != null) {
                            Toast.makeText(AddEditCategoryActivity.this, "Thêm danh mục thành công!", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(AddEditCategoryActivity.this, "Lỗi: " + response.code(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<Category>> call, @NonNull Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        btnSave.setEnabled(true);
                        Toast.makeText(AddEditCategoryActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateCategory(Category category) {
        RetrofitClient.getInstance(this).getApiService()
                .updateCategory(category.getId(), category)
                .enqueue(new Callback<ApiResponse<Category>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<Category>> call,
                                           @NonNull Response<ApiResponse<Category>> response) {
                        progressBar.setVisibility(View.GONE);
                        btnSave.setEnabled(true);

                        if (response.isSuccessful()) {
                            Toast.makeText(AddEditCategoryActivity.this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(AddEditCategoryActivity.this, "Lỗi: " + response.code(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<Category>> call, @NonNull Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        btnSave.setEnabled(true);
                        Toast.makeText(AddEditCategoryActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private boolean validateInputs() {
        String name = editName.getText().toString().trim();

        if (name.isEmpty()) {
            editName.setError("Vui lòng nhập tên danh mục");
            return false;
        }

        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Cần quyền truy cập camera", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
