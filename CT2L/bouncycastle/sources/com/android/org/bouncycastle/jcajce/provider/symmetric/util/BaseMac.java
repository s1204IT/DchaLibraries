package com.android.org.bouncycastle.jcajce.provider.symmetric.util;

import com.android.org.bouncycastle.crypto.CipherParameters;
import com.android.org.bouncycastle.crypto.Mac;
import com.android.org.bouncycastle.crypto.params.KeyParameter;
import com.android.org.bouncycastle.crypto.params.ParametersWithIV;
import com.android.org.bouncycastle.jcajce.provider.symmetric.util.PBE;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Hashtable;
import java.util.Map;
import javax.crypto.MacSpi;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEParameterSpec;

public class BaseMac extends MacSpi implements PBE {
    private int keySize;
    private Mac macEngine;
    private int pbeHash;
    private int pbeType;

    protected BaseMac(Mac macEngine) {
        this.pbeType = 2;
        this.pbeHash = 1;
        this.keySize = 160;
        this.macEngine = macEngine;
    }

    protected BaseMac(Mac macEngine, int pbeType, int pbeHash, int keySize) {
        this.pbeType = 2;
        this.pbeHash = 1;
        this.keySize = 160;
        this.macEngine = macEngine;
        this.pbeType = pbeType;
        this.pbeHash = pbeHash;
        this.keySize = keySize;
    }

    @Override
    protected void engineInit(Key key, AlgorithmParameterSpec params) throws InvalidKeyException, InvalidAlgorithmParameterException {
        CipherParameters param;
        if (key == null) {
            throw new InvalidKeyException("key is null");
        }
        if (key instanceof BCPBEKey) {
            BCPBEKey k = (BCPBEKey) key;
            if (k.getParam() != null) {
                param = k.getParam();
            } else if (params instanceof PBEParameterSpec) {
                param = PBE.Util.makePBEMacParameters(k, params);
            } else {
                throw new InvalidAlgorithmParameterException("PBE requires PBE parameters to be set.");
            }
        } else if (params instanceof IvParameterSpec) {
            param = new ParametersWithIV(new KeyParameter(key.getEncoded()), ((IvParameterSpec) params).getIV());
        } else if (params == null) {
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
