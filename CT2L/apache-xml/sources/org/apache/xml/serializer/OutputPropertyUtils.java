package org.apache.xml.serializer;

import java.util.Properties;

public final class OutputPropertyUtils {
    public static boolean getBooleanProperty(String key, Properties props) {
        String s = props.getProperty(key);
        return s != null && s.equals("yes");
    }

    public static int getIntProperty(String key, Properties props) {
        String s = props.getProperty(key);
        if (s == null) {
            return 0;
        }
        return Integer.parseInt(s);
    }
}
