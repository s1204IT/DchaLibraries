package com.android.org.bouncycastle.jcajce.provider.symmetric.util;

import com.android.org.bouncycastle.crypto.CipherParameters;
import com.android.org.bouncycastle.crypto.Mac;
import com.android.org.bouncycastle.crypto.params.KeyParameter;
import com.android.org.bouncycastle.crypto.params.ParametersWithIV;
import com.android.org.bouncycastle.jcajce.PKCS12Key;
import com.android.org.bouncycastle.jcajce.provider.symmetric.util.PBE;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Hashtable;
import java.util.Map;
import javax.crypto.MacSpi;
import javax.crypto.interfaces.PBEKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEParameterSpec;

public class BaseMac extends MacSpi implements PBE {
    private int keySize;
    private Mac macEngine;
    private int pbeHash;
    private int scheme;

    protected BaseMac(Mac macEngine) {
        this.scheme = 2;
        this.pbeHash = 1;
        this.keySize = 160;
        this.macEngine = macEngine;
    }

    protected BaseMac(Mac macEngine, int scheme, int pbeHash, int keySize) {
        this.scheme = 2;
        this.pbeHash = 1;
        this.keySize = 160;
        this.macEngine = macEngine;
        this.scheme = scheme;
        this.pbeHash = pbeHash;
        this.keySize = keySize;
    }

    @Override
    protected void engineInit(Key key, AlgorithmParameterSpec algorithmParameterSpec) throws InvalidKeyException, InvalidAlgorithmParameterException {
        CipherParameters param;
        if (key == 0) {
            throw new InvalidKeyException("key is null");
        }
        if (key instanceof PKCS12Key) {
            try {
                PBEParameterSpec pbeSpec = (PBEParameterSpec) algorithmParameterSpec;
                if ((key instanceof PBEKey) && pbeSpec == null) {
                    pbeSpec = new PBEParameterSpec(((PBEKey) key).getSalt(), ((PBEKey) key).getIterationCount());
                }
                int digest = 1;
                int keySize = 160;
                if (this.macEngine.getAlgorithmName().startsWith("SHA256")) {
                    digest = 4;
                    keySize = 256;
                }
                param = PBE.Util.makePBEMacParameters(key, 2, digest, keySize, pbeSpec);
            } catch (Exception e) {
                throw new InvalidAlgorithmParameterException("PKCS12 requires a PBEParameterSpec");
            }
        } else if (key instanceof BCPBEKey) {
            if (key.getParam() != null) {
                param = key.getParam();
            } else if (algorithmParameterSpec instanceof PBEParameterSpec) {
                param = PBE.Util.makePBEMacParameters(key, algorithmParameterSpec);
            } else {
                throw new InvalidAlgorithmParameterException("PBE requires PBE parameters to be set.");
            }
        } else if (algorithmParameterSpec instanceof IvParameterSpec) {
            param = new ParametersWithIV(new KeyParameter(key.getEncoded()), algorithmParameterSpec.getIV());
        } else if (algorithmParameterSpec == 0) {
            param = new KeyParameter(key.getEncoded());
        } else {
            throw new InvalidAlgorithmParameterException("unknown parameter type.");
        }
        this.macEngine.init(param);
    }

    @Override
    protected int engineGetMacLength() {
        return this.macEngine.getMacSize();
    }

    @Override
    protected void engineReset() {
        this.macEngine.reset();
    }

    @Override
    protected void engineUpdate(byte input) {
        this.macEngine.update(input);
    }

    @Override
    protected void engineUpdate(byte[] input, int offset, int len) {
        this.macEngine.update(input, offset, len);
    }

    @Override
    protected byte[] engineDoFinal() {
        byte[] out = new byte[engineGetMacLength()];
        this.macEngine.doFinal(out, 0);
        return out;
    }

    private static Hashtable copyMap(Map paramsMap) {
        Hashtable newTable = new Hashtable();
        for (Object key : paramsMap.keySet()) {
            newTable.put(key, paramsMap.get(key));
        }
        return newTable;
    }
}
