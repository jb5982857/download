package com.dhu.usdk.support.udownload.modules.download

import java.util.concurrent.ConcurrentLinkedQueue

class DownLoadTaskManager {
    private val lock = Object()
    private val downloadTasks = ConcurrentLinkedQueue<UInternalTask>()

    fun next(): UInternalTask {
        synchronized(lock) {
            while (downloadTasks.isEmpty()) {
                lock.wait()
            }
        }

        return downloadTasks.poll()
    }


    fun addTask(task: UInternalTask) {
        synchronized(lock) {
            downloadTasks.add(task)
            lock.notify()
        }
    }
}