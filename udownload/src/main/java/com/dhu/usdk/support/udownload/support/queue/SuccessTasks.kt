package com.dhu.usdk.support.udownload.support.queue

import com.dhu.usdk.support.udownload.Item
import com.dhu.usdk.support.udownload.ResultState
import com.dhu.usdk.support.udownload.State
import com.dhu.usdk.support.udownload.UTask
import com.dhu.usdk.support.udownload.modules.DownloadScheduleModule
import com.dhu.usdk.support.udownload.utils.mainHandler
import com.dhu.usdk.support.udownload.utils.switchCallbackThreadIfNeed
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.collections.ArrayList

class SuccessTasks(val task: UTask) : ConcurrentLinkedQueue<Item>() {

    fun addSuccessItem(item: Item): Boolean {
        synchronized(this) {
            if (contains(item)) {
                return true
            }
            val items = ArrayList<Item>()
            switchCallbackThreadIfNeed {
                task.downloadItemFinishListener(item)
            }
            items.add(item)
            item.resultState = ResultState()
            item.duplicateItem.forEach {
                it.resultState = ResultState()
                switchCallbackThreadIfNeed {
                    task.downloadItemFinishListener(it)
                }
                items.add(it)
            }

            return addAll(items)
        }
    }

}