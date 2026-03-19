package android.icu.text;

import android.icu.impl.ICUData;
import android.icu.impl.ICUResourceBundle;
import android.icu.impl.LocaleUtility;
import android.icu.impl.Utility;
import android.icu.lang.UScript;
import android.icu.text.RuleBasedTransliterator;
import android.icu.text.Transliterator;
import android.icu.util.CaseInsensitiveString;
import android.icu.util.UResourceBundle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

class TransliteratorRegistry {
    private static final String ANY = "Any";
    private static final boolean DEBUG = false;
    private static final char LOCALE_SEP = '_';
    private static final String NO_VARIANT = "";
    private Map<CaseInsensitiveString, Object[]> registry = Collections.synchronizedMap(new HashMap());
    private Map<CaseInsensitiveString, Map<CaseInsensitiveString, List<CaseInsensitiveString>>> specDAG = Collections.synchronizedMap(new HashMap());
    private List<CaseInsensitiveString> availableIDs = new ArrayList();

    static class Spec {
        private boolean isNextLocale;
        private boolean isSpecLocale;
        private String nextSpec;
        private ICUResourceBundle res;
        private String scriptName;
        private String spec = null;
        private String top;

        public Spec(String str) {
            this.top = str;
            this.scriptName = null;
            try {
                int codeFromName = UScript.getCodeFromName(this.top);
                int[] code = UScript.getCode(this.top);
                if (code != null) {
                    this.scriptName = UScript.getName(code[0]);
                    if (this.scriptName.equalsIgnoreCase(this.top)) {
                        this.scriptName = null;
                    }
                }
                this.isSpecLocale = false;
                this.res = null;
                if (codeFromName == -1) {
                    this.res = (ICUResourceBundle) UResourceBundle.getBundleInstance(ICUData.ICU_TRANSLIT_BASE_NAME, LocaleUtility.getLocaleFromName(this.top));
                    if (this.res != null && LocaleUtility.isFallbackOf(this.res.getULocale().toString(), this.top)) {
                        this.isSpecLocale = true;
                    }
                }
            } catch (MissingResourceException e) {
                this.scriptName = null;
            }
            reset();
        }

        public boolean hasFallback() {
            return this.nextSpec != null;
        }

        public void reset() {
            if (!Utility.sameObjects(this.spec, this.top)) {
                this.spec = this.top;
                this.isSpecLocale = this.res != null;
                setupNext();
            }
        }

        private void setupNext() {
            this.isNextLocale = false;
            if (this.isSpecLocale) {
                this.nextSpec = this.spec;
                int iLastIndexOf = this.nextSpec.lastIndexOf(95);
                if (iLastIndexOf > 0) {
                    this.nextSpec = this.spec.substring(0, iLastIndexOf);
                    this.isNextLocale = true;
                    return;
                } else {
                    this.nextSpec = this.scriptName;
                    return;
                }
            }
            if (!Utility.sameObjects(this.nextSpec, this.scriptName)) {
                this.nextSpec = this.scriptName;
            } else {
                this.nextSpec = null;
            }
        }

        public String next() {
            this.spec = this.nextSpec;
            this.isSpecLocale = this.isNextLocale;
            setupNext();
            return this.spec;
        }

        public String get() {
            return this.spec;
        }

        public boolean isLocale() {
            return this.isSpecLocale;
        }

        public ResourceBundle getBundle() {
            if (this.res != null && this.res.getULocale().toString().equals(this.spec)) {
                return this.res;
            }
            return null;
        }

        public String getTop() {
            return this.top;
        }
    }

    static class ResourceEntry {
        public int direction;
        public String resource;

        public ResourceEntry(String str, int i) {
            this.resource = str;
            this.direction = i;
        }
    }

    static class LocaleEntry {
        public int direction;
        public String rule;

        public LocaleEntry(String str, int i) {
            this.rule = str;
            this.direction = i;
        }
    }

    static class AliasEntry {
        public String alias;

        public AliasEntry(String str) {
            this.alias = str;
        }
    }

    static class CompoundRBTEntry {
        private String ID;
        private UnicodeSet compoundFilter;
        private List<RuleBasedTransliterator.Data> dataVector;
        private List<String> idBlockVector;

        public CompoundRBTEntry(String str, List<String> list, List<RuleBasedTransliterator.Data> list2, UnicodeSet unicodeSet) {
            this.ID = str;
            this.idBlockVector = list;
            this.dataVector = list2;
            this.compoundFilter = unicodeSet;
        }

