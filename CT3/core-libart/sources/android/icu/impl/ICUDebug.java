package android.icu.impl;

import android.icu.util.VersionInfo;

public final class ICUDebug {
    private static boolean debug;
    private static boolean help;
    public static final boolean isJDK14OrHigher;
    public static final VersionInfo javaVersion;
    public static final String javaVersionString;
    private static String params;

    static {
        try {
            params = System.getProperty("ICUDebug");
        } catch (SecurityException e) {
        }
        debug = params != null;
        help = debug && (params.equals("") || params.indexOf("help") != -1);
        if (debug) {
            System.out.println("\nICUDebug=" + params);
        }
        javaVersionString = System.getProperty("java.version", AndroidHardcodedSystemProperties.JAVA_VERSION);
        javaVersion = getInstanceLenient(javaVersionString);
        VersionInfo java14Version = VersionInfo.getInstance("1.4.0");
        isJDK14OrHigher = javaVersion.compareTo(java14Version) >= 0;
    }

    public static VersionInfo getInstanceLenient(String s) {
        int[] ver = new int[4];
        boolean numeric = false;
        int i = 0;
        int vidx = 0;
        while (true) {
            if (i >= s.length()) {
                break;
            }
            int i2 = i + 1;
            char c = s.charAt(i);
            if (c < '0' || c > '9') {
                if (!numeric) {
                    continue;
                } else {
                    if (vidx == 3) {
                        break;
                    }
                    numeric = false;
                    vidx++;
                }
                i = i2;
            } else {
                if (numeric) {
                    ver[vidx] = (ver[vidx] * 10) + (c - '0');
                    if (ver[vidx] > 255) {
                        ver[vidx] = 0;
                        break;
                    }
                } else {
                    numeric = true;
                    ver[vidx] = c - '0';
                }
                i = i2;
            }
        }
        return VersionInfo.getInstance(ver[0], ver[1], ver[2], ver[3]);
    }

    public static boolean enabled() {
        return debug;
    }

    public static boolean enabled(String arg) {
        if (debug) {
            boolean result = params.indexOf(arg) != -1;
            if (help) {
                System.out.println("\nICUDebug.enabled(" + arg + ") = " + result);
            }
            return result;
        }
        return false;
    }

    public static String value(String arg) {
        String result = "false";
        if (debug) {
            int index = params.indexOf(arg);
            if (index != -1) {
                int index2 = index + arg.length();
                if (params.length() > index2 && params.charAt(index2) == '=') {
                    int index3 = index2 + 1;
                    int limit = params.indexOf(",", index3);
                    String str = params;
                    if (limit == -1) {
                        limit = params.length();
                    }
                    result = str.substring(index3, limit);
                } else {
                    result = "true";
                }
            }
            if (help) {
                System.out.println("\nICUDebug.value(" + arg + ") = " + result);
            }
        }
        return result;
    }
}
