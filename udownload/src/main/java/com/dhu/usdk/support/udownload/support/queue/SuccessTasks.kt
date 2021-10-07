package com.dhu.usdk.support.udownload.support.queue

import com.dhu.usdk.support.udownload.Item
import com.dhu.usdk.support.udownload.State
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.collections.ArrayList

class SuccessTasks : ConcurrentLinkedQueue<Item>() {

    fun addSuccessItem(item: Item): Boolean {
        if (contains(item)) {
            return true
        }
        val items = ArrayList<Item>()
        items.add(item)
        item.state = State.SUCCESS
        item.duplicateItem.forEach {
            it.state = State.SUCCESS
            items.add(it)
        }

        return addAll(items)
    }

}