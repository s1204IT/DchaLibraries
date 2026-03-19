package com.android.internal.telephony.uicc.euicc.apdu;

import android.os.AsyncResult;
import android.os.Message;
import android.telephony.IccOpenLogicalChannelResponse;
import android.telephony.Rlog;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.uicc.euicc.async.AsyncMessageInvocation;

class OpenLogicalChannelInvocation extends AsyncMessageInvocation<String, IccOpenLogicalChannelResponse> {
    private static final String LOG_TAG = "OpenChan";
    private final CommandsInterface mCi;

    OpenLogicalChannelInvocation(CommandsInterface commandsInterface) {
        this.mCi = commandsInterface;
    }

    @Override
    protected void sendRequestMessage(String str, Message message) {
        this.mCi.iccOpenLogicalChannel(str, 0, message);
    }

    @Override
    protected IccOpenLogicalChannelResponse parseResult(AsyncResult asyncResult) {
        IccOpenLogicalChannelResponse iccOpenLogicalChannelResponse;
        byte[] bArr = null;
        if (asyncResult.exception == null && asyncResult.result != null) {
            int[] iArr = (int[]) asyncResult.result;
            int i = iArr[0];
            if (iArr.length > 1) {
                bArr = new byte[iArr.length - 1];
                for (int i2 = 1; i2 < iArr.length; i2++) {
                    bArr[i2 - 1] = (byte) iArr[i2];
                }
            }
            iccOpenLogicalChannelResponse = new IccOpenLogicalChannelResponse(i, 1, bArr);
        } else {
            if (asyncResult.result == null) {
                Rlog.e(LOG_TAG, "Empty response");
            }
            if (asyncResult.exception != null) {
                Rlog.e(LOG_TAG, "Exception", asyncResult.exception);
            }
            int i3 = 4;
            if (asyncResult.exception instanceof CommandException) {
                CommandException.Error commandError = ((CommandException) asyncResult.exception).getCommandError();
                if (commandError == CommandException.Error.MISSING_RESOURCE) {
                    i3 = 2;
                } else if (commandError == CommandException.Error.NO_SUCH_ELEMENT) {
                    i3 = 3;
                }
            }
            iccOpenLogicalChannelResponse = new IccOpenLogicalChannelResponse(-1, i3, null);
        }
        Rlog.v(LOG_TAG, "Response: " + iccOpenLogicalChannelResponse);
        return iccOpenLogicalChannelResponse;
    }
}
