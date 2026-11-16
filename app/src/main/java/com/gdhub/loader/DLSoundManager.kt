package com.gdhub.loader

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import java.io.File

/**
 * Plays DL Loader UI notification sounds stored in app/src/main/assets/sounds/.
 * Missing assets are tolerated so development builds remain launchable.
 */
internal class DLSoundManager(context: Context) {
    private val soundPool: SoundPool
    private val soundIds = mutableMapOf<String, Int>()
    private val loadedIds = mutableSetOf<Int>()
    @Volatile private var released = false

    init {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(attributes)
            .build()
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0 && !released) synchronized(loadedIds) { loadedIds.add(sampleId) }
            else if (status != 0) Log.w(TAG, "Could not load sound sample $sampleId (status=$status)")
        }

        val soundFiles = listOf(
            "newNotif00",
            "newNotif01",
            "newNotif02",
            "newNotif03",
            "byeNotif00"
        )
        val soundDirectory = File(context.cacheDir, "dl-loader-sounds").apply { mkdirs() }
        soundFiles.forEach { soundName ->
            runCatching {
                val assetName = "sounds/$soundName.ogg"
                val destination = File(soundDirectory, "$soundName.ogg")
                context.assets.open(assetName).use { input ->
                    destination.outputStream().use { output -> input.copyTo(output) }
                }
                soundIds[soundName] = soundPool.load(destination.absolutePath, 1)
            }.onFailure {
                Log.i(TAG, "Sound asset not installed yet: sounds/$soundName.ogg")
            }
        }
    }

    fun play(soundName: String) {
        if (released) return
        val sampleId = soundIds[soundName] ?: return
        val isLoaded = synchronized(loadedIds) { loadedIds.contains(sampleId) }
        if (isLoaded) soundPool.play(sampleId, 1f, 1f, 1, 0, 1f)
    }

    fun release() {
        if (released) return
        released = true
        synchronized(loadedIds) { loadedIds.clear() }
        soundPool.release()
    }

    private companion object {
        const val TAG = "DLSoundManager"
    }
}
