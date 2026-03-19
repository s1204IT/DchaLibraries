package android.icu.util;

import android.icu.impl.ICUCache;
import android.icu.impl.ICUData;
import android.icu.impl.ICUResourceBundle;
import android.icu.impl.SimpleCache;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;

@Deprecated
public class GenderInfo {
    private final ListGenderStyle style;
    private static GenderInfo neutral = new GenderInfo(ListGenderStyle.NEUTRAL);
    private static Cache genderInfoCache = new Cache();

    @Deprecated
    public enum Gender {
        MALE,
        FEMALE,
        OTHER
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

        static {
            fromNameMap.put("neutral", NEUTRAL);
            fromNameMap.put("maleTaints", MALE_TAINTS);
            fromNameMap.put("mixedNeutral", MIXED_NEUTRAL);
        }

        @Deprecated
        public static ListGenderStyle fromName(String str) {
            ListGenderStyle listGenderStyle = fromNameMap.get(str);
            if (listGenderStyle == null) {
                throw new IllegalArgumentException("Unknown gender style name: " + str);
            }
            return listGenderStyle;
        }
    }

    @Deprecated
    public Gender getListGender(Gender... genderArr) {
        return getListGender(Arrays.asList(genderArr));
    }

    @Deprecated
    public Gender getListGender(List<Gender> list) {
        if (list.size() == 0) {
            return Gender.OTHER;
        }
        boolean z = false;
        if (list.size() == 1) {
            return list.get(0);
        }
        switch (this.style) {
            case NEUTRAL:
                return Gender.OTHER;
            case MIXED_NEUTRAL:
                Iterator<Gender> it = list.iterator();
                boolean z2 = false;
                while (it.hasNext()) {
                    switch (it.next()) {
                        case FEMALE:
                            if (z) {
                                return Gender.OTHER;
                            }
                            z2 = true;
                            break;
                            break;
                        case MALE:
                            if (z2) {
                                return Gender.OTHER;
                            }
                            z = true;
                            break;
                            break;
                        case OTHER:
                            return Gender.OTHER;
                    }
                }
                return z ? Gender.MALE : Gender.FEMALE;
            case MALE_TAINTS:
                Iterator<Gender> it2 = list.iterator();
                while (it2.hasNext()) {
                    if (it2.next() != Gender.FEMALE) {
                        return Gender.MALE;
                    }
                }
                return Gender.FEMALE;
            default:
                return Gender.OTHER;
        }
    }

    @Deprecated
    public GenderInfo(ListGenderStyle listGenderStyle) {
        this.style = listGenderStyle;
    }

    private static class Cache {
        private final ICUCache<ULocale, GenderInfo> cache;

        private Cache() {
            this.cache = new SimpleCache();
        }

        public GenderInfo get(ULocale uLocale) {
            GenderInfo genderInfoLoad = this.cache.get(uLocale);
            if (genderInfoLoad == null) {
                genderInfoLoad = load(uLocale);
                if (genderInfoLoad == null) {
                    ULocale fallback = uLocale.getFallback();
                    genderInfoLoad = fallback == null ? GenderInfo.neutral : get(fallback);
                }
                this.cache.put(uLocale, genderInfoLoad);
            }
            return genderInfoLoad;
        }

        private static GenderInfo load(ULocale uLocale) {
            try {
                return new GenderInfo(ListGenderStyle.fromName(UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, "genderList", ICUResourceBundle.ICU_DATA_CLASS_LOADER, true).get("genderList").getString(uLocale.toString())));
            } catch (MissingResourceException e) {
                return null;
            }
        }
    }
}
