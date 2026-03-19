package java.net;

import dalvik.system.VMRuntime;

public class DebugUtils {
    protected static boolean isDebugLogOn() {
        String[] strArrProperties = VMRuntime.getRuntime().properties();
        for (int i = 0; i < strArrProperties.length; i++) {
            int iIndexOf = strArrProperties[i].indexOf(61);
            String strSubstring = strArrProperties[i].substring(0, iIndexOf);
            String strSubstring2 = strArrProperties[i].substring(iIndexOf + 1);
            if (strSubstring.equals("build-type") && ("eng".equals(strSubstring2) || "userdebug".equals(strSubstring2))) {
                return true;
            }
        }
        return false;
    }
}
