package dalvik.system;

public final class DalvikLogging {
    private DalvikLogging() {
    }

    public static String loggerNameToTag(String str) {
        if (str == null) {
            return "null";
        }
        int length = str.length();
        if (length <= 23) {
            return str;
        }
        int iLastIndexOf = str.lastIndexOf(".") + 1;
        if (length - iLastIndexOf > 23) {
            return str.substring(str.length() - 23);
        }
        return str.substring(iLastIndexOf);
    }
}
