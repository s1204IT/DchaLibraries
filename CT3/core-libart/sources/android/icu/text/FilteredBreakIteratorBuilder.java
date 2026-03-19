package android.icu.text;

import android.icu.impl.SimpleFilteredSentenceBreakIterator;
import android.icu.util.ULocale;

@Deprecated
public abstract class FilteredBreakIteratorBuilder {
    @Deprecated
    public abstract BreakIterator build(BreakIterator breakIterator);

    @Deprecated
    public abstract boolean suppressBreakAfter(String str);

    @Deprecated
    public abstract boolean unsuppressBreakAfter(String str);

    @Deprecated
    public static FilteredBreakIteratorBuilder createInstance(ULocale where) {
        FilteredBreakIteratorBuilder ret = new SimpleFilteredSentenceBreakIterator.Builder(where);
        return ret;
    }

    @Deprecated
    public static FilteredBreakIteratorBuilder createInstance() {
        FilteredBreakIteratorBuilder ret = new SimpleFilteredSentenceBreakIterator.Builder();
        return ret;
    }

    @Deprecated
    protected FilteredBreakIteratorBuilder() {
    }
}
