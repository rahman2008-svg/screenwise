package com.example.data.model

data class AppUsageItem(
    val packageName: String,
    val appName: String,
    val screenTimeMinutes: Int,
    val openCount: Int
)