        public Transliterator getInstance() {
            ArrayList arrayList = new ArrayList();
            int iMax = Math.max(this.idBlockVector.size(), this.dataVector.size());
            int i = 1;
            for (int i2 = 0; i2 < iMax; i2++) {
                if (i2 < this.idBlockVector.size()) {
                    String str = this.idBlockVector.get(i2);
                    if (str.length() > 0) {
                        arrayList.add(Transliterator.getInstance(str));
                    }
                }
                if (i2 < this.dataVector.size()) {
                    arrayList.add(new RuleBasedTransliterator("%Pass" + i, this.dataVector.get(i2), null));
                    i++;
                }
            }
            CompoundTransliterator compoundTransliterator = new CompoundTransliterator(arrayList, i - 1);
            compoundTransliterator.setID(this.ID);
            if (this.compoundFilter != null) {
                compoundTransliterator.setFilter(this.compoundFilter);
            }
            return compoundTransliterator;
        }
    }

    public Transliterator get(String str, StringBuffer stringBuffer) {
        Object[] objArrFind = find(str);
        if (objArrFind == null) {
            return null;
        }
        return instantiateEntry(str, objArrFind, stringBuffer);
    }

    public void put(String str, Class<? extends Transliterator> cls, boolean z) {
        registerEntry(str, cls, z);
    }

    public void put(String str, Transliterator.Factory factory, boolean z) {
        registerEntry(str, factory, z);
    }

    public void put(String str, String str2, int i, boolean z) {
        registerEntry(str, new ResourceEntry(str2, i), z);
    }

    public void put(String str, String str2, boolean z) {
        registerEntry(str, new AliasEntry(str2), z);
    }

    public void put(String str, Transliterator transliterator, boolean z) {
        registerEntry(str, transliterator, z);
    }

    public void remove(String str) {
        String[] strArrIDtoSTV = TransliteratorIDParser.IDtoSTV(str);
        String strSTVtoID = TransliteratorIDParser.STVtoID(strArrIDtoSTV[0], strArrIDtoSTV[1], strArrIDtoSTV[2]);
        this.registry.remove(new CaseInsensitiveString(strSTVtoID));
        removeSTV(strArrIDtoSTV[0], strArrIDtoSTV[1], strArrIDtoSTV[2]);
        this.availableIDs.remove(new CaseInsensitiveString(strSTVtoID));
    }

    private static class IDEnumeration implements Enumeration<String> {
        Enumeration<CaseInsensitiveString> en;

        public IDEnumeration(Enumeration<CaseInsensitiveString> enumeration) {
            this.en = enumeration;
        }

        @Override
        public boolean hasMoreElements() {
            return this.en != null && this.en.hasMoreElements();
        }

        @Override
        public String nextElement() {
            return this.en.nextElement().getString();
        }
    }

    public Enumeration<String> getAvailableIDs() {
        return new IDEnumeration(Collections.enumeration(this.availableIDs));
    }

    public Enumeration<String> getAvailableSources() {
        return new IDEnumeration(Collections.enumeration(this.specDAG.keySet()));
    }

    public Enumeration<String> getAvailableTargets(String str) {
        Map<CaseInsensitiveString, List<CaseInsensitiveString>> map = this.specDAG.get(new CaseInsensitiveString(str));
        if (map == null) {
            return new IDEnumeration(null);
        }
        return new IDEnumeration(Collections.enumeration(map.keySet()));
    }

    public Enumeration<String> getAvailableVariants(String str, String str2) {
        CaseInsensitiveString caseInsensitiveString = new CaseInsensitiveString(str);
        CaseInsensitiveString caseInsensitiveString2 = new CaseInsensitiveString(str2);
        Map<CaseInsensitiveString, List<CaseInsensitiveString>> map = this.specDAG.get(caseInsensitiveString);
        if (map == null) {
            return new IDEnumeration(null);
        }
        List<CaseInsensitiveString> list = map.get(caseInsensitiveString2);
        if (list == null) {
            return new IDEnumeration(null);
        }
        return new IDEnumeration(Collections.enumeration(list));
    }

    private void registerEntry(String str, String str2, String str3, Object obj, boolean z) {
        registerEntry(TransliteratorIDParser.STVtoID(str, str2, str3), str.length() == 0 ? ANY : str, str2, str3, obj, z);
    }

    private void registerEntry(String str, Object obj, boolean z) {
        String[] strArrIDtoSTV = TransliteratorIDParser.IDtoSTV(str);
        registerEntry(TransliteratorIDParser.STVtoID(strArrIDtoSTV[0], strArrIDtoSTV[1], strArrIDtoSTV[2]), strArrIDtoSTV[0], strArrIDtoSTV[1], strArrIDtoSTV[2], obj, z);
    }

