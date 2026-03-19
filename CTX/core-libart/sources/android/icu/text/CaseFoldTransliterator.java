package android.icu.text;

import android.icu.impl.UCaseProps;
import android.icu.lang.UCharacter;
import android.icu.text.Transliterator;

class CaseFoldTransliterator extends Transliterator {
    static final String _ID = "Any-CaseFold";
    static SourceTargetUtility sourceTargetUtility = null;
    private final UCaseProps csp;
    private ReplaceableContextIterator iter;
    private StringBuilder result;

    static void register() {
        Transliterator.registerFactory(_ID, new Transliterator.Factory() {
            @Override
            public Transliterator getInstance(String str) {
                return new CaseFoldTransliterator();
            }
        });
        Transliterator.registerSpecialInverse("CaseFold", "Upper", false);
    }

    public CaseFoldTransliterator() {
        super(_ID, null);
        this.csp = UCaseProps.INSTANCE;
        this.iter = new ReplaceableContextIterator();
        this.result = new StringBuilder();
    }

    @Override
    protected synchronized void handleTransliterate(Replaceable replaceable, Transliterator.Position position, boolean z) {
        int iReplace;
        if (this.csp == null) {
            return;
        }
        if (position.start >= position.limit) {
            return;
        }
        this.iter.setText(replaceable);
        this.result.setLength(0);
        this.iter.setIndex(position.start);
        this.iter.setLimit(position.limit);
        this.iter.setContextLimits(position.contextStart, position.contextLimit);
        while (true) {
            int iNextCaseMapCP = this.iter.nextCaseMapCP();
            if (iNextCaseMapCP >= 0) {
                int fullFolding = this.csp.toFullFolding(iNextCaseMapCP, this.result, 0);
                if (this.iter.didReachLimit() && z) {
                    position.start = this.iter.getCaseMapCPStart();
                    return;
                }
                if (fullFolding >= 0) {
                    if (fullFolding <= 31) {
                        iReplace = this.iter.replace(this.result.toString());
                        this.result.setLength(0);
                    } else {
                        iReplace = this.iter.replace(UTF16.valueOf(fullFolding));
                    }
                    if (iReplace != 0) {
                        position.limit += iReplace;
                        position.contextLimit += iReplace;
                    }
                }
            } else {
                position.start = position.limit;
                return;
            }
        }
    }

    @Override
    public void addSourceTargetSet(UnicodeSet unicodeSet, UnicodeSet unicodeSet2, UnicodeSet unicodeSet3) {
        synchronized (UppercaseTransliterator.class) {
            if (sourceTargetUtility == null) {
                sourceTargetUtility = new SourceTargetUtility(new Transform<String, String>() {
                    @Override
                    public String transform(String str) {
                        return UCharacter.foldCase(str, true);
                    }
                });
            }
        }
        sourceTargetUtility.addSourceTargetSet(this, unicodeSet, unicodeSet2, unicodeSet3);
    }
}
