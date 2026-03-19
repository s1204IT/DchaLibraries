package android.icu.text;

import android.icu.text.Transliterator;

class NullTransliterator extends Transliterator {
    static String SHORT_ID = "Null";
    static String _ID = "Any-Null";

    public NullTransliterator() {
        super(_ID, null);
    }

    @Override
    protected void handleTransliterate(Replaceable text, Transliterator.Position offsets, boolean incremental) {
        offsets.start = offsets.limit;
    }

    @Override
    public void addSourceTargetSet(UnicodeSet inputFilter, UnicodeSet sourceSet, UnicodeSet targetSet) {
    }
}
