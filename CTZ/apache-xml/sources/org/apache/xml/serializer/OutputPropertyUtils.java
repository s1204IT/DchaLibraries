package org.apache.xml.serializer;

import java.util.Properties;

public final class OutputPropertyUtils {
    public static boolean getBooleanProperty(String str, Properties properties) {
        String property = properties.getProperty(str);
        if (property == null || !property.equals("yes")) {
            return false;
        }
        return true;
    }

    public static int getIntProperty(String str, Properties properties) {
        String property = properties.getProperty(str);
        if (property == null) {
            return 0;
        }
        return Integer.parseInt(property);
    }
}
