package com.dhu.usdk.support.udownload.utils

import android.os.Binder
import android.os.Bundle

object ObjectWrapperForBinder {
    private const val BUNDLE = "bundle"
    fun create(data: Any): Bundle {
        val bundle = Bundle()
        bundle.putBinder(BUNDLE, ObjectBinder(data))
        return bundle
    }

    fun getData(bundle: Bundle?): Any? {
        val binder = bundle?.getBinder(BUNDLE) ?: return null
        if (binder is ObjectBinder) {
            return binder.data
        }
        return null
    }
}

data class ObjectBinder(val data: Any) : Binder()