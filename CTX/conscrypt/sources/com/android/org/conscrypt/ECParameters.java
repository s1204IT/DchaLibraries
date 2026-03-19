package com.android.org.conscrypt;

import com.android.org.conscrypt.NativeRef;
import java.io.IOException;
import java.security.AlgorithmParametersSpi;
import java.security.InvalidAlgorithmParameterException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.InvalidParameterSpecException;

public class ECParameters extends AlgorithmParametersSpi {
    private OpenSSLECGroupContext curve;

    @Override
    protected void engineInit(AlgorithmParameterSpec algorithmParameterSpec) throws InvalidParameterSpecException {
        if (algorithmParameterSpec instanceof ECGenParameterSpec) {
            String name = ((ECGenParameterSpec) algorithmParameterSpec).getName();
            OpenSSLECGroupContext curveByName = OpenSSLECGroupContext.getCurveByName(name);
            if (curveByName == null) {
                throw new InvalidParameterSpecException("Unknown EC curve name: " + name);
            }
            this.curve = curveByName;
            return;
        }
        if (algorithmParameterSpec instanceof ECParameterSpec) {
            ECParameterSpec eCParameterSpec = (ECParameterSpec) algorithmParameterSpec;
            try {
                OpenSSLECGroupContext openSSLECGroupContext = OpenSSLECGroupContext.getInstance(eCParameterSpec);
                if (openSSLECGroupContext == null) {
                    throw new InvalidParameterSpecException("Unknown EC curve: " + eCParameterSpec);
                }
                this.curve = openSSLECGroupContext;
                return;
            } catch (InvalidAlgorithmParameterException e) {
                throw new InvalidParameterSpecException(e.getMessage());
            }
        }
        throw new InvalidParameterSpecException("Only ECParameterSpec and ECGenParameterSpec are supported");
    }

    @Override
    protected void engineInit(byte[] bArr) throws IOException {
        long jEC_KEY_parse_curve_name = NativeCrypto.EC_KEY_parse_curve_name(bArr);
        if (jEC_KEY_parse_curve_name == 0) {
            throw new IOException("Error reading ASN.1 encoding");
        }
        this.curve = new OpenSSLECGroupContext(new NativeRef.EC_GROUP(jEC_KEY_parse_curve_name));
    }

    @Override
    protected void engineInit(byte[] bArr, String str) throws IOException {
        if (str == null || str.equals("ASN.1")) {
            engineInit(bArr);
            return;
        }
        throw new IOException("Unsupported format: " + str);
    }

    @Override
    protected <T extends AlgorithmParameterSpec> T engineGetParameterSpec(Class<T> cls) throws InvalidParameterSpecException {
        if (cls == ECParameterSpec.class) {
            return this.curve.getECParameterSpec();
        }
        if (cls == ECGenParameterSpec.class) {
            return new ECGenParameterSpec(Platform.getCurveName(this.curve.getECParameterSpec()));
        }
        throw new InvalidParameterSpecException("Unsupported class: " + cls);
    }

    @Override
    protected byte[] engineGetEncoded() throws IOException {
        return NativeCrypto.EC_KEY_marshal_curve_name(this.curve.getNativeRef());
    }

    @Override
    protected byte[] engineGetEncoded(String str) throws IOException {
        if (str == null || str.equals("ASN.1")) {
            return engineGetEncoded();
        }
        throw new IOException("Unsupported format: " + str);
    }

    @Override
    protected String engineToString() {
        return "Conscrypt EC AlgorithmParameters";
    }
}
