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

/**
 * ================================================================
 * 1. CATALOG SCREEN
 * ================================================================
 *
 * @Composable:
 * An annotation provided by Jetpack Compose.
 *
 * It tells the Compose compiler that this function participates
 * in describing the application's UI.
 *
 * Unlike traditional Android development, where we use XML files
 * to describe layouts, Compose allows us to build the UI directly
 * using Kotlin functions.
 *
 *
 * Stateless screen:
 *
 * This screen does NOT own or store the application's UI state.
 *
 * Instead:
 * - The ViewModel owns the UI state.
 * - MainActivity observes the ViewModel's StateFlow.
 * - MainActivity passes the current CatalogUiState to this screen.
 * - This screen displays the UI according to the received state.
 *
 * The screen also reports user interactions through callbacks.
 *
 * Parameters:
 *
 * @param state
 * The current UI state received from the ViewModel.
 *
 * Its possible values are:
 * - CatalogUiState.Loading
 * - CatalogUiState.Content
 * - CatalogUiState.Error
 *
 * @param onRefresh
 * A function provided by the caller.
 *
 * Type: () -> Unit
 *
 * - () means the function accepts no arguments.
 * - -> separates the parameters from the return type.
 * - Unit means it does not return a meaningful value.
 *
 * The screen invokes this callback when the user presses Refresh.
 * It does not implement the refresh operation itself.
 *
 * @param modifier
 * Allows the caller to customize the layout of this screen.
 *
 * Modifier is a Compose object used to describe layout and
 * appearance properties such as width, height, padding and alignment.
 *
 * Modifier = Modifier means that an empty Modifier is used
 * when the caller does not provide one.
 */
