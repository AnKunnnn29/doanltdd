package com.example.doan.Activities

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.doan.Models.ApiResponse
import com.example.doan.Models.Category
import com.example.doan.Network.RetrofitClient
import com.example.doan.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream

class AddEditCategoryActivity : AppCompatActivity() {
    private lateinit var imgCategory: ImageView
    private lateinit var btnSelectImage: MaterialButton
    private lateinit var editName: TextInputEditText
    private lateinit var editDescription: TextInputEditText
    private lateinit var btnSave: MaterialButton
    private lateinit var btnCancel: MaterialButton
    private lateinit var progressBar: ProgressBar
    private var isEditMode = false
    private var categoryId: Int = 0
    private var selectedImageUri: Uri? = null
    private var currentImageUrl: String? = null

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { selectedImageUri = it; imgCategory.setImageURI(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit_category)
        initViews()
        categoryId = intent.getIntExtra("CATEGORY_ID", 0)
        if (categoryId > 0) {
            isEditMode = true
            editName.setText(intent.getStringExtra("CATEGORY_NAME"))
            editDescription.setText(intent.getStringExtra("CATEGORY_DESCRIPTION"))
            currentImageUrl = intent.getStringExtra("CATEGORY_IMAGE")
            if (!currentImageUrl.isNullOrEmpty()) {
                Glide.with(this).load(currentImageUrl)
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_broken_image).into(imgCategory)
            }
        }
        setupListeners()
    }

    private fun initViews() {
        imgCategory = findViewById(R.id.img_category)
        btnSelectImage = findViewById(R.id.btn_select_image)
        editName = findViewById(R.id.edit_name)
        editDescription = findViewById(R.id.edit_description)
        btnSave = findViewById(R.id.btn_save)
        btnCancel = findViewById(R.id.btn_cancel)
        progressBar = findViewById(R.id.progress_bar)
    }

    private fun setupListeners() {
        btnSelectImage.setOnClickListener { imagePickerLauncher.launch("image/*") }
        btnCancel.setOnClickListener { finish() }
        btnSave.setOnClickListener {
            if (validateInputs()) {
                if (selectedImageUri != null) uploadImageAndSave() else saveCategory(currentImageUrl)
            }
        }
    }

    private fun uploadImageAndSave() {
        val uri = selectedImageUri ?: return
        showLoading(true)
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val tempFile = File.createTempFile("category_image", ".jpg", cacheDir)
            FileOutputStream(tempFile).use { out -> inputStream?.copyTo(out) }
            inputStream?.close()
            val requestFile = tempFile.asRequestBody("image/*".toMediaTypeOrNull())
            val imagePart = MultipartBody.Part.createFormData("file", tempFile.name, requestFile)
            val nameBody = editName.text.toString().trim().ifEmpty { "category" }
                .toRequestBody("text/plain".toMediaTypeOrNull())
            RetrofitClient.getInstance(this).apiService.uploadCategoryImage(imagePart, nameBody)
                .enqueue(object : Callback<ApiResponse<Map<String, String>>> {
                    override fun onResponse(call: Call<ApiResponse<Map<String, String>>>, 
                        response: Response<ApiResponse<Map<String, String>>>) {
                        tempFile.delete()
                        if (response.isSuccessful && response.body()?.success == true) {
                            saveCategory(response.body()?.data?.get("imageUrl"))
                        } else { showLoading(false); Toast.makeText(this@AddEditCategoryActivity, 
                            "Loi upload anh", Toast.LENGTH_SHORT).show() }
                    }
                    override fun onFailure(call: Call<ApiResponse<Map<String, String>>>, t: Throwable) {
                        showLoading(false); tempFile.delete(); saveCategory(currentImageUrl)
                    }
                })
        } catch (e: Exception) { showLoading(false); Toast.makeText(this, "Loi: ${e.message}", Toast.LENGTH_SHORT).show() }
    }

    private fun validateInputs(): Boolean {
        if (editName.text.toString().trim().isEmpty()) { editName.error = "Vui long nhap ten danh muc"; return false }
        return true
    }

    private fun saveCategory(imageUrl: String?) {
        showLoading(true)
        val categoryData: Map<String, String> = mapOf(
            "name" to editName.text.toString().trim(),
            "description" to editDescription.text.toString().trim(),
            "imageUrl" to (imageUrl ?: "")
        )
        val call = if (isEditMode) RetrofitClient.getInstance(this).apiService.updateCategory(categoryId.toLong(), categoryData)
            else RetrofitClient.getInstance(this).apiService.createCategory(categoryData)
        call.enqueue(object : Callback<ApiResponse<Category>> {
            override fun onResponse(call: Call<ApiResponse<Category>>, response: Response<ApiResponse<Category>>) {
                showLoading(false)
                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(this@AddEditCategoryActivity, 
                        if (isEditMode) "Da cap nhat danh muc" else "Da them danh muc", Toast.LENGTH_SHORT).show()
                    setResult(Activity.RESULT_OK); finish()
                } else { Toast.makeText(this@AddEditCategoryActivity, "Khong the luu danh muc", Toast.LENGTH_SHORT).show() }
            }
            override fun onFailure(call: Call<ApiResponse<Category>>, t: Throwable) {
                showLoading(false); Toast.makeText(this@AddEditCategoryActivity, "Loi: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnSave.isEnabled = !show; btnCancel.isEnabled = !show
    }
}
