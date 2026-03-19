package java.util;

import java.io.Serializable;
import java.util.function.ToLongFunction;

public final class $$Lambda$Comparator$4V5k8aLimtS0VsEILEAqQ9UGZYo implements Comparator, Serializable {
    private final ToLongFunction f$0;

    public $$Lambda$Comparator$4V5k8aLimtS0VsEILEAqQ9UGZYo(ToLongFunction toLongFunction) {
        this.f$0 = toLongFunction;
    }

    @Override
    public final int compare(Object obj, Object obj2) {
        ToLongFunction toLongFunction = this.f$0;
        return Long.compare(toLongFunction.applyAsLong(obj), toLongFunction.applyAsLong(obj2));
    }
}
