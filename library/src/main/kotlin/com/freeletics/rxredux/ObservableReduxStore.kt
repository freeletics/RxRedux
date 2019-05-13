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
 * @param initialStateSupplier  A function that computes the initial state. The computation is
 * done lazily once an observer subscribes and it is done on the [io.reactivex.Scheduler] that
 * you have specified in subscribeOn(). The computed initial state will be emitted directly
 * in onSubscribe()
 * @param sideEffects The sideEffects. See [SideEffect]
 * @param reducer The reducer.  See [Reducer].
 * @param S The type of the State
 * @param A The type of the Actions
 */
fun <S : Any, A : Any> Observable<A>.reduxStore(
    initialStateSupplier: () -> S,
    sideEffects: Iterable<SideEffect<S, A>>,
    reducer: Reducer<S, A>
): Observable<S> {
    return RxJavaPlugins.onAssembly(
        ObservableReduxStore(
            initialStateSupplier = initialStateSupplier,
            upstreamActionsObservable = this,
            reducer = reducer,
            sideEffects = sideEffects
        )
    )
}

/**
 * Just a convenience method to use a fixed value as initial state (rather than a supplier function).
 * However, under the hood it creates a fixed supplier function that captures this fixed value.
 *
 * @see reduxStore
 * @param initialState The initial state. The initial state is emitted directly in on onSubscribe().
 * @param sideEffects The SideEffects. See [SideEffect].
 * @param reducer The reducer. See [Reducer].
 */
fun <S : Any, A : Any> Observable<A>.reduxStore(
    initialState: S,
    sideEffects: Iterable<SideEffect<S, A>>,
    reducer: Reducer<S, A>
): Observable<S> = reduxStore(
    initialStateSupplier = { initialState },
    sideEffects = sideEffects,
    reducer = reducer
)

/**
 * Just a convenience method to use vararg for arbitrary many sideeffects instead a list of SideEffects.
 * See [reduxStore] documentation.
 *
 * @see reduxStore
 */
fun <S : Any, A : Any> Observable<A>.reduxStore(
    initialState: S,
    vararg sideEffects: SideEffect<S, A>,
    reducer: Reducer<S, A>
): Observable<S> = reduxStore(
    initialState = initialState,
    sideEffects = sideEffects.toList(),
    reducer = reducer
)

/**
 * Just a convenience method to use vararg for arbitrary many sideeffects instead a list of SideEffects.
 * See [reduxStore] documentation.
 *
 * @see reduxStore
 */
fun <S : Any, A : Any> Observable<A>.reduxStore(
    initialStateSupplier: () -> S,
    vararg sideEffects: SideEffect<S, A>,
    reducer: Reducer<S, A>
): Observable<S> = reduxStore(
    initialStateSupplier = initialStateSupplier,
    sideEffects = sideEffects.toList(),
    reducer = reducer
)

/**
 * Use [Observable.reduxStore] to create an instance of this kind of Observable.
 *
 * @param S The type of the State
 * @param A The type of the Actions
 * @see [Observable.reduxStore]
 */
private class ObservableReduxStore<S : Any, A : Any>(
    /**
     * The initial state. This one will be emitted directly in onSubscribe().
     * The supplier is runs on the Scheduler that has been specified in .subscribeOn(MyScheduler).
     */
    private val initialStateSupplier: () -> S,
    /**
     * The upstream that emits Actions (i.e. actions triggered by an User through User Interface)
     */
    private val upstreamActionsObservable: Observable<A>,

    /**
     * The Iterable of all sideEffects. A [SideEffect] takes an action, does something meaningful and returns another action.
     * Every Action is handled by the [Reducer] to create a new State.
     */
    private val sideEffects: Iterable<SideEffect<S, A>>,

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
            initialState = initialStateSupplier(),
            reducer = reducer
        )

        // Stream to cancel the subscriptions
        val actionsSubject = PublishSubject.create<A>()


        actionsSubject.subscribe(storeObserver) // This will make the reducer run on each action

        // TODO should SideEffects be composed with ObservableTransformer?
        // That would be the more idiomatic way I guess.
        sideEffects.forEach { sideEffect ->
            disposables += sideEffect(actionsSubject, storeObserver::currentState)
                .subscribe({ action ->
                    // Loop the "output" actions of a SideEffect back into the actions stream
                    actionsSubject.onNext(action)

                    // TODO how to get this run on the origin ReduxStore subscribeOn() Scheduler?
                    // I don't think that this is possible to implement. May need some scheduler
                    // passed in as parameter similar to what Observable.timer() does.

                }, { error ->
                    actionsSubject.onError(error) // Error in SideEffect causes whole stream to fail
                }, {
                    // Swallow onComplete because just if one SideEffect reaches onComplete we don't want to make
                    // everything incl. ReduxStore and other SideEffects reach onComplete
                }
                )
        }

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
    private class ReduxStoreObserver<S : Any, A : Any>(
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
        override fun onNextActually(t: A) {
            val currentState = currentState()
            val newState = try {
                reducer(currentState, t)
            } catch (error: Throwable) {
                onError(ReducerException(state = currentState, action = t, cause = error))
                return
            }
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
