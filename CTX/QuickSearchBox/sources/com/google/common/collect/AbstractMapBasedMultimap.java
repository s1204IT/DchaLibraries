package com.google.common.collect;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import java.io.Serializable;
import java.util.AbstractCollection;
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

abstract class AbstractMapBasedMultimap<K, V> extends AbstractMultimap<K, V> implements Serializable {
    private static final long serialVersionUID = 2447537837011683357L;
    private transient Map<K, Collection<V>> map;
    private transient int totalSize;

    private class AsMap extends Maps.ImprovedAbstractMap<K, Collection<V>> {
        final transient Map<K, Collection<V>> submap;
        final AbstractMapBasedMultimap this$0;

        class AsMapEntries extends Maps.EntrySet<K, Collection<V>> {
            final AsMap this$1;

            AsMapEntries(AsMap asMap) {
                this.this$1 = asMap;
            }

            @Override
            public boolean contains(Object obj) {
                return Collections2.safeContains(this.this$1.submap.entrySet(), obj);
            }

            @Override
            public Iterator<Map.Entry<K, Collection<V>>> iterator() {
                return new AsMapIterator(this.this$1);
            }

            @Override
            Map<K, Collection<V>> map() {
                return this.this$1;
            }

            @Override
            public boolean remove(Object obj) {
                if (!contains(obj)) {
                    return false;
                }
                this.this$1.this$0.removeValuesForKey(((Map.Entry) obj).getKey());
                return true;
            }
        }

        class AsMapIterator implements Iterator<Map.Entry<K, Collection<V>>> {
            Collection<V> collection;
            final Iterator<Map.Entry<K, Collection<V>>> delegateIterator;
            final AsMap this$1;

            AsMapIterator(AsMap asMap) {
                this.this$1 = asMap;
                this.delegateIterator = this.this$1.submap.entrySet().iterator();
            }

            @Override
            public boolean hasNext() {
                return this.delegateIterator.hasNext();
            }

            @Override
            public Map.Entry<K, Collection<V>> next() {
                Map.Entry<K, Collection<V>> next = this.delegateIterator.next();
                this.collection = next.getValue();
                return this.this$1.wrapEntry(next);
            }

            @Override
            public void remove() {
                this.delegateIterator.remove();
                AbstractMapBasedMultimap.access$220(this.this$1.this$0, this.collection.size());
                this.collection.clear();
            }
        }

        AsMap(AbstractMapBasedMultimap abstractMapBasedMultimap, Map<K, Collection<V>> map) {
            this.this$0 = abstractMapBasedMultimap;
            this.submap = map;
        }

        @Override
        public void clear() {
            if (this.submap == this.this$0.map) {
                this.this$0.clear();
            } else {
                Iterators.clear(new AsMapIterator(this));
            }
        }

        @Override
        public boolean containsKey(Object obj) {
            return Maps.safeContainsKey(this.submap, obj);
        }

        @Override
        protected Set<Map.Entry<K, Collection<V>>> createEntrySet() {
            return new AsMapEntries(this);
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj || this.submap.equals(obj);
        }

        @Override
        public Collection<V> get(Object obj) {
            Collection<V> collection = (Collection) Maps.safeGet(this.submap, obj);
            if (collection == null) {
                return null;
            }
            return this.this$0.wrapCollection(obj, collection);
        }

        @Override
        public int hashCode() {
            return this.submap.hashCode();
        }

        @Override
        public Set<K> keySet() {
            return this.this$0.keySet();
        }

        @Override
        public Collection<V> remove(Object obj) {
            Collection<V> collectionRemove = this.submap.remove(obj);
            if (collectionRemove == null) {
                return null;
            }
            Collection<V> collectionCreateCollection = this.this$0.createCollection();
            collectionCreateCollection.addAll(collectionRemove);
            AbstractMapBasedMultimap.access$220(this.this$0, collectionRemove.size());
            collectionRemove.clear();
            return collectionCreateCollection;
        }

        @Override
        public int size() {
            return this.submap.size();
        }

