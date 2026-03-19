package com.android.bluetooth.btservice;

import android.bluetooth.BluetoothAdapter;
import android.os.Message;
import android.util.Log;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;

final class AdapterState extends StateMachine {
    static final int BLE_STARTED = 7;
    static final int BLE_START_TIMEOUT = 12;
    static final int BLE_START_TIMEOUT_DELAY = 14000;
    static final int BLE_STOPPED = 8;
    static final int BLE_STOP_TIMEOUT = 11;
    static final int BLE_STOP_TIMEOUT_DELAY = 2000;
    static final int BLE_TURN_OFF = 4;
    static final int BLE_TURN_ON = 3;
    static final int BREDR_STARTED = 5;
    static final int BREDR_START_TIMEOUT = 9;
    static final int BREDR_START_TIMEOUT_DELAY = 4000;
    static final int BREDR_STOPPED = 6;
    static final int BREDR_STOP_TIMEOUT = 10;
    static final int BREDR_STOP_TIMEOUT_DELAY = 4000;
    private static final boolean DBG = true;
    private static final String TAG = AdapterState.class.getSimpleName();
    static final int USER_TURN_OFF = 2;
    static final int USER_TURN_ON = 1;
    private AdapterService mAdapterService;
    private BleOnState mBleOnState;
    private OffState mOffState;
    private OnState mOnState;
    private int mPrevState;
    private TurningBleOffState mTurningBleOffState;
    private TurningBleOnState mTurningBleOnState;
    private TurningOffState mTurningOffState;
    private TurningOnState mTurningOnState;

    private AdapterState(AdapterService adapterService) {
        super(TAG);
        this.mTurningOnState = new TurningOnState();
        this.mTurningBleOnState = new TurningBleOnState();
        this.mTurningOffState = new TurningOffState();
        this.mTurningBleOffState = new TurningBleOffState();
        this.mOnState = new OnState();
        this.mOffState = new OffState();
        this.mBleOnState = new BleOnState();
        this.mPrevState = 10;
        addState(this.mOnState);
        addState(this.mBleOnState);
        addState(this.mOffState);
        addState(this.mTurningOnState);
        addState(this.mTurningOffState);
        addState(this.mTurningBleOnState);
        addState(this.mTurningBleOffState);
        this.mAdapterService = adapterService;
        setInitialState(this.mOffState);
    }

    private String messageString(int i) {
        switch (i) {
            case 1:
                return "USER_TURN_ON";
            case 2:
                return "USER_TURN_OFF";
            case 3:
                return "BLE_TURN_ON";
            case 4:
                return "BLE_TURN_OFF";
            case 5:
                return "BREDR_STARTED";
            case 6:
                return "BREDR_STOPPED";
            case 7:
                return "BLE_STARTED";
            case 8:
                return "BLE_STOPPED";
            case 9:
                return "BREDR_START_TIMEOUT";
            case 10:
                return "BREDR_STOP_TIMEOUT";
            case 11:
                return "BLE_STOP_TIMEOUT";
            case 12:
                return "BLE_START_TIMEOUT";
            default:
                return "Unknown message (" + i + ")";
        }
    }

    public static AdapterState make(AdapterService adapterService) {
        Log.d(TAG, "make() - Creating AdapterState");
        AdapterState adapterState = new AdapterState(adapterService);
        adapterState.start();
        return adapterState;
    }

    public void doQuit() {
        quitNow();
    }

    private void cleanup() {
        if (this.mAdapterService != null) {
            this.mAdapterService = null;
        }
    }

    protected void onQuitting() {
        cleanup();
    }

    protected String getLogRecString(Message message) {
        return messageString(message.what);
    }

    private abstract class BaseAdapterState extends State {
        abstract int getStateValue();

        private BaseAdapterState() {
        }

        public void enter() {
            int stateValue = getStateValue();
            infoLog("entered ");
            AdapterState.this.mAdapterService.updateAdapterState(AdapterState.this.mPrevState, stateValue);
            AdapterState.this.mPrevState = stateValue;
        }

        void infoLog(String str) {
            Log.i(AdapterState.TAG, BluetoothAdapter.nameForState(getStateValue()) + " : " + str);
        }

        void errorLog(String str) {
            Log.e(AdapterState.TAG, BluetoothAdapter.nameForState(getStateValue()) + " : " + str);
        }
    }

    private class OffState extends BaseAdapterState {
        private OffState() {
            super();
        }

        @Override
        int getStateValue() {
            return 10;
        }

        public boolean processMessage(Message message) {
            if (message.what == 3) {
                AdapterState.this.transitionTo(AdapterState.this.mTurningBleOnState);
                return true;
            }
            infoLog("Unhandled message - " + AdapterState.this.messageString(message.what));
            return false;
        }
    }

