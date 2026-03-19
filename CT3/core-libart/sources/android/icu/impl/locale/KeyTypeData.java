package android.icu.impl.locale;

import android.icu.impl.ICUResourceBundle;
import android.icu.util.Output;
import android.icu.util.UResourceBundle;
import android.icu.util.UResourceBundleIterator;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Set;
import java.util.regex.Pattern;

public class KeyTypeData {

    static final boolean f59assertionsDisabled;
    private static final Map<String, KeyData> KEYMAP;
    private static final Object[][] KEY_DATA;

    private static abstract class SpecialTypeHandler {
        SpecialTypeHandler(SpecialTypeHandler specialTypeHandler) {
            this();
        }

        abstract boolean isValid(String str);

        private SpecialTypeHandler() {
        }

        String canonicalize(String value) {
            return AsciiUtil.toLowerString(value);
        }
    }

    private static class CodepointsTypeHandler extends SpecialTypeHandler {
        private static final Pattern pat = Pattern.compile("[0-9a-fA-F]{4,6}(-[0-9a-fA-F]{4,6})*");

        CodepointsTypeHandler(CodepointsTypeHandler codepointsTypeHandler) {
            this();
        }

        private CodepointsTypeHandler() {
            super(null);
        }

        @Override
        boolean isValid(String value) {
            return pat.matcher(value).matches();
        }
    }

    private static class ReorderCodeTypeHandler extends SpecialTypeHandler {
        private static final Pattern pat = Pattern.compile("[a-zA-Z]{3,8}(-[a-zA-Z]{3,8})*");

        ReorderCodeTypeHandler(ReorderCodeTypeHandler reorderCodeTypeHandler) {
            this();
        }

        private ReorderCodeTypeHandler() {
            super(null);
        }

        @Override
        boolean isValid(String value) {
            return pat.matcher(value).matches();
        }
    }

    private enum SpecialType {
        CODEPOINTS(new CodepointsTypeHandler(null)),
        REORDER_CODE(new ReorderCodeTypeHandler(0 == true ? 1 : 0));

        SpecialTypeHandler handler;

        public static SpecialType[] valuesCustom() {
            return values();
        }

        SpecialType(SpecialTypeHandler handler) {
            this.handler = handler;
        }
    }

    private static class KeyData {
        String bcpId;
        String legacyId;
        EnumSet<SpecialType> specialTypes;
        Map<String, Type> typeMap;

        KeyData(String legacyId, String bcpId, Map<String, Type> typeMap, EnumSet<SpecialType> specialTypes) {
            this.legacyId = legacyId;
            this.bcpId = bcpId;
            this.typeMap = typeMap;
            this.specialTypes = specialTypes;
        }
    }

    private static class Type {
        String bcpId;
        String legacyId;

        Type(String legacyId, String bcpId) {
            this.legacyId = legacyId;
            this.bcpId = bcpId;
        }
    }

    public static String toBcpKey(String key) {
        KeyData keyData = KEYMAP.get(AsciiUtil.toLowerString(key));
        if (keyData != null) {
            return keyData.bcpId;
        }
        return null;
    }

    public static String toLegacyKey(String key) {
        KeyData keyData = KEYMAP.get(AsciiUtil.toLowerString(key));
        if (keyData != null) {
            return keyData.legacyId;
        }
        return null;
    }

    public static String toBcpType(String key, String type, Output<Boolean> isKnownKey, Output<Boolean> isSpecialType) {
        if (isKnownKey != null) {
            isKnownKey.value = false;
        }
        if (isSpecialType != null) {
            isSpecialType.value = false;
        }
        String key2 = AsciiUtil.toLowerString(key);
        String type2 = AsciiUtil.toLowerString(type);
        KeyData keyData = KEYMAP.get(key2);
        if (keyData != null) {
            if (isKnownKey != null) {
                isKnownKey.value = Boolean.TRUE;
            }
            Type t = keyData.typeMap.get(type2);
            if (t != null) {
                return t.bcpId;
            }
            if (keyData.specialTypes != null) {
                for (SpecialType st : keyData.specialTypes) {
                    if (st.handler.isValid(type2)) {
                        if (isSpecialType != null) {
                            isSpecialType.value = true;
                        }
                        return st.handler.canonicalize(type2);
                    }
                }
            }
        }
        return null;
    }

