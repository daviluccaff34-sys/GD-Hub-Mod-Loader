package org.cocos2dx.lib

import android.content.Context
import android.content.res.AssetManager
import android.os.Process
import java.io.File

@Suppress("unused", "KotlinJniMissingFunction")
object Cocos2dxHelper {
    private var assetManager: AssetManager? = null
    private var accelerometer: Cocos2dxAccelerometer? = null
    private var accelerometerEnabled = false
    private var writablePath: String? = null
    private var listener: Cocos2dxHelperListener? = null
    private var context: Context? = null

    @JvmStatic external fun nativeSetApkPath(apkPath: String)
    @JvmStatic private external fun nativeSetEditTextDialogResult(bytes: ByteArray)

    @JvmStatic fun getCocos2dxWritablePath(): String? = writablePath
    @JvmStatic fun getAssetManager(): AssetManager? = assetManager
    @JvmStatic fun terminateProcess() = Process.killProcess(Process.myPid())
    @JvmStatic fun getDPI(): Int = context?.resources?.configuration?.densityDpi ?: -1

    @JvmStatic fun enableAccelerometer() {
        accelerometerEnabled = true
        accelerometer?.enable()
    }

    @JvmStatic fun setAccelerometerInterval(interval: Float) {
        accelerometer?.setInterval(interval)
    }

    @JvmStatic fun disableAccelerometer() {
        accelerometerEnabled = false
        accelerometer?.disable()
    }

    fun onResume() { if (accelerometerEnabled) accelerometer?.enable() }
    fun onPause() { if (accelerometerEnabled) accelerometer?.disable() }

    @JvmStatic fun showDialog(title: String, message: String) {
        listener?.showDialog(title, message)
    }

    fun init(context: Context, helperListener: Cocos2dxHelperListener) {
        this.context = context.applicationContext
        this.listener = helperListener
        this.writablePath = File(context.filesDir, "gdhub/save").apply { mkdirs() }.absolutePath + File.separator
        this.assetManager = context.assets
        this.accelerometer = Cocos2dxAccelerometer(context)
        Cocos2dxBitmap.setContext(context)
    }

    interface Cocos2dxHelperListener {
        fun runOnGLThread(runnable: Runnable)
        fun showDialog(title: String, message: String)
        fun showEditTextDialog(title: String, message: String, inputMode: Int, inputFlag: Int, returnType: Int, maxLength: Int)
    }
}
