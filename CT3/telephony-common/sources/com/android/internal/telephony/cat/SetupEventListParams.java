package com.android.internal.telephony.cat;

class SetupEventListParams extends CommandParams {
    byte[] eventList;

    SetupEventListParams(CommandDetails cmdDet, byte[] eventList) {
        super(cmdDet);
        this.eventList = eventList;
    }
}
