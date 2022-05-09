package com.dhu.usdk.support.udownload.modules.network

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import com.dhu.usdk.support.udownload.utils.ULog
import com.dhu.usdk.support.udownload.utils.application
import java.lang.ref.WeakReference


class NetWorkStateManager private constructor() {
    private val observers = mutableListOf<WeakReference<(State) -> Unit>>()

    companion object {
        val instance = Holder.hold
    }

    object Holder {
        val hold = NetWorkStateManager()
    }

    fun addObserver(observer: (State) -> Unit) {
        observers.add(WeakReference(observer))
    }

    enum class State {
        WIFI, CELLULAR, NO_NETWORK
    }

    init {
        val netReceiver: BroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val action = intent?.action
                if (action == ConnectivityManager.CONNECTIVITY_ACTION) {
                    val connectivityManager: ConnectivityManager? =
                        context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
                    val networkInfo = connectivityManager?.activeNetworkInfo
                    var state = State.CELLULAR
                    if (networkInfo != null && networkInfo.isAvailable) {
                        when (networkInfo.type) {
                            0 -> {
                                //移动 网络    2G 3G 4G 都是一样的 实测 mix2s 联通卡
                                state = State.CELLULAR
                            }
                            1, 9 -> {
                                //wifi网络
                                state = State.WIFI
                            }
                        }
                    } else {
                        // 无网络
                        state = State.NO_NETWORK
                    }
                    ULog.d("state change $state")
                    for (index in 0 until observers.size) {
                        observers[index].get()?.apply {
                            this(state)
                        }
                    }
                }
            }
        }

        val timeFilter = IntentFilter()
        timeFilter.addAction("android.net.ethernet.ETHERNET_STATE_CHANGED")
        timeFilter.addAction("android.net.ethernet.STATE_CHANGE")
        timeFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE")
        timeFilter.addAction("android.net.wifi.WIFI_STATE_CHANGED")
        timeFilter.addAction("android.net.wifi.STATE_CHANGE")
        timeFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
        application.registerReceiver(netReceiver, timeFilter)
    }
}