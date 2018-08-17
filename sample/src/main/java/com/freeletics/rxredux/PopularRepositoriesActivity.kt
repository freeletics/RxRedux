package com.freeletics.rxredux

import android.app.Activity
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.freeletics.rxredux.businesslogic.pagination.Action
import com.freeletics.rxredux.businesslogic.pagination.PaginationStateMachine
import com.freeletics.rxredux.di.AndroidScheduler
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_main.*
import javax.inject.Inject

class PopularRepositoriesActivity : AppCompatActivity() {

    class MainViewModelFactory @Inject constructor(
        private val paginationStateMachine: PaginationStateMachine,
        @AndroidScheduler private val scheduler: Scheduler
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T =
            PopularRepositoriesViewModel(paginationStateMachine, scheduler) as T
    }

    @Inject
    lateinit var viewModelFactory: MainViewModelFactory

    @Inject
    lateinit var viewBindingFactory: ViewBindingFactory

    private val viewBinding by lazy {
        viewBindingFactory.create<PopularRepositoriesViewBinding>(
            PopularRepositoriesActivity::class.java,
            rootView
        )
    }

    private val disposables = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        applicationComponent.inject(this)

        val viewModel = ViewModelProviders.of(this, viewModelFactory)[PopularRepositoriesViewModel::class.java]
        viewModel.state.observe(this, Observer {
            viewBinding.render(it!!)
        })

        disposables.add(
            viewBinding.endOfRecyclerViewReached
                .map { Action.LoadNextPageAction }
                .subscribe(viewModel.input)
        )

        viewModel.input.accept(Action.LoadFirstPageAction)

        disposables.add(
            viewBinding.retryLoadFirstPage
                .map { Action.LoadFirstPageAction }
                .subscribe(viewModel.input)
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        disposables.dispose()
    }

    private val Activity.applicationComponent
        get() = (application as SampleApplication).applicationComponent
}

