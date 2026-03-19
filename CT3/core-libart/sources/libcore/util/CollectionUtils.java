package libcore.util;

import java.lang.ref.Reference;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public final class CollectionUtils {
    private CollectionUtils() {
    }

    public static <T> Iterable<T> dereferenceIterable(final Iterable<? extends Reference<T>> iterable, final boolean trim) {
        return new Iterable<T>() {
            @Override
            public Iterator<T> iterator() {
                final Iterable iterable2 = iterable;
                final boolean z = trim;
                return new Iterator<T>() {
                    private final Iterator<? extends Reference<T>> delegate;
                    private T next;
                    private boolean removeIsOkay;

                    {
                        this.delegate = iterable2.iterator();
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
                        T result = this.next;
                        this.removeIsOkay = true;
                        this.next = null;
                        return result;
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
        int j = 1;
        for (int i = 1; i < list.size(); i++) {
            if (comparator.compare(list.get(j - 1), list.get(i)) != 0) {
                T object = list.get(i);
                list.set(j, object);
                j++;
            }
        }
        if (j >= list.size()) {
            return;
        }
        list.subList(j, list.size()).clear();
    }
}
