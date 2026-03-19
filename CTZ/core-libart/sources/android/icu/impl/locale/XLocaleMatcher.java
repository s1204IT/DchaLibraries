package android.icu.impl.locale;

import android.icu.impl.locale.XCldrStub;
import android.icu.impl.locale.XLikelySubtags;
import android.icu.impl.locale.XLocaleDistance;
import android.icu.util.LocalePriorityList;
import android.icu.util.Output;
import android.icu.util.ULocale;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class XLocaleMatcher {
    private static final XLikelySubtags.LSR UND = new XLikelySubtags.LSR("und", "", "");
    private static final ULocale UND_LOCALE = new ULocale("und");
    private final ULocale defaultLanguage;
    private final int demotionPerAdditionalDesiredLocale;
    private final XLocaleDistance.DistanceOption distanceOption;
    private final Set<ULocale> exactSupportedLocales;
    private final XLocaleDistance localeDistance;
    private final Map<XLikelySubtags.LSR, Set<ULocale>> supportedLanguages;
    private final int thresholdDistance;

    public static class Builder {
        private ULocale defaultLanguage;
        private XLocaleDistance.DistanceOption distanceOption;
        private XLocaleDistance localeDistance;
        private Set<ULocale> supportedLanguagesList;
        private int thresholdDistance = -1;
        private int demotionPerAdditionalDesiredLocale = -1;

        public Builder setSupportedLocales(String str) {
            this.supportedLanguagesList = XLocaleMatcher.asSet(LocalePriorityList.add(str).build());
            return this;
        }

        public Builder setSupportedLocales(LocalePriorityList localePriorityList) {
            this.supportedLanguagesList = XLocaleMatcher.asSet(localePriorityList);
            return this;
        }

        public Builder setSupportedLocales(Set<ULocale> set) {
            this.supportedLanguagesList = set;
            return this;
        }

        public Builder setThresholdDistance(int i) {
            this.thresholdDistance = i;
            return this;
        }

        public Builder setDemotionPerAdditionalDesiredLocale(int i) {
            this.demotionPerAdditionalDesiredLocale = i;
            return this;
        }

        public Builder setLocaleDistance(XLocaleDistance xLocaleDistance) {
            this.localeDistance = xLocaleDistance;
            return this;
        }

        public Builder setDefaultLanguage(ULocale uLocale) {
            this.defaultLanguage = uLocale;
            return this;
        }

        public Builder setDistanceOption(XLocaleDistance.DistanceOption distanceOption) {
            this.distanceOption = distanceOption;
            return this;
        }

        public XLocaleMatcher build() {
            return new XLocaleMatcher(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public XLocaleMatcher(String str) {
        this(builder().setSupportedLocales(str));
    }

    public XLocaleMatcher(LocalePriorityList localePriorityList) {
        this(builder().setSupportedLocales(localePriorityList));
    }

    public XLocaleMatcher(Set<ULocale> set) {
        this(builder().setSupportedLocales(set));
    }

    private XLocaleMatcher(Builder builder) {
        ULocale next;
        this.localeDistance = builder.localeDistance == null ? XLocaleDistance.getDefault() : builder.localeDistance;
        this.thresholdDistance = builder.thresholdDistance < 0 ? this.localeDistance.getDefaultScriptDistance() : builder.thresholdDistance;
        XCldrStub.Multimap<XLikelySubtags.LSR, ULocale> multimapExtractLsrMap = extractLsrMap(builder.supportedLanguagesList, extractLsrSet(this.localeDistance.getParadigms()));
        this.supportedLanguages = multimapExtractLsrMap.asMap();
        this.exactSupportedLocales = XCldrStub.ImmutableSet.copyOf(multimapExtractLsrMap.values());
        if (builder.defaultLanguage != null) {
            next = builder.defaultLanguage;
        } else {
            next = this.supportedLanguages.isEmpty() ? null : this.supportedLanguages.entrySet().iterator().next().getValue().iterator().next();
        }
        this.defaultLanguage = next;
        this.demotionPerAdditionalDesiredLocale = builder.demotionPerAdditionalDesiredLocale < 0 ? this.localeDistance.getDefaultRegionDistance() + 1 : builder.demotionPerAdditionalDesiredLocale;
        this.distanceOption = builder.distanceOption;
    }

    private Set<XLikelySubtags.LSR> extractLsrSet(Set<ULocale> set) {
        LinkedHashSet linkedHashSet = new LinkedHashSet();
        for (ULocale uLocale : set) {
            linkedHashSet.add(uLocale.equals(UND_LOCALE) ? UND : XLikelySubtags.LSR.fromMaximalized(uLocale));
        }
        return linkedHashSet;
    }

    private XCldrStub.Multimap<XLikelySubtags.LSR, ULocale> extractLsrMap(Set<ULocale> set, Set<XLikelySubtags.LSR> set2) {
        XCldrStub.LinkedHashMultimap linkedHashMultimapCreate;
        XCldrStub.LinkedHashMultimap linkedHashMultimapCreate2 = XCldrStub.LinkedHashMultimap.create();
        for (ULocale uLocale : set) {
            linkedHashMultimapCreate2.put(uLocale.equals(UND_LOCALE) ? UND : XLikelySubtags.LSR.fromMaximalized(uLocale), uLocale);
        }
        boolean z = true;
        if (linkedHashMultimapCreate2.size() > 1 && set2 != null) {
            linkedHashMultimapCreate = XCldrStub.LinkedHashMultimap.create();
            for (Map.Entry entry : linkedHashMultimapCreate2.asMap().entrySet()) {
                XLikelySubtags.LSR lsr = (XLikelySubtags.LSR) entry.getKey();
                if (z || set2.contains(lsr)) {
                    linkedHashMultimapCreate.putAll(lsr, (Collection) entry.getValue());
                    z = false;
                }
            }
            linkedHashMultimapCreate.putAll(linkedHashMultimapCreate2);
            if (!linkedHashMultimapCreate.equals(linkedHashMultimapCreate2)) {
                throw new IllegalArgumentException();
            }
        } else {
            linkedHashMultimapCreate = linkedHashMultimapCreate2;
        }
        return XCldrStub.ImmutableMultimap.copyOf(linkedHashMultimapCreate);
    }

    public ULocale getBestMatch(ULocale uLocale) {
        return getBestMatch(uLocale, (Output<ULocale>) null);
    }

    public ULocale getBestMatch(String str) {
        return getBestMatch(LocalePriorityList.add(str).build(), (Output<ULocale>) null);
    }

    public ULocale getBestMatch(ULocale... uLocaleArr) {
        return getBestMatch(new LinkedHashSet(Arrays.asList(uLocaleArr)), (Output<ULocale>) null);
    }

    public ULocale getBestMatch(Set<ULocale> set) {
        return getBestMatch(set, (Output<ULocale>) null);
    }

    public ULocale getBestMatch(LocalePriorityList localePriorityList) {
        return getBestMatch(localePriorityList, (Output<ULocale>) null);
    }

    public ULocale getBestMatch(LocalePriorityList localePriorityList, Output<ULocale> output) {
        return getBestMatch(asSet(localePriorityList), output);
    }

    private static Set<ULocale> asSet(LocalePriorityList localePriorityList) {
        LinkedHashSet linkedHashSet = new LinkedHashSet();
        Iterator<ULocale> it = localePriorityList.iterator();
        while (it.hasNext()) {
            linkedHashSet.add(it.next());
        }
        return linkedHashSet;
    }

    public ULocale getBestMatch(Set<ULocale> set, Output<ULocale> output) {
        if (set.size() == 1) {
            return getBestMatch(set.iterator().next(), output);
        }
        int i = Integer.MAX_VALUE;
        int i2 = 0;
        Iterator<Map.Entry<XLikelySubtags.LSR, ULocale>> it = extractLsrMap(set, null).entries().iterator();
        ?? r3 = 0;
        Set<ULocale> value = null;
        loop0: while (true) {
            if (!it.hasNext()) {
                break;
            }
            Map.Entry<XLikelySubtags.LSR, ULocale> next = it.next();
            ULocale value2 = next.getValue();
            XLikelySubtags.LSR key = next.getKey();
            if (i2 < i) {
                if (this.exactSupportedLocales.contains(value2)) {
                    if (output != null) {
                        output.value = value2;
                    }
                    return value2;
                }
                Set<ULocale> set2 = this.supportedLanguages.get(key);
                if (set2 != null) {
                    if (output != null) {
                        output.value = value2;
                    }
                    return set2.iterator().next();
                }
            }
            for (Map.Entry<XLikelySubtags.LSR, Set<ULocale>> entry : this.supportedLanguages.entrySet()) {
                int iDistanceRaw = this.localeDistance.distanceRaw(key, entry.getKey(), this.thresholdDistance, this.distanceOption) + i2;
                if (iDistanceRaw < i) {
                    value = entry.getValue();
                    if (iDistanceRaw != 0) {
                        r3 = value2;
                        i = iDistanceRaw;
                    } else {
                        r3 = value2;
                        i = iDistanceRaw;
                        break loop0;
                    }
                }
            }
            i2 += this.demotionPerAdditionalDesiredLocale;
            r3 = r3;
        }
        if (i >= this.thresholdDistance) {
            if (output != null) {
                output.value = null;
            }
            return this.defaultLanguage;
        }
        if (output != null) {
            output.value = r3;
        }
        if (value.contains(r3)) {
            return r3;
        }
        return value.iterator().next();
    }

    public ULocale getBestMatch(ULocale uLocale, Output<ULocale> output) {
        ?? r12;
        Set<ULocale> set;
        XLikelySubtags.LSR lsrFromMaximalized = uLocale.equals(UND_LOCALE) ? UND : XLikelySubtags.LSR.fromMaximalized(uLocale);
        if (this.exactSupportedLocales.contains(uLocale)) {
            if (output != null) {
                output.value = uLocale;
            }
            return uLocale;
        }
        if (this.distanceOption == XLocaleDistance.DistanceOption.NORMAL && (set = this.supportedLanguages.get(lsrFromMaximalized)) != null) {
            if (output != null) {
                output.value = uLocale;
            }
            return set.iterator().next();
        }
        Iterator<Map.Entry<XLikelySubtags.LSR, Set<ULocale>>> it = this.supportedLanguages.entrySet().iterator();
        int i = Integer.MAX_VALUE;
        Object obj = null;
        Set<ULocale> value = null;
        while (true) {
            if (it.hasNext()) {
                Map.Entry<XLikelySubtags.LSR, Set<ULocale>> next = it.next();
                int iDistanceRaw = this.localeDistance.distanceRaw(lsrFromMaximalized, next.getKey(), this.thresholdDistance, this.distanceOption);
                if (iDistanceRaw < i) {
                    value = next.getValue();
                    if (iDistanceRaw != 0) {
                        obj = uLocale;
                        i = iDistanceRaw;
                    } else {
                        i = iDistanceRaw;
                        r12 = uLocale;
                        break;
                    }
                }
            } else {
                r12 = obj;
                break;
            }
        }
        if (i >= this.thresholdDistance) {
            if (output != null) {
                output.value = null;
            }
            return this.defaultLanguage;
        }
        if (output != null) {
            output.value = r12;
        }
        if (value.contains(r12)) {
            return r12;
        }
        return value.iterator().next();
    }

    public static ULocale combine(ULocale uLocale, ULocale uLocale2) {
        if (!uLocale.equals(uLocale2) && uLocale2 != null) {
            ULocale.Builder locale = new ULocale.Builder().setLocale(uLocale);
            String country = uLocale2.getCountry();
            if (!country.isEmpty()) {
                locale.setRegion(country);
            }
            String variant = uLocale2.getVariant();
            if (!variant.isEmpty()) {
                locale.setVariant(variant);
            }
            Iterator<Character> it = uLocale2.getExtensionKeys().iterator();
            while (it.hasNext()) {
                char cCharValue = it.next().charValue();
                locale.setExtension(cCharValue, uLocale2.getExtension(cCharValue));
            }
            return locale.build();
        }
        return uLocale;
    }

    public int distance(ULocale uLocale, ULocale uLocale2) {
        return this.localeDistance.distanceRaw(XLikelySubtags.LSR.fromMaximalized(uLocale), XLikelySubtags.LSR.fromMaximalized(uLocale2), this.thresholdDistance, this.distanceOption);
    }

    public int distance(String str, String str2) {
        return this.localeDistance.distanceRaw(XLikelySubtags.LSR.fromMaximalized(new ULocale(str)), XLikelySubtags.LSR.fromMaximalized(new ULocale(str2)), this.thresholdDistance, this.distanceOption);
    }

    public String toString() {
        return this.exactSupportedLocales.toString();
    }

    public double match(ULocale uLocale, ULocale uLocale2) {
        return ((double) (100 - distance(uLocale, uLocale2))) / 100.0d;
    }

    @Deprecated
    public double match(ULocale uLocale, ULocale uLocale2, ULocale uLocale3, ULocale uLocale4) {
        return match(uLocale, uLocale3);
    }

    public ULocale canonicalize(ULocale uLocale) {
        return null;
    }

    public int getThresholdDistance() {
        return this.thresholdDistance;
    }
}
