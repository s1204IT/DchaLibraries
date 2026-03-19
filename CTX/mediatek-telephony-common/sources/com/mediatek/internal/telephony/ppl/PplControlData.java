package com.mediatek.internal.telephony.ppl;

import android.util.Log;
import com.mediatek.internal.telephony.ppl.PplMessageManager;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class PplControlData {
    private static final int HEADER_SIZE = 48;
    public static final int SALT_LIST_LENGTH = 40;
    public static final int SALT_SIZE = 20;
    public static final int SECRET_LIST_LENGTH = 40;
    public static final int SECRET_SIZE = 20;
    public static final int SIM_FINGERPRINT_LENGTH = 40;
    public static final byte STATUS_ENABLED = 2;
    public static final byte STATUS_LOCKED = 4;
    public static final byte STATUS_PROVISIONED = 1;
    public static final byte STATUS_SIM_LOCKED = 8;
    public static final byte STATUS_WIPE_REQUESTED = 16;
    private static final String TAG = "PPL/ControlData";
    public static final int TRUSTED_NUMBER_LENGTH = 40;
    public static final byte VERSION = 1;
    private static Comparator<byte[]> mSIMComparator = new Comparator<byte[]>() {
        @Override
        public int compare(byte[] bArr, byte[] bArr2) {
            return PplControlData.compareSIMFingerprints(bArr, bArr2);
        }
    };
    public byte version = 1;
    public byte status = 0;
    public byte[] secret = new byte[20];
    public byte[] salt = new byte[20];
    public List<byte[]> SIMFingerprintList = null;
    public List<String> TrustedNumberList = null;
    public List<PplMessageManager.PendingMessage> PendingMessageList = null;

    public byte[] encode() {
        byte[] bArr = new byte[getDataSize()];
        bArr[0] = this.version;
        bArr[1] = this.status;
        bArr[2] = this.SIMFingerprintList == null ? (byte) 0 : (byte) this.SIMFingerprintList.size();
        bArr[3] = this.TrustedNumberList == null ? (byte) 0 : (byte) this.TrustedNumberList.size();
        bArr[4] = this.PendingMessageList == null ? (byte) 0 : (byte) this.PendingMessageList.size();
        bArr[5] = 0;
        bArr[6] = 0;
        bArr[7] = 0;
        System.arraycopy(this.secret, 0, bArr, 8, this.secret.length);
        int length = 8 + this.secret.length;
        System.arraycopy(this.salt, 0, bArr, length, this.salt.length);
        int length2 = length + this.salt.length;
        if (this.SIMFingerprintList != null) {
            int i = length2;
            for (int i2 = 0; i2 < this.SIMFingerprintList.size(); i2++) {
                System.arraycopy(this.SIMFingerprintList.get(i2), 0, bArr, i, 40);
                i += 40;
            }
            length2 = i;
        }
        if (this.TrustedNumberList != null) {
            int i3 = length2;
            for (int i4 = 0; i4 < this.TrustedNumberList.size(); i4++) {
                byte[] bytes = this.TrustedNumberList.get(i4).getBytes();
                if (bytes.length > 40) {
                    throw new Error("Trusted number is too long");
                }
                System.arraycopy(Arrays.copyOf(bytes, 40), 0, bArr, i3, 40);
                i3 += 40;
            }
            length2 = i3;
        }
        if (this.PendingMessageList != null) {
            for (int i5 = 0; i5 < this.PendingMessageList.size(); i5++) {
                this.PendingMessageList.get(i5).encode(bArr, length2);
                length2 += 49;
            }
        }
        return bArr;
    }

    public void decode(byte[] bArr) {
        int i;
        this.version = bArr[0];
        this.status = bArr[1];
        byte b = bArr[2];
        byte b2 = bArr[3];
        byte b3 = bArr[4];
        System.arraycopy(bArr, 8, this.secret, 0, this.secret.length);
        int length = 8 + this.secret.length;
        System.arraycopy(bArr, length, this.salt, 0, this.salt.length);
        int length2 = length + this.salt.length;
        if (b != 0) {
            this.SIMFingerprintList = new LinkedList();
            for (int i2 = 0; i2 < b; i2++) {
                byte[] bArr2 = new byte[40];
                System.arraycopy(bArr, length2, bArr2, 0, 40);
                this.SIMFingerprintList.add(bArr2);
                length2 += 40;
            }
        } else {
            this.SIMFingerprintList = null;
        }
        if (b2 != 0) {
            this.TrustedNumberList = new LinkedList();
            int i3 = 0;
            while (i3 < b2) {
                int i4 = length2;
                while (true) {
                    i = length2 + 40;
                    if (i4 >= i || bArr[i4] == 0) {
                        break;
                    } else {
                        i4++;
                    }
                }
                this.TrustedNumberList.add(new String(bArr, length2, i4 - length2));
                i3++;
                length2 = i;
            }
        } else {
            this.TrustedNumberList = null;
        }
        if (b3 != 0) {
            this.PendingMessageList = new LinkedList();
            for (int i5 = 0; i5 < b3; i5++) {
                this.PendingMessageList.add(new PplMessageManager.PendingMessage(bArr, length2));
                length2 += 49;
            }
            return;
        }
        this.PendingMessageList = null;
    }

    private int getDataSize() {
        int size = this.SIMFingerprintList != null ? 48 + (this.SIMFingerprintList.size() * 40) : 48;
        if (this.TrustedNumberList != null) {
            size += 40 * this.TrustedNumberList.size();
        }
        if (this.PendingMessageList != null) {
            return size + (49 * this.PendingMessageList.size());
        }
        return size;
    }

    public static PplControlData buildControlData(byte[] bArr) {
        PplControlData pplControlData = new PplControlData();
        if (bArr != null && bArr.length != 0) {
            pplControlData.decode(bArr);
        } else {
            Log.w(TAG, "buildControlData: data is empty, return empty instance");
        }
        return pplControlData;
    }

    public PplControlData m3clone() {
        PplControlData pplControlData = new PplControlData();
        pplControlData.version = this.version;
        pplControlData.status = this.status;
        pplControlData.secret = (byte[]) this.secret.clone();
        pplControlData.salt = (byte[]) this.salt.clone();
        if (this.SIMFingerprintList != null) {
            pplControlData.SIMFingerprintList = new LinkedList();
            for (int i = 0; i < this.SIMFingerprintList.size(); i++) {
                pplControlData.SIMFingerprintList.add((byte[]) this.SIMFingerprintList.get(i).clone());
            }
        } else {
            pplControlData.SIMFingerprintList = null;
        }
        if (this.TrustedNumberList != null) {
            pplControlData.TrustedNumberList = new LinkedList();
            Iterator<String> it = this.TrustedNumberList.iterator();
            while (it.hasNext()) {
                pplControlData.TrustedNumberList.add(it.next());
            }
        } else {
            pplControlData.TrustedNumberList = null;
        }
        if (this.PendingMessageList != null) {
            pplControlData.PendingMessageList = new LinkedList();
            Iterator<PplMessageManager.PendingMessage> it2 = this.PendingMessageList.iterator();
            while (it2.hasNext()) {
                pplControlData.PendingMessageList.add(it2.next().m4clone());
            }
        } else {
            this.PendingMessageList = null;
        }
        return pplControlData;
    }

    public void clear() {
        this.version = (byte) 1;
        this.status = (byte) 0;
        this.secret = new byte[20];
        this.salt = new byte[20];
        this.SIMFingerprintList = null;
        this.TrustedNumberList = null;
        this.PendingMessageList = null;
    }

    public boolean isEnabled() {
        return (this.status & 2) == 2;
    }

    public void setEnable(boolean z) {
        if (z) {
            this.status = (byte) (this.status | 2);
        } else {
            this.status = (byte) (this.status & (-3));
        }
    }

    public boolean hasWipeFlag() {
        return (this.status & STATUS_WIPE_REQUESTED) == 16;
    }

    public void setWipeFlag(boolean z) {
        if (z) {
            this.status = (byte) (this.status | STATUS_WIPE_REQUESTED);
        } else {
            this.status = (byte) (this.status & (-17));
        }
    }

    public boolean isProvisioned() {
        return (this.status & 1) == 1;
    }

    public void setProvision(boolean z) {
        if (z) {
            this.status = (byte) (this.status | 1);
        } else {
            this.status = (byte) (this.status & (-2));
        }
    }

    public boolean isLocked() {
        return (this.status & 4) == 4;
    }

    public void setLock(boolean z) {
        if (z) {
            this.status = (byte) (this.status | 4);
        } else {
            this.status = (byte) (this.status & (-5));
        }
    }

    public boolean isSIMLocked() {
        return (this.status & 8) == 8;
    }

    public void setSIMLock(boolean z) {
        if (z) {
            this.status = (byte) (this.status | 8);
        } else {
            this.status = (byte) (this.status & (-9));
        }
    }

    public static byte[][] sortSIMFingerprints(byte[][] bArr) {
        byte[][] bArr2 = (byte[][]) bArr.clone();
        for (int i = 0; i < bArr2.length; i++) {
            bArr2[i] = (byte[]) bArr2[i].clone();
        }
        Arrays.sort(bArr2, mSIMComparator);
        return bArr2;
    }

    public static int compareSIMFingerprints(byte[] bArr, byte[] bArr2) {
        if (bArr.length != bArr2.length) {
            throw new Error("The two fingerprints must have the same length");
        }
        for (int i = 0; i < bArr.length; i++) {
            int i2 = bArr[i] - bArr2[i];
            if (i2 != 0) {
                return i2;
            }
        }
        return 0;
    }

    public String toString() {
        return "PplControlData " + hashCode() + " {" + Integer.toHexString(this.version) + ", " + Integer.toHexString(this.status) + ", " + this.SIMFingerprintList + ", " + this.TrustedNumberList + ", " + this.PendingMessageList + "}";
    }
}
