package android.icu.text;

import android.icu.impl.CSCharacterIterator;
import android.icu.impl.CacheValue;
import android.icu.impl.ICUDebug;
import android.icu.util.ICUCloneNotSupportedException;
import android.icu.util.ULocale;
import java.text.CharacterIterator;
import java.util.Locale;
import java.util.MissingResourceException;

public abstract class BreakIterator implements Cloneable {
    public static final int DONE = -1;
    public static final int KIND_CHARACTER = 0;
    private static final int KIND_COUNT = 5;
    public static final int KIND_LINE = 2;
    public static final int KIND_SENTENCE = 3;
    public static final int KIND_TITLE = 4;
    public static final int KIND_WORD = 1;
    public static final int WORD_IDEO = 400;
    public static final int WORD_IDEO_LIMIT = 500;
    public static final int WORD_KANA = 300;
    public static final int WORD_KANA_LIMIT = 400;
    public static final int WORD_LETTER = 200;
    public static final int WORD_LETTER_LIMIT = 300;
    public static final int WORD_NONE = 0;
    public static final int WORD_NONE_LIMIT = 100;
    public static final int WORD_NUMBER = 100;
    public static final int WORD_NUMBER_LIMIT = 200;
    private static BreakIteratorServiceShim shim;
    private ULocale actualLocale;
    private ULocale validLocale;
    private static final boolean DEBUG = ICUDebug.enabled("breakiterator");
    private static final CacheValue<?>[] iterCache = new CacheValue[5];

    public abstract int current();

    public abstract int first();

    public abstract int following(int i);

    public abstract CharacterIterator getText();

    public abstract int last();

    public abstract int next();

    public abstract int next(int i);

    public abstract int previous();

    public abstract void setText(CharacterIterator characterIterator);

