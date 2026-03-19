package com.android.internal.telephony.imsphone;

import android.R;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.radio.V1_0.LastCallFailCause;
import android.hardware.radio.V1_0.RadioError;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.NetworkStats;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.Registrant;
import android.os.RegistrantList;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.telecom.ConferenceParticipant;
import android.telecom.Connection;
import android.telecom.TelecomManager;
import android.telecom.VideoProfile;
import android.telephony.CarrierConfigManager;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import android.telephony.ims.ImsCallProfile;
import android.telephony.ims.ImsReasonInfo;
import android.telephony.ims.ImsSuppServiceNotification;
import android.telephony.ims.feature.ImsFeature;
import android.telephony.ims.feature.MmTelFeature;
import android.telephony.ims.stub.ImsConfigImplBase;
import android.telephony.ims.stub.ImsRegistrationImplBase;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Pair;
import android.util.SparseIntArray;
import com.android.ims.ImsCall;
import com.android.ims.ImsConfigListener;
import com.android.ims.ImsEcbm;
import com.android.ims.ImsException;
import com.android.ims.ImsManager;
import com.android.ims.ImsMultiEndpoint;
import com.android.ims.ImsUtInterface;
import com.android.ims.internal.IImsCallSession;
import com.android.ims.internal.IImsVideoCallProvider;
import com.android.ims.internal.ImsVideoCallProviderWrapper;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.os.SomeArgs;
import com.android.internal.telephony.Call;
import com.android.internal.telephony.CallStateException;
import com.android.internal.telephony.CallTracker;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneInternalInterface;
import com.android.internal.telephony.SubscriptionController;
import com.android.internal.telephony.gsm.SuppServiceNotification;
import com.android.internal.telephony.imsphone.ImsPhone;
import com.android.internal.telephony.metrics.TelephonyMetrics;
import com.google.android.mms.pdu.CharacterSets;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

