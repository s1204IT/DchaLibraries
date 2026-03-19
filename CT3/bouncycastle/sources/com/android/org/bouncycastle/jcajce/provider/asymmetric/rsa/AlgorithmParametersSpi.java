package com.android.org.bouncycastle.jcajce.provider.asymmetric.rsa;

import com.android.org.bouncycastle.asn1.ASN1Encoding;
import com.android.org.bouncycastle.asn1.ASN1Integer;
import com.android.org.bouncycastle.asn1.ASN1OctetString;
import com.android.org.bouncycastle.asn1.DERNull;
import com.android.org.bouncycastle.asn1.DEROctetString;
import com.android.org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import com.android.org.bouncycastle.asn1.pkcs.RSAESOAEPparams;
import com.android.org.bouncycastle.asn1.pkcs.RSASSAPSSparams;
import com.android.org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import com.android.org.bouncycastle.jcajce.provider.util.DigestFactory;
import java.io.IOException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PSSParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;

public abstract class AlgorithmParametersSpi extends java.security.AlgorithmParametersSpi {
    protected abstract AlgorithmParameterSpec localEngineGetParameterSpec(Class cls) throws InvalidParameterSpecException;

    protected boolean isASN1FormatString(String format) {
        if (format != null) {
            return format.equals("ASN.1");
        }
        return true;
    }

    @Override
    protected AlgorithmParameterSpec engineGetParameterSpec(Class paramSpec) throws InvalidParameterSpecException {
        if (paramSpec == null) {
            throw new NullPointerException("argument to getParameterSpec must not be null");
        }
        return localEngineGetParameterSpec(paramSpec);
    }

    public static class OAEP extends AlgorithmParametersSpi {
        OAEPParameterSpec currentSpec;

        @Override
        protected byte[] engineGetEncoded() {
            AlgorithmIdentifier hashAlgorithm = new AlgorithmIdentifier(DigestFactory.getOID(this.currentSpec.getDigestAlgorithm()), DERNull.INSTANCE);
            MGF1ParameterSpec mgfSpec = (MGF1ParameterSpec) this.currentSpec.getMGFParameters();
            AlgorithmIdentifier maskGenAlgorithm = new AlgorithmIdentifier(PKCSObjectIdentifiers.id_mgf1, new AlgorithmIdentifier(DigestFactory.getOID(mgfSpec.getDigestAlgorithm()), DERNull.INSTANCE));
            PSource.PSpecified pSource = (PSource.PSpecified) this.currentSpec.getPSource();
            AlgorithmIdentifier pSourceAlgorithm = new AlgorithmIdentifier(PKCSObjectIdentifiers.id_pSpecified, new DEROctetString(pSource.getValue()));
            RSAESOAEPparams oaepP = new RSAESOAEPparams(hashAlgorithm, maskGenAlgorithm, pSourceAlgorithm);
            try {
                return oaepP.getEncoded(ASN1Encoding.DER);
            } catch (IOException e) {
                throw new RuntimeException("Error encoding OAEPParameters");
            }
        }

        @Override
        protected byte[] engineGetEncoded(String format) {
            if (isASN1FormatString(format) || format.equalsIgnoreCase("X.509")) {
                return engineGetEncoded();
            }
            return null;
        }

        @Override
        protected AlgorithmParameterSpec localEngineGetParameterSpec(Class paramSpec) throws InvalidParameterSpecException {
            if (paramSpec == OAEPParameterSpec.class || paramSpec == AlgorithmParameterSpec.class) {
                return this.currentSpec;
            }
            throw new InvalidParameterSpecException("unknown parameter spec passed to OAEP parameters object.");
        }

        @Override
        protected void engineInit(AlgorithmParameterSpec algorithmParameterSpec) throws InvalidParameterSpecException {
            if (algorithmParameterSpec instanceof OAEPParameterSpec) {
                this.currentSpec = algorithmParameterSpec;
                return;
            }
            throw new InvalidParameterSpecException("OAEPParameterSpec required to initialise an OAEP algorithm parameters object");
        }

