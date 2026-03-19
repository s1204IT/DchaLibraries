package com.android.internal.telephony.uicc.euicc;

import android.telephony.Rlog;
import com.android.internal.telephony.uicc.asn1.Asn1Decoder;
import com.android.internal.telephony.uicc.asn1.Asn1Node;
import com.android.internal.telephony.uicc.asn1.InvalidAsn1DataException;
import com.android.internal.telephony.uicc.asn1.TagNotFoundException;
import java.util.Arrays;

public final class EuiccSpecVersion implements Comparable<EuiccSpecVersion> {
    private static final String LOG_TAG = "EuiccSpecVer";
    private static final int TAG_ISD_R_APP_TEMPLATE = 224;
    private static final int TAG_VERSION = 130;
    private final int[] mVersionValues;

    public static EuiccSpecVersion fromOpenChannelResponse(byte[] bArr) {
        byte[] bArrAsBytes;
        try {
            Asn1Decoder asn1Decoder = new Asn1Decoder(bArr);
            if (!asn1Decoder.hasNextNode()) {
                return null;
            }
            Asn1Node asn1NodeNextNode = asn1Decoder.nextNode();
            try {
                if (asn1NodeNextNode.getTag() == 224) {
                    bArrAsBytes = asn1NodeNextNode.getChild(130, new int[0]).asBytes();
                } else {
                    bArrAsBytes = asn1NodeNextNode.getChild(224, new int[]{130}).asBytes();
                }
            } catch (InvalidAsn1DataException | TagNotFoundException e) {
                Rlog.e(LOG_TAG, "Cannot parse select response of ISD-R: " + asn1NodeNextNode.toHex());
            }
            if (bArrAsBytes.length == 3) {
                return new EuiccSpecVersion(bArrAsBytes);
            }
            Rlog.e(LOG_TAG, "Cannot parse select response of ISD-R: " + asn1NodeNextNode.toHex());
            return null;
        } catch (InvalidAsn1DataException e2) {
            Rlog.e(LOG_TAG, "Cannot parse the select response of ISD-R.", e2);
            return null;
        }
    }

    public EuiccSpecVersion(int i, int i2, int i3) {
        this.mVersionValues = new int[3];
        this.mVersionValues[0] = i;
        this.mVersionValues[1] = i2;
        this.mVersionValues[2] = i3;
    }

    public EuiccSpecVersion(byte[] bArr) {
        this.mVersionValues = new int[3];
        this.mVersionValues[0] = bArr[0] & 255;
        this.mVersionValues[1] = bArr[1] & 255;
        this.mVersionValues[2] = bArr[2] & 255;
    }

    public int getMajor() {
        return this.mVersionValues[0];
    }

    public int getMinor() {
        return this.mVersionValues[1];
    }

    public int getRevision() {
        return this.mVersionValues[2];
    }

    @Override
    public int compareTo(EuiccSpecVersion euiccSpecVersion) {
        if (getMajor() > euiccSpecVersion.getMajor()) {
            return 1;
        }
        if (getMajor() < euiccSpecVersion.getMajor()) {
            return -1;
        }
        if (getMinor() > euiccSpecVersion.getMinor()) {
            return 1;
        }
        if (getMinor() < euiccSpecVersion.getMinor()) {
            return -1;
        }
        if (getRevision() > euiccSpecVersion.getRevision()) {
            return 1;
        }
        return getRevision() < euiccSpecVersion.getRevision() ? -1 : 0;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        return Arrays.equals(this.mVersionValues, ((EuiccSpecVersion) obj).mVersionValues);
    }

    public int hashCode() {
        return Arrays.hashCode(this.mVersionValues);
    }

    public String toString() {
        return this.mVersionValues[0] + "." + this.mVersionValues[1] + "." + this.mVersionValues[2];
    }
}