public class ImsPhoneCallTracker extends CallTracker implements ImsPullCall {
    protected static final boolean DBG = true;
    protected static final int EVENT_CHECK_FOR_WIFI_HANDOVER = 25;
    protected static final int EVENT_DATA_ENABLED_CHANGED = 23;
    protected static final int EVENT_DIAL_PENDINGMO = 20;
    private static final int EVENT_EXIT_ECBM_BEFORE_PENDINGMO = 21;
    protected static final int EVENT_HANGUP_PENDINGMO = 18;
    protected static final int EVENT_ON_FEATURE_CAPABILITY_CHANGED = 26;
    protected static final int EVENT_RESUME_BACKGROUND = 19;
    private static final int EVENT_SUPP_SERVICE_INDICATION = 27;
    private static final int EVENT_VT_DATA_USAGE_UPDATE = 22;
    private static final boolean FORCE_VERBOSE_STATE_LOGGING = false;
    protected static final int HANDOVER_TO_WIFI_TIMEOUT_MS = 60000;
    static final String LOG_TAG = "ImsPhoneCallTracker";
    static final int MAX_CONNECTIONS = 7;
    static final int MAX_CONNECTIONS_PER_CALL = 5;
    protected static final int TIMEOUT_HANGUP_PENDINGMO = 500;
    protected ImsManager mImsManager;
    protected final ImsManager.Connector mImsManagerConnector;
    protected int mPendingCallVideoState;
    protected Bundle mPendingIntentExtras;
    protected ImsPhoneConnection mPendingMO;
    public ImsPhone mPhone;
    protected ImsUtInterface mUtInterface;
    private volatile NetworkStats mVtDataUsageSnapshot;
    private volatile NetworkStats mVtDataUsageUidSnapshot;
    protected int pendingCallClirMode;
    static final String VERBOSE_STATE_TAG = "IPCTState";
    private static final boolean VERBOSE_STATE_LOGGING = Rlog.isLoggable(VERBOSE_STATE_TAG, 2);
    private static final SparseIntArray PRECISE_CAUSE_MAP = new SparseIntArray();
    private MmTelFeature.MmTelCapabilities mMmTelCapabilities = new MmTelFeature.MmTelCapabilities();
    private boolean mCarrierConfigLoaded = false;
    private final MmTelFeatureListener mMmTelFeatureListener = new MmTelFeatureListener();
    protected BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!intent.getAction().equals("android.telephony.action.CARRIER_CONFIG_CHANGED")) {
                if ("android.telecom.action.CHANGE_DEFAULT_DIALER".equals(intent.getAction())) {
                    ImsPhoneCallTracker.this.mDefaultDialerUid.set(ImsPhoneCallTracker.this.getPackageUid(context, intent.getStringExtra("android.telecom.extra.CHANGE_DEFAULT_DIALER_PACKAGE_NAME")));
                    return;
                }
                return;
            }
            int intExtra = intent.getIntExtra("subscription", -1);
            if (intExtra == ImsPhoneCallTracker.this.mPhone.getSubId()) {
                ImsPhoneCallTracker.this.cacheCarrierConfiguration(intExtra);
                ImsPhoneCallTracker.this.log("onReceive : Updating mAllowEmergencyVideoCalls = " + ImsPhoneCallTracker.this.mAllowEmergencyVideoCalls);
            }
        }
    };
    private boolean mIsMonitoringConnectivity = false;
    private ConnectivityManager.NetworkCallback mNetworkCallback = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(Network network) {
            Rlog.i(ImsPhoneCallTracker.LOG_TAG, "Network available: " + network);
            ImsPhoneCallTracker.this.scheduleHandoverCheck();
        }
    };
    protected CopyOnWriteArrayList<ImsPhoneConnection> mConnections = new CopyOnWriteArrayList<>();
    private RegistrantList mVoiceCallEndedRegistrants = new RegistrantList();
    private RegistrantList mVoiceCallStartedRegistrants = new RegistrantList();
    public ImsPhoneCall mRingingCall = new ImsPhoneCall(this, ImsPhoneCall.CONTEXT_RINGING);
    public ImsPhoneCall mForegroundCall = new ImsPhoneCall(this, ImsPhoneCall.CONTEXT_FOREGROUND);
    public ImsPhoneCall mBackgroundCall = new ImsPhoneCall(this, ImsPhoneCall.CONTEXT_BACKGROUND);
    public ImsPhoneCall mHandoverCall = new ImsPhoneCall(this, ImsPhoneCall.CONTEXT_HANDOVER);
    private final HashMap<Integer, Long> mVtDataUsageMap = new HashMap<>();
    private final AtomicInteger mDefaultDialerUid = new AtomicInteger(-1);
    protected int mClirMode = 0;
    protected Object mSyncHold = new Object();
    protected ImsCall mUssdSession = null;
    protected Message mPendingUssd = null;
    private boolean mDesiredMute = false;
    protected boolean mOnHoldToneStarted = false;
    protected int mOnHoldToneId = -1;
    protected PhoneConstants.State mState = PhoneConstants.State.IDLE;
    protected Call.SrvccState mSrvccState = Call.SrvccState.NONE;
    private boolean mIsInEmergencyCall = false;
    protected boolean mIsDataEnabled = false;
    protected boolean pendingCallInEcm = false;
    protected boolean mSwitchingFgAndBgCalls = false;
    protected ImsCall mCallExpectedToResume = null;
    protected boolean mAllowEmergencyVideoCalls = false;
    private boolean mIgnoreDataEnabledChangedForVideoCalls = false;
    protected boolean mIsViLteDataMetered = false;
    private boolean mAlwaysPlayRemoteHoldTone = false;
    private List<PhoneStateListener> mPhoneStateListeners = new ArrayList();
    private boolean mTreatDowngradedVideoCallsAsVideoCalls = false;
    private boolean mDropVideoCallWhenAnsweringAudioCall = false;
    protected boolean mAllowAddCallDuringVideoCall = true;
    protected boolean mNotifyVtHandoverToWifiFail = false;
    private boolean mSupportDowngradeVtToAudio = false;
    protected boolean mNotifyHandoverVideoFromWifiToLTE = false;
    private boolean mNotifyHandoverVideoFromLTEToWifi = false;
    private boolean mHasPerformedStartOfCallHandover = false;
    private boolean mSupportPauseVideo = false;
    private Map<Pair<Integer, String>, Integer> mImsReasonCodeMap = new ArrayMap();
    protected boolean mShouldUpdateImsConfigOnDisconnect = false;
    private SharedPreferenceProxy mSharedPreferenceProxy = new SharedPreferenceProxy() {
        @Override
        public final SharedPreferences getDefaultSharedPreferences(Context context) {
            return PreferenceManager.getDefaultSharedPreferences(context);
        }
    };
    private PhoneNumberUtilsProxy mPhoneNumberUtilsProxy = new PhoneNumberUtilsProxy() {
        @Override
        public final boolean isEmergencyNumber(String str) {
            return PhoneNumberUtils.isEmergencyNumber(str);
        }
    };
    protected ImsCall.Listener mImsCallListener = new ImsCall.Listener() {
        public void onCallProgressing(ImsCall imsCall) {
            ImsPhoneCallTracker.this.log("onCallProgressing");
            ImsPhoneCallTracker.this.mPendingMO = null;
            ImsPhoneCallTracker.this.processCallStateChange(imsCall, Call.State.ALERTING, 0);
            ImsPhoneCallTracker.this.mMetrics.writeOnImsCallProgressing(ImsPhoneCallTracker.this.mPhone.getPhoneId(), imsCall.getCallSession());
        }

        public void onCallStarted(ImsCall imsCall) {
            ImsPhoneCallTracker.this.log("onCallStarted");
            if (ImsPhoneCallTracker.this.mSwitchingFgAndBgCalls && ImsPhoneCallTracker.this.mCallExpectedToResume != null && ImsPhoneCallTracker.this.mCallExpectedToResume == imsCall) {
                ImsPhoneCallTracker.this.log("onCallStarted: starting a call as a result of a switch.");
                ImsPhoneCallTracker.this.mSwitchingFgAndBgCalls = false;
                ImsPhoneCallTracker.this.mCallExpectedToResume = null;
            }
            ImsPhoneCallTracker.this.mPendingMO = null;
            ImsPhoneCallTracker.this.processCallStateChange(imsCall, Call.State.ACTIVE, 0);
            if (ImsPhoneCallTracker.this.mNotifyVtHandoverToWifiFail && imsCall.isVideoCall() && !imsCall.isWifiCall()) {
                if (!ImsPhoneCallTracker.this.isWifiConnected()) {
                    ImsPhoneCallTracker.this.registerForConnectivityChanges();
                } else {
                    ImsPhoneCallTracker.this.sendMessageDelayed(ImsPhoneCallTracker.this.obtainMessage(25, imsCall), 60000L);
                }
            }
            ImsPhoneCallTracker.this.mHasPerformedStartOfCallHandover = false;
            ImsPhoneCallTracker.this.mMetrics.writeOnImsCallStarted(ImsPhoneCallTracker.this.mPhone.getPhoneId(), imsCall.getCallSession());
        }

        public void onCallUpdated(ImsCall imsCall) {
            ImsPhoneConnection imsPhoneConnectionFindConnection;
            ImsPhoneCallTracker.this.log("onCallUpdated");
            if (imsCall != null && (imsPhoneConnectionFindConnection = ImsPhoneCallTracker.this.findConnection(imsCall)) != null) {
                ImsPhoneCallTracker.this.log("onCallUpdated: profile is " + imsCall.getCallProfile());
                ImsPhoneCallTracker.this.processCallStateChange(imsCall, imsPhoneConnectionFindConnection.getCall().mState, 0, true);
                ImsPhoneCallTracker.this.mMetrics.writeImsCallState(ImsPhoneCallTracker.this.mPhone.getPhoneId(), imsCall.getCallSession(), imsPhoneConnectionFindConnection.getCall().mState);
            }
        }

        public void onCallStartFailed(ImsCall imsCall, ImsReasonInfo imsReasonInfo) {
            Call.State state;
            ImsPhoneCallTracker.this.log("onCallStartFailed reasonCode=" + imsReasonInfo.getCode());
            if (ImsPhoneCallTracker.this.mSwitchingFgAndBgCalls && ImsPhoneCallTracker.this.mCallExpectedToResume != null && ImsPhoneCallTracker.this.mCallExpectedToResume == imsCall) {
                ImsPhoneCallTracker.this.log("onCallStarted: starting a call as a result of a switch.");
                ImsPhoneCallTracker.this.mSwitchingFgAndBgCalls = false;
                ImsPhoneCallTracker.this.mCallExpectedToResume = null;
            }
            if (ImsPhoneCallTracker.this.mPendingMO != null) {
                if (ImsPhoneCallTracker.this.mPendingMO.isEmergency() && ImsPhoneCallTracker.this.isPhoneInEcbMode()) {
                    ImsPhoneCallTracker.this.log("onCallStartFailed: restart the ECM timer");
                    ImsPhoneCallTracker.this.handleEcmTimer(0);
                }
                if (imsReasonInfo.getCode() == 146 && ImsPhoneCallTracker.this.mBackgroundCall.getState() == Call.State.IDLE && ImsPhoneCallTracker.this.mRingingCall.getState() == Call.State.IDLE) {
                    ImsPhoneCallTracker.this.mForegroundCall.detach(ImsPhoneCallTracker.this.mPendingMO);
                    ImsPhoneCallTracker.this.removeConnection(ImsPhoneCallTracker.this.mPendingMO);
                    ImsPhoneCallTracker.this.mPendingMO.finalize();
                    ImsPhoneCallTracker.this.mPendingMO = null;
                    ImsPhoneCallTracker.this.mPhone.initiateSilentRedial();
                    return;
                }
                ImsPhoneCallTracker.this.mPendingMO = null;
                ImsPhoneConnection imsPhoneConnectionFindConnection = ImsPhoneCallTracker.this.findConnection(imsCall);
                if (imsPhoneConnectionFindConnection != null) {
                    state = imsPhoneConnectionFindConnection.getState();
                } else {
                    state = Call.State.DIALING;
                }
                int disconnectCauseFromReasonInfo = ImsPhoneCallTracker.this.getDisconnectCauseFromReasonInfo(imsReasonInfo, state);
                ImsPhoneCallTracker.this.setRedialAsEcc(disconnectCauseFromReasonInfo);
                if (imsPhoneConnectionFindConnection != null) {
                    imsPhoneConnectionFindConnection.setPreciseDisconnectCause(ImsPhoneCallTracker.this.getPreciseDisconnectCauseFromReasonInfo(imsReasonInfo));
                    ImsPhoneCallTracker.this.setVendorDisconnectCause(imsPhoneConnectionFindConnection, imsReasonInfo);
                }
                ImsPhoneCallTracker.this.processCallStateChange(imsCall, Call.State.DISCONNECTED, disconnectCauseFromReasonInfo);
                ImsPhoneCallTracker.this.mMetrics.writeOnImsCallStartFailed(ImsPhoneCallTracker.this.mPhone.getPhoneId(), imsCall.getCallSession(), imsReasonInfo);
            }
        }

        public void onCallTerminated(ImsCall imsCall, ImsReasonInfo imsReasonInfo) {
            Call.State state;
            ImsPhoneCallTracker.this.log("onCallTerminated reasonCode=" + imsReasonInfo.getCode());
            ImsPhoneConnection imsPhoneConnectionFindConnection = ImsPhoneCallTracker.this.findConnection(imsCall);
            if (imsPhoneConnectionFindConnection != null) {
                state = imsPhoneConnectionFindConnection.getState();
            } else {
                state = Call.State.ACTIVE;
            }
            int disconnectCauseFromReasonInfo = ImsPhoneCallTracker.this.getDisconnectCauseFromReasonInfo(imsReasonInfo, state);
            ImsPhoneCallTracker.this.log("cause = " + disconnectCauseFromReasonInfo + " conn = " + imsPhoneConnectionFindConnection);
            if (imsPhoneConnectionFindConnection != null) {
                ImsVideoCallProviderWrapper videoProvider = imsPhoneConnectionFindConnection.getVideoProvider();
                if (videoProvider instanceof ImsVideoCallProviderWrapper) {
                    ImsVideoCallProviderWrapper imsVideoCallProviderWrapper = videoProvider;
                    imsVideoCallProviderWrapper.unregisterForDataUsageUpdate(ImsPhoneCallTracker.this);
                    imsVideoCallProviderWrapper.removeImsVideoProviderCallback(imsPhoneConnectionFindConnection);
                }
            }
            if (ImsPhoneCallTracker.this.mOnHoldToneId == System.identityHashCode(imsPhoneConnectionFindConnection)) {
                if (imsPhoneConnectionFindConnection != null && ImsPhoneCallTracker.this.mOnHoldToneStarted) {
                    ImsPhoneCallTracker.this.mPhone.stopOnHoldTone(imsPhoneConnectionFindConnection);
                }
                ImsPhoneCallTracker.this.mOnHoldToneStarted = false;
                ImsPhoneCallTracker.this.mOnHoldToneId = -1;
            }
            if (imsPhoneConnectionFindConnection != null) {
                if (imsPhoneConnectionFindConnection.isPulledCall() && ((imsReasonInfo.getCode() == 1015 || imsReasonInfo.getCode() == 336 || imsReasonInfo.getCode() == 332) && ImsPhoneCallTracker.this.mPhone != null && ImsPhoneCallTracker.this.mPhone.getExternalCallTracker() != null)) {
                    ImsPhoneCallTracker.this.log("Call pull failed.");
                    imsPhoneConnectionFindConnection.onCallPullFailed(ImsPhoneCallTracker.this.mPhone.getExternalCallTracker().getConnectionById(imsPhoneConnectionFindConnection.getPulledDialogId()));
                    disconnectCauseFromReasonInfo = 0;
                } else if (imsPhoneConnectionFindConnection.isIncoming() && imsPhoneConnectionFindConnection.getConnectTime() == 0 && disconnectCauseFromReasonInfo != 52) {
                    if (disconnectCauseFromReasonInfo != 2) {
                        disconnectCauseFromReasonInfo = 16;
                    } else {
                        disconnectCauseFromReasonInfo = 1;
                    }
                    ImsPhoneCallTracker.this.log("Incoming connection of 0 connect time detected - translated cause = " + disconnectCauseFromReasonInfo);
                }
            }
            if (disconnectCauseFromReasonInfo == 2 && imsPhoneConnectionFindConnection != null && imsPhoneConnectionFindConnection.getImsCall().isMerged()) {
                disconnectCauseFromReasonInfo = 45;
            }
            int iUpdateDisconnectCause = ImsPhoneCallTracker.this.updateDisconnectCause(disconnectCauseFromReasonInfo, imsPhoneConnectionFindConnection);
            ImsPhoneCallTracker.this.setRedialAsEcc(iUpdateDisconnectCause);
            ImsPhoneCallTracker.this.setVendorDisconnectCause(imsPhoneConnectionFindConnection, imsReasonInfo);
            ImsPhoneCallTracker.this.mMetrics.writeOnImsCallTerminated(ImsPhoneCallTracker.this.mPhone.getPhoneId(), imsCall.getCallSession(), imsReasonInfo);
            if (imsPhoneConnectionFindConnection != null) {
                imsPhoneConnectionFindConnection.setPreciseDisconnectCause(ImsPhoneCallTracker.this.getPreciseDisconnectCauseFromReasonInfo(imsReasonInfo));
            }
            ImsPhoneCallTracker.this.processCallStateChange(imsCall, Call.State.DISCONNECTED, iUpdateDisconnectCause);
            if (ImsPhoneCallTracker.this.mForegroundCall.getState() != Call.State.ACTIVE) {
                if (ImsPhoneCallTracker.this.mRingingCall.getState().isRinging()) {
                    ImsPhoneCallTracker.this.mPendingMO = null;
                } else if (ImsPhoneCallTracker.this.canDailOnCallTerminated()) {
                    ImsPhoneCallTracker.this.sendEmptyMessage(20);
                }
            }
            if (ImsPhoneCallTracker.this.mSwitchingFgAndBgCalls) {
                ImsPhoneCallTracker.this.log("onCallTerminated: Call terminated in the midst of Switching Fg and Bg calls.");
                if (imsCall == ImsPhoneCallTracker.this.mCallExpectedToResume) {
                    ImsPhoneCallTracker.this.log("onCallTerminated: switching " + ImsPhoneCallTracker.this.mForegroundCall + " with " + ImsPhoneCallTracker.this.mBackgroundCall);
                    ImsPhoneCallTracker.this.mForegroundCall.switchWith(ImsPhoneCallTracker.this.mBackgroundCall);
                }
                ImsPhoneCallTracker imsPhoneCallTracker = ImsPhoneCallTracker.this;
                StringBuilder sb = new StringBuilder();
                sb.append("onCallTerminated: foreground call in state ");
                sb.append(ImsPhoneCallTracker.this.mForegroundCall.getState());
                sb.append(" and ringing call in state ");
                sb.append(ImsPhoneCallTracker.this.mRingingCall == null ? "null" : ImsPhoneCallTracker.this.mRingingCall.getState().toString());
                imsPhoneCallTracker.log(sb.toString());
                if (ImsPhoneCallTracker.this.mForegroundCall.getState() == Call.State.HOLDING || ImsPhoneCallTracker.this.mRingingCall.getState() == Call.State.WAITING) {
                    ImsPhoneCallTracker.this.sendEmptyMessage(19);
                    ImsPhoneCallTracker.this.mSwitchingFgAndBgCalls = false;
                    ImsPhoneCallTracker.this.mCallExpectedToResume = null;
                }
            }
            if (ImsPhoneCallTracker.this.mShouldUpdateImsConfigOnDisconnect) {
                if (ImsPhoneCallTracker.this.mImsManager != null) {
                    ImsPhoneCallTracker.this.mImsManager.updateImsServiceConfig(true);
                }
                ImsPhoneCallTracker.this.mShouldUpdateImsConfigOnDisconnect = false;
            }
        }

        public void onCallHeld(ImsCall imsCall) {
            if (ImsPhoneCallTracker.this.mForegroundCall.getImsCall() == imsCall) {
                ImsPhoneCallTracker.this.log("onCallHeld (fg) " + imsCall);
            } else if (ImsPhoneCallTracker.this.mBackgroundCall.getImsCall() == imsCall) {
                ImsPhoneCallTracker.this.log("onCallHeld (bg) " + imsCall);
            }
            synchronized (ImsPhoneCallTracker.this.mSyncHold) {
                Call.State state = ImsPhoneCallTracker.this.mBackgroundCall.getState();
                ImsPhoneCallTracker.this.processCallStateChange(imsCall, Call.State.HOLDING, 0);
                if (state == Call.State.ACTIVE) {
                    if (ImsPhoneCallTracker.this.mForegroundCall.getState() == Call.State.HOLDING || ImsPhoneCallTracker.this.mRingingCall.getState() == Call.State.WAITING) {
                        ImsPhoneCallTracker.this.releasePendingMOIfRequired();
                        ImsPhoneCallTracker.this.sendEmptyMessage(19);
                    } else {
                        if (ImsPhoneCallTracker.this.mPendingMO != null) {
                            ImsPhoneCallTracker.this.dialPendingMO();
                        }
                        ImsPhoneCallTracker.this.mSwitchingFgAndBgCalls = false;
                    }
                } else if (state == Call.State.IDLE && ImsPhoneCallTracker.this.mSwitchingFgAndBgCalls && ImsPhoneCallTracker.this.mForegroundCall.getState() == Call.State.HOLDING) {
                    ImsPhoneCallTracker.this.sendEmptyMessage(19);
                    ImsPhoneCallTracker.this.mSwitchingFgAndBgCalls = false;
                    ImsPhoneCallTracker.this.mCallExpectedToResume = null;
                }
            }
            ImsPhoneCallTracker.this.mMetrics.writeOnImsCallHeld(ImsPhoneCallTracker.this.mPhone.getPhoneId(), imsCall.getCallSession());
        }

        public void onCallHoldFailed(ImsCall imsCall, ImsReasonInfo imsReasonInfo) {
            ImsPhoneCallTracker.this.log("onCallHoldFailed reasonCode=" + imsReasonInfo.getCode());
            synchronized (ImsPhoneCallTracker.this.mSyncHold) {
                Call.State state = ImsPhoneCallTracker.this.mBackgroundCall.getState();
                if (imsReasonInfo.getCode() == 148) {
                    if (ImsPhoneCallTracker.this.mPendingMO != null) {
                        ImsPhoneCallTracker.this.dialPendingMO();
                    }
                } else if (state == Call.State.ACTIVE) {
                    ImsPhoneCallTracker.this.mForegroundCall.switchWith(ImsPhoneCallTracker.this.mBackgroundCall);
                    if (ImsPhoneCallTracker.this.mPendingMO != null) {
                        ImsPhoneCallTracker.this.mPendingMO.setDisconnectCause(36);
                        ImsPhoneCallTracker.this.sendEmptyMessageDelayed(18, 500L);
                    }
                }
                ImsPhoneCallTracker.this.mPhone.notifySuppServiceFailed(PhoneInternalInterface.SuppService.HOLD);
            }
            ImsPhoneCallTracker.this.mMetrics.writeOnImsCallHoldFailed(ImsPhoneCallTracker.this.mPhone.getPhoneId(), imsCall.getCallSession(), imsReasonInfo);
        }

        public void onCallResumed(ImsCall imsCall) {
            ImsPhoneCallTracker.this.log("onCallResumed");
            if (ImsPhoneCallTracker.this.mSwitchingFgAndBgCalls) {
                if (imsCall != ImsPhoneCallTracker.this.mCallExpectedToResume) {
                    ImsPhoneCallTracker.this.log("onCallResumed : switching " + ImsPhoneCallTracker.this.mForegroundCall + " with " + ImsPhoneCallTracker.this.mBackgroundCall);
                    ImsPhoneCallTracker.this.mForegroundCall.switchWith(ImsPhoneCallTracker.this.mBackgroundCall);
                } else {
                    ImsPhoneCallTracker.this.log("onCallResumed : expected call resumed.");
                }
                ImsPhoneCallTracker.this.mSwitchingFgAndBgCalls = false;
                ImsPhoneCallTracker.this.mCallExpectedToResume = null;
            }
            ImsPhoneCallTracker.this.processCallStateChange(imsCall, Call.State.ACTIVE, 0);
            ImsPhoneCallTracker.this.mMetrics.writeOnImsCallResumed(ImsPhoneCallTracker.this.mPhone.getPhoneId(), imsCall.getCallSession());
        }

        public void onCallResumeFailed(ImsCall imsCall, ImsReasonInfo imsReasonInfo) {
            if (ImsPhoneCallTracker.this.mSwitchingFgAndBgCalls) {
                if (imsCall == ImsPhoneCallTracker.this.mCallExpectedToResume) {
                    ImsPhoneCallTracker.this.log("onCallResumeFailed : switching " + ImsPhoneCallTracker.this.mForegroundCall + " with " + ImsPhoneCallTracker.this.mBackgroundCall);
                    ImsPhoneCallTracker.this.mForegroundCall.switchWith(ImsPhoneCallTracker.this.mBackgroundCall);
                    if (ImsPhoneCallTracker.this.mForegroundCall.getState() == Call.State.HOLDING) {
                        ImsPhoneCallTracker.this.sendEmptyMessage(19);
                    }
                }
                ImsPhoneCallTracker.this.mCallExpectedToResume = null;
                ImsPhoneCallTracker.this.mSwitchingFgAndBgCalls = false;
            }
            ImsPhoneCallTracker.this.mPhone.notifySuppServiceFailed(PhoneInternalInterface.SuppService.RESUME);
            ImsPhoneCallTracker.this.mMetrics.writeOnImsCallResumeFailed(ImsPhoneCallTracker.this.mPhone.getPhoneId(), imsCall.getCallSession(), imsReasonInfo);
        }

        public void onCallResumeReceived(ImsCall imsCall) {
            ImsPhoneCallTracker.this.log("onCallResumeReceived");
            ImsPhoneConnection imsPhoneConnectionFindConnection = ImsPhoneCallTracker.this.findConnection(imsCall);
            if (imsPhoneConnectionFindConnection != null) {
                if (ImsPhoneCallTracker.this.mOnHoldToneStarted) {
                    ImsPhoneCallTracker.this.mPhone.stopOnHoldTone(imsPhoneConnectionFindConnection);
                    ImsPhoneCallTracker.this.mOnHoldToneStarted = false;
                }
                imsPhoneConnectionFindConnection.onConnectionEvent("android.telecom.event.CALL_REMOTELY_UNHELD", null);
            }
            if (ImsPhoneCallTracker.this.mPhone.getContext().getResources().getBoolean(R.^attr-private.pointerIconVectorStrokeInverse) && ImsPhoneCallTracker.this.mSupportPauseVideo && VideoProfile.isVideo(imsPhoneConnectionFindConnection.getVideoState())) {
                imsPhoneConnectionFindConnection.changeToUnPausedState();
            }
            ImsPhoneCallTracker.this.mtkNotifyRemoteHeld(imsPhoneConnectionFindConnection, false);
            SuppServiceNotification suppServiceNotification = new SuppServiceNotification();
            suppServiceNotification.notificationType = 1;
            suppServiceNotification.code = 3;
            ImsPhoneCallTracker.this.mPhone.notifySuppSvcNotification(suppServiceNotification);
            ImsPhoneCallTracker.this.mMetrics.writeOnImsCallResumeReceived(ImsPhoneCallTracker.this.mPhone.getPhoneId(), imsCall.getCallSession());
        }

        public void onCallHoldReceived(ImsCall imsCall) {
            ImsPhoneCallTracker.this.onCallHoldReceived(imsCall);
        }

        public void onCallSuppServiceReceived(ImsCall imsCall, ImsSuppServiceNotification imsSuppServiceNotification) {
            ImsPhoneCallTracker.this.log("onCallSuppServiceReceived: suppServiceInfo=" + imsSuppServiceNotification);
            SuppServiceNotification suppServiceNotification = new SuppServiceNotification();
            suppServiceNotification.notificationType = imsSuppServiceNotification.notificationType;
            suppServiceNotification.code = imsSuppServiceNotification.code;
            suppServiceNotification.index = imsSuppServiceNotification.index;
            suppServiceNotification.number = imsSuppServiceNotification.number;
            suppServiceNotification.history = imsSuppServiceNotification.history;
            ImsPhoneCallTracker.this.mPhone.notifySuppSvcNotification(suppServiceNotification);
        }

        public void onCallMerged(ImsCall imsCall, ImsCall imsCall2, boolean z) {
            ImsPhoneCallTracker.this.log("onCallMerged");
            ImsPhoneCall call = ImsPhoneCallTracker.this.findConnection(imsCall).getCall();
            ImsPhoneConnection imsPhoneConnectionFindConnection = ImsPhoneCallTracker.this.findConnection(imsCall2);
            ImsPhoneCall call2 = imsPhoneConnectionFindConnection == null ? null : imsPhoneConnectionFindConnection.getCall();
            if (z) {
                ImsPhoneCallTracker.this.switchAfterConferenceSuccess();
            }
            call.merge(call2, Call.State.ACTIVE);
            try {
                ImsPhoneConnection imsPhoneConnectionFindConnection2 = ImsPhoneCallTracker.this.findConnection(imsCall);
                ImsPhoneCallTracker.this.log("onCallMerged: ImsPhoneConnection=" + imsPhoneConnectionFindConnection2);
                ImsPhoneCallTracker.this.log("onCallMerged: CurrentVideoProvider=" + imsPhoneConnectionFindConnection2.getVideoProvider());
                ImsPhoneCallTracker.this.setVideoCallProvider(imsPhoneConnectionFindConnection2, imsCall);
                ImsPhoneCallTracker.this.log("onCallMerged: CurrentVideoProvider=" + imsPhoneConnectionFindConnection2.getVideoProvider());
            } catch (Exception e) {
                ImsPhoneCallTracker.this.loge("onCallMerged: exception " + e);
            }
            ImsPhoneCallTracker.this.processCallStateChange(ImsPhoneCallTracker.this.mForegroundCall.getImsCall(), Call.State.ACTIVE, 0);
            if (imsPhoneConnectionFindConnection != null) {
                ImsPhoneCallTracker.this.processCallStateChange(ImsPhoneCallTracker.this.mBackgroundCall.getImsCall(), Call.State.HOLDING, 0);
            }
            if (!imsCall.isMergeRequestedByConf()) {
                ImsPhoneCallTracker.this.log("onCallMerged :: calling onMultipartyStateChanged()");
                onMultipartyStateChanged(imsCall, true);
            } else {
                ImsPhoneCallTracker.this.log("onCallMerged :: Merge requested by existing conference.");
                imsCall.resetIsMergeRequestedByConf(false);
            }
            ImsPhoneCallTracker.this.logState();
        }

        public void onCallMergeFailed(ImsCall imsCall, ImsReasonInfo imsReasonInfo) {
            ImsPhoneCallTracker.this.log("onCallMergeFailed reasonInfo=" + imsReasonInfo);
            ImsPhoneCallTracker.this.mPhone.notifySuppServiceFailed(PhoneInternalInterface.SuppService.CONFERENCE);
            ImsPhoneConnection imsPhoneConnectionFindConnection = ImsPhoneCallTracker.this.findConnection(imsCall);
            if (imsPhoneConnectionFindConnection != null) {
                imsPhoneConnectionFindConnection.onConferenceMergeFailed();
            }
            ImsPhoneConnection firstConnection = ImsPhoneCallTracker.this.mForegroundCall.getFirstConnection();
            if (firstConnection != null) {
                firstConnection.handleMergeComplete();
            }
            ImsCall imsCall2 = ImsPhoneCallTracker.this.mBackgroundCall.getImsCall();
            if (imsCall2 == null) {
                ImsPhoneCallTracker.this.log("onCallMergeFailed: conference no background ims call");
                return;
            }
            ImsPhoneConnection imsPhoneConnectionFindConnection2 = ImsPhoneCallTracker.this.findConnection(imsCall2);
            if (imsPhoneConnectionFindConnection2 != null) {
                imsPhoneConnectionFindConnection2.handleMergeComplete();
            }
        }

        public void onConferenceParticipantsStateChanged(ImsCall imsCall, List<ConferenceParticipant> list) {
            ImsPhoneCallTracker.this.log("onConferenceParticipantsStateChanged");
            ImsPhoneConnection imsPhoneConnectionFindConnection = ImsPhoneCallTracker.this.findConnection(imsCall);
            if (imsPhoneConnectionFindConnection != null) {
                imsPhoneConnectionFindConnection.updateConferenceParticipants(list);
            }
        }

        public void onCallSessionTtyModeReceived(ImsCall imsCall, int i) {
            ImsPhoneCallTracker.this.mPhone.onTtyModeReceived(i);
        }

        public void onCallHandover(ImsCall imsCall, int i, int i2, ImsReasonInfo imsReasonInfo) {
            ImsPhoneCallTracker.this.log("onCallHandover ::  srcAccessTech=" + i + ", targetAccessTech=" + i2 + ", reasonInfo=" + imsReasonInfo);
            boolean z = false;
            boolean z2 = (i == 0 || i == 18 || i2 != 18) ? false : true;
            if (i == 18 && i2 != 0 && i2 != 18) {
                z = true;
            }
            ImsPhoneConnection imsPhoneConnectionFindConnection = ImsPhoneCallTracker.this.findConnection(imsCall);
            if (imsPhoneConnectionFindConnection != null) {
                if (imsPhoneConnectionFindConnection.getDisconnectCause() == 0) {
                    if (z2) {
                        ImsPhoneCallTracker.this.removeMessages(25);
                        if (ImsPhoneCallTracker.this.mNotifyHandoverVideoFromLTEToWifi && ImsPhoneCallTracker.this.mHasPerformedStartOfCallHandover) {
                            imsPhoneConnectionFindConnection.onConnectionEvent("android.telephony.event.EVENT_HANDOVER_VIDEO_FROM_LTE_TO_WIFI", null);
                        }
                        ImsPhoneCallTracker.this.unregisterForConnectivityChanges();
                    } else if (z && imsCall.isVideoCall()) {
                        ImsPhoneCallTracker.this.registerForConnectivityChanges();
                    }
                }
                if (z && imsCall.isVideoCall()) {
                    if (ImsPhoneCallTracker.this.mNotifyHandoverVideoFromWifiToLTE && ImsPhoneCallTracker.this.mIsDataEnabled) {
                        if (imsPhoneConnectionFindConnection.getDisconnectCause() == 0) {
                            ImsPhoneCallTracker.this.log("onCallHandover :: notifying of WIFI to LTE handover.");
                            imsPhoneConnectionFindConnection.onConnectionEvent("android.telephony.event.EVENT_HANDOVER_VIDEO_FROM_WIFI_TO_LTE", null);
                        } else {
                            ImsPhoneCallTracker.this.log("onCallHandover :: skip notify of WIFI to LTE handover for disconnected call.");
                        }
                    }
                    if (!ImsPhoneCallTracker.this.mIsDataEnabled && ImsPhoneCallTracker.this.mIsViLteDataMetered) {
                        ImsPhoneCallTracker.this.downgradeVideoCall(1407, imsPhoneConnectionFindConnection);
                    }
                }
            } else {
                ImsPhoneCallTracker.this.loge("onCallHandover :: connection null.");
            }
            if (!ImsPhoneCallTracker.this.mHasPerformedStartOfCallHandover) {
                ImsPhoneCallTracker.this.mHasPerformedStartOfCallHandover = true;
            }
            ImsPhoneCallTracker.this.mMetrics.writeOnImsCallHandoverEvent(ImsPhoneCallTracker.this.mPhone.getPhoneId(), 18, imsCall.getCallSession(), i, i2, imsReasonInfo);
        }

        public void onCallHandoverFailed(ImsCall imsCall, int i, int i2, ImsReasonInfo imsReasonInfo) {
            boolean z;
            ImsPhoneCallTracker.this.log("onCallHandoverFailed :: srcAccessTech=" + i + ", targetAccessTech=" + i2 + ", reasonInfo=" + imsReasonInfo);
            ImsPhoneCallTracker.this.mMetrics.writeOnImsCallHandoverEvent(ImsPhoneCallTracker.this.mPhone.getPhoneId(), 19, imsCall.getCallSession(), i, i2, imsReasonInfo);
            if (i == 18 || i2 != 18) {
                z = false;
            } else {
                z = true;
            }
            ImsPhoneConnection imsPhoneConnectionFindConnection = ImsPhoneCallTracker.this.findConnection(imsCall);
            if (imsPhoneConnectionFindConnection != null && z) {
                ImsPhoneCallTracker.this.log("onCallHandoverFailed - handover to WIFI Failed");
                ImsPhoneCallTracker.this.removeMessages(25);
                if (imsCall.isVideoCall() && imsPhoneConnectionFindConnection.getDisconnectCause() == 0) {
                    ImsPhoneCallTracker.this.registerForConnectivityChanges();
                }
                if (ImsPhoneCallTracker.this.mNotifyVtHandoverToWifiFail) {
                    imsPhoneConnectionFindConnection.onHandoverToWifiFailed();
                }
            }
            if (!ImsPhoneCallTracker.this.mHasPerformedStartOfCallHandover) {
                ImsPhoneCallTracker.this.mHasPerformedStartOfCallHandover = true;
            }
        }

        public void onRttModifyRequestReceived(ImsCall imsCall) {
            ImsPhoneConnection imsPhoneConnectionFindConnection = ImsPhoneCallTracker.this.findConnection(imsCall);
            if (imsPhoneConnectionFindConnection != null) {
                imsPhoneConnectionFindConnection.onRttModifyRequestReceived();
            }
        }

        public void onRttModifyResponseReceived(ImsCall imsCall, int i) {
            ImsPhoneConnection imsPhoneConnectionFindConnection = ImsPhoneCallTracker.this.findConnection(imsCall);
            if (imsPhoneConnectionFindConnection != null) {
                imsPhoneConnectionFindConnection.onRttModifyResponseReceived(i);
            }
        }

        public void onRttMessageReceived(ImsCall imsCall, String str) {
            ImsPhoneConnection imsPhoneConnectionFindConnection = ImsPhoneCallTracker.this.findConnection(imsCall);
            if (imsPhoneConnectionFindConnection != null) {
                imsPhoneConnectionFindConnection.onRttMessageReceived(str);
            }
        }

        public void onMultipartyStateChanged(ImsCall imsCall, boolean z) {
            ImsPhoneCallTracker imsPhoneCallTracker = ImsPhoneCallTracker.this;
            StringBuilder sb = new StringBuilder();
            sb.append("onMultipartyStateChanged to ");
            sb.append(z ? "Y" : "N");
            imsPhoneCallTracker.log(sb.toString());
            ImsPhoneConnection imsPhoneConnectionFindConnection = ImsPhoneCallTracker.this.findConnection(imsCall);
            if (imsPhoneConnectionFindConnection != null) {
                imsPhoneConnectionFindConnection.updateMultipartyState(z);
            }
        }
    };
    protected ImsCall.Listener mImsUssdListener = new ImsCall.Listener() {
        public void onCallStarted(ImsCall imsCall) {
            ImsPhoneCallTracker.this.log("mImsUssdListener onCallStarted");
            if (imsCall == ImsPhoneCallTracker.this.mUssdSession && ImsPhoneCallTracker.this.mPendingUssd != null) {
                AsyncResult.forMessage(ImsPhoneCallTracker.this.mPendingUssd);
                ImsPhoneCallTracker.this.mPendingUssd.sendToTarget();
                ImsPhoneCallTracker.this.mPendingUssd = null;
            }
        }

        public void onCallStartFailed(ImsCall imsCall, ImsReasonInfo imsReasonInfo) {
            ImsPhoneCallTracker.this.log("mImsUssdListener onCallStartFailed reasonCode=" + imsReasonInfo.getCode());
            onCallTerminated(imsCall, imsReasonInfo);
        }

        public void onCallTerminated(ImsCall imsCall, ImsReasonInfo imsReasonInfo) {
            ImsPhoneCallTracker.this.log("mImsUssdListener onCallTerminated reasonCode=" + imsReasonInfo.getCode());
            ImsPhoneCallTracker.this.removeMessages(25);
            ImsPhoneCallTracker.this.mHasPerformedStartOfCallHandover = false;
            ImsPhoneCallTracker.this.unregisterForConnectivityChanges();
            if (imsCall == ImsPhoneCallTracker.this.mUssdSession) {
                ImsPhoneCallTracker.this.mUssdSession = null;
                if (ImsPhoneCallTracker.this.mPendingUssd != null) {
                    AsyncResult.forMessage(ImsPhoneCallTracker.this.mPendingUssd, (Object) null, new CommandException(CommandException.Error.GENERIC_FAILURE));
                    ImsPhoneCallTracker.this.mPendingUssd.sendToTarget();
                    ImsPhoneCallTracker.this.mPendingUssd = null;
                }
            }
            imsCall.close();
        }

        public void onCallUssdMessageReceived(ImsCall imsCall, int i, String str) {
            int i2;
            ImsPhoneCallTracker.this.log("mImsUssdListener onCallUssdMessageReceived mode=" + i);
            switch (i) {
                case 0:
                    i2 = 0;
                    break;
                case 1:
                    i2 = 1;
                    break;
                default:
                    i2 = -1;
                    break;
            }
            ImsPhoneCallTracker.this.mPhone.onIncomingUSSD(i2, str);
        }
    };
    private final ImsRegistrationImplBase.Callback mImsRegistrationCallback = new ImsRegistrationImplBase.Callback() {
        public void onRegistered(int i) {
            ImsPhoneCallTracker.this.log("onImsConnected imsRadioTech=" + i);
            ImsPhoneCallTracker.this.mPhone.setServiceState(0);
            ImsPhoneCallTracker.this.mPhone.setImsRegistered(true);
            ImsPhoneCallTracker.this.mMetrics.writeOnImsConnectionState(ImsPhoneCallTracker.this.mPhone.getPhoneId(), 1, null);
        }

        public void onRegistering(int i) {
            ImsPhoneCallTracker.this.log("onImsProgressing imsRadioTech=" + i);
            ImsPhoneCallTracker.this.mPhone.setServiceState(1);
            ImsPhoneCallTracker.this.mPhone.setImsRegistered(false);
            ImsPhoneCallTracker.this.mMetrics.writeOnImsConnectionState(ImsPhoneCallTracker.this.mPhone.getPhoneId(), 2, null);
        }

        public void onDeregistered(ImsReasonInfo imsReasonInfo) {
            ImsPhoneCallTracker.this.log("onImsDisconnected imsReasonInfo=" + imsReasonInfo);
            ImsPhoneCallTracker.this.mPhone.setServiceState(1);
            ImsPhoneCallTracker.this.mPhone.setImsRegistered(false);
            ImsPhoneCallTracker.this.mPhone.processDisconnectReason(imsReasonInfo);
            ImsPhoneCallTracker.this.mMetrics.writeOnImsConnectionState(ImsPhoneCallTracker.this.mPhone.getPhoneId(), 3, imsReasonInfo);
        }

        public void onSubscriberAssociatedUriChanged(Uri[] uriArr) {
            ImsPhoneCallTracker.this.log("registrationAssociatedUriChanged");
            ImsPhoneCallTracker.this.mPhone.setCurrentSubscriberUris(uriArr);
        }
    };
    private final ImsFeature.CapabilityCallback mImsCapabilityCallback = new ImsFeature.CapabilityCallback() {
        public void onCapabilitiesStatusChanged(ImsFeature.Capabilities capabilities) {
            ImsPhoneCallTracker.this.log("onCapabilitiesStatusChanged: " + capabilities);
            SomeArgs someArgsObtain = SomeArgs.obtain();
            someArgsObtain.arg1 = capabilities;
            ImsPhoneCallTracker.this.removeMessages(26);
            ImsPhoneCallTracker.this.obtainMessage(26, someArgsObtain).sendToTarget();
            ImsPhoneCallTracker.this.checkRttCallType();
        }
    };
    private ImsConfigListener.Stub mImsConfigListener = new ImsConfigListener.Stub() {
        public void onGetFeatureResponse(int i, int i2, int i3, int i4) {
        }

        public void onSetFeatureResponse(int i, int i2, int i3, int i4) {
            ImsPhoneCallTracker.this.mMetrics.writeImsSetFeatureValue(ImsPhoneCallTracker.this.mPhone.getPhoneId(), i, i2, i3);
        }

        public void onGetVideoQuality(int i, int i2) {
        }

        public void onSetVideoQuality(int i) {
        }
    };
    private final ImsConfigImplBase.Callback mConfigCallback = new ImsConfigImplBase.Callback() {
        public void onConfigChanged(int i, int i2) {
            sendConfigChangedIntent(i, Integer.toString(i2));
        }

        public void onConfigChanged(int i, String str) {
            sendConfigChangedIntent(i, str);
        }

        private void sendConfigChangedIntent(int i, String str) {
            ImsPhoneCallTracker.this.log("sendConfigChangedIntent - [" + i + ", " + str + "]");
            Intent intent = new Intent("com.android.intent.action.IMS_CONFIG_CHANGED");
            intent.putExtra("item", i);
            intent.putExtra("value", str);
            if (ImsPhoneCallTracker.this.mPhone != null && ImsPhoneCallTracker.this.mPhone.getContext() != null) {
                ImsPhoneCallTracker.this.mPhone.getContext().sendBroadcast(intent);
            }
        }
    };
    protected TelephonyMetrics mMetrics = TelephonyMetrics.getInstance();

    public interface PhoneNumberUtilsProxy {
        boolean isEmergencyNumber(String str);
    }

    public interface PhoneStateListener {
        void onPhoneStateChanged(PhoneConstants.State state, PhoneConstants.State state2);
    }

    public interface SharedPreferenceProxy {
        SharedPreferences getDefaultSharedPreferences(Context context);
    }

    static {
        PRECISE_CAUSE_MAP.append(101, 1200);
        PRECISE_CAUSE_MAP.append(102, 1201);
        PRECISE_CAUSE_MAP.append(103, 1202);
        PRECISE_CAUSE_MAP.append(106, 1203);
        PRECISE_CAUSE_MAP.append(107, 1204);
        PRECISE_CAUSE_MAP.append(108, 16);
        PRECISE_CAUSE_MAP.append(111, 1205);
        PRECISE_CAUSE_MAP.append(112, 1206);
        PRECISE_CAUSE_MAP.append(121, 1207);
        PRECISE_CAUSE_MAP.append(122, 1208);
        PRECISE_CAUSE_MAP.append(123, 1209);
        PRECISE_CAUSE_MAP.append(124, 1210);
        PRECISE_CAUSE_MAP.append(131, 1211);
        PRECISE_CAUSE_MAP.append(132, 1212);
        PRECISE_CAUSE_MAP.append(141, 1213);
        PRECISE_CAUSE_MAP.append(143, 1214);
        PRECISE_CAUSE_MAP.append(144, 1215);
        PRECISE_CAUSE_MAP.append(145, 1216);
        PRECISE_CAUSE_MAP.append(146, 1217);
        PRECISE_CAUSE_MAP.append(147, 1218);
        PRECISE_CAUSE_MAP.append(148, 1219);
        PRECISE_CAUSE_MAP.append(149, 1220);
        PRECISE_CAUSE_MAP.append(201, 1221);
        PRECISE_CAUSE_MAP.append(202, 1222);
        PRECISE_CAUSE_MAP.append(203, 1223);
        PRECISE_CAUSE_MAP.append(241, 241);
        PRECISE_CAUSE_MAP.append(321, 1300);
        PRECISE_CAUSE_MAP.append(331, 1310);
        PRECISE_CAUSE_MAP.append(332, 1311);
        PRECISE_CAUSE_MAP.append(333, 1312);
        PRECISE_CAUSE_MAP.append(334, 1313);
        PRECISE_CAUSE_MAP.append(335, 1314);
        PRECISE_CAUSE_MAP.append(336, 1315);
        PRECISE_CAUSE_MAP.append(337, 1316);
        PRECISE_CAUSE_MAP.append(338, 1317);
        PRECISE_CAUSE_MAP.append(339, 1318);
        PRECISE_CAUSE_MAP.append(340, 1319);
        PRECISE_CAUSE_MAP.append(341, 1320);
        PRECISE_CAUSE_MAP.append(342, 1321);
        PRECISE_CAUSE_MAP.append(351, 1330);
        PRECISE_CAUSE_MAP.append(352, 1331);
        PRECISE_CAUSE_MAP.append(353, 1332);
        PRECISE_CAUSE_MAP.append(354, 1333);
        PRECISE_CAUSE_MAP.append(361, 1340);
        PRECISE_CAUSE_MAP.append(362, 1341);
        PRECISE_CAUSE_MAP.append(363, 1342);
        PRECISE_CAUSE_MAP.append(364, 1343);
        PRECISE_CAUSE_MAP.append(401, 1400);
        PRECISE_CAUSE_MAP.append(402, 1401);
        PRECISE_CAUSE_MAP.append(403, 1402);
        PRECISE_CAUSE_MAP.append(404, 1403);
        PRECISE_CAUSE_MAP.append(RadioError.OEM_ERROR_1, 1500);
        PRECISE_CAUSE_MAP.append(RadioError.OEM_ERROR_2, 1501);
        PRECISE_CAUSE_MAP.append(RadioError.OEM_ERROR_3, 1502);
        PRECISE_CAUSE_MAP.append(RadioError.OEM_ERROR_4, 1503);
        PRECISE_CAUSE_MAP.append(RadioError.OEM_ERROR_5, 1504);
        PRECISE_CAUSE_MAP.append(RadioError.OEM_ERROR_6, 1505);
        PRECISE_CAUSE_MAP.append(RadioError.OEM_ERROR_10, 1510);
        PRECISE_CAUSE_MAP.append(801, 1800);
        PRECISE_CAUSE_MAP.append(802, 1801);
        PRECISE_CAUSE_MAP.append(803, 1802);
        PRECISE_CAUSE_MAP.append(804, 1803);
        PRECISE_CAUSE_MAP.append(821, 1804);
        PRECISE_CAUSE_MAP.append(901, 1900);
        PRECISE_CAUSE_MAP.append(902, 1901);
        PRECISE_CAUSE_MAP.append(1100, 2000);
        PRECISE_CAUSE_MAP.append(1014, 2100);
        PRECISE_CAUSE_MAP.append(CharacterSets.UTF_16, 2101);
        PRECISE_CAUSE_MAP.append(1016, 2102);
        PRECISE_CAUSE_MAP.append(1201, 2300);
        PRECISE_CAUSE_MAP.append(1202, 2301);
        PRECISE_CAUSE_MAP.append(1203, 2302);
        PRECISE_CAUSE_MAP.append(1300, 2400);
        PRECISE_CAUSE_MAP.append(1400, 2500);
        PRECISE_CAUSE_MAP.append(1401, 2501);
        PRECISE_CAUSE_MAP.append(1402, 2502);
        PRECISE_CAUSE_MAP.append(1403, 2503);
        PRECISE_CAUSE_MAP.append(1404, 2504);
        PRECISE_CAUSE_MAP.append(1405, 2505);
        PRECISE_CAUSE_MAP.append(1406, 2506);
        PRECISE_CAUSE_MAP.append(1407, 2507);
        PRECISE_CAUSE_MAP.append(1500, LastCallFailCause.RADIO_OFF);
        PRECISE_CAUSE_MAP.append(1501, LastCallFailCause.NO_VALID_SIM);
        PRECISE_CAUSE_MAP.append(1502, LastCallFailCause.RADIO_INTERNAL_ERROR);
        PRECISE_CAUSE_MAP.append(1503, LastCallFailCause.NETWORK_RESP_TIMEOUT);
        PRECISE_CAUSE_MAP.append(1504, LastCallFailCause.NETWORK_REJECT);
        PRECISE_CAUSE_MAP.append(1505, LastCallFailCause.RADIO_ACCESS_FAILURE);
        PRECISE_CAUSE_MAP.append(1506, LastCallFailCause.RADIO_LINK_FAILURE);
        PRECISE_CAUSE_MAP.append(1507, 255);
        PRECISE_CAUSE_MAP.append(1508, 256);
        PRECISE_CAUSE_MAP.append(1509, LastCallFailCause.RADIO_SETUP_FAILURE);
        PRECISE_CAUSE_MAP.append(1510, LastCallFailCause.RADIO_RELEASE_NORMAL);
        PRECISE_CAUSE_MAP.append(1511, LastCallFailCause.RADIO_RELEASE_ABNORMAL);
        PRECISE_CAUSE_MAP.append(1512, LastCallFailCause.ACCESS_CLASS_BLOCKED);
        PRECISE_CAUSE_MAP.append(1513, LastCallFailCause.NETWORK_DETACH);
        PRECISE_CAUSE_MAP.append(1515, 1);
        PRECISE_CAUSE_MAP.append(LastCallFailCause.OEM_CAUSE_1, LastCallFailCause.OEM_CAUSE_1);
        PRECISE_CAUSE_MAP.append(LastCallFailCause.OEM_CAUSE_2, LastCallFailCause.OEM_CAUSE_2);
        PRECISE_CAUSE_MAP.append(LastCallFailCause.OEM_CAUSE_3, LastCallFailCause.OEM_CAUSE_3);
        PRECISE_CAUSE_MAP.append(LastCallFailCause.OEM_CAUSE_4, LastCallFailCause.OEM_CAUSE_4);
        PRECISE_CAUSE_MAP.append(LastCallFailCause.OEM_CAUSE_5, LastCallFailCause.OEM_CAUSE_5);
        PRECISE_CAUSE_MAP.append(LastCallFailCause.OEM_CAUSE_6, LastCallFailCause.OEM_CAUSE_6);
        PRECISE_CAUSE_MAP.append(LastCallFailCause.OEM_CAUSE_7, LastCallFailCause.OEM_CAUSE_7);
        PRECISE_CAUSE_MAP.append(LastCallFailCause.OEM_CAUSE_8, LastCallFailCause.OEM_CAUSE_8);
        PRECISE_CAUSE_MAP.append(LastCallFailCause.OEM_CAUSE_9, LastCallFailCause.OEM_CAUSE_9);
        PRECISE_CAUSE_MAP.append(LastCallFailCause.OEM_CAUSE_10, LastCallFailCause.OEM_CAUSE_10);
        PRECISE_CAUSE_MAP.append(LastCallFailCause.OEM_CAUSE_11, LastCallFailCause.OEM_CAUSE_11);
        PRECISE_CAUSE_MAP.append(LastCallFailCause.OEM_CAUSE_12, LastCallFailCause.OEM_CAUSE_12);
        PRECISE_CAUSE_MAP.append(LastCallFailCause.OEM_CAUSE_13, LastCallFailCause.OEM_CAUSE_13);
        PRECISE_CAUSE_MAP.append(LastCallFailCause.OEM_CAUSE_14, LastCallFailCause.OEM_CAUSE_14);
        PRECISE_CAUSE_MAP.append(LastCallFailCause.OEM_CAUSE_15, LastCallFailCause.OEM_CAUSE_15);
    }

    private class MmTelFeatureListener extends MmTelFeature.Listener {
        private MmTelFeatureListener() {
        }

        public void onIncomingCall(IImsCallSession iImsCallSession, Bundle bundle) {
            ImsCall imsCall;
            ImsPhoneCallTracker.this.log("onReceive : incoming call intent");
            if (ImsPhoneCallTracker.this.mImsManager == null) {
                return;
            }
            try {
                if (bundle.getBoolean("android:ussd", false)) {
                    ImsPhoneCallTracker.this.log("onReceive : USSD");
                    ImsPhoneCallTracker.this.mUssdSession = ImsPhoneCallTracker.this.mImsManager.takeCall(iImsCallSession, bundle, ImsPhoneCallTracker.this.mImsUssdListener);
                    if (ImsPhoneCallTracker.this.mUssdSession != null) {
                        ImsPhoneCallTracker.this.mUssdSession.accept(2);
                        return;
                    }
                    return;
                }
                boolean z = bundle.getBoolean("android:isUnknown", false);
                ImsPhoneCallTracker.this.log("onReceive : isUnknown = " + z + " fg = " + ImsPhoneCallTracker.this.mForegroundCall.getState() + " bg = " + ImsPhoneCallTracker.this.mBackgroundCall.getState());
                ImsCall imsCallTakeCall = ImsPhoneCallTracker.this.takeCall(iImsCallSession, bundle);
                ImsPhoneConnection imsPhoneConnectionMakeImsPhoneConnectionForMT = ImsPhoneCallTracker.this.makeImsPhoneConnectionForMT(imsCallTakeCall, z);
                if (ImsPhoneCallTracker.this.mForegroundCall.hasConnections() && (imsCall = ImsPhoneCallTracker.this.mForegroundCall.getFirstConnection().getImsCall()) != null && imsCallTakeCall != null) {
                    imsPhoneConnectionMakeImsPhoneConnectionForMT.setActiveCallDisconnectedOnAnswer(ImsPhoneCallTracker.this.shouldDisconnectActiveCallOnAnswer(imsCall, imsCallTakeCall));
                }
                imsPhoneConnectionMakeImsPhoneConnectionForMT.setAllowAddCallDuringVideoCall(ImsPhoneCallTracker.this.mAllowAddCallDuringVideoCall);
                ImsPhoneCallTracker.this.addConnection(imsPhoneConnectionMakeImsPhoneConnectionForMT);
                ImsPhoneCallTracker.this.setVideoCallProvider(imsPhoneConnectionMakeImsPhoneConnectionForMT, imsCallTakeCall);
                TelephonyMetrics.getInstance().writeOnImsCallReceive(ImsPhoneCallTracker.this.mPhone.getPhoneId(), imsCallTakeCall.getSession());
                if (z) {
                    ImsPhoneCallTracker.this.mPhone.notifyUnknownConnection(imsPhoneConnectionMakeImsPhoneConnectionForMT);
                } else {
                    if (ImsPhoneCallTracker.this.mForegroundCall.getState() != Call.State.IDLE || ImsPhoneCallTracker.this.mBackgroundCall.getState() != Call.State.IDLE) {
                        imsPhoneConnectionMakeImsPhoneConnectionForMT.update(imsCallTakeCall, Call.State.WAITING);
                    }
                    ImsPhoneCallTracker.this.mPhone.notifyNewRingingConnection(imsPhoneConnectionMakeImsPhoneConnectionForMT);
                    ImsPhoneCallTracker.this.mPhone.notifyIncomingRing();
                }
                ImsPhoneCallTracker.this.updatePhoneState();
                ImsPhoneCallTracker.this.mPhone.notifyPreciseCallStateChanged();
            } catch (ImsException e) {
                ImsPhoneCallTracker.this.loge("onReceive : exception " + e);
            } catch (RemoteException e2) {
            }
        }

        public void onVoiceMessageCountUpdate(int i) {
            if (ImsPhoneCallTracker.this.mPhone != null && ImsPhoneCallTracker.this.mPhone.mDefaultPhone != null) {
                ImsPhoneCallTracker.this.log("onVoiceMessageCountChanged :: count=" + i);
                ImsPhoneCallTracker.this.mPhone.mDefaultPhone.setVoiceMessageCount(i);
                return;
            }
            ImsPhoneCallTracker.this.loge("onVoiceMessageCountUpdate: null phone");
        }
    }

    public ImsPhoneCallTracker(ImsPhone imsPhone) {
        this.mVtDataUsageSnapshot = null;
        this.mVtDataUsageUidSnapshot = null;
        this.mPhone = imsPhone;
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.telephony.action.CARRIER_CONFIG_CHANGED");
        intentFilter.addAction("android.telecom.action.CHANGE_DEFAULT_DIALER");
        this.mPhone.getContext().registerReceiver(this.mReceiver, intentFilter);
        cacheCarrierConfiguration(this.mPhone.getSubId());
        this.mPhone.getDefaultPhone().registerForDataEnabledChanged(this, 23, null);
        this.mDefaultDialerUid.set(getPackageUid(this.mPhone.getContext(), ((TelecomManager) this.mPhone.getContext().getSystemService("telecom")).getDefaultDialerPackage()));
        long jElapsedRealtime = SystemClock.elapsedRealtime();
        this.mVtDataUsageSnapshot = new NetworkStats(jElapsedRealtime, 1);
        this.mVtDataUsageUidSnapshot = new NetworkStats(jElapsedRealtime, 1);
        this.mImsManagerConnector = new ImsManager.Connector(imsPhone.getContext(), imsPhone.getPhoneId(), new ImsManager.Connector.Listener() {
            public void connectionReady(ImsManager imsManager) throws ImsException {
                ImsPhoneCallTracker.this.mImsManager = imsManager;
                ImsPhoneCallTracker.this.startListeningForCalls();
            }

            public void connectionUnavailable() {
                ImsPhoneCallTracker.this.stopListeningForCalls();
            }
        });
        this.mImsManagerConnector.connect();
    }

    @VisibleForTesting
    public void setSharedPreferenceProxy(SharedPreferenceProxy sharedPreferenceProxy) {
        this.mSharedPreferenceProxy = sharedPreferenceProxy;
    }

    @VisibleForTesting
    public void setPhoneNumberUtilsProxy(PhoneNumberUtilsProxy phoneNumberUtilsProxy) {
        this.mPhoneNumberUtilsProxy = phoneNumberUtilsProxy;
    }

    @VisibleForTesting
    public void setRetryTimeout(ImsManager.Connector.RetryTimeout retryTimeout) {
        this.mImsManagerConnector.mRetryTimeout = retryTimeout;
    }

    private int getPackageUid(Context context, String str) {
        if (str == null) {
            return -1;
        }
        try {
            return context.getPackageManager().getPackageUid(str, 0);
        } catch (PackageManager.NameNotFoundException e) {
            loge("Cannot find package uid. pkg = " + str);
            return -1;
        }
    }

    protected void startListeningForCalls() throws ImsException {
        log("startListeningForCalls");
        this.mImsManager.open(this.mMmTelFeatureListener);
        this.mImsManager.addRegistrationCallback(this.mImsRegistrationCallback);
        this.mImsManager.addCapabilitiesCallback(this.mImsCapabilityCallback);
        this.mImsManager.setConfigListener(this.mImsConfigListener);
        this.mImsManager.getConfigInterface().addConfigCallback(this.mConfigCallback);
        getEcbmInterface().setEcbmStateListener(this.mPhone.getImsEcbmStateListener());
        if (this.mPhone.isInEcm()) {
            this.mPhone.exitEmergencyCallbackMode();
        }
        this.mImsManager.setUiTTYMode(this.mPhone.getContext(), Settings.Secure.getInt(this.mPhone.getContext().getContentResolver(), "preferred_tty_mode", 0), (Message) null);
        ImsMultiEndpoint multiEndpointInterface = getMultiEndpointInterface();
        if (multiEndpointInterface != null) {
            multiEndpointInterface.setExternalCallStateListener(this.mPhone.getExternalCallTracker().getExternalCallStateListener());
        }
        this.mUtInterface = getUtInterface();
        if (this.mUtInterface != null) {
            this.mUtInterface.registerForSuppServiceIndication(this, 27, (Object) null);
        }
        if (this.mCarrierConfigLoaded) {
            this.mImsManager.updateImsServiceConfig(true);
        }
    }

    private void stopListeningForCalls() {
        log("stopListeningForCalls");
        resetImsCapabilities();
        if (this.mImsManager != null) {
            try {
                this.mImsManager.getConfigInterface().removeConfigCallback(this.mConfigCallback);
            } catch (ImsException e) {
                Log.w(LOG_TAG, "stopListeningForCalls: unable to remove config callback.");
            }
            this.mImsManager.close();
        }
    }

    public void dispose() {
        log("dispose");
        this.mRingingCall.dispose();
        this.mBackgroundCall.dispose();
        this.mForegroundCall.dispose();
        this.mHandoverCall.dispose();
        clearDisconnected();
        if (this.mUtInterface != null) {
            this.mUtInterface.unregisterForSuppServiceIndication(this);
        }
        this.mPhone.getContext().unregisterReceiver(this.mReceiver);
        this.mPhone.getDefaultPhone().unregisterForDataEnabledChanged(this);
        this.mImsManagerConnector.disconnect();
    }

    protected void finalize() {
        log("ImsPhoneCallTracker finalized");
    }

    @Override
    public void registerForVoiceCallStarted(Handler handler, int i, Object obj) {
        this.mVoiceCallStartedRegistrants.add(new Registrant(handler, i, obj));
    }

    @Override
    public void unregisterForVoiceCallStarted(Handler handler) {
        this.mVoiceCallStartedRegistrants.remove(handler);
    }

    @Override
    public void registerForVoiceCallEnded(Handler handler, int i, Object obj) {
        this.mVoiceCallEndedRegistrants.add(new Registrant(handler, i, obj));
    }

    @Override
    public void unregisterForVoiceCallEnded(Handler handler) {
        this.mVoiceCallEndedRegistrants.remove(handler);
    }

    public int getClirMode() {
        if (this.mSharedPreferenceProxy != null && this.mPhone.getDefaultPhone() != null) {
            return this.mSharedPreferenceProxy.getDefaultSharedPreferences(this.mPhone.getContext()).getInt(Phone.CLIR_KEY + this.mPhone.getDefaultPhone().getPhoneId(), 0);
        }
        loge("dial; could not get default CLIR mode.");
        return 0;
    }

    public Connection dial(String str, int i, Bundle bundle) throws CallStateException {
        return dial(str, new ImsPhone.ImsDialArgs.Builder().setIntentExtras(bundle).setVideoState(i).setClirMode(getClirMode()).build());
    }

    public synchronized Connection dial(String str, ImsPhone.ImsDialArgs imsDialArgs) throws CallStateException {
        boolean z;
        ImsPhoneConnection imsPhoneConnection;
        boolean zIsPhoneInEcbMode = isPhoneInEcbMode();
        boolean zIsEmergencyNumber = isEmergencyNumber(str);
        if (!shouldNumberBePlacedOnIms(zIsEmergencyNumber, str)) {
            Rlog.i(LOG_TAG, "dial: shouldNumberBePlacedOnIms = false");
            throw new CallStateException(Phone.CS_FALLBACK);
        }
        int i = imsDialArgs.clirMode;
        int i2 = imsDialArgs.videoState;
        log("dial clirMode=" + i);
        if (zIsEmergencyNumber) {
            i = 2;
            log("dial emergency call, set clirModIe=2");
        }
        clearDisconnected();
        if (this.mImsManager == null) {
            throw new CallStateException("service not available");
        }
        checkforCsfb();
        if (!canDial()) {
            throw new CallStateException("cannot dial in current state");
        }
        if (zIsPhoneInEcbMode && zIsEmergencyNumber) {
            handleEcmTimer(1);
        }
        boolean z2 = false;
        if (zIsEmergencyNumber && VideoProfile.isVideo(i2) && !this.mAllowEmergencyVideoCalls) {
            loge("dial: carrier does not support video emergency calls; downgrade to audio-only");
            i2 = 0;
        }
        if (this.mForegroundCall.getState() == Call.State.ACTIVE) {
            if (this.mBackgroundCall.getState() != Call.State.IDLE) {
                throw new CallStateException("cannot dial in current state");
            }
            this.mPendingCallVideoState = i2;
            this.mPendingIntentExtras = imsDialArgs.intentExtras;
            switchWaitingOrHoldingAndActive();
            z = true;
        } else {
            z = false;
        }
        Call.State state = Call.State.IDLE;
        Call.State state2 = Call.State.IDLE;
        this.mClirMode = i;
        synchronized (this.mSyncHold) {
            if (z) {
                try {
                    Call.State state3 = this.mForegroundCall.getState();
                    Call.State state4 = this.mBackgroundCall.getState();
                    if (state3 == Call.State.ACTIVE) {
                        throw new CallStateException("cannot dial in current state");
                    }
                    if (state4 != Call.State.HOLDING) {
                        z2 = z;
                    }
                    this.mPendingMO = makeImsPhoneConnectionForMO(str, zIsEmergencyNumber);
                    imsPhoneConnection = this.mPendingMO;
                    this.mPendingMO.setVideoState(i2);
                    if (imsDialArgs.rttTextStream != null) {
                        log("dial: setting RTT stream on mPendingMO");
                        this.mPendingMO.setCurrentRttTextStream(imsDialArgs.rttTextStream);
                    }
                } finally {
                }
            }
        }
        addConnection(this.mPendingMO);
        if (!z2) {
            if (!zIsPhoneInEcbMode || (zIsPhoneInEcbMode && zIsEmergencyNumber)) {
                dialInternal(this.mPendingMO, i, i2, imsDialArgs.intentExtras);
            } else {
                try {
                    getEcbmInterface().exitEmergencyCallbackMode();
                    this.mPhone.setOnEcbModeExitResponse(this, 14, null);
                    this.pendingCallClirMode = i;
                    this.mPendingCallVideoState = i2;
                    this.pendingCallInEcm = true;
                } catch (ImsException e) {
                    e.printStackTrace();
                    throw new CallStateException("service not available");
                }
            }
        }
        updatePhoneState();
        this.mPhone.notifyPreciseCallStateChanged();
        switchWfcModeIfRequired(this.mImsManager, isVowifiEnabled(), zIsEmergencyNumber);
        return imsPhoneConnection;
    }

    boolean isImsServiceReady() {
        if (this.mImsManager == null) {
            return false;
        }
        return this.mImsManager.isServiceReady();
    }

    private boolean shouldNumberBePlacedOnIms(boolean z, String str) {
        try {
            if (this.mImsManager != null) {
                int iShouldProcessCall = this.mImsManager.shouldProcessCall(z, new String[]{str});
                Rlog.i(LOG_TAG, "shouldProcessCall: number: " + Rlog.pii(LOG_TAG, str) + ", result: " + iShouldProcessCall);
                switch (iShouldProcessCall) {
                    case 0:
                        break;
                    case 1:
                        Rlog.i(LOG_TAG, "shouldProcessCall: place over CSFB instead.");
                        break;
                    default:
                        Rlog.w(LOG_TAG, "shouldProcessCall returned unknown result.");
                        break;
                }
                return false;
            }
            Rlog.w(LOG_TAG, "ImsManager unavailable, shouldProcessCall returning false.");
            return false;
        } catch (ImsException e) {
            Rlog.w(LOG_TAG, "ImsService unavailable, shouldProcessCall returning false.");
            return false;
        }
    }

    protected void cacheCarrierConfiguration(int i) {
        CarrierConfigManager carrierConfigManager = (CarrierConfigManager) this.mPhone.getContext().getSystemService("carrier_config");
        if (carrierConfigManager == null || !SubscriptionController.getInstance().isActiveSubId(i)) {
            loge("cacheCarrierConfiguration: No carrier config service found or not active subId = " + i);
            this.mCarrierConfigLoaded = false;
            return;
        }
        PersistableBundle configForSubId = carrierConfigManager.getConfigForSubId(i);
        if (configForSubId == null) {
            loge("cacheCarrierConfiguration: Empty carrier config.");
            this.mCarrierConfigLoaded = false;
        } else {
            this.mCarrierConfigLoaded = true;
            updateCarrierConfigCache(configForSubId);
        }
    }

    @VisibleForTesting
    public void updateCarrierConfigCache(PersistableBundle persistableBundle) {
        Integer numValueOf;
        this.mAllowEmergencyVideoCalls = persistableBundle.getBoolean("allow_emergency_video_calls_bool");
        this.mTreatDowngradedVideoCallsAsVideoCalls = persistableBundle.getBoolean("treat_downgraded_video_calls_as_video_calls_bool");
        this.mDropVideoCallWhenAnsweringAudioCall = persistableBundle.getBoolean("drop_video_call_when_answering_audio_call_bool");
        this.mAllowAddCallDuringVideoCall = persistableBundle.getBoolean("allow_add_call_during_video_call");
        this.mNotifyVtHandoverToWifiFail = persistableBundle.getBoolean("notify_vt_handover_to_wifi_failure_bool");
        this.mSupportDowngradeVtToAudio = persistableBundle.getBoolean("support_downgrade_vt_to_audio_bool");
        this.mNotifyHandoverVideoFromWifiToLTE = persistableBundle.getBoolean("notify_handover_video_from_wifi_to_lte_bool");
        this.mNotifyHandoverVideoFromLTEToWifi = persistableBundle.getBoolean("notify_handover_video_from_lte_to_wifi_bool");
        this.mIgnoreDataEnabledChangedForVideoCalls = persistableBundle.getBoolean("ignore_data_enabled_changed_for_video_calls");
        this.mIsViLteDataMetered = persistableBundle.getBoolean("vilte_data_is_metered_bool");
        this.mSupportPauseVideo = persistableBundle.getBoolean("support_pause_ims_video_calls_bool");
        this.mAlwaysPlayRemoteHoldTone = persistableBundle.getBoolean("always_play_remote_hold_tone_bool");
        String[] stringArray = persistableBundle.getStringArray("ims_reasoninfo_mapping_string_array");
        if (stringArray != null && stringArray.length > 0) {
            for (String str : stringArray) {
                String[] strArrSplit = str.split(Pattern.quote("|"));
                if (strArrSplit.length == 3) {
                    try {
                        if (strArrSplit[0].equals(CharacterSets.MIMENAME_ANY_CHARSET)) {
                            numValueOf = null;
                        } else {
                            numValueOf = Integer.valueOf(Integer.parseInt(strArrSplit[0]));
                        }
                        String str2 = strArrSplit[1];
                        int i = Integer.parseInt(strArrSplit[2]);
                        addReasonCodeRemapping(numValueOf, str2, Integer.valueOf(i));
                        log(("Loaded ImsReasonInfo mapping : fromCode = " + numValueOf) == null ? "any" : numValueOf + " ; message = " + str2 + " ; toCode = " + i);
                    } catch (NumberFormatException e) {
                        loge("Invalid ImsReasonInfo mapping found: " + str);
                    }
                }
            }
            return;
        }
        log("No carrier ImsReasonInfo mappings defined.");
    }

    protected void handleEcmTimer(int i) {
        this.mPhone.handleTimerInEmergencyCallbackMode(i);
        switch (i) {
            case 0:
            case 1:
                break;
            default:
                log("handleEcmTimer, unsupported action " + i);
                break;
        }
    }

    protected void dialInternal(ImsPhoneConnection imsPhoneConnection, int i, int i2, Bundle bundle) {
        int i3;
        if (imsPhoneConnection == null) {
            return;
        }
        if (imsPhoneConnection.getAddress() == null || imsPhoneConnection.getAddress().length() == 0 || imsPhoneConnection.getAddress().indexOf(78) >= 0) {
            imsPhoneConnection.setDisconnectCause(7);
            sendEmptyMessageDelayed(18, 500L);
            return;
        }
        setMute(false);
        if (this.mPhoneNumberUtilsProxy.isEmergencyNumber(imsPhoneConnection.getAddress())) {
            i3 = 2;
        } else {
            i3 = 1;
        }
        int callTypeFromVideoState = ImsCallProfile.getCallTypeFromVideoState(i2);
        imsPhoneConnection.setVideoState(i2);
        try {
            String[] strArr = {imsPhoneConnection.getAddress()};
            ImsCallProfile imsCallProfileCreateCallProfile = this.mImsManager.createCallProfile(i3, callTypeFromVideoState);
            imsCallProfileCreateCallProfile.setCallExtraInt("oir", i);
            if (bundle != null) {
                if (bundle.containsKey("android.telecom.extra.CALL_SUBJECT")) {
                    bundle.putString("DisplayText", cleanseInstantLetteringMessage(bundle.getString("android.telecom.extra.CALL_SUBJECT")));
                }
                if (imsPhoneConnection.hasRttTextStream()) {
                    imsCallProfileCreateCallProfile.mMediaProfile.mRttMode = 1;
                }
                if (bundle.containsKey("CallPull")) {
                    imsCallProfileCreateCallProfile.mCallExtras.putBoolean("CallPull", bundle.getBoolean("CallPull"));
                    int i4 = bundle.getInt(ImsExternalCallTracker.EXTRA_IMS_EXTERNAL_CALL_ID);
                    imsPhoneConnection.setIsPulledCall(true);
                    imsPhoneConnection.setPulledDialogId(i4);
                }
                imsCallProfileCreateCallProfile.mCallExtras.putBundle("OemCallExtras", bundle);
            }
            ImsCall imsCallMakeCall = this.mImsManager.makeCall(imsCallProfileCreateCallProfile, strArr, this.mImsCallListener);
            imsPhoneConnection.setImsCall(imsCallMakeCall);
            this.mMetrics.writeOnImsCallStart(this.mPhone.getPhoneId(), imsCallMakeCall.getSession());
            setVideoCallProvider(imsPhoneConnection, imsCallMakeCall);
            imsPhoneConnection.setAllowAddCallDuringVideoCall(this.mAllowAddCallDuringVideoCall);
        } catch (RemoteException e) {
        } catch (ImsException e2) {
            loge("dialInternal : " + e2);
            imsPhoneConnection.setDisconnectCause(36);
            sendEmptyMessageDelayed(18, 500L);
            retryGetImsService();
        }
    }

    public void acceptCall(int i) throws CallStateException {
        log("acceptCall");
        if (this.mForegroundCall.getState().isAlive() && this.mBackgroundCall.getState().isAlive()) {
            throw new CallStateException("cannot accept call");
        }
        boolean zShouldDisconnectActiveCallOnAnswer = false;
        if (this.mRingingCall.getState() != Call.State.WAITING || !this.mForegroundCall.getState().isAlive()) {
            if (this.mRingingCall.getState().isRinging()) {
                log("acceptCall: incoming...");
                setMute(false);
                try {
                    ImsCall imsCall = this.mRingingCall.getImsCall();
                    if (imsCall != null) {
                        imsCall.accept(ImsCallProfile.getCallTypeFromVideoState(i));
                        this.mMetrics.writeOnImsCommand(this.mPhone.getPhoneId(), imsCall.getSession(), 2);
                        return;
                    }
                    throw new CallStateException("no valid ims call");
                } catch (ImsException e) {
                    throw new CallStateException("cannot accept call");
                }
            }
            throw new CallStateException("phone not ringing");
        }
        setMute(false);
        ImsCall imsCall2 = this.mForegroundCall.getImsCall();
        ImsCall imsCall3 = this.mRingingCall.getImsCall();
        if (this.mForegroundCall.hasConnections() && this.mRingingCall.hasConnections()) {
            zShouldDisconnectActiveCallOnAnswer = shouldDisconnectActiveCallOnAnswer(imsCall2, imsCall3);
        }
        this.mPendingCallVideoState = i;
        if (zShouldDisconnectActiveCallOnAnswer) {
            this.mForegroundCall.hangup();
            try {
                imsCall3.accept(ImsCallProfile.getCallTypeFromVideoState(i));
                return;
            } catch (ImsException e2) {
                throw new CallStateException("cannot accept call");
            }
        }
        switchWaitingOrHoldingAndActive();
    }

    public void rejectCall() throws CallStateException {
        log("rejectCall");
        if (this.mRingingCall.getState().isRinging()) {
            hangup(this.mRingingCall);
            return;
        }
        throw new CallStateException("phone not ringing");
    }

    private void switchAfterConferenceSuccess() {
        log("switchAfterConferenceSuccess fg =" + this.mForegroundCall.getState() + ", bg = " + this.mBackgroundCall.getState());
        if (this.mBackgroundCall.getState() == Call.State.HOLDING) {
            log("switchAfterConferenceSuccess");
            this.mForegroundCall.switchWith(this.mBackgroundCall);
        }
    }

    public void switchWaitingOrHoldingAndActive() throws CallStateException {
        log("switchWaitingOrHoldingAndActive");
        if (this.mRingingCall.getState() == Call.State.INCOMING) {
            throw new CallStateException("cannot be in the incoming state");
        }
        if (this.mForegroundCall.getState() == Call.State.ACTIVE) {
            ImsCall imsCall = this.mForegroundCall.getImsCall();
            if (imsCall == null) {
                throw new CallStateException("no ims call");
            }
            boolean z = (this.mBackgroundCall.getState().isAlive() || this.mRingingCall == null || this.mRingingCall.getState() != Call.State.WAITING) ? false : true;
            this.mSwitchingFgAndBgCalls = true;
            if (z) {
                this.mCallExpectedToResume = this.mRingingCall.getImsCall();
            } else {
                this.mCallExpectedToResume = this.mBackgroundCall.getImsCall();
            }
            this.mForegroundCall.switchWith(this.mBackgroundCall);
            try {
                imsCall.hold();
                this.mMetrics.writeOnImsCommand(this.mPhone.getPhoneId(), imsCall.getSession(), 5);
                if (this.mCallExpectedToResume == null) {
                    log("mCallExpectedToResume is null");
                    this.mSwitchingFgAndBgCalls = false;
                    return;
                }
                return;
            } catch (ImsException e) {
                this.mForegroundCall.switchWith(this.mBackgroundCall);
                resetFlagWhenSwitchFailed();
                throw new CallStateException(e.getMessage());
            }
        }
        if (this.mBackgroundCall.getState() == Call.State.HOLDING) {
            resumeWaitingOrHolding();
        }
    }

    public void conference() {
        ImsCall imsCall = this.mForegroundCall.getImsCall();
        if (imsCall == null) {
            log("conference no foreground ims call");
            return;
        }
        ImsCall imsCall2 = this.mBackgroundCall.getImsCall();
        if (imsCall2 == null) {
            log("conference no background ims call");
            return;
        }
        if (imsCall.isCallSessionMergePending()) {
            log("conference: skip; foreground call already in process of merging.");
            return;
        }
        if (imsCall2.isCallSessionMergePending()) {
            log("conference: skip; background call already in process of merging.");
            return;
        }
        long earliestConnectTime = this.mForegroundCall.getEarliestConnectTime();
        long earliestConnectTime2 = this.mBackgroundCall.getEarliestConnectTime();
        if (earliestConnectTime > 0 && earliestConnectTime2 > 0) {
            earliestConnectTime = Math.min(this.mForegroundCall.getEarliestConnectTime(), this.mBackgroundCall.getEarliestConnectTime());
            log("conference - using connect time = " + earliestConnectTime);
        } else if (earliestConnectTime > 0) {
            log("conference - bg call connect time is 0; using fg = " + earliestConnectTime);
        } else {
            log("conference - fg call connect time is 0; using bg = " + earliestConnectTime2);
            earliestConnectTime = earliestConnectTime2;
        }
        String telecomCallId = "";
        ImsPhoneConnection firstConnection = this.mForegroundCall.getFirstConnection();
        if (firstConnection != null) {
            firstConnection.setConferenceConnectTime(earliestConnectTime);
            firstConnection.handleMergeStart();
            telecomCallId = firstConnection.getTelecomCallId();
        }
        String telecomCallId2 = "";
        ImsPhoneConnection imsPhoneConnectionFindConnection = findConnection(imsCall2);
        if (imsPhoneConnectionFindConnection != null) {
            imsPhoneConnectionFindConnection.handleMergeStart();
            telecomCallId2 = imsPhoneConnectionFindConnection.getTelecomCallId();
        }
        log("conference: fgCallId=" + telecomCallId + ", bgCallId=" + telecomCallId2);
        try {
            imsCall.merge(imsCall2);
        } catch (ImsException e) {
            log("conference " + e.getMessage());
            if (firstConnection != null) {
                firstConnection.handleMergeComplete();
            }
            if (imsPhoneConnectionFindConnection != null) {
                imsPhoneConnectionFindConnection.handleMergeComplete();
            }
        }
    }

    public void explicitCallTransfer() {
    }

    public void clearDisconnected() {
        log("clearDisconnected");
        internalClearDisconnected();
        updatePhoneState();
        this.mPhone.notifyPreciseCallStateChanged();
    }

    public boolean canConference() {
        return this.mForegroundCall.getState() == Call.State.ACTIVE && this.mBackgroundCall.getState() == Call.State.HOLDING && !this.mBackgroundCall.isFull() && !this.mForegroundCall.isFull();
    }

    public boolean canDial() {
        return (this.mPendingMO != null || this.mRingingCall.isRinging() || SystemProperties.get("ro.telephony.disable-call", "false").equals("true") || (this.mForegroundCall.getState().isAlive() && this.mBackgroundCall.getState().isAlive())) ? false : true;
    }

    public boolean canTransfer() {
        return this.mForegroundCall.getState() == Call.State.ACTIVE && this.mBackgroundCall.getState() == Call.State.HOLDING;
    }

    private void internalClearDisconnected() {
        this.mRingingCall.clearDisconnected();
        this.mForegroundCall.clearDisconnected();
        this.mBackgroundCall.clearDisconnected();
        this.mHandoverCall.clearDisconnected();
    }

    protected void updatePhoneState() {
        PhoneConstants.State state = this.mState;
        boolean z = this.mPendingMO == null || !this.mPendingMO.getState().isAlive();
        if (this.mRingingCall.isRinging()) {
            this.mState = PhoneConstants.State.RINGING;
        } else if (!z || !this.mForegroundCall.isIdle() || !this.mBackgroundCall.isIdle()) {
            this.mState = PhoneConstants.State.OFFHOOK;
        } else {
            this.mState = PhoneConstants.State.IDLE;
        }
        if (this.mState == PhoneConstants.State.IDLE && state != this.mState) {
            this.mVoiceCallEndedRegistrants.notifyRegistrants(getCallStateChangeAsyncResult());
        } else if (state == PhoneConstants.State.IDLE && state != this.mState) {
            this.mVoiceCallStartedRegistrants.notifyRegistrants(getCallStateChangeAsyncResult());
        }
        StringBuilder sb = new StringBuilder();
        sb.append("updatePhoneState pendingMo = ");
        sb.append(this.mPendingMO == null ? "null" : this.mPendingMO.getState());
        sb.append(", fg= ");
        sb.append(this.mForegroundCall.getState());
        sb.append("(");
        sb.append(this.mForegroundCall.getConnections().size());
        sb.append("), bg= ");
        sb.append(this.mBackgroundCall.getState());
        sb.append("(");
        sb.append(this.mBackgroundCall.getConnections().size());
        sb.append(")");
        log(sb.toString());
        log("updatePhoneState oldState=" + state + ", newState=" + this.mState);
        if (this.mState != state) {
            this.mPhone.notifyPhoneStateChanged();
            this.mMetrics.writePhoneState(this.mPhone.getPhoneId(), this.mState);
            notifyPhoneStateChanged(state, this.mState);
        }
    }

    private void handleRadioNotAvailable() {
        pollCallsWhenSafe();
    }

    private void dumpState() {
        log("Phone State:" + this.mState);
        log("Ringing call: " + this.mRingingCall.toString());
        List<Connection> connections = this.mRingingCall.getConnections();
        int size = connections.size();
        for (int i = 0; i < size; i++) {
            log(connections.get(i).toString());
        }
        log("Foreground call: " + this.mForegroundCall.toString());
        List<Connection> connections2 = this.mForegroundCall.getConnections();
        int size2 = connections2.size();
        for (int i2 = 0; i2 < size2; i2++) {
            log(connections2.get(i2).toString());
        }
        log("Background call: " + this.mBackgroundCall.toString());
        List<Connection> connections3 = this.mBackgroundCall.getConnections();
        int size3 = connections3.size();
        for (int i3 = 0; i3 < size3; i3++) {
            log(connections3.get(i3).toString());
        }
    }

    public void setTtyMode(int i) {
        if (this.mImsManager == null) {
            Log.w(LOG_TAG, "ImsManager is null when setting TTY mode");
            return;
        }
        try {
            this.mImsManager.setTtyMode(i);
        } catch (ImsException e) {
            loge("setTtyMode : " + e);
            retryGetImsService();
        }
    }

    public void setUiTTYMode(int i, Message message) {
        if (this.mImsManager == null) {
            this.mPhone.sendErrorResponse(message, getImsManagerIsNullException());
            return;
        }
        try {
            this.mImsManager.setUiTTYMode(this.mPhone.getContext(), i, message);
        } catch (ImsException e) {
            loge("setUITTYMode : " + e);
            this.mPhone.sendErrorResponse(message, e);
            retryGetImsService();
        }
    }

    public void setMute(boolean z) {
        this.mDesiredMute = z;
        this.mForegroundCall.setMute(z);
    }

    public boolean getMute() {
        return this.mDesiredMute;
    }

    public void sendDtmf(char c, Message message) {
        log("sendDtmf");
        ImsCall imsCall = this.mForegroundCall.getImsCall();
        if (imsCall != null) {
            imsCall.sendDtmf(c, message);
        }
    }

    public void startDtmf(char c) {
        log("startDtmf");
        ImsCall imsCall = this.mForegroundCall.getImsCall();
        if (imsCall != null) {
            imsCall.startDtmf(c);
        } else {
            loge("startDtmf : no foreground call");
        }
    }

    public void stopDtmf() {
        log("stopDtmf");
        ImsCall imsCall = this.mForegroundCall.getImsCall();
        if (imsCall != null) {
            imsCall.stopDtmf();
        } else {
            loge("stopDtmf : no foreground call");
        }
    }

    public void hangup(ImsPhoneConnection imsPhoneConnection) throws CallStateException {
        log("hangup connection");
        if (imsPhoneConnection.getOwner() != this) {
            throw new CallStateException("ImsPhoneConnection " + imsPhoneConnection + "does not belong to ImsPhoneCallTracker " + this);
        }
        hangup(imsPhoneConnection.getCall());
    }

    public void hangup(ImsPhoneCall imsPhoneCall) throws CallStateException {
        log("hangup call");
        if (imsPhoneCall.getConnections().size() == 0) {
            throw new CallStateException("no connections");
        }
        ImsCall imsCall = imsPhoneCall.getImsCall();
        boolean z = false;
        if (imsPhoneCall == this.mRingingCall) {
            log("(ringing) hangup incoming");
            z = true;
        } else if (imsPhoneCall == this.mForegroundCall) {
            if (imsPhoneCall.isDialingOrAlerting()) {
                log("(foregnd) hangup dialing or alerting...");
            } else {
                log("(foregnd) hangup foreground");
            }
        } else if (imsPhoneCall == this.mBackgroundCall) {
            log("(backgnd) hangup waiting or background");
        } else {
            throw new CallStateException("ImsPhoneCall " + imsPhoneCall + "does not belong to ImsPhoneCallTracker " + this);
        }
        imsPhoneCall.onHangupLocal();
        try {
            if (imsCall != null) {
                if (z) {
                    imsCall.reject(RadioError.OEM_ERROR_4);
                    this.mMetrics.writeOnImsCommand(this.mPhone.getPhoneId(), imsCall.getSession(), 3);
                } else {
                    imsCall.terminate(RadioError.OEM_ERROR_1);
                    this.mMetrics.writeOnImsCommand(this.mPhone.getPhoneId(), imsCall.getSession(), 4);
                }
            } else if (this.mPendingMO != null && imsPhoneCall == this.mForegroundCall) {
                this.mPendingMO.update(null, Call.State.DISCONNECTED);
                this.mPendingMO.onDisconnect();
                removeConnection(this.mPendingMO);
                this.mPendingMO = null;
                updatePhoneState();
                removeMessages(20);
            }
            this.mPhone.notifyPreciseCallStateChanged();
        } catch (ImsException e) {
            throw new CallStateException(e.getMessage());
        }
    }

    protected void callEndCleanupHandOverCallIfAny() {
        if (this.mHandoverCall.mConnections.size() > 0) {
            log("callEndCleanupHandOverCallIfAny, mHandoverCall.mConnections=" + this.mHandoverCall.mConnections);
            this.mHandoverCall.mConnections.clear();
            this.mConnections.clear();
            this.mState = PhoneConstants.State.IDLE;
        }
    }

    void resumeWaitingOrHolding() throws CallStateException {
        log("resumeWaitingOrHolding");
        try {
            if (this.mForegroundCall.getState().isAlive()) {
                ImsCall imsCall = this.mForegroundCall.getImsCall();
                if (imsCall != null) {
                    imsCall.resume();
                    this.mMetrics.writeOnImsCommand(this.mPhone.getPhoneId(), imsCall.getSession(), 6);
                }
                return;
            }
            if (this.mRingingCall.getState() == Call.State.WAITING) {
                if (hasPendingResumeRequest()) {
                    return;
                }
                ImsCall imsCall2 = this.mRingingCall.getImsCall();
                if (imsCall2 != null) {
                    imsCall2.accept(ImsCallProfile.getCallTypeFromVideoState(this.mPendingCallVideoState));
                    this.mMetrics.writeOnImsCommand(this.mPhone.getPhoneId(), imsCall2.getSession(), 2);
                }
                return;
            }
            ImsCall imsCall3 = this.mBackgroundCall.getImsCall();
            if (imsCall3 != null) {
                imsCall3.resume();
                setPendingResumeRequest(true);
                this.mMetrics.writeOnImsCommand(this.mPhone.getPhoneId(), imsCall3.getSession(), 6);
            }
        } catch (ImsException e) {
            throw new CallStateException(e.getMessage());
        }
    }

    public void sendUSSD(String str, Message message) {
        log("sendUSSD");
        try {
            if (this.mUssdSession != null) {
                this.mUssdSession.sendUssd(str);
                AsyncResult.forMessage(message, (Object) null, (Throwable) null);
                message.sendToTarget();
            } else {
                if (this.mImsManager == null) {
                    this.mPhone.sendErrorResponse(message, getImsManagerIsNullException());
                    return;
                }
                String[] strArr = {str};
                ImsCallProfile imsCallProfileCreateCallProfile = this.mImsManager.createCallProfile(1, 2);
                imsCallProfileCreateCallProfile.setCallExtraInt("dialstring", 2);
                this.mUssdSession = this.mImsManager.makeCall(imsCallProfileCreateCallProfile, strArr, this.mImsUssdListener);
            }
        } catch (ImsException e) {
            loge("sendUSSD : " + e);
            this.mPhone.sendErrorResponse(message, e);
            retryGetImsService();
        }
    }

    public void cancelUSSD() {
        if (this.mUssdSession == null) {
            return;
        }
        try {
            this.mUssdSession.terminate(RadioError.OEM_ERROR_1);
        } catch (ImsException e) {
        }
    }

    protected synchronized ImsPhoneConnection findConnection(ImsCall imsCall) {
        for (ImsPhoneConnection imsPhoneConnection : this.mConnections) {
            if (imsPhoneConnection.getImsCall() == imsCall) {
                return imsPhoneConnection;
            }
        }
        return null;
    }

    protected synchronized void removeConnection(ImsPhoneConnection imsPhoneConnection) {
        boolean z;
        this.mConnections.remove(imsPhoneConnection);
        if (this.mIsInEmergencyCall) {
            Iterator<ImsPhoneConnection> it = this.mConnections.iterator();
            while (true) {
                z = true;
                if (it.hasNext()) {
                    ImsPhoneConnection next = it.next();
                    if (next != null && next.isEmergency()) {
                        break;
                    }
                } else {
                    z = false;
                    break;
                }
            }
            if (!z) {
                this.mIsInEmergencyCall = false;
                this.mPhone.sendEmergencyCallStateChange(false);
                startRttEmcGuardTimer();
            }
        }
    }

    protected synchronized void addConnection(ImsPhoneConnection imsPhoneConnection) {
        this.mConnections.add(imsPhoneConnection);
        if (imsPhoneConnection.isEmergency()) {
            this.mIsInEmergencyCall = true;
            this.mPhone.sendEmergencyCallStateChange(true);
        }
    }

    protected void processCallStateChange(ImsCall imsCall, Call.State state, int i) {
        log("processCallStateChange " + imsCall + " state=" + state + " cause=" + i);
        processCallStateChange(imsCall, state, i, false);
    }

    protected void processCallStateChange(ImsCall imsCall, Call.State state, int i, boolean z) {
        ImsPhoneConnection imsPhoneConnectionFindConnection;
        log("processCallStateChange state=" + state + " cause=" + i + " ignoreState=" + z);
        if (imsCall == null || (imsPhoneConnectionFindConnection = findConnection(imsCall)) == null) {
            return;
        }
        imsPhoneConnectionFindConnection.updateMediaCapabilities(imsCall);
        if (z) {
            imsPhoneConnectionFindConnection.updateAddressDisplay(imsCall);
            imsPhoneConnectionFindConnection.updateExtras(imsCall);
            maybeSetVideoCallProvider(imsPhoneConnectionFindConnection, imsCall);
            return;
        }
        boolean zUpdate = imsPhoneConnectionFindConnection.update(imsCall, state);
        if (state == Call.State.DISCONNECTED) {
            zUpdate = imsPhoneConnectionFindConnection.onDisconnect(i) || zUpdate;
            imsPhoneConnectionFindConnection.getCall().detach(imsPhoneConnectionFindConnection);
            removeConnection(imsPhoneConnectionFindConnection);
        }
        if (!zUpdate || imsPhoneConnectionFindConnection.getCall() == this.mHandoverCall) {
            return;
        }
        updatePhoneState();
        checkRttCallType();
        this.mPhone.notifyPreciseCallStateChanged();
    }

    private void maybeSetVideoCallProvider(ImsPhoneConnection imsPhoneConnection, ImsCall imsCall) {
        if (imsPhoneConnection.getVideoProvider() != null || imsCall.getCallSession().getVideoCallProvider() == null) {
            return;
        }
        try {
            setVideoCallProvider(imsPhoneConnection, imsCall);
        } catch (RemoteException e) {
            loge("maybeSetVideoCallProvider: exception " + e);
        }
    }

    @VisibleForTesting
    public void addReasonCodeRemapping(Integer num, String str, Integer num2) {
        this.mImsReasonCodeMap.put(new Pair<>(num, str), num2);
    }

    @VisibleForTesting
    public int maybeRemapReasonCode(ImsReasonInfo imsReasonInfo) {
        int code = imsReasonInfo.getCode();
        Pair pair = new Pair(Integer.valueOf(code), imsReasonInfo.getExtraMessage());
        Pair pair2 = new Pair(null, imsReasonInfo.getExtraMessage());
        if (this.mImsReasonCodeMap.containsKey(pair)) {
            int iIntValue = this.mImsReasonCodeMap.get(pair).intValue();
            log("maybeRemapReasonCode : fromCode = " + imsReasonInfo.getCode() + " ; message = " + imsReasonInfo.getExtraMessage() + " ; toCode = " + iIntValue);
            return iIntValue;
        }
        if (this.mImsReasonCodeMap.containsKey(pair2)) {
            int iIntValue2 = this.mImsReasonCodeMap.get(pair2).intValue();
            log("maybeRemapReasonCode : fromCode(wildcard) = " + imsReasonInfo.getCode() + " ; message = " + imsReasonInfo.getExtraMessage() + " ; toCode = " + iIntValue2);
            return iIntValue2;
        }
        return code;
    }

    @VisibleForTesting
    public int getDisconnectCauseFromReasonInfo(ImsReasonInfo imsReasonInfo, Call.State state) {
        switch (maybeRemapReasonCode(imsReasonInfo)) {
            case 106:
            case 121:
            case 122:
            case 123:
            case 124:
            case 131:
            case 132:
            case 144:
                break;
            case 108:
                break;
            case 111:
                break;
            case 112:
            case RadioError.OEM_ERROR_5:
                if (state == Call.State.DIALING) {
                }
                break;
            case 143:
            case 1404:
                break;
            case 201:
            case 202:
            case 203:
            case 335:
                break;
            case 240:
                break;
            case 241:
                break;
            case 243:
                break;
            case 244:
                break;
            case 245:
                break;
            case 246:
                break;
            case LastCallFailCause.RADIO_OFF:
                break;
            case LastCallFailCause.OUT_OF_SERVICE:
                break;
            case LastCallFailCause.NO_VALID_SIM:
                break;
            case LastCallFailCause.RADIO_INTERNAL_ERROR:
                break;
            case LastCallFailCause.NETWORK_RESP_TIMEOUT:
                break;
            case 321:
            case 331:
            case 340:
            case 361:
            case 362:
                break;
            case 332:
                break;
            case 333:
            case 352:
            case 354:
                break;
            case 337:
            case 341:
                break;
            case 338:
                break;
            case 363:
                break;
            case 364:
                break;
            case RadioError.OEM_ERROR_1:
                break;
            case RadioError.OEM_ERROR_10:
                break;
            case 1014:
                break;
            case 1016:
                break;
            case 1403:
                break;
            case 1405:
                break;
            case 1406:
                break;
            case 1407:
                break;
            case 1512:
                break;
            case 1514:
                break;
            case 1515:
                break;
        }
        return 12;
    }

    protected int getPreciseDisconnectCauseFromReasonInfo(ImsReasonInfo imsReasonInfo) {
        return PRECISE_CAUSE_MAP.get(maybeRemapReasonCode(imsReasonInfo), 65535);
    }

    protected boolean isPhoneInEcbMode() {
        return this.mPhone != null && this.mPhone.isInEcm();
    }

    protected void dialPendingMO() {
        boolean zIsPhoneInEcbMode = isPhoneInEcbMode();
        boolean zIsEmergency = this.mPendingMO.isEmergency();
        if (!zIsPhoneInEcbMode || (zIsPhoneInEcbMode && zIsEmergency)) {
            sendEmptyMessage(20);
        } else {
            sendEmptyMessage(21);
        }
    }

    public ImsUtInterface getUtInterface() throws ImsException {
        if (this.mImsManager == null) {
            throw getImsManagerIsNullException();
        }
        return this.mImsManager.getSupplementaryServiceConfiguration();
    }

    protected void transferHandoverConnections(ImsPhoneCall imsPhoneCall) {
        if (imsPhoneCall.mConnections != null) {
            for (Connection connection : imsPhoneCall.mConnections) {
                connection.mPreHandoverState = imsPhoneCall.mState;
                log("Connection state before handover is " + connection.getStateBeforeHandover());
                setMultiPartyState(connection);
            }
        }
        if (this.mHandoverCall.mConnections == null) {
            this.mHandoverCall.mConnections = imsPhoneCall.mConnections;
        } else {
            this.mHandoverCall.mConnections.addAll(imsPhoneCall.mConnections);
        }
        if (this.mHandoverCall.mConnections != null) {
            if (imsPhoneCall.getImsCall() != null) {
                imsPhoneCall.getImsCall().close();
            }
            Iterator<Connection> it = this.mHandoverCall.mConnections.iterator();
            while (it.hasNext()) {
                ImsPhoneConnection imsPhoneConnection = (ImsPhoneConnection) it.next();
                imsPhoneConnection.changeParent(this.mHandoverCall);
                imsPhoneConnection.releaseWakeLock();
            }
        }
        if (imsPhoneCall.getState().isAlive()) {
            log("Call is alive and state is " + imsPhoneCall.mState);
            this.mHandoverCall.mState = imsPhoneCall.mState;
        }
        imsPhoneCall.mConnections.clear();
        imsPhoneCall.mState = Call.State.IDLE;
        resetRingBackTone(imsPhoneCall);
    }

    protected void notifySrvccState(Call.SrvccState srvccState) {
        log("notifySrvccState state=" + srvccState);
        this.mSrvccState = srvccState;
        if (this.mSrvccState == Call.SrvccState.COMPLETED) {
            transferHandoverConnections(this.mForegroundCall);
            transferHandoverConnections(this.mBackgroundCall);
            transferHandoverConnections(this.mRingingCall);
            updateForSrvccCompleted();
        }
    }

    @Override
    public void handleMessage(Message message) {
        log("handleMessage what=" + message.what);
        switch (message.what) {
            case 14:
                if (this.pendingCallInEcm) {
                    dialInternal(this.mPendingMO, this.pendingCallClirMode, this.mPendingCallVideoState, this.mPendingIntentExtras);
                    this.mPendingIntentExtras = null;
                    this.pendingCallInEcm = false;
                }
                this.mPhone.unsetOnEcbModeExitResponse(this);
                return;
            case 15:
            case 16:
            case 17:
            case 24:
            default:
                return;
            case 18:
                if (this.mPendingMO != null) {
                    this.mPendingMO.onDisconnect();
                    removeConnection(this.mPendingMO);
                    this.mPendingMO = null;
                }
                this.mPendingIntentExtras = null;
                updatePhoneState();
                this.mPhone.notifyPreciseCallStateChanged();
                return;
            case 19:
                try {
                    resumeWaitingOrHolding();
                    return;
                } catch (CallStateException e) {
                    loge("handleMessage EVENT_RESUME_BACKGROUND exception=" + e);
                    return;
                }
            case 20:
                dialInternal(this.mPendingMO, this.mClirMode, this.mPendingCallVideoState, this.mPendingIntentExtras);
                this.mPendingIntentExtras = null;
                return;
            case 21:
                if (this.mPendingMO != null) {
                    try {
                        getEcbmInterface().exitEmergencyCallbackMode();
                        this.mPhone.setOnEcbModeExitResponse(this, 14, null);
                        this.pendingCallClirMode = this.mClirMode;
                        this.pendingCallInEcm = true;
                        return;
                    } catch (ImsException e2) {
                        e2.printStackTrace();
                        this.mPendingMO.setDisconnectCause(36);
                        sendEmptyMessageDelayed(18, 500L);
                        return;
                    }
                }
                return;
            case 22:
                AsyncResult asyncResult = (AsyncResult) message.obj;
                ImsCall imsCall = (ImsCall) asyncResult.userObj;
                Long lValueOf = Long.valueOf(((Long) asyncResult.result).longValue());
                log("VT data usage update. usage = " + lValueOf + ", imsCall = " + imsCall);
                if (lValueOf.longValue() > 0) {
                    updateVtDataUsage(imsCall, lValueOf.longValue());
                    return;
                }
                return;
            case 23:
                AsyncResult asyncResult2 = (AsyncResult) message.obj;
                if (asyncResult2.result instanceof Pair) {
                    Pair pair = (Pair) asyncResult2.result;
                    onDataEnabledChanged(((Boolean) pair.first).booleanValue(), ((Integer) pair.second).intValue());
                    return;
                }
                return;
            case 25:
                if (message.obj instanceof ImsCall) {
                    ImsCall imsCall2 = (ImsCall) message.obj;
                    if (imsCall2 != this.mForegroundCall.getImsCall()) {
                        Rlog.i(LOG_TAG, "handoverCheck: no longer FG; check skipped.");
                        unregisterForConnectivityChanges();
                        return;
                    } else {
                        if (!imsCall2.isWifiCall()) {
                            ImsPhoneConnection imsPhoneConnectionFindConnection = findConnection(imsCall2);
                            if (imsPhoneConnectionFindConnection != null) {
                                Rlog.i(LOG_TAG, "handoverCheck: handover failed.");
                                imsPhoneConnectionFindConnection.onHandoverToWifiFailed();
                            }
                            if (imsCall2.isVideoCall() && imsPhoneConnectionFindConnection.getDisconnectCause() == 0) {
                                registerForConnectivityChanges();
                                return;
                            }
                            return;
                        }
                        return;
                    }
                }
                return;
            case 26:
                SomeArgs someArgs = (SomeArgs) message.obj;
                try {
                    handleFeatureCapabilityChanged((ImsFeature.Capabilities) someArgs.arg1);
                    return;
                } finally {
                    someArgs.recycle();
                }
            case 27:
                AsyncResult asyncResult3 = (AsyncResult) message.obj;
                ImsPhoneMmiCode imsPhoneMmiCode = new ImsPhoneMmiCode(this.mPhone);
                try {
                    imsPhoneMmiCode.setIsSsInfo(true);
                    imsPhoneMmiCode.processImsSsData(asyncResult3);
                    return;
                } catch (ImsException e3) {
                    Rlog.e(LOG_TAG, "Exception in parsing SS Data: " + e3);
                    return;
                }
        }
    }

    private void updateVtDataUsage(ImsCall imsCall, long j) {
        long jLongValue;
        if (this.mVtDataUsageMap.containsKey(Integer.valueOf(imsCall.uniqueId))) {
            jLongValue = this.mVtDataUsageMap.get(Integer.valueOf(imsCall.uniqueId)).longValue();
        } else {
            jLongValue = 0;
        }
        long j2 = j - jLongValue;
        this.mVtDataUsageMap.put(Integer.valueOf(imsCall.uniqueId), Long.valueOf(j));
        log("updateVtDataUsage: call=" + imsCall + ", delta=" + j2);
        long jElapsedRealtime = SystemClock.elapsedRealtime();
        boolean dataRoaming = this.mPhone.getServiceState().getDataRoaming();
        NetworkStats networkStats = new NetworkStats(jElapsedRealtime, 1);
        networkStats.combineAllValues(this.mVtDataUsageSnapshot);
        long j3 = j2 / 2;
        networkStats.combineValues(new NetworkStats.Entry(getVtInterface(), -1, 1, 0, 1, dataRoaming ? 1 : 0, 1, j3, 0L, j3, 0L, 0L));
        this.mVtDataUsageSnapshot = networkStats;
        NetworkStats networkStats2 = new NetworkStats(jElapsedRealtime, 1);
        networkStats2.combineAllValues(this.mVtDataUsageUidSnapshot);
        if (this.mDefaultDialerUid.get() == -1) {
            this.mDefaultDialerUid.set(getPackageUid(this.mPhone.getContext(), ((TelecomManager) this.mPhone.getContext().getSystemService("telecom")).getDefaultDialerPackage()));
        }
        networkStats2.combineValues(new NetworkStats.Entry(getVtInterface(), this.mDefaultDialerUid.get(), 1, 0, 1, dataRoaming ? 1 : 0, 1, j3, 0L, j3, 0L, 0L));
        this.mVtDataUsageUidSnapshot = networkStats2;
    }

    @Override
    protected void log(String str) {
        Rlog.d(LOG_TAG, "[" + this.mPhone.getPhoneId() + "] " + str);
    }

    protected void loge(String str) {
        Rlog.e(LOG_TAG, "[" + this.mPhone.getPhoneId() + "] " + str);
    }

    public void logState() {
        if (!VERBOSE_STATE_LOGGING) {
            return;
        }
        Rlog.v(LOG_TAG, "Current IMS PhoneCall State:\n Foreground: " + this.mForegroundCall + "\n Background: " + this.mBackgroundCall + "\n Ringing: " + this.mRingingCall + "\n Handover: " + this.mHandoverCall + "\n");
    }

    @Override
    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("ImsPhoneCallTracker extends:");
        super.dump(fileDescriptor, printWriter, strArr);
        printWriter.println(" mVoiceCallEndedRegistrants=" + this.mVoiceCallEndedRegistrants);
        printWriter.println(" mVoiceCallStartedRegistrants=" + this.mVoiceCallStartedRegistrants);
        printWriter.println(" mRingingCall=" + this.mRingingCall);
        printWriter.println(" mForegroundCall=" + this.mForegroundCall);
        printWriter.println(" mBackgroundCall=" + this.mBackgroundCall);
        printWriter.println(" mHandoverCall=" + this.mHandoverCall);
        printWriter.println(" mPendingMO=" + this.mPendingMO);
        printWriter.println(" mPhone=" + this.mPhone);
        printWriter.println(" mDesiredMute=" + this.mDesiredMute);
        printWriter.println(" mState=" + this.mState);
        printWriter.println(" mMmTelCapabilities=" + this.mMmTelCapabilities);
        printWriter.println(" mDefaultDialerUid=" + this.mDefaultDialerUid.get());
        printWriter.println(" mVtDataUsageSnapshot=" + this.mVtDataUsageSnapshot);
        printWriter.println(" mVtDataUsageUidSnapshot=" + this.mVtDataUsageUidSnapshot);
        printWriter.flush();
        printWriter.println("++++++++++++++++++++++++++++++++");
        try {
            if (this.mImsManager != null) {
                this.mImsManager.dump(fileDescriptor, printWriter, strArr);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (this.mConnections != null && this.mConnections.size() > 0) {
            printWriter.println("mConnections:");
            for (int i = 0; i < this.mConnections.size(); i++) {
                printWriter.println("  [" + i + "]: " + this.mConnections.get(i));
            }
        }
    }

    @Override
    protected void handlePollCalls(AsyncResult asyncResult) {
    }

    public ImsEcbm getEcbmInterface() throws ImsException {
        if (this.mImsManager == null) {
            throw getImsManagerIsNullException();
        }
        return this.mImsManager.getEcbmInterface();
    }

    public ImsMultiEndpoint getMultiEndpointInterface() throws ImsException {
        if (this.mImsManager == null) {
            throw getImsManagerIsNullException();
        }
        try {
            return this.mImsManager.getMultiEndpointInterface();
        } catch (ImsException e) {
            if (e.getCode() == 902) {
                return null;
            }
            throw e;
        }
    }

    public boolean isInEmergencyCall() {
        return this.mIsInEmergencyCall;
    }

    public boolean isVolteEnabled() {
        return (getImsRegistrationTech() == 0) && this.mMmTelCapabilities.isCapable(1);
    }

    public boolean isVowifiEnabled() {
        return (getImsRegistrationTech() == 1) && this.mMmTelCapabilities.isCapable(1);
    }

    public boolean isVideoCallEnabled() {
        return this.mMmTelCapabilities.isCapable(2);
    }

    @Override
    public PhoneConstants.State getState() {
        return this.mState;
    }

    public int getImsRegistrationTech() {
        if (this.mImsManager != null) {
            return this.mImsManager.getRegistrationTech();
        }
        return -1;
    }

    protected void retryGetImsService() {
        if (this.mImsManager.isServiceAvailable()) {
            return;
        }
        this.mImsManagerConnector.connect();
    }

    protected void setVideoCallProvider(ImsPhoneConnection imsPhoneConnection, ImsCall imsCall) throws RemoteException {
        IImsVideoCallProvider videoCallProvider = imsCall.getCallSession().getVideoCallProvider();
        if (videoCallProvider != null) {
            boolean z = this.mPhone.getContext().getResources().getBoolean(R.^attr-private.pointerIconVectorStrokeInverse);
            ImsVideoCallProviderWrapper imsVideoCallProviderWrapper = new ImsVideoCallProviderWrapper(videoCallProvider);
            if (z) {
                imsVideoCallProviderWrapper.setUseVideoPauseWorkaround(z);
            }
            imsPhoneConnection.setVideoProvider(imsVideoCallProviderWrapper);
            imsVideoCallProviderWrapper.registerForDataUsageUpdate(this, 22, imsCall);
            imsVideoCallProviderWrapper.addImsVideoProviderCallback(imsPhoneConnection);
        }
    }

    public boolean isUtEnabled() {
        return this.mMmTelCapabilities.isCapable(4);
    }

    protected String cleanseInstantLetteringMessage(String str) {
        CarrierConfigManager carrierConfigManager;
        PersistableBundle configForSubId;
        if (TextUtils.isEmpty(str) || (carrierConfigManager = (CarrierConfigManager) this.mPhone.getContext().getSystemService("carrier_config")) == null || (configForSubId = carrierConfigManager.getConfigForSubId(this.mPhone.getSubId())) == null) {
            return str;
        }
        String string = configForSubId.getString("carrier_instant_lettering_invalid_chars_string");
        if (!TextUtils.isEmpty(string)) {
            str = str.replaceAll(string, "");
        }
        String string2 = configForSubId.getString("carrier_instant_lettering_escaped_chars_string");
        if (!TextUtils.isEmpty(string2)) {
            return escapeChars(string2, str);
        }
        return str;
    }

    private String escapeChars(String str, String str2) {
        StringBuilder sb = new StringBuilder();
        for (char c : str2.toCharArray()) {
            if (str.contains(Character.toString(c))) {
                sb.append("\\");
            }
            sb.append(c);
        }
        return sb.toString();
    }

    @Override
    public void pullExternalCall(String str, int i, int i2) {
        Bundle bundle = new Bundle();
        bundle.putBoolean("CallPull", true);
        bundle.putInt(ImsExternalCallTracker.EXTRA_IMS_EXTERNAL_CALL_ID, i2);
        try {
            this.mPhone.notifyUnknownConnection(dial(str, i, bundle));
        } catch (CallStateException e) {
            loge("pullExternalCall failed - " + e);
        }
    }

    protected ImsException getImsManagerIsNullException() {
        return new ImsException("no ims manager", 102);
    }

    protected boolean shouldDisconnectActiveCallOnAnswer(ImsCall imsCall, ImsCall imsCall2) {
        if (imsCall == null || imsCall2 == null || !this.mDropVideoCallWhenAnsweringAudioCall) {
            return false;
        }
        boolean z = imsCall.isVideoCall() || (this.mTreatDowngradedVideoCallsAsVideoCalls && imsCall.wasVideoCall());
        boolean zIsWifiCall = imsCall.isWifiCall();
        boolean z2 = this.mImsManager.isWfcEnabledByPlatform() && this.mImsManager.isWfcEnabledByUser();
        boolean z3 = !imsCall2.isVideoCall();
        log("shouldDisconnectActiveCallOnAnswer : isActiveCallVideo=" + z + " isActiveCallOnWifi=" + zIsWifiCall + " isIncomingCallAudio=" + z3 + " isVowifiEnabled=" + z2);
        return z && zIsWifiCall && z3 && !z2;
    }

    public NetworkStats getVtDataUsage(boolean z) {
        if (this.mState != PhoneConstants.State.IDLE) {
            Iterator<ImsPhoneConnection> it = this.mConnections.iterator();
            while (it.hasNext()) {
                Connection.VideoProvider videoProvider = it.next().getVideoProvider();
                if (videoProvider != null) {
                    videoProvider.onRequestConnectionDataUsage();
                }
            }
        }
        return z ? this.mVtDataUsageUidSnapshot : this.mVtDataUsageSnapshot;
    }

    public void registerPhoneStateListener(PhoneStateListener phoneStateListener) {
        this.mPhoneStateListeners.add(phoneStateListener);
    }

    public void unregisterPhoneStateListener(PhoneStateListener phoneStateListener) {
        this.mPhoneStateListeners.remove(phoneStateListener);
    }

    private void notifyPhoneStateChanged(PhoneConstants.State state, PhoneConstants.State state2) {
        Iterator<PhoneStateListener> it = this.mPhoneStateListeners.iterator();
        while (it.hasNext()) {
            it.next().onPhoneStateChanged(state, state2);
        }
    }

    protected void modifyVideoCall(ImsCall imsCall, int i) {
        ImsPhoneConnection imsPhoneConnectionFindConnection = findConnection(imsCall);
        if (imsPhoneConnectionFindConnection != null) {
            int videoState = imsPhoneConnectionFindConnection.getVideoState();
            if (imsPhoneConnectionFindConnection.getVideoProvider() != null) {
                imsPhoneConnectionFindConnection.getVideoProvider().onSendSessionModifyRequest(new VideoProfile(videoState), new VideoProfile(i));
            }
        }
    }

    protected void onDataEnabledChanged(boolean z, int i) {
        log("onDataEnabledChanged: enabled=" + z + ", reason=" + i);
        this.mIsDataEnabled = z;
        if (!this.mIsViLteDataMetered) {
            StringBuilder sb = new StringBuilder();
            sb.append("Ignore data ");
            sb.append(z ? "enabled" : "disabled");
            sb.append(" - carrier policy indicates that data is not metered for ViLTE calls.");
            log(sb.toString());
            return;
        }
        if (this.mIsDataEnabled && isRoamingOnAndRoamingSettingOff()) {
            log("Ignore data on when roaming");
            return;
        }
        Iterator<ImsPhoneConnection> it = this.mConnections.iterator();
        while (it.hasNext()) {
            it.next().handleDataEnabledChange(z);
        }
        int i2 = 1406;
        if (i == 3) {
            i2 = 1405;
        } else if (i == 2) {
        }
        maybeNotifyDataDisabled(z, i2);
        handleDataEnabledChange(z, i2);
        if (!this.mShouldUpdateImsConfigOnDisconnect && i != 0 && this.mCarrierConfigLoaded && this.mImsManager != null) {
            this.mImsManager.updateImsServiceConfig(true);
        }
    }

    protected void maybeNotifyDataDisabled(boolean z, int i) {
        if (!z) {
            for (ImsPhoneConnection imsPhoneConnection : this.mConnections) {
                ImsCall imsCall = imsPhoneConnection.getImsCall();
                if (imsCall != null && imsCall.isVideoCall() && !imsCall.isWifiCall() && imsPhoneConnection.hasCapabilities(3)) {
                    if (i == 1406) {
                        imsPhoneConnection.onConnectionEvent("android.telephony.event.EVENT_DOWNGRADE_DATA_DISABLED", null);
                    } else if (i == 1405) {
                        imsPhoneConnection.onConnectionEvent("android.telephony.event.EVENT_DOWNGRADE_DATA_LIMIT_REACHED", null);
                    }
                }
            }
        }
    }

    protected void handleDataEnabledChange(boolean z, int i) {
        if (!z) {
            for (ImsPhoneConnection imsPhoneConnection : this.mConnections) {
                ImsCall imsCall = imsPhoneConnection.getImsCall();
                if (imsCall != null && imsCall.isVideoCall() && !imsCall.isWifiCall()) {
                    log("handleDataEnabledChange - downgrading " + imsPhoneConnection);
                    downgradeVideoCall(i, imsPhoneConnection);
                }
            }
            return;
        }
        if (this.mSupportPauseVideo) {
            for (ImsPhoneConnection imsPhoneConnection2 : this.mConnections) {
                log("handleDataEnabledChange - resuming " + imsPhoneConnection2);
                if (VideoProfile.isPaused(imsPhoneConnection2.getVideoState()) && imsPhoneConnection2.wasVideoPausedFromSource(2)) {
                    imsPhoneConnection2.resumeVideo(2);
                }
            }
            this.mShouldUpdateImsConfigOnDisconnect = false;
        }
    }

    private void downgradeVideoCall(int i, ImsPhoneConnection imsPhoneConnection) {
        ImsCall imsCall = imsPhoneConnection.getImsCall();
        if (imsCall != null) {
            if (imsPhoneConnection.hasCapabilities(3)) {
                modifyVideoCall(imsCall, 0);
                return;
            }
            if (this.mSupportPauseVideo && i != 1407 && isCarrierPauseAllowed(imsCall)) {
                this.mShouldUpdateImsConfigOnDisconnect = true;
                imsPhoneConnection.pauseVideo(2);
                return;
            }
            try {
                imsCall.terminate(RadioError.OEM_ERROR_1, i);
            } catch (ImsException e) {
                loge("Couldn't terminate call " + imsCall);
            }
        }
    }

    protected void resetImsCapabilities() {
        log("Resetting Capabilities...");
        this.mMmTelCapabilities = new MmTelFeature.MmTelCapabilities();
    }

    protected boolean isWifiConnected() {
        NetworkInfo activeNetworkInfo;
        ConnectivityManager connectivityManager = (ConnectivityManager) this.mPhone.getContext().getSystemService("connectivity");
        return connectivityManager != null && (activeNetworkInfo = connectivityManager.getActiveNetworkInfo()) != null && activeNetworkInfo.isConnected() && activeNetworkInfo.getType() == 1;
    }

    private void registerForConnectivityChanges() {
        ConnectivityManager connectivityManager;
        if (!this.mIsMonitoringConnectivity && this.mNotifyVtHandoverToWifiFail && (connectivityManager = (ConnectivityManager) this.mPhone.getContext().getSystemService("connectivity")) != null) {
            Rlog.i(LOG_TAG, "registerForConnectivityChanges");
            NetworkCapabilities networkCapabilities = new NetworkCapabilities();
            networkCapabilities.addTransportType(1);
            NetworkRequest.Builder builder = new NetworkRequest.Builder();
            builder.setCapabilities(networkCapabilities);
            connectivityManager.registerNetworkCallback(builder.build(), this.mNetworkCallback);
            this.mIsMonitoringConnectivity = true;
        }
    }

    private void unregisterForConnectivityChanges() {
        ConnectivityManager connectivityManager;
        if (this.mIsMonitoringConnectivity && this.mNotifyVtHandoverToWifiFail && (connectivityManager = (ConnectivityManager) this.mPhone.getContext().getSystemService("connectivity")) != null) {
            Rlog.i(LOG_TAG, "unregisterForConnectivityChanges");
            connectivityManager.unregisterNetworkCallback(this.mNetworkCallback);
            this.mIsMonitoringConnectivity = false;
        }
    }

    private void scheduleHandoverCheck() {
        ImsCall imsCall = this.mForegroundCall.getImsCall();
        ImsPhoneConnection firstConnection = this.mForegroundCall.getFirstConnection();
        if (this.mNotifyVtHandoverToWifiFail && imsCall != null && imsCall.isVideoCall() && firstConnection != null && firstConnection.getDisconnectCause() == 0 && !hasMessages(25)) {
            Rlog.i(LOG_TAG, "scheduleHandoverCheck: schedule");
            sendMessageDelayed(obtainMessage(25, imsCall), 60000L);
        }
    }

    public boolean isCarrierDowngradeOfVtCallSupported() {
        return this.mSupportDowngradeVtToAudio;
    }

    @VisibleForTesting
    public void setDataEnabled(boolean z) {
        this.mIsDataEnabled = z;
    }

    private void handleFeatureCapabilityChanged(ImsFeature.Capabilities capabilities) {
        boolean zIsVideoCallEnabled = isVideoCallEnabled();
        StringBuilder sb = new StringBuilder(120);
        sb.append("handleFeatureCapabilityChanged: ");
        sb.append(capabilities);
        this.mMmTelCapabilities = new MmTelFeature.MmTelCapabilities(capabilities);
        boolean zIsVideoCallEnabled2 = isVideoCallEnabled();
        boolean z = zIsVideoCallEnabled != zIsVideoCallEnabled2;
        sb.append(" isVideoEnabledStateChanged=");
        sb.append(z);
        if (z) {
            log("handleFeatureCapabilityChanged - notifyForVideoCapabilityChanged=" + zIsVideoCallEnabled2);
            this.mPhone.notifyForVideoCapabilityChanged(zIsVideoCallEnabled2);
        }
        log(sb.toString());
        log("handleFeatureCapabilityChanged: isVolteEnabled=" + isVolteEnabled() + ", isVideoCallEnabled=" + isVideoCallEnabled() + ", isVowifiEnabled=" + isVowifiEnabled() + ", isUtEnabled=" + isUtEnabled());
        this.mPhone.onFeatureCapabilityChanged();
        this.mMetrics.writeOnImsCapabilities(this.mPhone.getPhoneId(), getImsRegistrationTech(), this.mMmTelCapabilities);
    }

    @VisibleForTesting
    public void onCallHoldReceived(ImsCall imsCall) {
        log("onCallHoldReceived");
        ImsPhoneConnection imsPhoneConnectionFindConnection = findConnection(imsCall);
        if (imsPhoneConnectionFindConnection != null) {
            if (!this.mOnHoldToneStarted && ((ImsPhoneCall.isLocalTone(imsCall) || this.mAlwaysPlayRemoteHoldTone) && imsPhoneConnectionFindConnection.getState() == Call.State.ACTIVE)) {
                this.mPhone.startOnHoldTone(imsPhoneConnectionFindConnection);
                this.mOnHoldToneStarted = true;
                this.mOnHoldToneId = System.identityHashCode(imsPhoneConnectionFindConnection);
            }
            imsPhoneConnectionFindConnection.onConnectionEvent("android.telecom.event.CALL_REMOTELY_HELD", null);
            if (this.mPhone.getContext().getResources().getBoolean(R.^attr-private.pointerIconVectorStrokeInverse) && this.mSupportPauseVideo && VideoProfile.isVideo(imsPhoneConnectionFindConnection.getVideoState())) {
                imsPhoneConnectionFindConnection.changeToPausedState();
            }
        }
        mtkNotifyRemoteHeld(imsPhoneConnectionFindConnection, true);
        SuppServiceNotification suppServiceNotification = new SuppServiceNotification();
        suppServiceNotification.notificationType = 1;
        suppServiceNotification.code = 2;
        this.mPhone.notifySuppSvcNotification(suppServiceNotification);
        this.mMetrics.writeOnImsCallHoldReceived(this.mPhone.getPhoneId(), imsCall.getCallSession());
    }

    @VisibleForTesting
    public void setAlwaysPlayRemoteHoldTone(boolean z) {
        this.mAlwaysPlayRemoteHoldTone = z;
    }

    protected ImsPhoneConnection makeImsPhoneConnectionForMO(String str, boolean z) {
        return new ImsPhoneConnection(this.mPhone, checkForTestEmergencyNumber(str), this, this.mForegroundCall, z);
    }

    protected ImsPhoneConnection makeImsPhoneConnectionForMT(ImsCall imsCall, boolean z) {
        return new ImsPhoneConnection(this.mPhone, imsCall, this, z ? this.mForegroundCall : this.mRingingCall, z);
    }

    protected ImsCall takeCall(IImsCallSession iImsCallSession, Bundle bundle) throws ImsException {
        return this.mImsManager.takeCall(iImsCallSession, bundle, this.mImsCallListener);
    }

    protected void mtkNotifyRemoteHeld(ImsPhoneConnection imsPhoneConnection, boolean z) {
    }

    protected boolean isEmergencyNumber(String str) {
        return this.mPhoneNumberUtilsProxy.isEmergencyNumber(str);
    }

    protected void checkforCsfb() throws CallStateException {
    }

    protected void resetFlagWhenSwitchFailed() {
    }

    protected void setPendingResumeRequest(boolean z) {
    }

    protected boolean hasPendingResumeRequest() {
        return false;
    }

    protected boolean canDailOnCallTerminated() {
        return this.mPendingMO != null;
    }

    protected void setRedialAsEcc(int i) {
    }

    protected void setVendorDisconnectCause(ImsPhoneConnection imsPhoneConnection, ImsReasonInfo imsReasonInfo) {
    }

    protected int updateDisconnectCause(int i, ImsPhoneConnection imsPhoneConnection) {
        return i;
    }

    protected void setMultiPartyState(com.android.internal.telephony.Connection connection) {
    }

    protected void resetRingBackTone(ImsPhoneCall imsPhoneCall) {
    }

    protected void updateForSrvccCompleted() {
    }

    protected void logDebugMessagesWithOpFormat(String str, String str2, ImsPhoneConnection imsPhoneConnection, String str3) {
    }

    protected void logDebugMessagesWithDumpFormat(String str, ImsPhoneConnection imsPhoneConnection, String str2) {
    }

    protected void switchWfcModeIfRequired(ImsManager imsManager, boolean z, boolean z2) {
    }

    protected AsyncResult getCallStateChangeAsyncResult() {
        return new AsyncResult((Object) null, (Object) null, (Throwable) null);
    }

    protected boolean isCarrierPauseAllowed(ImsCall imsCall) {
        return true;
    }

    protected void releasePendingMOIfRequired() {
    }

    protected boolean isRoamingOnAndRoamingSettingOff() {
        return false;
    }

    protected void checkIncomingRtt(Intent intent, ImsCall imsCall, ImsPhoneConnection imsPhoneConnection) {
    }

    protected void checkRttCallType() {
    }

    protected void startRttEmcGuardTimer() {
    }

    protected String getVtInterface() {
        return "vt_data0";
    }
}
