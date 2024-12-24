package com.simulide.plugins.domain

import com.simulide.domain.UuidSerializerModule
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import java.util.*

val uuidSerializerModule = SerializersModule {
    contextual(UUID::class, UuidSerializerModule)
}

val jsonConfiguration = Json {
    serializersModule = uuidSerializerModule
    prettyPrint = true
    isLenient = true
    ignoreUnknownKeys = true
}
