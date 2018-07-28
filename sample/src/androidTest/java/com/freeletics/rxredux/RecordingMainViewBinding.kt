package com.freeletics.rxredux

import android.app.Activity
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.os.Environment
import android.view.ViewGroup
import android.view.ViewTreeObserver
import com.facebook.testing.screenshot.Screenshot
import com.freeletics.rxredux.businesslogic.pagination.PaginationStateMachine
import io.reactivex.Observable
import io.reactivex.subjects.ReplaySubject
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.util.*


class RecordingMainViewBinding(rootView: ViewGroup) : MainViewBinding(rootView) {
    companion object {
        lateinit var INSTANCE: RecordingMainViewBinding
    }

    val drawListener = ScreenshotTakingDrawListener()
    init {
        INSTANCE = this // I'm just to lazy to setup dagger properly :(
        rootView.viewTreeObserver.addOnDrawListener(drawListener)
    }

    private val subject = ReplaySubject.create<PaginationStateMachine.State>()
    val recordedStates: Observable<PaginationStateMachine.State> = subject

    override fun render(state: PaginationStateMachine.State) {
        super.render(state)
        drawListener.queue.offer(state)
    }


    inner class ScreenshotTakingDrawListener : ViewTreeObserver.OnDrawListener {
        val queue: Queue<PaginationStateMachine.State> = LinkedList()
        override fun onDraw() {
            if (queue.isNotEmpty()) {
                Screenshot.snap(rootView).setName("MainView State ${subject.values.size + 1}")
                    .record()

                val state = queue.poll()
                //takeScreenshot()
                //rootView.viewTreeObserver.removeOnDrawListener(this)
                //takeScreenshot()
                subject.onNext(state)
            }
        }
    }
}
