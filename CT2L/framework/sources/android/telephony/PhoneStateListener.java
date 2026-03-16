package android.telephony;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import com.android.internal.telephony.IPhoneStateListener;
import java.util.List;

public class PhoneStateListener {
    private static final boolean DBG = false;
    public static final int LISTEN_CALL_FORWARDING_INDICATOR = 8;
    public static final int LISTEN_CALL_STATE = 32;
    public static final int LISTEN_CELL_INFO = 1024;
    public static final int LISTEN_CELL_LOCATION = 16;
    public static final int LISTEN_DATA_ACTIVITY = 128;
    public static final int LISTEN_DATA_CONNECTION_REAL_TIME_INFO = 8192;
    public static final int LISTEN_DATA_CONNECTION_STATE = 64;
    public static final int LISTEN_MESSAGE_WAITING_INDICATOR = 4;
    public static final int LISTEN_NONE = 0;
    public static final int LISTEN_OEM_HOOK_RAW_EVENT = 32768;
    public static final int LISTEN_OTASP_CHANGED = 512;
    public static final int LISTEN_PRECISE_CALL_STATE = 2048;
    public static final int LISTEN_PRECISE_DATA_CONNECTION_STATE = 4096;
    public static final int LISTEN_SERVICE_STATE = 1;

    @Deprecated
    public static final int LISTEN_SIGNAL_STRENGTH = 2;
    public static final int LISTEN_SIGNAL_STRENGTHS = 256;
    public static final int LISTEN_VOLTE_STATE = 16384;
    private static final String LOG_TAG = "PhoneStateListener";
    IPhoneStateListener callback;
    private final Handler mHandler;
    protected int mSubId;

    public PhoneStateListener() {
        this(Integer.MAX_VALUE, Looper.myLooper());
    }

    public PhoneStateListener(Looper looper) {
        this(Integer.MAX_VALUE, looper);
    }

    public PhoneStateListener(int subId) {
        this(subId, Looper.myLooper());
    }

