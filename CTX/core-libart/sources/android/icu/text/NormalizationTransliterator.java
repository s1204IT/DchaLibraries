package android.icu.text;

import android.icu.impl.Norm2AllModes;
import android.icu.impl.Normalizer2Impl;
import android.icu.text.Transliterator;
import java.util.HashMap;
import java.util.Map;

final class NormalizationTransliterator extends Transliterator {
    static final Map<Normalizer2, SourceTargetUtility> SOURCE_CACHE = new HashMap();
    private final Normalizer2 norm2;

    static void register() {
        Transliterator.registerFactory("Any-NFC", new Transliterator.Factory() {
            @Override
            public Transliterator getInstance(String str) {
                return new NormalizationTransliterator("NFC", Normalizer2.getNFCInstance());
            }
        });
        Transliterator.registerFactory("Any-NFD", new Transliterator.Factory() {
            @Override
            public Transliterator getInstance(String str) {
                return new NormalizationTransliterator("NFD", Normalizer2.getNFDInstance());
            }
        });
        Transliterator.registerFactory("Any-NFKC", new Transliterator.Factory() {
            @Override
            public Transliterator getInstance(String str) {
                return new NormalizationTransliterator("NFKC", Normalizer2.getNFKCInstance());
            }
        });
        Transliterator.registerFactory("Any-NFKD", new Transliterator.Factory() {
            @Override
            public Transliterator getInstance(String str) {
                return new NormalizationTransliterator("NFKD", Normalizer2.getNFKDInstance());
            }
        });
        Transliterator.registerFactory("Any-FCD", new Transliterator.Factory() {
            @Override
            public Transliterator getInstance(String str) {
                return new NormalizationTransliterator("FCD", Norm2AllModes.getFCDNormalizer2());
            }
        });
        Transliterator.registerFactory("Any-FCC", new Transliterator.Factory() {
            @Override
            public Transliterator getInstance(String str) {
                return new NormalizationTransliterator("FCC", Norm2AllModes.getNFCInstance().fcc);
            }
        });
        Transliterator.registerSpecialInverse("NFC", "NFD", true);
        Transliterator.registerSpecialInverse("NFKC", "NFKD", true);
        Transliterator.registerSpecialInverse("FCC", "NFD", false);
        Transliterator.registerSpecialInverse("FCD", "FCD", false);
    }

    private NormalizationTransliterator(String str, Normalizer2 normalizer2) {
        super(str, null);
        this.norm2 = normalizer2;
    }

    @Override
    protected void handleTransliterate(Replaceable replaceable, Transliterator.Position position, boolean z) {
        int i = position.start;
        int i2 = position.limit;
        if (i >= i2) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        StringBuilder sb2 = new StringBuilder();
        int iChar32At = replaceable.char32At(i);
        do {
            sb.setLength(0);
            int iCharCount = i;
            while (true) {
                sb.appendCodePoint(iChar32At);
                iCharCount += Character.charCount(iChar32At);
                if (iCharCount >= i2) {
                    break;
                }
                Normalizer2 normalizer2 = this.norm2;
                int iChar32At2 = replaceable.char32At(iCharCount);
                if (!normalizer2.hasBoundaryBefore(iChar32At2)) {
                    iChar32At = iChar32At2;
                } else {
                    iChar32At = iChar32At2;
                    break;
                }
            }
            if (iCharCount == i2 && z && !this.norm2.hasBoundaryAfter(iChar32At)) {
                break;
            }
            this.norm2.normalize((CharSequence) sb, sb2);
            if (!Normalizer2Impl.UTF16Plus.equal(sb, sb2)) {
                replaceable.replace(i, iCharCount, sb2.toString());
                int length = sb2.length() - (iCharCount - i);
                iCharCount += length;
                i2 += length;
            }
            i = iCharCount;
        } while (i < i2);
        position.start = i;
        position.contextLimit += i2 - position.limit;
        position.limit = i2;
    }

    static class NormalizingTransform implements Transform<String, String> {
        final Normalizer2 norm2;

        public NormalizingTransform(Normalizer2 normalizer2) {
            this.norm2 = normalizer2;
        }

        @Override
        public String transform(String str) {
            return this.norm2.normalize(str);
        }
    }

    @Override
    public void addSourceTargetSet(UnicodeSet unicodeSet, UnicodeSet unicodeSet2, UnicodeSet unicodeSet3) {
        SourceTargetUtility sourceTargetUtility;
        synchronized (SOURCE_CACHE) {
            sourceTargetUtility = SOURCE_CACHE.get(this.norm2);
            if (sourceTargetUtility == null) {
                sourceTargetUtility = new SourceTargetUtility(new NormalizingTransform(this.norm2), this.norm2);
                SOURCE_CACHE.put(this.norm2, sourceTargetUtility);
            }
        }
        sourceTargetUtility.addSourceTargetSet(this, unicodeSet, unicodeSet2, unicodeSet3);
    }
}
