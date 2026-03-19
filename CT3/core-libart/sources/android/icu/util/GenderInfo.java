package android.icu.util;

import android.icu.impl.ICUCache;
import android.icu.impl.ICUResourceBundle;
import android.icu.impl.SimpleCache;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;

@Deprecated
public class GenderInfo {

    private static final int[] f108androidicuutilGenderInfo$GenderSwitchesValues = null;

    private static final int[] f109androidicuutilGenderInfo$ListGenderStyleSwitchesValues = null;
    private final ListGenderStyle style;
    private static GenderInfo neutral = new GenderInfo(ListGenderStyle.NEUTRAL);
    private static Cache genderInfoCache = new Cache(null);

    private static int[] m279getandroidicuutilGenderInfo$GenderSwitchesValues() {
        if (f108androidicuutilGenderInfo$GenderSwitchesValues != null) {
            return f108androidicuutilGenderInfo$GenderSwitchesValues;
        }
        int[] iArr = new int[Gender.valuesCustom().length];
        try {
            iArr[Gender.FEMALE.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[Gender.MALE.ordinal()] = 2;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[Gender.OTHER.ordinal()] = 3;
        } catch (NoSuchFieldError e3) {
        }
        f108androidicuutilGenderInfo$GenderSwitchesValues = iArr;
        return iArr;
    }

    private static int[] m280getandroidicuutilGenderInfo$ListGenderStyleSwitchesValues() {
        if (f109androidicuutilGenderInfo$ListGenderStyleSwitchesValues != null) {
            return f109androidicuutilGenderInfo$ListGenderStyleSwitchesValues;
        }
        int[] iArr = new int[ListGenderStyle.valuesCustom().length];
        try {
            iArr[ListGenderStyle.MALE_TAINTS.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[ListGenderStyle.MIXED_NEUTRAL.ordinal()] = 2;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[ListGenderStyle.NEUTRAL.ordinal()] = 3;
        } catch (NoSuchFieldError e3) {
        }
        f109androidicuutilGenderInfo$ListGenderStyleSwitchesValues = iArr;
        return iArr;
    }

    @Deprecated
    public enum Gender {
        MALE,
        FEMALE,
        OTHER;

        public static Gender[] valuesCustom() {
            return values();
        }
    }

    @Deprecated
    public static GenderInfo getInstance(ULocale uLocale) {
        return genderInfoCache.get(uLocale);
    }

    @Deprecated
    public static GenderInfo getInstance(Locale locale) {
        return getInstance(ULocale.forLocale(locale));
    }

    @Deprecated
    public enum ListGenderStyle {
        NEUTRAL,
        MIXED_NEUTRAL,
        MALE_TAINTS;

        private static Map<String, ListGenderStyle> fromNameMap = new HashMap(3);

        public static ListGenderStyle[] valuesCustom() {
            return values();
        }

        static {
            fromNameMap.put("neutral", NEUTRAL);
            fromNameMap.put("maleTaints", MALE_TAINTS);
            fromNameMap.put("mixedNeutral", MIXED_NEUTRAL);
        }

        @Deprecated
        public static ListGenderStyle fromName(String name) {
            ListGenderStyle result = fromNameMap.get(name);
            if (result == null) {
                throw new IllegalArgumentException("Unknown gender style name: " + name);
            }
            return result;
        }
    }

    @Deprecated
    public Gender getListGender(Gender... genders) {
        return getListGender(Arrays.asList(genders));
    }

    @Deprecated
    public Gender getListGender(List<Gender> genders) {
        if (genders.size() == 0) {
            return Gender.OTHER;
        }
        if (genders.size() == 1) {
            return genders.get(0);
        }
        switch (m280getandroidicuutilGenderInfo$ListGenderStyleSwitchesValues()[this.style.ordinal()]) {
            case 1:
                for (Gender gender : genders) {
                    if (gender != Gender.FEMALE) {
                        return Gender.MALE;
                    }
                }
                return Gender.FEMALE;
            case 2:
                boolean hasFemale = false;
                boolean hasMale = false;
                for (Gender gender2 : genders) {
                    switch (m279getandroidicuutilGenderInfo$GenderSwitchesValues()[gender2.ordinal()]) {
                        case 1:
                            if (hasMale) {
                                return Gender.OTHER;
                            }
                            hasFemale = true;
                            break;
                            break;
                        case 2:
                            if (hasFemale) {
                                return Gender.OTHER;
                            }
                            hasMale = true;
                            break;
                            break;
                        case 3:
                            return Gender.OTHER;
                    }
                }
                return hasMale ? Gender.MALE : Gender.FEMALE;
            case 3:
                return Gender.OTHER;
            default:
                return Gender.OTHER;
        }
    }

    @Deprecated
    public GenderInfo(ListGenderStyle genderStyle) {
        this.style = genderStyle;
    }

    private static class Cache {
        private final ICUCache<ULocale, GenderInfo> cache;

        Cache(Cache cache) {
            this();
        }

        private Cache() {
            this.cache = new SimpleCache();
        }

        public GenderInfo get(ULocale locale) {
            GenderInfo result = this.cache.get(locale);
            if (result == null) {
                result = load(locale);
                if (result == null) {
                    ULocale fallback = locale.getFallback();
                    result = fallback == null ? GenderInfo.neutral : get(fallback);
                }
                this.cache.put(locale, result);
            }
            return result;
        }

        private static GenderInfo load(ULocale ulocale) {
            UResourceBundle rb = UResourceBundle.getBundleInstance("android/icu/impl/data/icudt56b", "genderList", ICUResourceBundle.ICU_DATA_CLASS_LOADER, true);
            UResourceBundle genderList = rb.get("genderList");
            try {
                return new GenderInfo(ListGenderStyle.fromName(genderList.getString(ulocale.toString())));
            } catch (MissingResourceException e) {
                return null;
            }
        }
    }
}
