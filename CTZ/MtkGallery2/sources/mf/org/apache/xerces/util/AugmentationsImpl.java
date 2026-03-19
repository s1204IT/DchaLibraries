package mf.org.apache.xerces.util;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import mf.org.apache.xerces.xni.Augmentations;

public class AugmentationsImpl implements Augmentations {
    private AugmentationsItemsContainer fAugmentationsContainer = new SmallContainer();

    @Override
    public Object putItem(String key, Object item) {
        Object oldValue = this.fAugmentationsContainer.putItem(key, item);
        if (oldValue == null && this.fAugmentationsContainer.isFull()) {
            this.fAugmentationsContainer = this.fAugmentationsContainer.expand();
        }
        return oldValue;
    }

    @Override
    public Object getItem(String key) {
        return this.fAugmentationsContainer.getItem(key);
    }

    @Override
    public Object removeItem(String key) {
        return this.fAugmentationsContainer.removeItem(key);
    }

    @Override
    public Enumeration keys() {
        return this.fAugmentationsContainer.keys();
    }

    @Override
    public void removeAllItems() {
        this.fAugmentationsContainer.clear();
    }

    public String toString() {
        return this.fAugmentationsContainer.toString();
    }

    static abstract class AugmentationsItemsContainer {
        public abstract void clear();

        public abstract AugmentationsItemsContainer expand();

        public abstract Object getItem(Object obj);

        public abstract boolean isFull();

        public abstract Enumeration keys();

        public abstract Object putItem(Object obj, Object obj2);

        public abstract Object removeItem(Object obj);

        AugmentationsItemsContainer() {
        }
    }

    static final class SmallContainer extends AugmentationsItemsContainer {
        static final int SIZE_LIMIT = 10;
        final Object[] fAugmentations = new Object[20];
        int fNumEntries = 0;

        SmallContainer() {
        }

        @Override
        public Enumeration keys() {
            return new SmallContainerKeyEnumeration();
        }

        @Override
        public Object getItem(Object key) {
            for (int i = 0; i < this.fNumEntries * 2; i += 2) {
                if (this.fAugmentations[i].equals(key)) {
                    return this.fAugmentations[i + 1];
                }
            }
            return null;
        }

        @Override
        public Object putItem(Object key, Object item) {
            for (int i = 0; i < this.fNumEntries * 2; i += 2) {
                if (this.fAugmentations[i].equals(key)) {
                    Object oldValue = this.fAugmentations[i + 1];
                    this.fAugmentations[i + 1] = item;
                    return oldValue;
                }
            }
            this.fAugmentations[this.fNumEntries * 2] = key;
            this.fAugmentations[(this.fNumEntries * 2) + 1] = item;
            this.fNumEntries++;
            return null;
        }

        @Override
        public Object removeItem(Object key) {
            for (int i = 0; i < this.fNumEntries * 2; i += 2) {
                if (this.fAugmentations[i].equals(key)) {
                    Object oldValue = this.fAugmentations[i + 1];
                    for (int j = i; j < (this.fNumEntries * 2) - 2; j += 2) {
                        this.fAugmentations[j] = this.fAugmentations[j + 2];
                        this.fAugmentations[j + 1] = this.fAugmentations[j + 3];
                    }
                    this.fAugmentations[(this.fNumEntries * 2) - 2] = null;
                    this.fAugmentations[(this.fNumEntries * 2) - 1] = null;
                    this.fNumEntries--;
                    return oldValue;
                }
            }
            return null;
        }

        @Override
        public void clear() {
            for (int i = 0; i < this.fNumEntries * 2; i += 2) {
                this.fAugmentations[i] = null;
                this.fAugmentations[i + 1] = null;
            }
            this.fNumEntries = 0;
        }

        @Override
        public boolean isFull() {
            return this.fNumEntries == 10;
        }

        @Override
        public AugmentationsItemsContainer expand() {
            LargeContainer expandedContainer = new LargeContainer();
            for (int i = 0; i < this.fNumEntries * 2; i += 2) {
                expandedContainer.putItem(this.fAugmentations[i], this.fAugmentations[i + 1]);
            }
            return expandedContainer;
        }

        public String toString() {
            StringBuffer buff = new StringBuffer();
            buff.append("SmallContainer - fNumEntries == ");
            buff.append(this.fNumEntries);
            for (int i = 0; i < 20; i += 2) {
                buff.append("\nfAugmentations[");
                buff.append(i);
                buff.append("] == ");
                buff.append(this.fAugmentations[i]);
                buff.append("; fAugmentations[");
                buff.append(i + 1);
                buff.append("] == ");
                buff.append(this.fAugmentations[i + 1]);
            }
            return buff.toString();
        }

        final class SmallContainerKeyEnumeration implements Enumeration {
            Object[] enumArray;
            int next = 0;

            SmallContainerKeyEnumeration() {
                this.enumArray = new Object[SmallContainer.this.fNumEntries];
                for (int i = 0; i < SmallContainer.this.fNumEntries; i++) {
                    this.enumArray[i] = SmallContainer.this.fAugmentations[i * 2];
                }
            }

            @Override
            public boolean hasMoreElements() {
                return this.next < this.enumArray.length;
            }

            @Override
            public Object nextElement() {
                if (this.next >= this.enumArray.length) {
                    throw new NoSuchElementException();
                }
                Object nextVal = this.enumArray[this.next];
                this.enumArray[this.next] = null;
                this.next++;
                return nextVal;
            }
        }
    }

    static final class LargeContainer extends AugmentationsItemsContainer {
        private final HashMap fAugmentations = new HashMap();

        LargeContainer() {
        }

        @Override
        public Object getItem(Object key) {
            return this.fAugmentations.get(key);
        }

        @Override
        public Object putItem(Object key, Object item) {
            return this.fAugmentations.put(key, item);
        }

        @Override
        public Object removeItem(Object key) {
            return this.fAugmentations.remove(key);
        }

        @Override
        public Enumeration keys() {
            return Collections.enumeration(this.fAugmentations.keySet());
        }

        @Override
        public void clear() {
            this.fAugmentations.clear();
        }

        @Override
        public boolean isFull() {
            return false;
        }

        @Override
        public AugmentationsItemsContainer expand() {
            return this;
        }

        public String toString() {
            StringBuffer buff = new StringBuffer();
            buff.append("LargeContainer");
            for (Map.Entry entry : this.fAugmentations.entrySet()) {
                buff.append("\nkey == ");
                buff.append(entry.getKey());
                buff.append("; value == ");
                buff.append(entry.getValue());
            }
            return buff.toString();
        }
    }
}
