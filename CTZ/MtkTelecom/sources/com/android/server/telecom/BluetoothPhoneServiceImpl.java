package com.android.server.telecom;

import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.IBluetoothHeadsetPhone;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.telecom.Log;
import android.telecom.PhoneAccount;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.telecom.CallsManager;
import com.android.server.telecom.TelecomSystem;
import com.mediatek.server.telecom.MtkUtil;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class BluetoothPhoneServiceImpl {
    private BluetoothAdapterProxy mBluetoothAdapter;
    private BluetoothHeadsetProxy mBluetoothHeadset;
    private final CallsManager mCallsManager;
    private final Context mContext;
    private final TelecomSystem.SyncRoot mLock;
    private final PhoneAccountRegistrar mPhoneAccountRegistrar;
    private int mNumActiveCalls = 0;
    private int mNumHeldCalls = 0;
    private int mNumChildrenOfActiveCall = 0;
    private int mBluetoothCallState = 6;
    private String mRingingAddress = null;
    private int mRingingAddressType = 0;
    private Call mOldHeldCall = null;
    private boolean mIsDisconnectedTonePlaying = false;

    @VisibleForTesting
    public final IBluetoothHeadsetPhone.Stub mBinder = new IBluetoothHeadsetPhone.Stub() {
        public boolean answerCall() throws RemoteException {
            synchronized (BluetoothPhoneServiceImpl.this.mLock) {
                BluetoothPhoneServiceImpl.this.enforceModifyPermission();
                Log.startSession("BPSI.aC");
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    Log.i("BluetoothPhoneService", "BT - answering call", new Object[0]);
                    Call ringingCall = BluetoothPhoneServiceImpl.this.mCallsManager.getRingingCall();
                    if (ringingCall == null) {
                        return false;
                    }
                    BluetoothPhoneServiceImpl.this.mCallsManager.answerCall(ringingCall, 0);
                    return true;
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                    Log.endSession();
                }
            }
        }

        public boolean hangupCall() throws RemoteException {
            synchronized (BluetoothPhoneServiceImpl.this.mLock) {
                BluetoothPhoneServiceImpl.this.enforceModifyPermission();
                Log.startSession("BPSI.hC");
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    Log.i("BluetoothPhoneService", "BT - hanging up call", new Object[0]);
                    Call foregroundCall = BluetoothPhoneServiceImpl.this.mCallsManager.getForegroundCall();
                    if (foregroundCall == null) {
                        return false;
                    }
                    BluetoothPhoneServiceImpl.this.mCallsManager.disconnectCall(foregroundCall);
                    return true;
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                    Log.endSession();
                }
            }
        }

        public boolean sendDtmf(int i) throws RemoteException {
            synchronized (BluetoothPhoneServiceImpl.this.mLock) {
                BluetoothPhoneServiceImpl.this.enforceModifyPermission();
                Log.startSession("BPSI.sD");
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    Object[] objArr = new Object[1];
                    objArr[0] = Integer.valueOf(Log.DEBUG ? i : 46);
                    Log.i("BluetoothPhoneService", "BT - sendDtmf %c", objArr);
                    Call foregroundCall = BluetoothPhoneServiceImpl.this.mCallsManager.getForegroundCall();
                    if (foregroundCall == null) {
                        return false;
                    }
                    BluetoothPhoneServiceImpl.this.mCallsManager.playDtmfTone(foregroundCall, (char) i);
                    BluetoothPhoneServiceImpl.this.mCallsManager.stopDtmfTone(foregroundCall);
                    return true;
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                    Log.endSession();
                }
            }
        }

        public String getNetworkOperator() throws RemoteException {
            synchronized (BluetoothPhoneServiceImpl.this.mLock) {
                BluetoothPhoneServiceImpl.this.enforceModifyPermission();
                Log.startSession("BPSI.gNO");
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    Log.i("BluetoothPhoneService", "getNetworkOperator", new Object[0]);
                    PhoneAccount bestPhoneAccount = BluetoothPhoneServiceImpl.this.getBestPhoneAccount();
                    if (bestPhoneAccount != null && bestPhoneAccount.getLabel() != null) {
                        return bestPhoneAccount.getLabel().toString();
                    }
                    return TelephonyManager.from(BluetoothPhoneServiceImpl.this.mContext).getNetworkOperatorName();
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                    Log.endSession();
                }
            }
        }

        public String getSubscriberNumber() throws RemoteException {
            String line1Number;
            Uri address;
            synchronized (BluetoothPhoneServiceImpl.this.mLock) {
                BluetoothPhoneServiceImpl.this.enforceModifyPermission();
                Log.startSession("BPSI.gSN");
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    Log.i("BluetoothPhoneService", "getSubscriberNumber", new Object[0]);
                    line1Number = null;
                    PhoneAccount bestPhoneAccount = BluetoothPhoneServiceImpl.this.getBestPhoneAccount();
                    if (bestPhoneAccount != null && (address = bestPhoneAccount.getAddress()) != null) {
                        line1Number = address.getSchemeSpecificPart();
                    }
                    if (TextUtils.isEmpty(line1Number)) {
                        line1Number = TelephonyManager.from(BluetoothPhoneServiceImpl.this.mContext).getLine1Number();
                        if (line1Number == null) {
                            line1Number = "";
                        }
                    }
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                    Log.endSession();
                }
            }
            return line1Number;
        }

        public boolean listCurrentCalls() throws RemoteException {
            synchronized (BluetoothPhoneServiceImpl.this.mLock) {
                BluetoothPhoneServiceImpl.this.enforceModifyPermission();
                Log.startSession("BPSI.lCC");
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    boolean z = BluetoothPhoneServiceImpl.this.mHeadsetUpdatedRecently;
                    BluetoothPhoneServiceImpl.this.mHeadsetUpdatedRecently = false;
                    if (z) {
                        Log.i("BluetoothPhoneService", "listcurrentCalls", new Object[0]);
                    }
                    BluetoothPhoneServiceImpl.this.sendListOfCalls(z);
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                    Log.endSession();
                }
            }
            return true;
        }

        public boolean queryPhoneState() throws RemoteException {
            synchronized (BluetoothPhoneServiceImpl.this.mLock) {
                BluetoothPhoneServiceImpl.this.enforceModifyPermission();
                Log.startSession("BPSI.qPS");
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    Log.i("BluetoothPhoneService", "queryPhoneState", new Object[0]);
                    BluetoothPhoneServiceImpl.this.updateHeadsetWithCallState(true);
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                    Log.endSession();
                }
            }
            return true;
        }

        public boolean processChld(int i) throws RemoteException {
            boolean zProcessChld;
            synchronized (BluetoothPhoneServiceImpl.this.mLock) {
                BluetoothPhoneServiceImpl.this.enforceModifyPermission();
                Log.startSession("BPSI.pC");
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    Log.i("BluetoothPhoneService", "processChld %d", new Object[]{Integer.valueOf(i)});
                    zProcessChld = BluetoothPhoneServiceImpl.this.processChld(i);
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                    Log.endSession();
                }
            }
            return zProcessChld;
        }

        public void updateBtHandsfreeAfterRadioTechnologyChange() throws RemoteException {
            Log.d("BluetoothPhoneService", "RAT change - deprecated", new Object[0]);
        }

        public void cdmaSetSecondCallState(boolean z) throws RemoteException {
            Log.d("BluetoothPhoneService", "cdma 1 - deprecated", new Object[0]);
        }

        public void cdmaSwapSecondCallState() throws RemoteException {
            Log.d("BluetoothPhoneService", "cdma 2 - deprecated", new Object[0]);
        }
    };

    @VisibleForTesting
    public CallsManager.CallsManagerListener mCallsManagerListener = new CallsManagerListenerBase() {
        @Override
        public void onCallAdded(Call call) {
            if (!call.isExternalCall()) {
                BluetoothPhoneServiceImpl.this.updateHeadsetWithCallState(false);
            }
        }

        @Override
        public void onCallRemoved(Call call) {
            if (!call.isExternalCall()) {
                BluetoothPhoneServiceImpl.this.mClccIndexMap.remove(call);
                BluetoothPhoneServiceImpl.this.updateHeadsetWithCallState(false);
            }
        }

        @Override
        public void onExternalCallChanged(Call call, boolean z) {
            if (z) {
                onCallRemoved(call);
            } else {
                onCallAdded(call);
            }
        }

        @Override
        public void onCallStateChanged(Call call, int i, int i2) {
            if (call.isExternalCall()) {
                return;
            }
            if (i == 5 && i2 == 6) {
                for (Call call2 : BluetoothPhoneServiceImpl.this.mCallsManager.getCalls()) {
                    if (!Objects.equals(call2, call) && call2.getState() == 1) {
                        return;
                    }
                }
            }
            if (BluetoothPhoneServiceImpl.this.mCallsManager.getActiveCall() == null || i != 1 || (i2 != 3 && i2 != 10)) {
                BluetoothPhoneServiceImpl.this.updateHeadsetWithCallState(false);
            }
        }

        @Override
        public void onIsConferencedChanged(Call call) {
            if (call.isExternalCall()) {
                return;
            }
            if (call.getParentCall() != null) {
                Log.d(this, "Ignoring onIsConferenceChanged from child call with new parent", new Object[0]);
            } else if (call.getChildCalls().size() != 1) {
                BluetoothPhoneServiceImpl.this.updateHeadsetWithCallState(false);
            } else {
                Log.d(this, "Ignoring onIsConferenceChanged from parent with only one child call", new Object[0]);
            }
        }

        @Override
        public void onDisconnectedTonePlaying(boolean z) {
            BluetoothPhoneServiceImpl.this.mIsDisconnectedTonePlaying = z;
            BluetoothPhoneServiceImpl.this.updateHeadsetWithCallState(false);
        }

        @Override
        public void onIncomingCallRejected(Call call, boolean z, String str) {
            if (MtkUtil.isInDsdaMode()) {
                BluetoothPhoneServiceImpl.this.mRingingAddress = null;
                int i = 0;
                BluetoothPhoneServiceImpl.this.mRingingAddressType = 0;
                Call ringingCall = BluetoothPhoneServiceImpl.this.mCallsManager.getRingingCall(call);
                if (ringingCall != null) {
                    String schemeSpecificPart = ringingCall.getHandle() != null ? ringingCall.getHandle().getSchemeSpecificPart() : null;
                    if (schemeSpecificPart != null) {
                        i = PhoneNumberUtils.toaFromString(schemeSpecificPart);
                    } else {
                        schemeSpecificPart = "";
                    }
                    BluetoothPhoneServiceImpl.this.mRingingAddress = schemeSpecificPart;
                    BluetoothPhoneServiceImpl.this.mRingingAddressType = i;
                }
            }
        }

        @Override
        public void onCallAlertingNotified(Call call) {
            BluetoothPhoneServiceImpl.this.updateHeadsetWithCallState(false);
        }
    };

    @VisibleForTesting
    public BluetoothProfile.ServiceListener mProfileListener = new BluetoothProfile.ServiceListener() {
        @Override
        public void onServiceConnected(int i, BluetoothProfile bluetoothProfile) {
            synchronized (BluetoothPhoneServiceImpl.this.mLock) {
                BluetoothPhoneServiceImpl.this.setBluetoothHeadset(new BluetoothHeadsetProxy((BluetoothHeadset) bluetoothProfile));
            }
        }

        @Override
        public void onServiceDisconnected(int i) {
            synchronized (BluetoothPhoneServiceImpl.this.mLock) {
                BluetoothPhoneServiceImpl.this.mBluetoothHeadset = null;
            }
        }
    };

    @VisibleForTesting
    public final BroadcastReceiver mBluetoothAdapterReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            synchronized (BluetoothPhoneServiceImpl.this.mLock) {
                int intExtra = intent.getIntExtra("android.bluetooth.adapter.extra.STATE", Integer.MIN_VALUE);
                Log.d("BluetoothPhoneService", "Bluetooth Adapter state: %d", new Object[]{Integer.valueOf(intExtra)});
                if (intExtra == 12) {
                    try {
                        BluetoothPhoneServiceImpl.this.mBinder.queryPhoneState();
                    } catch (RemoteException e) {
                    }
                }
            }
        }
    };
    private Map<Call, Integer> mClccIndexMap = new HashMap();
    private boolean mHeadsetUpdatedRecently = false;

    public interface BluetoothPhoneServiceImplFactory {
        BluetoothPhoneServiceImpl makeBluetoothPhoneServiceImpl(Context context, TelecomSystem.SyncRoot syncRoot, CallsManager callsManager, PhoneAccountRegistrar phoneAccountRegistrar);
    }

    public IBinder getBinder() {
        return this.mBinder;
    }

    public BluetoothPhoneServiceImpl(Context context, TelecomSystem.SyncRoot syncRoot, CallsManager callsManager, BluetoothAdapterProxy bluetoothAdapterProxy, PhoneAccountRegistrar phoneAccountRegistrar) {
        Log.d(this, "onCreate", new Object[0]);
        this.mContext = context;
        this.mLock = syncRoot;
        this.mCallsManager = callsManager;
        this.mPhoneAccountRegistrar = phoneAccountRegistrar;
        this.mBluetoothAdapter = bluetoothAdapterProxy;
        if (this.mBluetoothAdapter == null) {
            Log.d(this, "BluetoothPhoneService shutting down, no BT Adapter found.", new Object[0]);
            return;
        }
        this.mBluetoothAdapter.getProfileProxy(context, this.mProfileListener, 1);
        context.registerReceiver(this.mBluetoothAdapterReceiver, new IntentFilter("android.bluetooth.adapter.action.STATE_CHANGED"));
        this.mCallsManager.addListener(this.mCallsManagerListener);
        updateHeadsetWithCallState(false);
    }

    @VisibleForTesting
    public void setBluetoothHeadset(BluetoothHeadsetProxy bluetoothHeadsetProxy) {
        this.mBluetoothHeadset = bluetoothHeadsetProxy;
    }

    private boolean processChld(int i) {
        Call activeCall = this.mCallsManager.getActiveCall();
        Call ringingCall = this.mCallsManager.getRingingCall();
        Call heldCall = this.mCallsManager.getHeldCall();
        Log.i("BluetoothPhoneService", "Active: %s\nRinging: %s\nHeld: %s", new Object[]{activeCall, ringingCall, heldCall});
        if (i == 0) {
            if (ringingCall != null) {
                this.mCallsManager.rejectCall(ringingCall, false, null);
                return true;
            }
            if (heldCall != null) {
                this.mCallsManager.disconnectCall(heldCall);
                return true;
            }
        } else {
            if (i == 1) {
                if (activeCall == null && ringingCall == null && heldCall == null) {
                    return false;
                }
                if (activeCall != null) {
                    if (ringingCall != null) {
                        this.mCallsManager.hangupActiveAndAnswerWaiting();
                    } else {
                        this.mCallsManager.disconnectCall(activeCall);
                    }
                    return true;
                }
                Call outgoingCall = this.mCallsManager.getOutgoingCall();
                if (outgoingCall != null) {
                    Log.d("BluetoothPhoneService", "chld = 1, outgoingCall: %s", new Object[]{outgoingCall});
                    if (ringingCall != null) {
                        this.mCallsManager.answerCall(ringingCall, ringingCall.getVideoState());
                    } else {
                        outgoingCall.disconnect();
                    }
                    return true;
                }
                if (ringingCall != null) {
                    this.mCallsManager.answerCall(ringingCall, ringingCall.getVideoState());
                    return true;
                }
                if (heldCall != null) {
                    this.mCallsManager.unholdCall(heldCall);
                    return true;
                }
                Log.w("BluetoothPhoneService", "[processChld] Should not execute here! no calls found ?", new Object[0]);
                return false;
            }
            if (i == 2) {
                if (activeCall != null && activeCall.can(8)) {
                    activeCall.swapConference();
                    Log.i("BluetoothPhoneService", "CDMA calls in conference swapped, updating headset", new Object[0]);
                    updateHeadsetWithCallState(true);
                    return true;
                }
                if (ringingCall != null) {
                    this.mCallsManager.answerCall(ringingCall, 0);
                    return true;
                }
                if (heldCall != null) {
                    this.mCallsManager.unholdCall(heldCall);
                    return true;
                }
                if (activeCall != null && activeCall.can(1)) {
                    this.mCallsManager.holdCall(activeCall);
                    return true;
                }
            } else if (i == 3 && activeCall != null) {
                if (activeCall.can(4)) {
                    activeCall.mergeConference();
                    return true;
                }
                List<Call> conferenceableCalls = activeCall.getConferenceableCalls();
                if (!conferenceableCalls.isEmpty()) {
                    this.mCallsManager.conference(activeCall, conferenceableCalls.get(0));
                    return true;
                }
            }
        }
        return false;
    }

    private void enforceModifyPermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MODIFY_PHONE_STATE", null);
    }

    private void sendListOfCalls(boolean z) {
        removeConferenceCallIfNeeded();
        for (Call call : this.mCallsManager.getCalls()) {
            if (!call.isConference() || (call.isConference() && call.can(2097152))) {
                sendClccForCall(call, z);
            }
        }
        sendClccEndMarker();
    }

    private void sendClccForCall(Call call, boolean z) {
        int i;
        boolean z2;
        Uri handle;
        boolean z3;
        int iConvertCallState = convertCallState(call.getState(), this.mCallsManager.getForegroundCall() == call, call);
        Object[] objArr = call.isConference() && call.can(2097152);
        if (iConvertCallState == 6) {
            return;
        }
        Call parentCall = call.getParentCall();
        if (parentCall != null) {
            Call conferenceLevelActiveCall = parentCall.getConferenceLevelActiveCall();
            if (iConvertCallState == 0 && conferenceLevelActiveCall != null) {
                if ((parentCall.can(4) || (parentCall.can(8) && !parentCall.wasConferencePreviouslyMerged())) != false) {
                    if (call == conferenceLevelActiveCall) {
                        iConvertCallState = 0;
                        z3 = false;
                    } else {
                        z3 = false;
                        iConvertCallState = 1;
                    }
                }
                if (parentCall.getState() == 6) {
                    iConvertCallState = 1;
                }
                i = iConvertCallState;
                z2 = z3;
            } else {
                z3 = true;
                if (parentCall.getState() == 6 && parentCall.can(128)) {
                    iConvertCallState = 1;
                }
                i = iConvertCallState;
                z2 = z3;
            }
        } else if (objArr == true) {
            i = iConvertCallState;
            z2 = true;
        } else {
            i = iConvertCallState;
            z2 = false;
        }
        int indexForCall = getIndexForCall(call);
        boolean zIsIncoming = call.isIncoming();
        if (call.getGatewayInfo() != null) {
            handle = call.getGatewayInfo().getOriginalAddress();
        } else {
            handle = call.getHandle();
        }
        String schemeSpecificPart = handle == null ? null : handle.getSchemeSpecificPart();
        if (schemeSpecificPart != null) {
            schemeSpecificPart = PhoneNumberUtils.stripSeparators(schemeSpecificPart);
        }
        String str = schemeSpecificPart;
        int i2 = str == null ? -1 : PhoneNumberUtils.toaFromString(str);
        if (z) {
            Log.i(this, "sending clcc for call %d, %d, %d, %b, %s, %d", new Object[]{Integer.valueOf(indexForCall), Integer.valueOf(zIsIncoming ? 1 : 0), Integer.valueOf(i), Boolean.valueOf(z2), Log.piiHandle(str), Integer.valueOf(i2)});
        }
        if (this.mBluetoothHeadset != null) {
            this.mBluetoothHeadset.clccResponse(indexForCall, zIsIncoming ? 1 : 0, i, 0, z2, str, i2);
        }
    }

    private void sendClccEndMarker() {
        if (this.mBluetoothHeadset != null) {
            this.mBluetoothHeadset.clccResponse(0, 0, 0, 0, false, null, 0);
        }
    }

    private int getIndexForCall(Call call) {
        if (this.mClccIndexMap.containsKey(call)) {
            return this.mClccIndexMap.get(call).intValue();
        }
        int i = 1;
        while (this.mClccIndexMap.containsValue(Integer.valueOf(i))) {
            i++;
        }
        this.mClccIndexMap.put(call, Integer.valueOf(i));
        return i;
    }

    private void updateHeadsetWithCallState(boolean z) {
        String schemeSpecificPart;
        int size;
        Object[] objArr;
        boolean z2;
        Call activeCall = this.mCallsManager.getActiveCall();
        Call ringingCall = this.mCallsManager.getRingingCall();
        Call heldCall = this.mCallsManager.getHeldCall();
        int bluetoothCallStateForUpdate = getBluetoothCallStateForUpdate();
        int i = 128;
        if (ringingCall != null && ringingCall.getHandle() != null) {
            schemeSpecificPart = ringingCall.getHandle().getSchemeSpecificPart();
            if (schemeSpecificPart != null) {
                i = PhoneNumberUtils.toaFromString(schemeSpecificPart);
            }
        } else {
            schemeSpecificPart = null;
        }
        if (schemeSpecificPart == null) {
            schemeSpecificPart = "";
        }
        int i2 = activeCall == null ? 0 : 1;
        int numHeldCalls = this.mCallsManager.getNumHeldCalls();
        if (activeCall != null) {
            size = activeCall.getChildCalls().size();
        } else {
            size = 0;
        }
        Object[] objArr2 = numHeldCalls == 2;
        if (activeCall != null && activeCall.isConference() && !activeCall.can(2097152)) {
            if (!activeCall.can(8)) {
                if (activeCall.can(4)) {
                    numHeldCalls = 1;
                }
            } else {
                numHeldCalls = !activeCall.wasConferencePreviouslyMerged() ? 1 : 0;
            }
            Iterator<Call> it = activeCall.getChildCalls().iterator();
            while (it.hasNext()) {
                if (this.mOldHeldCall == it.next()) {
                    objArr = true;
                    break;
                }
            }
            objArr = false;
        } else {
            objArr = false;
        }
        if (activeCall == null || !activeCall.isConference()) {
            z2 = false;
            break;
        }
        Iterator<Call> it2 = activeCall.getChildCalls().iterator();
        while (it2.hasNext()) {
            if (it2.next().getState() != 0) {
                z2 = false;
                break;
            }
        }
        z2 = true;
        Log.d("BluetoothPhoneService", "isConferenceWithNoActiveChildren = " + z2, new Object[0]);
        if (this.mBluetoothHeadset != null) {
            if (!z) {
                if (objArr2 == false && !z2) {
                    if (i2 == this.mNumActiveCalls && size == this.mNumChildrenOfActiveCall && numHeldCalls == this.mNumHeldCalls && bluetoothCallStateForUpdate == this.mBluetoothCallState && TextUtils.equals(schemeSpecificPart, this.mRingingAddress) && i == this.mRingingAddressType && (heldCall == this.mOldHeldCall || objArr != false)) {
                        return;
                    }
                } else {
                    return;
                }
            }
            this.mOldHeldCall = heldCall;
            this.mNumActiveCalls = i2;
            this.mNumChildrenOfActiveCall = size;
            this.mNumHeldCalls = numHeldCalls;
            this.mBluetoothCallState = bluetoothCallStateForUpdate;
            this.mRingingAddress = schemeSpecificPart;
            this.mRingingAddressType = i;
            Log.i("BluetoothPhoneService", "updateHeadsetWithCallState numActive %s, numHeld %s, callState %s, ringing number %s, ringing type %s", new Object[]{Integer.valueOf(this.mNumActiveCalls), Integer.valueOf(this.mNumHeldCalls), Integer.valueOf(this.mBluetoothCallState), Log.pii(this.mRingingAddress), Integer.valueOf(this.mRingingAddressType)});
            this.mBluetoothHeadset.phoneStateChanged(this.mNumActiveCalls, this.mNumHeldCalls, this.mBluetoothCallState, this.mRingingAddress, this.mRingingAddressType);
            this.mHeadsetUpdatedRecently = true;
        }
    }

    private int getBluetoothCallStateForUpdate() {
        CallsManager callsManager = this.mCallsManager;
        Call ringingCall = this.mCallsManager.getRingingCall();
        Call outgoingCall = this.mCallsManager.getOutgoingCall();
        int i = ringingCall != null ? 4 : outgoingCall != null ? outgoingCall.isInAlertingState() ? 3 : 2 : (this.mCallsManager.hasOnlyDisconnectedCalls() || this.mIsDisconnectedTonePlaying) ? 7 : 6;
        if (i != 4 || outgoingCall == null || !outgoingCall.isEmergencyCall()) {
            return i;
        }
        Log.d("BluetoothPhoneService", "force to update idle state to BT", new Object[0]);
        return 6;
    }

    private int convertCallState(int i, boolean z, Call call) {
        switch (i) {
            case 1:
            case CallState.SELECT_PHONE_ACCOUNT:
            case CallState.DIALING:
            case CallState.PULLING:
                if (call.isInAlertingState()) {
                }
                break;
            case CallState.RINGING:
                if (z) {
                }
                break;
        }
        return 6;
    }

    private PhoneAccount getBestPhoneAccount() {
        if (this.mPhoneAccountRegistrar == null) {
            return null;
        }
        Call foregroundCall = this.mCallsManager.getForegroundCall();
        PhoneAccount phoneAccountOfCurrentUser = foregroundCall != null ? this.mPhoneAccountRegistrar.getPhoneAccountOfCurrentUser(foregroundCall.getTargetPhoneAccount()) : null;
        if (phoneAccountOfCurrentUser == null) {
            return this.mPhoneAccountRegistrar.getPhoneAccountUnchecked(this.mPhoneAccountRegistrar.getOutgoingPhoneAccountForSchemeOfCurrentUser("tel"));
        }
        return phoneAccountOfCurrentUser;
    }

    private void removeConferenceCallIfNeeded() {
        for (Call call : this.mClccIndexMap.keySet()) {
            if (call.isConference() && !call.can(2097152)) {
                Log.i("BluetoothPhoneService", "remove conference call from mClccIndexMap", new Object[0]);
                this.mClccIndexMap.remove(call);
                return;
            }
        }
    }
}
