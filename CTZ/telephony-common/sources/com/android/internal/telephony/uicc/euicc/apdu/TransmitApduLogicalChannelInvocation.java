package com.android.internal.telephony.uicc.euicc.apdu;

import android.os.AsyncResult;
import android.os.Message;
import android.telephony.Rlog;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.uicc.IccIoResult;
import com.android.internal.telephony.uicc.euicc.async.AsyncMessageInvocation;

public class TransmitApduLogicalChannelInvocation extends AsyncMessageInvocation<ApduCommand, IccIoResult> {
    private static final String LOG_TAG = "TransApdu";
    private static final int SW1_ERROR = 111;
    private final CommandsInterface mCi;

    TransmitApduLogicalChannelInvocation(CommandsInterface commandsInterface) {
        this.mCi = commandsInterface;
    }

    @Override
    protected void sendRequestMessage(ApduCommand apduCommand, Message message) {
        Rlog.v(LOG_TAG, "Send: " + apduCommand);
        this.mCi.iccTransmitApduLogicalChannel(apduCommand.channel, apduCommand.cla | apduCommand.channel, apduCommand.ins, apduCommand.p1, apduCommand.p2, apduCommand.p3, apduCommand.cmdHex, message);
    }

    @Override
    protected IccIoResult parseResult(AsyncResult asyncResult) {
        IccIoResult iccIoResult;
        if (asyncResult.exception == null && asyncResult.result != null) {
            iccIoResult = (IccIoResult) asyncResult.result;
        } else {
            if (asyncResult.result == null) {
                Rlog.e(LOG_TAG, "Empty response");
            } else if (asyncResult.exception instanceof CommandException) {
                Rlog.e(LOG_TAG, "CommandException", asyncResult.exception);
            } else {
                Rlog.e(LOG_TAG, "CommandException", asyncResult.exception);
            }
            iccIoResult = new IccIoResult(111, 0, (byte[]) null);
        }
        Rlog.v(LOG_TAG, "Response: " + iccIoResult);
        return iccIoResult;
    }
}