    protected BreakIterator() {
    }

    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new ICUCloneNotSupportedException(e);
        }
    }

    public int preceding(int i) {
        int iFollowing = following(i);
        while (iFollowing >= i && iFollowing != -1) {
            iFollowing = previous();
        }
        return iFollowing;
    }

    public boolean isBoundary(int i) {
        return i == 0 || following(i + (-1)) == i;
    }

    public int getRuleStatus() {
        return 0;
    }

    public int getRuleStatusVec(int[] iArr) {
        if (iArr != null && iArr.length > 0) {
            iArr[0] = 0;
            return 1;
        }
        return 1;
    }

    public void setText(String str) {
        setText(new java.text.StringCharacterIterator(str));
    }

    public void setText(CharSequence charSequence) {
        setText(new CSCharacterIterator(charSequence));
    }

    public static BreakIterator getWordInstance() {
        return getWordInstance(ULocale.getDefault());
    }

    public static BreakIterator getWordInstance(Locale locale) {
        return getBreakInstance(ULocale.forLocale(locale), 1);
    }

    public static BreakIterator getWordInstance(ULocale uLocale) {
        return getBreakInstance(uLocale, 1);
    }

    public static BreakIterator getLineInstance() {
        return getLineInstance(ULocale.getDefault());
    }

    public static BreakIterator getLineInstance(Locale locale) {
        return getBreakInstance(ULocale.forLocale(locale), 2);
    }

    public static BreakIterator getLineInstance(ULocale uLocale) {
        return getBreakInstance(uLocale, 2);
    }

    public static BreakIterator getCharacterInstance() {
        return getCharacterInstance(ULocale.getDefault());
    }

    public static BreakIterator getCharacterInstance(Locale locale) {
        return getBreakInstance(ULocale.forLocale(locale), 0);
    }

    public static BreakIterator getCharacterInstance(ULocale uLocale) {
        return getBreakInstance(uLocale, 0);
    }

    public static BreakIterator getSentenceInstance() {
        return getSentenceInstance(ULocale.getDefault());
    }

    public static BreakIterator getSentenceInstance(Locale locale) {
        return getBreakInstance(ULocale.forLocale(locale), 3);
    }

    public static BreakIterator getSentenceInstance(ULocale uLocale) {
        return getBreakInstance(uLocale, 3);
    }

    public static BreakIterator getTitleInstance() {
        return getTitleInstance(ULocale.getDefault());
    }

    public static BreakIterator getTitleInstance(Locale locale) {
        return getBreakInstance(ULocale.forLocale(locale), 4);
    }

    public static BreakIterator getTitleInstance(ULocale uLocale) {
        return getBreakInstance(uLocale, 4);
    }

    public static Object registerInstance(BreakIterator breakIterator, Locale locale, int i) {
        return registerInstance(breakIterator, ULocale.forLocale(locale), i);
    }

    public static Object registerInstance(BreakIterator breakIterator, ULocale uLocale, int i) {
        BreakIteratorCache breakIteratorCache;
        if (iterCache[i] != null && (breakIteratorCache = (BreakIteratorCache) iterCache[i].get()) != null && breakIteratorCache.getLocale().equals(uLocale)) {
            iterCache[i] = null;
        }
        return getShim().registerInstance(breakIterator, uLocale, i);
    }

    public static boolean unregister(Object obj) {
        if (obj == null) {
            throw new IllegalArgumentException("registry key must not be null");
        }
        if (shim == null) {
            return false;
        }
        for (int i = 0; i < 5; i++) {
            iterCache[i] = null;
        }
        return shim.unregister(obj);
    }

    @Deprecated
    public static BreakIterator getBreakInstance(ULocale uLocale, int i) {
        BreakIteratorCache breakIteratorCache;
        if (uLocale == null) {
            throw new NullPointerException("Specified locale is null");
        }
        if (iterCache[i] != null && (breakIteratorCache = (BreakIteratorCache) iterCache[i].get()) != null && breakIteratorCache.getLocale().equals(uLocale)) {
            return breakIteratorCache.createBreakInstance();
        }
        BreakIterator breakIteratorCreateBreakIterator = getShim().createBreakIterator(uLocale, i);
        iterCache[i] = CacheValue.getInstance(new BreakIteratorCache(uLocale, breakIteratorCreateBreakIterator));
        if (breakIteratorCreateBreakIterator instanceof RuleBasedBreakIterator) {
            ((RuleBasedBreakIterator) breakIteratorCreateBreakIterator).setBreakType(i);
        }
        return breakIteratorCreateBreakIterator;
    }

    public static synchronized Locale[] getAvailableLocales() {
        return getShim().getAvailableLocales();
    }

    public static synchronized ULocale[] getAvailableULocales() {
        return getShim().getAvailableULocales();
    }

    private static final class BreakIteratorCache {
        private BreakIterator iter;
        private ULocale where;

        BreakIteratorCache(ULocale uLocale, BreakIterator breakIterator) {
            this.where = uLocale;
            this.iter = (BreakIterator) breakIterator.clone();
        }

        ULocale getLocale() {
            return this.where;
        }

        BreakIterator createBreakInstance() {
            return (BreakIterator) this.iter.clone();
        }
    }

    static abstract class BreakIteratorServiceShim {
        public abstract BreakIterator createBreakIterator(ULocale uLocale, int i);

        public abstract Locale[] getAvailableLocales();

        public abstract ULocale[] getAvailableULocales();

        public abstract Object registerInstance(BreakIterator breakIterator, ULocale uLocale, int i);

        public abstract boolean unregister(Object obj);

        BreakIteratorServiceShim() {
        }
    }

    private static BreakIteratorServiceShim getShim() {
        if (shim == null) {
            try {
                shim = (BreakIteratorServiceShim) Class.forName("android.icu.text.BreakIteratorFactory").newInstance();
            } catch (MissingResourceException e) {
                throw e;
            } catch (Exception e2) {
                if (DEBUG) {
                    e2.printStackTrace();
                }
                throw new RuntimeException(e2.getMessage());
            }
        }
        return shim;
    }

    public final ULocale getLocale(ULocale.Type type) {
        return type == ULocale.ACTUAL_LOCALE ? this.actualLocale : this.validLocale;
    }

    final void setLocale(ULocale uLocale, ULocale uLocale2) {
        if ((uLocale == null) != (uLocale2 == null)) {
            throw new IllegalArgumentException();
        }
        this.validLocale = uLocale;
        this.actualLocale = uLocale2;
    }
}
