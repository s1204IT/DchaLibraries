package com.android.server.telecom;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Trace;
import android.provider.ContactsContract;
import android.telecom.CallState;
import android.telecom.Connection;
import android.telecom.DisconnectCause;
import android.telecom.GatewayInfo;
import android.telecom.ParcelableConnection;
import android.telecom.PhoneAccountHandle;
import android.telecom.Response;
import android.telecom.StatusHints;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import com.android.internal.telecom.IVideoProvider;
import com.android.internal.telephony.CallerInfo;
import com.android.internal.telephony.CallerInfoAsyncQuery;
import com.android.internal.telephony.SmsApplication;
import com.android.internal.util.Preconditions;
import com.android.server.telecom.ContactsAsyncHelper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

final class Call implements CreateConnectionResponse {
    private static final CallerInfoAsyncQuery.OnQueryCompleteListener sCallerInfoQueryListener = new CallerInfoAsyncQuery.OnQueryCompleteListener() {
        public void onQueryComplete(int i, Object obj, CallerInfo callerInfo) {
            if (obj != null) {
                ((Call) obj).setCallerInfo(callerInfo, i);
            }
        }
    };
    private static final ContactsAsyncHelper.OnImageLoadCompleteListener sPhotoLoadListener = new ContactsAsyncHelper.OnImageLoadCompleteListener() {
        @Override
        public void onImageLoadComplete(int i, Drawable drawable, Bitmap bitmap, Object obj) {
            if (obj != null) {
                ((Call) obj).setPhoto(drawable, bitmap, i);
            }
        }
    };
    private String mCallerDisplayName;
    private int mCallerDisplayNamePresentation;
    private CallerInfo mCallerInfo;
    private List<String> mCannedSmsResponses;
    private boolean mCannedSmsResponsesLoadingStarted;
    private List<Call> mChildCalls;
    private Call mConferenceLevelActiveCall;
    private final List<Call> mConferenceableCalls;
    private long mConnectTimeMillis;
    private int mConnectionCapabilities;
    private PhoneAccountHandle mConnectionManagerPhoneAccountHandle;
    private ConnectionServiceWrapper mConnectionService;
    private final Context mContext;
    private CreateConnectionProcessor mCreateConnectionProcessor;
    private long mCreationTimeMillis;
    private boolean mDirectToVoicemailQueryPending;
    private final Runnable mDirectToVoicemailRunnable;
    private DisconnectCause mDisconnectCause;
    private long mDisconnectTimeMillis;
    private Bundle mExtras;
    private GatewayInfo mGatewayInfo;
    private Uri mHandle;
    private int mHandlePresentation;
    private final Handler mHandler;
    private boolean mIsConference;
    private boolean mIsEmergencyCall;
    private final boolean mIsIncoming;
    private boolean mIsLocallyDisconnecting;
    private boolean mIsUnknown;
    private boolean mIsVoipAudioMode;
    private final Set<Listener> mListeners;
    private Call mParentCall;
    private int mQueryToken;
    private final ConnectionServiceRepository mRepository;
    private boolean mRingbackRequested;
    private boolean mSpeakerphoneOn;
    private int mState;
    private StatusHints mStatusHints;
    private PhoneAccountHandle mTargetPhoneAccountHandle;
    private IVideoProvider mVideoProvider;
    private int mVideoState;
    private int mVideoStateHistory;
    private boolean mWasConferencePreviouslyMerged;

    interface Listener {
        void onCallerDisplayNameChanged(Call call);

        void onCallerInfoChanged(Call call);

        boolean onCanceledViaNewOutgoingCallBroadcast(Call call);

        void onCannedSmsResponsesLoaded(Call call);

        void onChildrenChanged(Call call);

        void onConferenceableCallsChanged(Call call);

        void onConnectionCapabilitiesChanged(Call call);

        void onConnectionManagerPhoneAccountChanged(Call call);

