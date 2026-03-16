package org.apache.harmony.security.x509;

import dalvik.bytecode.Opcodes;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import javax.security.auth.x500.X500Principal;
import org.apache.harmony.security.asn1.ASN1Choice;
import org.apache.harmony.security.asn1.ASN1Implicit;
import org.apache.harmony.security.asn1.ASN1OctetString;
import org.apache.harmony.security.asn1.ASN1Oid;
import org.apache.harmony.security.asn1.ASN1SequenceOf;
import org.apache.harmony.security.asn1.ASN1StringType;
import org.apache.harmony.security.asn1.ASN1Type;
import org.apache.harmony.security.asn1.BerInputStream;
import org.apache.harmony.security.asn1.ObjectIdentifier;
import org.apache.harmony.security.utils.Array;
import org.apache.harmony.security.x501.Name;

public final class GeneralName {
    public static final ASN1Choice ASN1;
    public static final int DIR_NAME = 4;
    public static final int DNS_NAME = 2;
    public static final int EDIP_NAME = 5;
    public static final int IP_ADDR = 7;
    private static final ASN1SequenceOf NAME_ASN1;
    public static final int OTHER_NAME = 0;
    public static final int REG_ID = 8;
    public static final int RFC822_NAME = 1;
    public static final int UR_ID = 6;
    public static final int X400_ADDR = 3;
    private static ASN1Type[] nameASN1 = new ASN1Type[9];
    private byte[] encoding;
    private Object name;
    private byte[] name_encoding;
    private int tag;

    static {
        nameASN1[0] = OtherName.ASN1;
        nameASN1[1] = ASN1StringType.IA5STRING;
        nameASN1[2] = ASN1StringType.IA5STRING;
        nameASN1[6] = ASN1StringType.IA5STRING;
        nameASN1[3] = ORAddress.ASN1;
        nameASN1[4] = Name.ASN1;
        nameASN1[5] = EDIPartyName.ASN1;
        nameASN1[7] = ASN1OctetString.getInstance();
        nameASN1[8] = ASN1Oid.getInstance();
        NAME_ASN1 = new ASN1SequenceOf(Name.ASN1) {
            @Override
            public Object decode(BerInputStream in) throws IOException {
                return ((List) super.decode(in)).get(0);
            }
        };
        ASN1 = new ASN1Choice(new ASN1Type[]{new ASN1Implicit(0, OtherName.ASN1), new ASN1Implicit(1, ASN1StringType.IA5STRING), new ASN1Implicit(2, ASN1StringType.IA5STRING), new ASN1Implicit(3, ORAddress.ASN1), new ASN1Implicit(4, NAME_ASN1), new ASN1Implicit(5, EDIPartyName.ASN1), new ASN1Implicit(6, ASN1StringType.IA5STRING), new ASN1Implicit(7, ASN1OctetString.getInstance()), new ASN1Implicit(8, ASN1Oid.getInstance())}) {
            @Override
            public Object getObjectToEncode(Object value) {
                return ((GeneralName) value).name;
            }

            @Override
            public int getIndex(Object object) {
                return ((GeneralName) object).tag;
            }

            @Override
            public Object getDecodedObject(BerInputStream in) throws IOException {
                GeneralName result;
                switch (in.choiceIndex) {
                    case 0:
                        result = new GeneralName((OtherName) in.content);
                        break;
                    case 1:
                    case 2:
                        result = new GeneralName(in.choiceIndex, (String) in.content);
                        break;
                    case 3:
                        result = new GeneralName((ORAddress) in.content);
                        break;
                    case 4:
                        result = new GeneralName((Name) in.content);
                        break;
                    case 5:
                        result = new GeneralName((EDIPartyName) in.content);
                        break;
                    case 6:
                        String uri = (String) in.content;
                        if (uri.indexOf(":") == -1) {
                            throw new IOException("GeneralName: scheme is missing in URI: " + uri);
                        }
                        result = new GeneralName(in.choiceIndex, uri);
                        break;
                        break;
                    case 7:
                        result = new GeneralName((byte[]) in.content);
                        break;
                    case 8:
                        result = new GeneralName(in.choiceIndex, ObjectIdentifier.toString((int[]) in.content));
                        break;
                    default:
                        throw new IOException("GeneralName: unknown tag: " + in.choiceIndex);
                }
                result.encoding = in.getEncoded();
                return result;
            }
        };
    }

    public GeneralName(int tag, String name) throws IOException {
        if (name == null) {
            throw new IOException("name == null");
        }
        this.tag = tag;
        switch (tag) {
            case 0:
            case 3:
            case 5:
                throw new IOException("Unknown string representation for type [" + tag + "]");
            case 1:
                this.name = name;
                return;
            case 2:
                checkDNS(name);
                this.name = name;
                return;
            case 4:
                this.name = new Name(name);
                return;
            case 6:
                checkURI(name);
                this.name = name;
                return;
            case 7:
                this.name = ipStrToBytes(name);
                return;
            case 8:
                this.name = oidStrToInts(name);
                return;
            default:
                throw new IOException("Unknown type: [" + tag + "]");
        }
    }

