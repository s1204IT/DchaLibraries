package android.icu.impl.locale;

import android.icu.impl.ICUData;
import android.icu.impl.ICUResourceBundle;
import android.icu.util.Output;
import android.icu.util.UResourceBundle;
import android.icu.util.UResourceBundleIterator;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Set;
import java.util.regex.Pattern;

public class KeyTypeData {
    static final boolean $assertionsDisabled = false;
    private static Map<String, Set<String>> BCP47_KEYS;
    static Set<String> DEPRECATED_KEYS = Collections.emptySet();
    static Map<String, ValueType> VALUE_TYPES = Collections.emptyMap();
    static Map<String, Set<String>> DEPRECATED_KEY_TYPES = Collections.emptyMap();
    private static final Object[][] KEY_DATA = new Object[0][];
    private static final Map<String, KeyData> KEYMAP = new HashMap();

    private enum KeyInfoType {
        deprecated,
        valueType
    }

    private enum TypeInfoType {
        deprecated
    }

    public enum ValueType {
        single,
        multiple,
        incremental,
        any
    }

    static {
        initFromResourceBundle();
    }

    private static abstract class SpecialTypeHandler {
        abstract boolean isWellFormed(String str);

        private SpecialTypeHandler() {
        }

        SpecialTypeHandler(AnonymousClass1 anonymousClass1) {
            this();
        }

        String canonicalize(String str) {
            return AsciiUtil.toLowerString(str);
        }
    }

    private static class CodepointsTypeHandler extends SpecialTypeHandler {
        private static final Pattern pat = Pattern.compile("[0-9a-fA-F]{4,6}(-[0-9a-fA-F]{4,6})*");

        private CodepointsTypeHandler() {
            super(null);
        }

        CodepointsTypeHandler(AnonymousClass1 anonymousClass1) {
            this();
        }

        @Override
        boolean isWellFormed(String str) {
            return pat.matcher(str).matches();
        }
    }

    private static class ReorderCodeTypeHandler extends SpecialTypeHandler {
        private static final Pattern pat = Pattern.compile("[a-zA-Z]{3,8}(-[a-zA-Z]{3,8})*");

        private ReorderCodeTypeHandler() {
            super(null);
        }

        ReorderCodeTypeHandler(AnonymousClass1 anonymousClass1) {
            this();
        }

        @Override
        boolean isWellFormed(String str) {
            return pat.matcher(str).matches();
        }
    }

    private static class RgKeyValueTypeHandler extends SpecialTypeHandler {
        private static final Pattern pat = Pattern.compile("([a-zA-Z]{2}|[0-9]{3})[zZ]{4}");

        private RgKeyValueTypeHandler() {
            super(null);
        }

        RgKeyValueTypeHandler(AnonymousClass1 anonymousClass1) {
            this();
        }

        @Override
        boolean isWellFormed(String str) {
            return pat.matcher(str).matches();
        }
    }

    private static class SubdivisionKeyValueTypeHandler extends SpecialTypeHandler {
        private static final Pattern pat = Pattern.compile("([a-zA-Z]{2}|[0-9]{3})");

        private SubdivisionKeyValueTypeHandler() {
            super(null);
        }

        SubdivisionKeyValueTypeHandler(AnonymousClass1 anonymousClass1) {
            this();
        }

        @Override
        boolean isWellFormed(String str) {
            return pat.matcher(str).matches();
        }
    }

    private static class PrivateUseKeyValueTypeHandler extends SpecialTypeHandler {
        private static final Pattern pat = Pattern.compile("[a-zA-Z0-9]{3,8}(-[a-zA-Z0-9]{3,8})*");

        private PrivateUseKeyValueTypeHandler() {
            super(null);
        }

        PrivateUseKeyValueTypeHandler(AnonymousClass1 anonymousClass1) {
            this();
        }

        @Override
        boolean isWellFormed(String str) {
            return pat.matcher(str).matches();
        }
    }