    private void registerEntry(String str, String str2, String str3, String str4, Object obj, boolean z) {
        Object[] objArr;
        CaseInsensitiveString caseInsensitiveString = new CaseInsensitiveString(str);
        if (obj instanceof Object[]) {
            objArr = (Object[]) obj;
        } else {
            objArr = new Object[]{obj};
        }
        this.registry.put(caseInsensitiveString, objArr);
        if (z) {
            registerSTV(str2, str3, str4);
            if (!this.availableIDs.contains(caseInsensitiveString)) {
                this.availableIDs.add(caseInsensitiveString);
                return;
            }
            return;
        }
        removeSTV(str2, str3, str4);
        this.availableIDs.remove(caseInsensitiveString);
    }

    private void registerSTV(String str, String str2, String str3) {
        CaseInsensitiveString caseInsensitiveString = new CaseInsensitiveString(str);
        CaseInsensitiveString caseInsensitiveString2 = new CaseInsensitiveString(str2);
        CaseInsensitiveString caseInsensitiveString3 = new CaseInsensitiveString(str3);
        Map<CaseInsensitiveString, List<CaseInsensitiveString>> mapSynchronizedMap = this.specDAG.get(caseInsensitiveString);
        if (mapSynchronizedMap == null) {
            mapSynchronizedMap = Collections.synchronizedMap(new HashMap());
            this.specDAG.put(caseInsensitiveString, mapSynchronizedMap);
        }
        List<CaseInsensitiveString> arrayList = mapSynchronizedMap.get(caseInsensitiveString2);
        if (arrayList == null) {
            arrayList = new ArrayList<>();
            mapSynchronizedMap.put(caseInsensitiveString2, arrayList);
        }
        if (!arrayList.contains(caseInsensitiveString3)) {
            if (str3.length() > 0) {
                arrayList.add(caseInsensitiveString3);
            } else {
                arrayList.add(0, caseInsensitiveString3);
            }
        }
    }

    private void removeSTV(String str, String str2, String str3) {
        List<CaseInsensitiveString> list;
        CaseInsensitiveString caseInsensitiveString = new CaseInsensitiveString(str);
        CaseInsensitiveString caseInsensitiveString2 = new CaseInsensitiveString(str2);
        CaseInsensitiveString caseInsensitiveString3 = new CaseInsensitiveString(str3);
        Map<CaseInsensitiveString, List<CaseInsensitiveString>> map = this.specDAG.get(caseInsensitiveString);
        if (map == null || (list = map.get(caseInsensitiveString2)) == null) {
            return;
        }
        list.remove(caseInsensitiveString3);
        if (list.size() == 0) {
            map.remove(caseInsensitiveString2);
            if (map.size() == 0) {
                this.specDAG.remove(caseInsensitiveString);
            }
        }
    }

    private Object[] findInDynamicStore(Spec spec, Spec spec2, String str) {
        return this.registry.get(new CaseInsensitiveString(TransliteratorIDParser.STVtoID(spec.get(), spec2.get(), str)));
    }

    private Object[] findInStaticStore(Spec spec, Spec spec2, String str) {
        Object[] objArrFindInBundle;
        if (spec.isLocale()) {
            objArrFindInBundle = findInBundle(spec, spec2, str, 0);
        } else if (spec2.isLocale()) {
            objArrFindInBundle = findInBundle(spec2, spec, str, 1);
        } else {
            objArrFindInBundle = null;
        }
        if (objArrFindInBundle != null) {
            registerEntry(spec.getTop(), spec2.getTop(), str, objArrFindInBundle, false);
        }
        return objArrFindInBundle;
    }

    private Object[] findInBundle(Spec spec, Spec spec2, String str, int i) {
        int i2;
        ResourceBundle bundle = spec.getBundle();
        if (bundle == null) {
            return null;
        }
        int i3 = 0;
        while (i3 < 2) {
            StringBuilder sb = new StringBuilder();
            if (i3 == 0) {
                sb.append(i == 0 ? "TransliterateTo" : "TransliterateFrom");
            } else {
                sb.append("Transliterate");
            }
            sb.append(spec2.get().toUpperCase(Locale.ENGLISH));
            try {
                String[] stringArray = bundle.getStringArray(sb.toString());
                if (str.length() != 0) {
                    i2 = 0;
                    while (i2 < stringArray.length && !stringArray[i2].equalsIgnoreCase(str)) {
                        i2 += 2;
                    }
                } else {
                    i2 = 0;
                }
                if (i2 < stringArray.length) {
                    return new Object[]{new LocaleEntry(stringArray[i2 + 1], i3 == 0 ? 0 : i)};
                }
                continue;
            } catch (MissingResourceException e) {
            }
            i3++;
        }
        return null;
    }

