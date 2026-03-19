package android.nfc;

import android.nfc.INfcTag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcB;
import android.nfc.tech.NfcBarcode;
import android.nfc.tech.NfcF;
import android.nfc.tech.NfcV;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

public final class Tag implements Parcelable {
    public static final Parcelable.Creator<Tag> CREATOR = new Parcelable.Creator<Tag>() {
        @Override
        public Tag createFromParcel(Parcel parcel) {
            INfcTag iNfcTagAsInterface;
            byte[] bytesWithNull = Tag.readBytesWithNull(parcel);
            int[] iArr = new int[parcel.readInt()];
            parcel.readIntArray(iArr);
            Bundle[] bundleArr = (Bundle[]) parcel.createTypedArray(Bundle.CREATOR);
            int i = parcel.readInt();
            if (parcel.readInt() == 0) {
                iNfcTagAsInterface = INfcTag.Stub.asInterface(parcel.readStrongBinder());
            } else {
                iNfcTagAsInterface = null;
            }
            return new Tag(bytesWithNull, iArr, bundleArr, i, iNfcTagAsInterface);
        }

        @Override
        public Tag[] newArray(int i) {
            return new Tag[i];
        }
    };
    int mConnectedTechnology;
    final byte[] mId;
    final int mServiceHandle;
    final INfcTag mTagService;
    final Bundle[] mTechExtras;
    final int[] mTechList;
    final String[] mTechStringList;

    public Tag(byte[] bArr, int[] iArr, Bundle[] bundleArr, int i, INfcTag iNfcTag) {
        if (iArr == null) {
            throw new IllegalArgumentException("rawTargets cannot be null");
        }
        this.mId = bArr;
        this.mTechList = Arrays.copyOf(iArr, iArr.length);
        this.mTechStringList = generateTechStringList(iArr);
        this.mTechExtras = (Bundle[]) Arrays.copyOf(bundleArr, iArr.length);
        this.mServiceHandle = i;
        this.mTagService = iNfcTag;
        this.mConnectedTechnology = -1;
    }

    public static Tag createMockTag(byte[] bArr, int[] iArr, Bundle[] bundleArr) {
        return new Tag(bArr, iArr, bundleArr, 0, null);
    }

    private String[] generateTechStringList(int[] iArr) {
        int length = iArr.length;
        String[] strArr = new String[length];
        for (int i = 0; i < length; i++) {
            switch (iArr[i]) {
                case 1:
                    strArr[i] = NfcA.class.getName();
                    break;
                case 2:
                    strArr[i] = NfcB.class.getName();
                    break;
                case 3:
                    strArr[i] = IsoDep.class.getName();
                    break;
                case 4:
                    strArr[i] = NfcF.class.getName();
                    break;
                case 5:
                    strArr[i] = NfcV.class.getName();
                    break;
                case 6:
                    strArr[i] = Ndef.class.getName();
                    break;
                case 7:
                    strArr[i] = NdefFormatable.class.getName();
                    break;
                case 8:
                    strArr[i] = MifareClassic.class.getName();
                    break;
                case 9:
                    strArr[i] = MifareUltralight.class.getName();
                    break;
                case 10:
                    strArr[i] = NfcBarcode.class.getName();
                    break;
                default:
                    throw new IllegalArgumentException("Unknown tech type " + iArr[i]);
            }
        }
        return strArr;
    }

    static int[] getTechCodesFromStrings(String[] strArr) throws IllegalArgumentException {
        if (strArr == null) {
            throw new IllegalArgumentException("List cannot be null");
        }
        int[] iArr = new int[strArr.length];
        HashMap<String, Integer> techStringToCodeMap = getTechStringToCodeMap();
        for (int i = 0; i < strArr.length; i++) {
            Integer num = techStringToCodeMap.get(strArr[i]);
            if (num == null) {
                throw new IllegalArgumentException("Unknown tech type " + strArr[i]);
            }
            iArr[i] = num.intValue();
        }
        return iArr;
    }

    private static HashMap<String, Integer> getTechStringToCodeMap() {
        HashMap<String, Integer> map = new HashMap<>();
        map.put(IsoDep.class.getName(), 3);
        map.put(MifareClassic.class.getName(), 8);
        map.put(MifareUltralight.class.getName(), 9);
        map.put(Ndef.class.getName(), 6);
        map.put(NdefFormatable.class.getName(), 7);
        map.put(NfcA.class.getName(), 1);
        map.put(NfcB.class.getName(), 2);
        map.put(NfcF.class.getName(), 4);
        map.put(NfcV.class.getName(), 5);
        map.put(NfcBarcode.class.getName(), 10);
        return map;
    }

    public int getServiceHandle() {
        return this.mServiceHandle;
    }

    public int[] getTechCodeList() {
        return this.mTechList;
    }

    public byte[] getId() {
        return this.mId;
    }

    public String[] getTechList() {
        return this.mTechStringList;
    }

    public Tag rediscover() throws IOException {
        if (getConnectedTechnology() != -1) {
            throw new IllegalStateException("Close connection to the technology first!");
        }
        if (this.mTagService == null) {
            throw new IOException("Mock tags don't support this operation.");
        }
        try {
            Tag tagRediscover = this.mTagService.rediscover(getServiceHandle());
            if (tagRediscover != null) {
                return tagRediscover;
            }
            throw new IOException("Failed to rediscover tag");
        } catch (RemoteException e) {
            throw new IOException("NFC service dead");
        }
    }

    public boolean hasTech(int i) {
        for (int i2 : this.mTechList) {
            if (i2 == i) {
                return true;
            }
        }
        return false;
    }

    public Bundle getTechExtras(int i) {
        int i2 = 0;
        while (true) {
            if (i2 < this.mTechList.length) {
                if (this.mTechList[i2] == i) {
                    break;
                }
                i2++;
            } else {
                i2 = -1;
                break;
            }
        }
        if (i2 < 0) {
            return null;
        }
        return this.mTechExtras[i2];
    }

    public INfcTag getTagService() {
        return this.mTagService;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("TAG: Tech [");
        String[] techList = getTechList();
        int length = techList.length;
        for (int i = 0; i < length; i++) {
            sb.append(techList[i]);
            if (i < length - 1) {
                sb.append(", ");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    static byte[] readBytesWithNull(Parcel parcel) {
        int i = parcel.readInt();
        if (i >= 0) {
            byte[] bArr = new byte[i];
            parcel.readByteArray(bArr);
            return bArr;
        }
        return null;
    }

    static void writeBytesWithNull(Parcel parcel, byte[] bArr) {
        if (bArr == null) {
            parcel.writeInt(-1);
        } else {
            parcel.writeInt(bArr.length);
            parcel.writeByteArray(bArr);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        int i2 = this.mTagService == null ? 1 : 0;
        writeBytesWithNull(parcel, this.mId);
        parcel.writeInt(this.mTechList.length);
        parcel.writeIntArray(this.mTechList);
        parcel.writeTypedArray(this.mTechExtras, 0);
        parcel.writeInt(this.mServiceHandle);
        parcel.writeInt(i2);
        if (i2 == 0) {
            parcel.writeStrongBinder(this.mTagService.asBinder());
        }
    }

    public synchronized void setConnectedTechnology(int i) {
        if (this.mConnectedTechnology == -1) {
            this.mConnectedTechnology = i;
        } else {
            throw new IllegalStateException("Close other technology first!");
        }
    }

    public int getConnectedTechnology() {
        return this.mConnectedTechnology;
    }

    public void setTechnologyDisconnected() {
        this.mConnectedTechnology = -1;
    }
}
