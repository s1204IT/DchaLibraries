package sun.security.provider.certpath;

import java.io.IOException;
import java.security.AccessController;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CRLReason;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateParsingException;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.security.auth.x500.X500Principal;
import sun.misc.HexDumpEncoder;
import sun.security.action.GetIntegerAction;
import sun.security.provider.certpath.OCSP;
import sun.security.util.Debug;
import sun.security.util.DerInputStream;
import sun.security.util.DerValue;
import sun.security.util.ObjectIdentifier;
import sun.security.x509.AlgorithmId;
import sun.security.x509.Extension;
import sun.security.x509.KeyIdentifier;
import sun.security.x509.PKIXExtensions;
import sun.security.x509.X509CertImpl;

public final class OCSPResponse {
    private static final int CERT_STATUS_GOOD = 0;
    private static final int CERT_STATUS_REVOKED = 1;
    private static final int CERT_STATUS_UNKNOWN = 2;
    private static final int DEFAULT_MAX_CLOCK_SKEW = 900000;
    private static final int KEY_TAG = 2;
    private static final String KP_OCSP_SIGNING_OID = "1.3.6.1.5.5.7.3.9";
    private static final int MAX_CLOCK_SKEW;
    private static final int NAME_TAG = 1;
    private static final ObjectIdentifier OCSP_BASIC_RESPONSE_OID;
    private static final boolean dump;
    private static CRLReason[] values;
    private List<X509CertImpl> certs;
    private KeyIdentifier responderKeyId;
    private X500Principal responderName;
    private final byte[] responseNonce;
    private final ResponseStatus responseStatus;
    private final AlgorithmId sigAlgId;
    private final byte[] signature;
    private X509CertImpl signerCert = null;
    private final Map<CertId, SingleResponse> singleResponseMap;
    private final byte[] tbsResponseData;
    private static ResponseStatus[] rsvalues = ResponseStatus.values();
    private static final Debug debug = Debug.getInstance("certpath");

    public enum ResponseStatus {
        SUCCESSFUL,
        MALFORMED_REQUEST,
        INTERNAL_ERROR,
        TRY_LATER,
        UNUSED,
        SIG_REQUIRED,
        UNAUTHORIZED
    }

    static {
        dump = debug != null && Debug.isOn("ocsp");
        OCSP_BASIC_RESPONSE_OID = ObjectIdentifier.newInternal(new int[]{1, 3, 6, 1, 5, 5, 7, 48, 1, 1});
        MAX_CLOCK_SKEW = initializeClockSkew();
        values = CRLReason.values();
    }

    private static int initializeClockSkew() {
        Integer num = (Integer) AccessController.doPrivileged(new GetIntegerAction("com.sun.security.ocsp.clockSkew"));
        if (num == null || num.intValue() < 0) {
            return DEFAULT_MAX_CLOCK_SKEW;
        }
        return num.intValue() * 1000;
    }