        void onFailedIncomingCall(Call call);

        void onFailedOutgoingCall(Call call, DisconnectCause disconnectCause);

        void onFailedUnknownCall(Call call);

        void onHandleChanged(Call call);

        void onIsVoipAudioModeChanged(Call call);

        void onParentChanged(Call call);

        void onPostDialChar(Call call, char c);

        void onPostDialWait(Call call, String str);

        void onRingbackRequested(Call call, boolean z);

        void onStatusHintsChanged(Call call);

        void onSuccessfulIncomingCall(Call call);

        void onSuccessfulOutgoingCall(Call call, int i);

        void onSuccessfulUnknownCall(Call call, int i);

        void onTargetPhoneAccountChanged(Call call);

        void onVideoCallProviderChanged(Call call);

        void onVideoStateChanged(Call call);
    }

    static abstract class ListenerBase implements Listener {
        ListenerBase() {
        }

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
        public void onHandleChanged(Call call) {
        }

        @Override
        public void onCallerDisplayNameChanged(Call call) {
        }

        @Override
        public void onVideoStateChanged(Call call) {
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
        public boolean onCanceledViaNewOutgoingCallBroadcast(Call call) {
            return false;
        }
    }

    Call(Context context, ConnectionServiceRepository connectionServiceRepository, Uri uri, GatewayInfo gatewayInfo, PhoneAccountHandle phoneAccountHandle, PhoneAccountHandle phoneAccountHandle2, boolean z, boolean z2) {
        this.mDirectToVoicemailRunnable = new Runnable() {
            @Override
            public void run() {
                Call.this.processDirectToVoicemail();
            }
        };
        this.mCreationTimeMillis = System.currentTimeMillis();
        this.mConnectTimeMillis = 0L;
        this.mDisconnectTimeMillis = 0L;
        this.mHandler = new Handler();
        this.mConferenceableCalls = new ArrayList();
        this.mDisconnectCause = new DisconnectCause(0);
        this.mExtras = Bundle.EMPTY;
        this.mListeners = Collections.newSetFromMap(new ConcurrentHashMap(8, 0.9f, 1));
        this.mQueryToken = 0;
        this.mRingbackRequested = false;
        this.mIsConference = false;
        this.mParentCall = null;
        this.mChildCalls = new LinkedList();
        this.mCannedSmsResponses = Collections.EMPTY_LIST;
        this.mCannedSmsResponsesLoadingStarted = false;
        this.mWasConferencePreviouslyMerged = false;
        this.mConferenceLevelActiveCall = null;
        this.mIsLocallyDisconnecting = false;
        this.mState = z2 ? 5 : 0;
        this.mContext = context;
        this.mRepository = connectionServiceRepository;
        setHandle(uri);
        setHandle(uri, 1);
        this.mGatewayInfo = gatewayInfo;
        setConnectionManagerPhoneAccount(phoneAccountHandle);
        setTargetPhoneAccount(phoneAccountHandle2);
        this.mIsIncoming = z;
        this.mIsConference = z2;
        maybeLoadCannedSmsResponses();
    }

    Call(Context context, ConnectionServiceRepository connectionServiceRepository, Uri uri, GatewayInfo gatewayInfo, PhoneAccountHandle phoneAccountHandle, PhoneAccountHandle phoneAccountHandle2, boolean z, boolean z2, long j) {
        this(context, connectionServiceRepository, uri, gatewayInfo, phoneAccountHandle, phoneAccountHandle2, z, z2);
        this.mConnectTimeMillis = j;
    }

    void addListener(Listener listener) {
        this.mListeners.add(listener);
    }

    void removeListener(Listener listener) {
        if (listener != null) {
            this.mListeners.remove(listener);
        }
    }

