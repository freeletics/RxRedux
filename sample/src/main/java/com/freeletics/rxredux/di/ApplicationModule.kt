package com.freeletics.rxredux.di

import com.freeletics.rxredux.ViewBindingFactory
import com.freeletics.rxredux.ViewBindingInstantiatorMap
import com.freeletics.rxredux.businesslogic.github.GithubApi
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Singleton

@Module
class ApplicationModule(
    private val baseUrl: String,
    private val viewBindingInstantiatorMap: ViewBindingInstantiatorMap
) {

    @Provides
    @Singleton
    fun provideGithubApi(): GithubApi {
        val retrofit =
            Retrofit.Builder().addConverterFactory(MoshiConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .baseUrl(baseUrl)
                .build()

        return retrofit.create(GithubApi::class.java)
    }

    @Provides
    @Singleton
    fun provideViewBindingFactory() = ViewBindingFactory(viewBindingInstantiatorMap)
}
