package com.freeletics.rxredux

import android.content.Intent
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import com.freeletics.rxredux.businesslogic.github.GithubRepository
import com.freeletics.rxredux.businesslogic.github.GithubSearchResults
import com.freeletics.rxredux.businesslogic.pagination.PaginationStateMachine
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.reactivex.Observable
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit


/**
 * Abstraction layer that shows what a user can do on the screen
 */
interface Screen {
    /**
     * Scroll to through the list of items until we reach the end of the screen
     */
    fun scrollToBottom()

    /**
     * Action on the screen: Clicks on the retry button to retry loading the first page
     */
    fun retryLoadingFirstPage()

    /**
     * Launches the screen.
     * After having this called, the screen is visible
     */
    fun startScreen()
}

/**
 * Can record states over time. This states are basically just rendered on the screen
 */
interface StateRecorder {
    /**
     * Observable of recorded States
     */
    fun renderedStates(): Observable<PaginationStateMachine.State>
}


private data class Given(
    private val screen: Screen,
    private val stateRecorder: StateRecorder,
    private val composedMessage: String
) {

    /**
     * All states that has been captured and asserted in an `on`cl
     */
    private var allCapturedStatesSoFar: List<PaginationStateMachine.State> = emptyList()

    inner class On(private val composedMessage: String) {

        inner class It(private val composedMessage: String) {

            fun renderedState(expectedState: PaginationStateMachine.State) {

                val states = stateRecorder.renderedStates()
                    .take(allCapturedStatesSoFar.size + 1L)
                    .toList()
                    .timeout(10, TimeUnit.SECONDS)
                    .blockingGet()

                val expectedStates = allCapturedStatesSoFar + expectedState

                Assert.assertEquals(
                    composedMessage,
                    expectedStates,
                    states
                )
                allCapturedStatesSoFar = states
            }
        }

        fun it(message: String, expectedState: PaginationStateMachine.State) {
            val it = It("$composedMessage\n\t\t$message")
            it.renderedState(expectedState)
        }
    }

    fun on(message: String, block: On.() -> Unit) {
        val on = On("- $composedMessage\n\t\t$message")
        on.block()
    }
}

/**
 * A simple holder object for all required configuration
 */
data class MainScreenConfig(
    val mockWebServer: MockWebServer
)

class MainScreenSpec(
    private val screen: Screen,
    private val stateRecorder: StateRecorder,
    private val config: MainScreenConfig
) {

    private fun given(message: String, block: Given.() -> Unit) {
        val given = Given(screen, stateRecorder, message)
        given.block()
    }

    fun runTests() {
        given("The Main screen") {
            val server = config.mockWebServer
            server.enqueue200(FIRST_PAGE)
            server.start(MOCK_WEB_SERVER_PORT)

            on("device is online and user starts the app") {

                screen.startScreen()

                it("shows loading", PaginationStateMachine.State.LoadingFirstPageState)

                it(
                    "shows first page", PaginationStateMachine.State.ShowContentState(
                        items = FIRST_PAGE,
                        page = 1
                    )
                )
            }
        }
    }
}

private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
private val githubSearchResultsAdapter = moshi.adapter(GithubSearchResults::class.java)

fun MockWebServer.enqueue200(items: List<GithubRepository>) {
    // TODO why is loading resources not working?
   // val body = MainActivityTest::class.java.getResource("response1.json").readText()

    enqueue(
        MockResponse()
            .setBody(githubSearchResultsAdapter.toJson(GithubSearchResults(items)))
    )
}


@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule
    val activityTestRule = ActivityTestRule(MainActivity::class.java, false, false)


    @Test
    fun runTests() {
        // Setup test environment
        MainScreenSpec(
            screen = AndroidScreen(activityTestRule),
            stateRecorder = AndroidStateRecorder(),
            config = MainScreenConfig(mockWebServer = MockWebServer())
        ).runTests()
    }

    class AndroidScreen(
        private val activityRule: ActivityTestRule<MainActivity>
    ) : Screen {
        override fun scrollToBottom() {
        }

        override fun retryLoadingFirstPage() {
        }

        override fun startScreen() {
            val activity = activityRule.launchActivity(Intent())
        }
    }

    class AndroidStateRecorder : StateRecorder {

        override fun renderedStates(): Observable<PaginationStateMachine.State> =
            RecordingMainViewBinding.INSTANCE.recordedStates
    }
}
