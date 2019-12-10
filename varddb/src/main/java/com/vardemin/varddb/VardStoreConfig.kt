package com.vardemin.varddb

import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

data class VardStoreConfig(
    val name: String = VardDb.KEY_DEFAULT,
    val multiProcess: Boolean = true,
    val isTTL: Boolean = false,
    val coroutineContext: CoroutineContext = Dispatchers.IO,
    val prefillLiveDataAsync: Boolean = false
)