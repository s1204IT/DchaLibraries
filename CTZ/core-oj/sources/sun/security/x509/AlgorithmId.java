package sun.security.x509;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.security.AlgorithmParameters;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import sun.security.util.DerEncoder;
import sun.security.util.DerInputStream;
import sun.security.util.DerOutputStream;
import sun.security.util.DerValue;
import sun.security.util.ObjectIdentifier;
import sun.util.locale.LanguageTag;

public class AlgorithmId implements Serializable, DerEncoder {
    private static final long serialVersionUID = 7205873507486557157L;
    private AlgorithmParameters algParams;
    private ObjectIdentifier algid;
    private boolean constructedFromDer;
    protected DerValue params;
    private static int initOidTableVersion = -1;
    private static final Map<String, ObjectIdentifier> oidTable = new HashMap(1);
    private static final Map<ObjectIdentifier, String> nameTable = new HashMap();
    public static final ObjectIdentifier MD2_oid = ObjectIdentifier.newInternal(new int[]{1, 2, 840, 113549, 2, 2});
    public static final ObjectIdentifier MD5_oid = ObjectIdentifier.newInternal(new int[]{1, 2, 840, 113549, 2, 5});
    public static final ObjectIdentifier SHA_oid = ObjectIdentifier.newInternal(new int[]{1, 3, 14, 3, 2, 26});
    public static final ObjectIdentifier SHA224_oid = ObjectIdentifier.newInternal(new int[]{2, 16, 840, 1, 101, 3, 4, 2, 4});
    public static final ObjectIdentifier SHA256_oid = ObjectIdentifier.newInternal(new int[]{2, 16, 840, 1, 101, 3, 4, 2, 1});
    public static final ObjectIdentifier SHA384_oid = ObjectIdentifier.newInternal(new int[]{2, 16, 840, 1, 101, 3, 4, 2, 2});
    public static final ObjectIdentifier SHA512_oid = ObjectIdentifier.newInternal(new int[]{2, 16, 840, 1, 101, 3, 4, 2, 3});
    private static final int[] DH_data = {1, 2, 840, 113549, 1, 3, 1};
    private static final int[] DH_PKIX_data = {1, 2, 840, 10046, 2, 1};
    private static final int[] DSA_OIW_data = {1, 3, 14, 3, 2, 12};
    private static final int[] DSA_PKIX_data = {1, 2, 840, 10040, 4, 1};
    private static final int[] RSA_data = {2, 5, 8, 1, 1};
    private static final int[] RSAEncryption_data = {1, 2, 840, 113549, 1, 1, 1};
    public static final ObjectIdentifier EC_oid = oid(1, 2, 840, 10045, 2, 1);
    public static final ObjectIdentifier ECDH_oid = oid(1, 3, 132, 1, 12);
    public static final ObjectIdentifier AES_oid = oid(2, 16, 840, 1, 101, 3, 4, 1);
    private static final int[] md2WithRSAEncryption_data = {1, 2, 840, 113549, 1, 1, 2};
    private static final int[] md5WithRSAEncryption_data = {1, 2, 840, 113549, 1, 1, 4};
    private static final int[] sha1WithRSAEncryption_data = {1, 2, 840, 113549, 1, 1, 5};
    private static final int[] sha1WithRSAEncryption_OIW_data = {1, 3, 14, 3, 2, 29};
    private static final int[] sha224WithRSAEncryption_data = {1, 2, 840, 113549, 1, 1, 14};
    private static final int[] sha256WithRSAEncryption_data = {1, 2, 840, 113549, 1, 1, 11};
    private static final int[] sha384WithRSAEncryption_data = {1, 2, 840, 113549, 1, 1, 12};
    private static final int[] sha512WithRSAEncryption_data = {1, 2, 840, 113549, 1, 1, 13};
    private static final int[] shaWithDSA_OIW_data = {1, 3, 14, 3, 2, 13};
    private static final int[] sha1WithDSA_OIW_data = {1, 3, 14, 3, 2, 27};
    private static final int[] dsaWithSHA1_PKIX_data = {1, 2, 840, 10040, 4, 3};
    public static final ObjectIdentifier sha224WithDSA_oid = oid(2, 16, 840, 1, 101, 3, 4, 3, 1);
    public static final ObjectIdentifier sha256WithDSA_oid = oid(2, 16, 840, 1, 101, 3, 4, 3, 2);
    public static final ObjectIdentifier sha1WithECDSA_oid = oid(1, 2, 840, 10045, 4, 1);
    public static final ObjectIdentifier sha224WithECDSA_oid = oid(1, 2, 840, 10045, 4, 3, 1);
    public static final ObjectIdentifier sha256WithECDSA_oid = oid(1, 2, 840, 10045, 4, 3, 2);
    public static final ObjectIdentifier sha384WithECDSA_oid = oid(1, 2, 840, 10045, 4, 3, 3);
    public static final ObjectIdentifier sha512WithECDSA_oid = oid(1, 2, 840, 10045, 4, 3, 4);
    public static final ObjectIdentifier specifiedWithECDSA_oid = oid(1, 2, 840, 10045, 4, 3);
    public static final ObjectIdentifier pbeWithMD5AndDES_oid = ObjectIdentifier.newInternal(new int[]{1, 2, 840, 113549, 1, 5, 3});
    public static final ObjectIdentifier pbeWithMD5AndRC2_oid = ObjectIdentifier.newInternal(new int[]{1, 2, 840, 113549, 1, 5, 6});
    public static final ObjectIdentifier pbeWithSHA1AndDES_oid = ObjectIdentifier.newInternal(new int[]{1, 2, 840, 113549, 1, 5, 10});
    public static final ObjectIdentifier pbeWithSHA1AndRC2_oid = ObjectIdentifier.newInternal(new int[]{1, 2, 840, 113549, 1, 5, 11});
    public static ObjectIdentifier pbeWithSHA1AndDESede_oid = ObjectIdentifier.newInternal(new int[]{1, 2, 840, 113549, 1, 12, 1, 3});
    public static ObjectIdentifier pbeWithSHA1AndRC2_40_oid = ObjectIdentifier.newInternal(new int[]{1, 2, 840, 113549, 1, 12, 1, 6});
    public static final ObjectIdentifier DH_oid = ObjectIdentifier.newInternal(DH_data);
    public static final ObjectIdentifier DH_PKIX_oid = ObjectIdentifier.newInternal(DH_PKIX_data);
    public static final ObjectIdentifier DSA_OIW_oid = ObjectIdentifier.newInternal(DSA_OIW_data);
    public static final ObjectIdentifier DSA_oid = ObjectIdentifier.newInternal(DSA_PKIX_data);
    public static final ObjectIdentifier RSA_oid = ObjectIdentifier.newInternal(RSA_data);
    public static final ObjectIdentifier RSAEncryption_oid = ObjectIdentifier.newInternal(RSAEncryption_data);
    public static final ObjectIdentifier md2WithRSAEncryption_oid = ObjectIdentifier.newInternal(md2WithRSAEncryption_data);
    public static final ObjectIdentifier md5WithRSAEncryption_oid = ObjectIdentifier.newInternal(md5WithRSAEncryption_data);
    public static final ObjectIdentifier sha1WithRSAEncryption_oid = ObjectIdentifier.newInternal(sha1WithRSAEncryption_data);
    public static final ObjectIdentifier sha1WithRSAEncryption_OIW_oid = ObjectIdentifier.newInternal(sha1WithRSAEncryption_OIW_data);
    public static final ObjectIdentifier sha224WithRSAEncryption_oid = ObjectIdentifier.newInternal(sha224WithRSAEncryption_data);
    public static final ObjectIdentifier sha256WithRSAEncryption_oid = ObjectIdentifier.newInternal(sha256WithRSAEncryption_data);
    public static final ObjectIdentifier sha384WithRSAEncryption_oid = ObjectIdentifier.newInternal(sha384WithRSAEncryption_data);
    public static final ObjectIdentifier sha512WithRSAEncryption_oid = ObjectIdentifier.newInternal(sha512WithRSAEncryption_data);
    public static final ObjectIdentifier shaWithDSA_OIW_oid = ObjectIdentifier.newInternal(shaWithDSA_OIW_data);
    public static final ObjectIdentifier sha1WithDSA_OIW_oid = ObjectIdentifier.newInternal(sha1WithDSA_OIW_data);
    public static final ObjectIdentifier sha1WithDSA_oid = ObjectIdentifier.newInternal(dsaWithSHA1_PKIX_data);

