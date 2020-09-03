package com.freeletics.rxredux


import java.util.concurrent.atomic.AtomicReference

import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.exceptions.*
import io.reactivex.rxjava3.internal.disposables.DisposableHelper
import io.reactivex.rxjava3.observers.LambdaConsumerIntrospection
import io.reactivex.rxjava3.plugins.RxJavaPlugins

internal abstract class SimpleObserver<T> : AtomicReference<Disposable>(), Observer<T>, Disposable, LambdaConsumerIntrospection {

    override fun onSubscribe(s: Disposable) {
        if (DisposableHelper.setOnce(this, s)) {
            try {
                onSubscribeActually(this)
            } catch (ex: Throwable) {
                Exceptions.throwIfFatal(ex)
                s.dispose()
                onError(ex)
            }

        }
    }

    /**
     * Already takes care of error handling in onSubscribe
     */
    protected abstract fun onSubscribeActually(d: Disposable)

    override fun onNext(t: T) {
        if (!isDisposed) {
            try {
                onNextActually(t)
            } catch (e: Throwable) {
                Exceptions.throwIfFatal(e)
                get().dispose()
                onError(e)
            }

        }
    }

    /**
     * Already takes care of error handling. All you have to do is really just to deal with the concrete implementation
     */
    protected abstract fun onNextActually(t: T)

    override fun onError(t: Throwable) {
        if (!isDisposed) {
            lazySet(DisposableHelper.DISPOSED)
            try {
                onErrorActually(t)
            } catch (e: Throwable) {
                Exceptions.throwIfFatal(e)
                RxJavaPlugins.onError(CompositeException(t, e))
            }

        }
    }

    /**
     * Alrady takes care of error handling. All a subclass has to do is really just react on onError events
     */
    protected abstract fun onErrorActually(t: Throwable)

    override fun onComplete() {
        if (!isDisposed) {
            lazySet(DisposableHelper.DISPOSED)
            try {
                onCompleteActually()
            } catch (e: Throwable) {
                Exceptions.throwIfFatal(e)
                RxJavaPlugins.onError(e)
            }

        }
    }

    /**
     * Already take care of error handling. All subclass have to do is to react on complete event per se.
     * Disposing is automatically done
     */
    protected abstract fun onCompleteActually()

    override fun dispose() {
        DisposableHelper.dispose(this)
        disposeActually()
    }

    /**
     * Actually dispose internal subclass logic
     */
    protected abstract fun disposeActually()

    override fun isDisposed(): Boolean {
        return get() == DisposableHelper.DISPOSED && isDisposedActually()
    }

    /**
     * Return true if the subclass has done disposable job and therefore the whole object can be seen as disposed
     */
    protected abstract fun isDisposedActually(): Boolean

    override fun hasCustomOnError(): Boolean = true

    companion object {
        private const val serialVersionUID = -7251123623727123452L
    }
}
