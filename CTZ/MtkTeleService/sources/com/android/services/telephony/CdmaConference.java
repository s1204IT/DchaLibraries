package com.android.services.telephony;

import android.os.Handler;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.SystemProperties;
import android.telecom.Conference;
import android.telecom.Connection;
import android.telecom.PhoneAccountHandle;
import android.telephony.CarrierConfigManager;
import com.android.internal.telephony.Call;
import com.android.internal.telephony.CallStateException;
import com.android.internal.telephony.Phone;
import com.android.phone.PhoneGlobals;
import com.android.phone.settings.SettingsConstants;
import com.mediatek.services.telephony.MtkGsmCdmaConnection;
import java.util.List;
import mediatek.telecom.MtkConnection;

public class CdmaConference extends Conference implements Holdable {
    private static final int FAKE_HOLD = 1;
    private static final int FAKE_UNHOLD = 0;
    private static final int MSG_CDMA_CALL_SWITCH = 3;
    private static final int MSG_CDMA_CALL_SWITCH_DELAY = 200;
    private static final boolean MTK_SVLTE_SUPPORT = SettingsConstants.DUA_VAL_ON.equals(SystemProperties.get("ro.boot.opt_c2k_lte_mode"));
    private int mCapabilities;
    private final Handler mHandler;
    private boolean mIsHoldable;
    private int mProperties;

