package com.freeletics.rxredux

import android.view.ViewGroup
import com.freeletics.rxredux.di.ApplicationModule
import com.freeletics.rxredux.di.DaggerApplicationComponent

const val MOCK_WEB_SERVER_PORT = 56543

class SampleTestApplication : SampleApplication() {

    override fun componentBuilder(builder: DaggerApplicationComponent.Builder) =
        builder.applicationModule(
            ApplicationModule(
                baseUrl = "http://127.0.0.1:$MOCK_WEB_SERVER_PORT",
                viewBindingInstantiatorMap = mapOf<Class<*>, ViewBindingInstantiator>(
                    MainActivity::class.java to { rootView: ViewGroup -> RecordingMainViewBinding(rootView) }
                )
            )
        )

}
