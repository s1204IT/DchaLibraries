package sun.security.pkcs;

import java.io.IOException;
import java.io.OutputStream;
import java.security.cert.CertificateException;
import java.util.Date;
import java.util.Hashtable;
import java.util.Locale;
import sun.misc.HexDumpEncoder;
import sun.security.util.Debug;
import sun.security.util.DerEncoder;
import sun.security.util.DerInputStream;
import sun.security.util.DerOutputStream;
import sun.security.util.DerValue;
import sun.security.util.ObjectIdentifier;
import sun.security.x509.CertificateExtensions;

public class PKCS9Attribute implements DerEncoder {
    private static final Class<?> BYTE_ARRAY_CLASS;
    public static final ObjectIdentifier CHALLENGE_PASSWORD_OID;
    public static final String CHALLENGE_PASSWORD_STR = "ChallengePassword";
    public static final ObjectIdentifier CONTENT_TYPE_OID;
    public static final String CONTENT_TYPE_STR = "ContentType";
    public static final ObjectIdentifier COUNTERSIGNATURE_OID;
    public static final String COUNTERSIGNATURE_STR = "Countersignature";
    public static final ObjectIdentifier EMAIL_ADDRESS_OID;
    public static final String EMAIL_ADDRESS_STR = "EmailAddress";
    public static final ObjectIdentifier EXTENDED_CERTIFICATE_ATTRIBUTES_OID;
    public static final String EXTENDED_CERTIFICATE_ATTRIBUTES_STR = "ExtendedCertificateAttributes";
    public static final ObjectIdentifier EXTENSION_REQUEST_OID;
    public static final String EXTENSION_REQUEST_STR = "ExtensionRequest";
    public static final ObjectIdentifier ISSUER_SERIALNUMBER_OID;
    public static final String ISSUER_SERIALNUMBER_STR = "IssuerAndSerialNumber";
    public static final ObjectIdentifier MESSAGE_DIGEST_OID;
    public static final String MESSAGE_DIGEST_STR = "MessageDigest";
    private static final Hashtable<String, ObjectIdentifier> NAME_OID_TABLE;
    private static final Hashtable<ObjectIdentifier, String> OID_NAME_TABLE;
    private static final Byte[][] PKCS9_VALUE_TAGS;
    private static final String RSA_PROPRIETARY_STR = "RSAProprietary";
    public static final ObjectIdentifier SIGNATURE_TIMESTAMP_TOKEN_OID;
    public static final String SIGNATURE_TIMESTAMP_TOKEN_STR = "SignatureTimestampToken";
    public static final ObjectIdentifier SIGNING_CERTIFICATE_OID;
    public static final String SIGNING_CERTIFICATE_STR = "SigningCertificate";
    public static final ObjectIdentifier SIGNING_TIME_OID;
    public static final String SIGNING_TIME_STR = "SigningTime";
    private static final boolean[] SINGLE_VALUED;
    public static final ObjectIdentifier SMIME_CAPABILITY_OID;
    public static final String SMIME_CAPABILITY_STR = "SMIMECapability";
    private static final String SMIME_SIGNING_DESC_STR = "SMIMESigningDesc";
    public static final ObjectIdentifier UNSTRUCTURED_ADDRESS_OID;
    public static final String UNSTRUCTURED_ADDRESS_STR = "UnstructuredAddress";
    public static final ObjectIdentifier UNSTRUCTURED_NAME_OID;
    public static final String UNSTRUCTURED_NAME_STR = "UnstructuredName";
    private static final Class<?>[] VALUE_CLASSES;
    private int index;
    private ObjectIdentifier oid;
    private Object value;
    private static final Debug debug = Debug.getInstance("jar");
    static final ObjectIdentifier[] PKCS9_OIDS = new ObjectIdentifier[18];

