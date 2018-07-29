package com.freeletics.rxredux

import com.freeletics.rxredux.businesslogic.github.GithubRepository
import com.freeletics.rxredux.businesslogic.github.GithubSearchResults
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer


private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
private val githubSearchResultsAdapter = moshi.adapter(GithubSearchResults::class.java)

fun MockWebServer.enqueue200(items: List<GithubRepository>) {
    // TODO why is loading resources not working?
    // val body = MainActivityTest::class.java.getResource("response1.json").readText()

    enqueue(
        MockResponse()
            .setBody(githubSearchResultsAdapter.toJson(GithubSearchResults(items)))
    )
}




const val MOCK_WEB_SERVER_PORT = 56543
