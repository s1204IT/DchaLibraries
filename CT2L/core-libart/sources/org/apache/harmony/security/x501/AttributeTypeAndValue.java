package org.apache.harmony.security.x501;

import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import javax.security.auth.x500.X500Principal;
import javax.xml.datatype.DatatypeConstants;
import org.apache.harmony.security.asn1.ASN1Oid;
import org.apache.harmony.security.asn1.ASN1Sequence;
import org.apache.harmony.security.asn1.ASN1StringType;
import org.apache.harmony.security.asn1.ASN1Type;
import org.apache.harmony.security.asn1.BerInputStream;
import org.apache.harmony.security.asn1.BerOutputStream;
import org.apache.harmony.security.utils.ObjectIdentifier;

public final class AttributeTypeAndValue {
    public static final ASN1Sequence ASN1;
    private static final int CAPACITY = 10;
    private static final int SIZE = 10;
    public static final ASN1Type attributeValue;
    private final ObjectIdentifier oid;
    private final AttributeValue value;
    private static final HashMap<String, ObjectIdentifier> RFC1779_NAMES = new HashMap<>(10);
    private static final HashMap<String, ObjectIdentifier> KNOWN_NAMES = new HashMap<>(30);
    private static final HashMap<String, ObjectIdentifier> RFC2253_NAMES = new HashMap<>(10);
    private static final HashMap<String, ObjectIdentifier> RFC2459_NAMES = new HashMap<>(10);
    private static final ObjectIdentifier C = new ObjectIdentifier(new int[]{2, 5, 4, 6}, "C", RFC1779_NAMES);
    private static final ObjectIdentifier CN = new ObjectIdentifier(new int[]{2, 5, 4, 3}, "CN", RFC1779_NAMES);
    public static final ObjectIdentifier DC = new ObjectIdentifier(new int[]{0, 9, 2342, 19200300, 100, 1, 25}, "DC", RFC2253_NAMES);
    private static final ObjectIdentifier DNQ = new ObjectIdentifier(new int[]{2, 5, 4, 46}, "DNQ", RFC2459_NAMES);
    private static final ObjectIdentifier DNQUALIFIER = new ObjectIdentifier(new int[]{2, 5, 4, 46}, "DNQUALIFIER", RFC2459_NAMES);
    public static final ObjectIdentifier EMAILADDRESS = new ObjectIdentifier(new int[]{1, 2, DatatypeConstants.MIN_TIMEZONE_OFFSET, 113549, 1, 9, 1}, "EMAILADDRESS", RFC2459_NAMES);
    private static final ObjectIdentifier GENERATION = new ObjectIdentifier(new int[]{2, 5, 4, 44}, "GENERATION", RFC2459_NAMES);
    private static final ObjectIdentifier GIVENNAME = new ObjectIdentifier(new int[]{2, 5, 4, 42}, "GIVENNAME", RFC2459_NAMES);
    private static final ObjectIdentifier INITIALS = new ObjectIdentifier(new int[]{2, 5, 4, 43}, "INITIALS", RFC2459_NAMES);
    private static final ObjectIdentifier L = new ObjectIdentifier(new int[]{2, 5, 4, 7}, "L", RFC1779_NAMES);
    private static final ObjectIdentifier O = new ObjectIdentifier(new int[]{2, 5, 4, 10}, "O", RFC1779_NAMES);
    private static final ObjectIdentifier OU = new ObjectIdentifier(new int[]{2, 5, 4, 11}, "OU", RFC1779_NAMES);
    private static final ObjectIdentifier SERIALNUMBER = new ObjectIdentifier(new int[]{2, 5, 4, 5}, "SERIALNUMBER", RFC2459_NAMES);
    private static final ObjectIdentifier ST = new ObjectIdentifier(new int[]{2, 5, 4, 8}, "ST", RFC1779_NAMES);
    private static final ObjectIdentifier STREET = new ObjectIdentifier(new int[]{2, 5, 4, 9}, "STREET", RFC1779_NAMES);
    private static final ObjectIdentifier SURNAME = new ObjectIdentifier(new int[]{2, 5, 4, 4}, "SURNAME", RFC2459_NAMES);
    private static final ObjectIdentifier T = new ObjectIdentifier(new int[]{2, 5, 4, 12}, "T", RFC2459_NAMES);
    private static final ObjectIdentifier UID = new ObjectIdentifier(new int[]{0, 9, 2342, 19200300, 100, 1, 1}, "UID", RFC2253_NAMES);
    private static final ObjectIdentifier[][] KNOWN_OIDS = (ObjectIdentifier[][]) Array.newInstance((Class<?>) ObjectIdentifier.class, 10, 10);