    static {
        for (int i = 1; i < PKCS9_OIDS.length - 2; i++) {
            PKCS9_OIDS[i] = ObjectIdentifier.newInternal(new int[]{1, 2, 840, 113549, 1, 9, i});
        }
        PKCS9_OIDS[PKCS9_OIDS.length - 2] = ObjectIdentifier.newInternal(new int[]{1, 2, 840, 113549, 1, 9, 16, 2, 12});
        PKCS9_OIDS[PKCS9_OIDS.length - 1] = ObjectIdentifier.newInternal(new int[]{1, 2, 840, 113549, 1, 9, 16, 2, 14});
        try {
            BYTE_ARRAY_CLASS = Class.forName("[B");
            EMAIL_ADDRESS_OID = PKCS9_OIDS[1];
            UNSTRUCTURED_NAME_OID = PKCS9_OIDS[2];
            CONTENT_TYPE_OID = PKCS9_OIDS[3];
            MESSAGE_DIGEST_OID = PKCS9_OIDS[4];
            SIGNING_TIME_OID = PKCS9_OIDS[5];
            COUNTERSIGNATURE_OID = PKCS9_OIDS[6];
            CHALLENGE_PASSWORD_OID = PKCS9_OIDS[7];
            UNSTRUCTURED_ADDRESS_OID = PKCS9_OIDS[8];
            EXTENDED_CERTIFICATE_ATTRIBUTES_OID = PKCS9_OIDS[9];
            ISSUER_SERIALNUMBER_OID = PKCS9_OIDS[10];
            EXTENSION_REQUEST_OID = PKCS9_OIDS[14];
            SMIME_CAPABILITY_OID = PKCS9_OIDS[15];
            SIGNING_CERTIFICATE_OID = PKCS9_OIDS[16];
            SIGNATURE_TIMESTAMP_TOKEN_OID = PKCS9_OIDS[17];
            NAME_OID_TABLE = new Hashtable<>(18);
            NAME_OID_TABLE.put("emailaddress", PKCS9_OIDS[1]);
            NAME_OID_TABLE.put("unstructuredname", PKCS9_OIDS[2]);
            NAME_OID_TABLE.put("contenttype", PKCS9_OIDS[3]);
            NAME_OID_TABLE.put("messagedigest", PKCS9_OIDS[4]);
            NAME_OID_TABLE.put("signingtime", PKCS9_OIDS[5]);
            NAME_OID_TABLE.put("countersignature", PKCS9_OIDS[6]);
            NAME_OID_TABLE.put("challengepassword", PKCS9_OIDS[7]);
            NAME_OID_TABLE.put("unstructuredaddress", PKCS9_OIDS[8]);
            NAME_OID_TABLE.put("extendedcertificateattributes", PKCS9_OIDS[9]);
            NAME_OID_TABLE.put("issuerandserialnumber", PKCS9_OIDS[10]);
            NAME_OID_TABLE.put("rsaproprietary", PKCS9_OIDS[11]);
            NAME_OID_TABLE.put("rsaproprietary", PKCS9_OIDS[12]);
            NAME_OID_TABLE.put("signingdescription", PKCS9_OIDS[13]);
            NAME_OID_TABLE.put("extensionrequest", PKCS9_OIDS[14]);
            NAME_OID_TABLE.put("smimecapability", PKCS9_OIDS[15]);
            NAME_OID_TABLE.put("signingcertificate", PKCS9_OIDS[16]);
            NAME_OID_TABLE.put("signaturetimestamptoken", PKCS9_OIDS[17]);
            OID_NAME_TABLE = new Hashtable<>(16);
            OID_NAME_TABLE.put(PKCS9_OIDS[1], EMAIL_ADDRESS_STR);
            OID_NAME_TABLE.put(PKCS9_OIDS[2], UNSTRUCTURED_NAME_STR);
            OID_NAME_TABLE.put(PKCS9_OIDS[3], CONTENT_TYPE_STR);
            OID_NAME_TABLE.put(PKCS9_OIDS[4], MESSAGE_DIGEST_STR);
            OID_NAME_TABLE.put(PKCS9_OIDS[5], SIGNING_TIME_STR);
            OID_NAME_TABLE.put(PKCS9_OIDS[6], COUNTERSIGNATURE_STR);
            OID_NAME_TABLE.put(PKCS9_OIDS[7], CHALLENGE_PASSWORD_STR);
            OID_NAME_TABLE.put(PKCS9_OIDS[8], UNSTRUCTURED_ADDRESS_STR);
            OID_NAME_TABLE.put(PKCS9_OIDS[9], EXTENDED_CERTIFICATE_ATTRIBUTES_STR);
            OID_NAME_TABLE.put(PKCS9_OIDS[10], ISSUER_SERIALNUMBER_STR);
            OID_NAME_TABLE.put(PKCS9_OIDS[11], RSA_PROPRIETARY_STR);
            OID_NAME_TABLE.put(PKCS9_OIDS[12], RSA_PROPRIETARY_STR);
            OID_NAME_TABLE.put(PKCS9_OIDS[13], SMIME_SIGNING_DESC_STR);
            OID_NAME_TABLE.put(PKCS9_OIDS[14], EXTENSION_REQUEST_STR);
            OID_NAME_TABLE.put(PKCS9_OIDS[15], SMIME_CAPABILITY_STR);
            OID_NAME_TABLE.put(PKCS9_OIDS[16], SIGNING_CERTIFICATE_STR);
            OID_NAME_TABLE.put(PKCS9_OIDS[17], SIGNATURE_TIMESTAMP_TOKEN_STR);
            PKCS9_VALUE_TAGS = new Byte[][]{null, new Byte[]{new Byte((byte) 22)}, new Byte[]{new Byte((byte) 22), new Byte((byte) 19)}, new Byte[]{new Byte((byte) 6)}, new Byte[]{new Byte((byte) 4)}, new Byte[]{new Byte((byte) 23)}, new Byte[]{new Byte((byte) 48)}, new Byte[]{new Byte((byte) 19), new Byte((byte) 20)}, new Byte[]{new Byte((byte) 19), new Byte((byte) 20)}, new Byte[]{new Byte((byte) 49)}, new Byte[]{new Byte((byte) 48)}, null, null, null, new Byte[]{new Byte((byte) 48)}, new Byte[]{new Byte((byte) 48)}, new Byte[]{new Byte((byte) 48)}, new Byte[]{new Byte((byte) 48)}};
            VALUE_CLASSES = new Class[18];
            try {
                Class<?> cls = Class.forName("[Ljava.lang.String;");
                VALUE_CLASSES[0] = null;
                VALUE_CLASSES[1] = cls;
                VALUE_CLASSES[2] = cls;
                VALUE_CLASSES[3] = Class.forName("sun.security.util.ObjectIdentifier");
                VALUE_CLASSES[4] = BYTE_ARRAY_CLASS;
                VALUE_CLASSES[5] = Class.forName("java.util.Date");
                VALUE_CLASSES[6] = Class.forName("[Lsun.security.pkcs.SignerInfo;");
                VALUE_CLASSES[7] = Class.forName("java.lang.String");
                VALUE_CLASSES[8] = cls;
                VALUE_CLASSES[9] = null;
                VALUE_CLASSES[10] = null;
                VALUE_CLASSES[11] = null;
                VALUE_CLASSES[12] = null;
                VALUE_CLASSES[13] = null;
                VALUE_CLASSES[14] = Class.forName("sun.security.x509.CertificateExtensions");
                VALUE_CLASSES[15] = null;
                VALUE_CLASSES[16] = null;
                VALUE_CLASSES[17] = BYTE_ARRAY_CLASS;
                SINGLE_VALUED = new boolean[]{false, false, false, true, true, true, false, true, false, false, true, false, false, false, true, true, true, true};
            } catch (ClassNotFoundException e) {
                throw new ExceptionInInitializerError(e.toString());
            }
        } catch (ClassNotFoundException e2) {
            throw new ExceptionInInitializerError(e2.toString());
        }
    }

