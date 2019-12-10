package com.vardemin.varddb

import androidx.annotation.MainThread
import androidx.collection.ArraySet
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer

class MutableLiveEvent<T : EventArgs<Any>> : MutableLiveData<T>() {

    internal val observers = ArraySet<PendingObserver<in T>>()

    @MainThread
    override fun observe(owner: LifecycleOwner, observer: Observer<in T>) {
        val wrapper = PendingObserver(observer)
        observers.add(wrapper)

        super.observe(owner, wrapper)
    }

    override fun observeForever(observer: Observer<in T>) {
        val wrapper = PendingObserver(observer)
        observers.add(wrapper)

        super.observeForever(observer)
    }

    @MainThread
    override fun removeObserver(observer: Observer<in T>) {

        when (observer) {
            is PendingObserver -> {
                observers.remove(observer)
                super.removeObserver(observer)
            }
            else -> {
                val pendingObserver = observers.firstOrNull { it.wrappedObserver == observer }
                if (pendingObserver != null) {
                    observers.remove(pendingObserver)
                    super.removeObserver(pendingObserver)
                }
            }
        }
    }

    @MainThread
    override fun setValue(event: T?) {
        observers.forEach { it.awaitValue() }
        super.setValue(event)
    }
}

internal class PendingObserver<T : EventArgs<Any>>(val wrappedObserver: Observer<in T>) : Observer<T> {

    private var pending = false

    override fun onChanged(event: T?) {
        if (pending && event?.handled != true) {
            pending = false
            wrappedObserver.onChanged(event)
        }
    }

    fun awaitValue() {
        pending = true
    }
}

open class EventArgs<out T>(private val content: T?) {

    var handled: Boolean = false

    val data: T?
        get() {
            return if (handled) {
                null
            } else {
                content
            }
        }
}