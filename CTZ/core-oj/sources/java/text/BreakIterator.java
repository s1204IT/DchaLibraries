package java.text;

import java.util.Locale;

public abstract class BreakIterator implements Cloneable {
    public static final int DONE = -1;

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
            throw new InternalError(e);
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
        if (i == 0) {
            return true;
        }
        int iFollowing = following(i - 1);
        if (iFollowing != -1) {
            return iFollowing == i;
        }
        throw new IllegalArgumentException();
    }

    public void setText(String str) {
        setText(new StringCharacterIterator(str));
    }

    public static BreakIterator getWordInstance() {
        return getWordInstance(Locale.getDefault());
    }

    public static BreakIterator getWordInstance(Locale locale) {
        return new IcuIteratorWrapper(android.icu.text.BreakIterator.getWordInstance(locale));
    }

    public static BreakIterator getLineInstance() {
        return getLineInstance(Locale.getDefault());
    }

    public static BreakIterator getLineInstance(Locale locale) {
        return new IcuIteratorWrapper(android.icu.text.BreakIterator.getLineInstance(locale));
    }

    public static BreakIterator getCharacterInstance() {
        return getCharacterInstance(Locale.getDefault());
    }

    public static BreakIterator getCharacterInstance(Locale locale) {
        return new IcuIteratorWrapper(android.icu.text.BreakIterator.getCharacterInstance(locale));
    }

    public static BreakIterator getSentenceInstance() {
        return getSentenceInstance(Locale.getDefault());
    }

    public static BreakIterator getSentenceInstance(Locale locale) {
        return new IcuIteratorWrapper(android.icu.text.BreakIterator.getSentenceInstance(locale));
    }

    public static synchronized Locale[] getAvailableLocales() {
        return android.icu.text.BreakIterator.getAvailableLocales();
    }
}
