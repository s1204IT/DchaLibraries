package com.mediatek.internal.telephony.phb;

import android.os.SystemProperties;
import com.mediatek.internal.telephony.datasub.DataSubConstants;

public class PhbEntry {
    private static final boolean isUserLoad = SystemProperties.get("ro.build.type").equals(DataSubConstants.REASON_MOBILE_DATA_ENABLE_USER);
    public String alphaId;
    public int index;
    public String number;
    public int ton;
    public int type;

    public String toString() {
        if (isUserLoad) {
            return "type: " + this.type + " index: " + this.index + " number: xxxxx ton: " + this.ton + " alphaId: " + this.alphaId;
        }
        return "type: " + this.type + " index: " + this.index + " number: " + this.number + " ton: " + this.ton + " alphaId: " + this.alphaId;
    }
}
