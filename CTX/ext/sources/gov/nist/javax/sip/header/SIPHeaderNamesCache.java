package gov.nist.javax.sip.header;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;

public abstract class SIPHeaderNamesCache {
    private static final HashMap lowercaseMap = new HashMap();

    static {
        for (Field field : SIPHeaderNames.class.getFields()) {
            if (field.getType().equals(String.class) && Modifier.isStatic(field.getModifiers())) {
                try {
                    String str = (String) field.get(null);
                    String lowerCase = str.toLowerCase();
                    lowercaseMap.put(str, lowerCase);
                    lowercaseMap.put(lowerCase, lowerCase);
                } catch (IllegalAccessException e) {
                }
            }
        }
    }

    public static String toLowerCase(String str) {
        String str2 = (String) lowercaseMap.get(str);
        if (str2 == null) {
            return str.toLowerCase();
        }
        return str2;
    }
}
