package android.icu.impl;

import android.icu.impl.locale.BaseLocale;
import android.icu.impl.number.Padder;
import android.icu.text.PluralRanges;
import android.icu.text.PluralRules;
import android.icu.util.ULocale;
import android.icu.util.UResourceBundle;
import java.text.ParseException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Set;
import java.util.TreeMap;

public class PluralRulesLoader extends PluralRules.Factory {
    private static Map<String, PluralRanges> localeIdToPluralRanges;
    private Map<String, String> localeIdToCardinalRulesId;
    private Map<String, String> localeIdToOrdinalRulesId;
    private Map<String, ULocale> rulesIdToEquivalentULocale;
    private final Map<String, PluralRules> rulesIdToRules = new HashMap();
    public static final PluralRulesLoader loader = new PluralRulesLoader();
    private static final PluralRanges UNKNOWN_RANGE = new PluralRanges().freeze();

    private PluralRulesLoader() {
    }

    @Override
    public ULocale[] getAvailableULocales() {
        Set<String> setKeySet = getLocaleIdToRulesIdMap(PluralRules.PluralType.CARDINAL).keySet();
        ULocale[] uLocaleArr = new ULocale[setKeySet.size()];
        Iterator<String> it = setKeySet.iterator();
        int i = 0;
        while (it.hasNext()) {
            uLocaleArr[i] = ULocale.createCanonical(it.next());
            i++;
        }
        return uLocaleArr;
    }

    @Override
    public ULocale getFunctionalEquivalent(ULocale uLocale, boolean[] zArr) {
        if (zArr != null && zArr.length > 0) {
            zArr[0] = getLocaleIdToRulesIdMap(PluralRules.PluralType.CARDINAL).containsKey(ULocale.canonicalize(uLocale.getBaseName()));
        }
        String rulesIdForLocale = getRulesIdForLocale(uLocale, PluralRules.PluralType.CARDINAL);
        if (rulesIdForLocale == null || rulesIdForLocale.trim().length() == 0) {
            return ULocale.ROOT;
        }
        ULocale uLocale2 = getRulesIdToEquivalentULocaleMap().get(rulesIdForLocale);
        if (uLocale2 == null) {
            return ULocale.ROOT;
        }
        return uLocale2;
    }

    private Map<String, String> getLocaleIdToRulesIdMap(PluralRules.PluralType pluralType) {
        checkBuildRulesIdMaps();
        return pluralType == PluralRules.PluralType.CARDINAL ? this.localeIdToCardinalRulesId : this.localeIdToOrdinalRulesId;
    }

    private Map<String, ULocale> getRulesIdToEquivalentULocaleMap() {
        checkBuildRulesIdMaps();
        return this.rulesIdToEquivalentULocale;
    }

    private void checkBuildRulesIdMaps() {
        int i;
        boolean z;
        Map<String, String> mapEmptyMap;
        Map<String, String> mapEmptyMap2;
        Map<String, ULocale> mapEmptyMap3;
        synchronized (this) {
            z = this.localeIdToCardinalRulesId != null;
        }
        if (!z) {
            try {
                UResourceBundle pluralBundle = getPluralBundle();
                UResourceBundle uResourceBundle = pluralBundle.get("locales");
                mapEmptyMap = new TreeMap<>();
                mapEmptyMap3 = new HashMap<>();
                for (int i2 = 0; i2 < uResourceBundle.getSize(); i2++) {
                    UResourceBundle uResourceBundle2 = uResourceBundle.get(i2);
                    String key = uResourceBundle2.getKey();
                    String strIntern = uResourceBundle2.getString().intern();
                    mapEmptyMap.put(key, strIntern);
                    if (!mapEmptyMap3.containsKey(strIntern)) {
                        mapEmptyMap3.put(strIntern, new ULocale(key));
                    }
                }
                UResourceBundle uResourceBundle3 = pluralBundle.get("locales_ordinals");
                mapEmptyMap2 = new TreeMap<>();
                for (i = 0; i < uResourceBundle3.getSize(); i++) {
                    UResourceBundle uResourceBundle4 = uResourceBundle3.get(i);
                    mapEmptyMap2.put(uResourceBundle4.getKey(), uResourceBundle4.getString().intern());
                }
            } catch (MissingResourceException e) {
                mapEmptyMap = Collections.emptyMap();
                mapEmptyMap2 = Collections.emptyMap();
                mapEmptyMap3 = Collections.emptyMap();
            }
            synchronized (this) {
                if (this.localeIdToCardinalRulesId == null) {
                    this.localeIdToCardinalRulesId = mapEmptyMap;
                    this.localeIdToOrdinalRulesId = mapEmptyMap2;
                    this.rulesIdToEquivalentULocale = mapEmptyMap3;
                }
            }
        }
    }

