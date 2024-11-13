package com.simulide.domain

import kotlinx.serialization.Serializable

@Serializable
data class ExecutableCode(val code: String, val test: String)
