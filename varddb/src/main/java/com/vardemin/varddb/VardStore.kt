package com.vardemin.varddb

import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.nustaq.serialization.FSTConfiguration
import java.lang.Exception
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext

data class VardStore(val config: VardStoreConfig) {

    private val fst = FSTConfiguration.createDefaultConfiguration()
    private val mmkv: MMKV = MMKV.mmkvWithID(
        config.name,
        if (config.multiProcess) MMKV.MULTI_PROCESS_MODE else MMKV.SINGLE_PROCESS_MODE
    )!!

    private val liveDataMap by lazy { ConcurrentHashMap<String, Any>() }

    fun <T> getLiveData(key: String): LiveData<T> {
        var liveData = liveDataMap[key]
        if (liveData == null) {
            liveData = MutableLiveData<T>()
            this.liveDataMap[key] = liveData
        }
        return liveData as LiveData<T>
    }

    fun <T> getFilledLivedData(key: String, entityType: EntityType, isTTL: Boolean = config.isTTL, asyncFill: Boolean = config.prefillLiveDataAsync): LiveData<T> {
        var liveData: MutableLiveData<T>? = liveDataMap[key] as MutableLiveData<T>?
        if (liveData == null) {
            liveData = if (!asyncFill) MutableLiveData(getDataByType(key, entityType, isTTL)) else MutableLiveData()
            liveDataMap[key] = liveData
        }
        if (liveData.value == null && asyncFill) {
            GlobalScope.launch(config.coroutineContext) {
                liveData.value = getDataByType(key, entityType, isTTL)
            }
        }
        return liveData
    }

    fun <T: Parcelable> getFilledLiveData(key: String, clazz: Class<T>, isTTL: Boolean = config.isTTL, asyncFill: Boolean = config.prefillLiveDataAsync): LiveData<T> {
        var liveData: MutableLiveData<T>? = liveDataMap[key] as MutableLiveData<T>?
        if (liveData == null) {
            liveData = if (!asyncFill) MutableLiveData<T>(readParcelable(key, clazz, isTTL)) else MutableLiveData()
            liveDataMap[key] = liveData
        }
        if (liveData.value == null && asyncFill) {
            GlobalScope.launch(config.coroutineContext) {
                liveData.value = readParcelable(key, clazz, isTTL)
            }
        }
        return liveData
    }

    fun <T: Parcelable> getFilledLiveData(key: String, creator: Parcelable.Creator<T>, isTTL: Boolean = config.isTTL, asyncFill: Boolean = config.prefillLiveDataAsync): LiveData<List<T>> {
        var liveData: MutableLiveData<List<T>>? = liveDataMap[key] as MutableLiveData<List<T>>?
        if (liveData == null) {
            liveData = if (!asyncFill) MutableLiveData(readParcelableList(key, creator, isTTL)) else MutableLiveData()
            liveDataMap[key] = liveData
        }
        if (liveData.value == null && asyncFill) {
            GlobalScope.launch(config.coroutineContext) {
                liveData.value = readParcelableList(key, creator, isTTL)
            }
        }
        return liveData
    }

    fun <T: Parcelable> getFilledLiveData(key: String, isTTL: Boolean = config.isTTL, asyncFill: Boolean = config.prefillLiveDataAsync): LiveData<Map<String, T?>> {
        var liveData: MutableLiveData<Map<String, T?>>? = liveDataMap[key] as MutableLiveData<Map<String, T?>>?
        if (liveData == null) {
            liveData = if (!asyncFill) MutableLiveData(readParcelableMap(key, isTTL)) else MutableLiveData()
            liveDataMap[key] = liveData
        }
        if (liveData.value == null && asyncFill) {
            GlobalScope.launch(config.coroutineContext) {
                liveData.value = readParcelableMap(key, isTTL)
            }
        }
        return liveData
    }

    public fun <T> getDataByType(key: String, entityType: EntityType = EntityType.NONE, isTTL: Boolean = config.isTTL): T? {
        return when(entityType) {
            EntityType.NONE -> null
            EntityType.BOOLEAN -> readBool(key, isTTL) as T?
            EntityType.BYTE_ARRAY -> readByteArray(key, isTTL) as T?
            EntityType.DOUBLE -> readDouble(key, isTTL) as T?
            EntityType.FLOAT -> readFloat(key, isTTL) as T?
            EntityType.INT -> readInt(key, isTTL) as T?
            EntityType.LONG -> readLong(key, isTTL) as T?
            EntityType.STRING -> readString(key, isTTL) as T?
            EntityType.SERIALIZABLE -> readObject(key, isTTL)
            else -> null
        }
    }

    public suspend fun <T> getDataByTypeAsync(key: String, entityType: EntityType = EntityType.NONE, isTTL: Boolean = config.isTTL): T? =
        withContext(config.coroutineContext) {
            getDataByType<T?>(key, entityType, isTTL)
        }