        @Override
        public String toString() {
            return this.submap.toString();
        }

        Map.Entry<K, Collection<V>> wrapEntry(Map.Entry<K, Collection<V>> entry) {
            K key = entry.getKey();
            return Maps.immutableEntry(key, this.this$0.wrapCollection(key, entry.getValue()));
        }
    }

    private abstract class Itr<T> implements Iterator<T> {
        final Iterator<Map.Entry<K, Collection<V>>> keyIterator;
        final AbstractMapBasedMultimap this$0;
        K key = null;
        Collection<V> collection = null;
        Iterator<V> valueIterator = Iterators.emptyModifiableIterator();

        Itr(AbstractMapBasedMultimap abstractMapBasedMultimap) {
            this.this$0 = abstractMapBasedMultimap;
            this.keyIterator = abstractMapBasedMultimap.map.entrySet().iterator();
        }

        @Override
        public boolean hasNext() {
            return this.keyIterator.hasNext() || this.valueIterator.hasNext();
        }

        @Override
        public T next() {
            if (!this.valueIterator.hasNext()) {
                Map.Entry<K, Collection<V>> next = this.keyIterator.next();
                this.key = next.getKey();
                this.collection = next.getValue();
                this.valueIterator = this.collection.iterator();
            }
            return output(this.key, this.valueIterator.next());
        }

        abstract T output(K k, V v);

        @Override
        public void remove() {
            this.valueIterator.remove();
            if (this.collection.isEmpty()) {
                this.keyIterator.remove();
            }
            AbstractMapBasedMultimap.access$210(this.this$0);
        }
    }

    private class KeySet extends Maps.KeySet<K, Collection<V>> {
        final AbstractMapBasedMultimap this$0;

        KeySet(AbstractMapBasedMultimap abstractMapBasedMultimap, Map<K, Collection<V>> map) {
            super(map);
            this.this$0 = abstractMapBasedMultimap;
        }

        @Override
        public void clear() {
            Iterators.clear(iterator());
        }

        @Override
        public boolean containsAll(Collection<?> collection) {
            return map().keySet().containsAll(collection);
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj || map().keySet().equals(obj);
        }

        @Override
        public int hashCode() {
            return map().keySet().hashCode();
        }

        @Override
        public Iterator<K> iterator() {
            return new Iterator<K>(this, map().entrySet().iterator()) {
                Map.Entry<K, Collection<V>> entry;
                final KeySet this$1;
                final Iterator val$entryIterator;

                {
                    this.this$1 = this;
                    this.val$entryIterator = it;
                }

                @Override
                public boolean hasNext() {
                    return this.val$entryIterator.hasNext();
                }

                @Override
                public K next() {
                    this.entry = (Map.Entry) this.val$entryIterator.next();
                    return this.entry.getKey();
                }

                @Override
                public void remove() {
                    CollectPreconditions.checkRemove(this.entry != null);
                    Collection<V> value = this.entry.getValue();
                    this.val$entryIterator.remove();
                    AbstractMapBasedMultimap.access$220(this.this$1.this$0, value.size());
                    value.clear();
                }
            };
        }

        @Override
        public boolean remove(Object obj) {
            int i;
            Collection<V> collectionRemove = map().remove(obj);
            if (collectionRemove != null) {
                int size = collectionRemove.size();
                collectionRemove.clear();
                AbstractMapBasedMultimap.access$220(this.this$0, size);
                i = size;
            } else {
                i = 0;
            }
            return i > 0;
        }
    }

    private class RandomAccessWrappedList extends AbstractMapBasedMultimap<K, V>.WrappedList implements RandomAccess {
        final AbstractMapBasedMultimap this$0;

        RandomAccessWrappedList(AbstractMapBasedMultimap abstractMapBasedMultimap, K k, List<V> list, AbstractMapBasedMultimap<K, V>.WrappedCollection wrappedCollection) {
            super(abstractMapBasedMultimap, k, list, wrappedCollection);
            this.this$0 = abstractMapBasedMultimap;
        }
    }

