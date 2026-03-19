package com.android.org.conscrypt;

import java.io.IOException;
import java.security.AlgorithmParametersSpi;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.MGF1ParameterSpec;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;

public class OAEPParameters extends AlgorithmParametersSpi {
    private static final String MGF1_OID = "1.2.840.113549.1.1.8";
    private static final String PSPECIFIED_OID = "1.2.840.113549.1.1.9";
    private OAEPParameterSpec spec = OAEPParameterSpec.DEFAULT;
    private static final Map<String, String> OID_TO_NAME = new HashMap();
    private static final Map<String, String> NAME_TO_OID = new HashMap();

    static {
        OID_TO_NAME.put("1.3.14.3.2.26", "SHA-1");
        OID_TO_NAME.put("2.16.840.1.101.3.4.2.4", "SHA-224");
        OID_TO_NAME.put("2.16.840.1.101.3.4.2.1", "SHA-256");
        OID_TO_NAME.put("2.16.840.1.101.3.4.2.2", "SHA-384");
        OID_TO_NAME.put("2.16.840.1.101.3.4.2.3", "SHA-512");
        for (Map.Entry<String, String> entry : OID_TO_NAME.entrySet()) {
            NAME_TO_OID.put(entry.getValue(), entry.getKey());
        }
    }

    @Override
    protected void engineInit(AlgorithmParameterSpec algorithmParameterSpec) throws InvalidParameterSpecException {
        if (algorithmParameterSpec instanceof OAEPParameterSpec) {
            this.spec = (OAEPParameterSpec) algorithmParameterSpec;
            return;
        }
        throw new InvalidParameterSpecException("Only OAEPParameterSpec is supported");
    }

