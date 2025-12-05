package com.example.doan.Activities

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.doan.Models.ApiResponse
import com.example.doan.Models.Category
import com.example.doan.Models.Drink
import com.example.doan.Network.RetrofitClient
import com.example.doan.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AddEditDrinkActivity : AppCompatActivity() {

    private lateinit var imgDrink: ImageView
    private lateinit var btnSelectImage: MaterialButton
    private lateinit var editName: TextInputEditText
    private lateinit var editDescription: TextInputEditText
    private lateinit var editPrice: TextInputEditText
    private lateinit var spinnerCategory: AutoCompleteTextView
    private lateinit var editImageUrl: TextInputEditText
    private lateinit var btnSave: MaterialButton
    private lateinit var btnCancel: MaterialButton
    private lateinit var progressBar: ProgressBar

    private var isEditMode = false
    private var drinkId: Int = 0
    private var selectedImageUri: Uri? = null
    private var currentImageUrl: String? = null
    private val categories = mutableListOf<Category>()
    private var selectedCategoryId: Int = 0

    companion object {
        private const val PICK_IMAGE_REQUEST = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit_drink)

        initViews()
        loadCategories()

        // Check if editing existing drink
        intent.getSerializableExtra("DRINK_DATA")?.let { drink ->
            isEditMode = true
            drinkId = (drink as Drink).id
            populateDrinkData(drink)
        }

        setupListeners()
    }

    private fun initViews() {
        imgDrink = findViewById(R.id.img_drink)
        btnSelectImage = findViewById(R.id.btn_select_image)
        editName = findViewById(R.id.edit_name)
        editDescription = findViewById(R.id.edit_description)
        editPrice = findViewById(R.id.edit_price)
        spinnerCategory = findViewById(R.id.spinner_category)
        editImageUrl = findViewById(R.id.edit_image_url)
        btnSave = findViewById(R.id.btn_save)
        btnCancel = findViewById(R.id.btn_cancel)
        progressBar = findViewById(R.id.progress_bar)
    }

    private fun setupListeners() {
        btnSelectImage.setOnClickListener {
            openImagePicker()
        }

        btnCancel.setOnClickListener {
            finish()
        }

        btnSave.setOnClickListener {
            if (validateInputs()) {
                saveDrink()
            }
        }
    }

    private fun loadCategories() {
        RetrofitClient.getInstance(this).apiService.getCategories()
            .enqueue(object : Callback<ApiResponse<List<Category>>> {
                override fun onResponse(
                    call: Call<ApiResponse<List<Category>>>,
                    response: Response<ApiResponse<List<Category>>>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        response.body()?.data?.let { categoryList ->
                            categories.clear()
                            categories.addAll(categoryList)
                            setupCategorySpinner()
                        }
                    }
                }

                override fun onFailure(call: Call<ApiResponse<List<Category>>>, t: Throwable) {
                    Toast.makeText(this@AddEditDrinkActivity, "Không thể tải danh mục", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun setupCategorySpinner() {
        val categoryNames = categories.map { it.name }
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categoryNames)
        spinnerCategory.setAdapter(adapter)
        
        spinnerCategory.setOnItemClickListener { _, _, position, _ ->
            selectedCategoryId = categories[position].id
        }
    }

    private fun populateDrinkData(drink: Drink) {
        editName.setText(drink.name)
        editDescription.setText(drink.description)
        editPrice.setText(drink.basePrice.toString())
        currentImageUrl = drink.imageUrl
        editImageUrl.setText(drink.imageUrl)

        // Load image
        if (!drink.imageUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(drink.imageUrl)
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_broken_image)
                .into(imgDrink)
        }

        // Set category
        val categoryIndex = categories.indexOfFirst { it.id == drink.categoryId }
        if (categoryIndex >= 0) {
            spinnerCategory.setText(categories[categoryIndex].name, false)
            selectedCategoryId = drink.categoryId
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                selectedImageUri = uri
                imgDrink.setImageURI(uri)
                // For now, we'll use the URL from editImageUrl
                // In production, you'd upload the image to a server
            }
        }
    }

    private fun validateInputs(): Boolean {
        val name = editName.text.toString().trim()
        val price = editPrice.text.toString().trim()

        if (name.isEmpty()) {
            editName.error = "Vui lòng nhập tên món"
            return false
        }

        if (price.isEmpty()) {
            editPrice.error = "Vui lòng nhập giá"
            return false
        }

        if (selectedCategoryId == 0) {
            Toast.makeText(this, "Vui lòng chọn danh mục", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun saveDrink() {
        showLoading(true)

        val drink = Drink(
            id = if (isEditMode) drinkId else 0,
            name = editName.text.toString().trim(),
            description = editDescription.text.toString().trim(),
            basePrice = editPrice.text.toString().toDoubleOrNull() ?: 0.0,
            categoryId = selectedCategoryId,
            imageUrl = editImageUrl.text.toString().trim().ifEmpty { currentImageUrl },
            isActive = true
        )

        val call = if (isEditMode) {
            RetrofitClient.getInstance(this).apiService.updateDrink(drinkId, drink)
        } else {
            RetrofitClient.getInstance(this).apiService.createDrink(drink)
        }

        call.enqueue(object : Callback<ApiResponse<Drink>> {
            override fun onResponse(
                call: Call<ApiResponse<Drink>>,
                response: Response<ApiResponse<Drink>>
            ) {
                showLoading(false)
                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(
                        this@AddEditDrinkActivity,
                        if (isEditMode) "Đã cập nhật món" else "Đã thêm món mới",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                } else {
                    Toast.makeText(
                        this@AddEditDrinkActivity,
                        "Không thể lưu món: ${response.message()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse<Drink>>, t: Throwable) {
                showLoading(false)
                Toast.makeText(
                    this@AddEditDrinkActivity,
                    "Lỗi: ${t.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnSave.isEnabled = !show
        btnCancel.isEnabled = !show
    }
}