    private class SortedAsMap extends AbstractMapBasedMultimap<K, V>.AsMap implements SortedMap<K, Collection<V>> {
        SortedSet<K> sortedKeySet;
        final AbstractMapBasedMultimap this$0;

        SortedAsMap(AbstractMapBasedMultimap abstractMapBasedMultimap, SortedMap<K, Collection<V>> sortedMap) {
            super(abstractMapBasedMultimap, sortedMap);
            this.this$0 = abstractMapBasedMultimap;
        }

        @Override
        public Comparator<? super K> comparator() {
            return sortedMap().comparator();
        }

        public SortedSet<K> createKeySet() {
            return new SortedKeySet(this.this$0, sortedMap());
        }

        @Override
        public K firstKey() {
            return sortedMap().firstKey();
        }

        @Override
        public SortedMap<K, Collection<V>> headMap(K k) {
            return new SortedAsMap(this.this$0, sortedMap().headMap(k));
        }

        @Override
        public SortedSet<K> keySet() {
            SortedSet<K> sortedSet = this.sortedKeySet;
            if (sortedSet != null) {
                return sortedSet;
            }
            SortedSet<K> sortedSetCreateKeySet = createKeySet();
            this.sortedKeySet = sortedSetCreateKeySet;
            return sortedSetCreateKeySet;
        }

        @Override
        public K lastKey() {
            return sortedMap().lastKey();
        }

        SortedMap<K, Collection<V>> sortedMap() {
            return (SortedMap) this.submap;
        }

        @Override
        public SortedMap<K, Collection<V>> subMap(K k, K k2) {
            return new SortedAsMap(this.this$0, sortedMap().subMap(k, k2));
        }

        @Override
        public SortedMap<K, Collection<V>> tailMap(K k) {
            return new SortedAsMap(this.this$0, sortedMap().tailMap(k));
        }
    }

    private class SortedKeySet extends AbstractMapBasedMultimap<K, V>.KeySet implements SortedSet<K> {
        final AbstractMapBasedMultimap this$0;

        SortedKeySet(AbstractMapBasedMultimap abstractMapBasedMultimap, SortedMap<K, Collection<V>> sortedMap) {
            super(abstractMapBasedMultimap, sortedMap);
            this.this$0 = abstractMapBasedMultimap;
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
        public SortedSet<K> headSet(K k) {
            return new SortedKeySet(this.this$0, sortedMap().headMap(k));
        }

        @Override
        public K last() {
            return sortedMap().lastKey();
        }

        SortedMap<K, Collection<V>> sortedMap() {
            return (SortedMap) super.map();
        }

        @Override
        public SortedSet<K> subSet(K k, K k2) {
            return new SortedKeySet(this.this$0, sortedMap().subMap(k, k2));
        }

        @Override
        public SortedSet<K> tailSet(K k) {
            return new SortedKeySet(this.this$0, sortedMap().tailMap(k));
        }
    }

    private class WrappedCollection extends AbstractCollection<V> {
        final AbstractMapBasedMultimap<K, V>.WrappedCollection ancestor;
        final Collection<V> ancestorDelegate;
        Collection<V> delegate;
        final K key;
        final AbstractMapBasedMultimap this$0;

        class WrappedIterator implements Iterator<V> {
            final Iterator<V> delegateIterator;
            final Collection<V> originalDelegate;
            final WrappedCollection this$1;

            WrappedIterator(WrappedCollection wrappedCollection) {
                this.this$1 = wrappedCollection;
                this.originalDelegate = this.this$1.delegate;
                this.delegateIterator = wrappedCollection.this$0.iteratorOrListIterator(wrappedCollection.delegate);
            }

            WrappedIterator(WrappedCollection wrappedCollection, Iterator<V> it) {
                this.this$1 = wrappedCollection;
                this.originalDelegate = this.this$1.delegate;
                this.delegateIterator = it;
            }

            Iterator<V> getDelegateIterator() {
                validateIterator();
                return this.delegateIterator;
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
                AbstractMapBasedMultimap.access$210(this.this$1.this$0);
                this.this$1.removeIfEmpty();
            }

