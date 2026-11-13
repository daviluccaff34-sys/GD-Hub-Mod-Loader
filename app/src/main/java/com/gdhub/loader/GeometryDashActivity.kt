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
    private lateinit var modsList: LinearLayout
    @Volatile private var runtimeReady = false
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
        activeActivity = this
        try {
            FMOD.init(this)
            val info = packageManager.getPackageInfo(gamePackage, 0)
            attachGameAssets(info)
            Cocos2dxHelper.init(this, this)
            loadGameLibrary(info, "fmod")
            loadGameLibrary(info, "cocos2dcpp")

            val saveDirectory = File(filesDir, "gdhub/save").apply { mkdirs() }
            LauncherFix.setOriginalDataPath("/data/data/$gamePackage")
            LauncherFix.setDataPath(saveDirectory.absolutePath)
            val gameVersionCode = if (android.os.Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()
            if (gameVersionCode >= 39L) LauncherFix.enableExceptionsRenaming()
            LauncherFix.performPatches()

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
        val addAssetPath = assets.javaClass.getDeclaredMethod("addAssetPath", String::class.java)
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
            contentDescription = "Abrir painel do GD Hub"
            setOnClickListener {
                if (overlayPanel.visibility == View.VISIBLE) {
                    overlayPanel.visibility = View.GONE
                } else {
                    refreshModList()
                    overlayPanel.visibility = View.VISIBLE
                }
            }
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
            text = "↻"
            contentDescription = "Atualizar lista de mods"
            setOnClickListener { refreshModList() }
        }, LinearLayout.LayoutParams(dp(42), dp(38)))
        header.addView(Button(this).apply {
            text = "×"
            contentDescription = "Fechar painel"
            setOnClickListener { overlayPanel.visibility = View.GONE }
        }, LinearLayout.LayoutParams(dp(42), dp(38)))
        overlayPanel.addView(header)

        output = TextView(this).apply {
            text = "Preparando runtime do GD Hub…"
            textSize = 11f
            setTextColor(Color.LTGRAY)
            setPadding(0, dp(8), 0, dp(8))
        }
        overlayPanel.addView(output)

        val scroll = ScrollView(this)
        modsList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(modsList)
        overlayPanel.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        val exit = Button(this).apply {
            text = "Sair do jogo"
            setOnClickListener {
                AlertDialog.Builder(this@GeometryDashActivity)
                    .setTitle("Fechar Geometry Dash?")
                    .setMessage("O jogo será encerrado e você voltará ao GD Hub.")
                    .setNegativeButton("Cancelar", null)
                    .setPositiveButton("Sair") { _, _ -> finish() }
                    .show()
            }
        }
        overlayPanel.addView(exit, LinearLayout.LayoutParams(-1, dp(42)))

        val panelParams = FrameLayout.LayoutParams(dp(292), dp(340), Gravity.BOTTOM or Gravity.END)
        panelParams.setMargins(0, 0, dp(14), dp(78))
        root.addView(overlayPanel, panelParams)
        refreshModList()
    }

    private fun refreshModList() {
        if (!::modsList.isInitialized) return
        modsList.removeAllViews()
        val modsDir = File(filesDir, "mods")
        val packages = modsDir.listFiles { file ->
            file.isFile && file.extension.equals("gdhb", true)
        }?.sortedBy { it.name } ?: emptyList()

        if (packages.isEmpty()) {
            modsList.addView(TextView(this).apply {
                text = "Nenhum pacote .gdhb instalado. Volte ao gerenciador para importar mods."
                textSize = 11f
                setTextColor(Color.LTGRAY)
                setPadding(0, dp(12), 0, dp(12))
            })
            return
        }

        val prefs = getSharedPreferences("gdhub_mod_states", MODE_PRIVATE)
        packages.forEach { file ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(9), dp(8), dp(9), dp(8))
                background = rounded(Color.rgb(39, 37, 56), 10)
            }
            val runButton = Button(this).apply {
                text = "Executar pacote"
                isEnabled = prefs.getBoolean(file.name, false) && runtimeReady
                alpha = if (isEnabled) 1f else 0.45f
                setOnClickListener {
                    if (!prefs.getBoolean(file.name, false)) {
                        output.text = "Ative este pacote antes de executar."
                    } else {
                        isEnabled = false
                        output.text = "Executando ${file.name}…"
                        Thread {
                            val result = runCatching {
                                GdHubRuntime.nativeRunPackage(file.absolutePath)
                            }.getOrElse { "Falha no runtime: ${it.message}" }
                            runOnUiThread {
                                output.text = result
                                isEnabled = prefs.getBoolean(file.name, false) && runtimeReady
                            }
                        }.start()
                    }
                }
            }
            val enabled = android.widget.Switch(this).apply {
                text = file.name
                textSize = 11f
                setTextColor(Color.WHITE)
                isChecked = prefs.getBoolean(file.name, false)
                setOnCheckedChangeListener { _, checked ->
                    prefs.edit().putBoolean(file.name, checked).apply()
                    runButton.isEnabled = checked && runtimeReady
                    runButton.alpha = if (runButton.isEnabled) 1f else 0.45f
                }
            }
            card.addView(enabled)
            card.addView(runButton, LinearLayout.LayoutParams(-1, dp(38)))
            val params = LinearLayout.LayoutParams(-1, -2)
            params.bottomMargin = dp(7)
            modsList.addView(card, params)
        }
    }

    companion object {
        @Volatile private var activeActivity: GeometryDashActivity? = null

        @JvmStatic
        fun onNativeRuntimeReady(message: String) {
            val activity = activeActivity ?: return
            activity.runOnUiThread {
                activity.runtimeReady = true
                if (activity::output.isInitialized) activity.output.text = message
                activity.refreshModList()
            }
        }
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

    @Deprecated("Deprecated in Android API")
    override fun onBackPressed() {
        if (::gameView.isInitialized) gameView.sendKeyBack() else super.onBackPressed()
    }

    override fun onDestroy() {
        runCatching { FMOD.close() }
        Cocos2dxHostContext.current = null
        if (activeActivity === this) activeActivity = null
        super.onDestroy()
    }
}