    private fun <T> getMutableLiveData(key: String): MutableLiveData<T> {
        var liveData = liveDataMap[key]
        if (liveData == null) {
            liveData = MutableLiveData<T>()
            liveDataMap[key] = liveData
        }
        return liveData as MutableLiveData<T>
    }

    public fun save(key: String, value: MutableSet<String>, ttl: Long = -1L) =
        setTTL(key, ttl) {
            mmkv.encode(key, value).also { notifyLiveData(key, value) }
        }
    public suspend fun saveAsync(key: String, value: MutableSet<String>, ttl: Long = -1L, context: CoroutineContext = config.coroutineContext) =
        withContext(context) { save(key, value, ttl)}

    public fun <T: Parcelable> save(key: String, value: T, ttl: Long = -1L) =
        setTTL(key, ttl) {
            mmkv.encode(key, value).also { notifyLiveData(key, value) }
        }
    public suspend fun <T: Parcelable> saveAsync(key: String, value: T, ttl: Long = -1L, context: CoroutineContext = config.coroutineContext) =
        withContext(context) { save(key, value, ttl)}

    public fun <T: Parcelable> save(key: String, list: List<T>, ttl: Long = -1L) =
        setTTL(key, ttl) {
            mmkv.encode(key, MarshalUtil.marshall(list)).also { notifyLiveData(key, list) }
        }
    public suspend fun <T: Parcelable> saveAsync(key: String, list: List<T>, ttl: Long = -1L, context: CoroutineContext = config.coroutineContext) =
        withContext(context) { save(key, list, ttl)}

    public fun <T: Parcelable> save(key: String, map: Map<String, T>, ttl: Long = -1L) =
        setTTL(key, ttl) {
            mmkv.encode(key, MarshalUtil.marshall(map)).also { notifyLiveData(key, map) }
        }
    public suspend fun <T: Parcelable> saveAsync(key: String, map: Map<String, T>, ttl: Long = -1L, context: CoroutineContext = config.coroutineContext) =
        withContext(context) { save(key, map, ttl)}

    public fun <T> save(key: String, value: T, ttl: Long = -1L) = mmkv.encode(key, value, ttl).also { if (it) notifyLiveData(key, value) }
    public suspend fun <T> saveAsync(key: String, value: T, ttl: Long = -1L, context: CoroutineContext = config.coroutineContext) =
        withContext(context) { save(key, value, ttl) }

    public fun <T> readObject(key: String, isTTL: Boolean = config.isTTL): T? {
        val obj =
            readTTL(key, isTTL) { fst.asObject(mmkv.decodeBytes(key)) }
        return if (obj != null)
            return obj as T
        else obj
    }

    public suspend fun <T> readObjectAsync(key: String, isTTL: Boolean = config.isTTL, context: CoroutineContext = config.coroutineContext): T? =
        withContext(context) { readObject<T>(key, isTTL) }

    public fun <T : Any> readObject(key: String, default: T, isTTL: Boolean = config.isTTL): T =
        readObject(key, isTTL) ?: default

    public suspend fun <T : Any> readObjectAsync(key: String, default: T, isTTL: Boolean = config.isTTL, context: CoroutineContext = config.coroutineContext): T =
        withContext(context) { readObject(key, default, isTTL) }

    public fun readBool(key: String, isTTL: Boolean = config.isTTL): Boolean? =
        readTTL(key, isTTL) { mmkv.decodeBool(key) }

    public suspend fun readBoolAsync(key: String, isTTL: Boolean = config.isTTL, context: CoroutineContext = config.coroutineContext): Boolean? =
        withContext(context) { readBool(key, isTTL) }

    public fun readBool(key: String, default: Boolean, isTTL: Boolean = config.isTTL): Boolean =
        readBool(key, isTTL) ?: default

    public suspend fun readBoolAsync(key: String, default: Boolean, isTTL: Boolean = config.isTTL, context: CoroutineContext = config.coroutineContext): Boolean =
        withContext(context) { readBool(key, default, isTTL) }

    public fun readInt(key: String, isTTL: Boolean = config.isTTL): Int? =
        readTTL(key, isTTL) { mmkv.decodeInt(key) }

    public suspend fun readIntAsync(key: String, isTTL: Boolean = config.isTTL, context: CoroutineContext = config.coroutineContext): Int? =
        withContext(context) { readInt(key, isTTL) }

    public fun readInt(key: String, default: Int, isTTL: Boolean = config.isTTL): Int =
        readInt(key, isTTL) ?: default

    public suspend fun readIntAsync(key: String, default: Int, isTTL: Boolean = config.isTTL, context: CoroutineContext = config.coroutineContext): Int =
        withContext(context) { readInt(key, default, isTTL) }

