package com.android.internal.telephony.cat;

import android.os.Handler;
import android.os.Message;
import android.telephony.TelephonyManager;
import com.android.internal.telephony.uicc.IccFileHandler;
import com.android.internal.telephony.uicc.IccUtils;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;

class BipRilMessageDecoder extends StateMachine {
    private static final int CMD_PARAMS_READY = 2;
    private static final int CMD_START = 1;
    private BipCommandParamsFactory mBipCmdParamsFactory;
    private Handler mCaller;
    private RilMessage mCurrentRilMessage;
    private int mSlotId;
    private StateCmdParamsReady mStateCmdParamsReady;
    private StateStart mStateStart;
    private static int mSimCount = 0;
    private static BipRilMessageDecoder[] mInstance = null;

    public static synchronized BipRilMessageDecoder getInstance(Handler caller, IccFileHandler fh, int slotId) {
        if (mInstance == null) {
            mSimCount = TelephonyManager.getDefault().getSimCount();
            mInstance = new BipRilMessageDecoder[mSimCount];
            for (int i = 0; i < mSimCount; i++) {
                mInstance[i] = null;
            }
        }
        if (slotId != -1 && slotId < mSimCount) {
            if (mInstance[slotId] == null) {
                mInstance[slotId] = new BipRilMessageDecoder(caller, fh, slotId);
            }
            return mInstance[slotId];
        }
        CatLog.d("BipRilMessageDecoder", "invaild slot id: " + slotId);
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
        Message msg = this.mCaller.obtainMessage(20, new RilMessage(rilMsg));
        msg.sendToTarget();
    }

    public int getSlotId() {
        return this.mSlotId;
    }

    private BipRilMessageDecoder(Handler handler, IccFileHandler iccFileHandler, int i) {
        super("BipRilMessageDecoder");
        this.mBipCmdParamsFactory = null;
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
        this.mBipCmdParamsFactory = BipCommandParamsFactory.getInstance(this, iccFileHandler);
    }

    private BipRilMessageDecoder() {
        super("BipRilMessageDecoder");
        this.mBipCmdParamsFactory = null;
        this.mCurrentRilMessage = null;
        this.mCaller = null;
        this.mStateStart = new StateStart(this, null);
        this.mStateCmdParamsReady = new StateCmdParamsReady(this, 0 == true ? 1 : 0);
    }

    private class StateStart extends State {
        StateStart(BipRilMessageDecoder this$0, StateStart stateStart) {
            this();
        }

        private StateStart() {
        }

        public boolean processMessage(Message msg) {
            if (msg.what == 1) {
                if (BipRilMessageDecoder.this.decodeMessageParams((RilMessage) msg.obj)) {
                    BipRilMessageDecoder.this.transitionTo(BipRilMessageDecoder.this.mStateCmdParamsReady);
                }
            } else {
                CatLog.d(this, "StateStart unexpected expecting START=1 got " + msg.what);
            }
            return true;
        }
    }

    private class StateCmdParamsReady extends State {
        StateCmdParamsReady(BipRilMessageDecoder this$0, StateCmdParamsReady stateCmdParamsReady) {
            this();
        }

        private StateCmdParamsReady() {
        }

        public boolean processMessage(Message msg) {
            if (msg.what == 2) {
                BipRilMessageDecoder.this.mCurrentRilMessage.mResCode = ResultCode.fromInt(msg.arg1);
                BipRilMessageDecoder.this.mCurrentRilMessage.mData = msg.obj;
                BipRilMessageDecoder.this.sendCmdForExecution(BipRilMessageDecoder.this.mCurrentRilMessage);
                BipRilMessageDecoder.this.transitionTo(BipRilMessageDecoder.this.mStateStart);
                return true;
            }
            CatLog.d(this, "StateCmdParamsReady expecting CMD_PARAMS_READY=2 got " + msg.what);
            BipRilMessageDecoder.this.deferMessage(msg);
            return true;
        }
    }

    private boolean decodeMessageParams(RilMessage rilMsg) {
        this.mCurrentRilMessage = rilMsg;
        switch (rilMsg.mId) {
            case 18:
            case 19:
                CatLog.d(this, "decodeMessageParams raw: " + ((String) rilMsg.mData));
                try {
                    byte[] rawData = IccUtils.hexStringToBytes((String) rilMsg.mData);
                    try {
                        this.mBipCmdParamsFactory.make(BerTlv.decode(rawData));
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
        this.mStateStart = null;
        this.mStateCmdParamsReady = null;
        this.mBipCmdParamsFactory.dispose();
        this.mBipCmdParamsFactory = null;
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
