package com.freeletics.di

import com.freeletics.rxredux.ViewBindingInstantiatorMap
import com.freeletics.rxredux.di.ApplicationModule
import com.freeletics.rxredux.localhostCertificate
import io.reactivex.Scheduler
import okhttp3.OkHttpClient
import okhttp3.tls.HandshakeCertificates
import java.util.concurrent.TimeUnit


class TestApplicationModule(
    baseUrl: String,
    viewBindingInstantiatorMap: ViewBindingInstantiatorMap,
    androidScheduler: Scheduler
) : ApplicationModule(
    baseUrl = baseUrl,
    viewBindingInstantiatorMap = viewBindingInstantiatorMap,
    androidScheduler = androidScheduler
) {

    override fun provideOkHttp(): OkHttpClient =
        super.provideOkHttp().newBuilder()
            .writeTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .connectTimeout(5, TimeUnit.SECONDS)
            .also {
                val clientCertificates = HandshakeCertificates.Builder()
                    .addTrustedCertificate(localhostCertificate.certificate())
                    .build()

                it.sslSocketFactory(
                    clientCertificates.sslSocketFactory(),
                    clientCertificates.trustManager()
                )

            }
            .build()


}
