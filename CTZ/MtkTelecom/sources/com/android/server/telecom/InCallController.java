package com.android.server.telecom;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.os.Trace;
import android.os.UserHandle;
import android.telecom.CallAudioState;
import android.telecom.DefaultDialerManager;
import android.telecom.Log;
import android.telecom.Logging.EventManager;
import android.telecom.Logging.Runnable;
import android.telecom.ParcelableCall;
import android.text.TextUtils;
import android.util.ArrayMap;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telecom.IInCallService;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.telecom.Call;
import com.android.server.telecom.CallIdMapper;
import com.android.server.telecom.InCallController;
import com.android.server.telecom.SystemStateProvider;
import com.android.server.telecom.TelecomSystem;
import com.android.server.telecom.Timeouts;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class InCallController extends CallsManagerListenerBase {
    private final CallsManager mCallsManager;
    private final Context mContext;
    private final DefaultDialerCache mDefaultDialerCache;
    private final EmergencyCallHelper mEmergencyCallHelper;
    private CarSwappingInCallServiceConnection mInCallServiceConnection;
    private ComponentName mInCallUIComponentName;
    private final TelecomSystem.SyncRoot mLock;
    private NonUIInCallServiceConnectionCollection mNonUIInCallServiceConnections;
    private final ComponentName mSystemInCallComponentName;
    private final SystemStateProvider mSystemStateProvider;
    private final Timeouts.Adapter mTimeoutsAdapter;
    private final Call.Listener mCallListener = new Call.ListenerBase() {
        @Override
        public void onConnectionCapabilitiesChanged(Call call) {
            InCallController.this.updateCall(call);
        }

        @Override
        public void onConnectionPropertiesChanged(Call call, boolean z) {
            InCallController.this.updateCall(call, false, z);
        }

        @Override
        public void onCannedSmsResponsesLoaded(Call call) {
            InCallController.this.updateCall(call);
        }

        @Override
        public void onVideoCallProviderChanged(Call call) {
            InCallController.this.updateCall(call, true, false);
        }

        @Override
        public void onStatusHintsChanged(Call call) {
            InCallController.this.updateCall(call);
        }

        @Override
        public void onExtrasChanged(Call call, int i, Bundle bundle) {
            if (i != 2) {
                InCallController.this.updateCall(call);
            }
        }

        @Override
        public void onExtrasRemoved(Call call, int i, List<String> list) {
            if (i != 2) {
                InCallController.this.updateCall(call);
            }
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
        public void onVideoStateChanged(Call call, int i, int i2) {
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

        @Override
        public void onConnectionEvent(Call call, String str, Bundle bundle) {
            InCallController.this.notifyConnectionEvent(call, str, bundle);
        }

        @Override
        public void onHandoverFailed(Call call, int i) {
            InCallController.this.notifyHandoverFailed(call, i);
        }

        @Override
        public void onHandoverComplete(Call call) {
            InCallController.this.notifyHandoverComplete(call);
        }

        @Override
        public void onRttInitiationFailure(Call call, int i) {
            InCallController.this.notifyRttInitiationFailure(call, i);
            InCallController.this.updateCall(call, false, true);
        }

        @Override
        public void onRemoteRttRequest(Call call, int i) {
            InCallController.this.notifyRemoteRttRequest(call, i);
        }

        @Override
        public void forceUpdateCall(Call call) {
            InCallController.this.updateCall(call);
        }
    };
    private final SystemStateProvider.SystemStateListener mSystemStateListener = new SystemStateProvider.SystemStateListener() {
        @Override
        public void onCarModeChanged(boolean z) {
            if (InCallController.this.mInCallServiceConnection != null) {
                InCallController.this.mInCallServiceConnection.setCarMode(InCallController.this.shouldUseCarModeUI());
            }
        }
    };
    private final Map<InCallServiceInfo, IInCallService> mInCallServices = new ArrayMap();
    private final CallIdMapper mCallIdMapper = new CallIdMapper(new CallIdMapper.ICallInfo() {
        @Override
        public final String getCallId(Call call) {
            return call.getId();
        }
    });

    public class InCallServiceConnection {
        protected Listener mListener;

        public InCallServiceConnection() {
        }

        public class Listener {
            public Listener() {
            }

            public void onDisconnect(InCallServiceConnection inCallServiceConnection) {
            }
        }

        public int connect(Call call) {
            return 2;
        }

        public void disconnect() {
        }

        public boolean isConnected() {
            return false;
        }

        public void setHasEmergency(boolean z) {
        }

        public void setListener(Listener listener) {
            this.mListener = listener;
        }

        public InCallServiceInfo getInfo() {
            return null;
        }

        public void dump(IndentingPrintWriter indentingPrintWriter) {
        }
    }

    private class InCallServiceInfo {
        private final ComponentName mComponentName;
        private boolean mIsExternalCallsSupported;
        private boolean mIsSelfManagedCallsSupported;
        private final int mType;

        public InCallServiceInfo(ComponentName componentName, boolean z, boolean z2, int i) {
            this.mComponentName = componentName;
            this.mIsExternalCallsSupported = z;
            this.mIsSelfManagedCallsSupported = z2;
            this.mType = i;
        }

        public ComponentName getComponentName() {
            return this.mComponentName;
        }

        public boolean isExternalCallsSupported() {
            return this.mIsExternalCallsSupported;
        }

        public boolean isSelfManagedCallsSupported() {
            return this.mIsSelfManagedCallsSupported;
        }

        public int getType() {
            return this.mType;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            InCallServiceInfo inCallServiceInfo = (InCallServiceInfo) obj;
            if (this.mIsExternalCallsSupported != inCallServiceInfo.mIsExternalCallsSupported || this.mIsSelfManagedCallsSupported != inCallServiceInfo.mIsSelfManagedCallsSupported) {
                return false;
            }
            return this.mComponentName.equals(inCallServiceInfo.mComponentName);
        }

        public int hashCode() {
            return Objects.hash(this.mComponentName, Boolean.valueOf(this.mIsExternalCallsSupported), Boolean.valueOf(this.mIsSelfManagedCallsSupported));
        }

        public String toString() {
            return "[" + this.mComponentName + " supportsExternal? " + this.mIsExternalCallsSupported + " supportsSelfMg?" + this.mIsSelfManagedCallsSupported + "]";
        }
    }

    private class InCallServiceBindingConnection extends InCallServiceConnection {
        private final InCallServiceInfo mInCallServiceInfo;
        private boolean mIsBound;
        private boolean mIsConnected;
        private final ServiceConnection mServiceConnection;

        public InCallServiceBindingConnection(InCallServiceInfo inCallServiceInfo) {
            super();
            this.mServiceConnection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                    Log.startSession("ICSBC.oSC");
                    synchronized (InCallController.this.mLock) {
                        try {
                            Log.d(this, "onServiceConnected: %s %b %b", new Object[]{componentName, Boolean.valueOf(InCallServiceBindingConnection.this.mIsBound), Boolean.valueOf(InCallServiceBindingConnection.this.mIsConnected)});
                            InCallServiceBindingConnection.this.mIsBound = true;
                            if (InCallServiceBindingConnection.this.mIsConnected) {
                                InCallServiceBindingConnection.this.onConnected(iBinder);
                            }
                        } finally {
                            Log.endSession();
                        }
                    }
                }

                @Override
                public void onServiceDisconnected(ComponentName componentName) {
                    Log.startSession("ICSBC.oSD");
                    synchronized (InCallController.this.mLock) {
                        try {
                            Log.d(this, "onDisconnected: %s", new Object[]{componentName});
                            InCallServiceBindingConnection.this.mIsBound = false;
                            InCallServiceBindingConnection.this.onDisconnected();
                        } finally {
                            Log.endSession();
                        }
                    }
                }
            };
            this.mIsConnected = false;
            this.mIsBound = false;
            this.mInCallServiceInfo = inCallServiceInfo;
        }

        @Override
        public int connect(Call call) {
            if (this.mIsConnected) {
                Log.addEvent(call, "INFO", "Already connected, ignoring request.");
                return 1;
            }
            if (call != null && call.isSelfManaged() && !this.mInCallServiceInfo.isSelfManagedCallsSupported()) {
                Log.i(this, "Skipping binding to %s - doesn't support self-mgd calls", new Object[]{this.mInCallServiceInfo});
                this.mIsConnected = false;
                return 3;
            }
            Intent intent = new Intent("android.telecom.InCallService");
            intent.setComponent(this.mInCallServiceInfo.getComponentName());
            if (call != null && !call.isIncoming() && !call.isExternalCall()) {
                intent.putExtra("android.telecom.extra.OUTGOING_CALL_EXTRAS", call.getIntentExtras());
                intent.putExtra("android.telecom.extra.PHONE_ACCOUNT_HANDLE", call.getTargetPhoneAccount());
            }
            if (call == null && InCallController.this.mCallsManager.getCalls().size() == 0) {
                intent.putExtra("android.telecom.extra.OUTGOING_CALL_EXTRAS", new Bundle());
            }
            Log.i(this, "Attempting to bind to InCall %s, with %s", new Object[]{this.mInCallServiceInfo, intent});
            this.mIsConnected = true;
            if (!InCallController.this.mContext.bindServiceAsUser(intent, this.mServiceConnection, 67108865, UserHandle.CURRENT)) {
                Log.w(this, "Failed to connect.", new Object[0]);
                this.mIsConnected = false;
            }
            if (call != null && this.mIsConnected) {
                call.getAnalytics().addInCallService(this.mInCallServiceInfo.getComponentName().flattenToShortString(), this.mInCallServiceInfo.getType());
            }
            return this.mIsConnected ? 1 : 2;
        }

        @Override
        public InCallServiceInfo getInfo() {
            return this.mInCallServiceInfo;
        }

        @Override
        public void disconnect() {
            if (this.mIsConnected) {
                InCallController.this.mContext.unbindService(this.mServiceConnection);
                this.mIsConnected = false;
            } else {
                Log.addEvent((EventManager.Loggable) null, "INFO", "Already disconnected, ignoring request.");
            }
        }

        @Override
        public boolean isConnected() {
            return this.mIsConnected;
        }

        @Override
        public void dump(IndentingPrintWriter indentingPrintWriter) {
            indentingPrintWriter.append("BindingConnection [");
            indentingPrintWriter.append(this.mIsConnected ? "" : "not ").append((CharSequence) "connected, ");
            indentingPrintWriter.append(this.mIsBound ? "" : "not ").append((CharSequence) "bound]\n");
        }

        protected void onConnected(IBinder iBinder) {
            if (!InCallController.this.onConnected(this.mInCallServiceInfo, iBinder)) {
                disconnect();
                InCallController.this.mInCallServiceConnection = null;
            }
        }

        protected void onDisconnected() {
            InCallController.this.onDisconnected(this.mInCallServiceInfo);
            disconnect();
            if (this.mListener != null) {
                this.mListener.onDisconnect(this);
            }
        }
    }

    private class EmergencyInCallServiceConnection extends InCallServiceBindingConnection {
        private boolean mIsConnected;
        private boolean mIsProxying;
        private final InCallServiceConnection mSubConnection;
        private InCallServiceConnection.Listener mSubListener;

        public EmergencyInCallServiceConnection(InCallServiceInfo inCallServiceInfo, InCallServiceConnection inCallServiceConnection) {
            super(inCallServiceInfo);
            this.mIsProxying = true;
            this.mIsConnected = false;
            this.mSubListener = new InCallServiceConnection.Listener() {
                @Override
                public void onDisconnect(InCallServiceConnection inCallServiceConnection2) {
                    if (inCallServiceConnection2 == EmergencyInCallServiceConnection.this.mSubConnection && EmergencyInCallServiceConnection.this.mIsConnected && EmergencyInCallServiceConnection.this.mIsProxying) {
                        EmergencyInCallServiceConnection.this.mIsProxying = false;
                        EmergencyInCallServiceConnection.this.connect(null);
                    }
                }
            };
            this.mSubConnection = inCallServiceConnection;
            if (this.mSubConnection != null) {
                this.mSubConnection.setListener(this.mSubListener);
            }
            this.mIsProxying = this.mSubConnection != null;
        }

        @Override
        public int connect(Call call) {
            boolean z;
            this.mIsConnected = true;
            if (this.mIsProxying) {
                int iConnect = this.mSubConnection.connect(call);
                if (iConnect == 1) {
                    z = true;
                } else {
                    z = false;
                }
                this.mIsConnected = z;
                if (iConnect != 2) {
                    return iConnect;
                }
                this.mIsProxying = false;
            }
            InCallController.this.mEmergencyCallHelper.maybeGrantTemporaryLocationPermission(call, InCallController.this.mCallsManager.getCurrentUserHandle());
            if (call != null && call.isIncoming() && InCallController.this.mEmergencyCallHelper.getLastEmergencyCallTimeMillis() > 0) {
                Bundle bundle = new Bundle();
                bundle.putLong("android.telecom.extra.LAST_EMERGENCY_CALLBACK_TIME_MILLIS", InCallController.this.mEmergencyCallHelper.getLastEmergencyCallTimeMillis());
                call.putExtras(1, bundle);
            }
            return super.connect(call);
        }

        @Override
        public void disconnect() {
            Log.i(this, "Disconnect forced!", new Object[0]);
            if (this.mIsProxying) {
                this.mSubConnection.disconnect();
            } else {
                super.disconnect();
                InCallController.this.mEmergencyCallHelper.maybeRevokeTemporaryLocationPermission();
            }
            this.mIsConnected = false;
        }

        @Override
        public void setHasEmergency(boolean z) {
            if (z) {
                takeControl();
            }
        }

        @Override
        public InCallServiceInfo getInfo() {
            if (this.mIsProxying) {
                return this.mSubConnection.getInfo();
            }
            return super.getInfo();
        }

        @Override
        protected void onDisconnected() {
            boolean z = this.mIsConnected;
            super.onDisconnected();
            if (z && !this.mIsProxying) {
                connect(null);
            }
        }

        @Override
        public void dump(IndentingPrintWriter indentingPrintWriter) {
            indentingPrintWriter.print("Emergency ICS Connection [");
            indentingPrintWriter.append(this.mIsProxying ? "" : "not ").append((CharSequence) "proxying, ");
            indentingPrintWriter.append(this.mIsConnected ? "" : "not ").append((CharSequence) "connected]\n");
            indentingPrintWriter.increaseIndent();
            indentingPrintWriter.print("Emergency: ");
            super.dump(indentingPrintWriter);
            if (this.mSubConnection != null) {
                indentingPrintWriter.print("Default-Dialer: ");
                this.mSubConnection.dump(indentingPrintWriter);
            }
            indentingPrintWriter.decreaseIndent();
        }

        private void takeControl() {
            if (this.mIsProxying) {
                this.mIsProxying = false;
                if (this.mIsConnected) {
                    this.mSubConnection.disconnect();
                    super.connect(null);
                }
            }
        }
    }

    private class CarSwappingInCallServiceConnection extends InCallServiceConnection {
        private final InCallServiceConnection mCarModeConnection;
        private InCallServiceConnection mCurrentConnection;
        private final InCallServiceConnection mDialerConnection;
        private boolean mIsCarMode;
        private boolean mIsConnected;

        public CarSwappingInCallServiceConnection(InCallServiceConnection inCallServiceConnection, InCallServiceConnection inCallServiceConnection2) {
            super();
            this.mIsCarMode = false;
            this.mIsConnected = false;
            this.mDialerConnection = inCallServiceConnection;
            this.mCarModeConnection = inCallServiceConnection2;
            this.mCurrentConnection = getCurrentConnection();
        }

        public synchronized void setCarMode(boolean z) {
            Log.i(this, "carmodechange: " + this.mIsCarMode + " => " + z, new Object[0]);
            if (z != this.mIsCarMode) {
                this.mIsCarMode = z;
                InCallServiceConnection currentConnection = getCurrentConnection();
                if (currentConnection != this.mCurrentConnection) {
                    if (this.mIsConnected) {
                        this.mCurrentConnection.disconnect();
                        this.mIsConnected = currentConnection.connect(null) == 1;
                    }
                    this.mCurrentConnection = currentConnection;
                }
            }
        }

        @Override
        public int connect(Call call) {
            if (this.mIsConnected) {
                Log.i(this, "already connected", new Object[0]);
                return 1;
            }
            int iConnect = this.mCurrentConnection.connect(call);
            if (iConnect == 2) {
                return 2;
            }
            this.mIsConnected = iConnect == 1;
            return iConnect;
        }

        @Override
        public void disconnect() {
            if (this.mIsConnected) {
                this.mCurrentConnection.disconnect();
                this.mIsConnected = false;
            } else {
                Log.i(this, "already disconnected", new Object[0]);
            }
        }

        @Override
        public boolean isConnected() {
            return this.mIsConnected;
        }

        @Override
        public void setHasEmergency(boolean z) {
            if (this.mDialerConnection != null) {
                this.mDialerConnection.setHasEmergency(z);
            }
            if (this.mCarModeConnection != null) {
                this.mCarModeConnection.setHasEmergency(z);
            }
        }

        @Override
        public InCallServiceInfo getInfo() {
            return this.mCurrentConnection.getInfo();
        }

        @Override
        public void dump(IndentingPrintWriter indentingPrintWriter) {
            indentingPrintWriter.print("Car Swapping ICS [");
            indentingPrintWriter.append(this.mIsConnected ? "" : "not ").append((CharSequence) "connected]\n");
            indentingPrintWriter.increaseIndent();
            if (this.mDialerConnection != null) {
                indentingPrintWriter.print("Dialer: ");
                this.mDialerConnection.dump(indentingPrintWriter);
            }
            if (this.mCarModeConnection != null) {
                indentingPrintWriter.print("Car Mode: ");
                this.mCarModeConnection.dump(indentingPrintWriter);
            }
        }

        private InCallServiceConnection getCurrentConnection() {
            if (this.mIsCarMode && this.mCarModeConnection != null) {
                return this.mCarModeConnection;
            }
            return this.mDialerConnection;
        }
    }

    private class NonUIInCallServiceConnectionCollection extends InCallServiceConnection {
        private final List<InCallServiceBindingConnection> mSubConnections;

        public NonUIInCallServiceConnectionCollection(List<InCallServiceBindingConnection> list) {
            super();
            this.mSubConnections = list;
        }

        @Override
        public int connect(Call call) {
            Iterator<InCallServiceBindingConnection> it = this.mSubConnections.iterator();
            while (it.hasNext()) {
                it.next().connect(call);
            }
            return 1;
        }

        @Override
        public void disconnect() {
            for (InCallServiceBindingConnection inCallServiceBindingConnection : this.mSubConnections) {
                if (inCallServiceBindingConnection.isConnected()) {
                    inCallServiceBindingConnection.disconnect();
                }
            }
        }

        @Override
        public boolean isConnected() {
            boolean z = false;
            for (InCallServiceBindingConnection inCallServiceBindingConnection : this.mSubConnections) {
                if (z || inCallServiceBindingConnection.isConnected()) {
                    z = true;
                } else {
                    z = false;
                }
            }
            return z;
        }

        @Override
        public void dump(IndentingPrintWriter indentingPrintWriter) {
            indentingPrintWriter.println("Non-UI Connections:");
            indentingPrintWriter.increaseIndent();
            Iterator<InCallServiceBindingConnection> it = this.mSubConnections.iterator();
            while (it.hasNext()) {
                it.next().dump(indentingPrintWriter);
            }
            indentingPrintWriter.decreaseIndent();
        }
    }

    public InCallController(Context context, TelecomSystem.SyncRoot syncRoot, CallsManager callsManager, SystemStateProvider systemStateProvider, DefaultDialerCache defaultDialerCache, Timeouts.Adapter adapter, EmergencyCallHelper emergencyCallHelper) {
        this.mContext = context;
        this.mLock = syncRoot;
        this.mCallsManager = callsManager;
        this.mSystemStateProvider = systemStateProvider;
        this.mTimeoutsAdapter = adapter;
        this.mDefaultDialerCache = defaultDialerCache;
        this.mEmergencyCallHelper = emergencyCallHelper;
        Resources resources = this.mContext.getResources();
        this.mSystemInCallComponentName = new ComponentName(resources.getString(R.string.ui_default_package), resources.getString(R.string.incall_default_class));
        this.mSystemStateProvider.addListener(this.mSystemStateListener);
    }

    @Override
    public void onCallAdded(Call call) {
        if (!isBoundAndConnectedToServices()) {
            Log.i(this, "onCallAdded: %s; not bound or connected.", new Object[]{call});
            bindToServices(call);
            return;
        }
        adjustServiceBindingsForEmergency();
        this.mEmergencyCallHelper.maybeGrantTemporaryLocationPermission(call, this.mCallsManager.getCurrentUserHandle());
        Log.i(this, "onCallAdded: %s", new Object[]{call});
        addCall(call);
        Log.i(this, "mInCallServiceConnection isConnected=%b", new Object[]{Boolean.valueOf(this.mInCallServiceConnection.isConnected())});
        ArrayList arrayList = new ArrayList();
        for (Map.Entry<InCallServiceInfo, IInCallService> entry : this.mInCallServices.entrySet()) {
            InCallServiceInfo key = entry.getKey();
            if (!call.isExternalCall() || key.isExternalCallsSupported()) {
                if (!call.isSelfManaged() || key.isSelfManagedCallsSupported()) {
                    boolean zEquals = key.equals(this.mInCallServiceConnection.getInfo());
                    arrayList.add(key.getComponentName());
                    IInCallService value = entry.getValue();
                    try {
                        value.addCall(ParcelableCallUtils.toParcelableCall(call, true, this.mCallsManager.getPhoneAccountRegistrar(), key.isExternalCallsSupported(), zEquals));
                    } catch (RemoteException e) {
                    }
                    try {
                        if (this.mCallsManager.getCalls().size() == 1) {
                            value.onCallAudioStateChanged(this.mCallsManager.getAudioState());
                            value.onCanAddCallChanged(this.mCallsManager.canAddCall());
                        }
                    } catch (RemoteException e2) {
                    }
                }
            }
        }
        Log.i(this, "Call added to components: %s", new Object[]{arrayList});
    }

    @Override
    public void onCallRemoved(Call call) {
        Log.i(this, "onCallRemoved: %s", new Object[]{call});
        if (this.mCallsManager.getCalls().isEmpty()) {
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable("ICC.oCR", this.mLock) {
                public void loggedRun() {
                    if (InCallController.this.mCallsManager.getCalls().isEmpty() && !InCallController.this.mCallsManager.hasPendingEcc()) {
                        InCallController.this.unbindFromServices();
                        InCallController.this.mEmergencyCallHelper.maybeRevokeTemporaryLocationPermission();
                    }
                }
            }.prepare(), this.mTimeoutsAdapter.getCallRemoveUnbindInCallServicesDelay(this.mContext.getContentResolver()));
        }
        call.removeListener(this.mCallListener);
        this.mCallIdMapper.removeCall(call);
    }

    @Override
    public void onExternalCallChanged(Call call, boolean z) {
        Log.i(this, "onExternalCallChanged: %s -> %b", new Object[]{call, Boolean.valueOf(z)});
        ArrayList arrayList = new ArrayList();
        if (!z) {
            for (Map.Entry<InCallServiceInfo, IInCallService> entry : this.mInCallServices.entrySet()) {
                InCallServiceInfo key = entry.getKey();
                if (!key.isExternalCallsSupported() && (!call.isSelfManaged() || key.isSelfManagedCallsSupported())) {
                    arrayList.add(key.getComponentName());
                    try {
                        entry.getValue().addCall(ParcelableCallUtils.toParcelableCall(call, true, this.mCallsManager.getPhoneAccountRegistrar(), key.isExternalCallsSupported(), key.equals(this.mInCallServiceConnection.getInfo())));
                    } catch (RemoteException e) {
                    }
                }
            }
            Log.i(this, "Previously external call added to components: %s", new Object[]{arrayList});
            return;
        }
        ParcelableCall parcelableCall = ParcelableCallUtils.toParcelableCall(call, false, this.mCallsManager.getPhoneAccountRegistrar(), false, 7, false);
        Log.i(this, "Removing external call %s ==> %s", new Object[]{call, parcelableCall});
        for (Map.Entry<InCallServiceInfo, IInCallService> entry2 : this.mInCallServices.entrySet()) {
            InCallServiceInfo key2 = entry2.getKey();
            if (!key2.isExternalCallsSupported()) {
                arrayList.add(key2.getComponentName());
                try {
                    entry2.getValue().updateCall(parcelableCall);
                } catch (RemoteException e2) {
                }
            }
        }
        Log.i(this, "External call removed from components: %s", new Object[]{arrayList});
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
    public void onCallAudioStateChanged(CallAudioState callAudioState, CallAudioState callAudioState2) {
        if (!this.mInCallServices.isEmpty()) {
            Log.i(this, "Calling onAudioStateChanged, audioState: %s -> %s", new Object[]{callAudioState, callAudioState2});
            Iterator<IInCallService> it = this.mInCallServices.values().iterator();
            while (it.hasNext()) {
                try {
                    it.next().onCallAudioStateChanged(callAudioState2);
                } catch (RemoteException e) {
                }
            }
        }
    }

    @Override
    public void onCanAddCallChanged(boolean z) {
        if (!this.mInCallServices.isEmpty()) {
            Log.i(this, "onCanAddCallChanged : %b", new Object[]{Boolean.valueOf(z)});
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
            Log.i(this, "Calling onPostDialWait, remaining = %s", new Object[]{str});
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
        Log.d(this, "onIsConferencedChanged %s", new Object[]{call});
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

    void silenceRinger() {
        if (!this.mInCallServices.isEmpty()) {
            Iterator<IInCallService> it = this.mInCallServices.values().iterator();
            while (it.hasNext()) {
                try {
                    it.next().silenceRinger();
                } catch (RemoteException e) {
                }
            }
        }
    }

    private void notifyConnectionEvent(Call call, String str, Bundle bundle) {
        if (!this.mInCallServices.isEmpty()) {
            for (IInCallService iInCallService : this.mInCallServices.values()) {
                try {
                    Object[] objArr = new Object[3];
                    objArr[0] = call != null ? call.toString() : "null";
                    objArr[1] = str != null ? str : "null";
                    objArr[2] = bundle != null ? bundle.toString() : "null";
                    Log.i(this, "notifyConnectionEvent {Call: %s, Event: %s, Extras:[%s]}", objArr);
                    iInCallService.onConnectionEvent(this.mCallIdMapper.getCallId(call), str, bundle);
                } catch (RemoteException e) {
                }
            }
        }
    }

    private void notifyRttInitiationFailure(final Call call, final int i) {
        if (!this.mInCallServices.isEmpty()) {
            this.mInCallServices.entrySet().stream().filter(new Predicate() {
                @Override
                public final boolean test(Object obj) {
                    return ((InCallController.InCallServiceInfo) ((Map.Entry) obj).getKey()).equals(this.f$0.mInCallServiceConnection.getInfo());
                }
            }).forEach(new Consumer() {
                @Override
                public final void accept(Object obj) {
                    InCallController.lambda$notifyRttInitiationFailure$1(this.f$0, call, i, (Map.Entry) obj);
                }
            });
        }
    }

    public static void lambda$notifyRttInitiationFailure$1(InCallController inCallController, Call call, int i, Map.Entry entry) {
        try {
            Log.i(inCallController, "notifyRttFailure, call %s, incall %s", new Object[]{call, entry.getKey()});
            ((IInCallService) entry.getValue()).onRttInitiationFailure(inCallController.mCallIdMapper.getCallId(call), i);
        } catch (RemoteException e) {
        }
    }

    private void notifyRemoteRttRequest(final Call call, final int i) {
        if (!this.mInCallServices.isEmpty()) {
            this.mInCallServices.entrySet().stream().filter(new Predicate() {
                @Override
                public final boolean test(Object obj) {
                    return ((InCallController.InCallServiceInfo) ((Map.Entry) obj).getKey()).equals(this.f$0.mInCallServiceConnection.getInfo());
                }
            }).forEach(new Consumer() {
                @Override
                public final void accept(Object obj) {
                    InCallController.lambda$notifyRemoteRttRequest$3(this.f$0, call, i, (Map.Entry) obj);
                }
            });
        }
    }

    public static void lambda$notifyRemoteRttRequest$3(InCallController inCallController, Call call, int i, Map.Entry entry) {
        try {
            Log.i(inCallController, "notifyRemoteRttRequest, call %s, incall %s", new Object[]{call, entry.getKey()});
            ((IInCallService) entry.getValue()).onRttUpgradeRequest(inCallController.mCallIdMapper.getCallId(call), i);
        } catch (RemoteException e) {
        }
    }

    private void notifyHandoverFailed(Call call, int i) {
        if (!this.mInCallServices.isEmpty()) {
            Iterator<IInCallService> it = this.mInCallServices.values().iterator();
            while (it.hasNext()) {
                try {
                    it.next().onHandoverFailed(this.mCallIdMapper.getCallId(call), i);
                } catch (RemoteException e) {
                }
            }
        }
    }

    private void notifyHandoverComplete(Call call) {
        if (!this.mInCallServices.isEmpty()) {
            Iterator<IInCallService> it = this.mInCallServices.values().iterator();
            while (it.hasNext()) {
                try {
                    it.next().onHandoverComplete(this.mCallIdMapper.getCallId(call));
                } catch (RemoteException e) {
                }
            }
        }
    }

    private void unbindFromServices() {
        if (this.mInCallServiceConnection != null) {
            this.mInCallServiceConnection.disconnect();
            this.mInCallServiceConnection = null;
        }
        if (this.mNonUIInCallServiceConnections != null) {
            this.mNonUIInCallServiceConnections.disconnect();
            this.mNonUIInCallServiceConnections = null;
        }
        this.mInCallServices.clear();
    }

    @VisibleForTesting
    public void bindToServices(Call call) {
        InCallServiceBindingConnection inCallServiceBindingConnection;
        if (this.mInCallServiceConnection == null) {
            InCallServiceInfo defaultDialerComponent = getDefaultDialerComponent();
            Log.i(this, "defaultDialer: " + defaultDialerComponent, new Object[0]);
            InCallServiceBindingConnection inCallServiceBindingConnection2 = null;
            if (defaultDialerComponent != null && !defaultDialerComponent.getComponentName().equals(this.mSystemInCallComponentName)) {
                inCallServiceBindingConnection = new InCallServiceBindingConnection(defaultDialerComponent);
            } else {
                inCallServiceBindingConnection = null;
            }
            Log.i(this, "defaultDialer: " + inCallServiceBindingConnection, new Object[0]);
            EmergencyInCallServiceConnection emergencyInCallServiceConnection = new EmergencyInCallServiceConnection(getInCallServiceComponent(this.mSystemInCallComponentName, 2), inCallServiceBindingConnection);
            boolean zHasEmergencyCall = this.mCallsManager.hasEmergencyCall();
            if (zHasEmergencyCall && defaultDialerComponent != null && defaultDialerComponent.getComponentName().getPackageName().equals("com.android.server.telecom.testapps")) {
                emergencyInCallServiceConnection.setHasEmergency(false);
            } else {
                emergencyInCallServiceConnection.setHasEmergency(zHasEmergencyCall);
            }
            InCallServiceInfo carModeComponent = getCarModeComponent();
            if (carModeComponent != null && !carModeComponent.getComponentName().equals(this.mSystemInCallComponentName)) {
                inCallServiceBindingConnection2 = new InCallServiceBindingConnection(carModeComponent);
            }
            this.mInCallServiceConnection = new CarSwappingInCallServiceConnection(emergencyInCallServiceConnection, inCallServiceBindingConnection2);
        }
        this.mInCallServiceConnection.setCarMode(shouldUseCarModeUI());
        if (this.mInCallServiceConnection.connect(call) == 1) {
            connectToNonUiInCallServices(call);
        } else {
            Log.i(this, "bindToServices: current UI doesn't support call; not binding.", new Object[0]);
        }
    }

    private void connectToNonUiInCallServices(Call call) {
        List<InCallServiceInfo> inCallServiceComponents = getInCallServiceComponents(4);
        LinkedList linkedList = new LinkedList();
        Iterator<InCallServiceInfo> it = inCallServiceComponents.iterator();
        while (it.hasNext()) {
            linkedList.add(new InCallServiceBindingConnection(it.next()));
        }
        this.mNonUIInCallServiceConnections = new NonUIInCallServiceConnectionCollection(linkedList);
        this.mNonUIInCallServiceConnections.connect(call);
    }

    private InCallServiceInfo getDefaultDialerComponent() {
        String defaultDialerApplication = this.mDefaultDialerCache.getDefaultDialerApplication(this.mCallsManager.getCurrentUserHandle().getIdentifier());
        Log.d(this, "Default Dialer package: " + defaultDialerApplication, new Object[0]);
        return getInCallServiceComponent(defaultDialerApplication, 1);
    }

    private InCallServiceInfo getCarModeComponent() {
        return getInCallServiceComponent((String) null, 3);
    }

    private InCallServiceInfo getInCallServiceComponent(ComponentName componentName, int i) {
        List<InCallServiceInfo> inCallServiceComponents = getInCallServiceComponents(componentName, i);
        if (inCallServiceComponents != null && !inCallServiceComponents.isEmpty()) {
            return inCallServiceComponents.get(0);
        }
        Log.e(this, new Exception(), "Package Manager could not find ComponentName: " + componentName + ". Trying to bind anyway.", new Object[0]);
        return new InCallServiceInfo(componentName, false, false, i);
    }

    private InCallServiceInfo getInCallServiceComponent(String str, int i) {
        List<InCallServiceInfo> inCallServiceComponents = getInCallServiceComponents(str, i);
        if (inCallServiceComponents != null && !inCallServiceComponents.isEmpty()) {
            return inCallServiceComponents.get(0);
        }
        return null;
    }

    private List<InCallServiceInfo> getInCallServiceComponents(int i) {
        return getInCallServiceComponents(null, null, i);
    }

    private List<InCallServiceInfo> getInCallServiceComponents(String str, int i) {
        return getInCallServiceComponents(str, null, i);
    }

    private List<InCallServiceInfo> getInCallServiceComponents(ComponentName componentName, int i) {
        return getInCallServiceComponents(null, componentName, i);
    }

    private List<InCallServiceInfo> getInCallServiceComponents(String str, ComponentName componentName, int i) {
        LinkedList linkedList = new LinkedList();
        Intent intent = new Intent("android.telecom.InCallService");
        if (str != null) {
            intent.setPackage(str);
        }
        if (componentName != null) {
            intent.setComponent(componentName);
        }
        PackageManager packageManager = this.mContext.getPackageManager();
        for (ResolveInfo resolveInfo : packageManager.queryIntentServicesAsUser(intent, 128, this.mCallsManager.getCurrentUserHandle().getIdentifier())) {
            ServiceInfo serviceInfo = resolveInfo.serviceInfo;
            if (serviceInfo != null) {
                boolean z = serviceInfo.metaData != null && serviceInfo.metaData.getBoolean("android.telecom.INCLUDE_EXTERNAL_CALLS", false);
                boolean z2 = serviceInfo.metaData != null && serviceInfo.metaData.getBoolean("android.telecom.INCLUDE_SELF_MANAGED_CALLS", false);
                int inCallServiceType = getInCallServiceType(resolveInfo.serviceInfo, packageManager);
                if (i == 0 || i == inCallServiceType) {
                    linkedList.add(new InCallServiceInfo(new ComponentName(serviceInfo.packageName, serviceInfo.name), z, i == 4 ? false : z2, i));
                }
            }
        }
        return linkedList;
    }

    private boolean shouldUseCarModeUI() {
        return this.mSystemStateProvider.isCarMode();
    }

    private int getInCallServiceType(ServiceInfo serviceInfo, PackageManager packageManager) {
        if (!(serviceInfo.permission != null && serviceInfo.permission.equals("android.permission.BIND_INCALL_SERVICE"))) {
            Log.w(this, "InCallService does not require BIND_INCALL_SERVICE permission: " + serviceInfo.packageName, new Object[0]);
            return 0;
        }
        if (this.mSystemInCallComponentName.getPackageName().equals(serviceInfo.packageName) && this.mSystemInCallComponentName.getClassName().equals(serviceInfo.name)) {
            return 2;
        }
        boolean z = packageManager.checkPermission("android.permission.CONTROL_INCALL_EXPERIENCE", serviceInfo.packageName) == 0;
        boolean z2 = serviceInfo.metaData != null && serviceInfo.metaData.getBoolean("android.telecom.IN_CALL_SERVICE_CAR_MODE_UI", false) && z;
        if (z2) {
            return 3;
        }
        boolean zEquals = Objects.equals(serviceInfo.packageName, this.mDefaultDialerCache.getDefaultDialerApplication(this.mCallsManager.getCurrentUserHandle().getIdentifier()));
        boolean z3 = serviceInfo.metaData != null && serviceInfo.metaData.getBoolean("android.telecom.IN_CALL_SERVICE_UI", false);
        if (zEquals && z3) {
            return 1;
        }
        if (z && !z3) {
            return 4;
        }
        Log.i(this, "Skipping binding to %s:%s, control: %b, car-mode: %b, ui: %b", new Object[]{serviceInfo.packageName, serviceInfo.name, Boolean.valueOf(z), Boolean.valueOf(z2), Boolean.valueOf(z3)});
        return 0;
    }

    private void adjustServiceBindingsForEmergency() {
        if (this.mCallsManager.hasEmergencyCall()) {
            InCallServiceInfo defaultDialerComponent = getDefaultDialerComponent();
            if (defaultDialerComponent != null && defaultDialerComponent.getComponentName().getPackageName().equals("com.android.server.telecom.testapps")) {
                Log.i(this, "adjustServiceBindingsForEmergency, do nothing for testapps", new Object[0]);
            } else {
                this.mInCallServiceConnection.setHasEmergency(true);
            }
        }
    }

    private boolean onConnected(InCallServiceInfo inCallServiceInfo, IBinder iBinder) {
        Trace.beginSection("onConnected: " + inCallServiceInfo.getComponentName());
        Log.i(this, "onConnected to %s", new Object[]{inCallServiceInfo.getComponentName()});
        IInCallService iInCallServiceAsInterface = IInCallService.Stub.asInterface(iBinder);
        this.mInCallServices.put(inCallServiceInfo, iInCallServiceAsInterface);
        try {
            iInCallServiceAsInterface.setInCallAdapter(new InCallAdapter(this.mCallsManager, this.mCallIdMapper, this.mLock, inCallServiceInfo.getComponentName().getPackageName()));
            List<Call> listOrderCallsWithChildrenFirst = orderCallsWithChildrenFirst(this.mCallsManager.getCalls());
            Log.i(this, "Adding %s calls to InCallService after onConnected: %s, including external calls", new Object[]{Integer.valueOf(listOrderCallsWithChildrenFirst.size()), inCallServiceInfo.getComponentName()});
            int i = 0;
            for (Call call : listOrderCallsWithChildrenFirst) {
                if ((!call.isSelfManaged() || inCallServiceInfo.isSelfManagedCallsSupported()) && (!call.isExternalCall() || inCallServiceInfo.isExternalCallsSupported())) {
                    boolean zEquals = inCallServiceInfo.equals(this.mInCallServiceConnection.getInfo());
                    addCall(call);
                    i++;
                    iInCallServiceAsInterface.addCall(ParcelableCallUtils.toParcelableCall(call, true, this.mCallsManager.getPhoneAccountRegistrar(), inCallServiceInfo.isExternalCallsSupported(), zEquals));
                }
            }
            try {
                if (listOrderCallsWithChildrenFirst.size() > 0) {
                    iInCallServiceAsInterface.onCallAudioStateChanged(this.mCallsManager.getAudioState());
                    iInCallServiceAsInterface.onCanAddCallChanged(this.mCallsManager.canAddCall());
                }
            } catch (RemoteException e) {
            }
            Log.i(this, "%s calls sent to InCallService.", new Object[]{Integer.valueOf(i)});
            Trace.endSection();
            return true;
        } catch (RemoteException e2) {
            Log.e(this, e2, "Failed to set the in-call adapter.", new Object[0]);
            Trace.endSection();
            return false;
        }
    }

    private void onDisconnected(InCallServiceInfo inCallServiceInfo) {
        Log.i(this, "onDisconnected from %s", new Object[]{inCallServiceInfo.getComponentName()});
        this.mInCallServices.remove(inCallServiceInfo);
    }

    private void updateCall(Call call) {
        updateCall(call, false, false);
    }

    private void updateCall(Call call, boolean z, boolean z2) {
        if (!this.mInCallServices.isEmpty()) {
            Log.i(this, "Sending updateCall %s", new Object[]{call});
            ArrayList arrayList = new ArrayList();
            for (Map.Entry<InCallServiceInfo, IInCallService> entry : this.mInCallServices.entrySet()) {
                InCallServiceInfo key = entry.getKey();
                if (!call.isExternalCall() || key.isExternalCallsSupported()) {
                    if (!call.isSelfManaged() || key.isSelfManagedCallsSupported()) {
                        ParcelableCall parcelableCall = ParcelableCallUtils.toParcelableCall(call, z, this.mCallsManager.getPhoneAccountRegistrar(), key.isExternalCallsSupported(), z2 && key.equals(this.mInCallServiceConnection.getInfo()));
                        ComponentName componentName = key.getComponentName();
                        IInCallService value = entry.getValue();
                        arrayList.add(componentName);
                        try {
                            value.updateCall(parcelableCall);
                        } catch (RemoteException e) {
                        }
                    }
                }
            }
            Log.v(this, "Components updated: %s", new Object[]{arrayList});
        }
    }

    private void addCall(Call call) {
        if (this.mCallIdMapper.getCallId(call) == null) {
            this.mCallIdMapper.addCall(call);
            call.addListener(this.mCallListener);
        }
    }

    public boolean isBoundAndConnectedToServices() {
        return this.mInCallServiceConnection != null && this.mInCallServiceConnection.isConnected();
    }

    public void dump(IndentingPrintWriter indentingPrintWriter) {
        indentingPrintWriter.println("mInCallServices (InCalls registered):");
        indentingPrintWriter.increaseIndent();
        Iterator<InCallServiceInfo> it = this.mInCallServices.keySet().iterator();
        while (it.hasNext()) {
            indentingPrintWriter.println(it.next());
        }
        indentingPrintWriter.decreaseIndent();
        indentingPrintWriter.println("ServiceConnections (InCalls bound):");
        indentingPrintWriter.increaseIndent();
        if (this.mInCallServiceConnection != null) {
            this.mInCallServiceConnection.dump(indentingPrintWriter);
        }
        indentingPrintWriter.decreaseIndent();
    }

    public boolean doesConnectedDialerSupportRinging() {
        String defaultDialerApplication;
        if (this.mInCallUIComponentName != null) {
            defaultDialerApplication = this.mInCallUIComponentName.getPackageName().trim();
        } else {
            defaultDialerApplication = null;
        }
        if (TextUtils.isEmpty(defaultDialerApplication)) {
            defaultDialerApplication = DefaultDialerManager.getDefaultDialerApplication(this.mContext, -2);
        }
        if (TextUtils.isEmpty(defaultDialerApplication)) {
            return false;
        }
        List listQueryIntentServicesAsUser = this.mContext.getPackageManager().queryIntentServicesAsUser(new Intent("android.telecom.InCallService").setPackage(defaultDialerApplication), 128, this.mCallsManager.getCurrentUserHandle().getIdentifier());
        if (listQueryIntentServicesAsUser.isEmpty()) {
            return false;
        }
        ResolveInfo resolveInfo = (ResolveInfo) listQueryIntentServicesAsUser.get(0);
        if (resolveInfo.serviceInfo == null || resolveInfo.serviceInfo.metaData == null) {
            return false;
        }
        return resolveInfo.serviceInfo.metaData.getBoolean("android.telecom.IN_CALL_SERVICE_RINGING", false);
    }

    private List<Call> orderCallsWithChildrenFirst(Collection<Call> collection) {
        LinkedList linkedList = new LinkedList();
        LinkedList linkedList2 = new LinkedList();
        for (Call call : collection) {
            if (call.getChildCalls().size() > 0) {
                linkedList.add(call);
            } else {
                linkedList2.add(call);
            }
        }
        linkedList2.addAll(linkedList);
        return linkedList2;
    }

    public void unbindUselessService() {
        Log.i(this, "unbindUselessService", new Object[0]);
        if (this.mCallsManager.getCalls().isEmpty() && !this.mCallsManager.hasPendingEcc()) {
            unbindFromServices();
            this.mEmergencyCallHelper.maybeRevokeTemporaryLocationPermission();
        }
    }
}
