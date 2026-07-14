package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.data.database.ScreenWiseDatabase
import com.example.data.repository.ScreenWiseRepository
import com.example.ui.screens.ScreenWiseAppContent
import com.example.ui.theme.ScreenWiseTheme
import com.example.viewmodel.ScreenWiseViewModel
import com.example.viewmodel.ScreenWiseViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Room Database, Repository, and ViewModel
        val database = ScreenWiseDatabase.getDatabase(applicationContext)
        val repository = ScreenWiseRepository(database.dao())
        val factory = ScreenWiseViewModelFactory(repository)
        val viewModel = ViewModelProvider(this, factory)[ScreenWiseViewModel::class.java]

        enableEdgeToEdge()
        setContent {
            val profile by viewModel.userProfile.collectAsState()
            val themeMode = profile?.themeMode ?: "Sleek Interface"

            // Reactive Theme Choice
            ScreenWiseTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background
                ) {
                    ScreenWiseAppContent(viewModel = viewModel)
                }
            }
        }
    }
}
