package libcore.util;

import java.lang.ref.Reference;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public final class CollectionUtils {
    private CollectionUtils() {
    }

    public static <T> Iterable<T> dereferenceIterable(final Iterable<? extends Reference<T>> iterable, final boolean z) {
        return new Iterable<T>() {
            @Override
            public Iterator<T> iterator() {
                return new Iterator<T>() {
                    private final Iterator<? extends Reference<T>> delegate;
                    private T next;
                    private boolean removeIsOkay;

                    {
                        this.delegate = iterable.iterator();
                    }

                    private void computeNext() {
                        this.removeIsOkay = false;
                        while (this.next == null && this.delegate.hasNext()) {
                            this.next = this.delegate.next().get();
                            if (z && this.next == null) {
                                this.delegate.remove();
                            }
                        }
                    }

                    @Override
                    public boolean hasNext() {
                        computeNext();
                        return this.next != null;
                    }

                    @Override
                    public T next() {
                        if (!hasNext()) {
                            throw new IllegalStateException();
                        }
                        T t = this.next;
                        this.removeIsOkay = true;
                        this.next = null;
                        return t;
                    }

                    @Override
                    public void remove() {
                        if (!this.removeIsOkay) {
                            throw new IllegalStateException();
                        }
                        this.delegate.remove();
                    }
                };
            }
        };
    }

    public static <T> void removeDuplicates(List<T> list, Comparator<? super T> comparator) {
        Collections.sort(list, comparator);
        int i = 1;
        for (int i2 = 1; i2 < list.size(); i2++) {
            if (comparator.compare(list.get(i - 1), list.get(i2)) != 0) {
                list.set(i, list.get(i2));
                i++;
            }
        }
        if (i < list.size()) {
            list.subList(i, list.size()).clear();
        }
    }
}
