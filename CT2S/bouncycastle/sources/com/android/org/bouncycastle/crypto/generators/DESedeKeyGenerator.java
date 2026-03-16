package com.android.org.bouncycastle.crypto.generators;

import com.android.org.bouncycastle.crypto.KeyGenerationParameters;
import com.android.org.bouncycastle.crypto.params.DESedeParameters;

public class DESedeKeyGenerator extends DESKeyGenerator {
    @Override
    public void init(KeyGenerationParameters param) {
        this.random = param.getRandom();
        this.strength = (param.getStrength() + 7) / 8;
        if (this.strength == 0 || this.strength == 21) {
            this.strength = 24;
        } else if (this.strength == 14) {
            this.strength = 16;
        } else if (this.strength != 24 && this.strength != 16) {
            throw new IllegalArgumentException("DESede key must be 192 or 128 bits long.");
        }
    }

    @Override
    public byte[] generateKey() {
        byte[] newKey = new byte[this.strength];
        do {
            this.random.nextBytes(newKey);
            DESedeParameters.setOddParity(newKey);
        } while (DESedeParameters.isWeakKey(newKey, 0, newKey.length));
        return newKey;
    }
}
