package android.icu.impl.text;

import android.icu.impl.ICUDebug;
import android.icu.text.CollationElementIterator;
import android.icu.text.Collator;
import android.icu.text.RbnfLenientScanner;
import android.icu.text.RbnfLenientScannerProvider;
import android.icu.text.RuleBasedCollator;
import android.icu.util.ULocale;
import java.util.HashMap;
import java.util.Map;

@Deprecated
public class RbnfScannerProviderImpl implements RbnfLenientScannerProvider {
    private static final boolean DEBUG = ICUDebug.enabled("rbnf");
    private Map<String, RbnfLenientScanner> cache = new HashMap();

    @Deprecated
    public RbnfScannerProviderImpl() {
    }

    @Override
    @Deprecated
    public RbnfLenientScanner get(ULocale uLocale, String str) {
        String str2 = uLocale.toString() + "/" + str;
        synchronized (this.cache) {
            RbnfLenientScanner rbnfLenientScanner = this.cache.get(str2);
            if (rbnfLenientScanner != null) {
                return rbnfLenientScanner;
            }
            RbnfLenientScanner rbnfLenientScannerCreateScanner = createScanner(uLocale, str);
            synchronized (this.cache) {
                this.cache.put(str2, rbnfLenientScannerCreateScanner);
            }
            return rbnfLenientScannerCreateScanner;
        }
    }

    @Deprecated
    protected RbnfLenientScanner createScanner(ULocale uLocale, String str) {
        RuleBasedCollator ruleBasedCollator;
        try {
            ruleBasedCollator = (RuleBasedCollator) Collator.getInstance(uLocale.toLocale());
            if (str != null) {
                ruleBasedCollator = new RuleBasedCollator(ruleBasedCollator.getRules() + str);
            }
            ruleBasedCollator.setDecomposition(17);
        } catch (Exception e) {
            if (DEBUG) {
                e.printStackTrace();
                System.out.println("++++");
            }
            ruleBasedCollator = null;
        }
        return new RbnfLenientScannerImpl(ruleBasedCollator);
    }

    private static class RbnfLenientScannerImpl implements RbnfLenientScanner {
        private final RuleBasedCollator collator;

        private RbnfLenientScannerImpl(RuleBasedCollator ruleBasedCollator) {
            this.collator = ruleBasedCollator;
        }

        @Override
        public boolean allIgnorable(String str) {
            CollationElementIterator collationElementIterator = this.collator.getCollationElementIterator(str);
            int next = collationElementIterator.next();
            while (next != -1 && CollationElementIterator.primaryOrder(next) == 0) {
                next = collationElementIterator.next();
            }
            return next == -1;
        }

        @Override
        public int[] findText(String str, String str2, int i) {
            int iPrefixLength = 0;
            while (i < str.length() && iPrefixLength == 0) {
                iPrefixLength = prefixLength(str.substring(i), str2);
                if (iPrefixLength != 0) {
                    return new int[]{i, iPrefixLength};
                }
                i++;
            }
            return new int[]{-1, 0};
        }

        public int[] findText2(String str, String str2, int i) {
            int offset;
            CollationElementIterator collationElementIterator = this.collator.getCollationElementIterator(str);
            CollationElementIterator collationElementIterator2 = this.collator.getCollationElementIterator(str2);
            collationElementIterator.setOffset(i);
            int next = collationElementIterator.next();
            int next2 = collationElementIterator2.next();
            loop0: while (true) {
                offset = -1;
                while (next2 != -1) {
                    while (next != -1 && CollationElementIterator.primaryOrder(next) == 0) {
                        next = collationElementIterator.next();
                    }
                    while (next2 != -1 && CollationElementIterator.primaryOrder(next2) == 0) {
                        next2 = collationElementIterator2.next();
                    }
                    if (next == -1) {
                        return new int[]{-1, 0};
                    }
                    if (next2 == -1) {
                        break loop0;
                    }
                    if (CollationElementIterator.primaryOrder(next) == CollationElementIterator.primaryOrder(next2)) {
                        offset = collationElementIterator.getOffset();
                        next = collationElementIterator.next();
                        next2 = collationElementIterator2.next();
                    } else {
                        if (offset != -1) {
                            break;
                        }
                        next = collationElementIterator.next();
                    }
                }
                collationElementIterator2.reset();
            }
            return new int[]{offset, collationElementIterator.getOffset() - offset};
        }

        @Override
        public int prefixLength(String str, String str2) {
            CollationElementIterator collationElementIterator = this.collator.getCollationElementIterator(str);
            CollationElementIterator collationElementIterator2 = this.collator.getCollationElementIterator(str2);
            int next = collationElementIterator.next();
            int next2 = collationElementIterator2.next();
            while (next2 != -1) {
                while (CollationElementIterator.primaryOrder(next) == 0 && next != -1) {
                    next = collationElementIterator.next();
                }
                while (CollationElementIterator.primaryOrder(next2) == 0 && next2 != -1) {
                    next2 = collationElementIterator2.next();
                }
                if (next2 == -1) {
                    break;
                }
                if (next == -1 || CollationElementIterator.primaryOrder(next) != CollationElementIterator.primaryOrder(next2)) {
                    return 0;
                }
                next = collationElementIterator.next();
                next2 = collationElementIterator2.next();
            }
            int offset = collationElementIterator.getOffset();
            if (next != -1) {
                return offset - 1;
            }
            return offset;
        }
    }
}