    OCSPResponse(byte[] bArr) throws IOException {
        byte[] bArr2 = 0;
        bArr2 = 0;
        this.responderName = null;
        this.responderKeyId = null;
        if (dump) {
            debug.println("OCSPResponse bytes...\n\n" + new HexDumpEncoder().encode(bArr) + "\n");
        }
        DerValue derValue = new DerValue(bArr);
        if (derValue.tag != 48) {
            throw new IOException("Bad encoding in OCSP response: expected ASN.1 SEQUENCE tag.");
        }
        DerInputStream data = derValue.getData();
        int enumerated = data.getEnumerated();
        if (enumerated >= 0 && enumerated < rsvalues.length) {
            this.responseStatus = rsvalues[enumerated];
            if (debug != null) {
                debug.println("OCSP response status: " + ((Object) this.responseStatus));
            }
            if (this.responseStatus != ResponseStatus.SUCCESSFUL) {
                this.singleResponseMap = Collections.emptyMap();
                this.certs = new ArrayList();
                this.sigAlgId = null;
                this.signature = null;
                this.tbsResponseData = null;
                this.responseNonce = null;
                return;
            }
            DerValue derValue2 = data.getDerValue();
            if (!derValue2.isContextSpecific((byte) 0)) {
                throw new IOException("Bad encoding in responseBytes element of OCSP response: expected ASN.1 context specific tag 0.");
            }
            DerValue derValue3 = derValue2.data.getDerValue();
            if (derValue3.tag != 48) {
                throw new IOException("Bad encoding in responseBytes element of OCSP response: expected ASN.1 SEQUENCE tag.");
            }
            DerInputStream derInputStream = derValue3.data;
            ObjectIdentifier oid = derInputStream.getOID();
            if (oid.equals((Object) OCSP_BASIC_RESPONSE_OID)) {
                if (debug != null) {
                    debug.println("OCSP response type: basic");
                }
                DerValue[] sequence = new DerInputStream(derInputStream.getOctetString()).getSequence(2);
                if (sequence.length < 3) {
                    throw new IOException("Unexpected BasicOCSPResponse value");
                }
                DerValue derValue4 = sequence[0];
                this.tbsResponseData = sequence[0].toByteArray();
                if (derValue4.tag != 48) {
                    throw new IOException("Bad encoding in tbsResponseData element of OCSP response: expected ASN.1 SEQUENCE tag.");
                }
                DerInputStream derInputStream2 = derValue4.data;
                DerValue derValue5 = derInputStream2.getDerValue();
                if (derValue5.isContextSpecific((byte) 0) && derValue5.isConstructed() && derValue5.isContextSpecific()) {
                    DerValue derValue6 = derValue5.data.getDerValue();
                    derValue6.getInteger();
                    if (derValue6.data.available() != 0) {
                        throw new IOException("Bad encoding in version  element of OCSP response: bad format");
                    }
                    derValue5 = derInputStream2.getDerValue();
                }
                short s = (byte) (derValue5.tag & 31);
                if (s == 1) {
                    this.responderName = new X500Principal(derValue5.getData().toByteArray());
                    if (debug != null) {
                        debug.println("Responder's name: " + ((Object) this.responderName));
                    }
                } else if (s == 2) {
                    this.responderKeyId = new KeyIdentifier(derValue5.getData().getOctetString());
                    if (debug != null) {
                        debug.println("Responder's key ID: " + Debug.toString(this.responderKeyId.getIdentifier()));
                    }
                } else {
                    throw new IOException("Bad encoding in responderID element of OCSP response: expected ASN.1 context specific tag 0 or 1");
                }
                DerValue derValue7 = derInputStream2.getDerValue();
                if (debug != null) {
                    debug.println("OCSP response produced at: " + ((Object) derValue7.getGeneralizedTime()));
                }
                DerValue[] sequence2 = derInputStream2.getSequence(1);
                this.singleResponseMap = new HashMap(sequence2.length);
                if (debug != null) {
                    debug.println("OCSP number of SingleResponses: " + sequence2.length);
                }
                for (DerValue derValue8 : sequence2) {
                    SingleResponse singleResponse = new SingleResponse(derValue8);
                    this.singleResponseMap.put(singleResponse.getCertId(), singleResponse);
                }
                if (derInputStream2.available() > 0) {
                    DerValue derValue9 = derInputStream2.getDerValue();
                    if (derValue9.isContextSpecific((byte) 1)) {
                        byte[] extensionValue = null;
                        for (DerValue derValue10 : derValue9.data.getSequence(3)) {
                            Extension extension = new Extension(derValue10);
                            if (debug != null) {
                                debug.println("OCSP extension: " + ((Object) extension));
                            }
                            if (extension.getExtensionId().equals((Object) OCSP.NONCE_EXTENSION_OID)) {
                                extensionValue = extension.getExtensionValue();
                            } else if (extension.isCritical()) {
                                throw new IOException("Unsupported OCSP critical extension: " + ((Object) extension.getExtensionId()));
                            }
                        }
                        bArr2 = extensionValue;
                    }
                }
                this.responseNonce = bArr2;
                this.sigAlgId = AlgorithmId.parse(sequence[1]);
                this.signature = sequence[2].getBitString();
                if (sequence.length > 3) {
                    DerValue derValue11 = sequence[3];
                    if (!derValue11.isContextSpecific((byte) 0)) {
                        throw new IOException("Bad encoding in certs element of OCSP response: expected ASN.1 context specific tag 0.");
                    }
                    DerValue[] sequence3 = derValue11.getData().getSequence(3);
                    this.certs = new ArrayList(sequence3.length);
                    for (int i = 0; i < sequence3.length; i++) {
                        try {
                            X509CertImpl x509CertImpl = new X509CertImpl(sequence3[i].toByteArray());
                            this.certs.add(x509CertImpl);
                            if (debug != null) {
                                debug.println("OCSP response cert #" + (i + 1) + ": " + ((Object) x509CertImpl.getSubjectX500Principal()));
                            }
                        } catch (CertificateException e) {
                            throw new IOException("Bad encoding in X509 Certificate", e);
                        }
                    }
                    return;
                }
                this.certs = new ArrayList();
                return;
            }
            if (debug != null) {
                debug.println("OCSP response type: " + ((Object) oid));
            }
            throw new IOException("Unsupported OCSP response type: " + ((Object) oid));
        }
        throw new IOException("Unknown OCSPResponse status: " + enumerated);
    }

