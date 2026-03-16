package com.google.common.collect;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimaps;
import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.RandomAccess;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;

abstract class AbstractMultimap<K, V> implements Multimap<K, V>, Serializable {
    private static final long serialVersionUID = 2447537837011683357L;
    private transient Map<K, Collection<V>> asMap;
    private transient Collection<Map.Entry<K, V>> entries;
    private transient Set<K> keySet;
    private transient Map<K, Collection<V>> map;
    private transient int totalSize;
    private transient Collection<V> valuesCollection;

    abstract Collection<V> createCollection();

    static int access$208(AbstractMultimap x0) {
        int i = x0.totalSize;
        x0.totalSize = i + 1;
        return i;
    }

    static int access$210(AbstractMultimap x0) {
        int i = x0.totalSize;
        x0.totalSize = i - 1;
        return i;
    }

    static int access$212(AbstractMultimap x0, int x1) {
        int i = x0.totalSize + x1;
        x0.totalSize = i;
        return i;
    }

    static int access$220(AbstractMultimap x0, int x1) {
        int i = x0.totalSize - x1;
        x0.totalSize = i;
        return i;
    }

    protected AbstractMultimap(Map<K, Collection<V>> map) {
        Preconditions.checkArgument(map.isEmpty());
        this.map = map;
    }

    final void setMap(Map<K, Collection<V>> map) {
        this.map = map;
        this.totalSize = 0;
        for (Collection<V> values : map.values()) {
            Preconditions.checkArgument(!values.isEmpty());
            this.totalSize += values.size();
        }
    }

    Collection<V> createCollection(K key) {
        return createCollection();
    }

    @Override
    public int size() {
        return this.totalSize;
    }

