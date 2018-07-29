package com.freeletics.rxredux

import com.freeletics.rxredux.businesslogic.pagination.PaginationStateMachine
import io.reactivex.Observable
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert
import java.util.concurrent.TimeUnit


/**
 * Abstraction layer that shows what a user can do on the screen
 */
interface Screen {
    /**
     * Scroll the list to the item at position
     */
    fun scrollTo(itemAtPosition: Int)

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
            }
        }

        fun it(message: String, expectedState: PaginationStateMachine.State) {
            val it = It("$composedMessage - $message")
            it.renderedState(expectedState)
        }
    }

    fun on(message: String, block: On.() -> Unit) {
        val on = On(" - $composedMessage - $message")
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
            val connectionErrorMessage = "Failed to connect to /127.0.0.1:$MOCK_WEB_SERVER_PORT"

            on("device is offline") {
                screen.loadFirstPage()
                it("shows loading first", PaginationStateMachine.State.LoadingFirstPageState)
                it(
                    "shows error",
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
                screen.scrollTo(FIRST_PAGE.size - 1)

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
                screen.scrollTo(FIRST_PAGE.size + SECOND_PAGE.size - 1)

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
