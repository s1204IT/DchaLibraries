package com.android.internal.telephony.cat;

class SetEventListParams extends CommandParams {
    int[] mEventInfo;

    SetEventListParams(CommandDetails cmdDet, int[] eventInfo) {
        super(cmdDet);
        this.mEventInfo = eventInfo;
    }
}
