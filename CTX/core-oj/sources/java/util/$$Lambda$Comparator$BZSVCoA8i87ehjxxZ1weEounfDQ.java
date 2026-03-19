package java.util;

import java.io.Serializable;

public final class $$Lambda$Comparator$BZSVCoA8i87ehjxxZ1weEounfDQ implements Comparator, Serializable {
    private final Comparator f$0;
    private final Comparator f$1;

    public $$Lambda$Comparator$BZSVCoA8i87ehjxxZ1weEounfDQ(Comparator comparator, Comparator comparator2) {
        this.f$0 = comparator;
        this.f$1 = comparator2;
    }

    @Override
    public final int compare(Object obj, Object obj2) {
        return Comparator.lambda$thenComparing$36697e65$1(this.f$0, this.f$1, obj, obj2);
    }
}
