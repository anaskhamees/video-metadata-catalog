package com.videocatalog.app.client
import com.videocatalog.app.VideoMetadata

//Three ONLY states, the screen can show (StateFlow)
sealed interface CatalogUiState {
    data object Loading: CatalogUiState
    data class Content(val videos: List<VideoMetadata>): CatalogUiState
    data class Error(val message: String): CatalogUiState
}