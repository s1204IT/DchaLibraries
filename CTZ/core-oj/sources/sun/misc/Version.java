package sun.misc;

import java.io.PrintStream;
import sun.util.locale.LanguageTag;

public class Version {
    private static final String java_profile_name = "";
    private static final String java_runtime_name = "Android Runtime";
    private static final String java_runtime_version = "0.9";
    private static final String java_version = "0";
    private static boolean jvmVersionInfoAvailable = false;
    private static final String launcher_name = "";
    private static boolean versionsInitialized = false;
    private static int jvm_major_version = 0;
    private static int jvm_minor_version = 0;
    private static int jvm_micro_version = 0;
    private static int jvm_update_version = 0;
    private static int jvm_build_number = 0;
    private static String jvm_special_version = null;
    private static int jdk_major_version = 0;
    private static int jdk_minor_version = 0;
    private static int jdk_micro_version = 0;
    private static int jdk_update_version = 0;
    private static int jdk_build_number = 0;
    private static String jdk_special_version = null;

    public static native String getJdkSpecialVersion();

    private static native void getJdkVersionInfo();

    public static native String getJvmSpecialVersion();

    private static native boolean getJvmVersionInfo();

    public static void initSystemProperties() {
        System.setUnchangeableSystemProperty("java.version", java_version);
        System.setUnchangeableSystemProperty("java.runtime.version", java_runtime_version);
        System.setUnchangeableSystemProperty("java.runtime.name", java_runtime_name);
    }

    public static void print() {
        print(System.err);
    }

    public static void println() {
        print(System.err);
        System.err.println();
    }

    public static void print(PrintStream printStream) {
        boolean z;
        String property = System.getProperty("java.awt.headless");
        if (property != null && property.equalsIgnoreCase("true")) {
            z = true;
        } else {
            z = false;
        }
        printStream.println(" version \"0\"");
        printStream.print("Android Runtime (build 0.9");
        if ("".length() > 0) {
            printStream.print(", profile ");
        }
        if (java_runtime_name.indexOf("Embedded") != -1 && z) {
            printStream.print(", headless");
        }
        printStream.println(')');
        printStream.println(System.getProperty("java.vm.name") + " (build " + System.getProperty("java.vm.version") + ", " + System.getProperty("java.vm.info") + ")");
    }

    public static synchronized int jvmMajorVersion() {
        if (!versionsInitialized) {
            initVersions();
        }
        return jvm_major_version;
    }

    public static synchronized int jvmMinorVersion() {
        if (!versionsInitialized) {
            initVersions();
        }
        return jvm_minor_version;
    }

    public static synchronized int jvmMicroVersion() {
        if (!versionsInitialized) {
            initVersions();
        }
        return jvm_micro_version;
    }

    public static synchronized int jvmUpdateVersion() {
        if (!versionsInitialized) {
            initVersions();
        }
        return jvm_update_version;
    }

    public static synchronized String jvmSpecialVersion() {
        if (!versionsInitialized) {
            initVersions();
        }
        if (jvm_special_version == null) {
            jvm_special_version = getJvmSpecialVersion();
        }
        return jvm_special_version;
    }

    public static synchronized int jvmBuildNumber() {
        if (!versionsInitialized) {
            initVersions();
        }
        return jvm_build_number;
    }

    public static synchronized int jdkMajorVersion() {
        if (!versionsInitialized) {
            initVersions();
        }
        return jdk_major_version;
    }

    public static synchronized int jdkMinorVersion() {
        if (!versionsInitialized) {
            initVersions();
        }
        return jdk_minor_version;
    }

    public static synchronized int jdkMicroVersion() {
        if (!versionsInitialized) {
            initVersions();
        }
        return jdk_micro_version;
    }

    public static synchronized int jdkUpdateVersion() {
        if (!versionsInitialized) {
            initVersions();
        }
        return jdk_update_version;
    }

    public static synchronized String jdkSpecialVersion() {
        if (!versionsInitialized) {
            initVersions();
        }
        if (jdk_special_version == null) {
            jdk_special_version = getJdkSpecialVersion();
        }
        return jdk_special_version;
    }

    public static synchronized int jdkBuildNumber() {
        if (!versionsInitialized) {
            initVersions();
        }
        return jdk_build_number;
    }

    private static synchronized void initVersions() {
        char cCharAt;
        if (versionsInitialized) {
            return;
        }
        jvmVersionInfoAvailable = getJvmVersionInfo();
        if (!jvmVersionInfoAvailable) {
            String property = System.getProperty("java.vm.version");
            if (property.length() >= 5 && Character.isDigit(property.charAt(0)) && property.charAt(1) == '.' && Character.isDigit(property.charAt(2)) && property.charAt(3) == '.') {
                int i = 4;
                if (Character.isDigit(property.charAt(4))) {
                    jvm_major_version = Character.digit(property.charAt(0), 10);
                    jvm_minor_version = Character.digit(property.charAt(2), 10);
                    jvm_micro_version = Character.digit(property.charAt(4), 10);
                    CharSequence charSequenceSubSequence = property.subSequence(5, property.length());
                    if (charSequenceSubSequence.charAt(0) == '_' && charSequenceSubSequence.length() >= 3) {
                        if (!Character.isDigit(charSequenceSubSequence.charAt(1)) || !Character.isDigit(charSequenceSubSequence.charAt(2)) || !Character.isDigit(charSequenceSubSequence.charAt(3))) {
                            if (!Character.isDigit(charSequenceSubSequence.charAt(1)) || !Character.isDigit(charSequenceSubSequence.charAt(2))) {
                                i = 0;
                            } else {
                                i = 3;
                            }
                        }
                        try {
                            jvm_update_version = Integer.valueOf(charSequenceSubSequence.subSequence(1, i).toString()).intValue();
                            int i2 = i + 1;
                            if (charSequenceSubSequence.length() >= i2 && (cCharAt = charSequenceSubSequence.charAt(i)) >= 'a' && cCharAt <= 'z') {
                                jvm_special_version = Character.toString(cCharAt);
                                i = i2;
                            }
                            charSequenceSubSequence = charSequenceSubSequence.subSequence(i, charSequenceSubSequence.length());
                        } catch (NumberFormatException e) {
                            return;
                        }
                    }
                    if (charSequenceSubSequence.charAt(0) == '-') {
                        String[] strArrSplit = charSequenceSubSequence.subSequence(1, charSequenceSubSequence.length()).toString().split(LanguageTag.SEP);
                        int length = strArrSplit.length;
                        int i3 = 0;
                        while (true) {
                            if (i3 >= length) {
                                break;
                            }
                            String str = strArrSplit[i3];
                            if (str.charAt(0) != 'b' || str.length() != 3 || !Character.isDigit(str.charAt(1)) || !Character.isDigit(str.charAt(2))) {
                                i3++;
                            } else {
                                jvm_build_number = Integer.valueOf(str.substring(1, 3)).intValue();
                                break;
                            }
                        }
                    }
                }
            }
        }
        getJdkVersionInfo();
        versionsInitialized = true;
    }
}
