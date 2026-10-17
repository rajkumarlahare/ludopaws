package com.rekixo.ludopaws

import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * One process-wide HTTP transport for gameplay REST calls and match WebSockets.
 * Sharing the client also shares OkHttp's connection pool and dispatcher, avoiding
 * per-screen client churn and allowing HTTP keep-alive/HTTP2 connections to be reused.
 */
internal object LudoPawsHttpClients {
    val shared: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectionPool(ConnectionPool(8, 5, TimeUnit.MINUTES))
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .followRedirects(false)
            .followSslRedirects(false)
            .pingInterval(20, TimeUnit.SECONDS)
            .build()
    }
}
