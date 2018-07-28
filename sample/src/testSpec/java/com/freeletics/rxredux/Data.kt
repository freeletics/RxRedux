package com.freeletics.rxredux

import com.freeletics.rxredux.businesslogic.github.GithubRepository

val FIRST_PAGE: List<GithubRepository> by lazy {
    val r = 1..10L

    r.map { i ->
        GithubRepository(
            id = i,
            name = "Repo$i",
            stars = 100
        )
    }
}

val SECOND_PAGE: List<GithubRepository> by lazy {
    val r = 11..20L

    r.map { i ->
        GithubRepository(
            id = i,
            name = "Repo$i",
            stars = 100
        )
    }
}
