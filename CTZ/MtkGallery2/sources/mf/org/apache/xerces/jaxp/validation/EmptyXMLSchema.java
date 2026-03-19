package mf.org.apache.xerces.jaxp.validation;

import mf.org.apache.xerces.xni.grammars.Grammar;
import mf.org.apache.xerces.xni.grammars.XMLGrammarDescription;
import mf.org.apache.xerces.xni.grammars.XMLGrammarPool;

final class EmptyXMLSchema extends AbstractXMLSchema implements XMLGrammarPool {
    private static final Grammar[] ZERO_LENGTH_GRAMMAR_ARRAY = new Grammar[0];

    @Override
    public Grammar[] retrieveInitialGrammarSet(String grammarType) {
        return ZERO_LENGTH_GRAMMAR_ARRAY;
    }

    @Override
    public void cacheGrammars(String grammarType, Grammar[] grammars) {
    }

    @Override
    public Grammar retrieveGrammar(XMLGrammarDescription desc) {
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