@Composable
fun CatalogScreen(
    state: CatalogUiState,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {

    /**
     * Scaffold:
     *
     * A Material 3 layout component that provides the basic
     * structure of an application screen.
     *
     * It can accommodate:
     * - Top app bar
     * - Bottom navigation
     * - Floating action button
     * - Snackbar
     * - Main screen content
     *
     * We currently use Scaffold as the main screen container.
     *
     * modifier.fillMaxSize():
     * Makes Scaffold occupy the maximum available width and height
     * permitted by its parent.
     *
     * Notice that Modifier functions are chained together.
     */
    Scaffold(
        modifier = modifier.fillMaxSize()
    ) {

        /**
         * innerPadding:
         *
         * Scaffold provides this value to its content lambda.
         *
         * It represents padding needed to account for Scaffold's
         * content-related insets and bars, where applicable.
         *
         * We pass it to the main Column to prevent the content
         * from overlapping those areas.
         *
         * The syntax:
         *
         * { innerPadding -> }
         *
         * Is a Kotlin lambda.
         *
         * innerPadding is the lambda parameter.
         */

            innerPadding ->

        /**
         * Column:
         *
         * A Compose layout that arranges its children vertically.
         *
         * Similar to a vertical LinearLayout in traditional Android.
         *
         * Its children will be arranged from top to bottom.
         *
         * Modifier chain:
         *
         * 1. padding(innerPadding)
         *    Respects the padding supplied by Scaffold.
         *
         * 2. fillMaxSize()
         *    Makes Column occupy the maximum available size.
         *
         * 3. wrapContentWidth(Alignment.CenterHorizontally)
         *    Uses the required content width instead of automatically
         *    filling all available horizontal space, and centers it.
         *
         * 4. widthIn(max = 840.dp)
         *    Restricts the maximum width to 840 density-independent
         *    pixels.
         *
         *    This is useful for wide automotive displays.
         *    It prevents the content from becoming excessively wide.
         *
         * 5. padding(16.dp)
         *    Adds 16 dp of spacing around the Column's content.
         *
         * IMPORTANT:
         * Modifier order matters because modifiers are applied
         * sequentially and can affect layout constraints.
         *
         * dp:
         * Density-independent pixels.
         * They help keep UI dimensions reasonably consistent
         * across screens with different pixel densities.
         */
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = 840.dp)
                .padding(16.dp)
        ) {

            /**
             * ====================================================
             * 2. SCREEN HEADER
             * ====================================================
             *
             * Row:
             *
             * A Compose layout that arranges its children horizontally.
             *
             * Similar to a horizontal LinearLayout in traditional Android.
             *
             * We use it to place:
             *
             * [ Video Catalog ]                [ Refresh ]
             *
             * verticalAlignment:
             *
             * Alignment.CenterVertically means that children are
             * vertically centered relative to the Row's height.
             */
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                /**
                 * Text:
                 *
                 * A composable that displays text on the screen.
                 *
                 * text:
                 * The actual String to display.
                 *
                 * style:
                 * MaterialTheme.typography.headlineSmall retrieves
                 * a predefined typography style from our application
                 * theme.
                 *
                 * MaterialTheme provides shared visual settings:
                 * - Colors
                 * - Typography
                 * - Shapes
                 *
                 * Modifier.weight(1f):
                 *
                 * weight() is available inside a Row's scope.
                 *
                 * It tells Row to allocate the remaining available
                 * horizontal space to this Text.
                 *
                 * The Refresh button takes the width it needs,
                 * while the title gets the remaining space.
                 *
                 * 1f is a Float weight value.
                 */
                Text(
                    text = "Video Catalog",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f)
                )

                /**
                 * Button:
                 *
                 * A Material 3 clickable button.
                 *
                 * onClick:
                 *
                 * We pass the onRefresh function received as a
                 * parameter of CatalogScreen.
                 *
                 * Notice:
                 *
                 * onClick = onRefresh
                 *
                 * We pass the function itself.
                 * We do NOT execute it here.
                 *
                 * When the user clicks the button, Compose invokes it.
                 *
                 * enabled:
                 *
                 * Controls whether the button can be clicked.
                 *
                 * state !is CatalogUiState.Loading
                 *
                 * Breakdown:
                 * - is checks whether an object belongs to a type.
                 * - !is means it does NOT belong to that type.
                 *
                 * Therefore:
                 * - Loading -> button disabled.
                 * - Content -> button enabled.
                 * - Error   -> button enabled.
                 *
                 * This prevents repeated refresh requests while
                 * the application is already loading.
                 *
                 * The trailing lambda { } represents the button's
                 * content, which in this case is a Text composable.
                 */
                Button(
                    onClick = onRefresh,
                    enabled = state !is CatalogUiState.Loading
                ) {
                    Text("Refresh")
                }
            }

            /**
             * Spacer:
             *
             * A composable used to create empty space between elements.
             *
             * Modifier.height(12.dp):
             * Creates a vertical space of 12 dp.
             *
             * This separates the header from the main screen content.
             */
            Spacer(
                Modifier.height(12.dp)
            )

            /**
             * ====================================================
             * 3. DISPLAY UI ACCORDING TO THE CURRENT STATE
             * ====================================================
             *
             * when:
             *
             * Kotlin control-flow expression.
             * Similar to switch in C++.
             *
             * Here, we inspect the current CatalogUiState and decide
             * which composable should be displayed.
             *
             * This is an example of declarative UI programming:
             *
             * We describe WHAT the UI should display for a given
             * state instead of manually modifying individual Views.
             *
             * Whenever the observed state changes, Compose can
             * recompose the affected UI.
             */

            when (state) {

                /**
                 * ------------------------------------------------
                 * CASE 1: LOADING
                 * ------------------------------------------------
                 *
                 * When the state is Loading, display a progress
                 * indicator in the center of the available area.
                 *
                 * CatalogUiState.Loading is a singleton object
                 * in the sealed interface.
                 */

                CatalogUiState.Loading ->

                    /**
                     * Box:
                     *
                     * A Compose layout that places its children
                     * inside a common area.
                     *
                     * It supports positioning and alignment.
                     *
                     * fillMaxSize():
                     * Makes Box occupy the maximum available space.
                     *
                     * contentAlignment = Alignment.Center:
                     * Places the Box's child in the center.
                     */
                    Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {

                        /**
                         * CircularProgressIndicator:
                         *
                         * A Material 3 composable that displays
                         * a circular loading animation.
                         *
                         * It informs the user that an operation
                         * is currently in progress.
                         */
                        CircularProgressIndicator()
                    }

                /**
                 * ------------------------------------------------
                 * CASE 2: ERROR
                 * ------------------------------------------------
                 *
                 * is CatalogUiState.Error:
                 *
                 * Checks whether state is an instance of Error.
                 *
                 * Error is a data class containing a message.
                 *
                 * Smart Casting:
                 *
                 * Kotlin automatically recognizes that state
                 * is CatalogUiState.Error inside this branch.
                 *
                 * Therefore, we can directly access:
                 *
                 * state.message
                 *
                 * Without manually casting the object.
                 */

                is CatalogUiState.Error ->

                    /**
                     * Display the error message.
                     *
                     * MaterialTheme.colorScheme.error:
                     * Retrieves the error color from the application's
                     * Material theme.
                     *
                     * This keeps error styling consistent throughout
                     * the application.
                     */
                    Text(
                        state.message,
                        color = MaterialTheme.colorScheme.error
                    )

                /**
                 * ------------------------------------------------
                 * CASE 3: CONTENT
                 * ------------------------------------------------
                 *
                 * is CatalogUiState.Content:
                 *
                 * Checks whether state contains successfully
                 * retrieved video catalog data.
                 *
                 * Content is a data class containing:
                 *
                 * videos: List<VideoMetadata>
                 *
                 * Kotlin Smart Casting allows us to access
                 * state.videos directly inside this branch.
                 */
                is CatalogUiState.Content ->

                    /**
                     * Check whether the video collection is empty.
                     *
                     * isEmpty():
                     * Returns true if the collection contains no elements.
                     */
                    if (state.videos.isEmpty()) {

                        /**
                         * No videos were found.
                         *
                         * Display a message instead of an empty list.
                         */
                        Text("No files in the service folder.")

                    } else {

                        /**
                         * Videos exist.
                         *
                         * Delegate their display to VideoList().
                         *
                         * We pass the video collection as an argument.
                         *
                         * This keeps CatalogScreen focused on managing
                         * the overall screen layout and UI state.
                         */
                        VideoList(state.videos)
                    }
            }
        }
    }
}


