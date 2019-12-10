package com.vardemin.varddbexample

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.vardemin.varddb.VardDb
import com.vardemin.varddb.VardStore
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.lang.Exception

class MainActivity : AppCompatActivity() {

    lateinit var db: VardStore
    lateinit var obj: List<SimpleDataClass>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        db = VardDb.store(prefillLiveDataAsync = true)

        obj = mutableListOf<SimpleDataClass>().apply {
            for (i in 0..1000) {
                add(SimpleDataClass("Vasa$i", i))
            }
        }

        tvObjInput.text = obj.toString()

        btnStrInput.setOnClickListener(this::onClick)
        btnStrOutput.setOnClickListener(this::onClick)
        btnObjInput.setOnClickListener(this::onClick)
        btnObjOutput.setOnClickListener(this::onClick)
    }

    private fun onClick(view: View) {
        val useTtl = checkTTL.isChecked
        val asyncFetch = checkAsyncFetch.isChecked

        val ttl = try {
            if (editTTL.text.toString().isBlank()) 20000L else editTTL.text.toString().toLong()
        } catch (ex: Exception) {
            2000L
        }
        when (view.id) {

            R.id.btnStrInput -> {
                GlobalScope.launch(Dispatchers.Main) {
                    val startTime = System.currentTimeMillis()
                    val saved = if (asyncFetch)
                        db.saveAsync("String", tvStrInput.text, if (useTtl) ttl else -1L)
                    else db.save("String", tvStrInput.text, if (useTtl) ttl else -1L)
                    Toast.makeText(
                        this@MainActivity,
                        "Saved($saved) for ${System.currentTimeMillis() - startTime} ms",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            R.id.btnStrOutput -> {
                GlobalScope.launch(Dispatchers.Main) {
                    val startTime = System.currentTimeMillis()
                    val saved = if (asyncFetch)
                        db.readStringAsync("String", useTtl)
                    else db.readString("String", useTtl)
                    Toast.makeText(
                        this@MainActivity,
                        "Read($saved) for ${System.currentTimeMillis() - startTime} ms",
                        Toast.LENGTH_SHORT
                    ).show()
                    tvStrOutput.text = saved
                }
            }

            R.id.btnObjInput -> {
                GlobalScope.launch(Dispatchers.Main) {
                    val startTime = System.currentTimeMillis()
                    val saved = if (asyncFetch)
                        db.saveAsync("Object", obj, if (useTtl) ttl else -1L)
                    else db.save("Object", obj, if (useTtl) ttl else -1L)
                    Toast.makeText(
                        this@MainActivity,
                        "Saved($saved) for ${System.currentTimeMillis() - startTime} ms",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            R.id.btnObjOutput -> {
                GlobalScope.launch(Dispatchers.Main) {
                    val startTime = System.currentTimeMillis()
                    val saved = try {
                        if (asyncFetch)
                            db.readObjectAsync<List<SimpleDataClass>>("Object", useTtl)
                        else db.readObject<List<SimpleDataClass>>("Object", useTtl)
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                        arrayListOf<SimpleDataClass>()
                    }
                    Toast.makeText(
                        this@MainActivity,
                        "Read(${saved?.size
                            ?: 0} items) for ${System.currentTimeMillis() - startTime} ms",
                        Toast.LENGTH_SHORT
                    ).show()
                    tvObjOutput.text = saved.toString()
                }
            }
        }
    }
}
