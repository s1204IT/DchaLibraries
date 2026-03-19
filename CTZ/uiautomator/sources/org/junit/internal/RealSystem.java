package org.junit.internal;

import java.io.PrintStream;

public class RealSystem implements JUnitSystem {
    @Override
    @Deprecated
    public void exit(int i) {
        System.exit(i);
    }

    @Override
    public PrintStream out() {
        return System.out;
    }
}