    @Override
    protected void engineInit(byte[] bArr) throws Throwable {
        long jAsn1_read_init;
        long jAsn1_read_sequence;
        long jAsn1_read_tagged;
        long jAsn1_read_sequence2;
        long jAsn1_read_tagged2;
        long jAsn1_read_sequence3;
        PSource.PSpecified pSpecified;
        long j = 0;
        try {
            jAsn1_read_init = NativeCrypto.asn1_read_init(bArr);
            try {
                jAsn1_read_sequence = NativeCrypto.asn1_read_sequence(jAsn1_read_init);
                try {
                    String hashName = "SHA-1";
                    String hashName2 = "SHA-1";
                    PSource.PSpecified pSpecified2 = PSource.PSpecified.DEFAULT;
                    if (NativeCrypto.asn1_read_next_tag_is(jAsn1_read_sequence, 0)) {
                        try {
                            long jAsn1_read_tagged3 = NativeCrypto.asn1_read_tagged(jAsn1_read_sequence);
                            try {
                                hashName = getHashName(jAsn1_read_tagged3);
                                NativeCrypto.asn1_read_free(jAsn1_read_tagged3);
                            } catch (Throwable th) {
                                th = th;
                                j = jAsn1_read_tagged3;
                                NativeCrypto.asn1_read_free(j);
                                throw th;
                            }
                        } catch (Throwable th2) {
                            th = th2;
                        }
                    }
                    if (NativeCrypto.asn1_read_next_tag_is(jAsn1_read_sequence, 1)) {
                        try {
                            jAsn1_read_tagged = NativeCrypto.asn1_read_tagged(jAsn1_read_sequence);
                            try {
                                jAsn1_read_sequence2 = NativeCrypto.asn1_read_sequence(jAsn1_read_tagged);
                                try {
                                    if (!NativeCrypto.asn1_read_oid(jAsn1_read_sequence2).equals(MGF1_OID)) {
                                        throw new IOException("Error reading ASN.1 encoding");
                                    }
                                    hashName2 = getHashName(jAsn1_read_sequence2);
                                    if (!NativeCrypto.asn1_read_is_empty(jAsn1_read_sequence2)) {
                                        throw new IOException("Error reading ASN.1 encoding");
                                    }
                                    NativeCrypto.asn1_read_free(jAsn1_read_sequence2);
                                    NativeCrypto.asn1_read_free(jAsn1_read_tagged);
                                } catch (Throwable th3) {
                                    th = th3;
                                    NativeCrypto.asn1_read_free(jAsn1_read_sequence2);
                                    NativeCrypto.asn1_read_free(jAsn1_read_tagged);
                                    throw th;
                                }
                            } catch (Throwable th4) {
                                th = th4;
                                jAsn1_read_sequence2 = 0;
                            }
                        } catch (Throwable th5) {
                            th = th5;
                            jAsn1_read_tagged = 0;
                            jAsn1_read_sequence2 = 0;
                        }
                    }
                    if (NativeCrypto.asn1_read_next_tag_is(jAsn1_read_sequence, 2)) {
                        try {
                            jAsn1_read_tagged2 = NativeCrypto.asn1_read_tagged(jAsn1_read_sequence);
                            try {
                                jAsn1_read_sequence3 = NativeCrypto.asn1_read_sequence(jAsn1_read_tagged2);
                            } catch (Throwable th6) {
                                th = th6;
                                jAsn1_read_sequence3 = 0;
                            }
                        } catch (Throwable th7) {
                            th = th7;
                            jAsn1_read_tagged2 = 0;
                            jAsn1_read_sequence3 = 0;
                        }
                        try {
                            if (!NativeCrypto.asn1_read_oid(jAsn1_read_sequence3).equals(PSPECIFIED_OID)) {
                                throw new IOException("Error reading ASN.1 encoding");
                            }
                            pSpecified = new PSource.PSpecified(NativeCrypto.asn1_read_octetstring(jAsn1_read_sequence3));
                            if (!NativeCrypto.asn1_read_is_empty(jAsn1_read_sequence3)) {
                                throw new IOException("Error reading ASN.1 encoding");
                            }
                            NativeCrypto.asn1_read_free(jAsn1_read_sequence3);
                            NativeCrypto.asn1_read_free(jAsn1_read_tagged2);
                        } catch (Throwable th8) {
                            th = th8;
                            NativeCrypto.asn1_read_free(jAsn1_read_sequence3);
                            NativeCrypto.asn1_read_free(jAsn1_read_tagged2);
                            throw th;
                        }
                    } else {
                        pSpecified = pSpecified2;
                    }
                    if (!NativeCrypto.asn1_read_is_empty(jAsn1_read_sequence) || !NativeCrypto.asn1_read_is_empty(jAsn1_read_init)) {
                        throw new IOException("Error reading ASN.1 encoding");
                    }
                    this.spec = new OAEPParameterSpec(hashName, "MGF1", new MGF1ParameterSpec(hashName2), pSpecified);
                    NativeCrypto.asn1_read_free(jAsn1_read_sequence);
                    NativeCrypto.asn1_read_free(jAsn1_read_init);
                } catch (Throwable th9) {
                    th = th9;
                    NativeCrypto.asn1_read_free(jAsn1_read_sequence);
                    NativeCrypto.asn1_read_free(jAsn1_read_init);
                    throw th;
                }
            } catch (Throwable th10) {
                th = th10;
                jAsn1_read_sequence = 0;
            }
        } catch (Throwable th11) {
            th = th11;
            jAsn1_read_init = 0;
            jAsn1_read_sequence = 0;
        }
    }

    @Override
    protected void engineInit(byte[] bArr, String str) throws Throwable {
        if (str == null || str.equals("ASN.1")) {
            engineInit(bArr);
            return;
        }
        throw new IOException("Unsupported format: " + str);
    }

    private static String getHashName(long j) throws Throwable {
        long jAsn1_read_sequence;
        try {
            jAsn1_read_sequence = NativeCrypto.asn1_read_sequence(j);
            try {
                String strAsn1_read_oid = NativeCrypto.asn1_read_oid(jAsn1_read_sequence);
                if (!NativeCrypto.asn1_read_is_empty(jAsn1_read_sequence)) {
                    NativeCrypto.asn1_read_null(jAsn1_read_sequence);
                }
                if (!NativeCrypto.asn1_read_is_empty(jAsn1_read_sequence) || !OID_TO_NAME.containsKey(strAsn1_read_oid)) {
                    throw new IOException("Error reading ASN.1 encoding");
                }
                String str = OID_TO_NAME.get(strAsn1_read_oid);
                NativeCrypto.asn1_read_free(jAsn1_read_sequence);
                return str;
            } catch (Throwable th) {
                th = th;
                NativeCrypto.asn1_read_free(jAsn1_read_sequence);
                throw th;
            }
        } catch (Throwable th2) {
            th = th2;
            jAsn1_read_sequence = 0;
        }
    }

