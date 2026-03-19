package java.util.concurrent;

import java.util.Collection;

class Helpers {
    private Helpers() {
    }

    static String collectionToString(Collection<?> collection) {
        Object[] array = collection.toArray();
        int length = array.length;
        if (length == 0) {
            return "[]";
        }
        int length2 = 0;
        for (int i = 0; i < length; i++) {
            Object obj = array[i];
            String strObjectToString = obj == collection ? "(this Collection)" : objectToString(obj);
            array[i] = strObjectToString;
            length2 += strObjectToString.length();
        }
        return toString(array, length, length2);
    }

    static String toString(Object[] objArr, int i, int i2) {
        char[] cArr = new char[i2 + (2 * i)];
        cArr[0] = '[';
        int i3 = 1;
        for (int i4 = 0; i4 < i; i4++) {
            if (i4 > 0) {
                int i5 = i3 + 1;
                cArr[i3] = ',';
                i3 = i5 + 1;
                cArr[i5] = ' ';
            }
            String str = (String) objArr[i4];
            int length = str.length();
            str.getChars(0, length, cArr, i3);
            i3 += length;
        }
        cArr[i3] = ']';
        return new String(cArr);
    }

    static String mapEntryToString(Object obj, Object obj2) {
        String strObjectToString = objectToString(obj);
        int length = strObjectToString.length();
        String strObjectToString2 = objectToString(obj2);
        int length2 = strObjectToString2.length();
        char[] cArr = new char[length + length2 + 1];
        strObjectToString.getChars(0, length, cArr, 0);
        cArr[length] = '=';
        strObjectToString2.getChars(0, length2, cArr, length + 1);
        return new String(cArr);
    }

    private static String objectToString(Object obj) {
        String string;
        return (obj == null || (string = obj.toString()) == null) ? "null" : string;
    }
}
