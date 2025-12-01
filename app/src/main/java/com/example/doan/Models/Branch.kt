package com.example.doan.Models

import com.google.gson.annotations.SerializedName

data class Branch(
    @SerializedName("id")
    val id: Int? = null,

    @SerializedName("branch_name")
    val branchName: String? = null,

    @SerializedName("address")
    val address: String? = null
)