    @Override
    protected <T extends AlgorithmParameterSpec> T engineGetParameterSpec(Class<T> cls) throws InvalidParameterSpecException {
        if (cls != null && cls == OAEPParameterSpec.class) {
            return this.spec;
        }
        throw new InvalidParameterSpecException("Unsupported class: " + cls);
    }

    @Override
    protected byte[] engineGetEncoded() throws Throwable {
        long j;
        Throwable th;
        long jAsn1_write_init;
        IOException e;
        long jAsn1_write_tag;
        long jWriteAlgorithmIdentifier;
        long jAsn1_write_tag2;
        long jWriteAlgorithmIdentifier2;
        Throwable th2;
        long jAsn1_write_tag3;
        long j2 = 0;
        try {
            try {
                jAsn1_write_init = NativeCrypto.asn1_write_init();
            } catch (Throwable th3) {
                th = th3;
            }
        } catch (IOException e2) {
            e = e2;
            jAsn1_write_init = 0;
        } catch (Throwable th4) {
            j = 0;
            th = th4;
            jAsn1_write_init = 0;
        }
        try {
            long jAsn1_write_sequence = NativeCrypto.asn1_write_sequence(jAsn1_write_init);
            try {
                if (!this.spec.getDigestAlgorithm().equals("SHA-1")) {
                    try {
                        jAsn1_write_tag3 = NativeCrypto.asn1_write_tag(jAsn1_write_sequence, 0);
                        try {
                            long jWriteAlgorithmIdentifier3 = writeAlgorithmIdentifier(jAsn1_write_tag3, NAME_TO_OID.get(this.spec.getDigestAlgorithm()));
                            try {
                                NativeCrypto.asn1_write_null(jWriteAlgorithmIdentifier3);
                                NativeCrypto.asn1_write_flush(jAsn1_write_sequence);
                                NativeCrypto.asn1_write_free(jWriteAlgorithmIdentifier3);
                                NativeCrypto.asn1_write_free(jAsn1_write_tag3);
                            } catch (Throwable th5) {
                                th2 = th5;
                                j2 = jWriteAlgorithmIdentifier3;
                                NativeCrypto.asn1_write_flush(jAsn1_write_sequence);
                                NativeCrypto.asn1_write_free(j2);
                                NativeCrypto.asn1_write_free(jAsn1_write_tag3);
                                throw th2;
                            }
                        } catch (Throwable th6) {
                            th2 = th6;
                        }
                    } catch (Throwable th7) {
                        th2 = th7;
                        jAsn1_write_tag3 = 0;
                    }
                }
                MGF1ParameterSpec mGF1ParameterSpec = (MGF1ParameterSpec) this.spec.getMGFParameters();
                if (!mGF1ParameterSpec.getDigestAlgorithm().equals("SHA-1")) {
                    try {
                        jAsn1_write_tag2 = NativeCrypto.asn1_write_tag(jAsn1_write_sequence, 1);
                        try {
                            jWriteAlgorithmIdentifier2 = writeAlgorithmIdentifier(jAsn1_write_tag2, MGF1_OID);
                            try {
                                long jWriteAlgorithmIdentifier4 = writeAlgorithmIdentifier(jWriteAlgorithmIdentifier2, NAME_TO_OID.get(mGF1ParameterSpec.getDigestAlgorithm()));
                                try {
                                    NativeCrypto.asn1_write_null(jWriteAlgorithmIdentifier4);
                                    NativeCrypto.asn1_write_flush(jAsn1_write_sequence);
                                    NativeCrypto.asn1_write_free(jWriteAlgorithmIdentifier4);
                                    NativeCrypto.asn1_write_free(jWriteAlgorithmIdentifier2);
                                    NativeCrypto.asn1_write_free(jAsn1_write_tag2);
                                } catch (Throwable th8) {
                                    th = th8;
                                    j2 = jWriteAlgorithmIdentifier4;
                                    NativeCrypto.asn1_write_flush(jAsn1_write_sequence);
                                    NativeCrypto.asn1_write_free(j2);
                                    NativeCrypto.asn1_write_free(jWriteAlgorithmIdentifier2);
                                    NativeCrypto.asn1_write_free(jAsn1_write_tag2);
                                    throw th;
                                }
                            } catch (Throwable th9) {
                                th = th9;
                            }
                        } catch (Throwable th10) {
                            th = th10;
                            jWriteAlgorithmIdentifier2 = 0;
                        }
                    } catch (Throwable th11) {
                        th = th11;
                        jAsn1_write_tag2 = 0;
                        jWriteAlgorithmIdentifier2 = 0;
                    }
                }
                PSource.PSpecified pSpecified = (PSource.PSpecified) this.spec.getPSource();
                if (pSpecified.getValue().length != 0) {
                    try {
                        jAsn1_write_tag = NativeCrypto.asn1_write_tag(jAsn1_write_sequence, 2);
                        try {
                            jWriteAlgorithmIdentifier = writeAlgorithmIdentifier(jAsn1_write_tag, PSPECIFIED_OID);
                        } catch (Throwable th12) {
                            th = th12;
                        }
                    } catch (Throwable th13) {
                        th = th13;
                        jAsn1_write_tag = 0;
                    }
                    try {
                        NativeCrypto.asn1_write_octetstring(jWriteAlgorithmIdentifier, pSpecified.getValue());
                        NativeCrypto.asn1_write_flush(jAsn1_write_sequence);
                        NativeCrypto.asn1_write_free(jWriteAlgorithmIdentifier);
                        NativeCrypto.asn1_write_free(jAsn1_write_tag);
                    } catch (Throwable th14) {
                        th = th14;
                        j2 = jWriteAlgorithmIdentifier;
                        NativeCrypto.asn1_write_flush(jAsn1_write_sequence);
                        NativeCrypto.asn1_write_free(j2);
                        NativeCrypto.asn1_write_free(jAsn1_write_tag);
                        throw th;
                    }
                }
                byte[] bArrAsn1_write_finish = NativeCrypto.asn1_write_finish(jAsn1_write_init);
                NativeCrypto.asn1_write_free(jAsn1_write_sequence);
                NativeCrypto.asn1_write_free(jAsn1_write_init);
                return bArrAsn1_write_finish;
            } catch (IOException e3) {
                e = e3;
                NativeCrypto.asn1_write_cleanup(jAsn1_write_init);
                throw e;
            }
        } catch (IOException e4) {
            e = e4;
        } catch (Throwable th15) {
            th = th15;
            j = 0;
            NativeCrypto.asn1_write_free(j);
            NativeCrypto.asn1_write_free(jAsn1_write_init);
            throw th;
        }
    }

    @Override
    protected byte[] engineGetEncoded(String str) throws IOException {
        if (str == null || str.equals("ASN.1")) {
            return engineGetEncoded();
        }
        throw new IOException("Unsupported format: " + str);
    }

    private static long writeAlgorithmIdentifier(long j, String str) throws IOException {
        long jAsn1_write_sequence;
        try {
            jAsn1_write_sequence = NativeCrypto.asn1_write_sequence(j);
            try {
                NativeCrypto.asn1_write_oid(jAsn1_write_sequence, str);
                return jAsn1_write_sequence;
            } catch (IOException e) {
                e = e;
                NativeCrypto.asn1_write_free(jAsn1_write_sequence);
                throw e;
            }
        } catch (IOException e2) {
            e = e2;
            jAsn1_write_sequence = 0;
        }
    }

    @Override
    protected String engineToString() {
        return "Conscrypt OAEP AlgorithmParameters";
    }
}
