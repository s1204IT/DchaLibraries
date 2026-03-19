package sun.security.x509;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import sun.security.util.DerOutputStream;
import sun.security.util.DerValue;
import sun.security.util.ObjectIdentifier;

public class ExtendedKeyUsageExtension extends Extension implements CertAttrSet<String> {
    public static final String IDENT = "x509.info.extensions.ExtendedKeyUsage";
    public static final String NAME = "ExtendedKeyUsage";
    public static final String USAGES = "usages";
    private Vector<ObjectIdentifier> keyUsages;
    private static final Map<ObjectIdentifier, String> map = new HashMap();
    private static final int[] anyExtendedKeyUsageOidData = {2, 5, 29, 37, 0};
    private static final int[] serverAuthOidData = {1, 3, 6, 1, 5, 5, 7, 3, 1};
    private static final int[] clientAuthOidData = {1, 3, 6, 1, 5, 5, 7, 3, 2};
    private static final int[] codeSigningOidData = {1, 3, 6, 1, 5, 5, 7, 3, 3};
    private static final int[] emailProtectionOidData = {1, 3, 6, 1, 5, 5, 7, 3, 4};
    private static final int[] ipsecEndSystemOidData = {1, 3, 6, 1, 5, 5, 7, 3, 5};
    private static final int[] ipsecTunnelOidData = {1, 3, 6, 1, 5, 5, 7, 3, 6};
    private static final int[] ipsecUserOidData = {1, 3, 6, 1, 5, 5, 7, 3, 7};
    private static final int[] timeStampingOidData = {1, 3, 6, 1, 5, 5, 7, 3, 8};
    private static final int[] OCSPSigningOidData = {1, 3, 6, 1, 5, 5, 7, 3, 9};

    static {
        map.put(ObjectIdentifier.newInternal(anyExtendedKeyUsageOidData), "anyExtendedKeyUsage");
        map.put(ObjectIdentifier.newInternal(serverAuthOidData), "serverAuth");
        map.put(ObjectIdentifier.newInternal(clientAuthOidData), "clientAuth");
        map.put(ObjectIdentifier.newInternal(codeSigningOidData), "codeSigning");
        map.put(ObjectIdentifier.newInternal(emailProtectionOidData), "emailProtection");
        map.put(ObjectIdentifier.newInternal(ipsecEndSystemOidData), "ipsecEndSystem");
        map.put(ObjectIdentifier.newInternal(ipsecTunnelOidData), "ipsecTunnel");
        map.put(ObjectIdentifier.newInternal(ipsecUserOidData), "ipsecUser");
        map.put(ObjectIdentifier.newInternal(timeStampingOidData), "timeStamping");
        map.put(ObjectIdentifier.newInternal(OCSPSigningOidData), "OCSPSigning");
    }

    private void encodeThis() throws IOException {
        if (this.keyUsages == null || this.keyUsages.isEmpty()) {
            this.extensionValue = null;
            return;
        }
        DerOutputStream derOutputStream = new DerOutputStream();
        DerOutputStream derOutputStream2 = new DerOutputStream();
        for (int i = 0; i < this.keyUsages.size(); i++) {
            derOutputStream2.putOID(this.keyUsages.elementAt(i));
        }
        derOutputStream.write((byte) 48, derOutputStream2);
        this.extensionValue = derOutputStream.toByteArray();
    }

    public ExtendedKeyUsageExtension(Vector<ObjectIdentifier> vector) throws IOException {
        this(Boolean.FALSE, vector);
    }

    public ExtendedKeyUsageExtension(Boolean bool, Vector<ObjectIdentifier> vector) throws IOException {
        this.keyUsages = vector;
        this.extensionId = PKIXExtensions.ExtendedKeyUsage_Id;
        this.critical = bool.booleanValue();
        encodeThis();
    }

    public ExtendedKeyUsageExtension(Boolean bool, Object obj) throws IOException {
        this.extensionId = PKIXExtensions.ExtendedKeyUsage_Id;
        this.critical = bool.booleanValue();
        this.extensionValue = (byte[]) obj;
        DerValue derValue = new DerValue(this.extensionValue);
        if (derValue.tag != 48) {
            throw new IOException("Invalid encoding for ExtendedKeyUsageExtension.");
        }
        this.keyUsages = new Vector<>();
        while (derValue.data.available() != 0) {
            this.keyUsages.addElement(derValue.data.getDerValue().getOID());
        }
    }

    @Override
    public String toString() {
        if (this.keyUsages == null) {
            return "";
        }
        String str = "  ";
        boolean z = true;
        for (ObjectIdentifier objectIdentifier : this.keyUsages) {
            if (!z) {
                str = str + "\n  ";
            }
            String str2 = map.get(objectIdentifier);
            str = str2 != null ? str + str2 : str + objectIdentifier.toString();
            z = false;
        }
        return super.toString() + "ExtendedKeyUsages [\n" + str + "\n]\n";
    }

    @Override
    public void encode(OutputStream outputStream) throws IOException {
        DerOutputStream derOutputStream = new DerOutputStream();
        if (this.extensionValue == null) {
            this.extensionId = PKIXExtensions.ExtendedKeyUsage_Id;
            this.critical = false;
            encodeThis();
        }
        super.encode(derOutputStream);
        outputStream.write(derOutputStream.toByteArray());
    }

    @Override
    public void set(String str, Object obj) throws IOException {
        if (str.equalsIgnoreCase(USAGES)) {
            if (!(obj instanceof Vector)) {
                throw new IOException("Attribute value should be of type Vector.");
            }
            this.keyUsages = (Vector) obj;
            encodeThis();
            return;
        }
        throw new IOException("Attribute name [" + str + "] not recognized by CertAttrSet:ExtendedKeyUsageExtension.");
    }

    @Override
    public Vector<ObjectIdentifier> get(String str) throws IOException {
        if (str.equalsIgnoreCase(USAGES)) {
            return this.keyUsages;
        }
        throw new IOException("Attribute name [" + str + "] not recognized by CertAttrSet:ExtendedKeyUsageExtension.");
    }

    @Override
    public void delete(String str) throws IOException {
        if (str.equalsIgnoreCase(USAGES)) {
            this.keyUsages = null;
            encodeThis();
        } else {
            throw new IOException("Attribute name [" + str + "] not recognized by CertAttrSet:ExtendedKeyUsageExtension.");
        }
    }

    @Override
    public Enumeration<String> getElements() {
        AttributeNameEnumeration attributeNameEnumeration = new AttributeNameEnumeration();
        attributeNameEnumeration.addElement(USAGES);
        return attributeNameEnumeration.elements();
    }

    @Override
    public String getName() {
        return NAME;
    }

    public List<String> getExtendedKeyUsage() {
        ArrayList arrayList = new ArrayList(this.keyUsages.size());
        Iterator<ObjectIdentifier> it = this.keyUsages.iterator();
        while (it.hasNext()) {
            arrayList.add(it.next().toString());
        }
        return arrayList;
    }
}