    public static String toLegacyType(String key, String type, Output<Boolean> isKnownKey, Output<Boolean> isSpecialType) {
        if (isKnownKey != null) {
            isKnownKey.value = false;
        }
        if (isSpecialType != null) {
            isSpecialType.value = false;
        }
        String key2 = AsciiUtil.toLowerString(key);
        String type2 = AsciiUtil.toLowerString(type);
        KeyData keyData = KEYMAP.get(key2);
        if (keyData != null) {
            if (isKnownKey != null) {
                isKnownKey.value = Boolean.TRUE;
            }
            Type t = keyData.typeMap.get(type2);
            if (t != null) {
                return t.legacyId;
            }
            if (keyData.specialTypes != null) {
                for (SpecialType st : keyData.specialTypes) {
                    if (st.handler.isValid(type2)) {
                        if (isSpecialType != null) {
                            isSpecialType.value = true;
                        }
                        return st.handler.canonicalize(type2);
                    }
                }
            }
        }
        return null;
    }

    private static void initFromResourceBundle() {
        Set<String> bcpTypeAliasSet;
        Set<String> typeAliasSet;
        UResourceBundle keyTypeDataRes = UResourceBundle.getBundleInstance("android/icu/impl/data/icudt56b", "keyTypeData", ICUResourceBundle.ICU_DATA_CLASS_LOADER);
        UResourceBundle keyMapRes = keyTypeDataRes.get("keyMap");
        UResourceBundle typeMapRes = keyTypeDataRes.get("typeMap");
        UResourceBundle typeAliasRes = null;
        UResourceBundle bcpTypeAliasRes = null;
        try {
            typeAliasRes = keyTypeDataRes.get("typeAlias");
        } catch (MissingResourceException e) {
        }
        try {
            bcpTypeAliasRes = keyTypeDataRes.get("bcpTypeAlias");
        } catch (MissingResourceException e2) {
        }
        UResourceBundleIterator keyMapItr = keyMapRes.getIterator();
        while (keyMapItr.hasNext()) {
            UResourceBundle keyMapEntry = keyMapItr.next();
            String legacyKeyId = keyMapEntry.getKey();
            String bcpKeyId = keyMapEntry.getString();
            boolean hasSameKey = false;
            if (bcpKeyId.length() == 0) {
                bcpKeyId = legacyKeyId;
                hasSameKey = true;
            }
            boolean isTZ = legacyKeyId.equals("timezone");
            Map<String, Set<String>> typeAliasMap = null;
            if (typeAliasRes != null) {
                UResourceBundle typeAliasResByKey = null;
                try {
                    typeAliasResByKey = typeAliasRes.get(legacyKeyId);
                } catch (MissingResourceException e3) {
                }
                if (typeAliasResByKey != null) {
                    typeAliasMap = new HashMap<>();
                    UResourceBundleIterator typeAliasResItr = typeAliasResByKey.getIterator();
                    while (typeAliasResItr.hasNext()) {
                        UResourceBundle typeAliasDataEntry = typeAliasResItr.next();
                        String from = typeAliasDataEntry.getKey();
                        String to = typeAliasDataEntry.getString();
                        if (isTZ) {
                            from = from.replace(':', '/');
                        }
                        Set<String> aliasSet = typeAliasMap.get(to);
                        if (aliasSet == null) {
                            aliasSet = new HashSet<>();
                            typeAliasMap.put(to, aliasSet);
                        }
                        aliasSet.add(from);
                    }
                }
            }
            Map<String, Set<String>> bcpTypeAliasMap = null;
            if (bcpTypeAliasRes != null) {
                UResourceBundle bcpTypeAliasResByKey = null;
                try {
                    bcpTypeAliasResByKey = bcpTypeAliasRes.get(bcpKeyId);
                } catch (MissingResourceException e4) {
                }
                if (bcpTypeAliasResByKey != null) {
                    bcpTypeAliasMap = new HashMap<>();
                    UResourceBundleIterator bcpTypeAliasResItr = bcpTypeAliasResByKey.getIterator();
                    while (bcpTypeAliasResItr.hasNext()) {
                        UResourceBundle bcpTypeAliasDataEntry = bcpTypeAliasResItr.next();
                        String from2 = bcpTypeAliasDataEntry.getKey();
                        String to2 = bcpTypeAliasDataEntry.getString();
                        Set<String> aliasSet2 = bcpTypeAliasMap.get(to2);
                        if (aliasSet2 == null) {
                            aliasSet2 = new HashSet<>();
                            bcpTypeAliasMap.put(to2, aliasSet2);
                        }
                        aliasSet2.add(from2);
                    }
                }
            }
            Map<String, Type> typeDataMap = new HashMap<>();
            Set<SpecialType> specialTypeSet = null;
            UResourceBundle typeMapResByKey = null;
            try {
                typeMapResByKey = typeMapRes.get(legacyKeyId);
            } catch (MissingResourceException e5) {
                if (!f59assertionsDisabled) {
                    throw new AssertionError();
                }
            }
            if (typeMapResByKey != null) {
                UResourceBundleIterator typeMapResByKeyItr = typeMapResByKey.getIterator();
                while (typeMapResByKeyItr.hasNext()) {
                    UResourceBundle typeMapEntry = typeMapResByKeyItr.next();
                    String legacyTypeId = typeMapEntry.getKey();
                    boolean isSpecialType = false;
                    SpecialType[] specialTypeArrValuesCustom = SpecialType.valuesCustom();
                    int i = 0;
                    int length = specialTypeArrValuesCustom.length;
                    while (true) {
                        if (i >= length) {
                            break;
                        }
                        SpecialType st = specialTypeArrValuesCustom[i];
                        if (!legacyTypeId.equals(st.toString())) {
                            i++;
                        } else {
                            isSpecialType = true;
                            if (specialTypeSet == null) {
                                specialTypeSet = new HashSet<>();
                            }
                            specialTypeSet.add(st);
                        }
                    }
                    if (!isSpecialType) {
                        if (isTZ) {
                            legacyTypeId = legacyTypeId.replace(':', '/');
                        }
                        String bcpTypeId = typeMapEntry.getString();
                        boolean hasSameType = false;
                        if (bcpTypeId.length() == 0) {
                            bcpTypeId = legacyTypeId;
                            hasSameType = true;
                        }
                        Type t = new Type(legacyTypeId, bcpTypeId);
                        typeDataMap.put(AsciiUtil.toLowerString(legacyTypeId), t);
                        if (!hasSameType) {
                            typeDataMap.put(AsciiUtil.toLowerString(bcpTypeId), t);
                        }
                        if (typeAliasMap != null && (typeAliasSet = typeAliasMap.get(legacyTypeId)) != null) {
                            for (String alias : typeAliasSet) {
                                typeDataMap.put(AsciiUtil.toLowerString(alias), t);
                            }
                        }
                        if (bcpTypeAliasMap != null && (bcpTypeAliasSet = bcpTypeAliasMap.get(bcpTypeId)) != null) {
                            for (String alias2 : bcpTypeAliasSet) {
                                typeDataMap.put(AsciiUtil.toLowerString(alias2), t);
                            }
                        }
                    }
                }
            }
            EnumSet<SpecialType> specialTypes = null;
            if (specialTypeSet != null) {
                specialTypes = EnumSet.copyOf((Collection) specialTypeSet);
            }
            KeyData keyData = new KeyData(legacyKeyId, bcpKeyId, typeDataMap, specialTypes);
            KEYMAP.put(AsciiUtil.toLowerString(legacyKeyId), keyData);
            if (!hasSameKey) {
                KEYMAP.put(AsciiUtil.toLowerString(bcpKeyId), keyData);
            }
        }
    }

