package java.util.concurrent;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Map;

public final class $$Lambda$ConcurrentSkipListMap$EntrySpliterator$y0KdhWWpZC4eKUM6bCtPBgl2u2o implements Comparator, Serializable {
    public static final $$Lambda$ConcurrentSkipListMap$EntrySpliterator$y0KdhWWpZC4eKUM6bCtPBgl2u2o INSTANCE = new $$Lambda$ConcurrentSkipListMap$EntrySpliterator$y0KdhWWpZC4eKUM6bCtPBgl2u2o();

    private $$Lambda$ConcurrentSkipListMap$EntrySpliterator$y0KdhWWpZC4eKUM6bCtPBgl2u2o() {
    }

    @Override
    public final int compare(Object obj, Object obj2) {
        return ((Comparable) ((Map.Entry) obj).getKey()).compareTo(((Map.Entry) obj2).getKey());
    }
}
