package com.mediatek.internal.telephony.dataconnection;

import android.os.AsyncResult;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.dataconnection.ApnSetting;
import com.android.internal.telephony.dataconnection.DcFailCause;
import java.util.ArrayList;

public interface IDataConnectionExt {
    long getDisconnectDoneRetryTimer(String str, long j);

    String getOperatorNumericFromImpi(String str, int i);

    void handlePcoDataAfterAttached(AsyncResult asyncResult, Phone phone, ArrayList<ApnSetting> arrayList);

    boolean ignoreDataRoaming(String str);

    boolean ignoreDefaultDataUnselected(String str);

    boolean isDataAllowedAsOff(String str);

    boolean isDomesticRoamingEnabled();

    boolean isFdnEnableSupport();

    boolean isIgnoredCause(DcFailCause dcFailCause);

    boolean isMeteredApnType(String str, boolean z);

    boolean isMeteredApnTypeByLoad();

    boolean isOnlySingleDcAllowed();

    boolean isSmartDataSwtichAllowed();

    void onDcActivated(String[] strArr, String str);

    void onDcDeactivated(String[] strArr, String str);

    void startDataRoamingStrategy(Phone phone);

    void stopDataRoamingStrategy();
}
