package mf.org.apache.xerces.util;

import mf.org.apache.xerces.dom3.as.ASContentModel;

public class SymbolTable {
    protected static final int TABLE_SIZE = 101;
    protected Entry[] fBuckets;
    protected transient int fCount;
    protected float fLoadFactor;
    protected int fTableSize;
    protected int fThreshold;

    public SymbolTable(int initialCapacity, float loadFactor) {
        this.fBuckets = null;
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("Illegal Capacity: " + initialCapacity);
        }
        if (loadFactor <= 0.0f || Float.isNaN(loadFactor)) {
            throw new IllegalArgumentException("Illegal Load: " + loadFactor);
        }
        initialCapacity = initialCapacity == 0 ? 1 : initialCapacity;
        this.fLoadFactor = loadFactor;
        this.fTableSize = initialCapacity;
        this.fBuckets = new Entry[this.fTableSize];
        this.fThreshold = (int) (this.fTableSize * loadFactor);
        this.fCount = 0;
    }

    public SymbolTable(int initialCapacity) {
        this(initialCapacity, 0.75f);
    }

    public SymbolTable() {
        this(TABLE_SIZE, 0.75f);
    }

    public String addSymbol(String symbol) {
        int bucket = hash(symbol) % this.fTableSize;
        for (Entry entry = this.fBuckets[bucket]; entry != null; entry = entry.next) {
            if (entry.symbol.equals(symbol)) {
                return entry.symbol;
            }
        }
        if (this.fCount >= this.fThreshold) {
            rehash();
            bucket = hash(symbol) % this.fTableSize;
        }
        Entry entry2 = new Entry(symbol, this.fBuckets[bucket]);
        this.fBuckets[bucket] = entry2;
        this.fCount++;
        return entry2.symbol;
    }

    public String addSymbol(char[] buffer, int offset, int length) {
        int bucket = hash(buffer, offset, length) % this.fTableSize;
        for (Entry entry = this.fBuckets[bucket]; entry != null; entry = entry.next) {
            if (length == entry.characters.length) {
                for (int i = 0; i < length; i++) {
                    if (buffer[offset + i] != entry.characters[i]) {
                        break;
                    }
                }
                return entry.symbol;
            }
        }
        if (this.fCount >= this.fThreshold) {
            rehash();
            bucket = hash(buffer, offset, length) % this.fTableSize;
        }
        Entry entry2 = new Entry(buffer, offset, length, this.fBuckets[bucket]);
        this.fBuckets[bucket] = entry2;
        this.fCount++;
        return entry2.symbol;
    }

    public int hash(String symbol) {
        return symbol.hashCode() & ASContentModel.AS_UNBOUNDED;
    }

    public int hash(char[] buffer, int offset, int length) {
        int code = 0;
        for (int i = 0; i < length; i++) {
            code = (code * 31) + buffer[offset + i];
        }
        return Integer.MAX_VALUE & code;
    }

    protected void rehash() {
        int oldCapacity = this.fBuckets.length;
        Entry[] oldTable = this.fBuckets;
        int newCapacity = (oldCapacity * 2) + 1;
        Entry[] newTable = new Entry[newCapacity];
        this.fThreshold = (int) (newCapacity * this.fLoadFactor);
        this.fBuckets = newTable;
        this.fTableSize = this.fBuckets.length;
        int i = oldCapacity;
        while (true) {
            int i2 = i - 1;
            if (i > 0) {
                Entry old = oldTable[i2];
                while (old != null) {
                    Entry e = old;
                    old = old.next;
                    int index = hash(e.characters, 0, e.characters.length) % newCapacity;
                    e.next = newTable[index];
                    newTable[index] = e;
                }
                i = i2;
            } else {
                return;
            }
        }
    }

    public boolean containsSymbol(String symbol) {
        int bucket = hash(symbol) % this.fTableSize;
        int length = symbol.length();
        for (Entry entry = this.fBuckets[bucket]; entry != null; entry = entry.next) {
            if (length == entry.characters.length) {
                for (int i = 0; i < length; i++) {
                    if (symbol.charAt(i) != entry.characters[i]) {
                        break;
                    }
                }
                return true;
            }
        }
        return false;
    }

    public boolean containsSymbol(char[] buffer, int offset, int length) {
        int bucket = hash(buffer, offset, length) % this.fTableSize;
        for (Entry entry = this.fBuckets[bucket]; entry != null; entry = entry.next) {
            if (length == entry.characters.length) {
                for (int i = 0; i < length; i++) {
                    if (buffer[offset + i] != entry.characters[i]) {
                        break;
                    }
                }
                return true;
            }
        }
        return false;
    }

    protected static final class Entry {
        public final char[] characters;
        public Entry next;
        public final String symbol;

        public Entry(String symbol, Entry next) {
            this.symbol = symbol.intern();
            this.characters = new char[symbol.length()];
            symbol.getChars(0, this.characters.length, this.characters, 0);
            this.next = next;
        }

        public Entry(char[] ch, int offset, int length, Entry next) {
            this.characters = new char[length];
            System.arraycopy(ch, offset, this.characters, 0, length);
            this.symbol = new String(this.characters).intern();
            this.next = next;
        }
    }
}
