package com.android.org.bouncycastle.asn1.misc;

import com.android.org.bouncycastle.asn1.DERIA5String;

public class NetscapeRevocationURL extends DERIA5String {
    public NetscapeRevocationURL(DERIA5String dERIA5String) {
        super(dERIA5String.getString());
    }

    @Override
    public String toString() {
        return "NetscapeRevocationURL: " + getString();
    }
}