    private static final class SpecialType {
        private static final SpecialType[] $VALUES;
        public static final SpecialType CODEPOINTS;
        public static final SpecialType PRIVATE_USE;
        public static final SpecialType REORDER_CODE;
        public static final SpecialType RG_KEY_VALUE;
        public static final SpecialType SUBDIVISION_CODE;
        SpecialTypeHandler handler;

        public static SpecialType valueOf(String str) {
            return (SpecialType) Enum.valueOf(SpecialType.class, str);
        }

        public static SpecialType[] values() {
            return (SpecialType[]) $VALUES.clone();
        }

        static {
            AnonymousClass1 anonymousClass1 = null;
            CODEPOINTS = new SpecialType("CODEPOINTS", 0, new CodepointsTypeHandler(anonymousClass1));
            REORDER_CODE = new SpecialType("REORDER_CODE", 1, new ReorderCodeTypeHandler(anonymousClass1));
            RG_KEY_VALUE = new SpecialType("RG_KEY_VALUE", 2, new RgKeyValueTypeHandler(anonymousClass1));
            SUBDIVISION_CODE = new SpecialType("SUBDIVISION_CODE", 3, new SubdivisionKeyValueTypeHandler(anonymousClass1));
            PRIVATE_USE = new SpecialType("PRIVATE_USE", 4, new PrivateUseKeyValueTypeHandler(anonymousClass1));
            $VALUES = new SpecialType[]{CODEPOINTS, REORDER_CODE, RG_KEY_VALUE, SUBDIVISION_CODE, PRIVATE_USE};
        }

        private SpecialType(String str, int i, SpecialTypeHandler specialTypeHandler) {
            this.handler = specialTypeHandler;
        }
    }

    private static class KeyData {
        String bcpId;
        String legacyId;
        EnumSet<SpecialType> specialTypes;
        Map<String, Type> typeMap;

        KeyData(String str, String str2, Map<String, Type> map, EnumSet<SpecialType> enumSet) {
            this.legacyId = str;
            this.bcpId = str2;
            this.typeMap = map;
            this.specialTypes = enumSet;
        }
    }

    private static class Type {
        String bcpId;
        String legacyId;

        Type(String str, String str2) {
            this.legacyId = str;
            this.bcpId = str2;
        }
    }

    public static String toBcpKey(String str) {
        KeyData keyData = KEYMAP.get(AsciiUtil.toLowerString(str));
        if (keyData != null) {
            return keyData.bcpId;
        }
        return null;
    }

    public static String toLegacyKey(String str) {
        KeyData keyData = KEYMAP.get(AsciiUtil.toLowerString(str));
        if (keyData != null) {
            return keyData.legacyId;
        }
        return null;
    }

    public static String toBcpType(String str, String str2, Output<Boolean> output, Output<Boolean> output2) {
        if (output != null) {
            output.value = false;
        }
        if (output2 != null) {
            output2.value = false;
        }
        String lowerString = AsciiUtil.toLowerString(str);
        String lowerString2 = AsciiUtil.toLowerString(str2);
        KeyData keyData = KEYMAP.get(lowerString);
        if (keyData != null) {
            if (output != null) {
                output.value = Boolean.TRUE;
            }
            Type type = keyData.typeMap.get(lowerString2);
            if (type != null) {
                return type.bcpId;
            }
            if (keyData.specialTypes != null) {
                for (SpecialType specialType : keyData.specialTypes) {
                    if (specialType.handler.isWellFormed(lowerString2)) {
                        if (output2 != null) {
                            output2.value = true;
                        }
                        return specialType.handler.canonicalize(lowerString2);
                    }
                }
                return null;
            }
            return null;
        }
        return null;
    }

