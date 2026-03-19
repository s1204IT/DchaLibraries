package com.google.common.collect;

import com.google.common.base.Preconditions;
import java.util.Comparator;
import java.util.List;
import java.util.RandomAccess;

final class SortedLists {

    public enum KeyAbsentBehavior {
        NEXT_LOWER {
            @Override
            int resultIndex(int i) {
                return i - 1;
            }
        },
        NEXT_HIGHER {
            @Override
            public int resultIndex(int i) {
                return i;
            }
        },
        INVERTED_INSERTION_INDEX {
            @Override
            public int resultIndex(int i) {
                return ~i;
            }
        };

        abstract int resultIndex(int i);
    }

    public enum KeyPresentBehavior {
        ANY_PRESENT {
            @Override
            <E> int resultIndex(Comparator<? super E> comparator, E e, List<? extends E> list, int i) {
                return i;
            }
        },
        LAST_PRESENT {
            @Override
            <E> int resultIndex(Comparator<? super E> comparator, E e, List<? extends E> list, int i) {
                int size = list.size() - 1;
                while (i < size) {
                    int i2 = ((i + size) + 1) >>> 1;
                    if (comparator.compare(list.get(i2), e) > 0) {
                        size = i2 - 1;
                    } else {
                        i = i2;
                    }
                }
                return i;
            }
        },
        FIRST_PRESENT {
            @Override
            <E> int resultIndex(Comparator<? super E> comparator, E e, List<? extends E> list, int i) {
                int i2 = 0;
                while (i2 < i) {
                    int i3 = (i2 + i) >>> 1;
                    if (comparator.compare(list.get(i3), e) < 0) {
                        i2 = i3 + 1;
                    } else {
                        i = i3;
                    }
                }
                return i2;
            }
        },
        FIRST_AFTER {
            @Override
            public <E> int resultIndex(Comparator<? super E> comparator, E e, List<? extends E> list, int i) {
                return LAST_PRESENT.resultIndex(comparator, e, list, i) + 1;
            }
        },
        LAST_BEFORE {
            @Override
            public <E> int resultIndex(Comparator<? super E> comparator, E e, List<? extends E> list, int i) {
                return FIRST_PRESENT.resultIndex(comparator, e, list, i) - 1;
            }
        };

        abstract <E> int resultIndex(Comparator<? super E> comparator, E e, List<? extends E> list, int i);
    }

    public static <E> int binarySearch(List<? extends E> list, E e, Comparator<? super E> comparator, KeyPresentBehavior keyPresentBehavior, KeyAbsentBehavior keyAbsentBehavior) {
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
                return i + keyPresentBehavior.resultIndex(comparator, e, list.subList(i, size + 1), i2 - i);
            }
        }
        return keyAbsentBehavior.resultIndex(i);
    }
}