    void verify(List<CertId> list, X509Certificate x509Certificate, X509Certificate x509Certificate2, Date date, byte[] bArr) throws CertPathValidatorException {
        switch (this.responseStatus) {
            case SUCCESSFUL:
                for (CertId certId : list) {
                    SingleResponse singleResponse = getSingleResponse(certId);
                    if (singleResponse == null) {
                        if (debug != null) {
                            debug.println("No response found for CertId: " + ((Object) certId));
                        }
                        throw new CertPathValidatorException("OCSP response does not include a response for a certificate supplied in the OCSP request");
                    }
                    if (debug != null) {
                        debug.println("Status of certificate (with serial number " + ((Object) certId.getSerialNumber()) + ") is: " + ((Object) singleResponse.getCertStatus()));
                    }
                }
                if (this.signerCert == null) {
                    try {
                        this.certs.add(X509CertImpl.toImpl(x509Certificate));
                        if (x509Certificate2 != null) {
                            this.certs.add(X509CertImpl.toImpl(x509Certificate2));
                        }
                        if (this.responderName != null) {
                            Iterator<X509CertImpl> it = this.certs.iterator();
                            while (true) {
                                if (it.hasNext()) {
                                    X509CertImpl next = it.next();
                                    if (next.getSubjectX500Principal().equals(this.responderName)) {
                                        this.signerCert = next;
                                    }
                                }
                            }
                        } else if (this.responderKeyId != null) {
                            Iterator<X509CertImpl> it2 = this.certs.iterator();
                            while (true) {
                                if (it2.hasNext()) {
                                    X509CertImpl next2 = it2.next();
                                    KeyIdentifier subjectKeyId = next2.getSubjectKeyId();
                                    if (subjectKeyId != null && this.responderKeyId.equals(subjectKeyId)) {
                                        this.signerCert = next2;
                                    } else {
                                        try {
                                            subjectKeyId = new KeyIdentifier(next2.getPublicKey());
                                        } catch (IOException e) {
                                        }
                                        if (this.responderKeyId.equals(subjectKeyId)) {
                                            this.signerCert = next2;
                                        }
                                    }
                                }
                            }
                        }
                    } catch (CertificateException e2) {
                        throw new CertPathValidatorException("Invalid issuer or trusted responder certificate", e2);
                    }
                    break;
                }
                if (this.signerCert != null) {
                    if (this.signerCert.equals(x509Certificate)) {
                        if (debug != null) {
                            debug.println("OCSP response is signed by the target's Issuing CA");
                        }
                    } else if (this.signerCert.equals(x509Certificate2)) {
                        if (debug != null) {
                            debug.println("OCSP response is signed by a Trusted Responder");
                        }
                    } else if (this.signerCert.getIssuerX500Principal().equals(x509Certificate.getSubjectX500Principal())) {
                        try {
                            List<String> extendedKeyUsage = this.signerCert.getExtendedKeyUsage();
                            if (extendedKeyUsage == null || !extendedKeyUsage.contains(KP_OCSP_SIGNING_OID)) {
                                throw new CertPathValidatorException("Responder's certificate not valid for signing OCSP responses");
                            }
                            AlgorithmChecker algorithmChecker = new AlgorithmChecker(new TrustAnchor(x509Certificate, null));
                            algorithmChecker.init(false);
                            algorithmChecker.check(this.signerCert, Collections.emptySet());
                            if (this.signerCert.getExtension(PKIXExtensions.OCSPNoCheck_Id) != null && debug != null) {
                                debug.println("Responder's certificate includes the extension id-pkix-ocsp-nocheck.");
                            }
                            try {
                                this.signerCert.verify(x509Certificate.getPublicKey());
                                if (debug != null) {
                                    debug.println("OCSP response is signed by an Authorized Responder");
                                }
                            } catch (GeneralSecurityException e3) {
                                this.signerCert = null;
                            }
                        } catch (CertificateParsingException e4) {
                            throw new CertPathValidatorException("Responder's certificate not valid for signing OCSP responses", e4);
                        }
                    } else {
                        throw new CertPathValidatorException("Responder's certificate is not authorized to sign OCSP responses");
                    }
                    break;
                }
                if (this.signerCert != null) {
                    AlgorithmChecker.check(this.signerCert.getPublicKey(), this.sigAlgId);
                    if (!verifySignature(this.signerCert)) {
                        throw new CertPathValidatorException("Error verifying OCSP Response's signature");
                    }
                    if (bArr != null && this.responseNonce != null && !Arrays.equals(bArr, this.responseNonce)) {
                        throw new CertPathValidatorException("Nonces don't match");
                    }
                    return;
                }
                throw new CertPathValidatorException("Unable to verify OCSP Response's signature");
            case TRY_LATER:
            case INTERNAL_ERROR:
                throw new CertPathValidatorException("OCSP response error: " + ((Object) this.responseStatus), null, null, -1, CertPathValidatorException.BasicReason.UNDETERMINED_REVOCATION_STATUS);
            default:
                throw new CertPathValidatorException("OCSP response error: " + ((Object) this.responseStatus));
        }
    }

