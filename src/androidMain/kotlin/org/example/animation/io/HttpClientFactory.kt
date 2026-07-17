package org.example.animation.io

import io.ktor.client.engine.okhttp.OkHttp

actual fun createHttpClientEngine(): io.ktor.client.engine.HttpClientEngine = OkHttp.create()
