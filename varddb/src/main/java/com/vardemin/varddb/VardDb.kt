package com.vardemin.varddb

import android.content.Context
import com.tencent.mmkv.MMKV
import com.tencent.mmkv.MMKVLogLevel
import kotlinx.coroutines.Dispatchers
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext

object VardDb {
    const val KEY_DEFAULT = "VARD_DEFAULT_STORE_KEY"

    private val stores by lazy { ConcurrentHashMap<String, VardStore>() }
    var initString: String? = null
    lateinit var mmkv: MMKV

    @JvmStatic
    fun init(context: Context, logLevel: MMKVLogLevel = MMKVLogLevel.LevelNone) {
       initString = MMKV.initialize(context, logLevel)
    }

    @JvmStatic
    fun init(rootDir: String, logLevel: MMKVLogLevel = MMKVLogLevel.LevelNone) {
        initString = MMKV.initialize(rootDir, logLevel)
    }

    fun store(name: String = KEY_DEFAULT,
              multiProcess: Boolean = true,
              isTTL: Boolean = false,
              coroutineContext: CoroutineContext = Dispatchers.IO,
              prefillLiveDataAsync: Boolean = false): VardStore {
        if (initString == null) throw VardDbNotInitializedException("Please call init first")
        var targetStore = stores[name]
        if (targetStore == null) {
            targetStore = VardStore(
                VardStoreConfig(name, multiProcess, isTTL, coroutineContext, prefillLiveDataAsync)
            )
            stores[name] = targetStore
        }
        return targetStore
    }

    fun store(config: VardStoreConfig): VardStore {
        if (initString == null) throw VardDbNotInitializedException("Please call init first")
        var targetStore = stores[config.name]
        if (targetStore == null) {
            targetStore = VardStore(config)
            stores[config.name] = targetStore
        }
        return targetStore
    }

}