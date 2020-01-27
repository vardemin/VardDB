# Vard DB
Modern key-value database for Android in Kotlin based on [MMKV database](https://github.com/Tencent/MMKV) and [Fast serilization](https://github.com/RuedigerMoeller/fast-serialization). With support of TTL(Time-to-live) objects, Android LiveData and kotlin coroutines
P.S. Vard[վարդ (in armenian)] - rose as entry point for Database with petals as separate Store 

[![](https://jitpack.io/v/vardemin/VardDB.svg)](https://jitpack.io/#vardemin/VardDB)

#Quick start
## 1. Include library

**Using Gradle**

Vard DB is currently available in on Jitpack so add the following line before every other thing if you have not done that already.

```gradle
allprojects {
  repositories {
    ...
    maven { url 'https://jitpack.io' }
  }
}
```
	
Then add the following line (Please view JitPack/GitHub Release for actual version)

``` gradle
dependencies {
  implementation 'com.github.vardemin:VardDB:0.1.3'
}
```

## 2. Supported types
Supported types: Int, String, Boolean, Double, Float, ByteArray, Long, Set<String>, Serializable, List<Parcelable>, Map<String, Parcelable?>

## 3. Usage
###1. Initialize
```kotlin
class App: Application() {
    override fun onCreate() {
        super.onCreate()
        VardDb.init(this, MMKVLogLevel.LevelDebug)
    }
}
```
###2. Create or access created store instance
```kotlin
db = VardDb.store(name = KEY_DEFAULT,
                          multiProcess = true,
                          isTTL = false,
                          coroutineContext = Dispatchers.IO,
                          prefillLiveDataAsync = false)
```
Or using VardStoreConfig
```kotlin
data class VardStoreConfig(
    val name: String = VardDb.KEY_DEFAULT,
    val multiProcess: Boolean = true,
    val isTTL: Boolean = false,
    val coroutineContext: CoroutineContext = Dispatchers.IO,
    val prefillLiveDataAsync: Boolean = false
)

db = VardDb.store(config)
```
###3. Write to store

```kotlin
db.save(key, value, ttl = -1L)
db.saveAsync(key, value, ttl = -1L, context = config.coroutineContext)
```
TTL = -1L means no Time-To-Live (default -1). Value in milliseconds

###4. Read from store

```kotlin
//READ METHOD SYNTAX: db.read + [Int,String,Bool,Long,Float,Double,StringSet,Object,Parcelable,ParcelableList,ParcelableMap] + [Async, _] + (key, [defaultValue], isTTL = config.isTTL,  context: CoroutineContext = config.coroutineContext)
db.readBool("someFlag", isTTL = false) //Nullable
db.readBool("someFlag", default = true, isTTL = false) //Non-null
```
##4. LiveData support

```kotlin
//Empty non-prefilled livedata
fun <T> getLiveData(key: String): LiveData<T>
//For simple types
fun <T> getFilledLivedData(key: String, entityType: EntityType, isTTL: Boolean = config.isTTL, asyncFill: Boolean = config.prefillLiveDataAsync): LiveData<T>
enum class EntityType {
    NONE,
    SERIALIZABLE,
    BOOLEAN,
    STRING,
    INT,
    LONG,
    FLOAT,
    DOUBLE,
    BYTE_ARRAY
}
//For Parcelable
fun <T: Parcelable> getFilledLiveData(key: String, clazz: Class<T>, isTTL: Boolean = config.isTTL, asyncFill: Boolean = config.prefillLiveDataAsync): LiveData<T>
fun <T: Parcelable> getFilledLiveData(key: String, creator: Parcelable.Creator<T>, isTTL: Boolean = config.isTTL, asyncFill: Boolean = config.prefillLiveDataAsync): LiveData<List<T>>
fun <T: Parcelable> getFilledLiveData(key: String, isTTL: Boolean = config.isTTL, asyncFill: Boolean = config.prefillLiveDataAsync): LiveData<Map<String, T?>>
```
In case of asyncFill = false MutableLiveData(value) will be created

##5. Fast serialization
By default Fast Serialization library is used to deserialize target object if type of object not found. Exception will be thrown in case of object non-serializable.

## License

VarddDB is distributed under the MIT license. [See LICENSE](https://github.com/vardemin/VardDB/blob/master/LICENSE.md) for details.
