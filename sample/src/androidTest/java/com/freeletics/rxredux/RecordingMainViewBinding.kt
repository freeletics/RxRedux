package com.freeletics.rxredux

import android.view.ViewGroup
import android.view.ViewTreeObserver
import com.freeletics.rxredux.businesslogic.pagination.PaginationStateMachine
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.ReplaySubject
import timber.log.Timber


class RecordingMainViewBinding(rootView: ViewGroup) : MainViewBinding(rootView) {
    companion object {
        lateinit var INSTANCE: RecordingMainViewBinding
    }

    ///val drawListener = ScreenshotTakingDrawListener()


    private val subject = ReplaySubject.create<PaginationStateMachine.State>()
    val recordedStates: Observable<PaginationStateMachine.State> =
        subject.observeOn(Schedulers.io())
    private val screenshotTaker = QueueingScreenshotTaker(
        rootView = rootView,
        subject = subject,
        dispatchRendering = { super.render(it) }
    )

    init {
        INSTANCE = this // I'm just to lazy to setup dagger properly :(
    }

    override fun render(state: PaginationStateMachine.State) {
        screenshotTaker.enqueue(state)
    }

    /*
    override fun render(state: PaginationStateMachine.State) {
        super.render(state)
        drawListener.queue.offer(state)
        Timber.d("View should render $state")
    }

    inner class ScreenshotTakingDrawListener : ViewTreeObserver.OnPreDrawListener {
        val queue: Queue<PaginationStateMachine.State> = LinkedList()
        override fun onPreDraw(): Boolean {
            if (queue.isNotEmpty()) {
                Screenshot.snap(rootView).setName("MainView State ${subject.values.size + 1}")
                    .record()
                val state = queue.poll()
                Timber.d("View is drawing --> dispatching $state : Queue $queue")
                subject.onNext(state)
            }
            return true
        }
    }
    */
}
