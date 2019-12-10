package com.vardemin.varddbexample

import android.app.Application
import com.tencent.mmkv.MMKVLogLevel
import com.vardemin.varddb.VardDb

class App: Application() {
    override fun onCreate() {
        super.onCreate()
        VardDb.init(this, MMKVLogLevel.LevelDebug)

    }
}