    public GeneralName(OtherName name) {
        this.tag = 0;
        this.name = name;
    }

    public GeneralName(ORAddress name) {
        this.tag = 3;
        this.name = name;
    }

    public GeneralName(Name name) {
        this.tag = 4;
        this.name = name;
    }

    public GeneralName(EDIPartyName name) {
        this.tag = 5;
        this.name = name;
    }

    public GeneralName(byte[] name) throws IllegalArgumentException {
        this.tag = 7;
        this.name = new byte[name.length];
        System.arraycopy(name, 0, this.name, 0, name.length);
    }

    public GeneralName(int tag, byte[] name) throws IOException {
        if (name == null) {
            throw new NullPointerException("name == null");
        }
        if (tag < 0 || tag > 8) {
            throw new IOException("GeneralName: unknown tag: " + tag);
        }
        this.tag = tag;
        this.name_encoding = new byte[name.length];
        System.arraycopy(name, 0, this.name_encoding, 0, name.length);
        this.name = nameASN1[tag].decode(this.name_encoding);
    }

    public int getTag() {
        return this.tag;
    }

    public Object getName() {
        return this.name;
    }

    public boolean equals(Object other) {
        if (!(other instanceof GeneralName)) {
            return false;
        }
        GeneralName gname = (GeneralName) other;
        if (this.tag != gname.tag) {
            return false;
        }
        switch (this.tag) {
        }
        return false;
    }

    public int hashCode() {
        switch (this.tag) {
            case 0:
            case 3:
            case 4:
            case 5:
                return Arrays.hashCode(getEncoded());
            case 1:
            case 2:
            case 6:
            case 7:
            case 8:
                return this.name.hashCode();
            default:
                return super.hashCode();
        }
    }

    public boolean isAcceptable(GeneralName gname) {
        if (this.tag != gname.getTag()) {
            return false;
        }
        switch (this.tag) {
            case 0:
            case 3:
            case 4:
            case 5:
            case 8:
                return Arrays.equals(getEncoded(), gname.getEncoded());
            case 1:
                return ((String) gname.getName()).toLowerCase(Locale.US).endsWith(((String) this.name).toLowerCase(Locale.US));
            case 2:
                String dns = (String) this.name;
                String _dns = (String) gname.getName();
                if (dns.equalsIgnoreCase(_dns)) {
                    return true;
                }
                return _dns.toLowerCase(Locale.US).endsWith("." + dns.toLowerCase(Locale.US));
            case 6:
                String uri = (String) this.name;
                int begin = uri.indexOf("://") + 3;
                int end = uri.indexOf(47, begin);
                String host = end == -1 ? uri.substring(begin) : uri.substring(begin, end);
                String uri2 = (String) gname.getName();
                int begin2 = uri2.indexOf("://") + 3;
                int end2 = uri2.indexOf(47, begin2);
                String _host = end2 == -1 ? uri2.substring(begin2) : uri2.substring(begin2, end2);
                if (host.startsWith(".")) {
                    return _host.toLowerCase(Locale.US).endsWith(host.toLowerCase(Locale.US));
                }
                return host.equalsIgnoreCase(_host);
            case 7:
                byte[] address = (byte[]) this.name;
                byte[] _address = (byte[]) gname.getName();
                int length = address.length;
                if (length != 4 && length != 8 && length != 16 && length != 32) {
                    return false;
                }
                int _length = _address.length;
                if (length == _length) {
                    return Arrays.equals(address, _address);
                }
                if (length == _length * 2) {
                    for (int i = 0; i < _address.length; i++) {
                        int octet = _address[i] & Opcodes.OP_CONST_CLASS_JUMBO;
                        int min = address[i] & Opcodes.OP_CONST_CLASS_JUMBO;
                        int max = address[i + _length] & Opcodes.OP_CONST_CLASS_JUMBO;
                        if (octet < min || octet > max) {
                            return false;
                        }
                    }
                    return true;
                }
                return false;
            default:
                return true;
        }
    }

    public List<Object> getAsList() {
        ArrayList<Object> result = new ArrayList<>();
        result.add(Integer.valueOf(this.tag));
        switch (this.tag) {
            case 0:
                result.add(((OtherName) this.name).getEncoded());
                break;
            case 1:
            case 2:
            case 6:
                result.add(this.name);
                break;
            case 3:
                result.add(((ORAddress) this.name).getEncoded());
                break;
            case 4:
                result.add(((Name) this.name).getName(X500Principal.RFC2253));
                break;
            case 5:
                result.add(((EDIPartyName) this.name).getEncoded());
                break;
            case 7:
                result.add(ipBytesToStr((byte[]) this.name));
                break;
            case 8:
                result.add(ObjectIdentifier.toString((int[]) this.name));
                break;
        }
        return Collections.unmodifiableList(result);
    }

