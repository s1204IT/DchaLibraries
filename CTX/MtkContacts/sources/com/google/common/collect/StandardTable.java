package com.google.common.collect;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Supplier;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

class StandardTable<R, C, V> extends AbstractTable<R, C, V> implements Serializable {
    private static final long serialVersionUID = 0;

    @GwtTransient
    final Map<R, Map<C, V>> backingMap;
    private transient Set<C> columnKeySet;
    private transient StandardTable<R, C, V>.ColumnMap columnMap;

    @GwtTransient
    final Supplier<? extends Map<C, V>> factory;
    private transient Map<R, Map<C, V>> rowMap;

    StandardTable(Map<R, Map<C, V>> map, Supplier<? extends Map<C, V>> supplier) {
        this.backingMap = map;
        this.factory = supplier;
    }

    @Override
    public boolean contains(Object obj, Object obj2) {
        return (obj == null || obj2 == null || !super.contains(obj, obj2)) ? false : true;
    }

    @Override
    public boolean containsColumn(Object obj) {
        if (obj == null) {
            return false;
        }
        Iterator<Map<C, V>> it = this.backingMap.values().iterator();
        while (it.hasNext()) {
            if (Maps.safeContainsKey(it.next(), obj)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean containsRow(Object obj) {
        return obj != null && Maps.safeContainsKey(this.backingMap, obj);
    }

    @Override
    public boolean containsValue(Object obj) {
        return obj != null && super.containsValue(obj);
    }

    @Override
    public V get(Object obj, Object obj2) {
        if (obj == null || obj2 == null) {
            return null;
        }
        return (V) super.get(obj, obj2);
    }

    @Override
    public boolean isEmpty() {
        return this.backingMap.isEmpty();
    }

    @Override
    public int size() {
        Iterator<Map<C, V>> it = this.backingMap.values().iterator();
        int size = 0;
        while (it.hasNext()) {
            size += it.next().size();
        }
        return size;
    }

    @Override
    public void clear() {
        this.backingMap.clear();
    }

    private Map<C, V> getOrCreate(R r) {
        Map<C, V> map = this.backingMap.get(r);
        if (map == null) {
            Map<C, V> map2 = this.factory.get();
            this.backingMap.put(r, map2);
            return map2;
        }
        return map;
    }

    @Override
    public V put(R r, C c, V v) {
        Preconditions.checkNotNull(r);
        Preconditions.checkNotNull(c);
        Preconditions.checkNotNull(v);
        return getOrCreate(r).put(c, v);
    }

    @Override
    public V remove(Object obj, Object obj2) {
        Map map;
        if (obj == null || obj2 == null || (map = (Map) Maps.safeGet(this.backingMap, obj)) == null) {
            return null;
        }
        V v = (V) map.remove(obj2);
        if (map.isEmpty()) {
            this.backingMap.remove(obj);
        }
        return v;
    }

    private Map<R, V> removeColumn(Object obj) {
        LinkedHashMap linkedHashMap = new LinkedHashMap();
        Iterator<Map.Entry<R, Map<C, V>>> it = this.backingMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<R, Map<C, V>> next = it.next();
            V vRemove = next.getValue().remove(obj);
            if (vRemove != null) {
                linkedHashMap.put(next.getKey(), vRemove);
                if (next.getValue().isEmpty()) {
                    it.remove();
                }
            }
        }
        return linkedHashMap;
    }

    private boolean containsMapping(Object obj, Object obj2, Object obj3) {
        return obj3 != null && obj3.equals(get(obj, obj2));
    }

    private boolean removeMapping(Object obj, Object obj2, Object obj3) {
        if (containsMapping(obj, obj2, obj3)) {
            remove(obj, obj2);
            return true;
        }
        return false;
    }

    private abstract class TableSet<T> extends Sets.ImprovedAbstractSet<T> {
        private TableSet() {
        }

        @Override
        public boolean isEmpty() {
            return StandardTable.this.backingMap.isEmpty();
        }

        @Override
        public void clear() {
            StandardTable.this.backingMap.clear();
        }
    }

    @Override
    public Set<Table.Cell<R, C, V>> cellSet() {
        return super.cellSet();
    }

    @Override
    Iterator<Table.Cell<R, C, V>> cellIterator() {
        return new CellIterator();
    }

    private class CellIterator implements Iterator<Table.Cell<R, C, V>> {
        Iterator<Map.Entry<C, V>> columnIterator;
        Map.Entry<R, Map<C, V>> rowEntry;
        final Iterator<Map.Entry<R, Map<C, V>>> rowIterator;

        private CellIterator() {
            this.rowIterator = StandardTable.this.backingMap.entrySet().iterator();
            this.columnIterator = Iterators.emptyModifiableIterator();
        }

        @Override
        public boolean hasNext() {
            return this.rowIterator.hasNext() || this.columnIterator.hasNext();
        }

        @Override
        public Table.Cell<R, C, V> next() {
            if (!this.columnIterator.hasNext()) {
                this.rowEntry = this.rowIterator.next();
                this.columnIterator = this.rowEntry.getValue().entrySet().iterator();
            }
            Map.Entry<C, V> next = this.columnIterator.next();
            return Tables.immutableCell(this.rowEntry.getKey(), next.getKey(), next.getValue());
        }

        @Override
        public void remove() {
            this.columnIterator.remove();
            if (this.rowEntry.getValue().isEmpty()) {
                this.rowIterator.remove();
            }
        }
    }

    @Override
    public Map<C, V> row(R r) {
        return new Row(r);
    }

    class Row extends Maps.ImprovedAbstractMap<C, V> {
        Map<C, V> backingRowMap;
        final R rowKey;

        Row(R r) {
            this.rowKey = (R) Preconditions.checkNotNull(r);
        }

        Map<C, V> backingRowMap() {
            if (this.backingRowMap == null || (this.backingRowMap.isEmpty() && StandardTable.this.backingMap.containsKey(this.rowKey))) {
                Map<C, V> mapComputeBackingRowMap = computeBackingRowMap();
                this.backingRowMap = mapComputeBackingRowMap;
                return mapComputeBackingRowMap;
            }
            return this.backingRowMap;
        }

        Map<C, V> computeBackingRowMap() {
            return StandardTable.this.backingMap.get(this.rowKey);
        }

        void maintainEmptyInvariant() {
            if (backingRowMap() != null && this.backingRowMap.isEmpty()) {
                StandardTable.this.backingMap.remove(this.rowKey);
                this.backingRowMap = null;
            }
        }

        @Override
        public boolean containsKey(Object obj) {
            Map<C, V> mapBackingRowMap = backingRowMap();
            return (obj == null || mapBackingRowMap == null || !Maps.safeContainsKey(mapBackingRowMap, obj)) ? false : true;
        }

        @Override
        public V get(Object obj) {
            Map<C, V> mapBackingRowMap = backingRowMap();
            if (obj != null && mapBackingRowMap != null) {
                return (V) Maps.safeGet(mapBackingRowMap, obj);
            }
            return null;
        }

        @Override
        public V put(C c, V v) {
            Preconditions.checkNotNull(c);
            Preconditions.checkNotNull(v);
            if (this.backingRowMap != null && !this.backingRowMap.isEmpty()) {
                return this.backingRowMap.put(c, v);
            }
            return (V) StandardTable.this.put(this.rowKey, c, v);
        }

        @Override
        public V remove(Object obj) {
            Map<C, V> mapBackingRowMap = backingRowMap();
            if (mapBackingRowMap == null) {
                return null;
            }
            V v = (V) Maps.safeRemove(mapBackingRowMap, obj);
            maintainEmptyInvariant();
            return v;
        }

        @Override
        public void clear() {
            Map<C, V> mapBackingRowMap = backingRowMap();
            if (mapBackingRowMap != null) {
                mapBackingRowMap.clear();
            }
            maintainEmptyInvariant();
        }

        @Override
        protected Set<Map.Entry<C, V>> createEntrySet() {
            return new RowEntrySet();
        }

        private final class RowEntrySet extends Maps.EntrySet<C, V> {
            private RowEntrySet() {
            }

            @Override
            Map<C, V> map() {
                return Row.this;
            }

            @Override
            public int size() {
                Map<C, V> mapBackingRowMap = Row.this.backingRowMap();
                if (mapBackingRowMap == null) {
                    return 0;
                }
                return mapBackingRowMap.size();
            }

            @Override
            public Iterator<Map.Entry<C, V>> iterator() {
                Map<C, V> mapBackingRowMap = Row.this.backingRowMap();
                if (mapBackingRowMap == null) {
                    return Iterators.emptyModifiableIterator();
                }
                final Iterator<Map.Entry<C, V>> it = mapBackingRowMap.entrySet().iterator();
                return new Iterator<Map.Entry<C, V>>() {
                    @Override
                    public boolean hasNext() {
                        return it.hasNext();
                    }

                    @Override
                    public Map.Entry<C, V> next() {
                        final Map.Entry entry = (Map.Entry) it.next();
                        return new ForwardingMapEntry<C, V>() {
                            @Override
                            protected Map.Entry<C, V> delegate() {
                                return entry;
                            }

                            @Override
                            public V setValue(V v) {
                                return (V) super.setValue(Preconditions.checkNotNull(v));
                            }

                            @Override
                            public boolean equals(Object obj) {
                                return standardEquals(obj);
                            }
                        };
                    }

                    @Override
                    public void remove() {
                        it.remove();
                        Row.this.maintainEmptyInvariant();
                    }
                };
            }
        }
    }

    @Override
    public Map<R, V> column(C c) {
        return new Column(c);
    }

    private class Column extends Maps.ImprovedAbstractMap<R, V> {
        final C columnKey;

        Column(C c) {
            this.columnKey = (C) Preconditions.checkNotNull(c);
        }

        @Override
        public V put(R r, V v) {
            return (V) StandardTable.this.put(r, this.columnKey, v);
        }

        @Override
        public V get(Object obj) {
            return (V) StandardTable.this.get(obj, this.columnKey);
        }

        @Override
        public boolean containsKey(Object obj) {
            return StandardTable.this.contains(obj, this.columnKey);
        }

        @Override
        public V remove(Object obj) {
            return (V) StandardTable.this.remove(obj, this.columnKey);
        }

        boolean removeFromColumnIf(Predicate<? super Map.Entry<R, V>> predicate) {
            Iterator<Map.Entry<R, Map<C, V>>> it = StandardTable.this.backingMap.entrySet().iterator();
            boolean z = false;
            while (it.hasNext()) {
                Map.Entry<R, Map<C, V>> next = it.next();
                Map<C, V> value = next.getValue();
                V v = value.get(this.columnKey);
                if (v != null && predicate.apply(Maps.immutableEntry(next.getKey(), v))) {
                    value.remove(this.columnKey);
                    z = true;
                    if (value.isEmpty()) {
                        it.remove();
                    }
                }
            }
            return z;
        }

        @Override
        Set<Map.Entry<R, V>> createEntrySet() {
            return new EntrySet();
        }

        private class EntrySet extends Sets.ImprovedAbstractSet<Map.Entry<R, V>> {
            private EntrySet() {
            }

            @Override
            public Iterator<Map.Entry<R, V>> iterator() {
                return new EntrySetIterator();
            }

            @Override
            public int size() {
                Iterator<Map<C, V>> it = StandardTable.this.backingMap.values().iterator();
                int i = 0;
                while (it.hasNext()) {
                    if (it.next().containsKey(Column.this.columnKey)) {
                        i++;
                    }
                }
                return i;
            }

            @Override
            public boolean isEmpty() {
                return !StandardTable.this.containsColumn(Column.this.columnKey);
            }

            @Override
            public void clear() {
                Column.this.removeFromColumnIf(Predicates.alwaysTrue());
            }

            @Override
            public boolean contains(Object obj) {
                if (obj instanceof Map.Entry) {
                    Map.Entry entry = (Map.Entry) obj;
                    return StandardTable.this.containsMapping(entry.getKey(), Column.this.columnKey, entry.getValue());
                }
                return false;
            }

            @Override
            public boolean remove(Object obj) {
                if (obj instanceof Map.Entry) {
                    Map.Entry entry = (Map.Entry) obj;
                    return StandardTable.this.removeMapping(entry.getKey(), Column.this.columnKey, entry.getValue());
                }
                return false;
            }

            @Override
            public boolean retainAll(Collection<?> collection) {
                return Column.this.removeFromColumnIf(Predicates.not(Predicates.in(collection)));
            }
        }

        private class EntrySetIterator extends AbstractIterator<Map.Entry<R, V>> {
            final Iterator<Map.Entry<R, Map<C, V>>> iterator;

            private EntrySetIterator() {
                this.iterator = StandardTable.this.backingMap.entrySet().iterator();
            }

            @Override
            protected Map.Entry<R, V> computeNext() {
                while (this.iterator.hasNext()) {
                    final Map.Entry<R, Map<C, V>> next = this.iterator.next();
                    if (next.getValue().containsKey(Column.this.columnKey)) {
                        return new AbstractMapEntry<R, V>() {
                            @Override
                            public R getKey() {
                                return (R) next.getKey();
                            }

                            @Override
                            public V getValue() {
                                return (V) ((Map) next.getValue()).get(Column.this.columnKey);
                            }

                            @Override
                            public V setValue(V v) {
                                return (V) ((Map) next.getValue()).put(Column.this.columnKey, Preconditions.checkNotNull(v));
                            }
                        };
                    }
                }
                return endOfData();
            }
        }

        @Override
        Set<R> createKeySet() {
            return new KeySet();
        }

        private class KeySet extends Maps.KeySet<R, V> {
            KeySet() {
                super(Column.this);
            }

            @Override
            public boolean contains(Object obj) {
                return StandardTable.this.contains(obj, Column.this.columnKey);
            }

            @Override
            public boolean remove(Object obj) {
                return StandardTable.this.remove(obj, Column.this.columnKey) != null;
            }

            @Override
            public boolean retainAll(Collection<?> collection) {
                return Column.this.removeFromColumnIf(Maps.keyPredicateOnEntries(Predicates.not(Predicates.in(collection))));
            }
        }

        @Override
        Collection<V> createValues() {
            return new Values();
        }

        private class Values extends Maps.Values<R, V> {
            Values() {
                super(Column.this);
            }

            @Override
            public boolean remove(Object obj) {
                return obj != null && Column.this.removeFromColumnIf(Maps.valuePredicateOnEntries(Predicates.equalTo(obj)));
            }

            @Override
            public boolean removeAll(Collection<?> collection) {
                return Column.this.removeFromColumnIf(Maps.valuePredicateOnEntries(Predicates.in(collection)));
            }

            @Override
            public boolean retainAll(Collection<?> collection) {
                return Column.this.removeFromColumnIf(Maps.valuePredicateOnEntries(Predicates.not(Predicates.in(collection))));
            }
        }
    }

    @Override
    public Set<R> rowKeySet() {
        return rowMap().keySet();
    }

    @Override
    public Set<C> columnKeySet() {
        Set<C> set = this.columnKeySet;
        if (set != null) {
            return set;
        }
        ColumnKeySet columnKeySet = new ColumnKeySet();
        this.columnKeySet = columnKeySet;
        return columnKeySet;
    }

    private class ColumnKeySet extends StandardTable<R, C, V>.TableSet<C> {
        private ColumnKeySet() {
            super();
        }

        public Iterator<C> iterator() {
            return StandardTable.this.createColumnKeyIterator();
        }

        public int size() {
            return Iterators.size(iterator());
        }

        public boolean remove(Object obj) {
            boolean z = false;
            if (obj == null) {
                return false;
            }
            Iterator<Map<C, V>> it = StandardTable.this.backingMap.values().iterator();
            while (it.hasNext()) {
                Map<C, V> next = it.next();
                if (next.keySet().remove(obj)) {
                    z = true;
                    if (next.isEmpty()) {
                        it.remove();
                    }
                }
            }
            return z;
        }

        public boolean removeAll(Collection<?> collection) {
            Preconditions.checkNotNull(collection);
            Iterator<Map<C, V>> it = StandardTable.this.backingMap.values().iterator();
            boolean z = false;
            while (it.hasNext()) {
                Map<C, V> next = it.next();
                if (Iterators.removeAll(next.keySet().iterator(), collection)) {
                    z = true;
                    if (next.isEmpty()) {
                        it.remove();
                    }
                }
            }
            return z;
        }

        public boolean retainAll(Collection<?> collection) {
            Preconditions.checkNotNull(collection);
            Iterator<Map<C, V>> it = StandardTable.this.backingMap.values().iterator();
            boolean z = false;
            while (it.hasNext()) {
                Map<C, V> next = it.next();
                if (next.keySet().retainAll(collection)) {
                    z = true;
                    if (next.isEmpty()) {
                        it.remove();
                    }
                }
            }
            return z;
        }

        public boolean contains(Object obj) {
            return StandardTable.this.containsColumn(obj);
        }
    }

    Iterator<C> createColumnKeyIterator() {
        return new ColumnKeyIterator();
    }

    private class ColumnKeyIterator extends AbstractIterator<C> {
        Iterator<Map.Entry<C, V>> entryIterator;
        final Iterator<Map<C, V>> mapIterator;
        final Map<C, V> seen;

        private ColumnKeyIterator() {
            this.seen = StandardTable.this.factory.get();
            this.mapIterator = StandardTable.this.backingMap.values().iterator();
            this.entryIterator = Iterators.emptyIterator();
        }

        @Override
        protected C computeNext() {
            while (true) {
                if (this.entryIterator.hasNext()) {
                    Map.Entry<C, V> next = this.entryIterator.next();
                    if (!this.seen.containsKey(next.getKey())) {
                        this.seen.put(next.getKey(), next.getValue());
                        return next.getKey();
                    }
                } else if (this.mapIterator.hasNext()) {
                    this.entryIterator = this.mapIterator.next().entrySet().iterator();
                } else {
                    return endOfData();
                }
            }
        }
    }

    @Override
    public Collection<V> values() {
        return super.values();
    }

    @Override
    public Map<R, Map<C, V>> rowMap() {
        Map<R, Map<C, V>> map = this.rowMap;
        if (map != null) {
            return map;
        }
        Map<R, Map<C, V>> mapCreateRowMap = createRowMap();
        this.rowMap = mapCreateRowMap;
        return mapCreateRowMap;
    }

    Map<R, Map<C, V>> createRowMap() {
        return new RowMap();
    }

    class RowMap extends Maps.ImprovedAbstractMap<R, Map<C, V>> {
        RowMap() {
        }

        @Override
        public boolean containsKey(Object obj) {
            return StandardTable.this.containsRow(obj);
        }

        @Override
        public Map<C, V> get(Object obj) {
            if (StandardTable.this.containsRow(obj)) {
                return StandardTable.this.row(obj);
            }
            return null;
        }

        @Override
        public Map<C, V> remove(Object obj) {
            if (obj == null) {
                return null;
            }
            return StandardTable.this.backingMap.remove(obj);
        }

        @Override
        protected Set<Map.Entry<R, Map<C, V>>> createEntrySet() {
            return new EntrySet();
        }

        class EntrySet extends StandardTable<R, C, V>.TableSet<Map.Entry<R, Map<C, V>>> {
            EntrySet() {
                super();
            }

            public Iterator<Map.Entry<R, Map<C, V>>> iterator() {
                return Maps.asMapEntryIterator(StandardTable.this.backingMap.keySet(), new Function<R, Map<C, V>>() {
                    @Override
                    public Map<C, V> apply(R r) {
                        return StandardTable.this.row(r);
                    }
                });
            }

            public int size() {
                return StandardTable.this.backingMap.size();
            }

            public boolean contains(Object obj) {
                if (!(obj instanceof Map.Entry)) {
                    return false;
                }
                Map.Entry entry = (Map.Entry) obj;
                return entry.getKey() != null && (entry.getValue() instanceof Map) && Collections2.safeContains(StandardTable.this.backingMap.entrySet(), entry);
            }

            public boolean remove(Object obj) {
                if (!(obj instanceof Map.Entry)) {
                    return false;
                }
                Map.Entry entry = (Map.Entry) obj;
                return entry.getKey() != null && (entry.getValue() instanceof Map) && StandardTable.this.backingMap.entrySet().remove(entry);
            }
        }
    }

    @Override
    public Map<C, Map<R, V>> columnMap() {
        StandardTable<R, C, V>.ColumnMap columnMap = this.columnMap;
        if (columnMap != null) {
            return columnMap;
        }
        ColumnMap columnMap2 = new ColumnMap();
        this.columnMap = columnMap2;
        return columnMap2;
    }

    private class ColumnMap extends Maps.ImprovedAbstractMap<C, Map<R, V>> {
        private ColumnMap() {
        }

        @Override
        public Map<R, V> get(Object obj) {
            if (StandardTable.this.containsColumn(obj)) {
                return StandardTable.this.column(obj);
            }
            return null;
        }

        @Override
        public boolean containsKey(Object obj) {
            return StandardTable.this.containsColumn(obj);
        }

        @Override
        public Map<R, V> remove(Object obj) {
            if (StandardTable.this.containsColumn(obj)) {
                return StandardTable.this.removeColumn(obj);
            }
            return null;
        }

        @Override
        public Set<Map.Entry<C, Map<R, V>>> createEntrySet() {
            return new ColumnMapEntrySet();
        }

        @Override
        public Set<C> keySet() {
            return StandardTable.this.columnKeySet();
        }

        @Override
        Collection<Map<R, V>> createValues() {
            return new ColumnMapValues();
        }

        class ColumnMapEntrySet extends StandardTable<R, C, V>.TableSet<Map.Entry<C, Map<R, V>>> {
            ColumnMapEntrySet() {
                super();
            }

            public Iterator<Map.Entry<C, Map<R, V>>> iterator() {
                return Maps.asMapEntryIterator(StandardTable.this.columnKeySet(), new Function<C, Map<R, V>>() {
                    @Override
                    public Map<R, V> apply(C c) {
                        return StandardTable.this.column(c);
                    }
                });
            }

            public int size() {
                return StandardTable.this.columnKeySet().size();
            }

            public boolean contains(Object obj) {
                if (obj instanceof Map.Entry) {
                    Map.Entry entry = (Map.Entry) obj;
                    if (StandardTable.this.containsColumn(entry.getKey())) {
                        return ColumnMap.this.get(entry.getKey()).equals(entry.getValue());
                    }
                    return false;
                }
                return false;
            }

            public boolean remove(Object obj) {
                if (contains(obj)) {
                    StandardTable.this.removeColumn(((Map.Entry) obj).getKey());
                    return true;
                }
                return false;
            }

            public boolean removeAll(Collection<?> collection) {
                Preconditions.checkNotNull(collection);
                return Sets.removeAllImpl(this, collection.iterator());
            }

            public boolean retainAll(Collection<?> collection) {
                Preconditions.checkNotNull(collection);
                boolean z = false;
                for (Object obj : Lists.newArrayList(StandardTable.this.columnKeySet().iterator())) {
                    if (!collection.contains(Maps.immutableEntry(obj, StandardTable.this.column(obj)))) {
                        StandardTable.this.removeColumn(obj);
                        z = true;
                    }
                }
                return z;
            }
        }

        private class ColumnMapValues extends Maps.Values<C, Map<R, V>> {
            ColumnMapValues() {
                super(ColumnMap.this);
            }

            @Override
            public boolean remove(Object obj) {
                for (Map.Entry<C, Map<R, V>> entry : ColumnMap.this.entrySet()) {
                    if (entry.getValue().equals(obj)) {
                        StandardTable.this.removeColumn(entry.getKey());
                        return true;
                    }
                }
                return false;
            }

            @Override
            public boolean removeAll(Collection<?> collection) {
                Preconditions.checkNotNull(collection);
                boolean z = false;
                for (Object obj : Lists.newArrayList(StandardTable.this.columnKeySet().iterator())) {
                    if (collection.contains(StandardTable.this.column(obj))) {
                        StandardTable.this.removeColumn(obj);
                        z = true;
                    }
                }
                return z;
            }

            @Override
            public boolean retainAll(Collection<?> collection) {
                Preconditions.checkNotNull(collection);
                boolean z = false;
                for (Object obj : Lists.newArrayList(StandardTable.this.columnKeySet().iterator())) {
                    if (!collection.contains(StandardTable.this.column(obj))) {
                        StandardTable.this.removeColumn(obj);
                        z = true;
                    }
                }
                return z;
            }
        }
    }
}
