package com.android.server.telecom;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.Trace;
import android.os.UserHandle;
import android.provider.ContactsContract;
import android.telecom.Connection;
import android.telecom.DisconnectCause;
import android.telecom.GatewayInfo;
import android.telecom.Log;
import android.telecom.Logging.EventManager;
import android.telecom.ParcelableConference;
import android.telecom.ParcelableConnection;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.Response;
import android.telecom.StatusHints;
import android.telecom.TelecomManager;
import android.telecom.VideoProfile;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.StatsLog;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telecom.IVideoProvider;
import com.android.internal.telephony.CallerInfo;
import com.android.internal.telephony.SmsApplication;
import com.android.internal.util.Preconditions;
import com.android.server.telecom.Analytics;
import com.android.server.telecom.CallerInfoLookupHelper;
import com.android.server.telecom.ConnectionServiceFocusManager;
import com.android.server.telecom.TelecomSystem;
import com.mediatek.server.telecom.CallRecorderManager;
import com.mediatek.server.telecom.MtkUtil;
import com.mediatek.server.telecom.SuppMessageHelper;
import com.mediatek.server.telecom.ext.ExtensionManager;
import com.mediatek.server.telecom.ext.IGttEventExt;
import com.mediatek.server.telecom.ext.IRttEventExt;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import mediatek.telecom.MtkConnection;

@VisibleForTesting
public class Call implements EventManager.Loggable, ConnectionServiceFocusManager.CallFocus, CreateConnectionResponse {
    private Analytics.CallInfo mAnalytics;
    private long mCallDataUsage;
    private final int mCallDirection;
    private String mCallerDisplayName;
    private int mCallerDisplayNamePresentation;
    private CallerInfo mCallerInfo;
    private final CallerInfoLookupHelper.OnQueryCompleteListener mCallerInfoQueryListener;
    private final CallsManager mCallsManager;
    private List<String> mCannedSmsResponses;
    private boolean mCannedSmsResponsesLoadingStarted;
    private List<Call> mChildCalls;
    private final ClockProxy mClockProxy;
    private long mConferenceCallLogId;
    private List<String> mConferenceInvitationNumbers;
    private Call mConferenceLevelActiveCall;
    private final List<Call> mConferenceableCalls;
    private long mConnectElapsedTimeMillis;
    private long mConnectTimeMillis;
    private List<String> mConnectedConferenceInvitationNumbers;
    private int mConnectionCapabilities;
    private String mConnectionId;
    private PhoneAccountHandle mConnectionManagerPhoneAccountHandle;
    private int mConnectionProperties;
    private ConnectionServiceWrapper mConnectionService;
    private ParcelFileDescriptor[] mConnectionServiceToInCallStreams;
    private final Context mContext;
    private CreateConnectionProcessor mCreateConnectionProcessor;
    private long mCreationTimeMillis;
    private boolean mDidRequestToStartWithRtt;
    private DisconnectCause mDisconnectCause;
    private long mDisconnectElapsedTimeMillis;
    private long mDisconnectTimeMillis;
    private Bundle mExtras;
    private GatewayInfo mGatewayInfo;
    private final IGttEventExt mGttEventExt;
    private Uri mHandle;
    private int mHandlePresentation;
    private final Handler mHandler;
    private Call mHandoverDestinationCall;
    private Call mHandoverSourceCall;
    private int mHandoverState;
    private final String mId;
    private ParcelFileDescriptor[] mInCallToConnectionServiceStreams;
    private UserHandle mInitiatingUser;
    private Bundle mIntentExtras;
    private boolean mIsAcceptedCdmaMoCall;
    private boolean mIsConference;
    private boolean mIsConferenceInvitation;
    private boolean mIsDisconnectingChildCall;
    private boolean mIsEmergencyCall;
    private boolean mIsInAlertingState;
    private boolean mIsLocallyDisconnecting;
    private boolean mIsNewOutgoingCallIntentBroadcastDone;
    private boolean mIsRemotelyHeld;
    private boolean mIsSelfManaged;
    private boolean mIsVideoCallingSupported;
    private boolean mIsVoiceMailEmergencyCall;
    private boolean mIsVoiceRecording;
    private boolean mIsVoipAudioMode;
    private boolean mIsWorkCall;
    private int mLastConnectionCapabilities;
    private int mLastConnectionProperties;
    private final Set<Listener> mListeners;
    private final TelecomSystem.SyncRoot mLock;
    private Intent mOriginalCallIntent;
    private String mOriginalConnectionId;
    private Call mParentCall;
    private int mPendingRttRequestId;
    private PhoneNumberUtilsAdapter mPhoneNumberUtilsAdapter;
    private char mPlayingDtmfTone;
    private final String mPostDialDigits;
    private int mQueryToken;
    private CallRecorderManager.RecordStateListener mRecordStateListener;
    private final ConnectionServiceRepository mRepository;
    private boolean mRingbackRequested;
    private final IRttEventExt mRttEventExt;
    private int mRttMode;
    private final boolean mShouldAttachToExistingConnection;
    private boolean mSpeakerphoneOn;
    private int mState;
    private StatusHints mStatusHints;
    private int mSubId;
    private int mSupportedAudioRoutes;
    private PhoneAccountHandle mTargetPhoneAccountHandle;
    private boolean mUseCallRecordingTone;
    private String mViaNumber;
    private IVideoProvider mVideoProvider;
    private VideoProviderProxy mVideoProviderProxy;
    private int mVideoState;
    private int mVideoStateHistory;
    private boolean mWasConferencePreviouslyMerged;
    private boolean mWasEverRtt;
    private boolean mWasHighDefAudio;

    @VisibleForTesting
    public interface Listener {
        void forceUpdateCall(Call call);

        void onCallerDisplayNameChanged(Call call);

        void onCallerInfoChanged(Call call);

        boolean onCanceledViaNewOutgoingCallBroadcast(Call call, long j);

        void onCannedSmsResponsesLoaded(Call call);

        void onChildrenChanged(Call call);

        void onConferenceableCallsChanged(Call call);

        void onConnectionCapabilitiesChanged(Call call);

        void onConnectionEvent(Call call, String str, Bundle bundle);

        void onConnectionManagerPhoneAccountChanged(Call call);

        void onConnectionPropertiesChanged(Call call, boolean z);

        void onExternalCallChanged(Call call, boolean z);

        void onExtrasChanged(Call call, int i, Bundle bundle);

        void onExtrasRemoved(Call call, int i, List<String> list);

        void onFailedIncomingCall(Call call);

        void onFailedOutgoingCall(Call call, DisconnectCause disconnectCause);

        void onFailedUnknownCall(Call call);

        void onHandleChanged(Call call);

        void onHandoverComplete(Call call);

        void onHandoverFailed(Call call, int i);

        void onHandoverRequested(Call call, PhoneAccountHandle phoneAccountHandle, int i, Bundle bundle, boolean z);

        void onHoldToneRequested(Call call);

        void onIsVoipAudioModeChanged(Call call);

        void onParentChanged(Call call);

        void onPostDialChar(Call call, char c);

        void onPostDialWait(Call call, String str);

        void onRemoteRttRequest(Call call, int i);

        void onRingbackRequested(Call call, boolean z);

        void onRttInitiationFailure(Call call, int i);

        void onStatusHintsChanged(Call call);

        void onSuccessfulIncomingCall(Call call);

        void onSuccessfulOutgoingCall(Call call, int i);

        void onSuccessfulUnknownCall(Call call, int i);

        void onTargetPhoneAccountChanged(Call call);

        void onVideoCallProviderChanged(Call call);

        void onVideoStateChanged(Call call, int i, int i2);
    }

    public static abstract class ListenerBase implements Listener {
        @Override
        public void onSuccessfulOutgoingCall(Call call, int i) {
        }

        @Override
        public void onFailedOutgoingCall(Call call, DisconnectCause disconnectCause) {
        }

        @Override
        public void onSuccessfulIncomingCall(Call call) {
        }

        @Override
        public void onFailedIncomingCall(Call call) {
        }

        @Override
        public void onSuccessfulUnknownCall(Call call, int i) {
        }

        @Override
        public void onFailedUnknownCall(Call call) {
        }

        @Override
        public void onRingbackRequested(Call call, boolean z) {
        }

        @Override
        public void onPostDialWait(Call call, String str) {
        }

        @Override
        public void onPostDialChar(Call call, char c) {
        }

        @Override
        public void onConnectionCapabilitiesChanged(Call call) {
        }

        @Override
        public void onConnectionPropertiesChanged(Call call, boolean z) {
        }

        @Override
        public void onParentChanged(Call call) {
        }

        @Override
        public void onChildrenChanged(Call call) {
        }

        @Override
        public void onCannedSmsResponsesLoaded(Call call) {
        }

        @Override
        public void onVideoCallProviderChanged(Call call) {
        }

        @Override
        public void onCallerInfoChanged(Call call) {
        }

        @Override
        public void onIsVoipAudioModeChanged(Call call) {
        }

        @Override
        public void onStatusHintsChanged(Call call) {
        }

        @Override
        public void onExtrasChanged(Call call, int i, Bundle bundle) {
        }

        @Override
        public void onExtrasRemoved(Call call, int i, List<String> list) {
        }

        @Override
        public void onHandleChanged(Call call) {
        }

        @Override
        public void onCallerDisplayNameChanged(Call call) {
        }

        @Override
        public void onVideoStateChanged(Call call, int i, int i2) {
        }

        @Override
        public void onTargetPhoneAccountChanged(Call call) {
        }

        @Override
        public void onConnectionManagerPhoneAccountChanged(Call call) {
        }

        @Override
        public void onConferenceableCallsChanged(Call call) {
        }

        @Override
        public boolean onCanceledViaNewOutgoingCallBroadcast(Call call, long j) {
            return false;
        }

        @Override
        public void onHoldToneRequested(Call call) {
        }

        @Override
        public void onConnectionEvent(Call call, String str, Bundle bundle) {
        }

        @Override
        public void onExternalCallChanged(Call call, boolean z) {
        }

        @Override
        public void onRttInitiationFailure(Call call, int i) {
        }

        @Override
        public void onRemoteRttRequest(Call call, int i) {
        }

        @Override
        public void onHandoverRequested(Call call, PhoneAccountHandle phoneAccountHandle, int i, Bundle bundle, boolean z) {
        }

        @Override
        public void onHandoverFailed(Call call, int i) {
        }

        @Override
        public void onHandoverComplete(Call call) {
        }

        @Override
        public void forceUpdateCall(Call call) {
        }
    }

