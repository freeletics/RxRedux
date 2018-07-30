package com.freeletics.rxredux

import android.arch.core.executor.testing.InstantTaskExecutorRule
import com.freeletics.di.TestApplicationModule
import com.freeletics.rxredux.businesslogic.pagination.Action
import com.freeletics.rxredux.businesslogic.pagination.PaginationStateMachine
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.ReplaySubject
import io.reactivex.subjects.Subject
import okhttp3.mockwebserver.MockWebServer
import org.junit.Rule
import org.junit.Test
import timber.log.Timber

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class MainScreenJvmTest {

    class JvmScreen(
        private val viewModel: MainViewModel
    ) : Screen, StateRecorder {
        val stateSubject: Subject<PaginationStateMachine.State> = ReplaySubject.create()

        override fun scrollToEndOfList() {
            Observable.just(Action.LoadNextPageAction).subscribe(viewModel.input)
        }

        override fun retryLoadingFirstPage() {
            Observable.just(Action.LoadFirstPageAction).subscribe(viewModel.input)
        }

        override fun loadFirstPage() {
            Observable.just(Action.LoadFirstPageAction).subscribe(viewModel.input)
        }

        override fun renderedStates(): Observable<PaginationStateMachine.State> = stateSubject
    }


    @Rule
    @JvmField
    val rule = InstantTaskExecutorRule()


    @Test
    fun runTests() {
        Timber.plant(object : Timber.Tree() {
            override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                println(message)
                t?.printStackTrace()
            }
        })
        val applicationComponent = DaggerTestComponent.builder().applicationModule(
            TestApplicationModule(
                baseUrl = "http://127.0.0.1:$MOCK_WEB_SERVER_PORT",
                viewBindingInstantiatorMap = emptyMap(),
                androidScheduler = Schedulers.trampoline()
            )
        ).build()

        val paginationStateMachine = applicationComponent
            .paginationStateMachine()

        val viewModel = MainViewModel(paginationStateMachine, Schedulers.trampoline())
        val screen = JvmScreen(viewModel)
        viewModel.state.observeForever {
            screen.stateSubject.onNext(it!!)
        }

        val mockWebServer = MockWebServer()
        mockWebServer.setupForHttps()
        mockWebServer.use {
            MainScreenSpec(
                config = MainScreenConfig(it),
                screen = screen,
                stateRecorder = screen
            ).runTests()
        }
    }
}
