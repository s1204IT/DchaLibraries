package sun.security.util;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ResourceBundle;

public class ResourcesMgr {
    private static ResourceBundle altBundle;
    private static ResourceBundle bundle;

    public static String getString(String str) {
        if (bundle == null) {
            bundle = (ResourceBundle) AccessController.doPrivileged(new PrivilegedAction<ResourceBundle>() {
                @Override
                public ResourceBundle run() {
                    return ResourceBundle.getBundle(Resources.class.getName());
                }
            });
        }
        return bundle.getString(str);
    }

    public static String getString(String str, final String str2) {
        if (altBundle == null) {
            altBundle = (ResourceBundle) AccessController.doPrivileged(new PrivilegedAction<ResourceBundle>() {
                @Override
                public ResourceBundle run() {
                    return ResourceBundle.getBundle(str2);
                }
            });
        }
        return altBundle.getString(str);
    }
}