    static {
        RFC1779_NAMES.put(CN.getName(), CN);
        RFC1779_NAMES.put(L.getName(), L);
        RFC1779_NAMES.put(ST.getName(), ST);
        RFC1779_NAMES.put(O.getName(), O);
        RFC1779_NAMES.put(OU.getName(), OU);
        RFC1779_NAMES.put(C.getName(), C);
        RFC1779_NAMES.put(STREET.getName(), STREET);
        RFC2253_NAMES.putAll(RFC1779_NAMES);
        RFC2253_NAMES.put(DC.getName(), DC);
        RFC2253_NAMES.put(UID.getName(), UID);
        RFC2459_NAMES.put(DNQ.getName(), DNQ);
        RFC2459_NAMES.put(DNQUALIFIER.getName(), DNQUALIFIER);
        RFC2459_NAMES.put(EMAILADDRESS.getName(), EMAILADDRESS);
        RFC2459_NAMES.put(GENERATION.getName(), GENERATION);
        RFC2459_NAMES.put(GIVENNAME.getName(), GIVENNAME);
        RFC2459_NAMES.put(INITIALS.getName(), INITIALS);
        RFC2459_NAMES.put(SERIALNUMBER.getName(), SERIALNUMBER);
        RFC2459_NAMES.put(SURNAME.getName(), SURNAME);
        RFC2459_NAMES.put(T.getName(), T);
        for (ObjectIdentifier objectIdentifier : RFC2253_NAMES.values()) {
            addOID(objectIdentifier);
        }
        for (ObjectIdentifier o : RFC2459_NAMES.values()) {
            if (o != DNQUALIFIER) {
                addOID(o);
            }
        }
        KNOWN_NAMES.putAll(RFC2253_NAMES);
        KNOWN_NAMES.putAll(RFC2459_NAMES);
        attributeValue = new ASN1Type(19) {
            @Override
            public boolean checkTag(int tag) {
                return true;
            }

            @Override
            public Object decode(BerInputStream in) throws IOException {
                String str = null;
                if (DirectoryString.ASN1.checkTag(in.tag)) {
                    str = (String) DirectoryString.ASN1.decode(in);
                } else {
                    in.readContent();
                }
                byte[] bytesEncoded = new byte[in.getOffset() - in.getTagOffset()];
                System.arraycopy(in.getBuffer(), in.getTagOffset(), bytesEncoded, 0, bytesEncoded.length);
                return new AttributeValue(str, bytesEncoded, in.tag);
            }

            @Override
            public Object getDecodedObject(BerInputStream in) throws IOException {
                throw new RuntimeException("AttributeValue getDecodedObject MUST NOT be invoked");
            }

            @Override
            public void encodeASN(BerOutputStream out) {
                AttributeValue av = (AttributeValue) out.content;
                if (av.encoded != null) {
                    out.content = av.encoded;
                    out.encodeANY();
                } else {
                    out.encodeTag(av.getTag());
                    out.content = av.bytes;
                    out.encodeString();
                }
            }

            @Override
            public void setEncodingContent(BerOutputStream out) {
                AttributeValue av = (AttributeValue) out.content;
                if (av.encoded != null) {
                    out.length = av.encoded.length;
                    return;
                }
                if (av.getTag() == 12) {
                    out.content = av.rawString;
                    ASN1StringType.UTF8STRING.setEncodingContent(out);
                    av.bytes = (byte[]) out.content;
                    out.content = av;
                    return;
                }
                av.bytes = av.rawString.getBytes(StandardCharsets.UTF_8);
                out.length = av.bytes.length;
            }

            @Override
            public void encodeContent(BerOutputStream out) {
                throw new RuntimeException("AttributeValue encodeContent MUST NOT be invoked");
            }

            @Override
            public int getEncodedLength(BerOutputStream out) {
                AttributeValue av = (AttributeValue) out.content;
                return av.encoded != null ? out.length : super.getEncodedLength(out);
            }
        };
        ASN1 = new ASN1Sequence(new ASN1Type[]{ASN1Oid.getInstance(), attributeValue}) {
            @Override
            protected Object getDecodedObject(BerInputStream in) throws IOException {
                Object[] values = (Object[]) in.content;
                return new AttributeTypeAndValue((int[]) values[0], (AttributeValue) values[1]);
            }

            @Override
            protected void getValues(Object object, Object[] values) {
                AttributeTypeAndValue atav = (AttributeTypeAndValue) object;
                values[0] = atav.oid.getOid();
                values[1] = atav.value;
            }
        };
    }

