package com.android.org.bouncycastle.crypto.engines;

import com.android.org.bouncycastle.crypto.CipherParameters;
import com.android.org.bouncycastle.crypto.DataLengthException;
import com.android.org.bouncycastle.crypto.OutputLengthException;
import com.android.org.bouncycastle.crypto.StreamCipher;
import com.android.org.bouncycastle.crypto.params.KeyParameter;

public class RC4Engine implements StreamCipher {
    private static final int STATE_LENGTH = 256;
    private byte[] engineState = null;
    private int x = 0;
    private int y = 0;
    private byte[] workingKey = null;

    @Override
    public void init(boolean z, CipherParameters cipherParameters) {
        if (cipherParameters instanceof KeyParameter) {
            this.workingKey = ((KeyParameter) cipherParameters).getKey();
            setKey(this.workingKey);
        } else {
            throw new IllegalArgumentException("invalid parameter passed to RC4 init - " + cipherParameters.getClass().getName());
        }
    }

    @Override
    public String getAlgorithmName() {
        return "RC4";
    }

    @Override
    public byte returnByte(byte b) {
        this.x = (this.x + 1) & 255;
        this.y = (this.engineState[this.x] + this.y) & 255;
        byte b2 = this.engineState[this.x];
        this.engineState[this.x] = this.engineState[this.y];
        this.engineState[this.y] = b2;
        return (byte) (b ^ this.engineState[(this.engineState[this.x] + this.engineState[this.y]) & 255]);
    }

    @Override
    public int processBytes(byte[] bArr, int i, int i2, byte[] bArr2, int i3) {
        if (i + i2 > bArr.length) {
            throw new DataLengthException("input buffer too short");
        }
        if (i3 + i2 > bArr2.length) {
            throw new OutputLengthException("output buffer too short");
        }
        for (int i4 = 0; i4 < i2; i4++) {
            this.x = (this.x + 1) & 255;
            this.y = (this.engineState[this.x] + this.y) & 255;
            byte b = this.engineState[this.x];
            this.engineState[this.x] = this.engineState[this.y];
            this.engineState[this.y] = b;
            bArr2[i4 + i3] = (byte) (bArr[i4 + i] ^ this.engineState[(this.engineState[this.x] + this.engineState[this.y]) & 255]);
        }
        return i2;
    }

    @Override
    public void reset() {
        setKey(this.workingKey);
    }

    private void setKey(byte[] bArr) {
        this.workingKey = bArr;
        this.x = 0;
        this.y = 0;
        if (this.engineState == null) {
            this.engineState = new byte[STATE_LENGTH];
        }
        for (int i = 0; i < STATE_LENGTH; i++) {
            this.engineState[i] = (byte) i;
        }
        int length = 0;
        int i2 = 0;
        for (int i3 = 0; i3 < STATE_LENGTH; i3++) {
            i2 = ((bArr[length] & 255) + this.engineState[i3] + i2) & 255;
            byte b = this.engineState[i3];
            this.engineState[i3] = this.engineState[i2];
            this.engineState[i2] = b;
            length = (length + 1) % bArr.length;
        }
    }
}
