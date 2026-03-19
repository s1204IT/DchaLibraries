package sun.security.x509;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Enumeration;
import sun.security.util.BitArray;
import sun.security.util.DerOutputStream;
import sun.security.util.DerValue;

public class KeyUsageExtension extends Extension implements CertAttrSet<String> {
    public static final String CRL_SIGN = "crl_sign";
    public static final String DATA_ENCIPHERMENT = "data_encipherment";
    public static final String DECIPHER_ONLY = "decipher_only";
    public static final String DIGITAL_SIGNATURE = "digital_signature";
    public static final String ENCIPHER_ONLY = "encipher_only";
    public static final String IDENT = "x509.info.extensions.KeyUsage";
    public static final String KEY_AGREEMENT = "key_agreement";
    public static final String KEY_CERTSIGN = "key_certsign";
    public static final String KEY_ENCIPHERMENT = "key_encipherment";
    public static final String NAME = "KeyUsage";
    public static final String NON_REPUDIATION = "non_repudiation";
    private boolean[] bitString;

    private void encodeThis() throws IOException {
        DerOutputStream derOutputStream = new DerOutputStream();
        derOutputStream.putTruncatedUnalignedBitString(new BitArray(this.bitString));
        this.extensionValue = derOutputStream.toByteArray();
    }

    private boolean isSet(int i) {
        return i < this.bitString.length && this.bitString[i];
    }

    private void set(int i, boolean z) {
        if (i >= this.bitString.length) {
            boolean[] zArr = new boolean[i + 1];
            System.arraycopy((Object) this.bitString, 0, (Object) zArr, 0, this.bitString.length);
            this.bitString = zArr;
        }
        this.bitString[i] = z;
    }

    public KeyUsageExtension(byte[] bArr) throws IOException {
        this.bitString = new BitArray(bArr.length * 8, bArr).toBooleanArray();
        this.extensionId = PKIXExtensions.KeyUsage_Id;
        this.critical = true;
        encodeThis();
    }

    public KeyUsageExtension(boolean[] zArr) throws IOException {
        this.bitString = zArr;
        this.extensionId = PKIXExtensions.KeyUsage_Id;
        this.critical = true;
        encodeThis();
    }

    public KeyUsageExtension(BitArray bitArray) throws IOException {
        this.bitString = bitArray.toBooleanArray();
        this.extensionId = PKIXExtensions.KeyUsage_Id;
        this.critical = true;
        encodeThis();
    }

    public KeyUsageExtension(Boolean bool, Object obj) throws IOException {
        this.extensionId = PKIXExtensions.KeyUsage_Id;
        this.critical = bool.booleanValue();
        byte[] bArr = (byte[]) obj;
        if (bArr[0] == 4) {
            this.extensionValue = new DerValue(bArr).getOctetString();
        } else {
            this.extensionValue = bArr;
        }
        this.bitString = new DerValue(this.extensionValue).getUnalignedBitString().toBooleanArray();
    }

    public KeyUsageExtension() {
        this.extensionId = PKIXExtensions.KeyUsage_Id;
        this.critical = true;
        this.bitString = new boolean[0];
    }

    @Override
    public void set(String str, Object obj) throws IOException {
        if (!(obj instanceof Boolean)) {
            throw new IOException("Attribute must be of type Boolean.");
        }
        boolean zBooleanValue = ((Boolean) obj).booleanValue();
        if (str.equalsIgnoreCase(DIGITAL_SIGNATURE)) {
            set(0, zBooleanValue);
        } else if (str.equalsIgnoreCase(NON_REPUDIATION)) {
            set(1, zBooleanValue);
        } else if (str.equalsIgnoreCase(KEY_ENCIPHERMENT)) {
            set(2, zBooleanValue);
        } else if (str.equalsIgnoreCase(DATA_ENCIPHERMENT)) {
            set(3, zBooleanValue);
        } else if (str.equalsIgnoreCase(KEY_AGREEMENT)) {
            set(4, zBooleanValue);
        } else if (str.equalsIgnoreCase(KEY_CERTSIGN)) {
            set(5, zBooleanValue);
        } else if (str.equalsIgnoreCase(CRL_SIGN)) {
            set(6, zBooleanValue);
        } else if (str.equalsIgnoreCase(ENCIPHER_ONLY)) {
            set(7, zBooleanValue);
        } else if (str.equalsIgnoreCase(DECIPHER_ONLY)) {
            set(8, zBooleanValue);
        } else {
            throw new IOException("Attribute name not recognized by CertAttrSet:KeyUsage.");
        }
        encodeThis();
    }

