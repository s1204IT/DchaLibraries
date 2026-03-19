package sun.security.x509;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.Principal;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.security.auth.x500.X500Principal;
import sun.security.util.DerInputStream;
import sun.security.util.DerOutputStream;
import sun.security.util.DerValue;
import sun.security.util.ObjectIdentifier;

public class X500Name implements GeneralNameInterface, Principal {
    private static final Constructor<X500Principal> principalConstructor;
    private static final Field principalField;
    private volatile List<AVA> allAvaList;
    private String canonicalDn;
    private String dn;
    private byte[] encoded;
    private RDN[] names;
    private volatile List<RDN> rdnList;
    private String rfc1779Dn;
    private String rfc2253Dn;
    private X500Principal x500Principal;
    private static final Map<ObjectIdentifier, ObjectIdentifier> internedOIDs = new HashMap();
    private static final int[] commonName_data = {2, 5, 4, 3};
    private static final int[] SURNAME_DATA = {2, 5, 4, 4};
    private static final int[] SERIALNUMBER_DATA = {2, 5, 4, 5};
    private static final int[] countryName_data = {2, 5, 4, 6};
    private static final int[] localityName_data = {2, 5, 4, 7};
    private static final int[] stateName_data = {2, 5, 4, 8};
    private static final int[] streetAddress_data = {2, 5, 4, 9};
    private static final int[] orgName_data = {2, 5, 4, 10};
    private static final int[] orgUnitName_data = {2, 5, 4, 11};
    private static final int[] title_data = {2, 5, 4, 12};
    private static final int[] GIVENNAME_DATA = {2, 5, 4, 42};
    private static final int[] INITIALS_DATA = {2, 5, 4, 43};
    private static final int[] GENERATIONQUALIFIER_DATA = {2, 5, 4, 44};
    private static final int[] DNQUALIFIER_DATA = {2, 5, 4, 46};
    private static final int[] ipAddress_data = {1, 3, 6, 1, 4, 1, 42, 2, 11, 2, 1};
    private static final int[] DOMAIN_COMPONENT_DATA = {0, 9, 2342, 19200300, 100, 1, 25};
    private static final int[] userid_data = {0, 9, 2342, 19200300, 100, 1, 1};
    public static final ObjectIdentifier commonName_oid = intern(ObjectIdentifier.newInternal(commonName_data));
    public static final ObjectIdentifier SERIALNUMBER_OID = intern(ObjectIdentifier.newInternal(SERIALNUMBER_DATA));
    public static final ObjectIdentifier countryName_oid = intern(ObjectIdentifier.newInternal(countryName_data));
    public static final ObjectIdentifier localityName_oid = intern(ObjectIdentifier.newInternal(localityName_data));
    public static final ObjectIdentifier orgName_oid = intern(ObjectIdentifier.newInternal(orgName_data));
    public static final ObjectIdentifier orgUnitName_oid = intern(ObjectIdentifier.newInternal(orgUnitName_data));
    public static final ObjectIdentifier stateName_oid = intern(ObjectIdentifier.newInternal(stateName_data));
    public static final ObjectIdentifier streetAddress_oid = intern(ObjectIdentifier.newInternal(streetAddress_data));
    public static final ObjectIdentifier title_oid = intern(ObjectIdentifier.newInternal(title_data));
    public static final ObjectIdentifier DNQUALIFIER_OID = intern(ObjectIdentifier.newInternal(DNQUALIFIER_DATA));
    public static final ObjectIdentifier SURNAME_OID = intern(ObjectIdentifier.newInternal(SURNAME_DATA));
    public static final ObjectIdentifier GIVENNAME_OID = intern(ObjectIdentifier.newInternal(GIVENNAME_DATA));
    public static final ObjectIdentifier INITIALS_OID = intern(ObjectIdentifier.newInternal(INITIALS_DATA));
    public static final ObjectIdentifier GENERATIONQUALIFIER_OID = intern(ObjectIdentifier.newInternal(GENERATIONQUALIFIER_DATA));
    public static final ObjectIdentifier ipAddress_oid = intern(ObjectIdentifier.newInternal(ipAddress_data));
    public static final ObjectIdentifier DOMAIN_COMPONENT_OID = intern(ObjectIdentifier.newInternal(DOMAIN_COMPONENT_DATA));
    public static final ObjectIdentifier userid_oid = intern(ObjectIdentifier.newInternal(userid_data));

