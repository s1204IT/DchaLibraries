package com.android.bluetooth.hfpclient;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadsetClientCall;
import android.bluetooth.BluetoothUuid;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.os.Message;
import android.os.Parcelable;
import android.os.SystemClock;
import android.support.annotation.VisibleForTesting;
import android.util.Log;
import android.util.Pair;
import com.android.bluetooth.BluetoothMetricsProto;
import com.android.bluetooth.R;
import com.android.bluetooth.Utils;
import com.android.bluetooth.btservice.AdapterService;
import com.android.bluetooth.btservice.MetricsLogger;
import com.android.bluetooth.btservice.ProfileService;
import com.android.internal.util.IState;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.vcard.VCardConfig;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public class HeadsetClientStateMachine extends StateMachine {
    public static final int ACCEPT_CALL = 12;
    public static final int AT_OK = 0;
    public static final int CONNECT = 1;
    private static final int CONNECTING_TIMEOUT = 53;

    @VisibleForTesting
    static final int CONNECTING_TIMEOUT_MS = 12000;
    public static final int CONNECT_AUDIO = 3;
    private static final boolean DBG = false;
    public static final int DIAL_NUMBER = 10;
    public static final int DISABLE_NREC = 20;
    public static final int DISCONNECT = 2;
    public static final int DISCONNECT_AUDIO = 4;
    public static final int ENTER_PRIVATE_MODE = 16;
    public static final int EXPLICIT_CALL_TRANSFER = 18;
    static final int HF_ORIGINATED_CALL_ID = -1;
    public static final int HOLD_CALL = 14;
    static final int IN_BAND_RING_ENABLED = 1;
    private static final int MAX_HFP_SCO_VOICE_CALL_VOLUME = 15;
    private static final int MIN_HFP_SCO_VOICE_CALL_VOLUME = 1;
    static final int NO_ACTION = 0;
    private static final long OUTGOING_TIMEOUT_MILLI = 10000;
    private static final int QUERY_CURRENT_CALLS = 50;
    private static final long QUERY_CURRENT_CALLS_WAIT_MILLIS = 2000;
    private static final int QUERY_OPERATOR_NAME = 51;
    public static final int REJECT_CALL = 13;
    private static final int ROUTING_DELAY_MS = 250;
    public static final int SEND_DTMF = 17;
    public static final int SET_MIC_VOLUME = 7;
    public static final int SET_SPEAKER_VOLUME = 8;
    private static final int SUBSCRIBER_INFO = 52;
    private static final String TAG = "HeadsetClientStateMachine";
    public static final int TERMINATE_CALL = 15;
    static final int TERMINATE_SPECIFIC_CALL = 53;
    public static final int VOICE_RECOGNITION_START = 5;
    public static final int VOICE_RECOGNITION_STOP = 6;
    private static int sMaxAmVcVol;
    private static int sMinAmVcVol;
    private Uri alert;
    private final BluetoothAdapter mAdapter;
    private AudioFocusRequest mAudioFocusRequest;
    private AudioManager mAudioManager;
    private final AudioOn mAudioOn;
    private int mAudioState;
    private boolean mAudioWbs;
    private final Hashtable<Integer, BluetoothHeadsetClientCall> mCalls;
    private final Hashtable<Integer, BluetoothHeadsetClientCall> mCallsUpdate;
    private int mChldFeatures;
    private long mClccTimer;
    private final Connected mConnected;
    private final Connecting mConnecting;
    private BluetoothDevice mCurrentDevice;
    private final Disconnected mDisconnected;
    private boolean mInBandRing;
    private int mInBandRingtone;
    private int mIndicatorBatteryLevel;
    private int mIndicatorNetworkSignal;
    private int mIndicatorNetworkState;
    private int mIndicatorNetworkType;
    private NativeInterface mNativeInterface;
    private String mOperatorName;
    private int mPeerFeatures;
    private Pair<Integer, Object> mPendingAction;
    private State mPrevState;
    private Queue<Pair<Integer, Object>> mQueuedActions;
    private Ringtone mRingtone;
    private final HeadsetClientService mService;
    private String mSubscriberInfo;
    private int mVoiceRecognitionActive;
    public static boolean unexpected_call = false;
    private static boolean sAudioIsRouted = false;

    public void setNativeInterface(NativeInterface nativeInterface) {
        this.mNativeInterface = nativeInterface;
        Log.d(TAG, "setNativeInterface");
    }

    public IState getDisconnectedState() {
        return this.mDisconnected;
    }

    public boolean getInBandRing() {
        return this.mInBandRing;
    }

    public void dump(StringBuilder sb) {
        ProfileService.println(sb, "mCurrentDevice: " + this.mCurrentDevice);
        ProfileService.println(sb, "mAudioState: " + this.mAudioState);
        ProfileService.println(sb, "mAudioWbs: " + this.mAudioWbs);
        ProfileService.println(sb, "mIndicatorNetworkState: " + this.mIndicatorNetworkState);
        ProfileService.println(sb, "mIndicatorNetworkType: " + this.mIndicatorNetworkType);
        ProfileService.println(sb, "mIndicatorNetworkSignal: " + this.mIndicatorNetworkSignal);
        ProfileService.println(sb, "mIndicatorBatteryLevel: " + this.mIndicatorBatteryLevel);
        ProfileService.println(sb, "mRingtone: " + this.mRingtone);
        ProfileService.println(sb, "mOperatorName: " + this.mOperatorName);
        ProfileService.println(sb, "mSubscriberInfo: " + this.mSubscriberInfo);
        ProfileService.println(sb, "mInBandRingtone: " + this.mInBandRingtone);
        ProfileService.println(sb, "mCalls:");
        if (this.mCalls != null) {
            Iterator<BluetoothHeadsetClientCall> it = this.mCalls.values().iterator();
            while (it.hasNext()) {
                ProfileService.println(sb, "  " + it.next());
            }
        }
        ProfileService.println(sb, "mCallsUpdate:");
        if (this.mCallsUpdate != null) {
            Iterator<BluetoothHeadsetClientCall> it2 = this.mCallsUpdate.values().iterator();
            while (it2.hasNext()) {
                ProfileService.println(sb, "  " + it2.next());
            }
        }
        ProfileService.println(sb, "State machine stats:");
        ProfileService.println(sb, toString());
    }

    private void clearPendingAction() {
        this.mPendingAction = new Pair<>(0, 0);
    }

    private void addQueuedAction(int i) {
        addQueuedAction(i, 0);
    }

    private void addQueuedAction(int i, Object obj) {
        this.mQueuedActions.add(new Pair<>(Integer.valueOf(i), obj));
    }

    private void addQueuedAction(int i, int i2) {
        this.mQueuedActions.add(new Pair<>(Integer.valueOf(i), Integer.valueOf(i2)));
    }

    private BluetoothHeadsetClientCall getCall(int... iArr) {
        for (BluetoothHeadsetClientCall bluetoothHeadsetClientCall : this.mCalls.values()) {
            for (int i : iArr) {
                if (bluetoothHeadsetClientCall.getState() == i) {
                    return bluetoothHeadsetClientCall;
                }
            }
        }
        return null;
    }

    private int callsInState(int i) {
        Iterator<BluetoothHeadsetClientCall> it = this.mCalls.values().iterator();
        int i2 = 0;
        while (it.hasNext()) {
            if (it.next().getState() == i) {
                i2++;
            }
        }
        return i2;
    }

    private void sendCallChangedIntent(BluetoothHeadsetClientCall bluetoothHeadsetClientCall) {
        Intent intent = new Intent("android.bluetooth.headsetclient.profile.action.AG_CALL_CHANGED");
        intent.addFlags(VCardConfig.FLAG_REFRAIN_QP_TO_NAME_PROPERTIES);
        intent.putExtra("android.bluetooth.headsetclient.extra.CALL", (Parcelable) bluetoothHeadsetClientCall);
        this.mService.sendBroadcast(intent, ProfileService.BLUETOOTH_PERM);
    }

    private boolean queryCallsStart() {
        clearPendingAction();
        this.mNativeInterface.queryCurrentCallsNative(getByteAddress(this.mCurrentDevice));
        addQueuedAction(QUERY_CURRENT_CALLS, 0);
        return true;
    }

    private void queryCallsDone() {
        HashSet hashSet = new HashSet();
        hashSet.addAll(this.mCalls.keySet());
        hashSet.remove(-1);
        HashSet hashSet2 = new HashSet();
        hashSet2.addAll(this.mCallsUpdate.keySet());
        HashSet<Integer> hashSet3 = new HashSet();
        hashSet3.addAll(hashSet2);
        hashSet3.removeAll(hashSet);
        HashSet hashSet4 = new HashSet();
        hashSet4.addAll(hashSet);
        hashSet4.removeAll(hashSet2);
        HashSet<Integer> hashSet5 = new HashSet();
        hashSet5.addAll(hashSet);
        hashSet5.retainAll(hashSet2);
        Integer.valueOf(-1);
        if (this.mCalls.containsKey(-1)) {
            long creationElapsedMilli = this.mCalls.get(-1).getCreationElapsedMilli();
            if (hashSet3.size() > 0) {
                Integer num = (Integer) hashSet3.toArray()[0];
                this.mCalls.put(num, this.mCalls.get(-1));
                this.mCalls.remove(-1);
                hashSet3.remove(num);
                hashSet5.add(num);
            } else if (SystemClock.elapsedRealtime() - creationElapsedMilli > OUTGOING_TIMEOUT_MILLI) {
                Log.w(TAG, "Outgoing call did not see a response, clear the calls and send CHUP");
                terminateCall();
                Iterator<Integer> it = this.mCalls.keySet().iterator();
                while (it.hasNext()) {
                    BluetoothHeadsetClientCall bluetoothHeadsetClientCall = this.mCalls.get(it.next());
                    bluetoothHeadsetClientCall.setState(7);
                    sendCallChangedIntent(bluetoothHeadsetClientCall);
                    if (this.mAudioManager.getMode() != 0) {
                        this.mAudioManager.setMode(0);
                        Log.d(TAG, "abandonAudioFocus ");
                        this.mAudioManager.abandonAudioFocusForCall();
                    }
                }
                this.mCalls.clear();
                return;
            }
        }
        Iterator it2 = hashSet4.iterator();
        while (it2.hasNext()) {
            BluetoothHeadsetClientCall bluetoothHeadsetClientCallRemove = this.mCalls.remove((Integer) it2.next());
            bluetoothHeadsetClientCallRemove.setState(7);
            sendCallChangedIntent(bluetoothHeadsetClientCallRemove);
            if (this.mAudioManager.getMode() != 0) {
                this.mAudioManager.setMode(0);
                Log.d(TAG, "abandonAudioFocus ");
                this.mAudioManager.abandonAudioFocusForCall();
            }
        }
        for (Integer num2 : hashSet3) {
            BluetoothHeadsetClientCall bluetoothHeadsetClientCall2 = this.mCallsUpdate.get(num2);
            this.mCalls.put(num2, bluetoothHeadsetClientCall2);
            sendCallChangedIntent(bluetoothHeadsetClientCall2);
        }
        for (Integer num3 : hashSet5) {
            BluetoothHeadsetClientCall bluetoothHeadsetClientCall3 = this.mCalls.get(num3);
            BluetoothHeadsetClientCall bluetoothHeadsetClientCall4 = this.mCallsUpdate.get(num3);
            bluetoothHeadsetClientCall3.setNumber(bluetoothHeadsetClientCall4.getNumber());
            bluetoothHeadsetClientCall3.setState(bluetoothHeadsetClientCall4.getState());
            bluetoothHeadsetClientCall3.setMultiParty(bluetoothHeadsetClientCall4.isMultiParty());
            sendCallChangedIntent(bluetoothHeadsetClientCall3);
        }
        if (this.mCalls.size() > 0) {
            if (this.mService.getResources().getBoolean(R.bool.hfp_clcc_poll_during_call)) {
                sendMessageDelayed(QUERY_CURRENT_CALLS, QUERY_CURRENT_CALLS_WAIT_MILLIS);
            } else if (getCall(4) != null) {
                Log.d(TAG, "Still have incoming call; polling");
                sendMessageDelayed(QUERY_CURRENT_CALLS, QUERY_CURRENT_CALLS_WAIT_MILLIS);
            } else {
                removeMessages(QUERY_CURRENT_CALLS);
            }
        }
        this.mCallsUpdate.clear();
    }

    private void queryCallsUpdate(int i, int i2, String str, boolean z, boolean z2) {
        this.mCallsUpdate.put(Integer.valueOf(i), new BluetoothHeadsetClientCall(this.mCurrentDevice, i, i2, str, z, z2, this.mInBandRing));
    }

    private void acceptCall(int i) {
        int i2 = 2;
        BluetoothHeadsetClientCall call = getCall(4, 5);
        if (call == null && (call = getCall(6, 1)) == null) {
            return;
        }
        int state = call.getState();
        if (state != 1) {
            switch (state) {
                case 4:
                    if (i == 0) {
                        i2 = 7;
                        break;
                    }
                    break;
                case 5:
                    if (callsInState(0) == 0) {
                        if (i != 0) {
                        }
                    } else {
                        if (i != 1 && i != 0) {
                            if (i != 2) {
                                Log.e(TAG, "Aceept call with invalid flag: " + i);
                            }
                            i2 = 1;
                        }
                        break;
                    }
                    break;
                case 6:
                    i2 = 10;
                    break;
            }
            return;
        }
        if (i != 1) {
            if (i == 2) {
                i2 = 1;
            } else if (getCall(0) != null) {
                i2 = 3;
            } else if (i == 0) {
            }
        }
        if (i == 1) {
            routeHfpAudio(true);
        }
        if (this.mNativeInterface.handleCallActionNative(getByteAddress(this.mCurrentDevice), i2, 0)) {
            addQueuedAction(12, i2);
            return;
        }
        Log.e(TAG, "ERROR: Couldn't accept a call, action:" + i2);
    }

    private void rejectCall() {
        int i;
        if (this.mRingtone != null && this.mRingtone.isPlaying()) {
            Log.d(TAG, "stopping ring after call reject");
            this.mRingtone.stop();
        }
        BluetoothHeadsetClientCall call = getCall(4, 5, 6, 1);
        if (call == null) {
            return;
        }
        int state = call.getState();
        if (state != 1) {
            switch (state) {
                case 4:
                    i = 8;
                    break;
                case 5:
                    i = 0;
                    break;
                case 6:
                    i = 11;
                    break;
                default:
                    return;
            }
        }
        if (this.mNativeInterface.handleCallActionNative(getByteAddress(this.mCurrentDevice), i, 0)) {
            addQueuedAction(13, i);
            return;
        }
        Log.e(TAG, "ERROR: Couldn't reject a call, action:" + i);
    }

    private void holdCall() {
        int i;
        if (getCall(4) != null) {
            i = 9;
        } else if (getCall(0) == null) {
            return;
        } else {
            i = 2;
        }
        if (this.mNativeInterface.handleCallActionNative(getByteAddress(this.mCurrentDevice), i, 0)) {
            addQueuedAction(14, i);
            return;
        }
        Log.e(TAG, "ERROR: Couldn't hold a call, action:" + i);
    }

    private void terminateCall() {
        int i;
        BluetoothHeadsetClientCall call = getCall(2, 3, 0);
        if (call == null) {
            call = getCall(1);
            i = 0;
        } else {
            i = 8;
        }
        if (call != null) {
            if (this.mNativeInterface.handleCallActionNative(getByteAddress(this.mCurrentDevice), i, 0)) {
                addQueuedAction(15, i);
            } else {
                Log.e(TAG, "ERROR: Couldn't terminate outgoing call");
            }
        }
    }

    private void enterPrivateMode(int i) {
        BluetoothHeadsetClientCall bluetoothHeadsetClientCall = this.mCalls.get(Integer.valueOf(i));
        if (bluetoothHeadsetClientCall == null || bluetoothHeadsetClientCall.getState() != 0 || !bluetoothHeadsetClientCall.isMultiParty()) {
            return;
        }
        if (this.mNativeInterface.handleCallActionNative(getByteAddress(this.mCurrentDevice), 6, i)) {
            addQueuedAction(16, bluetoothHeadsetClientCall);
            return;
        }
        Log.e(TAG, "ERROR: Couldn't enter private  id:" + i);
    }

    private void explicitCallTransfer() {
        if (this.mCalls.size() < 2) {
            return;
        }
        if (this.mNativeInterface.handleCallActionNative(getByteAddress(this.mCurrentDevice), 4, -1)) {
            addQueuedAction(18);
        } else {
            Log.e(TAG, "ERROR: Couldn't transfer call");
        }
    }

    public Bundle getCurrentAgFeatures() {
        Bundle bundle = new Bundle();
        if ((this.mPeerFeatures & 1) == 1) {
            bundle.putBoolean("android.bluetooth.headsetclient.extra.EXTRA_AG_FEATURE_3WAY_CALLING", true);
        }
        if ((this.mPeerFeatures & 32) == 32) {
            bundle.putBoolean("android.bluetooth.headsetclient.extra.EXTRA_AG_FEATURE_REJECT_CALL", true);
        }
        if ((this.mPeerFeatures & 128) == 128) {
            bundle.putBoolean("android.bluetooth.headsetclient.extra.EXTRA_AG_FEATURE_ECC", true);
        }
        if ((this.mChldFeatures & 8) == 8) {
            bundle.putBoolean("android.bluetooth.headsetclient.extra.EXTRA_AG_FEATURE_ACCEPT_HELD_OR_WAITING_CALL", true);
        }
        if ((this.mChldFeatures & 1) == 1) {
            bundle.putBoolean("android.bluetooth.headsetclient.extra.EXTRA_AG_FEATURE_RELEASE_HELD_OR_WAITING_CALL", true);
        }
        if ((this.mChldFeatures & 2) == 2) {
            bundle.putBoolean("android.bluetooth.headsetclient.extra.EXTRA_AG_FEATURE_RELEASE_AND_ACCEPT", true);
        }
        if ((this.mChldFeatures & 32) == 32) {
            bundle.putBoolean("android.bluetooth.headsetclient.extra.EXTRA_AG_FEATURE_MERGE", true);
        }
        if ((this.mChldFeatures & 64) == 64) {
            bundle.putBoolean("android.bluetooth.headsetclient.extra.EXTRA_AG_FEATURE_MERGE_AND_DETACH", true);
        }
        return bundle;
    }

    HeadsetClientStateMachine(HeadsetClientService headsetClientService, Looper looper) {
        super(TAG, looper);
        this.mClccTimer = 0L;
        this.mNativeInterface = null;
        this.mCalls = new Hashtable<>();
        this.mCallsUpdate = new Hashtable<>();
        this.alert = RingtoneManager.getDefaultUri(4);
        this.mRingtone = null;
        this.mCurrentDevice = null;
        this.mService = headsetClientService;
        this.mAudioManager = this.mService.getAudioManager();
        this.mAdapter = BluetoothAdapter.getDefaultAdapter();
        this.mAudioState = 0;
        this.mAudioWbs = false;
        if (this.alert == null) {
            this.alert = RingtoneManager.getDefaultUri(2);
            if (this.alert == null) {
                this.alert = RingtoneManager.getDefaultUri(1);
            }
        }
        if (this.alert != null) {
            this.mRingtone = RingtoneManager.getRingtone(this.mService, this.alert);
            Log.i(TAG, "mRingtone: " + this.mRingtone);
        } else {
            Log.e(TAG, "alert is NULL no ringtone");
        }
        this.mVoiceRecognitionActive = 0;
        this.mIndicatorNetworkState = 0;
        this.mIndicatorNetworkType = 0;
        this.mIndicatorNetworkSignal = 0;
        this.mIndicatorBatteryLevel = 0;
        sMaxAmVcVol = this.mAudioManager.getStreamMaxVolume(0);
        sMinAmVcVol = this.mAudioManager.getStreamMinVolume(0);
        this.mOperatorName = null;
        this.mSubscriberInfo = null;
        this.mInBandRingtone = 0;
        this.mQueuedActions = new LinkedList();
        clearPendingAction();
        this.mCalls.clear();
        this.mCallsUpdate.clear();
        this.mDisconnected = new Disconnected();
        this.mConnecting = new Connecting();
        this.mConnected = new Connected();
        this.mAudioOn = new AudioOn();
        addState(this.mDisconnected);
        addState(this.mConnecting);
        addState(this.mConnected);
        addState(this.mAudioOn, this.mConnected);
        setInitialState(this.mDisconnected);
    }

    static HeadsetClientStateMachine make(HeadsetClientService headsetClientService, Looper looper) {
        HeadsetClientStateMachine headsetClientStateMachine = new HeadsetClientStateMachine(headsetClientService, looper);
        headsetClientStateMachine.start();
        return headsetClientStateMachine;
    }

    synchronized void routeHfpAudio(boolean z) {
        if (this.mAudioManager == null) {
            Log.e(TAG, "AudioManager is null!");
            return;
        }
        if (z && !sAudioIsRouted) {
            this.mAudioManager.setParameters("hfp_enable=true");
        } else if (!z) {
            this.mAudioManager.setParameters("hfp_enable=false");
        }
        sAudioIsRouted = z;
    }

    private AudioFocusRequest requestAudioFocus() {
        AudioFocusRequest audioFocusRequestBuild = new AudioFocusRequest.Builder(2).setAudioAttributes(new AudioAttributes.Builder().setUsage(2).setContentType(1).build()).build();
        this.mAudioManager.requestAudioFocus(audioFocusRequestBuild);
        return audioFocusRequestBuild;
    }

    public void doQuit() {
        Log.d(TAG, "doQuit");
        routeHfpAudio(false);
        returnAudioFocusIfNecessary();
        quitNow();
    }

    private void returnAudioFocusIfNecessary() {
        if (this.mAudioFocusRequest == null) {
            return;
        }
        this.mAudioManager.abandonAudioFocusRequest(this.mAudioFocusRequest);
        this.mAudioFocusRequest = null;
    }

    static int hfToAmVol(int i) {
        int i2 = sMinAmVcVol + (((sMaxAmVcVol - sMinAmVcVol) * (i - 1)) / 14);
        Log.d(TAG, "HF -> AM " + i + " " + i2);
        return i2;
    }

    static int amToHfVol(int i) {
        int i2 = 1 + ((14 * (i - sMinAmVcVol)) / (sMaxAmVcVol > sMinAmVcVol ? sMaxAmVcVol - sMinAmVcVol : 1));
        Log.d(TAG, "AM -> HF " + i + " " + i2);
        return i2;
    }

    class Disconnected extends State {
        Disconnected() {
        }

        public void enter() {
            Log.d(HeadsetClientStateMachine.TAG, "Enter Disconnected: " + HeadsetClientStateMachine.this.getCurrentMessage().what);
            HeadsetClientStateMachine.unexpected_call = true;
            if (HeadsetClientStateMachine.this.mRingtone != null && HeadsetClientStateMachine.this.mRingtone.isPlaying()) {
                Log.d(HeadsetClientStateMachine.TAG, "stopping ring when disconnected");
                HeadsetClientStateMachine.this.mRingtone.stop();
                if (HeadsetClientStateMachine.this.mAudioManager.getMode() != 0) {
                    HeadsetClientStateMachine.this.mAudioManager.setMode(0);
                    Log.d(HeadsetClientStateMachine.TAG, "abandonAudioFocus ");
                    HeadsetClientStateMachine.this.mAudioManager.abandonAudioFocusForCall();
                }
            }
            HeadsetClientStateMachine.this.mIndicatorNetworkState = 0;
            HeadsetClientStateMachine.this.mIndicatorNetworkType = 0;
            HeadsetClientStateMachine.this.mIndicatorNetworkSignal = 0;
            HeadsetClientStateMachine.this.mIndicatorBatteryLevel = 0;
            HeadsetClientStateMachine.this.mInBandRingtone = 0;
            HeadsetClientStateMachine.this.mInBandRing = false;
            HeadsetClientStateMachine.this.mAudioWbs = false;
            HeadsetClientStateMachine.this.mOperatorName = null;
            HeadsetClientStateMachine.this.mSubscriberInfo = null;
            HeadsetClientStateMachine.this.mQueuedActions = new LinkedList();
            HeadsetClientStateMachine.this.clearPendingAction();
            HeadsetClientStateMachine.this.mCalls.clear();
            HeadsetClientStateMachine.this.mCallsUpdate.clear();
            HeadsetClientStateMachine.this.mPeerFeatures = 0;
            HeadsetClientStateMachine.this.mChldFeatures = 0;
            HeadsetClientStateMachine.this.removeMessages(HeadsetClientStateMachine.QUERY_CURRENT_CALLS);
            if (HeadsetClientStateMachine.this.mPrevState == HeadsetClientStateMachine.this.mConnecting) {
                HeadsetClientStateMachine.this.broadcastConnectionState(HeadsetClientStateMachine.this.mCurrentDevice, 0, 1);
            } else if (HeadsetClientStateMachine.this.mPrevState == HeadsetClientStateMachine.this.mConnected || HeadsetClientStateMachine.this.mPrevState == HeadsetClientStateMachine.this.mAudioOn) {
                HeadsetClientStateMachine.this.broadcastConnectionState(HeadsetClientStateMachine.this.mCurrentDevice, 0, 2);
            } else if (HeadsetClientStateMachine.this.mPrevState != null) {
                Log.e(HeadsetClientStateMachine.TAG, "Connected: Illegal state transition from " + HeadsetClientStateMachine.this.mPrevState.getName() + " to Connecting, mCurrentDevice=" + HeadsetClientStateMachine.this.mCurrentDevice);
            }
            HeadsetClientStateMachine.this.mCurrentDevice = null;
        }

        public synchronized boolean processMessage(Message message) {
            Log.d(HeadsetClientStateMachine.TAG, "Disconnected process message: " + message.what);
            if (HeadsetClientStateMachine.this.mCurrentDevice != null) {
                Log.e(HeadsetClientStateMachine.TAG, "ERROR: current device not null in Disconnected");
                return false;
            }
            int i = message.what;
            if (i != 100) {
                switch (i) {
                    case 1:
                        BluetoothDevice bluetoothDevice = (BluetoothDevice) message.obj;
                        if (!HeadsetClientStateMachine.this.mNativeInterface.connectNative(HeadsetClientStateMachine.this.getByteAddress(bluetoothDevice))) {
                            HeadsetClientStateMachine.this.broadcastConnectionState(bluetoothDevice, 0, 0);
                        } else {
                            HeadsetClientStateMachine.this.mCurrentDevice = bluetoothDevice;
                            HeadsetClientStateMachine.this.transitionTo(HeadsetClientStateMachine.this.mConnecting);
                        }
                        break;
                    case 2:
                        break;
                    default:
                        return false;
                }
            } else {
                StackEvent stackEvent = (StackEvent) message.obj;
                if (stackEvent.type == 1) {
                    processConnectionEvent(stackEvent.valueInt, stackEvent.device);
                } else {
                    Log.e(HeadsetClientStateMachine.TAG, "Disconnected: Unexpected stack event: " + stackEvent.type);
                }
            }
            return true;
        }

        private void processConnectionEvent(int i, BluetoothDevice bluetoothDevice) {
            if (i == 2) {
                Log.w(HeadsetClientStateMachine.TAG, "HFPClient Connecting from Disconnected state");
                if (!HeadsetClientStateMachine.this.okToConnect(bluetoothDevice)) {
                    Log.i(HeadsetClientStateMachine.TAG, "Incoming AG rejected. priority=" + HeadsetClientStateMachine.this.mService.getPriority(bluetoothDevice) + " bondState=" + bluetoothDevice.getBondState());
                    HeadsetClientStateMachine.this.mNativeInterface.disconnectNative(HeadsetClientStateMachine.this.getByteAddress(bluetoothDevice));
                    AdapterService.getAdapterService();
                    HeadsetClientStateMachine.this.broadcastConnectionState(bluetoothDevice, 0, 0);
                    return;
                }
                Log.i(HeadsetClientStateMachine.TAG, "Incoming AG accepted");
                HeadsetClientStateMachine.this.mCurrentDevice = bluetoothDevice;
                HeadsetClientStateMachine.this.transitionTo(HeadsetClientStateMachine.this.mConnecting);
                return;
            }
            Log.i(HeadsetClientStateMachine.TAG, "ignoring state: " + i);
        }

        public void exit() {
            HeadsetClientStateMachine.this.mPrevState = this;
        }
    }

    class Connecting extends State {
        Connecting() {
        }

        public void enter() {
            HeadsetClientStateMachine.this.sendMessageDelayed(53, 12000L);
            if (HeadsetClientStateMachine.this.mPrevState == HeadsetClientStateMachine.this.mDisconnected) {
                HeadsetClientStateMachine.this.broadcastConnectionState(HeadsetClientStateMachine.this.mCurrentDevice, 1, 0);
                return;
            }
            Log.e(HeadsetClientStateMachine.TAG, "Connected: Illegal state transition from " + (HeadsetClientStateMachine.this.mPrevState == null ? "null" : HeadsetClientStateMachine.this.mPrevState.getName()) + " to Connecting, mCurrentDevice=" + HeadsetClientStateMachine.this.mCurrentDevice);
        }

        public synchronized boolean processMessage(Message message) {
            int i = message.what;
            if (i == 53) {
                Log.w(HeadsetClientStateMachine.TAG, "Connection timeout for " + HeadsetClientStateMachine.this.mCurrentDevice);
                HeadsetClientStateMachine.this.transitionTo(HeadsetClientStateMachine.this.mDisconnected);
            } else if (i != 100) {
                switch (i) {
                    case 1:
                    case 2:
                    case 3:
                        HeadsetClientStateMachine.this.deferMessage(message);
                        break;
                    default:
                        Log.w(HeadsetClientStateMachine.TAG, "Message not handled " + message);
                        return false;
                }
            } else {
                StackEvent stackEvent = (StackEvent) message.obj;
                switch (stackEvent.type) {
                    case 1:
                        processConnectionEvent(stackEvent.valueInt, stackEvent.valueInt2, stackEvent.valueInt3, stackEvent.device);
                        break;
                    case 2:
                    case 4:
                    case 5:
                    case 6:
                    case 7:
                    case 9:
                    case 10:
                    case 11:
                    case 12:
                    case 13:
                    case 14:
                    case 16:
                        HeadsetClientStateMachine.this.deferMessage(message);
                        break;
                    case 3:
                    case 8:
                    case 15:
                    default:
                        Log.e(HeadsetClientStateMachine.TAG, "Connecting: ignoring stack event: " + stackEvent.type);
                        break;
                }
            }
            return true;
        }

        private void processConnectionEvent(int i, int i2, int i3, BluetoothDevice bluetoothDevice) {
            switch (i) {
                case 0:
                    HeadsetClientStateMachine.this.transitionTo(HeadsetClientStateMachine.this.mDisconnected);
                    break;
                case 1:
                    break;
                case 2:
                    if (!HeadsetClientStateMachine.this.mCurrentDevice.equals(bluetoothDevice)) {
                        Log.w(HeadsetClientStateMachine.TAG, "incoming connection event, device: " + bluetoothDevice);
                        HeadsetClientStateMachine.this.broadcastConnectionState(HeadsetClientStateMachine.this.mCurrentDevice, 0, 1);
                        HeadsetClientStateMachine.this.broadcastConnectionState(bluetoothDevice, 1, 0);
                        HeadsetClientStateMachine.this.mCurrentDevice = bluetoothDevice;
                    }
                    break;
                case 3:
                    Log.d(HeadsetClientStateMachine.TAG, "HFPClient Connected from Connecting state");
                    HeadsetClientStateMachine.this.mPeerFeatures = i2;
                    HeadsetClientStateMachine.this.mChldFeatures = i3;
                    if ((HeadsetClientStateMachine.this.mPeerFeatures & 64) == 0) {
                        HeadsetClientStateMachine.this.mNativeInterface.disconnectNative(HeadsetClientStateMachine.this.getByteAddress(bluetoothDevice));
                    } else {
                        if ((HeadsetClientStateMachine.this.mPeerFeatures & 2) == 2) {
                            if (HeadsetClientStateMachine.this.mNativeInterface.sendATCmdNative(HeadsetClientStateMachine.this.getByteAddress(HeadsetClientStateMachine.this.mCurrentDevice), 15, 1, 0, null)) {
                                HeadsetClientStateMachine.this.addQueuedAction(20);
                            } else {
                                Log.e(HeadsetClientStateMachine.TAG, "Failed to send NREC");
                            }
                        }
                        HeadsetClientStateMachine.this.deferMessage(HeadsetClientStateMachine.this.obtainMessage(8, HeadsetClientStateMachine.this.mAudioManager.getStreamVolume(0), 0));
                        HeadsetClientStateMachine.this.deferMessage(HeadsetClientStateMachine.this.obtainMessage(7, HeadsetClientStateMachine.this.mAudioManager.isMicrophoneMute() ? 0 : 15, 0));
                        HeadsetClientStateMachine.this.deferMessage(HeadsetClientStateMachine.this.obtainMessage(HeadsetClientStateMachine.SUBSCRIBER_INFO));
                        HeadsetClientStateMachine.this.transitionTo(HeadsetClientStateMachine.this.mConnected);
                    }
                    break;
                default:
                    Log.e(HeadsetClientStateMachine.TAG, "Incorrect state: " + i);
                    break;
            }
        }

        public void exit() {
            HeadsetClientStateMachine.this.removeMessages(53);
            HeadsetClientStateMachine.this.mPrevState = this;
        }
    }

    class Connected extends State {
        int mCommandedSpeakerVolume = -1;

        Connected() {
        }

        public void enter() {
            HeadsetClientStateMachine.this.mAudioWbs = false;
            this.mCommandedSpeakerVolume = -1;
            if (HeadsetClientStateMachine.this.mPrevState == HeadsetClientStateMachine.this.mConnecting) {
                HeadsetClientStateMachine.this.broadcastConnectionState(HeadsetClientStateMachine.this.mCurrentDevice, 2, 1);
                MetricsLogger.logProfileConnectionEvent(BluetoothMetricsProto.ProfileId.HEADSET_CLIENT);
            } else if (HeadsetClientStateMachine.this.mPrevState != HeadsetClientStateMachine.this.mAudioOn) {
                Log.e(HeadsetClientStateMachine.TAG, "Connected: Illegal state transition from " + (HeadsetClientStateMachine.this.mPrevState == null ? "null" : HeadsetClientStateMachine.this.mPrevState.getName()) + " to Connecting, mCurrentDevice=" + HeadsetClientStateMachine.this.mCurrentDevice);
            }
        }

        public synchronized boolean processMessage(Message message) {
            int i = message.what;
            if (i == 10) {
                BluetoothHeadsetClientCall bluetoothHeadsetClientCall = (BluetoothHeadsetClientCall) message.obj;
                HeadsetClientStateMachine.this.mCalls.put(-1, bluetoothHeadsetClientCall);
                if (HeadsetClientStateMachine.this.mNativeInterface.dialNative(HeadsetClientStateMachine.this.getByteAddress(HeadsetClientStateMachine.this.mCurrentDevice), bluetoothHeadsetClientCall.getNumber())) {
                    HeadsetClientStateMachine.this.addQueuedAction(10, bluetoothHeadsetClientCall.getNumber());
                    HeadsetClientStateMachine.this.sendMessage(HeadsetClientStateMachine.QUERY_CURRENT_CALLS);
                } else {
                    Log.e(HeadsetClientStateMachine.TAG, "ERROR: Cannot dial with a given number:" + ((String) message.obj));
                    bluetoothHeadsetClientCall.setState(7);
                    HeadsetClientStateMachine.this.sendCallChangedIntent(bluetoothHeadsetClientCall);
                    HeadsetClientStateMachine.this.mCalls.remove(-1);
                    if (HeadsetClientStateMachine.this.mAudioManager.getMode() != 0) {
                        HeadsetClientStateMachine.this.mAudioManager.setMode(0);
                        Log.d(HeadsetClientStateMachine.TAG, "abandonAudioFocus ");
                        HeadsetClientStateMachine.this.mAudioManager.abandonAudioFocusForCall();
                    }
                }
            } else if (i == HeadsetClientStateMachine.QUERY_CURRENT_CALLS) {
                HeadsetClientStateMachine.this.removeMessages(HeadsetClientStateMachine.QUERY_CURRENT_CALLS);
                if (HeadsetClientStateMachine.this.mCalls.size() > 0) {
                    HeadsetClientStateMachine.this.sendMessageDelayed(HeadsetClientStateMachine.QUERY_CURRENT_CALLS, HeadsetClientStateMachine.QUERY_CURRENT_CALLS_WAIT_MILLIS);
                }
                HeadsetClientStateMachine.this.queryCallsStart();
            } else if (i != HeadsetClientStateMachine.SUBSCRIBER_INFO) {
                if (i != 100) {
                    switch (i) {
                        case 1:
                            BluetoothDevice bluetoothDevice = (BluetoothDevice) message.obj;
                            if (!HeadsetClientStateMachine.this.mCurrentDevice.equals(bluetoothDevice)) {
                                HeadsetClientStateMachine.this.mNativeInterface.connectNative(HeadsetClientStateMachine.this.getByteAddress(bluetoothDevice));
                            }
                            break;
                        case 2:
                            BluetoothDevice bluetoothDevice2 = (BluetoothDevice) message.obj;
                            if (HeadsetClientStateMachine.this.mCurrentDevice.equals(bluetoothDevice2) && !HeadsetClientStateMachine.this.mNativeInterface.disconnectNative(HeadsetClientStateMachine.this.getByteAddress(bluetoothDevice2))) {
                                Log.e(HeadsetClientStateMachine.TAG, "disconnectNative failed for " + bluetoothDevice2);
                            }
                            break;
                        case 3:
                            if (HeadsetClientStateMachine.this.mNativeInterface.connectAudioNative(HeadsetClientStateMachine.this.getByteAddress(HeadsetClientStateMachine.this.mCurrentDevice))) {
                                HeadsetClientStateMachine.this.mAudioState = 1;
                            } else {
                                Log.e(HeadsetClientStateMachine.TAG, "ERROR: Couldn't connect Audio for device " + HeadsetClientStateMachine.this.mCurrentDevice);
                                HeadsetClientStateMachine.this.broadcastAudioState(HeadsetClientStateMachine.this.mCurrentDevice, 0, 0);
                            }
                            break;
                        case 4:
                            if (!HeadsetClientStateMachine.this.mNativeInterface.disconnectAudioNative(HeadsetClientStateMachine.this.getByteAddress(HeadsetClientStateMachine.this.mCurrentDevice))) {
                                Log.e(HeadsetClientStateMachine.TAG, "ERROR: Couldn't disconnect Audio for device " + HeadsetClientStateMachine.this.mCurrentDevice);
                            }
                            break;
                        case 5:
                            if (HeadsetClientStateMachine.this.mVoiceRecognitionActive == 0) {
                                if (HeadsetClientStateMachine.this.mNativeInterface.startVoiceRecognitionNative(HeadsetClientStateMachine.this.getByteAddress(HeadsetClientStateMachine.this.mCurrentDevice))) {
                                    HeadsetClientStateMachine.this.addQueuedAction(5);
                                } else {
                                    Log.e(HeadsetClientStateMachine.TAG, "ERROR: Couldn't start voice recognition");
                                }
                            }
                            break;
                        case 6:
                            if (HeadsetClientStateMachine.this.mVoiceRecognitionActive == 1) {
                                if (HeadsetClientStateMachine.this.mNativeInterface.stopVoiceRecognitionNative(HeadsetClientStateMachine.this.getByteAddress(HeadsetClientStateMachine.this.mCurrentDevice))) {
                                    HeadsetClientStateMachine.this.addQueuedAction(6);
                                } else {
                                    Log.e(HeadsetClientStateMachine.TAG, "ERROR: Couldn't stop voice recognition");
                                }
                            }
                            break;
                        case 7:
                            HeadsetClientStateMachine.this.mNativeInterface.setVolumeNative(HeadsetClientStateMachine.this.getByteAddress(HeadsetClientStateMachine.this.mCurrentDevice), 1, message.arg1);
                            break;
                        case 8:
                            int i2 = message.arg1;
                            int iAmToHfVol = HeadsetClientStateMachine.amToHfVol(i2);
                            if (i2 != this.mCommandedSpeakerVolume) {
                                Log.d(HeadsetClientStateMachine.TAG, "Volume" + i2 + ":" + this.mCommandedSpeakerVolume);
                                this.mCommandedSpeakerVolume = -1;
                                if (HeadsetClientStateMachine.this.mNativeInterface.setVolumeNative(HeadsetClientStateMachine.this.getByteAddress(HeadsetClientStateMachine.this.mCurrentDevice), 0, iAmToHfVol)) {
                                    HeadsetClientStateMachine.this.addQueuedAction(8);
                                }
                            }
                            break;
                        default:
                            switch (i) {
                                case 12:
                                    HeadsetClientStateMachine.this.acceptCall(message.arg1);
                                    break;
                                case 13:
                                    HeadsetClientStateMachine.this.rejectCall();
                                    break;
                                case 14:
                                    HeadsetClientStateMachine.this.holdCall();
                                    break;
                                case 15:
                                    HeadsetClientStateMachine.this.terminateCall();
                                    break;
                                case 16:
                                    HeadsetClientStateMachine.this.enterPrivateMode(message.arg1);
                                    break;
                                case 17:
                                    if (HeadsetClientStateMachine.this.mNativeInterface.sendDtmfNative(HeadsetClientStateMachine.this.getByteAddress(HeadsetClientStateMachine.this.mCurrentDevice), (byte) message.arg1)) {
                                        HeadsetClientStateMachine.this.addQueuedAction(17);
                                    } else {
                                        Log.e(HeadsetClientStateMachine.TAG, "ERROR: Couldn't send DTMF");
                                    }
                                    break;
                                case 18:
                                    HeadsetClientStateMachine.this.explicitCallTransfer();
                                    break;
                                default:
                                    return false;
                            }
                            break;
                    }
                } else {
                    StackEvent stackEvent = (StackEvent) message.obj;
                    switch (stackEvent.type) {
                        case 1:
                            processConnectionEvent(stackEvent.valueInt, stackEvent.device);
                            break;
                        case 2:
                            processAudioEvent(stackEvent.valueInt, stackEvent.device);
                            break;
                        case 3:
                            if (HeadsetClientStateMachine.this.mVoiceRecognitionActive != stackEvent.valueInt) {
                                HeadsetClientStateMachine.this.mVoiceRecognitionActive = stackEvent.valueInt;
                                Intent intent = new Intent("android.bluetooth.headsetclient.profile.action.AG_EVENT");
                                intent.putExtra("android.bluetooth.headsetclient.extra.VOICE_RECOGNITION", HeadsetClientStateMachine.this.mVoiceRecognitionActive);
                                intent.putExtra("android.bluetooth.device.extra.DEVICE", stackEvent.device);
                                HeadsetClientStateMachine.this.mService.sendBroadcast(intent, ProfileService.BLUETOOTH_PERM);
                            }
                            break;
                        case 4:
                            HeadsetClientStateMachine.this.mIndicatorNetworkState = stackEvent.valueInt;
                            Intent intent2 = new Intent("android.bluetooth.headsetclient.profile.action.AG_EVENT");
                            intent2.putExtra("android.bluetooth.headsetclient.extra.NETWORK_STATUS", stackEvent.valueInt);
                            if (HeadsetClientStateMachine.this.mIndicatorNetworkState == 0) {
                                HeadsetClientStateMachine.this.mOperatorName = null;
                                intent2.putExtra("android.bluetooth.headsetclient.extra.OPERATOR_NAME", HeadsetClientStateMachine.this.mOperatorName);
                            }
                            intent2.putExtra("android.bluetooth.device.extra.DEVICE", stackEvent.device);
                            HeadsetClientStateMachine.this.mService.sendBroadcast(intent2, ProfileService.BLUETOOTH_PERM);
                            if (HeadsetClientStateMachine.this.mIndicatorNetworkState == 1) {
                                if (HeadsetClientStateMachine.this.mNativeInterface.queryCurrentOperatorNameNative(HeadsetClientStateMachine.this.getByteAddress(HeadsetClientStateMachine.this.mCurrentDevice))) {
                                    HeadsetClientStateMachine.this.addQueuedAction(HeadsetClientStateMachine.QUERY_OPERATOR_NAME);
                                } else {
                                    Log.e(HeadsetClientStateMachine.TAG, "ERROR: Couldn't querry operator name");
                                }
                            }
                            break;
                        case 5:
                            HeadsetClientStateMachine.this.mIndicatorNetworkType = stackEvent.valueInt;
                            Intent intent3 = new Intent("android.bluetooth.headsetclient.profile.action.AG_EVENT");
                            intent3.putExtra("android.bluetooth.headsetclient.extra.NETWORK_ROAMING", stackEvent.valueInt);
                            intent3.putExtra("android.bluetooth.device.extra.DEVICE", stackEvent.device);
                            HeadsetClientStateMachine.this.mService.sendBroadcast(intent3, ProfileService.BLUETOOTH_PERM);
                            break;
                        case 6:
                            HeadsetClientStateMachine.this.mIndicatorNetworkSignal = stackEvent.valueInt;
                            Intent intent4 = new Intent("android.bluetooth.headsetclient.profile.action.AG_EVENT");
                            intent4.putExtra("android.bluetooth.headsetclient.extra.NETWORK_SIGNAL_STRENGTH", stackEvent.valueInt);
                            intent4.putExtra("android.bluetooth.device.extra.DEVICE", stackEvent.device);
                            HeadsetClientStateMachine.this.mService.sendBroadcast(intent4, ProfileService.BLUETOOTH_PERM);
                            break;
                        case 7:
                            HeadsetClientStateMachine.this.mIndicatorBatteryLevel = stackEvent.valueInt;
                            Intent intent5 = new Intent("android.bluetooth.headsetclient.profile.action.AG_EVENT");
                            intent5.putExtra("android.bluetooth.headsetclient.extra.BATTERY_LEVEL", stackEvent.valueInt);
                            intent5.putExtra("android.bluetooth.device.extra.DEVICE", stackEvent.device);
                            HeadsetClientStateMachine.this.mService.sendBroadcast(intent5, ProfileService.BLUETOOTH_PERM);
                            break;
                        case 8:
                            HeadsetClientStateMachine.this.mOperatorName = stackEvent.valueString;
                            Intent intent6 = new Intent("android.bluetooth.headsetclient.profile.action.AG_EVENT");
                            intent6.putExtra("android.bluetooth.headsetclient.extra.OPERATOR_NAME", stackEvent.valueString);
                            intent6.putExtra("android.bluetooth.device.extra.DEVICE", stackEvent.device);
                            HeadsetClientStateMachine.this.mService.sendBroadcast(intent6, ProfileService.BLUETOOTH_PERM);
                            break;
                        case 9:
                        case 11:
                        case 12:
                        case 13:
                        case 14:
                            HeadsetClientStateMachine.this.sendMessage(HeadsetClientStateMachine.QUERY_CURRENT_CALLS);
                            break;
                        case 10:
                            if (HeadsetClientStateMachine.this.mRingtone != null && HeadsetClientStateMachine.this.mRingtone.isPlaying()) {
                                Log.d(HeadsetClientStateMachine.TAG, "stopping ring after no response");
                                HeadsetClientStateMachine.this.mRingtone.stop();
                            }
                            HeadsetClientStateMachine.this.sendMessage(HeadsetClientStateMachine.QUERY_CURRENT_CALLS);
                            break;
                        case 15:
                            HeadsetClientStateMachine.this.queryCallsUpdate(stackEvent.valueInt, stackEvent.valueInt3, stackEvent.valueString, stackEvent.valueInt4 == 1, stackEvent.valueInt2 == 0);
                            if (HeadsetClientStateMachine.unexpected_call) {
                                Log.d(HeadsetClientStateMachine.TAG, "unexpected incoming call");
                                HeadsetClientStateMachine.this.queryCallsDone();
                                HeadsetClientStateMachine.unexpected_call = false;
                            }
                            break;
                        case 16:
                            if (stackEvent.valueInt == 0) {
                                this.mCommandedSpeakerVolume = HeadsetClientStateMachine.hfToAmVol(stackEvent.valueInt2);
                                Log.d(HeadsetClientStateMachine.TAG, "AM volume set to " + this.mCommandedSpeakerVolume);
                                HeadsetClientStateMachine.this.mAudioManager.setStreamVolume(0, this.mCommandedSpeakerVolume, 1);
                            } else if (stackEvent.valueInt == 1) {
                                HeadsetClientStateMachine.this.mAudioManager.setMicrophoneMute(stackEvent.valueInt2 == 0);
                            }
                            break;
                        case 17:
                            Pair pair = (Pair) HeadsetClientStateMachine.this.mQueuedActions.poll();
                            if (pair == null || ((Integer) pair.first).intValue() == 0) {
                                HeadsetClientStateMachine.this.clearPendingAction();
                                break;
                            } else {
                                int iIntValue = ((Integer) pair.first).intValue();
                                if (iIntValue == HeadsetClientStateMachine.QUERY_CURRENT_CALLS) {
                                    HeadsetClientStateMachine.this.queryCallsDone();
                                    break;
                                } else {
                                    switch (iIntValue) {
                                        case 5:
                                            if (stackEvent.valueInt == 0) {
                                                HeadsetClientStateMachine.this.mVoiceRecognitionActive = 1;
                                            }
                                            break;
                                        case 6:
                                            if (stackEvent.valueInt == 0) {
                                                HeadsetClientStateMachine.this.mVoiceRecognitionActive = 0;
                                            }
                                            break;
                                        default:
                                            Log.w(HeadsetClientStateMachine.TAG, "Unhandled AT OK " + stackEvent);
                                            break;
                                    }
                                }
                            }
                            break;
                        case 18:
                            HeadsetClientStateMachine.this.mSubscriberInfo = stackEvent.valueString;
                            Intent intent7 = new Intent("android.bluetooth.headsetclient.profile.action.AG_EVENT");
                            intent7.putExtra("android.bluetooth.headsetclient.extra.SUBSCRIBER_INFO", HeadsetClientStateMachine.this.mSubscriberInfo);
                            intent7.putExtra("android.bluetooth.device.extra.DEVICE", stackEvent.device);
                            HeadsetClientStateMachine.this.mService.sendBroadcast(intent7, ProfileService.BLUETOOTH_PERM);
                            break;
                        case 19:
                            Intent intent8 = new Intent("android.bluetooth.headsetclient.profile.action.AG_EVENT");
                            HeadsetClientStateMachine.this.mInBandRing = stackEvent.valueInt == 1;
                            intent8.putExtra("android.bluetooth.headsetclient.extra.IN_BAND_RING", stackEvent.valueInt);
                            intent8.putExtra("android.bluetooth.device.extra.DEVICE", stackEvent.device);
                            HeadsetClientStateMachine.this.mService.sendBroadcast(intent8, ProfileService.BLUETOOTH_PERM);
                            HeadsetClientStateMachine.this.mInBandRingtone = 1;
                            break;
                        case 20:
                        default:
                            Log.e(HeadsetClientStateMachine.TAG, "Unknown stack event: " + stackEvent.type);
                            break;
                        case 21:
                            Log.e(HeadsetClientStateMachine.TAG, "start ringing");
                            if (HeadsetClientStateMachine.this.mRingtone == null || !HeadsetClientStateMachine.this.mRingtone.isPlaying()) {
                                int mode = HeadsetClientStateMachine.this.mAudioManager.getMode();
                                if (mode != 1) {
                                    HeadsetClientStateMachine.this.mAudioManager.requestAudioFocusForCall(2, 2);
                                    Log.d(HeadsetClientStateMachine.TAG, "mAudioManager Setting audio mode from " + mode + " to 1");
                                    HeadsetClientStateMachine.this.mAudioManager.setMode(1);
                                }
                                if (HeadsetClientStateMachine.this.mInBandRingtone == 0 && HeadsetClientStateMachine.this.mRingtone != null) {
                                    Log.d(HeadsetClientStateMachine.TAG, "ring start playing");
                                    HeadsetClientStateMachine.this.mRingtone.play();
                                }
                            } else {
                                Log.d(HeadsetClientStateMachine.TAG, "ring already playing");
                            }
                            break;
                    }
                }
            } else if (HeadsetClientStateMachine.this.mNativeInterface.retrieveSubscriberInfoNative(HeadsetClientStateMachine.this.getByteAddress(HeadsetClientStateMachine.this.mCurrentDevice))) {
                HeadsetClientStateMachine.this.addQueuedAction(HeadsetClientStateMachine.SUBSCRIBER_INFO);
            } else {
                Log.e(HeadsetClientStateMachine.TAG, "ERROR: Couldn't retrieve subscriber info");
            }
            return true;
        }

        private void processConnectionEvent(int i, BluetoothDevice bluetoothDevice) {
            if (i == 0) {
                if (HeadsetClientStateMachine.this.mCurrentDevice.equals(bluetoothDevice)) {
                    HeadsetClientStateMachine.this.transitionTo(HeadsetClientStateMachine.this.mDisconnected);
                    return;
                }
                Log.e(HeadsetClientStateMachine.TAG, "Disconnected from unknown device: " + bluetoothDevice);
                return;
            }
            Log.e(HeadsetClientStateMachine.TAG, "Connection State Device: " + bluetoothDevice + " bad state: " + i);
        }

        private void processAudioEvent(int i, BluetoothDevice bluetoothDevice) {
            if (HeadsetClientStateMachine.this.mCurrentDevice.equals(bluetoothDevice)) {
                switch (i) {
                    case 0:
                        HeadsetClientStateMachine.this.broadcastAudioState(bluetoothDevice, 0, HeadsetClientStateMachine.this.mAudioState);
                        HeadsetClientStateMachine.this.mAudioState = 0;
                        return;
                    case 1:
                        HeadsetClientStateMachine.this.broadcastAudioState(bluetoothDevice, 1, HeadsetClientStateMachine.this.mAudioState);
                        HeadsetClientStateMachine.this.mAudioState = 1;
                        return;
                    case 2:
                        break;
                    case 3:
                        HeadsetClientStateMachine.this.mAudioWbs = true;
                        break;
                    default:
                        Log.e(HeadsetClientStateMachine.TAG, "Audio State Device: " + bluetoothDevice + " bad state: " + i);
                        return;
                }
                if (!HeadsetClientStateMachine.this.mService.isScoRouted()) {
                    if (HeadsetClientStateMachine.this.mRingtone != null && HeadsetClientStateMachine.this.mRingtone.isPlaying()) {
                        Log.d(HeadsetClientStateMachine.TAG, "stopping ring and request focus for call");
                        HeadsetClientStateMachine.this.mRingtone.stop();
                    }
                    int mode = HeadsetClientStateMachine.this.mAudioManager.getMode();
                    if (mode != 2) {
                        HeadsetClientStateMachine.this.mAudioManager.requestAudioFocusForCall(0, 2);
                        Log.d(HeadsetClientStateMachine.TAG, "setAudioMode Setting audio mode from " + mode + " to 2");
                        HeadsetClientStateMachine.this.mAudioManager.setMode(2);
                    }
                    HeadsetClientStateMachine.this.mAudioState = 2;
                    int iAmToHfVol = HeadsetClientStateMachine.amToHfVol(HeadsetClientStateMachine.this.mAudioManager.getStreamVolume(0));
                    if (HeadsetClientStateMachine.this.mAudioWbs) {
                        HeadsetClientStateMachine.this.mAudioManager.setParameters("hfp_set_sampling_rate=16000");
                    } else {
                        HeadsetClientStateMachine.this.mAudioManager.setParameters("hfp_set_sampling_rate=8000");
                    }
                    HeadsetClientStateMachine.this.routeHfpAudio(true);
                    HeadsetClientStateMachine.this.mAudioFocusRequest = HeadsetClientStateMachine.this.requestAudioFocus();
                    HeadsetClientStateMachine.this.mAudioManager.setParameters("hfp_volume=" + iAmToHfVol);
                    HeadsetClientStateMachine.this.transitionTo(HeadsetClientStateMachine.this.mAudioOn);
                    return;
                }
                StackEvent stackEvent = new StackEvent(2);
                stackEvent.valueInt = i;
                stackEvent.device = bluetoothDevice;
                HeadsetClientStateMachine.this.sendMessageDelayed(100, stackEvent, 250L);
                return;
            }
            Log.e(HeadsetClientStateMachine.TAG, "Audio changed on disconnected device: " + bluetoothDevice);
        }

        public void exit() {
            HeadsetClientStateMachine.this.mPrevState = this;
        }
    }

    class AudioOn extends State {
        AudioOn() {
        }

        public void enter() {
            HeadsetClientStateMachine.this.broadcastAudioState(HeadsetClientStateMachine.this.mCurrentDevice, 2, 1);
        }

        public synchronized boolean processMessage(Message message) {
            int i = message.what;
            if (i == 2) {
                if (HeadsetClientStateMachine.this.mCurrentDevice.equals((BluetoothDevice) message.obj)) {
                    HeadsetClientStateMachine.this.deferMessage(message);
                    if (HeadsetClientStateMachine.this.mNativeInterface.disconnectAudioNative(HeadsetClientStateMachine.this.getByteAddress(HeadsetClientStateMachine.this.mCurrentDevice))) {
                    }
                }
            } else if (i != 4) {
                if (i == 14) {
                    HeadsetClientStateMachine.this.holdCall();
                } else {
                    if (i != 100) {
                        return false;
                    }
                    StackEvent stackEvent = (StackEvent) message.obj;
                    switch (stackEvent.type) {
                        case 1:
                            processConnectionEvent(stackEvent.valueInt, stackEvent.device);
                            break;
                        case 2:
                            processAudioEvent(stackEvent.valueInt, stackEvent.device);
                            break;
                        default:
                            return false;
                    }
                }
            } else if (HeadsetClientStateMachine.this.mNativeInterface.disconnectAudioNative(HeadsetClientStateMachine.this.getByteAddress(HeadsetClientStateMachine.this.mCurrentDevice))) {
                HeadsetClientStateMachine.this.routeHfpAudio(false);
                HeadsetClientStateMachine.this.returnAudioFocusIfNecessary();
            }
            return true;
        }

        private void processConnectionEvent(int i, BluetoothDevice bluetoothDevice) {
            if (i == 0) {
                if (HeadsetClientStateMachine.this.mCurrentDevice.equals(bluetoothDevice)) {
                    processAudioEvent(0, bluetoothDevice);
                    HeadsetClientStateMachine.this.transitionTo(HeadsetClientStateMachine.this.mDisconnected);
                    return;
                } else {
                    Log.e(HeadsetClientStateMachine.TAG, "Disconnected from unknown device: " + bluetoothDevice);
                    return;
                }
            }
            Log.e(HeadsetClientStateMachine.TAG, "Connection State Device: " + bluetoothDevice + " bad state: " + i);
        }

        private void processAudioEvent(int i, BluetoothDevice bluetoothDevice) {
            if (!HeadsetClientStateMachine.this.mCurrentDevice.equals(bluetoothDevice)) {
                Log.e(HeadsetClientStateMachine.TAG, "Audio changed on disconnected device: " + bluetoothDevice);
                return;
            }
            if (i == 0) {
                HeadsetClientStateMachine.this.removeMessages(4);
                HeadsetClientStateMachine.this.mAudioState = 0;
                HeadsetClientStateMachine.this.routeHfpAudio(false);
                HeadsetClientStateMachine.this.returnAudioFocusIfNecessary();
                HeadsetClientStateMachine.this.transitionTo(HeadsetClientStateMachine.this.mConnected);
                return;
            }
            Log.e(HeadsetClientStateMachine.TAG, "Audio State Device: " + bluetoothDevice + " bad state: " + i);
        }

        public void exit() {
            HeadsetClientStateMachine.this.mPrevState = this;
            HeadsetClientStateMachine.this.broadcastAudioState(HeadsetClientStateMachine.this.mCurrentDevice, 0, 2);
        }
    }

    public synchronized int getConnectionState(BluetoothDevice bluetoothDevice) {
        if (this.mCurrentDevice == null) {
            return 0;
        }
        if (!this.mCurrentDevice.equals(bluetoothDevice)) {
            return 0;
        }
        Connecting currentState = getCurrentState();
        if (currentState == this.mConnecting) {
            return 1;
        }
        if (currentState != this.mConnected && currentState != this.mAudioOn) {
            Log.e(TAG, "Bad currentState: " + currentState);
            return 0;
        }
        return 2;
    }

    private void broadcastAudioState(BluetoothDevice bluetoothDevice, int i, int i2) {
        Intent intent = new Intent("android.bluetooth.headsetclient.profile.action.AUDIO_STATE_CHANGED");
        intent.putExtra("android.bluetooth.profile.extra.PREVIOUS_STATE", i2);
        intent.putExtra("android.bluetooth.profile.extra.STATE", i);
        if (i == 2) {
            intent.putExtra("android.bluetooth.headsetclient.extra.AUDIO_WBS", this.mAudioWbs);
        }
        intent.putExtra("android.bluetooth.device.extra.DEVICE", bluetoothDevice);
        this.mService.sendBroadcast(intent, ProfileService.BLUETOOTH_PERM);
    }

    private void broadcastConnectionState(BluetoothDevice bluetoothDevice, int i, int i2) {
        Intent intent = new Intent("android.bluetooth.headsetclient.profile.action.CONNECTION_STATE_CHANGED");
        intent.putExtra("android.bluetooth.profile.extra.PREVIOUS_STATE", i2);
        intent.putExtra("android.bluetooth.profile.extra.STATE", i);
        intent.putExtra("android.bluetooth.device.extra.DEVICE", bluetoothDevice);
        if (i == 2) {
            if ((this.mPeerFeatures & 1) == 1) {
                intent.putExtra("android.bluetooth.headsetclient.extra.EXTRA_AG_FEATURE_3WAY_CALLING", true);
            }
            if ((this.mPeerFeatures & 32) == 32) {
                intent.putExtra("android.bluetooth.headsetclient.extra.EXTRA_AG_FEATURE_REJECT_CALL", true);
            }
            if ((this.mPeerFeatures & 128) == 128) {
                intent.putExtra("android.bluetooth.headsetclient.extra.EXTRA_AG_FEATURE_ECC", true);
            }
            if ((this.mChldFeatures & 8) == 8) {
                intent.putExtra("android.bluetooth.headsetclient.extra.EXTRA_AG_FEATURE_ACCEPT_HELD_OR_WAITING_CALL", true);
            }
            if ((this.mChldFeatures & 1) == 1) {
                intent.putExtra("android.bluetooth.headsetclient.extra.EXTRA_AG_FEATURE_RELEASE_HELD_OR_WAITING_CALL", true);
            }
            if ((this.mChldFeatures & 2) == 2) {
                intent.putExtra("android.bluetooth.headsetclient.extra.EXTRA_AG_FEATURE_RELEASE_AND_ACCEPT", true);
            }
            if ((this.mChldFeatures & 32) == 32) {
                intent.putExtra("android.bluetooth.headsetclient.extra.EXTRA_AG_FEATURE_MERGE", true);
            }
            if ((this.mChldFeatures & 64) == 64) {
                intent.putExtra("android.bluetooth.headsetclient.extra.EXTRA_AG_FEATURE_MERGE_AND_DETACH", true);
            }
        }
        this.mService.sendBroadcast(intent, ProfileService.BLUETOOTH_PERM);
    }

    boolean isConnected() {
        Connected currentState = getCurrentState();
        return currentState == this.mConnected || currentState == this.mAudioOn;
    }

    List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] iArr) {
        ArrayList arrayList = new ArrayList();
        Set<BluetoothDevice> bondedDevices = this.mAdapter.getBondedDevices();
        synchronized (this) {
            for (BluetoothDevice bluetoothDevice : bondedDevices) {
                if (BluetoothUuid.isUuidPresent(bluetoothDevice.getUuids(), BluetoothUuid.Handsfree_AG)) {
                    int connectionState = getConnectionState(bluetoothDevice);
                    for (int i : iArr) {
                        if (connectionState == i) {
                            arrayList.add(bluetoothDevice);
                        }
                    }
                }
            }
        }
        return arrayList;
    }

    boolean okToConnect(BluetoothDevice bluetoothDevice) {
        int priority = this.mService.getPriority(bluetoothDevice);
        if (priority > 0 || (-1 == priority && bluetoothDevice.getBondState() != 10)) {
            return true;
        }
        return false;
    }

    boolean isAudioOn() {
        return getCurrentState() == this.mAudioOn;
    }

    synchronized int getAudioState(BluetoothDevice bluetoothDevice) {
        if (this.mCurrentDevice != null && this.mCurrentDevice.equals(bluetoothDevice)) {
            return this.mAudioState;
        }
        return 0;
    }

    List<BluetoothDevice> getConnectedDevices() {
        ArrayList arrayList = new ArrayList();
        synchronized (this) {
            if (isConnected()) {
                arrayList.add(this.mCurrentDevice);
            }
        }
        return arrayList;
    }

    private byte[] getByteAddress(BluetoothDevice bluetoothDevice) {
        return Utils.getBytesFromAddress(bluetoothDevice.getAddress());
    }

    public List<BluetoothHeadsetClientCall> getCurrentCalls() {
        return new ArrayList(this.mCalls.values());
    }

    public Bundle getCurrentAgEvents() {
        Bundle bundle = new Bundle();
        bundle.putInt("android.bluetooth.headsetclient.extra.NETWORK_STATUS", this.mIndicatorNetworkState);
        bundle.putInt("android.bluetooth.headsetclient.extra.NETWORK_SIGNAL_STRENGTH", this.mIndicatorNetworkSignal);
        bundle.putInt("android.bluetooth.headsetclient.extra.NETWORK_ROAMING", this.mIndicatorNetworkType);
        bundle.putInt("android.bluetooth.headsetclient.extra.BATTERY_LEVEL", this.mIndicatorBatteryLevel);
        bundle.putString("android.bluetooth.headsetclient.extra.OPERATOR_NAME", this.mOperatorName);
        bundle.putString("android.bluetooth.headsetclient.extra.SUBSCRIBER_INFO", this.mSubscriberInfo);
        return bundle;
    }
}