/**
 * ================================================================
 * 4. VIDEO LIST
 * ================================================================
 *
 * @Composable:
 * This function describes a part of the Compose UI.
 *
 * private:
 * This function is accessible only within this Kotlin file.
 *
 * @param videos:
 * A List containing VideoMetadata objects.
 *
 * Each VideoMetadata object represents one video file and its
 * extracted information.
 *
 * Responsibility:
 * Display all received videos in a vertically scrollable list.
 */
@Composable
private fun VideoList(
    videos: List<VideoMetadata>
) {

    /**
     * LazyColumn:
     *
     * A vertically scrolling Compose list.
     *
     * Similar to RecyclerView in traditional Android development.
     *
     * Why LazyColumn instead of Column?
     *
     * A normal Column generally composes all its children.
     *
     * LazyColumn composes and lays out items as they are needed
     * for the visible area and scrolling.
     *
     * This makes it more suitable for potentially large collections.
     *
     * verticalArrangement:
     *
     * Arrangement.spacedBy(8.dp) adds 8 dp of vertical spacing
     * between list items.
     */
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        /**
         * items():
         *
         * A LazyListScope DSL function.
         *
         * It tells LazyColumn how to create an item for each
         * element in the videos collection.
         *
         * For example, if videos contains three objects:
         *
         * video1, video2, video3
         *
         * LazyColumn will compose the corresponding VideoRow
         * items as they are needed.
         *
         * key:
         *
         * Provides a stable identity for each list item.
         *
         * { it.fileName } is a Kotlin lambda.
         *
         * 'it' is the implicit name for the single lambda parameter.
         * It represents the current VideoMetadata object.
         *
         * Therefore:
         * it.fileName returns the filename of the current video.
         *
         * Why stable keys?
         *
         * They help Compose identify items when the collection changes.
         *
         * IMPORTANT:
         * Keys must be unique within this LazyColumn.
         * Filenames work here if duplicate filenames are impossible.
         *
         * { video -> VideoRow(video) }:
         *
         * The lambda receives one VideoMetadata object.
         *
         * We pass that object to VideoRow() to display it.
         */
        items(
            videos,
            key = { it.fileName }
        ) { video ->

            VideoRow(video)
        }
    }
}


/**
 * ================================================================
 * 5. INDIVIDUAL VIDEO ROW
 * ================================================================
 *
 * @param video:
 * The VideoMetadata object representing one video.
 *
 * Responsibility:
 * Display the filename and either:
 * - Successfully extracted metadata.
 * - An error description.
 *
 * Notice that this function handles only ONE video.
 *
 * VideoList() calls this function for each video item.
 */
