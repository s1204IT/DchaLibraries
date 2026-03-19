package com.mediatek.internal.telephony;

import android.R;
import android.content.res.Resources;
import android.os.AsyncResult;
import com.android.internal.telephony.HardwareConfig;
import com.android.internal.telephony.TelephonyDevController;
import java.util.List;

public class MtkTelephonyDevController extends TelephonyDevController {
    protected static final String LOG_TAG = "MtkTDC";

    public MtkTelephonyDevController() {
        logd("MtkTelephonyDevController constructor");
    }

    protected void initFromResource() {
        String[] stringArray = Resources.getSystem().getStringArray(R.array.config_deviceStatesOnWhichToWakeUp);
        if (stringArray != null) {
            for (String str : stringArray) {
                MtkHardwareConfig mtkHardwareConfig = new MtkHardwareConfig(str);
                if (((HardwareConfig) mtkHardwareConfig).type == 0) {
                    updateOrInsert(mtkHardwareConfig, mModems);
                } else if (((HardwareConfig) mtkHardwareConfig).type == 1) {
                    updateOrInsert(mtkHardwareConfig, mSims);
                }
            }
        }
    }

    protected void handleGetHardwareConfigChanged(AsyncResult asyncResult) {
        if (asyncResult.exception == null && asyncResult.result != null) {
            List list = (List) asyncResult.result;
            for (int i = 0; i < list.size(); i++) {
                HardwareConfig hardwareConfig = (HardwareConfig) list.get(i);
                if (hardwareConfig != null) {
                    String str = hardwareConfig.type + "," + hardwareConfig.uuid + "," + hardwareConfig.state;
                    MtkHardwareConfig mtkHardwareConfig = new MtkHardwareConfig(hardwareConfig.type == 0 ? str + "," + hardwareConfig.rilModel + ",0," + hardwareConfig.maxActiveVoiceCall + "," + hardwareConfig.maxActiveDataCall + "," + hardwareConfig.maxStandby : str + "," + hardwareConfig.modemUuid);
                    if (mtkHardwareConfig.type == 0) {
                        updateOrInsert(mtkHardwareConfig, mModems);
                    } else if (hardwareConfig.type == 1) {
                        updateOrInsert(mtkHardwareConfig, mSims);
                    }
                }
            }
            return;
        }
        loge("handleGetHardwareConfigChanged - returned an error.");
    }
}
