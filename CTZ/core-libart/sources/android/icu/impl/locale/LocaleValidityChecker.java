package android.icu.impl.locale;

import android.icu.impl.ValidIdentifiers;
import android.icu.impl.locale.KeyTypeData;
import android.icu.text.DateFormat;
import android.icu.util.IllformedLocaleException;
import android.icu.util.ULocale;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class LocaleValidityChecker {
    private final boolean allowsDeprecated;
    private final Set<ValidIdentifiers.Datasubtype> datasubtypes;
    static Pattern SEPARATOR = Pattern.compile("[-_]");
    private static final Pattern VALID_X = Pattern.compile("[a-zA-Z0-9]{2,8}(-[a-zA-Z0-9]{2,8})*");
    static final Set<String> REORDERING_INCLUDE = new HashSet(Arrays.asList("space", "punct", "symbol", "currency", "digit", "others", DateFormat.SPECIFIC_TZ));
    static final Set<String> REORDERING_EXCLUDE = new HashSet(Arrays.asList("zinh", "zyyy"));
    static final Set<ValidIdentifiers.Datasubtype> REGULAR_ONLY = EnumSet.of(ValidIdentifiers.Datasubtype.regular);

    public static class Where {
        public String codeFailure;
        public ValidIdentifiers.Datatype fieldFailure;

        public boolean set(ValidIdentifiers.Datatype datatype, String str) {
            this.fieldFailure = datatype;
            this.codeFailure = str;
            return false;
        }

        public String toString() {
            if (this.fieldFailure == null) {
                return "OK";
            }
            return "{" + this.fieldFailure + ", " + this.codeFailure + "}";
        }
    }

    public LocaleValidityChecker(Set<ValidIdentifiers.Datasubtype> set) {
        this.datasubtypes = EnumSet.copyOf((Collection) set);
        this.allowsDeprecated = set.contains(ValidIdentifiers.Datasubtype.deprecated);
    }

    public LocaleValidityChecker(ValidIdentifiers.Datasubtype... datasubtypeArr) {
        this.datasubtypes = EnumSet.copyOf((Collection) Arrays.asList(datasubtypeArr));
        this.allowsDeprecated = this.datasubtypes.contains(ValidIdentifiers.Datasubtype.deprecated);
    }

    public Set<ValidIdentifiers.Datasubtype> getDatasubtypes() {
        return EnumSet.copyOf((Collection) this.datasubtypes);
    }

    public boolean isValid(ULocale uLocale, Where where) {
        where.set(null, null);
        String language = uLocale.getLanguage();
        String script = uLocale.getScript();
        String country = uLocale.getCountry();
        String variant = uLocale.getVariant();
        Set<Character> extensionKeys = uLocale.getExtensionKeys();
        if (!isValid(ValidIdentifiers.Datatype.language, language, where)) {
            if (!language.equals(LanguageTag.PRIVATEUSE)) {
                return false;
            }
            where.set(null, null);
            return true;
        }
        if (!isValid(ValidIdentifiers.Datatype.script, script, where) || !isValid(ValidIdentifiers.Datatype.region, country, where)) {
            return false;
        }
        if (!variant.isEmpty()) {
            for (String str : SEPARATOR.split(variant)) {
                if (!isValid(ValidIdentifiers.Datatype.variant, str, where)) {
                    return false;
                }
            }
        }
        for (Character ch : extensionKeys) {
            try {
                ValidIdentifiers.Datatype datatypeValueOf = ValidIdentifiers.Datatype.valueOf(ch + "");
                switch (datatypeValueOf) {
                    case x:
                        return true;
                    case t:
                    case u:
                        if (!isValidU(uLocale, datatypeValueOf, uLocale.getExtension(ch.charValue()), where)) {
                            return false;
                        }
                        continue;
                        break;
                    default:
                        continue;
                }
            } catch (Exception e) {
                return where.set(ValidIdentifiers.Datatype.illegal, ch + "");
            }
            return where.set(ValidIdentifiers.Datatype.illegal, ch + "");
        }
        return true;
    }

    enum SpecialCase {
        normal,
        anything,
        reorder,
        codepoints,
        subdivision,
        rgKey;

        static SpecialCase get(String str) {
            if (str.equals("kr")) {
                return reorder;
            }
            if (str.equals("vt")) {
                return codepoints;
            }
            if (str.equals("sd")) {
                return subdivision;
            }
            if (str.equals("rg")) {
                return rgKey;
            }
            if (str.equals("x0")) {
                return anything;
            }
            return normal;
        }
    }

    private boolean isValidU(android.icu.util.ULocale r19, android.icu.impl.ValidIdentifiers.Datatype r20, java.lang.String r21, android.icu.impl.locale.LocaleValidityChecker.Where r22) {
        r4 = new java.lang.StringBuilder();
        r5 = new java.util.HashSet();
        if (r20 == android.icu.impl.ValidIdentifiers.Datatype.t) {
            r6 = new java.lang.StringBuilder();
        } else {
            r6 = null;
        }
        r8 = android.icu.impl.locale.LocaleValidityChecker.SEPARATOR.split(r21);
        r9 = r8.length;
        r13 = "";
        r3 = 0;
        r11 = 0;
        r12 = null;
        r14 = null;
        while (r3 < r9) {
            r7 = r8[r3];
            if (r7.length() != 2 || (r6 != null && r7.charAt(1) > '9')) {
                if (r6 != null) {
                    if (r6.length() != 0) {
                        r6.append('-');
                    }
                    r6.append(r7);
                    r17 = r4;
                } else {
                    r11 = r11 + 1;
                    switch (android.icu.impl.locale.LocaleValidityChecker.AnonymousClass1.$SwitchMap$android$icu$impl$locale$KeyTypeData$ValueType[r12.ordinal()]) {
                        case 1:
                            if (r11 > 1) {
                                r0 = new java.lang.StringBuilder();
                                r0.append(r13);
                                r0.append(android.icu.impl.locale.LanguageTag.SEP);
                                r0.append(r7);
                                return r22.set(r20, r0.toString());
                            }
                            switch (android.icu.impl.locale.LocaleValidityChecker.AnonymousClass1.$SwitchMap$android$icu$impl$locale$LocaleValidityChecker$SpecialCase[r14.ordinal()]) {
                                case 1:
                                    r17 = r4;
                                    continue;
                                    continue;
                                    continue;
                                    continue;
                                    r3 = r3 + 1;
                                    r4 = r17;
                                    break;
                                case 2:
                                    r17 = r4;
                                    try {
                                        if (java.lang.Integer.parseInt(r7, 16) > 1114111) {
                                            r0 = new java.lang.StringBuilder();
                                            r0.append(r13);
                                            r0.append(android.icu.impl.locale.LanguageTag.SEP);
                                            r0.append(r7);
                                            return r22.set(r20, r0.toString());
                                        } else {
                                            continue;
                                            continue;
                                            continue;
                                            continue;
                                            r3 = r3 + 1;
                                            r4 = r17;
                                            break;
                                        }
                                    } catch (java.lang.NumberFormatException e) {
                                        r0 = new java.lang.StringBuilder();
                                        r0.append(r13);
                                        r0.append(android.icu.impl.locale.LanguageTag.SEP);
                                        r0.append(r7);
                                        return r22.set(r20, r0.toString());
                                    }
                                    break;
                                case 3:
                                    r17 = r4;
                                    if (r7.equals(android.icu.text.DateFormat.SPECIFIC_TZ)) {
                                        r10 = "others";
                                    } else {
                                        r10 = r7;
                                    }
                                    if (!r5.add(r10) || !isScriptReorder(r7)) {
                                        r0 = new java.lang.StringBuilder();
                                        r0.append(r13);
                                        r0.append(android.icu.impl.locale.LanguageTag.SEP);
                                        r0.append(r7);
                                        return r22.set(r20, r0.toString());
                                    } else {
                                        continue;
                                        continue;
                                        continue;
                                        continue;
                                        r3 = r3 + 1;
                                        r4 = r17;
                                        break;
                                    }
                                    break;
                                case 4:
                                    r17 = r4;
                                    if (!isSubdivision(r19, r7)) {
                                        r0 = new java.lang.StringBuilder();
                                        r0.append(r13);
                                        r0.append(android.icu.impl.locale.LanguageTag.SEP);
                                        r0.append(r7);
                                        return r22.set(r20, r0.toString());
                                    } else {
                                        continue;
                                        continue;
                                        continue;
                                        continue;
                                        r3 = r3 + 1;
                                        r4 = r17;
                                        break;
                                    }
                                    break;
                                case 5:
                                    if (r7.length() < 6 || !r7.endsWith(android.icu.text.DateFormat.SPECIFIC_TZ)) {
                                        return r22.set(r20, r7);
                                    } else {
                                        r17 = r4;
                                        if (!isValid(android.icu.impl.ValidIdentifiers.Datatype.region, r7.substring(0, r7.length() + (-4)), r22)) {
                                            return false;
                                        }
                                    }
                                    break;
                                default:
                                    r17 = r4;
                                    if (android.icu.impl.locale.KeyTypeData.toBcpType(r13, r7, new android.icu.util.Output(), new android.icu.util.Output()) == null) {
                                        r0 = new java.lang.StringBuilder();
                                        r0.append(r13);
                                        r0.append(android.icu.impl.locale.LanguageTag.SEP);
                                        r0.append(r7);
                                        return r22.set(r20, r0.toString());
                                    } else {
                                        if (!r18.allowsDeprecated) {
                                            if (android.icu.impl.locale.KeyTypeData.isDeprecated(r13, r7)) {
                                                r0 = new java.lang.StringBuilder();
                                                r0.append(r13);
                                                r0.append(android.icu.impl.locale.LanguageTag.SEP);
                                                r0.append(r7);
                                                return r22.set(r20, r0.toString());
                                            }
                                        } else {
                                            continue;
                                            continue;
                                            continue;
                                            continue;
                                        }
                                        r3 = r3 + 1;
                                        r4 = r17;
                                        break;
                                    }
                                    break;
                            }
                            break;
                        case 2:
                            if (r11 == 1) {
                                r4.setLength(0);
                                r4.append(r7);
                            } else {
                                r4.append('-');
                                r4.append(r7);
                                r7 = r4.toString();
                            }
                            switch (android.icu.impl.locale.LocaleValidityChecker.AnonymousClass1.$SwitchMap$android$icu$impl$locale$LocaleValidityChecker$SpecialCase[r14.ordinal()]) {
                            }
                        case 3:
                            if (r11 == 1) {
                                r5.clear();
                            }
                            switch (android.icu.impl.locale.LocaleValidityChecker.AnonymousClass1.$SwitchMap$android$icu$impl$locale$LocaleValidityChecker$SpecialCase[r14.ordinal()]) {
                            }
                        default:
                            switch (android.icu.impl.locale.LocaleValidityChecker.AnonymousClass1.$SwitchMap$android$icu$impl$locale$LocaleValidityChecker$SpecialCase[r14.ordinal()]) {
                            }
                    }
                }
            } else {
                if (r6 != null) {
                    if (r6.length() == 0 || isValidLocale(r6.toString(), r22)) {
                        r6 = null;
                    } else {
                        return false;
                    }
                }
                r10 = android.icu.impl.locale.KeyTypeData.toBcpKey(r7);
                if (r10 == null) {
                    return r22.set(r20, r7);
                } else {
                    if (r18.allowsDeprecated || !android.icu.impl.locale.KeyTypeData.isDeprecated(r10)) {
                        r7 = android.icu.impl.locale.KeyTypeData.getValueType(r10);
                        r17 = r4;
                        r12 = r7;
                        r13 = r10;
                        r14 = android.icu.impl.locale.LocaleValidityChecker.SpecialCase.get(r10);
                        r11 = 0;
                    } else {
                        return r22.set(r20, r10);
                    }
                }
            }
            r3 = r3 + 1;
            r4 = r17;
        }
        if (r6 == null || r6.length() == 0 || isValidLocale(r6.toString(), r22)) {
            return true;
        } else {
            return false;
        }
    }

    static class AnonymousClass1 {
        static final int[] $SwitchMap$android$icu$impl$locale$KeyTypeData$ValueType;
        static final int[] $SwitchMap$android$icu$impl$locale$LocaleValidityChecker$SpecialCase = new int[SpecialCase.values().length];

        static {
            try {
                $SwitchMap$android$icu$impl$locale$LocaleValidityChecker$SpecialCase[SpecialCase.anything.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$android$icu$impl$locale$LocaleValidityChecker$SpecialCase[SpecialCase.codepoints.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$android$icu$impl$locale$LocaleValidityChecker$SpecialCase[SpecialCase.reorder.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$android$icu$impl$locale$LocaleValidityChecker$SpecialCase[SpecialCase.subdivision.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$android$icu$impl$locale$LocaleValidityChecker$SpecialCase[SpecialCase.rgKey.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
            $SwitchMap$android$icu$impl$locale$KeyTypeData$ValueType = new int[KeyTypeData.ValueType.values().length];
            try {
                $SwitchMap$android$icu$impl$locale$KeyTypeData$ValueType[KeyTypeData.ValueType.single.ordinal()] = 1;
            } catch (NoSuchFieldError e6) {
            }
            try {
                $SwitchMap$android$icu$impl$locale$KeyTypeData$ValueType[KeyTypeData.ValueType.incremental.ordinal()] = 2;
            } catch (NoSuchFieldError e7) {
            }
            try {
                $SwitchMap$android$icu$impl$locale$KeyTypeData$ValueType[KeyTypeData.ValueType.multiple.ordinal()] = 3;
            } catch (NoSuchFieldError e8) {
            }
            $SwitchMap$android$icu$impl$ValidIdentifiers$Datatype = new int[ValidIdentifiers.Datatype.values().length];
            try {
                $SwitchMap$android$icu$impl$ValidIdentifiers$Datatype[ValidIdentifiers.Datatype.x.ordinal()] = 1;
            } catch (NoSuchFieldError e9) {
            }
            try {
                $SwitchMap$android$icu$impl$ValidIdentifiers$Datatype[ValidIdentifiers.Datatype.t.ordinal()] = 2;
            } catch (NoSuchFieldError e10) {
            }
            try {
                $SwitchMap$android$icu$impl$ValidIdentifiers$Datatype[ValidIdentifiers.Datatype.u.ordinal()] = 3;
            } catch (NoSuchFieldError e11) {
            }
        }
    }

    private boolean isSubdivision(ULocale uLocale, String str) {
        if (str.length() < 3) {
            return false;
        }
        String strSubstring = str.substring(0, str.charAt(0) > '9' ? 2 : 3);
        if (ValidIdentifiers.isValid(ValidIdentifiers.Datatype.subdivision, this.datasubtypes, strSubstring, str.substring(strSubstring.length())) == null) {
            return false;
        }
        String country = uLocale.getCountry();
        if (country.isEmpty()) {
            country = ULocale.addLikelySubtags(uLocale).getCountry();
        }
        return strSubstring.equalsIgnoreCase(country);
    }

    private boolean isScriptReorder(String str) {
        String lowerString = AsciiUtil.toLowerString(str);
        if (REORDERING_INCLUDE.contains(lowerString)) {
            return true;
        }
        return (REORDERING_EXCLUDE.contains(lowerString) || ValidIdentifiers.isValid(ValidIdentifiers.Datatype.script, REGULAR_ONLY, lowerString) == null) ? false : true;
    }

    private boolean isValidLocale(String str, Where where) {
        try {
            return isValid(new ULocale.Builder().setLanguageTag(str).build(), where);
        } catch (IllformedLocaleException e) {
            return where.set(ValidIdentifiers.Datatype.t, SEPARATOR.split(str.substring(e.getErrorIndex()))[0]);
        } catch (Exception e2) {
            return where.set(ValidIdentifiers.Datatype.t, e2.getMessage());
        }
    }

    private boolean isValid(ValidIdentifiers.Datatype datatype, String str, Where where) {
        if (str.isEmpty()) {
            return true;
        }
        if ((datatype == ValidIdentifiers.Datatype.variant && "posix".equalsIgnoreCase(str)) || ValidIdentifiers.isValid(datatype, this.datasubtypes, str) != null) {
            return true;
        }
        if (where == null) {
            return false;
        }
        return where.set(datatype, str);
    }
}
