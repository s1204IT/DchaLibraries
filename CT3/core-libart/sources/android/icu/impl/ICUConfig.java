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
            InputStream is = ICUData.getStream(CONFIG_PROPS_FILE);
            if (is == null) {
                return;
            }
            try {
                CONFIG_PROPS.load(is);
            } finally {
                is.close();
            }
        } catch (IOException e) {
        } catch (MissingResourceException e2) {
        }
    }

    public static String get(String name) {
        return get(name, null);
    }

    public static String get(final String name, String def) {
        String val = null;
        if (System.getSecurityManager() != null) {
            try {
                val = (String) AccessController.doPrivileged(new PrivilegedAction<String>() {
                    @Override
                    public String run() {
                        return System.getProperty(name);
                    }
                });
            } catch (AccessControlException e) {
            }
        } else {
            val = System.getProperty(name);
        }
        if (val == null) {
            return CONFIG_PROPS.getProperty(name, def);
        }
        return val;
    }
}
