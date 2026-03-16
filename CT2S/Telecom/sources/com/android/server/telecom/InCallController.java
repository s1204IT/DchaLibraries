package com.android.server.telecom;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.net.Uri;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.Trace;
import android.os.UserHandle;
import android.telecom.AudioState;
import android.telecom.ParcelableCall;
import android.util.ArrayMap;
import com.android.internal.telecom.IInCallService;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.telecom.Call;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class InCallController extends CallsManagerListenerBase {
    private static final int[] CONNECTION_TO_CALL_CAPABILITY = {1, 1, 2, 2, 4, 4, 8, 8, 16, 16, 32, 32, 64, 64, 128, 128, 256, 256, 512, 512, 1024, 1024, 2048, 2048, 4096, 4096, 8192, 8192, 16384, 16384};
    private final Context mContext;
    private final ComponentName mInCallComponentName;
    private final Call.Listener mCallListener = new Call.ListenerBase() {
        @Override
        public void onConnectionCapabilitiesChanged(Call call) {
            InCallController.this.updateCall(call);
        }

        @Override
        public void onCannedSmsResponsesLoaded(Call call) {
            InCallController.this.updateCall(call);
        }

        @Override
        public void onVideoCallProviderChanged(Call call) {
            InCallController.this.updateCall(call);
        }

        @Override
        public void onStatusHintsChanged(Call call) {
            InCallController.this.updateCall(call);
        }

        @Override
        public void onHandleChanged(Call call) {
            InCallController.this.updateCall(call);
        }

        @Override
        public void onCallerDisplayNameChanged(Call call) {
            InCallController.this.updateCall(call);
        }

        @Override
        public void onVideoStateChanged(Call call) {
            InCallController.this.updateCall(call);
        }

        @Override
        public void onTargetPhoneAccountChanged(Call call) {
            InCallController.this.updateCall(call);
        }

        @Override
        public void onConferenceableCallsChanged(Call call) {
            InCallController.this.updateCall(call);
        }
    };
    private final Map<ComponentName, InCallServiceConnection> mServiceConnections = new ConcurrentHashMap(8, 0.9f, 1);
    private final Map<ComponentName, IInCallService> mInCallServices = new ArrayMap();
    private final CallIdMapper mCallIdMapper = new CallIdMapper("InCall");

    @Override
    public void onForegroundCallChanged(Call call, Call call2) {
        super.onForegroundCallChanged(call, call2);
    }

    @Override
    public void onIncomingCallAnswered(Call call) {
        super.onIncomingCallAnswered(call);
    }

    @Override
    public void onIncomingCallRejected(Call call, boolean z, String str) {
        super.onIncomingCallRejected(call, z, str);
    }

    @Override
    public void onIsVoipAudioModeChanged(Call call) {
        super.onIsVoipAudioModeChanged(call);
    }

    @Override
    public void onRingbackRequested(Call call, boolean z) {
        super.onRingbackRequested(call, z);
    }

    @Override
    public void onVideoStateChanged(Call call) {
        super.onVideoStateChanged(call);
    }

    private class InCallServiceConnection implements ServiceConnection {
        private InCallServiceConnection() {
        }

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.d(this, "onServiceConnected: %s", componentName);
            InCallController.this.onConnected(componentName, iBinder);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d(this, "onDisconnected: %s", componentName);
            InCallController.this.onDisconnected(componentName);
        }
    }

    public InCallController(Context context) {
        this.mContext = context;
        Resources resources = this.mContext.getResources();
        this.mInCallComponentName = new ComponentName(resources.getString(R.string.ui_default_package), resources.getString(R.string.incall_default_class));
    }

    @Override
    public void onCallAdded(Call call) {
        if (this.mInCallServices.isEmpty()) {
            bind(call);
            return;
        }
        Log.i(this, "onCallAdded: %s", call);
        addCall(call);
        for (Map.Entry<ComponentName, IInCallService> entry : this.mInCallServices.entrySet()) {
            try {
                entry.getValue().addCall(toParcelableCall(call, entry.getKey().equals(this.mInCallComponentName)));
            } catch (RemoteException e) {
            }
        }
    }

    @Override
    public void onCallRemoved(Call call) {
        Log.i(this, "onCallRemoved: %s", call);
        if (CallsManager.getInstance().getCalls().isEmpty()) {
            unbind();
        }
        call.removeListener(this.mCallListener);
        this.mCallIdMapper.removeCall(call);
    }

    @Override
    public void onCallStateChanged(Call call, int i, int i2) {
        updateCall(call);
    }

    @Override
    public void onConnectionServiceChanged(Call call, ConnectionServiceWrapper connectionServiceWrapper, ConnectionServiceWrapper connectionServiceWrapper2) {
        updateCall(call);
    }

    @Override
    public void onAudioStateChanged(AudioState audioState, AudioState audioState2) {
        if (!this.mInCallServices.isEmpty()) {
            Log.i(this, "Calling onAudioStateChanged, audioState: %s -> %s", audioState, audioState2);
            Iterator<IInCallService> it = this.mInCallServices.values().iterator();
            while (it.hasNext()) {
                try {
                    it.next().onAudioStateChanged(audioState2);
                } catch (RemoteException e) {
                }
            }
        }
    }

    @Override
    public void onCanAddCallChanged(boolean z) {
        if (!this.mInCallServices.isEmpty()) {
            Log.i(this, "onCanAddCallChanged : %b", Boolean.valueOf(z));
            Iterator<IInCallService> it = this.mInCallServices.values().iterator();
            while (it.hasNext()) {
                try {
                    it.next().onCanAddCallChanged(z);
                } catch (RemoteException e) {
                }
            }
        }
    }

    void onPostDialWait(Call call, String str) {
        if (!this.mInCallServices.isEmpty()) {
            Log.i(this, "Calling onPostDialWait, remaining = %s", str);
            Iterator<IInCallService> it = this.mInCallServices.values().iterator();
            while (it.hasNext()) {
                try {
                    it.next().setPostDialWait(this.mCallIdMapper.getCallId(call), str);
                } catch (RemoteException e) {
                }
            }
        }
    }

    @Override
    public void onIsConferencedChanged(Call call) {
        Log.d(this, "onIsConferencedChanged %s", call);
        updateCall(call);
    }

    void bringToForeground(boolean z) {
        if (!this.mInCallServices.isEmpty()) {
            Iterator<IInCallService> it = this.mInCallServices.values().iterator();
            while (it.hasNext()) {
                try {
                    it.next().bringToForeground(z);
                } catch (RemoteException e) {
                }
            }
            return;
        }
        Log.w(this, "Asking to bring unbound in-call UI to foreground.", new Object[0]);
    }

    private void unbind() {
        ThreadUtil.checkOnMainThread();
        Iterator<Map.Entry<ComponentName, InCallServiceConnection>> it = this.mServiceConnections.entrySet().iterator();
        while (it.hasNext()) {
            Log.i(this, "Unbinding from InCallService %s", new Object[0]);
            this.mContext.unbindService(it.next().getValue());
            it.remove();
        }
        this.mInCallServices.clear();
    }

    private void bind(Call call) {
        int i;
        ThreadUtil.checkOnMainThread();
        if (this.mInCallServices.isEmpty()) {
            PackageManager packageManager = this.mContext.getPackageManager();
            Iterator<ResolveInfo> it = packageManager.queryIntentServices(new Intent("android.telecom.InCallService"), 0).iterator();
            while (it.hasNext()) {
                ServiceInfo serviceInfo = it.next().serviceInfo;
                if (serviceInfo != null) {
                    boolean z = serviceInfo.permission != null && serviceInfo.permission.equals("android.permission.BIND_INCALL_SERVICE");
                    boolean z2 = packageManager.checkPermission("android.permission.CONTROL_INCALL_EXPERIENCE", serviceInfo.packageName) == 0;
                    if (!z) {
                        Log.w(this, "InCallService does not have BIND_INCALL_SERVICE permission: " + serviceInfo.packageName, new Object[0]);
                    } else if (!z2) {
                        Log.w(this, "InCall UI does not have CONTROL_INCALL_EXPERIENCE permission: " + serviceInfo.packageName, new Object[0]);
                    } else {
                        InCallServiceConnection inCallServiceConnection = new InCallServiceConnection();
                        ComponentName componentName = new ComponentName(serviceInfo.packageName, serviceInfo.name);
                        Log.i(this, "Attempting to bind to InCall %s, is dupe? %b ", serviceInfo.packageName, Boolean.valueOf(this.mServiceConnections.containsKey(componentName)));
                        if (!this.mServiceConnections.containsKey(componentName)) {
                            Intent intent = new Intent("android.telecom.InCallService");
                            intent.setComponent(componentName);
                            if (this.mInCallComponentName.equals(componentName)) {
                                i = 65;
                                if (!call.isIncoming()) {
                                    intent.putExtra("android.telecom.extra.OUTGOING_CALL_EXTRAS", call.getExtras());
                                    intent.putExtra("android.telecom.extra.PHONE_ACCOUNT_HANDLE", call.getTargetPhoneAccount());
                                }
                            } else {
                                i = 1;
                            }
                            if (this.mContext.bindServiceAsUser(intent, inCallServiceConnection, i, UserHandle.CURRENT)) {
                                this.mServiceConnections.put(componentName, inCallServiceConnection);
                            }
                        }
                    }
                }
            }
        }
    }

    private void onConnected(ComponentName componentName, IBinder iBinder) {
        ThreadUtil.checkOnMainThread();
        Trace.beginSection("onConnected: " + componentName);
        Log.i(this, "onConnected to %s", componentName);
        IInCallService iInCallServiceAsInterface = IInCallService.Stub.asInterface(iBinder);
        try {
            iInCallServiceAsInterface.setInCallAdapter(new InCallAdapter(CallsManager.getInstance(), this.mCallIdMapper));
            this.mInCallServices.put(componentName, iInCallServiceAsInterface);
            Collection<Call> calls = CallsManager.getInstance().getCalls();
            if (!calls.isEmpty()) {
                Log.i(this, "Adding %s calls to InCallService after onConnected: %s", Integer.valueOf(calls.size()), componentName);
                for (Call call : calls) {
                    try {
                        Log.i(this, "addCall after binding: %s", call);
                        addCall(call);
                        iInCallServiceAsInterface.addCall(toParcelableCall(call, componentName.equals(this.mInCallComponentName)));
                    } catch (RemoteException e) {
                    }
                }
                onAudioStateChanged(null, CallsManager.getInstance().getAudioState());
                onCanAddCallChanged(CallsManager.getInstance().canAddCall());
            } else {
                unbind();
            }
            Trace.endSection();
        } catch (RemoteException e2) {
            Log.e(this, e2, "Failed to set the in-call adapter.", new Object[0]);
            Trace.endSection();
        }
    }

    private void onDisconnected(ComponentName componentName) {
        Log.i(this, "onDisconnected from %s", componentName);
        ThreadUtil.checkOnMainThread();
        if (this.mInCallServices.containsKey(componentName)) {
            this.mInCallServices.remove(componentName);
        }
        if (this.mServiceConnections.containsKey(componentName)) {
            if (componentName.equals(this.mInCallComponentName)) {
                Log.i(this, "In-call UI %s disconnected.", componentName);
                CallsManager.getInstance().disconnectAllCalls();
                unbind();
            } else {
                Log.i(this, "In-Call Service %s suddenly disconnected", componentName);
                this.mContext.unbindService(this.mServiceConnections.get(componentName));
                this.mServiceConnections.remove(componentName);
                this.mInCallServices.remove(componentName);
            }
        }
    }

    private void updateCall(Call call) {
        if (!this.mInCallServices.isEmpty()) {
            for (Map.Entry<ComponentName, IInCallService> entry : this.mInCallServices.entrySet()) {
                ComponentName key = entry.getKey();
                IInCallService value = entry.getValue();
                ParcelableCall parcelableCall = toParcelableCall(call, key.equals(this.mInCallComponentName));
                Log.v(this, "updateCall %s ==> %s", call, parcelableCall);
                try {
                    value.updateCall(parcelableCall);
                } catch (RemoteException e) {
                }
            }
        }
    }

    private ParcelableCall toParcelableCall(Call call, boolean z) {
        String callId = this.mCallIdMapper.getCallId(call);
        int state = call.getState();
        int iConvertConnectionToCallCapabilities = convertConnectionToCallCapabilities(call.getConnectionCapabilities());
        boolean zIsUserSelectedSmsPhoneAccount = CallsManager.getInstance().getPhoneAccountRegistrar().isUserSelectedSmsPhoneAccount(call.getTargetPhoneAccount());
        if (call.isRespondViaSmsCapable() && zIsUserSelectedSmsPhoneAccount) {
            iConvertConnectionToCallCapabilities |= 32;
        }
        int iRemoveCapability = call.isEmergencyCall() ? removeCapability(iConvertConnectionToCallCapabilities, 64) : iConvertConnectionToCallCapabilities;
        if (state == 3) {
            iRemoveCapability = removeCapability(removeCapability(iRemoveCapability, 256), 512);
        }
        int i = state == 8 ? 7 : state;
        int i2 = (!call.isLocallyDisconnecting() || i == 7) ? i : 9;
        String callId2 = null;
        Call parentCall = call.getParentCall();
        if (parentCall != null) {
            callId2 = this.mCallIdMapper.getCallId(parentCall);
        }
        long connectTimeMillis = call.getConnectTimeMillis();
        List<Call> childCalls = call.getChildCalls();
        ArrayList arrayList = new ArrayList();
        if (!childCalls.isEmpty()) {
            long jMin = Long.MAX_VALUE;
            for (Call call2 : childCalls) {
                if (call2.getConnectTimeMillis() > 0) {
                    jMin = Math.min(call2.getConnectTimeMillis(), jMin);
                }
                arrayList.add(this.mCallIdMapper.getCallId(call2));
            }
            if (jMin != Long.MAX_VALUE) {
                connectTimeMillis = jMin;
            }
        }
        Uri handle = call.getHandlePresentation() == 1 ? call.getHandle() : null;
        String callerDisplayName = call.getCallerDisplayNamePresentation() == 1 ? call.getCallerDisplayName() : null;
        List<Call> conferenceableCalls = call.getConferenceableCalls();
        ArrayList arrayList2 = new ArrayList(conferenceableCalls.size());
        Iterator<Call> it = conferenceableCalls.iterator();
        while (it.hasNext()) {
            String callId3 = this.mCallIdMapper.getCallId(it.next());
            if (callId3 != null) {
                arrayList2.add(callId3);
            }
        }
        return new ParcelableCall(callId, i2, call.getDisconnectCause(), call.getCannedSmsResponses(), iRemoveCapability, call.isConference() ? 1 : 0, connectTimeMillis, handle, call.getHandlePresentation(), callerDisplayName, call.getCallerDisplayNamePresentation(), call.getGatewayInfo(), call.getTargetPhoneAccount(), z ? call.getVideoProvider() : null, callId2, arrayList, call.getStatusHints(), call.getVideoState(), arrayList2, call.getExtras());
    }

    private static int convertConnectionToCallCapabilities(int i) {
        int i2 = 0;
        for (int i3 = 0; i3 < CONNECTION_TO_CALL_CAPABILITY.length; i3 += 2) {
            if ((CONNECTION_TO_CALL_CAPABILITY[i3] & i) != 0) {
                i2 |= CONNECTION_TO_CALL_CAPABILITY[i3 + 1];
            }
        }
        return i2;
    }

    private void addCall(Call call) {
        if (this.mCallIdMapper.getCallId(call) == null) {
            this.mCallIdMapper.addCall(call);
            call.addListener(this.mCallListener);
        }
    }

    private static int removeCapability(int i, int i2) {
        return (i2 ^ (-1)) & i;
    }

    public void dump(IndentingPrintWriter indentingPrintWriter) {
        indentingPrintWriter.println("mInCallServices (InCalls registered):");
        indentingPrintWriter.increaseIndent();
        Iterator<ComponentName> it = this.mInCallServices.keySet().iterator();
        while (it.hasNext()) {
            indentingPrintWriter.println(it.next());
        }
        indentingPrintWriter.decreaseIndent();
        indentingPrintWriter.println("mServiceConnections (InCalls bound):");
        indentingPrintWriter.increaseIndent();
        Iterator<ComponentName> it2 = this.mServiceConnections.keySet().iterator();
        while (it2.hasNext()) {
            indentingPrintWriter.println(it2.next());
        }
        indentingPrintWriter.decreaseIndent();
    }
}
