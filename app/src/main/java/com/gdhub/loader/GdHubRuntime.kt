package com.gdhub.loader

object GdHubRuntime {
    init { System.loadLibrary("gdhub_loader") }
    @JvmStatic external fun nativeRunPackage(absolutePath: String): String
    @JvmStatic external fun nativeOnGameReady(): String
}
