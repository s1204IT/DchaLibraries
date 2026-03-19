package com.mediatek.internal.telephony;

import android.content.Context;
import android.telephony.Rlog;
import com.android.internal.telephony.CommandsInterface;
import com.mediatek.internal.telephony.dataconnection.DataConnectionExt;
import com.mediatek.internal.telephony.dataconnection.IDataConnectionExt;
import com.mediatek.internal.telephony.datasub.DataSubSelectorOpExt;
import com.mediatek.internal.telephony.datasub.IDataSubSelectorOPExt;
import com.mediatek.internal.telephony.datasub.ISimSwitchForDSSExt;
import com.mediatek.internal.telephony.datasub.SimSwitchForDSSExt;
import com.mediatek.internal.telephony.devreg.DefaultDeviceRegisterExt;
import com.mediatek.internal.telephony.devreg.DeviceRegisterController;
import com.mediatek.internal.telephony.devreg.IDeviceRegisterExt;
import com.mediatek.internal.telephony.digits.DigitsUssdManager;
import com.mediatek.internal.telephony.digits.DigitsUssdManagerBase;
import com.mediatek.internal.telephony.digits.DigitsUtil;
import com.mediatek.internal.telephony.digits.DigitsUtilBase;
import com.mediatek.internal.telephony.imsphone.op.OpImsPhoneCallTracker;
import com.mediatek.internal.telephony.imsphone.op.OpImsPhoneCallTrackerBase;
import com.mediatek.internal.telephony.selfactivation.ISelfActivation;
import com.mediatek.internal.telephony.selfactivation.SelfActivationDefault;
import com.mediatek.internal.telephony.uicc.IMtkSimHandler;
import com.mediatek.internal.telephony.uicc.MtkSimHandler;

public class OpTelephonyCustomizationFactoryBase {
    public IServiceStateTrackerExt makeServiceStateTrackerExt(Context context) {
        return new ServiceStateTrackerExt(context);
    }

    public IMtkDupSmsFilter makeMtkDupSmsFilter(Context context) {
        return new MtkDupSmsFilter(context);
    }

    public IMtkConcatenatedSmsFwk makeMtkConcatenatedSmsFwk(Context context) {
        return new MtkConcatenatedSmsFwk(context);
    }

    public IDataConnectionExt makeDataConnectionExt(Context context) {
        return new DataConnectionExt(context);
    }

    public IDefaultTelephonyExt makeTelephonyExt(Context context) {
        return new DefaultTelephonyExt(context);
    }

    public IDeviceRegisterExt makeDeviceRegisterExt(Context context, DeviceRegisterController deviceRegisterController) {
        return new DefaultDeviceRegisterExt(context, deviceRegisterController);
    }

    public ISelfActivation makeSelfActivationInstance(int i) {
        return new SelfActivationDefault(i);
    }

    public IDataSubSelectorOPExt makeDataSubSelectorOPExt(Context context) {
        return new DataSubSelectorOpExt(context);
    }

    public ISimSwitchForDSSExt makeSimSwitchForDSSOPExt(Context context) {
        return new SimSwitchForDSSExt(context);
    }

    public IMtkGsmCdmaCallTrackerExt makeMtkGsmCdmaCallTrackerExt(Context context) {
        return new MtkGsmCdmaCallTrackerExt(context);
    }

    public DigitsUtil makeDigitsUtil() {
        return new DigitsUtilBase();
    }

    public DigitsUssdManager makeDigitsUssdManager() {
        return new DigitsUssdManagerBase();
    }

    public IMtkSimHandler makeMtkSimHandler(Context context, CommandsInterface commandsInterface) {
        Rlog.d("makeMtkSimHandler", "[makeMtkSimHandler] ");
        return new MtkSimHandler(context, commandsInterface);
    }

    public OpImsPhoneCallTracker makeOpImsPhoneCallTracker() {
        return new OpImsPhoneCallTrackerBase();
    }

    public IMtkProxyControllerExt makeMtkProxyControllerExt(Context context) {
        return new MtkProxyControllerExt(context);
    }
}
