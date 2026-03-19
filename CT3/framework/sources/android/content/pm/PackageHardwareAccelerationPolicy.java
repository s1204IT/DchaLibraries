package android.content.pm;

import java.util.ArrayList;
import java.util.List;

public final class PackageHardwareAccelerationPolicy {
    private static List<String> sWhistlist = new ArrayList();

    public static List<String> getList() {
        return sWhistlist;
    }

    public static boolean match(String pkgName) {
        return sWhistlist.contains(pkgName);
    }
}
