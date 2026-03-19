package mf.org.apache.xerces.util;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;

public class SoftReferenceSymbolTable extends SymbolTable {
    protected SREntry[] fBuckets;
    private final ReferenceQueue fReferenceQueue;

    public SoftReferenceSymbolTable(int initialCapacity, float loadFactor) {
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
        this.fBuckets = new SREntry[this.fTableSize];
        this.fThreshold = (int) (this.fTableSize * loadFactor);
        this.fCount = 0;
        this.fReferenceQueue = new ReferenceQueue();
    }

    public SoftReferenceSymbolTable(int initialCapacity) {
        this(initialCapacity, 0.75f);
    }

    public SoftReferenceSymbolTable() {
        this(101, 0.75f);
    }

    @Override
    public String addSymbol(String symbol) {
        clean();
        int bucket = hash(symbol) % this.fTableSize;
        for (SREntry entry = this.fBuckets[bucket]; entry != null; entry = entry.next) {
            SREntryData data = (SREntryData) entry.get();
            if (data != null && data.symbol.equals(symbol)) {
                return data.symbol;
            }
        }
        if (this.fCount >= this.fThreshold) {
            rehash();
            bucket = hash(symbol) % this.fTableSize;
        }
        String symbol2 = symbol.intern();
        SREntry entry2 = new SREntry(symbol2, this.fBuckets[bucket], bucket, this.fReferenceQueue);
        this.fBuckets[bucket] = entry2;
        this.fCount++;
        return symbol2;
    }

    @Override
    public String addSymbol(char[] buffer, int offset, int length) {
        clean();
        int bucket = hash(buffer, offset, length) % this.fTableSize;
        for (SREntry entry = this.fBuckets[bucket]; entry != null; entry = entry.next) {
            SREntryData data = (SREntryData) entry.get();
            if (data != null && length == data.characters.length) {
                for (int i = 0; i < length; i++) {
                    if (buffer[offset + i] != data.characters[i]) {
                        break;
                    }
                }
                return data.symbol;
            }
        }
        if (this.fCount >= this.fThreshold) {
            rehash();
            bucket = hash(buffer, offset, length) % this.fTableSize;
        }
        String symbol = new String(buffer, offset, length).intern();
        SREntry entry2 = new SREntry(symbol, buffer, offset, length, this.fBuckets[bucket], bucket, this.fReferenceQueue);
        this.fBuckets[bucket] = entry2;
        this.fCount++;
        return symbol;
    }

    @Override
    protected void rehash() {
        int oldCapacity = this.fBuckets.length;
        SREntry[] oldTable = this.fBuckets;
        int newCapacity = (oldCapacity * 2) + 1;
        SREntry[] newTable = new SREntry[newCapacity];
        this.fThreshold = (int) (newCapacity * this.fLoadFactor);
        this.fBuckets = newTable;
        this.fTableSize = this.fBuckets.length;
        int i = oldCapacity;
        while (true) {
            int i2 = i - 1;
            if (i > 0) {
                SREntry old = oldTable[i2];
                while (old != null) {
                    SREntry e = old;
                    old = old.next;
                    SREntryData data = (SREntryData) e.get();
                    if (data != null) {
                        int index = hash(data.characters, 0, data.characters.length) % newCapacity;
                        if (newTable[index] != null) {
                            newTable[index].prev = e;
                        }
                        e.next = newTable[index];
                        e.prev = null;
                        newTable[index] = e;
                    } else {
                        this.fCount--;
                    }
                }
                i = i2;
            } else {
                return;
            }
        }
    }

    @Override
    public boolean containsSymbol(String symbol) {
        int bucket = hash(symbol) % this.fTableSize;
        int length = symbol.length();
        for (SREntry entry = this.fBuckets[bucket]; entry != null; entry = entry.next) {
            SREntryData data = (SREntryData) entry.get();
            if (data != null && length == data.characters.length) {
                for (int i = 0; i < length; i++) {
                    if (symbol.charAt(i) != data.characters[i]) {
                        break;
                    }
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean containsSymbol(char[] buffer, int offset, int length) {
        int bucket = hash(buffer, offset, length) % this.fTableSize;
        for (SREntry entry = this.fBuckets[bucket]; entry != null; entry = entry.next) {
            SREntryData data = (SREntryData) entry.get();
            if (data != null && length == data.characters.length) {
                for (int i = 0; i < length; i++) {
                    if (buffer[offset + i] != data.characters[i]) {
                        break;
                    }
                }
                return true;
            }
        }
        return false;
    }

    private void removeEntry(SREntry entry) {
        if (entry.next != null) {
            entry.next.prev = entry.prev;
        }
        if (entry.prev != null) {
            entry.prev.next = entry.next;
        } else {
            this.fBuckets[entry.bucket] = entry.next;
        }
        this.fCount--;
    }

    private void clean() {
        SREntry entry = (SREntry) this.fReferenceQueue.poll();
        while (entry != null) {
            removeEntry(entry);
            entry = (SREntry) this.fReferenceQueue.poll();
        }
    }

    protected static final class SREntry extends SoftReference {
        public int bucket;
        public SREntry next;
        public SREntry prev;

        public SREntry(String internedSymbol, SREntry next, int bucket, ReferenceQueue q) {
            super(new SREntryData(internedSymbol), q);
            initialize(next, bucket);
        }

        public SREntry(String internedSymbol, char[] ch, int offset, int length, SREntry next, int bucket, ReferenceQueue q) {
            super(new SREntryData(internedSymbol, ch, offset, length), q);
            initialize(next, bucket);
        }

        private void initialize(SREntry next, int bucket) {
            this.next = next;
            if (next != null) {
                next.prev = this;
            }
            this.prev = null;
            this.bucket = bucket;
        }
    }

    protected static final class SREntryData {
        public final char[] characters;
        public final String symbol;

        public SREntryData(String internedSymbol) {
            this.symbol = internedSymbol;
            this.characters = new char[this.symbol.length()];
            this.symbol.getChars(0, this.characters.length, this.characters, 0);
        }

        public SREntryData(String internedSymbol, char[] ch, int offset, int length) {
            this.symbol = internedSymbol;
            this.characters = new char[length];
            System.arraycopy(ch, offset, this.characters, 0, length);
        }
    }
}
