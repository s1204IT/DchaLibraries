package android.icu.text;

import android.icu.text.Transliterator;
import java.util.HashMap;
import java.util.Map;

@Deprecated
public class RuleBasedTransliterator extends Transliterator {
    private final Data data;

    RuleBasedTransliterator(String str, Data data, UnicodeFilter unicodeFilter) {
        super(str, unicodeFilter);
        this.data = data;
        setMaximumContextLength(data.ruleSet.getMaximumContextLength());
    }

    @Override
    @Deprecated
    protected void handleTransliterate(Replaceable replaceable, Transliterator.Position position, boolean z) {
        synchronized (this.data) {
            int i = (position.limit - position.start) << 4;
            if (i < 0) {
                i = Integer.MAX_VALUE;
            }
            for (int i2 = 0; position.start < position.limit && i2 <= i && this.data.ruleSet.transliterate(replaceable, position, z); i2++) {
            }
        }
    }

    static class Data {
        Object[] variables;
        char variablesBase;
        Map<String, char[]> variableNames = new HashMap();
        public TransliterationRuleSet ruleSet = new TransliterationRuleSet();

        public UnicodeMatcher lookupMatcher(int i) {
            int i2 = i - this.variablesBase;
            if (i2 < 0 || i2 >= this.variables.length) {
                return null;
            }
            return (UnicodeMatcher) this.variables[i2];
        }

        public UnicodeReplacer lookupReplacer(int i) {
            int i2 = i - this.variablesBase;
            if (i2 < 0 || i2 >= this.variables.length) {
                return null;
            }
            return (UnicodeReplacer) this.variables[i2];
        }
    }

    @Override
    @Deprecated
    public String toRules(boolean z) {
        return this.data.ruleSet.toRules(z);
    }

    @Override
    @Deprecated
    public void addSourceTargetSet(UnicodeSet unicodeSet, UnicodeSet unicodeSet2, UnicodeSet unicodeSet3) {
        this.data.ruleSet.addSourceTargetSet(unicodeSet, unicodeSet2, unicodeSet3);
    }

    @Deprecated
    public Transliterator safeClone() {
        UnicodeFilter filter = getFilter();
        if (filter != null && (filter instanceof UnicodeSet)) {
            filter = new UnicodeSet((UnicodeSet) filter);
        }
        return new RuleBasedTransliterator(getID(), this.data, filter);
    }
}
