package com.mediatek.internal.telephony.devreg;

import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.Phone;

public class DeviceRegisterHandler extends Handler {
    private static final int EVENT_CDMA_CARD_INITIAL_ESN_OR_MEID = 107;
    private final CommandsInterface mCi;
    private final DeviceRegisterController mController;
    private final Phone mPhone;

    public DeviceRegisterHandler(Phone phone, DeviceRegisterController deviceRegisterController) {
        this.mPhone = phone;
        this.mCi = phone.mCi;
        this.mController = deviceRegisterController;
        this.mCi.setCDMACardInitalEsnMeid(this, EVENT_CDMA_CARD_INITIAL_ESN_OR_MEID, null);
    }

    @Override
    public void handleMessage(Message message) {
        if (message.what == EVENT_CDMA_CARD_INITIAL_ESN_OR_MEID) {
            AsyncResult asyncResult = (AsyncResult) message.obj;
            if (asyncResult != null && asyncResult.exception == null && asyncResult.result != null) {
                try {
                    this.mController.setCdmaCardEsnOrMeid((String) asyncResult.result);
                    return;
                } catch (ClassCastException e) {
                    return;
                }
            }
            return;
        }
        super.handleMessage(message);
    }
}
