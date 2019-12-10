# Vard DB
Modern key-value database for Android in Kotlin based on [MMKV database](https://github.com/Tencent/MMKV) and [Fast serilization](https://github.com/RuedigerMoeller/fast-serialization). With support of TTL(Time-to-live) objects, Android LiveData and kotlin coroutines
P.S. Vard[վարդ (in armenian)] - rose as entry point for Database with petals as separate Store 

[![](https://jitpack.io/v/vardemin/VardDB.svg)](https://jitpack.io/#vardemin/VardDB)

## Quick Setup
### 1. Include library

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
	
Then add the following line 

``` gradle
dependencies {
  implementation 'com.github.vardemin.varddb:varddb:0.1.0'
}
```

### 2. Usage
To be filled...

## License

VarddDB is distributed under the MIT license. [See LICENSE](https://github.com/vardemin/VardDB/blob/master/LICENSE.md) for details.
