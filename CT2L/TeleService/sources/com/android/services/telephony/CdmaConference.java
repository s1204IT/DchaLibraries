package com.android.services.telephony;

import android.content.Context;
import android.content.res.Resources;
import android.telecom.Conference;
import android.telecom.Connection;
import android.telecom.PhoneAccountHandle;
import com.android.internal.telephony.Call;
import com.android.internal.telephony.CallStateException;
import com.android.phone.PhoneGlobals;
import com.android.phone.R;
import java.util.List;

public class CdmaConference extends Conference {
    private int mCapabilities;

    public CdmaConference(PhoneAccountHandle phoneAccount) {
        super(phoneAccount);
        setActive();
    }

    public void updateCapabilities(int capabilities) {
        setCapabilities(capabilities | 16448);
    }

    @Override
    public void onDisconnect() {
        Call call = getOriginalCall();
        if (call != null) {
            Log.d(this, "Found multiparty call to hangup for conference.", new Object[0]);
            try {
                call.hangup();
            } catch (CallStateException e) {
                Log.e(this, e, "Exception thrown trying to hangup conference", new Object[0]);
            }
        }
    }

    @Override
    public void onSeparate(Connection connection) {
        Log.e(this, new Exception(), "Separate not supported for CDMA conference call.", new Object[0]);
    }

    @Override
    public void onHold() {
        Log.e(this, new Exception(), "Hold not supported for CDMA conference call.", new Object[0]);
    }

    @Override
    public void onUnhold() {
        Log.e(this, new Exception(), "Unhold not supported for CDMA conference call.", new Object[0]);
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
    }

    @Override
    public void onPlayDtmfTone(char c) {
        CdmaConnection connection = getFirstConnection();
        if (connection != null) {
            connection.onPlayDtmfTone(c);
        } else {
            Log.w(this, "No CDMA connection found while trying to play dtmf tone.", new Object[0]);
        }
    }

    @Override
    public void onStopDtmfTone() {
        CdmaConnection connection = getFirstConnection();
        if (connection != null) {
            connection.onStopDtmfTone();
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
        Call call = getOriginalCall();
        if (call != null) {
            try {
                call.getPhone().switchHoldingAndActive();
            } catch (CallStateException e) {
                Log.e(this, e, "Error while trying to send flash command.", new Object[0]);
            }
        }
    }

    private Call getOriginalCall() {
        com.android.internal.telephony.Connection originalConnection;
        List<Connection> connections = getConnections();
        if (connections.isEmpty() || (originalConnection = getOriginalConnection(connections.get(0))) == null) {
            return null;
        }
        return originalConnection.getCall();
    }

    private final boolean isSwapSupportedAfterMerge() {
        Resources r;
        Context context = PhoneGlobals.getInstance();
        if (context == null || (r = context.getResources()) == null) {
            return true;
        }
        boolean supportSwapAfterMerge = r.getBoolean(R.bool.support_swap_after_merge);
        Log.d(this, "Current network support swap after call merged capability is " + supportSwapAfterMerge, new Object[0]);
        return supportSwapAfterMerge;
    }

    private com.android.internal.telephony.Connection getOriginalConnection(Connection connection) {
        if (connection instanceof CdmaConnection) {
            return ((CdmaConnection) connection).getOriginalConnection();
        }
        Log.e(this, null, "Non CDMA connection found in a CDMA conference", new Object[0]);
        return null;
    }

    private CdmaConnection getFirstConnection() {
        List<Connection> connections = getConnections();
        if (connections.isEmpty()) {
            return null;
        }
        return (CdmaConnection) connections.get(0);
    }
}
