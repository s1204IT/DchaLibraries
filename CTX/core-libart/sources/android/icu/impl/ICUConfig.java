package android.icu.impl;

import java.io.IOException;
import java.io.InputStream;
import java.security.AccessControlException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.MissingResourceException;
import java.util.Properties;

public class ICUConfig {
    private static final Properties CONFIG_PROPS = new Properties();
    public static final String CONFIG_PROPS_FILE = "/android/icu/ICUConfig.properties";

    static {
        try {
            InputStream stream = ICUData.getStream(CONFIG_PROPS_FILE);
            if (stream != null) {
                try {
                    CONFIG_PROPS.load(stream);
                    stream.close();
                } catch (Throwable th) {
                    stream.close();
                    throw th;
                }
            }
        } catch (IOException e) {
        } catch (MissingResourceException e2) {
        }
    }

    public static String get(String str) {
        return get(str, null);
    }

    public static String get(final String str, String str2) {
        String property;
        if (System.getSecurityManager() != null) {
            try {
                property = (String) AccessController.doPrivileged(new PrivilegedAction<String>() {
                    @Override
                    public String run() {
                        return System.getProperty(str);
                    }
                });
            } catch (AccessControlException e) {
                property = null;
            }
        } else {
            property = System.getProperty(str);
        }
        if (property == null) {
            return CONFIG_PROPS.getProperty(str, str2);
        }
        return property;
    }
}
