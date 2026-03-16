package java.text;

import libcore.icu.RuleBasedCollatorICU;

public class RuleBasedCollator extends Collator {
    RuleBasedCollator(RuleBasedCollatorICU wrapper) {
        super(wrapper);
    }

    public RuleBasedCollator(String rules) throws Exception {
        if (rules == null) {
            throw new NullPointerException("rules == null");
        }
        try {
            this.icuColl = new RuleBasedCollatorICU(rules);
        } catch (Exception e) {
            if (e instanceof ParseException) {
                throw ((ParseException) e);
            }
            throw new ParseException(e.getMessage(), -1);
        }
    }

    public CollationElementIterator getCollationElementIterator(CharacterIterator source) {
        if (source == null) {
            throw new NullPointerException("source == null");
        }
        return new CollationElementIterator(this.icuColl.getCollationElementIterator(source));
    }

    public CollationElementIterator getCollationElementIterator(String source) {
        if (source == null) {
            throw new NullPointerException("source == null");
        }
        return new CollationElementIterator(this.icuColl.getCollationElementIterator(source));
    }

    public String getRules() {
        return this.icuColl.getRules();
    }

    @Override
    public Object clone() {
        RuleBasedCollator clone = (RuleBasedCollator) super.clone();
        return clone;
    }

    @Override
    public int compare(String source, String target) {
        if (source == null) {
            throw new NullPointerException("source == null");
        }
        if (target == null) {
            throw new NullPointerException("target == null");
        }
        return this.icuColl.compare(source, target);
    }

    @Override
    public CollationKey getCollationKey(String source) {
        return this.icuColl.getCollationKey(source);
    }

    @Override
    public int hashCode() {
        return this.icuColl.getRules().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Collator) {
            return super.equals(obj);
        }
        return false;
    }
}