@Composable
private fun VideoRow(
    video: VideoMetadata
) {

    /**
     * Card:
     *
     * A Material 3 composable that provides a card-shaped container
     * for related information.
     *
     * fillMaxWidth():
     * Makes the Card occupy the available horizontal space.
     */
    Card(
        Modifier.fillMaxWidth()
    ) {

        /**
         * Column:
         *
         * Arranges the Card's children vertically.
         *
         * padding(12.dp):
         * Adds internal spacing between the card boundaries
         * and its content.
         */
        Column(
            Modifier.padding(12.dp)
        ) {

            /**
             * Display the video filename.
             *
             * MaterialTheme.typography.titleMedium:
             * Uses a predefined Material typography style.
             */
            Text(
                video.fileName,
                style = MaterialTheme.typography.titleMedium
            )

            /**
             * Check the extraction status.
             *
             * ExtractStatus.OK means the native extractor
             * successfully retrieved the required video metadata.
             *
             * If successful:
             * Display the formatted metadata summary.
             *
             * Otherwise:
             * Display an appropriate error description.
             *
             * IMPORTANT ARCHITECTURAL SEPARATION:
             *
             * The NDK/JNI layer extracts raw metadata.
             * The IPC layer transports that metadata.
             * The UI layer decides how to present it.
             */
            if (video.status == ExtractStatus.OK) {

                /**
                 * summary():
                 *
                 * A Kotlin extension function defined below.
                 *
                 * Converts the raw metadata into a human-readable
                 * String.
                 */
                Text(
                    video.summary(),
                    style = MaterialTheme.typography.bodyMedium
                )

            } else {

                /**
                 * errorText():
                 *
                 * Another extension function.
                 *
                 * Converts the extraction status into a
                 * human-readable error message.
                 *
                 * colorScheme.error:
                 * Displays the message using the theme's error color.
                 */
                Text(
                    video.errorText(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}


/**
 * ================================================================
 * 6. VIDEO METADATA FORMATTING
 * ================================================================
 *
 * Formatting lives only in the UI.
 *
 * The IPC layer transports raw values instead of presentation strings.
 *
 * For example, the service transports:
 *
 * durationUs = 5_000_000
 * width = 1280
 * height = 720
 * videoMime = "video/avc"
 * hasAudio = true
 *
 * The UI transforms these values into:
 *
 * 0:05 · 1280×720 · video/avc · Audio: yes
 *
 * This keeps the service independent of the UI presentation.
 *
 *
 * Kotlin Extension Function:
 *
 * fun VideoMetadata.summary()
 *
 * Adds a callable extension function to VideoMetadata without
 * modifying the original VideoMetadata class.
 *
 * It allows us to write:
 *
 * video.summary()
 *
 * Instead of:
 *
 * summary(video)
 *
 * Return type:
 * String
 */
private fun VideoMetadata.summary(): String = listOf(

    /**
     * Convert the duration from microseconds into MM:SS format.
     */
    formatDuration(durationUs),

    /**
     * Check whether valid video dimensions are available.
     *
     * width > 0 && height > 0:
     *
     * && is the logical AND operator.
     * Both conditions must be true.
     *
     * If valid:
     * Display width × height.
     *
     * Otherwise:
     * Display a dash to indicate unavailable information.
     *
     * "$width×$height":
     * Kotlin string interpolation inserts variable values
     * into the String.
     */
    if (width > 0 && height > 0) "$width×$height" else "—",

    /**
     * Elvis operator (?:):
     *
     * videoMime is nullable.
     *
     * If videoMime is NOT null:
     * Use its actual value.
     *
     * If videoMime IS null:
     * Use "—" as a fallback.
     *
     * Equivalent logic:
     *
     * if (videoMime != null) {
     *     videoMime
     * } else {
     *     "—"
     * }
     */
    videoMime ?: "—",

    /**
     * audioText():
     *
     * Returns either:
     * "Audio: yes"
     * "Audio: no"
     *
     * Implemented as another extension function below.
     */
    audioText()

    /**
     * joinToString():
     *
     * Joins all elements of the list into one String.
     *
     * The separator is:
     * " · "
     *
     * Example result:
     *
     * 0:05 · 1280×720 · video/avc · Audio: yes
     */
).joinToString(" · ")


/**
 * ================================================================
 * 7. VIDEO EXTRACTION ERROR FORMATTING
 * ================================================================
 *
 * Another extension function on VideoMetadata.
 *
 * Responsibility:
 * Convert the extraction status into a user-readable error.
 *
 * when(status):
 * Checks the current extraction status.
 *
 * NO_VIDEO_TRACK:
 * The file does not contain a video track.
 *
 * else:
 * Handles all other unsuccessful extraction statuses.
 */
private fun VideoMetadata.errorText(): String =
    when (status) {

        /**
         * If there is no video track, display a specific message.
         *
         * ${audioText()}:
         * String interpolation evaluates the audioText() function
         * and inserts its returned String into the message.
         */
        ExtractStatus.NO_VIDEO_TRACK ->
            "No video track · ${audioText()}"

        /**
         * Any other status not handled above.
         *
         * Display a general error message.
         */
        else ->
            "Unreadable file"
    }


/**
 * ================================================================
 * 8. AUDIO INFORMATION FORMATTING
 * ================================================================
 *
 * Extension function on VideoMetadata.
 *
 * hasAudio is a Boolean:
 * - true  -> The file contains an audio track.
 * - false -> The file does not contain an audio track.
 *
 * Kotlin can infer the return type as String because both
 * branches of the if expression return Strings.
 */
private fun VideoMetadata.audioText() =
    if (hasAudio) "Audio: yes" else "Audio: no"


/**
 * ================================================================
 * 9. DURATION FORMATTING
 * ================================================================
 *
 * Converts a duration expressed in microseconds into MM:SS.
 *
 * @param us:
 * Duration in microseconds.
 *
 * Long:
 * A 64-bit signed integer type.
 *
 * @return:
 * A formatted String representing the duration.
 */
private fun formatDuration(us: Long): String {

    /**
     * Negative duration means that a valid duration
     * is not available.
     *
     * Return a dash instead of displaying an invalid value.
     */
    if (us < 0) return "—"

    /**
     * Convert microseconds into seconds.
     *
     * 1 second = 1,000,000 microseconds.
     *
     * Why add 500,000?
     *
     * To round to the nearest second instead of simply
     * truncating the fractional part.
     *
     * Example:
     *
     * 1,200,000 us -> 1 second
     * 1,600,000 us -> 2 seconds
     * 5,000,000 us -> 5 seconds
     */
    val seconds = (us + 500_000) / 1_000_000

    /**
     * String formatting:
     *
     * %d   -> Integer.
     * %02d -> Integer padded with a leading zero when necessary.
     *
     * seconds / 60:
     * Integer division calculates the number of complete minutes.
     *
     * seconds % 60:
     * Modulo calculates the remaining seconds.
     *
     * Example:
     *
     * seconds = 125
     *
     * minutes = 125 / 60 = 2
     * remaining seconds = 125 % 60 = 5
     *
     * Result:
     * 2:05
     */
    return "%d:%02d".format(
        seconds / 60,
        seconds % 60
    )
}


/**
 * ================================================================
 * 10. COMPOSE PREVIEW
 * ================================================================
 *
 * @Preview:
 *
 * An Android Studio annotation that allows us to render
 * a Compose UI directly inside the IDE.
 *
 * We do not need to launch the application on an emulator
 * just to see this screen.
 *
 * showBackground = true:
 * Displays a background behind the preview content.
 *
 * @Composable:
 * The preview function itself builds Compose UI.
 *
 * IMPORTANT:
 *
 * This function is only for development and visualization.
 * It is not called as part of the normal application flow.
 */
@Preview(showBackground = true)
@Composable
private fun CatalogScreenPreview() {

    /**
     * Create a sample VideoMetadata object.
     *
     * We use fake data instead of calling the actual:
     *
     * Service -> AIDL -> JNI -> NDK
     *
     * This allows Android Studio to preview the UI
     * independently of the backend.
     *
     * VideoMetadata():
     * Creates a new VideoMetadata object.
     *
     * apply { }:
     * Kotlin scope function.
     *
     * Inside the apply block, 'this' refers to the
     * VideoMetadata object being configured.
     *
     * We can assign its properties directly without
     * repeatedly writing sample.fileName, sample.width, etc.
     *
     * apply returns the configured object.
     */
    val sample = VideoMetadata().apply {

        fileName = "h264_aac_720p.mp4"

        status = ExtractStatus.OK

        durationUs = 5_000_000

        width = 1280

        height = 720

        videoMime = "video/avc"

        hasAudio = true
    }

    /**
     * VideoCatalogTheme:
     *
     * Wraps the preview in the application's Material theme.
     *
     * This provides:
     * - Color scheme
     * - Typography
     * - Shapes
     *
     * The screen uses MaterialTheme values, so the theme
     * should also be applied when previewing it.
     *
     * CatalogUiState.Content(listOf(sample)):
     *
     * Creates a Content state containing one sample video.
     *
     * listOf(sample):
     * Creates a Kotlin List containing our sample object.
     *
     * onRefresh = { }:
     *
     * Provides an empty lambda.
     *
     * It means that pressing Refresh in the preview
     * does nothing.
     *
     * Why?
     * Because we are previewing the UI, not testing the
     * actual ViewModel refresh operation.
     */
    VideoCatalogTheme {

        CatalogScreen(
            state = CatalogUiState.Content(
                listOf(sample)
            ),
            onRefresh = {}
        )
    }
}
