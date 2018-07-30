package com.freeletics.rxredux

import android.content.Intent
import android.support.test.espresso.Espresso
import android.support.test.espresso.action.ViewActions
import android.support.test.espresso.contrib.RecyclerViewActions
import android.support.test.espresso.matcher.ViewMatchers
import android.support.test.rule.ActivityTestRule
import android.support.test.rule.GrantPermissionRule
import android.support.test.runner.AndroidJUnit4
import android.support.v7.widget.RecyclerView
import com.freeletics.rxredux.businesslogic.pagination.PaginationStateMachine
import io.reactivex.Observable
import okhttp3.mockwebserver.MockWebServer
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import timber.log.Timber

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule
    val activityTestRule = ActivityTestRule(MainActivity::class.java, false, false)

    @get:Rule
    val permission = GrantPermissionRule.grant(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)

    @Test
    fun runTests() {
        // Setup test environment
        MainScreenSpec(
            screen = AndroidScreen(activityTestRule),
            stateRecorder = AndroidStateRecorder(),
            config = MainScreenConfig(mockWebServer = MockWebServer().setupForHttps())
        ).runTests()
    }

    class AndroidScreen(
        private val activityRule: ActivityTestRule<MainActivity>
    ) : Screen {
        override fun scrollTo(itemAtPosition: Int) {
            Timber.d("Scroll to $itemAtPosition")
            Espresso
                .onView(ViewMatchers.withId(R.id.recyclerView))
                .perform(
                    RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(
                        10
                    )
                )
        }

        override fun retryLoadingFirstPage() {
            Espresso.onView(ViewMatchers.withId(R.id.error))
                .perform(ViewActions.click())
        }

        override fun loadFirstPage() {
            activityRule.launchActivity(Intent())
        }
    }

    inner class AndroidStateRecorder : StateRecorder {

        override fun renderedStates(): Observable<PaginationStateMachine.State> =
            RecordingMainViewBinding.INSTANCE.recordedStates
    }
}