            void validateIterator() {
                this.this$1.refreshIfEmpty();
                if (this.this$1.delegate != this.originalDelegate) {
                    throw new ConcurrentModificationException();
                }
            }
        }

        WrappedCollection(AbstractMapBasedMultimap abstractMapBasedMultimap, K k, Collection<V> collection, AbstractMapBasedMultimap<K, V>.WrappedCollection wrappedCollection) {
            this.this$0 = abstractMapBasedMultimap;
            this.key = k;
            this.delegate = collection;
            this.ancestor = wrappedCollection;
            this.ancestorDelegate = wrappedCollection == null ? null : wrappedCollection.getDelegate();
        }

        @Override
        public boolean add(V v) {
            refreshIfEmpty();
            boolean zIsEmpty = this.delegate.isEmpty();
            boolean zAdd = this.delegate.add(v);
            if (zAdd) {
                AbstractMapBasedMultimap.access$208(this.this$0);
                if (zIsEmpty) {
                    addToMap();
                }
            }
            return zAdd;
        }

        @Override
        public boolean addAll(Collection<? extends V> collection) {
            if (collection.isEmpty()) {
                return false;
            }
            int size = size();
            boolean zAddAll = this.delegate.addAll(collection);
            if (!zAddAll) {
                return zAddAll;
            }
            AbstractMapBasedMultimap.access$212(this.this$0, this.delegate.size() - size);
            if (size != 0) {
                return zAddAll;
            }
            addToMap();
            return zAddAll;
        }

        void addToMap() {
            if (this.ancestor != null) {
                this.ancestor.addToMap();
            } else {
                this.this$0.map.put(this.key, this.delegate);
            }
        }

        @Override
        public void clear() {
            int size = size();
            if (size == 0) {
                return;
            }
            this.delegate.clear();
            AbstractMapBasedMultimap.access$220(this.this$0, size);
            removeIfEmpty();
        }

        @Override
        public boolean contains(Object obj) {
            refreshIfEmpty();
            return this.delegate.contains(obj);
        }

        @Override
        public boolean containsAll(Collection<?> collection) {
            refreshIfEmpty();
            return this.delegate.containsAll(collection);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            refreshIfEmpty();
            return this.delegate.equals(obj);
        }

        AbstractMapBasedMultimap<K, V>.WrappedCollection getAncestor() {
            return this.ancestor;
        }

        Collection<V> getDelegate() {
            return this.delegate;
        }

        K getKey() {
            return this.key;
        }

        @Override
        public int hashCode() {
            refreshIfEmpty();
            return this.delegate.hashCode();
        }

        @Override
        public Iterator<V> iterator() {
            refreshIfEmpty();
            return new WrappedIterator(this);
        }

        void refreshIfEmpty() {
            Collection<V> collection;
            if (this.ancestor != null) {
                this.ancestor.refreshIfEmpty();
                if (this.ancestor.getDelegate() != this.ancestorDelegate) {
                    throw new ConcurrentModificationException();
                }
            } else {
                if (!this.delegate.isEmpty() || (collection = (Collection) this.this$0.map.get(this.key)) == null) {
                    return;
                }
                this.delegate = collection;
            }
        }

        @Override
        public boolean remove(Object obj) {
            refreshIfEmpty();
            boolean zRemove = this.delegate.remove(obj);
            if (zRemove) {
                AbstractMapBasedMultimap.access$210(this.this$0);
                removeIfEmpty();
            }
            return zRemove;
        }

        @Override
        public boolean removeAll(Collection<?> collection) {
            if (collection.isEmpty()) {
                return false;
            }
            int size = size();
            boolean zRemoveAll = this.delegate.removeAll(collection);
            if (!zRemoveAll) {
                return zRemoveAll;
            }
            AbstractMapBasedMultimap.access$212(this.this$0, this.delegate.size() - size);
            removeIfEmpty();
            return zRemoveAll;
        }