    public fun readLong(key: String, isTTL: Boolean = config.isTTL): Long? =
        readTTL(key, isTTL) { mmkv.decodeLong(key) }

    public suspend fun readLongASync(key: String, isTTL: Boolean = config.isTTL, context: CoroutineContext = config.coroutineContext): Long? =
        withContext(context) { readLong(key, isTTL) }

    public fun readLong(key: String, default: Long, isTTL: Boolean = config.isTTL): Long =
        readLong(key, isTTL) ?: default

    public suspend fun readLongAsync(key: String, default: Long, isTTL: Boolean = config.isTTL, context: CoroutineContext = config.coroutineContext): Long =
        withContext(context) { readLong(key, default, isTTL) }

    public fun readFloat(key: String, isTTL: Boolean = config.isTTL): Float? =
        readTTL(key, isTTL) { mmkv.decodeFloat(key) }

    public suspend fun readFloatAsync(key: String, isTTL: Boolean = config.isTTL, context: CoroutineContext = config.coroutineContext): Float? =
        withContext(context) { readFloat(key, isTTL) }

    public fun readFloat(key: String, default: Float, isTTL: Boolean = config.isTTL): Float =
        readFloat(key, isTTL) ?: default

    public suspend fun readFloatAsync(key: String, default: Float, isTTL: Boolean = config.isTTL, context: CoroutineContext = config.coroutineContext): Float =
        withContext(context) { readFloat(key, default, isTTL) }

    public fun readDouble(key: String, isTTL: Boolean = config.isTTL): Double? =
        readTTL(key, isTTL) { mmkv.decodeDouble(key) }

    public suspend fun readDoubleAsync(key: String, isTTL: Boolean = config.isTTL, context: CoroutineContext = config.coroutineContext): Double? =
        withContext(context) { readDouble(key, isTTL) }

    public fun readDouble(key: String, default: Double, isTTL: Boolean = config.isTTL): Double =
        readDouble(key, isTTL) ?: default

    public suspend fun readDoubleAsync(key: String, default: Double, isTTL: Boolean = config.isTTL, context: CoroutineContext = config.coroutineContext): Double =
        withContext(context) { readDouble(key, default, isTTL) }

    public fun readString(key: String, isTTL: Boolean = config.isTTL): String? =
        readTTL(key, isTTL) { mmkv.decodeString(key) }

    public suspend fun readStringAsync(key: String, isTTL: Boolean = config.isTTL, context: CoroutineContext = config.coroutineContext): String? =
        withContext(context) { readString(key, isTTL) }

    public fun readString(key: String, default: String, isTTL: Boolean = config.isTTL): String =
        readString(key, isTTL) ?: default

    public suspend fun readStringAsync(key: String, default: String, isTTL: Boolean = config.isTTL, context: CoroutineContext = config.coroutineContext): String =
        withContext(context) { readString(key, default, isTTL) }

    public fun readByteArray(key: String, isTTL: Boolean = config.isTTL): ByteArray? =
        readTTL(key, isTTL) { mmkv.decodeBytes(key) }

    public suspend fun readByteArrayAsync(key: String, isTTL: Boolean = config.isTTL, context: CoroutineContext = config.coroutineContext): ByteArray? =
        withContext(context) { readByteArray(key, isTTL) }

    public fun readByteArray(key: String, default: ByteArray, isTTL: Boolean = config.isTTL): ByteArray =
        readByteArray(key, isTTL) ?: default

     public suspend fun readByteArrayAsync(key: String, default: ByteArray, isTTL: Boolean = config.isTTL, context: CoroutineContext = config.coroutineContext): ByteArray =
         withContext(context) { readByteArray(key, default, isTTL) }


    public fun <T: Parcelable> readParcelable(
        key: String,
        objClass: Class<T>,
        isTTL: Boolean = config.isTTL
    ): T? =
        readTTL(key, isTTL) {
            mmkv.decodeParcelable(key, objClass)
        }

    public suspend fun <T: Parcelable> readParcelableAsync(
        key: String,
        objClass: Class<T>,
        isTTL: Boolean = config.isTTL, context: CoroutineContext = config.coroutineContext
    ): T? = withContext(context) { readParcelable(key, objClass, isTTL) }

    public fun <T: Parcelable> readParcelable(
        key: String,
        objClass: Class<T>,
        default: T,
        isTTL: Boolean = config.isTTL
    ): T =
        readParcelable(key, objClass, isTTL) ?: default

    public suspend fun <T: Parcelable> readParcelableAsync(
        key: String,
        objClass: Class<T>,
        default: T,
        isTTL: Boolean = config.isTTL, context: CoroutineContext = config.coroutineContext
    ): T =
        withContext(context) { readParcelable(key, objClass,default, isTTL) }

