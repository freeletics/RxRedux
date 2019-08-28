package com.freeletics.rxredux.logs

import com.freeletics.rxredux.ReduxLogger
import timber.log.Timber

class SampleLogger : ReduxLogger<Any, Any> {
    override fun onStateInitialize(initialState: Any) {
        Timber.d("StateInitialize $initialState")
    }

    override fun onActionReceived(action: Any, state: Any) {
        Timber.d("ActionReceived action = $action, state = $state")
    }

    override fun onStateUpdate(oldState: Any, newState: Any, action: Any) {
        Timber.d("StateUpdate action = $action, oldState = $oldState, newState = $newState")
    }

    override fun onSideEffectError(currentState: Any, error: Throwable) {
        Timber.e(error, "SideEffectError currentState = $currentState")
    }

    override fun onReduceError(action: Any, state: Any, error: Throwable) {
        Timber.e(error, "ReduceError state = $state, action = $action")
    }
}
