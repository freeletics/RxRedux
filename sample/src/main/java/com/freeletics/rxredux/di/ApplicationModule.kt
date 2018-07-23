package com.freeletics.rxredux.di

import com.freeletics.rxredux.businesslogic.github.GithubApi
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Singleton

@Module
object ApplicationModule {

    @Provides
    @Singleton
    fun provideGithubApi() : GithubApi {

        val retrofit =
            Retrofit.Builder().addConverterFactory(MoshiConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build()

        return retrofit.create(GithubApi::class.java)

    }
}
