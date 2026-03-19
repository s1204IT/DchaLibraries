package sun.security.x509;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.security.AccessController;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import sun.security.action.GetBooleanAction;
import sun.security.pkcs.PKCS9Attribute;
import sun.security.util.Debug;
import sun.security.util.DerEncoder;
import sun.security.util.DerInputStream;
import sun.security.util.DerOutputStream;
import sun.security.util.DerValue;
import sun.security.util.ObjectIdentifier;

public class AVA implements DerEncoder {
    static final int DEFAULT = 1;
    static final int RFC1779 = 2;
    static final int RFC2253 = 3;
    private static final String escapedDefault = ",+<>;\"";
    private static final String hexDigits = "0123456789ABCDEF";
    private static final String specialChars1779 = ",=\n+<>#;\\\"";
    private static final String specialChars2253 = ",=+<>#;\\\"";
    private static final String specialCharsDefault = ",=\n+<>#;\\\" ";
    final ObjectIdentifier oid;
    final DerValue value;
    private static final Debug debug = Debug.getInstance(X509CertImpl.NAME, "\t[AVA]");
    private static final boolean PRESERVE_OLD_DC_ENCODING = ((Boolean) AccessController.doPrivileged(new GetBooleanAction("com.sun.security.preserveOldDCEncoding"))).booleanValue();

    public AVA(ObjectIdentifier objectIdentifier, DerValue derValue) {
        if (objectIdentifier == null || derValue == null) {
            throw new NullPointerException();
        }
        this.oid = objectIdentifier;
        this.value = derValue;
    }

    AVA(Reader reader) throws IOException {
        this(reader, 1);
    }

    AVA(Reader reader, Map<String, String> map) throws IOException {
        this(reader, 1, map);
    }

    AVA(Reader reader, int i) throws IOException {
        this(reader, i, Collections.emptyMap());
    }

    AVA(Reader reader, int i, Map<String, String> map) throws IOException {
        int i2;
        StringBuilder sb = new StringBuilder();
        while (true) {
            int i3 = readChar(reader, "Incorrect AVA format");
            if (i3 == 61) {
                break;
            } else {
                sb.append((char) i3);
            }
        }
        this.oid = AVAKeyword.getOID(sb.toString(), i, map);
        sb.setLength(0);
        if (i != 3) {
            while (true) {
                i2 = reader.read();
                if (i2 != 32 && i2 != 10) {
                    break;
                }
            }
        } else {
            i2 = reader.read();
            if (i2 == 32) {
                throw new IOException("Incorrect AVA RFC2253 format - leading space must be escaped");
            }
        }
        if (i2 == -1) {
            this.value = new DerValue("");
            return;
        }
        if (i2 == 35) {
            this.value = parseHexString(reader, i);
        } else if (i2 == 34 && i != 3) {
            this.value = parseQuotedString(reader, sb);
        } else {
            this.value = parseString(reader, i2, i, sb);
        }
    }

    public ObjectIdentifier getObjectIdentifier() {
        return this.oid;
    }

    public DerValue getDerValue() {
        return this.value;
    }

    public String getValueString() {
        try {
            String asString = this.value.getAsString();
            if (asString == null) {
                throw new RuntimeException("AVA string is null");
            }
            return asString;
        } catch (IOException e) {
            throw new RuntimeException("AVA error: " + ((Object) e), e);
        }
    }