    public Call(String str, Context context, CallsManager callsManager, TelecomSystem.SyncRoot syncRoot, ConnectionServiceRepository connectionServiceRepository, ContactsAsyncHelper contactsAsyncHelper, CallerInfoAsyncQueryFactory callerInfoAsyncQueryFactory, PhoneNumberUtilsAdapter phoneNumberUtilsAdapter, Uri uri, GatewayInfo gatewayInfo, PhoneAccountHandle phoneAccountHandle, PhoneAccountHandle phoneAccountHandle2, int i, boolean z, boolean z2, ClockProxy clockProxy) {
        this.mCallerInfoQueryListener = new CallerInfoLookupHelper.OnQueryCompleteListener() {
            @Override
            public void onCallerInfoQueryComplete(Uri uri2, CallerInfo callerInfo) {
                synchronized (Call.this.mLock) {
                    Call.this.setCallerInfo(uri2, callerInfo);
                }
            }

            @Override
            public void onContactPhotoQueryComplete(Uri uri2, CallerInfo callerInfo) {
                synchronized (Call.this.mLock) {
                    Call.this.setCallerInfo(uri2, callerInfo);
                }
            }
        };
        this.mViaNumber = "";
        this.mConnectTimeMillis = 0L;
        this.mConnectElapsedTimeMillis = 0L;
        this.mDisconnectTimeMillis = 0L;
        this.mDisconnectElapsedTimeMillis = 0L;
        this.mHandler = new Handler(Looper.getMainLooper());
        this.mConferenceableCalls = new ArrayList();
        this.mIsDisconnectingChildCall = false;
        this.mDisconnectCause = new DisconnectCause(0);
        this.mIntentExtras = new Bundle();
        this.mOriginalCallIntent = null;
        this.mListeners = Collections.newSetFromMap(new ConcurrentHashMap(8, 0.9f, 1));
        this.mQueryToken = 0;
        this.mRingbackRequested = false;
        this.mSupportedAudioRoutes = 15;
        this.mIsConference = false;
        this.mParentCall = null;
        this.mChildCalls = new LinkedList();
        this.mCannedSmsResponses = Collections.EMPTY_LIST;
        this.mCannedSmsResponsesLoadingStarted = false;
        this.mWasConferencePreviouslyMerged = false;
        this.mWasHighDefAudio = false;
        this.mConferenceLevelActiveCall = null;
        this.mIsLocallyDisconnecting = false;
        this.mCallDataUsage = -1L;
        this.mIsNewOutgoingCallIntentBroadcastDone = false;
        this.mIsRemotelyHeld = false;
        this.mIsSelfManaged = false;
        this.mIsVideoCallingSupported = false;
        this.mDidRequestToStartWithRtt = false;
        this.mWasEverRtt = false;
        this.mPendingRttRequestId = -1;
        this.mHandoverDestinationCall = null;
        this.mHandoverSourceCall = null;
        this.mHandoverState = 1;
        this.mIsInAlertingState = false;
        this.mIsConferenceInvitation = false;
        this.mConferenceInvitationNumbers = new ArrayList(5);
        this.mConnectedConferenceInvitationNumbers = new ArrayList(5);
        this.mIsVoiceMailEmergencyCall = false;
        this.mLastConnectionCapabilities = 0;
        this.mLastConnectionProperties = 0;
        this.mIsVoiceRecording = false;
        this.mRecordStateListener = new CallRecorderManager.RecordStateListener() {
            @Override
            public final void onRecordStateChanged(int i2) {
                Call.lambda$new$0(this.f$0, i2);
            }
        };
        this.mIsAcceptedCdmaMoCall = false;
        this.mSubId = -1;
        this.mConferenceCallLogId = 0L;
        this.mId = str;
        this.mConnectionId = str;
        this.mState = z2 ? 5 : 0;
        this.mContext = context;
        this.mCallsManager = callsManager;
        this.mLock = syncRoot;
        this.mRepository = connectionServiceRepository;
        this.mPhoneNumberUtilsAdapter = phoneNumberUtilsAdapter;
        setHandle(uri);
        this.mPostDialDigits = uri != null ? PhoneNumberUtils.extractPostDialPortion(uri.getSchemeSpecificPart()) : "";
        this.mGatewayInfo = gatewayInfo;
        setConnectionManagerPhoneAccount(phoneAccountHandle);
        setTargetPhoneAccount(phoneAccountHandle2);
        this.mCallDirection = i;
        this.mIsConference = z2;
        this.mShouldAttachToExistingConnection = z || i == 2;
        maybeLoadCannedSmsResponses();
        this.mAnalytics = new Analytics.CallInfo();
        this.mClockProxy = clockProxy;
        this.mCreationTimeMillis = this.mClockProxy.currentTimeMillis();
        this.mGttEventExt = ExtensionManager.makeGttEventExt();
        this.mRttEventExt = ExtensionManager.makeRttEventExt();
    }

    Call(String str, Context context, CallsManager callsManager, TelecomSystem.SyncRoot syncRoot, ConnectionServiceRepository connectionServiceRepository, ContactsAsyncHelper contactsAsyncHelper, CallerInfoAsyncQueryFactory callerInfoAsyncQueryFactory, PhoneNumberUtilsAdapter phoneNumberUtilsAdapter, Uri uri, GatewayInfo gatewayInfo, PhoneAccountHandle phoneAccountHandle, PhoneAccountHandle phoneAccountHandle2, int i, boolean z, boolean z2, long j, long j2, ClockProxy clockProxy) {
        this(str, context, callsManager, syncRoot, connectionServiceRepository, contactsAsyncHelper, callerInfoAsyncQueryFactory, phoneNumberUtilsAdapter, uri, gatewayInfo, phoneAccountHandle, phoneAccountHandle2, i, z, z2, clockProxy);
        this.mConnectTimeMillis = j;
        this.mConnectElapsedTimeMillis = j2;
        this.mAnalytics.setCallStartTime(j);
    }

    public void addListener(Listener listener) {
        this.mListeners.add(listener);
    }

    public void removeListener(Listener listener) {
        if (listener != null) {
            this.mListeners.remove(listener);
        }
    }

    public void initAnalytics() {
        int i;
        switch (this.mCallDirection) {
            case 1:
                i = 2;
                break;
            case CallState.SELECT_PHONE_ACCOUNT:
                i = 1;
                break;
            default:
                i = 0;
                break;
        }
        this.mAnalytics = Analytics.initiateCallAnalytics(this.mId, i);
        Log.addEvent(this, "CREATED");
    }

    public Analytics.CallInfo getAnalytics() {
        return this.mAnalytics;
    }

    public void destroy() {
        if (this.mCallerInfo != null) {
            this.mCallerInfo.cachedPhotoIcon = null;
            this.mCallerInfo.cachedPhoto = null;
        }
        if (this.mVideoProviderProxy != null) {
            this.mVideoProviderProxy.removeVideoCallListenerBinder();
            this.mVideoProviderProxy = null;
        }
        Log.addEvent(this, "DESTROYED");
    }

    public String toString() {
        String strFlattenToShortString;
        ConnectionServiceWrapper connectionServiceWrapper = this.mConnectionService;
        if (connectionServiceWrapper != null && connectionServiceWrapper.getComponentName() != null) {
            strFlattenToShortString = connectionServiceWrapper.getComponentName().flattenToShortString();
        } else {
            strFlattenToShortString = null;
        }
        Locale locale = Locale.US;
        Object[] objArr = new Object[9];
        objArr[0] = this.mId;
        objArr[1] = CallState.toString(this.mState);
        objArr[2] = strFlattenToShortString;
        objArr[3] = Log.piiHandle(this.mHandle);
        objArr[4] = getVideoStateDescription(getVideoState());
        objArr[5] = Integer.valueOf(getChildCalls().size());
        objArr[6] = Boolean.valueOf(getParentCall() != null);
        objArr[7] = MtkConnection.capabilitiesToString(getConnectionCapabilities());
        objArr[8] = MtkConnection.propertiesToString(getConnectionProperties());
        return String.format(locale, "[%s, %s, %s, %s, %s, childs(%d), has_parent(%b), %s, %s]", objArr);
    }

    public String getDescription() {
        StringBuilder sb = new StringBuilder();
        if (isSelfManaged()) {
            sb.append("SelfMgd Call");
        } else if (isExternalCall()) {
            sb.append("External Call");
        } else {
            sb.append("Call");
        }
        sb.append(getId());
        sb.append(" [");
        sb.append(SimpleDateFormat.getDateTimeInstance().format(new Date(getCreationTimeMillis())));
        sb.append("]");
        sb.append(isIncoming() ? "(MT - incoming)" : "(MO - outgoing)");
        sb.append("\n\tVia PhoneAccount: ");
        PhoneAccountHandle targetPhoneAccount = getTargetPhoneAccount();
        if (targetPhoneAccount != null) {
            sb.append(targetPhoneAccount);
            sb.append(" (");
            sb.append(getTargetPhoneAccountLabel());
            sb.append(")");
        } else {
            sb.append("not set");
        }
        sb.append("\n\tTo address: ");
        sb.append(Log.piiHandle(getHandle()));
        sb.append(" Presentation: ");
        switch (getHandlePresentation()) {
            case 1:
                sb.append("Allowed");
                break;
            case CallState.SELECT_PHONE_ACCOUNT:
                sb.append("Restricted");
                break;
            case CallState.DIALING:
                sb.append("Unknown");
                break;
            case CallState.RINGING:
                sb.append("Payphone");
                break;
            default:
                sb.append("<undefined>");
                break;
        }
        sb.append("\n");
        return sb.toString();
    }

    private String getVideoStateDescription(int i) {
        StringBuilder sb = new StringBuilder();
        sb.append("A");
        if (VideoProfile.isTransmissionEnabled(i)) {
            sb.append("T");
        }
        if (VideoProfile.isReceptionEnabled(i)) {
            sb.append("R");
        }
        if (VideoProfile.isPaused(i)) {
            sb.append("P");
        }
        return sb.toString();
    }

    @Override
    public ConnectionServiceFocusManager.ConnectionServiceFocus getConnectionServiceWrapper() {
        return this.mConnectionService;
    }

    @Override
    @VisibleForTesting
    public int getState() {
        return this.mState;
    }

    @Override
    public boolean isFocusable() {
        return ((getParentCall() != null) || isExternalCall()) ? false : true;
    }

    private boolean shouldContinueProcessingAfterDisconnect() {
        if (CreateConnectionTimeout.isCallBeingPlaced(this) && isEmergencyCall() && this.mCreateConnectionProcessor != null && this.mCreateConnectionProcessor.isProcessingComplete() && this.mCreateConnectionProcessor.hasMorePhoneAccounts() && this.mDisconnectCause != null) {
            return this.mDisconnectCause.getCode() == 1 || this.mCreateConnectionProcessor.isCallTimedOut();
        }
        return false;
    }

    public String getId() {
        return this.mId;
    }

    public String getConnectionId() {
        if (this.mCreateConnectionProcessor != null) {
            this.mConnectionId = this.mId + "_" + String.valueOf(this.mCreateConnectionProcessor.getConnectionAttempt());
            return this.mConnectionId;
        }
        return this.mConnectionId;
    }

    public void setState(int i, String str) {
        String str2;
        if (this.mState != i) {
            Log.v(this, "setState %s -> %s", new Object[]{Integer.valueOf(this.mState), Integer.valueOf(i)});
            if (i == 7 && shouldContinueProcessingAfterDisconnect()) {
                Log.w(this, "continuing processing disconnected call with another service", new Object[0]);
                this.mCreateConnectionProcessor.continueProcessingIfPossible(this, this.mDisconnectCause);
                return;
            }
            updateVideoHistoryViaState(this.mState, i);
            this.mState = i;
            maybeLoadCannedSmsResponses();
            if (this.mState != 5 && this.mState != 6) {
                if (this.mState == 7) {
                    this.mDisconnectTimeMillis = this.mClockProxy.currentTimeMillis();
                    this.mDisconnectElapsedTimeMillis = this.mClockProxy.elapsedRealtime();
                    this.mAnalytics.setCallEndTime(this.mDisconnectTimeMillis);
                    setLocallyDisconnecting(false);
                    fixParentAfterDisconnect();
                    if (MtkUtil.isMmiWithDataOff(this.mDisconnectCause)) {
                        MtkUtil.showNoDataDialog(this.mContext, this.mTargetPhoneAccountHandle);
                        setDisconnectCause(new DisconnectCause(9));
                    }
                }
            } else {
                if (this.mConnectTimeMillis == 0) {
                    this.mConnectTimeMillis = this.mClockProxy.currentTimeMillis();
                    this.mConnectElapsedTimeMillis = this.mClockProxy.elapsedRealtime();
                    this.mAnalytics.setCallStartTime(this.mConnectTimeMillis);
                }
                this.mDisconnectTimeMillis = 0L;
                this.mDisconnectElapsedTimeMillis = 0L;
            }
            maybeUpdateVoiceRecordState();
            this.mIsInAlertingState = false;
            DisconnectCause disconnectCause = null;
            switch (i) {
                case 1:
                    str2 = "SET_CONNECTING";
                    break;
                case CallState.SELECT_PHONE_ACCOUNT:
                    str2 = "SET_SELECT_PHONE_ACCOUNT";
                    break;
                case CallState.DIALING:
                    str2 = "SET_DIALING";
                    break;
                case CallState.RINGING:
                    str2 = "SET_RINGING";
                    break;
                case CallState.ACTIVE:
                    str2 = "SET_ACTIVE";
                    break;
                case CallState.ON_HOLD:
                    str2 = "SET_HOLD";
                    break;
                case CallState.DISCONNECTED:
                    str2 = "SET_DISCONNECTED";
                    disconnectCause = getDisconnectCause();
                    break;
                case CallState.ABORTED:
                default:
                    str2 = null;
                    break;
                case 9:
                    str2 = "SET_DISCONNECTING";
                    break;
                case CallState.PULLING:
                    str2 = "SET_PULLING";
                    break;
            }
            if (str2 != null) {
                if (disconnectCause != null) {
                    if (str == null) {
                        str = disconnectCause.toString();
                    } else {
                        str = str + "> " + disconnectCause;
                    }
                }
                Log.addEvent(this, str2, str);
            }
            StatsLog.write(61, i, i == 7 ? getDisconnectCause().getCode() : 0, isSelfManaged(), isExternalCall());
        }
    }

