package com.android.org.bouncycastle.jcajce.provider.asymmetric.ec;

import com.android.org.bouncycastle.asn1.ASN1EncodableVector;
import com.android.org.bouncycastle.asn1.ASN1Encoding;
import com.android.org.bouncycastle.asn1.ASN1Integer;
import com.android.org.bouncycastle.asn1.ASN1Primitive;
import com.android.org.bouncycastle.asn1.ASN1Sequence;
import com.android.org.bouncycastle.asn1.DERSequence;
import com.android.org.bouncycastle.crypto.CipherParameters;
import com.android.org.bouncycastle.crypto.DSA;
import com.android.org.bouncycastle.crypto.Digest;
import com.android.org.bouncycastle.crypto.digests.AndroidDigestFactory;
import com.android.org.bouncycastle.crypto.digests.NullDigest;
import com.android.org.bouncycastle.crypto.params.ParametersWithRandom;
import com.android.org.bouncycastle.crypto.signers.ECDSASigner;
import com.android.org.bouncycastle.jcajce.provider.asymmetric.util.DSABase;
import com.android.org.bouncycastle.jcajce.provider.asymmetric.util.DSAEncoder;
import com.android.org.bouncycastle.jcajce.provider.asymmetric.util.ECUtil;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.PublicKey;

public class SignatureSpi extends DSABase {
    SignatureSpi(Digest digest, DSA signer, DSAEncoder encoder) {
        super(digest, signer, encoder);
    }

    @Override
    protected void engineInitVerify(PublicKey publicKey) throws InvalidKeyException {
        CipherParameters param = ECUtil.generatePublicKeyParameter(publicKey);
        this.digest.reset();
        this.signer.init(false, param);
    }

    @Override
    protected void engineInitSign(PrivateKey privateKey) throws InvalidKeyException {
        CipherParameters param = ECUtil.generatePrivateKeyParameter(privateKey);
        this.digest.reset();
        if (this.appRandom != null) {
            this.signer.init(true, new ParametersWithRandom(param, this.appRandom));
        } else {
            this.signer.init(true, param);
        }
    }

    public static class ecDSA extends SignatureSpi {
        public ecDSA() {
            super(AndroidDigestFactory.getSHA1(), new ECDSASigner(), new StdDSAEncoder());
        }
    }

    public static class ecDSAnone extends SignatureSpi {
        public ecDSAnone() {
            super(new NullDigest(), new ECDSASigner(), new StdDSAEncoder());
        }
    }

    public static class ecDSA224 extends SignatureSpi {
        public ecDSA224() {
            super(AndroidDigestFactory.getSHA224(), new ECDSASigner(), new StdDSAEncoder());
        }
    }

    public static class ecDSA256 extends SignatureSpi {
        public ecDSA256() {
            super(AndroidDigestFactory.getSHA256(), new ECDSASigner(), new StdDSAEncoder());
        }
    }

    public static class ecDSA384 extends SignatureSpi {
        public ecDSA384() {
            super(AndroidDigestFactory.getSHA384(), new ECDSASigner(), new StdDSAEncoder());
        }
    }

    public static class ecDSA512 extends SignatureSpi {
        public ecDSA512() {
            super(AndroidDigestFactory.getSHA512(), new ECDSASigner(), new StdDSAEncoder());
        }
    }

    private static class StdDSAEncoder implements DSAEncoder {
        private StdDSAEncoder() {
        }

        @Override
        public byte[] encode(BigInteger r, BigInteger s) throws IOException {
            ASN1EncodableVector v = new ASN1EncodableVector();
            v.add(new ASN1Integer(r));
            v.add(new ASN1Integer(s));
            return new DERSequence(v).getEncoded(ASN1Encoding.DER);
        }

        @Override
        public BigInteger[] decode(byte[] encoding) throws IOException {
            ASN1Sequence s = (ASN1Sequence) ASN1Primitive.fromByteArray(encoding);
            BigInteger[] sig = {ASN1Integer.getInstance(s.getObjectAt(0)).getValue(), ASN1Integer.getInstance(s.getObjectAt(1)).getValue()};
            return sig;
        }
    }

    private static class CVCDSAEncoder implements DSAEncoder {
        private CVCDSAEncoder() {
        }

        @Override
        public byte[] encode(BigInteger r, BigInteger s) throws IOException {
            byte[] res;
            byte[] first = makeUnsigned(r);
            byte[] second = makeUnsigned(s);
            if (first.length > second.length) {
                res = new byte[first.length * 2];
            } else {
                res = new byte[second.length * 2];
            }
            System.arraycopy(first, 0, res, (res.length / 2) - first.length, first.length);
            System.arraycopy(second, 0, res, res.length - second.length, second.length);
            return res;
        }

        private byte[] makeUnsigned(BigInteger val) {
            byte[] res = val.toByteArray();
            if (res[0] != 0) {
                return res;
            }
            byte[] tmp = new byte[res.length - 1];
            System.arraycopy(res, 1, tmp, 0, tmp.length);
            return tmp;
        }

        @Override
        public BigInteger[] decode(byte[] encoding) throws IOException {
            byte[] first = new byte[encoding.length / 2];
            byte[] second = new byte[encoding.length / 2];
            System.arraycopy(encoding, 0, first, 0, first.length);
            System.arraycopy(encoding, first.length, second, 0, second.length);
            BigInteger[] sig = {new BigInteger(1, first), new BigInteger(1, second)};
            return sig;
        }
    }
}
