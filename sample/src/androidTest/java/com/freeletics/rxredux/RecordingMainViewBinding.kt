package com.freeletics.rxredux

import android.view.ViewGroup
import com.freeletics.rxredux.businesslogic.pagination.PaginationStateMachine
import io.reactivex.Observable
import io.reactivex.subjects.ReplaySubject

class RecordingMainViewBinding(rootView: ViewGroup) : MainViewBinding(rootView) {
    companion object {
        lateinit var INSTANCE : RecordingMainViewBinding
    }

    init {
        INSTANCE = this // I'm just to lazy to setup dagger properly :(
    }

    private val subject = ReplaySubject.create<PaginationStateMachine.State>()
    val recordedStates: Observable<PaginationStateMachine.State> = subject

    override fun render(state: PaginationStateMachine.State) {
        super.render(state)
        subject.onNext(state)
    }
}
