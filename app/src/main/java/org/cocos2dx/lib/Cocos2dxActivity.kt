package org.cocos2dx.lib

import android.content.Intent
import android.net.Uri

@Suppress("unused")
object Cocos2dxActivity {
    @JvmStatic fun openURL(url: String) {
        val context = Cocos2dxHostContext.current ?: return
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}

