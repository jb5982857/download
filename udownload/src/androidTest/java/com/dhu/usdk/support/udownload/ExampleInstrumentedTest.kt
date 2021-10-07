package com.dhu.usdk.support.udownload

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dhu.usdk.support.udownload.utils.ULog
import com.dhu.usdk.support.udownload.utils.md5

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import java.io.File

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val file =
            File("/storage/emulated/0/Android/data/com.dhu.usdk.udownload/files/udownload/assets-arts-audio-bgm_assets_all.bundle")
        print("file ${file.md5()}")
        assertEquals("com.dhu.usdk.support.udownload.test", appContext.packageName)
    }
}