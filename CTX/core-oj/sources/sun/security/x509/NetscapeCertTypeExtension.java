package sun.security.x509;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Vector;
import sun.security.util.BitArray;
import sun.security.util.DerOutputStream;
import sun.security.util.DerValue;
import sun.security.util.ObjectIdentifier;

public class NetscapeCertTypeExtension extends Extension implements CertAttrSet<String> {
    private static final int[] CertType_data = {2, 16, 840, 1, 113730, 1, 1};
    public static final String IDENT = "x509.info.extensions.NetscapeCertType";
    public static final String NAME = "NetscapeCertType";
    public static ObjectIdentifier NetscapeCertType_Id = null;
    public static final String OBJECT_SIGNING = "object_signing";
    public static final String OBJECT_SIGNING_CA = "object_signing_ca";
    public static final String SSL_CA = "ssl_ca";
    public static final String SSL_CLIENT = "ssl_client";
    public static final String SSL_SERVER = "ssl_server";
    public static final String S_MIME = "s_mime";
    public static final String S_MIME_CA = "s_mime_ca";
    private static final Vector<String> mAttributeNames;
    private static MapEntry[] mMapData;
    private boolean[] bitString;

    static {
        try {
            NetscapeCertType_Id = new ObjectIdentifier(CertType_data);
        } catch (IOException e) {
        }
        mMapData = new MapEntry[]{new MapEntry(SSL_CLIENT, 0), new MapEntry(SSL_SERVER, 1), new MapEntry(S_MIME, 2), new MapEntry(OBJECT_SIGNING, 3), new MapEntry(SSL_CA, 5), new MapEntry(S_MIME_CA, 6), new MapEntry(OBJECT_SIGNING_CA, 7)};
        mAttributeNames = new Vector<>();
        for (MapEntry mapEntry : mMapData) {
            mAttributeNames.add(mapEntry.mName);
        }
    }

    private static class MapEntry {
        String mName;
        int mPosition;

        MapEntry(String str, int i) {
            this.mName = str;
            this.mPosition = i;
        }
    }

    private static int getPosition(String str) throws IOException {
        for (int i = 0; i < mMapData.length; i++) {
            if (str.equalsIgnoreCase(mMapData[i].mName)) {
                return mMapData[i].mPosition;
            }
        }
        throw new IOException("Attribute name [" + str + "] not recognized by CertAttrSet:NetscapeCertType.");
    }

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

    public NetscapeCertTypeExtension(byte[] bArr) throws IOException {
        this.bitString = new BitArray(bArr.length * 8, bArr).toBooleanArray();
        this.extensionId = NetscapeCertType_Id;
        this.critical = true;
        encodeThis();
    }

    public NetscapeCertTypeExtension(boolean[] zArr) throws IOException {
        this.bitString = zArr;
        this.extensionId = NetscapeCertType_Id;
        this.critical = true;
        encodeThis();
    }

    public NetscapeCertTypeExtension(Boolean bool, Object obj) throws IOException {
        this.extensionId = NetscapeCertType_Id;
        this.critical = bool.booleanValue();
        this.extensionValue = (byte[]) obj;
        this.bitString = new DerValue(this.extensionValue).getUnalignedBitString().toBooleanArray();
    }

    public NetscapeCertTypeExtension() {
        this.extensionId = NetscapeCertType_Id;
        this.critical = true;
        this.bitString = new boolean[0];
    }

    @Override
    public void set(String str, Object obj) throws IOException {
        if (!(obj instanceof Boolean)) {
            throw new IOException("Attribute must be of type Boolean.");
        }
        set(getPosition(str), ((Boolean) obj).booleanValue());
        encodeThis();
    }

    @Override
    public Boolean get(String str) throws IOException {
        return Boolean.valueOf(isSet(getPosition(str)));
    }

    @Override
    public void delete(String str) throws IOException {
        set(getPosition(str), false);
        encodeThis();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString());
        sb.append("NetscapeCertType [\n");
        if (isSet(0)) {
            sb.append("   SSL client\n");
        }
        if (isSet(1)) {
            sb.append("   SSL server\n");
        }
        if (isSet(2)) {
            sb.append("   S/MIME\n");
        }
        if (isSet(3)) {
            sb.append("   Object Signing\n");
        }
        if (isSet(5)) {
            sb.append("   SSL CA\n");
        }
        if (isSet(6)) {
            sb.append("   S/MIME CA\n");
        }
        if (isSet(7)) {
            sb.append("   Object Signing CA");
        }
        sb.append("]\n");
        return sb.toString();
    }

    @Override
    public void encode(OutputStream outputStream) throws IOException {
        DerOutputStream derOutputStream = new DerOutputStream();
        if (this.extensionValue == null) {
            this.extensionId = NetscapeCertType_Id;
            this.critical = true;
            encodeThis();
        }
        super.encode(derOutputStream);
        outputStream.write(derOutputStream.toByteArray());
    }

    @Override
    public Enumeration<String> getElements() {
        return mAttributeNames.elements();
    }

    @Override
    public String getName() {
        return NAME;
    }

    public boolean[] getKeyUsageMappedBits() {
        KeyUsageExtension keyUsageExtension = new KeyUsageExtension();
        Boolean bool = Boolean.TRUE;
        try {
            if (isSet(getPosition(SSL_CLIENT)) || isSet(getPosition(S_MIME)) || isSet(getPosition(OBJECT_SIGNING))) {
                keyUsageExtension.set(KeyUsageExtension.DIGITAL_SIGNATURE, bool);
            }
            if (isSet(getPosition(SSL_SERVER))) {
                keyUsageExtension.set(KeyUsageExtension.KEY_ENCIPHERMENT, bool);
            }
            if (isSet(getPosition(SSL_CA)) || isSet(getPosition(S_MIME_CA)) || isSet(getPosition(OBJECT_SIGNING_CA))) {
                keyUsageExtension.set(KeyUsageExtension.KEY_CERTSIGN, bool);
            }
        } catch (IOException e) {
        }
        return keyUsageExtension.getBits();
    }
}