    @Override
    public boolean containsValue(Object value) {
        for (Collection<V> collection : this.map.values()) {
            if (collection.contains(value)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean containsEntry(Object key, Object value) {
        Collection<V> collection = this.map.get(key);
        return collection != null && collection.contains(value);
    }

    @Override
    public boolean put(K key, V value) {
        Collection<V> collection = getOrCreateCollection(key);
        if (!collection.add(value)) {
            return false;
        }
        this.totalSize++;
        return true;
    }

    private Collection<V> getOrCreateCollection(K key) {
        Collection<V> collection = this.map.get(key);
        if (collection == null) {
            Collection<V> collection2 = createCollection(key);
            this.map.put(key, collection2);
            return collection2;
        }
        return collection;
    }

    @Override
    public boolean remove(Object key, Object value) {
        Collection<V> collection = this.map.get(key);
        if (collection == null) {
            return false;
        }
        boolean changed = collection.remove(value);
        if (changed) {
            this.totalSize--;
            if (collection.isEmpty()) {
                this.map.remove(key);
                return changed;
            }
            return changed;
        }
        return changed;
    }

    @Override
    public boolean putAll(K key, Iterable<? extends V> values) {
        if (!values.iterator().hasNext()) {
            return false;
        }
        Collection<V> collection = getOrCreateCollection(key);
        int oldSize = collection.size();
        boolean changed = false;
        if (values instanceof Collection) {
            Collection<? extends V> c = Collections2.cast(values);
            changed = collection.addAll(c);
        } else {
            for (V value : values) {
                changed |= collection.add(value);
            }
        }
        this.totalSize += collection.size() - oldSize;
        return changed;
    }

    @Override
    public void clear() {
        for (Collection<V> collection : this.map.values()) {
            collection.clear();
        }
        this.map.clear();
        this.totalSize = 0;
    }

    @Override
    public Collection<V> get(K key) {
        Collection<V> collection = this.map.get(key);
        if (collection == null) {
            collection = createCollection(key);
        }
        return wrapCollection(key, collection);
    }

    private Collection<V> wrapCollection(K key, Collection<V> collection) {
        if (collection instanceof SortedSet) {
            return new WrappedSortedSet(key, (SortedSet) collection, null);
        }
        if (collection instanceof Set) {
            return new WrappedSet(key, (Set) collection);
        }
        if (collection instanceof List) {
            return wrapList(key, (List) collection, null);
        }
        return new WrappedCollection(key, collection, null);
    }

    private List<V> wrapList(K key, List<V> list, AbstractMultimap<K, V>.WrappedCollection ancestor) {
        return list instanceof RandomAccess ? new RandomAccessWrappedList(key, list, ancestor) : new WrappedList(key, list, ancestor);
    }

    private class WrappedCollection extends AbstractCollection<V> {
        final AbstractMultimap<K, V>.WrappedCollection ancestor;
        final Collection<V> ancestorDelegate;
        Collection<V> delegate;
        final K key;

        WrappedCollection(K key, Collection<V> delegate, AbstractMultimap<K, V>.WrappedCollection ancestor) {
            this.key = key;
            this.delegate = delegate;
            this.ancestor = ancestor;
            this.ancestorDelegate = ancestor == null ? null : ancestor.getDelegate();
        }

        void refreshIfEmpty() {
            Collection<V> newDelegate;
            if (this.ancestor != null) {
                this.ancestor.refreshIfEmpty();
                if (this.ancestor.getDelegate() != this.ancestorDelegate) {
                    throw new ConcurrentModificationException();
                }
            } else if (this.delegate.isEmpty() && (newDelegate = (Collection) AbstractMultimap.this.map.get(this.key)) != null) {
                this.delegate = newDelegate;
            }
        }

        void removeIfEmpty() {
            if (this.ancestor != null) {
                this.ancestor.removeIfEmpty();
            } else if (this.delegate.isEmpty()) {
                AbstractMultimap.this.map.remove(this.key);
            }
        }

        K getKey() {
            return this.key;
        }

        void addToMap() {
            if (this.ancestor == null) {
                AbstractMultimap.this.map.put(this.key, this.delegate);
            } else {
                this.ancestor.addToMap();
            }
        }

        @Override
        public int size() {
            refreshIfEmpty();
            return this.delegate.size();
        }

        @Override
        public boolean equals(Object object) {
            if (object == this) {
                return true;
            }
            refreshIfEmpty();
            return this.delegate.equals(object);
        }

        @Override
        public int hashCode() {
            refreshIfEmpty();
            return this.delegate.hashCode();
        }

        @Override
        public String toString() {
            refreshIfEmpty();
            return this.delegate.toString();
        }

        Collection<V> getDelegate() {
            return this.delegate;
        }

        @Override
        public Iterator<V> iterator() {
            refreshIfEmpty();
            return new WrappedIterator();
        }

        class WrappedIterator implements Iterator<V> {
            final Iterator<V> delegateIterator;
            final Collection<V> originalDelegate;

            WrappedIterator() {
                this.originalDelegate = WrappedCollection.this.delegate;
                this.delegateIterator = AbstractMultimap.this.iteratorOrListIterator(WrappedCollection.this.delegate);
            }

            WrappedIterator(Iterator<V> delegateIterator) {
                this.originalDelegate = WrappedCollection.this.delegate;
                this.delegateIterator = delegateIterator;
            }

            void validateIterator() {
                WrappedCollection.this.refreshIfEmpty();
                if (WrappedCollection.this.delegate != this.originalDelegate) {
                    throw new ConcurrentModificationException();
                }
            }

            @Override
            public boolean hasNext() {
                validateIterator();
                return this.delegateIterator.hasNext();
            }

            @Override
            public V next() {
                validateIterator();
                return this.delegateIterator.next();
            }

            @Override
            public void remove() {
                this.delegateIterator.remove();
                AbstractMultimap.access$210(AbstractMultimap.this);
                WrappedCollection.this.removeIfEmpty();
            }

            Iterator<V> getDelegateIterator() {
                validateIterator();
                return this.delegateIterator;
            }
        }

        @Override
        public boolean add(V value) {
            refreshIfEmpty();
            boolean wasEmpty = this.delegate.isEmpty();
            boolean changed = this.delegate.add(value);
            if (changed) {
                AbstractMultimap.access$208(AbstractMultimap.this);
                if (wasEmpty) {
                    addToMap();
                }
            }
            return changed;
        }

        AbstractMultimap<K, V>.WrappedCollection getAncestor() {
            return this.ancestor;
        }

        @Override
        public boolean addAll(Collection<? extends V> collection) {
            if (collection.isEmpty()) {
                return false;
            }
            int oldSize = size();
            boolean changed = this.delegate.addAll(collection);
            if (changed) {
                int newSize = this.delegate.size();
                AbstractMultimap.access$212(AbstractMultimap.this, newSize - oldSize);
                if (oldSize == 0) {
                    addToMap();
                    return changed;
                }
                return changed;
            }
            return changed;
        }

        @Override
        public boolean contains(Object o) {
            refreshIfEmpty();
            return this.delegate.contains(o);
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            refreshIfEmpty();
            return this.delegate.containsAll(c);
        }

        @Override
        public void clear() {
            int oldSize = size();
            if (oldSize != 0) {
                this.delegate.clear();
                AbstractMultimap.access$220(AbstractMultimap.this, oldSize);
                removeIfEmpty();
            }
        }

        @Override
        public boolean remove(Object o) {
            refreshIfEmpty();
            boolean changed = this.delegate.remove(o);
            if (changed) {
                AbstractMultimap.access$210(AbstractMultimap.this);
                removeIfEmpty();
            }
            return changed;
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            if (c.isEmpty()) {
                return false;
            }
            int oldSize = size();
            boolean changed = this.delegate.removeAll(c);
            if (changed) {
                int newSize = this.delegate.size();
                AbstractMultimap.access$212(AbstractMultimap.this, newSize - oldSize);
                removeIfEmpty();
                return changed;
            }
            return changed;
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            Preconditions.checkNotNull(c);
            int oldSize = size();
            boolean changed = this.delegate.retainAll(c);
            if (changed) {
                int newSize = this.delegate.size();
                AbstractMultimap.access$212(AbstractMultimap.this, newSize - oldSize);
                removeIfEmpty();
            }
            return changed;
        }
    }

    private Iterator<V> iteratorOrListIterator(Collection<V> collection) {
        return collection instanceof List ? ((List) collection).listIterator() : collection.iterator();
    }

    private class WrappedSet extends AbstractMultimap<K, V>.WrappedCollection implements Set<V> {
        WrappedSet(K key, Set<V> delegate) {
            super(key, delegate, null);
        }
    }

    private class WrappedSortedSet extends AbstractMultimap<K, V>.WrappedCollection implements SortedSet<V> {
        WrappedSortedSet(K key, SortedSet<V> delegate, AbstractMultimap<K, V>.WrappedCollection ancestor) {
            super(key, delegate, ancestor);
        }

        SortedSet<V> getSortedSetDelegate() {
            return (SortedSet) getDelegate();
        }

        @Override
        public Comparator<? super V> comparator() {
            return getSortedSetDelegate().comparator();
        }

        @Override
        public V first() {
            refreshIfEmpty();
            return getSortedSetDelegate().first();
        }

        @Override
        public V last() {
            refreshIfEmpty();
            return getSortedSetDelegate().last();
        }

        @Override
        public SortedSet<V> headSet(V v) {
            refreshIfEmpty();
            AbstractMultimap abstractMultimap = AbstractMultimap.this;
            Object key = getKey();
            SortedSet<V> sortedSetHeadSet = getSortedSetDelegate().headSet(v);
            AbstractMultimap<K, V>.WrappedCollection ancestor = getAncestor();
            ?? ancestor2 = this;
            if (ancestor != null) {
                ancestor2 = getAncestor();
            }
            return new WrappedSortedSet(key, sortedSetHeadSet, ancestor2);
        }

        @Override
        public SortedSet<V> subSet(V v, V v2) {
            refreshIfEmpty();
            AbstractMultimap abstractMultimap = AbstractMultimap.this;
            Object key = getKey();
            SortedSet<V> sortedSetSubSet = getSortedSetDelegate().subSet(v, v2);
            AbstractMultimap<K, V>.WrappedCollection ancestor = getAncestor();
            ?? ancestor2 = this;
            if (ancestor != null) {
                ancestor2 = getAncestor();
            }
            return new WrappedSortedSet(key, sortedSetSubSet, ancestor2);
        }

        @Override
        public SortedSet<V> tailSet(V v) {
            refreshIfEmpty();
            AbstractMultimap abstractMultimap = AbstractMultimap.this;
            Object key = getKey();
            SortedSet<V> sortedSetTailSet = getSortedSetDelegate().tailSet(v);
            AbstractMultimap<K, V>.WrappedCollection ancestor = getAncestor();
            ?? ancestor2 = this;
            if (ancestor != null) {
                ancestor2 = getAncestor();
            }
            return new WrappedSortedSet(key, sortedSetTailSet, ancestor2);
        }
    }

    private class WrappedList extends AbstractMultimap<K, V>.WrappedCollection implements List<V> {
        WrappedList(K key, List<V> delegate, AbstractMultimap<K, V>.WrappedCollection ancestor) {
            super(key, delegate, ancestor);
        }

        List<V> getListDelegate() {
            return (List) getDelegate();
        }

        @Override
        public boolean addAll(int index, Collection<? extends V> c) {
            if (c.isEmpty()) {
                return false;
            }
            int oldSize = size();
            boolean changed = getListDelegate().addAll(index, c);
            if (changed) {
                int newSize = getDelegate().size();
                AbstractMultimap.access$212(AbstractMultimap.this, newSize - oldSize);
                if (oldSize == 0) {
                    addToMap();
                    return changed;
                }
                return changed;
            }
            return changed;
        }

        @Override
        public V get(int index) {
            refreshIfEmpty();
            return getListDelegate().get(index);
        }

        @Override
        public V set(int index, V element) {
            refreshIfEmpty();
            return getListDelegate().set(index, element);
        }

        @Override
        public void add(int index, V element) {
            refreshIfEmpty();
            boolean wasEmpty = getDelegate().isEmpty();
            getListDelegate().add(index, element);
            AbstractMultimap.access$208(AbstractMultimap.this);
            if (wasEmpty) {
                addToMap();
            }
        }

        @Override
        public V remove(int index) {
            refreshIfEmpty();
            V value = getListDelegate().remove(index);
            AbstractMultimap.access$210(AbstractMultimap.this);
            removeIfEmpty();
            return value;
        }

        @Override
        public int indexOf(Object o) {
            refreshIfEmpty();
            return getListDelegate().indexOf(o);
        }

        @Override
        public int lastIndexOf(Object o) {
            refreshIfEmpty();
            return getListDelegate().lastIndexOf(o);
        }

        @Override
        public ListIterator<V> listIterator() {
            refreshIfEmpty();
            return new WrappedListIterator();
        }

        @Override
        public ListIterator<V> listIterator(int index) {
            refreshIfEmpty();
            return new WrappedListIterator(index);
        }

        @Override
        public List<V> subList(int i, int i2) {
            refreshIfEmpty();
            AbstractMultimap abstractMultimap = AbstractMultimap.this;
            Object key = getKey();
            List<V> listSubList = getListDelegate().subList(i, i2);
            AbstractMultimap<K, V>.WrappedCollection ancestor = getAncestor();
            ?? ancestor2 = this;
            if (ancestor != null) {
                ancestor2 = getAncestor();
            }
            return abstractMultimap.wrapList(key, listSubList, ancestor2);
        }

        private class WrappedListIterator extends AbstractMultimap<K, V>.WrappedCollection.WrappedIterator implements ListIterator<V> {
            WrappedListIterator() {
                super();
            }

            public WrappedListIterator(int index) {
                super(WrappedList.this.getListDelegate().listIterator(index));
            }

            private ListIterator<V> getDelegateListIterator() {
                return (ListIterator) getDelegateIterator();
            }

            @Override
            public boolean hasPrevious() {
                return getDelegateListIterator().hasPrevious();
            }

            @Override
            public V previous() {
                return getDelegateListIterator().previous();
            }

            @Override
            public int nextIndex() {
                return getDelegateListIterator().nextIndex();
            }

            @Override
            public int previousIndex() {
                return getDelegateListIterator().previousIndex();
            }

            @Override
            public void set(V value) {
                getDelegateListIterator().set(value);
            }

            @Override
            public void add(V value) {
                boolean wasEmpty = WrappedList.this.isEmpty();
                getDelegateListIterator().add(value);
                AbstractMultimap.access$208(AbstractMultimap.this);
                if (wasEmpty) {
                    WrappedList.this.addToMap();
                }
            }
        }
    }

    private class RandomAccessWrappedList extends AbstractMultimap<K, V>.WrappedList implements RandomAccess {
        RandomAccessWrappedList(K key, List<V> delegate, AbstractMultimap<K, V>.WrappedCollection ancestor) {
            super(key, delegate, ancestor);
        }
    }

    public Set<K> keySet() {
        Set<K> result = this.keySet;
        if (result != null) {
            return result;
        }
        Set<K> result2 = createKeySet();
        this.keySet = result2;
        return result2;
    }

    private Set<K> createKeySet() {
        return this.map instanceof SortedMap ? new SortedKeySet((SortedMap) this.map) : new KeySet(this.map);
    }

    private class KeySet extends Maps.KeySet<K, Collection<V>> {
        final Map<K, Collection<V>> subMap;

        KeySet(Map<K, Collection<V>> subMap) {
            this.subMap = subMap;
        }

        @Override
        Map<K, Collection<V>> map() {
            return this.subMap;
        }

        @Override
        public Iterator<K> iterator() {
            return new Iterator<K>() {
                Map.Entry<K, Collection<V>> entry;
                final Iterator<Map.Entry<K, Collection<V>>> entryIterator;

                {
                    this.entryIterator = KeySet.this.subMap.entrySet().iterator();
                }

                @Override
                public boolean hasNext() {
                    return this.entryIterator.hasNext();
                }

                @Override
                public K next() {
                    this.entry = this.entryIterator.next();
                    return this.entry.getKey();
                }

                @Override
                public void remove() {
                    Preconditions.checkState(this.entry != null);
                    Collection<V> collection = this.entry.getValue();
                    this.entryIterator.remove();
                    AbstractMultimap.access$220(AbstractMultimap.this, collection.size());
                    collection.clear();
                }
            };
        }

        @Override
        public boolean remove(Object key) {
            int count = 0;
            Collection<V> collection = this.subMap.remove(key);
            if (collection != null) {
                count = collection.size();
                collection.clear();
                AbstractMultimap.access$220(AbstractMultimap.this, count);
            }
            return count > 0;
        }

        @Override
        public void clear() {
            Iterators.clear(iterator());
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            return this.subMap.keySet().containsAll(c);
        }

        @Override
        public boolean equals(Object object) {
            return this == object || this.subMap.keySet().equals(object);
        }

        @Override
        public int hashCode() {
            return this.subMap.keySet().hashCode();
        }
    }

    private class SortedKeySet extends AbstractMultimap<K, V>.KeySet implements SortedSet<K> {
        SortedKeySet(SortedMap<K, Collection<V>> subMap) {
            super(subMap);
        }

        SortedMap<K, Collection<V>> sortedMap() {
            return (SortedMap) this.subMap;
        }

        @Override
        public Comparator<? super K> comparator() {
            return sortedMap().comparator();
        }

        @Override
        public K first() {
            return sortedMap().firstKey();
        }

        @Override
        public SortedSet<K> headSet(K toElement) {
            return new SortedKeySet(sortedMap().headMap(toElement));
        }

        @Override
        public K last() {
            return sortedMap().lastKey();
        }

        @Override
        public SortedSet<K> subSet(K fromElement, K toElement) {
            return new SortedKeySet(sortedMap().subMap(fromElement, toElement));
        }

        @Override
        public SortedSet<K> tailSet(K fromElement) {
            return new SortedKeySet(sortedMap().tailMap(fromElement));
        }
    }

    private int removeValuesForKey(Object key) {
        try {
            Collection<V> collection = this.map.remove(key);
            if (collection == null) {
                return 0;
            }
            int count = collection.size();
            collection.clear();
            this.totalSize -= count;
            return count;
        } catch (ClassCastException e) {
            return 0;
        } catch (NullPointerException e2) {
            return 0;
        }
    }

    @Override
    public Collection<V> values() {
        Collection<V> result = this.valuesCollection;
        if (result == null) {
            Collection<V> result2 = new Multimaps.Values<K, V>() {
                @Override
                Multimap<K, V> multimap() {
                    return AbstractMultimap.this;
                }
            };
            this.valuesCollection = result2;
            return result2;
        }
        return result;
    }

    @Override
    public Collection<Map.Entry<K, V>> entries() {
        Collection<Map.Entry<K, V>> result = this.entries;
        if (result != null) {
            return result;
        }
        Collection<Map.Entry<K, V>> result2 = createEntries();
        this.entries = result2;
        return result2;
    }

    Collection<Map.Entry<K, V>> createEntries() {
        return this instanceof SetMultimap ? new Multimaps.EntrySet<K, V>() {
            @Override
            Multimap<K, V> multimap() {
                return AbstractMultimap.this;
            }

            @Override
            public Iterator<Map.Entry<K, V>> iterator() {
                return AbstractMultimap.this.createEntryIterator();
            }
        } : new Multimaps.Entries<K, V>() {
            @Override
            Multimap<K, V> multimap() {
                return AbstractMultimap.this;
            }

            @Override
            public Iterator<Map.Entry<K, V>> iterator() {
                return AbstractMultimap.this.createEntryIterator();
            }
        };
    }

    Iterator<Map.Entry<K, V>> createEntryIterator() {
        return new EntryIterator();
    }

    private class EntryIterator implements Iterator<Map.Entry<K, V>> {
        Collection<V> collection;
        K key;
        final Iterator<Map.Entry<K, Collection<V>>> keyIterator;
        Iterator<V> valueIterator;

        EntryIterator() {
            this.keyIterator = AbstractMultimap.this.map.entrySet().iterator();
            if (this.keyIterator.hasNext()) {
                findValueIteratorAndKey();
            } else {
                this.valueIterator = Iterators.emptyModifiableIterator();
            }
        }

        void findValueIteratorAndKey() {
            Map.Entry<K, Collection<V>> entry = this.keyIterator.next();
            this.key = entry.getKey();
            this.collection = entry.getValue();
            this.valueIterator = this.collection.iterator();
        }

        @Override
        public boolean hasNext() {
            return this.keyIterator.hasNext() || this.valueIterator.hasNext();
        }

        @Override
        public Map.Entry<K, V> next() {
            if (!this.valueIterator.hasNext()) {
                findValueIteratorAndKey();
            }
            return Maps.immutableEntry(this.key, this.valueIterator.next());
        }

        @Override
        public void remove() {
            this.valueIterator.remove();
            if (this.collection.isEmpty()) {
                this.keyIterator.remove();
            }
            AbstractMultimap.access$210(AbstractMultimap.this);
        }
    }

    @Override
    public Map<K, Collection<V>> asMap() {
        Map<K, Collection<V>> result = this.asMap;
        if (result != null) {
            return result;
        }
        Map<K, Collection<V>> result2 = createAsMap();
        this.asMap = result2;
        return result2;
    }

    private Map<K, Collection<V>> createAsMap() {
        return this.map instanceof SortedMap ? new SortedAsMap((SortedMap) this.map) : new AsMap(this.map);
    }

    private class AsMap extends AbstractMap<K, Collection<V>> {
        transient Set<Map.Entry<K, Collection<V>>> entrySet;
        final transient Map<K, Collection<V>> submap;

        AsMap(Map<K, Collection<V>> submap) {
            this.submap = submap;
        }

        @Override
        public Set<Map.Entry<K, Collection<V>>> entrySet() {
            Set<Map.Entry<K, Collection<V>>> result = this.entrySet;
            if (result != null) {
                return result;
            }
            Set<Map.Entry<K, Collection<V>>> result2 = new AsMapEntries();
            this.entrySet = result2;
            return result2;
        }

        @Override
        public boolean containsKey(Object key) {
            return Maps.safeContainsKey(this.submap, key);
        }

        @Override
        public Collection<V> get(Object key) {
            Collection<V> collection = (Collection) Maps.safeGet(this.submap, key);
            if (collection != null) {
                return AbstractMultimap.this.wrapCollection(key, collection);
            }
            return null;
        }

        @Override
        public Set<K> keySet() {
            return AbstractMultimap.this.keySet();
        }

        @Override
        public int size() {
            return this.submap.size();
        }

        @Override
        public Collection<V> remove(Object key) {
            Collection<V> collection = this.submap.remove(key);
            if (collection == null) {
                return null;
            }
            Collection<V> output = AbstractMultimap.this.createCollection();
            output.addAll(collection);
            AbstractMultimap.access$220(AbstractMultimap.this, collection.size());
            collection.clear();
            return output;
        }

        @Override
        public boolean equals(Object object) {
            return this == object || this.submap.equals(object);
        }

        @Override
        public int hashCode() {
            return this.submap.hashCode();
        }

        @Override
        public String toString() {
            return this.submap.toString();
        }

        @Override
        public void clear() {
            if (this.submap == AbstractMultimap.this.map) {
                AbstractMultimap.this.clear();
            } else {
                Iterators.clear(new AsMapIterator());
            }
        }

        class AsMapEntries extends Maps.EntrySet<K, Collection<V>> {
            AsMapEntries() {
            }

            @Override
            Map<K, Collection<V>> map() {
                return AsMap.this;
            }

            @Override
            public Iterator<Map.Entry<K, Collection<V>>> iterator() {
                return AsMap.this.new AsMapIterator();
            }

            @Override
            public boolean contains(Object o) {
                return Collections2.safeContains(AsMap.this.submap.entrySet(), o);
            }

            @Override
            public boolean remove(Object o) {
                if (!contains(o)) {
                    return false;
                }
                Map.Entry<?, ?> entry = (Map.Entry) o;
                AbstractMultimap.this.removeValuesForKey(entry.getKey());
                return true;
            }
        }

        class AsMapIterator implements Iterator<Map.Entry<K, Collection<V>>> {
            Collection<V> collection;
            final Iterator<Map.Entry<K, Collection<V>>> delegateIterator;

            AsMapIterator() {
                this.delegateIterator = AsMap.this.submap.entrySet().iterator();
            }

            @Override
            public boolean hasNext() {
                return this.delegateIterator.hasNext();
            }

            @Override
            public Map.Entry<K, Collection<V>> next() {
                Map.Entry<K, Collection<V>> entry = this.delegateIterator.next();
                K key = entry.getKey();
                this.collection = entry.getValue();
                return Maps.immutableEntry(key, AbstractMultimap.this.wrapCollection(key, this.collection));
            }

            @Override
            public void remove() {
                this.delegateIterator.remove();
                AbstractMultimap.access$220(AbstractMultimap.this, this.collection.size());
                this.collection.clear();
            }
        }
    }

    private class SortedAsMap extends AbstractMultimap<K, V>.AsMap implements SortedMap<K, Collection<V>> {
        SortedSet<K> sortedKeySet;

        SortedAsMap(SortedMap<K, Collection<V>> submap) {
            super(submap);
        }

        SortedMap<K, Collection<V>> sortedMap() {
            return (SortedMap) this.submap;
        }

        @Override
        public Comparator<? super K> comparator() {
            return sortedMap().comparator();
        }

        @Override
        public K firstKey() {
            return sortedMap().firstKey();
        }

        @Override
        public K lastKey() {
            return sortedMap().lastKey();
        }

        @Override
        public SortedMap<K, Collection<V>> headMap(K toKey) {
            return new SortedAsMap(sortedMap().headMap(toKey));
        }

        @Override
        public SortedMap<K, Collection<V>> subMap(K fromKey, K toKey) {
            return new SortedAsMap(sortedMap().subMap(fromKey, toKey));
        }

        @Override
        public SortedMap<K, Collection<V>> tailMap(K fromKey) {
            return new SortedAsMap(sortedMap().tailMap(fromKey));
        }

        @Override
        public SortedSet<K> keySet() {
            SortedSet<K> result = this.sortedKeySet;
            if (result != null) {
                return result;
            }
            SortedKeySet sortedKeySet = new SortedKeySet(sortedMap());
            this.sortedKeySet = sortedKeySet;
            return sortedKeySet;
        }
    }

    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (object instanceof Multimap) {
            Multimap<?, ?> that = (Multimap) object;
            return this.map.equals(that.asMap());
        }
        return false;
    }

    public int hashCode() {
        return this.map.hashCode();
    }

    public String toString() {
        return this.map.toString();
    }
}