    public X500Name(String str) throws IOException {
        this(str, (Map<String, String>) Collections.emptyMap());
    }

    public X500Name(String str, Map<String, String> map) throws IOException {
        parseDN(str, map);
    }

    public X500Name(String str, String str2) throws IOException {
        if (str == null) {
            throw new NullPointerException("Name must not be null");
        }
        if (str2.equalsIgnoreCase(X500Principal.RFC2253)) {
            parseRFC2253DN(str);
        } else {
            if (str2.equalsIgnoreCase("DEFAULT")) {
                parseDN(str, Collections.emptyMap());
                return;
            }
            throw new IOException("Unsupported format " + str2);
        }
    }

    public X500Name(String str, String str2, String str3, String str4) throws IOException {
        this.names = new RDN[4];
        this.names[3] = new RDN(1);
        this.names[3].assertion[0] = new AVA(commonName_oid, new DerValue(str));
        this.names[2] = new RDN(1);
        this.names[2].assertion[0] = new AVA(orgUnitName_oid, new DerValue(str2));
        this.names[1] = new RDN(1);
        this.names[1].assertion[0] = new AVA(orgName_oid, new DerValue(str3));
        this.names[0] = new RDN(1);
        this.names[0].assertion[0] = new AVA(countryName_oid, new DerValue(str4));
    }

    public X500Name(String str, String str2, String str3, String str4, String str5, String str6) throws IOException {
        this.names = new RDN[6];
        this.names[5] = new RDN(1);
        this.names[5].assertion[0] = new AVA(commonName_oid, new DerValue(str));
        this.names[4] = new RDN(1);
        this.names[4].assertion[0] = new AVA(orgUnitName_oid, new DerValue(str2));
        this.names[3] = new RDN(1);
        this.names[3].assertion[0] = new AVA(orgName_oid, new DerValue(str3));
        this.names[2] = new RDN(1);
        this.names[2].assertion[0] = new AVA(localityName_oid, new DerValue(str4));
        this.names[1] = new RDN(1);
        this.names[1].assertion[0] = new AVA(stateName_oid, new DerValue(str5));
        this.names[0] = new RDN(1);
        this.names[0].assertion[0] = new AVA(countryName_oid, new DerValue(str6));
    }

    public X500Name(RDN[] rdnArr) throws IOException {
        if (rdnArr == null) {
            this.names = new RDN[0];
            return;
        }
        this.names = (RDN[]) rdnArr.clone();
        for (int i = 0; i < this.names.length; i++) {
            if (this.names[i] == null) {
                throw new IOException("Cannot create an X500Name");
            }
        }
    }

    public X500Name(DerValue derValue) throws IOException {
        this(derValue.toDerInputStream());
    }

    public X500Name(DerInputStream derInputStream) throws IOException {
        parseDER(derInputStream);
    }

    public X500Name(byte[] bArr) throws IOException {
        parseDER(new DerInputStream(bArr));
    }

    public List<RDN> rdns() {
        List<RDN> list = this.rdnList;
        if (list == null) {
            List<RDN> listUnmodifiableList = Collections.unmodifiableList(Arrays.asList(this.names));
            this.rdnList = listUnmodifiableList;
            return listUnmodifiableList;
        }
        return list;
    }

    public int size() {
        return this.names.length;
    }

