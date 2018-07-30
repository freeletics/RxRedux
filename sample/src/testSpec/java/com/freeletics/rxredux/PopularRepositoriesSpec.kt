package com.freeletics.rxredux

import com.freeletics.rxredux.businesslogic.pagination.PaginationStateMachine
import io.reactivex.Observable
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert
import timber.log.Timber
import java.util.concurrent.TimeUnit


/**
 * Abstraction layer that shows what a user can do on the screen
 */
interface Screen {
    /**
     * Scroll the list to the item at position
     */
    fun scrollToEndOfList()

    /**
     * Action on the screen: Clicks on the retry button to retry loading the first page
     */
    fun retryLoadingFirstPage()

    /**
     * Launches the screen.
     * After having this called, the screen is visible
     */
    fun loadFirstPage()
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
                Timber.d("✅ $composedMessage")

            }
        }

        fun it(message: String, expectedState: PaginationStateMachine.State) {
            val it = It("$composedMessage *IT* $message")
            it.renderedState(expectedState)
        }
    }

    fun on(message: String, block: On.() -> Unit) {
        val on = On("*GIVEN* $composedMessage *ON* $message")
        on.block()
    }
}

/**
 * A simple holder object for all required configuration
 */
data class ScreenConfig(
    val mockWebServer: MockWebServer
)

class PopularRepositoriesSpec(
    private val screen: Screen,
    private val stateRecorder: StateRecorder,
    private val config: ScreenConfig
) {

    private fun given(message: String, block: Given.() -> Unit) {
        val given = Given(screen, stateRecorder, message)
        given.block()
    }

    fun runTests() {
        given("the Repositories List Screen") {

            val server = config.mockWebServer
            val connectionErrorMessage = "Failed to connect to /127.0.0.1:$MOCK_WEB_SERVER_PORT"

            on("device is offline") {

                server.shutdown()
                screen.loadFirstPage()
                it("shows loading first page", PaginationStateMachine.State.LoadingFirstPageState)
                it(
                    "shows error loading first page",
                    PaginationStateMachine.State.ErrorLoadingFirstPageState(connectionErrorMessage)
                )
            }

            on("device is online again and user clicks retry loading first page") {

                server.enqueue200(FIRST_PAGE)
                server.start(MOCK_WEB_SERVER_PORT)

                screen.retryLoadingFirstPage()

                it("shows loading", PaginationStateMachine.State.LoadingFirstPageState)

                it(
                    "shows first page", PaginationStateMachine.State.ShowContentState(
                        items = FIRST_PAGE,
                        page = 1
                    )
                )
            }

            on("scrolling to the end of the first page") {

                server.enqueue200(SECOND_PAGE)
                screen.scrollToEndOfList()

                it(
                    "shows loading next page",
                    PaginationStateMachine.State.ShowContentAndLoadNextPageState(
                        items = FIRST_PAGE,
                        page = 1
                    )
                )

                it(
                    "shows next page content",
                    PaginationStateMachine.State.ShowContentState(
                        items = FIRST_PAGE + SECOND_PAGE,
                        page = 2
                    )
                )
            }

            on("device is offline again and scrolling to end of second page") {

                server.shutdown()
                screen.scrollToEndOfList()

                it(
                    "shows loading next page",
                    PaginationStateMachine.State.ShowContentAndLoadNextPageState(
                        items = FIRST_PAGE + SECOND_PAGE,
                        page = 2
                    )
                )

                it(
                    "shows error info for few seconds on top of the list of items",
                    PaginationStateMachine.State.ShowContentAndLoadNextPageErrorState(
                        items = FIRST_PAGE + SECOND_PAGE,
                        page = 2,
                        errorMessage = connectionErrorMessage
                    )
                )

                it(
                    "hides error information and shows items only",
                    PaginationStateMachine.State.ShowContentState(
                        items = FIRST_PAGE + SECOND_PAGE,
                        page = 2
                    )
                )
            }
        }
    }
}