    public PKCS9Attribute(ObjectIdentifier objectIdentifier, Object obj) throws IllegalArgumentException {
        init(objectIdentifier, obj);
    }

    public PKCS9Attribute(String str, Object obj) throws IllegalArgumentException {
        ObjectIdentifier oid = getOID(str);
        if (oid == null) {
            throw new IllegalArgumentException("Unrecognized attribute name " + str + " constructing PKCS9Attribute.");
        }
        init(oid, obj);
    }

    private void init(ObjectIdentifier objectIdentifier, Object obj) throws IllegalArgumentException {
        this.oid = objectIdentifier;
        this.index = indexOf(objectIdentifier, PKCS9_OIDS, 1);
        Class<?> cls = this.index == -1 ? BYTE_ARRAY_CLASS : VALUE_CLASSES[this.index];
        if (!cls.isInstance(obj)) {
            throw new IllegalArgumentException("Wrong value class  for attribute " + ((Object) objectIdentifier) + " constructing PKCS9Attribute; was " + obj.getClass().toString() + ", should be " + cls.toString());
        }
        this.value = obj;
    }

    public PKCS9Attribute(DerValue derValue) throws IOException {
        DerInputStream derInputStream = new DerInputStream(derValue.toByteArray());
        DerValue[] sequence = derInputStream.getSequence(2);
        if (derInputStream.available() == 0) {
            if (sequence.length != 2) {
                throw new IOException("PKCS9Attribute doesn't have two components");
            }
            int i = 0;
            this.oid = sequence[0].getOID();
            byte[] byteArray = sequence[1].toByteArray();
            DerValue[] set = new DerInputStream(byteArray).getSet(1);
            this.index = indexOf(this.oid, PKCS9_OIDS, 1);
            if (this.index == -1) {
                if (debug != null) {
                    debug.println("Unsupported signer attribute: " + ((Object) this.oid));
                }
                this.value = byteArray;
                return;
            }
            if (SINGLE_VALUED[this.index] && set.length > 1) {
                throwSingleValuedException();
            }
            for (DerValue derValue2 : set) {
                Byte b = new Byte(derValue2.tag);
                if (indexOf(b, PKCS9_VALUE_TAGS[this.index], 0) == -1) {
                    throwTagException(b);
                }
            }
            switch (this.index) {
                case 1:
                case 2:
                case 8:
                    String[] strArr = new String[set.length];
                    while (i < set.length) {
                        strArr[i] = set[i].getAsString();
                        i++;
                    }
                    this.value = strArr;
                    return;
                case 3:
                    this.value = set[0].getOID();
                    return;
                case 4:
                    this.value = set[0].getOctetString();
                    return;
                case 5:
                    this.value = new DerInputStream(set[0].toByteArray()).getUTCTime();
                    return;
                case 6:
                    SignerInfo[] signerInfoArr = new SignerInfo[set.length];
                    while (i < set.length) {
                        signerInfoArr[i] = new SignerInfo(set[i].toDerInputStream());
                        i++;
                    }
                    this.value = signerInfoArr;
                    return;
                case 7:
                    this.value = set[0].getAsString();
                    return;
                case 9:
                    throw new IOException("PKCS9 extended-certificate attribute not supported.");
                case 10:
                    throw new IOException("PKCS9 IssuerAndSerialNumberattribute not supported.");
                case 11:
                case 12:
                    throw new IOException("PKCS9 RSA DSI attributes11 and 12, not supported.");
                case 13:
                    throw new IOException("PKCS9 attribute #13 not supported.");
                case 14:
                    this.value = new CertificateExtensions(new DerInputStream(set[0].toByteArray()));
                    return;
                case 15:
                    throw new IOException("PKCS9 SMIMECapability attribute not supported.");
                case 16:
                    this.value = new SigningCertificateInfo(set[0].toByteArray());
                    return;
                case 17:
                    this.value = set[0].toByteArray();
                    return;
                default:
                    return;
            }
        }
        throw new IOException("Excess data parsing PKCS9Attribute");
    }

