package moe.megurine.music

import android.app.Application
import moe.megurine.music.music.AudioFocusMonitor
import moe.megurine.music.music.FairMemoryGuard
import moe.megurine.music.music.MusicLibrary

class HyperApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        MusicLibrary.init(this)
        AudioFocusMonitor.init(this)
        FairMemoryGuard.init(this)
    }

    companion object {
        lateinit var instance: HyperApp
            private set
    }
}
