package com.gdhub.loader

object GdHubRuntime {
    @JvmStatic external fun nativeRunPackage(absolutePath: String): String
}
