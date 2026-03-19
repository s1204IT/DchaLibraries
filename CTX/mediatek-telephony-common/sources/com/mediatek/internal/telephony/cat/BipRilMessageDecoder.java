package com.mediatek.internal.telephony.cat;

import android.os.Handler;
import android.os.Message;
import android.telephony.TelephonyManager;
import com.android.internal.telephony.cat.BerTlv;
import com.android.internal.telephony.cat.CommandParams;
import com.android.internal.telephony.cat.ResultCode;
import com.android.internal.telephony.cat.ResultException;
import com.android.internal.telephony.uicc.IccFileHandler;
import com.android.internal.telephony.uicc.IccUtils;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;

class BipRilMessageDecoder extends StateMachine {
    private static final int CMD_PARAMS_READY = 2;
    private static final int CMD_START = 1;
    private BipCommandParamsFactory mBipCmdParamsFactory;
    private Handler mCaller;
    private MtkRilMessage mCurrentRilMessage;
    private int mSlotId;
    private StateCmdParamsReady mStateCmdParamsReady;
    private StateStart mStateStart;
    private static int mSimCount = 0;
    private static BipRilMessageDecoder[] mInstance = null;

    public static synchronized BipRilMessageDecoder getInstance(Handler handler, IccFileHandler iccFileHandler, int i) {
        if (mInstance == null) {
            mSimCount = TelephonyManager.getDefault().getSimCount();
            mInstance = new BipRilMessageDecoder[mSimCount];
            for (int i2 = 0; i2 < mSimCount; i2++) {
                mInstance[i2] = null;
            }
        }
        if (i != -1 && i < mSimCount) {
            if (mInstance[i] == null) {
                mInstance[i] = new BipRilMessageDecoder(handler, iccFileHandler, i);
            }
            return mInstance[i];
        }
        MtkCatLog.d("BipRilMessageDecoder", "invaild slot id: " + i);
        return null;
    }

    public void sendStartDecodingMessageParams(MtkRilMessage mtkRilMessage) {
        Message messageObtainMessage = obtainMessage(1);
        messageObtainMessage.obj = mtkRilMessage;
        sendMessage(messageObtainMessage);
    }

    public void sendMsgParamsDecoded(ResultCode resultCode, CommandParams commandParams) {
        Message messageObtainMessage = obtainMessage(2);
        messageObtainMessage.arg1 = resultCode.value();
        messageObtainMessage.obj = commandParams;
        sendMessage(messageObtainMessage);
    }

    private void sendCmdForExecution(MtkRilMessage mtkRilMessage) {
        this.mCaller.obtainMessage(20, new MtkRilMessage(mtkRilMessage)).sendToTarget();
    }

    public int getSlotId() {
        return this.mSlotId;
    }

    private BipRilMessageDecoder(Handler handler, IccFileHandler iccFileHandler, int i) {
        super("BipRilMessageDecoder");
        this.mBipCmdParamsFactory = null;
        this.mCurrentRilMessage = null;
        this.mCaller = null;
        this.mStateStart = new StateStart();
        this.mStateCmdParamsReady = new StateCmdParamsReady();
        addState(this.mStateStart);
        addState(this.mStateCmdParamsReady);
        setInitialState(this.mStateStart);
        this.mCaller = handler;
        this.mSlotId = i;
        MtkCatLog.d(this, "mCaller is " + this.mCaller.getClass().getName());
        this.mBipCmdParamsFactory = BipCommandParamsFactory.getInstance(this, iccFileHandler);
    }

    private BipRilMessageDecoder() {
        super("BipRilMessageDecoder");
        this.mBipCmdParamsFactory = null;
        this.mCurrentRilMessage = null;
        this.mCaller = null;
        this.mStateStart = new StateStart();
        this.mStateCmdParamsReady = new StateCmdParamsReady();
    }

    private class StateStart extends State {
        private StateStart() {
        }

        public boolean processMessage(Message message) {
            if (message.what == 1) {
                if (BipRilMessageDecoder.this.decodeMessageParams((MtkRilMessage) message.obj)) {
                    BipRilMessageDecoder.this.transitionTo(BipRilMessageDecoder.this.mStateCmdParamsReady);
                }
            } else {
                MtkCatLog.d(this, "StateStart unexpected expecting START=1 got " + message.what);
            }
            return true;
        }
    }

    private class StateCmdParamsReady extends State {
        private StateCmdParamsReady() {
        }

        public boolean processMessage(Message message) {
            if (message.what == 2) {
                BipRilMessageDecoder.this.mCurrentRilMessage.mResCode = ResultCode.fromInt(message.arg1);
                BipRilMessageDecoder.this.mCurrentRilMessage.mData = message.obj;
                BipRilMessageDecoder.this.sendCmdForExecution(BipRilMessageDecoder.this.mCurrentRilMessage);
                BipRilMessageDecoder.this.transitionTo(BipRilMessageDecoder.this.mStateStart);
                return true;
            }
            MtkCatLog.d(this, "StateCmdParamsReady expecting CMD_PARAMS_READY=2 got " + message.what);
            BipRilMessageDecoder.this.deferMessage(message);
            return true;
        }
    }

    private boolean decodeMessageParams(MtkRilMessage mtkRilMessage) {
        this.mCurrentRilMessage = mtkRilMessage;
        switch (mtkRilMessage.mId) {
            case 18:
            case 19:
                MtkCatLog.d(this, "decodeMessageParams raw: " + ((String) mtkRilMessage.mData));
                try {
                    try {
                        this.mBipCmdParamsFactory.make(BerTlv.decode(IccUtils.hexStringToBytes((String) mtkRilMessage.mData)));
                        return true;
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
            default:
                return false;
        }
    }

    public void dispose() {
        this.mStateStart = null;
        this.mStateCmdParamsReady = null;
        this.mBipCmdParamsFactory.dispose();
        this.mBipCmdParamsFactory = null;
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
