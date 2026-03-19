package java.lang;

import java.util.Iterator;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;

public interface Iterable<T> {
    Iterator<T> iterator();

    default void forEach(Consumer<? super T> consumer) {
        Objects.requireNonNull(consumer);
        Iterator<T> it = iterator();
        while (it.hasNext()) {
            consumer.accept(it.next());
        }
    }

    default Spliterator<T> spliterator() {
        return Spliterators.spliteratorUnknownSize(iterator(), 0);
    }
}
