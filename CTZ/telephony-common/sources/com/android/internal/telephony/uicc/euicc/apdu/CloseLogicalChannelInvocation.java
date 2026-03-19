package com.android.internal.telephony.uicc.euicc.apdu;

import android.os.AsyncResult;
import android.os.Message;
import android.telephony.Rlog;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.uicc.euicc.async.AsyncMessageInvocation;

class CloseLogicalChannelInvocation extends AsyncMessageInvocation<Integer, Boolean> {
    private static final String LOG_TAG = "CloseChan";
    private final CommandsInterface mCi;

    CloseLogicalChannelInvocation(CommandsInterface commandsInterface) {
        this.mCi = commandsInterface;
    }

    @Override
    protected void sendRequestMessage(Integer num, Message message) {
        Rlog.v(LOG_TAG, "Channel: " + num);
        this.mCi.iccCloseLogicalChannel(num.intValue(), message);
    }

    @Override
    protected Boolean parseResult(AsyncResult asyncResult) {
        if (asyncResult.exception == null) {
            return true;
        }
        if (asyncResult.exception instanceof CommandException) {
            Rlog.e(LOG_TAG, "CommandException", asyncResult.exception);
        } else {
            Rlog.e(LOG_TAG, "Unknown exception", asyncResult.exception);
        }
        return false;
    }
}