    @Override
    public void derEncode(OutputStream outputStream) throws IOException {
        DerOutputStream derOutputStream = new DerOutputStream();
        derOutputStream.putOID(this.oid);
        int i = this.index;
        if (i == -1) {
            derOutputStream.write((byte[]) this.value);
        } else {
            int i2 = 0;
            switch (i) {
                case 1:
                case 2:
                    String[] strArr = (String[]) this.value;
                    DerOutputStream[] derOutputStreamArr = new DerOutputStream[strArr.length];
                    while (i2 < strArr.length) {
                        derOutputStreamArr[i2] = new DerOutputStream();
                        derOutputStreamArr[i2].putIA5String(strArr[i2]);
                        i2++;
                    }
                    derOutputStream.putOrderedSetOf((byte) 49, derOutputStreamArr);
                    break;
                case 3:
                    DerOutputStream derOutputStream2 = new DerOutputStream();
                    derOutputStream2.putOID((ObjectIdentifier) this.value);
                    derOutputStream.write((byte) 49, derOutputStream2.toByteArray());
                    break;
                case 4:
                    DerOutputStream derOutputStream3 = new DerOutputStream();
                    derOutputStream3.putOctetString((byte[]) this.value);
                    derOutputStream.write((byte) 49, derOutputStream3.toByteArray());
                    break;
                case 5:
                    DerOutputStream derOutputStream4 = new DerOutputStream();
                    derOutputStream4.putUTCTime((Date) this.value);
                    derOutputStream.write((byte) 49, derOutputStream4.toByteArray());
                    break;
                case 6:
                    derOutputStream.putOrderedSetOf((byte) 49, (DerEncoder[]) this.value);
                    break;
                case 7:
                    DerOutputStream derOutputStream5 = new DerOutputStream();
                    derOutputStream5.putPrintableString((String) this.value);
                    derOutputStream.write((byte) 49, derOutputStream5.toByteArray());
                    break;
                case 8:
                    String[] strArr2 = (String[]) this.value;
                    DerOutputStream[] derOutputStreamArr2 = new DerOutputStream[strArr2.length];
                    while (i2 < strArr2.length) {
                        derOutputStreamArr2[i2] = new DerOutputStream();
                        derOutputStreamArr2[i2].putPrintableString(strArr2[i2]);
                        i2++;
                    }
                    derOutputStream.putOrderedSetOf((byte) 49, derOutputStreamArr2);
                    break;
                case 9:
                    throw new IOException("PKCS9 extended-certificate attribute not supported.");
                case 10:
                    throw new IOException("PKCS9 IssuerAndSerialNumberattribute not supported.");
                case 11:
                case 12:
                    throw new IOException("PKCS9 RSA DSI attributes11 and 12, not supported.");
                case 13:
                    throw new IOException("PKCS9 attribute #13 not supported.");
                case 14:
                    DerOutputStream derOutputStream6 = new DerOutputStream();
                    try {
                        ((CertificateExtensions) this.value).encode(derOutputStream6, true);
                        derOutputStream.write((byte) 49, derOutputStream6.toByteArray());
                    } catch (CertificateException e) {
                        throw new IOException(e.toString());
                    }
                    break;
                case 15:
                    throw new IOException("PKCS9 attribute #15 not supported.");
                case 16:
                    throw new IOException("PKCS9 SigningCertificate attribute not supported.");
                case 17:
                    derOutputStream.write((byte) 49, (byte[]) this.value);
                    break;
            }
        }
        DerOutputStream derOutputStream7 = new DerOutputStream();
        derOutputStream7.write((byte) 48, derOutputStream.toByteArray());
        outputStream.write(derOutputStream7.toByteArray());
    }

