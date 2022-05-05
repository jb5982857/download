package com.dhu.usdk.udownload

import android.app.Application
import com.dhu.usdk.support.udownload.UTask

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        UTask.setApplication(this)
    }
}