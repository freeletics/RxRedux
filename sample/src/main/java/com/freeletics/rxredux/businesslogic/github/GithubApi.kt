package com.freeletics.rxredux.businesslogic.github

import io.reactivex.Single
import retrofit2.http.Query

interface GithubApi {

    fun search(
        @Query("q") query: String,
        @Query("sort") sort: String,
        @Query("page") page: Int
    ): Single<GithubSearchResults>
}
