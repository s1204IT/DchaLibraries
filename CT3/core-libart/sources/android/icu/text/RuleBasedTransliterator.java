package android.icu.text;

import android.icu.text.Transliterator;
import java.util.HashMap;
import java.util.Map;

@Deprecated
public class RuleBasedTransliterator extends Transliterator {
    private Data data;

    RuleBasedTransliterator(String ID, Data data, UnicodeFilter filter) {
        super(ID, filter);
        this.data = data;
        setMaximumContextLength(data.ruleSet.getMaximumContextLength());
    }

    @Override
    @Deprecated
    protected void handleTransliterate(Replaceable text, Transliterator.Position index, boolean incremental) {
        synchronized (this.data) {
            int loopLimit = (index.limit - index.start) << 4;
            if (loopLimit < 0) {
                loopLimit = Integer.MAX_VALUE;
            }
            for (int loopCount = 0; index.start < index.limit && loopCount <= loopLimit && this.data.ruleSet.transliterate(text, index, incremental); loopCount++) {
            }
        }
    }

    static class Data {
        Object[] variables;
        char variablesBase;
        Map<String, char[]> variableNames = new HashMap();
        public TransliterationRuleSet ruleSet = new TransliterationRuleSet();

        public UnicodeMatcher lookupMatcher(int standIn) {
            int i = standIn - this.variablesBase;
            if (i < 0 || i >= this.variables.length) {
                return null;
            }
            return (UnicodeMatcher) this.variables[i];
        }

        public UnicodeReplacer lookupReplacer(int standIn) {
            int i = standIn - this.variablesBase;
            if (i < 0 || i >= this.variables.length) {
                return null;
            }
            return (UnicodeReplacer) this.variables[i];
        }
    }

    @Override
    @Deprecated
    public String toRules(boolean escapeUnprintable) {
        return this.data.ruleSet.toRules(escapeUnprintable);
    }

    @Override
    @Deprecated
    public void addSourceTargetSet(UnicodeSet filter, UnicodeSet sourceSet, UnicodeSet targetSet) {
        this.data.ruleSet.addSourceTargetSet(filter, sourceSet, targetSet);
    }

    @Deprecated
    public Transliterator safeClone() {
        ?? filter = getFilter();
        ?? unicodeSet = filter;
        if (filter != 0) {
            boolean z = filter instanceof UnicodeSet;
            unicodeSet = filter;
            if (z) {
                unicodeSet = new UnicodeSet((UnicodeSet) filter);
            }
        }
        return new RuleBasedTransliterator(getID(), this.data, unicodeSet);
    }
}
