package com.videocatalog.app.client

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.videocatalog.app.client.ui.theme.VideoCatalogTheme

class MainActivity : ComponentActivity() {

    private val viewModel: CatalogViewModel by viewModels ()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VideoCatalogTheme {
                //collect the StateFlows, when activity STARTED
                val state by viewModel.state.collectAsStateWithLifecycle()
                CatalogScreen(state=state, onRefresh = viewModel::refresh)
            }
        }
    }
}
