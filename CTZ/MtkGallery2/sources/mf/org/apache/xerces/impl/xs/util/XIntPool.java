package mf.org.apache.xerces.impl.xs.util;

public final class XIntPool {
    private static final short POOL_SIZE = 10;
    private static final XInt[] fXIntPool = new XInt[10];

    static {
        for (int i = 0; i < 10; i++) {
            fXIntPool[i] = new XInt(i);
        }
    }

    public final XInt getXInt(int value) {
        if (value >= 0 && value < fXIntPool.length) {
            return fXIntPool[value];
        }
        return new XInt(value);
    }
}
