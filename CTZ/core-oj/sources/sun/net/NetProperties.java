package sun.net;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Properties;

public class NetProperties {
    private static Properties props = new Properties();

    static {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                NetProperties.loadDefaultProperties();
                return null;
            }
        });
    }

    private NetProperties() {
    }

    private static void loadDefaultProperties() {
        String property = System.getProperty("java.home");
        if (property == null) {
            throw new Error("Can't find java.home ??");
        }
        try {
            BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(new File(new File(property, "lib"), "net.properties").getCanonicalPath()));
            props.load(bufferedInputStream);
            bufferedInputStream.close();
        } catch (Exception e) {
        }
    }

    public static String get(String str) {
        try {
            return System.getProperty(str, props.getProperty(str));
        } catch (IllegalArgumentException | NullPointerException e) {
            return null;
        }
    }

    public static Integer getInteger(String str, int i) {
        String property;
        try {
            property = System.getProperty(str, props.getProperty(str));
        } catch (IllegalArgumentException | NullPointerException e) {
            property = null;
        }
        if (property != null) {
            try {
                return Integer.decode(property);
            } catch (NumberFormatException e2) {
            }
        }
        return new Integer(i);
    }

    public static Boolean getBoolean(String str) {
        String property;
        try {
            property = System.getProperty(str, props.getProperty(str));
        } catch (IllegalArgumentException | NullPointerException e) {
            property = null;
        }
        if (property != null) {
            try {
                return Boolean.valueOf(property);
            } catch (NumberFormatException e2) {
            }
        }
        return null;
    }
}
