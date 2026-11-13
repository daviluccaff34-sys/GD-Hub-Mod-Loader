package com.gdhub.loader;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.os.Build;
import java.io.File;
import java.util.zip.ZipFile;

/** Read-only inspection of the installed Geometry Dash package. */
public final class GameInstallProbe {
    public static final String GAME_PACKAGE = "com.robtopx.geometryjump";
    private GameInstallProbe() {}

    public static final class Result {
        public final boolean installed;
        public final String versionName;
        public final String unifiedVersion;
        public final long versionCode;
        public final String sourceApk;
        public final String nativeLibraryDir;
        public final String[] splitApks;
        public final String[] supportedAbis;

        private Result(boolean installed, String versionName, String unifiedVersion, long versionCode,
                       String sourceApk, String nativeLibraryDir,
                       String[] splitApks, String[] supportedAbis) {
            this.installed = installed;
            this.versionName = versionName;
            this.unifiedVersion = unifiedVersion;
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
            String displayVersion = info.versionName == null ? "desconhecida" : info.versionName;
            return new Result(true, displayVersion, unifiedVersionFor(code, displayVersion),
                code, app.sourceDir, app.nativeLibraryDir,
                app.splitSourceDirs == null ? new String[0] : app.splitSourceDirs.clone(),
                Build.SUPPORTED_ABIS.clone());
        } catch (Exception ignored) {
            return new Result(false, "", "", 0L, "", "", new String[0], Build.SUPPORTED_ABIS.clone());
        }
    }

    private static String unifiedVersionFor(long versionCode, String fallback) {
        switch ((int) versionCode) {
            case 37: return "2.200";
            case 38: return "2.205";
            case 39: return "2.206";
            case 40: return "2.2074";
            case 41: return "2.2081";
            case 42: return "2.209";
            case 43: return "2.2091";
            default: return fallback;
        }
    }

    /** Checks that both required game libraries exist for the host process ABI. */
    public static boolean hasGameLibraries(Result result, String abi) {
        if (result == null || !result.installed || abi == null) return false;
        String[] names = {"libfmod.so", "libcocos2dcpp.so"};
        String[] apkPaths = new String[1 + result.splitApks.length];
        apkPaths[0] = result.sourceApk;
        System.arraycopy(result.splitApks, 0, apkPaths, 1, result.splitApks.length);
        for (String name : names) {
            boolean found = result.nativeLibraryDir != null
                && new File(result.nativeLibraryDir, name).isFile();
            if (!found) {
                for (String apkPath : apkPaths) {
                    if (apkPath == null || apkPath.isEmpty()) continue;
                    try (ZipFile zip = new ZipFile(apkPath)) {
                        if (zip.getEntry("lib/" + abi + "/" + name) != null) {
                            found = true;
                            break;
                        }
                    } catch (Exception ignored) { }
                }
            }
            if (!found) return false;
        }
        return true;
    }

    public static boolean hasNativeLibraryDirectory(Result result) {
        return result != null && result.installed && result.nativeLibraryDir != null
            && new File(result.nativeLibraryDir).isDirectory();
    }
}
