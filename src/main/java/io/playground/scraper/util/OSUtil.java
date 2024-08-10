package io.playground.scraper.util;

public class OSUtil {
    
    private static final String osProperty = System.getProperty("os.name").toLowerCase();
    private static final String archProperty = System.getProperty("os.arch").toLowerCase();
    
    private OSUtil() {}
    
    public static boolean isWindows() {
        return osProperty.contains("win");
    }

    public static boolean isLinux() {
        return osProperty.contains("nix") || osProperty.contains("nux") || osProperty.contains("aix");
    }

    public static boolean isIntelMacOS() {
        return osProperty.contains("mac") && !archProperty.contains("aarch");
    }

    public static boolean isArmMacOS() {
        return osProperty.contains("mac") && archProperty.contains("aarch");
    }

    public static boolean isSolaris() {
        return osProperty.contains("sunos");
    }
    
    public static String getOSNameForChromeDriver() {
        if (isWindows()) {
            return "win32";
        } else if (isLinux()) {
            return "linux64";
        } else if (isArmMacOS()) {
            return "mac-arm64";
        } else if (isIntelMacOS()) {
            return "mac-x64";
        } else {
            return "win64";
        }
    }
}
