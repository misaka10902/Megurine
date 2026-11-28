package moe.megurine.music.music

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Parcel
import android.util.Log

object FairMemoryGuard {

    private const val TAG = "FairMemoryGuard"
    private const val ACTION_TRIM = "itgsa.intent.action.TRIM"
    private const val ACTION_KILL = "itgsa.intent.action.KILL"

    private const val NOTIFY_PHYSICAL_MEMORY = 1000
    private const val RESULT_OK = 0

    @Volatile
    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        initialized = true
        val app = context.applicationContext
        val thread = HandlerThread(TAG).apply { start() }
        val handler = Handler(thread.looper)
        val filter = IntentFilter().apply {
            addAction(ACTION_TRIM)
            addAction(ACTION_KILL)
        }
        runCatching {
            app.registerReceiver(receiver, filter, null, handler, Context.RECEIVER_EXPORTED)
        }.onFailure { Log.w(TAG, "register fair-memory receiver failed", it) }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            val common = intent.getBundleExtra(KEY_COMMON) ?: return
            val extra = intent.getBundleExtra(KEY_EXTRA) ?: Bundle()
            val notifyType = common.getInt("notifyType")
            val notifyId = common.getInt("notifyId")
            val callback = common.getBinder("callback")

            when (action) {
                ACTION_TRIM -> releaseMemory(notifyType)
                ACTION_KILL -> {
                    saveSession()
                    releaseMemory(notifyType)
                }
            }

            if (callback != null) {
                reply(callback, notifyType, notifyId, RESULT_OK, extra)
            }
        }
    }

    private fun releaseMemory(notifyType: Int) {
        CoverLoader.trimMemory()
        if (notifyType == NOTIFY_PHYSICAL_MEMORY) {
            CoverLoader.trimResolved()
        }
    }

    private fun saveSession() {
        runCatching { PlaybackController.saveSession(blocking = true) }
            .onFailure { Log.w(TAG, "save session failed", it) }
    }

    private fun reply(callback: IBinder, notifyType: Int, notifyId: Int, result: Int, extra: Bundle) {
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        try {
            data.writeInt(notifyType)
            data.writeInt(notifyId)
            data.writeInt(result)
            data.writeBundle(extra)
            callback.transact(IBinder.FIRST_CALL_TRANSACTION, data, reply, IBinder.FLAG_ONEWAY)
            reply.readException()
        } catch (t: Throwable) {
            Log.w(TAG, "reply fair-memory result failed", t)
        } finally {
            reply.recycle()
            data.recycle()
        }
    }

    private const val KEY_COMMON = "common"
    private const val KEY_EXTRA = "extra"
}
