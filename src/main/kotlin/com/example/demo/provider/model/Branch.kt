package com.example.demo.provider.model

/** Provider-agnostic representation of a single branch of a [Repository]. */
data class Branch(
    val name: String,
    val lastCommitSha: String
)
