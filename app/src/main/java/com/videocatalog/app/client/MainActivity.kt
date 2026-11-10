
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


    /**
     * Holds the CatalogViewModel instance associated with this Activity.
     *
     * by viewModels() is a Kotlin property delegate provided by AndroidX.
     *
     * WHAT DOES THE DELEGATE DO?
     * - It manages how this Activity obtains its ViewModel.
     * - It looks for the ViewModel in the Activity's ViewModelStore.
     * - If an appropriate ViewModel already exists, it returns that instance.
     * - Otherwise, it uses the supplied Factory to create a new instance.
     *
     * LIFECYCLE:
     * - The ViewModel survives Activity configuration changes, such as screen rotation.
     * - When Android destroys and recreates the Activity during rotation,
     *   the new Activity obtains the existing ViewModel from its ViewModelStore.
     * - Therefore, the ViewModel is not recreated just because the Activity rotates.
     * - The ViewModel does NOT automatically survive process death.
     *
     * WHY val?
     * - val means the property cannot be reassigned to another ViewModel.
     * - It does NOT mean that the ViewModel object itself is immutable.
     * - The ViewModel can still change its internal state and execute functions.
     *
     * Example:
     * viewModel = otherViewModel     // ERROR: cannot reassign a val.
     * viewModel.refresh()            // OK: calling a function is allowed.
     */
    private val viewModel: CatalogViewModel by viewModels {

        /*
         * FACTORY:
         *
         * A ViewModel normally needs a way to be constructed.
         *
         * The default factory can only build a ViewModel whose constructor is empty,
         * or takes only Application and/or SavedStateHandle.
         * Our CatalogViewModel requires a CatalogRepository parameter (an interface),
         * so the default factory cannot build it.
         *
         * Therefore, we provide a custom Factory that explains HOW to create it.
         *
         * IMPORTANT:
         * Providing this factory does not mean the ViewModel is recreated every time.
         * The factory is used only when the ViewModelStore does not already contain
         * the requested ViewModel.
         *
         * COMPOSITION ROOT:
         * This is the place where we connect the application components together:
         * - We choose the concrete repository implementation.
         * - We pass that repository into the ViewModel.
         */

        viewModelFactory {

            /*
             * INITIALIZER:
             *
             * Defines the actual construction instructions for CatalogViewModel.
             *
             * It is executed when the ViewModel needs to be created.
             *
             * applicationContext:
             * - Provides the Application-level Context.
             * - It is not tied to this Activity's lifecycle.
             * - Using it avoids keeping an Activity reference inside the repository.
             *
             * ServiceCatalogRepository(applicationContext):
             * - Creates the concrete repository responsible for communicating
             *   with CatalogService.
             * - The ViewModel only sees it as a CatalogRepository (the interface),
             *   so tests can pass a fake implementation instead.
             *
             * CatalogViewModel(repository):
             * - Creates the ViewModel and injects the repository through its
             *   constructor.
             *
             * This is constructor dependency injection:
             * We provide the dependency instead of making the ViewModel create it.
             */
            initializer {
                CatalogViewModel(
                    ServiceCatalogRepository(applicationContext)
                )
            }
        }
    }


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