    ResponseStatus getResponseStatus() {
        return this.responseStatus;
    }

    private boolean verifySignature(X509Certificate x509Certificate) throws CertPathValidatorException {
        try {
            Signature signature = Signature.getInstance(this.sigAlgId.getName());
            signature.initVerify(x509Certificate.getPublicKey());
            signature.update(this.tbsResponseData);
            if (signature.verify(this.signature)) {
                if (debug != null) {
                    debug.println("Verified signature of OCSP Response");
                    return true;
                }
                return true;
            }
            if (debug != null) {
                debug.println("Error verifying signature of OCSP Response");
                return false;
            }
            return false;
        } catch (InvalidKeyException | NoSuchAlgorithmException | SignatureException e) {
            throw new CertPathValidatorException(e);
        }
    }

    SingleResponse getSingleResponse(CertId certId) {
        return this.singleResponseMap.get(certId);
    }

    X509Certificate getSignerCertificate() {
        return this.signerCert;
    }

    static final class SingleResponse implements OCSP.RevocationStatus {
        private final CertId certId;
        private final OCSP.RevocationStatus.CertStatus certStatus;
        private final Date nextUpdate;
        private final CRLReason revocationReason;
        private final Date revocationTime;
        private final Map<String, java.security.cert.Extension> singleExtensions;
        private final Date thisUpdate;

