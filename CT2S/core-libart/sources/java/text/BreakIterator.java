package java.text;

import java.util.Locale;
import libcore.icu.ICU;
import libcore.icu.NativeBreakIterator;

public abstract class BreakIterator implements Cloneable {
    public static final int DONE = -1;
    NativeBreakIterator wrapped;

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

    BreakIterator(NativeBreakIterator iterator) {
        this.wrapped = iterator;
    }

    public static Locale[] getAvailableLocales() {
        return ICU.getAvailableBreakIteratorLocales();
    }

    public static BreakIterator getCharacterInstance() {
        return getCharacterInstance(Locale.getDefault());
    }

    public static BreakIterator getCharacterInstance(Locale locale) {
        return new RuleBasedBreakIterator(NativeBreakIterator.getCharacterInstance(locale));
    }

    public static BreakIterator getLineInstance() {
        return getLineInstance(Locale.getDefault());
    }

    public static BreakIterator getLineInstance(Locale locale) {
        return new RuleBasedBreakIterator(NativeBreakIterator.getLineInstance(locale));
    }

    public static BreakIterator getSentenceInstance() {
        return getSentenceInstance(Locale.getDefault());
    }

    public static BreakIterator getSentenceInstance(Locale locale) {
        return new RuleBasedBreakIterator(NativeBreakIterator.getSentenceInstance(locale));
    }

    public static BreakIterator getWordInstance() {
        return getWordInstance(Locale.getDefault());
    }

    public static BreakIterator getWordInstance(Locale locale) {
        return new RuleBasedBreakIterator(NativeBreakIterator.getWordInstance(locale));
    }

    public boolean isBoundary(int offset) {
        return this.wrapped.isBoundary(offset);
    }

    public int preceding(int offset) {
        return this.wrapped.preceding(offset);
    }

    public void setText(String newText) {
        if (newText == null) {
            throw new NullPointerException("newText == null");
        }
        this.wrapped.setText(newText);
    }

    public Object clone() {
        try {
            BreakIterator cloned = (BreakIterator) super.clone();
            cloned.wrapped = (NativeBreakIterator) this.wrapped.clone();
            return cloned;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }
}
