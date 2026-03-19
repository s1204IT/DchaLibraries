package mf.org.apache.xerces.jaxp.validation;

import mf.org.apache.xerces.xni.grammars.Grammar;
import mf.org.apache.xerces.xni.grammars.XMLGrammarDescription;
import mf.org.apache.xerces.xni.grammars.XMLGrammarPool;

final class ReadOnlyGrammarPool implements XMLGrammarPool {
    private final XMLGrammarPool core;

    public ReadOnlyGrammarPool(XMLGrammarPool pool) {
        this.core = pool;
    }

    @Override
    public void cacheGrammars(String grammarType, Grammar[] grammars) {
    }

    @Override
    public void clear() {
    }

    @Override
    public void lockPool() {
    }

    @Override
    public Grammar retrieveGrammar(XMLGrammarDescription desc) {
        return this.core.retrieveGrammar(desc);
    }

    @Override
    public Grammar[] retrieveInitialGrammarSet(String grammarType) {
        return this.core.retrieveInitialGrammarSet(grammarType);
    }

    @Override
    public void unlockPool() {
    }
}
