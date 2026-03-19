package com.mediatek.internal.telephony.cat;

import android.os.Handler;
import com.android.internal.telephony.cat.BerTlv;
import com.android.internal.telephony.cat.ResultCode;
import com.android.internal.telephony.cat.ResultException;
import com.android.internal.telephony.cat.RilMessage;
import com.android.internal.telephony.cat.RilMessageDecoder;
import com.android.internal.telephony.uicc.IccFileHandler;
import com.android.internal.telephony.uicc.IccUtils;

public class MtkRilMessageDecoder extends RilMessageDecoder {
    private int mSlotId;

    public int getSlotId() {
        return this.mSlotId;
    }

    public MtkRilMessageDecoder(Handler handler, IccFileHandler iccFileHandler, int i) {
        super(handler, iccFileHandler);
        this.mSlotId = i;
        MtkCatLog.d(this, "mCaller is " + this.mCaller.getClass().getName());
    }

    public MtkRilMessageDecoder() {
    }

    protected void sendCmdForExecution(RilMessage rilMessage) {
        MtkCatLog.d(this, "sendCmdForExecution");
        if (rilMessage instanceof MtkRilMessage) {
            this.mCaller.obtainMessage(10, new MtkRilMessage((MtkRilMessage) rilMessage)).sendToTarget();
        } else {
            super.sendCmdForExecution(rilMessage);
        }
    }

    public boolean decodeMessageParams(RilMessage rilMessage) {
        MtkCatLog.d(this, "decodeMessageParams");
        this.mCurrentRilMessage = rilMessage;
        switch (rilMessage.mId) {
            case 1:
            case 4:
                this.mCurrentRilMessage.mResCode = ResultCode.OK;
                sendCmdForExecution(this.mCurrentRilMessage);
                break;
            case 2:
            case 3:
            case 5:
                try {
                    byte[] bArrHexStringToBytes = IccUtils.hexStringToBytes((String) rilMessage.mData);
                    try {
                        if (this.mCmdParamsFactory != null) {
                            this.mCmdParamsFactory.make(BerTlv.decode(bArrHexStringToBytes));
                        }
                    } catch (ResultException e) {
                        MtkCatLog.d(this, "decodeMessageParams: caught ResultException e=" + e);
                        this.mCurrentRilMessage.mId = 1;
                        this.mCurrentRilMessage.mResCode = e.result();
                        sendCmdForExecution(this.mCurrentRilMessage);
                        return false;
                    }
                } catch (Exception e2) {
                    MtkCatLog.d(this, "decodeMessageParams dropping zombie messages");
                    return false;
                }
                break;
        }
        return false;
    }

    public void dispose() {
        quitNow();
        this.mStateStart = null;
        this.mStateCmdParamsReady = null;
        this.mCmdParamsFactory.dispose();
        this.mCmdParamsFactory = null;
        this.mCurrentRilMessage = null;
        this.mCaller = null;
        if (mInstance != null) {
            if (mInstance[this.mSlotId] != null) {
                mInstance[this.mSlotId].quit();
                mInstance[this.mSlotId] = null;
            }
            int i = 0;
            while (i < mSimCount && mInstance[i] == null) {
                i++;
            }
            if (i == mSimCount) {
                mInstance = null;
            }
        }
    }
}