    static {
        f59assertionsDisabled = !KeyTypeData.class.desiredAssertionStatus();
        KEY_DATA = new Object[0][];
        KEYMAP = new HashMap();
        initFromResourceBundle();
    }

    private static void initFromTables() {
        Object[][] objArr = KEY_DATA;
        int i = 0;
        int length = objArr.length;
        while (true) {
            int i2 = i;
            if (i2 >= length) {
                return;
            }
            Object[] keyDataEntry = objArr[i2];
            String legacyKeyId = (String) keyDataEntry[0];
            String bcpKeyId = (String) keyDataEntry[1];
            String[][] typeData = (String[][]) keyDataEntry[2];
            String[][] typeAliasData = (String[][]) keyDataEntry[3];
            String[][] bcpTypeAliasData = (String[][]) keyDataEntry[4];
            boolean hasSameKey = false;
            if (bcpKeyId == null) {
                bcpKeyId = legacyKeyId;
                hasSameKey = true;
            }
            Map<String, Set<String>> typeAliasMap = null;
            if (typeAliasData != null) {
                typeAliasMap = new HashMap<>();
                for (String[] typeAliasDataEntry : typeAliasData) {
                    String from = typeAliasDataEntry[0];
                    String to = typeAliasDataEntry[1];
                    Set<String> aliasSet = typeAliasMap.get(to);
                    if (aliasSet == null) {
                        aliasSet = new HashSet<>();
                        typeAliasMap.put(to, aliasSet);
                    }
                    aliasSet.add(from);
                }
            }
            Map<String, Set<String>> bcpTypeAliasMap = null;
            if (bcpTypeAliasData != null) {
                bcpTypeAliasMap = new HashMap<>();
                for (String[] bcpTypeAliasDataEntry : bcpTypeAliasData) {
                    String from2 = bcpTypeAliasDataEntry[0];
                    String to2 = bcpTypeAliasDataEntry[1];
                    Set<String> aliasSet2 = bcpTypeAliasMap.get(to2);
                    if (aliasSet2 == null) {
                        aliasSet2 = new HashSet<>();
                        bcpTypeAliasMap.put(to2, aliasSet2);
                    }
                    aliasSet2.add(from2);
                }
            }
            if (!f59assertionsDisabled) {
                if (!(typeData != null)) {
                    throw new AssertionError();
                }
            }
            Map<String, Type> typeDataMap = new HashMap<>();
            Set<SpecialType> specialTypeSet = null;
            int i3 = 0;
            int length2 = typeData.length;
            while (true) {
                int i4 = i3;
                if (i4 >= length2) {
                    break;
                }
                String[] typeDataEntry = typeData[i4];
                String legacyTypeId = typeDataEntry[0];
                String bcpTypeId = typeDataEntry[1];
                boolean isSpecialType = false;
                SpecialType[] specialTypeArrValuesCustom = SpecialType.valuesCustom();
                int i5 = 0;
                int length3 = specialTypeArrValuesCustom.length;
                while (true) {
                    if (i5 >= length3) {
                        break;
                    }
                    SpecialType st = specialTypeArrValuesCustom[i5];
                    if (!legacyTypeId.equals(st.toString())) {
                        i5++;
                    } else {
                        isSpecialType = true;
                        if (specialTypeSet == null) {
                            specialTypeSet = new HashSet<>();
                        }
                        specialTypeSet.add(st);
                    }
                }
                if (!isSpecialType) {
                    boolean hasSameType = false;
                    if (bcpTypeId == null) {
                        bcpTypeId = legacyTypeId;
                        hasSameType = true;
                    }
                    Type t = new Type(legacyTypeId, bcpTypeId);
                    typeDataMap.put(AsciiUtil.toLowerString(legacyTypeId), t);
                    if (!hasSameType) {
                        typeDataMap.put(AsciiUtil.toLowerString(bcpTypeId), t);
                    }
                    Set<String> typeAliasSet = typeAliasMap.get(legacyTypeId);
                    if (typeAliasSet != null) {
                        for (String alias : typeAliasSet) {
                            typeDataMap.put(AsciiUtil.toLowerString(alias), t);
                        }
                    }
                    Set<String> bcpTypeAliasSet = bcpTypeAliasMap.get(bcpTypeId);
                    if (bcpTypeAliasSet != null) {
                        for (String alias2 : bcpTypeAliasSet) {
                            typeDataMap.put(AsciiUtil.toLowerString(alias2), t);
                        }
                    }
                }
                i3 = i4 + 1;
            }
            EnumSet<SpecialType> specialTypes = null;
            if (specialTypeSet != null) {
                specialTypes = EnumSet.copyOf((Collection) specialTypeSet);
            }
            KeyData keyData = new KeyData(legacyKeyId, bcpKeyId, typeDataMap, specialTypes);
            KEYMAP.put(AsciiUtil.toLowerString(legacyKeyId), keyData);
            if (!hasSameKey) {
                KEYMAP.put(AsciiUtil.toLowerString(bcpKeyId), keyData);
            }
            i = i2 + 1;
        }
    }
}
