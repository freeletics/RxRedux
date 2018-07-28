package com.freeletics.rxredux

import android.app.Application
import android.content.Context
import android.support.test.runner.AndroidJUnitRunner

class SampleAppRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader?, className: String?, context: Context?) : Application {
        val application = super.newApplication(cl, SampleTestApplication::class.java.canonicalName, context)
        return application
    }
}
