package java.util;

import java.io.Serializable;
import java.util.function.Function;

public final class $$Lambda$Comparator$SPB8K9Yj7Pw1mljm7LpasV7zxWw implements Comparator, Serializable {
    private final Function f$0;

    public $$Lambda$Comparator$SPB8K9Yj7Pw1mljm7LpasV7zxWw(Function function) {
        this.f$0 = function;
    }

    @Override
    public final int compare(Object obj, Object obj2) {
        Function function = this.f$0;
        return ((Comparable) function.apply(obj)).compareTo(function.apply(obj2));
    }
}
