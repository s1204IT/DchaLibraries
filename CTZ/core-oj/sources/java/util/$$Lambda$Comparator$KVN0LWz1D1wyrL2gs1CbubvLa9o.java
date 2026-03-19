package java.util;

import java.io.Serializable;
import java.util.function.Function;

public final class $$Lambda$Comparator$KVN0LWz1D1wyrL2gs1CbubvLa9o implements Comparator, Serializable {
    private final Comparator f$0;
    private final Function f$1;

    public $$Lambda$Comparator$KVN0LWz1D1wyrL2gs1CbubvLa9o(Comparator comparator, Function function) {
        this.f$0 = comparator;
        this.f$1 = function;
    }

    @Override
    public final int compare(Object obj, Object obj2) {
        Comparator comparator = this.f$0;
        Function function = this.f$1;
        return comparator.compare(function.apply(obj), function.apply(obj2));
    }
}