    public fun <T: Parcelable> readParcelableList(key: String, creator: Parcelable.Creator<T>, isTTL: Boolean = config.isTTL): List<T> {
        val obj =
            readTTL(key, isTTL) { MarshalUtil.unmarshall(mmkv.decodeBytes(key), creator) }
        return obj ?: listOf()
    }

    public suspend fun <T: Parcelable> readParcelableListAsync(key: String, creator: Parcelable.Creator<T>, isTTL: Boolean = config.isTTL, context: CoroutineContext = config.coroutineContext): List<T> =
        withContext(context) { readParcelableList(key, creator, isTTL) }

    public fun <T: Parcelable> readParcelableMap(key: String, isTTL: Boolean = config.isTTL): Map<String, T?> {
        val obj: Map<String, T?>? =
            readTTL(key, isTTL) { val map: Map<String, T?> = MarshalUtil.unmarshall(mmkv.decodeBytes(key)); return@readTTL map; }
        return obj ?: mapOf()
    }

    public suspend fun <T: Parcelable> readParcelableMapAsync(key: String, isTTL: Boolean = config.isTTL, context: CoroutineContext = config.coroutineContext): Map<String, T?> =
        withContext(context) { val result: Map<String, T?> =  readParcelableMap(key, isTTL); return@withContext result; }


    public fun readStringSet(key: String, isTTL: Boolean = config.isTTL): MutableSet<String>? =
        readTTL(key, isTTL) { mmkv.decodeStringSet(key) }

    public suspend fun readStringSetAsync(
        key: String,
        isTTL: Boolean = config.isTTL, context: CoroutineContext = config.coroutineContext
    ): MutableSet<String>? =
        withContext(context) { readStringSet(key, isTTL) }

    public fun readStringSet(
        key: String,
        default: MutableSet<String>,
        isTTL: Boolean = config.isTTL
    ): MutableSet<String> =
        readStringSet(key, isTTL) ?: default

    public suspend fun readStringSetAsync(
        key: String,
        default: MutableSet<String>,
        isTTL: Boolean = config.isTTL, context: CoroutineContext = config.coroutineContext
    ): MutableSet<String> =
        withContext(context) { readStringSet(key, default, isTTL) }

    public fun containsKey(key: String): Boolean = mmkv.containsKey(key)
    public suspend fun containsAsync(key: String, context: CoroutineContext = config.coroutineContext): Boolean =
        withContext(context) { containsKey(key) }

    public fun remove(vararg keys: String) = mmkv.removeValuesForKeys(keys).also { notifyKeysLiveCleared(*keys) }
    public suspend fun removeAsync(vararg keys: String, context: CoroutineContext = config.coroutineContext)
            = withContext(context) { mmkv.removeValuesForKeys(keys) }

    public fun clear() = mmkv.clearAll().also { notifyAllLiveCleared() }
    public suspend fun clearAsync(context: CoroutineContext = config.coroutineContext) =
        withContext(context) { clear() }

    public fun checkKeyAlive(key: String, isTTL: Boolean = config.isTTL): Boolean {
        return mmkv.containsKey(key)
                && (!isTTL ||
                (isTTL && mmkv.containsKey(key + TTL_SUFFIX)
                        && mmkv.decodeLong(key + TTL_SUFFIX, -1L) > System.currentTimeMillis()))
    }

    private fun <T> readTTL(key: String, isTTL: Boolean = config.isTTL, body: () -> T): T? =
        if (checkKeyAlive(key, isTTL)) {
            body.invoke()
        } else null

    private fun <T> setTTL(key: String, ttl: Long, block: () -> T): T {
        if (ttl > 0) mmkv.encode(key + TTL_SUFFIX, System.currentTimeMillis() + ttl)
        return block.invoke()
    }

    private fun <T> MMKV.encode(key: String, value: T, ttl: Long) =
        setTTL(key, ttl) {
            return@setTTL when (value) {
                is Parcelable -> encode(key, value)
                is Boolean -> encode(key, value)
                is ByteArray -> encode(key, value)
                is Double -> encode(key, value)
                is Float -> encode(key, value)
                is Int -> encode(key, value)
                is Long -> encode(key, value)
                is String -> encode(key, value)
                else -> encode(key, fst.asByteArray(value))
            }
        }

    private fun <T> notifyLiveData(key: String, data: T?) {
        try {
            val liveData = getMutableLiveData<T>(key)
            liveData.postValue(data)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    private fun notifyKeysLiveCleared(vararg keys: String) {
        try {
            for (liveData in liveDataMap.filter{ keys.contains(it.key) }.values) {
                (liveData as MutableLiveData<*>).postValue(null)
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    private fun notifyAllLiveCleared() {
        try {
            for (liveData in liveDataMap.values) {
                (liveData as MutableLiveData<*>).postValue(null)
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    companion object {
        const val TTL_SUFFIX = "_TTL"
    }
}