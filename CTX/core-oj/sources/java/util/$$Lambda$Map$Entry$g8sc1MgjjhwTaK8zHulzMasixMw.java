package java.util;

import java.io.Serializable;
import java.util.Map;

public final class $$Lambda$Map$Entry$g8sc1MgjjhwTaK8zHulzMasixMw implements Comparator, Serializable {
    private final Comparator f$0;

    public $$Lambda$Map$Entry$g8sc1MgjjhwTaK8zHulzMasixMw(Comparator comparator) {
        this.f$0 = comparator;
    }

    @Override
    public final int compare(Object obj, Object obj2) {
        return this.f$0.compare(((Map.Entry) obj).getKey(), ((Map.Entry) obj2).getKey());
    }
}
