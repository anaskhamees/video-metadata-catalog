package com.videocatalog.app.client

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.videocatalog.app.client.data.ServiceCatalogRepository
import com.videocatalog.app.client.ui.theme.VideoCatalogTheme

class MainActivity : ComponentActivity() {

    // Composition root: the concrete repository is chosen here and injected into the ViewModel.
    // The application context is used so the repository never holds a reference to this Activity.
    private val viewModel: CatalogViewModel by viewModels {
        viewModelFactory {
            initializer {
                CatalogViewModel(ServiceCatalogRepository(applicationContext))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VideoCatalogTheme {
                val state by viewModel.state.collectAsStateWithLifecycle()
                CatalogScreen(
                    state = state,
                    onRefresh = viewModel::refresh
                )
            }
        }
    }
}
