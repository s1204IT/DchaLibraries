package com.mediatek.internal.telephony.uicc;

import android.os.AsyncResult;
import android.os.Message;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.uicc.UiccCarrierPrivilegeRules;
import com.android.internal.telephony.uicc.UiccProfile;

public class MtkUiccCarrierPrivilegeRules extends UiccCarrierPrivilegeRules {
    public MtkUiccCarrierPrivilegeRules(UiccProfile uiccProfile, Message message) {
        super(uiccProfile, message);
    }

    public void handleMessage(Message message) {
        if (message.what == 1) {
            log("M: EVENT_OPEN_LOGICAL_CHANNEL_DONE");
            AsyncResult asyncResult = (AsyncResult) message.obj;
            if (asyncResult.exception != null || asyncResult.result == null) {
                if ((asyncResult.exception instanceof CommandException) && asyncResult.exception.getCommandError() == CommandException.Error.RADIO_NOT_AVAILABLE) {
                    updateState(2, "RADIO_NOT_AVAILABLE");
                    return;
                } else {
                    super.handleMessage(message);
                    return;
                }
            }
            super.handleMessage(message);
            return;
        }
        log("Handled by AOSP handleMessage" + message.what);
        super.handleMessage(message);
    }
}