    public List<AVA> allAvas() {
        List<AVA> list = this.allAvaList;
        if (list == null) {
            ArrayList arrayList = new ArrayList();
            for (int i = 0; i < this.names.length; i++) {
                arrayList.addAll(this.names[i].avas());
            }
            List<AVA> listUnmodifiableList = Collections.unmodifiableList(arrayList);
            this.allAvaList = listUnmodifiableList;
            return listUnmodifiableList;
        }
        return list;
    }

    public int avaSize() {
        return allAvas().size();
    }

    public boolean isEmpty() {
        int length = this.names.length;
        for (int i = 0; i < length; i++) {
            if (this.names[i].assertion.length != 0) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        return getRFC2253CanonicalName().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof X500Name)) {
            return false;
        }
        X500Name x500Name = (X500Name) obj;
        if (this.canonicalDn != null && x500Name.canonicalDn != null) {
            return this.canonicalDn.equals(x500Name.canonicalDn);
        }
        int length = this.names.length;
        if (length != x500Name.names.length) {
            return false;
        }
        for (int i = 0; i < length; i++) {
            if (this.names[i].assertion.length != x500Name.names[i].assertion.length) {
                return false;
            }
        }
        return getRFC2253CanonicalName().equals(x500Name.getRFC2253CanonicalName());
    }

    private String getString(DerValue derValue) throws IOException {
        if (derValue == null) {
            return null;
        }
        String asString = derValue.getAsString();
        if (asString == null) {
            throw new IOException("not a DER string encoding, " + ((int) derValue.tag));
        }
        return asString;
    }

    @Override
    public int getType() {
        return 4;
    }

    public String getCountry() throws IOException {
        return getString(findAttribute(countryName_oid));
    }

    public String getOrganization() throws IOException {
        return getString(findAttribute(orgName_oid));
    }

    public String getOrganizationalUnit() throws IOException {
        return getString(findAttribute(orgUnitName_oid));
    }

    public String getCommonName() throws IOException {
        return getString(findAttribute(commonName_oid));
    }

    public String getLocality() throws IOException {
        return getString(findAttribute(localityName_oid));
    }

    public String getState() throws IOException {
        return getString(findAttribute(stateName_oid));
    }

    public String getDomain() throws IOException {
        return getString(findAttribute(DOMAIN_COMPONENT_OID));
    }

    public String getDNQualifier() throws IOException {
        return getString(findAttribute(DNQUALIFIER_OID));
    }

    public String getSurname() throws IOException {
        return getString(findAttribute(SURNAME_OID));
    }

    public String getGivenName() throws IOException {
        return getString(findAttribute(GIVENNAME_OID));
    }

    public String getInitials() throws IOException {
        return getString(findAttribute(INITIALS_OID));
    }

    public String getGeneration() throws IOException {
        return getString(findAttribute(GENERATIONQUALIFIER_OID));
    }

    public String getIP() throws IOException {
        return getString(findAttribute(ipAddress_oid));
    }

    @Override
    public String toString() {
        if (this.dn == null) {
            generateDN();
        }
        return this.dn;
    }

    public String getRFC1779Name() {
        return getRFC1779Name(Collections.emptyMap());
    }

    public String getRFC1779Name(Map<String, String> map) throws IllegalArgumentException {
        if (map.isEmpty()) {
            if (this.rfc1779Dn != null) {
                return this.rfc1779Dn;
            }
            this.rfc1779Dn = generateRFC1779DN(map);
            return this.rfc1779Dn;
        }
        return generateRFC1779DN(map);
    }

    public String getRFC2253Name() {
        return getRFC2253Name(Collections.emptyMap());
    }

    public String getRFC2253Name(Map<String, String> map) {
        if (map.isEmpty()) {
            if (this.rfc2253Dn != null) {
                return this.rfc2253Dn;
            }
            this.rfc2253Dn = generateRFC2253DN(map);
            return this.rfc2253Dn;
        }
        return generateRFC2253DN(map);
    }

    private String generateRFC2253DN(Map<String, String> map) {
        if (this.names.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(48);
        for (int length = this.names.length - 1; length >= 0; length--) {
            if (length < this.names.length - 1) {
                sb.append(',');
            }
            sb.append(this.names[length].toRFC2253String(map));
        }
        return sb.toString();
    }

    public String getRFC2253CanonicalName() {
        if (this.canonicalDn != null) {
            return this.canonicalDn;
        }
        if (this.names.length == 0) {
            this.canonicalDn = "";
            return this.canonicalDn;
        }
        StringBuilder sb = new StringBuilder(48);
        for (int length = this.names.length - 1; length >= 0; length--) {
            if (length < this.names.length - 1) {
                sb.append(',');
            }
            sb.append(this.names[length].toRFC2253String(true));
        }
        this.canonicalDn = sb.toString();
        return this.canonicalDn;
    }

    @Override
    public String getName() {
        return toString();
    }

    private DerValue findAttribute(ObjectIdentifier objectIdentifier) {
        if (this.names != null) {
            for (int i = 0; i < this.names.length; i++) {
                DerValue derValueFindAttribute = this.names[i].findAttribute(objectIdentifier);
                if (derValueFindAttribute != null) {
                    return derValueFindAttribute;
                }
            }
            return null;
        }
        return null;
    }

    public DerValue findMostSpecificAttribute(ObjectIdentifier objectIdentifier) {
        if (this.names != null) {
            for (int length = this.names.length - 1; length >= 0; length--) {
                DerValue derValueFindAttribute = this.names[length].findAttribute(objectIdentifier);
                if (derValueFindAttribute != null) {
                    return derValueFindAttribute;
                }
            }
            return null;
        }
        return null;
    }

    private void parseDER(DerInputStream derInputStream) throws IOException {
        DerValue[] sequence;
        byte[] byteArray = derInputStream.toByteArray();
        try {
            sequence = derInputStream.getSequence(5);
        } catch (IOException e) {
            sequence = byteArray == null ? null : new DerInputStream(new DerValue((byte) 48, byteArray).toByteArray()).getSequence(5);
        }
        if (sequence == null) {
            this.names = new RDN[0];
            return;
        }
        this.names = new RDN[sequence.length];
        for (int i = 0; i < sequence.length; i++) {
            this.names[i] = new RDN(sequence[i]);
        }
    }

    @Deprecated
    public void emit(DerOutputStream derOutputStream) throws IOException {
        encode(derOutputStream);
    }

    @Override
    public void encode(DerOutputStream derOutputStream) throws IOException {
        DerOutputStream derOutputStream2 = new DerOutputStream();
        for (int i = 0; i < this.names.length; i++) {
            this.names[i].encode(derOutputStream2);
        }
        derOutputStream.write((byte) 48, derOutputStream2);
    }

    public byte[] getEncodedInternal() throws IOException {
        if (this.encoded == null) {
            DerOutputStream derOutputStream = new DerOutputStream();
            DerOutputStream derOutputStream2 = new DerOutputStream();
            for (int i = 0; i < this.names.length; i++) {
                this.names[i].encode(derOutputStream2);
            }
            derOutputStream.write((byte) 48, derOutputStream2);
            this.encoded = derOutputStream.toByteArray();
        }
        return this.encoded;
    }

    public byte[] getEncoded() throws IOException {
        return (byte[]) getEncodedInternal().clone();
    }

    private void parseDN(String str, Map<String, String> map) throws IOException {
        if (str == null || str.length() == 0) {
            this.names = new RDN[0];
            return;
        }
        checkNoNewLinesNorTabsAtBeginningOfDN(str);
        ArrayList arrayList = new ArrayList();
        int iIndexOf = str.indexOf(44);
        int iIndexOf2 = str.indexOf(59);
        int i = 0;
        int iCountQuotes = 0;
        int i2 = 0;
        while (true) {
            if (iIndexOf >= 0 || iIndexOf2 >= 0) {
                if (iIndexOf2 >= 0) {
                    if (iIndexOf >= 0) {
                        iIndexOf = Math.min(iIndexOf, iIndexOf2);
                    } else {
                        iIndexOf = iIndexOf2;
                    }
                }
                iCountQuotes += countQuotes(str, i2, iIndexOf);
                if (iIndexOf >= 0 && iCountQuotes != 1 && !escaped(iIndexOf, i2, str)) {
                    arrayList.add(new RDN(str.substring(i, iIndexOf), map));
                    iCountQuotes = 0;
                    i = iIndexOf + 1;
                }
                i2 = iIndexOf + 1;
                iIndexOf = str.indexOf(44, i2);
                iIndexOf2 = str.indexOf(59, i2);
            } else {
                arrayList.add(new RDN(str.substring(i), map));
                Collections.reverse(arrayList);
                this.names = (RDN[]) arrayList.toArray(new RDN[arrayList.size()]);
                return;
            }
        }
    }

    private void checkNoNewLinesNorTabsAtBeginningOfDN(String str) {
        for (int i = 0; i < str.length(); i++) {
            char cCharAt = str.charAt(i);
            if (cCharAt != ' ') {
                if (cCharAt == '\t' || cCharAt == '\n') {
                    throw new IllegalArgumentException("DN cannot start with newline nor tab");
                }
                return;
            }
        }
    }

    private void parseRFC2253DN(String str) throws IOException {
        int i = 0;
        if (str.length() == 0) {
            this.names = new RDN[0];
            return;
        }
        ArrayList arrayList = new ArrayList();
        int iIndexOf = str.indexOf(44);
        int i2 = 0;
        while (iIndexOf >= 0) {
            if (iIndexOf > 0 && !escaped(iIndexOf, i2, str)) {
                arrayList.add(new RDN(str.substring(i, iIndexOf), X500Principal.RFC2253));
                i = iIndexOf + 1;
            }
            i2 = iIndexOf + 1;
            iIndexOf = str.indexOf(44, i2);
        }
        arrayList.add(new RDN(str.substring(i), X500Principal.RFC2253));
        Collections.reverse(arrayList);
        this.names = (RDN[]) arrayList.toArray(new RDN[arrayList.size()]);
    }

    static int countQuotes(String str, int i, int i2) {
        int i3 = 0;
        int i4 = 0;
        while (i < i2) {
            if (str.charAt(i) == '\"' && i4 % 2 == 0) {
                i3++;
            }
            i4 = str.charAt(i) == '\\' ? i4 + 1 : 0;
            i++;
        }
        return i3;
    }

    private static boolean escaped(int i, int i2, String str) {
        if (i == 1 && str.charAt(i - 1) == '\\') {
            return true;
        }
        if (i > 1 && str.charAt(i - 1) == '\\' && str.charAt(i - 2) != '\\') {
            return true;
        }
        if (i <= 1 || str.charAt(i - 1) != '\\' || str.charAt(i - 2) != '\\') {
            return false;
        }
        int i3 = 0;
        for (int i4 = i - 1; i4 >= i2; i4--) {
            if (str.charAt(i4) == '\\') {
                i3++;
            }
        }
        return i3 % 2 != 0;
    }

    private void generateDN() {
        if (this.names.length == 1) {
            this.dn = this.names[0].toString();
            return;
        }
        StringBuilder sb = new StringBuilder(48);
        if (this.names != null) {
            for (int length = this.names.length - 1; length >= 0; length--) {
                if (length != this.names.length - 1) {
                    sb.append(", ");
                }
                sb.append(this.names[length].toString());
            }
        }
        this.dn = sb.toString();
    }

    private String generateRFC1779DN(Map<String, String> map) {
        if (this.names.length == 1) {
            return this.names[0].toRFC1779String(map);
        }
        StringBuilder sb = new StringBuilder(48);
        if (this.names != null) {
            for (int length = this.names.length - 1; length >= 0; length--) {
                if (length != this.names.length - 1) {
                    sb.append(", ");
                }
                sb.append(this.names[length].toRFC1779String(map));
            }
        }
        return sb.toString();
    }

    static ObjectIdentifier intern(ObjectIdentifier objectIdentifier) {
        ObjectIdentifier objectIdentifierPutIfAbsent = internedOIDs.putIfAbsent(objectIdentifier, objectIdentifier);
        return objectIdentifierPutIfAbsent == null ? objectIdentifier : objectIdentifierPutIfAbsent;
    }

    static {
        try {
            Object[] objArr = (Object[]) AccessController.doPrivileged(new PrivilegedExceptionAction<Object[]>() {
                @Override
                public Object[] run() throws Exception {
                    Constructor declaredConstructor = X500Principal.class.getDeclaredConstructor(X500Name.class);
                    declaredConstructor.setAccessible(true);
                    Field declaredField = X500Principal.class.getDeclaredField("thisX500Name");
                    declaredField.setAccessible(true);
                    return new Object[]{declaredConstructor, declaredField};
                }
            });
            principalConstructor = (Constructor) objArr[0];
            principalField = (Field) objArr[1];
        } catch (Exception e) {
            throw new InternalError("Could not obtain X500Principal access", e);
        }
    }

    @Override
    public int constrains(GeneralNameInterface generalNameInterface) throws UnsupportedOperationException {
        if (generalNameInterface == null || generalNameInterface.getType() != 4) {
            return -1;
        }
        X500Name x500Name = (X500Name) generalNameInterface;
        if (x500Name.equals(this)) {
            return 0;
        }
        if (x500Name.names.length != 0) {
            if (this.names.length == 0 || x500Name.isWithinSubtree(this)) {
                return 1;
            }
            if (!isWithinSubtree(x500Name)) {
                return 3;
            }
        }
        return 2;
    }

    private boolean isWithinSubtree(X500Name x500Name) {
        if (this == x500Name) {
            return true;
        }
        if (x500Name == null) {
            return false;
        }
        if (x500Name.names.length == 0) {
            return true;
        }
        if (this.names.length == 0 || this.names.length < x500Name.names.length) {
            return false;
        }
        for (int i = 0; i < x500Name.names.length; i++) {
            if (!this.names[i].equals(x500Name.names[i])) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int subtreeDepth() throws UnsupportedOperationException {
        return this.names.length;
    }

    public X500Name commonAncestor(X500Name x500Name) {
        if (x500Name == null) {
            return null;
        }
        int length = x500Name.names.length;
        int length2 = this.names.length;
        if (length2 == 0 || length == 0) {
            return null;
        }
        if (length2 < length) {
            length = length2;
        }
        int i = 0;
        while (true) {
            if (i >= length) {
                break;
            }
            if (this.names[i].equals(x500Name.names[i])) {
                i++;
            } else if (i == 0) {
                return null;
            }
        }
        RDN[] rdnArr = new RDN[i];
        for (int i2 = 0; i2 < i; i2++) {
            rdnArr[i2] = this.names[i2];
        }
        try {
            return new X500Name(rdnArr);
        } catch (IOException e) {
            return null;
        }
    }

    public X500Principal asX500Principal() {
        if (this.x500Principal == null) {
            try {
                this.x500Principal = principalConstructor.newInstance(this);
            } catch (Exception e) {
                throw new RuntimeException("Unexpected exception", e);
            }
        }
        return this.x500Principal;
    }

    public static X500Name asX500Name(X500Principal x500Principal) {
        try {
            X500Name x500Name = (X500Name) principalField.get(x500Principal);
            x500Name.x500Principal = x500Principal;
            return x500Name;
        } catch (Exception e) {
            throw new RuntimeException("Unexpected exception", e);
        }
    }
}
