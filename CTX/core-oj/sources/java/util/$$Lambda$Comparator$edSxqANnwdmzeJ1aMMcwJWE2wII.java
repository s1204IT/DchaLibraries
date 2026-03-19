package java.util;

import java.io.Serializable;
import java.util.function.ToDoubleFunction;

public final class $$Lambda$Comparator$edSxqANnwdmzeJ1aMMcwJWE2wII implements Comparator, Serializable {
    private final ToDoubleFunction f$0;

    public $$Lambda$Comparator$edSxqANnwdmzeJ1aMMcwJWE2wII(ToDoubleFunction toDoubleFunction) {
        this.f$0 = toDoubleFunction;
    }

    @Override
    public final int compare(Object obj, Object obj2) {
        ToDoubleFunction toDoubleFunction = this.f$0;
        return Double.compare(toDoubleFunction.applyAsDouble(obj), toDoubleFunction.applyAsDouble(obj2));
    }
}
