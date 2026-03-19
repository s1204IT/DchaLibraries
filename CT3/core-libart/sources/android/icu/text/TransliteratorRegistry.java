package android.icu.text;

import android.icu.impl.ICUResourceBundle;
import android.icu.impl.LocaleUtility;
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

        public Spec(String theSpec) {
            this.top = theSpec;
            this.scriptName = null;
            try {
                int script = UScript.getCodeFromName(this.top);
                int[] s = UScript.getCode(this.top);
                if (s != null) {
                    this.scriptName = UScript.getName(s[0]);
                    if (this.scriptName.equalsIgnoreCase(this.top)) {
                        this.scriptName = null;
                    }
                }
                this.isSpecLocale = false;
                this.res = null;
                if (script == -1) {
                    Locale toploc = LocaleUtility.getLocaleFromName(this.top);
                    this.res = (ICUResourceBundle) UResourceBundle.getBundleInstance("android/icu/impl/data/icudt56b/translit", toploc);
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
            if (this.spec == this.top) {
                return;
            }
            this.spec = this.top;
            this.isSpecLocale = this.res != null;
            setupNext();
        }

        private void setupNext() {
            this.isNextLocale = false;
            if (this.isSpecLocale) {
                this.nextSpec = this.spec;
                int i = this.nextSpec.lastIndexOf(95);
                if (i > 0) {
                    this.nextSpec = this.spec.substring(0, i);
                    this.isNextLocale = true;
                    return;
                } else {
                    this.nextSpec = this.scriptName;
                    return;
                }
            }
            if (this.nextSpec != this.scriptName) {
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
            if (this.res == null || !this.res.getULocale().toString().equals(this.spec)) {
                return null;
            }
            return this.res;
        }

        public String getTop() {
            return this.top;
        }
    }

    static class ResourceEntry {
        public int direction;
        public String encoding;
        public String resource;

        public ResourceEntry(String n, String enc, int d) {
            this.resource = n;
            this.encoding = enc;
            this.direction = d;
        }
    }

    static class LocaleEntry {
        public int direction;
        public String rule;

        public LocaleEntry(String r, int d) {
            this.rule = r;
            this.direction = d;
        }
    }

    static class AliasEntry {
        public String alias;

        public AliasEntry(String a) {
            this.alias = a;
        }
    }

    static class CompoundRBTEntry {
        private String ID;
        private UnicodeSet compoundFilter;
        private List<RuleBasedTransliterator.Data> dataVector;
        private List<String> idBlockVector;

        public CompoundRBTEntry(String theID, List<String> theIDBlockVector, List<RuleBasedTransliterator.Data> theDataVector, UnicodeSet theCompoundFilter) {
            this.ID = theID;
            this.idBlockVector = theIDBlockVector;
            this.dataVector = theDataVector;
            this.compoundFilter = theCompoundFilter;
        }

        public Transliterator getInstance() {
            int passNumber;
            List<Transliterator> transliterators = new ArrayList<>();
            int limit = Math.max(this.idBlockVector.size(), this.dataVector.size());
            int i = 0;
            int passNumber2 = 1;
            while (i < limit) {
                if (i < this.idBlockVector.size()) {
                    String idBlock = this.idBlockVector.get(i);
                    if (idBlock.length() > 0) {
                        transliterators.add(Transliterator.getInstance(idBlock));
                    }
                }
                if (i < this.dataVector.size()) {
                    RuleBasedTransliterator.Data data = this.dataVector.get(i);
                    passNumber = passNumber2 + 1;
                    transliterators.add(new RuleBasedTransliterator("%Pass" + passNumber2, data, null));
                } else {
                    passNumber = passNumber2;
                }
                i++;
                passNumber2 = passNumber;
            }
            Transliterator t = new CompoundTransliterator(transliterators, passNumber2 - 1);
            t.setID(this.ID);
            if (this.compoundFilter != null) {
                t.setFilter(this.compoundFilter);
            }
            return t;
        }
    }

    public Transliterator get(String ID, StringBuffer aliasReturn) {
        Object[] entry = find(ID);
        if (entry == null) {
            return null;
        }
        return instantiateEntry(ID, entry, aliasReturn);
    }

    public void put(String ID, Class<? extends Transliterator> transliteratorSubclass, boolean visible) {
        registerEntry(ID, transliteratorSubclass, visible);
    }

    public void put(String ID, Transliterator.Factory factory, boolean visible) {
        registerEntry(ID, factory, visible);
    }

    public void put(String ID, String resourceName, String encoding, int dir, boolean visible) {
        registerEntry(ID, new ResourceEntry(resourceName, encoding, dir), visible);
    }

    public void put(String ID, String alias, boolean visible) {
        registerEntry(ID, new AliasEntry(alias), visible);
    }

    public void put(String ID, Transliterator trans, boolean visible) {
        registerEntry(ID, trans, visible);
    }

    public void remove(String ID) {
        String[] stv = TransliteratorIDParser.IDtoSTV(ID);
        String id = TransliteratorIDParser.STVtoID(stv[0], stv[1], stv[2]);
        this.registry.remove(new CaseInsensitiveString(id));
        removeSTV(stv[0], stv[1], stv[2]);
        this.availableIDs.remove(new CaseInsensitiveString(id));
    }

    private static class IDEnumeration implements Enumeration<String> {
        Enumeration<CaseInsensitiveString> en;

        public IDEnumeration(Enumeration<CaseInsensitiveString> e) {
            this.en = e;
        }

        @Override
        public boolean hasMoreElements() {
            if (this.en != null) {
                return this.en.hasMoreElements();
            }
            return false;
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

    public Enumeration<String> getAvailableTargets(String source) {
        CaseInsensitiveString cisrc = new CaseInsensitiveString(source);
        Map<CaseInsensitiveString, List<CaseInsensitiveString>> targets = this.specDAG.get(cisrc);
        if (targets == null) {
            return new IDEnumeration(null);
        }
        return new IDEnumeration(Collections.enumeration(targets.keySet()));
    }

    public Enumeration<String> getAvailableVariants(String source, String target) {
        CaseInsensitiveString cisrc = new CaseInsensitiveString(source);
        CaseInsensitiveString citrg = new CaseInsensitiveString(target);
        Map<CaseInsensitiveString, List<CaseInsensitiveString>> targets = this.specDAG.get(cisrc);
        if (targets == null) {
            return new IDEnumeration(null);
        }
        List<CaseInsensitiveString> variants = targets.get(citrg);
        if (variants == null) {
            return new IDEnumeration(null);
        }
        return new IDEnumeration(Collections.enumeration(variants));
    }

    private void registerEntry(String source, String target, String variant, Object entry, boolean visible) {
        String s = source;
        if (source.length() == 0) {
            s = ANY;
        }
        String ID = TransliteratorIDParser.STVtoID(source, target, variant);
        registerEntry(ID, s, target, variant, entry, visible);
    }

    private void registerEntry(String ID, Object entry, boolean visible) {
        String[] stv = TransliteratorIDParser.IDtoSTV(ID);
        String id = TransliteratorIDParser.STVtoID(stv[0], stv[1], stv[2]);
        registerEntry(id, stv[0], stv[1], stv[2], entry, visible);
    }

    private void registerEntry(String str, String str2, String str3, String str4, Object obj, boolean z) {
        CaseInsensitiveString caseInsensitiveString = new CaseInsensitiveString(str);
        this.registry.put(caseInsensitiveString, (Object[]) (obj instanceof Object[] ? obj : new Object[]{obj}));
        if (z) {
            registerSTV(str2, str3, str4);
            if (this.availableIDs.contains(caseInsensitiveString)) {
                return;
            }
            this.availableIDs.add(caseInsensitiveString);
            return;
        }
        removeSTV(str2, str3, str4);
        this.availableIDs.remove(caseInsensitiveString);
    }

    private void registerSTV(String source, String target, String variant) {
        CaseInsensitiveString cisrc = new CaseInsensitiveString(source);
        CaseInsensitiveString citrg = new CaseInsensitiveString(target);
        CaseInsensitiveString civar = new CaseInsensitiveString(variant);
        Map<CaseInsensitiveString, List<CaseInsensitiveString>> targets = this.specDAG.get(cisrc);
        if (targets == null) {
            targets = Collections.synchronizedMap(new HashMap());
            this.specDAG.put(cisrc, targets);
        }
        List<CaseInsensitiveString> variants = targets.get(citrg);
        if (variants == null) {
            variants = new ArrayList<>();
            targets.put(citrg, variants);
        }
        if (variants.contains(civar)) {
            return;
        }
        if (variant.length() > 0) {
            variants.add(civar);
        } else {
            variants.add(0, civar);
        }
    }

    private void removeSTV(String source, String target, String variant) {
        List<CaseInsensitiveString> variants;
        CaseInsensitiveString cisrc = new CaseInsensitiveString(source);
        CaseInsensitiveString citrg = new CaseInsensitiveString(target);
        CaseInsensitiveString civar = new CaseInsensitiveString(variant);
        Map<CaseInsensitiveString, List<CaseInsensitiveString>> targets = this.specDAG.get(cisrc);
        if (targets == null || (variants = targets.get(citrg)) == null) {
            return;
        }
        variants.remove(civar);
        if (variants.size() != 0) {
            return;
        }
        targets.remove(citrg);
        if (targets.size() != 0) {
            return;
        }
        this.specDAG.remove(cisrc);
    }

    private Object[] findInDynamicStore(Spec src, Spec trg, String variant) {
        String ID = TransliteratorIDParser.STVtoID(src.get(), trg.get(), variant);
        return this.registry.get(new CaseInsensitiveString(ID));
    }

    private Object[] findInStaticStore(Spec src, Spec trg, String variant) {
        Object[] entry = null;
        if (src.isLocale()) {
            entry = findInBundle(src, trg, variant, 0);
        } else if (trg.isLocale()) {
            entry = findInBundle(trg, src, variant, 1);
        }
        if (entry != null) {
            registerEntry(src.getTop(), trg.getTop(), variant, entry, false);
        }
        return entry;
    }

    private Object[] findInBundle(Spec specToOpen, Spec specToFind, String variant, int direction) {
        ResourceBundle res = specToOpen.getBundle();
        if (res == null) {
            return null;
        }
        int pass = 0;
        while (pass < 2) {
            StringBuilder tag = new StringBuilder();
            if (pass == 0) {
                tag.append(direction == 0 ? "TransliterateTo" : "TransliterateFrom");
            } else {
                tag.append("Transliterate");
            }
            tag.append(specToFind.get().toUpperCase(Locale.ENGLISH));
            try {
                String[] subres = res.getStringArray(tag.toString());
                int i = 0;
                if (variant.length() != 0) {
                    i = 0;
                    while (i < subres.length && !subres[i].equalsIgnoreCase(variant)) {
                        i += 2;
                    }
                }
                if (i < subres.length) {
                    int dir = pass == 0 ? 0 : direction;
                    return new Object[]{new LocaleEntry(subres[i + 1], dir)};
                }
                continue;
            } catch (MissingResourceException e) {
            }
            pass++;
        }
        return null;
    }

    private Object[] find(String ID) {
        String[] stv = TransliteratorIDParser.IDtoSTV(ID);
        return find(stv[0], stv[1], stv[2]);
    }

    private Object[] find(String source, String target, String variant) {
        Spec src = new Spec(source);
        Spec trg = new Spec(target);
        if (variant.length() != 0) {
            Object[] entry = findInDynamicStore(src, trg, variant);
            if (entry != null) {
                return entry;
            }
            Object[] entry2 = findInStaticStore(src, trg, variant);
            if (entry2 != null) {
                return entry2;
            }
        }
        while (true) {
            src.reset();
            while (true) {
                Object[] entry3 = findInDynamicStore(src, trg, "");
                if (entry3 != null) {
                    return entry3;
                }
                Object[] entry4 = findInStaticStore(src, trg, "");
                if (entry4 != null) {
                    return entry4;
                }
                if (!src.hasFallback()) {
                    break;
                }
                src.next();
            }
            trg.next();
        }
    }

    private android.icu.text.Transliterator instantiateEntry(java.lang.String r17, java.lang.Object[] r18, java.lang.StringBuffer r19) {
        throw new UnsupportedOperationException("Method not decompiled: android.icu.text.TransliteratorRegistry.instantiateEntry(java.lang.String, java.lang.Object[], java.lang.StringBuffer):android.icu.text.Transliterator");
    }
}
