package com.freeletics.rxredux

import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.observers.SerializedObserver
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject

/**
 * A ReduxStore is a RxJava based implementation of Redux and redux-observable.js.org.
 * A ReduxStore takes Actions from upstream as input events.
 * [SideEffect]s can be registered to listen for a certain
 * Action to react on a that Action as a (impure) side effect and create yet another Action as
 * output. Every Action goes through the a [Reducer], which is basically a pure function that takes
 * the current State and an Action to compute a new State.
 * The new state will be emitted downstream to any listener interested in it.
 *
 * A ReduxStore observable never reaches onComplete(). If a error occurs in the [Reducer] or in any
 * side effect ([Throwable] has been thrown) then the ReduxStore reaches onError() as well and
 * according to the reactive stream specs the store cannot recover the error state.
 *
 * @param initialState The initial state. This one will be emitted directly in onSubscribe()
 * @param sideEffects The sideEffects. See [SideEffect]
 * @param reducer The reducer.  See [Reducer].
 * @param S The type of the State
 * @param A The type of the Actions
 */
fun <S, A> Observable<A>.reduxStore(
    initialState: S,
    sideEffects: List<SideEffect<S, A>>,
    reducer: Reducer<S, A>
): Observable<S> {
    return RxJavaPlugins.onAssembly(
        ObservableReduxStore(
            initialState = initialState,
            upstreamActionsObservable = this,
            reducer = reducer,
            sideEffects = sideEffects
        )
    )
}

/**
 * Use [Observable.reduxStore] to create an instance of this kind of Observable.
 *
 * @param S The type of the State
 * @param A The type of the Actions
 * @see [Observable.reduxStore]
 */
private class ObservableReduxStore<S, A>(
    /**
     * The initial state. This one will be emitted directly in onSubscribe()
     */
    private val initialState: S,
    /**
     * The upstream that emits Actions (i.e. actions triggered by an User through User Interface)
     */
    private val upstreamActionsObservable: Observable<A>,

    /**
     * The list of all sideEffects. A [SideEffect] takes an action, does something meaningful and returns another action.
     * Every Action is handled by the [Reducer] to create a new State.
     */
    private val sideEffects: List<SideEffect<S, A>>,

    /**
     * The Reducer. Takes the current state and an action and computes a new state.
     */
    private val reducer: Reducer<S, A>
) : Observable<S>() {

    override fun subscribeActual(observer: Observer<in S>) {
        val disposables = CompositeDisposable()

        // avoids threading issues
        val serializedObserver = SerializedObserver(observer)
        val storeObserver = ReduxStoreObserver(
            actualObserver = serializedObserver,
            internalDisposables = disposables,
            initialState = initialState,
            reducer = reducer
        )

        // Stream to cancel the subscriptions
        val actionsSubject = PublishSubject.create<A>()

        sideEffects.forEach { sideEffect ->
            disposables += sideEffect(actionsSubject, storeObserver::currentState)
                .subscribe({ action ->
                    // Loop the "output" actions of a SideEffect back into the actions stream
                    actionsSubject.onNext(action)

                    // TODO how to get this run on the origin ReduxStore subscribeOn() Scheduler?
                    // I don't think that this is possible to implement

                }, { error ->
                    actionsSubject.onError(error)
                }, {
                    // Swallow onComplete because just if one SideEffect reaches onComplete we don't want to make
                    // everything incl. ReduxStore an other SideEffects reach onComplete
                }
                )
        }

        actionsSubject.subscribe(storeObserver) // This will make the reducer run on each action

        upstreamActionsObservable.subscribe(
            UpstreamObserver(
                actionsSubject = actionsSubject,
                internalDisposables = disposables
            )
        )
    }

    /**
     * Simple observer for internal reduxStore
     */
    private class ReduxStoreObserver<S, A>(
        private val actualObserver: Observer<in S>,
        private val internalDisposables: CompositeDisposable,
        initialState: S,
        private val reducer: Reducer<S, A>
    ) : SimpleObserver<A>() {

        @Volatile
        private var state: S = initialState

        /**
         * Get the current state
         */
        internal fun currentState(): S = state

        override fun onSubscribeActually(d: Disposable) {
            // start with the initial state
            actualObserver.onSubscribe(d)
            actualObserver.onNext(currentState())

            // TODO do we need to add Disposable d to internal Disposables?
            // I think it is handled by the actualObserver.onSubscribe()
        }

        @Synchronized
        override fun onNextActually(action: A) {
            val currentState = currentState()
            val newState = reducer(currentState, action)
            state = newState
            actualObserver.onNext(newState)
        }

        override fun onErrorActually(t: Throwable) {
            actualObserver.onError(t)
        }

        override fun onCompleteActually() {
            actualObserver.onComplete()
        }

        override fun disposeActually() {
            internalDisposables.dispose()
        }

        override fun isDisposedActually(): Boolean = internalDisposables.isDisposed
    }

    private operator fun CompositeDisposable.plusAssign(disposable: Disposable) {
        this.add(disposable)
    }

    /**
     * Observer for upstream Actions.
     * All Actions are basically forwarded to actionSubject
     */
    private class UpstreamObserver<T>(
        private val actionsSubject: Subject<T>,
        private val internalDisposables: CompositeDisposable
    ) : SimpleObserver<T>() {

        private lateinit var disposable: Disposable

        override fun onSubscribeActually(d: Disposable) {
            disposable = d
            internalDisposables.add(disposable)
        }

        override fun onNextActually(t: T) {
            actionsSubject.onNext(t)
        }

        override fun onErrorActually(t: Throwable) {
            actionsSubject.onError(t)
        }

        override fun onCompleteActually() {
            actionsSubject.onComplete()
        }

        override fun disposeActually() {
            // Nothing to do.
            // InternalDisposables takes care of disposing all internal disposables at once
        }

        override fun isDisposedActually(): Boolean = disposable.isDisposed
    }
}