    @Deprecated
    public AlgorithmId() {
        this.constructedFromDer = true;
    }

    public AlgorithmId(ObjectIdentifier objectIdentifier) {
        this.constructedFromDer = true;
        this.algid = objectIdentifier;
    }

    public AlgorithmId(ObjectIdentifier objectIdentifier, AlgorithmParameters algorithmParameters) {
        this.constructedFromDer = true;
        this.algid = objectIdentifier;
        this.algParams = algorithmParameters;
        this.constructedFromDer = false;
    }

    private AlgorithmId(ObjectIdentifier objectIdentifier, DerValue derValue) throws IOException {
        this.constructedFromDer = true;
        this.algid = objectIdentifier;
        this.params = derValue;
        if (this.params != null) {
            decodeParams();
        }
    }

    protected void decodeParams() throws IOException {
        try {
            this.algParams = AlgorithmParameters.getInstance(this.algid.toString());
            this.algParams.init(this.params.toByteArray());
        } catch (NoSuchAlgorithmException e) {
            this.algParams = null;
        }
    }

    public final void encode(DerOutputStream derOutputStream) throws IOException {
        derEncode(derOutputStream);
    }

    @Override
    public void derEncode(OutputStream outputStream) throws IOException {
        DerOutputStream derOutputStream = new DerOutputStream();
        DerOutputStream derOutputStream2 = new DerOutputStream();
        derOutputStream.putOID(this.algid);
        if (!this.constructedFromDer) {
            if (this.algParams != null) {
                this.params = new DerValue(this.algParams.getEncoded());
            } else {
                this.params = null;
            }
        }
        if (this.params == null) {
            derOutputStream.putNull();
        } else {
            derOutputStream.putDerValue(this.params);
        }
        derOutputStream2.write((byte) 48, derOutputStream);
        outputStream.write(derOutputStream2.toByteArray());
    }