        void removeIfEmpty() {
            if (this.ancestor != null) {
                this.ancestor.removeIfEmpty();
            } else if (this.delegate.isEmpty()) {
                this.this$0.map.remove(this.key);
            }
        }

        @Override
        public boolean retainAll(Collection<?> collection) {
            Preconditions.checkNotNull(collection);
            int size = size();
            boolean zRetainAll = this.delegate.retainAll(collection);
            if (zRetainAll) {
                AbstractMapBasedMultimap.access$212(this.this$0, this.delegate.size() - size);
                removeIfEmpty();
            }
            return zRetainAll;
        }

        @Override
        public int size() {
            refreshIfEmpty();
            return this.delegate.size();
        }

        @Override
        public String toString() {
            refreshIfEmpty();
            return this.delegate.toString();
        }
    }

    private class WrappedList extends AbstractMapBasedMultimap<K, V>.WrappedCollection implements List<V> {
        final AbstractMapBasedMultimap this$0;

        private class WrappedListIterator extends AbstractMapBasedMultimap<K, V>.WrappedCollection.WrappedIterator implements ListIterator<V> {
            final WrappedList this$1;

            WrappedListIterator(WrappedList wrappedList) {
                super(wrappedList);
                this.this$1 = wrappedList;
            }

            public WrappedListIterator(WrappedList wrappedList, int i) {
                super(wrappedList, wrappedList.getListDelegate().listIterator(i));
                this.this$1 = wrappedList;
            }

            private ListIterator<V> getDelegateListIterator() {
                return (ListIterator) getDelegateIterator();
            }

            @Override
            public void add(V v) {
                boolean zIsEmpty = this.this$1.isEmpty();
                getDelegateListIterator().add(v);
                AbstractMapBasedMultimap.access$208(this.this$1.this$0);
                if (zIsEmpty) {
                    this.this$1.addToMap();
                }
            }

            @Override
            public boolean hasPrevious() {
                return getDelegateListIterator().hasPrevious();
            }

            @Override
            public int nextIndex() {
                return getDelegateListIterator().nextIndex();
            }

            @Override
            public V previous() {
                return getDelegateListIterator().previous();
            }

            @Override
            public int previousIndex() {
                return getDelegateListIterator().previousIndex();
            }

            @Override
            public void set(V v) {
                getDelegateListIterator().set(v);
            }
        }

        WrappedList(AbstractMapBasedMultimap abstractMapBasedMultimap, K k, List<V> list, AbstractMapBasedMultimap<K, V>.WrappedCollection wrappedCollection) {
            super(abstractMapBasedMultimap, k, list, wrappedCollection);
            this.this$0 = abstractMapBasedMultimap;
        }

        @Override
        public void add(int i, V v) {
            refreshIfEmpty();
            boolean zIsEmpty = getDelegate().isEmpty();
            getListDelegate().add(i, v);
            AbstractMapBasedMultimap.access$208(this.this$0);
            if (zIsEmpty) {
                addToMap();
            }
        }

        @Override
        public boolean addAll(int i, Collection<? extends V> collection) {
            if (collection.isEmpty()) {
                return false;
            }
            int size = size();
            boolean zAddAll = getListDelegate().addAll(i, collection);
            if (!zAddAll) {
                return zAddAll;
            }
            AbstractMapBasedMultimap.access$212(this.this$0, getDelegate().size() - size);
            if (size != 0) {
                return zAddAll;
            }
            addToMap();
            return zAddAll;
        }

        @Override
        public V get(int i) {
            refreshIfEmpty();
            return getListDelegate().get(i);
        }

        List<V> getListDelegate() {
            return (List) getDelegate();
        }

        @Override
        public int indexOf(Object obj) {
            refreshIfEmpty();
            return getListDelegate().indexOf(obj);
        }

        @Override
        public int lastIndexOf(Object obj) {
            refreshIfEmpty();
            return getListDelegate().lastIndexOf(obj);
        }

        @Override
        public ListIterator<V> listIterator() {
            refreshIfEmpty();
            return new WrappedListIterator(this);
        }

