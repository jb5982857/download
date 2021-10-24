package com.dhu.usdk.support.udownload.modules.download

import com.dhu.usdk.support.udownload.modules.UInternalTask
import java.util.concurrent.ConcurrentLinkedQueue

class DownLoadTaskManager {
    private val lock = Object()
    private val downloadTasks = ConcurrentLinkedQueue<UInternalTask>()

    fun getTask(): UInternalTask {
        synchronized(lock) {
            while (downloadTasks.isEmpty()) {
                lock.wait()
            }
        }

        return downloadTasks.first()
    }


    fun addTask(task: UInternalTask) {
        synchronized(lock) {
            downloadTasks.add(task)
            lock.notify()
        }
    }
}