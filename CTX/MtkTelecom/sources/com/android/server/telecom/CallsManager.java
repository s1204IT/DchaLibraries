package com.android.server.telecom;

import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.UserInfo;
import android.media.AudioManager;
import android.media.AudioSystem;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PersistableBundle;
import android.os.Process;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.SystemVibrator;
import android.os.Trace;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.BlockedNumberContract;
import android.provider.Settings;
import android.telecom.CallAudioState;
import android.telecom.DisconnectCause;
import android.telecom.GatewayInfo;
import android.telecom.Log;
import android.telecom.Logging.Runnable;
import android.telecom.ParcelableConference;
import android.telecom.ParcelableConnection;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.VideoProfile;
import android.telephony.CarrierConfigManager;
import android.telephony.PhoneNumberUtils;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.AsyncEmergencyContactNotifier;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.telecom.Call;
import com.android.server.telecom.CallAudioManager;
import com.android.server.telecom.CallsManager;
import com.android.server.telecom.ConnectionServiceFocusManager;
import com.android.server.telecom.DtmfLocalTonePlayer;
import com.android.server.telecom.InCallTonePlayer;
import com.android.server.telecom.MissedCallNotifier;
import com.android.server.telecom.ParcelableCallUtils;
import com.android.server.telecom.PhoneAccountRegistrar;
import com.android.server.telecom.TelecomSystem;
import com.android.server.telecom.Timeouts;
import com.android.server.telecom.VideoProviderProxy;
import com.android.server.telecom.bluetooth.BluetoothRouteManager;
import com.android.server.telecom.bluetooth.BluetoothStateReceiver;
import com.android.server.telecom.callfiltering.AsyncBlockCheckFilter;
import com.android.server.telecom.callfiltering.BlockCheckerAdapter;
import com.android.server.telecom.callfiltering.CallFilterResultCallback;
import com.android.server.telecom.callfiltering.CallFilteringResult;
import com.android.server.telecom.callfiltering.CallScreeningServiceFilter;
import com.android.server.telecom.callfiltering.DirectToVoicemailCallFilter;
import com.android.server.telecom.callfiltering.IncomingCallFilter;
import com.android.server.telecom.components.ErrorDialogActivity;
import com.android.server.telecom.settings.BlockedNumbersUtil;
import com.android.server.telecom.ui.ConfirmCallDialogActivity;
import com.android.server.telecom.ui.IncomingCallNotifier;
import com.mediatek.server.telecom.CallConnectedVibrator;
import com.mediatek.server.telecom.CallRecorderManager;
import com.mediatek.server.telecom.MtkTelecomGlobals;
import com.mediatek.server.telecom.MtkUtil;
import com.mediatek.server.telecom.ext.ExtensionManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@VisibleForTesting
public class CallsManager extends Call.ListenerBase implements CurrentUserProxy, VideoProviderProxy.Listener, CallFilterResultCallback {
    public static final int[] ONGOING_CALL_STATES_EX;
    private static final Map<Integer, Integer> mVoiceRoamingInfoMap;
    private final BluetoothRouteManager mBluetoothRouteManager;
    private final CallAudioManager mCallAudioManager;
    private final CallLogManager mCallLogManager;
    private final CallRecorderManager mCallRecorderManager;
    private final CallRecordingTonePlayer mCallRecordingTonePlayer;
    private final CallerInfoAsyncQueryFactory mCallerInfoAsyncQueryFactory;
    private final CallerInfoLookupHelper mCallerInfoLookupHelper;
    private final ClockProxy mClockProxy;
    private final ConnectionServiceRepository mConnectionServiceRepository;
    private final ConnectionServiceFocusManager mConnectionSvrFocusMgr;
    private final ContactsAsyncHelper mContactsAsyncHelper;
    private final Context mContext;
    private final DefaultDialerCache mDefaultDialerCache;
    private final DockManager mDockManager;
    private final DtmfLocalTonePlayer mDtmfLocalTonePlayer;
    private final EmergencyCallHelper mEmergencyCallHelper;
    private Call mForegroundIncomingCall;
    private final HeadsetMediaButton mHeadsetMediaButton;
    private final InCallController mInCallController;
    private final InCallWakeLockController mInCallWakeLockController;
    private IncomingCallNotifier mIncomingCallNotifier;
    private final TelecomSystem.SyncRoot mLock;
    private final MissedCallNotifier mMissedCallNotifier;
    private boolean mNeedDisconnectLater;
    private Call mPendingCall;
    private Call mPendingEccCall;
    private final PhoneAccountRegistrar mPhoneAccountRegistrar;
    private final PhoneNumberUtilsAdapter mPhoneNumberUtilsAdapter;
    private final PhoneStateBroadcaster mPhoneStateBroadcaster;
    private final ProximitySensorManager mProximitySensorManager;
    private RespondViaSmsManager mRespondViaSmsManager;
    private final Ringer mRinger;
    private Runnable mStopTone;
    private final Timeouts.Adapter mTimeoutsAdapter;
    private final TtyManager mTtyManager;
    private final WiredHeadsetManager mWiredHeadsetManager;
    private static final int[] OUTGOING_CALL_STATES = {1, 2, 3, 10};
    private static final int[] LIVE_CALL_STATES = {1, 2, 3, 10, 5};
    public static final int[] ONGOING_CALL_STATES = {2, 3, 10, 5, 6, 4};
    private static final int[] ANY_CALL_STATE = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
    private static final Map<Integer, Integer> sAnalyticsTechnologyMap = new HashMap(5);
    private final Set<Call> mCalls = Collections.newSetFromMap(new ConcurrentHashMap(8, 0.9f, 1));
    private int mCallId = 0;
    private int mRttRequestId = 0;
    private UserHandle mCurrentUserHandle = UserHandle.of(ActivityManager.getCurrentUser());
    private final Set<CallsManagerListener> mListeners = Collections.newSetFromMap(new ConcurrentHashMap(16, 0.9f, 1));
    private final Set<Call> mLocallyDisconnectingCalls = new HashSet();
    private final Set<Call> mPendingCallsToDisconnect = new HashSet();
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final ConnectionServiceFocusManager.CallsManagerRequester mRequester = new AnonymousClass1();
    private boolean mCanAddCall = true;
    private TelephonyManager.MultiSimVariants mRadioSimVariants = null;
    private PhoneAccountRegistrar.Listener mPhoneAccountListener = new PhoneAccountRegistrar.Listener() {
        @Override
        public void onPhoneAccountRegistered(PhoneAccountRegistrar phoneAccountRegistrar, PhoneAccountHandle phoneAccountHandle) {
            CallsManager.this.broadcastRegisterIntent(phoneAccountHandle);
        }

        @Override
        public void onPhoneAccountUnRegistered(PhoneAccountRegistrar phoneAccountRegistrar, PhoneAccountHandle phoneAccountHandle) {
            CallsManager.this.broadcastUnregisterIntent(phoneAccountHandle);
        }
    };
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.telephony.action.CARRIER_CONFIG_CHANGED".equals(action) || "android.provider.action.BLOCK_SUPPRESSION_STATE_CHANGED".equals(action)) {
                BlockedNumbersUtil.updateEmergencyCallNotification(context, BlockedNumberContract.SystemContract.shouldShowEmergencyCallNotification(context));
            }
        }
    };
    private boolean mCanMakeCall = false;
    private final Map<Call, WaitingCallAction> mWaitingCallActions = new HashMap();
    private boolean mIsCallRecorderSupported = true;
    private BroadcastReceiver mReceiverForDsda = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Call backgroundIncomingCall;
            if (MtkUtil.isInDsdaMode() && intent.getAction().equals("android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED") && intent.getIntExtra("android.bluetooth.profile.extra.STATE", -1) == 2 && (backgroundIncomingCall = CallsManager.this.getBackgroundIncomingCall()) != null) {
                Log.i("CallsManager", "[onReceive] reject background incoming call: %s", new Object[]{backgroundIncomingCall.getId()});
                CallsManager.this.rejectCall(backgroundIncomingCall, false, null);
            }
        }
    };
    private BroadcastReceiver mReceiverRoaming = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            intent.getAction();
            if (intent.getAction().equals("android.intent.action.SERVICE_STATE")) {
                int intExtra = intent.getIntExtra("subscription", -1);
                ServiceState serviceStateNewFromBundle = ServiceState.newFromBundle(intent.getExtras());
                if (intExtra != -1) {
                    CallsManager.mVoiceRoamingInfoMap.put(Integer.valueOf(intExtra), Integer.valueOf(serviceStateNewFromBundle.getVoiceRoamingType()));
                    Log.d("CallsManager", "broadcast receiver onReceive roaming value = " + serviceStateNewFromBundle.getVoiceRoamingType() + "subid=" + intExtra, new Object[0]);
                    return;
                }
                return;
            }
            if (intent.getAction().equals("com.mediatek.telecom.plugin.MAKE_CALL")) {
                CallsManager.this.mCanMakeCall = true;
            }
        }
    };

    @VisibleForTesting
    public interface CallsManagerListener {
        void onCallAdded(Call call);

        void onCallAlertingNotified(Call call);

        void onCallAudioStateChanged(CallAudioState callAudioState, CallAudioState callAudioState2);

        void onCallRemoved(Call call);

        void onCallStateChanged(Call call, int i, int i2);

        void onCanAddCallChanged(boolean z);

        void onConnectionServiceChanged(Call call, ConnectionServiceWrapper connectionServiceWrapper, ConnectionServiceWrapper connectionServiceWrapper2);

        void onDisconnectedTonePlaying(boolean z);

        void onExternalCallChanged(Call call, boolean z);

        void onHoldToneRequested(Call call);

        void onIncomingCallAnswered(Call call);

        void onIncomingCallRejected(Call call, boolean z, String str);

        void onIsConferencedChanged(Call call);

        void onIsVoipAudioModeChanged(Call call);

        void onRingbackRequested(Call call, boolean z);

        void onSessionModifyRequestReceived(Call call, VideoProfile videoProfile);

        void onVideoStateChanged(Call call, int i, int i2);
    }

    interface PendingAction {
        void performAction();
    }

    static {
        sAnalyticsTechnologyMap.put(2, 1);
        sAnalyticsTechnologyMap.put(1, 2);
        sAnalyticsTechnologyMap.put(5, 4);
        sAnalyticsTechnologyMap.put(3, 8);
        sAnalyticsTechnologyMap.put(4, 16);
        ONGOING_CALL_STATES_EX = new int[]{2, 3, 10, 5, 6, 4, 1};
        mVoiceRoamingInfoMap = new ConcurrentHashMap();
    }

    class AnonymousClass1 implements ConnectionServiceFocusManager.CallsManagerRequester {
        AnonymousClass1() {
        }

        @Override
        public void releaseConnectionService(final ConnectionServiceFocusManager.ConnectionServiceFocus connectionServiceFocus) {
            CallsManager.this.mCalls.stream().filter(new Predicate() {
                @Override
                public final boolean test(Object obj) {
                    return CallsManager.AnonymousClass1.lambda$releaseConnectionService$0(connectionServiceFocus, (Call) obj);
                }
            }).forEach(new Consumer() {
                @Override
                public final void accept(Object obj) {
                    ((Call) obj).disconnect("release " + connectionServiceFocus.getComponentName().getPackageName());
                }
            });
        }

        static boolean lambda$releaseConnectionService$0(ConnectionServiceFocusManager.ConnectionServiceFocus connectionServiceFocus, Call call) {
            return call.getConnectionServiceWrapper() != null && call.getConnectionServiceWrapper().equals(connectionServiceFocus);
        }

        @Override
        public void setCallsManagerListener(CallsManagerListener callsManagerListener) {
            CallsManager.this.mListeners.add(callsManagerListener);
        }
    }

    @VisibleForTesting
    public CallsManager(Context context, TelecomSystem.SyncRoot syncRoot, ContactsAsyncHelper contactsAsyncHelper, CallerInfoAsyncQueryFactory callerInfoAsyncQueryFactory, MissedCallNotifier missedCallNotifier, PhoneAccountRegistrar phoneAccountRegistrar, HeadsetMediaButtonFactory headsetMediaButtonFactory, ProximitySensorManagerFactory proximitySensorManagerFactory, InCallWakeLockControllerFactory inCallWakeLockControllerFactory, ConnectionServiceFocusManager.ConnectionServiceFocusManagerFactory connectionServiceFocusManagerFactory, CallAudioManager.AudioServiceFactory audioServiceFactory, BluetoothRouteManager bluetoothRouteManager, WiredHeadsetManager wiredHeadsetManager, SystemStateProvider systemStateProvider, DefaultDialerCache defaultDialerCache, Timeouts.Adapter adapter, AsyncRingtonePlayer asyncRingtonePlayer, PhoneNumberUtilsAdapter phoneNumberUtilsAdapter, EmergencyCallHelper emergencyCallHelper, InCallTonePlayer.ToneGeneratorFactory toneGeneratorFactory, ClockProxy clockProxy, BluetoothStateReceiver bluetoothStateReceiver, InCallControllerFactory inCallControllerFactory) {
        this.mContext = context;
        this.mLock = syncRoot;
        this.mPhoneNumberUtilsAdapter = phoneNumberUtilsAdapter;
        this.mContactsAsyncHelper = contactsAsyncHelper;
        this.mCallerInfoAsyncQueryFactory = callerInfoAsyncQueryFactory;
        this.mPhoneAccountRegistrar = phoneAccountRegistrar;
        this.mPhoneAccountRegistrar.addListener(this.mPhoneAccountListener);
        this.mMissedCallNotifier = missedCallNotifier;
        StatusBarNotifier statusBarNotifier = new StatusBarNotifier(context, this);
        this.mWiredHeadsetManager = wiredHeadsetManager;
        this.mDefaultDialerCache = defaultDialerCache;
        this.mBluetoothRouteManager = bluetoothRouteManager;
        this.mDockManager = new DockManager(context);
        this.mTimeoutsAdapter = adapter;
        this.mEmergencyCallHelper = emergencyCallHelper;
        this.mCallerInfoLookupHelper = new CallerInfoLookupHelper(context, this.mCallerInfoAsyncQueryFactory, this.mContactsAsyncHelper, this.mLock);
        this.mDtmfLocalTonePlayer = new DtmfLocalTonePlayer(new DtmfLocalTonePlayer.ToneGeneratorProxy());
        CallAudioRouteStateMachine callAudioRouteStateMachine = new CallAudioRouteStateMachine(context, this, bluetoothRouteManager, wiredHeadsetManager, statusBarNotifier, audioServiceFactory, 2);
        callAudioRouteStateMachine.initialize();
        InCallTonePlayer.Factory factory = new InCallTonePlayer.Factory(new CallAudioRoutePeripheralAdapter(callAudioRouteStateMachine, bluetoothRouteManager, wiredHeadsetManager, this.mDockManager), syncRoot, toneGeneratorFactory);
        SystemSettingsUtil systemSettingsUtil = new SystemSettingsUtil();
        RingtoneFactory ringtoneFactory = new RingtoneFactory(this, context);
        SystemVibrator systemVibrator = new SystemVibrator(context);
        this.mInCallController = inCallControllerFactory.create(context, this.mLock, this, systemStateProvider, defaultDialerCache, this.mTimeoutsAdapter, emergencyCallHelper);
        this.mRinger = new Ringer(factory, context, systemSettingsUtil, asyncRingtonePlayer, ringtoneFactory, systemVibrator, this.mInCallController);
        this.mCallRecordingTonePlayer = new CallRecordingTonePlayer(this.mContext, (AudioManager) this.mContext.getSystemService("audio"), this.mLock);
        this.mCallAudioManager = new CallAudioManager(callAudioRouteStateMachine, this, new CallAudioModeStateMachine((AudioManager) this.mContext.getSystemService("audio")), factory, this.mRinger, new RingbackPlayer(factory), bluetoothStateReceiver, this.mDtmfLocalTonePlayer);
        this.mConnectionSvrFocusMgr = connectionServiceFocusManagerFactory.create(this.mRequester, Looper.getMainLooper());
        this.mHeadsetMediaButton = headsetMediaButtonFactory.create(context, this, this.mLock);
        this.mTtyManager = new TtyManager(context, this.mWiredHeadsetManager);
        this.mProximitySensorManager = proximitySensorManagerFactory.create(context, this);
        this.mPhoneStateBroadcaster = new PhoneStateBroadcaster(this);
        this.mCallLogManager = new CallLogManager(context, phoneAccountRegistrar, this.mMissedCallNotifier);
        this.mConnectionServiceRepository = new ConnectionServiceRepository(this.mPhoneAccountRegistrar, this.mContext, this.mLock, this);
        this.mInCallWakeLockController = inCallWakeLockControllerFactory.create(context, this);
        this.mClockProxy = clockProxy;
        this.mListeners.add(this.mInCallWakeLockController);
        this.mListeners.add(statusBarNotifier);
        this.mListeners.add(this.mCallLogManager);
        this.mListeners.add(this.mPhoneStateBroadcaster);
        this.mListeners.add(this.mInCallController);
        this.mListeners.add(this.mCallAudioManager);
        this.mListeners.add(this.mCallRecordingTonePlayer);
        this.mListeners.add(missedCallNotifier);
        this.mListeners.add(this.mHeadsetMediaButton);
        this.mListeners.add(this.mProximitySensorManager);
        this.mListeners.add(new CallConnectedVibrator(this.mContext));
        if (UserManager.get(this.mContext).isPrimaryUser()) {
            onUserSwitch(Process.myUserHandle());
        }
        if (MtkUtil.isInDsdaMode()) {
            this.mContext.registerReceiver(this.mReceiverForDsda, new IntentFilter("android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED"));
        }
        this.mCallRecorderManager = new CallRecorderManager(this.mContext, this);
        IntentFilter intentFilter = new IntentFilter("android.telephony.action.CARRIER_CONFIG_CHANGED");
        intentFilter.addAction("android.provider.action.BLOCK_SUPPRESSION_STATE_CHANGED");
        context.registerReceiver(this.mReceiver, intentFilter);
        ExtensionManager.getRttUtilExt().makeRttAudioController(this.mContext, this);
        IntentFilter intentFilter2 = new IntentFilter();
        intentFilter2.addAction("android.intent.action.SERVICE_STATE");
        intentFilter2.addAction("com.mediatek.telecom.plugin.MAKE_CALL");
        this.mContext.registerReceiver(this.mReceiverRoaming, intentFilter2);
    }

    public void setIncomingCallNotifier(IncomingCallNotifier incomingCallNotifier) {
        if (this.mIncomingCallNotifier != null) {
            this.mListeners.remove(this.mIncomingCallNotifier);
        }
        this.mIncomingCallNotifier = incomingCallNotifier;
        this.mListeners.add(this.mIncomingCallNotifier);
    }

    public void setRespondViaSmsManager(RespondViaSmsManager respondViaSmsManager) {
        if (this.mRespondViaSmsManager != null) {
            this.mListeners.remove(this.mRespondViaSmsManager);
        }
        this.mRespondViaSmsManager = respondViaSmsManager;
        this.mListeners.add(respondViaSmsManager);
    }

    public RespondViaSmsManager getRespondViaSmsManager() {
        return this.mRespondViaSmsManager;
    }

    public CallerInfoLookupHelper getCallerInfoLookupHelper() {
        return this.mCallerInfoLookupHelper;
    }

    @Override
    public void onSuccessfulOutgoingCall(Call call, int i) {
        Log.v(this, "onSuccessfulOutgoingCall, %s", new Object[]{call});
        setCallState(call, i, "successful outgoing call");
        if (!this.mCalls.contains(call)) {
            addCall(call);
        }
        Iterator<CallsManagerListener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onConnectionServiceChanged(call, null, call.getConnectionService());
        }
        if (!call.isEmergencyCall()) {
            markCallAsDialing(call);
        }
    }

    @Override
    public void onFailedOutgoingCall(Call call, DisconnectCause disconnectCause) {
        Log.v(this, "onFailedOutgoingCall, call: %s", new Object[]{call});
        markCallAsRemoved(call);
    }

    @Override
    public void onSuccessfulIncomingCall(Call call) {
        Log.d(this, "onSuccessfulIncomingCall", new Object[0]);
        if (call.hasProperty(1)) {
            Log.i(this, "Skipping call filtering due to ECBM", new Object[0]);
            onCallFilteringComplete(call, new CallFilteringResult(true, false, true, true));
            return;
        }
        ArrayList arrayList = new ArrayList();
        arrayList.add(new DirectToVoicemailCallFilter(this.mCallerInfoLookupHelper));
        arrayList.add(new AsyncBlockCheckFilter(this.mContext, new BlockCheckerAdapter(), this.mCallerInfoLookupHelper));
        arrayList.add(new CallScreeningServiceFilter(this.mContext, this, this.mPhoneAccountRegistrar, this.mDefaultDialerCache, new ParcelableCallUtils.Converter(), this.mLock));
        new IncomingCallFilter(this.mContext, this, call, this.mLock, this.mTimeoutsAdapter, arrayList).performFiltering();
    }

    @Override
    public void onCallFilteringComplete(Call call, CallFilteringResult callFilteringResult) {
        if (call.getState() != 7 && call.getState() != 9) {
            if (call.getState() != 5) {
                setCallState(call, 4, callFilteringResult.shouldAllowCall ? "successful incoming call" : "blocking call");
            }
            if (callFilteringResult.shouldAllowCall) {
                if (isIncomingCallAndEccInDiffPhoneAccount(call) && !MtkUtil.isInDsdaMode()) {
                    Log.i(this, "onCallFilteringCompleted: ECC in another PhoneAccount, reject incomingCall", new Object[0]);
                    rejectCallAndLog(call);
                    return;
                }
                if (MtkUtil.isInSingleVideoCallMode(call) && shouldBlockCallUnderSingleVideo(call)) {
                    Log.i("CallsManager", "[isIncomingCallPermitted] Already has video call. or other call in the same phone account with differ video state", new Object[0]);
                    rejectCallAndLog(call);
                    return;
                } else {
                    if (hasMaximumManagedRingingCalls(call)) {
                        if (shouldSilenceInsteadOfReject(call)) {
                            call.silence();
                            return;
                        } else {
                            Log.i(this, "onCallFilteringCompleted: Call rejected! Exceeds maximum number of ringing calls.", new Object[0]);
                            rejectCallAndLog(call);
                            return;
                        }
                    }
                    addCall(call);
                    return;
                }
            }
            if (callFilteringResult.shouldReject) {
                Log.i(this, "onCallFilteringCompleted: blocked call, rejecting.", new Object[0]);
                call.reject(false, null);
            }
            if (callFilteringResult.shouldAddToCallLog) {
                Log.i(this, "onCallScreeningCompleted: blocked call, adding to call log.", new Object[0]);
                if (callFilteringResult.shouldShowNotification) {
                    Log.w(this, "onCallScreeningCompleted: blocked call, showing notification.", new Object[0]);
                }
                this.mCallLogManager.logCall(call, 3, callFilteringResult.shouldShowNotification);
                return;
            }
            if (callFilteringResult.shouldShowNotification) {
                Log.i(this, "onCallScreeningCompleted: blocked call, showing notification.", new Object[0]);
                this.mMissedCallNotifier.showMissedCallNotification(new MissedCallNotifier.CallInfo(call));
                return;
            }
            return;
        }
        Log.i(this, "onCallFilteringCompleted: call already disconnected.", new Object[0]);
    }

    private boolean shouldSilenceInsteadOfReject(Call call) {
        if (!this.mContext.getResources().getBoolean(R.bool.silence_incoming_when_different_service_and_maximum_ringing)) {
            return false;
        }
        for (Call call2 : this.mCalls) {
            if (call2.getParentCall() == null && !call2.isExternalCall() && 4 == call2.getState() && call2.getConnectionService() == call.getConnectionService()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onFailedIncomingCall(Call call) {
        setCallState(call, 7, "failed incoming call");
        call.removeListener(this);
    }

    @Override
    public void onSuccessfulUnknownCall(Call call, int i) {
        setCallState(call, i, "successful unknown call");
        Log.i(this, "onSuccessfulUnknownCall for call %s", new Object[]{call});
        addCall(call);
    }

    @Override
    public void onFailedUnknownCall(Call call) {
        Log.i(this, "onFailedUnknownCall for call %s", new Object[]{call});
        setCallState(call, 7, "failed unknown call");
        call.removeListener(this);
    }

    @Override
    public void onRingbackRequested(Call call, boolean z) {
        Iterator<CallsManagerListener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onRingbackRequested(call, z);
        }
    }

    @Override
    public void onPostDialWait(Call call, String str) {
        this.mInCallController.onPostDialWait(call, str);
    }

    @Override
    public void onPostDialChar(final Call call, char c) {
        if (PhoneNumberUtils.is12Key(c)) {
            if (this.mStopTone != null) {
                this.mHandler.removeCallbacks(this.mStopTone.getRunnableToCancel());
                this.mStopTone.cancel();
            }
            this.mDtmfLocalTonePlayer.playTone(call, c);
            this.mStopTone = new Runnable("CM.oPDC", this.mLock) {
                public void loggedRun() {
                    CallsManager.this.mDtmfLocalTonePlayer.stopTone(call);
                }
            };
            this.mHandler.postDelayed(this.mStopTone.prepare(), Timeouts.getDelayBetweenDtmfTonesMillis(this.mContext.getContentResolver()));
            return;
        }
        if (c == 0 || c == ';' || c == ',') {
            if (this.mStopTone != null) {
                this.mHandler.removeCallbacks(this.mStopTone.getRunnableToCancel());
                this.mStopTone.cancel();
            }
            this.mDtmfLocalTonePlayer.stopTone(call);
            return;
        }
        Log.w(this, "onPostDialChar: invalid value %d", new Object[]{Character.valueOf(c)});
    }

    @Override
    public void onParentChanged(Call call) {
        updateCanAddCall();
        Iterator<CallsManagerListener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onIsConferencedChanged(call);
        }
    }

    @Override
    public void onChildrenChanged(Call call) {
        updateCanAddCall();
        Iterator<CallsManagerListener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onIsConferencedChanged(call);
        }
    }

    @Override
    public void onIsVoipAudioModeChanged(Call call) {
        Iterator<CallsManagerListener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onIsVoipAudioModeChanged(call);
        }
    }

    @Override
    public void onVideoStateChanged(Call call, int i, int i2) {
        Iterator<CallsManagerListener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onVideoStateChanged(call, i, i2);
        }
    }

    @Override
    public boolean onCanceledViaNewOutgoingCallBroadcast(final Call call, long j) {
        this.mPendingCallsToDisconnect.add(call);
        this.mHandler.postDelayed(new Runnable("CM.oCVNOCB", this.mLock) {
            public void loggedRun() {
                if (CallsManager.this.mPendingCallsToDisconnect.remove(call)) {
                    Log.i(this, "Delayed disconnection of call: %s", new Object[]{call});
                    call.disconnect();
                }
            }
        }.prepare(), j);
        return true;
    }

    @Override
    public void onVideoCallProviderChanged(Call call) {
        VideoProviderProxy videoProviderProxy = call.getVideoProviderProxy();
        if (videoProviderProxy == null) {
            return;
        }
        videoProviderProxy.addListener(this);
    }

    @Override
    public void onSessionModifyRequestReceived(Call call, VideoProfile videoProfile) {
        Log.v("CallsManager", "onSessionModifyRequestReceived : videoProfile = " + VideoProfile.videoStateToString(videoProfile != null ? videoProfile.getVideoState() : 0), new Object[0]);
        Iterator<CallsManagerListener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onSessionModifyRequestReceived(call, videoProfile);
        }
    }

    public Collection<Call> getCalls() {
        return Collections.unmodifiableCollection(this.mCalls);
    }

    @Override
    public void onHoldToneRequested(Call call) {
        Iterator<CallsManagerListener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onHoldToneRequested(call);
        }
    }

    @Override
    public void onHandoverRequested(Call call, PhoneAccountHandle phoneAccountHandle, int i, Bundle bundle, boolean z) {
        if (z) {
            requestHandoverViaEvents(call, phoneAccountHandle, i, bundle);
        } else {
            requestHandover(call, phoneAccountHandle, i, bundle);
        }
    }

    @VisibleForTesting
    public Call getForegroundCall() {
        if (this.mCallAudioManager == null) {
            return null;
        }
        return this.mCallAudioManager.getForegroundCall();
    }

    @Override
    public UserHandle getCurrentUserHandle() {
        return this.mCurrentUserHandle;
    }

    public CallAudioManager getCallAudioManager() {
        return this.mCallAudioManager;
    }

    @VisibleForTesting
    public InCallController getInCallController() {
        return this.mInCallController;
    }

    EmergencyCallHelper getEmergencyCallHelper() {
        return this.mEmergencyCallHelper;
    }

    @VisibleForTesting
    public boolean hasEmergencyCall() {
        Iterator<Call> it = this.mCalls.iterator();
        while (it.hasNext()) {
            if (it.next().isEmergencyCall()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasEmergencyRttCall() {
        for (Call call : this.mCalls) {
            if (call.isEmergencyCall() && call.isRttCall()) {
                return true;
            }
        }
        return false;
    }

    @VisibleForTesting
    public boolean hasOnlyDisconnectedCalls() {
        if (this.mCalls.size() == 0) {
            return false;
        }
        Iterator<Call> it = this.mCalls.iterator();
        while (it.hasNext()) {
            if (!it.next().isDisconnected()) {
                return false;
            }
        }
        return true;
    }

    public boolean hasVideoCall() {
        Iterator<Call> it = this.mCalls.iterator();
        while (it.hasNext()) {
            if (VideoProfile.isVideo(it.next().getVideoState())) {
                return true;
            }
        }
        return false;
    }

    @VisibleForTesting
    public CallAudioState getAudioState() {
        return this.mCallAudioManager.getCallAudioState();
    }

    boolean isTtySupported() {
        return this.mTtyManager.isTtySupported();
    }

    int getCurrentTtyMode() {
        return this.mTtyManager.getCurrentTtyMode();
    }

    @VisibleForTesting
    public void addListener(CallsManagerListener callsManagerListener) {
        this.mListeners.add(callsManagerListener);
    }

    void processIncomingCallIntent(PhoneAccountHandle phoneAccountHandle, Bundle bundle) {
        Call call;
        Bundle bundle2;
        int i;
        int i2;
        int i3;
        char c;
        Log.d(this, "processIncomingCallIntent", new Object[0]);
        boolean z = bundle.getBoolean("android.telecom.extra.IS_HANDOVER");
        Uri uri = (Uri) bundle.getParcelable("android.telecom.extra.INCOMING_CALL_ADDRESS");
        if (uri == null) {
            uri = (Uri) bundle.getParcelable("incoming_number");
        }
        Uri uri2 = uri;
        Call call2 = new Call(getNextCallId(), this.mContext, this, this.mLock, this.mConnectionServiceRepository, this.mContactsAsyncHelper, this.mCallerInfoAsyncQueryFactory, this.mPhoneNumberUtilsAdapter, uri2, null, null, phoneAccountHandle, 2, false, false, this.mClockProxy);
        PhoneAccount phoneAccountUnchecked = this.mPhoneAccountRegistrar.getPhoneAccountUnchecked(phoneAccountHandle);
        int i4 = 1;
        i4 = 1;
        if (phoneAccountUnchecked != null) {
            call = call2;
            call.setIsSelfManaged(phoneAccountUnchecked.isSelfManaged());
            if (call.isSelfManaged()) {
                call.setIsVoipAudioMode(true);
            } else {
                Call call3 = (Call) this.mConnectionSvrFocusMgr.getCurrentFocusCall();
                if (call3 != null && !canHold(call3) && call3.isSelfManaged()) {
                    Bundle bundle3 = new Bundle();
                    bundle3.putBoolean("android.telecom.extra.ANSWERING_DROPS_FG_CALL", true);
                    CharSequence targetPhoneAccountLabel = call3.getTargetPhoneAccountLabel();
                    bundle3.putCharSequence("android.telecom.extra.ANSWERING_DROPS_FG_CALL_APP_NAME", targetPhoneAccountLabel);
                    c = 0;
                    Log.i(this, "Incoming managed call will drop %s call.", new Object[]{targetPhoneAccountLabel});
                    call.putExtras(1, bundle3);
                }
                bundle2 = bundle;
                i = c;
                if (bundle2.getBoolean("android.telecom.extra.ALWAYS_USE_VOIP_AUDIO_MODE")) {
                    Object[] objArr = new Object[1];
                    objArr[c] = call.getId();
                    Log.d(this, "processIncomingCallIntent: defaulting to voip mode for call %s", objArr);
                    call.setIsVoipAudioMode(true);
                    i = c;
                }
            }
            c = 0;
            bundle2 = bundle;
            i = c;
            if (bundle2.getBoolean("android.telecom.extra.ALWAYS_USE_VOIP_AUDIO_MODE")) {
            }
        } else {
            call = call2;
            bundle2 = bundle;
            i = 0;
        }
        if (isRttSettingOn() || bundle2.getBoolean("android.telecom.extra.START_CALL_WITH_RTT", i)) {
            Object[] objArr2 = new Object[1];
            objArr2[i] = Boolean.valueOf(isRttSettingOn());
            Log.i(this, "Incoming call requesting RTT, rtt setting is %b", objArr2);
            call.createRttStreams();
            call.setRequestedToStartWithRtt();
        }
        if (bundle2.containsKey("android.telecom.extra.INCOMING_VIDEO_STATE") && phoneAccountUnchecked != null && phoneAccountUnchecked.hasCapabilities(8)) {
            i2 = bundle2.getInt("android.telecom.extra.INCOMING_VIDEO_STATE");
            call.setVideoState(i2);
        } else {
            i2 = i;
        }
        call.initAnalytics();
        if (getForegroundCall() != null) {
            getForegroundCall().getAnalytics().setCallIsInterrupted(true);
            call.getAnalytics().setCallIsAdditional(true);
        }
        setIntentExtrasAndStartTime(call, bundle2);
        call.addListener(this);
        if (z) {
            if (isHandoverInProgress() || !isHandoverToPhoneAccountSupported(phoneAccountHandle)) {
                Log.w(this, "processIncomingCallIntent: To account doesn't support handover.", new Object[i]);
            } else {
                final String schemeSpecificPart = uri2.getSchemeSpecificPart();
                Call callOrElse = this.mCalls.stream().filter(new Predicate() {
                    @Override
                    public final boolean test(Object obj) {
                        Call call4 = (Call) obj;
                        return this.f$0.mPhoneNumberUtilsAdapter.isSamePhoneNumber(call4.getHandle() == null ? null : call4.getHandle().getSchemeSpecificPart(), schemeSpecificPart);
                    }
                }).findFirst().orElse(null);
                if (callOrElse != null) {
                    if (!isHandoverFromPhoneAccountSupported(callOrElse.getTargetPhoneAccount())) {
                        Log.w(this, "processIncomingCallIntent: From account doesn't support handover.", new Object[i]);
                    } else {
                        i3 = 1;
                        if (i3 != 0) {
                            callOrElse.setHandoverDestinationCall(call);
                            call.setHandoverSourceCall(callOrElse);
                            call.setHandoverState(2);
                            callOrElse.setHandoverState(3);
                            Object[] objArr3 = new Object[2];
                            objArr3[i] = callOrElse.getId();
                            objArr3[1] = call.getId();
                            Log.addEvent(callOrElse, "START_HANDOVER", "handOverFrom=%s, handOverTo=%s", objArr3);
                            Object[] objArr4 = new Object[2];
                            objArr4[i] = callOrElse.getId();
                            objArr4[1] = call.getId();
                            Log.addEvent(call, "START_HANDOVER", "handOverFrom=%s, handOverTo=%s", objArr4);
                            if (isSpeakerEnabledForVideoCalls() && VideoProfile.isVideo(i2)) {
                                call.setStartWithSpeakerphoneOn(true);
                            }
                        }
                        i4 = i3;
                    }
                } else {
                    Log.w(this, "processIncomingCallIntent: handover fail; can't find from call.", new Object[i]);
                }
                i3 = i;
                if (i3 != 0) {
                }
                i4 = i3;
            }
        }
        if (i4 == 0 || (call.isSelfManaged() && !isIncomingCallPermitted(call, call.getTargetPhoneAccount()))) {
            notifyCreateConnectionFailed(phoneAccountHandle, call);
        } else {
            call.startCreateConnection(this.mPhoneAccountRegistrar);
        }
    }

    void addNewUnknownCall(PhoneAccountHandle phoneAccountHandle, Bundle bundle) {
        Uri uri = (Uri) bundle.getParcelable("android.telecom.extra.UNKNOWN_CALL_HANDLE");
        Log.i(this, "addNewUnknownCall with handle: %s", new Object[]{Log.pii(uri)});
        Call call = new Call(getNextCallId(), this.mContext, this, this.mLock, this.mConnectionServiceRepository, this.mContactsAsyncHelper, this.mCallerInfoAsyncQueryFactory, this.mPhoneNumberUtilsAdapter, uri, null, null, phoneAccountHandle, 3, true, false, this.mClockProxy);
        call.initAnalytics();
        setIntentExtrasAndStartTime(call, bundle);
        call.addListener(this);
        call.startCreateConnection(this.mPhoneAccountRegistrar);
    }

    private boolean areHandlesEqual(Uri uri, Uri uri2) {
        if (uri == null || uri2 == null) {
            return uri == uri2;
        }
        if (!TextUtils.equals(uri.getScheme(), uri2.getScheme())) {
            return false;
        }
        return TextUtils.equals(PhoneNumberUtils.normalizeNumber(uri.getSchemeSpecificPart()), PhoneNumberUtils.normalizeNumber(uri2.getSchemeSpecificPart()));
    }

    private Call reuseOutgoingCall(Uri uri) {
        Iterator<Call> it = this.mPendingCallsToDisconnect.iterator();
        Call call = null;
        while (it.hasNext()) {
            Call next = it.next();
            if (call == null && areHandlesEqual(next.getHandle(), uri)) {
                it.remove();
                Log.i(this, "Reusing disconnected call %s", new Object[]{next});
                call = next;
            } else {
                Log.i(this, "Not reusing disconnected call %s", new Object[]{next});
                it.remove();
                next.disconnect();
            }
        }
        return call;
    }

    @VisibleForTesting
    public Call startOutgoingCall(Uri uri, PhoneAccountHandle phoneAccountHandle, Bundle bundle, UserHandle userHandle, Intent intent) {
        boolean z;
        PhoneAccount phoneAccount;
        UserHandle userHandle2;
        int i;
        boolean z2;
        ?? r0;
        ?? r6;
        ?? r9;
        int i2;
        PhoneAccountHandle phoneAccountHandle2;
        ?? r1;
        CallsManager callsManager;
        CallsManager callsManager2;
        PhoneAccount phoneAccount2;
        CallsManager callsManager3;
        boolean z3;
        if (ExtensionManager.getCallMgrExt().blockOutgoingCall(uri, phoneAccountHandle, bundle)) {
            Log.w(this, "[startOutgoingCall] blockOutgoingCall as it is emergency num on roaming", new Object[0]);
            return null;
        }
        Call callReuseOutgoingCall = reuseOutgoingCall(uri);
        PhoneAccount phoneAccount3 = this.mPhoneAccountRegistrar.getPhoneAccount(phoneAccountHandle, userHandle);
        boolean z4 = phoneAccount3 != null && phoneAccount3.isSelfManaged();
        if (callReuseOutgoingCall == null) {
            phoneAccount = phoneAccount3;
            Call call = new Call(getNextCallId(), this.mContext, this, this.mLock, this.mConnectionServiceRepository, this.mContactsAsyncHelper, this.mCallerInfoAsyncQueryFactory, this.mPhoneNumberUtilsAdapter, uri, null, null, null, 1, false, false, this.mClockProxy);
            call.initAnalytics();
            z = z4;
            call.setIsSelfManaged(z);
            if (z) {
                z3 = true;
                call.setIsVoipAudioMode(true);
            } else {
                z3 = true;
            }
            userHandle2 = userHandle;
            call.setInitiatingUser(userHandle2);
            z2 = false;
            r0 = call;
            i = z3;
        } else {
            z = z4;
            phoneAccount = phoneAccount3;
            userHandle2 = userHandle;
            i = 1;
            z2 = true;
            r0 = callReuseOutgoingCall;
        }
        if (bundle != 0) {
            r6 = 0;
            int i3 = bundle.getInt("android.telecom.extra.START_CALL_WITH_VIDEO_STATE", 0);
            if (VideoProfile.isVideo(i3)) {
                if (r0.isEmergencyCall()) {
                    phoneAccount2 = phoneAccount;
                    if (phoneAccount2 != null && !phoneAccount2.hasCapabilities(512)) {
                        CallsManager callsManager4 = this;
                        Log.i(callsManager4, "startOutgoingCall - emergency video calls not supported; falling back to audio-only", new Object[0]);
                        callsManager3 = callsManager4;
                    }
                    i2 = 0;
                    callsManager2 = callsManager3;
                    r0.setVideoState(i2);
                    r9 = callsManager2;
                } else {
                    phoneAccount2 = phoneAccount;
                }
                CallsManager callsManager5 = this;
                callsManager = callsManager5;
                if (phoneAccount2 != null) {
                    callsManager = callsManager5;
                    if (!phoneAccount2.hasCapabilities(8)) {
                        Log.i(callsManager5, "startOutgoingCall - video calls not supported; fallback to audio-only.", new Object[0]);
                        callsManager3 = callsManager5;
                        i2 = 0;
                        callsManager2 = callsManager3;
                        r0.setVideoState(i2);
                        r9 = callsManager2;
                    }
                }
            } else {
                callsManager = this;
            }
            i2 = i3;
            callsManager2 = callsManager;
            r0.setVideoState(i2);
            r9 = callsManager2;
        } else {
            r6 = 0;
            r9 = this;
            i2 = 0;
        }
        List<PhoneAccountHandle> listFindOutgoingCallPhoneAccount = r9.findOutgoingCallPhoneAccount(phoneAccountHandle, uri, VideoProfile.isVideo(i2), userHandle2);
        if (listFindOutgoingCallPhoneAccount.size() == i) {
            phoneAccountHandle2 = listFindOutgoingCallPhoneAccount.get(r6);
        } else {
            phoneAccountHandle2 = null;
        }
        r0.setTargetPhoneAccount(phoneAccountHandle2);
        r0.setVoiceMailEmergencyCallIfNeeded();
        if (r9.blockOutgoingCallInRoaming(r9.mContext, uri, phoneAccountHandle2, bundle)) {
            return null;
        }
        ?? r11 = (!isPotentialInCallMMICode(uri) || z) ? r6 : i;
        if (r11 == 0 && !z2 && !r9.makeRoomForOutgoingCall(r0, r0.isEmergencyCall())) {
            if (!z) {
                MtkUtil.showOutgoingFailedToast("Failed to make room. " + dumpCurrentCallStates());
            }
            Call foregroundCall = getForegroundCall();
            ?? r12 = new Object[i];
            r12[r6] = r0;
            Log.d((Object) r9, "No more room for outgoing call %s ", (Object[]) r12);
            if (foregroundCall != null && foregroundCall.isSelfManaged()) {
                r0.setOriginalCallIntent(intent);
                r9.startCallConfirmation(r0);
            } else {
                r9.notifyCreateConnectionFailed(r0.getTargetPhoneAccount(), r0);
            }
            return null;
        }
        ?? r3 = (phoneAccountHandle2 != null || listFindOutgoingCallPhoneAccount.size() <= i || r0.isEmergencyCall() || z) ? r6 : i;
        if (r3 != 0) {
            r0.setState(2, "needs account selection");
            Bundle bundle2 = new Bundle((Bundle) bundle);
            bundle2.putParcelableList("selectPhoneAccountAccounts", listFindOutgoingCallPhoneAccount);
            r1 = bundle2;
        } else {
            PhoneAccount phoneAccount4 = r9.mPhoneAccountRegistrar.getPhoneAccount(phoneAccountHandle2, userHandle2);
            if (phoneAccount4 != null && phoneAccount4.getExtras() != null && phoneAccount4.getExtras().getBoolean("android.telecom.extra.ALWAYS_USE_VOIP_AUDIO_MODE")) {
                Object[] objArr = new Object[i];
                objArr[r6] = r0.getId();
                Log.d((Object) r9, "startOutgoingCall: defaulting to voip mode for call %s", objArr);
                r0.setIsVoipAudioMode(i);
            }
            r0.setState(i, phoneAccountHandle2 == null ? "no-handle" : phoneAccountHandle2.toString());
            if (isRttSettingOn() || (bundle != 0 && bundle.getBoolean("android.telecom.extra.START_CALL_WITH_RTT", r6))) {
                Object[] objArr2 = new Object[i];
                objArr2[r6] = Boolean.valueOf(isRttSettingOn());
                Log.d((Object) r9, "Outgoing call requesting RTT, rtt setting is %b", objArr2);
                if (phoneAccount4 != null && phoneAccount4.hasCapabilities(4096)) {
                    r0.createRttStreams();
                }
                r0.setRequestedToStartWithRtt();
            }
            r1 = bundle;
        }
        r9.setIntentExtrasAndStartTime(r0, r1);
        if (!r0.isEmergencyCall() && ((isPotentialMMICode(uri) || r11 != 0) && r3 == 0)) {
            r0.addListener(r9);
        } else if (!r9.mCalls.contains(r0)) {
            r9.addCall(r0);
        }
        return r0;
    }

    @VisibleForTesting
    public List<PhoneAccountHandle> findOutgoingCallPhoneAccount(PhoneAccountHandle phoneAccountHandle, Uri uri, boolean z, UserHandle userHandle) {
        PhoneAccountHandle outgoingPhoneAccountForScheme;
        if (isSelfManaged(phoneAccountHandle, userHandle)) {
            return Arrays.asList(phoneAccountHandle);
        }
        List<PhoneAccountHandle> listConstructPossiblePhoneAccounts = constructPossiblePhoneAccounts(uri, userHandle, z);
        if (z && listConstructPossiblePhoneAccounts.size() == 0) {
            listConstructPossiblePhoneAccounts = constructPossiblePhoneAccounts(uri, userHandle, false);
        }
        Log.v(this, "findOutgoingCallPhoneAccount: accounts = " + listConstructPossiblePhoneAccounts, new Object[0]);
        if (this.mCallAudioManager.getPossiblyHeldForegroundCall() != null && isPotentialInCallMMICode(uri)) {
            Call possiblyHeldForegroundCall = this.mCallAudioManager.getPossiblyHeldForegroundCall();
            if (possiblyHeldForegroundCall.getTargetPhoneAccount() == null && !possiblyHeldForegroundCall.getChildCalls().isEmpty()) {
                possiblyHeldForegroundCall = possiblyHeldForegroundCall.getChildCalls().get(0);
            }
            if (possiblyHeldForegroundCall.getTargetPhoneAccount() != null) {
                phoneAccountHandle = possiblyHeldForegroundCall.getTargetPhoneAccount();
            }
        }
        if (phoneAccountHandle != null) {
            if (listConstructPossiblePhoneAccounts.contains(phoneAccountHandle)) {
                return Arrays.asList(phoneAccountHandle);
            }
            phoneAccountHandle = null;
        }
        if (phoneAccountHandle == null && listConstructPossiblePhoneAccounts.size() > 0 && listConstructPossiblePhoneAccounts.size() > 1 && (outgoingPhoneAccountForScheme = this.mPhoneAccountRegistrar.getOutgoingPhoneAccountForScheme(uri.getScheme(), userHandle)) != null && listConstructPossiblePhoneAccounts.contains(outgoingPhoneAccountForScheme)) {
            listConstructPossiblePhoneAccounts.clear();
            listConstructPossiblePhoneAccounts.add(outgoingPhoneAccountForScheme);
            return listConstructPossiblePhoneAccounts;
        }
        return listConstructPossiblePhoneAccounts;
    }

    public boolean isSelfManaged(PhoneAccountHandle phoneAccountHandle, UserHandle userHandle) {
        PhoneAccount phoneAccount = this.mPhoneAccountRegistrar.getPhoneAccount(phoneAccountHandle, userHandle);
        return phoneAccount != null && phoneAccount.isSelfManaged();
    }

    @VisibleForTesting
    public void placeOutgoingCall(Call call, Uri uri, GatewayInfo gatewayInfo, boolean z, int i) {
        Uri gatewayAddress;
        boolean z2;
        String scheme;
        if (call == null) {
            Log.i(this, "Canceling unknown call.", new Object[0]);
            return;
        }
        int iCheckForVideoCallOverWifi = checkForVideoCallOverWifi(call, i);
        if (gatewayInfo != null) {
            gatewayAddress = gatewayInfo.getGatewayAddress();
        } else {
            gatewayAddress = uri;
        }
        if (gatewayInfo == null) {
            Log.i(this, "Creating a new outgoing call with handle: %s", new Object[]{Log.piiHandle(gatewayAddress)});
        } else {
            Log.i(this, "Creating a new outgoing call with gateway handle: %s, original handle: %s", new Object[]{Log.pii(gatewayAddress), Log.pii(uri)});
        }
        call.setHandle(gatewayAddress);
        call.setGatewayInfo(gatewayInfo);
        boolean z3 = this.mContext.getResources().getBoolean(R.bool.use_speaker_when_docked);
        boolean zIsSpeakerphoneEnabledForDock = isSpeakerphoneEnabledForDock();
        boolean zIsSpeakerphoneAutoEnabledForVideoCalls = isSpeakerphoneAutoEnabledForVideoCalls(iCheckForVideoCallOverWifi);
        boolean zIsSpeakerphoneEnabledForTablet = isSpeakerphoneEnabledForTablet();
        if (!z && !zIsSpeakerphoneAutoEnabledForVideoCalls && ((!z3 || !zIsSpeakerphoneEnabledForDock) && !zIsSpeakerphoneEnabledForTablet)) {
            z2 = false;
        } else {
            z2 = true;
        }
        call.setStartWithSpeakerphoneOn(z2);
        call.setVideoState(iCheckForVideoCallOverWifi);
        if (z) {
            Log.i(this, "%s Starting with speakerphone as requested", new Object[]{call});
        } else if (z3 && zIsSpeakerphoneEnabledForDock) {
            Log.i(this, "%s Starting with speakerphone because car is docked.", new Object[]{call});
        } else if (zIsSpeakerphoneAutoEnabledForVideoCalls) {
            Log.i(this, "%s Starting with speakerphone because its a video call.", new Object[]{call});
        }
        if (call.isEmergencyCall()) {
            new AsyncEmergencyContactNotifier(this.mContext).execute(new Void[0]);
            if (ExtensionManager.getCallMgrExt().shouldDisconnectCallsWhenEcc() && shouldDisconnectCallsWhenEccByCarrierConfig(call.getTargetPhoneAccount()) && !isOkForECC(call)) {
                Log.i(this, "placeOutgoingCall now is not ok for ECC, waiting ......", new Object[0]);
                this.mCalls.remove(call);
                this.mPendingEccCall = call;
                if (!hasConnectingCall(null)) {
                    this.mNeedDisconnectLater = false;
                    disconnectAllCalls();
                } else {
                    Log.d(this, "Need disconnect all calls later", new Object[0]);
                    this.mNeedDisconnectLater = true;
                }
                if (this.mPendingEccCall != null) {
                    this.mCalls.add(this.mPendingEccCall);
                    return;
                }
                return;
            }
        }
        boolean z4 = this.mContext.getResources().getBoolean(android.R.^attr-private.magnifierWidth);
        boolean zIsOutgoingCallPermitted = isOutgoingCallPermitted(call, call.getTargetPhoneAccount());
        if (call.getHandle() != null) {
            scheme = call.getHandle().getScheme();
        } else {
            scheme = null;
        }
        if (call.getTargetPhoneAccount() != null || call.isEmergencyCall()) {
            if (call.isSelfManaged() && !zIsOutgoingCallPermitted) {
                notifyCreateConnectionFailed(call.getTargetPhoneAccount(), call);
                return;
            }
            if (call.isEmergencyCall()) {
                disconnectSelfManagedCalls("place emerg call");
            }
            call.startCreateConnection(this.mPhoneAccountRegistrar);
            return;
        }
        if (this.mPhoneAccountRegistrar.getCallCapablePhoneAccounts(z4 ? scheme : null, false, call.getInitiatingUser()).isEmpty()) {
            markCallAsDisconnected(call, new DisconnectCause(4, "No registered PhoneAccounts"));
            markCallAsRemoved(call);
        }
    }

    @VisibleForTesting
    public void conference(Call call, Call call2) {
        call.conferenceWith(call2);
    }

    @VisibleForTesting
    public void answerCall(Call call, int i) {
        if (!this.mCalls.contains(call)) {
            Log.i(this, "Request to answer a non-existent call %s", new Object[]{call});
            return;
        }
        Log.d(this, "Incoming call = %s Ongoing call %s", new Object[]{call, (Call) this.mConnectionSvrFocusMgr.getCurrentFocusCall()});
        Call foregroundCall = getForegroundCall();
        if (MtkUtil.isInDsdaMode() && shouldDisconnectActiveCall(foregroundCall, call)) {
            foregroundCall.disconnect();
        } else if (holdActiveAndAnswerRinging(call, i)) {
            return;
        }
        this.mConnectionSvrFocusMgr.requestFocus(call, new RequestCallback(new ActionAnswerCall(call, i)));
    }

    @VisibleForTesting
    public void deflectCall(Call call, Uri uri) {
        if (!this.mCalls.contains(call)) {
            Log.i(this, "Request to deflect a non-existent call %s", new Object[]{call});
        } else {
            call.deflect(uri);
        }
    }

    public boolean isSpeakerphoneAutoEnabledForVideoCalls(int i) {
        return VideoProfile.isVideo(i) && !this.mWiredHeadsetManager.isPluggedIn() && !this.mBluetoothRouteManager.isBluetoothAvailable() && isSpeakerEnabledForVideoCalls();
    }

    private boolean isSpeakerphoneEnabledForDock() {
        return (!this.mDockManager.isDocked() || this.mWiredHeadsetManager.isPluggedIn() || this.mBluetoothRouteManager.isBluetoothAvailable()) ? false : true;
    }

    private static boolean isSpeakerEnabledForVideoCalls() {
        return SystemProperties.getInt("persist.radio.call.audio.output", 0) == 0;
    }

    @VisibleForTesting
    public void rejectCall(Call call, boolean z, String str) {
        if (!this.mCalls.contains(call)) {
            Log.i(this, "Request to reject a non-existent call %s", new Object[]{call});
            return;
        }
        Iterator<CallsManagerListener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onIncomingCallRejected(call, z, str);
        }
        call.reject(z, str);
    }

    @VisibleForTesting
    public void playDtmfTone(Call call, char c) {
        if (!this.mCalls.contains(call)) {
            Log.i(this, "Request to play DTMF in a non-existent call %s", new Object[]{call});
        } else if (call.getState() != 6) {
            call.playDtmfTone(c);
            this.mDtmfLocalTonePlayer.playTone(call, c);
        } else {
            Log.i(this, "Request to play DTMF tone for held call %s", new Object[]{call.getId()});
        }
    }

    @VisibleForTesting
    public void stopDtmfTone(Call call) {
        if (!this.mCalls.contains(call)) {
            Log.i(this, "Request to stop DTMF in a non-existent call %s", new Object[]{call});
        } else {
            call.stopDtmfTone();
            this.mDtmfLocalTonePlayer.stopTone(call);
        }
    }

    void postDialContinue(Call call, boolean z) {
        if (call != null) {
            if (!this.mCalls.contains(call)) {
                Log.i(this, "Request to continue post-dial string in a non-existent call %s", new Object[]{call});
            } else {
                call.postDialContinue(z);
            }
        }
    }

    @VisibleForTesting
    public void disconnectCall(Call call) {
        Log.v(this, "disconnectCall %s", new Object[]{call});
        if (!this.mCalls.contains(call)) {
            Log.w(this, "Unknown call (%s) asked to disconnect", new Object[]{call});
            return;
        }
        this.mLocallyDisconnectingCalls.add(call);
        call.disconnect();
        if (this.mPendingEccCall != null && this.mPendingEccCall == call) {
            this.mPendingEccCall = null;
            this.mNeedDisconnectLater = false;
        }
    }

    void disconnectAllCalls() {
        Log.v(this, "disconnectAllCalls", new Object[0]);
        Iterator<Call> it = this.mCalls.iterator();
        while (it.hasNext()) {
            disconnectCall(it.next());
        }
    }

    private void disconnectOtherCalls(final PhoneAccountHandle phoneAccountHandle) {
        this.mCalls.stream().filter(new Predicate() {
            @Override
            public final boolean test(Object obj) {
                return CallsManager.lambda$disconnectOtherCalls$1(phoneAccountHandle, (Call) obj);
            }
        }).forEach(new Consumer() {
            @Override
            public final void accept(Object obj) {
                this.f$0.disconnectCall((Call) obj);
            }
        });
    }

    static boolean lambda$disconnectOtherCalls$1(PhoneAccountHandle phoneAccountHandle, Call call) {
        return (call.isEmergencyCall() || call.getTargetPhoneAccount().equals(phoneAccountHandle)) ? false : true;
    }

    @VisibleForTesting
    public void holdCall(Call call) {
        if (!this.mCalls.contains(call)) {
            Log.w(this, "Unknown call (%s) asked to be put on hold", new Object[]{call});
        } else {
            Log.d(this, "Putting call on hold: (%s)", new Object[]{call});
            call.hold();
        }
        Call heldCall = getHeldCall();
        if (heldCall != null && !Objects.equals(call.getTargetPhoneAccount(), heldCall.getTargetPhoneAccount())) {
            heldCall.unhold();
        }
    }

    @VisibleForTesting
    public void unholdCall(Call call) {
        if (!this.mCalls.contains(call)) {
            Log.w(this, "Unknown call (%s) asked to be removed from hold", new Object[]{call});
            return;
        }
        Call call2 = (Call) this.mConnectionSvrFocusMgr.getCurrentFocusCall();
        String id = null;
        if (call2 != null) {
            id = call2.getId();
            if (canHold(call2)) {
                call2.hold("Swap to " + call.getId());
                Log.addEvent(call2, "SWAP", "To " + call.getId());
                Log.addEvent(call, "SWAP", "From " + call2.getId());
                if (MtkUtil.isInDsdaMode()) {
                    addWaitingCallAction(call2, call, "OPERATION_ANSWER_UNHOLD", call.getVideoState());
                    return;
                }
            } else if (call2.getConnectionService() != call.getConnectionService()) {
                call2.disconnect("Swap to " + call.getId());
            } else {
                call2.hold("Swap to " + call.getId());
            }
        }
        this.mConnectionSvrFocusMgr.requestFocus(call, new RequestCallback(new ActionUnHoldCall(call, id)));
    }

    @Override
    public void onExtrasRemoved(Call call, int i, List<String> list) {
        if (i != 1) {
            return;
        }
        updateCanAddCall();
    }

    @Override
    public void onExtrasChanged(Call call, int i, Bundle bundle) {
        if (i != 1) {
            return;
        }
        handleCallTechnologyChange(call);
        handleChildAddressChange(call);
        updateCanAddCall();
    }

    @VisibleForTesting
    public List<PhoneAccountHandle> constructPossiblePhoneAccounts(Uri uri, UserHandle userHandle, boolean z) {
        if (uri == null) {
            return Collections.emptyList();
        }
        List<PhoneAccountHandle> callCapablePhoneAccounts = this.mPhoneAccountRegistrar.getCallCapablePhoneAccounts(uri.getScheme(), false, userHandle, z ? 8 : 0);
        if (!MtkUtil.isInDsdaMode()) {
            List<PhoneAccountHandle> simPhoneAccountsOfCurrentUser = this.mPhoneAccountRegistrar.getSimPhoneAccountsOfCurrentUser();
            PhoneAccountHandle targetPhoneAccount = null;
            Iterator<Call> it = this.mCalls.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                Call next = it.next();
                if (!next.isDisconnected() && !next.isNew() && simPhoneAccountsOfCurrentUser.contains(next.getTargetPhoneAccount())) {
                    targetPhoneAccount = next.getTargetPhoneAccount();
                    break;
                }
            }
            if (targetPhoneAccount != null) {
                simPhoneAccountsOfCurrentUser.remove(targetPhoneAccount);
                ExtensionManager.getDigitsUtilExt().updatePhoneAccounts(simPhoneAccountsOfCurrentUser, targetPhoneAccount);
                callCapablePhoneAccounts.removeAll(simPhoneAccountsOfCurrentUser);
            }
        }
        return callCapablePhoneAccounts;
    }

    @Override
    public void onExternalCallChanged(Call call, boolean z) {
        Log.v(this, "onConnectionPropertiesChanged: %b", new Object[]{Boolean.valueOf(z)});
        Iterator<CallsManagerListener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onExternalCallChanged(call, z);
        }
    }

    private void handleCallTechnologyChange(Call call) {
        if (call.getExtras() != null && call.getExtras().containsKey("android.telecom.extra.CALL_TECHNOLOGY_TYPE")) {
            Integer num = sAnalyticsTechnologyMap.get(Integer.valueOf(call.getExtras().getInt("android.telecom.extra.CALL_TECHNOLOGY_TYPE")));
            if (num == null) {
                num = 16;
            }
            call.getAnalytics().addCallTechnology(num.intValue());
        }
    }

    public void handleChildAddressChange(Call call) {
        if (call.getExtras() != null && call.getExtras().containsKey("android.telecom.extra.CHILD_ADDRESS")) {
            call.setViaNumber(call.getExtras().getString("android.telecom.extra.CHILD_ADDRESS"));
        }
    }

    void mute(boolean z) {
        if (hasEmergencyCall() && z) {
            Log.i(this, "Refusing to turn on mute because we're in an emergency call", new Object[0]);
            z = false;
        }
        this.mCallAudioManager.mute(z);
    }

    void setAudioRoute(int i, String str) {
        if (hasEmergencyRttCall() && i != 8) {
            Log.i(this, "In an emergency RTT call. Forcing route to speaker.", new Object[0]);
            str = null;
            i = 8;
        }
        this.mCallAudioManager.setAudioRoute(i, str);
    }

    void turnOnProximitySensor() {
        this.mProximitySensorManager.turnOn();
    }

    void turnOffProximitySensor(boolean z) {
        this.mProximitySensorManager.turnOff(z);
    }

    private boolean isRttSettingOn() {
        return Settings.Secure.getInt(this.mContext.getContentResolver(), "rtt_calling_mode", 0) != 0;
    }

    void phoneAccountSelected(Call call, PhoneAccountHandle phoneAccountHandle, boolean z) {
        if (!this.mCalls.contains(call)) {
            Log.i(this, "Attempted to add account to unknown call %s", new Object[]{call});
            return;
        }
        call.setTargetPhoneAccount(phoneAccountHandle);
        call.setVoiceMailEmergencyCallIfNeeded();
        checkForVideoCallOverWifi(call, call.getVideoState());
        PhoneAccount phoneAccountUnchecked = this.mPhoneAccountRegistrar.getPhoneAccountUnchecked(phoneAccountHandle);
        if (phoneAccountUnchecked != null && phoneAccountUnchecked.getExtras() != null && phoneAccountUnchecked.getExtras().getBoolean("android.telecom.extra.ALWAYS_USE_VOIP_AUDIO_MODE")) {
            Log.d("phoneAccountSelected: default to voip mode for call %s", call.getId(), new Object[0]);
            call.setIsVoipAudioMode(true);
        }
        if (isRttSettingOn() || call.getIntentExtras().getBoolean("android.telecom.extra.START_CALL_WITH_RTT", false)) {
            Log.d(this, "Outgoing call after account selection requesting RTT, rtt setting is %b", new Object[]{Boolean.valueOf(isRttSettingOn())});
            if (phoneAccountUnchecked != null && phoneAccountUnchecked.hasCapabilities(4096)) {
                call.createRttStreams();
            }
            call.setRequestedToStartWithRtt();
        }
        if (!call.isNewOutgoingCallIntentBroadcastDone()) {
            return;
        }
        if (makeRoomForOutgoingCall(call, false)) {
            call.startCreateConnection(this.mPhoneAccountRegistrar);
        } else {
            MtkUtil.showOutgoingFailedToast("Failed to make room after account selection. " + dumpCurrentCallStates());
            call.disconnect("no room");
        }
        if (z) {
            this.mPhoneAccountRegistrar.setUserSelectedOutgoingPhoneAccount(phoneAccountHandle, call.getInitiatingUser());
        }
    }

    @VisibleForTesting
    public void onCallAudioStateChanged(CallAudioState callAudioState, CallAudioState callAudioState2) {
        Log.v(this, "onAudioStateChanged, audioState: %s -> %s", new Object[]{callAudioState, callAudioState2});
        Iterator<CallsManagerListener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onCallAudioStateChanged(callAudioState, callAudioState2);
        }
    }

    @VisibleForTesting
    public void onDisconnectedTonePlaying(boolean z) {
        Object[] objArr = new Object[1];
        objArr[0] = z ? "started" : "stopped";
        Log.v(this, "onDisconnectedTonePlaying, %s", objArr);
        Iterator<CallsManagerListener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onDisconnectedTonePlaying(z);
        }
    }

    void markCallAsRinging(Call call) {
        setCallState(call, 4, "ringing set explicitly");
    }

    void markCallAsDialing(Call call) {
        setCallState(call, 3, "dialing set explicitly");
        maybeMoveToSpeakerPhone(call);
        maybeTurnOffMute(call);
        ensureCallAudible();
    }

    void markCallAsPulling(Call call) {
        setCallState(call, 10, "pulling set explicitly");
        maybeMoveToSpeakerPhone(call);
    }

    boolean holdActiveCallForNewCall(Call call) {
        Call call2 = (Call) this.mConnectionSvrFocusMgr.getCurrentFocusCall();
        if (call2 != null && call2 != call) {
            if (canHold(call2)) {
                call2.hold();
                return true;
            }
            if (supportsHold(call2) && call2.getConnectionService() == call.getConnectionService()) {
                Call heldCallByConnectionService = getHeldCallByConnectionService(call.getConnectionService());
                if (heldCallByConnectionService != null) {
                    heldCallByConnectionService.disconnect();
                    Log.i(this, "holdActiveCallForNewCall: Disconnect held call %s before holding active call %s.", new Object[]{heldCallByConnectionService.getId(), call2.getId()});
                }
                Log.i(this, "holdActiveCallForNewCall: Holding active %s before making %s active.", new Object[]{call2.getId(), call.getId()});
                call2.hold();
                return true;
            }
            if (call2.getConnectionService() != call.getConnectionService()) {
                Log.i(this, "holdActiveCallForNewCall: disconnecting %s so that %s can be made active.", new Object[]{call2.getId(), call.getId()});
                call2.disconnect();
            }
        }
        return false;
    }

    @VisibleForTesting
    public void markCallAsActive(Call call) {
        if (call.isSelfManaged()) {
            holdActiveCallForNewCall(call);
            this.mConnectionSvrFocusMgr.requestFocus(call, new RequestCallback(new ActionSetCallState(call, 5, "active set explicitly for self-managed")));
        } else {
            setCallState(call, 5, "active set explicitly");
            maybeMoveToSpeakerPhone(call);
            ensureCallAudible();
        }
    }

    @VisibleForTesting
    public void markCallAsOnHold(Call call) {
        setCallState(call, 6, "on-hold set explicitly");
    }

    void markCallAsDisconnected(Call call, DisconnectCause disconnectCause) {
        call.setDisconnectCause(disconnectCause);
        setCallState(call, 7, "disconnected set explicitly");
    }

    void markCallAsRemoved(Call call) {
        call.maybeCleanupHandover();
        removeCall(call);
        Call possiblyHeldForegroundCall = this.mCallAudioManager.getPossiblyHeldForegroundCall();
        if (this.mLocallyDisconnectingCalls.contains(call) || ExtensionManager.getCallMgrExt().shouldResumeHoldCall() || shouldResumeHoldCall(call)) {
            boolean zIsDisconnectingChildCall = call.isDisconnectingChildCall();
            Log.v(this, "markCallAsRemoved: isDisconnectingChildCall = " + zIsDisconnectingChildCall + "call -> %s", new Object[]{call});
            this.mLocallyDisconnectingCalls.remove(call);
            if (!zIsDisconnectingChildCall && possiblyHeldForegroundCall != null && possiblyHeldForegroundCall.getState() == 6) {
                possiblyHeldForegroundCall.unhold();
            }
        } else if (possiblyHeldForegroundCall != null && !possiblyHeldForegroundCall.can(2) && possiblyHeldForegroundCall.getState() == 6) {
            Log.i(this, "Auto-unholding held foreground call (call doesn't support hold)", new Object[0]);
            possiblyHeldForegroundCall.unhold();
        }
        Log.d(this, "markCallAsRemoved mCalls size = " + this.mCalls.size(), new Object[0]);
        if (this.mPendingEccCall != null && isOkForECC(this.mPendingEccCall)) {
            Log.i(this, "markCallAsRemoved, dial pending ECC:" + this.mPendingEccCall, new Object[0]);
            if (!this.mCalls.contains(this.mPendingEccCall)) {
                this.mCalls.add(this.mPendingEccCall);
            }
            this.mPendingEccCall.startCreateConnection(this.mPhoneAccountRegistrar);
            this.mPendingEccCall = null;
            this.mNeedDisconnectLater = false;
        }
        this.mPendingCallsToDisconnect.remove(call);
    }

    void markCallDisconnectedDueToSelfManagedCall(Call call) {
        CharSequence string;
        Call activeCall = getActiveCall();
        if (activeCall == null) {
            string = this.mContext.getText(R.string.cant_call_due_to_ongoing_unknown_call);
        } else {
            string = this.mContext.getString(R.string.cant_call_due_to_ongoing_call, activeCall.getTargetPhoneAccountLabel());
        }
        markCallAsDisconnected(call, new DisconnectCause(1, string, string, "Ongoing call in another app."));
        markCallAsRemoved(call);
    }

    void handleConnectionServiceDeath(ConnectionServiceWrapper connectionServiceWrapper) {
        if (connectionServiceWrapper != null) {
            Log.i(this, "handleConnectionServiceDeath: service %s died", new Object[]{connectionServiceWrapper});
            for (Call call : this.mCalls) {
                if (call.getConnectionService() == connectionServiceWrapper) {
                    if (call.getState() != 7) {
                        markCallAsDisconnected(call, new DisconnectCause(1, "CS_DEATH"));
                    }
                    markCallAsRemoved(call);
                }
            }
        }
    }

    boolean hasAnyCalls() {
        if (this.mCalls.isEmpty()) {
            return false;
        }
        Iterator<Call> it = this.mCalls.iterator();
        while (it.hasNext()) {
            if (!it.next().isExternalCall()) {
                return true;
            }
        }
        return false;
    }

    boolean hasRingingCall() {
        return getFirstCallWithState(4) != null;
    }

    boolean onMediaButton(int i) {
        if (hasAnyCalls()) {
            Call firstCallWithState = getFirstCallWithState(4);
            if (1 == i) {
                if (firstCallWithState == null) {
                    Call firstCallWithState2 = getFirstCallWithState(4, 3, 10, 5, 6);
                    Log.addEvent(firstCallWithState2, "INFO", "media btn short press - end call.");
                    if (firstCallWithState2 != null) {
                        disconnectCall(firstCallWithState2);
                        return true;
                    }
                } else {
                    firstCallWithState.answer(0);
                    return true;
                }
            } else if (2 == i) {
                if (firstCallWithState != null) {
                    Log.addEvent(getForegroundCall(), "INFO", "media btn long press - reject");
                    firstCallWithState.reject(false, null);
                } else {
                    Log.addEvent(getForegroundCall(), "INFO", "media btn long press - mute");
                    this.mCallAudioManager.toggleMute();
                }
                return true;
            }
        }
        return false;
    }

    @VisibleForTesting
    public boolean canAddCall() {
        if (!(Settings.Global.getInt(this.mContext.getContentResolver(), "device_provisioned", 0) != 0)) {
            Log.d("CallsManager", "Device not provisioned, canAddCall is false.", new Object[0]);
            return false;
        }
        if (getDialingCdmaCall() != null || getFirstCallWithState(OUTGOING_CALL_STATES) != null) {
            return false;
        }
        int i = 0;
        for (Call call : this.mCalls) {
            if (call.isEmergencyCall()) {
                return false;
            }
            if (!call.isExternalCall()) {
                if (call.getParentCall() == null) {
                    i++;
                }
                Bundle extras = call.getExtras();
                if ((extras != null && extras.getBoolean("android.telecom.extra.DISABLE_ADD_CALL", false)) || i >= 2) {
                    return false;
                }
            }
        }
        return true;
    }

    @VisibleForTesting
    public Call getRingingCall() {
        return getFirstCallWithState(4);
    }

    public Call getActiveCall() {
        return getFirstCallWithState(5);
    }

    Call getDialingCall() {
        return getFirstCallWithState(3);
    }

    @VisibleForTesting
    public Call getHeldCall() {
        return getFirstCallWithState(6);
    }

    public Call getHeldCallByConnectionService(final ConnectionServiceWrapper connectionServiceWrapper) {
        Optional<Call> optionalFindFirst = this.mCalls.stream().filter(new Predicate() {
            @Override
            public final boolean test(Object obj) {
                return CallsManager.lambda$getHeldCallByConnectionService$3(connectionServiceWrapper, (Call) obj);
            }
        }).findFirst();
        if (optionalFindFirst.isPresent()) {
            return optionalFindFirst.get();
        }
        return null;
    }

    static boolean lambda$getHeldCallByConnectionService$3(ConnectionServiceWrapper connectionServiceWrapper, Call call) {
        return call.getConnectionService() == connectionServiceWrapper && call.getState() == 6 && call.getParentCall() == null;
    }

    @VisibleForTesting
    public int getNumHeldCalls() {
        int i = 0;
        for (Call call : this.mCalls) {
            if (call.getParentCall() == null && call.getState() == 6) {
                i++;
            }
        }
        return i;
    }

    @VisibleForTesting
    public Call getOutgoingCall() {
        return getFirstCallWithState(OUTGOING_CALL_STATES);
    }

    @VisibleForTesting
    public Call getFirstCallWithState(int... iArr) {
        return getFirstCallWithState(null, iArr);
    }

    @VisibleForTesting
    public PhoneNumberUtilsAdapter getPhoneNumberUtilsAdapter() {
        return this.mPhoneNumberUtilsAdapter;
    }

    Call getFirstCallWithState(Call call, int... iArr) {
        for (int i : iArr) {
            Call foregroundCall = getForegroundCall();
            if (foregroundCall != null && foregroundCall.getState() == i) {
                return foregroundCall;
            }
            for (Call call2 : this.mCalls) {
                if (!Objects.equals(call, call2) && call2.getParentCall() == null && !call2.isExternalCall() && i == call2.getState()) {
                    return call2;
                }
            }
        }
        return null;
    }

    Call createConferenceCall(String str, PhoneAccountHandle phoneAccountHandle, ParcelableConference parcelableConference) {
        long connectTimeMillis;
        long connectElapsedTimeMillis;
        if (parcelableConference.getConnectTimeMillis() == 0) {
            connectTimeMillis = this.mClockProxy.currentTimeMillis();
        } else {
            connectTimeMillis = parcelableConference.getConnectTimeMillis();
        }
        long j = connectTimeMillis;
        if (parcelableConference.getConnectElapsedTimeMillis() == 0) {
            connectElapsedTimeMillis = this.mClockProxy.elapsedRealtime();
        } else {
            connectElapsedTimeMillis = parcelableConference.getConnectElapsedTimeMillis();
        }
        Call call = new Call(str, this.mContext, this, this.mLock, this.mConnectionServiceRepository, this.mContactsAsyncHelper, this.mCallerInfoAsyncQueryFactory, this.mPhoneNumberUtilsAdapter, null, null, null, phoneAccountHandle, 0, false, true, j, connectElapsedTimeMillis, this.mClockProxy);
        setCallState(call, Call.getStateFromConnectionState(parcelableConference.getState()), "new conference call");
        call.setConnectionCapabilities(parcelableConference.getConnectionCapabilities());
        call.setConnectionProperties(parcelableConference.getConnectionProperties());
        call.setVideoState(parcelableConference.getVideoState());
        call.setVideoProvider(parcelableConference.getVideoProvider());
        call.setStatusHints(parcelableConference.getStatusHints());
        call.putExtras(1, parcelableConference.getExtras());
        Bundle extras = parcelableConference.getExtras();
        if (extras != null && extras.containsKey("android.telecom.extra.ORIGINAL_CONNECTION_ID")) {
            call.setOriginalConnectionId(extras.getString("android.telecom.extra.ORIGINAL_CONNECTION_ID"));
        }
        call.addListener(this);
        addCall(call);
        return call;
    }

    int getCallState() {
        return this.mPhoneStateBroadcaster.getCallState();
    }

    PhoneAccountRegistrar getPhoneAccountRegistrar() {
        return this.mPhoneAccountRegistrar;
    }

    MissedCallNotifier getMissedCallNotifier() {
        return this.mMissedCallNotifier;
    }

    IncomingCallNotifier getIncomingCallNotifier() {
        return this.mIncomingCallNotifier;
    }

    private void rejectCallAndLog(Call call) {
        if (call.getConnectionService() != null) {
            call.reject(false, null);
        } else {
            Log.i(this, "rejectCallAndLog - call already destroyed.", new Object[0]);
        }
        this.mCallLogManager.logCall(call, 3, true);
    }

    @VisibleForTesting
    public void addCall(Call call) {
        Trace.beginSection("addCall");
        Log.v(this, "addCall(%s)", new Object[]{call});
        call.addListener(this);
        this.mCalls.add(call);
        call.getIntentExtras().putLong("android.telecom.extra.CALL_TELECOM_ROUTING_END_TIME_MILLIS", SystemClock.elapsedRealtime());
        updateCanAddCall();
        Iterator<CallsManagerListener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onCallAdded(call);
        }
        if (MtkUtil.isInSingleVideoCallMode(call)) {
            Iterator<Call> it2 = this.mCalls.iterator();
            while (it2.hasNext()) {
                it2.next().refreshConnectionCapabilities();
            }
        }
        Trace.endSection();
    }

    private void removeCall(Call call) {
        Trace.beginSection("removeCall");
        boolean z = true;
        Log.v(this, "removeCall(%s)", new Object[]{call});
        call.setParentAndChildCall(null);
        call.removeListener(this);
        call.clearConnectionService();
        if (this.mCalls.contains(call)) {
            this.mCalls.remove(call);
        } else {
            this.mInCallController.unbindUselessService();
            z = false;
        }
        call.destroy();
        if (z) {
            updateCanAddCall();
            Iterator<CallsManagerListener> it = this.mListeners.iterator();
            while (it.hasNext()) {
                it.next().onCallRemoved(call);
            }
        }
        if (MtkUtil.isInSingleVideoCallMode(call)) {
            Iterator<Call> it2 = this.mCalls.iterator();
            while (it2.hasNext()) {
                it2.next().refreshConnectionCapabilities();
            }
        }
        Trace.endSection();
    }

    private void setCallState(Call call, int i, String str) {
        if (call == null) {
            return;
        }
        int state = call.getState();
        Log.i(this, "setCallState %s -> %s, call: %s", new Object[]{CallState.toString(state), CallState.toString(i), call});
        if (i != state) {
            if (i == 6 && call.isDtmfTonePlaying()) {
                stopDtmfTone(call);
            }
            call.setState(i, str);
            maybeShowErrorDialogOnDisconnect(call);
            Trace.beginSection("onCallStateChanged");
            maybeHandleHandover(call, i);
            if (this.mCalls.contains(call)) {
                updateCanAddCall();
                this.mInCallController.onCallStateChanged(call, state, i);
                for (CallsManagerListener callsManagerListener : this.mListeners) {
                    if (!InCallController.class.getSimpleName().equals(callsManagerListener.getClass().getSimpleName())) {
                        callsManagerListener.onCallStateChanged(call, state, i);
                    }
                }
                ExtensionManager.getRttUtilExt().onCallStateChanged(call, state, i);
            }
            handleActionProcessComplete(call);
            Trace.endSection();
        }
        if (call.isEmergencyCall() && state == 1 && i == 1 && getForegroundCall() != null && getForegroundCall().isEmergencyCall()) {
            this.mCallAudioManager.resetAudioMode();
        }
        if (this.mPendingEccCall != null && this.mNeedDisconnectLater) {
            this.mCalls.remove(this.mPendingEccCall);
            if (!hasConnectingCall(null)) {
                Log.d(this, "Disconnect all call for ECC later", new Object[0]);
                this.mNeedDisconnectLater = false;
                disconnectAllCalls();
            }
            this.mCalls.add(this.mPendingEccCall);
        }
    }

    private void maybeHandleHandover(Call call, int i) {
        if (call.getHandoverSourceCall() != null) {
            if (call.getHandoverState() == 2) {
                if (i == 5) {
                    Log.i(this, "setCallState: handover to accepted", new Object[0]);
                    acceptHandoverTo(call);
                    return;
                } else {
                    if (i == 7) {
                        Log.i(this, "setCallState: handover to rejected", new Object[0]);
                        rejectHandoverTo(call);
                        return;
                    }
                    return;
                }
            }
            return;
        }
        if (call.getHandoverDestinationCall() != null && i == 7) {
            int handoverState = call.getHandoverState();
            if (handoverState == 3) {
                Log.i(this, "setCallState: disconnect before handover accepted", new Object[0]);
                call.getHandoverDestinationCall().sendCallEvent("android.telecom.event.HANDOVER_SOURCE_DISCONNECTED", null);
            } else if (handoverState == 4) {
                Log.i(this, "setCallState: handover from complete", new Object[0]);
                completeHandoverFrom(call);
            }
        }
    }

    private void completeHandoverFrom(Call call) {
        Call handoverDestinationCall = call.getHandoverDestinationCall();
        Log.addEvent(handoverDestinationCall, "HANDOVER_COMPLETE", "from=%s, to=%s", new Object[]{call.getId(), handoverDestinationCall.getId()});
        Log.addEvent(call, "HANDOVER_COMPLETE", "from=%s, to=%s", new Object[]{call.getId(), handoverDestinationCall.getId()});
        call.onConnectionEvent("android.telecom.event.HANDOVER_COMPLETE", null);
        call.onHandoverComplete();
        handoverDestinationCall.sendCallEvent("android.telecom.event.HANDOVER_COMPLETE", null);
        handoverDestinationCall.onHandoverComplete();
        answerCall(handoverDestinationCall, handoverDestinationCall.getVideoState());
        call.markFinishedHandoverStateAndCleanup(5);
        if (handoverDestinationCall.isSelfManaged()) {
            disconnectOtherCalls(handoverDestinationCall.getTargetPhoneAccount());
        }
    }

    private void rejectHandoverTo(Call call) {
        Call handoverSourceCall = call.getHandoverSourceCall();
        Log.i(this, "rejectHandoverTo: from=%s, to=%s", new Object[]{handoverSourceCall.getId(), call.getId()});
        Log.addEvent(handoverSourceCall, "HANDOVER_FAILED", "from=%s, to=%s, rejected", new Object[]{call.getId(), handoverSourceCall.getId()});
        Log.addEvent(call, "HANDOVER_FAILED", "from=%s, to=%s, rejected", new Object[]{call.getId(), handoverSourceCall.getId()});
        handoverSourceCall.onConnectionEvent("android.telecom.event.HANDOVER_FAILED", null);
        handoverSourceCall.onHandoverFailed(3);
        if (call.getConnectionService() != null) {
            call.sendCallEvent("android.telecom.event.HANDOVER_FAILED", null);
            call.getConnectionService().handoverFailed(call, 3);
        }
        call.markFinishedHandoverStateAndCleanup(6);
    }

    private void acceptHandoverTo(Call call) {
        Call handoverSourceCall = call.getHandoverSourceCall();
        Log.i(this, "acceptHandoverTo: from=%s, to=%s", new Object[]{handoverSourceCall.getId(), call.getId()});
        call.setHandoverState(4);
        call.onHandoverComplete();
        handoverSourceCall.setHandoverState(4);
        handoverSourceCall.onHandoverComplete();
        Log.addEvent(call, "ACCEPT_HANDOVER", "from=%s, to=%s", new Object[]{handoverSourceCall.getId(), call.getId()});
        Log.addEvent(handoverSourceCall, "ACCEPT_HANDOVER", "from=%s, to=%s", new Object[]{handoverSourceCall.getId(), call.getId()});
        disconnectCall(handoverSourceCall);
        if (call.isSelfManaged()) {
            disconnectOtherCalls(call.getTargetPhoneAccount());
        }
    }

    private void updateCanAddCall() {
        boolean zCanAddCall = canAddCall();
        if (zCanAddCall != this.mCanAddCall) {
            this.mCanAddCall = zCanAddCall;
            Iterator<CallsManagerListener> it = this.mListeners.iterator();
            while (it.hasNext()) {
                it.next().onCanAddCallChanged(this.mCanAddCall);
            }
        }
    }

    private boolean isPotentialMMICode(Uri uri) {
        return (uri == null || uri.getSchemeSpecificPart() == null || !uri.getSchemeSpecificPart().contains("#")) ? false : true;
    }

    private boolean isPotentialInCallMMICode(Uri uri) {
        if (uri == null || uri.getSchemeSpecificPart() == null || uri.getScheme() == null || !uri.getScheme().equals("tel")) {
            return false;
        }
        String schemeSpecificPart = uri.getSchemeSpecificPart();
        return schemeSpecificPart.equals("0") || (schemeSpecificPart.startsWith("1") && schemeSpecificPart.length() <= 2) || ((schemeSpecificPart.startsWith("2") && schemeSpecificPart.length() <= 2) || schemeSpecificPart.equals("3") || schemeSpecificPart.equals("4") || schemeSpecificPart.equals("5"));
    }

    @VisibleForTesting
    public int getNumCallsWithState(boolean z, Call call, PhoneAccountHandle phoneAccountHandle, int... iArr) {
        return getNumCallsWithState(z ? 1 : 2, call, phoneAccountHandle, iArr);
    }

    @VisibleForTesting
    public int getNumCallsWithState(int i, final Call call, final PhoneAccountHandle phoneAccountHandle, int... iArr) {
        final Set set = (Set) IntStream.of(iArr).boxed().collect(Collectors.toSet());
        Stream<Call> streamFilter = this.mCalls.stream().filter(new Predicate() {
            @Override
            public final boolean test(Object obj) {
                return CallsManager.lambda$getNumCallsWithState$4(set, (Call) obj);
            }
        });
        if (i == 2) {
            streamFilter = streamFilter.filter(new Predicate() {
                @Override
                public final boolean test(Object obj) {
                    return CallsManager.lambda$getNumCallsWithState$5((Call) obj);
                }
            });
        } else if (i == 1) {
            streamFilter = streamFilter.filter(new Predicate() {
                @Override
                public final boolean test(Object obj) {
                    return ((Call) obj).isSelfManaged();
                }
            });
        }
        if (call != null) {
            streamFilter = streamFilter.filter(new Predicate() {
                @Override
                public final boolean test(Object obj) {
                    return CallsManager.lambda$getNumCallsWithState$7(call, (Call) obj);
                }
            });
        }
        if (phoneAccountHandle != null) {
            streamFilter = streamFilter.filter(new Predicate() {
                @Override
                public final boolean test(Object obj) {
                    return phoneAccountHandle.equals(((Call) obj).getTargetPhoneAccount());
                }
            });
        }
        return (int) streamFilter.count();
    }

    static boolean lambda$getNumCallsWithState$4(Set set, Call call) {
        return set.contains(Integer.valueOf(call.getState())) && call.getParentCall() == null && !call.isExternalCall();
    }

    static boolean lambda$getNumCallsWithState$5(Call call) {
        return !call.isSelfManaged();
    }

    static boolean lambda$getNumCallsWithState$7(Call call, Call call2) {
        return call2 != call;
    }

    private boolean hasMaximumLiveCalls(Call call) {
        return 1 <= getNumCallsWithState(3, call, (PhoneAccountHandle) null, LIVE_CALL_STATES);
    }

    private boolean hasMaximumManagedLiveCalls(Call call) {
        return 1 <= getNumCallsWithState(false, call, (PhoneAccountHandle) null, LIVE_CALL_STATES);
    }

    private boolean hasMaximumSelfManagedCalls(Call call, PhoneAccountHandle phoneAccountHandle) {
        return 10 <= getNumCallsWithState(true, call, phoneAccountHandle, ANY_CALL_STATE);
    }

    private boolean hasMaximumManagedHoldingCalls(Call call) {
        return 1 <= getNumCallsWithState(false, call, (PhoneAccountHandle) null, 6);
    }

    private boolean hasMaximumManagedRingingCalls(Call call) {
        return 2 <= getNumCallsWithState(false, call, (PhoneAccountHandle) null, 4);
    }

    private boolean hasMaximumSelfManagedRingingCalls(Call call, PhoneAccountHandle phoneAccountHandle) {
        return 1 <= getNumCallsWithState(true, call, phoneAccountHandle, 4);
    }

    private boolean hasMaximumManagedOutgoingCalls(Call call) {
        return 1 <= getNumCallsWithState(false, call, (PhoneAccountHandle) null, OUTGOING_CALL_STATES);
    }

    private boolean hasMaximumManagedDialingCalls(Call call) {
        return 1 <= getNumCallsWithState(false, call, (PhoneAccountHandle) null, 3, 10);
    }

    public boolean hasCallsForOtherPhoneAccount(PhoneAccountHandle phoneAccountHandle) {
        return getNumCallsForOtherPhoneAccount(phoneAccountHandle) > 0;
    }

    public int getNumCallsForOtherPhoneAccount(final PhoneAccountHandle phoneAccountHandle) {
        return (int) this.mCalls.stream().filter(new Predicate() {
            @Override
            public final boolean test(Object obj) {
                return CallsManager.lambda$getNumCallsForOtherPhoneAccount$9(phoneAccountHandle, (Call) obj);
            }
        }).count();
    }

    static boolean lambda$getNumCallsForOtherPhoneAccount$9(PhoneAccountHandle phoneAccountHandle, Call call) {
        return (phoneAccountHandle.equals(call.getTargetPhoneAccount()) || call.getParentCall() != null || call.isExternalCall()) ? false : true;
    }

    public boolean hasSelfManagedCalls() {
        return this.mCalls.stream().filter(new Predicate() {
            @Override
            public final boolean test(Object obj) {
                return ((Call) obj).isSelfManaged();
            }
        }).count() > 0;
    }

    public boolean hasOngoingCalls() {
        return getNumCallsWithState(3, (Call) null, (PhoneAccountHandle) null, ONGOING_CALL_STATES) > 0;
    }

    public boolean hasOngoingManagedCalls() {
        return getNumCallsWithState(2, (Call) null, (PhoneAccountHandle) null, ONGOING_CALL_STATES) > 0;
    }

    public boolean shouldShowSystemIncomingCallUi(Call call) {
        return call.isIncoming() && call.isSelfManaged() && hasCallsForOtherPhoneAccount(call.getTargetPhoneAccount()) && call.getHandoverSourceCall() == null;
    }

    private boolean makeRoomForOutgoingCall(Call call, boolean z) {
        if (hasNonDisconnectedEmergencyCall(call)) {
            return false;
        }
        if (z) {
            return true;
        }
        if (!call.isCdma() && isPotentialInCallMMICode(call.getHandle())) {
            return true;
        }
        Call dialingCdmaCall = getDialingCdmaCall();
        if (dialingCdmaCall != null) {
            if (z && !dialingCdmaCall.isEmergencyCall()) {
                dialingCdmaCall.disconnect();
                return true;
            }
            Log.d(this, "[makeRoomForOutgoingCall] has dialing CDMA call: %s", new Object[]{dialingCdmaCall.getId()});
            return false;
        }
        if (hasRingingCall() && !z) {
            Log.d(this, "can not start outgoing call, have ringing call.", new Object[0]);
            return false;
        }
        if (MtkUtil.isInSingleVideoCallMode(call) && shouldBlockCallUnderSingleVideo(call)) {
            Log.i("CallsManager", "[makeRoomForOutgoingCall] no new outgoing call allowed, video call or other diff video state call exists", new Object[0]);
            return false;
        }
        if (!hasMaximumLiveCalls(call)) {
            return true;
        }
        Call firstCallWithState = getFirstCallWithState(call, LIVE_CALL_STATES);
        Call firstCallWithState2 = getFirstCallWithState(call, OUTGOING_CALL_STATES);
        if (firstCallWithState2 == null) {
            firstCallWithState2 = getDialingCdmaCall();
        }
        if (firstCallWithState2 != null) {
            if (z && !firstCallWithState2.isEmergencyCall()) {
                call.getAnalytics().setCallIsAdditional(true);
                firstCallWithState2.getAnalytics().setCallIsInterrupted(true);
                firstCallWithState2.disconnect();
                return true;
            }
            if (firstCallWithState2.getState() != 2) {
                return false;
            }
            call.getAnalytics().setCallIsAdditional(true);
            firstCallWithState2.getAnalytics().setCallIsInterrupted(true);
            firstCallWithState2.disconnect();
            return true;
        }
        if (hasMaximumManagedHoldingCalls(call) && z && !canHold(firstCallWithState)) {
            call.getAnalytics().setCallIsAdditional(true);
            firstCallWithState.getAnalytics().setCallIsInterrupted(true);
            firstCallWithState.disconnect("disconnecting to make room for emergency call " + call.getId());
            return true;
        }
        PhoneAccountHandle targetPhoneAccount = firstCallWithState.getTargetPhoneAccount();
        if (targetPhoneAccount == null && firstCallWithState.isConference() && !firstCallWithState.getChildCalls().isEmpty()) {
            targetPhoneAccount = getFirstChildPhoneAccount(firstCallWithState);
            Log.i(this, "makeRoomForOutgoingCall: using child call PhoneAccount = " + targetPhoneAccount, new Object[0]);
        }
        if (Objects.equals(targetPhoneAccount, call.getTargetPhoneAccount())) {
            Log.i(this, "makeRoomForOutgoingCall: phoneAccount matches.", new Object[0]);
            call.getAnalytics().setCallIsAdditional(true);
            firstCallWithState.getAnalytics().setCallIsInterrupted(true);
            return true;
        }
        if (call.getTargetPhoneAccount() == null) {
            return true;
        }
        if (!canHold(firstCallWithState)) {
            return false;
        }
        Log.i(this, "makeRoomForOutgoingCall: holding live call.", new Object[0]);
        call.getAnalytics().setCallIsAdditional(true);
        firstCallWithState.getAnalytics().setCallIsInterrupted(true);
        firstCallWithState.hold("calling " + call.getId());
        return true;
    }

    private PhoneAccountHandle getFirstChildPhoneAccount(Call call) {
        Iterator<Call> it = call.getChildCalls().iterator();
        while (it.hasNext()) {
            PhoneAccountHandle targetPhoneAccount = it.next().getTargetPhoneAccount();
            if (targetPhoneAccount != null) {
                return targetPhoneAccount;
            }
        }
        return null;
    }

    private void maybeMoveToSpeakerPhone(Call call) {
        if ((!call.isHandoverInProgress() || call.getState() != 3) && call.getStartWithSpeakerphoneOn()) {
            setAudioRoute(8, null);
            call.setStartWithSpeakerphoneOn(false);
        }
    }

    private void maybeTurnOffMute(Call call) {
        if (call.isEmergencyCall()) {
            mute(false);
        }
    }

    private void ensureCallAudible() {
        AudioManager audioManager = (AudioManager) this.mContext.getSystemService(AudioManager.class);
        if (audioManager == null) {
            Log.w(this, "ensureCallAudible: audio manager is null", new Object[0]);
        } else if (audioManager.getStreamVolume(0) == 0) {
            Log.i(this, "ensureCallAudible: voice call stream has volume 0. Adjusting to default.", new Object[0]);
            audioManager.setStreamVolume(0, AudioSystem.getDefaultStreamVolume(0), 0);
        }
    }

    Call createCallForExistingConnection(String str, ParcelableConnection parcelableConnection) {
        Call call = new Call(str, this.mContext, this, this.mLock, this.mConnectionServiceRepository, this.mContactsAsyncHelper, this.mCallerInfoAsyncQueryFactory, this.mPhoneNumberUtilsAdapter, parcelableConnection.getHandle(), null, null, parcelableConnection.getPhoneAccount(), 0, false, (parcelableConnection.getConnectionProperties() & 64) != 0, parcelableConnection.getConnectTimeMillis(), parcelableConnection.getConnectElapsedTimeMillis(), this.mClockProxy);
        call.initAnalytics();
        call.getAnalytics().setCreatedFromExistingConnection(true);
        setCallState(call, Call.getStateFromConnectionState(parcelableConnection.getState()), "existing connection");
        call.setConnectionCapabilities(parcelableConnection.getConnectionCapabilities());
        call.setConnectionProperties(parcelableConnection.getConnectionProperties());
        call.setHandle(parcelableConnection.getHandle(), parcelableConnection.getHandlePresentation());
        call.setCallerDisplayName(parcelableConnection.getCallerDisplayName(), parcelableConnection.getCallerDisplayNamePresentation());
        call.addListener(this);
        Bundle extras = parcelableConnection.getExtras();
        if (extras != null && extras.containsKey("android.telecom.extra.ORIGINAL_CONNECTION_ID")) {
            call.setOriginalConnectionId(extras.getString("android.telecom.extra.ORIGINAL_CONNECTION_ID"));
        }
        Log.i(this, "createCallForExistingConnection: %s", new Object[]{parcelableConnection});
        Call callOrElse = null;
        if (!TextUtils.isEmpty(parcelableConnection.getParentCallId())) {
            final String parentCallId = parcelableConnection.getParentCallId();
            callOrElse = this.mCalls.stream().filter(new Predicate() {
                @Override
                public final boolean test(Object obj) {
                    return ((Call) obj).getId().equals(parentCallId);
                }
            }).findFirst().orElse(null);
            if (callOrElse != null) {
                Log.i(this, "createCallForExistingConnection: %s added as child of %s.", new Object[]{call.getId(), callOrElse.getId()});
                call.setParentCall(callOrElse);
            }
        }
        if (call.isEmergencyCall() && getForegroundCall() != null && getForegroundCall().isEmergencyCall()) {
            this.mCallAudioManager.resetAudioMode();
        }
        addCall(call);
        if (callOrElse != null) {
            call.setChildOf(callOrElse);
            call.notifyParentChanged(callOrElse);
        }
        return call;
    }

    Call getAlreadyAddedConnection(final String str) {
        Optional<Call> optionalFindFirst = this.mCalls.stream().filter(new Predicate() {
            @Override
            public final boolean test(Object obj) {
                return CallsManager.lambda$getAlreadyAddedConnection$13(str, (Call) obj);
            }
        }).findFirst();
        if (optionalFindFirst.isPresent()) {
            Log.i(this, "isExistingConnectionAlreadyAdded - call %s already added with id %s", new Object[]{str, optionalFindFirst.get().getId()});
            return optionalFindFirst.get();
        }
        return null;
    }

    static boolean lambda$getAlreadyAddedConnection$13(String str, Call call) {
        return str.equals(call.getOriginalConnectionId()) || str.equals(call.getId());
    }

    private String getNextCallId() {
        String string;
        synchronized (this.mLock) {
            StringBuilder sb = new StringBuilder();
            sb.append("TC@");
            int i = this.mCallId + 1;
            this.mCallId = i;
            sb.append(i);
            string = sb.toString();
        }
        return string;
    }

    public int getNextRttRequestId() {
        int i;
        synchronized (this.mLock) {
            i = this.mRttRequestId + 1;
            this.mRttRequestId = i;
        }
        return i;
    }

    @VisibleForTesting
    public void onUserSwitch(UserHandle userHandle) {
        this.mCurrentUserHandle = userHandle;
        this.mMissedCallNotifier.setCurrentUserHandle(userHandle);
        Iterator it = UserManager.get(this.mContext).getEnabledProfiles(userHandle.getIdentifier()).iterator();
        while (it.hasNext()) {
            reloadMissedCallsOfUser(((UserInfo) it.next()).getUserHandle());
        }
    }

    void onUserStarting(UserHandle userHandle) {
        if (UserUtil.isProfile(this.mContext, userHandle)) {
            reloadMissedCallsOfUser(userHandle);
        }
    }

    public TelecomSystem.SyncRoot getLock() {
        return this.mLock;
    }

    private void reloadMissedCallsOfUser(UserHandle userHandle) {
        this.mMissedCallNotifier.reloadFromDatabase(this.mCallerInfoLookupHelper, new MissedCallNotifier.CallInfoFactory(), userHandle);
    }

    public void onBootCompleted() {
        this.mMissedCallNotifier.reloadAfterBootComplete(this.mCallerInfoLookupHelper, new MissedCallNotifier.CallInfoFactory());
        this.mIsCallRecorderSupported = this.mCallRecorderManager.isCallRecorderInstalled();
    }

    public boolean isIncomingCallPermitted(PhoneAccountHandle phoneAccountHandle) {
        return isIncomingCallPermitted(null, phoneAccountHandle);
    }

    public boolean isIncomingCallPermitted(Call call, PhoneAccountHandle phoneAccountHandle) {
        PhoneAccount phoneAccountUnchecked;
        if (phoneAccountHandle == null || (phoneAccountUnchecked = this.mPhoneAccountRegistrar.getPhoneAccountUnchecked(phoneAccountHandle)) == null) {
            return false;
        }
        if (MtkUtil.isInSingleVideoCallMode(call) && shouldBlockCallUnderSingleVideo(call)) {
            Log.i("CallsManager", "[isIncomingCallPermitted] Already has video call. or other call in the same phone account with differ video state", new Object[0]);
            return false;
        }
        if (hasModifyingVideoSessionCall()) {
            Log.i("CallsManager", "[isIncomingCallPermitted] hasVideoSessionChangingCall", new Object[0]);
            return false;
        }
        if (!MtkUtil.isInDsdaMode() || this.mBluetoothRouteManager.isBluetoothAvailable() || phoneAccountUnchecked.isSelfManaged()) {
            return !phoneAccountUnchecked.isSelfManaged() ? (hasMaximumManagedRingingCalls(call) || hasMaximumManagedHoldingCalls(call)) ? false : true : (hasEmergencyCall() || hasMaximumSelfManagedRingingCalls(call, phoneAccountHandle) || hasMaximumSelfManagedCalls(call, phoneAccountHandle)) ? false : true;
        }
        boolean zHasMaximumDsdaRingingCalls = hasMaximumDsdaRingingCalls(phoneAccountHandle);
        boolean zHasMaximumManagedHoldingCalls = hasMaximumManagedHoldingCalls(call);
        Log.d("CallsManager", "[isIncomingCallPermitted] has max ringing: %s, has max hold: %s", new Object[]{Boolean.valueOf(zHasMaximumDsdaRingingCalls), Boolean.valueOf(zHasMaximumManagedHoldingCalls)});
        return (zHasMaximumDsdaRingingCalls || zHasMaximumManagedHoldingCalls) ? false : true;
    }

    public boolean isOutgoingCallPermitted(PhoneAccountHandle phoneAccountHandle) {
        return isOutgoingCallPermitted(null, phoneAccountHandle);
    }

    public boolean isOutgoingCallPermitted(Call call, PhoneAccountHandle phoneAccountHandle) {
        PhoneAccount phoneAccountUnchecked;
        if (phoneAccountHandle == null || (phoneAccountUnchecked = this.mPhoneAccountRegistrar.getPhoneAccountUnchecked(phoneAccountHandle)) == null) {
            return false;
        }
        if (!phoneAccountUnchecked.isSelfManaged()) {
            return (hasMaximumManagedOutgoingCalls(call) || hasMaximumManagedDialingCalls(call) || hasMaximumManagedLiveCalls(call) || hasMaximumManagedHoldingCalls(call)) ? false : true;
        }
        Call call2 = (Call) this.mConnectionSvrFocusMgr.getCurrentFocusCall();
        if (hasEmergencyCall()) {
            return false;
        }
        if (call == null || call.getHandoverSourceCall() == null) {
            if (hasMaximumSelfManagedCalls(call, phoneAccountHandle)) {
                return false;
            }
            if (call2 != null && !canHold(call2)) {
                return false;
            }
        }
        return true;
    }

    public boolean isReplyWithSmsAllowed(int i) {
        UserHandle userHandleOf = UserHandle.of(UserHandle.getUserId(i));
        UserManager userManager = (UserManager) this.mContext.getSystemService(UserManager.class);
        KeyguardManager keyguardManager = (KeyguardManager) this.mContext.getSystemService(KeyguardManager.class);
        boolean z = userManager != null && userManager.hasUserRestriction("no_sms", userHandleOf);
        Log.d(this, "isReplyWithSmsAllowed: isUserRestricted: %s, isLockscreenRestricted: %s", new Object[]{Boolean.valueOf(z), Boolean.valueOf(keyguardManager != null && keyguardManager.isDeviceLocked())});
        return !z;
    }

    public void waitOnHandlers() {
        final CountDownLatch countDownLatch = new CountDownLatch(3);
        this.mHandler.post(new Runnable() {
            @Override
            public final void run() {
                countDownLatch.countDown();
            }
        });
        this.mCallAudioManager.getCallAudioModeStateMachine().getHandler().post(new Runnable() {
            @Override
            public final void run() {
                countDownLatch.countDown();
            }
        });
        this.mCallAudioManager.getCallAudioRouteStateMachine().getHandler().post(new Runnable() {
            @Override
            public final void run() {
                countDownLatch.countDown();
            }
        });
        try {
            countDownLatch.await(10000L, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Log.w(this, "waitOnHandlers: interrupted %s", new Object[]{e});
        }
    }

    public void confirmPendingCall(String str) {
        Log.i(this, "confirmPendingCall: callId=%s", new Object[]{str});
        if (this.mPendingCall != null && this.mPendingCall.getId().equals(str)) {
            Log.addEvent(this.mPendingCall, "USER_CONFIRMED");
            addCall(this.mPendingCall);
            disconnectSelfManagedCalls("outgoing call " + str);
            CallIntentProcessor.sendNewOutgoingCallIntent(this.mContext, this.mPendingCall, this, this.mPendingCall.getOriginalCallIntent());
            this.mPendingCall = null;
        }
    }

    public void cancelPendingCall(String str) {
        Log.i(this, "cancelPendingCall: callId=%s", new Object[]{str});
        if (this.mPendingCall != null && this.mPendingCall.getId().equals(str)) {
            Log.addEvent(this.mPendingCall, "USER_CANCELLED");
            markCallAsDisconnected(this.mPendingCall, new DisconnectCause(4));
            markCallAsRemoved(this.mPendingCall);
            this.mPendingCall = null;
        }
    }

    private void startCallConfirmation(Call call) {
        if (this.mPendingCall != null) {
            Log.i(this, "startCallConfirmation: call %s is already pending; disconnecting %s", new Object[]{this.mPendingCall.getId(), call.getId()});
            markCallDisconnectedDueToSelfManagedCall(call);
            return;
        }
        Log.addEvent(call, "USER_CONFIRMATION");
        this.mPendingCall = call;
        Call call2 = (Call) this.mConnectionSvrFocusMgr.getCurrentFocusCall();
        if (call2 != null) {
            CharSequence targetPhoneAccountLabel = call2.getTargetPhoneAccountLabel();
            Log.i(this, "startCallConfirmation: callId=%s, ongoingApp=%s", new Object[]{call.getId(), targetPhoneAccountLabel});
            Intent intent = new Intent(this.mContext, (Class<?>) ConfirmCallDialogActivity.class);
            intent.putExtra("android.telecom.extra.OUTGOING_CALL_ID", call.getId());
            intent.putExtra("android.telecom.extra.ONGOING_APP_NAME", targetPhoneAccountLabel);
            intent.setFlags(268435456);
            this.mContext.startActivityAsUser(intent, UserHandle.CURRENT);
        }
    }

    private void disconnectSelfManagedCalls(final String str) {
        this.mCalls.stream().filter(new Predicate() {
            @Override
            public final boolean test(Object obj) {
                return ((Call) obj).isSelfManaged();
            }
        }).forEach(new Consumer() {
            @Override
            public final void accept(Object obj) {
                ((Call) obj).disconnect(str);
            }
        });
        this.mCallAudioManager.switchBaseline();
    }

    public void dump(IndentingPrintWriter indentingPrintWriter) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.DUMP", "CallsManager");
        if (this.mCalls != null) {
            indentingPrintWriter.println("mCalls: ");
            indentingPrintWriter.increaseIndent();
            Iterator<Call> it = this.mCalls.iterator();
            while (it.hasNext()) {
                indentingPrintWriter.println(it.next());
            }
            indentingPrintWriter.decreaseIndent();
        }
        if (this.mPendingCall != null) {
            indentingPrintWriter.print("mPendingCall:");
            indentingPrintWriter.println(this.mPendingCall.getId());
        }
        if (this.mCallAudioManager != null) {
            indentingPrintWriter.println("mCallAudioManager:");
            indentingPrintWriter.increaseIndent();
            this.mCallAudioManager.dump(indentingPrintWriter);
            indentingPrintWriter.decreaseIndent();
        }
        if (this.mTtyManager != null) {
            indentingPrintWriter.println("mTtyManager:");
            indentingPrintWriter.increaseIndent();
            this.mTtyManager.dump(indentingPrintWriter);
            indentingPrintWriter.decreaseIndent();
        }
        if (this.mInCallController != null) {
            indentingPrintWriter.println("mInCallController:");
            indentingPrintWriter.increaseIndent();
            this.mInCallController.dump(indentingPrintWriter);
            indentingPrintWriter.decreaseIndent();
        }
        if (this.mDefaultDialerCache != null) {
            indentingPrintWriter.println("mDefaultDialerCache:");
            indentingPrintWriter.increaseIndent();
            this.mDefaultDialerCache.dumpCache(indentingPrintWriter);
            indentingPrintWriter.decreaseIndent();
        }
        if (this.mConnectionServiceRepository != null) {
            indentingPrintWriter.println("mConnectionServiceRepository:");
            indentingPrintWriter.increaseIndent();
            this.mConnectionServiceRepository.dump(indentingPrintWriter);
            indentingPrintWriter.decreaseIndent();
        }
    }

    private void maybeShowErrorDialogOnDisconnect(Call call) {
        if (call.getState() == 7) {
            if ((isPotentialMMICode(call.getHandle()) || isPotentialInCallMMICode(call.getHandle())) && !this.mCalls.contains(call)) {
                DisconnectCause disconnectCause = call.getDisconnectCause();
                if (!TextUtils.isEmpty(disconnectCause.getDescription()) && disconnectCause.getCode() == 1) {
                    Intent intent = new Intent(this.mContext, (Class<?>) ErrorDialogActivity.class);
                    intent.putExtra("error_message_string", disconnectCause.getDescription());
                    intent.setFlags(268435456);
                    this.mContext.startActivityAsUser(intent, UserHandle.CURRENT);
                    Log.i(this, "set description is null to ensure not show dialog in ui again", new Object[0]);
                    call.setDisconnectCause(new DisconnectCause(disconnectCause.getCode(), disconnectCause.getLabel(), null, disconnectCause.getReason(), disconnectCause.getTone()));
                }
            }
        }
    }

    private void setIntentExtrasAndStartTime(Call call, Bundle bundle) {
        Bundle bundle2;
        if (bundle != null) {
            bundle2 = new Bundle(bundle);
        } else {
            bundle2 = new Bundle();
        }
        bundle2.putLong("android.telecom.extra.CALL_TELECOM_ROUTING_START_TIME_MILLIS", SystemClock.elapsedRealtime());
        call.setIntentExtras(bundle2);
    }

    private void notifyCreateConnectionFailed(PhoneAccountHandle phoneAccountHandle, Call call) {
        if (phoneAccountHandle == null) {
            return;
        }
        ConnectionServiceWrapper service = this.mConnectionServiceRepository.getService(phoneAccountHandle.getComponentName(), phoneAccountHandle.getUserHandle());
        if (service == null) {
            Log.i(this, "Found no connection service.", new Object[0]);
        } else {
            call.setConnectionService(service);
            service.createConnectionFailed(call);
        }
    }

    private void notifyHandoverFailed(Call call, int i) {
        call.getConnectionService().handoverFailed(call, i);
        call.setDisconnectCause(new DisconnectCause(4));
        call.disconnect("handover failed");
    }

    private void requestHandoverViaEvents(Call call, PhoneAccountHandle phoneAccountHandle, int i, Bundle bundle) {
        boolean zIsHandoverFromPhoneAccountSupported = isHandoverFromPhoneAccountSupported(call.getTargetPhoneAccount());
        boolean zIsHandoverToPhoneAccountSupported = isHandoverToPhoneAccountSupported(phoneAccountHandle);
        if (!zIsHandoverFromPhoneAccountSupported || !zIsHandoverToPhoneAccountSupported || hasEmergencyCall()) {
            call.sendCallEvent("android.telecom.event.HANDOVER_FAILED", null);
            return;
        }
        Log.addEvent(call, "HANDOVER_REQUEST", phoneAccountHandle);
        Bundle bundle2 = new Bundle();
        bundle2.putBoolean("android.telecom.extra.IS_HANDOVER", true);
        bundle2.putParcelable("android.telecom.extra.HANDOVER_FROM_PHONE_ACCOUNT", call.getTargetPhoneAccount());
        bundle2.putInt("android.telecom.extra.START_CALL_WITH_VIDEO_STATE", i);
        if (bundle != null) {
            bundle2.putAll(bundle);
        }
        bundle2.putParcelable("android.telecom.extra.CALL_AUDIO_STATE", this.mCallAudioManager.getCallAudioState());
        Call callStartOutgoingCall = startOutgoingCall(call.getHandle(), phoneAccountHandle, bundle2, getCurrentUserHandle(), null);
        Log.addEvent(call, "START_HANDOVER", "handOverFrom=%s, handOverTo=%s", new Object[]{call.getId(), callStartOutgoingCall.getId()});
        call.setHandoverDestinationCall(callStartOutgoingCall);
        call.setHandoverState(3);
        callStartOutgoingCall.setHandoverState(2);
        callStartOutgoingCall.setHandoverSourceCall(call);
        callStartOutgoingCall.setNewOutgoingCallIntentBroadcastIsDone();
        placeOutgoingCall(callStartOutgoingCall, callStartOutgoingCall.getHandle(), null, false, i);
    }

    private void requestHandover(Call call, PhoneAccountHandle phoneAccountHandle, int i, Bundle bundle) {
        int i2;
        PhoneAccount phoneAccount;
        char c;
        int i3;
        CallsManager callsManager;
        Bundle bundle2;
        boolean z;
        if (hasEmergencyCall()) {
            call.onHandoverFailed(4);
            return;
        }
        boolean zIsHandoverFromPhoneAccountSupported = isHandoverFromPhoneAccountSupported(call.getTargetPhoneAccount());
        boolean zIsHandoverToPhoneAccountSupported = isHandoverToPhoneAccountSupported(phoneAccountHandle);
        if (!zIsHandoverFromPhoneAccountSupported || !zIsHandoverToPhoneAccountSupported) {
            call.onHandoverFailed(2);
            return;
        }
        Log.addEvent(call, "HANDOVER_REQUEST", phoneAccountHandle);
        PhoneAccount phoneAccount2 = this.mPhoneAccountRegistrar.getPhoneAccount(phoneAccountHandle, getCurrentUserHandle());
        boolean z2 = phoneAccount2 != null && phoneAccount2.isSelfManaged();
        Call call2 = new Call(getNextCallId(), this.mContext, this, this.mLock, this.mConnectionServiceRepository, this.mContactsAsyncHelper, this.mCallerInfoAsyncQueryFactory, this.mPhoneNumberUtilsAdapter, call.getHandle(), null, null, null, 1, false, false, this.mClockProxy);
        call2.initAnalytics();
        call2.setIsSelfManaged(z2);
        if (z2) {
            i2 = 1;
            call2.setIsVoipAudioMode(true);
        } else {
            i2 = 1;
        }
        call2.setInitiatingUser(getCurrentUserHandle());
        if (VideoProfile.isVideo(i)) {
            phoneAccount = phoneAccount2;
            if (phoneAccount != null && !phoneAccount.hasCapabilities(8)) {
                c = 0;
                call2.setVideoState(0);
                i3 = i;
            }
            call2.setTargetPhoneAccount(phoneAccountHandle);
            if (phoneAccount == null && phoneAccount.getExtras() != null && phoneAccount.getExtras().getBoolean("android.telecom.extra.ALWAYS_USE_VOIP_AUDIO_MODE")) {
                Object[] objArr = new Object[i2];
                objArr[c] = call2.getId();
                CallsManager callsManager2 = this;
                Log.d(callsManager2, "requestHandover: defaulting to voip mode for call %s", objArr);
                call2.setIsVoipAudioMode(i2);
                callsManager = callsManager2;
            } else {
                callsManager = this;
            }
            call2.setState(i2, phoneAccountHandle != null ? "no-handle" : phoneAccountHandle.toString());
            if (bundle != null) {
                bundle2 = new Bundle();
            } else {
                bundle2 = bundle;
            }
            bundle2.putBoolean("android.telecom.extra.IS_HANDOVER_CONNECTION", i2);
            bundle2.putParcelable("android.telecom.extra.HANDOVER_FROM_PHONE_ACCOUNT", call.getTargetPhoneAccount());
            callsManager.setIntentExtrasAndStartTime(call2, bundle2);
            if (!callsManager.mCalls.contains(call2)) {
                callsManager.addCall(call2);
            }
            Object[] objArr2 = new Object[2];
            objArr2[c] = call.getId();
            objArr2[i2] = call2.getId();
            Log.addEvent(call, "START_HANDOVER", "handOverFrom=%s, handOverTo=%s", objArr2);
            call.setHandoverDestinationCall(call2);
            call.setHandoverState(3);
            call2.setHandoverState(2);
            call2.setHandoverSourceCall(call);
            call2.setNewOutgoingCallIntentBroadcastIsDone();
            boolean z3 = callsManager.mContext.getResources().getBoolean(R.bool.use_speaker_when_docked);
            boolean zIsSpeakerphoneEnabledForDock = isSpeakerphoneEnabledForDock();
            z = i2;
            z = i2;
            if (!callsManager.isSpeakerphoneAutoEnabledForVideoCalls(i3) && (!z3 || !zIsSpeakerphoneEnabledForDock)) {
                z = c;
            }
            call2.setStartWithSpeakerphoneOn(z);
            call2.setVideoState(i3);
            boolean zIsOutgoingCallPermitted = callsManager.isOutgoingCallPermitted(call2, call2.getTargetPhoneAccount());
            if (!call2.isSelfManaged() && !zIsOutgoingCallPermitted) {
                callsManager.notifyCreateConnectionFailed(call2.getTargetPhoneAccount(), call2);
                return;
            }
            if (call2.isSelfManaged() && hasSelfManagedCalls() && !call2.isEmergencyCall()) {
                callsManager.markCallDisconnectedDueToSelfManagedCall(call2);
                return;
            }
            if (call2.isEmergencyCall()) {
                callsManager.disconnectSelfManagedCalls("emergency call");
            }
            call2.startCreateConnection(callsManager.mPhoneAccountRegistrar);
        }
        phoneAccount = phoneAccount2;
        c = 0;
        i3 = i;
        call2.setVideoState(i3);
        call2.setTargetPhoneAccount(phoneAccountHandle);
        if (phoneAccount == null) {
            callsManager = this;
        }
        call2.setState(i2, phoneAccountHandle != null ? "no-handle" : phoneAccountHandle.toString());
        if (bundle != null) {
        }
        bundle2.putBoolean("android.telecom.extra.IS_HANDOVER_CONNECTION", i2);
        bundle2.putParcelable("android.telecom.extra.HANDOVER_FROM_PHONE_ACCOUNT", call.getTargetPhoneAccount());
        callsManager.setIntentExtrasAndStartTime(call2, bundle2);
        if (!callsManager.mCalls.contains(call2)) {
        }
        Object[] objArr22 = new Object[2];
        objArr22[c] = call.getId();
        objArr22[i2] = call2.getId();
        Log.addEvent(call, "START_HANDOVER", "handOverFrom=%s, handOverTo=%s", objArr22);
        call.setHandoverDestinationCall(call2);
        call.setHandoverState(3);
        call2.setHandoverState(2);
        call2.setHandoverSourceCall(call);
        call2.setNewOutgoingCallIntentBroadcastIsDone();
        boolean z32 = callsManager.mContext.getResources().getBoolean(R.bool.use_speaker_when_docked);
        boolean zIsSpeakerphoneEnabledForDock2 = isSpeakerphoneEnabledForDock();
        z = i2;
        z = i2;
        if (!callsManager.isSpeakerphoneAutoEnabledForVideoCalls(i3)) {
            z = c;
        }
        call2.setStartWithSpeakerphoneOn(z);
        call2.setVideoState(i3);
        boolean zIsOutgoingCallPermitted2 = callsManager.isOutgoingCallPermitted(call2, call2.getTargetPhoneAccount());
        if (!call2.isSelfManaged()) {
        }
        if (call2.isSelfManaged()) {
        }
        if (call2.isEmergencyCall()) {
        }
        call2.startCreateConnection(callsManager.mPhoneAccountRegistrar);
    }

    private boolean isHandoverFromPhoneAccountSupported(PhoneAccountHandle phoneAccountHandle) {
        return getBooleanPhoneAccountExtra(phoneAccountHandle, "android.telecom.extra.SUPPORTS_HANDOVER_FROM");
    }

    private boolean isHandoverToPhoneAccountSupported(PhoneAccountHandle phoneAccountHandle) {
        return getBooleanPhoneAccountExtra(phoneAccountHandle, "android.telecom.extra.SUPPORTS_HANDOVER_TO");
    }

    private boolean getBooleanPhoneAccountExtra(PhoneAccountHandle phoneAccountHandle, String str) {
        Bundle extras;
        PhoneAccount phoneAccountUnchecked = getPhoneAccountRegistrar().getPhoneAccountUnchecked(phoneAccountHandle);
        if (phoneAccountUnchecked == null || (extras = phoneAccountUnchecked.getExtras()) == null) {
            return false;
        }
        return extras.getBoolean(str);
    }

    private boolean isHandoverInProgress() {
        return this.mCalls.stream().filter(new Predicate() {
            @Override
            public final boolean test(Object obj) {
                return CallsManager.lambda$isHandoverInProgress$19((Call) obj);
            }
        }).count() > 0;
    }

    static boolean lambda$isHandoverInProgress$19(Call call) {
        return (call.getHandoverSourceCall() == null && call.getHandoverDestinationCall() == null) ? false : true;
    }

    private void broadcastUnregisterIntent(PhoneAccountHandle phoneAccountHandle) {
        Intent intent = new Intent("android.telecom.action.PHONE_ACCOUNT_UNREGISTERED");
        intent.addFlags(16777216);
        intent.putExtra("android.telecom.extra.PHONE_ACCOUNT_HANDLE", phoneAccountHandle);
        Log.i(this, "Sending phone-account %s unregistered intent as user", new Object[]{phoneAccountHandle});
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL, "android.permission.PROCESS_PHONE_ACCOUNT_REGISTRATION");
        String defaultDialerApplication = this.mDefaultDialerCache.getDefaultDialerApplication(getCurrentUserHandle().getIdentifier());
        if (!TextUtils.isEmpty(defaultDialerApplication)) {
            Intent intent2 = new Intent("android.telecom.action.PHONE_ACCOUNT_UNREGISTERED").setPackage(defaultDialerApplication);
            intent2.putExtra("android.telecom.extra.PHONE_ACCOUNT_HANDLE", phoneAccountHandle);
            Log.i(this, "Sending phone-account unregistered intent to default dialer", new Object[0]);
            this.mContext.sendBroadcastAsUser(intent2, UserHandle.ALL, null);
        }
    }

    private void broadcastRegisterIntent(PhoneAccountHandle phoneAccountHandle) {
        Intent intent = new Intent("android.telecom.action.PHONE_ACCOUNT_REGISTERED");
        intent.addFlags(16777216);
        intent.putExtra("android.telecom.extra.PHONE_ACCOUNT_HANDLE", phoneAccountHandle);
        Log.i(this, "Sending phone-account %s registered intent as user", new Object[]{phoneAccountHandle});
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL, "android.permission.PROCESS_PHONE_ACCOUNT_REGISTRATION");
        String defaultDialerApplication = this.mDefaultDialerCache.getDefaultDialerApplication(getCurrentUserHandle().getIdentifier());
        if (!TextUtils.isEmpty(defaultDialerApplication)) {
            Intent intent2 = new Intent("android.telecom.action.PHONE_ACCOUNT_REGISTERED").setPackage(defaultDialerApplication);
            intent2.putExtra("android.telecom.extra.PHONE_ACCOUNT_HANDLE", phoneAccountHandle);
            Log.i(this, "Sending phone-account registered intent to default dialer", new Object[0]);
            this.mContext.sendBroadcastAsUser(intent2, UserHandle.ALL, null);
        }
    }

    public void acceptHandover(Uri uri, int i, PhoneAccountHandle phoneAccountHandle) {
        CallsManager callsManager;
        final String schemeSpecificPart = uri.getSchemeSpecificPart();
        Call callOrElse = this.mCalls.stream().filter(new Predicate() {
            @Override
            public final boolean test(Object obj) {
                Call call = (Call) obj;
                return this.f$0.mPhoneNumberUtilsAdapter.isSamePhoneNumber(call.getHandle() == null ? null : call.getHandle().getSchemeSpecificPart(), schemeSpecificPart);
            }
        }).findFirst().orElse(null);
        Call call = new Call(getNextCallId(), this.mContext, this, this.mLock, this.mConnectionServiceRepository, this.mContactsAsyncHelper, this.mCallerInfoAsyncQueryFactory, this.mPhoneNumberUtilsAdapter, uri, null, null, phoneAccountHandle, 2, false, false, this.mClockProxy);
        if (callOrElse != null && !isHandoverInProgress()) {
            callsManager = this;
            if (callsManager.isHandoverFromPhoneAccountSupported(callOrElse.getTargetPhoneAccount()) && callsManager.isHandoverToPhoneAccountSupported(phoneAccountHandle) && !hasEmergencyCall()) {
                PhoneAccount phoneAccountUnchecked = callsManager.mPhoneAccountRegistrar.getPhoneAccountUnchecked(phoneAccountHandle);
                if (phoneAccountUnchecked == null) {
                    Log.w(callsManager, "acceptHandover: Handover not supported. phoneAccount = null", new Object[0]);
                    callsManager.notifyHandoverFailed(call, 2);
                    return;
                }
                call.setIsSelfManaged(phoneAccountUnchecked.isSelfManaged());
                if (call.isSelfManaged() || (phoneAccountUnchecked.getExtras() != null && phoneAccountUnchecked.getExtras().getBoolean("android.telecom.extra.ALWAYS_USE_VOIP_AUDIO_MODE"))) {
                    call.setIsVoipAudioMode(true);
                }
                if (phoneAccountUnchecked.hasCapabilities(8)) {
                    call.setVideoState(i);
                } else {
                    call.setVideoState(0);
                }
                call.initAnalytics();
                call.addListener(callsManager);
                callOrElse.setHandoverDestinationCall(call);
                call.setHandoverSourceCall(callOrElse);
                call.setHandoverState(2);
                callOrElse.setHandoverState(3);
                if (isSpeakerEnabledForVideoCalls() && VideoProfile.isVideo(i)) {
                    call.setStartWithSpeakerphoneOn(true);
                }
                Bundle intentExtras = call.getIntentExtras();
                if (intentExtras == null) {
                    intentExtras = new Bundle();
                }
                intentExtras.putBoolean("android.telecom.extra.IS_HANDOVER_CONNECTION", true);
                intentExtras.putParcelable("android.telecom.extra.HANDOVER_FROM_PHONE_ACCOUNT", callOrElse.getTargetPhoneAccount());
                call.startCreateConnection(callsManager.mPhoneAccountRegistrar);
                return;
            }
        } else {
            callsManager = this;
        }
        Log.w(callsManager, "acceptHandover: Handover not supported", new Object[0]);
        callsManager.notifyHandoverFailed(call, 2);
    }

    ConnectionServiceFocusManager getConnectionServiceFocusManager() {
        return this.mConnectionSvrFocusMgr;
    }

    private boolean canHold(Call call) {
        return call.can(1);
    }

    private boolean supportsHold(Call call) {
        return call.can(2);
    }

    private final class ActionSetCallState implements PendingAction {
        private final Call mCall;
        private final int mState;
        private final String mTag;

        ActionSetCallState(Call call, int i, String str) {
            this.mCall = call;
            this.mState = i;
            this.mTag = str;
        }

        @Override
        public void performAction() {
            Log.d(this, "perform set call state for %s, state = %s", new Object[]{this.mCall, Integer.valueOf(this.mState)});
            CallsManager.this.setCallState(this.mCall, this.mState, this.mTag);
        }
    }

    private final class ActionUnHoldCall implements PendingAction {
        private final Call mCall;
        private final String mPreviouslyHeldCallId;

        ActionUnHoldCall(Call call, String str) {
            this.mCall = call;
            this.mPreviouslyHeldCallId = str;
        }

        @Override
        public void performAction() {
            Log.d(this, "perform unhold call for %s", new Object[]{this.mCall});
            this.mCall.unhold("held " + this.mPreviouslyHeldCallId);
        }
    }

    private final class ActionAnswerCall implements PendingAction {
        private final Call mCall;
        private final int mVideoState;

        ActionAnswerCall(Call call, int i) {
            this.mCall = call;
            this.mVideoState = i;
        }

        @Override
        public void performAction() {
            Log.d(this, "perform answer call for %s, videoState = %d", new Object[]{this.mCall, Integer.valueOf(this.mVideoState)});
            Iterator it = CallsManager.this.mListeners.iterator();
            while (it.hasNext()) {
                ((CallsManagerListener) it.next()).onIncomingCallAnswered(this.mCall);
            }
            this.mCall.doAnswer(this.mVideoState);
            if (CallsManager.this.isSpeakerphoneAutoEnabledForVideoCalls(this.mVideoState)) {
                this.mCall.setStartWithSpeakerphoneOn(true);
            }
        }
    }

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public static final class RequestCallback implements ConnectionServiceFocusManager.RequestFocusCallback {
        private PendingAction mPendingAction;

        RequestCallback(PendingAction pendingAction) {
            this.mPendingAction = pendingAction;
        }

        @Override
        public void onRequestFocusDone(ConnectionServiceFocusManager.CallFocus callFocus) {
            if (this.mPendingAction != null) {
                this.mPendingAction.performAction();
            }
        }
    }

    private class WaitingCallAction {
        private Call mActionCall;
        private int mVideoState;
        private String mWaitingAction;
        private Call mWaitingCall;

        public WaitingCallAction(Call call, Call call2, String str, int i) {
            this.mActionCall = call;
            this.mWaitingCall = call2;
            this.mWaitingAction = str;
            this.mVideoState = i;
        }

        public void handleActionProcessSuccessful() {
            Log.d("CallsManager", "waiting action = %s, call= %s", new Object[]{this.mWaitingAction, this.mWaitingCall});
            if (this.mWaitingAction.equals("OPERATION_ANSWER_CALL")) {
                if (this.mWaitingCall.getState() == 4) {
                    CallsManager.this.mConnectionSvrFocusMgr.requestFocus(this.mWaitingCall, new RequestCallback(CallsManager.this.new ActionAnswerCall(this.mWaitingCall, this.mVideoState)));
                }
            } else {
                if (this.mWaitingAction.equals("OPERATION_ANSWER_UNHOLD")) {
                    if (this.mWaitingCall.getState() == 6) {
                        CallsManager.this.mConnectionSvrFocusMgr.requestFocus(this.mWaitingCall, new RequestCallback(CallsManager.this.new ActionUnHoldCall(this.mWaitingCall, this.mActionCall != null ? this.mActionCall.getId() : null)));
                        return;
                    }
                    return;
                }
                Log.d("CallsManager", "Weird action, not handle", new Object[0]);
            }
        }

        public void handleActionProcessFailed() {
            Log.d(this, "handleActionProcessFailed, call= %s", new Object[]{this.mWaitingCall});
        }
    }

    private void addWaitingCallAction(Call call, Call call2, String str, int i) {
        this.mWaitingCallActions.put(call, new WaitingCallAction(call, call2, str, i));
    }

    private WaitingCallAction removeWaitingCallAction(Call call) {
        return this.mWaitingCallActions.remove(call);
    }

    private void handleActionProcessComplete(Call call) {
        Log.d(this, "Have waiting call actions: %s", new Object[]{Boolean.valueOf(this.mWaitingCallActions.containsKey(call))});
        if (this.mWaitingCallActions.containsKey(call)) {
            if (call.getState() == 6 || call.getState() == 7) {
                removeWaitingCallAction(call).handleActionProcessSuccessful();
            }
        }
    }

    public void notifyActionFailed(Call call, int i) {
        if (this.mWaitingCallActions.containsKey(call)) {
            Log.i("CallsManager", "notifyActionFailed, remove waiting action", new Object[0]);
            this.mWaitingCallActions.remove(call).handleActionProcessFailed();
        }
    }

    void hangupHoldCall() {
        Log.v("CallsManager", "hangupHoldCall", new Object[0]);
        for (Call call : this.mCalls) {
            if (call.getParentCall() == null && 6 == call.getState()) {
                call.disconnect();
            }
        }
    }

    void hangupAll() {
        Log.v("CallsManager", "hangupAll", new Object[0]);
        for (Call call : this.mCalls) {
            if (call.getParentCall() == null) {
                call.hangupAll();
            }
        }
    }

    public void notifyCallAlertingEvent(Call call) {
        Iterator<CallsManagerListener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onCallAlertingNotified(call);
        }
    }

    private Call getCallForVoiceRecord() {
        for (Call call : this.mCalls) {
            if (call.canVoiceRecord() && !call.isVoiceRecording()) {
                return call;
            }
        }
        Log.w(this, "[getCallForVoiceRecord] no call can perform record", new Object[0]);
        return null;
    }

    private Call getRecordingCall() {
        for (Call call : this.mCalls) {
            if (call.isVoiceRecording()) {
                return call;
            }
        }
        Log.w(this, "[getCallForVoiceRecord] no call is recording", new Object[0]);
        return null;
    }

    public void startVoiceRecord() {
        Call callForVoiceRecord = getCallForVoiceRecord();
        if (callForVoiceRecord != null) {
            callForVoiceRecord.startVoiceRecord();
        }
    }

    public void stopVoiceRecord() {
        Call recordingCall = getRecordingCall();
        if (recordingCall != null) {
            recordingCall.stopVoiceRecord();
        }
    }

    private String dumpCurrentCallStates() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Current calls: [");
        this.mCalls.stream().forEach(new Consumer() {
            @Override
            public final void accept(Object obj) {
                CallsManager.lambda$dumpCurrentCallStates$21(sb, (Call) obj);
            }
        });
        sb.append("]");
        return sb.toString();
    }

    static void lambda$dumpCurrentCallStates$21(StringBuilder sb, Call call) {
        sb.append(call.getId());
        sb.append(": ");
        sb.append(CallState.toString(call.getState()));
        sb.append(", ");
    }

    public void onForegroundCallChanged(Call call, Call call2) {
        Call recordingCall = getRecordingCall();
        Log.d("CallsManager", "foreground call change, recordingCall:" + recordingCall + " foregroundcall:" + call2, new Object[0]);
        if (recordingCall != null && recordingCall != call2) {
            stopVoiceRecord();
        }
        ExtensionManager.getRttUtilExt().onForegroundCallChanged(call, call2);
    }

    CallRecorderManager getCallRecorderManager() {
        return this.mCallRecorderManager;
    }

    private boolean hasModifyingVideoSessionCall() {
        Iterator<Call> it = getCalls().iterator();
        while (it.hasNext()) {
            if (it.next().isModifyingVideoSession()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasOngoingCallsEx() {
        return getNumCallsWithState(3, (Call) null, (PhoneAccountHandle) null, ONGOING_CALL_STATES_EX) > 0;
    }

    private boolean hasNonDisconnectedEmergencyCall(Call call) {
        for (Call call2 : getCalls()) {
            if (!Objects.equals(call2, call) && call2.isEmergencyCall() && call2.getState() != 7) {
                return true;
            }
        }
        return false;
    }

    private Call getDialingCdmaCall() {
        for (Call call : getCalls()) {
            if (call.isDialingCdmaCall()) {
                Log.d(this, "getDialingCdmaCall: %s", new Object[]{call});
                return call;
            }
        }
        return null;
    }

    void onCdmaMoCallAccepted() {
        updateCanAddCall();
    }

    private boolean shouldDisconnectActiveCall(Call call, Call call2) {
        if (Objects.equals(call, call2)) {
            return false;
        }
        if (Objects.equals(call.getTargetPhoneAccount(), call2.getTargetPhoneAccount())) {
            Log.d(this, "[shouldDisconnectForegroundCall] same account", new Object[0]);
            return false;
        }
        if (!call.can(1) || getNumCallsWithState(false, call, call.getTargetPhoneAccount(), 4) > 0) {
            Log.i(this, "[shouldDisconnectForegroundCall] foreground cannot HOLD or has WAITING call in the foreground account. Disconnect the foreground call", new Object[0]);
            return true;
        }
        Log.d(this, "[shouldDisconnectForegroundCall] false by default", new Object[0]);
        return false;
    }

    public void setForegroundIncomingCall(Call call) {
        this.mForegroundIncomingCall = call;
    }

    private boolean hasMaximumDsdaRingingCalls(PhoneAccountHandle phoneAccountHandle) {
        if (2 <= getNumCallsWithState(false, (Call) null, (PhoneAccountHandle) null, 4)) {
            return false;
        }
        return !Objects.equals(phoneAccountHandle, getFirstCallWithState(4).getTargetPhoneAccount());
    }

    public Call getBackgroundIncomingCall() {
        if (this.mForegroundIncomingCall == null) {
            return null;
        }
        return getFirstCallWithState(this.mForegroundIncomingCall, 4);
    }

    public void hangupActiveAndAnswerWaiting() {
        Call ringingCall;
        Log.v("CallsManager", "hangupActiveAndAnswerWaiting", new Object[0]);
        if (this.mForegroundIncomingCall != null) {
            ringingCall = this.mForegroundIncomingCall;
        } else {
            ringingCall = getRingingCall();
        }
        if (!this.mCalls.contains(ringingCall)) {
            Log.d("CallsManager", "Request to answer a non-existent call %s", new Object[]{ringingCall});
            return;
        }
        Call foregroundCall = getForegroundCall();
        if (foregroundCall == null) {
            Log.d("CallsManager", "getForegroundCall error.", new Object[0]);
        } else if (foregroundCall != null && foregroundCall.isActive()) {
            addWaitingCallAction(foregroundCall, ringingCall, "OPERATION_ANSWER_CALL", ringingCall.getVideoState());
            foregroundCall.disconnectActiveForAnswerWaiting();
        }
    }

    private boolean isSpeakerphoneEnabledForTablet() {
        Log.i("CallsManager", "isSpeakerphoneEnabledForTablet", new Object[0]);
        if (SystemProperties.get("ro.vendor.mtk_tb_call_speaker_on").equals("1")) {
            Log.i("CallsManager", "isSpeakerphoneEnabledForTablet, ro.vendor.mtk_tb_call_speaker_on == 1", new Object[0]);
            if (!this.mWiredHeadsetManager.isPluggedIn() && !this.mBluetoothRouteManager.isBluetoothAvailable()) {
                Log.i("CallsManager", "isSpeakerphoneEnabledForTablet,ro.vendor.mtk_tb_call_speaker_on == 1 && no headset && no bt!", new Object[0]);
                if (this.mCallAudioManager.getCallAudioState().getRoute() != 8) {
                    Log.i("CallsManager", "isSpeakerphoneEnabledForTablet, set route to speaker", new Object[0]);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isOkForECC(Call call) {
        if (this.mCalls.size() != 0) {
            return this.mCalls.size() == 1 && this.mCalls.contains(call);
        }
        return true;
    }

    public boolean hasPendingEcc() {
        return this.mPendingEccCall != null;
    }

    boolean shouldBlockCallUnderSingleVideo(Call call) {
        return (call.isVideo() && hasVideoCall(call)) || hasOtherCallInSamePhoneAccountWithDiffVideoState(call);
    }

    private boolean hasVideoCall(Call call) {
        for (Call call2 : this.mCalls) {
            if (!Objects.equals(call2, call) && call2.isVideo() && call2.getState() != 2) {
                return true;
            }
        }
        return false;
    }

    boolean hasOtherCallInSamePhoneAccountWithDiffVideoState(Call call) {
        for (Call call2 : this.mCalls) {
            if (!Objects.equals(call2, call) && call2.getTargetPhoneAccount() != null && call.getTargetPhoneAccount() != null && Objects.equals(call2.getTargetPhoneAccount(), call.getTargetPhoneAccount()) && call2.getVideoState() != call.getVideoState() && call2.getState() != 2) {
                Log.d("CallsManager", "has other call in the same phone account with differ vido state", new Object[0]);
                return true;
            }
        }
        return false;
    }

    boolean hasOtherAliveCallInSamePhoneAccount(Call call) {
        for (Call call2 : this.mCalls) {
            if (!Objects.equals(call2, call) && call2.isAlive() && Objects.equals(call2.getTargetPhoneAccount(), call.getTargetPhoneAccount())) {
                Log.d("CallsManager", "has other alive call in the same phone account", new Object[0]);
                return true;
            }
        }
        return false;
    }

    public static boolean isVideoCallOverWifiAllowed(Context context, int i) {
        PersistableBundle configForSubId;
        Log.v("CallsManager", "calling isVideoCallOverWifiAllowed", new Object[0]);
        CarrierConfigManager carrierConfigManager = (CarrierConfigManager) context.getSystemService("carrier_config");
        boolean z = true;
        if (carrierConfigManager != null && (configForSubId = carrierConfigManager.getConfigForSubId(i)) != null) {
            Log.v("CallsManager", "calling isVideoCallOverWifiAllowed from configmanager", new Object[0]);
            z = true ^ configForSubId.getBoolean("disable_vt_over_wifi_bool");
        }
        Log.v("CallsManager", "calling isVideoCallOverWifiAllowed " + z, new Object[0]);
        return z;
    }

    public int checkForVideoCallOverWifi(Call call, int i) {
        int subsciptionId;
        Log.v("CallsManager", "checkForVideoCallOverWifi call = " + call, new Object[0]);
        if (call == null) {
            return i;
        }
        Log.v("CallsManager", "checkForVideoCallOverWifi videoState=" + i, new Object[0]);
        if (!VideoProfile.isVideo(i)) {
            return i;
        }
        boolean zIsWifiCallingAvailable = TelephonyManager.from(this.mContext).isWifiCallingAvailable();
        Log.v("CallsManager", "checkForVideoCallOverWifi WFC=" + zIsWifiCallingAvailable, new Object[0]);
        if (!zIsWifiCallingAvailable || call.getTargetPhoneAccount() == null || (subsciptionId = call.getSubsciptionId()) == -1) {
            return i;
        }
        Log.v("CallsManager", "checkForVideoCallOverWifi subId = " + subsciptionId, new Object[0]);
        if (!isVideoCallOverWifiAllowed(this.mContext, subsciptionId)) {
            MtkTelecomGlobals.getInstance().showToast(R.string.video_over_wifi_not_available);
            call.setVideoState(0);
            return 0;
        }
        return i;
    }

    private boolean blockOutgoingCallInRoaming(Context context, Uri uri, PhoneAccountHandle phoneAccountHandle, Bundle bundle) {
        int i;
        int subscriptionIdForPhoneAccount = this.mPhoneAccountRegistrar.getSubscriptionIdForPhoneAccount(phoneAccountHandle);
        Log.d("CallsManager", "blockOutgoingCallInRoaming subId = " + subscriptionIdForPhoneAccount, new Object[0]);
        if (!mVoiceRoamingInfoMap.containsKey(Integer.valueOf(subscriptionIdForPhoneAccount)) || subscriptionIdForPhoneAccount == -1) {
            return false;
        }
        Log.d(this, "blockOutgoingCallInRoaming mVoiceRoaming = " + mVoiceRoamingInfoMap.get(Integer.valueOf(subscriptionIdForPhoneAccount)).toString(), new Object[0]);
        if (this.mCanMakeCall) {
            Log.d("CallsManager", "make roaming call", new Object[0]);
            this.mCanMakeCall = false;
            return false;
        }
        if (mVoiceRoamingInfoMap.get(Integer.valueOf(subscriptionIdForPhoneAccount)).intValue() == 0) {
            Log.d("CallsManager", "not in roaming, return", new Object[0]);
            return false;
        }
        if (!operatorCheck(context, subscriptionIdForPhoneAccount)) {
            Log.d(this, "Not Valid for this OP MCC/MNC, no handling further", new Object[0]);
            return false;
        }
        String contactNumber = getContactNumber(uri);
        if (PhoneNumberUtils.isEmergencyNumber(contactNumber)) {
            Log.d("CallsManager", "emergency number, return", new Object[0]);
            return false;
        }
        if (bundle != null) {
            i = bundle.getInt("android.telecom.extra.START_CALL_WITH_VIDEO_STATE", 0);
        } else {
            i = 0;
        }
        int i2 = Settings.Global.getInt(context.getContentResolver(), "domestic_voice_text_roaming" + subscriptionIdForPhoneAccount, 0);
        int i3 = Settings.Global.getInt(context.getContentResolver(), "international_voice_text_roaming" + subscriptionIdForPhoneAccount, 0);
        int i4 = Settings.Global.getInt(context.getContentResolver(), "domestic_voice_text_roaming_guard" + subscriptionIdForPhoneAccount, 0);
        int i5 = Settings.Global.getInt(context.getContentResolver(), "international_voice_roaming_guard" + subscriptionIdForPhoneAccount, 0);
        Log.d("CallsManager", "domesticVoiceRoamingButton = " + i2 + " internationalVoiceRoamingButton = " + i3 + " domesticVoiceRoamingGuard = " + i4 + " internationalVoiceRoamingGuard = " + i5 + "SubId = " + subscriptionIdForPhoneAccount + "blockOutgoingCall, videoState =" + i, new Object[0]);
        if ((mVoiceRoamingInfoMap.get(Integer.valueOf(subscriptionIdForPhoneAccount)).intValue() != 2 || i2 != 1 || i4 != 1) && (mVoiceRoamingInfoMap.get(Integer.valueOf(subscriptionIdForPhoneAccount)).intValue() != 3 || i3 != 1 || i5 != 1)) {
            return false;
        }
        Log.d("CallsManager", "show roaming guard", new Object[0]);
        Intent intent = new Intent(context, (Class<?>) ErrorDialogActivity.class);
        intent.putExtra("show_roaming_alert_dialog", true);
        if (mVoiceRoamingInfoMap.get(Integer.valueOf(subscriptionIdForPhoneAccount)).intValue() == 2) {
            intent.putExtra("is_domestic_roaming", true);
        }
        intent.putExtra("is_video_call", VideoProfile.isVideo(i));
        intent.putExtra("contact_number", contactNumber);
        intent.setFlags(268435456);
        context.startActivity(intent);
        return true;
    }

    private boolean operatorCheck(Context context, int i) {
        boolean z;
        PersistableBundle configForSubId;
        CarrierConfigManager carrierConfigManager = (CarrierConfigManager) context.getSystemService("carrier_config");
        if (carrierConfigManager != null && (configForSubId = carrierConfigManager.getConfigForSubId(i)) != null) {
            z = configForSubId.getBoolean("mtk_key_roaming_bar_guard_bool");
        } else {
            z = false;
        }
        Log.d(this, "blockOutgoingCall: %s" + z, new Object[0]);
        return z;
    }

    private String getContactNumber(Uri uri) {
        String schemeSpecificPart = "";
        if (uri != null) {
            schemeSpecificPart = uri.getSchemeSpecificPart();
        }
        if (!TextUtils.isEmpty(schemeSpecificPart) && !PhoneNumberUtils.isUriNumber(schemeSpecificPart)) {
            return PhoneNumberUtils.stripSeparators(PhoneNumberUtils.convertKeypadLettersToDigits(schemeSpecificPart));
        }
        return schemeSpecificPart;
    }

    public boolean isCallRecorderSupported() {
        return this.mIsCallRecorderSupported;
    }

    Call getRingingCall(Call call) {
        return getFirstCallWithState(call, 4);
    }

    boolean holdActiveAndAnswerRinging(Call call, int i) {
        Call call2 = (Call) this.mConnectionSvrFocusMgr.getCurrentFocusCall();
        if (call2 != null && call2 != call) {
            if (canHold(call2)) {
                Call heldCallByConnectionService = getHeldCallByConnectionService(call.getConnectionService());
                if (heldCallByConnectionService != null) {
                    heldCallByConnectionService.disconnect();
                    Log.i(this, "Disconnecting held call %s, hold and answer call.", new Object[]{heldCallByConnectionService});
                    addWaitingCallAction(call2, call, "OPERATION_ANSWER_CALL", i);
                    call2.hold();
                    return true;
                }
                call2.hold();
                return false;
            }
            if (supportsHold(call2) && call2.getConnectionService() == call.getConnectionService()) {
                Call heldCallByConnectionService2 = getHeldCallByConnectionService(call.getConnectionService());
                if (heldCallByConnectionService2 != null) {
                    heldCallByConnectionService2.disconnect();
                    Log.i(this, "holdActiveCallForNewCall: Disconnect held call %s before holding active call %s.", new Object[]{heldCallByConnectionService2.getId(), call2.getId()});
                }
                Log.i(this, "holdActiveAndAnswerRinging: Holding active %s beforemaking %s active.", new Object[]{call2.getId(), call.getId()});
                call2.hold();
                return false;
            }
            if (call2.getConnectionService() != call.getConnectionService()) {
                Log.i(this, "holdActiveAndAnswerRinging: disconnecting %s so that %s can be made active.", new Object[]{call2.getId(), call.getId()});
                call2.disconnect();
            }
        }
        return false;
    }

    private boolean shouldResumeHoldCall(Call call) {
        boolean z;
        PersistableBundle configForSubId;
        CarrierConfigManager carrierConfigManager = (CarrierConfigManager) this.mContext.getSystemService("carrier_config");
        if (carrierConfigManager != null && (configForSubId = carrierConfigManager.getConfigForSubId(call.getSubsciptionId())) != null) {
            z = configForSubId.getBoolean("mtk_resume_hold_call_after_active_call_end_by_remote");
        } else {
            z = false;
        }
        Log.v("CallsManager", "shouldResumeHoldCall = " + z, new Object[0]);
        return z;
    }

    private boolean isIncomingCallAndEccInDiffPhoneAccount(Call call) {
        for (Call call2 : this.mCalls) {
            if (call2.isEmergencyCall() && !Objects.equals(call2.getTargetPhoneAccount(), call.getTargetPhoneAccount())) {
                return true;
            }
        }
        return false;
    }

    private boolean hasConnectingCall(Call call) {
        return getNumCallsWithState(false, call, (PhoneAccountHandle) null, 1) != 0;
    }

    private boolean shouldDisconnectCallsWhenEccByCarrierConfig(PhoneAccountHandle phoneAccountHandle) {
        boolean z;
        PersistableBundle configForSubId = ((CarrierConfigManager) this.mContext.getSystemService("carrier_config")).getConfigForSubId(this.mPhoneAccountRegistrar.getSubscriptionIdForPhoneAccount(phoneAccountHandle));
        if (configForSubId != null) {
            z = configForSubId.getBoolean("mtk_key_disconnect_all_calls_when_ecc_bool");
        } else {
            z = true;
        }
        Log.i(this, "shouldDisconnectCallsWhenEccByCarrierConfig " + z, new Object[0]);
        return z;
    }
}
