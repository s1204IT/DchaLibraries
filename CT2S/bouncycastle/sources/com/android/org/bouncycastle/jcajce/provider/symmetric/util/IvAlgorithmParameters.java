package com.android.org.bouncycastle.jcajce.provider.symmetric.util;

import com.android.org.bouncycastle.asn1.ASN1OctetString;
import com.android.org.bouncycastle.asn1.ASN1Primitive;
import com.android.org.bouncycastle.asn1.DEROctetString;
import com.android.org.bouncycastle.util.Arrays;
import java.io.IOException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidParameterSpecException;
import javax.crypto.spec.IvParameterSpec;

public class IvAlgorithmParameters extends BaseAlgorithmParameters {
    private byte[] iv;

    @Override
    protected byte[] engineGetEncoded() throws IOException {
        return engineGetEncoded("ASN.1");
    }

    @Override
    protected byte[] engineGetEncoded(String format) throws IOException {
        if (isASN1FormatString(format)) {
            return new DEROctetString(engineGetEncoded("RAW")).getEncoded();
        }
        if (format.equals("RAW")) {
            return Arrays.clone(this.iv);
        }
        return null;
    }

    @Override
    protected AlgorithmParameterSpec localEngineGetParameterSpec(Class paramSpec) throws InvalidParameterSpecException {
        if (paramSpec == IvParameterSpec.class) {
            return new IvParameterSpec(this.iv);
        }
        throw new InvalidParameterSpecException("unknown parameter spec passed to IV parameters object.");
    }

    @Override
    protected void engineInit(AlgorithmParameterSpec paramSpec) throws InvalidParameterSpecException {
        if (!(paramSpec instanceof IvParameterSpec)) {
            throw new InvalidParameterSpecException("IvParameterSpec required to initialise a IV parameters algorithm parameters object");
        }
        this.iv = ((IvParameterSpec) paramSpec).getIV();
    }

    @Override
    protected void engineInit(byte[] params) throws IOException {
        if (params.length % 8 != 0 && params[0] == 4 && params[1] == params.length - 2) {
            ASN1OctetString oct = (ASN1OctetString) ASN1Primitive.fromByteArray(params);
            params = oct.getOctets();
        }
        this.iv = Arrays.clone(params);
    }

    @Override
    protected void engineInit(byte[] params, String format) throws IOException {
        if (isASN1FormatString(format)) {
            try {
                ASN1OctetString oct = (ASN1OctetString) ASN1Primitive.fromByteArray(params);
                engineInit(oct.getOctets());
                return;
            } catch (Exception e) {
                throw new IOException("Exception decoding: " + e);
            }
        }
        if (format.equals("RAW")) {
            engineInit(params);
            return;
        }
        throw new IOException("Unknown parameters format in IV parameters object");
    }

    @Override
    protected String engineToString() {
        return "IV Parameters";
    }
}
