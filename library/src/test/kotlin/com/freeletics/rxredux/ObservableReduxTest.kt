package com.freeletics.rxredux

import io.reactivex.Observable
import org.junit.Test
import java.util.concurrent.TimeUnit

class ObservableReduxTest {

    @Test
    fun `SideEffects react on upstream Actions but Reducer Reacts first`() {
        val inputs = listOf("InputAction1", "InputAction2")
        val inputActions = Observable.fromIterable(inputs)
        val sideEffect1: SideEffect<String, String> =
            { actions, state -> actions.filter { inputs.contains(it) }.map { it + "SideEffect1" } }
        val sideEffect2: SideEffect<String, String> =
            { actions, state -> actions.filter { inputs.contains(it) }.map { it + "SideEffect2" } }

        inputActions
            .reduxStore(
                initialState = "InitialState",
                sideEffects = listOf(sideEffect1, sideEffect2)
            ) { state, action ->
                action
            }
            .test()
            .assertValues(
                "InitialState",
                "InputAction1",
                "InputAction1SideEffect1",
                "InputAction1SideEffect2",
                "InputAction2",
                "InputAction2SideEffect1",
                "InputAction2SideEffect2"
            )
            .assertComplete()
            .assertNoErrors()
    }

    @Test
    fun `Empty upstream just emits initial state and completes`() {
        val upstream: Observable<String> = Observable.empty()
        upstream.reduxStore(
            "InitialState",
            sideEffects = emptyList()
        ) { state, action -> state }
            .test()
            .assertNoErrors()
            .assertValues("InitialState")
            .assertComplete()
    }

    @Test
    fun `Error upstream just emits initial state and run in onError`() {
        val exception = Exception("FakeException")
        val upstream: Observable<String> = Observable.error<String>(exception)
        upstream.reduxStore(
            "InitialState",
            sideEffects = emptyList()
        ) { state, action -> state }
            .test()
            .assertValues("InitialState")
            .assertError(exception)
    }
}
