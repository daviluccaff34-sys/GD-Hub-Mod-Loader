package com.gdhub.loader;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.os.Build;
import java.io.File;

/** Read-only inspection of the installed Geometry Dash package. */
public final class GameInstallProbe {
    public static final String GAME_PACKAGE = "com.robtopx.geometryjump";
    private GameInstallProbe() {}

    public static final class Result {
        public final boolean installed;
        public final String versionName;
        public final long versionCode;
        public final String sourceApk;
        public final String nativeLibraryDir;
        public final String[] splitApks;
        public final String[] supportedAbis;

        private Result(boolean installed, String versionName, long versionCode,
                       String sourceApk, String nativeLibraryDir,
                       String[] splitApks, String[] supportedAbis) {
            this.installed = installed;
            this.versionName = versionName;
            this.versionCode = versionCode;
            this.sourceApk = sourceApk;
            this.nativeLibraryDir = nativeLibraryDir;
            this.splitApks = splitApks;
            this.supportedAbis = supportedAbis;
        }
    }

    public static Result inspect(Context context) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(GAME_PACKAGE, 0);
            ApplicationInfo app = info.applicationInfo;
            long code = Build.VERSION.SDK_INT >= 28 ? info.getLongVersionCode() : info.versionCode;
            return new Result(true, info.versionName == null ? "desconhecida" : info.versionName,
                code, app.sourceDir, app.nativeLibraryDir,
                app.splitSourceDirs == null ? new String[0] : app.splitSourceDirs.clone(),
                Build.SUPPORTED_ABIS.clone());
        } catch (Exception ignored) {
            return new Result(false, "", 0L, "", "", new String[0], Build.SUPPORTED_ABIS.clone());
        }
    }

    public static boolean hasNativeLibraryDirectory(Result result) {
        return result != null && result.installed && result.nativeLibraryDir != null
            && new File(result.nativeLibraryDir).isDirectory();
    }
}
