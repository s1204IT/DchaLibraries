package android.icu.impl.locale;

import android.icu.impl.ICUData;
import android.icu.impl.ICUResourceBundle;
import android.icu.impl.Utility;
import android.icu.impl.locale.XCldrStub;
import android.icu.util.ICUException;
import android.icu.util.ULocale;
import android.icu.util.UResourceBundle;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class XLikelySubtags {
    private static final XLikelySubtags DEFAULT = new XLikelySubtags();
    final Map<String, Map<String, Map<String, LSR>>> langTable;

    public static final XLikelySubtags getDefault() {
        return DEFAULT;
    }

    static abstract class Maker {
        static final Maker HASHMAP = new Maker() {
            @Override
            public Map<Object, Object> make() {
                return new HashMap();
            }
        };
        static final Maker TREEMAP = new Maker() {
            @Override
            public Map<Object, Object> make() {
                return new TreeMap();
            }
        };

        abstract <V> V make();

        Maker() {
        }

        public <K, V> V getSubtable(Map<K, V> map, K k) {
            V v = map.get(k);
            if (v == null) {
                V v2 = (V) make();
                map.put(k, v2);
                return v2;
            }
            return v;
        }
    }

    public static class Aliases {
        final XCldrStub.Multimap<String, String> toAliases;
        final Map<String, String> toCanonical;

        public String getCanonical(String str) {
            String str2 = this.toCanonical.get(str);
            return str2 == null ? str : str2;
        }

        public Set<String> getAliases(String str) {
            Set<String> set = this.toAliases.get(str);
            return set == null ? Collections.singleton(str) : set;
        }

        public Aliases(String str) {
            UResourceBundle uResourceBundle = UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, "metadata", ICUResourceBundle.ICU_DATA_CLASS_LOADER).get("alias").get(str);
            HashMap map = new HashMap();
            for (int i = 0; i < uResourceBundle.getSize(); i++) {
                UResourceBundle uResourceBundle2 = uResourceBundle.get(i);
                String key = uResourceBundle2.getKey();
                if (!key.contains(BaseLocale.SEP) && !uResourceBundle2.get("reason").getString().equals("overlong")) {
                    String string = uResourceBundle2.get("replacement").getString();
                    int iIndexOf = string.indexOf(32);
                    string = iIndexOf >= 0 ? string.substring(0, iIndexOf) : string;
                    if (!string.contains(BaseLocale.SEP)) {
                        map.put(key, string);
                    }
                }
            }
            if (str.equals("language")) {
                map.put("mo", "ro");
            }
            this.toCanonical = Collections.unmodifiableMap(map);
            this.toAliases = XCldrStub.Multimaps.invertFrom(map, XCldrStub.HashMultimap.create());
        }
    }

    public static class LSR {
        public static Aliases LANGUAGE_ALIASES = new Aliases("language");
        public static Aliases REGION_ALIASES = new Aliases("territory");
        public final String language;
        public final String region;
        public final String script;

        public static LSR from(String str, String str2, String str3) {
            return new LSR(str, str2, str3);
        }

        static LSR from(String str) {
            String[] strArrSplit = str.split("[-_]");
            if (strArrSplit.length < 1 || strArrSplit.length > 3) {
                throw new ICUException("too many subtags");
            }
            String lowerCase = strArrSplit[0].toLowerCase();
            String str2 = strArrSplit.length < 2 ? "" : strArrSplit[1];
            return str2.length() < 4 ? new LSR(lowerCase, "", str2) : new LSR(lowerCase, str2, strArrSplit.length < 3 ? "" : strArrSplit[2]);
        }

        public static LSR from(ULocale uLocale) {
            return new LSR(uLocale.getLanguage(), uLocale.getScript(), uLocale.getCountry());
        }

        public static LSR fromMaximalized(ULocale uLocale) {
            return fromMaximalized(uLocale.getLanguage(), uLocale.getScript(), uLocale.getCountry());
        }

        public static LSR fromMaximalized(String str, String str2, String str3) {
            return XLikelySubtags.DEFAULT.maximize(LANGUAGE_ALIASES.getCanonical(str), str2, REGION_ALIASES.getCanonical(str3));
        }

        public LSR(String str, String str2, String str3) {
            this.language = str;
            this.script = str2;
            this.region = str3;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder(this.language);
            if (!this.script.isEmpty()) {
                sb.append('-');
                sb.append(this.script);
            }
            if (!this.region.isEmpty()) {
                sb.append('-');
                sb.append(this.region);
            }
            return sb.toString();
        }

        public LSR replace(String str, String str2, String str3) {
            if (str == null && str2 == null && str3 == null) {
                return this;
            }
            if (str == null) {
                str = this.language;
            }
            if (str2 == null) {
                str2 = this.script;
            }
            if (str3 == null) {
                str3 = this.region;
            }
            return new LSR(str, str2, str3);
        }

        public boolean equals(Object obj) {
            if (this != obj) {
                if (obj != null && obj.getClass() == getClass()) {
                    LSR lsr = (LSR) obj;
                    if (!this.language.equals(lsr.language) || !this.script.equals(lsr.script) || !this.region.equals(lsr.region)) {
                    }
                }
                return false;
            }
            return true;
        }

        public int hashCode() {
            return Utility.hash(this.language, this.script, this.region);
        }
    }

    public XLikelySubtags() {
        this(getDefaultRawData(), true);
    }

    private static Map<String, String> getDefaultRawData() {
        TreeMap treeMap = new TreeMap();
        UResourceBundle bundleInstance = UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, "likelySubtags");
        Enumeration<String> keys = bundleInstance.getKeys();
        while (keys.hasMoreElements()) {
            String strNextElement = keys.nextElement();
            treeMap.put(strNextElement, bundleInstance.getString(strNextElement));
        }
        return treeMap;
    }

    public XLikelySubtags(Map<String, String> map, boolean z) {
        this.langTable = init(map, z);
    }

    private Map<String, Map<String, Map<String, LSR>>> init(Map<String, String> map, boolean z) {
        Map<String, Map<String, Map<String, LSR>>> map2 = (Map) Maker.TREEMAP.make();
        HashMap map3 = new HashMap();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            LSR lsrFrom = LSR.from(entry.getKey());
            String str = lsrFrom.language;
            String str2 = lsrFrom.script;
            String str3 = lsrFrom.region;
            LSR lsrFrom2 = LSR.from(entry.getValue());
            String str4 = lsrFrom2.language;
            String str5 = lsrFrom2.script;
            String str6 = lsrFrom2.region;
            set(map2, str, str2, str3, str4, str5, str6, map3);
            Set<String> aliases = LSR.LANGUAGE_ALIASES.getAliases(str);
            Set<String> aliases2 = LSR.REGION_ALIASES.getAliases(str3);
            Iterator<String> it = aliases.iterator();
            while (it.hasNext()) {
                String next = it.next();
                Iterator<String> it2 = aliases2.iterator();
                while (it2.hasNext()) {
                    String next2 = it2.next();
                    if (!next.equals(str) || !next2.equals(str3)) {
                        set(map2, next, str2, next2, str4, str5, str6, map3);
                        it2 = it2;
                        next = next;
                        it = it;
                        aliases2 = aliases2;
                    }
                }
            }
        }
        set(map2, "und", "Latn", "", "en", "Latn", "US", map3);
        Iterator<Map.Entry<String, LSR>> it3 = map2.get("und").get("").entrySet().iterator();
        while (it3.hasNext()) {
            LSR value = it3.next().getValue();
            set(map2, "und", value.script, value.region, value);
        }
        if (!map2.containsKey("und")) {
            throw new IllegalArgumentException("failure: base");
        }
        for (Map.Entry<String, Map<String, Map<String, LSR>>> entry2 : map2.entrySet()) {
            String key = entry2.getKey();
            Map<String, Map<String, LSR>> value2 = entry2.getValue();
            if (!value2.containsKey("")) {
                throw new IllegalArgumentException("failure: " + key);
            }
            for (Map.Entry<String, Map<String, LSR>> entry3 : value2.entrySet()) {
                String key2 = entry3.getKey();
                if (!entry3.getValue().containsKey("")) {
                    throw new IllegalArgumentException("failure: " + key + LanguageTag.SEP + key2);
                }
            }
        }
        return map2;
    }

    private void set(Map<String, Map<String, Map<String, LSR>>> map, String str, String str2, String str3, String str4, String str5, String str6, Map<LSR, LSR> map2) {
        LSR lsr;
        LSR lsr2 = new LSR(str4, str5, str6);
        LSR lsr3 = map2.get(lsr2);
        if (lsr3 == null) {
            map2.put(lsr2, lsr2);
            lsr = lsr2;
        } else {
            lsr = lsr3;
        }
        set(map, str, str2, str3, lsr);
    }

    private void set(Map<String, Map<String, Map<String, LSR>>> map, String str, String str2, String str3, LSR lsr) {
        ((Map) Maker.TREEMAP.getSubtable((Map) Maker.TREEMAP.getSubtable(map, str), str2)).put(str3, lsr);
    }

    public LSR maximize(String str) {
        return maximize(ULocale.forLanguageTag(str));
    }

    public LSR maximize(ULocale uLocale) {
        return maximize(uLocale.getLanguage(), uLocale.getScript(), uLocale.getCountry());
    }

    public LSR maximize(LSR lsr) {
        return maximize(lsr.language, lsr.script, lsr.region);
    }

    public LSR maximize(String str, String str2, String str3) {
        Map<String, Map<String, LSR>> map = this.langTable.get(str);
        int i = 4;
        if (map == null) {
            map = this.langTable.get("und");
        } else if (str.equals("und")) {
            i = 0;
        }
        if (str2.equals("Zzzz")) {
            str2 = "";
        }
        Map<String, LSR> map2 = map.get(str2);
        if (map2 == null) {
            i |= 2;
            map2 = map.get("");
        } else if (!str2.isEmpty()) {
            i |= 2;
        }
        if (str3.equals("ZZ")) {
            str3 = "";
        }
        LSR lsr = map2.get(str3);
        if (lsr == null) {
            i |= 1;
            lsr = map2.get("");
            if (lsr == null) {
                return null;
            }
        } else if (!str3.isEmpty()) {
            i |= 1;
        }
        switch (i) {
            case 1:
                return lsr.replace(null, null, str3);
            case 2:
                return lsr.replace(null, str2, null);
            case 3:
                return lsr.replace(null, str2, str3);
            case 4:
                return lsr.replace(str, null, null);
            case 5:
                return lsr.replace(str, null, str3);
            case 6:
                return lsr.replace(str, str2, null);
            case 7:
                return lsr.replace(str, str2, str3);
            default:
                return lsr;
        }
    }

    private LSR minimizeSubtags(String str, String str2, String str3, ULocale.Minimize minimize) {
        boolean z;
        LSR lsrMaximize = maximize(str, str2, str3);
        LSR lsr = this.langTable.get(lsrMaximize.language).get("").get("");
        if (lsrMaximize.script.equals(lsr.script)) {
            if (lsrMaximize.region.equals(lsr.region)) {
                return lsrMaximize.replace(null, "", "");
            }
            if (minimize == ULocale.Minimize.FAVOR_REGION) {
                return lsrMaximize.replace(null, "", null);
            }
            z = true;
        } else {
            z = false;
        }
        if (maximize(str, str2, "").equals(lsrMaximize)) {
            return lsrMaximize.replace(null, null, "");
        }
        if (z) {
            return lsrMaximize.replace(null, "", null);
        }
        return lsrMaximize;
    }

    private static StringBuilder show(Map<?, ?> map, String str, StringBuilder sb) {
        String str2 = str.isEmpty() ? "" : "\t";
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String string = entry.getKey().toString();
            Object value = entry.getValue();
            StringBuilder sb2 = new StringBuilder();
            sb2.append(str2);
            if (string.isEmpty()) {
                string = "∅";
            }
            sb2.append(string);
            sb.append(sb2.toString());
            if (value instanceof Map) {
                show((Map) value, str + "\t", sb);
            } else {
                sb.append("\t" + Utility.toString(value));
                sb.append("\n");
            }
            str2 = str;
        }
        return sb;
    }

    public String toString() {
        return show(this.langTable, "", new StringBuilder()).toString();
    }
}
