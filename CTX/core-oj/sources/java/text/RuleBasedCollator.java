package java.text;

import libcore.icu.CollationKeyICU;

public class RuleBasedCollator extends Collator {
    RuleBasedCollator(android.icu.text.RuleBasedCollator ruleBasedCollator) {
        super(ruleBasedCollator);
    }

    public RuleBasedCollator(String str) throws ParseException {
        if (str == null) {
            throw new NullPointerException("rules == null");
        }
        try {
            this.icuColl = new android.icu.text.RuleBasedCollator(str);
        } catch (Exception e) {
            if (e instanceof ParseException) {
                throw ((ParseException) e);
            }
            throw new ParseException(e.getMessage(), -1);
        }
    }

    public String getRules() {
        return collAsICU().getRules();
    }

    public CollationElementIterator getCollationElementIterator(String str) {
        if (str == null) {
            throw new NullPointerException("source == null");
        }
        return new CollationElementIterator(collAsICU().getCollationElementIterator(str));
    }

    public CollationElementIterator getCollationElementIterator(CharacterIterator characterIterator) {
        if (characterIterator == null) {
            throw new NullPointerException("source == null");
        }
        return new CollationElementIterator(collAsICU().getCollationElementIterator(characterIterator));
    }

    @Override
    public synchronized int compare(String str, String str2) {
        if (str == null || str2 == null) {
            throw new NullPointerException();
        }
        return this.icuColl.compare(str, str2);
    }

    @Override
    public synchronized CollationKey getCollationKey(String str) {
        if (str == null) {
            return null;
        }
        return new CollationKeyICU(str, this.icuColl.getCollationKey(str));
    }

    @Override
    public Object clone() {
        return super.clone();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return this.icuColl.hashCode();
    }

    private android.icu.text.RuleBasedCollator collAsICU() {
        return (android.icu.text.RuleBasedCollator) this.icuColl;
    }
}
