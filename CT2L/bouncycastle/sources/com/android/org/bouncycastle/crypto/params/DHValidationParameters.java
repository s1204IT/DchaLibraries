package com.android.org.bouncycastle.crypto.params;

import com.android.org.bouncycastle.util.Arrays;

public class DHValidationParameters {
    private int counter;
    private byte[] seed;

    public DHValidationParameters(byte[] seed, int counter) {
        this.seed = seed;
        this.counter = counter;
    }

    public int getCounter() {
        return this.counter;
    }

    public byte[] getSeed() {
        return this.seed;
    }

    public boolean equals(Object o) {
        if (!(o instanceof DHValidationParameters)) {
            return false;
        }
        DHValidationParameters other = (DHValidationParameters) o;
        if (other.counter == this.counter) {
            return Arrays.areEqual(this.seed, other.seed);
        }
        return false;
    }

    public int hashCode() {
        return this.counter ^ Arrays.hashCode(this.seed);
    }
}
