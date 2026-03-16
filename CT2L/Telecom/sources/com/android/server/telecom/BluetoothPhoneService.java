package com.android.server.telecom;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.IBluetoothHeadsetPhone;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.telecom.PhoneAccount;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import com.android.server.telecom.CallsManager;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class BluetoothPhoneService extends Service {
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothHeadset mBluetoothHeadset;
    private int mNumActiveCalls = 0;
    private int mNumHeldCalls = 0;
    private int mBluetoothCallState = 6;
    private String mRingingAddress = null;
    private int mRingingAddressType = 0;
    private Call mOldHeldCall = null;
    private final IBluetoothHeadsetPhone.Stub mBinder = new IBluetoothHeadsetPhone.Stub() {
        public boolean answerCall() throws RemoteException {
            BluetoothPhoneService.this.enforceModifyPermission();
            Log.i("BluetoothPhoneService", "BT - answering call", new Object[0]);
            return ((Boolean) BluetoothPhoneService.this.sendSynchronousRequest(1)).booleanValue();
        }

        public boolean hangupCall() throws RemoteException {
            BluetoothPhoneService.this.enforceModifyPermission();
            Log.i("BluetoothPhoneService", "BT - hanging up call", new Object[0]);
            return ((Boolean) BluetoothPhoneService.this.sendSynchronousRequest(2)).booleanValue();
        }

        public boolean sendDtmf(int i) throws RemoteException {
            BluetoothPhoneService.this.enforceModifyPermission();
            Object[] objArr = new Object[1];
            objArr[0] = Integer.valueOf(Log.DEBUG ? i : 46);
            Log.i("BluetoothPhoneService", "BT - sendDtmf %c", objArr);
            return ((Boolean) BluetoothPhoneService.this.sendSynchronousRequest(3, i)).booleanValue();
        }

        public String getNetworkOperator() throws RemoteException {
            Log.i("BluetoothPhoneService", "getNetworkOperator", new Object[0]);
            BluetoothPhoneService.this.enforceModifyPermission();
            return (String) BluetoothPhoneService.this.sendSynchronousRequest(5);
        }

        public String getSubscriberNumber() throws RemoteException {
            Log.i("BluetoothPhoneService", "getSubscriberNumber", new Object[0]);
            BluetoothPhoneService.this.enforceModifyPermission();
            return (String) BluetoothPhoneService.this.sendSynchronousRequest(8);
        }

        public boolean listCurrentCalls() throws RemoteException {
            boolean z = BluetoothPhoneService.this.mHeadsetUpdatedRecently;
            BluetoothPhoneService.this.mHeadsetUpdatedRecently = false;
            if (z) {
                Log.i("BluetoothPhoneService", "listcurrentCalls", new Object[0]);
            }
            BluetoothPhoneService.this.enforceModifyPermission();
            return ((Boolean) BluetoothPhoneService.this.sendSynchronousRequest(6, z ? 1 : 0)).booleanValue();
        }

        public boolean queryPhoneState() throws RemoteException {
            Log.i("BluetoothPhoneService", "queryPhoneState", new Object[0]);
            BluetoothPhoneService.this.enforceModifyPermission();
            return ((Boolean) BluetoothPhoneService.this.sendSynchronousRequest(7)).booleanValue();
        }

        public boolean processChld(int i) throws RemoteException {
            Log.i("BluetoothPhoneService", "processChld %d", Integer.valueOf(i));
            BluetoothPhoneService.this.enforceModifyPermission();
            return ((Boolean) BluetoothPhoneService.this.sendSynchronousRequest(4, i)).booleanValue();
        }

        public void updateBtHandsfreeAfterRadioTechnologyChange() throws RemoteException {
            Log.d("BluetoothPhoneService", "RAT change", new Object[0]);
        }

        public void cdmaSetSecondCallState(boolean z) throws RemoteException {
            Log.d("BluetoothPhoneService", "cdma 1", new Object[0]);
        }

        public void cdmaSwapSecondCallState() throws RemoteException {
            Log.d("BluetoothPhoneService", "cdma 2", new Object[0]);
        }
    };
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message message) throws Throwable {
            String networkOperatorName;
            Call foregroundCall;
            Throwable th;
            boolean zValueOf;
            String str;
            Throwable th2;
            Uri address;
            Call ringingCall;
            Throwable th3;
            Call foregroundCall2;
            Throwable th4;
            String line1Number = null;
            Call call = message.obj instanceof MainThreadRequest ? (MainThreadRequest) message.obj : null;
            CallsManager callsManager = BluetoothPhoneService.this.getCallsManager();
            Object[] objArr = new Object[2];
            objArr[0] = Integer.valueOf(message.what);
            objArr[1] = call == null ? null : Integer.valueOf(call.param);
            Log.d("BluetoothPhoneService", "handleMessage(%d) w/ param %s", objArr);
            switch (message.what) {
                case 1:
                    try {
                        ringingCall = callsManager.getRingingCall();
                        if (ringingCall != null) {
                            try {
                                BluetoothPhoneService.this.getCallsManager().answerCall(ringingCall, 0);
                            } catch (Throwable th5) {
                                th3 = th5;
                                z = ringingCall != null;
                                throw th3;
                            }
                            break;
                        }
                        zValueOf = Boolean.valueOf(ringingCall != null);
                        return;
                    } catch (Throwable th6) {
                        ringingCall = null;
                        th3 = th6;
                    }
                    break;
                case 2:
                    try {
                        foregroundCall = callsManager.getForegroundCall();
                        if (foregroundCall != null) {
                            try {
                                callsManager.disconnectCall(foregroundCall);
                            } catch (Throwable th7) {
                                th = th7;
                                z = foregroundCall != null;
                                throw th;
                            }
                            break;
                        }
                        z = foregroundCall != null;
                        return;
                    } catch (Throwable th8) {
                        foregroundCall = null;
                        th = th8;
                    }
                    break;
                case 3:
                    try {
                        foregroundCall2 = callsManager.getForegroundCall();
                        if (foregroundCall2 != null) {
                            try {
                                callsManager.playDtmfTone(foregroundCall2, (char) call.param);
                                callsManager.stopDtmfTone(foregroundCall2);
                            } catch (Throwable th9) {
                                th4 = th9;
                                throw th4;
                            }
                            break;
                        }
                        z = foregroundCall2 != null;
                        return;
                    } catch (Throwable th10) {
                        foregroundCall2 = null;
                        th4 = th10;
                    }
                    break;
                case 4:
                    zValueOf = false;
                    try {
                        zValueOf = Boolean.valueOf(BluetoothPhoneService.this.processChld(call.param));
                        return;
                    } finally {
                        call.setResult(zValueOf);
                    }
                case 5:
                    try {
                        PhoneAccount bestPhoneAccount = BluetoothPhoneService.this.getBestPhoneAccount();
                        if (bestPhoneAccount != null) {
                            networkOperatorName = bestPhoneAccount.getLabel().toString();
                        } else {
                            networkOperatorName = TelephonyManager.from(BluetoothPhoneService.this).getNetworkOperatorName();
                        }
                        call.setResult(networkOperatorName);
                        return;
                    } finally {
                        call.setResult(null);
                    }
                case 6:
                    try {
                        BluetoothPhoneService.this.sendListOfCalls(call.param == 1);
                        return;
                    } finally {
                        zValueOf = true;
                    }
                case 7:
                    try {
                        BluetoothPhoneService.this.updateHeadsetWithCallState(true);
                        if (call != null) {
                            return;
                        } else {
                            return;
                        }
                    } finally {
                        if (call != null) {
                        }
                    }
                case 8:
                    try {
                        PhoneAccount bestPhoneAccount2 = BluetoothPhoneService.this.getBestPhoneAccount();
                        if (bestPhoneAccount2 != null && (address = bestPhoneAccount2.getAddress()) != null) {
                            line1Number = address.getSchemeSpecificPart();
                        }
                    } catch (Throwable th11) {
                        str = null;
                        th2 = th11;
                    }
                    try {
                        if (TextUtils.isEmpty(line1Number)) {
                            line1Number = TelephonyManager.from(BluetoothPhoneService.this).getLine1Number();
                            break;
                        }
                        return;
                    } catch (Throwable th12) {
                        str = line1Number;
                        th2 = th12;
                        call.setResult(str);
                        throw th2;
                    }
                default:
                    return;
            }
        }
    };
    private CallsManager.CallsManagerListener mCallsManagerListener = new CallsManagerListenerBase() {
        @Override
        public void onCallAdded(Call call) {
            BluetoothPhoneService.this.updateHeadsetWithCallState(false);
        }

        @Override
        public void onCallRemoved(Call call) {
            BluetoothPhoneService.this.mClccIndexMap.remove(call);
            BluetoothPhoneService.this.updateHeadsetWithCallState(false);
        }

        @Override
        public void onCallStateChanged(Call call, int i, int i2) {
            if (i == 5 && i2 == 6) {
                Iterator<Call> it = CallsManager.getInstance().getCalls().iterator();
                while (it.hasNext()) {
                    if (it.next().getState() == 1) {
                        return;
                    }
                }
            }
            if (CallsManager.getInstance().getActiveCall() == null || i != 1 || i2 != 3) {
                BluetoothPhoneService.this.updateHeadsetWithCallState(false);
            }
        }

        @Override
        public void onForegroundCallChanged(Call call, Call call2) {
        }

        @Override
        public void onIsConferencedChanged(Call call) {
            if (call.getParentCall() != null) {
                Log.d(this, "Ignoring onIsConferenceChanged from child call with new parent", new Object[0]);
            } else if (call.getChildCalls().size() != 1) {
                BluetoothPhoneService.this.updateHeadsetWithCallState(false);
            } else {
                Log.d(this, "Ignoring onIsConferenceChanged from parent with only one child call", new Object[0]);
            }
        }
    };
    private BluetoothProfile.ServiceListener mProfileListener = new BluetoothProfile.ServiceListener() {
        @Override
        public void onServiceConnected(int i, BluetoothProfile bluetoothProfile) {
            BluetoothPhoneService.this.mBluetoothHeadset = (BluetoothHeadset) bluetoothProfile;
        }

        @Override
        public void onServiceDisconnected(int i) {
            BluetoothPhoneService.this.mBluetoothHeadset = null;
        }
    };
    private final BroadcastReceiver mBluetoothAdapterReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int intExtra = intent.getIntExtra("android.bluetooth.adapter.extra.STATE", Integer.MIN_VALUE);
            Log.d("BluetoothPhoneService", "Bluetooth Adapter state: %d", Integer.valueOf(intExtra));
            if (intExtra == 12) {
                BluetoothPhoneService.this.mHandler.sendEmptyMessage(7);
            }
        }
    };
    private Map<Call, Integer> mClccIndexMap = new HashMap();
    private boolean mHeadsetUpdatedRecently = false;

    private static class MainThreadRequest {
        private static final Object RESULT_NOT_SET = new Object();
        int param;
        Object result = RESULT_NOT_SET;

        MainThreadRequest(int i) {
            this.param = i;
        }

        void setResult(Object obj) {
            this.result = obj;
            synchronized (this) {
                notifyAll();
            }
        }
    }

    public BluetoothPhoneService() {
        Log.v("BluetoothPhoneService", "Constructor", new Object[0]);
    }

    public static final void start(Context context) {
        if (BluetoothAdapter.getDefaultAdapter() != null) {
            context.startService(new Intent(context, (Class<?>) BluetoothPhoneService.class));
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d("BluetoothPhoneService", "Binding service", new Object[0]);
        return this.mBinder;
    }

    @Override
    public void onCreate() {
        Log.d("BluetoothPhoneService", "onCreate", new Object[0]);
        this.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (this.mBluetoothAdapter == null) {
            Log.d("BluetoothPhoneService", "BluetoothPhoneService shutting down, no BT Adapter found.", new Object[0]);
            return;
        }
        this.mBluetoothAdapter.getProfileProxy(this, this.mProfileListener, 1);
        registerReceiver(this.mBluetoothAdapterReceiver, new IntentFilter("android.bluetooth.adapter.action.STATE_CHANGED"));
        CallsManager.getInstance().addListener(this.mCallsManagerListener);
        updateHeadsetWithCallState(false);
    }

    @Override
    public void onDestroy() {
        Log.d("BluetoothPhoneService", "onDestroy", new Object[0]);
        CallsManager.getInstance().removeListener(this.mCallsManagerListener);
        super.onDestroy();
    }

    private boolean processChld(int i) {
        CallsManager callsManager = CallsManager.getInstance();
        Call activeCall = callsManager.getActiveCall();
        Call ringingCall = callsManager.getRingingCall();
        Call heldCall = callsManager.getHeldCall();
        Log.i("BluetoothPhoneService", "Active: %s\nRinging: %s\nHeld: %s", activeCall, ringingCall, heldCall);
        if (i == 0) {
            if (ringingCall != null) {
                callsManager.rejectCall(ringingCall, false, null);
                return true;
            }
            if (heldCall == null) {
                return false;
            }
            callsManager.disconnectCall(heldCall);
            return true;
        }
        if (i == 1) {
            if (activeCall == null) {
                return false;
            }
            callsManager.disconnectCall(activeCall);
            if (ringingCall != null) {
                callsManager.answerCall(ringingCall, 0);
            } else if (heldCall != null) {
                callsManager.unholdCall(heldCall);
            }
            return true;
        }
        if (i == 2) {
            if (activeCall != null && activeCall.can(8)) {
                activeCall.swapConference();
                return true;
            }
            if (ringingCall != null) {
                callsManager.answerCall(ringingCall, 0);
                return true;
            }
            if (heldCall != null) {
                callsManager.unholdCall(heldCall);
                return true;
            }
            if (activeCall == null || !activeCall.can(1)) {
                return false;
            }
            callsManager.holdCall(activeCall);
            return true;
        }
        if (i != 3 || activeCall == null) {
            return false;
        }
        if (activeCall.can(4)) {
            activeCall.mergeConference();
            return true;
        }
        List<Call> conferenceableCalls = activeCall.getConferenceableCalls();
        if (conferenceableCalls.isEmpty()) {
            return false;
        }
        callsManager.conference(activeCall, conferenceableCalls.get(0));
        return true;
    }

    private void enforceModifyPermission() {
        enforceCallingOrSelfPermission("android.permission.MODIFY_PHONE_STATE", null);
    }

    private <T> T sendSynchronousRequest(int i) {
        return (T) sendSynchronousRequest(i, 0);
    }

    private <T> T sendSynchronousRequest(int i, int i2) {
        if (Looper.myLooper() == this.mHandler.getLooper()) {
            Log.w("BluetoothPhoneService", "This method will deadlock if called from the main thread.", new Object[0]);
        }
        MainThreadRequest mainThreadRequest = new MainThreadRequest(i2);
        this.mHandler.obtainMessage(i, mainThreadRequest).sendToTarget();
        synchronized (mainThreadRequest) {
            while (mainThreadRequest.result == MainThreadRequest.RESULT_NOT_SET) {
                try {
                    mainThreadRequest.wait();
                } catch (InterruptedException e) {
                    Log.e("BluetoothPhoneService", (Throwable) e, "InterruptedException", new Object[0]);
                }
            }
        }
        if (mainThreadRequest.result != null) {
            return (T) mainThreadRequest.result;
        }
        return null;
    }

    private void sendListOfCalls(boolean z) {
        for (Call call : getCallsManager().getCalls()) {
            if (!call.isConference()) {
                sendClccForCall(call, z);
            }
        }
        sendClccEndMarker();
    }

    private void sendClccForCall(Call call, boolean z) {
        boolean z2;
        Uri handle;
        int iConvertCallState = convertCallState(call.getState(), getCallsManager().getForegroundCall() == call);
        if (iConvertCallState != 6) {
            Call parentCall = call.getParentCall();
            if (parentCall != null) {
                Call conferenceLevelActiveCall = parentCall.getConferenceLevelActiveCall();
                if (iConvertCallState != 0 || conferenceLevelActiveCall == null) {
                    z2 = true;
                } else {
                    if (parentCall.can(4) || (parentCall.can(8) && !parentCall.wasConferencePreviouslyMerged())) {
                        if (call == conferenceLevelActiveCall) {
                            z2 = false;
                            iConvertCallState = 0;
                        } else {
                            z2 = false;
                            iConvertCallState = 1;
                        }
                    }
                }
            } else {
                z2 = false;
            }
            int indexForCall = getIndexForCall(call);
            int i = call.isIncoming() ? 1 : 0;
            if (call.getGatewayInfo() != null) {
                handle = call.getGatewayInfo().getOriginalAddress();
            } else {
                handle = call.getHandle();
            }
            String schemeSpecificPart = handle == null ? null : handle.getSchemeSpecificPart();
            int i2 = schemeSpecificPart == null ? -1 : PhoneNumberUtils.toaFromString(schemeSpecificPart);
            if (z) {
                Log.i(this, "sending clcc for call %d, %d, %d, %b, %s, %d", Integer.valueOf(indexForCall), Integer.valueOf(i), Integer.valueOf(iConvertCallState), Boolean.valueOf(z2), Log.piiHandle(schemeSpecificPart), Integer.valueOf(i2));
            }
            if (this.mBluetoothHeadset != null) {
                this.mBluetoothHeadset.clccResponse(indexForCall, i, iConvertCallState, 0, z2, schemeSpecificPart, i2);
            }
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
        int i;
        String str;
        int i2;
        boolean z2;
        CallsManager callsManager = getCallsManager();
        Call activeCall = callsManager.getActiveCall();
        Call ringingCall = callsManager.getRingingCall();
        Call heldCall = callsManager.getHeldCall();
        int bluetoothCallStateForUpdate = getBluetoothCallStateForUpdate();
        if (ringingCall == null || ringingCall.getHandle() == null) {
            i = 128;
            str = null;
        } else {
            String schemeSpecificPart = ringingCall.getHandle().getSchemeSpecificPart();
            if (schemeSpecificPart == null) {
                i = 128;
                str = schemeSpecificPart;
            } else {
                i = PhoneNumberUtils.toaFromString(schemeSpecificPart);
                str = schemeSpecificPart;
            }
        }
        String str2 = str == null ? "" : str;
        int i3 = activeCall == null ? 0 : 1;
        int numHeldCalls = callsManager.getNumHeldCalls();
        if (activeCall != null && activeCall.isConference()) {
            if (activeCall.can(8)) {
                i2 = activeCall.wasConferencePreviouslyMerged() ? 0 : 1;
            } else {
                i2 = activeCall.can(4) ? 1 : numHeldCalls;
            }
            Iterator<Call> it = activeCall.getChildCalls().iterator();
            while (true) {
                if (!it.hasNext()) {
                    z2 = false;
                    break;
                } else if (this.mOldHeldCall == it.next()) {
                    z2 = true;
                    break;
                }
            }
        } else {
            i2 = numHeldCalls;
            z2 = false;
        }
        if (this.mBluetoothHeadset != null) {
            if (i3 != this.mNumActiveCalls || i2 != this.mNumHeldCalls || bluetoothCallStateForUpdate != this.mBluetoothCallState || !TextUtils.equals(str2, this.mRingingAddress) || i != this.mRingingAddressType || ((heldCall != this.mOldHeldCall && !z2) || z)) {
                boolean z3 = this.mBluetoothCallState != bluetoothCallStateForUpdate && bluetoothCallStateForUpdate == 3;
                this.mOldHeldCall = heldCall;
                this.mNumActiveCalls = i3;
                this.mNumHeldCalls = i2;
                this.mBluetoothCallState = bluetoothCallStateForUpdate;
                this.mRingingAddress = str2;
                this.mRingingAddressType = i;
                if (z3) {
                    Log.i("BluetoothPhoneService", "updateHeadsetWithCallState numActive %s, numHeld %s, callState %s, ringing number %s, ringing type %s", Integer.valueOf(this.mNumActiveCalls), Integer.valueOf(this.mNumHeldCalls), 2, Log.pii(this.mRingingAddress), Integer.valueOf(this.mRingingAddressType));
                    this.mBluetoothHeadset.phoneStateChanged(this.mNumActiveCalls, this.mNumHeldCalls, 2, this.mRingingAddress, this.mRingingAddressType);
                }
                Log.i("BluetoothPhoneService", "updateHeadsetWithCallState numActive %s, numHeld %s, callState %s, ringing number %s, ringing type %s", Integer.valueOf(this.mNumActiveCalls), Integer.valueOf(this.mNumHeldCalls), Integer.valueOf(this.mBluetoothCallState), Log.pii(this.mRingingAddress), Integer.valueOf(this.mRingingAddressType));
                this.mBluetoothHeadset.phoneStateChanged(this.mNumActiveCalls, this.mNumHeldCalls, this.mBluetoothCallState, this.mRingingAddress, this.mRingingAddressType);
                this.mHeadsetUpdatedRecently = true;
            }
        }
    }

    private int getBluetoothCallStateForUpdate() {
        CallsManager callsManager = getCallsManager();
        Call ringingCall = callsManager.getRingingCall();
        Call dialingCall = callsManager.getDialingCall();
        if (ringingCall != null) {
            return 4;
        }
        if (dialingCall == null) {
            return 6;
        }
        return 3;
    }

    private int convertCallState(int i, boolean z) {
        switch (i) {
            case 0:
            case 1:
            case 2:
            case 7:
            case 8:
            default:
                return 6;
            case 3:
                return 3;
            case 4:
                if (z) {
                    return 4;
                }
                return 5;
            case 5:
                return 0;
            case 6:
                return 1;
        }
    }

    private CallsManager getCallsManager() {
        return CallsManager.getInstance();
    }

    private PhoneAccount getBestPhoneAccount() {
        PhoneAccountRegistrar phoneAccountRegistrar = TelecomGlobals.getInstance().getPhoneAccountRegistrar();
        if (phoneAccountRegistrar == null) {
            return null;
        }
        Call foregroundCall = getCallsManager().getForegroundCall();
        PhoneAccount phoneAccount = foregroundCall != null ? phoneAccountRegistrar.getPhoneAccount(foregroundCall.getTargetPhoneAccount()) : null;
        if (phoneAccount == null) {
            return phoneAccountRegistrar.getPhoneAccount(phoneAccountRegistrar.getDefaultOutgoingPhoneAccount("tel"));
        }
        return phoneAccount;
    }
}