        private SingleResponse(DerValue derValue) throws IOException {
            int enumerated;
            if (derValue.tag != 48) {
                throw new IOException("Bad ASN.1 encoding in SingleResponse");
            }
            DerInputStream derInputStream = derValue.data;
            this.certId = new CertId(derInputStream.getDerValue().data);
            DerValue derValue2 = derInputStream.getDerValue();
            short s = (byte) (derValue2.tag & 31);
            if (s == 1) {
                this.certStatus = OCSP.RevocationStatus.CertStatus.REVOKED;
                this.revocationTime = derValue2.data.getGeneralizedTime();
                if (derValue2.data.available() != 0) {
                    DerValue derValue3 = derValue2.data.getDerValue();
                    if (((byte) (derValue3.tag & 31)) == 0 && (enumerated = derValue3.data.getEnumerated()) >= 0 && enumerated < OCSPResponse.values.length) {
                        this.revocationReason = OCSPResponse.values[enumerated];
                    } else {
                        this.revocationReason = CRLReason.UNSPECIFIED;
                    }
                } else {
                    this.revocationReason = CRLReason.UNSPECIFIED;
                }
                if (OCSPResponse.debug != null) {
                    OCSPResponse.debug.println("Revocation time: " + ((Object) this.revocationTime));
                    OCSPResponse.debug.println("Revocation reason: " + ((Object) this.revocationReason));
                }
            } else {
                this.revocationTime = null;
                this.revocationReason = CRLReason.UNSPECIFIED;
                if (s == 0) {
                    this.certStatus = OCSP.RevocationStatus.CertStatus.GOOD;
                } else if (s == 2) {
                    this.certStatus = OCSP.RevocationStatus.CertStatus.UNKNOWN;
                } else {
                    throw new IOException("Invalid certificate status");
                }
            }
            this.thisUpdate = derInputStream.getGeneralizedTime();
            if (derInputStream.available() == 0) {
                this.nextUpdate = null;
            } else {
                DerValue derValue4 = derInputStream.getDerValue();
                if (((byte) (derValue4.tag & 31)) == 0) {
                    this.nextUpdate = derValue4.data.getGeneralizedTime();
                    if (derInputStream.available() != 0) {
                        byte b = derInputStream.getDerValue().tag;
                    }
                } else {
                    this.nextUpdate = null;
                }
            }
            if (derInputStream.available() > 0) {
                DerValue derValue5 = derInputStream.getDerValue();
                if (derValue5.isContextSpecific((byte) 1)) {
                    DerValue[] sequence = derValue5.data.getSequence(3);
                    this.singleExtensions = new HashMap(sequence.length);
                    for (DerValue derValue6 : sequence) {
                        Extension extension = new Extension(derValue6);
                        if (OCSPResponse.debug != null) {
                            OCSPResponse.debug.println("OCSP single extension: " + ((Object) extension));
                        }
                        if (extension.isCritical()) {
                            throw new IOException("Unsupported OCSP critical extension: " + ((Object) extension.getExtensionId()));
                        }
                        this.singleExtensions.put(extension.getId(), extension);
                    }
                    return;
                }
                this.singleExtensions = Collections.emptyMap();
                return;
            }
            this.singleExtensions = Collections.emptyMap();
        }

        @Override
        public OCSP.RevocationStatus.CertStatus getCertStatus() {
            return this.certStatus;
        }

        private CertId getCertId() {
            return this.certId;
        }

        @Override
        public Date getRevocationTime() {
            return (Date) this.revocationTime.clone();
        }

        @Override
        public CRLReason getRevocationReason() {
            return this.revocationReason;
        }

        @Override
        public Map<String, java.security.cert.Extension> getSingleExtensions() {
            return Collections.unmodifiableMap(this.singleExtensions);
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("SingleResponse:  \n");
            sb.append((Object) this.certId);
            sb.append("\nCertStatus: " + ((Object) this.certStatus) + "\n");
            if (this.certStatus == OCSP.RevocationStatus.CertStatus.REVOKED) {
                sb.append("revocationTime is " + ((Object) this.revocationTime) + "\n");
                sb.append("revocationReason is " + ((Object) this.revocationReason) + "\n");
            }
            sb.append("thisUpdate is " + ((Object) this.thisUpdate) + "\n");
            if (this.nextUpdate != null) {
                sb.append("nextUpdate is " + ((Object) this.nextUpdate) + "\n");
            }
            return sb.toString();
        }
    }
}
