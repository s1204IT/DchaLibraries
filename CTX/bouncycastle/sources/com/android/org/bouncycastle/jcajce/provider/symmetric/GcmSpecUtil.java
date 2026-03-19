package com.android.org.bouncycastle.jcajce.provider.symmetric;

import com.android.org.bouncycastle.asn1.ASN1Primitive;
import com.android.org.bouncycastle.asn1.cms.GCMParameters;
import com.android.org.bouncycastle.util.Integers;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidParameterSpecException;

class GcmSpecUtil {
    static final Class gcmSpecClass = lookup("javax.crypto.spec.GCMParameterSpec");

    GcmSpecUtil() {
    }

    static boolean gcmSpecExists() {
        return gcmSpecClass != null;
    }

    static boolean isGcmSpec(AlgorithmParameterSpec algorithmParameterSpec) {
        return gcmSpecClass != null && gcmSpecClass.isInstance(algorithmParameterSpec);
    }

    static boolean isGcmSpec(Class cls) {
        return gcmSpecClass == cls;
    }

    static AlgorithmParameterSpec extractGcmSpec(ASN1Primitive aSN1Primitive) throws InvalidParameterSpecException {
        try {
            GCMParameters gCMParameters = GCMParameters.getInstance(aSN1Primitive);
            return (AlgorithmParameterSpec) gcmSpecClass.getConstructor(Integer.TYPE, byte[].class).newInstance(Integers.valueOf(gCMParameters.getIcvLen() * 8), gCMParameters.getNonce());
        } catch (NoSuchMethodException e) {
            throw new InvalidParameterSpecException("No constructor found!");
        } catch (Exception e2) {
            throw new InvalidParameterSpecException("Construction failed: " + e2.getMessage());
        }
    }

    static GCMParameters extractGcmParameters(AlgorithmParameterSpec algorithmParameterSpec) throws InvalidParameterSpecException {
        try {
            return new GCMParameters((byte[]) gcmSpecClass.getDeclaredMethod("getIV", new Class[0]).invoke(algorithmParameterSpec, new Object[0]), ((Integer) gcmSpecClass.getDeclaredMethod("getTLen", new Class[0]).invoke(algorithmParameterSpec, new Object[0])).intValue() / 8);
        } catch (Exception e) {
            throw new InvalidParameterSpecException("Cannot process GCMParameterSpec");
        }
    }

    private static Class lookup(String str) {
        try {
            return GcmSpecUtil.class.getClassLoader().loadClass(str);
        } catch (Exception e) {
            return null;
        }
    }
}
