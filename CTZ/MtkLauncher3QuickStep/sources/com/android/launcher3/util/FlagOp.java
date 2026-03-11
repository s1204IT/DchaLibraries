package com.android.launcher3.util;

public abstract class FlagOp {
    public static FlagOp NO_OP = new FlagOp() {
    };

    private FlagOp() {
    }

    public int apply(int i) {
        return i;
    }

    public static FlagOp addFlag(final int i) {
        return new FlagOp() {
            {
                super();
            }

            @Override
            public int apply(int i2) {
                return i2 | i;
            }
        };
    }

    public static FlagOp removeFlag(final int i) {
        return new FlagOp() {
            {
                super();
            }

            @Override
            public int apply(int i2) {
                return i2 & (~i);
            }
        };
    }
}