    private static DerValue parseHexString(Reader reader, int i) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        int i2 = 0;
        byte b = 0;
        while (true) {
            int i3 = reader.read();
            if (isTerminator(i3, i)) {
                break;
            }
            if (i3 == 32 || i3 == 10) {
                break;
            }
            char c = (char) i3;
            int iIndexOf = hexDigits.indexOf(Character.toUpperCase(c));
            if (iIndexOf == -1) {
                throw new IOException("AVA parse, invalid hex digit: " + c);
            }
            if (i2 % 2 == 1) {
                b = (byte) ((b * 16) + ((byte) iIndexOf));
                byteArrayOutputStream.write(b);
            } else {
                b = (byte) iIndexOf;
            }
            i2++;
        }
    }

    private DerValue parseQuotedString(Reader reader, StringBuilder sb) throws IOException {
        int i;
        int i2 = readChar(reader, "Quoted string did not end in quote");
        ArrayList arrayList = new ArrayList();
        boolean zIsPrintableStringChar = true;
        while (i2 != 34) {
            if (i2 == 92) {
                i2 = readChar(reader, "Quoted string did not end in quote");
                Byte embeddedHexPair = getEmbeddedHexPair(i2, reader);
                if (embeddedHexPair != null) {
                    zIsPrintableStringChar = false;
                    arrayList.add(embeddedHexPair);
                    i2 = reader.read();
                } else {
                    char c = (char) i2;
                    if (specialChars1779.indexOf(c) < 0) {
                        throw new IOException("Invalid escaped character in AVA: " + c);
                    }
                }
            }
            if (arrayList.size() > 0) {
                sb.append(getEmbeddedHexString(arrayList));
                arrayList.clear();
            }
            char c2 = (char) i2;
            zIsPrintableStringChar &= DerValue.isPrintableStringChar(c2);
            sb.append(c2);
            i2 = readChar(reader, "Quoted string did not end in quote");
        }
        if (arrayList.size() > 0) {
            sb.append(getEmbeddedHexString(arrayList));
            arrayList.clear();
        }
        while (true) {
            i = reader.read();
            if (i != 10 && i != 32) {
                break;
            }
        }
        if (i != -1) {
            throw new IOException("AVA had characters other than whitespace after terminating quote");
        }
        if (this.oid.equals((Object) PKCS9Attribute.EMAIL_ADDRESS_OID) || (this.oid.equals((Object) X500Name.DOMAIN_COMPONENT_OID) && !PRESERVE_OLD_DC_ENCODING)) {
            return new DerValue((byte) 22, sb.toString());
        }
        if (zIsPrintableStringChar) {
            return new DerValue(sb.toString());
        }
        return new DerValue((byte) 12, sb.toString());
    }

    private DerValue parseString(Reader reader, int i, int i2, StringBuilder sb) throws IOException {
        int i3;
        boolean z;
        int i4;
        ArrayList arrayList = new ArrayList();
        boolean z2 = true;
        boolean zIsPrintableStringChar = true;
        int i5 = 0;
        while (true) {
            if (i == 92) {
                int i6 = readChar(reader, "Invalid trailing backslash");
                Byte embeddedHexPair = getEmbeddedHexPair(i6, reader);
                if (embeddedHexPair != null) {
                    arrayList.add(embeddedHexPair);
                    i = reader.read();
                    zIsPrintableStringChar = false;
                    if (isTerminator(i, i2)) {
                        z2 = false;
                    } else {
                        if (i2 == 3 && i5 > 0) {
                            throw new IOException("Incorrect AVA RFC2253 format - trailing space must be escaped");
                        }
                        if (arrayList.size() > 0) {
                            sb.append(getEmbeddedHexString(arrayList));
                            arrayList.clear();
                        }
                        if (this.oid.equals((Object) PKCS9Attribute.EMAIL_ADDRESS_OID) || (this.oid.equals((Object) X500Name.DOMAIN_COMPONENT_OID) && !PRESERVE_OLD_DC_ENCODING)) {
                            return new DerValue((byte) 22, sb.toString());
                        }
                        if (zIsPrintableStringChar) {
                            return new DerValue(sb.toString());
                        }
                        return new DerValue((byte) 12, sb.toString());
                    }
                } else {
                    if (i2 == 1) {
                        char c = (char) i6;
                        if (specialCharsDefault.indexOf(c) == -1) {
                            throw new IOException("Invalid escaped character in AVA: '" + c + "'");
                        }
                    }
                    if (i2 == 3) {
                        if (i6 == 32) {
                            if (!z2 && !trailingSpace(reader)) {
                                throw new IOException("Invalid escaped space character in AVA.  Only a leading or trailing space character can be escaped.");
                            }
                        } else if (i6 == 35) {
                            if (!z2) {
                                throw new IOException("Invalid escaped '#' character in AVA.  Only a leading '#' can be escaped.");
                            }
                        } else {
                            char c2 = (char) i6;
                            if (specialChars2253.indexOf(c2) == -1) {
                                throw new IOException("Invalid escaped character in AVA: '" + c2 + "'");
                            }
                        }
                    }
                    i3 = i6;
                    z = true;
                }
            } else {
                if (i2 == 3) {
                    char c3 = (char) i;
                    if (specialChars2253.indexOf(c3) != -1) {
                        throw new IOException("Character '" + c3 + "' in AVA appears without escape");
                    }
                }
                i3 = i;
                z = false;
            }
            if (arrayList.size() > 0) {
                for (int i7 = 0; i7 < i5; i7++) {
                    sb.append(" ");
                }
                sb.append(getEmbeddedHexString(arrayList));
                arrayList.clear();
                i5 = 0;
            }
            char c4 = (char) i3;
            zIsPrintableStringChar &= DerValue.isPrintableStringChar(c4);
            if (i3 == 32 && !z) {
                i4 = i5 + 1;
            } else {
                for (int i8 = 0; i8 < i5; i8++) {
                    sb.append(" ");
                }
                sb.append(c4);
                i4 = 0;
            }
            i5 = i4;
            i = reader.read();
            if (isTerminator(i, i2)) {
            }
        }
    }

    private static Byte getEmbeddedHexPair(int i, Reader reader) throws IOException {
        char c = (char) i;
        if (hexDigits.indexOf(Character.toUpperCase(c)) >= 0) {
            char c2 = (char) readChar(reader, "unexpected EOF - escaped hex value must include two valid digits");
            if (hexDigits.indexOf(Character.toUpperCase(c2)) >= 0) {
                return new Byte((byte) ((Character.digit(c, 16) << 4) + Character.digit(c2, 16)));
            }
            throw new IOException("escaped hex value must include two valid digits");
        }
        return null;
    }

    private static String getEmbeddedHexString(List<Byte> list) throws IOException {
        int size = list.size();
        byte[] bArr = new byte[size];
        for (int i = 0; i < size; i++) {
            bArr[i] = list.get(i).byteValue();
        }
        return new String(bArr, "UTF8");
    }

    private static boolean isTerminator(int i, int i2) {
        if (i != -1) {
            if (i == 59) {
                return i2 != 3;
            }
            switch (i) {
            }
            return false;
        }
        return true;
    }

    private static int readChar(Reader reader, String str) throws IOException {
        int i = reader.read();
        if (i == -1) {
            throw new IOException(str);
        }
        return i;
    }

    private static boolean trailingSpace(Reader reader) throws IOException {
        boolean z;
        if (!reader.markSupported()) {
            return true;
        }
        reader.mark(9999);
        while (true) {
            int i = reader.read();
            z = false;
            if (i != -1) {
                if (i != 32 && (i != 92 || reader.read() != 32)) {
                    break;
                }
            } else {
                z = true;
                break;
            }
        }
        reader.reset();
        return z;
    }

    AVA(DerValue derValue) throws IOException {
        if (derValue.tag != 48) {
            throw new IOException("AVA not a sequence");
        }
        this.oid = X500Name.intern(derValue.data.getOID());
        this.value = derValue.data.getDerValue();
        if (derValue.data.available() != 0) {
            throw new IOException("AVA, extra bytes = " + derValue.data.available());
        }
    }

    AVA(DerInputStream derInputStream) throws IOException {
        this(derInputStream.getDerValue());
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof AVA)) {
            return false;
        }
        return toRFC2253CanonicalString().equals(((AVA) obj).toRFC2253CanonicalString());
    }

    public int hashCode() {
        return toRFC2253CanonicalString().hashCode();
    }

    public void encode(DerOutputStream derOutputStream) throws IOException {
        derEncode(derOutputStream);
    }

    @Override
    public void derEncode(OutputStream outputStream) throws IOException {
        DerOutputStream derOutputStream = new DerOutputStream();
        DerOutputStream derOutputStream2 = new DerOutputStream();
        derOutputStream.putOID(this.oid);
        this.value.encode(derOutputStream);
        derOutputStream2.write((byte) 48, derOutputStream);
        outputStream.write(derOutputStream2.toByteArray());
    }

    private String toKeyword(int i, Map<String, String> map) {
        return AVAKeyword.getKeyword(this.oid, i, map);
    }

    public String toString() {
        return toKeywordValueString(toKeyword(1, Collections.emptyMap()));
    }

    public String toRFC1779String() {
        return toRFC1779String(Collections.emptyMap());
    }

    public String toRFC1779String(Map<String, String> map) {
        return toKeywordValueString(toKeyword(2, map));
    }

    public String toRFC2253String() {
        return toRFC2253String(Collections.emptyMap());
    }

    public String toRFC2253String(Map<String, String> map) {
        StringBuilder sb = new StringBuilder(100);
        sb.append(toKeyword(3, map));
        sb.append('=');
        int i = 0;
        if ((sb.charAt(0) >= '0' && sb.charAt(0) <= '9') || !isDerString(this.value, false)) {
            try {
                byte[] byteArray = this.value.toByteArray();
                sb.append('#');
                while (i < byteArray.length) {
                    byte b = byteArray[i];
                    sb.append(Character.forDigit((b >>> 4) & 15, 16));
                    sb.append(Character.forDigit(b & 15, 16));
                    i++;
                }
            } catch (IOException e) {
                throw new IllegalArgumentException("DER Value conversion");
            }
        } else {
            try {
                String str = new String(this.value.getDataBytes(), "UTF8");
                StringBuilder sb2 = new StringBuilder();
                for (int i2 = 0; i2 < str.length(); i2++) {
                    char cCharAt = str.charAt(i2);
                    if (DerValue.isPrintableStringChar(cCharAt) || ",=+<>#;\"\\".indexOf(cCharAt) >= 0) {
                        if (",=+<>#;\"\\".indexOf(cCharAt) >= 0) {
                            sb2.append('\\');
                        }
                        sb2.append(cCharAt);
                    } else if (cCharAt == 0) {
                        sb2.append("\\00");
                    } else if (debug != null && Debug.isOn("ava")) {
                        try {
                            byte[] bytes = Character.toString(cCharAt).getBytes("UTF8");
                            for (int i3 = 0; i3 < bytes.length; i3++) {
                                sb2.append('\\');
                                sb2.append(Character.toUpperCase(Character.forDigit((bytes[i3] >>> 4) & 15, 16)));
                                sb2.append(Character.toUpperCase(Character.forDigit(bytes[i3] & 15, 16)));
                            }
                        } catch (IOException e2) {
                            throw new IllegalArgumentException("DER Value conversion");
                        }
                    } else {
                        sb2.append(cCharAt);
                    }
                }
                char[] charArray = sb2.toString().toCharArray();
                StringBuilder sb3 = new StringBuilder();
                int i4 = 0;
                while (i4 < charArray.length && (charArray[i4] == ' ' || charArray[i4] == '\r')) {
                    i4++;
                }
                int length = charArray.length - 1;
                while (length >= 0 && (charArray[length] == ' ' || charArray[length] == '\r')) {
                    length--;
                }
                while (i < charArray.length) {
                    char c = charArray[i];
                    if (i < i4 || i > length) {
                        sb3.append('\\');
                    }
                    sb3.append(c);
                    i++;
                }
                sb.append(sb3.toString());
            } catch (IOException e3) {
                throw new IllegalArgumentException("DER Value conversion");
            }
        }
        return sb.toString();
    }

    public String toRFC2253CanonicalString() {
        StringBuilder sb = new StringBuilder(40);
        sb.append(toKeyword(3, Collections.emptyMap()));
        sb.append('=');
        if ((sb.charAt(0) >= '0' && sb.charAt(0) <= '9') || (!isDerString(this.value, true) && this.value.tag != 20)) {
            try {
                byte[] byteArray = this.value.toByteArray();
                sb.append('#');
                for (byte b : byteArray) {
                    sb.append(Character.forDigit((b >>> 4) & 15, 16));
                    sb.append(Character.forDigit(b & 15, 16));
                }
            } catch (IOException e) {
                throw new IllegalArgumentException("DER Value conversion");
            }
        } else {
            try {
                String str = new String(this.value.getDataBytes(), "UTF8");
                StringBuilder sb2 = new StringBuilder();
                boolean z = false;
                for (int i = 0; i < str.length(); i++) {
                    char cCharAt = str.charAt(i);
                    if (DerValue.isPrintableStringChar(cCharAt) || ",+<>;\"\\".indexOf(cCharAt) >= 0 || (i == 0 && cCharAt == '#')) {
                        if ((i == 0 && cCharAt == '#') || ",+<>;\"\\".indexOf(cCharAt) >= 0) {
                            sb2.append('\\');
                        }
                        if (!Character.isWhitespace(cCharAt)) {
                            sb2.append(cCharAt);
                        } else {
                            if (!z) {
                                sb2.append(cCharAt);
                                z = true;
                            }
                        }
                    } else if (debug != null && Debug.isOn("ava")) {
                        try {
                            byte[] bytes = Character.toString(cCharAt).getBytes("UTF8");
                            for (int i2 = 0; i2 < bytes.length; i2++) {
                                sb2.append('\\');
                                sb2.append(Character.forDigit((bytes[i2] >>> 4) & 15, 16));
                                sb2.append(Character.forDigit(bytes[i2] & 15, 16));
                            }
                        } catch (IOException e2) {
                            throw new IllegalArgumentException("DER Value conversion");
                        }
                    } else {
                        sb2.append(cCharAt);
                    }
                    z = false;
                }
                sb.append(sb2.toString().trim());
            } catch (IOException e3) {
                throw new IllegalArgumentException("DER Value conversion");
            }
        }
        return Normalizer.normalize(sb.toString().toUpperCase(Locale.US).toLowerCase(Locale.US), Normalizer.Form.NFKD);
    }

    private static boolean isDerString(DerValue derValue, boolean z) {
        if (z) {
            byte b = derValue.tag;
            return b == 12 || b == 19;
        }
        byte b2 = derValue.tag;
        if (b2 != 12 && b2 != 22 && b2 != 27 && b2 != 30) {
            switch (b2) {
                case 19:
                case 20:
                    break;
                default:
                    return false;
            }
        }
        return true;
    }

    boolean hasRFC2253Keyword() {
        return AVAKeyword.hasKeyword(this.oid, 3);
    }

    private String toKeywordValueString(String str) {
        char cCharAt;
        boolean z;
        StringBuilder sb = new StringBuilder(40);
        sb.append(str);
        sb.append("=");
        try {
            String asString = this.value.getAsString();
            if (asString == null) {
                byte[] byteArray = this.value.toByteArray();
                sb.append('#');
                for (int i = 0; i < byteArray.length; i++) {
                    sb.append(hexDigits.charAt((byteArray[i] >> 4) & 15));
                    sb.append(hexDigits.charAt(byteArray[i] & 15));
                }
            } else {
                StringBuilder sb2 = new StringBuilder();
                int length = asString.length();
                boolean z2 = true;
                boolean z3 = length > 1 && asString.charAt(0) == '\"' && asString.charAt(length + (-1)) == '\"';
                boolean z4 = false;
                boolean z5 = false;
                for (int i2 = 0; i2 < length; i2++) {
                    char cCharAt2 = asString.charAt(i2);
                    if (z3 && (i2 == 0 || i2 == length - 1)) {
                        sb2.append(cCharAt2);
                    } else if (DerValue.isPrintableStringChar(cCharAt2) || ",+=\n<>#;\\\"".indexOf(cCharAt2) >= 0) {
                        if (!z4 && ((i2 == 0 && (cCharAt2 == ' ' || cCharAt2 == '\n')) || ",+=\n<>#;\\\"".indexOf(cCharAt2) >= 0)) {
                            z4 = true;
                        }
                        if (cCharAt2 == ' ' || cCharAt2 == '\n') {
                            if (!z4 && z5) {
                                z4 = true;
                            }
                            z = true;
                        } else {
                            if (cCharAt2 == '\"' || cCharAt2 == '\\') {
                                sb2.append('\\');
                            }
                            z = false;
                        }
                        sb2.append(cCharAt2);
                        z5 = z;
                    } else {
                        if (debug == null || !Debug.isOn("ava")) {
                            sb2.append(cCharAt2);
                        } else {
                            byte[] bytes = Character.toString(cCharAt2).getBytes("UTF8");
                            for (int i3 = 0; i3 < bytes.length; i3++) {
                                sb2.append('\\');
                                sb2.append(Character.toUpperCase(Character.forDigit((bytes[i3] >>> 4) & 15, 16)));
                                sb2.append(Character.toUpperCase(Character.forDigit(bytes[i3] & 15, 16)));
                            }
                        }
                        z5 = false;
                    }
                }
                if (sb2.length() <= 0 || ((cCharAt = sb2.charAt(sb2.length() - 1)) != ' ' && cCharAt != '\n')) {
                    z2 = z4;
                }
                if (z3 || !z2) {
                    sb.append(sb2.toString());
                } else {
                    sb.append("\"" + sb2.toString() + "\"");
                }
            }
            return sb.toString();
        } catch (IOException e) {
            throw new IllegalArgumentException("DER Value conversion");
        }
    }
}
