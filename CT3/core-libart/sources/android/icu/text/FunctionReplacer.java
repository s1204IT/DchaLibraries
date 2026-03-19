package android.icu.text;

class FunctionReplacer implements UnicodeReplacer {
    private UnicodeReplacer replacer;
    private Transliterator translit;

    public FunctionReplacer(Transliterator theTranslit, UnicodeReplacer theReplacer) {
        this.translit = theTranslit;
        this.replacer = theReplacer;
    }

    @Override
    public int replace(Replaceable text, int start, int limit, int[] cursor) {
        int len = this.replacer.replace(text, start, limit, cursor);
        int limit2 = start + len;
        return this.translit.transliterate(text, start, limit2) - start;
    }

    @Override
    public String toReplacerPattern(boolean escapeUnprintable) {
        return "&" + this.translit.getID() + "( " + this.replacer.toReplacerPattern(escapeUnprintable) + " )";
    }

    @Override
    public void addReplacementSetTo(UnicodeSet toUnionTo) {
        toUnionTo.addAll(this.translit.getTargetSet());
    }
}
