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
        isJDK14OrHigher = javaVersion.compareTo(VersionInfo.getInstance("1.4.0")) >= 0;
    }

    public static VersionInfo getInstanceLenient(String str) {
        int[] iArr = new int[4];
        int i = 0;
        boolean z = false;
        int i2 = 0;
        while (true) {
            if (i >= str.length()) {
                break;
            }
            int i3 = i + 1;
            char cCharAt = str.charAt(i);
            if (cCharAt < '0' || cCharAt > '9') {
                if (!z) {
                    continue;
                } else {
                    if (i2 == 3) {
                        break;
                    }
                    i2++;
                    z = false;
                }
                i = i3;
            } else {
                if (z) {
                    iArr[i2] = (iArr[i2] * 10) + (cCharAt - '0');
                    if (iArr[i2] > 255) {
                        iArr[i2] = 0;
                        break;
                    }
                } else {
                    iArr[i2] = cCharAt - '0';
                    z = true;
                }
                i = i3;
            }
        }
        return VersionInfo.getInstance(iArr[0], iArr[1], iArr[2], iArr[3]);
    }

    public static boolean enabled() {
        return debug;
    }

    public static boolean enabled(String str) {
        if (!debug) {
            return false;
        }
        boolean z = params.indexOf(str) != -1;
        if (help) {
            System.out.println("\nICUDebug.enabled(" + str + ") = " + z);
        }
        return z;
    }

    public static String value(String str) {
        String strSubstring = "false";
        if (debug) {
            int iIndexOf = params.indexOf(str);
            if (iIndexOf != -1) {
                int length = iIndexOf + str.length();
                if (params.length() > length && params.charAt(length) == '=') {
                    int i = length + 1;
                    int iIndexOf2 = params.indexOf(",", i);
                    String str2 = params;
                    if (iIndexOf2 == -1) {
                        iIndexOf2 = params.length();
                    }
                    strSubstring = str2.substring(i, iIndexOf2);
                } else {
                    strSubstring = "true";
                }
            }
            if (help) {
                System.out.println("\nICUDebug.value(" + str + ") = " + strSubstring);
            }
        }
        return strSubstring;
    }
}
