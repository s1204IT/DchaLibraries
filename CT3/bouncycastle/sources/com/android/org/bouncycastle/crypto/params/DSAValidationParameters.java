package com.android.org.bouncycastle.crypto.params;

import com.android.org.bouncycastle.util.Arrays;

public class DSAValidationParameters {
    private int counter;
    private byte[] seed;
    private int usageIndex;

    public DSAValidationParameters(byte[] seed, int counter) {
        this(seed, counter, -1);
    }

    public DSAValidationParameters(byte[] seed, int counter, int usageIndex) {
        this.seed = seed;
        this.counter = counter;
        this.usageIndex = usageIndex;
    }

    public int getCounter() {
        return this.counter;
    }

    public byte[] getSeed() {
        return this.seed;
    }

    public int getUsageIndex() {
        return this.usageIndex;
    }

    public int hashCode() {
        return this.counter ^ Arrays.hashCode(this.seed);
    }

    public boolean equals(Object obj) {
        if ((obj instanceof DSAValidationParameters) && obj.counter == this.counter) {
            return Arrays.areEqual(this.seed, obj.seed);
        }
        return false;
    }
}
