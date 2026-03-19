package java.util;

import java.io.Serializable;
import java.util.function.ToIntFunction;

public final class $$Lambda$Comparator$DNgpxUFZqmT4lOBzlVyPjWwvEvw implements Comparator, Serializable {
    private final ToIntFunction f$0;

    public $$Lambda$Comparator$DNgpxUFZqmT4lOBzlVyPjWwvEvw(ToIntFunction toIntFunction) {
        this.f$0 = toIntFunction;
    }

    @Override
    public final int compare(Object obj, Object obj2) {
        ToIntFunction toIntFunction = this.f$0;
        return Integer.compare(toIntFunction.applyAsInt(obj), toIntFunction.applyAsInt(obj2));
    }
}