    public static String toLegacyType(String str, String str2, Output<Boolean> output, Output<Boolean> output2) {
        if (output != null) {
            output.value = false;
        }
        if (output2 != null) {
            output2.value = false;
        }
        String lowerString = AsciiUtil.toLowerString(str);
        String lowerString2 = AsciiUtil.toLowerString(str2);
        KeyData keyData = KEYMAP.get(lowerString);
        if (keyData != null) {
            if (output != null) {
                output.value = Boolean.TRUE;
            }
            Type type = keyData.typeMap.get(lowerString2);
            if (type != null) {
                return type.legacyId;
            }
            if (keyData.specialTypes != null) {
                for (SpecialType specialType : keyData.specialTypes) {
                    if (specialType.handler.isWellFormed(lowerString2)) {
                        if (output2 != null) {
                            output2.value = true;
                        }
                        return specialType.handler.canonicalize(lowerString2);
                    }
                }
                return null;
            }
            return null;
        }
        return null;
    }

    private static void initFromResourceBundle() {
        UResourceBundle uResourceBundle;
        UResourceBundle uResourceBundle2;
        String str;
        boolean z;
        UResourceBundle uResourceBundle3;
        HashMap map;
        Set hashSet;
        UResourceBundle uResourceBundle4;
        HashMap map2;
        Set hashSet2;
        UResourceBundle uResourceBundle5;
        UResourceBundle uResourceBundle6;
        UResourceBundleIterator uResourceBundleIterator;
        EnumSet enumSetNoneOf;
        boolean z2;
        Set set;
        Set set2;
        UResourceBundle bundleInstance = UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, "keyTypeData", ICUResourceBundle.ICU_DATA_CLASS_LOADER);
        getKeyInfo(bundleInstance.get("keyInfo"));
        getTypeInfo(bundleInstance.get("typeInfo"));
        UResourceBundle uResourceBundle7 = bundleInstance.get("keyMap");
        UResourceBundle uResourceBundle8 = bundleInstance.get("typeMap");
        try {
            uResourceBundle = bundleInstance.get("typeAlias");
        } catch (MissingResourceException e) {
            uResourceBundle = null;
        }
        try {
            uResourceBundle2 = bundleInstance.get("bcpTypeAlias");
        } catch (MissingResourceException e2) {
            uResourceBundle2 = null;
        }
        UResourceBundleIterator iterator = uResourceBundle7.getIterator();
        LinkedHashMap linkedHashMap = new LinkedHashMap();
        while (iterator.hasNext()) {
            UResourceBundle next = iterator.next();
            String key = next.getKey();
            String string = next.getString();
            if (string.length() == 0) {
                str = key;
                z = true;
            } else {
                str = string;
                z = false;
            }
            LinkedHashSet linkedHashSet = new LinkedHashSet();
            linkedHashMap.put(str, Collections.unmodifiableSet(linkedHashSet));
            boolean zEquals = key.equals("timezone");
            char c = '/';
            if (uResourceBundle != null) {
                try {
                    uResourceBundle3 = uResourceBundle.get(key);
                } catch (MissingResourceException e3) {
                    uResourceBundle3 = null;
                }
                if (uResourceBundle3 != null) {
                    map = new HashMap();
                    UResourceBundleIterator iterator2 = uResourceBundle3.getIterator();
                    while (iterator2.hasNext()) {
                        UResourceBundle next2 = iterator2.next();
                        String key2 = next2.getKey();
                        String string2 = next2.getString();
                        if (zEquals) {
                            key2 = key2.replace(':', c);
                        }
                        Set set3 = (Set) map.get(string2);
                        if (set3 == null) {
                            hashSet = new HashSet();
                            map.put(string2, hashSet);
                        } else {
                            hashSet = set3;
                        }
                        hashSet.add(key2);
                        c = '/';
                    }
                } else {
                    map = null;
                }
            }
            if (uResourceBundle2 != null) {
                try {
                    uResourceBundle4 = uResourceBundle2.get(str);
                } catch (MissingResourceException e4) {
                    uResourceBundle4 = null;
                }
                if (uResourceBundle4 != null) {
                    map2 = new HashMap();
                    UResourceBundleIterator iterator3 = uResourceBundle4.getIterator();
                    while (iterator3.hasNext()) {
                        UResourceBundle next3 = iterator3.next();
                        String key3 = next3.getKey();
                        String string3 = next3.getString();
                        Set set4 = (Set) map2.get(string3);
                        if (set4 == null) {
                            hashSet2 = new HashSet();
                            map2.put(string3, hashSet2);
                        } else {
                            hashSet2 = set4;
                        }
                        hashSet2.add(key3);
                    }
                } else {
                    map2 = null;
                }
            }
            HashMap map3 = new HashMap();
            try {
                uResourceBundle5 = uResourceBundle8.get(key);
            } catch (MissingResourceException e5) {
                uResourceBundle5 = null;
            }
            if (uResourceBundle5 != null) {
                UResourceBundleIterator iterator4 = uResourceBundle5.getIterator();
                enumSetNoneOf = null;
                while (iterator4.hasNext()) {
                    UResourceBundle next4 = iterator4.next();
                    UResourceBundleIterator uResourceBundleIterator2 = iterator4;
                    String key4 = next4.getKey();
                    String string4 = next4.getString();
                    UResourceBundle uResourceBundle9 = uResourceBundle2;
                    UResourceBundleIterator uResourceBundleIterator3 = iterator;
                    char cCharAt = key4.charAt(0);
                    if ('9' < cCharAt && cCharAt < 'a' && string4.length() == 0) {
                        if (enumSetNoneOf == null) {
                            enumSetNoneOf = EnumSet.noneOf(SpecialType.class);
                        }
                        enumSetNoneOf.add(SpecialType.valueOf(key4));
                        linkedHashSet.add(key4);
                    } else {
                        if (zEquals) {
                            key4 = key4.replace(':', '/');
                        }
                        if (string4.length() == 0) {
                            string4 = key4;
                            z2 = true;
                        } else {
                            z2 = false;
                        }
                        linkedHashSet.add(string4);
                        Type type = new Type(key4, string4);
                        map3.put(AsciiUtil.toLowerString(key4), type);
                        if (!z2) {
                            map3.put(AsciiUtil.toLowerString(string4), type);
                        }
                        if (map != null && (set2 = (Set) map.get(key4)) != null) {
                            Iterator it = set2.iterator();
                            while (it.hasNext()) {
                                map3.put(AsciiUtil.toLowerString((String) it.next()), type);
                            }
                        }
                        if (map2 != null && (set = (Set) map2.get(string4)) != null) {
                            Iterator it2 = set.iterator();
                            while (it2.hasNext()) {
                                map3.put(AsciiUtil.toLowerString((String) it2.next()), type);
                            }
                        }
                    }
                    iterator4 = uResourceBundleIterator2;
                    uResourceBundle2 = uResourceBundle9;
                    iterator = uResourceBundleIterator3;
                }
                uResourceBundle6 = uResourceBundle2;
                uResourceBundleIterator = iterator;
            } else {
                uResourceBundle6 = uResourceBundle2;
                uResourceBundleIterator = iterator;
                enumSetNoneOf = null;
            }
            KeyData keyData = new KeyData(key, str, map3, enumSetNoneOf);
            KEYMAP.put(AsciiUtil.toLowerString(key), keyData);
            if (!z) {
                KEYMAP.put(AsciiUtil.toLowerString(str), keyData);
            }
            uResourceBundle2 = uResourceBundle6;
            iterator = uResourceBundleIterator;
        }
        BCP47_KEYS = Collections.unmodifiableMap(linkedHashMap);
    }

    private static void getKeyInfo(UResourceBundle uResourceBundle) {
        LinkedHashSet linkedHashSet = new LinkedHashSet();
        LinkedHashMap linkedHashMap = new LinkedHashMap();
        UResourceBundleIterator iterator = uResourceBundle.getIterator();
        while (iterator.hasNext()) {
            UResourceBundle next = iterator.next();
            KeyInfoType keyInfoTypeValueOf = KeyInfoType.valueOf(next.getKey());
            UResourceBundleIterator iterator2 = next.getIterator();
            while (iterator2.hasNext()) {
                UResourceBundle next2 = iterator2.next();
                String key = next2.getKey();
                String string = next2.getString();
                switch (keyInfoTypeValueOf) {
                    case deprecated:
                        linkedHashSet.add(key);
                        break;
                    case valueType:
                        linkedHashMap.put(key, ValueType.valueOf(string));
                        break;
                }
            }
        }
        DEPRECATED_KEYS = Collections.unmodifiableSet(linkedHashSet);
        VALUE_TYPES = Collections.unmodifiableMap(linkedHashMap);
    }

    private static void getTypeInfo(UResourceBundle uResourceBundle) {
        LinkedHashMap linkedHashMap = new LinkedHashMap();
        UResourceBundleIterator iterator = uResourceBundle.getIterator();
        while (iterator.hasNext()) {
            UResourceBundle next = iterator.next();
            TypeInfoType typeInfoTypeValueOf = TypeInfoType.valueOf(next.getKey());
            UResourceBundleIterator iterator2 = next.getIterator();
            while (iterator2.hasNext()) {
                UResourceBundle next2 = iterator2.next();
                String key = next2.getKey();
                LinkedHashSet linkedHashSet = new LinkedHashSet();
                UResourceBundleIterator iterator3 = next2.getIterator();
                while (iterator3.hasNext()) {
                    String key2 = iterator3.next().getKey();
                    if (AnonymousClass1.$SwitchMap$android$icu$impl$locale$KeyTypeData$TypeInfoType[typeInfoTypeValueOf.ordinal()] == 1) {
                        linkedHashSet.add(key2);
                    }
                }
                linkedHashMap.put(key, Collections.unmodifiableSet(linkedHashSet));
            }
        }
        DEPRECATED_KEY_TYPES = Collections.unmodifiableMap(linkedHashMap);
    }

    static class AnonymousClass1 {
        static final int[] $SwitchMap$android$icu$impl$locale$KeyTypeData$TypeInfoType = new int[TypeInfoType.values().length];

        static {
            try {
                $SwitchMap$android$icu$impl$locale$KeyTypeData$TypeInfoType[TypeInfoType.deprecated.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            $SwitchMap$android$icu$impl$locale$KeyTypeData$KeyInfoType = new int[KeyInfoType.values().length];
            try {
                $SwitchMap$android$icu$impl$locale$KeyTypeData$KeyInfoType[KeyInfoType.deprecated.ordinal()] = 1;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$android$icu$impl$locale$KeyTypeData$KeyInfoType[KeyInfoType.valueType.ordinal()] = 2;
            } catch (NoSuchFieldError e3) {
            }
        }
    }

    private static void initFromTables() {
        String str;
        int i;
        HashMap map;
        HashMap map2;
        EnumSet enumSetCopyOf;
        boolean z;
        boolean z2;
        Set hashSet;
        Object[][] objArr = KEY_DATA;
        int length = objArr.length;
        int i2 = 0;
        int i3 = 0;
        while (i3 < length) {
            Object[] objArr2 = objArr[i3];
            String str2 = (String) objArr2[i2];
            char c = 1;
            String str3 = (String) objArr2[1];
            String[][] strArr = (String[][]) objArr2[2];
            String[][] strArr2 = (String[][]) objArr2[3];
            String[][] strArr3 = (String[][]) objArr2[4];
            if (str3 == null) {
                str = str2;
                i = 1;
            } else {
                str = str3;
                i = i2;
            }
            if (strArr2 != null) {
                map = new HashMap();
                int length2 = strArr2.length;
                int i4 = i2;
                while (i4 < length2) {
                    String[] strArr4 = strArr2[i4];
                    String str4 = strArr4[i2];
                    String str5 = strArr4[c];
                    Set set = (Set) map.get(str5);
                    if (set == null) {
                        hashSet = new HashSet();
                        map.put(str5, hashSet);
                    } else {
                        hashSet = set;
                    }
                    hashSet.add(str4);
                    i4++;
                    c = 1;
                }
            } else {
                map = null;
            }
            if (strArr3 != null) {
                map2 = new HashMap();
                int length3 = strArr3.length;
                for (int i5 = i2; i5 < length3; i5++) {
                    String[] strArr5 = strArr3[i5];
                    String str6 = strArr5[i2];
                    String str7 = strArr5[1];
                    Set hashSet2 = (Set) map2.get(str7);
                    if (hashSet2 == null) {
                        hashSet2 = new HashSet();
                        map2.put(str7, hashSet2);
                    }
                    hashSet2.add(str6);
                }
            } else {
                map2 = null;
            }
            HashMap map3 = new HashMap();
            int length4 = strArr.length;
            int i6 = i2;
            HashSet hashSet3 = null;
            while (i6 < length4) {
                String[] strArr6 = strArr[i6];
                String str8 = strArr6[i2];
                String str9 = strArr6[1];
                SpecialType[] specialTypeArrValues = SpecialType.values();
                Object[][] objArr3 = objArr;
                int length5 = specialTypeArrValues.length;
                int i7 = length;
                int i8 = 0;
                while (true) {
                    if (i8 < length5) {
                        int i9 = length5;
                        SpecialType specialType = specialTypeArrValues[i8];
                        SpecialType[] specialTypeArr = specialTypeArrValues;
                        if (!str8.equals(specialType.toString())) {
                            i8++;
                            length5 = i9;
                            specialTypeArrValues = specialTypeArr;
                        } else {
                            if (hashSet3 == null) {
                                hashSet3 = new HashSet();
                            }
                            hashSet3.add(specialType);
                            z = true;
                        }
                    } else {
                        z = false;
                        break;
                    }
                }
                if (!z) {
                    if (str9 == null) {
                        str9 = str8;
                        z2 = true;
                    } else {
                        z2 = false;
                    }
                    Type type = new Type(str8, str9);
                    map3.put(AsciiUtil.toLowerString(str8), type);
                    if (!z2) {
                        map3.put(AsciiUtil.toLowerString(str9), type);
                    }
                    Set set2 = (Set) map.get(str8);
                    if (set2 != null) {
                        Iterator it = set2.iterator();
                        while (it.hasNext()) {
                            map3.put(AsciiUtil.toLowerString((String) it.next()), type);
                        }
                    }
                    Set set3 = (Set) map2.get(str9);
                    if (set3 != null) {
                        Iterator it2 = set3.iterator();
                        while (it2.hasNext()) {
                            map3.put(AsciiUtil.toLowerString((String) it2.next()), type);
                        }
                    }
                }
                i6++;
                objArr = objArr3;
                length = i7;
                i2 = 0;
            }
            Object[][] objArr4 = objArr;
            int i10 = length;
            if (hashSet3 != null) {
                enumSetCopyOf = EnumSet.copyOf((Collection) hashSet3);
            } else {
                enumSetCopyOf = null;
            }
            KeyData keyData = new KeyData(str2, str, map3, enumSetCopyOf);
            KEYMAP.put(AsciiUtil.toLowerString(str2), keyData);
            if (i == 0) {
                KEYMAP.put(AsciiUtil.toLowerString(str), keyData);
            }
            i3++;
            objArr = objArr4;
            length = i10;
            i2 = 0;
        }
    }

    public static Set<String> getBcp47Keys() {
        return BCP47_KEYS.keySet();
    }

    public static Set<String> getBcp47KeyTypes(String str) {
        return BCP47_KEYS.get(str);
    }

    public static boolean isDeprecated(String str) {
        return DEPRECATED_KEYS.contains(str);
    }

    public static boolean isDeprecated(String str, String str2) {
        Set<String> set = DEPRECATED_KEY_TYPES.get(str);
        if (set == null) {
            return false;
        }
        return set.contains(str2);
    }

    public static ValueType getValueType(String str) {
        ValueType valueType = VALUE_TYPES.get(str);
        return valueType == null ? ValueType.single : valueType;
    }
}
