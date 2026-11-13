package com.customRobTop

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import org.cocos2dx.lib.Cocos2dxGLSurfaceView
import java.lang.ref.WeakReference
import java.util.UUID

/**
 * Java/JNI compatibility bridge used by Geometry Dash 2.2.
 * The game expects these static methods even when optional platform services
 * (ads, Everyplay and Google Play Games) are not provided by the host launcher.
 */
@Suppress("unused", "UNUSED_PARAMETER")
object BaseRobTopActivity {
    const val KEYBOARD_INPUT_MULTILINE = 1
    const val KEYBOARD_INPUT_PASSWORD = 2
    const val KEYBOARD_INPUT_NUMERIC = 4

    @JvmField var isPaused: Boolean = false
    @JvmField var blockBackButton: Boolean = false
    @JvmField var curKeyboardInputFlags: Int = 0
    @JvmField var me: WeakReference<Activity> = WeakReference(null)

    private var keyboardActive = false
    private var resumeSound = true

    @JvmStatic fun setCurrentActivity(activity: Activity) {
        me = WeakReference(activity)
    }

    @JvmStatic fun getUserID(): String {
        val activity = me.get() ?: return UUID.randomUUID().toString()
        val id = Settings.Secure.getString(activity.contentResolver, Settings.Secure.ANDROID_ID) ?: return UUID.randomUUID().toString()
        return if (id == "9774d56d682e549c") UUID.randomUUID().toString()
        else UUID.nameUUIDFromBytes(id.toByteArray()).toString()
    }

    @JvmStatic fun isNetworkAvailable(): Boolean {
        val activity = me.get() ?: return false
        val manager = activity.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = manager.activeNetwork ?: return false
        val caps = manager.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    @JvmStatic fun openURL(url: String) {
        me.get()?.runOnUiThread {
            runCatching {
                it.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }.onFailure { error -> Toast.makeText(it, error.message ?: "Não foi possível abrir o link", Toast.LENGTH_SHORT).show() }
        }
    }

    @JvmStatic fun copyToClipboard(value: String) {
        val activity = me.get() ?: return
        activity.runOnUiThread {
            val manager = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            manager.setPrimaryClip(ClipData.newPlainText("text", value))
        }
    }

    @JvmStatic fun getClipboardString(): String {
        val activity = me.get() ?: return ""
        val clip = (activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).primaryClip
        return if (clip != null && clip.itemCount > 0) clip.getItemAt(0).coerceToText(activity).toString() else ""
    }

    @JvmStatic fun sendMail(subject: String, body: String, to: String) {
        me.get()?.runOnUiThread {
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$to"))
                .putExtra(Intent.EXTRA_SUBJECT, subject)
                .putExtra(Intent.EXTRA_TEXT, body)
            runCatching { it.startActivity(intent) }
        }
    }

    @JvmStatic fun shouldResumeSound(): Boolean = resumeSound
    @JvmStatic fun setKeyboardState(value: Boolean) { keyboardActive = value }
    @JvmStatic fun onToggleKeyboard() {
        me.get()?.runOnUiThread {
            if (keyboardActive) Cocos2dxGLSurfaceView.openIMEKeyboard()
            else Cocos2dxGLSurfaceView.closeIMEKeyboard()
        }
    }
    @JvmStatic fun loadingFinished() {}
    @Suppress("DEPRECATION")\n    @JvmStatic fun getDeviceRefreshRate(): Float {
        val activity = me.get() ?: return 60f
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) activity.display?.refreshRate ?: 60f
        else activity.windowManager.defaultDisplay.refreshRate
    }

    @JvmStatic fun setupEveryplay() {}
    @JvmStatic fun isEveryplaySupported(): Boolean = false
    @JvmStatic fun isRecordingSupported(): Boolean = false
    @JvmStatic fun isRecordingPaused(): Boolean = false
    @JvmStatic fun isRecording(): Boolean = false
    @JvmStatic fun startRecording() {}
    @JvmStatic fun stopRecording() {}
    @JvmStatic fun resumeRecording() {}
    @JvmStatic fun pauseRecording() {}
    @JvmStatic fun playLastRecording() {}
    @JvmStatic fun showEveryplay() {}
    @JvmStatic fun setEveryplayMetadata(levelID: String, levelName: String) {}
    @JvmStatic fun onNativePause() {}
    @JvmStatic fun onNativeResume() {}

    @JvmStatic fun gameServicesIsSignedIn(): Boolean = false
    @JvmStatic fun gameServicesSignIn() {}
    @JvmStatic fun gameServicesSignOut() {}
    @JvmStatic fun unlockAchievement(id: String) {}
    @JvmStatic fun showAchievements() {}

    @JvmStatic fun enableBanner() {}
    @JvmStatic fun disableBanner() {}
    @JvmStatic fun showInterstitial() {}
    @JvmStatic fun cacheInterstitial() {}
    @JvmStatic fun hasCachedInterstitial(): Boolean = false
    @JvmStatic fun showRewardedVideo() {}
    @JvmStatic fun cacheRewardedVideo() {}
    @JvmStatic fun hasCachedRewardedVideo(): Boolean = false
    @JvmStatic fun queueRefreshBanner() {}
    @JvmStatic fun enableBannerNoRefresh() {}
    @JvmStatic fun tryShowRateDialog(appName: String) {}
    @JvmStatic fun openAppPage() {}
    @JvmStatic fun logEvent(event: String) {}

    @JvmStatic fun setKeyboardInputFlags(flags: Int) { curKeyboardInputFlags = flags }
    @JvmStatic fun pickImportFiles() {}
    @JvmStatic fun shareFiles(files: Array<String>) {}
}
