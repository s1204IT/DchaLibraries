package org.apache.commons.logging.impl;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

@Deprecated
public final class WeakHashtable extends Hashtable {
    private static final int MAX_CHANGES_BEFORE_PURGE = 100;
    private static final int PARTIAL_PURGE_COUNT = 10;
    private ReferenceQueue queue = new ReferenceQueue();
    private int changeCount = 0;

    @Override
    public boolean containsKey(Object obj) {
        return super.containsKey(new Referenced(obj));
    }

    @Override
    public Enumeration elements() {
        purge();
        return super.elements();
    }

    @Override
    public Set entrySet() {
        purge();
        Set<Map.Entry> setEntrySet = super.entrySet();
        HashSet hashSet = new HashSet();
        for (Map.Entry entry : setEntrySet) {
            Object value = ((Referenced) entry.getKey()).getValue();
            Object value2 = entry.getValue();
            if (value != null) {
                hashSet.add(new Entry(value, value2));
            }
        }
        return hashSet;
    }

    @Override
    public Object get(Object obj) {
        return super.get(new Referenced(obj));
    }

    @Override
    public Enumeration keys() {
        purge();
        final Enumeration enumerationKeys = super.keys();
        return new Enumeration() {
            @Override
            public boolean hasMoreElements() {
                return enumerationKeys.hasMoreElements();
            }

            @Override
            public Object nextElement() {
                return ((Referenced) enumerationKeys.nextElement()).getValue();
            }
        };
    }

    @Override
    public Set keySet() {
        purge();
        Set setKeySet = super.keySet();
        HashSet hashSet = new HashSet();
        Iterator it = setKeySet.iterator();
        while (it.hasNext()) {
            Object value = ((Referenced) it.next()).getValue();
            if (value != null) {
                hashSet.add(value);
            }
        }
        return hashSet;
    }

    @Override
    public Object put(Object obj, Object obj2) {
        if (obj == null) {
            throw new NullPointerException("Null keys are not allowed");
        }
        if (obj2 == null) {
            throw new NullPointerException("Null values are not allowed");
        }
        int i = this.changeCount;
        this.changeCount = i + 1;
        if (i > 100) {
            purge();
            this.changeCount = 0;
        } else if (this.changeCount % 10 == 0) {
            purgeOne();
        }
        return super.put(new Referenced(obj, this.queue), obj2);
    }

    @Override
    public void putAll(Map map) {
        if (map != null) {
            for (Map.Entry entry : map.entrySet()) {
                put(entry.getKey(), entry.getValue());
            }
        }
    }

    @Override
    public Collection values() {
        purge();
        return super.values();
    }

    @Override
    public Object remove(Object obj) {
        int i = this.changeCount;
        this.changeCount = i + 1;
        if (i > 100) {
            purge();
            this.changeCount = 0;
        } else if (this.changeCount % 10 == 0) {
            purgeOne();
        }
        return super.remove(new Referenced(obj));
    }

    @Override
    public boolean isEmpty() {
        purge();
        return super.isEmpty();
    }

    @Override
    public int size() {
        purge();
        return super.size();
    }

    @Override
    public String toString() {
        purge();
        return super.toString();
    }

    @Override
    protected void rehash() {
        purge();
        super.rehash();
    }

    private void purge() {
        synchronized (this.queue) {
            while (true) {
                WeakKey weakKey = (WeakKey) this.queue.poll();
                if (weakKey != null) {
                    super.remove(weakKey.getReferenced());
                }
            }
        }
    }

    private void purgeOne() {
        synchronized (this.queue) {
            WeakKey weakKey = (WeakKey) this.queue.poll();
            if (weakKey != null) {
                super.remove(weakKey.getReferenced());
            }
        }
    }

    private static final class Entry implements Map.Entry {
        private final Object key;
        private final Object value;

        private Entry(Object obj, Object obj2) {
            this.key = obj;
            this.value = obj2;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null || !(obj instanceof Map.Entry)) {
                return false;
            }
            Map.Entry entry = (Map.Entry) obj;
            if (getKey() == null) {
                if (entry.getKey() != null) {
                    return false;
                }
            } else if (!getKey().equals(entry.getKey())) {
                return false;
            }
            if (getValue() == null) {
                if (entry.getValue() != null) {
                    return false;
                }
            } else if (!getValue().equals(entry.getValue())) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int iHashCode;
            if (getKey() != null) {
                iHashCode = getKey().hashCode();
            } else {
                iHashCode = 0;
            }
            return iHashCode ^ (getValue() != null ? getValue().hashCode() : 0);
        }

        @Override
        public Object setValue(Object obj) {
            throw new UnsupportedOperationException("Entry.setValue is not supported.");
        }

        @Override
        public Object getValue() {
            return this.value;
        }

        @Override
        public Object getKey() {
            return this.key;
        }
    }

    private static final class Referenced {
        private final int hashCode;
        private final WeakReference reference;

        private Referenced(Object obj) {
            this.reference = new WeakReference(obj);
            this.hashCode = obj.hashCode();
        }

        private Referenced(Object obj, ReferenceQueue referenceQueue) {
            this.reference = new WeakKey(obj, referenceQueue, this);
            this.hashCode = obj.hashCode();
        }

        public int hashCode() {
            return this.hashCode;
        }

        private Object getValue() {
            return this.reference.get();
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof Referenced)) {
                return false;
            }
            Referenced referenced = (Referenced) obj;
            Object value = getValue();
            Object value2 = referenced.getValue();
            if (value == null) {
                boolean z = value2 == null;
                if (z) {
                    return hashCode() == referenced.hashCode();
                }
                return z;
            }
            return value.equals(value2);
        }
    }

    private static final class WeakKey extends WeakReference {
        private final Referenced referenced;

        private WeakKey(Object obj, ReferenceQueue referenceQueue, Referenced referenced) {
            super(obj, referenceQueue);
            this.referenced = referenced;
        }

        private Referenced getReferenced() {
            return this.referenced;
        }
    }
}
