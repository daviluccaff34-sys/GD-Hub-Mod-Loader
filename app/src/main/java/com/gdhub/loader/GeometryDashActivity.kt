package com.gdhub.loader

import android.app.Activity
import android.app.AlertDialog
import android.content.pm.ActivityInfo
import android.content.pm.PackageInfo
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Environment
import android.os.Process
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.util.Log
import com.customRobTop.BaseRobTopActivity
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
    private lateinit var dlSounds: DLSoundManager
    private lateinit var renderer: Cocos2dxRenderer
    private lateinit var overlayPanel: LinearLayout
    private lateinit var output: TextView
    private lateinit var modsList: LinearLayout
    private var packageDialog: AlertDialog? = null
    @Volatile private var runtimeReady = false
    private val executedMods = mutableSetOf<String>()
    private val gamePackage = GameInstallProbe.GAME_PACKAGE
    private val panelColor = Color.rgb(25, 24, 38)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dlSounds = DLSoundManager(this)
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
            setBootStage("Preparando FMOD")
            FMOD.init(this)
            setBootStage("Lendo instalação do Geometry Dash")
            val info = packageManager.getPackageInfo(gamePackage, 0)
            setBootStage("Anexando assets do APK")
            attachGameAssets(info)

            val saveDirectory = File(filesDir, "gdhub/save").apply { mkdirs() }
            // Match the upstream launcher order: shared C++ runtime and path hooks
            // must be ready before the game's native libraries are loaded.
            setBootStage("Carregando runtime C++")
            System.loadLibrary("c++_shared")
            LauncherFix.setOriginalDataPath("/data/data/$gamePackage")
            LauncherFix.setDataPath(saveDirectory.absolutePath)

            setBootStage("Inicializando Cocos2dxHelper")
            Cocos2dxHelper.init(this, this)
            setBootStage("Carregando FMOD nativo")
            loadGameLibrary(info, "fmod")
            setBootStage("Carregando Cocos2d-x nativo")
            loadGameLibrary(info, "cocos2dcpp")

            val gameVersionCode = if (android.os.Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()
            if (gameVersionCode >= 39L) LauncherFix.enableExceptionsRenaming()
            setBootStage("Aplicando patches de compatibilidade")
            LauncherFix.performPatches()

            BaseRobTopActivity.setCurrentActivity(this)
            BaseRobTopActivity.isPaused = false
            setBootStage("Configurando caminho do APK")
            Cocos2dxHelper.nativeSetApkPath(info.applicationInfo!!.sourceDir)
            runCatching {
                JniToCpp.setupHSSAssets(info.applicationInfo!!.sourceDir, Environment.getExternalStorageDirectory().absolutePath)
            }.onFailure { Log.w("GDHub", "HSS asset setup was not available", it) }
            setBootStage("Criando superfície OpenGL")
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

    private fun setBootStage(stage: String) {
        getSharedPreferences("gdhub_boot_diagnostics", MODE_PRIVATE).edit()
            .putString("stage", stage).putLong("time", System.currentTimeMillis()).commit()
        Log.i("GDHubBoot", stage)
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
        gameView.queueEvent {
            val state = runCatching { GdHubRuntime.nativeOnGameReady() }
                .getOrElse { "ERROR|Falha ao verificar o runtime: ${it.message}" }
            runOnUiThread {
                runtimeReady = !state.startsWith("ERROR|")
                output.text = state.substringAfter("|", state)
                refreshModList()
            }
        }
        buildModOverlay(root)
        setContentView(root)
    }

    /**
     * Builds the dedicated DL Loader screen above the game's Cocos surface.
     * The entry point is intentionally not an Android View: the native
     * Geometry Dash MenuLayer integration will call openDLLoaderScreen().
     */
    private fun buildModOverlay(root: FrameLayout) {
        overlayPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(14), dp(18), dp(14))
            background = rounded(panelColor, 0)
            elevation = dp(16).toFloat()
            visibility = View.GONE
        }

        val header = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(10))
        }
        header.addView(TextView(this).apply {
            text = "DL LOADER"
            textSize = 21f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.08f
        }, LinearLayout.LayoutParams(0, -2, 1f))

        val refresh = Button(this).apply {
            text = "↻"
            contentDescription = "Atualizar lista de mods"
            setOnClickListener {
                refreshModList()
                dlSounds.play("newNotif01")
            }
        }
        header.addView(refresh, LinearLayout.LayoutParams(dp(48), dp(42)))

        val close = Button(this).apply {
            text = "×"
            contentDescription = "Voltar ao Geometry Dash"
            setOnClickListener { closeDLLoaderScreen() }
        }
        header.addView(close, LinearLayout.LayoutParams(dp(48), dp(42)))
        overlayPanel.addView(header)

        val subtitle = TextView(this).apply {
            text = "MODS INSTALADOS"
            textSize = 11f
            setTextColor(Color.rgb(174, 143, 255))
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, dp(2), 0, dp(8))
        }
        overlayPanel.addView(subtitle)

        output = TextView(this).apply {
            text = "Preparando runtime do DL Loader…"
            textSize = 12f
            setTextColor(Color.LTGRAY)
            setPadding(0, dp(5), 0, dp(10))
        }
        overlayPanel.addView(output)

        val scroll = ScrollView(this).apply {
            isFillViewport = true
        }
        modsList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        scroll.addView(modsList, FrameLayout.LayoutParams(-1, -2))
        overlayPanel.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        val footer = LinearLayout(this).apply {
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            setPadding(0, dp(10), 0, 0)
        }
        footer.addView(Button(this).apply {
            text = "VOLTAR AO JOGO"
            setOnClickListener { closeDLLoaderScreen() }
        }, LinearLayout.LayoutParams(-2, dp(46)))
        overlayPanel.addView(footer)

        root.addView(overlayPanel, FrameLayout.LayoutParams(-1, -1))

        refreshModList()
    }

    private fun openDLLoaderScreenFromNative() {
        if (!::overlayPanel.isInitialized) return
        refreshModList()
        overlayPanel.visibility = View.VISIBLE
        dlSounds.play("newNotif00")
    }

    private fun closeDLLoaderScreen() {
        if (::overlayPanel.isInitialized) overlayPanel.visibility = View.GONE
        dlSounds.play("byeNotif00")
    }

    private fun showPackageInterface(result: String) {
        val uiLines = result.lineSequence().filter { it.startsWith("@gdhub-ui|") }.toList()
        if (uiLines.isEmpty()) return

        var title = "GD Hub"
        val labels = mutableListOf<String>()
        val buttons = mutableListOf<String>()
        uiLines.forEach { line ->
            val parts = line.split("|", limit = 3)
            if (parts.size < 3) return@forEach
            when (parts[1]) {
                "title" -> title = parts[2]
                "label" -> labels.add(parts[2])
                "button" -> buttons.add(parts[2])
            }
        }

        val generateLabel = buttons.firstOrNull { it.contains("Gerar", ignoreCase = true) } ?: "Gerar blocos"
        val closeLabel = buttons.firstOrNull { it.contains("Fechar", ignoreCase = true) } ?: "Fechar"

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(8))
        }
        labels.forEach { label ->
            content.addView(TextView(this).apply {
                text = label
                textSize = 14f
                setTextColor(Color.WHITE)
                setPadding(0, dp(4), 0, dp(8))
            })
        }
        content.addView(TextView(this).apply {
            text = "A criação de objetos reais depende da integração nativa com o editor do Geometry Dash."
            textSize = 12f
            setTextColor(Color.LTGRAY)
            setPadding(0, dp(8), 0, dp(12))
        })

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.END
        }
        val closeButton = Button(this).apply {
            text = closeLabel
            setOnClickListener { packageDialog?.dismiss() }
        }
        val generateButton = Button(this).apply {
            text = generateLabel
            isEnabled = false
            contentDescription = "Geração real de blocos ainda não disponível"
        }
        actions.addView(generateButton)
        actions.addView(closeButton)
        content.addView(actions)

        packageDialog?.dismiss()
        packageDialog = AlertDialog.Builder(this)
            .setTitle(title)
            .setView(content)
            .create()
        packageDialog?.show()
    }

    private fun refreshModList() {
        if (!::modsList.isInitialized) return
        modsList.removeAllViews()
        val modsDir = File(filesDir, "mods")
        val packages = modsDir.listFiles { file -> file.isFile && file.extension.equals("gdhb", true) }?.sortedBy { it.name } ?: emptyList()
        if (packages.isEmpty()) {
            modsList.addView(TextView(this).apply { text = "Nenhum pacote .gdhb instalado. Volte ao gerenciador para importar mods."; textSize = 11f; setTextColor(Color.LTGRAY); setPadding(0, dp(12), 0, dp(12)) })
            return
        }
        val prefs = getSharedPreferences("gdhub_mod_states", MODE_PRIVATE)
        packages.forEach { file ->
            val card = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(9), dp(8), dp(9), dp(8)); background = rounded(Color.rgb(39, 37, 56), 10) }
            val enabled = android.widget.Switch(this).apply {
                text = file.name; textSize = 11f; setTextColor(Color.WHITE); isChecked = prefs.getBoolean(file.name, true)
                setOnCheckedChangeListener { _, checked ->
                    prefs.edit().putBoolean(file.name, checked).apply()
                    if (checked) { output.text = if (runtimeReady) "Carregando " + file.name + "…" else "Ativado: será carregado quando o jogo estiver pronto."; if (runtimeReady) autoRunMod(file) }
                    else { synchronized(executedMods) { executedMods.remove(file.name) }; output.text = file.name + " desativado. Não será carregado novamente nesta sessão." }
                }
            }
            card.addView(enabled)
            card.addView(TextView(this).apply { text = if (enabled.isChecked) "Ativo • carregamento automático" else "Desativado • toque para ativar"; textSize = 10f; setTextColor(Color.LTGRAY); setPadding(dp(4), dp(3), 0, 0) })
            val params = LinearLayout.LayoutParams(-1, -2); params.bottomMargin = dp(7); modsList.addView(card, params)
        }
        if (runtimeReady) autoRunEnabledMods(packages)
    }

    private fun autoRunEnabledMods(packages: List<File>) {
        val prefs = getSharedPreferences("gdhub_mod_states", MODE_PRIVATE)
        packages.filter { prefs.getBoolean(it.name, true) }.forEach { autoRunMod(it) }
    }

    private fun autoRunMod(file: File) {
        if (!runtimeReady || !file.isFile) return
        val prefs = getSharedPreferences("gdhub_mod_states", MODE_PRIVATE)
        if (!prefs.getBoolean(file.name, true)) return
        val shouldRun = synchronized(executedMods) { executedMods.add(file.name) }
        if (!shouldRun) return
        Thread {
            val result = runCatching { GdHubRuntime.nativeRunPackage(file.absolutePath) }.getOrElse { "Falha ao carregar " + file.name + ": " + it.message }
            runOnUiThread { if (prefs.getBoolean(file.name, true)) { output.text = result; showPackageInterface(result) } }
        }.start()
    }

    companion object {
        @Volatile private var activeActivity: GeometryDashActivity? = null

        /**
         * Called by the native Cocos menu button callback once the verified
         * MenuLayer hook is installed. No Android entry widget is created.
         */
        @JvmStatic
        fun openDLLoaderScreen() {
            val activity = activeActivity ?: return
            activity.runOnUiThread { activity.openDLLoaderScreenFromNative() }
        }

        @JvmStatic
        fun onNativeRuntimeReady(message: String) {
            val activity = activeActivity ?: return
            activity.runOnUiThread {
                activity.runtimeReady = !message.startsWith("Runtime GD Hub falhou")
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
        BaseRobTopActivity.isPaused = false
        if (::gameView.isInitialized) {
            gameView.onResume()
            gameView.queueEvent { renderer.handleOnResume() }
            Cocos2dxHelper.onResume()
        }
    }

    override fun onPause() {
        BaseRobTopActivity.isPaused = true
        if (::gameView.isInitialized) {
            gameView.queueEvent { renderer.handleOnPause() }
            Cocos2dxHelper.onPause()
            gameView.onPause()
        }
        super.onPause()
    }

    @Deprecated("Deprecated in Android API")
    override fun onBackPressed() {
        if (::overlayPanel.isInitialized && overlayPanel.visibility == View.VISIBLE) {
            closeDLLoaderScreen()
            return
        }
        if (::gameView.isInitialized) gameView.sendKeyBack() else super.onBackPressed()
    }

    override fun onDestroy() {
        getSharedPreferences("gdhub_boot_diagnostics", MODE_PRIVATE).edit().putString("stage", "Activity encerrada normalmente").commit()
        runCatching { dlSounds.release() }
        runCatching { FMOD.close() }
        BaseRobTopActivity.clearCurrentActivity(this)
        Cocos2dxHostContext.current = null
        if (activeActivity === this) activeActivity = null
        super.onDestroy()
    }
}
