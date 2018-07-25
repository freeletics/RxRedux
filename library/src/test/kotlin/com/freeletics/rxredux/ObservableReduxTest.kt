package com.freeletics.rxredux

import io.reactivex.Observable
import org.junit.Test

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
    }

    @Test
    fun `once upstream of actions complete, reduxstore completes too`(){

    }
}