    public String getRulesIdForLocale(ULocale uLocale, PluralRules.PluralType pluralType) {
        String str;
        int iLastIndexOf;
        Map<String, String> localeIdToRulesIdMap = getLocaleIdToRulesIdMap(pluralType);
        String strCanonicalize = ULocale.canonicalize(uLocale.getBaseName());
        while (true) {
            str = localeIdToRulesIdMap.get(strCanonicalize);
            if (str != null || (iLastIndexOf = strCanonicalize.lastIndexOf(BaseLocale.SEP)) == -1) {
                break;
            }
            strCanonicalize = strCanonicalize.substring(0, iLastIndexOf);
        }
        return str;
    }

    public PluralRules getRulesForRulesId(String str) {
        boolean zContainsKey;
        PluralRules pluralRules;
        PluralRules description;
        synchronized (this.rulesIdToRules) {
            zContainsKey = this.rulesIdToRules.containsKey(str);
            if (zContainsKey) {
                pluralRules = this.rulesIdToRules.get(str);
            } else {
                pluralRules = null;
            }
        }
        if (!zContainsKey) {
            try {
                UResourceBundle uResourceBundle = getPluralBundle().get("rules").get(str);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < uResourceBundle.getSize(); i++) {
                    UResourceBundle uResourceBundle2 = uResourceBundle.get(i);
                    if (i > 0) {
                        sb.append("; ");
                    }
                    sb.append(uResourceBundle2.getKey());
                    sb.append(PluralRules.KEYWORD_RULE_SEPARATOR);
                    sb.append(uResourceBundle2.getString());
                }
                description = PluralRules.parseDescription(sb.toString());
            } catch (ParseException | MissingResourceException e) {
                description = pluralRules;
            }
            synchronized (this.rulesIdToRules) {
                if (this.rulesIdToRules.containsKey(str)) {
                    pluralRules = this.rulesIdToRules.get(str);
                } else {
                    this.rulesIdToRules.put(str, description);
                    pluralRules = description;
                }
            }
        }
        return pluralRules;
    }

    public UResourceBundle getPluralBundle() throws MissingResourceException {
        return ICUResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, "plurals", ICUResourceBundle.ICU_DATA_CLASS_LOADER, true);
    }

    @Override
    public PluralRules forLocale(ULocale uLocale, PluralRules.PluralType pluralType) {
        String rulesIdForLocale = getRulesIdForLocale(uLocale, pluralType);
        if (rulesIdForLocale == null || rulesIdForLocale.trim().length() == 0) {
            return PluralRules.DEFAULT;
        }
        PluralRules rulesForRulesId = getRulesForRulesId(rulesIdForLocale);
        if (rulesForRulesId == null) {
            return PluralRules.DEFAULT;
        }
        return rulesForRulesId;
    }

    static {
        String[][] strArr = {new String[]{"locales", "id ja km ko lo ms my th vi zh"}, new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER}, new String[]{"locales", "am bn fr gu hi hy kn mr pa zu"}, new String[]{PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_ONE}, new String[]{PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER}, new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER}, new String[]{"locales", "fa"}, new String[]{PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_OTHER}, new String[]{PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER}, new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER}, new String[]{"locales", "ka"}, new String[]{PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_ONE}, new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_OTHER}, new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER}, new String[]{"locales", "az de el gl hu it kk ky ml mn ne nl pt sq sw ta te tr ug uz"}, new String[]{PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER}, new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_ONE}, new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER}, new String[]{"locales", "af bg ca en es et eu fi nb sv ur"}, new String[]{PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER}, new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_OTHER}, new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER}, new String[]{"locales", "da fil is"}, new String[]{PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_ONE}, new String[]{PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER}, new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_ONE}, new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER}, new String[]{"locales", "si"}, new String[]{PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_ONE}, new String[]{PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER}, new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_OTHER}, new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER}, new String[]{"locales", "mk"}, new String[]{PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_OTHER}, new String[]{PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER}, new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_OTHER}, new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER}, new String[]{"locales", "lv"}, new String[]{PluralRules.KEYWORD_ZERO, PluralRules.KEYWORD_ZERO, PluralRules.KEYWORD_OTHER}, new String[]{PluralRules.KEYWORD_ZERO, PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_ONE}, new String[]{PluralRules.KEYWORD_ZERO, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER}, new String[]{PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_ZERO, PluralRules.KEYWORD_OTHER}, new String[]{PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_ONE}, new String[]{PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER}, new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_ZERO, PluralRules.KEYWORD_OTHER}, new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_ONE}, new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER}, new String[]{"locales", "ro"}, new String[]{PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_FEW}, new String[]{PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER}, new String[]{PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_FEW}, new String[]{PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_FEW}, new String[]{PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER}, new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_FEW}, new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER}, new String[]{"locales", "hr sr bs"}, new String[]{PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_ONE}, new String[]{PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_FEW}, new String[]{PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER}, new String[]{PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_ONE}, new String[]{PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_FEW}, new String[]{PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER}, new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_ONE}, new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_FEW}, new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER}, new String[]{"locales", "sl"}, new String[]{PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_FEW}, new String[]{PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_TWO, PluralRules.KEYWORD_TWO}, new String[]{PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_FEW}, new String[]{PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER}, new String[]{PluralRules.KEYWORD_TWO, PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_FEW}, new String[]{PluralRules.KEYWORD_TWO, PluralRules.KEYWORD_TWO, PluralRules.KEYWORD_TWO}, new String[]{PluralRules.KEYWORD_TWO, PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_FEW}, new String[]{PluralRules.KEYWORD_TWO, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER}, new String[]{PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_FEW}, new String[]{PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_TWO, PluralRules.KEYWORD_TWO}, new String[]{PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_FEW}, new String[]{PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER}, new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_FEW}, new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_TWO, PluralRules.KEYWORD_TWO}, new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_FEW}, new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER}, new String[]{"locales", "he"}, new String[]{PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_TWO, PluralRules.KEYWORD_OTHER}, new String[]{PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_MANY, PluralRules.KEYWORD_MANY}, new String[]{PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER}, new String[]{PluralRules.KEYWORD_TWO, PluralRules.KEYWORD_MANY, PluralRules.KEYWORD_OTHER}, new String[]{PluralRules.KEYWORD_TWO, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER}, new String[]{PluralRules.KEYWORD_MANY, PluralRules.KEYWORD_MANY, PluralRules.KEYWORD_MANY}, new String[]{PluralRules.KEYWORD_MANY, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_MANY}, new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_OTHER}, new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_TWO, PluralRules.KEYWORD_OTHER}, new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_MANY, PluralRules.KEYWORD_MANY}, new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER}, new String[]{"locales", "cs pl sk"}, new String[]{PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_FEW}, new String[]{PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_MANY, PluralRules.KEYWORD_MANY}, new String[]{PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER}, new String[]{PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_FEW}, new String[]{PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_MANY, PluralRules.KEYWORD_MANY}, new String[]{PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER}, new String[]{PluralRules.KEYWORD_MANY, PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_ONE}, new String[]{PluralRules.KEYWORD_MANY, PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_FEW}, new String[]{PluralRules.KEYWORD_MANY, PluralRules.KEYWORD_MANY, PluralRules.KEYWORD_MANY}, new String[]{PluralRules.KEYWORD_MANY, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER}, new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_ONE}, new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_FEW}, new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_MANY, PluralRules.KEYWORD_MANY}, new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER}, new String[]{"locales", "lt ru uk"}, new String[]{PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_ONE}, new String[]{PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_FEW}, new String[]{PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_MANY, PluralRules.KEYWORD_MANY}, new String[]{PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER}, new String[]{PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_ONE}, new String[]{PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_FEW}, new String[]{PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_MANY, PluralRules.KEYWORD_MANY}, new String[]{PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER}, new String[]{PluralRules.KEYWORD_MANY, PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_ONE}, new String[]{PluralRules.KEYWORD_MANY, PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_FEW}, new String[]{PluralRules.KEYWORD_MANY, PluralRules.KEYWORD_MANY, PluralRules.KEYWORD_MANY}, new String[]{PluralRules.KEYWORD_MANY, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER}, new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_ONE}, new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_FEW}, new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_MANY, PluralRules.KEYWORD_MANY}, new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER}, new String[]{"locales", "cy"}, new String[]{PluralRules.KEYWORD_ZERO, PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_ONE}, new String[]{PluralRules.KEYWORD_ZERO, PluralRules.KEYWORD_TWO, PluralRules.KEYWORD_TWO}, new String[]{PluralRules.KEYWORD_ZERO, PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_FEW}, new String[]{PluralRules.KEYWORD_ZERO, PluralRules.KEYWORD_MANY, PluralRules.KEYWORD_MANY}, new String[]{PluralRules.KEYWORD_ZERO, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER}, new String[]{PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_TWO, PluralRules.KEYWORD_TWO}, new String[]{PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_FEW}, new String[]{PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_MANY, PluralRules.KEYWORD_MANY}, new String[]{PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER}, new String[]{PluralRules.KEYWORD_TWO, PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_FEW}, new String[]{PluralRules.KEYWORD_TWO, PluralRules.KEYWORD_MANY, PluralRules.KEYWORD_MANY}, new String[]{PluralRules.KEYWORD_TWO, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER}, new String[]{PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_MANY, PluralRules.KEYWORD_MANY}, new String[]{PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER}, new String[]{PluralRules.KEYWORD_MANY, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER}, new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_ONE}, new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_TWO, PluralRules.KEYWORD_TWO}, new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_FEW}, new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_MANY, PluralRules.KEYWORD_MANY}, new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER}, new String[]{"locales", "ar"}, new String[]{PluralRules.KEYWORD_ZERO, PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_ZERO}, new String[]{PluralRules.KEYWORD_ZERO, PluralRules.KEYWORD_TWO, PluralRules.KEYWORD_ZERO}, new String[]{PluralRules.KEYWORD_ZERO, PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_FEW}, new String[]{PluralRules.KEYWORD_ZERO, PluralRules.KEYWORD_MANY, PluralRules.KEYWORD_MANY}, new String[]{PluralRules.KEYWORD_ZERO, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER}, new String[]{PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_TWO, PluralRules.KEYWORD_OTHER}, new String[]{PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_FEW}, new String[]{PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_MANY, PluralRules.KEYWORD_MANY}, new String[]{PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER}, new String[]{PluralRules.KEYWORD_TWO, PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_FEW}, new String[]{PluralRules.KEYWORD_TWO, PluralRules.KEYWORD_MANY, PluralRules.KEYWORD_MANY}, new String[]{PluralRules.KEYWORD_TWO, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER}, new String[]{PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_FEW}, new String[]{PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_MANY, PluralRules.KEYWORD_MANY}, new String[]{PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER}, new String[]{PluralRules.KEYWORD_MANY, PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_FEW}, new String[]{PluralRules.KEYWORD_MANY, PluralRules.KEYWORD_MANY, PluralRules.KEYWORD_MANY}, new String[]{PluralRules.KEYWORD_MANY, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER}, new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_OTHER}, new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_TWO, PluralRules.KEYWORD_OTHER}, new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_FEW}, new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_MANY, PluralRules.KEYWORD_MANY}, new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER}};
        HashMap map = new HashMap();
        String[] strArrSplit = null;
        PluralRanges pluralRanges = null;
        for (String[] strArr2 : strArr) {
            if (strArr2[0].equals("locales")) {
                if (pluralRanges != null) {
                    pluralRanges.freeze();
                    for (String str : strArrSplit) {
                        map.put(str, pluralRanges);
                    }
                }
                strArrSplit = strArr2[1].split(Padder.FALLBACK_PADDING_STRING);
                pluralRanges = new PluralRanges();
            } else {
                pluralRanges.add(StandardPlural.fromString(strArr2[0]), StandardPlural.fromString(strArr2[1]), StandardPlural.fromString(strArr2[2]));
            }
        }
        for (String str2 : strArrSplit) {
            map.put(str2, pluralRanges);
        }
        localeIdToPluralRanges = Collections.unmodifiableMap(map);
    }

    @Override
    public boolean hasOverride(ULocale uLocale) {
        return false;
    }

    public PluralRanges getPluralRanges(ULocale uLocale) {
        String strCanonicalize = ULocale.canonicalize(uLocale.getBaseName());
        while (true) {
            PluralRanges pluralRanges = localeIdToPluralRanges.get(strCanonicalize);
            if (pluralRanges == null) {
                int iLastIndexOf = strCanonicalize.lastIndexOf(BaseLocale.SEP);
                if (iLastIndexOf == -1) {
                    return UNKNOWN_RANGE;
                }
                strCanonicalize = strCanonicalize.substring(0, iLastIndexOf);
            } else {
                return pluralRanges;
            }
        }
    }

    public boolean isPluralRangesAvailable(ULocale uLocale) {
        return getPluralRanges(uLocale) == UNKNOWN_RANGE;
    }
}
