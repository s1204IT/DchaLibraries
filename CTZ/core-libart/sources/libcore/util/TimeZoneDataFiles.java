package libcore.util;

public final class TimeZoneDataFiles {
    private static final String ANDROID_DATA_ENV = "ANDROID_DATA";
    private static final String ANDROID_ROOT_ENV = "ANDROID_ROOT";

    private TimeZoneDataFiles() {
    }

    public static String[] getTimeZoneFilePaths(String str) {
        return new String[]{getDataTimeZoneFile(str), getSystemTimeZoneFile(str)};
    }

    private static String getDataTimeZoneFile(String str) {
        return System.getenv(ANDROID_DATA_ENV) + "/misc/zoneinfo/current/" + str;
    }

    public static String getSystemTimeZoneFile(String str) {
        return System.getenv(ANDROID_ROOT_ENV) + "/usr/share/zoneinfo/" + str;
    }

    public static String generateIcuDataPath() {
        StringBuilder sb = new StringBuilder();
        String environmentPath = getEnvironmentPath(ANDROID_DATA_ENV, "/misc/zoneinfo/current/icu");
        if (environmentPath != null) {
            sb.append(environmentPath);
        }
        String environmentPath2 = getEnvironmentPath(ANDROID_ROOT_ENV, "/usr/icu");
        if (environmentPath2 != null) {
            if (sb.length() > 0) {
                sb.append(":");
            }
            sb.append(environmentPath2);
        }
        return sb.toString();
    }

    private static String getEnvironmentPath(String str, String str2) {
        String str3 = System.getenv(str);
        if (str3 == null) {
            return null;
        }
        return str3 + str2;
    }
}