    public static ObjectIdentifier getObjectIdentifier(String sOid) throws IOException {
        if (sOid.charAt(0) >= '0' && sOid.charAt(0) <= '9') {
            int[] array = org.apache.harmony.security.asn1.ObjectIdentifier.toIntArray(sOid);
            ObjectIdentifier thisOid = getOID(array);
            if (thisOid == null) {
                return new ObjectIdentifier(array);
            }
            return thisOid;
        }
        ObjectIdentifier thisOid2 = KNOWN_NAMES.get(sOid.toUpperCase(Locale.US));
        if (thisOid2 == null) {
            throw new IOException("Unrecognizable attribute name: " + sOid);
        }
        return thisOid2;
    }

    private AttributeTypeAndValue(int[] oid, AttributeValue value) throws IOException {
        ObjectIdentifier thisOid = getOID(oid);
        this.oid = thisOid == null ? new ObjectIdentifier(oid) : thisOid;
        this.value = value;
    }

    public AttributeTypeAndValue(ObjectIdentifier oid, AttributeValue value) throws IOException {
        this.oid = oid;
        this.value = value;
    }

    public void appendName(String attrFormat, StringBuilder sb) {
        boolean hexFormat = false;
        if (X500Principal.RFC1779.equals(attrFormat)) {
            if (RFC1779_NAMES == this.oid.getGroup()) {
                sb.append(this.oid.getName());
            } else {
                sb.append(this.oid.toOIDString());
            }
            sb.append('=');
            if (this.value.escapedString == this.value.getHexString()) {
                sb.append(this.value.getHexString().toUpperCase(Locale.US));
                return;
            } else if (this.value.escapedString.length() != this.value.rawString.length()) {
                this.value.appendQEString(sb);
                return;
            } else {
                sb.append(this.value.escapedString);
                return;
            }
        }
        Object group = this.oid.getGroup();
        if (RFC1779_NAMES == group || RFC2253_NAMES == group) {
            sb.append(this.oid.getName());
            if (X500Principal.CANONICAL.equals(attrFormat)) {
                int tag = this.value.getTag();
                if (!ASN1StringType.UTF8STRING.checkTag(tag) && !ASN1StringType.PRINTABLESTRING.checkTag(tag) && !ASN1StringType.TELETEXSTRING.checkTag(tag)) {
                    hexFormat = true;
                }
            }
        } else {
            sb.append(this.oid.toString());
            hexFormat = true;
        }
        sb.append('=');
        if (hexFormat) {
            sb.append(this.value.getHexString());
            return;
        }
        if (X500Principal.CANONICAL.equals(attrFormat)) {
            sb.append(this.value.makeCanonical());
        } else if (X500Principal.RFC2253.equals(attrFormat)) {
            sb.append(this.value.getRFC2253String());
        } else {
            sb.append(this.value.escapedString);
        }
    }

    public ObjectIdentifier getType() {
        return this.oid;
    }

    public AttributeValue getValue() {
        return this.value;
    }

    private static ObjectIdentifier getOID(int[] oid) {
        int index = hashIntArray(oid) % 10;
        ObjectIdentifier[] list = KNOWN_OIDS[index];
        for (int i = 0; list[i] != null; i++) {
            if (Arrays.equals(oid, list[i].getOid())) {
                return list[i];
            }
        }
        return null;
    }

    private static void addOID(ObjectIdentifier oid) {
        int[] newOid = oid.getOid();
        int index = hashIntArray(newOid) % 10;
        ObjectIdentifier[] list = KNOWN_OIDS[index];
        int i = 0;
        while (list[i] != null) {
            if (!Arrays.equals(newOid, list[i].getOid())) {
                i++;
            } else {
                throw new Error("ObjectIdentifier: invalid static initialization; duplicate OIDs: " + oid.getName() + " " + list[i].getName());
            }
        }
        if (i == 9) {
            throw new Error("ObjectIdentifier: invalid static initialization; small OID pool capacity");
        }
        list[i] = oid;
    }

    private static int hashIntArray(int[] oid) {
        int intHash = 0;
        for (int i = 0; i < oid.length && i < 4; i++) {
            intHash += oid[i] << (i * 8);
        }
        return Integer.MAX_VALUE & intHash;
    }
}