    private class BleOnState extends BaseAdapterState {
        private BleOnState() {
            super();
        }

        @Override
        int getStateValue() {
            return 15;
        }

        public boolean processMessage(Message message) {
            int i = message.what;
            if (i == 1) {
                AdapterState.this.transitionTo(AdapterState.this.mTurningOnState);
            } else if (i == 4) {
                AdapterState.this.transitionTo(AdapterState.this.mTurningBleOffState);
            } else {
                infoLog("Unhandled message - " + AdapterState.this.messageString(message.what));
                return false;
            }
            return true;
        }
    }

    private class OnState extends BaseAdapterState {
        private OnState() {
            super();
        }

        @Override
        int getStateValue() {
            return 12;
        }

        public boolean processMessage(Message message) {
            if (message.what == 2) {
                AdapterState.this.transitionTo(AdapterState.this.mTurningOffState);
                return true;
            }
            infoLog("Unhandled message - " + AdapterState.this.messageString(message.what));
            return false;
        }
    }

    private class TurningBleOnState extends BaseAdapterState {
        private TurningBleOnState() {
            super();
        }

        @Override
        int getStateValue() {
            return 14;
        }

        @Override
        public void enter() {
            super.enter();
            AdapterState.this.sendMessageDelayed(12, 14000L);
            AdapterState.this.mAdapterService.bringUpBle();
        }

        public void exit() {
            AdapterState.this.removeMessages(12);
            super.exit();
        }

        public boolean processMessage(Message message) {
            int i = message.what;
            if (i == 7) {
                AdapterState.this.transitionTo(AdapterState.this.mBleOnState);
                return true;
            }
            if (i == 12) {
                errorLog(AdapterState.this.messageString(message.what));
                AdapterState.this.transitionTo(AdapterState.this.mTurningBleOffState);
                return true;
            }
            infoLog("Unhandled message - " + AdapterState.this.messageString(message.what));
            return false;
        }
    }

    private class TurningOnState extends BaseAdapterState {
        private TurningOnState() {
            super();
        }

        @Override
        int getStateValue() {
            return 11;
        }

        @Override
        public void enter() {
            super.enter();
            AdapterState.this.sendMessageDelayed(9, 4000L);
            AdapterState.this.mAdapterService.startProfileServices();
        }

        public void exit() {
            AdapterState.this.removeMessages(9);
            super.exit();
        }

        public boolean processMessage(Message message) {
            int i = message.what;
            if (i == 5) {
                AdapterState.this.transitionTo(AdapterState.this.mOnState);
                return true;
            }
            if (i == 9) {
                errorLog(AdapterState.this.messageString(message.what));
                AdapterState.this.transitionTo(AdapterState.this.mTurningOffState);
                return true;
            }
            infoLog("Unhandled message - " + AdapterState.this.messageString(message.what));
            return false;
        }
    }

    private class TurningOffState extends BaseAdapterState {
        private TurningOffState() {
            super();
        }

        @Override
        int getStateValue() {
            return 13;
        }

        @Override
        public void enter() {
            super.enter();
            AdapterState.this.sendMessageDelayed(10, 4000L);
            AdapterState.this.mAdapterService.stopProfileServices();
        }

        public void exit() {
            AdapterState.this.removeMessages(10);
            super.exit();
        }

        public boolean processMessage(Message message) {
            int i = message.what;
            if (i == 6) {
                AdapterState.this.transitionTo(AdapterState.this.mBleOnState);
                return true;
            }
            if (i == 10) {
                errorLog(AdapterState.this.messageString(message.what));
                AdapterState.this.transitionTo(AdapterState.this.mTurningBleOffState);
                return true;
            }
            infoLog("Unhandled message - " + AdapterState.this.messageString(message.what));
            return false;
        }
    }

    private class TurningBleOffState extends BaseAdapterState {
        private TurningBleOffState() {
            super();
        }

        @Override
        int getStateValue() {
            return 16;
        }

        @Override
        public void enter() {
            super.enter();
            AdapterState.this.sendMessageDelayed(11, 2000L);
            AdapterState.this.mAdapterService.bringDownBle();
        }

        public void exit() {
            AdapterState.this.removeMessages(11);
            super.exit();
        }

        public boolean processMessage(Message message) {
            int i = message.what;
            if (i == 8) {
                AdapterState.this.transitionTo(AdapterState.this.mOffState);
                return true;
            }
            if (i == 11) {
                errorLog(AdapterState.this.messageString(message.what));
                AdapterState.this.transitionTo(AdapterState.this.mOffState);
                return true;
            }
            infoLog("Unhandled message - " + AdapterState.this.messageString(message.what));
            return false;
        }
    }
}