    public boolean isKnown() {
        return this.index != -1;
    }

    public Object getValue() {
        return this.value;
    }

    public boolean isSingleValued() {
        return this.index == -1 || SINGLE_VALUED[this.index];
    }

    public ObjectIdentifier getOID() {
        return this.oid;
    }

    public String getName() {
        if (this.index == -1) {
            return this.oid.toString();
        }
        return OID_NAME_TABLE.get(PKCS9_OIDS[this.index]);
    }

    public static ObjectIdentifier getOID(String str) {
        return NAME_OID_TABLE.get(str.toLowerCase(Locale.ENGLISH));
    }

    public static String getName(ObjectIdentifier objectIdentifier) {
        return OID_NAME_TABLE.get(objectIdentifier);
    }

    public String toString() {
        StringBuffer stringBuffer = new StringBuffer(100);
        stringBuffer.append("[");
        if (this.index == -1) {
            stringBuffer.append(this.oid.toString());
        } else {
            stringBuffer.append(OID_NAME_TABLE.get(PKCS9_OIDS[this.index]));
        }
        stringBuffer.append(": ");
        if (this.index == -1 || SINGLE_VALUED[this.index]) {
            if (this.value instanceof byte[]) {
                stringBuffer.append(new HexDumpEncoder().encodeBuffer((byte[]) this.value));
            } else {
                stringBuffer.append(this.value.toString());
            }
            stringBuffer.append("]");
            return stringBuffer.toString();
        }
        boolean z = true;
        for (Object obj : (Object[]) this.value) {
            if (!z) {
                stringBuffer.append(", ");
            } else {
                z = false;
            }
            stringBuffer.append(obj.toString());
        }
        return stringBuffer.toString();
    }

    static int indexOf(Object obj, Object[] objArr, int i) {
        while (i < objArr.length) {
            if (obj.equals(objArr[i])) {
                return i;
            }
            i++;
        }
        return -1;
    }

    private void throwSingleValuedException() throws IOException {
        throw new IOException("Single-value attribute " + ((Object) this.oid) + " (" + getName() + ") has multiple values.");
    }

    private void throwTagException(Byte b) throws IOException {
        Byte[] bArr = PKCS9_VALUE_TAGS[this.index];
        StringBuffer stringBuffer = new StringBuffer(100);
        stringBuffer.append("Value of attribute ");
        stringBuffer.append(this.oid.toString());
        stringBuffer.append(" (");
        stringBuffer.append(getName());
        stringBuffer.append(") has wrong tag: ");
        stringBuffer.append(b.toString());
        stringBuffer.append(".  Expected tags: ");
        stringBuffer.append(bArr[0].toString());
        for (int i = 1; i < bArr.length; i++) {
            stringBuffer.append(", ");
            stringBuffer.append(bArr[i].toString());
        }
        stringBuffer.append(".");
        throw new IOException(stringBuffer.toString());
    }
}