    public String toString() {
        switch (this.tag) {
            case 0:
                String result = "otherName[0]: " + Array.getBytesAsString(getEncoded());
                return result;
            case 1:
                String result2 = "rfc822Name[1]: " + this.name;
                return result2;
            case 2:
                String result3 = "dNSName[2]: " + this.name;
                return result3;
            case 3:
                String result4 = "x400Address[3]: " + Array.getBytesAsString(getEncoded());
                return result4;
            case 4:
                String result5 = "directoryName[4]: " + ((Name) this.name).getName(X500Principal.RFC2253);
                return result5;
            case 5:
                String result6 = "ediPartyName[5]: " + Array.getBytesAsString(getEncoded());
                return result6;
            case 6:
                String result7 = "uniformResourceIdentifier[6]: " + this.name;
                return result7;
            case 7:
                String result8 = "iPAddress[7]: " + ipBytesToStr((byte[]) this.name);
                return result8;
            case 8:
                String result9 = "registeredID[8]: " + ObjectIdentifier.toString((int[]) this.name);
                return result9;
            default:
                return "";
        }
    }

    public byte[] getEncoded() {
        if (this.encoding == null) {
            this.encoding = ASN1.encode(this);
        }
        return this.encoding;
    }

    public byte[] getEncodedName() {
        if (this.name_encoding == null) {
            this.name_encoding = nameASN1[this.tag].encode(this.name);
        }
        return this.name_encoding;
    }

    public static void checkDNS(String dns) throws IOException {
        String string = dns.toLowerCase(Locale.US);
        int length = string.length();
        boolean first_letter = true;
        for (int i = 0; i < length; i++) {
            char ch = string.charAt(i);
            if (first_letter) {
                if ((ch > 'z' || ch < 'a') && ((ch < '0' || ch > '9') && ch != '*')) {
                    throw new IOException("DNS name must start with a letter: " + dns);
                }
                first_letter = false;
            } else {
                if ((ch < 'a' || ch > 'z') && ((ch < '0' || ch > '9') && ch != '-' && ch != '.' && ch != '*')) {
                    throw new IOException("Incorrect DNS name: " + dns);
                }
                if (ch != '.') {
                    continue;
                } else {
                    if (string.charAt(i - 1) == '-') {
                        throw new IOException("Incorrect DNS name: label ends with '-': " + dns);
                    }
                    first_letter = true;
                }
            }
        }
    }

    public static void checkURI(String uri) throws IOException {
        try {
            URI ur = new URI(uri);
            if (ur.getScheme() == null || ur.getRawSchemeSpecificPart().isEmpty()) {
                throw new IOException("No scheme or scheme-specific-part in uniformResourceIdentifier: " + uri);
            }
            if (!ur.isAbsolute()) {
                throw new IOException("Relative uniformResourceIdentifier: " + uri);
            }
        } catch (URISyntaxException e) {
            throw ((IOException) new IOException("Bad representation of uniformResourceIdentifier: " + uri).initCause(e));
        }
    }

    public static int[] oidStrToInts(String oid) throws IOException {
        int number;
        int length = oid.length();
        if (oid.charAt(length - 1) == '.') {
            throw new IOException("Bad OID: " + oid);
        }
        int[] result = new int[(length / 2) + 1];
        int i = 0;
        int number2 = 0;
        while (true) {
            if (i >= length) {
                number = number2;
                break;
            }
            int value = 0;
            int pos = i;
            while (i < length) {
                char ch = oid.charAt(i);
                if (ch < '0' || ch > '9') {
                    break;
                }
                value = (value * 10) + (ch - '0');
                i++;
            }
            if (i == pos) {
                throw new IOException("Bad OID: " + oid);
            }
            number = number2 + 1;
            result[number2] = value;
            if (i == length) {
                break;
            }
            if (oid.charAt(i) == '.') {
                i++;
                number2 = number;
            } else {
                throw new IOException("Bad OID: " + oid);
            }
        }
    }

    public static byte[] ipStrToBytes(String ip) throws IOException {
        if (!InetAddress.isNumeric(ip)) {
            throw new IOException("Not an IP address: " + ip);
        }
        return InetAddress.getByName(ip).getAddress();
    }

    public static String ipBytesToStr(byte[] ip) {
        try {
            return InetAddress.getByAddress(null, ip).getHostAddress();
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Unexpected IP address: " + Arrays.toString(ip));
        }
    }
}
