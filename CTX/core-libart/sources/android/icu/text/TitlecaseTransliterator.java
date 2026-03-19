package android.icu.text;

import android.icu.impl.UCaseProps;
import android.icu.lang.UCharacter;
import android.icu.text.Transliterator;
import android.icu.util.ULocale;

class TitlecaseTransliterator extends Transliterator {
    static final String _ID = "Any-Title";
    private int caseLocale;
    private final UCaseProps csp;
    private ReplaceableContextIterator iter;
    private final ULocale locale;
    private StringBuilder result;
    SourceTargetUtility sourceTargetUtility;

    static void register() {
        Transliterator.registerFactory(_ID, new Transliterator.Factory() {
            @Override
            public Transliterator getInstance(String str) {
                return new TitlecaseTransliterator(ULocale.US);
            }
        });
        registerSpecialInverse("Title", "Lower", false);
    }

    public TitlecaseTransliterator(ULocale uLocale) {
        super(_ID, null);
        this.sourceTargetUtility = null;
        this.locale = uLocale;
        setMaximumContextLength(2);
        this.csp = UCaseProps.INSTANCE;
        this.iter = new ReplaceableContextIterator();
        this.result = new StringBuilder();
        this.caseLocale = UCaseProps.getCaseLocale(this.locale);
    }

    @Override
    protected synchronized void handleTransliterate(Replaceable replaceable, Transliterator.Position position, boolean z) {
        boolean z2;
        int fullLower;
        int iReplace;
        if (position.start >= position.limit) {
            return;
        }
        int charCount = position.start - 1;
        while (charCount >= position.contextStart) {
            int iChar32At = replaceable.char32At(charCount);
            int typeOrIgnorable = this.csp.getTypeOrIgnorable(iChar32At);
            if (typeOrIgnorable <= 0) {
                if (typeOrIgnorable == 0) {
                    break;
                } else {
                    charCount -= UTF16.getCharCount(iChar32At);
                }
            } else {
                z2 = false;
                break;
            }
        }
        z2 = true;
        this.iter.setText(replaceable);
        this.iter.setIndex(position.start);
        this.iter.setLimit(position.limit);
        this.iter.setContextLimits(position.contextStart, position.contextLimit);
        this.result.setLength(0);
        while (true) {
            int iNextCaseMapCP = this.iter.nextCaseMapCP();
            if (iNextCaseMapCP >= 0) {
                int typeOrIgnorable2 = this.csp.getTypeOrIgnorable(iNextCaseMapCP);
                if (typeOrIgnorable2 >= 0) {
                    if (z2) {
                        fullLower = this.csp.toFullTitle(iNextCaseMapCP, this.iter, this.result, this.caseLocale);
                    } else {
                        fullLower = this.csp.toFullLower(iNextCaseMapCP, this.iter, this.result, this.caseLocale);
                    }
                    z2 = typeOrIgnorable2 == 0;
                    if (this.iter.didReachLimit() && z) {
                        position.start = this.iter.getCaseMapCPStart();
                        return;
                    }
                    if (fullLower >= 0) {
                        if (fullLower <= 31) {
                            iReplace = this.iter.replace(this.result.toString());
                            this.result.setLength(0);
                        } else {
                            iReplace = this.iter.replace(UTF16.valueOf(fullLower));
                        }
                        if (iReplace != 0) {
                            position.limit += iReplace;
                            position.contextLimit += iReplace;
                        }
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
        synchronized (this) {
            if (this.sourceTargetUtility == null) {
                this.sourceTargetUtility = new SourceTargetUtility(new Transform<String, String>() {
                    @Override
                    public String transform(String str) {
                        return UCharacter.toTitleCase(TitlecaseTransliterator.this.locale, str, (BreakIterator) null);
                    }
                });
            }
        }
        this.sourceTargetUtility.addSourceTargetSet(this, unicodeSet, unicodeSet2, unicodeSet3);
    }
}
