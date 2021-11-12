package com.dhu.usdk.udownload

import android.app.NotificationManager
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import com.dh.usdk.support.ugson.Gson
import com.dh.usdk.support.uokhttp.*
import com.dhu.usdk.support.udownload.Item
import com.dhu.usdk.support.udownload.UTask
import com.dhu.usdk.support.udownload.utils.MD5Util
import com.dhu.usdk.support.udownload.utils.ULog
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.IOException

class MainActivity : AppCompatActivity() {
    companion object {
        //        const val URL = "https://f52755f1886e823dc1e1fbba521d504d.dlied1.cdntips.net/sqdd.myapp.com/myapp/qqteam/qq_hd/apad/qqhd_hd_5.8.8.3445_release.apk?mkey=615d132ab69630c8&f=0000&cip=182.150.22.61&proto=https"
        const val URL = "https://inner-cdn.dhgames.cn:12345/ih/9f5d08bd16083e796a6c7ff933613442"
        const val TEST_URL =
            "https://inner-cdn.dhgames.cn:12345/ih/76ff3a21d4ddb5a557fd2872785c9bae"
        const val TAG = "MainActivity"
    }

    private lateinit var dirPath: String

    private var uTask: UTask? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dirPath =
            applicationContext.getExternalFilesDir("")?.getAbsolutePath() + "/udownload/"

    }

    fun btDownload(view: View) {
        val request = Request.Builder()
            .url(URL)
            .build()
        OkHttpClient.Builder().build().newCall(request).enqueue(object : Callback {
            override fun onFailure(p0: Call, p1: IOException) {
                ULog.e("test http error ", p1)
            }

            override fun onResponse(p0: Call, response: Response) {
                val result = response.body()?.string()
                val data =
                    Gson().fromJson(result, RootData::class.java)
                var aIndex = 0
                var bIndex = 0
                uTask = UTask(true, "test").apply {
                    data.manifiest.forEach {
                        if (aIndex >= 400) {
                            return@apply
                        }
                        aIndex++
                        add(
                            Item(
                                "https://inner-cdn.dhgames.cn:12345/ih/${it.md5}",
                                dirPath + it.path, it.md5, it.size
                            )
                        )
                    }
                }.apply {
                    downloadFinishListener =
                        { tasks: Collection<Item>, successTasks: Collection<Item>, failedTasks: Collection<Item> ->
                            appendResult(
                                "下载结束了 总数${tasks.size} , 成功数${successTasks.size} , 失败数${failedTasks.size}"
                            )
                        }
                    downloadItemFinishListener = {
                        appendResult("item ${it.url} 下载完成")
                    }
                    downloadProgressListener =
                        { totalBytes: Long, finishBytes: Long, speed: String ->
                            appendResult("下载进度，总大小 $totalBytes ，已经完成的大小 $finishBytes , 进度 ${finishBytes / totalBytes.toFloat()} , 当前速度 $speed")
                        }
                    downloadStateChangeListener = {
                        appendResult("状态变化 $it")
                    }
                    start(this@MainActivity)
                }

//                UTask(true, "test2").apply {
//                    data.manifiest.forEach {
//                        if (bIndex >= 80) {
//                            return@apply
//                        }
//                        bIndex++
//                        add(
//                                Item(
//                                        "https://inner-cdn.dhgames.cn:12345/ih/${it.md5}",
//                                        applicationContext.getExternalFilesDir("")
//                                                ?.getAbsolutePath() + "/udownload1/" + it.path,
//                                        it.md5, it.size
//                                )
//                        )
//                    }
//                }.apply {
//                    start(this@MainActivity)
//                }
            }
        })
    }

    fun btMd5(view: View) {
        Thread {
            Log.d(
                "aaaa", "" + MD5Util.getFileMD5(
                    File(
                        applicationContext.getExternalFilesDir("")
                            ?.absolutePath + "/dev_debug-1.8.0.apk"
                    )
                )
            )
        }.start()

    }


    fun btPause(view: View) {
        uTask?.pause(this)
    }

    fun btRestart(view: View) {
        uTask?.restart(this)
    }


    fun btStop(view: View) {
        uTask?.stop(this)
    }

    fun btClear(view: View) {
        val file = File(applicationContext.getExternalFilesDir("")?.absolutePath)
        delFile(file)
        Toast.makeText(this, "清空成功", Toast.LENGTH_LONG).show()
    }

    fun btClearNotificaiton(view: View) {
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?)?.cancelAll()
    }

    private fun delFile(file: File) {
        if (file.isDirectory) {
            val childFiles = file.listFiles()
            childFiles?.forEach {
                delFile(it)
            }
        } else {
            file.delete()
        }
    }

    private fun appendResult(msg: String) {
        Log.d(TAG, msg)
        tv_log.text = "$msg\n\n${tv_log.text}"
    }

}

data class Data(val path: String, val md5: String, val size: Long)

data class RootData(val manifiest: List<Data>)