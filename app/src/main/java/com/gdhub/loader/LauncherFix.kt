package com.gdhub.loader

object LauncherFix {
    init { System.loadLibrary("gdhub_launcherfix") }

    @JvmStatic external fun setDataPath(path: String)
    @JvmStatic external fun setOriginalDataPath(path: String)
    @JvmStatic external fun enableExceptionsRenaming()
    @JvmStatic external fun performPatches()
}
