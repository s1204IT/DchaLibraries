package com.android.org.bouncycastle.jcajce;

import com.android.org.bouncycastle.crypto.PBEParametersGenerator;

public class PKCS12Key implements PBKDFKey {
    private final char[] password;
    private final boolean useWrongZeroLengthConversion;

    public PKCS12Key(char[] cArr) {
        this(cArr, false);
    }

    public PKCS12Key(char[] cArr, boolean z) {
        cArr = cArr == null ? new char[0] : cArr;
        this.password = new char[cArr.length];
        this.useWrongZeroLengthConversion = z;
        System.arraycopy(cArr, 0, this.password, 0, cArr.length);
    }

    public char[] getPassword() {
        return this.password;
    }

    @Override
    public String getAlgorithm() {
        return "PKCS12";
    }

    @Override
    public String getFormat() {
        return "PKCS12";
    }

    @Override
    public byte[] getEncoded() {
        if (this.useWrongZeroLengthConversion && this.password.length == 0) {
            return new byte[2];
        }
        return PBEParametersGenerator.PKCS12PasswordToBytes(this.password);
    }
}
