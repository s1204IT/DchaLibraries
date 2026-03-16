package org.apache.harmony.security.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import javax.security.auth.x500.X500Principal;
import javax.xml.datatype.DatatypeConstants;
import org.apache.harmony.security.asn1.ASN1OctetString;
import org.apache.harmony.security.asn1.BerInputStream;
import org.apache.harmony.security.pkcs7.ContentInfo;
import org.apache.harmony.security.pkcs7.SignedData;
import org.apache.harmony.security.pkcs7.SignerInfo;
import org.apache.harmony.security.x501.AttributeTypeAndValue;

public class JarUtils {
    private static final int[] MESSAGE_DIGEST_OID = {1, 2, DatatypeConstants.MIN_TIMEZONE_OFFSET, 113549, 1, 9, 4};

    public static Certificate[] verifySignature(InputStream signature, InputStream signatureBlock) throws GeneralSecurityException, IOException {
        BerInputStream bis = new BerInputStream(signatureBlock);
        ContentInfo info = (ContentInfo) ContentInfo.ASN1.decode(bis);
        SignedData signedData = info.getSignedData();
        if (signedData == null) {
            throw new IOException("No SignedData found");
        }
        Collection<org.apache.harmony.security.x509.Certificate> encCerts = signedData.getCertificates();
        if (encCerts.isEmpty()) {
            return null;
        }
        X509Certificate[] certs = new X509Certificate[encCerts.size()];
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        int i = 0;
        for (org.apache.harmony.security.x509.Certificate encCert : encCerts) {
            byte[] encoded = encCert.getEncoded();
            InputStream is = new ByteArrayInputStream(encoded);
            certs[i] = new VerbatimX509Certificate((X509Certificate) cf.generateCertificate(is), encoded);
            i++;
        }
        List<SignerInfo> sigInfos = signedData.getSignerInfos();
        if (!sigInfos.isEmpty()) {
            SignerInfo sigInfo = sigInfos.get(0);
            X500Principal issuer = sigInfo.getIssuer();
            BigInteger snum = sigInfo.getSerialNumber();
            int issuerSertIndex = 0;
            int i2 = 0;
            while (true) {
                if (i2 >= certs.length) {
                    break;
                }
                if (!issuer.equals(certs[i2].getIssuerDN()) || !snum.equals(certs[i2].getSerialNumber())) {
                    i2++;
                } else {
                    issuerSertIndex = i2;
                    break;
                }
            }
            if (i2 == certs.length) {
                return null;
            }
            if (certs[issuerSertIndex].hasUnsupportedCriticalExtension()) {
                throw new SecurityException("Can not recognize a critical extension");
            }
            String daOid = sigInfo.getDigestAlgorithm();
            String daName = sigInfo.getDigestAlgorithmName();
            String deaOid = sigInfo.getDigestEncryptionAlgorithm();
            String deaName = sigInfo.getDigestEncryptionAlgorithmName();
            Signature sig = null;
            if (daOid != null && deaOid != null) {
                String alg = daOid + "with" + deaOid;
                try {
                    sig = Signature.getInstance(alg);
                } catch (NoSuchAlgorithmException e) {
                }
                if (sig == null && daName != null && deaName != null) {
                    String alg2 = daName + "with" + deaName;
                    try {
                        sig = Signature.getInstance(alg2);
                    } catch (NoSuchAlgorithmException e2) {
                    }
                }
            }
            if (sig == null && deaOid != null) {
                try {
                    sig = Signature.getInstance(deaOid);
                } catch (NoSuchAlgorithmException e3) {
                }
                if (sig == null) {
                    try {
                        sig = Signature.getInstance(deaName);
                    } catch (NoSuchAlgorithmException e4) {
                    }
                }
            }
            if (sig == null) {
                return null;
            }
            sig.initVerify(certs[issuerSertIndex]);
            List<AttributeTypeAndValue> atr = sigInfo.getAuthenticatedAttributes();
            byte[] sfBytes = new byte[signature.available()];
            signature.read(sfBytes);
            if (atr == null) {
                sig.update(sfBytes);
            } else {
                sig.update(sigInfo.getEncodedAuthenticatedAttributes());
                byte[] existingDigest = null;
                for (AttributeTypeAndValue a : atr) {
                    if (Arrays.equals(a.getType().getOid(), MESSAGE_DIGEST_OID)) {
                        if (existingDigest != null) {
                            throw new SecurityException("Too many MessageDigest attributes");
                        }
                        Collection<?> entries = a.getValue().getValues(ASN1OctetString.getInstance());
                        if (entries.size() != 1) {
                            throw new SecurityException("Too many values for MessageDigest attribute");
                        }
                        byte[] existingDigest2 = (byte[]) entries.iterator().next();
                        existingDigest = existingDigest2;
                    }
                }
                if (existingDigest == null) {
                    throw new SecurityException("Missing MessageDigest in Authenticated Attributes");
                }
                MessageDigest md = null;
                if (daOid != null) {
                    md = MessageDigest.getInstance(daOid);
                }
                if (md == null && daName != null) {
                    md = MessageDigest.getInstance(daName);
                }
                if (md == null) {
                    return null;
                }
                byte[] computedDigest = md.digest(sfBytes);
                if (!Arrays.equals(existingDigest, computedDigest)) {
                    throw new SecurityException("Incorrect MD");
                }
            }
            if (!sig.verify(sigInfo.getEncryptedDigest())) {
                throw new SecurityException("Incorrect signature");
            }
            return createChain(certs[issuerSertIndex], certs);
        }
        return null;
    }

    private static X509Certificate[] createChain(X509Certificate signer, X509Certificate[] candidates) {
        X509Certificate issuerCert;
        Principal issuer = signer.getIssuerDN();
        if (signer.getSubjectDN().equals(issuer)) {
            return new X509Certificate[]{signer};
        }
        ArrayList<X509Certificate> chain = new ArrayList<>(candidates.length + 1);
        chain.add(0, signer);
        int count = 1;
        do {
            issuerCert = findCert(issuer, candidates);
            if (issuerCert != null) {
                chain.add(issuerCert);
                count++;
                if (count > candidates.length) {
                    break;
                }
                issuer = issuerCert.getIssuerDN();
            } else {
                break;
            }
        } while (!issuerCert.getSubjectDN().equals(issuer));
        return (X509Certificate[]) chain.toArray(new X509Certificate[count]);
    }

    private static X509Certificate findCert(Principal issuer, X509Certificate[] candidates) {
        for (int i = 0; i < candidates.length; i++) {
            if (issuer.equals(candidates[i].getSubjectDN())) {
                return candidates[i];
            }
        }
        return null;
    }

    private static class VerbatimX509Certificate extends WrappedX509Certificate {
        private byte[] encodedVerbatim;

        public VerbatimX509Certificate(X509Certificate wrapped, byte[] encodedVerbatim) {
            super(wrapped);
            this.encodedVerbatim = encodedVerbatim;
        }

        @Override
        public byte[] getEncoded() throws CertificateEncodingException {
            return this.encodedVerbatim;
        }
    }
}
