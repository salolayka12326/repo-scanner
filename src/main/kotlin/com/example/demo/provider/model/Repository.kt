package com.example.demo.provider.model

/** Provider-agnostic representation of a source-controlled repository. */
data class Repository(
    val name: String,
    val owner: String
)
