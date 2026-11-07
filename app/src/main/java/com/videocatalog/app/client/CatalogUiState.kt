package com.videocatalog.app.client

import com.videocatalog.app.VideoMetadata

// sealed = restricts the possible states to the types defined here.
sealed interface CatalogUiState {

    // data object = one fixed state; no properties or data to store.
    data object Loading : CatalogUiState

    // data class = a state that carries data (here, a list of videos).
    data class Content(val videos: List<VideoMetadata>) : CatalogUiState

    // data class = a state that carries an error message.
    data class Error(val message: String) : CatalogUiState
}