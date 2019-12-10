package com.vardemin.varddb

import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

suspend fun <T> async(context: CoroutineContext = Dispatchers.IO, body: ()-> T) =
    withContext(context) {
        body.invoke()
    }