package com.freeletics.rxredux.di

import com.freeletics.rxredux.MainActivity
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules =[ApplicationModule::class] )
interface ApplicationComponent {

    fun inject(into: MainActivity)
}
