package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableCollection;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Set;
import javax.annotation.Nullable;

@GwtCompatible(emulated = true, serializable = true)
public abstract class ImmutableSet<E> extends ImmutableCollection<E> implements Set<E> {
    @Override
    public abstract UnmodifiableIterator<E> iterator();

    public static <E> ImmutableSet<E> of() {
        return EmptyImmutableSet.INSTANCE;
    }

    public static <E> ImmutableSet<E> of(E element) {
        return new SingletonImmutableSet(element);
    }

    public static <E> ImmutableSet<E> construct(int n, Object... elements) {
        Object[] uniqueElements;
        switch (n) {
            case 0:
                return of();
            case 1:
                return of(elements[0]);
            default:
                int tableSize = chooseTableSize(n);
                Object[] table = new Object[tableSize];
                int mask = tableSize - 1;
                int hashCode = 0;
                int uniques = 0;
                int i = 0;
                while (true) {
                    int uniques2 = uniques;
                    if (i < n) {
                        Object element = ObjectArrays.checkElementNotNull(elements[i], i);
                        int hash = element.hashCode();
                        int j = Hashing.smear(hash);
                        while (true) {
                            int index = j & mask;
                            Object value = table[index];
                            if (value == null) {
                                uniques = uniques2 + 1;
                                elements[uniques2] = element;
                                table[index] = element;
                                hashCode += hash;
                            } else if (value.equals(element)) {
                                uniques = uniques2;
                            } else {
                                j++;
                            }
                        }
                        i++;
                    } else {
                        Arrays.fill(elements, uniques2, n, (Object) null);
                        if (uniques2 == 1) {
                            return new SingletonImmutableSet(elements[0], hashCode);
                        }
                        if (tableSize != chooseTableSize(uniques2)) {
                            return construct(uniques2, elements);
                        }
                        if (uniques2 < elements.length) {
                            uniqueElements = ObjectArrays.arraysCopyOf(elements, uniques2);
                        } else {
                            uniqueElements = elements;
                        }
                        return new RegularImmutableSet(uniqueElements, hashCode, table, mask);
                    }
                }
                break;
        }
    }

    @VisibleForTesting
    static int chooseTableSize(int setSize) {
        if (setSize < 751619276) {
            int tableSize = Integer.highestOneBit(setSize - 1) << 1;
            while (((double) tableSize) * 0.7d < setSize) {
                tableSize <<= 1;
            }
            return tableSize;
        }
        Preconditions.checkArgument(setSize < 1073741824, "collection too large");
        return 1073741824;
    }

    public static <E> ImmutableSet<E> copyOf(E[] elements) {
        switch (elements.length) {
            case 0:
                return of();
            case 1:
                return of((Object) elements[0]);
            default:
                return construct(elements.length, (Object[]) elements.clone());
        }
    }

    ImmutableSet() {
    }

    boolean isHashCodeFast() {
        return false;
    }

    @Override
    public boolean equals(@Nullable Object object) {
        if (object == this) {
            return true;
        }
        if ((object instanceof ImmutableSet) && isHashCodeFast() && ((ImmutableSet) object).isHashCodeFast() && hashCode() != object.hashCode()) {
            return false;
        }
        return Sets.equalsImpl(this, object);
    }

    @Override
    public int hashCode() {
        return Sets.hashCodeImpl(this);
    }

    private static class SerializedForm implements Serializable {
        private static final long serialVersionUID = 0;
        final Object[] elements;

        SerializedForm(Object[] elements) {
            this.elements = elements;
        }

        Object readResolve() {
            return ImmutableSet.copyOf(this.elements);
        }
    }

    @Override
    Object writeReplace() {
        return new SerializedForm(toArray());
    }

    public static <E> Builder<E> builder() {
        return new Builder<>();
    }

    public static class Builder<E> extends ImmutableCollection.ArrayBasedBuilder<E> {
        @Override
        public ImmutableCollection.Builder add(Object element) {
            return super.add(element);
        }

        public Builder() {
            this(4);
        }

        Builder(int capacity) {
            super(capacity);
        }

        @Override
        public Builder<E> add(E element) {
            super.add((Object) element);
            return this;
        }

        @Override
        public Builder<E> add(E... elements) {
            super.add((Object[]) elements);
            return this;
        }

        @Override
        public Builder<E> addAll(Iterable<? extends E> elements) {
            super.addAll((Iterable) elements);
            return this;
        }

        public ImmutableSet<E> build() {
            ImmutableSet<E> result = ImmutableSet.construct(this.size, this.contents);
            this.size = result.size();
            return result;
        }
    }
}
