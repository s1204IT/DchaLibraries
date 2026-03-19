package mf.org.apache.xerces.jaxp.validation;

import mf.org.apache.xerces.xni.grammars.Grammar;
import mf.org.apache.xerces.xni.grammars.XMLGrammarDescription;
import mf.org.apache.xerces.xni.grammars.XMLGrammarPool;

final class SimpleXMLSchema extends AbstractXMLSchema implements XMLGrammarPool {
    private static final Grammar[] ZERO_LENGTH_GRAMMAR_ARRAY = new Grammar[0];
    private final Grammar fGrammar;
    private final XMLGrammarDescription fGrammarDescription;
    private final Grammar[] fGrammars;

    public SimpleXMLSchema(Grammar grammar) {
        this.fGrammar = grammar;
        this.fGrammars = new Grammar[]{grammar};
        this.fGrammarDescription = grammar.getGrammarDescription();
    }

    @Override
    public Grammar[] retrieveInitialGrammarSet(String grammarType) {
        return "http://www.w3.org/2001/XMLSchema".equals(grammarType) ? (Grammar[]) this.fGrammars.clone() : ZERO_LENGTH_GRAMMAR_ARRAY;
    }

    @Override
    public void cacheGrammars(String grammarType, Grammar[] grammars) {
    }

    @Override
    public Grammar retrieveGrammar(XMLGrammarDescription desc) {
        if (this.fGrammarDescription.equals(desc)) {
            return this.fGrammar;
        }
        return null;
    }

    @Override
    public void lockPool() {
    }

    @Override
    public void unlockPool() {
    }

    @Override
    public void clear() {
    }

    @Override
    public XMLGrammarPool getGrammarPool() {
        return this;
    }

    @Override
    public boolean isFullyComposed() {
        return true;
    }
}