    public String toString() {
        String strFlattenToShortString = null;
        if (this.mConnectionService != null && this.mConnectionService.getComponentName() != null) {
            strFlattenToShortString = this.mConnectionService.getComponentName().flattenToShortString();
        }
        Locale locale = Locale.US;
        Object[] objArr = new Object[8];
        objArr[0] = Integer.valueOf(System.identityHashCode(this));
        objArr[1] = CallState.toString(this.mState);
        objArr[2] = strFlattenToShortString;
        objArr[3] = Log.piiHandle(this.mHandle);
        objArr[4] = Integer.valueOf(getVideoState());
        objArr[5] = Integer.valueOf(getChildCalls().size());
        objArr[6] = Boolean.valueOf(getParentCall() != null);
        objArr[7] = Connection.capabilitiesToString(getConnectionCapabilities());
        return String.format(locale, "[%s, %s, %s, %s, %d, childs(%d), has_parent(%b), [%s]", objArr);
    }

    int getState() {
        return this.mState;
    }

    private boolean shouldContinueProcessingAfterDisconnect() {
        if (CreateConnectionTimeout.isCallBeingPlaced(this) && this.mCreateConnectionProcessor != null && this.mCreateConnectionProcessor.isProcessingComplete() && this.mCreateConnectionProcessor.hasMorePhoneAccounts() && this.mDisconnectCause != null) {
            return this.mDisconnectCause.getCode() == 1 || this.mCreateConnectionProcessor.isCallTimedOut();
        }
        return false;
    }

