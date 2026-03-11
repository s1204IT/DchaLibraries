package com.google.common.collect;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Preconditions;
import java.util.Comparator;
import java.util.List;
import java.util.RandomAccess;
import javax.annotation.Nullable;

@Beta
@GwtCompatible
final class SortedLists {
    private SortedLists() {
    }

    public enum KeyPresentBehavior {
        ANY_PRESENT {
            @Override
            <E> int resultIndex(Comparator<? super E> comparator, E key, List<? extends E> list, int foundIndex) {
                return foundIndex;
            }
        },
        LAST_PRESENT {
            @Override
            <E> int resultIndex(Comparator<? super E> comparator, E e, List<? extends E> list, int i) {
                int i2 = i;
                int size = list.size() - 1;
                while (i2 < size) {
                    int i3 = ((i2 + size) + 1) >>> 1;
                    if (comparator.compare(list.get(i3), e) > 0) {
                        size = i3 - 1;
                    } else {
                        i2 = i3;
                    }
                }
                return i2;
            }
        },
        FIRST_PRESENT {
            @Override
            <E> int resultIndex(Comparator<? super E> comparator, E e, List<? extends E> list, int i) {
                int i2 = 0;
                int i3 = i;
                while (i2 < i3) {
                    int i4 = (i2 + i3) >>> 1;
                    if (comparator.compare(list.get(i4), e) < 0) {
                        i2 = i4 + 1;
                    } else {
                        i3 = i4;
                    }
                }
                return i2;
            }
        },
        FIRST_AFTER {
            @Override
            public <E> int resultIndex(Comparator<? super E> comparator, E key, List<? extends E> list, int foundIndex) {
                return LAST_PRESENT.resultIndex(comparator, key, list, foundIndex) + 1;
            }
        },
        LAST_BEFORE {
            @Override
            public <E> int resultIndex(Comparator<? super E> comparator, E key, List<? extends E> list, int foundIndex) {
                return FIRST_PRESENT.resultIndex(comparator, key, list, foundIndex) - 1;
            }
        };

        KeyPresentBehavior(KeyPresentBehavior keyPresentBehavior) {
            this();
        }

        abstract <E> int resultIndex(Comparator<? super E> comparator, E e, List<? extends E> list, int i);

        public static KeyPresentBehavior[] valuesCustom() {
            return values();
        }
    }

    public enum KeyAbsentBehavior {
        NEXT_LOWER {
            @Override
            int resultIndex(int higherIndex) {
                return higherIndex - 1;
            }
        },
        NEXT_HIGHER {
            @Override
            public int resultIndex(int higherIndex) {
                return higherIndex;
            }
        },
        INVERTED_INSERTION_INDEX {
            @Override
            public int resultIndex(int higherIndex) {
                return ~higherIndex;
            }
        };

        KeyAbsentBehavior(KeyAbsentBehavior keyAbsentBehavior) {
            this();
        }

        abstract int resultIndex(int i);

        public static KeyAbsentBehavior[] valuesCustom() {
            return values();
        }
    }

    public static <E> int binarySearch(List<? extends E> list, @Nullable E e, Comparator<? super E> comparator, KeyPresentBehavior keyPresentBehavior, KeyAbsentBehavior keyAbsentBehavior) {
        Preconditions.checkNotNull(comparator);
        Preconditions.checkNotNull(list);
        Preconditions.checkNotNull(keyPresentBehavior);
        Preconditions.checkNotNull(keyAbsentBehavior);
        if (!(list instanceof RandomAccess)) {
            list = Lists.newArrayList(list);
        }
        int i = 0;
        int size = list.size() - 1;
        while (i <= size) {
            int i2 = (i + size) >>> 1;
            int iCompare = comparator.compare(e, list.get(i2));
            if (iCompare < 0) {
                size = i2 - 1;
            } else if (iCompare > 0) {
                i = i2 + 1;
            } else {
                return keyPresentBehavior.resultIndex(comparator, e, list.subList(i, size + 1), i2 - i) + i;
            }
        }
        return keyAbsentBehavior.resultIndex(i);
    }
}
