package mf.org.apache.xerces.parsers;

import mf.org.apache.xerces.util.ShadowedSymbolTable;
import mf.org.apache.xerces.util.SymbolTable;
import mf.org.apache.xerces.util.SynchronizedSymbolTable;
import mf.org.apache.xerces.util.XMLGrammarPoolImpl;
import mf.org.apache.xerces.xni.grammars.Grammar;
import mf.org.apache.xerces.xni.grammars.XMLGrammarDescription;
import mf.org.apache.xerces.xni.grammars.XMLGrammarPool;

public class CachingParserPool {
    public static final boolean DEFAULT_SHADOW_GRAMMAR_POOL = false;
    public static final boolean DEFAULT_SHADOW_SYMBOL_TABLE = false;
    protected boolean fShadowGrammarPool;
    protected boolean fShadowSymbolTable;
    protected XMLGrammarPool fSynchronizedGrammarPool;
    protected SymbolTable fSynchronizedSymbolTable;

    public CachingParserPool() {
        this(new SymbolTable(), new XMLGrammarPoolImpl());
    }

    public CachingParserPool(SymbolTable symbolTable, XMLGrammarPool grammarPool) {
        this.fShadowSymbolTable = false;
        this.fShadowGrammarPool = false;
        this.fSynchronizedSymbolTable = new SynchronizedSymbolTable(symbolTable);
        this.fSynchronizedGrammarPool = new SynchronizedGrammarPool(grammarPool);
    }

    public SymbolTable getSymbolTable() {
        return this.fSynchronizedSymbolTable;
    }

    public XMLGrammarPool getXMLGrammarPool() {
        return this.fSynchronizedGrammarPool;
    }

    public void setShadowSymbolTable(boolean shadow) {
        this.fShadowSymbolTable = shadow;
    }

    public DOMParser createDOMParser() {
        SymbolTable symbolTable;
        XMLGrammarPool grammarPool;
        if (this.fShadowSymbolTable) {
            symbolTable = new ShadowedSymbolTable(this.fSynchronizedSymbolTable);
        } else {
            symbolTable = this.fSynchronizedSymbolTable;
        }
        if (this.fShadowGrammarPool) {
            grammarPool = new ShadowedGrammarPool(this.fSynchronizedGrammarPool);
        } else {
            grammarPool = this.fSynchronizedGrammarPool;
        }
        return new DOMParser(symbolTable, grammarPool);
    }

    public SAXParser createSAXParser() {
        SymbolTable symbolTable;
        XMLGrammarPool grammarPool;
        if (this.fShadowSymbolTable) {
            symbolTable = new ShadowedSymbolTable(this.fSynchronizedSymbolTable);
        } else {
            symbolTable = this.fSynchronizedSymbolTable;
        }
        if (this.fShadowGrammarPool) {
            grammarPool = new ShadowedGrammarPool(this.fSynchronizedGrammarPool);
        } else {
            grammarPool = this.fSynchronizedGrammarPool;
        }
        return new SAXParser(symbolTable, grammarPool);
    }

    public static final class SynchronizedGrammarPool implements XMLGrammarPool {
        private XMLGrammarPool fGrammarPool;

        public SynchronizedGrammarPool(XMLGrammarPool grammarPool) {
            this.fGrammarPool = grammarPool;
        }

        @Override
        public Grammar[] retrieveInitialGrammarSet(String grammarType) {
            Grammar[] grammarArrRetrieveInitialGrammarSet;
            synchronized (this.fGrammarPool) {
                grammarArrRetrieveInitialGrammarSet = this.fGrammarPool.retrieveInitialGrammarSet(grammarType);
            }
            return grammarArrRetrieveInitialGrammarSet;
        }

        @Override
        public Grammar retrieveGrammar(XMLGrammarDescription gDesc) {
            Grammar grammarRetrieveGrammar;
            synchronized (this.fGrammarPool) {
                grammarRetrieveGrammar = this.fGrammarPool.retrieveGrammar(gDesc);
            }
            return grammarRetrieveGrammar;
        }

        @Override
        public void cacheGrammars(String grammarType, Grammar[] grammars) {
            synchronized (this.fGrammarPool) {
                this.fGrammarPool.cacheGrammars(grammarType, grammars);
            }
        }

        @Override
        public void lockPool() {
            synchronized (this.fGrammarPool) {
                this.fGrammarPool.lockPool();
            }
        }

        @Override
        public void clear() {
            synchronized (this.fGrammarPool) {
                this.fGrammarPool.clear();
            }
        }

        @Override
        public void unlockPool() {
            synchronized (this.fGrammarPool) {
                this.fGrammarPool.unlockPool();
            }
        }
    }

    public static final class ShadowedGrammarPool extends XMLGrammarPoolImpl {
        private XMLGrammarPool fGrammarPool;

        public ShadowedGrammarPool(XMLGrammarPool grammarPool) {
            this.fGrammarPool = grammarPool;
        }

        @Override
        public Grammar[] retrieveInitialGrammarSet(String grammarType) {
            Grammar[] grammars = super.retrieveInitialGrammarSet(grammarType);
            return grammars != null ? grammars : this.fGrammarPool.retrieveInitialGrammarSet(grammarType);
        }

        @Override
        public Grammar retrieveGrammar(XMLGrammarDescription gDesc) {
            Grammar g = super.retrieveGrammar(gDesc);
            return g != null ? g : this.fGrammarPool.retrieveGrammar(gDesc);
        }

        @Override
        public void cacheGrammars(String grammarType, Grammar[] grammars) {
            super.cacheGrammars(grammarType, grammars);
            this.fGrammarPool.cacheGrammars(grammarType, grammars);
        }

        @Override
        public Grammar getGrammar(XMLGrammarDescription desc) {
            if (super.containsGrammar(desc)) {
                return super.getGrammar(desc);
            }
            return null;
        }

        @Override
        public boolean containsGrammar(XMLGrammarDescription desc) {
            return super.containsGrammar(desc);
        }
    }
}