    void setState(int i) {
        if (this.mState != i) {
            Log.v(this, "setState %s -> %s", Integer.valueOf(this.mState), Integer.valueOf(i));
            if (i == 7 && shouldContinueProcessingAfterDisconnect()) {
                Log.w(this, "continuing processing disconnected call with another service", new Object[0]);
                this.mCreateConnectionProcessor.continueProcessingIfPossible(this, this.mDisconnectCause);
                return;
            }
            this.mState = i;
            maybeLoadCannedSmsResponses();
            if (this.mState == 5 || this.mState == 6) {
                if (this.mConnectTimeMillis == 0) {
                    this.mConnectTimeMillis = System.currentTimeMillis();
                }
                this.mDisconnectTimeMillis = 0L;
            } else if (this.mState == 7) {
                this.mDisconnectTimeMillis = System.currentTimeMillis();
                setLocallyDisconnecting(false);
                fixParentAfterDisconnect();
            }
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

    boolean isConference() {
        return this.mIsConference;
    }

    Uri getHandle() {
        return this.mHandle;
    }

    int getHandlePresentation() {
        return this.mHandlePresentation;
    }

    void setHandle(Uri uri) {
        setHandle(uri, 1);
    }

    void setHandle(Uri uri, int i) {
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
            this.mIsEmergencyCall = this.mHandle != null && PhoneNumberUtils.isLocalEmergencyNumber(this.mContext, this.mHandle.getSchemeSpecificPart());
            startCallerInfoLookup();
            Iterator<Listener> it = this.mListeners.iterator();
            while (it.hasNext()) {
                it.next().onHandleChanged(this);
            }
        }
    }

    String getCallerDisplayName() {
        return this.mCallerDisplayName;
    }

    int getCallerDisplayNamePresentation() {
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

    String getName() {
        if (this.mCallerInfo == null) {
            return null;
        }
        return this.mCallerInfo.name;
    }

    Bitmap getPhotoIcon() {
        if (this.mCallerInfo == null) {
            return null;
        }
        return this.mCallerInfo.cachedPhotoIcon;
    }

    Drawable getPhoto() {
        if (this.mCallerInfo == null) {
            return null;
        }
        return this.mCallerInfo.cachedPhoto;
    }

    void setDisconnectCause(DisconnectCause disconnectCause) {
        this.mDisconnectCause = disconnectCause;
    }

    DisconnectCause getDisconnectCause() {
        return this.mDisconnectCause;
    }

    boolean isEmergencyCall() {
        return this.mIsEmergencyCall;
    }

    public Uri getOriginalHandle() {
        return (this.mGatewayInfo == null || this.mGatewayInfo.isEmpty()) ? getHandle() : this.mGatewayInfo.getOriginalAddress();
    }

    GatewayInfo getGatewayInfo() {
        return this.mGatewayInfo;
    }

    void setGatewayInfo(GatewayInfo gatewayInfo) {
        this.mGatewayInfo = gatewayInfo;
    }

    PhoneAccountHandle getConnectionManagerPhoneAccount() {
        return this.mConnectionManagerPhoneAccountHandle;
    }

    void setConnectionManagerPhoneAccount(PhoneAccountHandle phoneAccountHandle) {
        if (!Objects.equals(this.mConnectionManagerPhoneAccountHandle, phoneAccountHandle)) {
            this.mConnectionManagerPhoneAccountHandle = phoneAccountHandle;
            Iterator<Listener> it = this.mListeners.iterator();
            while (it.hasNext()) {
                it.next().onConnectionManagerPhoneAccountChanged(this);
            }
        }
    }

    PhoneAccountHandle getTargetPhoneAccount() {
        return this.mTargetPhoneAccountHandle;
    }

    void setTargetPhoneAccount(PhoneAccountHandle phoneAccountHandle) {
        if (!Objects.equals(this.mTargetPhoneAccountHandle, phoneAccountHandle)) {
            this.mTargetPhoneAccountHandle = phoneAccountHandle;
            Iterator<Listener> it = this.mListeners.iterator();
            while (it.hasNext()) {
                it.next().onTargetPhoneAccountChanged(this);
            }
        }
    }

    boolean isIncoming() {
        return this.mIsIncoming;
    }

    long getAgeMillis() {
        if ((this.mState == 7 && (this.mDisconnectCause.getCode() == 6 || this.mDisconnectCause.getCode() == 5)) || this.mConnectTimeMillis == 0) {
            return 0L;
        }
        if (this.mDisconnectTimeMillis == 0) {
            return System.currentTimeMillis() - this.mConnectTimeMillis;
        }
        return this.mDisconnectTimeMillis - this.mConnectTimeMillis;
    }

    long getCreationTimeMillis() {
        return this.mCreationTimeMillis;
    }

    void setCreationTimeMillis(long j) {
        this.mCreationTimeMillis = j;
    }

    long getConnectTimeMillis() {
        return this.mConnectTimeMillis;
    }

    int getConnectionCapabilities() {
        return this.mConnectionCapabilities;
    }

    void setConnectionCapabilities(int i) {
        setConnectionCapabilities(i, false);
    }

    void setConnectionCapabilities(int i, boolean z) {
        Log.v(this, "setConnectionCapabilities: %s", Connection.capabilitiesToString(i));
        if (z || this.mConnectionCapabilities != i) {
            this.mConnectionCapabilities = i;
            Iterator<Listener> it = this.mListeners.iterator();
            while (it.hasNext()) {
                it.next().onConnectionCapabilitiesChanged(this);
            }
        }
    }

    Call getParentCall() {
        return this.mParentCall;
    }

    List<Call> getChildCalls() {
        return this.mChildCalls;
    }

    boolean wasConferencePreviouslyMerged() {
        return this.mWasConferencePreviouslyMerged;
    }

    Call getConferenceLevelActiveCall() {
        return this.mConferenceLevelActiveCall;
    }

    ConnectionServiceWrapper getConnectionService() {
        return this.mConnectionService;
    }

    Context getContext() {
        return this.mContext;
    }

    void setConnectionService(ConnectionServiceWrapper connectionServiceWrapper) {
        Preconditions.checkNotNull(connectionServiceWrapper);
        clearConnectionService();
        connectionServiceWrapper.incrementAssociatedCallCount();
        this.mConnectionService = connectionServiceWrapper;
        this.mConnectionService.addCall(this);
    }

    void clearConnectionService() {
        if (this.mConnectionService != null) {
            ConnectionServiceWrapper connectionServiceWrapper = this.mConnectionService;
            this.mConnectionService = null;
            connectionServiceWrapper.removeCall(this);
            decrementAssociatedCallCount(connectionServiceWrapper);
        }
    }

    private void processDirectToVoicemail() {
        if (this.mDirectToVoicemailQueryPending) {
            if (this.mCallerInfo != null && this.mCallerInfo.shouldSendToVoicemail) {
                Log.i(this, "Directing call to voicemail: %s.", this);
                setState(4);
                reject(false, null);
            } else {
                Iterator<Listener> it = this.mListeners.iterator();
                while (it.hasNext()) {
                    it.next().onSuccessfulIncomingCall(this);
                }
            }
            this.mDirectToVoicemailQueryPending = false;
        }
    }

    void startCreateConnection(PhoneAccountRegistrar phoneAccountRegistrar) {
        Preconditions.checkState(this.mCreateConnectionProcessor == null);
        this.mCreateConnectionProcessor = new CreateConnectionProcessor(this, this.mRepository, this, phoneAccountRegistrar, this.mContext);
        this.mCreateConnectionProcessor.process();
    }

    @Override
    public void handleCreateConnectionSuccess(CallIdMapper callIdMapper, ParcelableConnection parcelableConnection) {
        Log.v(this, "handleCreateConnectionSuccessful %s", parcelableConnection);
        setTargetPhoneAccount(parcelableConnection.getPhoneAccount());
        setHandle(parcelableConnection.getHandle(), parcelableConnection.getHandlePresentation());
        setCallerDisplayName(parcelableConnection.getCallerDisplayName(), parcelableConnection.getCallerDisplayNamePresentation());
        setConnectionCapabilities(parcelableConnection.getConnectionCapabilities());
        setVideoProvider(parcelableConnection.getVideoProvider());
        setVideoState(parcelableConnection.getVideoState());
        setRingbackRequested(parcelableConnection.isRingbackRequested());
        setIsVoipAudioMode(parcelableConnection.getIsVoipAudioMode());
        setStatusHints(parcelableConnection.getStatusHints());
        this.mConferenceableCalls.clear();
        Iterator it = parcelableConnection.getConferenceableConnectionIds().iterator();
        while (it.hasNext()) {
            this.mConferenceableCalls.add(callIdMapper.getCall((String) it.next()));
        }
        if (this.mIsUnknown) {
            Iterator<Listener> it2 = this.mListeners.iterator();
            while (it2.hasNext()) {
                it2.next().onSuccessfulUnknownCall(this, getStateFromConnectionState(parcelableConnection.getState()));
            }
        } else if (this.mIsIncoming) {
            this.mDirectToVoicemailQueryPending = true;
            this.mHandler.postDelayed(this.mDirectToVoicemailRunnable, Timeouts.getDirectToVoicemailMillis(this.mContext.getContentResolver()));
        } else {
            Iterator<Listener> it3 = this.mListeners.iterator();
            while (it3.hasNext()) {
                it3.next().onSuccessfulOutgoingCall(this, getStateFromConnectionState(parcelableConnection.getState()));
            }
        }
    }

    @Override
    public void handleCreateConnectionFailure(DisconnectCause disconnectCause) {
        clearConnectionService();
        setDisconnectCause(disconnectCause);
        CallsManager.getInstance().markCallAsDisconnected(this, disconnectCause);
        if (this.mIsUnknown) {
            Iterator<Listener> it = this.mListeners.iterator();
            while (it.hasNext()) {
                it.next().onFailedUnknownCall(this);
            }
        } else if (this.mIsIncoming) {
            Iterator<Listener> it2 = this.mListeners.iterator();
            while (it2.hasNext()) {
                it2.next().onFailedIncomingCall(this);
            }
        } else {
            Iterator<Listener> it3 = this.mListeners.iterator();
            while (it3.hasNext()) {
                it3.next().onFailedOutgoingCall(this, disconnectCause);
            }
        }
    }

    void playDtmfTone(char c) {
        if (this.mConnectionService == null) {
            Log.w(this, "playDtmfTone() request on a call without a connection service.", new Object[0]);
        } else {
            Log.i(this, "Send playDtmfTone to connection service for call %s", this);
            this.mConnectionService.playDtmfTone(this, c);
        }
    }

    void stopDtmfTone() {
        if (this.mConnectionService == null) {
            Log.w(this, "stopDtmfTone() request on a call without a connection service.", new Object[0]);
        } else {
            Log.i(this, "Send stopDtmfTone to connection service for call %s", this);
            this.mConnectionService.stopDtmfTone(this);
        }
    }

    void disconnect() {
        disconnect(false);
    }

    void disconnect(boolean z) {
        setLocallyDisconnecting(true);
        if (this.mState == 0 || this.mState == 2 || this.mState == 1) {
            Log.v(this, "Aborting call %s", this);
            abort(z);
        } else if (this.mState != 8 && this.mState != 7) {
            if (this.mConnectionService == null) {
                Log.e(this, new Exception(), "disconnect() request on a call without a connection service.", new Object[0]);
            } else {
                Log.i(this, "Send disconnect to connection service for call: %s", this);
                this.mConnectionService.disconnect(this);
            }
        }
    }

    void abort(boolean z) {
        if (this.mCreateConnectionProcessor != null && !this.mCreateConnectionProcessor.isProcessingComplete()) {
            this.mCreateConnectionProcessor.abort();
            return;
        }
        if (this.mState == 0 || this.mState == 2 || this.mState == 1) {
            if (z) {
                Iterator<Listener> it = this.mListeners.iterator();
                while (it.hasNext()) {
                    if (it.next().onCanceledViaNewOutgoingCallBroadcast(this)) {
                        setLocallyDisconnecting(false);
                        return;
                    }
                }
            }
            handleCreateConnectionFailure(new DisconnectCause(4));
            return;
        }
        Log.v(this, "Cannot abort a call which isn't either PRE_DIAL_WAIT or CONNECTING", new Object[0]);
    }

    void answer(int i) {
        Preconditions.checkNotNull(this.mConnectionService);
        if (isRinging("answer")) {
            this.mConnectionService.answer(this, i);
        }
    }

    void reject(boolean z, String str) {
        Preconditions.checkNotNull(this.mConnectionService);
        if (isRinging("reject")) {
            this.mConnectionService.reject(this);
        }
    }

    void hold() {
        Preconditions.checkNotNull(this.mConnectionService);
        if (this.mState == 5) {
            this.mConnectionService.hold(this);
        }
    }

    void unhold() {
        Preconditions.checkNotNull(this.mConnectionService);
        if (this.mState == 6) {
            this.mConnectionService.unhold(this);
        }
    }

    boolean isAlive() {
        switch (this.mState) {
            case 0:
            case 4:
            case 7:
            case 8:
                return false;
            case 1:
            case 2:
            case 3:
            case 5:
            case 6:
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

    void setExtras(Bundle bundle) {
        this.mExtras = bundle;
    }

    Uri getContactUri() {
        return (this.mCallerInfo == null || !this.mCallerInfo.contactExists) ? getHandle() : ContactsContract.Contacts.getLookupUri(this.mCallerInfo.contactIdOrZero, this.mCallerInfo.lookupKey);
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
        this.mConnectionService.onPostDialContinue(this, z);
    }

    void conferenceWith(Call call) {
        if (this.mConnectionService == null) {
            Log.w(this, "conference requested on a call without a connection service.", new Object[0]);
        } else {
            this.mConnectionService.conference(this, call);
        }
    }

    void splitFromConference() {
        if (this.mConnectionService == null) {
            Log.w(this, "splitting from conference call without a connection service", new Object[0]);
        } else {
            this.mConnectionService.splitFromConference(this);
        }
    }

    void mergeConference() {
        if (this.mConnectionService == null) {
            Log.w(this, "merging conference calls without a connection service.", new Object[0]);
        } else if (can(4)) {
            this.mConnectionService.mergeConference(this);
            this.mWasConferencePreviouslyMerged = true;
        }
    }

    void swapConference() {
        if (this.mConnectionService == null) {
            Log.w(this, "swapping conference calls without a connection service.", new Object[0]);
        }
        if (can(8)) {
            this.mConnectionService.swapConference(this);
            switch (this.mChildCalls.size()) {
                case 1:
                    this.mConferenceLevelActiveCall = this.mChildCalls.get(0);
                    break;
                case 2:
                    this.mConferenceLevelActiveCall = this.mChildCalls.get(0) == this.mConferenceLevelActiveCall ? this.mChildCalls.get(1) : this.mChildCalls.get(0);
                    break;
                default:
                    this.mConferenceLevelActiveCall = null;
                    break;
            }
        }
    }

    void setParentCall(Call call) {
        if (call == this) {
            Log.e(this, new Exception(), "setting the parent to self", new Object[0]);
            return;
        }
        if (call != this.mParentCall) {
            Preconditions.checkState(call == null || this.mParentCall == null);
            Call call2 = this.mParentCall;
            if (this.mParentCall != null) {
                this.mParentCall.removeChildCall(this);
            }
            this.mParentCall = call;
            if (this.mParentCall != null) {
                this.mParentCall.addChildCall(this);
            }
            Iterator<Listener> it = this.mListeners.iterator();
            while (it.hasNext()) {
                it.next().onParentChanged(this);
            }
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

    List<Call> getConferenceableCalls() {
        return this.mConferenceableCalls;
    }

    boolean can(int i) {
        return (this.mConnectionCapabilities & i) == i;
    }

    private void addChildCall(Call call) {
        if (!this.mChildCalls.contains(call)) {
            this.mConferenceLevelActiveCall = call;
            this.mChildCalls.add(call);
            Iterator<Listener> it = this.mListeners.iterator();
            while (it.hasNext()) {
                it.next().onChildrenChanged(this);
            }
        }
    }

    private void removeChildCall(Call call) {
        if (this.mChildCalls.remove(call)) {
            Iterator<Listener> it = this.mListeners.iterator();
            while (it.hasNext()) {
                it.next().onChildrenChanged(this);
            }
        }
    }

    boolean isRespondViaSmsCapable() {
        return (this.mState != 4 || getHandle() == null || PhoneNumberUtils.isUriNumber(getHandle().toString()) || SmsApplication.getDefaultRespondViaMessageApplication(this.mContext, true) == null) ? false : true;
    }

    List<String> getCannedSmsResponses() {
        return this.mCannedSmsResponses;
    }

    private void fixParentAfterDisconnect() {
        setParentCall(null);
    }

    private boolean isRinging(String str) {
        if (this.mState == 4) {
            return true;
        }
        Log.i(this, "Request to %s a non-ringing call %s", str, this);
        return false;
    }

    private void decrementAssociatedCallCount(ServiceBinder serviceBinder) {
        if (serviceBinder != null) {
            serviceBinder.decrementAssociatedCallCount();
        }
    }

    private void startCallerInfoLookup() {
        String schemeSpecificPart = this.mHandle == null ? null : this.mHandle.getSchemeSpecificPart();
        this.mQueryToken++;
        this.mCallerInfo = null;
        if (!TextUtils.isEmpty(schemeSpecificPart)) {
            Log.v(this, "Looking up information for: %s.", Log.piiHandle(schemeSpecificPart));
            CallerInfoAsyncQuery.startQuery(this.mQueryToken, this.mContext, schemeSpecificPart, sCallerInfoQueryListener, this);
        }
    }

    private void setCallerInfo(CallerInfo callerInfo, int i) {
        Trace.beginSection("setCallerInfo");
        Preconditions.checkNotNull(callerInfo);
        if (this.mQueryToken == i) {
            this.mCallerInfo = callerInfo;
            Log.i(this, "CallerInfo received for %s: %s", Log.piiHandle(this.mHandle), callerInfo);
            if (this.mCallerInfo.contactDisplayPhotoUri != null) {
                Log.d(this, "Searching person uri %s for call %s", this.mCallerInfo.contactDisplayPhotoUri, this);
                ContactsAsyncHelper.startObtainPhotoAsync(i, this.mContext, this.mCallerInfo.contactDisplayPhotoUri, sPhotoLoadListener, this);
            } else {
                Iterator<Listener> it = this.mListeners.iterator();
                while (it.hasNext()) {
                    it.next().onCallerInfoChanged(this);
                }
            }
            processDirectToVoicemail();
        }
        Trace.endSection();
    }

    CallerInfo getCallerInfo() {
        return this.mCallerInfo;
    }

    private void setPhoto(Drawable drawable, Bitmap bitmap, int i) {
        if (this.mQueryToken == i) {
            this.mCallerInfo.cachedPhoto = drawable;
            this.mCallerInfo.cachedPhotoIcon = bitmap;
            Iterator<Listener> it = this.mListeners.iterator();
            while (it.hasNext()) {
                it.next().onCallerInfoChanged(this);
            }
        }
    }

    private void maybeLoadCannedSmsResponses() {
        if (this.mIsIncoming && isRespondViaSmsCapable() && !this.mCannedSmsResponsesLoadingStarted) {
            Log.d(this, "maybeLoadCannedSmsResponses: starting task to load messages", new Object[0]);
            this.mCannedSmsResponsesLoadingStarted = true;
            RespondViaSmsManager.getInstance().loadCannedTextMessages(new Response<Void, List<String>>() {
                public void onResult(Void r5, List<String>... listArr) {
                    if (listArr.length > 0) {
                        Log.d(this, "maybeLoadCannedSmsResponses: got %s", listArr[0]);
                        Call.this.mCannedSmsResponses = listArr[0];
                        Iterator it = Call.this.mListeners.iterator();
                        while (it.hasNext()) {
                            ((Listener) it.next()).onCannedSmsResponsesLoaded(Call.this);
                        }
                    }
                }

                public void onError(Void r6, int i, String str) {
                    Log.w(Call.this, "Error obtaining canned SMS responses: %d %s", Integer.valueOf(i), str);
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

    public void setVideoProvider(IVideoProvider iVideoProvider) {
        this.mVideoProvider = iVideoProvider;
        Iterator<Listener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onVideoCallProviderChanged(this);
        }
    }

    public IVideoProvider getVideoProvider() {
        return this.mVideoProvider;
    }

    public int getVideoState() {
        return this.mVideoState;
    }

    public int getVideoStateHistory() {
        return this.mVideoStateHistory;
    }

    public void setVideoState(int i) {
        this.mVideoStateHistory |= i;
        this.mVideoState = i;
        Iterator<Listener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onVideoStateChanged(this);
        }
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
        return this.mIsUnknown;
    }

    public void setIsUnknown(boolean z) {
        this.mIsUnknown = z;
    }

    public boolean isLocallyDisconnecting() {
        return this.mIsLocallyDisconnecting;
    }

    private void setLocallyDisconnecting(boolean z) {
        this.mIsLocallyDisconnecting = z;
    }

    static int getStateFromConnectionState(int i) {
        switch (i) {
            case 0:
                return 1;
            case 1:
                return 0;
            case 2:
                return 4;
            case 3:
                return 3;
            case 4:
                return 5;
            case 5:
                return 6;
            case 6:
            default:
                return 7;
        }
    }
}
