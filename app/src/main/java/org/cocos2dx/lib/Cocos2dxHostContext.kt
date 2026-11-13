package org.cocos2dx.lib

import android.content.Context
import java.lang.ref.WeakReference

object Cocos2dxHostContext {
    @Volatile private var reference: WeakReference<Context>? = null
    var current: Context?
        get() = reference?.get()
        set(value) { reference = value?.let(::WeakReference) }
}
