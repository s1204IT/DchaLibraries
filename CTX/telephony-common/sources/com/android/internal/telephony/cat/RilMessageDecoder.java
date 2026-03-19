package com.android.internal.telephony.cat;

import android.os.Handler;
import android.os.Message;
import android.telephony.TelephonyManager;
import com.android.internal.telephony.TelephonyComponentFactory;
import com.android.internal.telephony.uicc.IccFileHandler;
import com.android.internal.telephony.uicc.IccUtils;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;

public class RilMessageDecoder extends StateMachine {
    protected static final int CMD_PARAMS_READY = 2;
    protected static final int CMD_START = 1;
    public Handler mCaller;
    protected CommandParamsFactory mCmdParamsFactory;
    protected RilMessage mCurrentRilMessage;
    protected StateCmdParamsReady mStateCmdParamsReady;
    protected StateStart mStateStart;
    protected static int mSimCount = 0;
    protected static RilMessageDecoder[] mInstance = null;

    public static synchronized RilMessageDecoder getInstance(Handler handler, IccFileHandler iccFileHandler, int i) {
        if (mInstance == null) {
            mSimCount = TelephonyManager.getDefault().getSimCount();
            mInstance = new RilMessageDecoder[mSimCount];
            for (int i2 = 0; i2 < mSimCount; i2++) {
                mInstance[i2] = null;
            }
        }
        if (i != -1 && i < mSimCount) {
            if (mInstance[i] == null) {
                mInstance[i] = TelephonyComponentFactory.getInstance().makeRilMessageDecoder(handler, iccFileHandler, i);
            }
            return mInstance[i];
        }
        CatLog.d("RilMessageDecoder", "invaild slot id: " + i);
        return null;
    }

    public void sendStartDecodingMessageParams(RilMessage rilMessage) {
        Message messageObtainMessage = obtainMessage(1);
        messageObtainMessage.obj = rilMessage;
        sendMessage(messageObtainMessage);
    }

    public void sendMsgParamsDecoded(ResultCode resultCode, CommandParams commandParams) {
        Message messageObtainMessage = obtainMessage(2);
        messageObtainMessage.arg1 = resultCode.value();
        messageObtainMessage.obj = commandParams;
        sendMessage(messageObtainMessage);
    }

    protected void sendCmdForExecution(RilMessage rilMessage) {
        this.mCaller.obtainMessage(10, new RilMessage(rilMessage)).sendToTarget();
    }

    public RilMessageDecoder(Handler handler, IccFileHandler iccFileHandler) {
        super("RilMessageDecoder");
        this.mCmdParamsFactory = null;
        this.mCurrentRilMessage = null;
        this.mCaller = null;
        this.mStateStart = new StateStart();
        this.mStateCmdParamsReady = new StateCmdParamsReady();
        addState(this.mStateStart);
        addState(this.mStateCmdParamsReady);
        setInitialState(this.mStateStart);
        this.mCaller = handler;
        this.mCmdParamsFactory = CommandParamsFactory.getInstance(this, iccFileHandler);
    }

    public RilMessageDecoder() {
        super("RilMessageDecoder");
        this.mCmdParamsFactory = null;
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
                if (RilMessageDecoder.this.decodeMessageParams((RilMessage) message.obj)) {
                    RilMessageDecoder.this.transitionTo(RilMessageDecoder.this.mStateCmdParamsReady);
                }
            } else {
                CatLog.d(this, "StateStart unexpected expecting START=1 got " + message.what);
            }
            return true;
        }
    }

    private class StateCmdParamsReady extends State {
        private StateCmdParamsReady() {
        }

        public boolean processMessage(Message message) {
            if (message.what == 2) {
                RilMessageDecoder.this.mCurrentRilMessage.mResCode = ResultCode.fromInt(message.arg1);
                RilMessageDecoder.this.mCurrentRilMessage.mData = message.obj;
                RilMessageDecoder.this.sendCmdForExecution(RilMessageDecoder.this.mCurrentRilMessage);
                RilMessageDecoder.this.transitionTo(RilMessageDecoder.this.mStateStart);
                return true;
            }
            CatLog.d(this, "StateCmdParamsReady expecting CMD_PARAMS_READY=2 got " + message.what);
            RilMessageDecoder.this.deferMessage(message);
            return true;
        }
    }

    protected boolean decodeMessageParams(RilMessage rilMessage) {
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
                    try {
                        this.mCmdParamsFactory.make(BerTlv.decode(IccUtils.hexStringToBytes((String) rilMessage.mData)));
                    } catch (ResultException e) {
                        CatLog.d(this, "decodeMessageParams: caught ResultException e=" + e);
                        this.mCurrentRilMessage.mResCode = e.result();
                        sendCmdForExecution(this.mCurrentRilMessage);
                        return false;
                    }
                } catch (Exception e2) {
                    CatLog.d(this, "decodeMessageParams dropping zombie messages");
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
        mInstance = null;
    }
}
