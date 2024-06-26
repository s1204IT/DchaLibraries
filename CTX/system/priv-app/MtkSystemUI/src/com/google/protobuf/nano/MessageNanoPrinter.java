package com.google.protobuf.nano;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
/* loaded from: classes.dex */
public final class MessageNanoPrinter {
    public static <T extends MessageNano> String print(T t) {
        if (t == null) {
            return "";
        }
        StringBuffer stringBuffer = new StringBuffer();
        try {
            print(null, t, new StringBuffer(), stringBuffer);
            return stringBuffer.toString();
        } catch (IllegalAccessException e) {
            return "Error printing proto: " + e.getMessage();
        } catch (InvocationTargetException e2) {
            return "Error printing proto: " + e2.getMessage();
        }
    }

    private static void print(String str, Object obj, StringBuffer stringBuffer, StringBuffer stringBuffer2) throws IllegalAccessException, InvocationTargetException {
        Field[] fields;
        int length;
        if (obj != null) {
            if (!(obj instanceof MessageNano)) {
                if (obj instanceof Map) {
                    String deCamelCaseify = deCamelCaseify(str);
                    for (Map.Entry entry : ((Map) obj).entrySet()) {
                        stringBuffer2.append(stringBuffer);
                        stringBuffer2.append(deCamelCaseify);
                        stringBuffer2.append(" <\n");
                        int length2 = stringBuffer.length();
                        stringBuffer.append("  ");
                        print("key", entry.getKey(), stringBuffer, stringBuffer2);
                        print("value", entry.getValue(), stringBuffer, stringBuffer2);
                        stringBuffer.setLength(length2);
                        stringBuffer2.append(stringBuffer);
                        stringBuffer2.append(">\n");
                    }
                    return;
                }
                String deCamelCaseify2 = deCamelCaseify(str);
                stringBuffer2.append(stringBuffer);
                stringBuffer2.append(deCamelCaseify2);
                stringBuffer2.append(": ");
                if (obj instanceof String) {
                    String sanitizeString = sanitizeString((String) obj);
                    stringBuffer2.append("\"");
                    stringBuffer2.append(sanitizeString);
                    stringBuffer2.append("\"");
                } else if (obj instanceof byte[]) {
                    appendQuotedBytes((byte[]) obj, stringBuffer2);
                } else {
                    stringBuffer2.append(obj);
                }
                stringBuffer2.append("\n");
                return;
            }
            int length3 = stringBuffer.length();
            if (str != null) {
                stringBuffer2.append(stringBuffer);
                stringBuffer2.append(deCamelCaseify(str));
                stringBuffer2.append(" <\n");
                stringBuffer.append("  ");
            }
            Class<?> cls = obj.getClass();
            for (Field field : cls.getFields()) {
                int modifiers = field.getModifiers();
                String name = field.getName();
                if (!"cachedSize".equals(name) && (modifiers & 1) == 1 && (modifiers & 8) != 8 && !name.startsWith("_") && !name.endsWith("_")) {
                    Class<?> type = field.getType();
                    Object obj2 = field.get(obj);
                    if (type.isArray()) {
                        if (type.getComponentType() == Byte.TYPE) {
                            print(name, obj2, stringBuffer, stringBuffer2);
                        } else {
                            if (obj2 != null) {
                                length = Array.getLength(obj2);
                            } else {
                                length = 0;
                            }
                            for (int i = 0; i < length; i++) {
                                print(name, Array.get(obj2, i), stringBuffer, stringBuffer2);
                            }
                        }
                    } else {
                        print(name, obj2, stringBuffer, stringBuffer2);
                    }
                }
            }
            for (Method method : cls.getMethods()) {
                String name2 = method.getName();
                if (name2.startsWith("set")) {
                    String substring = name2.substring(3);
                    try {
                        if (((Boolean) cls.getMethod("has" + substring, new Class[0]).invoke(obj, new Object[0])).booleanValue()) {
                            try {
                                print(substring, cls.getMethod("get" + substring, new Class[0]).invoke(obj, new Object[0]), stringBuffer, stringBuffer2);
                            } catch (NoSuchMethodException e) {
                            }
                        }
                    } catch (NoSuchMethodException e2) {
                    }
                }
            }
            if (str != null) {
                stringBuffer.setLength(length3);
                stringBuffer2.append(stringBuffer);
                stringBuffer2.append(">\n");
            }
        }
    }

    private static String deCamelCaseify(String str) {
        StringBuffer stringBuffer = new StringBuffer();
        for (int i = 0; i < str.length(); i++) {
            char charAt = str.charAt(i);
            if (i == 0) {
                stringBuffer.append(Character.toLowerCase(charAt));
            } else if (Character.isUpperCase(charAt)) {
                stringBuffer.append('_');
                stringBuffer.append(Character.toLowerCase(charAt));
            } else {
                stringBuffer.append(charAt);
            }
        }
        return stringBuffer.toString();
    }

    private static String sanitizeString(String str) {
        if (!str.startsWith("http") && str.length() > 200) {
            str = str.substring(0, 200) + "[...]";
        }
        return escapeString(str);
    }

    private static String escapeString(String str) {
        int length = str.length();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            char charAt = str.charAt(i);
            if (charAt >= ' ' && charAt <= '~' && charAt != '\"' && charAt != '\'') {
                sb.append(charAt);
            } else {
                sb.append(String.format("\\u%04x", Integer.valueOf(charAt)));
            }
        }
        return sb.toString();
    }

    private static void appendQuotedBytes(byte[] bArr, StringBuffer stringBuffer) {
        if (bArr == null) {
            stringBuffer.append("\"\"");
            return;
        }
        stringBuffer.append('\"');
        for (byte b : bArr) {
            int i = b & 255;
            if (i == 92 || i == 34) {
                stringBuffer.append('\\');
                stringBuffer.append((char) i);
            } else if (i >= 32 && i < 127) {
                stringBuffer.append((char) i);
            } else {
                stringBuffer.append(String.format("\\%03o", Integer.valueOf(i)));
            }
        }
        stringBuffer.append('\"');
    }
}