    private Object[] find(String str) {
        String[] strArrIDtoSTV = TransliteratorIDParser.IDtoSTV(str);
        return find(strArrIDtoSTV[0], strArrIDtoSTV[1], strArrIDtoSTV[2]);
    }

    private Object[] find(String str, String str2, String str3) {
        Spec spec = new Spec(str);
        Spec spec2 = new Spec(str2);
        if (str3.length() != 0) {
            Object[] objArrFindInDynamicStore = findInDynamicStore(spec, spec2, str3);
            if (objArrFindInDynamicStore != null) {
                return objArrFindInDynamicStore;
            }
            Object[] objArrFindInStaticStore = findInStaticStore(spec, spec2, str3);
            if (objArrFindInStaticStore != null) {
                return objArrFindInStaticStore;
            }
        }
        while (true) {
            spec.reset();
            while (true) {
                Object[] objArrFindInDynamicStore2 = findInDynamicStore(spec, spec2, "");
                if (objArrFindInDynamicStore2 != null) {
                    return objArrFindInDynamicStore2;
                }
                Object[] objArrFindInStaticStore2 = findInStaticStore(spec, spec2, "");
                if (objArrFindInStaticStore2 != null) {
                    return objArrFindInStaticStore2;
                }
                if (!spec.hasFallback()) {
                    break;
                }
                spec.next();
            }
            spec2.next();
        }
    }

    private Transliterator instantiateEntry(String str, Object[] objArr, StringBuffer stringBuffer) {
        while (true) {
            Object obj = objArr[0];
            if (obj instanceof RuleBasedTransliterator.Data) {
                return new RuleBasedTransliterator(str, (RuleBasedTransliterator.Data) obj, null);
            }
            if (obj instanceof Class) {
                try {
                    return (Transliterator) ((Class) obj).newInstance();
                } catch (IllegalAccessException | InstantiationException e) {
                    return null;
                }
            }
            if (obj instanceof AliasEntry) {
                stringBuffer.append(((AliasEntry) obj).alias);
                return null;
            }
            if (obj instanceof Transliterator.Factory) {
                return ((Transliterator.Factory) obj).getInstance(str);
            }
            if (obj instanceof CompoundRBTEntry) {
                return ((CompoundRBTEntry) obj).getInstance();
            }
            if (obj instanceof AnyTransliterator) {
                return ((AnyTransliterator) obj).safeClone();
            }
            if (obj instanceof RuleBasedTransliterator) {
                return ((RuleBasedTransliterator) obj).safeClone();
            }
            if (obj instanceof CompoundTransliterator) {
                return ((CompoundTransliterator) obj).safeClone();
            }
            if (obj instanceof Transliterator) {
                return (Transliterator) obj;
            }
            TransliteratorParser transliteratorParser = new TransliteratorParser();
            try {
                ResourceEntry resourceEntry = (ResourceEntry) obj;
                transliteratorParser.parse(resourceEntry.resource, resourceEntry.direction);
            } catch (ClassCastException e2) {
                LocaleEntry localeEntry = (LocaleEntry) obj;
                transliteratorParser.parse(localeEntry.rule, localeEntry.direction);
            }
            if (transliteratorParser.idBlockVector.size() == 0 && transliteratorParser.dataVector.size() == 0) {
                objArr[0] = new AliasEntry("Any-Null");
            } else if (transliteratorParser.idBlockVector.size() == 0 && transliteratorParser.dataVector.size() == 1) {
                objArr[0] = transliteratorParser.dataVector.get(0);
            } else if (transliteratorParser.idBlockVector.size() != 1 || transliteratorParser.dataVector.size() != 0) {
                objArr[0] = new CompoundRBTEntry(str, transliteratorParser.idBlockVector, transliteratorParser.dataVector, transliteratorParser.compoundFilter);
            } else if (transliteratorParser.compoundFilter != null) {
                objArr[0] = new AliasEntry(transliteratorParser.compoundFilter.toPattern(false) + ";" + transliteratorParser.idBlockVector.get(0));
            } else {
                objArr[0] = new AliasEntry(transliteratorParser.idBlockVector.get(0));
            }
        }
    }
}
