package android.hardware.radio.V1_0;

import com.android.internal.telephony.IccCardConstants;
import java.util.ArrayList;

public final class CardState {
    public static final int ABSENT = 0;
    public static final int ERROR = 2;
    public static final int PRESENT = 1;
    public static final int RESTRICTED = 3;

    public static final String toString(int i) {
        if (i == 0) {
            return IccCardConstants.INTENT_VALUE_ICC_ABSENT;
        }
        if (i == 1) {
            return IccCardConstants.INTENT_VALUE_ICC_PRESENT;
        }
        if (i == 2) {
            return "ERROR";
        }
        if (i == 3) {
            return "RESTRICTED";
        }
        return "0x" + Integer.toHexString(i);
    }

    public static final String dumpBitfield(int i) {
        ArrayList arrayList = new ArrayList();
        arrayList.add(IccCardConstants.INTENT_VALUE_ICC_ABSENT);
        int i2 = 1;
        if ((i & 1) == 1) {
            arrayList.add(IccCardConstants.INTENT_VALUE_ICC_PRESENT);
        } else {
            i2 = 0;
        }
        if ((i & 2) == 2) {
            arrayList.add("ERROR");
            i2 |= 2;
        }
        if ((i & 3) == 3) {
            arrayList.add("RESTRICTED");
            i2 |= 3;
        }
        if (i != i2) {
            arrayList.add("0x" + Integer.toHexString(i & (~i2)));
        }
        return String.join(" | ", arrayList);
    }
}
