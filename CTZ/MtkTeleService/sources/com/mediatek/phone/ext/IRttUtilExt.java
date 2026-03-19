package com.mediatek.phone.ext;

import android.os.Bundle;
import com.android.internal.telephony.Connection;

public interface IRttUtilExt {
    boolean isRttCallAndNotAllowMerge(Connection connection);

    void onStopRtt(boolean z, Connection connection);

    void setIncomingRttCall(Connection connection, Bundle bundle);

    boolean setTelephonyConnectionRttStatus(boolean z);

    boolean updateConnectionProperties();

    int updatePropertyRtt(int i, boolean z, int i2);
}