    @Override
    public Boolean get(String str) throws IOException {
        if (str.equalsIgnoreCase(DIGITAL_SIGNATURE)) {
            return Boolean.valueOf(isSet(0));
        }
        if (str.equalsIgnoreCase(NON_REPUDIATION)) {
            return Boolean.valueOf(isSet(1));
        }
        if (str.equalsIgnoreCase(KEY_ENCIPHERMENT)) {
            return Boolean.valueOf(isSet(2));
        }
        if (str.equalsIgnoreCase(DATA_ENCIPHERMENT)) {
            return Boolean.valueOf(isSet(3));
        }
        if (str.equalsIgnoreCase(KEY_AGREEMENT)) {
            return Boolean.valueOf(isSet(4));
        }
        if (str.equalsIgnoreCase(KEY_CERTSIGN)) {
            return Boolean.valueOf(isSet(5));
        }
        if (str.equalsIgnoreCase(CRL_SIGN)) {
            return Boolean.valueOf(isSet(6));
        }
        if (str.equalsIgnoreCase(ENCIPHER_ONLY)) {
            return Boolean.valueOf(isSet(7));
        }
        if (str.equalsIgnoreCase(DECIPHER_ONLY)) {
            return Boolean.valueOf(isSet(8));
        }
        throw new IOException("Attribute name not recognized by CertAttrSet:KeyUsage.");
    }

    @Override
    public void delete(String str) throws IOException {
        if (str.equalsIgnoreCase(DIGITAL_SIGNATURE)) {
            set(0, false);
        } else if (str.equalsIgnoreCase(NON_REPUDIATION)) {
            set(1, false);
        } else if (str.equalsIgnoreCase(KEY_ENCIPHERMENT)) {
            set(2, false);
        } else if (str.equalsIgnoreCase(DATA_ENCIPHERMENT)) {
            set(3, false);
        } else if (str.equalsIgnoreCase(KEY_AGREEMENT)) {
            set(4, false);
        } else if (str.equalsIgnoreCase(KEY_CERTSIGN)) {
            set(5, false);
        } else if (str.equalsIgnoreCase(CRL_SIGN)) {
            set(6, false);
        } else if (str.equalsIgnoreCase(ENCIPHER_ONLY)) {
            set(7, false);
        } else if (str.equalsIgnoreCase(DECIPHER_ONLY)) {
            set(8, false);
        } else {
            throw new IOException("Attribute name not recognized by CertAttrSet:KeyUsage.");
        }
        encodeThis();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString());
        sb.append("KeyUsage [\n");
        if (isSet(0)) {
            sb.append("  DigitalSignature\n");
        }
        if (isSet(1)) {
            sb.append("  Non_repudiation\n");
        }
        if (isSet(2)) {
            sb.append("  Key_Encipherment\n");
        }
        if (isSet(3)) {
            sb.append("  Data_Encipherment\n");
        }
        if (isSet(4)) {
            sb.append("  Key_Agreement\n");
        }
        if (isSet(5)) {
            sb.append("  Key_CertSign\n");
        }
        if (isSet(6)) {
            sb.append("  Crl_Sign\n");
        }
        if (isSet(7)) {
            sb.append("  Encipher_Only\n");
        }
        if (isSet(8)) {
            sb.append("  Decipher_Only\n");
        }
        sb.append("]\n");
        return sb.toString();
    }

    @Override
    public void encode(OutputStream outputStream) throws IOException {
        DerOutputStream derOutputStream = new DerOutputStream();
        if (this.extensionValue == null) {
            this.extensionId = PKIXExtensions.KeyUsage_Id;
            this.critical = true;
            encodeThis();
        }
        super.encode(derOutputStream);
        outputStream.write(derOutputStream.toByteArray());
    }

    @Override
    public Enumeration<String> getElements() {
        AttributeNameEnumeration attributeNameEnumeration = new AttributeNameEnumeration();
        attributeNameEnumeration.addElement(DIGITAL_SIGNATURE);
        attributeNameEnumeration.addElement(NON_REPUDIATION);
        attributeNameEnumeration.addElement(KEY_ENCIPHERMENT);
        attributeNameEnumeration.addElement(DATA_ENCIPHERMENT);
        attributeNameEnumeration.addElement(KEY_AGREEMENT);
        attributeNameEnumeration.addElement(KEY_CERTSIGN);
        attributeNameEnumeration.addElement(CRL_SIGN);
        attributeNameEnumeration.addElement(ENCIPHER_ONLY);
        attributeNameEnumeration.addElement(DECIPHER_ONLY);
        return attributeNameEnumeration.elements();
    }

    public boolean[] getBits() {
        return (boolean[]) this.bitString.clone();
    }

    @Override
    public String getName() {
        return NAME;
    }
}