    public PhoneStateListener(int subId, Looper looper) {
        this.mSubId = -1;
        this.callback = new IPhoneStateListener.Stub() {
            @Override
            public void onServiceStateChanged(ServiceState serviceState) {
                Message.obtain(PhoneStateListener.this.mHandler, 1, 0, 0, serviceState).sendToTarget();
            }

            @Override
            public void onSignalStrengthChanged(int asu) {
                Message.obtain(PhoneStateListener.this.mHandler, 2, asu, 0, null).sendToTarget();
            }

            @Override
            public void onMessageWaitingIndicatorChanged(boolean mwi) {
                Message.obtain(PhoneStateListener.this.mHandler, 4, mwi ? 1 : 0, 0, null).sendToTarget();
            }

            @Override
            public void onCallForwardingIndicatorChanged(boolean cfi) {
                Message.obtain(PhoneStateListener.this.mHandler, 8, cfi ? 1 : 0, 0, null).sendToTarget();
            }

            @Override
            public void onCellLocationChanged(Bundle bundle) {
                CellLocation location = CellLocation.newFromBundle(bundle);
                Message.obtain(PhoneStateListener.this.mHandler, 16, 0, 0, location).sendToTarget();
            }

            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                Message.obtain(PhoneStateListener.this.mHandler, 32, state, 0, incomingNumber).sendToTarget();
            }

            @Override
            public void onDataConnectionStateChanged(int state, int networkType) {
                Message.obtain(PhoneStateListener.this.mHandler, 64, state, networkType).sendToTarget();
            }

            @Override
            public void onDataActivity(int direction) {
                Message.obtain(PhoneStateListener.this.mHandler, 128, direction, 0, null).sendToTarget();
            }

            @Override
            public void onSignalStrengthsChanged(SignalStrength signalStrength) {
                Message.obtain(PhoneStateListener.this.mHandler, 256, 0, 0, signalStrength).sendToTarget();
            }

            @Override
            public void onOtaspChanged(int otaspMode) {
                Message.obtain(PhoneStateListener.this.mHandler, 512, otaspMode, 0).sendToTarget();
            }

            @Override
            public void onCellInfoChanged(List<CellInfo> cellInfo) {
                Message.obtain(PhoneStateListener.this.mHandler, 1024, 0, 0, cellInfo).sendToTarget();
            }

            @Override
            public void onPreciseCallStateChanged(PreciseCallState callState) {
                Message.obtain(PhoneStateListener.this.mHandler, 2048, 0, 0, callState).sendToTarget();
            }

            @Override
            public void onPreciseDataConnectionStateChanged(PreciseDataConnectionState dataConnectionState) {
                Message.obtain(PhoneStateListener.this.mHandler, 4096, 0, 0, dataConnectionState).sendToTarget();
            }

            @Override
            public void onDataConnectionRealTimeInfoChanged(DataConnectionRealTimeInfo dcRtInfo) {
                Message.obtain(PhoneStateListener.this.mHandler, 8192, 0, 0, dcRtInfo).sendToTarget();
            }

            @Override
            public void onVoLteServiceStateChanged(VoLteServiceState lteState) {
                Message.obtain(PhoneStateListener.this.mHandler, 16384, 0, 0, lteState).sendToTarget();
            }

            @Override
            public void onOemHookRawEvent(byte[] rawData) {
                Message.obtain(PhoneStateListener.this.mHandler, 32768, 0, 0, rawData).sendToTarget();
            }
        };
        this.mSubId = subId;
        this.mHandler = new Handler(looper) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        PhoneStateListener.this.onServiceStateChanged((ServiceState) msg.obj);
                        break;
                    case 2:
                        PhoneStateListener.this.onSignalStrengthChanged(msg.arg1);
                        break;
                    case 4:
                        PhoneStateListener.this.onMessageWaitingIndicatorChanged(msg.arg1 != 0);
                        break;
                    case 8:
                        PhoneStateListener.this.onCallForwardingIndicatorChanged(msg.arg1 != 0);
                        break;
                    case 16:
                        PhoneStateListener.this.onCellLocationChanged((CellLocation) msg.obj);
                        break;
                    case 32:
                        PhoneStateListener.this.onCallStateChanged(msg.arg1, (String) msg.obj);
                        break;
                    case 64:
                        PhoneStateListener.this.onDataConnectionStateChanged(msg.arg1, msg.arg2);
                        PhoneStateListener.this.onDataConnectionStateChanged(msg.arg1);
                        break;
                    case 128:
                        PhoneStateListener.this.onDataActivity(msg.arg1);
                        break;
                    case 256:
                        PhoneStateListener.this.onSignalStrengthsChanged((SignalStrength) msg.obj);
                        break;
                    case 512:
                        PhoneStateListener.this.onOtaspChanged(msg.arg1);
                        break;
                    case 1024:
                        PhoneStateListener.this.onCellInfoChanged((List) msg.obj);
                        break;
                    case 2048:
                        PhoneStateListener.this.onPreciseCallStateChanged((PreciseCallState) msg.obj);
                        break;
                    case 4096:
                        PhoneStateListener.this.onPreciseDataConnectionStateChanged((PreciseDataConnectionState) msg.obj);
                        break;
                    case 8192:
                        PhoneStateListener.this.onDataConnectionRealTimeInfoChanged((DataConnectionRealTimeInfo) msg.obj);
                        break;
                    case 16384:
                        PhoneStateListener.this.onVoLteServiceStateChanged((VoLteServiceState) msg.obj);
                        break;
                    case 32768:
                        PhoneStateListener.this.onOemHookRawEvent((byte[]) msg.obj);
                        break;
                }
            }
        };
    }

    public void onServiceStateChanged(ServiceState serviceState) {
    }

    @Deprecated
    public void onSignalStrengthChanged(int asu) {
    }

    public void onMessageWaitingIndicatorChanged(boolean mwi) {
    }

    public void onCallForwardingIndicatorChanged(boolean cfi) {
    }

    public void onCellLocationChanged(CellLocation location) {
    }

    public void onCallStateChanged(int state, String incomingNumber) {
    }

    public void onDataConnectionStateChanged(int state) {
    }

    public void onDataConnectionStateChanged(int state, int networkType) {
    }

    public void onDataActivity(int direction) {
    }

    public void onSignalStrengthsChanged(SignalStrength signalStrength) {
    }

    public void onOtaspChanged(int otaspMode) {
    }

    public void onCellInfoChanged(List<CellInfo> cellInfo) {
    }

    public void onPreciseCallStateChanged(PreciseCallState callState) {
    }

    public void onPreciseDataConnectionStateChanged(PreciseDataConnectionState dataConnectionState) {
    }

    public void onDataConnectionRealTimeInfoChanged(DataConnectionRealTimeInfo dcRtInfo) {
    }

    public void onVoLteServiceStateChanged(VoLteServiceState stateInfo) {
    }

    public void onOemHookRawEvent(byte[] rawData) {
    }

    private void log(String s) {
        Rlog.d(LOG_TAG, s);
    }
}
