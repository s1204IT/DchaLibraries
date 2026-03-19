package com.android.internal.telephony.imsphone;

import android.util.ArrayMap;
import android.util.Log;
import com.android.ims.ImsCallProfile;
import com.android.ims.ImsExternalCallState;
import com.android.ims.ImsExternalCallStateListener;
import com.android.internal.telephony.Call;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.imsphone.ImsExternalConnection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ImsExternalCallTracker {
    public static final String EXTRA_IMS_EXTERNAL_CALL_ID = "android.telephony.ImsExternalCallTracker.extra.EXTERNAL_CALL_ID";
    public static final String TAG = "ImsExternalCallTracker";
    private final ImsPullCall mCallPuller;
    private final ImsPhone mPhone;
    private Map<Integer, ImsExternalConnection> mExternalConnections = new ArrayMap();
    private final ExternalConnectionListener mExternalConnectionListener = new ExternalConnectionListener();
    private final ExternalCallStateListener mExternalCallStateListener = new ExternalCallStateListener();

    public class ExternalCallStateListener extends ImsExternalCallStateListener {
        public ExternalCallStateListener() {
        }

        public void onImsExternalCallStateUpdate(List<ImsExternalCallState> externalCallState) {
            ImsExternalCallTracker.this.refreshExternalCallState(externalCallState);
        }
    }

    public class ExternalConnectionListener implements ImsExternalConnection.Listener {
        public ExternalConnectionListener() {
        }

        @Override
        public void onPullExternalCall(ImsExternalConnection connection) {
            Log.d(ImsExternalCallTracker.TAG, "onPullExternalCall: connection = " + connection);
            ImsExternalCallTracker.this.mCallPuller.pullExternalCall(connection.getAddress(), connection.getVideoState());
        }
    }

    public ImsExternalCallTracker(ImsPhone phone, ImsPullCall callPuller) {
        this.mPhone = phone;
        this.mCallPuller = callPuller;
    }

    public ExternalCallStateListener getExternalCallStateListener() {
        return this.mExternalCallStateListener;
    }

    public void refreshExternalCallState(List<ImsExternalCallState> externalCallStates) {
        Log.d(TAG, "refreshExternalCallState: depSize = " + externalCallStates.size());
        Iterator<Map.Entry<Integer, ImsExternalConnection>> connectionIterator = this.mExternalConnections.entrySet().iterator();
        boolean wasCallRemoved = false;
        while (connectionIterator.hasNext()) {
            Map.Entry<Integer, ImsExternalConnection> entry = connectionIterator.next();
            int callId = entry.getKey().intValue();
            if (!containsCallId(externalCallStates, callId)) {
                ImsExternalConnection externalConnection = entry.getValue();
                externalConnection.setTerminated();
                externalConnection.removeListener(this.mExternalConnectionListener);
                connectionIterator.remove();
                wasCallRemoved = true;
            }
        }
        if (wasCallRemoved) {
            this.mPhone.notifyPreciseCallStateChanged();
        }
        for (ImsExternalCallState callState : externalCallStates) {
            if (!this.mExternalConnections.containsKey(Integer.valueOf(callState.getCallId()))) {
                Log.d(TAG, "refreshExternalCallState: got = " + callState);
                if (callState.getCallState() == 1) {
                    createExternalConnection(callState);
                }
            } else {
                updateExistingConnection(this.mExternalConnections.get(Integer.valueOf(callState.getCallId())), callState);
            }
        }
    }

    public Connection getConnectionById(int callId) {
        return this.mExternalConnections.get(Integer.valueOf(callId));
    }

    private void createExternalConnection(ImsExternalCallState state) {
        Log.i(TAG, "createExternalConnection");
        ImsExternalConnection connection = new ImsExternalConnection(this.mPhone, state.getCallId(), state.getAddress().getSchemeSpecificPart(), state.isCallPullable());
        connection.setVideoState(ImsCallProfile.getVideoStateFromCallType(state.getCallType()));
        connection.addListener(this.mExternalConnectionListener);
        this.mExternalConnections.put(Integer.valueOf(connection.getCallId()), connection);
        this.mPhone.notifyUnknownConnection(connection);
    }

    private void updateExistingConnection(ImsExternalConnection connection, ImsExternalCallState state) {
        Call.State existingState = connection.getState();
        Call.State newState = state.getCallState() == 1 ? Call.State.ACTIVE : Call.State.DISCONNECTED;
        if (existingState != newState) {
            if (newState == Call.State.ACTIVE) {
                connection.setActive();
            } else {
                connection.setTerminated();
                connection.removeListener(this.mExternalConnectionListener);
                this.mExternalConnections.remove(connection);
                this.mPhone.notifyPreciseCallStateChanged();
            }
        }
        connection.setIsPullable(state.isCallPullable());
        int newVideoState = ImsCallProfile.getVideoStateFromCallType(state.getCallType());
        if (newVideoState == connection.getVideoState()) {
            return;
        }
        connection.setVideoState(newVideoState);
    }

    private boolean containsCallId(List<ImsExternalCallState> externalCallStates, int callId) {
        for (ImsExternalCallState state : externalCallStates) {
            if (state.getCallId() == callId) {
                return true;
            }
        }
        return false;
    }
}
