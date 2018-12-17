package com.freeletics.rxredux

import io.reactivex.Observable


/**
 * It is a function which takes a stream of actions and returns a stream of actions. Actions in, actions out
 * (concept borrowed from redux-observable.js.or - so called epics).
 * @param actions Input action. Every SideEffect should be responsible to handle a single Action
 * (i.e using filter or ofType operator)
 * @param state [StateAccessor] to get the latest state of the state machine
 */
// TODO find better name?
typealias SideEffect<S, A> = (actions: Observable<A>, state: StateAccessor<S>) -> Observable<out A>


/**
 * The StateAccessor is basically just a deferred way to get a state of a [ObservableReduxStore] at any given point in time.
 * So you have to call this method to get the state.
 */
// TODO find better name
typealias StateAccessor<S> = () -> S
