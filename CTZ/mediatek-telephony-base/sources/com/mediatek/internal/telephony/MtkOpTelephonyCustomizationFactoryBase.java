package com.mediatek.internal.telephony;

import mediatek.telephony.ISignalStrengthExt;

public class MtkOpTelephonyCustomizationFactoryBase {
    public IMtkCallerInfoExt makeMtkCallerInfoExt() {
        return new DefaultMtkCallerInfoExt();
    }

    public ISignalStrengthExt makeSignalStrengthExt() {
        return new SignalStrengthExt();
    }
}