        @Override
        public ListIterator<V> listIterator(int i) {
            refreshIfEmpty();
            return new WrappedListIterator(this, i);
        }

        @Override
        public V remove(int i) {
            refreshIfEmpty();
            V vRemove = getListDelegate().remove(i);
            AbstractMapBasedMultimap.access$210(this.this$0);
            removeIfEmpty();
            return vRemove;
        }

        @Override
        public V set(int i, V v) {
            refreshIfEmpty();
            return getListDelegate().set(i, v);
        }

        @Override
        public List<V> subList(int i, int i2) {
            refreshIfEmpty();
            AbstractMapBasedMultimap abstractMapBasedMultimap = this.this$0;
            Object key = getKey();
            List<V> listSubList = getListDelegate().subList(i, i2);
            WrappedCollection ancestor = getAncestor();
            ?? ancestor2 = this;
            if (ancestor != null) {
                ancestor2 = getAncestor();
            }
            return abstractMapBasedMultimap.wrapList(key, listSubList, ancestor2);
        }
    }

    private class WrappedSet extends AbstractMapBasedMultimap<K, V>.WrappedCollection implements Set<V> {
        final AbstractMapBasedMultimap this$0;

        WrappedSet(AbstractMapBasedMultimap abstractMapBasedMultimap, K k, Set<V> set) {
            super(abstractMapBasedMultimap, k, set, null);
            this.this$0 = abstractMapBasedMultimap;
        }

        @Override
        public boolean removeAll(Collection<?> collection) {
            if (collection.isEmpty()) {
                return false;
            }
            int size = size();
            boolean zRemoveAllImpl = Sets.removeAllImpl((Set<?>) this.delegate, collection);
            if (!zRemoveAllImpl) {
                return zRemoveAllImpl;
            }
            AbstractMapBasedMultimap.access$212(this.this$0, this.delegate.size() - size);
            removeIfEmpty();
            return zRemoveAllImpl;
        }
    }

    private class WrappedSortedSet extends AbstractMapBasedMultimap<K, V>.WrappedCollection implements SortedSet<V> {
        final AbstractMapBasedMultimap this$0;

        WrappedSortedSet(AbstractMapBasedMultimap abstractMapBasedMultimap, K k, SortedSet<V> sortedSet, AbstractMapBasedMultimap<K, V>.WrappedCollection wrappedCollection) {
            super(abstractMapBasedMultimap, k, sortedSet, wrappedCollection);
            this.this$0 = abstractMapBasedMultimap;
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

        SortedSet<V> getSortedSetDelegate() {
            return (SortedSet) getDelegate();
        }

        @Override
        public SortedSet<V> headSet(V v) {
            refreshIfEmpty();
            AbstractMapBasedMultimap abstractMapBasedMultimap = this.this$0;
            Object key = getKey();
            SortedSet<V> sortedSetHeadSet = getSortedSetDelegate().headSet(v);
            WrappedCollection ancestor = getAncestor();
            ?? ancestor2 = this;
            if (ancestor != null) {
                ancestor2 = getAncestor();
            }
            return new WrappedSortedSet(abstractMapBasedMultimap, key, sortedSetHeadSet, ancestor2);
        }

        @Override
        public V last() {
            refreshIfEmpty();
            return getSortedSetDelegate().last();
        }

        @Override
        public SortedSet<V> subSet(V v, V v2) {
            refreshIfEmpty();
            AbstractMapBasedMultimap abstractMapBasedMultimap = this.this$0;
            Object key = getKey();
            SortedSet<V> sortedSetSubSet = getSortedSetDelegate().subSet(v, v2);
            WrappedCollection ancestor = getAncestor();
            ?? ancestor2 = this;
            if (ancestor != null) {
                ancestor2 = getAncestor();
            }
            return new WrappedSortedSet(abstractMapBasedMultimap, key, sortedSetSubSet, ancestor2);
        }

        @Override
        public SortedSet<V> tailSet(V v) {
            refreshIfEmpty();
            AbstractMapBasedMultimap abstractMapBasedMultimap = this.this$0;
            Object key = getKey();
            SortedSet<V> sortedSetTailSet = getSortedSetDelegate().tailSet(v);
            WrappedCollection ancestor = getAncestor();
            ?? ancestor2 = this;
            if (ancestor != null) {
                ancestor2 = getAncestor();
            }
            return new WrappedSortedSet(abstractMapBasedMultimap, key, sortedSetTailSet, ancestor2);
        }
    }

