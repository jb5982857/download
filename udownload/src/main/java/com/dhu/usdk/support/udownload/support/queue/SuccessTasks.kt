package com.dhu.usdk.support.udownload.support.queue

import com.dhu.usdk.support.udownload.Item
import com.dhu.usdk.support.udownload.State
import com.dhu.usdk.support.udownload.UTask
import com.dhu.usdk.support.udownload.modules.DownloadScheduleModule
import com.dhu.usdk.support.udownload.utils.mainHandler
import com.dhu.usdk.support.udownload.utils.switchUiThreadIfNeeded
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.collections.ArrayList

class SuccessTasks(val task: UTask) : ConcurrentLinkedQueue<Item>() {

    fun addSuccessItem(item: Item): Boolean {
        if (contains(item)) {
            return true
        }
        val items = ArrayList<Item>()
        switchUiThreadIfNeeded {
            task.downloadItemFinishListener(item)
        }
        items.add(item)
        item.state = State.SUCCESS
        item.duplicateItem.forEach {
            it.state = State.SUCCESS
            switchUiThreadIfNeeded {
                task.downloadItemFinishListener(it)
            }
            items.add(it)
        }

        return addAll(items)
    }

}