        @Override
        protected void engineInit(byte[] params) throws IOException {
            try {
                RSAESOAEPparams oaepP = RSAESOAEPparams.getInstance(params);
                this.currentSpec = new OAEPParameterSpec(oaepP.getHashAlgorithm().getAlgorithm().getId(), oaepP.getMaskGenAlgorithm().getAlgorithm().getId(), new MGF1ParameterSpec(AlgorithmIdentifier.getInstance(oaepP.getMaskGenAlgorithm().getParameters()).getAlgorithm().getId()), new PSource.PSpecified(ASN1OctetString.getInstance(oaepP.getPSourceAlgorithm().getParameters()).getOctets()));
            } catch (ArrayIndexOutOfBoundsException e) {
                throw new IOException("Not a valid OAEP Parameter encoding.");
            } catch (ClassCastException e2) {
                throw new IOException("Not a valid OAEP Parameter encoding.");
            }
        }

        @Override
        protected void engineInit(byte[] params, String format) throws IOException {
            if (format.equalsIgnoreCase("X.509") || format.equalsIgnoreCase("ASN.1")) {
                engineInit(params);
                return;
            }
            throw new IOException("Unknown parameter format " + format);
        }

        @Override
        protected String engineToString() {
            return "OAEP Parameters";
        }
    }

    public static class PSS extends AlgorithmParametersSpi {
        PSSParameterSpec currentSpec;

        @Override
        protected byte[] engineGetEncoded() throws IOException {
            PSSParameterSpec pssSpec = this.currentSpec;
            AlgorithmIdentifier hashAlgorithm = new AlgorithmIdentifier(DigestFactory.getOID(pssSpec.getDigestAlgorithm()), DERNull.INSTANCE);
            MGF1ParameterSpec mgfSpec = (MGF1ParameterSpec) pssSpec.getMGFParameters();
            AlgorithmIdentifier maskGenAlgorithm = new AlgorithmIdentifier(PKCSObjectIdentifiers.id_mgf1, new AlgorithmIdentifier(DigestFactory.getOID(mgfSpec.getDigestAlgorithm()), DERNull.INSTANCE));
            RSASSAPSSparams pssP = new RSASSAPSSparams(hashAlgorithm, maskGenAlgorithm, new ASN1Integer(pssSpec.getSaltLength()), new ASN1Integer(pssSpec.getTrailerField()));
            return pssP.getEncoded(ASN1Encoding.DER);
        }

        @Override
        protected byte[] engineGetEncoded(String format) throws IOException {
            if (format.equalsIgnoreCase("X.509") || format.equalsIgnoreCase("ASN.1")) {
                return engineGetEncoded();
            }
            return null;
        }

        @Override
        protected AlgorithmParameterSpec localEngineGetParameterSpec(Class paramSpec) throws InvalidParameterSpecException {
            if (paramSpec == PSSParameterSpec.class && this.currentSpec != null) {
                return this.currentSpec;
            }
            throw new InvalidParameterSpecException("unknown parameter spec passed to PSS parameters object.");
        }

        @Override
        protected void engineInit(AlgorithmParameterSpec algorithmParameterSpec) throws InvalidParameterSpecException {
            if (algorithmParameterSpec instanceof PSSParameterSpec) {
                this.currentSpec = algorithmParameterSpec;
                return;
            }
            throw new InvalidParameterSpecException("PSSParameterSpec required to initialise an PSS algorithm parameters object");
        }

        @Override
        protected void engineInit(byte[] params) throws IOException {
            try {
                RSASSAPSSparams pssP = RSASSAPSSparams.getInstance(params);
                this.currentSpec = new PSSParameterSpec(pssP.getHashAlgorithm().getAlgorithm().getId(), pssP.getMaskGenAlgorithm().getAlgorithm().getId(), new MGF1ParameterSpec(AlgorithmIdentifier.getInstance(pssP.getMaskGenAlgorithm().getParameters()).getAlgorithm().getId()), pssP.getSaltLength().intValue(), pssP.getTrailerField().intValue());
            } catch (ArrayIndexOutOfBoundsException e) {
                throw new IOException("Not a valid PSS Parameter encoding.");
            } catch (ClassCastException e2) {
                throw new IOException("Not a valid PSS Parameter encoding.");
            }
        }

        @Override
        protected void engineInit(byte[] params, String format) throws IOException {
            if (isASN1FormatString(format) || format.equalsIgnoreCase("X.509")) {
                engineInit(params);
                return;
            }
            throw new IOException("Unknown parameter format " + format);
        }

        @Override
        protected String engineToString() {
            return "PSS Parameters";
        }
    }
}