    public CdmaConference(PhoneAccountHandle phoneAccountHandle) {
        super(phoneAccountHandle);
        this.mHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                if (message.what == 3) {
                    CdmaConference.this.handleFakeHold(message.arg1);
                }
            }
        };
        setActive();
        this.mProperties = 2;
        this.mProperties |= 65536;
        setConnectionProperties(this.mProperties);
        this.mIsHoldable = false;
    }

    public void updateCapabilities(int i) {
        this.mCapabilities = i | 64 | this.mCapabilities;
        setConnectionCapabilities(buildConnectionCapabilities());
    }

    @Override
    public void onDisconnect() {
        Call originalCall = getOriginalCall();
        if (originalCall != null) {
            Log.d(this, "Found multiparty call to hangup for conference.", new Object[0]);
            try {
                originalCall.hangup();
            } catch (CallStateException e) {
                Log.e((Object) this, (Throwable) e, "Exception thrown trying to hangup conference", new Object[0]);
            }
        }
    }

    @Override
    public void onSeparate(Connection connection) {
        Log.e(this, new Exception(), "Separate not supported for CDMA conference call.", new Object[0]);
    }

    @Override
    public void onHold() {
        Log.d(this, "onHold, just set the hold status.", new Object[0]);
        this.mHandler.sendMessageDelayed(Message.obtain(this.mHandler, 3, 1, 0), 200L);
    }

    @Override
    public void onUnhold() {
        Log.d(this, "onUnhold, just set the unhold status.", new Object[0]);
        this.mHandler.sendMessageDelayed(Message.obtain(this.mHandler, 3, 0, 0), 200L);
    }

    @Override
    public void onMerge() {
        Log.i(this, "Merging CDMA conference call.", new Object[0]);
        this.mCapabilities &= -5;
        if (isSwapSupportedAfterMerge()) {
            this.mCapabilities |= 8;
        }
        updateCapabilities(this.mCapabilities);
        sendFlash();
        if (getState() == 5) {
            onUnhold();
        }
    }

    @Override
    public void onPlayDtmfTone(char c) {
        MtkGsmCdmaConnection firstConnection = getFirstConnection();
        if (firstConnection != null) {
            firstConnection.onPlayDtmfTone(c);
        } else {
            Log.w(this, "No CDMA connection found while trying to play dtmf tone.", new Object[0]);
        }
    }

    @Override
    public void onStopDtmfTone() {
        MtkGsmCdmaConnection firstConnection = getFirstConnection();
        if (firstConnection != null) {
            firstConnection.onStopDtmfTone();
        } else {
            Log.w(this, "No CDMA connection found while trying to stop dtmf tone.", new Object[0]);
        }
    }

    @Override
    public void onSwap() {
        Log.i(this, "Swapping CDMA conference call.", new Object[0]);
        sendFlash();
    }

    private void sendFlash() {
        Call originalCall = getOriginalCall();
        if (originalCall != null) {
            try {
                originalCall.getPhone().switchHoldingAndActive();
            } catch (CallStateException e) {
                Log.e((Object) this, (Throwable) e, "Error while trying to send flash command.", new Object[0]);
            }
        }
    }

    private Call getMultipartyCallForConnection(Connection connection) {
        Call call;
        com.android.internal.telephony.Connection originalConnection = getOriginalConnection(connection);
        if (originalConnection != null && (call = originalConnection.getCall()) != null && call.isMultiparty()) {
            return call;
        }
        return null;
    }

    private Call getOriginalCall() {
        com.android.internal.telephony.Connection originalConnection;
        List<Connection> connections = getConnections();
        if (!connections.isEmpty() && (originalConnection = getOriginalConnection(connections.get(0))) != null) {
            return originalConnection.getCall();
        }
        return null;
    }

    private final boolean isSwapSupportedAfterMerge() {
        PhoneGlobals phoneGlobals = PhoneGlobals.getInstance();
        if (phoneGlobals != null) {
            CarrierConfigManager carrierConfigManager = (CarrierConfigManager) phoneGlobals.getSystemService("carrier_config");
            PersistableBundle configForSubId = null;
            Call originalCall = getOriginalCall();
            if (originalCall != null && originalCall.getPhone() != null) {
                configForSubId = carrierConfigManager.getConfigForSubId(originalCall.getPhone().getSubId());
            }
            if (configForSubId != null) {
                boolean z = configForSubId.getBoolean("support_swap_after_merge_bool");
                Log.d(this, "Current network support swap after call merged capability is " + z, new Object[0]);
                return z;
            }
        }
        return true;
    }

    private com.android.internal.telephony.Connection getOriginalConnection(Connection connection) {
        if (connection instanceof MtkGsmCdmaConnection) {
            return ((MtkGsmCdmaConnection) connection).getOriginalConnection();
        }
        Log.e(this, (Throwable) null, "Non CDMA connection found in a CDMA conference", new Object[0]);
        return null;
    }

    private MtkGsmCdmaConnection getFirstConnection() {
        List<Connection> connections = getConnections();
        if (connections.isEmpty()) {
            return null;
        }
        return (MtkGsmCdmaConnection) connections.get(0);
    }

    @Override
    public void setHoldable(boolean z) {
        this.mIsHoldable = z;
    }

    @Override
    public boolean isChildHoldable() {
        return false;
    }

    private void handleFakeHold(int i) {
        Log.d(this, "handleFakeHold, operation=", Integer.valueOf(i));
        if (1 == i) {
            setOnHold();
        } else if (i == 0) {
            setActive();
        }
        resetConnectionState();
        updateCapabilities(this.mCapabilities);
    }

    public void resetConnectionState() {
        int state = getState();
        if (state != 4 && state != 5) {
            return;
        }
        for (Connection connection : getConnections()) {
            if (connection.getState() != state) {
                if (state == 4) {
                    connection.setActive();
                } else {
                    connection.setOnHold();
                }
                if (connection instanceof MtkGsmCdmaConnection) {
                    ((MtkGsmCdmaConnection) connection).updateState();
                }
            }
        }
    }

    protected int buildConnectionCapabilities() {
        boolean zIsInEcm;
        Phone phone;
        Log.d(this, "buildConnectionCapabilities", new Object[0]);
        if (getConnections() == null || getConnections().size() == 0) {
            Log.d(this, "No connection exist, update capability to 0.", new Object[0]);
            return 0;
        }
        Call originalCall = getOriginalCall();
        if (originalCall != null && (phone = originalCall.getPhone()) != null) {
            zIsInEcm = phone.isInEcm();
        } else {
            zIsInEcm = false;
        }
        if (!zIsInEcm) {
            this.mCapabilities |= 64;
        } else {
            this.mCapabilities &= -65;
        }
        if (MTK_SVLTE_SUPPORT) {
            this.mCapabilities |= 2;
            if (getState() == 4 || getState() == 5) {
                this.mCapabilities |= 1;
            }
        }
        Log.d(this, MtkConnection.capabilitiesToString(this.mCapabilities), new Object[0]);
        return this.mCapabilities;
    }

    protected void updateConnectionCapabilities() {
        setConnectionCapabilities(buildConnectionCapabilities());
    }

    protected void removeCapabilities(int i) {
        this.mCapabilities = (~i) & this.mCapabilities;
        setConnectionCapabilities(buildConnectionCapabilities());
    }

    public void onHangupAll() {
        Log.d(this, "onHangupAll", new Object[0]);
        if (getFirstConnection() != null) {
            try {
                Call call = getFirstConnection().getOriginalConnection().getCall();
                if (call != null) {
                    call.hangup();
                } else {
                    Log.w(this, "call is null.", new Object[0]);
                }
            } catch (CallStateException e) {
                Log.e((Object) this, (Throwable) e, "Failed to hangup the call.", new Object[0]);
            }
        }
    }
}
