package org.cocos2dx.lib

import android.content.Context
import android.opengl.GLSurfaceView
import android.view.KeyEvent
import android.view.MotionEvent

class Cocos2dxGLSurfaceView(context: Context) : GLSurfaceView(context) {
    @Volatile private var cocosRenderer: Cocos2dxRenderer? = null

    init {
        setEGLContextClientVersion(2)
        setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        renderMode = RENDERMODE_CONTINUOUSLY
        currentView = this
    }

    fun setCocos2dxRenderer(renderer: Cocos2dxRenderer) {
        cocosRenderer = renderer
        renderer.setScreenWidthAndHeight(width.coerceAtLeast(1), height.coerceAtLeast(1))
        setRenderer(renderer)
    }

    fun sendKeyBack() {
        queueEvent { cocosRenderer?.handleKeyDown(KeyEvent.KEYCODE_BACK) }
    }

    fun updateRefreshRate() { /* The Android display scheduler controls the refresh rate. */ }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val renderer = cocosRenderer ?: return true
        val timestamp = event.eventTime / 1000.0
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val index = event.actionIndex
                val id = event.getPointerId(index)
                val x = event.getX(index)
                val y = event.getY(index)
                queueEvent { renderer.handleActionDown(id, x, y, timestamp) }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                val index = event.actionIndex
                val id = event.getPointerId(index)
                val x = event.getX(index)
                val y = event.getY(index)
                queueEvent { renderer.handleActionUp(id, x, y, timestamp) }
            }
            MotionEvent.ACTION_MOVE -> {
                val ids = IntArray(event.pointerCount) { event.getPointerId(it) }
                val xs = FloatArray(event.pointerCount) { event.getX(it) }
                val ys = FloatArray(event.pointerCount) { event.getY(it) }
                queueEvent { renderer.handleActionMove(ids, xs, ys, timestamp) }
            }
            MotionEvent.ACTION_CANCEL -> {
                val ids = IntArray(event.pointerCount) { event.getPointerId(it) }
                val xs = FloatArray(event.pointerCount) { event.getX(it) }
                val ys = FloatArray(event.pointerCount) { event.getY(it) }
                queueEvent { renderer.handleActionCancel(ids, xs, ys, timestamp) }
            }
        }
        return true
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val renderer = cocosRenderer ?: return super.onKeyDown(keyCode, event)
        queueEvent { renderer.handleKeyDown(keyCode) }
        return true
    }

    companion object {
        @Volatile private var currentView: Cocos2dxGLSurfaceView? = null

        @JvmStatic fun queueAccelerometer(x: Float, y: Float, z: Float, timestamp: Long) {
            currentView?.queueEvent { Cocos2dxAccelerometer.onSensorChanged(x, y, z, timestamp) }
        }
    }
}
