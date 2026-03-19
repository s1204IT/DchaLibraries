package android.icu.text;

import android.icu.impl.ICUConfig;
import android.icu.lang.UScript;
import android.icu.text.DisplayContext;
import android.icu.util.ULocale;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public abstract class LocaleDisplayNames {
    private static final Method FACTORY_DIALECTHANDLING;
    private static final Method FACTORY_DISPLAYCONTEXT;

    public abstract DisplayContext getContext(DisplayContext.Type type);

    public abstract DialectHandling getDialectHandling();

    public abstract ULocale getLocale();

    public abstract List<UiListItem> getUiListCompareWholeItems(Set<ULocale> set, Comparator<UiListItem> comparator);

    public abstract String keyDisplayName(String str);

    public abstract String keyValueDisplayName(String str, String str2);

    public abstract String languageDisplayName(String str);

    public abstract String localeDisplayName(ULocale uLocale);

    public abstract String localeDisplayName(String str);

    public abstract String localeDisplayName(Locale locale);

    public abstract String regionDisplayName(String str);

    public abstract String scriptDisplayName(int i);

    public abstract String scriptDisplayName(String str);

    public abstract String variantDisplayName(String str);

    public enum DialectHandling {
        STANDARD_NAMES,
        DIALECT_NAMES;

        public static DialectHandling[] valuesCustom() {
            return values();
        }
    }

    public static LocaleDisplayNames getInstance(ULocale locale) {
        return getInstance(locale, DialectHandling.STANDARD_NAMES);
    }

    public static LocaleDisplayNames getInstance(Locale locale) {
        return getInstance(ULocale.forLocale(locale));
    }

    public static LocaleDisplayNames getInstance(ULocale locale, DialectHandling dialectHandling) {
        LastResortLocaleDisplayNames lastResortLocaleDisplayNames = null;
        LocaleDisplayNames result = null;
        if (FACTORY_DIALECTHANDLING != null) {
            try {
                result = (LocaleDisplayNames) FACTORY_DIALECTHANDLING.invoke(null, locale, dialectHandling);
            } catch (IllegalAccessException e) {
            } catch (InvocationTargetException e2) {
            }
        }
        if (result == null) {
            return new LastResortLocaleDisplayNames(locale, dialectHandling, lastResortLocaleDisplayNames);
        }
        return result;
    }

    public static LocaleDisplayNames getInstance(ULocale locale, DisplayContext... contexts) {
        LastResortLocaleDisplayNames lastResortLocaleDisplayNames = null;
        LocaleDisplayNames result = null;
        if (FACTORY_DISPLAYCONTEXT != null) {
            try {
                result = (LocaleDisplayNames) FACTORY_DISPLAYCONTEXT.invoke(null, locale, contexts);
            } catch (IllegalAccessException e) {
            } catch (InvocationTargetException e2) {
            }
        }
        if (result == null) {
            return new LastResortLocaleDisplayNames(locale, contexts, lastResortLocaleDisplayNames);
        }
        return result;
    }

    public static LocaleDisplayNames getInstance(Locale locale, DisplayContext... contexts) {
        return getInstance(ULocale.forLocale(locale), contexts);
    }

    @Deprecated
    public String scriptDisplayNameInContext(String script) {
        return scriptDisplayName(script);
    }

    public List<UiListItem> getUiList(Set<ULocale> localeSet, boolean inSelf, Comparator<Object> collator) {
        return getUiListCompareWholeItems(localeSet, UiListItem.getComparator(collator, inSelf));
    }

    public static class UiListItem {
        public final ULocale minimized;
        public final ULocale modified;
        public final String nameInDisplayLocale;
        public final String nameInSelf;

        public UiListItem(ULocale minimized, ULocale modified, String nameInDisplayLocale, String nameInSelf) {
            this.minimized = minimized;
            this.modified = modified;
            this.nameInDisplayLocale = nameInDisplayLocale;
            this.nameInSelf = nameInSelf;
        }

        public boolean equals(Object obj) {
            UiListItem other = (UiListItem) obj;
            if (this.nameInDisplayLocale.equals(other.nameInDisplayLocale) && this.nameInSelf.equals(other.nameInSelf) && this.minimized.equals(other.minimized)) {
                return this.modified.equals(other.modified);
            }
            return false;
        }

        public int hashCode() {
            return this.modified.hashCode() ^ this.nameInDisplayLocale.hashCode();
        }

        public String toString() {
            return "{" + this.minimized + ", " + this.modified + ", " + this.nameInDisplayLocale + ", " + this.nameInSelf + "}";
        }

        public static Comparator<UiListItem> getComparator(Comparator<Object> comparator, boolean inSelf) {
            return new UiListItemComparator(comparator, inSelf);
        }

        private static class UiListItemComparator implements Comparator<UiListItem> {
            private final Comparator<Object> collator;
            private final boolean useSelf;

            UiListItemComparator(Comparator<Object> collator, boolean useSelf) {
                this.collator = collator;
                this.useSelf = useSelf;
            }

            @Override
            public int compare(UiListItem o1, UiListItem o2) {
                int result = this.useSelf ? this.collator.compare(o1.nameInSelf, o2.nameInSelf) : this.collator.compare(o1.nameInDisplayLocale, o2.nameInDisplayLocale);
                if (result != 0) {
                    return result;
                }
                return o1.modified.compareTo(o2.modified);
            }
        }
    }

    @Deprecated
    protected LocaleDisplayNames() {
    }

    static {
        String implClassName = ICUConfig.get("android.icu.text.LocaleDisplayNames.impl", "android.icu.impl.LocaleDisplayNamesImpl");
        Method factoryDialectHandling = null;
        Method factoryDisplayContext = null;
        try {
            Class<?> implClass = Class.forName(implClassName);
            try {
                factoryDialectHandling = implClass.getMethod("getInstance", ULocale.class, DialectHandling.class);
            } catch (NoSuchMethodException e) {
            }
            try {
                factoryDisplayContext = implClass.getMethod("getInstance", ULocale.class, DisplayContext[].class);
            } catch (NoSuchMethodException e2) {
            }
        } catch (ClassNotFoundException e3) {
        }
        FACTORY_DIALECTHANDLING = factoryDialectHandling;
        FACTORY_DISPLAYCONTEXT = factoryDisplayContext;
    }

    private static class LastResortLocaleDisplayNames extends LocaleDisplayNames {
        private DisplayContext[] contexts;
        private ULocale locale;

        LastResortLocaleDisplayNames(ULocale locale, DialectHandling dialectHandling, LastResortLocaleDisplayNames lastResortLocaleDisplayNames) {
            this(locale, dialectHandling);
        }

        LastResortLocaleDisplayNames(ULocale locale, DisplayContext[] contexts, LastResortLocaleDisplayNames lastResortLocaleDisplayNames) {
            this(locale, contexts);
        }

        private LastResortLocaleDisplayNames(ULocale locale, DialectHandling dialectHandling) {
            this.locale = locale;
            DisplayContext context = dialectHandling == DialectHandling.DIALECT_NAMES ? DisplayContext.DIALECT_NAMES : DisplayContext.STANDARD_NAMES;
            this.contexts = new DisplayContext[]{context};
        }

        private LastResortLocaleDisplayNames(ULocale locale, DisplayContext... contexts) {
            this.locale = locale;
            this.contexts = new DisplayContext[contexts.length];
            System.arraycopy(contexts, 0, this.contexts, 0, contexts.length);
        }

        @Override
        public ULocale getLocale() {
            return this.locale;
        }

        @Override
        public DialectHandling getDialectHandling() {
            DialectHandling result = DialectHandling.STANDARD_NAMES;
            for (DisplayContext context : this.contexts) {
                if (context.type() == DisplayContext.Type.DIALECT_HANDLING && context.value() == DisplayContext.DIALECT_NAMES.ordinal()) {
                    DialectHandling result2 = DialectHandling.DIALECT_NAMES;
                    return result2;
                }
            }
            return result;
        }

        @Override
        public DisplayContext getContext(DisplayContext.Type type) {
            DisplayContext result = DisplayContext.STANDARD_NAMES;
            for (DisplayContext context : this.contexts) {
                if (context.type() == type) {
                    return context;
                }
            }
            return result;
        }

        @Override
        public String localeDisplayName(ULocale locale) {
            return locale.getName();
        }

        @Override
        public String localeDisplayName(Locale locale) {
            return ULocale.forLocale(locale).getName();
        }

        @Override
        public String localeDisplayName(String localeId) {
            return new ULocale(localeId).getName();
        }

        @Override
        public String languageDisplayName(String lang) {
            return lang;
        }

        @Override
        public String scriptDisplayName(String script) {
            return script;
        }

        @Override
        public String scriptDisplayName(int scriptCode) {
            return UScript.getShortName(scriptCode);
        }

        @Override
        public String regionDisplayName(String region) {
            return region;
        }

        @Override
        public String variantDisplayName(String variant) {
            return variant;
        }

        @Override
        public String keyDisplayName(String key) {
            return key;
        }

        @Override
        public String keyValueDisplayName(String key, String value) {
            return value;
        }

        @Override
        public List<UiListItem> getUiListCompareWholeItems(Set<ULocale> localeSet, Comparator<UiListItem> comparator) {
            return Collections.emptyList();
        }
    }
}