    void setRingbackRequested(boolean z) {
        this.mRingbackRequested = z;
        Iterator<Listener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onRingbackRequested(this, this.mRingbackRequested);
        }
    }

    boolean isRingbackRequested() {
        return this.mRingbackRequested;
    }

    @VisibleForTesting
    public boolean isConference() {
        return this.mIsConference;
    }

    public Uri getHandle() {
        return this.mHandle;
    }

    public String getPostDialDigits() {
        return this.mPostDialDigits;
    }

    public String getViaNumber() {
        return this.mViaNumber;
    }

    public void setViaNumber(String str) {
        if (!TextUtils.isEmpty(str)) {
            this.mViaNumber = str;
        }
    }

    public int getHandlePresentation() {
        return this.mHandlePresentation;
    }

    void setHandle(Uri uri) {
        setHandle(uri, 1);
    }

    public void setHandle(Uri uri, int i) {
        if (!Objects.equals(uri, this.mHandle) || i != this.mHandlePresentation) {
            this.mHandlePresentation = i;
            if (this.mHandlePresentation == 2 || this.mHandlePresentation == 3) {
                this.mHandle = null;
            } else {
                this.mHandle = uri;
                if (this.mHandle != null && !"voicemail".equals(this.mHandle.getScheme()) && TextUtils.isEmpty(this.mHandle.getSchemeSpecificPart())) {
                    this.mHandle = null;
                }
            }
            if (!this.mIsEmergencyCall) {
                this.mIsEmergencyCall = this.mHandle != null && this.mPhoneNumberUtilsAdapter.isLocalEmergencyNumber(this.mContext, this.mHandle.getSchemeSpecificPart());
            }
            setVoiceMailEmergencyCallIfNeeded();
            startCallerInfoLookup();
            Iterator<Listener> it = this.mListeners.iterator();
            while (it.hasNext()) {
                it.next().onHandleChanged(this);
            }
        }
    }

    public String getCallerDisplayName() {
        return this.mCallerDisplayName;
    }

    public int getCallerDisplayNamePresentation() {
        return this.mCallerDisplayNamePresentation;
    }

    void setCallerDisplayName(String str, int i) {
        if (!TextUtils.equals(str, this.mCallerDisplayName) || i != this.mCallerDisplayNamePresentation) {
            this.mCallerDisplayName = str;
            this.mCallerDisplayNamePresentation = i;
            Iterator<Listener> it = this.mListeners.iterator();
            while (it.hasNext()) {
                it.next().onCallerDisplayNameChanged(this);
            }
        }
    }

    public String getName() {
        if (this.mCallerInfo == null) {
            return null;
        }
        return this.mCallerInfo.name;
    }

    public String getPhoneNumber() {
        if (this.mCallerInfo == null) {
            return null;
        }
        return this.mCallerInfo.phoneNumber;
    }

    public void setDisconnectCause(DisconnectCause disconnectCause) {
        this.mAnalytics.setCallDisconnectCause(disconnectCause);
        this.mDisconnectCause = disconnectCause;
    }

    public DisconnectCause getDisconnectCause() {
        return this.mDisconnectCause;
    }

    @VisibleForTesting
    public boolean isEmergencyCall() {
        return this.mIsEmergencyCall || this.mIsVoiceMailEmergencyCall;
    }

    public Uri getOriginalHandle() {
        if (this.mGatewayInfo != null && !this.mGatewayInfo.isEmpty()) {
            return this.mGatewayInfo.getOriginalAddress();
        }
        return getHandle();
    }

    @VisibleForTesting
    public GatewayInfo getGatewayInfo() {
        return this.mGatewayInfo;
    }

    void setGatewayInfo(GatewayInfo gatewayInfo) {
        this.mGatewayInfo = gatewayInfo;
    }

    @VisibleForTesting
    public PhoneAccountHandle getConnectionManagerPhoneAccount() {
        return this.mConnectionManagerPhoneAccountHandle;
    }

    @VisibleForTesting
    public void setConnectionManagerPhoneAccount(PhoneAccountHandle phoneAccountHandle) {
        if (!Objects.equals(this.mConnectionManagerPhoneAccountHandle, phoneAccountHandle)) {
            this.mConnectionManagerPhoneAccountHandle = phoneAccountHandle;
            Iterator<Listener> it = this.mListeners.iterator();
            while (it.hasNext()) {
                it.next().onConnectionManagerPhoneAccountChanged(this);
            }
        }
        checkIfRttCapable();
    }

    @VisibleForTesting
    public PhoneAccountHandle getTargetPhoneAccount() {
        return this.mTargetPhoneAccountHandle;
    }

    @VisibleForTesting
    public void setTargetPhoneAccount(PhoneAccountHandle phoneAccountHandle) {
        if (!Objects.equals(this.mTargetPhoneAccountHandle, phoneAccountHandle)) {
            this.mTargetPhoneAccountHandle = phoneAccountHandle;
            Iterator<Listener> it = this.mListeners.iterator();
            while (it.hasNext()) {
                it.next().onTargetPhoneAccountChanged(this);
            }
            configureCallAttributes();
            updateSubscriptionId();
        }
        checkIfVideoCapable();
        checkIfRttCapable();
    }

    public CharSequence getTargetPhoneAccountLabel() {
        PhoneAccount phoneAccountUnchecked;
        if (getTargetPhoneAccount() == null || (phoneAccountUnchecked = this.mCallsManager.getPhoneAccountRegistrar().getPhoneAccountUnchecked(getTargetPhoneAccount())) == null) {
            return null;
        }
        return phoneAccountUnchecked.getLabel();
    }

    public boolean isLoggedSelfManaged() {
        PhoneAccount phoneAccountUnchecked;
        if (!isSelfManaged()) {
            return true;
        }
        if (getTargetPhoneAccount() == null || (phoneAccountUnchecked = this.mCallsManager.getPhoneAccountRegistrar().getPhoneAccountUnchecked(getTargetPhoneAccount())) == null || getHandle() == null) {
            return false;
        }
        if ("sip".equals(getHandle().getScheme()) || "tel".equals(getHandle().getScheme())) {
            return phoneAccountUnchecked.getExtras() != null && phoneAccountUnchecked.getExtras().getBoolean("android.telecom.extra.LOG_SELF_MANAGED_CALLS", false);
        }
        return false;
    }

    @VisibleForTesting
    public boolean isIncoming() {
        return this.mCallDirection == 2;
    }

    public boolean isExternalCall() {
        return (getConnectionProperties() & 16) == 16;
    }

    public boolean isWorkCall() {
        return this.mIsWorkCall;
    }

    public boolean isUsingCallRecordingTone() {
        return this.mUseCallRecordingTone;
    }

    public boolean isVideoCallingSupported() {
        return this.mIsVideoCallingSupported;
    }

    public boolean isSelfManaged() {
        return this.mIsSelfManaged;
    }

    public void setIsSelfManaged(boolean z) {
        this.mIsSelfManaged = z;
        setConnectionProperties(getConnectionProperties());
    }

    public void markFinishedHandoverStateAndCleanup(int i) {
        if (this.mHandoverSourceCall != null) {
            this.mHandoverSourceCall.setHandoverState(i);
        } else if (this.mHandoverDestinationCall != null) {
            this.mHandoverDestinationCall.setHandoverState(i);
        }
        setHandoverState(i);
        maybeCleanupHandover();
    }

    public void maybeCleanupHandover() {
        if (this.mHandoverSourceCall != null) {
            this.mHandoverSourceCall.setHandoverSourceCall(null);
            this.mHandoverSourceCall.setHandoverDestinationCall(null);
            this.mHandoverSourceCall = null;
        } else if (this.mHandoverDestinationCall != null) {
            this.mHandoverDestinationCall.setHandoverSourceCall(null);
            this.mHandoverDestinationCall.setHandoverDestinationCall(null);
            this.mHandoverDestinationCall = null;
        }
    }

    public boolean isHandoverInProgress() {
        return (this.mHandoverSourceCall == null && this.mHandoverDestinationCall == null) ? false : true;
    }

    public Call getHandoverDestinationCall() {
        return this.mHandoverDestinationCall;
    }

    public void setHandoverDestinationCall(Call call) {
        this.mHandoverDestinationCall = call;
    }

    public Call getHandoverSourceCall() {
        return this.mHandoverSourceCall;
    }

    public void setHandoverSourceCall(Call call) {
        this.mHandoverSourceCall = call;
    }

    public void setHandoverState(int i) {
        Log.d(this, "setHandoverState: callId=%s, handoverState=%s", new Object[]{getId(), HandoverState.stateToString(i)});
        this.mHandoverState = i;
    }

    public int getHandoverState() {
        return this.mHandoverState;
    }

    private void configureCallAttributes() {
        boolean z;
        UserHandle userHandle;
        boolean zIsManagedProfile;
        PhoneAccount phoneAccountUnchecked = this.mCallsManager.getPhoneAccountRegistrar().getPhoneAccountUnchecked(this.mTargetPhoneAccountHandle);
        boolean z2 = false;
        if (phoneAccountUnchecked == null) {
            z = false;
        } else {
            if (phoneAccountUnchecked.hasCapabilities(32)) {
                userHandle = this.mInitiatingUser;
            } else {
                userHandle = this.mTargetPhoneAccountHandle.getUserHandle();
            }
            if (userHandle != null) {
                zIsManagedProfile = UserUtil.isManagedProfile(this.mContext, userHandle);
            } else {
                zIsManagedProfile = false;
            }
            if (phoneAccountUnchecked.hasCapabilities(4) && phoneAccountUnchecked.getExtras() != null && phoneAccountUnchecked.getExtras().getBoolean("android.telecom.extra.PLAY_CALL_RECORDING_TONE", false)) {
                z2 = true;
            }
            z = z2;
            z2 = zIsManagedProfile;
        }
        this.mIsWorkCall = z2;
        this.mUseCallRecordingTone = z;
    }

    private void checkIfVideoCapable() {
        PhoneAccountRegistrar phoneAccountRegistrar = this.mCallsManager.getPhoneAccountRegistrar();
        if (this.mTargetPhoneAccountHandle == null) {
            this.mIsVideoCallingSupported = true;
            Log.d(this, "checkIfVideoCapable: no phone account selected; assume video capable.", new Object[0]);
            return;
        }
        PhoneAccount phoneAccountUnchecked = phoneAccountRegistrar.getPhoneAccountUnchecked(this.mTargetPhoneAccountHandle);
        this.mIsVideoCallingSupported = phoneAccountUnchecked != null && phoneAccountUnchecked.hasCapabilities(8);
        if (!this.mIsVideoCallingSupported && VideoProfile.isVideo(getVideoState())) {
            setVideoState(0);
            Log.d(this, "checkIfVideoCapable: selected phone account doesn't support video.", new Object[0]);
        }
    }

    private void checkIfRttCapable() {
        PhoneAccountRegistrar phoneAccountRegistrar = this.mCallsManager.getPhoneAccountRegistrar();
        if (this.mTargetPhoneAccountHandle == null) {
            return;
        }
        PhoneAccount phoneAccountUnchecked = phoneAccountRegistrar.getPhoneAccountUnchecked(this.mTargetPhoneAccountHandle);
        PhoneAccount phoneAccountUnchecked2 = phoneAccountRegistrar.getPhoneAccountUnchecked(this.mConnectionManagerPhoneAccountHandle);
        boolean z = true;
        boolean z2 = phoneAccountUnchecked != null && phoneAccountUnchecked.hasCapabilities(4096);
        if (phoneAccountUnchecked2 == null || !phoneAccountUnchecked2.hasCapabilities(4096)) {
            z = false;
        }
        if ((z || z2) && this.mDidRequestToStartWithRtt && !areRttStreamsInitialized()) {
            createRttStreams();
            Log.i(this, "Setting RTT streams after target phone account selected", new Object[0]);
        }
    }

    boolean shouldAttachToExistingConnection() {
        return this.mShouldAttachToExistingConnection;
    }

    @VisibleForTesting
    public long getAgeMillis() {
        if ((this.mState == 7 && (this.mDisconnectCause.getCode() == 6 || this.mDisconnectCause.getCode() == 5)) || this.mConnectElapsedTimeMillis == 0) {
            return 0L;
        }
        if (this.mDisconnectElapsedTimeMillis == 0) {
            return this.mClockProxy.elapsedRealtime() - this.mConnectElapsedTimeMillis;
        }
        return this.mDisconnectElapsedTimeMillis - this.mConnectElapsedTimeMillis;
    }

    public long getCreationTimeMillis() {
        return this.mCreationTimeMillis;
    }

    long getConnectTimeMillis() {
        return this.mConnectTimeMillis;
    }

    int getConnectionCapabilities() {
        return this.mConnectionCapabilities;
    }

    int getConnectionProperties() {
        return this.mConnectionProperties;
    }

    void setConnectionCapabilities(int i) {
        setConnectionCapabilities(i, false);
    }

    void setConnectionCapabilities(int i, boolean z) {
        int i2;
        int i3;
        Log.v(this, "setConnectionCapabilities: %s", new Object[]{Connection.capabilitiesToString(i)});
        this.mLastConnectionCapabilities = i;
        if (MtkUtil.isInSingleVideoCallMode(this)) {
            if (this.mCallsManager.hasOtherAliveCallInSamePhoneAccount(this)) {
                i2 = (-524289) & i & (-513);
            } else {
                i2 = i;
            }
            if (isVideo()) {
                i2 &= -2;
            }
        } else {
            i2 = i;
        }
        if (canVoiceRecord()) {
            i3 = i2 | 1073741824;
        } else {
            i3 = i2 & (-1073741825);
        }
        if (i3 != i) {
            Log.d(this, "[setConnectionCapabilities] changed: %s -> %s", new Object[]{Integer.valueOf(i), Integer.valueOf(i3)});
            i = i3;
        }
        if (z || this.mConnectionCapabilities != i) {
            if (!isVideoCallingSupported() && doesCallSupportVideo(i)) {
                Log.w(this, "setConnectionCapabilities: attempt to set connection as video capable when not supported by the phone account.", new Object[0]);
                i = removeVideoCapabilities(i);
            }
            int i4 = this.mConnectionCapabilities;
            this.mConnectionCapabilities = i;
            Iterator<Listener> it = this.mListeners.iterator();
            while (it.hasNext()) {
                it.next().onConnectionCapabilitiesChanged(this);
            }
            int i5 = this.mConnectionCapabilities ^ i4;
            Log.addEvent(this, "CAPABILITY_CHANGE", "Current: [%s], Removed [%s], Added [%s]", new Object[]{Connection.capabilitiesToStringShort(this.mConnectionCapabilities), Connection.capabilitiesToStringShort(i4 & i5), Connection.capabilitiesToStringShort(i5 & this.mConnectionCapabilities)});
        }
    }

    void setConnectionProperties(int i) {
        int i2;
        int i3;
        Log.v(this, "setConnectionProperties: %s", new Object[]{MtkConnection.propertiesToString(i)});
        this.mLastConnectionProperties = i;
        if (this.mIsVoiceRecording) {
            i2 = 131072 | i;
        } else {
            i2 = (-131073) & i;
        }
        if (i2 != i) {
            Log.d(this, "[setConnectionProperties] changed: %s -> %s", new Object[]{Integer.valueOf(i), Integer.valueOf(i2)});
            i = i2;
        }
        if (isSelfManaged()) {
            i3 = i | 128;
        } else {
            i3 = i & (-129);
        }
        int i4 = this.mConnectionProperties ^ i3;
        if (i4 != 0) {
            int i5 = this.mConnectionProperties;
            this.mConnectionProperties = i3;
            if ((this.mConnectionProperties & 256) == 256) {
                createRttStreams();
                this.mConnectionService.startRtt(this, getInCallToCsRttPipeForCs(), getCsToInCallRttPipeForCs());
                this.mWasEverRtt = true;
                if (isEmergencyCall()) {
                    this.mCallsManager.setAudioRoute(8, null);
                    this.mCallsManager.mute(false);
                }
            }
            this.mWasHighDefAudio = (i3 & 4) == 4;
            boolean z = (i4 & 256) == 256;
            Iterator<Listener> it = this.mListeners.iterator();
            while (it.hasNext()) {
                it.next().onConnectionPropertiesChanged(this, z);
            }
            boolean z2 = (i5 & 16) == 16;
            boolean z3 = (i3 & 16) == 16;
            if (z2 != z3) {
                Log.v(this, "setConnectionProperties: external call changed isExternal = %b", new Object[]{Boolean.valueOf(z3)});
                Log.addEvent(this, "IS_EXTERNAL", Boolean.valueOf(z3));
                Iterator<Listener> it2 = this.mListeners.iterator();
                while (it2.hasNext()) {
                    it2.next().onExternalCallChanged(this, z3);
                }
            }
            this.mAnalytics.addCallProperties(this.mConnectionProperties);
            int i6 = this.mConnectionProperties ^ i5;
            Log.addEvent(this, "PROPERTY_CHANGE", "Current: [%s], Removed [%s], Added [%s]", new Object[]{MtkConnection.propertiesToStringShort(this.mConnectionProperties), MtkConnection.propertiesToStringShort(i5 & i6), MtkConnection.propertiesToStringShort(i6 & this.mConnectionProperties)});
        }
    }

    public int getSupportedAudioRoutes() {
        return this.mSupportedAudioRoutes;
    }

    void setSupportedAudioRoutes(int i) {
        if (this.mSupportedAudioRoutes != i) {
            this.mSupportedAudioRoutes = i;
        }
    }

    @VisibleForTesting
    public Call getParentCall() {
        return this.mParentCall;
    }

    @VisibleForTesting
    public List<Call> getChildCalls() {
        return this.mChildCalls;
    }

    @VisibleForTesting
    public boolean wasConferencePreviouslyMerged() {
        return this.mWasConferencePreviouslyMerged;
    }

    public boolean isDisconnectingChildCall() {
        return this.mIsDisconnectingChildCall;
    }

    private void maybeSetCallAsDisconnectingChild() {
        if (this.mParentCall != null) {
            this.mIsDisconnectingChildCall = true;
        }
    }

    @VisibleForTesting
    public Call getConferenceLevelActiveCall() {
        return this.mConferenceLevelActiveCall;
    }

    @VisibleForTesting
    public ConnectionServiceWrapper getConnectionService() {
        return this.mConnectionService;
    }

    public Context getContext() {
        return this.mContext;
    }

    @VisibleForTesting
    public void setConnectionService(ConnectionServiceWrapper connectionServiceWrapper) {
        Preconditions.checkNotNull(connectionServiceWrapper);
        clearConnectionService();
        connectionServiceWrapper.incrementAssociatedCallCount();
        this.mConnectionService = connectionServiceWrapper;
        this.mAnalytics.setCallConnectionService(connectionServiceWrapper.getComponentName().flattenToShortString());
        this.mConnectionService.addCall(this);
    }

    public void replaceConnectionService(ConnectionServiceWrapper connectionServiceWrapper) {
        Preconditions.checkNotNull(connectionServiceWrapper);
        if (this.mConnectionService != null) {
            ConnectionServiceWrapper connectionServiceWrapper2 = this.mConnectionService;
            this.mConnectionService = null;
            connectionServiceWrapper2.removeCall(this);
            connectionServiceWrapper2.decrementAssociatedCallCount(true);
        }
        connectionServiceWrapper.incrementAssociatedCallCount();
        this.mConnectionService = connectionServiceWrapper;
        this.mAnalytics.setCallConnectionService(connectionServiceWrapper.getComponentName().flattenToShortString());
    }

    void clearConnectionService() {
        if (this.mConnectionService != null) {
            ConnectionServiceWrapper connectionServiceWrapper = this.mConnectionService;
            this.mConnectionService = null;
            connectionServiceWrapper.removeCall(this);
            decrementAssociatedCallCount(connectionServiceWrapper);
        }
    }

    void startCreateConnection(PhoneAccountRegistrar phoneAccountRegistrar) {
        if (this.mCreateConnectionProcessor != null) {
            Log.w(this, "mCreateConnectionProcessor in startCreateConnection is not null. This is due to a race between NewOutgoingCallIntentBroadcaster and phoneAccountSelected, but is harmlessly resolved by ignoring the second invocation.", new Object[0]);
        } else {
            this.mCreateConnectionProcessor = new CreateConnectionProcessor(this, this.mRepository, this, phoneAccountRegistrar, this.mContext);
            this.mCreateConnectionProcessor.process();
        }
    }

    @Override
    public void handleCreateConnectionSuccess(CallIdMapper callIdMapper, ParcelableConnection parcelableConnection) {
        Log.v(this, "handleCreateConnectionSuccessful %s", new Object[]{parcelableConnection});
        setTargetPhoneAccount(parcelableConnection.getPhoneAccount());
        setHandle(parcelableConnection.getHandle(), parcelableConnection.getHandlePresentation());
        setCallerDisplayName(parcelableConnection.getCallerDisplayName(), parcelableConnection.getCallerDisplayNamePresentation());
        setConnectionCapabilities(parcelableConnection.getConnectionCapabilities());
        setConnectionProperties(parcelableConnection.getConnectionProperties());
        setIsVoipAudioMode(parcelableConnection.getIsVoipAudioMode());
        setSupportedAudioRoutes(parcelableConnection.getSupportedAudioRoutes());
        setVideoProvider(parcelableConnection.getVideoProvider());
        setVideoState(parcelableConnection.getVideoState());
        setRingbackRequested(parcelableConnection.isRingbackRequested());
        setStatusHints(parcelableConnection.getStatusHints());
        putExtras(1, parcelableConnection.getExtras());
        this.mConferenceableCalls.clear();
        Iterator it = parcelableConnection.getConferenceableConnectionIds().iterator();
        while (it.hasNext()) {
            this.mConferenceableCalls.add(callIdMapper.getCall((String) it.next()));
        }
        switch (this.mCallDirection) {
            case 1:
                Iterator<Listener> it2 = this.mListeners.iterator();
                while (it2.hasNext()) {
                    it2.next().onSuccessfulOutgoingCall(this, getStateFromConnectionState(parcelableConnection.getState()));
                }
                break;
            case CallState.SELECT_PHONE_ACCOUNT:
                Iterator<Listener> it3 = this.mListeners.iterator();
                while (it3.hasNext()) {
                    it3.next().onSuccessfulIncomingCall(this);
                }
                break;
            case CallState.DIALING:
                Iterator<Listener> it4 = this.mListeners.iterator();
                while (it4.hasNext()) {
                    it4.next().onSuccessfulUnknownCall(this, getStateFromConnectionState(parcelableConnection.getState()));
                }
                break;
        }
    }

    @Override
    public void handleCreateConnectionFailure(DisconnectCause disconnectCause) {
        clearConnectionService();
        setDisconnectCause(disconnectCause);
        this.mCallsManager.markCallAsDisconnected(this, disconnectCause);
        switch (this.mCallDirection) {
            case CallState.NEW:
                this.mCallsManager.markCallAsRemoved(this);
                break;
            case 1:
                Iterator<Listener> it = this.mListeners.iterator();
                while (it.hasNext()) {
                    it.next().onFailedOutgoingCall(this, disconnectCause);
                }
                break;
            case CallState.SELECT_PHONE_ACCOUNT:
                Iterator<Listener> it2 = this.mListeners.iterator();
                while (it2.hasNext()) {
                    it2.next().onFailedIncomingCall(this);
                }
                break;
            case CallState.DIALING:
                Iterator<Listener> it3 = this.mListeners.iterator();
                while (it3.hasNext()) {
                    it3.next().onFailedUnknownCall(this);
                }
                break;
        }
    }

    @VisibleForTesting
    public void playDtmfTone(char c) {
        if (this.mConnectionService == null) {
            Log.w(this, "playDtmfTone() request on a call without a connection service.", new Object[0]);
        } else {
            Log.i(this, "Send playDtmfTone to connection service for call %s", new Object[]{this});
            this.mConnectionService.playDtmfTone(this, c);
            Log.addEvent(this, "START_DTMF", Log.pii(Character.valueOf(c)));
        }
        this.mPlayingDtmfTone = c;
    }

    @VisibleForTesting
    public void stopDtmfTone() {
        if (this.mConnectionService == null) {
            Log.w(this, "stopDtmfTone() request on a call without a connection service.", new Object[0]);
        } else {
            Log.i(this, "Send stopDtmfTone to connection service for call %s", new Object[]{this});
            Log.addEvent(this, "STOP_DTMF");
            this.mConnectionService.stopDtmfTone(this);
        }
        this.mPlayingDtmfTone = (char) 0;
    }

    boolean isDtmfTonePlaying() {
        return this.mPlayingDtmfTone != 0;
    }

    void silence() {
        if (this.mConnectionService == null) {
            Log.w(this, "silence() request on a call without a connection service.", new Object[0]);
            return;
        }
        Log.i(this, "Send silence to connection service for call %s", new Object[]{this});
        Log.addEvent(this, "SILENCE");
        this.mConnectionService.silence(this);
    }

    @VisibleForTesting
    public void disconnect() {
        disconnect(0L);
    }

    @VisibleForTesting
    public void disconnect(String str) {
        disconnect(0L, str);
    }

    @VisibleForTesting
    public void disconnect(long j) {
        disconnect(j, "internal");
    }

    @VisibleForTesting
    public void disconnect(long j, String str) {
        Log.addEvent(this, "REQUEST_DISCONNECT", str);
        setLocallyDisconnecting(true);
        maybeSetCallAsDisconnectingChild();
        if (this.mState == 0 || this.mState == 2 || this.mState == 1) {
            Log.v(this, "Aborting call %s", new Object[]{this});
            abort(j);
        } else if (this.mState != 8 && this.mState != 7) {
            if (this.mConnectionService == null) {
                Log.e(this, new Exception(), "disconnect() request on a call without a connection service.", new Object[0]);
            } else {
                Log.i(this, "Send disconnect to connection service for call: %s", new Object[]{this});
                this.mConnectionService.disconnect(this);
            }
        }
    }

    void abort(long j) {
        if (this.mCreateConnectionProcessor != null && !this.mCreateConnectionProcessor.isProcessingComplete()) {
            this.mCreateConnectionProcessor.abort();
            return;
        }
        if (this.mState == 0 || this.mState == 2 || this.mState == 1) {
            if (j > 0) {
                Iterator<Listener> it = this.mListeners.iterator();
                while (it.hasNext()) {
                    if (it.next().onCanceledViaNewOutgoingCallBroadcast(this, j)) {
                        setLocallyDisconnecting(false);
                        return;
                    }
                }
            }
            handleCreateConnectionFailure(new DisconnectCause(4));
            return;
        }
        Log.v(this, "Cannot abort a call which is neither SELECT_PHONE_ACCOUNT or CONNECTING", new Object[0]);
    }

    @VisibleForTesting
    public void answer(int i) {
        this.mCallsManager.answerCall(this, i);
    }

    @VisibleForTesting
    public void doAnswer(int i) {
        if (isRinging("answer")) {
            if (!isVideoCallingSupported() && VideoProfile.isVideo(i)) {
                i = 0;
            }
            if (this.mConnectionService != null) {
                this.mConnectionService.answer(this, i);
            } else {
                Log.e(this, new NullPointerException(), "answer call failed due to null CS callId=%s", new Object[]{getId()});
            }
            Log.addEvent(this, "REQUEST_ACCEPT");
        }
    }

    @VisibleForTesting
    public void deflect(Uri uri) {
        if (isRinging("deflect")) {
            this.mVideoStateHistory |= this.mVideoState;
            if (this.mConnectionService != null) {
                this.mConnectionService.deflect(this, uri);
            } else {
                Log.e(this, new NullPointerException(), "deflect call failed due to null CS callId=%s", new Object[]{getId()});
            }
            Log.addEvent(this, "REQUEST_DEFLECT", Log.pii(uri));
        }
    }

    @VisibleForTesting
    public void reject(boolean z, String str) {
        reject(z, str, "internal");
    }

    @VisibleForTesting
    public void reject(boolean z, String str, String str2) {
        if (isRinging("reject")) {
            this.mVideoStateHistory |= this.mVideoState;
            if (this.mConnectionService != null) {
                this.mConnectionService.reject(this, z, str);
            } else {
                Log.e(this, new NullPointerException(), "reject call failed due to null CS callId=%s", new Object[]{getId()});
            }
            Log.addEvent(this, "REQUEST_REJECT", str2);
        }
    }

    @VisibleForTesting
    public void hold() {
        hold(null);
    }

    public void hold(String str) {
        if (this.mState == 5) {
            if (this.mConnectionService != null) {
                this.mConnectionService.hold(this);
            } else {
                Log.e(this, new NullPointerException(), "hold call failed due to null CS callId=%s", new Object[]{getId()});
            }
            Log.addEvent(this, "REQUEST_HOLD", str);
        }
    }

    @VisibleForTesting
    public void unhold() {
        unhold(null);
    }

    public void unhold(String str) {
        if (this.mState == 6) {
            if (this.mConnectionService != null) {
                this.mConnectionService.unhold(this);
            } else {
                Log.e(this, new NullPointerException(), "unhold call failed due to null CS callId=%s", new Object[]{getId()});
            }
            Log.addEvent(this, "REQUEST_UNHOLD", str);
        }
    }

    @VisibleForTesting
    public boolean isAlive() {
        int i = this.mState;
        if (i == 0 || i == 4) {
            return false;
        }
        switch (i) {
            case CallState.DISCONNECTED:
            case CallState.ABORTED:
                return false;
            default:
                return true;
        }
    }

    boolean isActive() {
        return this.mState == 5;
    }

    Bundle getExtras() {
        return this.mExtras;
    }

    void putExtras(int i, Bundle bundle) {
        if (bundle == null) {
            return;
        }
        if (this.mExtras == null) {
            this.mExtras = new Bundle();
        }
        this.mExtras.putAll(bundle);
        Iterator<Listener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onExtrasChanged(this, i, bundle);
        }
        if (i == 2) {
            if (this.mConnectionService != null) {
                this.mConnectionService.onExtrasChanged(this, this.mExtras);
            } else {
                Log.e(this, new NullPointerException(), "putExtras failed due to null CS callId=%s", new Object[]{getId()});
            }
        }
    }

    void removeExtras(int i, List<String> list) {
        if (this.mExtras == null) {
            return;
        }
        Iterator<String> it = list.iterator();
        while (it.hasNext()) {
            this.mExtras.remove(it.next());
        }
        Iterator<Listener> it2 = this.mListeners.iterator();
        while (it2.hasNext()) {
            it2.next().onExtrasRemoved(this, i, list);
        }
        if (i == 2) {
            if (this.mConnectionService != null) {
                this.mConnectionService.onExtrasChanged(this, this.mExtras);
            } else {
                Log.e(this, new NullPointerException(), "removeExtras failed due to null CS callId=%s", new Object[]{getId()});
            }
        }
    }

    @VisibleForTesting
    public Bundle getIntentExtras() {
        return this.mIntentExtras;
    }

    void setIntentExtras(Bundle bundle) {
        this.mIntentExtras = bundle;
        this.mIsConferenceInvitation = MtkUtil.isConferenceInvitation(bundle);
        if (this.mIsConferenceInvitation || MtkUtil.isIncomingConferenceCall(bundle)) {
            this.mIsConference = true;
        }
        if (this.mIsConferenceInvitation) {
            addConferenceInvitationParticipants(MtkUtil.getConferenceInvitationNumbers(bundle));
            Iterator<String> it = this.mConferenceInvitationNumbers.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                String next = it.next();
                if (next != null && this.mPhoneNumberUtilsAdapter.isLocalEmergencyNumber(this.mContext, next)) {
                    this.mIsEmergencyCall = true;
                    break;
                }
            }
        }
        Log.d(this, "Call IntentExtras: %s", new Object[]{MtkUtil.dumpBundle(bundle)});
    }

    public Intent getOriginalCallIntent() {
        return this.mOriginalCallIntent;
    }

    public void setOriginalCallIntent(Intent intent) {
        this.mOriginalCallIntent = intent;
    }

    @VisibleForTesting
    public Uri getContactUri() {
        if (this.mCallerInfo == null || !this.mCallerInfo.contactExists) {
            return getHandle();
        }
        return ContactsContract.Contacts.getLookupUri(this.mCallerInfo.contactIdOrZero, this.mCallerInfo.lookupKey);
    }

    Uri getRingtone() {
        if (this.mCallerInfo == null) {
            return null;
        }
        return this.mCallerInfo.contactRingtoneUri;
    }

    void onPostDialWait(String str) {
        Iterator<Listener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onPostDialWait(this, str);
        }
    }

    void onPostDialChar(char c) {
        Iterator<Listener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onPostDialChar(this, c);
        }
    }

    void postDialContinue(boolean z) {
        if (this.mConnectionService != null) {
            this.mConnectionService.onPostDialContinue(this, z);
        } else {
            Log.e(this, new NullPointerException(), "postDialContinue failed due to null CS callId=%s", new Object[]{getId()});
        }
    }

    void conferenceWith(Call call) {
        if (this.mConnectionService == null) {
            Log.w(this, "conference requested on a call without a connection service.", new Object[0]);
        } else {
            Log.addEvent(this, "CONF_WITH", call);
            this.mConnectionService.conference(this, call);
        }
    }

    void splitFromConference() {
        if (this.mConnectionService == null) {
            Log.w(this, "splitting from conference call without a connection service", new Object[0]);
        } else {
            Log.addEvent(this, "CONF_SPLIT");
            this.mConnectionService.splitFromConference(this);
        }
    }

    @VisibleForTesting
    public void mergeConference() {
        if (this.mConnectionService == null) {
            Log.w(this, "merging conference calls without a connection service.", new Object[0]);
        } else if (can(4)) {
            Log.addEvent(this, "CONF_WITH");
            this.mConnectionService.mergeConference(this);
            this.mWasConferencePreviouslyMerged = true;
        }
    }

    @VisibleForTesting
    public void swapConference() {
        List<Call> list;
        int i = 0;
        if (this.mConnectionService == null) {
            Log.w(this, "swapping conference calls without a connection service.", new Object[0]);
            return;
        }
        if (can(8)) {
            Log.addEvent(this, "SWAP");
            this.mConnectionService.swapConference(this);
            switch (this.mChildCalls.size()) {
                case 1:
                    this.mConferenceLevelActiveCall = this.mChildCalls.get(0);
                    break;
                case CallState.SELECT_PHONE_ACCOUNT:
                    if (this.mChildCalls.get(0) == this.mConferenceLevelActiveCall) {
                        list = this.mChildCalls;
                        i = 1;
                    } else {
                        list = this.mChildCalls;
                    }
                    this.mConferenceLevelActiveCall = list.get(i);
                    break;
                default:
                    this.mConferenceLevelActiveCall = null;
                    break;
            }
        }
    }

    public void pullExternalCall() {
        if (this.mConnectionService == null) {
            Log.w(this, "pulling a call without a connection service.", new Object[0]);
        }
        if (!hasProperty(16)) {
            Log.w(this, "pullExternalCall - call %s is not an external call.", new Object[]{this.mId});
        } else if (!can(16777216)) {
            Log.w(this, "pullExternalCall - call %s is external but cannot be pulled.", new Object[]{this.mId});
        } else {
            Log.addEvent(this, "PULL");
            this.mConnectionService.pullExternalCall(this);
        }
    }

    public void sendCallEvent(String str, Bundle bundle) {
        sendCallEvent(str, 0, bundle);
    }

    public void sendCallEvent(String str, int i, Bundle bundle) {
        if (this.mConnectionService != null) {
            if ("android.telecom.event.REQUEST_HANDOVER".equals(str)) {
                if (i > 27) {
                    Log.e(this, new Exception(), "sendCallEvent failed. Use public api handoverTo for API > 27(O-MR1)", new Object[0]);
                }
                Bundle bundle2 = null;
                if (bundle == null) {
                    Log.w(this, "sendCallEvent: %s event received with null extras.", new Object[]{"android.telecom.event.REQUEST_HANDOVER"});
                    this.mConnectionService.sendCallEvent(this, "android.telecom.event.HANDOVER_FAILED", null);
                    return;
                }
                Parcelable parcelable = bundle.getParcelable("android.telecom.extra.HANDOVER_PHONE_ACCOUNT_HANDLE");
                if (!(parcelable instanceof PhoneAccountHandle) || parcelable == null) {
                    Log.w(this, "sendCallEvent: %s event received with invalid handover acct.", new Object[]{"android.telecom.event.REQUEST_HANDOVER"});
                    this.mConnectionService.sendCallEvent(this, "android.telecom.event.HANDOVER_FAILED", null);
                    return;
                }
                PhoneAccountHandle phoneAccountHandle = (PhoneAccountHandle) parcelable;
                int i2 = bundle.getInt("android.telecom.extra.HANDOVER_VIDEO_STATE", 0);
                Parcelable parcelable2 = bundle.getParcelable("android.telecom.extra.HANDOVER_EXTRAS");
                if (parcelable2 instanceof Bundle) {
                    bundle2 = (Bundle) parcelable2;
                }
                requestHandover(phoneAccountHandle, i2, bundle2, true);
                return;
            }
            Log.addEvent(this, "CALL_EVENT", str);
            this.mConnectionService.sendCallEvent(this, str, bundle);
            return;
        }
        Log.e(this, new NullPointerException(), "sendCallEvent failed due to null CS callId=%s", new Object[]{getId()});
    }

    public void handoverTo(PhoneAccountHandle phoneAccountHandle, int i, Bundle bundle) {
        requestHandover(phoneAccountHandle, i, bundle, false);
    }

    void setParentAndChildCall(Call call) {
        boolean z = this.mParentCall != call;
        setParentCall(call);
        setChildOf(call);
        if (z) {
            notifyParentChanged(call);
        }
    }

    void notifyParentChanged(Call call) {
        Log.addEvent(this, "SET_PARENT", call);
        Iterator<Listener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onParentChanged(this);
        }
    }

    void setParentCall(Call call) {
        if (call == this) {
            Log.e(this, new Exception(), "setting the parent to self", new Object[0]);
            return;
        }
        if (call == this.mParentCall) {
            return;
        }
        if (this.mParentCall != null) {
            this.mParentCall.removeChildCall(this);
        }
        boolean z = this.mParentCall != call && call == null;
        this.mParentCall = call;
        if (z) {
            Log.i(this, "Update capability for call %s", new Object[]{this});
            refreshConnectionCapabilities();
        }
    }

    void setChildOf(Call call) {
        if (call != null && !call.getChildCalls().contains(this)) {
            call.addChildCall(this);
        }
    }

    void setConferenceableCalls(List<Call> list) {
        this.mConferenceableCalls.clear();
        this.mConferenceableCalls.addAll(list);
        Iterator<Listener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onConferenceableCallsChanged(this);
        }
    }

    @VisibleForTesting
    public List<Call> getConferenceableCalls() {
        return this.mConferenceableCalls;
    }

    @VisibleForTesting
    public boolean can(int i) {
        return (this.mConnectionCapabilities & i) == i;
    }

    @VisibleForTesting
    public boolean hasProperty(int i) {
        return (this.mConnectionProperties & i) == i;
    }

    private void addChildCall(Call call) {
        if (!this.mChildCalls.contains(call)) {
            this.mConferenceLevelActiveCall = call;
            this.mChildCalls.add(call);
            Log.addEvent(this, "ADD_CHILD", call);
            Iterator<Listener> it = this.mListeners.iterator();
            while (it.hasNext()) {
                it.next().onChildrenChanged(this);
            }
            if (this.mIsConferenceInvitation) {
                String schemeSpecificPart = call.getHandle().getSchemeSpecificPart();
                if (!TextUtils.isEmpty(schemeSpecificPart)) {
                    this.mConnectedConferenceInvitationNumbers.add(schemeSpecificPart);
                }
            }
        }
    }

    private void removeChildCall(Call call) {
        if (this.mChildCalls.remove(call)) {
            Log.addEvent(this, "REMOVE_CHILD", call);
            Iterator<Listener> it = this.mListeners.iterator();
            while (it.hasNext()) {
                it.next().onChildrenChanged(this);
            }
        }
    }

    boolean isRespondViaSmsCapable() {
        return (this.mState != 4 || getHandle() == null || this.mPhoneNumberUtilsAdapter.isUriNumber(getHandle().toString()) || SmsApplication.getDefaultRespondViaMessageApplication(this.mContext, true) == null) ? false : true;
    }

    List<String> getCannedSmsResponses() {
        return this.mCannedSmsResponses;
    }

    private void fixParentAfterDisconnect() {
        setParentAndChildCall(null);
    }

    private boolean isRinging(String str) {
        if (this.mState == 4) {
            return true;
        }
        Log.i(this, "Request to %s a non-ringing call %s", new Object[]{str, this});
        return false;
    }

    private void decrementAssociatedCallCount(ServiceBinder serviceBinder) {
        if (serviceBinder != null) {
            serviceBinder.decrementAssociatedCallCount();
        }
    }

    private void startCallerInfoLookup() {
        this.mCallerInfo = null;
        this.mCallsManager.getCallerInfoLookupHelper().startLookup(this.mHandle, this.mCallerInfoQueryListener, getSubsciptionId());
    }

    private void setCallerInfo(Uri uri, CallerInfo callerInfo) {
        Trace.beginSection("setCallerInfo");
        if (callerInfo == null) {
            Log.i(this, "CallerInfo lookup returned null, skipping update", new Object[0]);
            return;
        }
        if (uri != null && !uri.equals(this.mHandle)) {
            Log.i(this, "setCallerInfo received stale caller info for an old handle. Ignoring.", new Object[0]);
            return;
        }
        this.mCallerInfo = callerInfo;
        Log.i(this, "CallerInfo received for %s: %s", new Object[]{Log.piiHandle(this.mHandle), callerInfo});
        if (this.mCallerInfo.contactDisplayPhotoUri == null || this.mCallerInfo.cachedPhotoIcon != null || this.mCallerInfo.cachedPhoto != null) {
            Iterator<Listener> it = this.mListeners.iterator();
            while (it.hasNext()) {
                it.next().onCallerInfoChanged(this);
            }
        }
        Trace.endSection();
    }

    public CallerInfo getCallerInfo() {
        return this.mCallerInfo;
    }

    private void maybeLoadCannedSmsResponses() {
        if (this.mCallDirection == 2 && isRespondViaSmsCapable() && !this.mCannedSmsResponsesLoadingStarted) {
            Log.d(this, "maybeLoadCannedSmsResponses: starting task to load messages", new Object[0]);
            this.mCannedSmsResponsesLoadingStarted = true;
            this.mCallsManager.getRespondViaSmsManager().loadCannedTextMessages(new Response<Void, List<String>>() {
                public void onResult(Void r4, List<String>... listArr) {
                    if (listArr.length > 0) {
                        Log.d(this, "maybeLoadCannedSmsResponses: got %s", new Object[]{listArr[0]});
                        Call.this.mCannedSmsResponses = listArr[0];
                        Iterator it = Call.this.mListeners.iterator();
                        while (it.hasNext()) {
                            ((Listener) it.next()).onCannedSmsResponsesLoaded(Call.this);
                        }
                    }
                }

                public void onError(Void r4, int i, String str) {
                    Log.w(Call.this, "Error obtaining canned SMS responses: %d %s", new Object[]{Integer.valueOf(i), str});
                }
            }, this.mContext);
            return;
        }
        Log.d(this, "maybeLoadCannedSmsResponses: doing nothing", new Object[0]);
    }

    public void setStartWithSpeakerphoneOn(boolean z) {
        this.mSpeakerphoneOn = z;
    }

    public boolean getStartWithSpeakerphoneOn() {
        return this.mSpeakerphoneOn;
    }

    public void setRequestedToStartWithRtt() {
        this.mDidRequestToStartWithRtt = true;
    }

    public void stopRtt() {
        if (this.mConnectionService != null) {
            this.mConnectionService.stopRtt(this);
        } else {
            Log.w(this, "stopRtt() called before connection service is set.", new Object[0]);
        }
    }

    public void sendRttRequest() {
        createRttStreams();
        this.mConnectionService.startRtt(this, getInCallToCsRttPipeForCs(), getCsToInCallRttPipeForCs());
    }

    private boolean areRttStreamsInitialized() {
        return (this.mInCallToConnectionServiceStreams == null || this.mConnectionServiceToInCallStreams == null) ? false : true;
    }

    public void createRttStreams() {
        if (!areRttStreamsInitialized()) {
            Log.i(this, "Initializing RTT streams", new Object[0]);
            try {
                this.mInCallToConnectionServiceStreams = ParcelFileDescriptor.createReliablePipe();
                this.mConnectionServiceToInCallStreams = ParcelFileDescriptor.createReliablePipe();
            } catch (IOException e) {
                Log.e(this, e, "Failed to create pipes for RTT call.", new Object[0]);
            }
        }
    }

    public void onRttConnectionFailure(int i) {
        Log.i(this, "Got RTT initiation failure with reason %d", new Object[]{Integer.valueOf(i)});
        Iterator<Listener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onRttInitiationFailure(this, i);
        }
    }

    public void onRemoteRttRequest() {
        if (isRttCall()) {
            Log.w(this, "Remote RTT request on a call that's already RTT", new Object[0]);
            return;
        }
        this.mPendingRttRequestId = this.mCallsManager.getNextRttRequestId();
        Iterator<Listener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onRemoteRttRequest(this, this.mPendingRttRequestId);
        }
    }

    public void handleRttRequestResponse(int i, boolean z) {
        if (this.mPendingRttRequestId == -1) {
            Log.w(this, "Response received to a nonexistent RTT request: %d", new Object[]{Integer.valueOf(i)});
            return;
        }
        if (i != this.mPendingRttRequestId) {
            Log.w(this, "Response ID %d does not match expected %d", new Object[]{Integer.valueOf(i), Integer.valueOf(this.mPendingRttRequestId)});
            return;
        }
        if (z) {
            createRttStreams();
            Log.i(this, "RTT request %d accepted.", new Object[]{Integer.valueOf(i)});
            this.mConnectionService.respondToRttRequest(this, getInCallToCsRttPipeForCs(), getCsToInCallRttPipeForCs());
        } else {
            Log.i(this, "RTT request %d rejected.", new Object[]{Integer.valueOf(i)});
            this.mConnectionService.respondToRttRequest(this, null, null);
        }
    }

    public boolean isRttCall() {
        return (this.mConnectionProperties & 256) == 256;
    }

    public boolean wasEverRttCall() {
        return this.mWasEverRtt;
    }

    public ParcelFileDescriptor getCsToInCallRttPipeForCs() {
        if (this.mConnectionServiceToInCallStreams == null) {
            return null;
        }
        return this.mConnectionServiceToInCallStreams[1];
    }

    public ParcelFileDescriptor getInCallToCsRttPipeForCs() {
        if (this.mInCallToConnectionServiceStreams == null) {
            return null;
        }
        return this.mInCallToConnectionServiceStreams[0];
    }

    public ParcelFileDescriptor getCsToInCallRttPipeForInCall() {
        if (this.mConnectionServiceToInCallStreams == null) {
            return null;
        }
        return this.mConnectionServiceToInCallStreams[0];
    }

    public ParcelFileDescriptor getInCallToCsRttPipeForInCall() {
        if (this.mInCallToConnectionServiceStreams == null) {
            return null;
        }
        return this.mInCallToConnectionServiceStreams[1];
    }

    public int getRttMode() {
        return this.mRttMode;
    }

    public void setVideoProvider(IVideoProvider iVideoProvider) {
        Log.v(this, "setVideoProvider", new Object[0]);
        if (this.mVideoProviderProxy != null) {
            this.mVideoProviderProxy.clearVideoCallback();
            this.mVideoProviderProxy = null;
        }
        if (iVideoProvider != null) {
            try {
                this.mVideoProviderProxy = new VideoProviderProxy(this.mLock, iVideoProvider, this, this.mCallsManager);
            } catch (RemoteException e) {
            }
        }
        this.mVideoProvider = iVideoProvider;
        Iterator<Listener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onVideoCallProviderChanged(this);
        }
    }

    public IVideoProvider getVideoProvider() {
        if (this.mVideoProviderProxy == null) {
            return null;
        }
        return this.mVideoProviderProxy.getInterface();
    }

    public VideoProviderProxy getVideoProviderProxy() {
        return this.mVideoProviderProxy;
    }

    public int getVideoState() {
        return this.mVideoState;
    }

    public int getVideoStateHistory() {
        return this.mVideoStateHistory;
    }

    public void setVideoState(int i) {
        if (!isVideoCallingSupported()) {
            Log.d(this, "setVideoState: videoState=%s defaulted to audio (video not supported)", new Object[]{VideoProfile.videoStateToString(i)});
            i = 0;
        }
        if (isActive() || getState() == 7) {
            this.mVideoStateHistory |= i;
        }
        int i2 = this.mVideoState;
        this.mVideoState = i;
        if (this.mVideoState != i2) {
            Log.addEvent(this, "VIDEO_STATE_CHANGED", VideoProfile.videoStateToString(i));
            Iterator<Listener> it = this.mListeners.iterator();
            while (it.hasNext()) {
                it.next().onVideoStateChanged(this, i2, this.mVideoState);
            }
        }
        if (VideoProfile.isVideo(i)) {
            this.mAnalytics.setCallIsVideo(true);
        }
        refreshConnectionCapabilities();
    }

    public boolean getIsVoipAudioMode() {
        return this.mIsVoipAudioMode;
    }

    public void setIsVoipAudioMode(boolean z) {
        this.mIsVoipAudioMode = z;
        Iterator<Listener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onIsVoipAudioModeChanged(this);
        }
    }

    public StatusHints getStatusHints() {
        return this.mStatusHints;
    }

    public void setStatusHints(StatusHints statusHints) {
        this.mStatusHints = statusHints;
        Iterator<Listener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onStatusHintsChanged(this);
        }
    }

    public boolean isUnknown() {
        return this.mCallDirection == 3;
    }

    public boolean isLocallyDisconnecting() {
        return this.mIsLocallyDisconnecting;
    }

    private void setLocallyDisconnecting(boolean z) {
        this.mIsLocallyDisconnecting = z;
    }

    public UserHandle getInitiatingUser() {
        return this.mInitiatingUser;
    }

    public void setInitiatingUser(UserHandle userHandle) {
        Preconditions.checkNotNull(userHandle);
        this.mInitiatingUser = userHandle;
    }

    static int getStateFromConnectionState(int i) {
        switch (i) {
        }
        return 7;
    }

    public boolean isDisconnected() {
        return getState() == 7 || getState() == 8;
    }

    public boolean isNew() {
        return getState() == 0;
    }

    public void setCallDataUsage(long j) {
        this.mCallDataUsage = j;
    }

    public long getCallDataUsage() {
        return this.mCallDataUsage;
    }

    public boolean isNewOutgoingCallIntentBroadcastDone() {
        return this.mIsNewOutgoingCallIntentBroadcastDone;
    }

    public void setNewOutgoingCallIntentBroadcastIsDone() {
        this.mIsNewOutgoingCallIntentBroadcastDone = true;
    }

    public boolean isRemotelyHeld() {
        return this.mIsRemotelyHeld;
    }

    public void onConnectionEvent(String str, Bundle bundle) {
        Log.addEvent(this, "CONNECTION_EVENT", str);
        if (handleConnectionEvent(str, bundle)) {
            return;
        }
        if ("android.telecom.event.ON_HOLD_TONE_START".equals(str)) {
            this.mIsRemotelyHeld = true;
            Log.addEvent(this, "REMOTELY_HELD");
            Iterator<Listener> it = this.mListeners.iterator();
            while (it.hasNext()) {
                it.next().onHoldToneRequested(this);
            }
            return;
        }
        if ("android.telecom.event.ON_HOLD_TONE_END".equals(str)) {
            this.mIsRemotelyHeld = false;
            Log.addEvent(this, "REMOTELY_UNHELD");
            Iterator<Listener> it2 = this.mListeners.iterator();
            while (it2.hasNext()) {
                it2.next().onHoldToneRequested(this);
            }
            return;
        }
        Iterator<Listener> it3 = this.mListeners.iterator();
        while (it3.hasNext()) {
            it3.next().onConnectionEvent(this, str, bundle);
        }
    }

    public void onHandoverComplete() {
        Log.i(this, "onHandoverComplete; callId=%s", new Object[]{getId()});
        if (this.mConnectionService != null) {
            this.mConnectionService.handoverComplete(this);
        }
        Iterator<Listener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onHandoverComplete(this);
        }
    }

    public void onHandoverFailed(int i) {
        Log.i(this, "onHandoverFailed; callId=%s, handoverError=%d", new Object[]{getId(), Integer.valueOf(i)});
        Iterator<Listener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onHandoverFailed(this, i);
        }
    }

    public void setOriginalConnectionId(String str) {
        this.mOriginalConnectionId = str;
    }

    public String getOriginalConnectionId() {
        return this.mOriginalConnectionId;
    }

    public ConnectionServiceFocusManager getConnectionServiceFocusManager() {
        return this.mCallsManager.getConnectionServiceFocusManager();
    }

    private boolean doesCallSupportVideo(int i) {
        return ((i & 768) == 0 && (i & 3072) == 0) ? false : true;
    }

    private int removeVideoCapabilities(int i) {
        return i & (-3841);
    }

    private void requestHandover(PhoneAccountHandle phoneAccountHandle, int i, Bundle bundle, boolean z) {
        Iterator<Listener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onHandoverRequested(this, phoneAccountHandle, i, bundle, z);
        }
    }

    private void updateVideoHistoryViaState(int i, int i2) {
        if (isVoiceCallInPlayVideoRingtone()) {
            Log.d(this, "updateVideoHistoryViaState:voice call in play video ringtone,ignore setvideo history, mVideoStateHistory" + this.mVideoStateHistory, new Object[0]);
            this.mVideoStateHistory = 0;
            return;
        }
        if ((i == 3 || i == 4) && i2 == 5) {
            this.mVideoStateHistory = this.mVideoState;
        }
        this.mVideoStateHistory |= this.mVideoState;
    }

    boolean wasHighDefAudio() {
        return this.mWasHighDefAudio;
    }

    public static void lambda$new$0(Call call, int i) {
        boolean z = i == 1;
        Log.i(call, "[onRecordStateChanged] isVoiceRecording: %s, new state: %s,new isVoiceRecording: %s, mState: %s", new Object[]{Boolean.valueOf(call.mIsVoiceRecording), Integer.valueOf(i), Boolean.valueOf(z), Integer.valueOf(call.mState)});
        if (z != call.mIsVoiceRecording) {
            call.mIsVoiceRecording = z;
            if (call.mIsVoiceRecording && (call.mState != 5 || call.mParentCall != null)) {
                Log.i(call, "stop record because state is not active", new Object[0]);
                call.stopVoiceRecord();
            }
            call.refreshConnectionProperties();
        }
    }

    private void maybeUpdateVoiceRecordState() {
        if (this.mState != 7) {
            refreshConnectionCapabilities();
        }
        if (this.mIsVoiceRecording && this.mState != 5) {
            Log.w(this, "[maybeUpdateVoiceRecordState] no longer ACTIVE, force stop recording", new Object[0]);
            this.mCallsManager.stopVoiceRecord();
        }
    }

    boolean canVoiceRecord() {
        return this.mCallsManager.isCallRecorderSupported() && getTargetPhoneAccount() != null && TelephonyUtil.isPstnComponentName(getTargetPhoneAccount().getComponentName()) && this.mState == 5 && getParentCall() == null;
    }

    boolean isVoiceRecording() {
        return this.mIsVoiceRecording;
    }

    void startVoiceRecord() {
        CallRecorderManager callRecorderManager = this.mCallsManager.getCallRecorderManager();
        if (callRecorderManager.startVoiceRecord(this)) {
            callRecorderManager.setListener(this.mRecordStateListener);
        }
    }

    void stopVoiceRecord() {
        this.mCallsManager.getCallRecorderManager().stopVoiceRecord(this);
    }

    public boolean isCdma() {
        return hasProperty(65536);
    }

    public boolean isDialingCdmaCall() {
        return isCdma() && !isIncoming() && this.mState == 5 && !this.mIsAcceptedCdmaMoCall && getParentCall() == null && this.mChildCalls.size() == 0 && !hasProperty(32768);
    }

    private boolean handleConnectionEvent(String str, Bundle bundle) {
        if (this.mGttEventExt.handleGttEvent(str, bundle, this, this.mListeners)) {
            Log.d(this, "GTT event " + str, new Object[0]);
            return true;
        }
        if (this.mRttEventExt.handleRttEvent(str, bundle, this, this.mListeners)) {
            Log.d(this, "RTT event " + str, new Object[0]);
            return true;
        }
        switch (str) {
            case "mediatek.telecom.event.SS_NOTIFICATION":
                new SuppMessageHelper().onSsNotification(bundle);
                return true;
            case "mediatek.telecom.event.OPERATION_FAILED":
                int i = bundle.getInt("mediatek.telecom.extra.FAILED_OPERATION");
                Log.d(this, "operation failed: " + i, new Object[0]);
                this.mCallsManager.notifyActionFailed(this, i);
                Iterator<Listener> it = this.mListeners.iterator();
                while (it.hasNext()) {
                    it.next().onConnectionEvent(this, str, bundle);
                }
                return true;
            case "mediatek.telecom.event.EVENT_CALL_ALERTING_NOTIFICATION":
                Log.d(this, "[handleConnectionEvent] EVENT_CALL_ALERTING_NOTIFICATION", new Object[0]);
                this.mIsInAlertingState = true;
                this.mCallsManager.notifyCallAlertingEvent(this);
                return true;
            case "mediatek.telecom.event.EVENT_VOLTE_MARKED_AS_EMERGENCY":
                Log.d(this, "[handleConnectionEvent] EVENT_VOLTE_MARKED_AS_EMERGENCY", new Object[0]);
                this.mIsEmergencyCall = true;
                Iterator<Listener> it2 = this.mListeners.iterator();
                while (it2.hasNext()) {
                    it2.next().onConnectionCapabilitiesChanged(this);
                }
                return true;
            case "mediatek.telecom.event.PHONE_ACCOUNT_CHANGED":
                PhoneAccountHandle phoneAccountHandle = (PhoneAccountHandle) bundle.getParcelable("mediatek.telecom.extra.PHONE_ACCOUNT_HANDLE");
                Log.d(this, "[handleConnectionEvent] EVENT_PHONE_ACCOUNT_CHANGED : %s", new Object[]{phoneAccountHandle});
                setTargetPhoneAccount(phoneAccountHandle);
                return true;
            case "mediatek.telecom.event.NUMBER_UPDATED":
                String string = bundle.getString("mediatek.telecom.extra.NEW_NUMBER");
                Log.d(this, "[handleConnectionEvent] EVENT_NUMBER_UPDATED : %s", new Object[]{string});
                Uri handle = getHandle();
                setHandle(Uri.fromParts(handle.getScheme(), string, handle.getFragment()), getHandlePresentation());
                return true;
            case "mediatek.telecom.event.INCOMING_INFO_UPDATED":
                int i2 = bundle.getInt("mediatek.telecom.extra.UPDATED_INCOMING_INFO_CLI_VALIDITY", -1);
                int iCliValidityToPresentation = MtkUtil.cliValidityToPresentation(i2);
                Log.d(this, "[handleConnectionEvent] EVENT_INCOMING_INFO_UPDATED, new presentation: %s -> %s", new Object[]{Integer.valueOf(i2), Integer.valueOf(iCliValidityToPresentation)});
                if (iCliValidityToPresentation >= 0) {
                    setHandle(getHandle(), iCliValidityToPresentation);
                }
                return true;
            case "mediatek.telecom.event.CDMA_CALL_ACCEPTED":
                Log.d(this, "[handleConnectionEvent] CDMA call accepted", new Object[0]);
                this.mIsAcceptedCdmaMoCall = true;
                this.mConnectTimeMillis = this.mClockProxy.currentTimeMillis();
                this.mConnectElapsedTimeMillis = this.mClockProxy.elapsedRealtime();
                Iterator<Listener> it3 = this.mListeners.iterator();
                while (it3.hasNext()) {
                    it3.next().forceUpdateCall(this);
                }
                this.mCallsManager.onCdmaMoCallAccepted();
                return true;
            case "mediatek.telecom.event.EVENT_DEVICE_SWITCH_SUCCESS":
                Log.d(this, "[handleConnectionEvent] EVENT_DEVICE_SWITCH_SUCCESS", new Object[0]);
                return false;
            case "mediatek.telecom.event.EVENT_DEVICE_SWITCH_FAILED":
                Log.d(this, "[handleConnectionEvent] EVENT_DEVICE_SWITCH_FAILED", new Object[0]);
                return false;
            default:
                return false;
        }
    }

    void hangupAll() {
        Preconditions.checkNotNull(this.mConnectionService);
        Log.d(this, "hangupAll %s", new Object[]{this});
        this.mConnectionService.hangupAll(this);
    }

    public boolean isInAlertingState() {
        return this.mIsInAlertingState;
    }

    void explicitCallTransfer() {
        Log.d(this, "explicitCallTransfer %s", new Object[]{getId()});
        this.mConnectionService.explicitCallTransfer(this);
    }

    void refreshConnectionCapabilities() {
        setConnectionCapabilities(this.mLastConnectionCapabilities, false);
    }

    void refreshConnectionProperties() {
        setConnectionProperties(this.mLastConnectionProperties);
    }

    private void updateSubscriptionId() {
        this.mSubId = MtkUtil.getSubIdForPhoneAccountHandle(getTargetPhoneAccount());
    }

    public int getSubsciptionId() {
        return this.mSubId;
    }

    boolean isModifyingVideoSession() {
        if (this.mVideoProviderProxy == null) {
            return false;
        }
        return this.mVideoProviderProxy.isModifyingVideoSession();
    }

    private void addConferenceInvitationParticipants(List<String> list) {
        this.mConferenceInvitationNumbers.clear();
        if (list != null) {
            this.mConferenceInvitationNumbers.addAll(getFilteredPaticipants(list));
        }
    }

    List<String> getConferenceInvitationNumbers() {
        return this.mConferenceInvitationNumbers;
    }

    boolean isConferenceInvitation() {
        return this.mIsConferenceInvitation;
    }

    List<String> getUnconnectedConferenceInvitationNumbers() {
        boolean z;
        ArrayList arrayList = new ArrayList();
        for (String str : this.mConferenceInvitationNumbers) {
            for (String str2 : this.mConnectedConferenceInvitationNumbers) {
                if (str.equals(str2) || PhoneNumberUtils.compare(str, str2)) {
                    z = true;
                    break;
                }
            }
            z = false;
            if (!z) {
                Log.d(this, "getUnconnectedParticipants, participant = " + str, new Object[0]);
                arrayList.add(str);
            }
        }
        return arrayList;
    }

    public void handleCreateConferenceSuccess(ParcelableConference parcelableConference) {
        Log.d(this, "handleCreateConferenceSuccess %s", new Object[]{parcelableConference});
        this.mCreateConnectionProcessor = null;
        setTargetPhoneAccount(parcelableConference.getPhoneAccount());
        setConnectionCapabilities(parcelableConference.getConnectionCapabilities(), false);
        setConnectionProperties(parcelableConference.getConnectionProperties());
        Iterator<Listener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onSuccessfulOutgoingCall(this, getStateFromConnectionState(parcelableConference.getState()));
        }
    }

    public void inviteNumbersToConference(ArrayList<String> arrayList) {
        if (arrayList == null || arrayList.isEmpty()) {
            Log.w(this, "inviteNumbersToConference, no number found", new Object[0]);
            return;
        }
        List<String> newAddedParticipants = getNewAddedParticipants(arrayList);
        if (newAddedParticipants == null || newAddedParticipants.isEmpty()) {
            Log.w(this, "inviteNumbersToConference, no new number found", new Object[0]);
        } else {
            this.mConnectionService.inviteNumbersToConference(this, newAddedParticipants);
        }
    }

    void disconnectActiveForAnswerWaiting() {
        if (this.mConnectionService == null) {
            Log.e(this, new Exception(), "disconnect() request on a call without a connection service.", new Object[0]);
            return;
        }
        Log.i(this, "[disconnectActiveForAnswerWaiting] on " + getId(), new Object[0]);
        this.mConnectionService.disconnectWithPendingAction(this, "mediatek.telecom.operation.ANSWER_CALL");
    }

    public boolean isVideoCallExcludeVideoRingtone() {
        if (VideoProfile.isTransmissionEnabled(this.mVideoState)) {
            return true;
        }
        if (VideoProfile.isVideo(this.mVideoState) && this.mState != 3) {
            return true;
        }
        return false;
    }

    boolean isVideo() {
        return VideoProfile.isVideo(getVideoState());
    }

    public IGttEventExt getGttEventExt() {
        return this.mGttEventExt;
    }

    public IRttEventExt getRttEventExt() {
        return this.mRttEventExt;
    }

    public boolean isVoiceCallInPlayVideoRingtone() {
        if (VideoProfile.isVideo(this.mVideoState) && !VideoProfile.isTransmissionEnabled(this.mVideoState) && (this.mConnectionCapabilities & Integer.MIN_VALUE) == Integer.MIN_VALUE) {
            return true;
        }
        return false;
    }

    void deviceSwitch(Call call, String str, String str2) {
        Log.d(this, "deviceSwitch  %s %s %s", new Object[]{getId(), str, str2});
        this.mConnectionService.deviceSwitch(call, str, str2);
    }

    void cancelDeviceSwitch(Call call) {
        Log.d(this, "cancelDeviceSwitch  %s", new Object[]{getId()});
        this.mConnectionService.cancelDeviceSwitch(call);
    }

    public long getConferenceCallLogId() {
        return this.mConferenceCallLogId;
    }

    public void setConferenceCallLogId(long j) {
        this.mConferenceCallLogId = j;
    }

    void explicitCallTransfer(Call call, String str, int i) {
        Log.d(this, "explicitCallTransfer  %s %s %d", new Object[]{getId(), Log.piiHandle(str), Integer.valueOf(i)});
        if (str == null || str.isEmpty()) {
            Log.w(this, "explicitCallTransfer, no number found", new Object[0]);
        } else {
            this.mConnectionService.explicitCallTransfer(call, str, i);
        }
    }

    public void setVoiceMailEmergencyCallIfNeeded() {
        if (this.mHandle != null && getTargetPhoneAccount() != null && "voicemail".equals(this.mHandle.getScheme())) {
            TelecomManager telecomManagerFrom = TelecomManager.from(this.mContext);
            if (telecomManagerFrom != null) {
                String voiceMailNumber = telecomManagerFrom.getVoiceMailNumber(getTargetPhoneAccount());
                Log.i(this, "setVoiceMailEccIfNeeded, voicemail number = " + Log.piiHandle(voiceMailNumber), new Object[0]);
                if (!TextUtils.isEmpty(voiceMailNumber) && this.mPhoneNumberUtilsAdapter.isLocalEmergencyNumber(this.mContext, voiceMailNumber)) {
                    this.mIsVoiceMailEmergencyCall = true;
                    return;
                }
                return;
            }
            Log.w(this, "Weird, TelecomManager is not found", new Object[0]);
        }
    }

    private List<String> getFilteredPaticipants(List<String> list) {
        ArrayList arrayList = new ArrayList(list);
        int size = list.size();
        int i = 0;
        while (i < size) {
            int i2 = i + 1;
            int i3 = i2;
            while (true) {
                if (i3 >= size) {
                    break;
                }
                if (!this.mPhoneNumberUtilsAdapter.isSamePhoneNumber(list.get(i), list.get(i3))) {
                    i3++;
                } else {
                    Log.i(this, "getFilteredPaticipants, same number = " + Log.pii(list.get(i)), new Object[0]);
                    arrayList.remove(list.get(i));
                    break;
                }
            }
            i = i2;
        }
        return arrayList;
    }

    private List<String> getNewAddedParticipants(List<String> list) {
        ArrayList arrayList = new ArrayList(list);
        int size = list.size();
        for (int i = 0; i < size; i++) {
            Iterator<Call> it = this.mCallsManager.getCalls().iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                Call next = it.next();
                if (next != null && next.getHandle() != null) {
                    if (this.mPhoneNumberUtilsAdapter.isSamePhoneNumber(list.get(i), next.getHandle().getSchemeSpecificPart())) {
                        Log.i(this, "getNewAddedParticipants, same number = " + Log.pii(list.get(i)), new Object[0]);
                        arrayList.remove(list.get(i));
                        break;
                    }
                }
            }
        }
        return arrayList;
    }
}
