package android.icu.text;

import android.icu.impl.UCaseProps;
import android.icu.lang.UCharacter;
import android.icu.text.Transliterator;
import android.icu.util.ULocale;

class UppercaseTransliterator extends Transliterator {
    static final String _ID = "Any-Upper";
    private UCaseProps csp;
    private ReplaceableContextIterator iter;
    private int[] locCache;
    private ULocale locale;
    private StringBuilder result;
    SourceTargetUtility sourceTargetUtility;

    static void register() {
        Transliterator.registerFactory(_ID, new Transliterator.Factory() {
            @Override
            public Transliterator getInstance(String ID) {
                return new UppercaseTransliterator(ULocale.US);
            }
        });
    }

    public UppercaseTransliterator(ULocale loc) {
        super(_ID, null);
        this.sourceTargetUtility = null;
        this.locale = loc;
        this.csp = UCaseProps.INSTANCE;
        this.iter = new ReplaceableContextIterator();
        this.result = new StringBuilder();
        this.locCache = new int[1];
        this.locCache[0] = 0;
    }

    @Override
    protected synchronized void handleTransliterate(Replaceable text, Transliterator.Position offsets, boolean isIncremental) {
        int delta;
        if (this.csp == null) {
            return;
        }
        if (offsets.start >= offsets.limit) {
            return;
        }
        this.iter.setText(text);
        this.result.setLength(0);
        this.iter.setIndex(offsets.start);
        this.iter.setLimit(offsets.limit);
        this.iter.setContextLimits(offsets.contextStart, offsets.contextLimit);
        while (true) {
            int c = this.iter.nextCaseMapCP();
            if (c >= 0) {
                int c2 = this.csp.toFullUpper(c, this.iter, this.result, this.locale, this.locCache);
                if (this.iter.didReachLimit() && isIncremental) {
                    offsets.start = this.iter.getCaseMapCPStart();
                    return;
                }
                if (c2 >= 0) {
                    if (c2 <= 31) {
                        delta = this.iter.replace(this.result.toString());
                        this.result.setLength(0);
                    } else {
                        delta = this.iter.replace(UTF16.valueOf(c2));
                    }
                    if (delta != 0) {
                        offsets.limit += delta;
                        offsets.contextLimit += delta;
                    }
                }
            } else {
                offsets.start = offsets.limit;
                return;
            }
        }
    }

    @Override
    public void addSourceTargetSet(UnicodeSet inputFilter, UnicodeSet sourceSet, UnicodeSet targetSet) {
        synchronized (this) {
            if (this.sourceTargetUtility == null) {
                this.sourceTargetUtility = new SourceTargetUtility(new Transform<String, String>() {
                    @Override
                    public String transform(String source) {
                        return UCharacter.toUpperCase(UppercaseTransliterator.this.locale, source);
                    }
                });
            }
        }
        this.sourceTargetUtility.addSourceTargetSet(this, inputFilter, sourceSet, targetSet);
    }
}
