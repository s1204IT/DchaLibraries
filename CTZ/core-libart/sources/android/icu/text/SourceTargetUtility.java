package android.icu.text;

import android.icu.lang.CharSequences;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

class SourceTargetUtility {
    final UnicodeSet sourceCache;
    final Set<String> sourceStrings;
    final Transform<String, String> transform;
    static final UnicodeSet NON_STARTERS = new UnicodeSet("[:^ccc=0:]").freeze();
    static Normalizer2 NFC = Normalizer2.getNFCInstance();

    public SourceTargetUtility(Transform<String, String> transform) {
        this(transform, null);
    }

    public SourceTargetUtility(Transform<String, String> transform, Normalizer2 normalizer2) {
        boolean z;
        String decomposition;
        this.transform = transform;
        if (normalizer2 != null) {
            this.sourceCache = new UnicodeSet("[:^ccc=0:]");
        } else {
            this.sourceCache = new UnicodeSet();
        }
        this.sourceStrings = new HashSet();
        for (int i = 0; i <= 1114111; i++) {
            if (CharSequences.equals(i, transform.transform(UTF16.valueOf(i)))) {
                z = false;
            } else {
                this.sourceCache.add(i);
                z = true;
            }
            if (normalizer2 != null && (decomposition = NFC.getDecomposition(i)) != null) {
                if (!decomposition.equals(transform.transform(decomposition))) {
                    this.sourceStrings.add(decomposition);
                }
                if (!z && !normalizer2.isInert(i)) {
                    this.sourceCache.add(i);
                }
            }
        }
        this.sourceCache.freeze();
    }

    public void addSourceTargetSet(Transliterator transliterator, UnicodeSet unicodeSet, UnicodeSet unicodeSet2, UnicodeSet unicodeSet3) {
        UnicodeSet filterAsUnicodeSet = transliterator.getFilterAsUnicodeSet(unicodeSet);
        UnicodeSet unicodeSetRetainAll = new UnicodeSet(this.sourceCache).retainAll(filterAsUnicodeSet);
        unicodeSet2.addAll(unicodeSetRetainAll);
        Iterator<String> it = unicodeSetRetainAll.iterator();
        while (it.hasNext()) {
            unicodeSet3.addAll(this.transform.transform(it.next()));
        }
        for (String str : this.sourceStrings) {
            if (filterAsUnicodeSet.containsAll(str)) {
                String strTransform = this.transform.transform(str);
                if (!str.equals(strTransform)) {
                    unicodeSet3.addAll(strTransform);
                    unicodeSet2.addAll(str);
                }
            }
        }
    }
}
