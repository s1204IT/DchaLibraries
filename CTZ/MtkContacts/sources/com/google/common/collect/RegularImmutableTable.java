package com.google.common.collect;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Table;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

abstract class RegularImmutableTable<R, C, V> extends ImmutableTable<R, C, V> {
    abstract Table.Cell<R, C, V> getCell(int i);

    abstract V getValue(int i);

    RegularImmutableTable() {
    }

    @Override
    final ImmutableSet<Table.Cell<R, C, V>> createCellSet() {
        return isEmpty() ? ImmutableSet.of() : new CellSet();
    }

    private final class CellSet extends ImmutableSet<Table.Cell<R, C, V>> {
        private CellSet() {
        }

        @Override
        public int size() {
            return RegularImmutableTable.this.size();
        }

        @Override
        public UnmodifiableIterator<Table.Cell<R, C, V>> iterator() {
            return asList().iterator();
        }

        @Override
        ImmutableList<Table.Cell<R, C, V>> createAsList() {
            return new ImmutableAsList<Table.Cell<R, C, V>>() {
                @Override
                public Table.Cell<R, C, V> get(int i) {
                    return RegularImmutableTable.this.getCell(i);
                }

                @Override
                ImmutableCollection<Table.Cell<R, C, V>> delegateCollection() {
                    return CellSet.this;
                }
            };
        }

        @Override
        public boolean contains(Object obj) {
            if (!(obj instanceof Table.Cell)) {
                return false;
            }
            Table.Cell cell = (Table.Cell) obj;
            Object obj2 = RegularImmutableTable.this.get(cell.getRowKey(), cell.getColumnKey());
            return obj2 != null && obj2.equals(cell.getValue());
        }

        @Override
        boolean isPartialView() {
            return false;
        }
    }

    @Override
    final ImmutableCollection<V> createValues() {
        return isEmpty() ? ImmutableList.of() : new Values();
    }

    private final class Values extends ImmutableList<V> {
        private Values() {
        }

        @Override
        public int size() {
            return RegularImmutableTable.this.size();
        }

        @Override
        public V get(int i) {
            return (V) RegularImmutableTable.this.getValue(i);
        }

        @Override
        boolean isPartialView() {
            return true;
        }
    }

    static <R, C, V> RegularImmutableTable<R, C, V> forCells(List<Table.Cell<R, C, V>> list, final Comparator<? super R> comparator, final Comparator<? super C> comparator2) {
        Preconditions.checkNotNull(list);
        if (comparator != null || comparator2 != null) {
            Collections.sort(list, new Comparator<Table.Cell<R, C, V>>() {
                @Override
                public int compare(Table.Cell<R, C, V> cell, Table.Cell<R, C, V> cell2) {
                    int iCompare;
                    if (comparator != null) {
                        iCompare = comparator.compare(cell.getRowKey(), cell2.getRowKey());
                    } else {
                        iCompare = 0;
                    }
                    if (iCompare != 0) {
                        return iCompare;
                    }
                    if (comparator2 == null) {
                        return 0;
                    }
                    return comparator2.compare(cell.getColumnKey(), cell2.getColumnKey());
                }
            });
        }
        return forCellsInternal(list, comparator, comparator2);
    }

    static <R, C, V> RegularImmutableTable<R, C, V> forCells(Iterable<Table.Cell<R, C, V>> iterable) {
        return forCellsInternal(iterable, null, null);
    }

    private static final <R, C, V> RegularImmutableTable<R, C, V> forCellsInternal(Iterable<Table.Cell<R, C, V>> iterable, Comparator<? super R> comparator, Comparator<? super C> comparator2) {
        ImmutableSet.Builder builder = ImmutableSet.builder();
        ImmutableSet.Builder builder2 = ImmutableSet.builder();
        ImmutableList immutableListCopyOf = ImmutableList.copyOf(iterable);
        UnmodifiableIterator it = immutableListCopyOf.iterator();
        while (it.hasNext()) {
            Table.Cell cell = (Table.Cell) it.next();
            builder.add(cell.getRowKey());
            builder2.add(cell.getColumnKey());
        }
        ImmutableSet immutableSetBuild = builder.build();
        if (comparator != null) {
            ArrayList arrayListNewArrayList = Lists.newArrayList(immutableSetBuild);
            Collections.sort(arrayListNewArrayList, comparator);
            immutableSetBuild = ImmutableSet.copyOf((Collection) arrayListNewArrayList);
        }
        ImmutableSet immutableSetBuild2 = builder2.build();
        if (comparator2 != null) {
            ArrayList arrayListNewArrayList2 = Lists.newArrayList(immutableSetBuild2);
            Collections.sort(arrayListNewArrayList2, comparator2);
            immutableSetBuild2 = ImmutableSet.copyOf((Collection) arrayListNewArrayList2);
        }
        if (immutableListCopyOf.size() > (((long) immutableSetBuild.size()) * ((long) immutableSetBuild2.size())) / 2) {
            return new DenseImmutableTable(immutableListCopyOf, immutableSetBuild, immutableSetBuild2);
        }
        return new SparseImmutableTable(immutableListCopyOf, immutableSetBuild, immutableSetBuild2);
    }
}
