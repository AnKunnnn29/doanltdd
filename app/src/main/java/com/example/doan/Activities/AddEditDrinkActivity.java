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
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.doan.Models.ApiResponse;
import com.example.doan.Models.Category;
import com.example.doan.Models.Drink;
import com.example.doan.Network.RetrofitClient;
import com.example.doan.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddEditDrinkActivity extends AppCompatActivity {

    private static final String TAG = "AddEditDrinkActivity";
    private static final int CAMERA_PERMISSION_CODE = 100;

    // Views
    private ImageView imgDrink;
    private TextInputEditText editName, editDescription, editPrice, editImageUrl;
    private AutoCompleteTextView spinnerCategory;
    private MaterialButton btnSelectImage, btnSave, btnCancel;
    private ProgressBar progressBar;
    private TextInputLayout layoutImageUrl;

    // Data
    private List<Category> categoryList = new ArrayList<>();
    private int selectedCategoryId = -1;
    private Uri selectedImageUri = null;
    private String imageUrlFromWeb = null;
    private Drink editingDrink = null;
    private boolean isEditMode = false;

    // Image selection launchers
    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_drink);

        initViews();
        setupImageLaunchers();
        loadCategories();
        checkEditMode();
        setupListeners();
    }

    private void initViews() {
        imgDrink = findViewById(R.id.img_drink);
        editName = findViewById(R.id.edit_name);
        editDescription = findViewById(R.id.edit_description);
        editPrice = findViewById(R.id.edit_price);
        editImageUrl = findViewById(R.id.edit_image_url);
        spinnerCategory = findViewById(R.id.spinner_category);
        btnSelectImage = findViewById(R.id.btn_select_image);
        btnSave = findViewById(R.id.btn_save);
        btnCancel = findViewById(R.id.btn_cancel);
        progressBar = findViewById(R.id.progress_bar);
        layoutImageUrl = findViewById(R.id.layout_image_url);
    }

    private void setupImageLaunchers() {
        // Gallery launcher
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

        // Camera launcher
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
        if (intent.hasExtra("drink_id")) {
            isEditMode = true;
            setTitle("Chỉnh sửa món");
            // Load drink data
            int drinkId = intent.getIntExtra("drink_id", -1);
            loadDrinkData(drinkId);
        } else {
            setTitle("Thêm món mới");
        }
    }

    private void loadDrinkData(int drinkId) {
        progressBar.setVisibility(View.VISIBLE);
        RetrofitClient.getInstance(this).getApiService()
                .getDrinkById(drinkId)
                .enqueue(new Callback<ApiResponse<Drink>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<Drink>> call,
                                           @NonNull Response<ApiResponse<Drink>> response) {
                        progressBar.setVisibility(View.GONE);
                        if (response.isSuccessful() && response.body() != null) {
                            editingDrink = response.body().getData();
                            populateFields();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<Drink>> call, @NonNull Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(AddEditDrinkActivity.this, "Không thể tải dữ liệu", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void populateFields() {
        if (editingDrink == null) return;

        editName.setText(editingDrink.getName());
        editDescription.setText(editingDrink.getDescription());
        editPrice.setText(String.valueOf(editingDrink.getBasePrice()));
        
        if (editingDrink.getImageUrl() != null) {
            imageUrlFromWeb = editingDrink.getImageUrl();
            Glide.with(this)
                    .load(editingDrink.getImageUrl())
                    .placeholder(R.drawable.ic_image_placeholder)
                    .into(imgDrink);
        }

        selectedCategoryId = editingDrink.getCategoryId();
        // Category will be selected after categories are loaded
    }

    private void loadCategories() {
        RetrofitClient.getInstance(this).getApiService()
                .getCategories()
                .enqueue(new Callback<ApiResponse<List<Category>>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<List<Category>>> call,
                                           @NonNull Response<ApiResponse<List<Category>>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            categoryList = response.body().getData();
                            setupCategorySpinner();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<List<Category>>> call, @NonNull Throwable t) {
                        Toast.makeText(AddEditDrinkActivity.this, "Không thể tải danh mục", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setupCategorySpinner() {
        List<String> categoryNames = new ArrayList<>();
        for (Category category : categoryList) {
            categoryNames.add(category.getName());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                categoryNames
        );
        spinnerCategory.setAdapter(adapter);

        spinnerCategory.setOnItemClickListener((parent, view, position, id) -> {
            selectedCategoryId = categoryList.get(position).getId();
        });

        // Select category if in edit mode
        if (isEditMode && selectedCategoryId != -1) {
            for (int i = 0; i < categoryList.size(); i++) {
                if (categoryList.get(i).getId() == selectedCategoryId) {
                    spinnerCategory.setText(categoryList.get(i).getName(), false);
                    break;
                }
            }
        }
    }

    private void setupListeners() {
        btnSelectImage.setOnClickListener(v -> showImageSourceDialog());
        btnSave.setOnClickListener(v -> saveDrink());
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
                    .into(imgDrink);
        } else if (imageUrlFromWeb != null) {
            Glide.with(this)
                    .load(imageUrlFromWeb)
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_broken_image)
                    .into(imgDrink);
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

    private void saveDrink() {
        if (!validateInputs()) return;

        String name = editName.getText().toString().trim();
        String description = editDescription.getText().toString().trim();
        double price = Double.parseDouble(editPrice.getText().toString().trim());

        progressBar.setVisibility(View.VISIBLE);
        btnSave.setEnabled(false);

        if (isEditMode) {
            updateDrink(name, description, price);
        } else {
            createDrink(name, description, price);
        }
    }

    private void createDrink(String name, String description, double price) {
        // Create drink object
        Drink drink = new Drink();
        drink.setName(name);
        drink.setDescription(description);
        drink.setBasePrice(price);
        drink.setCategoryId(selectedCategoryId);
        
        if (imageUrlFromWeb != null) {
            drink.setImageUrl(imageUrlFromWeb);
        }

        RetrofitClient.getInstance(this).getApiService()
                .createDrink(drink)
                .enqueue(new Callback<ApiResponse<Drink>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<Drink>> call,
                                           @NonNull Response<ApiResponse<Drink>> response) {
                        progressBar.setVisibility(View.GONE);
                        btnSave.setEnabled(true);

                        if (response.isSuccessful() && response.body() != null) {
                            Toast.makeText(AddEditDrinkActivity.this, "Thêm món thành công!", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(AddEditDrinkActivity.this, "Lỗi: " + response.code(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<Drink>> call, @NonNull Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        btnSave.setEnabled(true);
                        Toast.makeText(AddEditDrinkActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateDrink(String name, String description, double price) {
        editingDrink.setName(name);
        editingDrink.setDescription(description);
        editingDrink.setBasePrice(price);
        editingDrink.setCategoryId(selectedCategoryId);
        
        if (imageUrlFromWeb != null) {
            editingDrink.setImageUrl(imageUrlFromWeb);
        }

        RetrofitClient.getInstance(this).getApiService()
                .updateDrink(editingDrink.getId(), editingDrink)
                .enqueue(new Callback<ApiResponse<Drink>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<Drink>> call,
                                           @NonNull Response<ApiResponse<Drink>> response) {
                        progressBar.setVisibility(View.GONE);
                        btnSave.setEnabled(true);

                        if (response.isSuccessful()) {
                            Toast.makeText(AddEditDrinkActivity.this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(AddEditDrinkActivity.this, "Lỗi: " + response.code(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<Drink>> call, @NonNull Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        btnSave.setEnabled(true);
                        Toast.makeText(AddEditDrinkActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private boolean validateInputs() {
        String name = editName.getText().toString().trim();
        String priceStr = editPrice.getText().toString().trim();

        if (name.isEmpty()) {
            editName.setError("Vui lòng nhập tên món");
            return false;
        }

        if (priceStr.isEmpty()) {
            editPrice.setError("Vui lòng nhập giá");
            return false;
        }

        try {
            double price = Double.parseDouble(priceStr);
            if (price <= 0) {
                editPrice.setError("Giá phải lớn hơn 0");
                return false;
            }
        } catch (NumberFormatException e) {
            editPrice.setError("Giá không hợp lệ");
            return false;
        }

        if (selectedCategoryId == -1) {
            Toast.makeText(this, "Vui lòng chọn danh mục", Toast.LENGTH_SHORT).show();
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
