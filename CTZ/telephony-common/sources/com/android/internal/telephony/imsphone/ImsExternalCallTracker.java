package com.android.internal.telephony.imsphone;

import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.telecom.VideoProfile;
import android.telephony.ims.ImsCallProfile;
import android.telephony.ims.ImsExternalCallState;
import android.util.ArrayMap;
import android.util.Log;
import com.android.ims.ImsExternalCallStateListener;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.Call;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.imsphone.ImsExternalConnection;
import com.android.internal.telephony.imsphone.ImsPhoneCallTracker;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ImsExternalCallTracker implements ImsPhoneCallTracker.PhoneStateListener {
    private static final int EVENT_VIDEO_CAPABILITIES_CHANGED = 1;
    public static final String EXTRA_IMS_EXTERNAL_CALL_ID = "android.telephony.ImsExternalCallTracker.extra.EXTERNAL_CALL_ID";
    public static final String TAG = "ImsExternalCallTracker";
    private ImsPullCall mCallPuller;
    private final ImsCallNotify mCallStateNotifier;
    private Map<Integer, Boolean> mExternalCallPullableState;
    private final ExternalCallStateListener mExternalCallStateListener;
    private final ExternalConnectionListener mExternalConnectionListener;
    private Map<Integer, ImsExternalConnection> mExternalConnections;
    private final Handler mHandler;
    private boolean mHasActiveCalls;
    private boolean mIsVideoCapable;
    private final ImsPhone mPhone;

    public interface ImsCallNotify {
        void notifyPreciseCallStateChanged();

        void notifyUnknownConnection(Connection connection);
    }

    public class ExternalCallStateListener extends ImsExternalCallStateListener {
        public ExternalCallStateListener() {
        }

        public void onImsExternalCallStateUpdate(List<ImsExternalCallState> list) {
            ImsExternalCallTracker.this.refreshExternalCallState(list);
        }
    }

    public class ExternalConnectionListener implements ImsExternalConnection.Listener {
        public ExternalConnectionListener() {
        }

        @Override
        public void onPullExternalCall(ImsExternalConnection imsExternalConnection) {
            Log.d(ImsExternalCallTracker.TAG, "onPullExternalCall: connection = " + imsExternalConnection);
            if (ImsExternalCallTracker.this.mCallPuller == null) {
                Log.e(ImsExternalCallTracker.TAG, "onPullExternalCall : No call puller defined");
            } else {
                ImsExternalCallTracker.this.mCallPuller.pullExternalCall(imsExternalConnection.getAddress(), imsExternalConnection.getVideoState(), imsExternalConnection.getCallId());
            }
        }
    }

    @VisibleForTesting
    public ImsExternalCallTracker(ImsPhone imsPhone, ImsPullCall imsPullCall, ImsCallNotify imsCallNotify) {
        this.mExternalConnections = new ArrayMap();
        this.mExternalCallPullableState = new ArrayMap();
        this.mExternalConnectionListener = new ExternalConnectionListener();
        this.mHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                if (message.what == 1) {
                    ImsExternalCallTracker.this.handleVideoCapabilitiesChanged((AsyncResult) message.obj);
                }
            }
        };
        this.mPhone = imsPhone;
        this.mCallStateNotifier = imsCallNotify;
        this.mExternalCallStateListener = new ExternalCallStateListener();
        this.mCallPuller = imsPullCall;
    }

    public ImsExternalCallTracker(ImsPhone imsPhone) {
        this.mExternalConnections = new ArrayMap();
        this.mExternalCallPullableState = new ArrayMap();
        this.mExternalConnectionListener = new ExternalConnectionListener();
        this.mHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                if (message.what == 1) {
                    ImsExternalCallTracker.this.handleVideoCapabilitiesChanged((AsyncResult) message.obj);
                }
            }
        };
        this.mPhone = imsPhone;
        this.mCallStateNotifier = new ImsCallNotify() {
            @Override
            public void notifyUnknownConnection(Connection connection) {
                ImsExternalCallTracker.this.mPhone.notifyUnknownConnection(connection);
            }

            @Override
            public void notifyPreciseCallStateChanged() {
                ImsExternalCallTracker.this.mPhone.notifyPreciseCallStateChanged();
            }
        };
        this.mExternalCallStateListener = new ExternalCallStateListener();
        registerForNotifications();
    }

    public void tearDown() {
        unregisterForNotifications();
    }

    public void setCallPuller(ImsPullCall imsPullCall) {
        this.mCallPuller = imsPullCall;
    }

    public ExternalCallStateListener getExternalCallStateListener() {
        return this.mExternalCallStateListener;
    }

    @Override
    public void onPhoneStateChanged(PhoneConstants.State state, PhoneConstants.State state2) {
        this.mHasActiveCalls = state2 != PhoneConstants.State.IDLE;
        Log.i(TAG, "onPhoneStateChanged : hasActiveCalls = " + this.mHasActiveCalls);
        refreshCallPullState();
    }

    private void registerForNotifications() {
        if (this.mPhone != null) {
            Log.d(TAG, "Registering: " + this.mPhone);
            this.mPhone.getDefaultPhone().registerForVideoCapabilityChanged(this.mHandler, 1, null);
        }
    }

    private void unregisterForNotifications() {
        if (this.mPhone != null) {
            Log.d(TAG, "Unregistering: " + this.mPhone);
            this.mPhone.getDefaultPhone().unregisterForVideoCapabilityChanged(this.mHandler);
        }
    }

    public void refreshExternalCallState(List<ImsExternalCallState> list) {
        Log.d(TAG, "refreshExternalCallState");
        Iterator<Map.Entry<Integer, ImsExternalConnection>> it = this.mExternalConnections.entrySet().iterator();
        boolean z = false;
        while (it.hasNext()) {
            Map.Entry<Integer, ImsExternalConnection> next = it.next();
            if (!containsCallId(list, next.getKey().intValue())) {
                ImsExternalConnection value = next.getValue();
                value.setTerminated();
                value.removeListener(this.mExternalConnectionListener);
                it.remove();
                z = true;
            }
        }
        if (z) {
            this.mCallStateNotifier.notifyPreciseCallStateChanged();
        }
        if (list != null && !list.isEmpty()) {
            for (ImsExternalCallState imsExternalCallState : list) {
                if (!this.mExternalConnections.containsKey(Integer.valueOf(imsExternalCallState.getCallId()))) {
                    Log.d(TAG, "refreshExternalCallState: got = " + imsExternalCallState);
                    if (imsExternalCallState.getCallState() == 1) {
                        createExternalConnection(imsExternalCallState);
                    }
                } else {
                    updateExistingConnection(this.mExternalConnections.get(Integer.valueOf(imsExternalCallState.getCallId())), imsExternalCallState);
                }
            }
        }
    }

    public Connection getConnectionById(int i) {
        return this.mExternalConnections.get(Integer.valueOf(i));
    }

    private void createExternalConnection(ImsExternalCallState imsExternalCallState) {
        Log.i(TAG, "createExternalConnection : state = " + imsExternalCallState);
        int videoStateFromCallType = ImsCallProfile.getVideoStateFromCallType(imsExternalCallState.getCallType());
        boolean zIsCallPullPermitted = isCallPullPermitted(imsExternalCallState.isCallPullable(), videoStateFromCallType);
        ImsExternalConnection imsExternalConnection = new ImsExternalConnection(this.mPhone, imsExternalCallState.getCallId(), imsExternalCallState.getAddress(), zIsCallPullPermitted);
        imsExternalConnection.setVideoState(videoStateFromCallType);
        imsExternalConnection.addListener(this.mExternalConnectionListener);
        Log.d(TAG, "createExternalConnection - pullable state : externalCallId = " + imsExternalConnection.getCallId() + " ; isPullable = " + zIsCallPullPermitted + " ; networkPullable = " + imsExternalCallState.isCallPullable() + " ; isVideo = " + VideoProfile.isVideo(videoStateFromCallType) + " ; videoEnabled = " + this.mIsVideoCapable + " ; hasActiveCalls = " + this.mHasActiveCalls);
        this.mExternalConnections.put(Integer.valueOf(imsExternalConnection.getCallId()), imsExternalConnection);
        this.mExternalCallPullableState.put(Integer.valueOf(imsExternalConnection.getCallId()), Boolean.valueOf(imsExternalCallState.isCallPullable()));
        this.mCallStateNotifier.notifyUnknownConnection(imsExternalConnection);
    }

    private void updateExistingConnection(ImsExternalConnection imsExternalConnection, ImsExternalCallState imsExternalCallState) {
        Log.i(TAG, "updateExistingConnection : state = " + imsExternalCallState);
        Call.State state = imsExternalConnection.getState();
        Call.State state2 = imsExternalCallState.getCallState() == 1 ? Call.State.ACTIVE : Call.State.DISCONNECTED;
        if (state != state2) {
            if (state2 == Call.State.ACTIVE) {
                imsExternalConnection.setActive();
            } else {
                imsExternalConnection.setTerminated();
                imsExternalConnection.removeListener(this.mExternalConnectionListener);
                this.mExternalConnections.remove(Integer.valueOf(imsExternalConnection.getCallId()));
                this.mExternalCallPullableState.remove(Integer.valueOf(imsExternalConnection.getCallId()));
                this.mCallStateNotifier.notifyPreciseCallStateChanged();
            }
        }
        int videoStateFromCallType = ImsCallProfile.getVideoStateFromCallType(imsExternalCallState.getCallType());
        if (videoStateFromCallType != imsExternalConnection.getVideoState()) {
            imsExternalConnection.setVideoState(videoStateFromCallType);
        }
        this.mExternalCallPullableState.put(Integer.valueOf(imsExternalCallState.getCallId()), Boolean.valueOf(imsExternalCallState.isCallPullable()));
        boolean zIsCallPullPermitted = isCallPullPermitted(imsExternalCallState.isCallPullable(), videoStateFromCallType);
        Log.d(TAG, "updateExistingConnection - pullable state : externalCallId = " + imsExternalConnection.getCallId() + " ; isPullable = " + zIsCallPullPermitted + " ; networkPullable = " + imsExternalCallState.isCallPullable() + " ; isVideo = " + VideoProfile.isVideo(imsExternalConnection.getVideoState()) + " ; videoEnabled = " + this.mIsVideoCapable + " ; hasActiveCalls = " + this.mHasActiveCalls);
        imsExternalConnection.setIsPullable(zIsCallPullPermitted);
    }

    private void refreshCallPullState() {
        Log.d(TAG, "refreshCallPullState");
        for (ImsExternalConnection imsExternalConnection : this.mExternalConnections.values()) {
            boolean zBooleanValue = this.mExternalCallPullableState.get(Integer.valueOf(imsExternalConnection.getCallId())).booleanValue();
            boolean zIsCallPullPermitted = isCallPullPermitted(zBooleanValue, imsExternalConnection.getVideoState());
            Log.d(TAG, "refreshCallPullState : externalCallId = " + imsExternalConnection.getCallId() + " ; isPullable = " + zIsCallPullPermitted + " ; networkPullable = " + zBooleanValue + " ; isVideo = " + VideoProfile.isVideo(imsExternalConnection.getVideoState()) + " ; videoEnabled = " + this.mIsVideoCapable + " ; hasActiveCalls = " + this.mHasActiveCalls);
            imsExternalConnection.setIsPullable(zIsCallPullPermitted);
        }
    }

    private boolean containsCallId(List<ImsExternalCallState> list, int i) {
        if (list == null) {
            return false;
        }
        Iterator<ImsExternalCallState> it = list.iterator();
        while (it.hasNext()) {
            if (it.next().getCallId() == i) {
                return true;
            }
        }
        return false;
    }

    private void handleVideoCapabilitiesChanged(AsyncResult asyncResult) {
        this.mIsVideoCapable = ((Boolean) asyncResult.result).booleanValue();
        Log.i(TAG, "handleVideoCapabilitiesChanged : isVideoCapable = " + this.mIsVideoCapable);
        refreshCallPullState();
    }

    private boolean isCallPullPermitted(boolean z, int i) {
        if ((!VideoProfile.isVideo(i) || this.mIsVideoCapable) && !this.mHasActiveCalls) {
            return z;
        }
        return false;
    }
}
