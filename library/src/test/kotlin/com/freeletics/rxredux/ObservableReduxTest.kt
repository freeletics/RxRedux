package com.freeletics.rxredux

import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import org.junit.Assert
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

    @Test
    fun `disposing reduxStore disposes all side effects and upstream`() {
        var disposedSideffectsCount = 0
        val dummyAction = "SomeAction"
        val upstream = PublishSubject.create<String>()
        val outputedStates = ArrayList<String>()
        var outputedError: Throwable? = null
        var outputCompleted = false

        val sideEffect1: SideEffect<String, String> = { actions, state ->
            actions.filter { it == dummyAction }.map { "SideEffectAction1" }
                .doOnDispose { disposedSideffectsCount++ }
        }


        val sideEffect2: SideEffect<String, String> = { actions, state ->
            actions.filter { it == dummyAction }.map { "SideEffectAction2" }
                .doOnDispose { disposedSideffectsCount++ }
        }

        val disposable = upstream
            .reduxStore(
                "InitialState", sideEffect1, sideEffect2
            ) { state, action ->
                action
            }
            .subscribeOn(Schedulers.io())
            .subscribe(
                { outputedStates.add(it) },
                { outputedError = it },
                { outputCompleted = true })

        Thread.sleep(100)        // I know it's bad, but it does the job

        // Trigger some action
        upstream.onNext(dummyAction)

        Thread.sleep(100)        // I know it's bad, but it does the job

        // Dispose the whole cain
        disposable.dispose()

        // Verify everything is fine
        Assert.assertEquals(2, disposedSideffectsCount)
        Assert.assertFalse(upstream.hasObservers())
        Assert.assertEquals(
            listOf(
                "InitialState",
                dummyAction,
                "SideEffectAction1",
                "SideEffectAction2"
            ),
            outputedStates
        )
        Assert.assertNull(outputedError)
        Assert.assertFalse(outputCompleted)
    }

    @Test
    fun `SideEffect that returns no Action is supported`() {

        fun returnNoActionEffect(
            actions: Observable<String>,
            accessor: StateAccessor<String>
        ): Observable<String> = actions.flatMap {
            println("Doing something with $it")
            Observable.empty<String>()
        }


        val action1 = "Action1"
        val action2 = "Action2"
        val action3 = "Action3"
        val initial = "Initial"

        Observable.just(action1, action2, action3)
            .reduxStore("Initial", sideEffects = listOf(::returnNoActionEffect)) { state, action ->
                state + action
            }
            .test()
            .assertValues(
                initial,
                initial + action1,
                initial + action1 + action2,
                initial + action1 + action2 + action3
            )
            .assertNoErrors()
    }

    @Test
    fun `exception in reducer enhanced with state and action`() {
        val testException = Exception("test")

        Observable
                .just("Action1")
                .reduxStore("Initial", sideEffects = emptyList()) { _, _ ->
                    throw testException
                }
                .test()
                .assertError(ReducerException::class.java)
                .assertErrorMessage("Exception was thrown by reducer, state = 'Initial', action = 'Action1'")
    }
}
