package com.android.org.bouncycastle.math.field;

import java.math.BigInteger;

class PrimeField implements FiniteField {
    protected final BigInteger characteristic;

    PrimeField(BigInteger characteristic) {
        this.characteristic = characteristic;
    }

    @Override
    public BigInteger getCharacteristic() {
        return this.characteristic;
    }

    @Override
    public int getDimension() {
        return 1;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PrimeField)) {
            return false;
        }
        return this.characteristic.equals(obj.characteristic);
    }

    public int hashCode() {
        return this.characteristic.hashCode();
    }
}
