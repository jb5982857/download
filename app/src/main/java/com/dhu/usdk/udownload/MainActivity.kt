package com.dhu.usdk.udownload

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.dh.usdk.support.ugson.Gson
import com.dh.usdk.support.uokhttp.*
import com.dhu.usdk.support.udownload.Item
import com.dhu.usdk.support.udownload.UTask
import com.dhu.usdk.support.udownload.utils.ULog
import java.io.BufferedInputStream
import java.io.IOException

class MainActivity : AppCompatActivity() {
    companion object {
        //        const val URL = "https://f52755f1886e823dc1e1fbba521d504d.dlied1.cdntips.net/sqdd.myapp.com/myapp/qqteam/qq_hd/apad/qqhd_hd_5.8.8.3445_release.apk?mkey=615d132ab69630c8&f=0000&cip=182.150.22.61&proto=https"
        const val URL = "https://inner-cdn.dhgames.cn:12345/ih/9f5d08bd16083e796a6c7ff933613442"
        const val TEST_URL =
            "https://inner-cdn.dhgames.cn:12345/ih/76ff3a21d4ddb5a557fd2872785c9bae"
    }

    private lateinit var dirPath: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dirPath =
            applicationContext.getExternalFilesDir("")?.getAbsolutePath() + "/udownload/" ?: ""
        val request = Request.Builder()
            .url(URL)
            .build()
        OkHttpClient.Builder().build().newCall(request).enqueue(object : Callback {
            override fun onFailure(p0: Call, p1: IOException) {
                ULog.e("test http error ", p1)
            }

            override fun onResponse(p0: Call, response: Response) {
                val data =
                    Gson().fromJson(response.body()?.string(), RootData::class.java)
                UTask(true, "test").apply {
                    data.manifiest.forEach {
                        add(
                            Item(
                                "https://inner-cdn.dhgames.cn:12345/ih/${it.md5}",
                                dirPath + it.path, it.md5, it.size
                            )
                        )
                    }
                }.start(this@MainActivity)
            }
        })
//        testOkhttp()
    }

    private fun testOkhttp() {
        Thread {
            val request = Request.Builder()
                .addHeader("Range", "bytes=0-")
                .url(TEST_URL)
                .build()
            val ins = OkHttpClient.Builder().build().newCall(request).execute().body()?.byteStream()
            ULog.d("test http log , $ins")
        }.start()
    }
}

data class Data(val path: String, val md5: String, val size: Long)

data class RootData(val manifiest: List<Data>)