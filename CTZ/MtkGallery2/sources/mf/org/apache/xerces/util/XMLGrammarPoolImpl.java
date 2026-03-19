package mf.org.apache.xerces.util;

import mf.org.apache.xerces.xni.grammars.Grammar;
import mf.org.apache.xerces.xni.grammars.XMLGrammarDescription;
import mf.org.apache.xerces.xni.grammars.XMLGrammarPool;

public class XMLGrammarPoolImpl implements XMLGrammarPool {
    private static final boolean DEBUG = false;
    protected static final int TABLE_SIZE = 11;
    protected Entry[] fGrammars;
    protected int fGrammarCount = 0;
    protected boolean fPoolIsLocked = false;

    public XMLGrammarPoolImpl() {
        this.fGrammars = null;
        this.fGrammars = new Entry[11];
    }

    public XMLGrammarPoolImpl(int initialCapacity) {
        this.fGrammars = null;
        this.fGrammars = new Entry[initialCapacity];
    }

    @Override
    public Grammar[] retrieveInitialGrammarSet(String grammarType) {
        Grammar[] toReturn;
        synchronized (this.fGrammars) {
            int grammarSize = this.fGrammars.length;
            Grammar[] tempGrammars = new Grammar[this.fGrammarCount];
            int pos = 0;
            for (int i = 0; i < grammarSize; i++) {
                for (Entry e = this.fGrammars[i]; e != null; e = e.next) {
                    if (e.desc.getGrammarType().equals(grammarType)) {
                        tempGrammars[pos] = e.grammar;
                        pos++;
                    }
                }
            }
            toReturn = new Grammar[pos];
            System.arraycopy(tempGrammars, 0, toReturn, 0, pos);
        }
        return toReturn;
    }

    @Override
    public void cacheGrammars(String grammarType, Grammar[] grammars) {
        if (!this.fPoolIsLocked) {
            for (Grammar grammar : grammars) {
                putGrammar(grammar);
            }
        }
    }

    @Override
    public Grammar retrieveGrammar(XMLGrammarDescription desc) {
        return getGrammar(desc);
    }

    public void putGrammar(Grammar grammar) {
        if (!this.fPoolIsLocked) {
            synchronized (this.fGrammars) {
                XMLGrammarDescription desc = grammar.getGrammarDescription();
                int hash = hashCode(desc);
                int index = (Integer.MAX_VALUE & hash) % this.fGrammars.length;
                for (Entry entry = this.fGrammars[index]; entry != null; entry = entry.next) {
                    if (entry.hash == hash && equals(entry.desc, desc)) {
                        entry.grammar = grammar;
                        return;
                    }
                }
                Entry entry2 = new Entry(hash, desc, grammar, this.fGrammars[index]);
                this.fGrammars[index] = entry2;
                this.fGrammarCount++;
            }
        }
    }

    public Grammar getGrammar(XMLGrammarDescription desc) {
        synchronized (this.fGrammars) {
            int hash = hashCode(desc);
            int index = (Integer.MAX_VALUE & hash) % this.fGrammars.length;
            for (Entry entry = this.fGrammars[index]; entry != null; entry = entry.next) {
                if (entry.hash == hash && equals(entry.desc, desc)) {
                    return entry.grammar;
                }
            }
            return null;
        }
    }

    public Grammar removeGrammar(XMLGrammarDescription desc) {
        synchronized (this.fGrammars) {
            int hash = hashCode(desc);
            int index = (Integer.MAX_VALUE & hash) % this.fGrammars.length;
            Entry prev = null;
            for (Entry entry = this.fGrammars[index]; entry != null; entry = entry.next) {
                if (entry.hash != hash || !equals(entry.desc, desc)) {
                    prev = entry;
                } else {
                    if (prev != null) {
                        prev.next = entry.next;
                    } else {
                        this.fGrammars[index] = entry.next;
                    }
                    Grammar tempGrammar = entry.grammar;
                    entry.grammar = null;
                    this.fGrammarCount--;
                    return tempGrammar;
                }
            }
            return null;
        }
    }

    public boolean containsGrammar(XMLGrammarDescription desc) {
        synchronized (this.fGrammars) {
            int hash = hashCode(desc);
            int index = (Integer.MAX_VALUE & hash) % this.fGrammars.length;
            for (Entry entry = this.fGrammars[index]; entry != null; entry = entry.next) {
                if (entry.hash == hash && equals(entry.desc, desc)) {
                    return true;
                }
            }
            return false;
        }
    }

    @Override
    public void lockPool() {
        this.fPoolIsLocked = true;
    }

    @Override
    public void unlockPool() {
        this.fPoolIsLocked = false;
    }

    @Override
    public void clear() {
        for (int i = 0; i < this.fGrammars.length; i++) {
            if (this.fGrammars[i] != null) {
                this.fGrammars[i].clear();
                this.fGrammars[i] = null;
            }
        }
        this.fGrammarCount = 0;
    }

    public boolean equals(XMLGrammarDescription desc1, XMLGrammarDescription desc2) {
        return desc1.equals(desc2);
    }

    public int hashCode(XMLGrammarDescription desc) {
        return desc.hashCode();
    }

    protected static final class Entry {
        public XMLGrammarDescription desc;
        public Grammar grammar;
        public int hash;
        public Entry next;

        protected Entry(int hash, XMLGrammarDescription desc, Grammar grammar, Entry next) {
            this.hash = hash;
            this.desc = desc;
            this.grammar = grammar;
            this.next = next;
        }

        protected void clear() {
            this.desc = null;
            this.grammar = null;
            if (this.next != null) {
                this.next.clear();
                this.next = null;
            }
        }
    }
}
