package android.filterfw.core;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

public class KeyValueMap extends HashMap<String, Object> {
    public void setKeyValues(Object... objArr) {
        if (objArr.length % 2 != 0) {
            throw new RuntimeException("Key-Value arguments passed into setKeyValues must be an alternating list of keys and values!");
        }
        for (int i = 0; i < objArr.length; i += 2) {
            if (!(objArr[i] instanceof String)) {
                throw new RuntimeException("Key-value argument " + i + " must be a key of type String, but found an object of type " + objArr[i].getClass() + "!");
            }
            put((String) objArr[i], objArr[i + 1]);
        }
    }

    public static KeyValueMap fromKeyValues(Object... objArr) {
        KeyValueMap keyValueMap = new KeyValueMap();
        keyValueMap.setKeyValues(objArr);
        return keyValueMap;
    }

    public String getString(String str) {
        Object obj = get(str);
        if (obj != null) {
            return (String) obj;
        }
        return null;
    }

    public int getInt(String str) {
        Object obj = get(str);
        return (obj != null ? (Integer) obj : null).intValue();
    }

    public float getFloat(String str) {
        Object obj = get(str);
        return (obj != null ? (Float) obj : null).floatValue();
    }

    @Override
    public String toString() {
        StringWriter stringWriter = new StringWriter();
        for (Map.Entry<String, Object> entry : entrySet()) {
            Object value = entry.getValue();
            stringWriter.write(entry.getKey() + " = " + (value instanceof String ? "\"" + value + "\"" : value.toString()) + ";\n");
        }
        return stringWriter.toString();
    }
}
