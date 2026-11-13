package com.gdhub.loader

import android.app.Activity
import android.app.AlertDialog
import android.content.pm.ActivityInfo
import android.content.pm.PackageInfo
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Environment
import android.os.Process
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.util.Log
import com.customRobTop.JniToCpp
import org.cocos2dx.lib.Cocos2dxGLSurfaceView
import org.cocos2dx.lib.Cocos2dxHelper
import org.cocos2dx.lib.Cocos2dxRenderer
import org.cocos2dx.lib.Cocos2dxHostContext
import org.fmod.FMOD
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile

class GeometryDashActivity : Activity(), Cocos2dxHelper.Cocos2dxHelperListener {
    private lateinit var gameView: Cocos2dxGLSurfaceView
    private lateinit var renderer: Cocos2dxRenderer
    private lateinit var overlayPanel: LinearLayout
    private lateinit var output: TextView
    private val gamePackage = GameInstallProbe.GAME_PACKAGE
    private val purple = Color.rgb(116, 75, 230)
    private val panelColor = Color.rgb(25, 24, 38)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        )
        Cocos2dxHostContext.current = this
        try {
            FMOD.init(this)
            val info = packageManager.getPackageInfo(gamePackage, 0)
            attachGameAssets(info)
            Cocos2dxHelper.init(this, this)
            loadGameLibrary(info, "fmod")
            loadGameLibrary(info, "cocos2dcpp")
            Cocos2dxHelper.nativeSetApkPath(info.applicationInfo!!.sourceDir)
            runCatching {
                JniToCpp.setupHSSAssets(info.applicationInfo!!.sourceDir, Environment.getExternalStorageDirectory().absolutePath)
            }.onFailure { Log.w("GDHub", "HSS asset setup was not available", it) }
            createGameSurface()
        } catch (error: Throwable) {
            Log.e("GDHub", "Could not start the in-process Geometry Dash runtime", error)
            AlertDialog.Builder(this)
                .setTitle("Não foi possível iniciar o Geometry Dash")
                .setMessage(error.message ?: error.javaClass.simpleName)
                .setPositiveButton("Voltar") { _, _ -> finish() }
                .setOnCancelListener { finish() }
                .show()
        }
    }

    private fun attachGameAssets(info: PackageInfo) {
        val addAssetPath = assets.javaClass.getMethod("addAssetPath", String::class.java)
        val app = info.applicationInfo!!
        addAssetPath.invoke(assets, app.sourceDir)
        app.splitSourceDirs?.forEach { addAssetPath.invoke(assets, it) }
    }

    private fun loadGameLibrary(info: PackageInfo, name: String) {
        val abi = if (Process.is64Bit()) "arm64-v8a" else "armeabi-v7a"
        val destinationDir = File(filesDir, "gdhub/game-libs/$abi").apply { mkdirs() }
        val destination = File(destinationDir, "lib$name.so")
        if (!destination.exists() || destination.length() == 0L) {
            val app = info.applicationInfo!!
            val extracted = File(app.nativeLibraryDir, "lib$name.so")
            if (extracted.isFile) {
                extracted.inputStream().use { input -> destination.outputStream().use { input.copyTo(it) } }
            } else {
                val apkPaths = listOf(app.sourceDir) + (app.splitSourceDirs?.toList() ?: emptyList())
                var copied = false
                for (apkPath in apkPaths) {
                    try {
                        ZipFile(apkPath).use { zip ->
                            val entry = zip.getEntry("lib/$abi/lib$name.so") ?: return@use
                            zip.getInputStream(entry).use { input ->
                                FileOutputStream(destination).use { output -> input.copyTo(output) }
                            }
                            copied = destination.length() > 0
                        }
                    } catch (e: Exception) {
                        Log.w("GDHub", "Could not read $name from $apkPath", e)
                    }
                    if (copied) break
                }
                if (!copied) throw UnsatisfiedLinkError("lib$name.so não foi encontrada para $abi")
            }
        }
        System.load(destination.absolutePath)
    }

    private fun createGameSurface() {
        val root = FrameLayout(this)
        gameView = Cocos2dxGLSurfaceView(this)
        renderer = Cocos2dxRenderer(gameView)
        gameView.setCocos2dxRenderer(renderer)
        root.addView(gameView, FrameLayout.LayoutParams(-1, -1))
        buildModOverlay(root)
        setContentView(root)
    }

    private fun buildModOverlay(root: FrameLayout) {
        val orb = TextView(this).apply {
            text = "GD"
            textSize = 14f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            background = rounded(purple, 28)
            elevation = dp(8).toFloat()
            setOnClickListener { overlayPanel.visibility = if (overlayPanel.visibility == View.VISIBLE) View.GONE else View.VISIBLE }
        }
        val orbParams = FrameLayout.LayoutParams(dp(54), dp(54), Gravity.BOTTOM or Gravity.END)
        orbParams.setMargins(0, 0, dp(14), dp(14))
        root.addView(orb, orbParams)

        overlayPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = rounded(panelColor, 16)
            elevation = dp(12).toFloat()
            visibility = View.GONE
        }
        val header = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        header.addView(TextView(this).apply {
            text = "GD HUB • MODS"
            textSize = 13f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
        }, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(Button(this).apply {
            text = "×"
            setOnClickListener { overlayPanel.visibility = View.GONE }
        }, LinearLayout.LayoutParams(dp(42), dp(38)))
        overlayPanel.addView(header)
        output = TextView(this).apply {
            text = "Selecione um pacote para executar."
            textSize = 11f
            setTextColor(Color.LTGRAY)
            setPadding(0, dp(8), 0, dp(8))
        }
        overlayPanel.addView(output)
        val scroll = ScrollView(this)
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val modsDir = File(filesDir, "mods")
        val packages = modsDir.listFiles { file -> file.isFile && file.extension.equals("gdhb", true) }?.sortedBy { it.name } ?: emptyList()
        if (packages.isEmpty()) {
            list.addView(TextView(this).apply {
                text = "Nenhum .gdhb instalado. Importe pacotes pelo gerenciador do GD Hub."
                textSize = 11f
                setTextColor(Color.LTGRAY)
                setPadding(0, dp(8), 0, dp(8))
            })
        } else {
            packages.forEach { file ->
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(8), dp(8), dp(8), dp(8))
                    background = rounded(Color.rgb(39, 37, 56), 10)
                }
                row.addView(TextView(this).apply {
                    text = file.name
                    textSize = 12f
                    setTextColor(Color.WHITE)
                    typeface = Typeface.DEFAULT_BOLD
                })
                row.addView(Button(this).apply {
                    text = "Executar pacote"
                    setOnClickListener {
                        output.text = runCatching { GdHubRuntime.nativeRunPackage(file.absolutePath) }
                            .getOrElse { "Falha: ${it.message}" }
                    }
                }, LinearLayout.LayoutParams(-1, dp(40)))
                val params = LinearLayout.LayoutParams(-1, -2)
                params.bottomMargin = dp(7)
                list.addView(row, params)
            }
        }
        scroll.addView(list)
        overlayPanel.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        val panelParams = FrameLayout.LayoutParams(dp(280), dp(310), Gravity.BOTTOM or Gravity.END)
        panelParams.setMargins(0, 0, dp(14), dp(78))
        root.addView(overlayPanel, panelParams)
    }

    private fun rounded(color: Int, radiusDp: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radiusDp).toFloat()
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density + 0.5f).toInt()

    override fun runOnGLThread(runnable: Runnable) {
        if (::gameView.isInitialized) gameView.queueEvent(runnable)
    }

    override fun showDialog(title: String, message: String) {
        runOnUiThread { AlertDialog.Builder(this).setTitle(title).setMessage(message).setPositiveButton("OK", null).show() }
    }

    override fun showEditTextDialog(title: String, message: String, inputMode: Int, inputFlag: Int, returnType: Int, maxLength: Int) {
        runOnUiThread {
            val input = EditText(this).apply { setText(message); setSelection(text.length) }
            AlertDialog.Builder(this).setTitle(title).setView(input)
                .setPositiveButton("OK", null).setNegativeButton("Cancelar", null).show()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::gameView.isInitialized) {
            gameView.onResume()
            gameView.queueEvent { renderer.handleOnResume() }
            Cocos2dxHelper.onResume()
        }
    }

    override fun onPause() {
        if (::gameView.isInitialized) {
            gameView.queueEvent { renderer.handleOnPause() }
            Cocos2dxHelper.onPause()
            gameView.onPause()
        }
        super.onPause()
    }

    override fun onDestroy() {
        runCatching { FMOD.close() }
        Cocos2dxHostContext.current = null
        super.onDestroy()
    }
}
