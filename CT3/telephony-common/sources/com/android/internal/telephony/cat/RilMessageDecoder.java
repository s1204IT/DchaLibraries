package com.android.internal.telephony.cat;

import android.os.Handler;
import android.os.Message;
import android.telephony.TelephonyManager;
import com.android.internal.telephony.uicc.IccFileHandler;
import com.android.internal.telephony.uicc.IccUtils;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;

class RilMessageDecoder extends StateMachine {
    private static final int CMD_PARAMS_READY = 2;
    private static final int CMD_START = 1;
    private Handler mCaller;
    private CommandParamsFactory mCmdParamsFactory;
    private RilMessage mCurrentRilMessage;
    private int mSlotId;
    private StateCmdParamsReady mStateCmdParamsReady;
    private StateStart mStateStart;
    private static int mSimCount = 0;
    private static RilMessageDecoder[] mInstance = null;

    public static synchronized RilMessageDecoder getInstance(Handler caller, IccFileHandler fh, int slotId) {
        if (mInstance == null) {
            mSimCount = TelephonyManager.getDefault().getSimCount();
            mInstance = new RilMessageDecoder[mSimCount];
            for (int i = 0; i < mSimCount; i++) {
                mInstance[i] = null;
            }
        }
        if (slotId != -1 && slotId < mSimCount) {
            if (mInstance[slotId] == null) {
                mInstance[slotId] = new RilMessageDecoder(caller, fh, slotId);
            }
            return mInstance[slotId];
        }
        CatLog.d("RilMessageDecoder", "invaild slot id: " + slotId);
        return null;
    }

    public void sendStartDecodingMessageParams(RilMessage rilMsg) {
        Message msg = obtainMessage(1);
        msg.obj = rilMsg;
        sendMessage(msg);
    }

    public void sendMsgParamsDecoded(ResultCode resCode, CommandParams cmdParams) {
        Message msg = obtainMessage(2);
        msg.arg1 = resCode.value();
        msg.obj = cmdParams;
        sendMessage(msg);
    }

    private void sendCmdForExecution(RilMessage rilMsg) {
        Message msg = this.mCaller.obtainMessage(10, new RilMessage(rilMsg));
        msg.sendToTarget();
    }

    public int getSlotId() {
        return this.mSlotId;
    }

    private RilMessageDecoder(Handler handler, IccFileHandler iccFileHandler, int i) {
        super("RilMessageDecoder");
        this.mCmdParamsFactory = null;
        this.mCurrentRilMessage = null;
        this.mCaller = null;
        this.mStateStart = new StateStart(this, null);
        this.mStateCmdParamsReady = new StateCmdParamsReady(this, 0 == true ? 1 : 0);
        addState(this.mStateStart);
        addState(this.mStateCmdParamsReady);
        setInitialState(this.mStateStart);
        this.mCaller = handler;
        this.mSlotId = i;
        CatLog.d(this, "mCaller is " + this.mCaller.getClass().getName());
        this.mCmdParamsFactory = CommandParamsFactory.getInstance(this, iccFileHandler, ((CatService) this.mCaller).getContext());
    }

    private RilMessageDecoder() {
        super("RilMessageDecoder");
        this.mCmdParamsFactory = null;
        this.mCurrentRilMessage = null;
        this.mCaller = null;
        this.mStateStart = new StateStart(this, null);
        this.mStateCmdParamsReady = new StateCmdParamsReady(this, 0 == true ? 1 : 0);
    }

    private class StateStart extends State {
        StateStart(RilMessageDecoder this$0, StateStart stateStart) {
            this();
        }

        private StateStart() {
        }

        public boolean processMessage(Message msg) {
            if (msg.what == 1) {
                if (RilMessageDecoder.this.decodeMessageParams((RilMessage) msg.obj)) {
                    RilMessageDecoder.this.transitionTo(RilMessageDecoder.this.mStateCmdParamsReady);
                }
            } else {
                CatLog.d(this, "StateStart unexpected expecting START=1 got " + msg.what);
            }
            return true;
        }
    }

    private class StateCmdParamsReady extends State {
        StateCmdParamsReady(RilMessageDecoder this$0, StateCmdParamsReady stateCmdParamsReady) {
            this();
        }

        private StateCmdParamsReady() {
        }

        public boolean processMessage(Message msg) {
            if (msg.what == 2) {
                RilMessageDecoder.this.mCurrentRilMessage.mResCode = ResultCode.fromInt(msg.arg1);
                RilMessageDecoder.this.mCurrentRilMessage.mData = msg.obj;
                RilMessageDecoder.this.sendCmdForExecution(RilMessageDecoder.this.mCurrentRilMessage);
                RilMessageDecoder.this.transitionTo(RilMessageDecoder.this.mStateStart);
                return true;
            }
            CatLog.d(this, "StateCmdParamsReady expecting CMD_PARAMS_READY=2 got " + msg.what);
            RilMessageDecoder.this.deferMessage(msg);
            return true;
        }
    }

    private boolean decodeMessageParams(RilMessage rilMsg) {
        this.mCurrentRilMessage = rilMsg;
        switch (rilMsg.mId) {
            case 1:
            case 4:
                this.mCurrentRilMessage.mResCode = ResultCode.OK;
                sendCmdForExecution(this.mCurrentRilMessage);
                break;
            case 2:
            case 3:
            case 5:
                CatLog.d(this, "decodeMessageParams raw: " + ((String) rilMsg.mData));
                try {
                    byte[] rawData = IccUtils.hexStringToBytes((String) rilMsg.mData);
                    try {
                        this.mCmdParamsFactory.make(BerTlv.decode(rawData));
                    } catch (ResultException e) {
                        CatLog.d(this, "decodeMessageParams: caught ResultException e=" + e);
                        this.mCurrentRilMessage.mId = 1;
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
        if (mInstance == null) {
            return;
        }
        if (mInstance[this.mSlotId] != null) {
            mInstance[this.mSlotId].quit();
            mInstance[this.mSlotId] = null;
        }
        int i = 0;
        while (i < mSimCount && mInstance[i] == null) {
            i++;
        }
        if (i != mSimCount) {
            return;
        }
        mInstance = null;
    }
}
