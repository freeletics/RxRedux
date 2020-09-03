package com.freeletics.rxredux

import android.view.ViewGroup
import com.freeletics.rxredux.businesslogic.pagination.PaginationStateMachine
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.ReplaySubject
import timber.log.Timber


class RecordingPopularRepositoriesViewBinding(rootView: ViewGroup) : PopularRepositoriesViewBinding(rootView) {
    companion object {
        lateinit var INSTANCE: RecordingPopularRepositoriesViewBinding
    }

    private val subject = ReplaySubject.create<PaginationStateMachine.State>()
    val recordedStates: Observable<PaginationStateMachine.State> =
        subject.observeOn(Schedulers.io())
    private val screenshotTaker = QueueingScreenshotTaker(
        rootView = rootView,
        subject = subject,
        dispatchRendering = { super.render(it) }
    )

    fun lastPositionInAdapter() = adapter.itemCount

    init {
        INSTANCE = this // I'm just to lazy to setup dagger properly :(
    }

    override fun render(state: PaginationStateMachine.State) {
        Timber.d("Screen State to render: $state")
        screenshotTaker.enqueue(state)
    }
}