    static int access$208(AbstractMapBasedMultimap abstractMapBasedMultimap) {
        int i = abstractMapBasedMultimap.totalSize;
        abstractMapBasedMultimap.totalSize = i + 1;
        return i;
    }

    static int access$210(AbstractMapBasedMultimap abstractMapBasedMultimap) {
        int i = abstractMapBasedMultimap.totalSize;
        abstractMapBasedMultimap.totalSize = i - 1;
        return i;
    }

    static int access$212(AbstractMapBasedMultimap abstractMapBasedMultimap, int i) {
        int i2 = abstractMapBasedMultimap.totalSize + i;
        abstractMapBasedMultimap.totalSize = i2;
        return i2;
    }

    static int access$220(AbstractMapBasedMultimap abstractMapBasedMultimap, int i) {
        int i2 = abstractMapBasedMultimap.totalSize - i;
        abstractMapBasedMultimap.totalSize = i2;
        return i2;
    }

    public Iterator<V> iteratorOrListIterator(Collection<V> collection) {
        return collection instanceof List ? ((List) collection).listIterator() : collection.iterator();
    }

    public int removeValuesForKey(Object obj) {
        Collection collection = (Collection) Maps.safeRemove(this.map, obj);
        if (collection == null) {
            return 0;
        }
        int size = collection.size();
        collection.clear();
        this.totalSize -= size;
        return size;
    }

    public List<V> wrapList(K k, List<V> list, AbstractMapBasedMultimap<K, V>.WrappedCollection wrappedCollection) {
        return list instanceof RandomAccess ? new RandomAccessWrappedList(this, k, list, wrappedCollection) : new WrappedList(this, k, list, wrappedCollection);
    }

    @Override
    public void clear() {
        Iterator<Collection<V>> it = this.map.values().iterator();
        while (it.hasNext()) {
            it.next().clear();
        }
        this.map.clear();
        this.totalSize = 0;
    }

    @Override
    Map<K, Collection<V>> createAsMap() {
        return this.map instanceof SortedMap ? new SortedAsMap(this, (SortedMap) this.map) : new AsMap(this, this.map);
    }

    abstract Collection<V> createCollection();

    Collection<V> createCollection(K k) {
        return createCollection();
    }

    @Override
    Set<K> createKeySet() {
        return this.map instanceof SortedMap ? new SortedKeySet(this, (SortedMap) this.map) : new KeySet(this, this.map);
    }

    @Override
    public Collection<Map.Entry<K, V>> entries() {
        return super.entries();
    }

    @Override
    Iterator<Map.Entry<K, V>> entryIterator() {
        return new AbstractMapBasedMultimap<K, V>.Itr<Map.Entry<K, V>>(this) {
            final AbstractMapBasedMultimap this$0;

            {
                this.this$0 = this;
            }

            public Map.Entry<K, V> output(K k, V v) {
                return Maps.immutableEntry(k, v);
            }
        };
    }

    final void setMap(Map<K, Collection<V>> map) {
        this.map = map;
        this.totalSize = 0;
        for (Collection<V> collection : map.values()) {
            Preconditions.checkArgument(!collection.isEmpty());
            this.totalSize = collection.size() + this.totalSize;
        }
    }

    @Override
    public int size() {
        return this.totalSize;
    }

    Collection<V> wrapCollection(K k, Collection<V> collection) {
        return collection instanceof SortedSet ? new WrappedSortedSet(this, k, (SortedSet) collection, null) : collection instanceof Set ? new WrappedSet(this, k, (Set) collection) : collection instanceof List ? wrapList(k, (List) collection, null) : new WrappedCollection(this, k, collection, null);
    }
}
