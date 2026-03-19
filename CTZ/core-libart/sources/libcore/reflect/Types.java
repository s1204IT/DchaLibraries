package libcore.reflect;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.HashMap;
import java.util.Map;
import libcore.util.EmptyArray;

public final class Types {
    private static final Map<Class<?>, String> PRIMITIVE_TO_SIGNATURE = new HashMap(9);

    private Types() {
    }

    static {
        PRIMITIVE_TO_SIGNATURE.put(Byte.TYPE, "B");
        PRIMITIVE_TO_SIGNATURE.put(Character.TYPE, "C");
        PRIMITIVE_TO_SIGNATURE.put(Short.TYPE, "S");
        PRIMITIVE_TO_SIGNATURE.put(Integer.TYPE, "I");
        PRIMITIVE_TO_SIGNATURE.put(Long.TYPE, "J");
        PRIMITIVE_TO_SIGNATURE.put(Float.TYPE, "F");
        PRIMITIVE_TO_SIGNATURE.put(Double.TYPE, "D");
        PRIMITIVE_TO_SIGNATURE.put(Void.TYPE, "V");
        PRIMITIVE_TO_SIGNATURE.put(Boolean.TYPE, "Z");
    }

    public static Type[] getTypeArray(ListOfTypes listOfTypes, boolean z) {
        if (listOfTypes.length() == 0) {
            return EmptyArray.TYPE;
        }
        Type[] resolvedTypes = listOfTypes.getResolvedTypes();
        return z ? (Type[]) resolvedTypes.clone() : resolvedTypes;
    }

    public static Type getType(Type type) {
        if (type instanceof ParameterizedTypeImpl) {
            return ((ParameterizedTypeImpl) type).getResolvedType();
        }
        return type;
    }

    public static String getSignature(Class<?> cls) {
        String str = PRIMITIVE_TO_SIGNATURE.get(cls);
        if (str != null) {
            return str;
        }
        if (cls.isArray()) {
            return "[" + getSignature(cls.getComponentType());
        }
        return "L" + cls.getName() + ";";
    }

    public static String toString(Class<?>[] clsArr) {
        if (clsArr.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        appendTypeName(sb, clsArr[0]);
        for (int i = 1; i < clsArr.length; i++) {
            sb.append(',');
            appendTypeName(sb, clsArr[i]);
        }
        return sb.toString();
    }

    public static void appendTypeName(StringBuilder sb, Class<?> cls) {
        int i = 0;
        while (cls.isArray()) {
            cls = cls.getComponentType();
            i++;
        }
        sb.append(cls.getName());
        for (int i2 = 0; i2 < i; i2++) {
            sb.append("[]");
        }
    }

    public static void appendArrayGenericType(StringBuilder sb, Type[] typeArr) {
        if (typeArr.length == 0) {
            return;
        }
        appendGenericType(sb, typeArr[0]);
        for (int i = 1; i < typeArr.length; i++) {
            sb.append(',');
            appendGenericType(sb, typeArr[i]);
        }
    }

    public static void appendGenericType(StringBuilder sb, Type type) {
        if (type instanceof TypeVariable) {
            sb.append(((TypeVariable) type).getName());
            return;
        }
        if (type instanceof ParameterizedType) {
            sb.append(type.toString());
            return;
        }
        if (type instanceof GenericArrayType) {
            appendGenericType(sb, ((GenericArrayType) type).getGenericComponentType());
            sb.append("[]");
            return;
        }
        if (type instanceof Class) {
            Class cls = (Class) type;
            if (cls.isArray()) {
                String[] strArrSplit = cls.getName().split("\\[");
                int length = strArrSplit.length - 1;
                if (strArrSplit[length].length() > 1) {
                    sb.append(strArrSplit[length].substring(1, strArrSplit[length].length() - 1));
                } else {
                    char cCharAt = strArrSplit[length].charAt(0);
                    if (cCharAt == 'I') {
                        sb.append("int");
                    } else if (cCharAt == 'B') {
                        sb.append("byte");
                    } else if (cCharAt == 'J') {
                        sb.append("long");
                    } else if (cCharAt == 'F') {
                        sb.append("float");
                    } else if (cCharAt == 'D') {
                        sb.append("double");
                    } else if (cCharAt == 'S') {
                        sb.append("short");
                    } else if (cCharAt == 'C') {
                        sb.append("char");
                    } else if (cCharAt == 'Z') {
                        sb.append("boolean");
                    } else if (cCharAt == 'V') {
                        sb.append("void");
                    }
                }
                for (int i = 0; i < length; i++) {
                    sb.append("[]");
                }
                return;
            }
            sb.append(cls.getName());
        }
    }
}
