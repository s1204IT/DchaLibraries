package com.android.server.telecom;

import android.content.ComponentName;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.telecom.Log;
import android.telecom.Logging.Session;
import android.text.TextUtils;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.telecom.CallsManager;
import com.android.server.telecom.ConnectionServiceFocusManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ConnectionServiceFocusManager {
    private static final int[] PRIORITY_FOCUS_CALL_STATE = {5, 1, 3};

    @VisibleForTesting
    public static final int RELEASE_FOCUS_TIMEOUT_MS = 5000;
    private final List<CallFocus> mCalls;
    private CallsManagerRequester mCallsManagerRequester;
    private ConnectionServiceFocus mCurrentFocus;
    private CallFocus mCurrentFocusCall;
    private FocusRequest mCurrentFocusRequest;
    private FocusManagerHandler mEventHandler;
    private final CallsManagerListenerBase mCallsManagerListener = new CallsManagerListenerBase() {
        @Override
        public void onCallAdded(Call call) {
            if (callShouldBeIgnored(call)) {
                return;
            }
            ConnectionServiceFocusManager.this.mEventHandler.obtainMessage(5, new MessageArgs(Log.createSubsession(), "CSFM.oCA", call)).sendToTarget();
        }

        @Override
        public void onCallRemoved(Call call) {
            if (callShouldBeIgnored(call)) {
                return;
            }
            ConnectionServiceFocusManager.this.mEventHandler.obtainMessage(6, new MessageArgs(Log.createSubsession(), "CSFM.oCR", call)).sendToTarget();
        }

        @Override
        public void onCallStateChanged(Call call, int i, int i2) {
            if (callShouldBeIgnored(call)) {
                return;
            }
            ConnectionServiceFocusManager.this.mEventHandler.obtainMessage(7, i, i2, new MessageArgs(Log.createSubsession(), "CSFM.oCSS", call)).sendToTarget();
        }

        @Override
        public void onExternalCallChanged(Call call, boolean z) {
            if (z) {
                ConnectionServiceFocusManager.this.mEventHandler.obtainMessage(6, new MessageArgs(Log.createSubsession(), "CSFM.oECC", call)).sendToTarget();
            } else {
                ConnectionServiceFocusManager.this.mEventHandler.obtainMessage(5, new MessageArgs(Log.createSubsession(), "CSFM.oECC", call)).sendToTarget();
            }
        }

        boolean callShouldBeIgnored(Call call) {
            return call.isExternalCall();
        }
    };
    private final ConnectionServiceFocusListener mConnectionServiceFocusListener = new ConnectionServiceFocusListener() {
        @Override
        public void onConnectionServiceReleased(ConnectionServiceFocus connectionServiceFocus) {
            ConnectionServiceFocusManager.this.mEventHandler.obtainMessage(2, new MessageArgs(Log.createSubsession(), "CSFM.oCSR", connectionServiceFocus)).sendToTarget();
        }

        @Override
        public void onConnectionServiceDeath(ConnectionServiceFocus connectionServiceFocus) {
            ConnectionServiceFocusManager.this.mEventHandler.obtainMessage(4, new MessageArgs(Log.createSubsession(), "CSFM.oCSD", connectionServiceFocus)).sendToTarget();
        }
    };

    public interface CallFocus {
        ConnectionServiceFocus getConnectionServiceWrapper();

        int getState();

        boolean isFocusable();
    }

    public interface CallsManagerRequester {
        void releaseConnectionService(ConnectionServiceFocus connectionServiceFocus);

        void setCallsManagerListener(CallsManager.CallsManagerListener callsManagerListener);
    }

    public interface ConnectionServiceFocus {
        void connectionServiceFocusGained();

        void connectionServiceFocusLost();

        ComponentName getComponentName();

        void setConnectionServiceFocusListener(ConnectionServiceFocusListener connectionServiceFocusListener);
    }

    public interface ConnectionServiceFocusListener {
        void onConnectionServiceDeath(ConnectionServiceFocus connectionServiceFocus);

        void onConnectionServiceReleased(ConnectionServiceFocus connectionServiceFocus);
    }

    public interface ConnectionServiceFocusManagerFactory {
        ConnectionServiceFocusManager create(CallsManagerRequester callsManagerRequester, Looper looper);
    }

    public interface RequestFocusCallback {
        void onRequestFocusDone(CallFocus callFocus);
    }

    public ConnectionServiceFocusManager(CallsManagerRequester callsManagerRequester, Looper looper) {
        this.mCallsManagerRequester = callsManagerRequester;
        this.mCallsManagerRequester.setCallsManagerListener(this.mCallsManagerListener);
        this.mEventHandler = new FocusManagerHandler(looper);
        this.mCalls = new ArrayList();
    }

    public void requestFocus(CallFocus callFocus, RequestFocusCallback requestFocusCallback) {
        this.mEventHandler.obtainMessage(1, new MessageArgs(Log.createSubsession(), "CSFM.rF", new FocusRequest(callFocus, requestFocusCallback))).sendToTarget();
    }

    public CallFocus getCurrentFocusCall() {
        return this.mCurrentFocusCall;
    }

    @VisibleForTesting
    public Handler getHandler() {
        return this.mEventHandler;
    }

    @VisibleForTesting
    public List<CallFocus> getAllCall() {
        return this.mCalls;
    }

    private void updateConnectionServiceFocus(ConnectionServiceFocus connectionServiceFocus) {
        if (!Objects.equals(this.mCurrentFocus, connectionServiceFocus)) {
            if (connectionServiceFocus != null) {
                connectionServiceFocus.setConnectionServiceFocusListener(this.mConnectionServiceFocusListener);
                connectionServiceFocus.connectionServiceFocusGained();
            }
            this.mCurrentFocus = connectionServiceFocus;
            Log.d(this, "updateConnectionServiceFocus connSvr = %s", new Object[]{connectionServiceFocus});
        }
    }

    private void updateCurrentFocusCall() {
        this.mCurrentFocusCall = null;
        if (this.mCurrentFocus == null) {
            return;
        }
        List<CallFocus> list = (List) this.mCalls.stream().filter(new Predicate() {
            @Override
            public final boolean test(Object obj) {
                return ConnectionServiceFocusManager.lambda$updateCurrentFocusCall$0(this.f$0, (ConnectionServiceFocusManager.CallFocus) obj);
            }
        }).collect(Collectors.toList());
        for (int i = 0; i < PRIORITY_FOCUS_CALL_STATE.length; i++) {
            for (CallFocus callFocus : list) {
                if (callFocus.getState() == PRIORITY_FOCUS_CALL_STATE[i]) {
                    this.mCurrentFocusCall = callFocus;
                    Log.d(this, "updateCurrentFocusCall %s", new Object[]{this.mCurrentFocusCall});
                    return;
                }
            }
        }
        Log.d(this, "updateCurrentFocusCall = null", new Object[0]);
    }

    public static boolean lambda$updateCurrentFocusCall$0(ConnectionServiceFocusManager connectionServiceFocusManager, CallFocus callFocus) {
        return connectionServiceFocusManager.mCurrentFocus.equals(callFocus.getConnectionServiceWrapper()) && callFocus.isFocusable();
    }

    private void onRequestFocusDone(FocusRequest focusRequest) {
        if (focusRequest.callback != null) {
            focusRequest.callback.onRequestFocusDone(focusRequest.call);
        }
    }

    private void handleRequestFocus(FocusRequest focusRequest) {
        Log.d(this, "handleRequestFocus req = %s", new Object[]{focusRequest});
        if (this.mCurrentFocus == null || this.mCurrentFocus.equals(focusRequest.call.getConnectionServiceWrapper())) {
            updateConnectionServiceFocus(focusRequest.call.getConnectionServiceWrapper());
            updateCurrentFocusCall();
            onRequestFocusDone(focusRequest);
        } else {
            this.mCurrentFocus.connectionServiceFocusLost();
            this.mCurrentFocusRequest = focusRequest;
            this.mEventHandler.sendMessageDelayed(this.mEventHandler.obtainMessage(3, new MessageArgs(Log.createSubsession(), "CSFM.hRF", focusRequest)), 5000L);
        }
    }

    private void handleReleasedFocus(ConnectionServiceFocus connectionServiceFocus) {
        ConnectionServiceFocus connectionServiceWrapper;
        Log.d(this, "handleReleasedFocus connSvr = %s", new Object[]{connectionServiceFocus});
        if (Objects.equals(this.mCurrentFocus, connectionServiceFocus)) {
            this.mEventHandler.removeMessages(3);
            if (this.mCurrentFocusRequest != null) {
                connectionServiceWrapper = this.mCurrentFocusRequest.call.getConnectionServiceWrapper();
            } else {
                connectionServiceWrapper = null;
            }
            updateConnectionServiceFocus(connectionServiceWrapper);
            updateCurrentFocusCall();
            if (this.mCurrentFocusRequest != null) {
                onRequestFocusDone(this.mCurrentFocusRequest);
                this.mCurrentFocusRequest = null;
            }
        }
    }

    private void handleReleasedFocusTimeout(FocusRequest focusRequest) {
        Log.d(this, "handleReleasedFocusTimeout req = %s", new Object[]{focusRequest});
        this.mCallsManagerRequester.releaseConnectionService(this.mCurrentFocus);
        updateConnectionServiceFocus(focusRequest.call.getConnectionServiceWrapper());
        updateCurrentFocusCall();
        onRequestFocusDone(focusRequest);
        this.mCurrentFocusRequest = null;
    }

    private void handleConnectionServiceDeath(ConnectionServiceFocus connectionServiceFocus) {
        Log.d(this, "handleConnectionServiceDeath %s", new Object[]{connectionServiceFocus});
        if (Objects.equals(connectionServiceFocus, this.mCurrentFocus)) {
            updateConnectionServiceFocus(null);
            updateCurrentFocusCall();
        }
    }

    private void handleAddedCall(CallFocus callFocus) {
        Log.d(this, "handleAddedCall %s", new Object[]{callFocus});
        if (!this.mCalls.contains(callFocus)) {
            this.mCalls.add(callFocus);
        }
        if (Objects.equals(this.mCurrentFocus, callFocus.getConnectionServiceWrapper())) {
            updateCurrentFocusCall();
        }
    }

    private void handleRemovedCall(CallFocus callFocus) {
        Log.d(this, "handleRemovedCall %s", new Object[]{callFocus});
        this.mCalls.remove(callFocus);
        if (callFocus.equals(this.mCurrentFocusCall)) {
            updateCurrentFocusCall();
        }
    }

    private void handleCallStateChanged(CallFocus callFocus, int i, int i2) {
        Log.d(this, "handleCallStateChanged %s, oldState = %d, newState = %d", new Object[]{callFocus, Integer.valueOf(i), Integer.valueOf(i2)});
        if (this.mCalls.contains(callFocus) && Objects.equals(this.mCurrentFocus, callFocus.getConnectionServiceWrapper())) {
            updateCurrentFocusCall();
        }
    }

    private final class FocusManagerHandler extends Handler {
        FocusManagerHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            Session session = ((MessageArgs) message.obj).logSession;
            String str = ((MessageArgs) message.obj).shortName;
            if (TextUtils.isEmpty(str)) {
                str = "hM";
            }
            Log.continueSession(session, str);
            Object obj = ((MessageArgs) message.obj).obj;
            try {
                switch (message.what) {
                    case 1:
                        ConnectionServiceFocusManager.this.handleRequestFocus((FocusRequest) obj);
                        break;
                    case CallState.SELECT_PHONE_ACCOUNT:
                        ConnectionServiceFocusManager.this.handleReleasedFocus((ConnectionServiceFocus) obj);
                        break;
                    case CallState.DIALING:
                        ConnectionServiceFocusManager.this.handleReleasedFocusTimeout((FocusRequest) obj);
                        break;
                    case CallState.RINGING:
                        ConnectionServiceFocusManager.this.handleConnectionServiceDeath((ConnectionServiceFocus) obj);
                        break;
                    case CallState.ACTIVE:
                        ConnectionServiceFocusManager.this.handleAddedCall((CallFocus) obj);
                        break;
                    case CallState.ON_HOLD:
                        ConnectionServiceFocusManager.this.handleRemovedCall((CallFocus) obj);
                        break;
                    case CallState.DISCONNECTED:
                        ConnectionServiceFocusManager.this.handleCallStateChanged((CallFocus) obj, message.arg1, message.arg2);
                        break;
                }
            } finally {
                Log.endSession();
            }
        }
    }

    private static final class FocusRequest {
        CallFocus call;
        RequestFocusCallback callback;

        FocusRequest(CallFocus callFocus, RequestFocusCallback requestFocusCallback) {
            this.call = callFocus;
            this.callback = requestFocusCallback;
        }
    }

    private static final class MessageArgs {
        Session logSession;
        Object obj;
        String shortName;

        MessageArgs(Session session, String str, Object obj) {
            this.logSession = session;
            this.shortName = str;
            this.obj = obj;
        }
    }
}
