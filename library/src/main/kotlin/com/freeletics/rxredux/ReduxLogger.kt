package com.freeletics.rxredux

interface ReduxLogger<S : Any, A : Any> {

    fun onStateInitialize(initialState: S)

    fun onActionReceived(action: A, state: S)

    fun onStateUpdate(oldState: S, newState: S, action: A)

    fun onSideEffectError(currentState: S, error: Throwable)

    fun onReduceError(action: A, state: S, error: Throwable)

    companion object {
        var default: ReduxLogger<Any, Any>? = null
    }
}
