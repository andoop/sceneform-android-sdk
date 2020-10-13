package com.google.ar.sceneform.samples.hellosceneform

import android.os.Handler
import android.os.Message

open class Timer : Handler() {
    var callback: Callback? = null
    override fun handleMessage(msg: Message?) {
        super.handleMessage(msg)
        callback?.onTick()
        sendEmptyMessageDelayed(0, 1000)
    }

    fun star() {
        sendEmptyMessage(0)
    }

    fun stop() {
        removeCallbacksAndMessages(null)
    }

    interface Callback {
        fun onTick()
    }
}