    public final byte[] encode() throws IOException {
        DerOutputStream derOutputStream = new DerOutputStream();
        derEncode(derOutputStream);
        return derOutputStream.toByteArray();
    }

    public final ObjectIdentifier getOID() {
        return this.algid;
    }

    public String getName() {
        String str;
        String str2 = nameTable.get(this.algid);
        if (str2 != null) {
            return str2;
        }
        if (this.params != null && this.algid.equals((Object) specifiedWithECDSA_oid)) {
            try {
                makeSigAlg(parse(new DerValue(getEncodedParams())).getName(), "EC");
            } catch (IOException e) {
            }
        }
        synchronized (oidTable) {
            reinitializeMappingTableLocked();
            str = nameTable.get(this.algid);
        }
        return str == null ? this.algid.toString() : str;
    }

    public AlgorithmParameters getParameters() {
        return this.algParams;
    }

    public byte[] getEncodedParams() throws IOException {
        if (this.params == null) {
            return null;
        }
        return this.params.toByteArray();
    }

    public boolean equals(AlgorithmId algorithmId) {
        boolean zEquals;
        if (this.params != null) {
            zEquals = this.params.equals(algorithmId.params);
        } else {
            zEquals = algorithmId.params == null;
        }
        return this.algid.equals((Object) algorithmId.algid) && zEquals;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof AlgorithmId) {
            return equals((AlgorithmId) obj);
        }
        if (obj instanceof ObjectIdentifier) {
            return equals((ObjectIdentifier) obj);
        }
        return false;
    }

    public final boolean equals(ObjectIdentifier objectIdentifier) {
        return this.algid.equals((Object) objectIdentifier);
    }

    public int hashCode() {
        return (this.algid.toString() + paramsToString()).hashCode();
    }

    protected String paramsToString() {
        if (this.params == null) {
            return "";
        }
        if (this.algParams != null) {
            return this.algParams.toString();
        }
        return ", params unparsed";
    }

    public String toString() {
        return getName() + paramsToString();
    }

    public static AlgorithmId parse(DerValue derValue) throws IOException {
        if (derValue.tag != 48) {
            throw new IOException("algid parse error, not a sequence");
        }
        DerInputStream derInputStream = derValue.toDerInputStream();
        ObjectIdentifier oid = derInputStream.getOID();
        DerValue derValue2 = null;
        if (derInputStream.available() != 0) {
            DerValue derValue3 = derInputStream.getDerValue();
            if (derValue3.tag == 5) {
                if (derValue3.length() != 0) {
                    throw new IOException("invalid NULL");
                }
            } else {
                derValue2 = derValue3;
            }
            if (derInputStream.available() != 0) {
                throw new IOException("Invalid AlgorithmIdentifier: extra data");
            }
        }
        return new AlgorithmId(oid, derValue2);
    }

    @Deprecated
    public static AlgorithmId getAlgorithmId(String str) throws NoSuchAlgorithmException {
        return get(str);
    }

    public static AlgorithmId get(String str) throws NoSuchAlgorithmException {
        try {
            ObjectIdentifier objectIdentifierAlgOID = algOID(str);
            if (objectIdentifierAlgOID == null) {
                throw new NoSuchAlgorithmException("unrecognized algorithm name: " + str);
            }
            return new AlgorithmId(objectIdentifierAlgOID);
        } catch (IOException e) {
            throw new NoSuchAlgorithmException("Invalid ObjectIdentifier " + str);
        }
    }

    public static AlgorithmId get(AlgorithmParameters algorithmParameters) throws NoSuchAlgorithmException {
        String algorithm = algorithmParameters.getAlgorithm();
        try {
            ObjectIdentifier objectIdentifierAlgOID = algOID(algorithm);
            if (objectIdentifierAlgOID == null) {
                throw new NoSuchAlgorithmException("unrecognized algorithm name: " + algorithm);
            }
            return new AlgorithmId(objectIdentifierAlgOID, algorithmParameters);
        } catch (IOException e) {
            throw new NoSuchAlgorithmException("Invalid ObjectIdentifier " + algorithm);
        }
    }

    private static ObjectIdentifier algOID(String str) throws IOException {
        ObjectIdentifier objectIdentifier;
        if (str.indexOf(46) != -1) {
            if (str.startsWith("OID.")) {
                return new ObjectIdentifier(str.substring("OID.".length()));
            }
            return new ObjectIdentifier(str);
        }
        if (str.equalsIgnoreCase("MD5")) {
            return MD5_oid;
        }
        if (str.equalsIgnoreCase("MD2")) {
            return MD2_oid;
        }
        if (str.equalsIgnoreCase("SHA") || str.equalsIgnoreCase("SHA1") || str.equalsIgnoreCase("SHA-1")) {
            return SHA_oid;
        }
        if (str.equalsIgnoreCase("SHA-256") || str.equalsIgnoreCase("SHA256")) {
            return SHA256_oid;
        }
        if (str.equalsIgnoreCase("SHA-384") || str.equalsIgnoreCase("SHA384")) {
            return SHA384_oid;
        }
        if (str.equalsIgnoreCase("SHA-512") || str.equalsIgnoreCase("SHA512")) {
            return SHA512_oid;
        }
        if (str.equalsIgnoreCase("SHA-224") || str.equalsIgnoreCase("SHA224")) {
            return SHA224_oid;
        }
        if (str.equalsIgnoreCase("RSA")) {
            return RSAEncryption_oid;
        }
        if (str.equalsIgnoreCase("Diffie-Hellman") || str.equalsIgnoreCase("DH")) {
            return DH_oid;
        }
        if (str.equalsIgnoreCase("DSA")) {
            return DSA_oid;
        }
        if (str.equalsIgnoreCase("EC")) {
            return EC_oid;
        }
        if (str.equalsIgnoreCase("ECDH")) {
            return ECDH_oid;
        }
        if (str.equalsIgnoreCase("AES")) {
            return AES_oid;
        }
        if (str.equalsIgnoreCase("MD5withRSA") || str.equalsIgnoreCase("MD5/RSA")) {
            return md5WithRSAEncryption_oid;
        }
        if (str.equalsIgnoreCase("MD2withRSA") || str.equalsIgnoreCase("MD2/RSA")) {
            return md2WithRSAEncryption_oid;
        }
        if (str.equalsIgnoreCase("SHAwithDSA") || str.equalsIgnoreCase("SHA1withDSA") || str.equalsIgnoreCase("SHA/DSA") || str.equalsIgnoreCase("SHA1/DSA") || str.equalsIgnoreCase("DSAWithSHA1") || str.equalsIgnoreCase("DSS") || str.equalsIgnoreCase("SHA-1/DSA")) {
            return sha1WithDSA_oid;
        }
        if (str.equalsIgnoreCase("SHA224WithDSA")) {
            return sha224WithDSA_oid;
        }
        if (str.equalsIgnoreCase("SHA256WithDSA")) {
            return sha256WithDSA_oid;
        }
        if (str.equalsIgnoreCase("SHA1WithRSA") || str.equalsIgnoreCase("SHA1/RSA")) {
            return sha1WithRSAEncryption_oid;
        }
        if (str.equalsIgnoreCase("SHA1withECDSA") || str.equalsIgnoreCase("ECDSA")) {
            return sha1WithECDSA_oid;
        }
        if (str.equalsIgnoreCase("SHA224withECDSA")) {
            return sha224WithECDSA_oid;
        }
        if (str.equalsIgnoreCase("SHA256withECDSA")) {
            return sha256WithECDSA_oid;
        }
        if (str.equalsIgnoreCase("SHA384withECDSA")) {
            return sha384WithECDSA_oid;
        }
        if (str.equalsIgnoreCase("SHA512withECDSA")) {
            return sha512WithECDSA_oid;
        }
        synchronized (oidTable) {
            reinitializeMappingTableLocked();
            objectIdentifier = oidTable.get(str.toUpperCase(Locale.ENGLISH));
        }
        return objectIdentifier;
    }

    private static void reinitializeMappingTableLocked() {
        ObjectIdentifier objectIdentifier;
        ObjectIdentifier objectIdentifier2;
        String property;
        int version = Security.getVersion();
        if (initOidTableVersion != version) {
            Provider[] providers = Security.getProviders();
            for (int i = 0; i < providers.length; i++) {
                Enumeration<Object> enumerationKeys = providers[i].keys();
                while (enumerationKeys.hasMoreElements()) {
                    String str = (String) enumerationKeys.nextElement();
                    String upperCase = str.toUpperCase(Locale.ENGLISH);
                    if (upperCase.startsWith("ALG.ALIAS")) {
                        int iIndexOf = upperCase.indexOf("OID.", 0);
                        if (iIndexOf != -1) {
                            int length = iIndexOf + "OID.".length();
                            if (length == str.length()) {
                                break;
                            }
                            String strSubstring = str.substring(length);
                            String property2 = providers[i].getProperty(str);
                            if (property2 != null) {
                                String upperCase2 = property2.toUpperCase(Locale.ENGLISH);
                                try {
                                    objectIdentifier = new ObjectIdentifier(strSubstring);
                                } catch (IOException e) {
                                    objectIdentifier = null;
                                }
                                if (objectIdentifier != null) {
                                    if (!oidTable.containsKey(upperCase2)) {
                                        oidTable.put(upperCase2, objectIdentifier);
                                    }
                                    if (!nameTable.containsKey(objectIdentifier)) {
                                        nameTable.put(objectIdentifier, upperCase2);
                                    }
                                }
                            }
                        } else {
                            try {
                                objectIdentifier2 = new ObjectIdentifier(str.substring(str.indexOf(46, "ALG.ALIAS.".length()) + 1));
                            } catch (IOException e2) {
                                objectIdentifier2 = null;
                            }
                            if (objectIdentifier2 != null && (property = providers[i].getProperty(str)) != null) {
                                String upperCase3 = property.toUpperCase(Locale.ENGLISH);
                                if (!oidTable.containsKey(upperCase3)) {
                                    oidTable.put(upperCase3, objectIdentifier2);
                                }
                                if (!nameTable.containsKey(objectIdentifier2)) {
                                    nameTable.put(objectIdentifier2, upperCase3);
                                }
                            }
                        }
                    }
                }
            }
            initOidTableVersion = version;
        }
    }

    private static ObjectIdentifier oid(int... iArr) {
        return ObjectIdentifier.newInternal(iArr);
    }

    static {
        nameTable.put(MD5_oid, "MD5");
        nameTable.put(MD2_oid, "MD2");
        nameTable.put(SHA_oid, "SHA-1");
        nameTable.put(SHA224_oid, "SHA-224");
        nameTable.put(SHA256_oid, "SHA-256");
        nameTable.put(SHA384_oid, "SHA-384");
        nameTable.put(SHA512_oid, "SHA-512");
        nameTable.put(RSAEncryption_oid, "RSA");
        nameTable.put(RSA_oid, "RSA");
        nameTable.put(DH_oid, "Diffie-Hellman");
        nameTable.put(DH_PKIX_oid, "Diffie-Hellman");
        nameTable.put(DSA_oid, "DSA");
        nameTable.put(DSA_OIW_oid, "DSA");
        nameTable.put(EC_oid, "EC");
        nameTable.put(ECDH_oid, "ECDH");
        nameTable.put(AES_oid, "AES");
        nameTable.put(sha1WithECDSA_oid, "SHA1withECDSA");
        nameTable.put(sha224WithECDSA_oid, "SHA224withECDSA");
        nameTable.put(sha256WithECDSA_oid, "SHA256withECDSA");
        nameTable.put(sha384WithECDSA_oid, "SHA384withECDSA");
        nameTable.put(sha512WithECDSA_oid, "SHA512withECDSA");
        nameTable.put(md5WithRSAEncryption_oid, "MD5withRSA");
        nameTable.put(md2WithRSAEncryption_oid, "MD2withRSA");
        nameTable.put(sha1WithDSA_oid, "SHA1withDSA");
        nameTable.put(sha1WithDSA_OIW_oid, "SHA1withDSA");
        nameTable.put(shaWithDSA_OIW_oid, "SHA1withDSA");
        nameTable.put(sha224WithDSA_oid, "SHA224withDSA");
        nameTable.put(sha256WithDSA_oid, "SHA256withDSA");
        nameTable.put(sha1WithRSAEncryption_oid, "SHA1withRSA");
        nameTable.put(sha1WithRSAEncryption_OIW_oid, "SHA1withRSA");
        nameTable.put(sha224WithRSAEncryption_oid, "SHA224withRSA");
        nameTable.put(sha256WithRSAEncryption_oid, "SHA256withRSA");
        nameTable.put(sha384WithRSAEncryption_oid, "SHA384withRSA");
        nameTable.put(sha512WithRSAEncryption_oid, "SHA512withRSA");
        nameTable.put(pbeWithMD5AndDES_oid, "PBEWithMD5AndDES");
        nameTable.put(pbeWithMD5AndRC2_oid, "PBEWithMD5AndRC2");
        nameTable.put(pbeWithSHA1AndDES_oid, "PBEWithSHA1AndDES");
        nameTable.put(pbeWithSHA1AndRC2_oid, "PBEWithSHA1AndRC2");
        nameTable.put(pbeWithSHA1AndDESede_oid, "PBEWithSHA1AndDESede");
        nameTable.put(pbeWithSHA1AndRC2_40_oid, "PBEWithSHA1AndRC2_40");
    }

    public static String makeSigAlg(String str, String str2) {
        String strReplace = str.replace(LanguageTag.SEP, "");
        if (str2.equalsIgnoreCase("EC")) {
            str2 = "ECDSA";
        }
        return strReplace + "with" + str2;
    }

    public static String getEncAlgFromSigAlg(String str) {
        String strSubstring;
        String upperCase = str.toUpperCase(Locale.ENGLISH);
        int iIndexOf = upperCase.indexOf("WITH");
        if (iIndexOf > 0) {
            int i = iIndexOf + 4;
            int iIndexOf2 = upperCase.indexOf("AND", i);
            if (iIndexOf2 > 0) {
                strSubstring = upperCase.substring(i, iIndexOf2);
            } else {
                strSubstring = upperCase.substring(i);
            }
            return strSubstring.equalsIgnoreCase("ECDSA") ? "EC" : strSubstring;
        }
        return null;
    }

    public static String getDigAlgFromSigAlg(String str) {
        String upperCase = str.toUpperCase(Locale.ENGLISH);
        int iIndexOf = upperCase.indexOf("WITH");
        if (iIndexOf > 0) {
            return upperCase.substring(0, iIndexOf);
        }
        return null;
    }
}
