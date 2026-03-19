package java.util;

import java.io.Serializable;
import java.util.Map;

public final class $$Lambda$Map$Entry$acJOHw6hO1wh4v9r2vtUuCFe5vI implements Comparator, Serializable {
    public static final $$Lambda$Map$Entry$acJOHw6hO1wh4v9r2vtUuCFe5vI INSTANCE = new $$Lambda$Map$Entry$acJOHw6hO1wh4v9r2vtUuCFe5vI();

    private $$Lambda$Map$Entry$acJOHw6hO1wh4v9r2vtUuCFe5vI() {
    }

    @Override
    public final int compare(Object obj, Object obj2) {
        return ((Comparable) ((Map.Entry) obj).getValue()).compareTo(((Map.Entry) obj2).getValue());
    }
}
