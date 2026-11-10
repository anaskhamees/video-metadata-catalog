
package com.videocatalog.app.client

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.videocatalog.app.client.ui.theme.VideoCatalogTheme

class MainActivity : ComponentActivity() {

    /**
     * ViewModel instance associated with this Activity.
     *
     * by viewModels() is a Kotlin property delegate provided by AndroidX.
     *
     * It obtains the ViewModel from the Activity's ViewModelStore.
     *
     * Important:
     * - The ViewModel survives configuration changes, such as screen rotation.
     * - When the Activity is recreated after rotation, the existing ViewModel
     *   instance is reused instead of creating a new one.
     * - The ViewModel is not destroyed just because the Activity is recreated.
     * - It does NOT automatically survive process death.
     *
     * Why val?
     * val means this property cannot be reassigned to another ViewModel.
     *
     * However, it does NOT mean that the ViewModel object is immutable.
     * We can still call its functions, and its internal state can change.
     *
     * Example:
     * viewModel = CatalogViewModel() // ERROR: cannot reassign a val
     * viewModel.refresh()            // OK: we can call its functions
     */
    private val viewModel: CatalogViewModel by viewModels()

    /**
     * Called by Android when this Activity is created.
     *
     * @param savedInstanceState
     * Nullable Bundle containing previously saved Activity state.
     *
     * Bundle? means the parameter can hold either:
     * - A Bundle object.
     * - null.
     *
     * Why nullable?
     * - First launch: usually null because there is no previous state.
     * - Configuration change: may contain previously saved state.
     * - Process recreation: may contain state Android previously saved.
     *
     * Kotlin uses ? to explicitly represent nullable types.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /**
         * Allows the application to draw behind system bars
         * (status bar and navigation bar).
         *
         * This gives the application more control over its screen layout.
         */
        enableEdgeToEdge()

        /**
         * setContent is the entry point for building the Activity UI
         * using Jetpack Compose.
         *
         * Instead of using XML layouts and setContentView(),
         * we describe the UI using Kotlin composable functions.
         */
        setContent {

            /**
             * VideoCatalogTheme is the application's Compose theme.
             *
             * It provides shared visual settings to all composables
             * inside it, such as:
             * - Colors
             * - Typography (font sizes, weights, styles)
             * - Shapes (rounded corners, etc.)
             *
             * It normally wraps MaterialTheme internally.
             *
             * This means CatalogScreen can access theme values through:
             * MaterialTheme.colorScheme
             * MaterialTheme.typography
             * MaterialTheme.shapes
             *
             * The lambda { ... } represents the UI content passed
             * to VideoCatalogTheme.
             */
            VideoCatalogTheme {

                /**
                 * Observe the ViewModel's StateFlow and convert its
                 * latest emitted value into Compose State.
                 *
                 * viewModel.state:
                 * The public StateFlow exposed by the ViewModel.
                 *
                 * collectAsStateWithLifecycle():
                 * - Collects the StateFlow when the Activity lifecycle reaches STARTED.
                 * - Stops collecting when the Activity goes below STARTED,
                 *   for example when it enters the STOPPED state.
                 * - Automatically resumes collection when the Activity
                 *   becomes STARTED again.
                 * - Returns a Compose State object containing the latest value.
                 *
                 * Why lifecycle-aware collection?
                 * We don't need the UI to keep collecting updates while
                 * the Activity is in the background.
                 *
                 * Important:
                 * Stopping collection does NOT destroy the ViewModel
                 * or reset its StateFlow.
                 *
                 * What does 'by' mean?
                 *
                 * Without 'by':
                 * val state = viewModel.state.collectAsStateWithLifecycle()
                 * CatalogScreen(state = state.value)
                 *
                 * Here, state is a Compose State<CatalogUiState> object.
                 * We access its actual value using .value.
                 *
                 * With 'by':
                 * val state by viewModel.state.collectAsStateWithLifecycle()
                 * CatalogScreen(state = state)
                 *
                 * Kotlin property delegation automatically accesses
                 * the Compose State.value for us.
                 *
                 * Therefore, state here is CatalogUiState, not State<CatalogUiState>.
                 *
                 * Why does Compose need this?
                 * Whenever the ViewModel emits a new value, Compose State
                 * is updated. Compose detects the change and schedules
                 * recomposition for the composables that read this state.
                 *
                 * Example:
                 * Loading -> Content(videos)
                 *
                 * CatalogScreen receives the new state and updates
                 * the displayed UI automatically.
                 */
                val state by viewModel.state.collectAsStateWithLifecycle()

                /**
                 * Display the application screen.
                 *
                 * state:
                 * The latest UI state received from the ViewModel.
                 * It could represent Loading, Content, or Error.
                 *
                 * onRefresh:
                 * Passes the ViewModel's refresh function to CatalogScreen.
                 *
                 * viewModel::refresh is a callable reference.
                 * It passes the function itself instead of executing it now.
                 *
                 * It is equivalent to:
                 *
                 * onRefresh = {
                 *     viewModel.refresh()
                 * }
                 *
                 * This allows CatalogScreen to call refresh when the
                 * user presses the Refresh button.
                 *
                 * Data flow:
                 * ViewModel -> StateFlow -> Compose State -> CatalogScreen
                 *
                 * User interaction:
                 * Refresh button -> viewModel.refresh() -> ViewModel
                 * updates StateFlow -> Compose recomposes the UI.
                 */
                CatalogScreen(
                    state = state,
                    onRefresh = viewModel::refresh
                )
            }
        }
    }
}
