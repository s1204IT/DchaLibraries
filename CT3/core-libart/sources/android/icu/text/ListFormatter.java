package android.icu.text;

import android.icu.impl.ICUCache;
import android.icu.impl.ICUResourceBundle;
import android.icu.impl.SimpleCache;
import android.icu.impl.SimplePatternFormatter;
import android.icu.util.ULocale;
import android.icu.util.UResourceBundle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;

public final class ListFormatter {
    static Cache cache = new Cache(null);
    private final String end;
    private final ULocale locale;
    private final String middle;
    private final String start;
    private final String two;

    ListFormatter(String two, String start, String middle, String end, ULocale locale, ListFormatter listFormatter) {
        this(two, start, middle, end, locale);
    }

    @Deprecated
    public enum Style {
        STANDARD("standard"),
        DURATION("unit"),
        DURATION_SHORT("unit-short"),
        DURATION_NARROW("unit-narrow");

        private final String name;

        public static Style[] valuesCustom() {
            return values();
        }

        Style(String name) {
            this.name = name;
        }

        @Deprecated
        public String getName() {
            return this.name;
        }
    }

    @Deprecated
    public ListFormatter(String two, String start, String middle, String end) {
        this(compilePattern(two, new StringBuilder()), compilePattern(start, new StringBuilder()), compilePattern(middle, new StringBuilder()), compilePattern(end, new StringBuilder()), null);
    }

    private ListFormatter(String two, String start, String middle, String end, ULocale locale) {
        this.two = two;
        this.start = start;
        this.middle = middle;
        this.end = end;
        this.locale = locale;
    }

    private static String compilePattern(String pattern, StringBuilder sb) {
        return SimplePatternFormatter.compileToStringMinMaxPlaceholders(pattern, sb, 2, 2);
    }

    public static ListFormatter getInstance(ULocale locale) {
        return getInstance(locale, Style.STANDARD);
    }

    public static ListFormatter getInstance(Locale locale) {
        return getInstance(ULocale.forLocale(locale), Style.STANDARD);
    }

    @Deprecated
    public static ListFormatter getInstance(ULocale locale, Style style) {
        return cache.get(locale, style.getName());
    }

    public static ListFormatter getInstance() {
        return getInstance(ULocale.getDefault(ULocale.Category.FORMAT));
    }

    public String format(Object... items) {
        return format(Arrays.asList(items));
    }

    public String format(Collection<?> items) {
        return format(items, -1).toString();
    }

    FormattedListBuilder format(Collection<?> items, int index) {
        Iterator<?> it = items.iterator();
        int count = items.size();
        switch (count) {
            case 0:
                return new FormattedListBuilder("", false);
            case 1:
                return new FormattedListBuilder(it.next(), index == 0);
            case 2:
                return new FormattedListBuilder(it.next(), index == 0).append(this.two, it.next(), index == 1);
            default:
                FormattedListBuilder builder = new FormattedListBuilder(it.next(), index == 0);
                builder.append(this.start, it.next(), index == 1);
                int idx = 2;
                while (idx < count - 1) {
                    builder.append(this.middle, it.next(), index == idx);
                    idx++;
                }
                return builder.append(this.end, it.next(), index == count + (-1));
        }
    }

    public String getPatternForNumItems(int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("count must be > 0");
        }
        ArrayList<String> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(String.format("{%d}", Integer.valueOf(i)));
        }
        return format(list);
    }

    @Deprecated
    public ULocale getLocale() {
        return this.locale;
    }

    static class FormattedListBuilder {
        private StringBuilder current;
        private int offset;

        public FormattedListBuilder(Object start, boolean recordOffset) {
            this.current = new StringBuilder(start.toString());
            this.offset = recordOffset ? 0 : -1;
        }

        public FormattedListBuilder append(String pattern, Object next, boolean recordOffset) {
            int[] iArr = (recordOffset || offsetRecorded()) ? new int[2] : null;
            SimplePatternFormatter.formatAndReplace(pattern, this.current, iArr, this.current, next.toString());
            if (iArr != null) {
                if (iArr[0] == -1 || iArr[1] == -1) {
                    throw new IllegalArgumentException("{0} or {1} missing from pattern " + pattern);
                }
                if (recordOffset) {
                    this.offset = iArr[1];
                } else {
                    this.offset += iArr[0];
                }
            }
            return this;
        }

        public String toString() {
            return this.current.toString();
        }

        public int getOffset() {
            return this.offset;
        }

        private boolean offsetRecorded() {
            return this.offset >= 0;
        }
    }

    private static class Cache {
        private final ICUCache<String, ListFormatter> cache;

        Cache(Cache cache) {
            this();
        }

        private Cache() {
            this.cache = new SimpleCache();
        }

        public ListFormatter get(ULocale locale, String style) {
            String key = String.format("%s:%s", locale.toString(), style);
            ListFormatter result = this.cache.get(key);
            if (result == null) {
                ListFormatter result2 = load(locale, style);
                this.cache.put(key, result2);
                return result2;
            }
            return result;
        }

        private static ListFormatter load(ULocale ulocale, String style) {
            ICUResourceBundle r = (ICUResourceBundle) UResourceBundle.getBundleInstance("android/icu/impl/data/icudt56b", ulocale);
            StringBuilder sb = new StringBuilder();
            return new ListFormatter(ListFormatter.compilePattern(r.getWithFallback("listPattern/" + style + "/2").getString(), sb), ListFormatter.compilePattern(r.getWithFallback("listPattern/" + style + "/start").getString(), sb), ListFormatter.compilePattern(r.getWithFallback("listPattern/" + style + "/middle").getString(), sb), ListFormatter.compilePattern(r.getWithFallback("listPattern/" + style + "/end").getString(), sb), ulocale, null);
        }
    }
}
