package com.videocatalog.app.client

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.videocatalog.app.ExtractStatus
import com.videocatalog.app.VideoMetadata
import com.videocatalog.app.client.ui.theme.VideoCatalogTheme

/** Stateless screen: it only draws [state] and reports Refresh clicks. */
@Composable
fun CatalogScreen(state: CatalogUiState, onRefresh: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        // On a wide car display, cap the width and centre the list; no effect on a phone.
        Column(
            modifier = Modifier.padding(innerPadding).fillMaxSize()
                .wrapContentWidth(Alignment.CenterHorizontally).widthIn(max = 840.dp).padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Video Catalog",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f),
                )
                Button(onClick = onRefresh, enabled = state !is CatalogUiState.Loading) {
                    Text("Refresh")
                }
            }
            Spacer(Modifier.height(12.dp))
            when (state) {
                CatalogUiState.Loading ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                is CatalogUiState.Error ->
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                is CatalogUiState.Content ->
                    if (state.videos.isEmpty()) Text("No files in the service folder.") else VideoList(state.videos)
            }
        }
    }
}

@Composable
private fun VideoList(videos: List<VideoMetadata>) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(videos, key = { it.fileName }) { video -> VideoRow(video) }
    }
}

@Composable
private fun VideoRow(video: VideoMetadata) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(video.fileName, style = MaterialTheme.typography.titleMedium)
            if (video.status == ExtractStatus.OK) {
                Text(video.summary(), style = MaterialTheme.typography.bodyMedium)
            } else {
                Text(video.errorText(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

// Formatting lives only in the UI; the IPC carries raw values.
private fun VideoMetadata.summary(): String = listOf(
    formatDuration(durationUs),
    if (width > 0 && height > 0) "$width×$height" else "—",
    videoMime ?: "—",
    audioText(),
).joinToString(" · ")

private fun VideoMetadata.errorText(): String = when (status) {
    ExtractStatus.NO_VIDEO_TRACK -> "No video track · ${audioText()}"
    else -> "Unreadable file"
}

private fun VideoMetadata.audioText() = if (hasAudio) "Audio: yes" else "Audio: no"

private fun formatDuration(us: Long): String {
    if (us < 0) return "—"
    val seconds = (us + 500_000) / 1_000_000   // round to the nearest second
    return "%d:%02d".format(seconds / 60, seconds % 60)
}

@Preview(showBackground = true)
@Composable
private fun CatalogScreenPreview() {
    val sample = VideoMetadata().apply {
        fileName = "h264_aac_720p.mp4"; status = ExtractStatus.OK
        durationUs = 5_000_000; width = 1280; height = 720; videoMime = "video/avc"; hasAudio = true
    }
    VideoCatalogTheme { CatalogScreen(CatalogUiState.Content(listOf(sample)), onRefresh = {}) }
}