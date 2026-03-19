package android.telecom;

import android.annotation.SystemApi;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.telecom.InCallService;
import com.android.internal.telephony.IccCardConstants;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public final class Call {
    public static final String AVAILABLE_PHONE_ACCOUNTS = "selectPhoneAccountAccounts";
    public static final String EVENT_HANDOVER_COMPLETE = "android.telecom.event.HANDOVER_COMPLETE";
    public static final String EVENT_HANDOVER_FAILED = "android.telecom.event.HANDOVER_FAILED";
    public static final String EVENT_HANDOVER_SOURCE_DISCONNECTED = "android.telecom.event.HANDOVER_SOURCE_DISCONNECTED";
    public static final String EVENT_REQUEST_HANDOVER = "android.telecom.event.REQUEST_HANDOVER";
    public static final String EXTRA_HANDOVER_EXTRAS = "android.telecom.extra.HANDOVER_EXTRAS";
    public static final String EXTRA_HANDOVER_PHONE_ACCOUNT_HANDLE = "android.telecom.extra.HANDOVER_PHONE_ACCOUNT_HANDLE";
    public static final String EXTRA_HANDOVER_VIDEO_STATE = "android.telecom.extra.HANDOVER_VIDEO_STATE";
    public static final String EXTRA_LAST_EMERGENCY_CALLBACK_TIME_MILLIS = "android.telecom.extra.LAST_EMERGENCY_CALLBACK_TIME_MILLIS";
    public static final int STATE_ACTIVE = 4;
    public static final int STATE_CONNECTING = 9;
    public static final int STATE_DIALING = 1;
    public static final int STATE_DISCONNECTED = 7;
    public static final int STATE_DISCONNECTING = 10;
    public static final int STATE_HOLDING = 3;
    public static final int STATE_NEW = 0;

    @SystemApi
    @Deprecated
    public static final int STATE_PRE_DIAL_WAIT = 8;
    public static final int STATE_PULLING_CALL = 11;
    public static final int STATE_RINGING = 2;
    public static final int STATE_SELECT_PHONE_ACCOUNT = 8;
    private final List<CallbackRecord<Callback>> mCallbackRecords;
    private String mCallingPackage;
    private List<String> mCannedTextResponses;
    private final List<Call> mChildren;
    private boolean mChildrenCached;
    private final List<String> mChildrenIds;
    private final List<Call> mConferenceableCalls;
    private Details mDetails;
    private Bundle mExtras;
    private final InCallAdapter mInCallAdapter;
    private String mParentId;
    private final Phone mPhone;
    private String mRemainingPostDialSequence;
    private RttCall mRttCall;
    private int mState;
    private int mTargetSdkVersion;
    private final String mTelecomCallId;
    private final List<Call> mUnmodifiableChildren;
    private final List<Call> mUnmodifiableConferenceableCalls;
    private VideoCallImpl mVideoCallImpl;

    @SystemApi
    @Deprecated
    public static abstract class Listener extends Callback {
    }

    public static class Details {
        public static final int CAPABILITY_CANNOT_DOWNGRADE_VIDEO_TO_AUDIO = 4194304;
        public static final int CAPABILITY_CAN_PAUSE_VIDEO = 1048576;
        public static final int CAPABILITY_CAN_PULL_CALL = 8388608;
        public static final int CAPABILITY_CAN_SEND_RESPONSE_VIA_CONNECTION = 2097152;
        public static final int CAPABILITY_CAN_UPGRADE_TO_VIDEO = 524288;
        public static final int CAPABILITY_DISCONNECT_FROM_CONFERENCE = 8192;
        public static final int CAPABILITY_HOLD = 1;
        public static final int CAPABILITY_MANAGE_CONFERENCE = 128;
        public static final int CAPABILITY_MERGE_CONFERENCE = 4;
        public static final int CAPABILITY_MUTE = 64;
        public static final int CAPABILITY_RESPOND_VIA_TEXT = 32;
        public static final int CAPABILITY_SEPARATE_FROM_CONFERENCE = 4096;
        public static final int CAPABILITY_SPEED_UP_MT_AUDIO = 262144;
        public static final int CAPABILITY_SUPPORTS_VT_LOCAL_BIDIRECTIONAL = 768;
        public static final int CAPABILITY_SUPPORTS_VT_LOCAL_RX = 256;
        public static final int CAPABILITY_SUPPORTS_VT_LOCAL_TX = 512;
        public static final int CAPABILITY_SUPPORTS_VT_REMOTE_BIDIRECTIONAL = 3072;
        public static final int CAPABILITY_SUPPORTS_VT_REMOTE_RX = 1024;
        public static final int CAPABILITY_SUPPORTS_VT_REMOTE_TX = 2048;
        public static final int CAPABILITY_SUPPORT_DEFLECT = 16777216;
        public static final int CAPABILITY_SUPPORT_HOLD = 2;
        public static final int CAPABILITY_SWAP_CONFERENCE = 8;
        public static final int CAPABILITY_UNUSED_1 = 16;
        public static final int PROPERTY_ASSISTED_DIALING_USED = 512;
        public static final int PROPERTY_CONFERENCE = 1;
        public static final int PROPERTY_EMERGENCY_CALLBACK_MODE = 4;
        public static final int PROPERTY_ENTERPRISE_CALL = 32;
        public static final int PROPERTY_GENERIC_CONFERENCE = 2;
        public static final int PROPERTY_HAS_CDMA_VOICE_PRIVACY = 128;
        public static final int PROPERTY_HIGH_DEF_AUDIO = 16;
        public static final int PROPERTY_IS_EXTERNAL_CALL = 64;
        public static final int PROPERTY_RTT = 1024;
        public static final int PROPERTY_SELF_MANAGED = 256;
        public static final int PROPERTY_WIFI = 8;
        private final PhoneAccountHandle mAccountHandle;
        private final int mCallCapabilities;
        private final int mCallProperties;
        private final String mCallerDisplayName;
        private final int mCallerDisplayNamePresentation;
        private final long mConnectTimeMillis;
        private final long mCreationTimeMillis;
        private final DisconnectCause mDisconnectCause;
        private final Bundle mExtras;
        private final GatewayInfo mGatewayInfo;
        private final Uri mHandle;
        private final int mHandlePresentation;
        private final Bundle mIntentExtras;
        private final StatusHints mStatusHints;
        private final int mSupportedAudioRoutes = 15;
        private final String mTelecomCallId;
        private final int mVideoState;

        public static boolean can(int i, int i2) {
            return (i & i2) == i2;
        }

        public boolean can(int i) {
            return can(this.mCallCapabilities, i);
        }

        public static String capabilitiesToString(int i) {
            StringBuilder sb = new StringBuilder();
            sb.append("[Capabilities:");
            if (can(i, 1)) {
                sb.append(" CAPABILITY_HOLD");
            }
            if (can(i, 2)) {
                sb.append(" CAPABILITY_SUPPORT_HOLD");
            }
            if (can(i, 4)) {
                sb.append(" CAPABILITY_MERGE_CONFERENCE");
            }
            if (can(i, 8)) {
                sb.append(" CAPABILITY_SWAP_CONFERENCE");
            }
            if (can(i, 32)) {
                sb.append(" CAPABILITY_RESPOND_VIA_TEXT");
            }
            if (can(i, 64)) {
                sb.append(" CAPABILITY_MUTE");
            }
            if (can(i, 128)) {
                sb.append(" CAPABILITY_MANAGE_CONFERENCE");
            }
            if (can(i, 256)) {
                sb.append(" CAPABILITY_SUPPORTS_VT_LOCAL_RX");
            }
            if (can(i, 512)) {
                sb.append(" CAPABILITY_SUPPORTS_VT_LOCAL_TX");
            }
            if (can(i, 768)) {
                sb.append(" CAPABILITY_SUPPORTS_VT_LOCAL_BIDIRECTIONAL");
            }
            if (can(i, 1024)) {
                sb.append(" CAPABILITY_SUPPORTS_VT_REMOTE_RX");
            }
            if (can(i, 2048)) {
                sb.append(" CAPABILITY_SUPPORTS_VT_REMOTE_TX");
            }
            if (can(i, 4194304)) {
                sb.append(" CAPABILITY_CANNOT_DOWNGRADE_VIDEO_TO_AUDIO");
            }
            if (can(i, 3072)) {
                sb.append(" CAPABILITY_SUPPORTS_VT_REMOTE_BIDIRECTIONAL");
            }
            if (can(i, 262144)) {
                sb.append(" CAPABILITY_SPEED_UP_MT_AUDIO");
            }
            if (can(i, 524288)) {
                sb.append(" CAPABILITY_CAN_UPGRADE_TO_VIDEO");
            }
            if (can(i, 1048576)) {
                sb.append(" CAPABILITY_CAN_PAUSE_VIDEO");
            }
            if (can(i, 8388608)) {
                sb.append(" CAPABILITY_CAN_PULL_CALL");
            }
            if (can(i, 16777216)) {
                sb.append(" CAPABILITY_SUPPORT_DEFLECT");
            }
            sb.append("]");
            return sb.toString();
        }

        public static boolean hasProperty(int i, int i2) {
            return (i & i2) == i2;
        }

        public boolean hasProperty(int i) {
            return hasProperty(this.mCallProperties, i);
        }

        public static String propertiesToString(int i) {
            StringBuilder sb = new StringBuilder();
            sb.append("[Properties:");
            if (hasProperty(i, 1)) {
                sb.append(" PROPERTY_CONFERENCE");
            }
            if (hasProperty(i, 2)) {
                sb.append(" PROPERTY_GENERIC_CONFERENCE");
            }
            if (hasProperty(i, 8)) {
                sb.append(" PROPERTY_WIFI");
            }
            if (hasProperty(i, 16)) {
                sb.append(" PROPERTY_HIGH_DEF_AUDIO");
            }
            if (hasProperty(i, 4)) {
                sb.append(" PROPERTY_EMERGENCY_CALLBACK_MODE");
            }
            if (hasProperty(i, 64)) {
                sb.append(" PROPERTY_IS_EXTERNAL_CALL");
            }
            if (hasProperty(i, 128)) {
                sb.append(" PROPERTY_HAS_CDMA_VOICE_PRIVACY");
            }
            if (hasProperty(i, 512)) {
                sb.append(" PROPERTY_ASSISTED_DIALING_USED");
            }
            sb.append("]");
            return sb.toString();
        }

        public String getTelecomCallId() {
            return this.mTelecomCallId;
        }

        public Uri getHandle() {
            return this.mHandle;
        }

        public int getHandlePresentation() {
            return this.mHandlePresentation;
        }

        public String getCallerDisplayName() {
            return this.mCallerDisplayName;
        }

        public int getCallerDisplayNamePresentation() {
            return this.mCallerDisplayNamePresentation;
        }

        public PhoneAccountHandle getAccountHandle() {
            return this.mAccountHandle;
        }

        public int getCallCapabilities() {
            return this.mCallCapabilities;
        }

        public int getCallProperties() {
            return this.mCallProperties;
        }

        public int getSupportedAudioRoutes() {
            return 15;
        }

        public DisconnectCause getDisconnectCause() {
            return this.mDisconnectCause;
        }

        public final long getConnectTimeMillis() {
            return this.mConnectTimeMillis;
        }

        public GatewayInfo getGatewayInfo() {
            return this.mGatewayInfo;
        }

        public int getVideoState() {
            return this.mVideoState;
        }

        public StatusHints getStatusHints() {
            return this.mStatusHints;
        }

        public Bundle getExtras() {
            return this.mExtras;
        }

        public Bundle getIntentExtras() {
            return this.mIntentExtras;
        }

        public long getCreationTimeMillis() {
            return this.mCreationTimeMillis;
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof Details)) {
                return false;
            }
            Details details = (Details) obj;
            return Objects.equals(this.mHandle, details.mHandle) && Objects.equals(Integer.valueOf(this.mHandlePresentation), Integer.valueOf(details.mHandlePresentation)) && Objects.equals(this.mCallerDisplayName, details.mCallerDisplayName) && Objects.equals(Integer.valueOf(this.mCallerDisplayNamePresentation), Integer.valueOf(details.mCallerDisplayNamePresentation)) && Objects.equals(this.mAccountHandle, details.mAccountHandle) && Objects.equals(Integer.valueOf(this.mCallCapabilities), Integer.valueOf(details.mCallCapabilities)) && Objects.equals(Integer.valueOf(this.mCallProperties), Integer.valueOf(details.mCallProperties)) && Objects.equals(this.mDisconnectCause, details.mDisconnectCause) && Objects.equals(Long.valueOf(this.mConnectTimeMillis), Long.valueOf(details.mConnectTimeMillis)) && Objects.equals(this.mGatewayInfo, details.mGatewayInfo) && Objects.equals(Integer.valueOf(this.mVideoState), Integer.valueOf(details.mVideoState)) && Objects.equals(this.mStatusHints, details.mStatusHints) && Call.areBundlesEqual(this.mExtras, details.mExtras) && Call.areBundlesEqual(this.mIntentExtras, details.mIntentExtras) && Objects.equals(Long.valueOf(this.mCreationTimeMillis), Long.valueOf(details.mCreationTimeMillis));
        }

        public int hashCode() {
            return Objects.hash(this.mHandle, Integer.valueOf(this.mHandlePresentation), this.mCallerDisplayName, Integer.valueOf(this.mCallerDisplayNamePresentation), this.mAccountHandle, Integer.valueOf(this.mCallCapabilities), Integer.valueOf(this.mCallProperties), this.mDisconnectCause, Long.valueOf(this.mConnectTimeMillis), this.mGatewayInfo, Integer.valueOf(this.mVideoState), this.mStatusHints, this.mExtras, this.mIntentExtras, Long.valueOf(this.mCreationTimeMillis));
        }

        public Details(String str, Uri uri, int i, String str2, int i2, PhoneAccountHandle phoneAccountHandle, int i3, int i4, DisconnectCause disconnectCause, long j, GatewayInfo gatewayInfo, int i5, StatusHints statusHints, Bundle bundle, Bundle bundle2, long j2) {
            this.mTelecomCallId = str;
            this.mHandle = uri;
            this.mHandlePresentation = i;
            this.mCallerDisplayName = str2;
            this.mCallerDisplayNamePresentation = i2;
            this.mAccountHandle = phoneAccountHandle;
            this.mCallCapabilities = i3;
            this.mCallProperties = i4;
            this.mDisconnectCause = disconnectCause;
            this.mConnectTimeMillis = j;
            this.mGatewayInfo = gatewayInfo;
            this.mVideoState = i5;
            this.mStatusHints = statusHints;
            this.mExtras = bundle;
            this.mIntentExtras = bundle2;
            this.mCreationTimeMillis = j2;
        }

        public static Details createFromParcelableCall(ParcelableCall parcelableCall) {
            return new Details(parcelableCall.getId(), parcelableCall.getHandle(), parcelableCall.getHandlePresentation(), parcelableCall.getCallerDisplayName(), parcelableCall.getCallerDisplayNamePresentation(), parcelableCall.getAccountHandle(), parcelableCall.getCapabilities(), parcelableCall.getProperties(), parcelableCall.getDisconnectCause(), parcelableCall.getConnectTimeMillis(), parcelableCall.getGatewayInfo(), parcelableCall.getVideoState(), parcelableCall.getStatusHints(), parcelableCall.getExtras(), parcelableCall.getIntentExtras(), parcelableCall.getCreationTimeMillis());
        }

        public String toString() {
            return "[pa: " + this.mAccountHandle + ", hdl: " + Log.pii(this.mHandle) + ", caps: " + capabilitiesToString(this.mCallCapabilities) + ", props: " + propertiesToString(this.mCallProperties) + "]";
        }
    }

    public static abstract class Callback {
        public static final int HANDOVER_FAILURE_DEST_APP_REJECTED = 1;
        public static final int HANDOVER_FAILURE_NOT_SUPPORTED = 2;
        public static final int HANDOVER_FAILURE_ONGOING_EMERGENCY_CALL = 4;
        public static final int HANDOVER_FAILURE_UNKNOWN = 5;
        public static final int HANDOVER_FAILURE_USER_REJECTED = 3;

        @Retention(RetentionPolicy.SOURCE)
        public @interface HandoverFailureErrors {
        }

        public void onStateChanged(Call call, int i) {
        }

        public void onParentChanged(Call call, Call call2) {
        }

        public void onChildrenChanged(Call call, List<Call> list) {
        }

        public void onDetailsChanged(Call call, Details details) {
        }

        public void onCannedTextResponsesLoaded(Call call, List<String> list) {
        }

        public void onPostDialWait(Call call, String str) {
        }

        public void onVideoCallChanged(Call call, InCallService.VideoCall videoCall) {
        }

        public void onCallDestroyed(Call call) {
        }

        public void onConferenceableCallsChanged(Call call, List<Call> list) {
        }

        public void onConnectionEvent(Call call, String str, Bundle bundle) {
        }

        public void onRttModeChanged(Call call, int i) {
        }

        public void onRttStatusChanged(Call call, boolean z, RttCall rttCall) {
        }

        public void onRttRequest(Call call, int i) {
        }

        public void onRttInitiationFailure(Call call, int i) {
        }

        public void onHandoverComplete(Call call) {
        }

        public void onHandoverFailed(Call call, int i) {
        }
    }

    public static final class RttCall {
        private static final int READ_BUFFER_SIZE = 1000;
        public static final int RTT_MODE_FULL = 1;
        public static final int RTT_MODE_HCO = 2;
        public static final int RTT_MODE_INVALID = 0;
        public static final int RTT_MODE_VCO = 3;
        private final InCallAdapter mInCallAdapter;
        private char[] mReadBuffer = new char[1000];
        private InputStreamReader mReceiveStream;
        private int mRttMode;
        private final String mTelecomCallId;
        private OutputStreamWriter mTransmitStream;

        @Retention(RetentionPolicy.SOURCE)
        public @interface RttAudioMode {
        }

        public RttCall(String str, InputStreamReader inputStreamReader, OutputStreamWriter outputStreamWriter, int i, InCallAdapter inCallAdapter) {
            this.mTelecomCallId = str;
            this.mReceiveStream = inputStreamReader;
            this.mTransmitStream = outputStreamWriter;
            this.mRttMode = i;
            this.mInCallAdapter = inCallAdapter;
        }

        public int getRttAudioMode() {
            return this.mRttMode;
        }

        public void setRttMode(int i) {
            this.mInCallAdapter.setRttMode(this.mTelecomCallId, i);
        }

        public void write(String str) throws IOException {
            this.mTransmitStream.write(str);
            this.mTransmitStream.flush();
        }

        public String read() {
            try {
                int i = this.mReceiveStream.read(this.mReadBuffer, 0, 1000);
                if (i < 0) {
                    return null;
                }
                return new String(this.mReadBuffer, 0, i);
            } catch (IOException e) {
                Log.w(this, "Exception encountered when reading from InputStreamReader: %s", e);
                return null;
            }
        }

        public String readImmediately() throws IOException {
            int i;
            if (!this.mReceiveStream.ready() || (i = this.mReceiveStream.read(this.mReadBuffer, 0, 1000)) < 0) {
                return null;
            }
            return new String(this.mReadBuffer, 0, i);
        }

        public void close() {
            try {
                this.mReceiveStream.close();
            } catch (IOException e) {
            }
            try {
                this.mTransmitStream.close();
            } catch (IOException e2) {
            }
        }
    }

    public String getRemainingPostDialSequence() {
        return this.mRemainingPostDialSequence;
    }

    public void answer(int i) {
        this.mInCallAdapter.answerCall(this.mTelecomCallId, i);
    }

    public void deflect(Uri uri) {
        this.mInCallAdapter.deflectCall(this.mTelecomCallId, uri);
    }

    public void reject(boolean z, String str) {
        this.mInCallAdapter.rejectCall(this.mTelecomCallId, z, str);
    }

    public void disconnect() {
        this.mInCallAdapter.disconnectCall(this.mTelecomCallId);
    }

    public void hold() {
        this.mInCallAdapter.holdCall(this.mTelecomCallId);
    }

    public void unhold() {
        this.mInCallAdapter.unholdCall(this.mTelecomCallId);
    }

    public void playDtmfTone(char c) {
        this.mInCallAdapter.playDtmfTone(this.mTelecomCallId, c);
    }

    public void stopDtmfTone() {
        this.mInCallAdapter.stopDtmfTone(this.mTelecomCallId);
    }

    public void postDialContinue(boolean z) {
        this.mInCallAdapter.postDialContinue(this.mTelecomCallId, z);
    }

    public void phoneAccountSelected(PhoneAccountHandle phoneAccountHandle, boolean z) {
        this.mInCallAdapter.phoneAccountSelected(this.mTelecomCallId, phoneAccountHandle, z);
    }

    public void conference(Call call) {
        if (call != null) {
            this.mInCallAdapter.conference(this.mTelecomCallId, call.mTelecomCallId);
        }
    }

    public void splitFromConference() {
        this.mInCallAdapter.splitFromConference(this.mTelecomCallId);
    }

    public void mergeConference() {
        this.mInCallAdapter.mergeConference(this.mTelecomCallId);
    }

    public void swapConference() {
        this.mInCallAdapter.swapConference(this.mTelecomCallId);
    }

    public void pullExternalCall() {
        if (!this.mDetails.hasProperty(64)) {
            return;
        }
        this.mInCallAdapter.pullExternalCall(this.mTelecomCallId);
    }

    public void sendCallEvent(String str, Bundle bundle) {
        this.mInCallAdapter.sendCallEvent(this.mTelecomCallId, str, this.mTargetSdkVersion, bundle);
    }

    public void sendRttRequest() {
        this.mInCallAdapter.sendRttRequest(this.mTelecomCallId);
    }

    public void respondToRttRequest(int i, boolean z) {
        this.mInCallAdapter.respondToRttRequest(this.mTelecomCallId, i, z);
    }

    public void handoverTo(PhoneAccountHandle phoneAccountHandle, int i, Bundle bundle) {
        this.mInCallAdapter.handoverTo(this.mTelecomCallId, phoneAccountHandle, i, bundle);
    }

    public void stopRtt() {
        this.mInCallAdapter.stopRtt(this.mTelecomCallId);
    }

    public final void putExtras(Bundle bundle) {
        if (bundle == null) {
            return;
        }
        if (this.mExtras == null) {
            this.mExtras = new Bundle();
        }
        this.mExtras.putAll(bundle);
        this.mInCallAdapter.putExtras(this.mTelecomCallId, bundle);
    }

    public final void putExtra(String str, boolean z) {
        if (this.mExtras == null) {
            this.mExtras = new Bundle();
        }
        this.mExtras.putBoolean(str, z);
        this.mInCallAdapter.putExtra(this.mTelecomCallId, str, z);
    }

    public final void putExtra(String str, int i) {
        if (this.mExtras == null) {
            this.mExtras = new Bundle();
        }
        this.mExtras.putInt(str, i);
        this.mInCallAdapter.putExtra(this.mTelecomCallId, str, i);
    }

    public final void putExtra(String str, String str2) {
        if (this.mExtras == null) {
            this.mExtras = new Bundle();
        }
        this.mExtras.putString(str, str2);
        this.mInCallAdapter.putExtra(this.mTelecomCallId, str, str2);
    }

    public final void removeExtras(List<String> list) {
        if (this.mExtras != null) {
            Iterator<String> it = list.iterator();
            while (it.hasNext()) {
                this.mExtras.remove(it.next());
            }
            if (this.mExtras.size() == 0) {
                this.mExtras = null;
            }
        }
        this.mInCallAdapter.removeExtras(this.mTelecomCallId, list);
    }

    public final void removeExtras(String... strArr) {
        removeExtras(Arrays.asList(strArr));
    }

    public Call getParent() {
        if (this.mParentId != null) {
            return this.mPhone.internalGetCallByTelecomId(this.mParentId);
        }
        return null;
    }

    public List<Call> getChildren() {
        if (!this.mChildrenCached) {
            this.mChildrenCached = true;
            this.mChildren.clear();
            Iterator<String> it = this.mChildrenIds.iterator();
            while (it.hasNext()) {
                Call callInternalGetCallByTelecomId = this.mPhone.internalGetCallByTelecomId(it.next());
                if (callInternalGetCallByTelecomId == null) {
                    this.mChildrenCached = false;
                } else {
                    this.mChildren.add(callInternalGetCallByTelecomId);
                }
            }
        }
        return this.mUnmodifiableChildren;
    }

    public List<Call> getConferenceableCalls() {
        return this.mUnmodifiableConferenceableCalls;
    }

    public int getState() {
        return this.mState;
    }

    public List<String> getCannedTextResponses() {
        return this.mCannedTextResponses;
    }

    public InCallService.VideoCall getVideoCall() {
        return this.mVideoCallImpl;
    }

    public Details getDetails() {
        return this.mDetails;
    }

    public RttCall getRttCall() {
        return this.mRttCall;
    }

    public boolean isRttActive() {
        return this.mRttCall != null && this.mDetails.hasProperty(1024);
    }

    public void registerCallback(Callback callback) {
        registerCallback(callback, new Handler());
    }

    public void registerCallback(Callback callback, Handler handler) {
        unregisterCallback(callback);
        if (callback != null && handler != null && this.mState != 7) {
            this.mCallbackRecords.add(new CallbackRecord<>(callback, handler));
        }
    }

    public void unregisterCallback(Callback callback) {
        if (callback != null && this.mState != 7) {
            for (CallbackRecord<Callback> callbackRecord : this.mCallbackRecords) {
                if (callbackRecord.getCallback() == callback) {
                    this.mCallbackRecords.remove(callbackRecord);
                    return;
                }
            }
        }
    }

    public String toString() {
        return "Call [id: " + this.mTelecomCallId + ", state: " + stateToString(this.mState) + ", details: " + this.mDetails + "]";
    }

    private static String stateToString(int i) {
        switch (i) {
            case 0:
                return "NEW";
            case 1:
                return "DIALING";
            case 2:
                return "RINGING";
            case 3:
                return "HOLDING";
            case 4:
                return "ACTIVE";
            case 5:
            case 6:
            default:
                Log.w(Call.class, "Unknown state %d", Integer.valueOf(i));
                return IccCardConstants.INTENT_VALUE_ICC_UNKNOWN;
            case 7:
                return "DISCONNECTED";
            case 8:
                return "SELECT_PHONE_ACCOUNT";
            case 9:
                return "CONNECTING";
            case 10:
                return "DISCONNECTING";
        }
    }

    @SystemApi
    @Deprecated
    public void addListener(Listener listener) {
        registerCallback(listener);
    }

    @SystemApi
    @Deprecated
    public void removeListener(Listener listener) {
        unregisterCallback(listener);
    }

    Call(Phone phone, String str, InCallAdapter inCallAdapter, String str2, int i) {
        this.mChildrenIds = new ArrayList();
        this.mChildren = new ArrayList();
        this.mUnmodifiableChildren = Collections.unmodifiableList(this.mChildren);
        this.mCallbackRecords = new CopyOnWriteArrayList();
        this.mConferenceableCalls = new ArrayList();
        this.mUnmodifiableConferenceableCalls = Collections.unmodifiableList(this.mConferenceableCalls);
        this.mParentId = null;
        this.mCannedTextResponses = null;
        this.mPhone = phone;
        this.mTelecomCallId = str;
        this.mInCallAdapter = inCallAdapter;
        this.mState = 0;
        this.mCallingPackage = str2;
        this.mTargetSdkVersion = i;
    }

    Call(Phone phone, String str, InCallAdapter inCallAdapter, int i, String str2, int i2) {
        this.mChildrenIds = new ArrayList();
        this.mChildren = new ArrayList();
        this.mUnmodifiableChildren = Collections.unmodifiableList(this.mChildren);
        this.mCallbackRecords = new CopyOnWriteArrayList();
        this.mConferenceableCalls = new ArrayList();
        this.mUnmodifiableConferenceableCalls = Collections.unmodifiableList(this.mConferenceableCalls);
        this.mParentId = null;
        this.mCannedTextResponses = null;
        this.mPhone = phone;
        this.mTelecomCallId = str;
        this.mInCallAdapter = inCallAdapter;
        this.mState = i;
        this.mCallingPackage = str2;
        this.mTargetSdkVersion = i2;
    }

    final String internalGetCallId() {
        return this.mTelecomCallId;
    }

    final void internalUpdate(ParcelableCall parcelableCall, Map<String, Call> map) {
        boolean z;
        boolean z2;
        boolean z3;
        Details detailsCreateFromParcelableCall = Details.createFromParcelableCall(parcelableCall);
        boolean z4 = !Objects.equals(this.mDetails, detailsCreateFromParcelableCall);
        if (z4) {
            this.mDetails = detailsCreateFromParcelableCall;
        }
        if (this.mCannedTextResponses != null || parcelableCall.getCannedSmsResponses() == null || parcelableCall.getCannedSmsResponses().isEmpty()) {
            z = false;
        } else {
            this.mCannedTextResponses = Collections.unmodifiableList(parcelableCall.getCannedSmsResponses());
            z = true;
        }
        VideoCallImpl videoCallImpl = parcelableCall.getVideoCallImpl(this.mCallingPackage, this.mTargetSdkVersion);
        boolean z5 = parcelableCall.isVideoCallProviderChanged() && !Objects.equals(this.mVideoCallImpl, videoCallImpl);
        if (z5) {
            this.mVideoCallImpl = videoCallImpl;
        }
        if (this.mVideoCallImpl != null) {
            this.mVideoCallImpl.setVideoState(getDetails().getVideoState());
        }
        int state = parcelableCall.getState();
        boolean z6 = this.mState != state;
        if (z6) {
            this.mState = state;
        }
        String parentCallId = parcelableCall.getParentCallId();
        boolean z7 = !Objects.equals(this.mParentId, parentCallId);
        if (z7) {
            this.mParentId = parentCallId;
        }
        boolean z8 = !Objects.equals(parcelableCall.getChildCallIds(), this.mChildrenIds);
        if (z8) {
            this.mChildrenIds.clear();
            this.mChildrenIds.addAll(parcelableCall.getChildCallIds());
            this.mChildrenCached = false;
        }
        List<String> conferenceableCallIds = parcelableCall.getConferenceableCallIds();
        ArrayList arrayList = new ArrayList(conferenceableCallIds.size());
        for (String str : conferenceableCallIds) {
            if (map.containsKey(str)) {
                arrayList.add(map.get(str));
            }
        }
        if (!Objects.equals(this.mConferenceableCalls, arrayList)) {
            this.mConferenceableCalls.clear();
            this.mConferenceableCalls.addAll(arrayList);
            fireConferenceableCallsChanged();
        }
        if (parcelableCall.getIsRttCallChanged() && this.mDetails.hasProperty(1024)) {
            ParcelableRttCall parcelableRttCall = parcelableCall.getParcelableRttCall();
            RttCall rttCall = new RttCall(this.mTelecomCallId, new InputStreamReader(new ParcelFileDescriptor.AutoCloseInputStream(parcelableRttCall.getReceiveStream()), StandardCharsets.UTF_8), new OutputStreamWriter(new ParcelFileDescriptor.AutoCloseOutputStream(parcelableRttCall.getTransmitStream()), StandardCharsets.UTF_8), parcelableRttCall.getRttMode(), this.mInCallAdapter);
            if (this.mRttCall != null) {
                if (this.mRttCall.getRttAudioMode() != rttCall.getRttAudioMode()) {
                    z3 = true;
                    z2 = false;
                } else {
                    z2 = false;
                    z3 = false;
                }
            } else {
                z2 = true;
                z3 = false;
            }
            this.mRttCall = rttCall;
        } else if (this.mRttCall != null && parcelableCall.getParcelableRttCall() == null && parcelableCall.getIsRttCallChanged()) {
            this.mRttCall = null;
            z2 = true;
            z3 = false;
        } else {
            z2 = false;
            z3 = false;
        }
        if (z6) {
            fireStateChanged(this.mState);
        }
        if (z4) {
            fireDetailsChanged(this.mDetails);
        }
        if (z) {
            fireCannedTextResponsesLoaded(this.mCannedTextResponses);
        }
        if (z5) {
            fireVideoCallChanged(this.mVideoCallImpl);
        }
        if (z7) {
            fireParentChanged(getParent());
        }
        if (z8) {
            fireChildrenChanged(getChildren());
        }
        if (z2) {
            fireOnIsRttChanged(this.mRttCall != null, this.mRttCall);
        }
        if (z3) {
            fireOnRttModeChanged(this.mRttCall.getRttAudioMode());
        }
        if (this.mState == 7) {
            fireCallDestroyed();
        }
    }

    final void internalSetPostDialWait(String str) {
        this.mRemainingPostDialSequence = str;
        firePostDialWait(this.mRemainingPostDialSequence);
    }

    final void internalSetDisconnected() {
        if (this.mState != 7) {
            this.mState = 7;
            fireStateChanged(this.mState);
            fireCallDestroyed();
        }
    }

    final void internalOnConnectionEvent(String str, Bundle bundle) {
        fireOnConnectionEvent(str, bundle);
    }

    final void internalOnRttUpgradeRequest(final int i) {
        for (CallbackRecord<Callback> callbackRecord : this.mCallbackRecords) {
            final Callback callback = callbackRecord.getCallback();
            callbackRecord.getHandler().post(new Runnable() {
                @Override
                public final void run() {
                    callback.onRttRequest(this, i);
                }
            });
        }
    }

    final void internalOnRttInitiationFailure(final int i) {
        for (CallbackRecord<Callback> callbackRecord : this.mCallbackRecords) {
            final Callback callback = callbackRecord.getCallback();
            callbackRecord.getHandler().post(new Runnable() {
                @Override
                public final void run() {
                    callback.onRttInitiationFailure(this, i);
                }
            });
        }
    }

    final void internalOnHandoverFailed(final int i) {
        for (CallbackRecord<Callback> callbackRecord : this.mCallbackRecords) {
            final Callback callback = callbackRecord.getCallback();
            callbackRecord.getHandler().post(new Runnable() {
                @Override
                public final void run() {
                    callback.onHandoverFailed(this, i);
                }
            });
        }
    }

    final void internalOnHandoverComplete() {
        for (CallbackRecord<Callback> callbackRecord : this.mCallbackRecords) {
            final Callback callback = callbackRecord.getCallback();
            callbackRecord.getHandler().post(new Runnable() {
                @Override
                public final void run() {
                    callback.onHandoverComplete(this);
                }
            });
        }
    }

    private void fireStateChanged(final int i) {
        for (CallbackRecord<Callback> callbackRecord : this.mCallbackRecords) {
            final Callback callback = callbackRecord.getCallback();
            callbackRecord.getHandler().post(new Runnable() {
                @Override
                public void run() {
                    callback.onStateChanged(this, i);
                }
            });
        }
    }

    private void fireParentChanged(final Call call) {
        for (CallbackRecord<Callback> callbackRecord : this.mCallbackRecords) {
            final Callback callback = callbackRecord.getCallback();
            callbackRecord.getHandler().post(new Runnable() {
                @Override
                public void run() {
                    callback.onParentChanged(this, call);
                }
            });
        }
    }

    private void fireChildrenChanged(final List<Call> list) {
        for (CallbackRecord<Callback> callbackRecord : this.mCallbackRecords) {
            final Callback callback = callbackRecord.getCallback();
            callbackRecord.getHandler().post(new Runnable() {
                @Override
                public void run() {
                    callback.onChildrenChanged(this, list);
                }
            });
        }
    }

    private void fireDetailsChanged(final Details details) {
        for (CallbackRecord<Callback> callbackRecord : this.mCallbackRecords) {
            final Callback callback = callbackRecord.getCallback();
            callbackRecord.getHandler().post(new Runnable() {
                @Override
                public void run() {
                    callback.onDetailsChanged(this, details);
                }
            });
        }
    }

    private void fireCannedTextResponsesLoaded(final List<String> list) {
        for (CallbackRecord<Callback> callbackRecord : this.mCallbackRecords) {
            final Callback callback = callbackRecord.getCallback();
            callbackRecord.getHandler().post(new Runnable() {
                @Override
                public void run() {
                    callback.onCannedTextResponsesLoaded(this, list);
                }
            });
        }
    }

    private void fireVideoCallChanged(final InCallService.VideoCall videoCall) {
        for (CallbackRecord<Callback> callbackRecord : this.mCallbackRecords) {
            final Callback callback = callbackRecord.getCallback();
            callbackRecord.getHandler().post(new Runnable() {
                @Override
                public void run() {
                    callback.onVideoCallChanged(this, videoCall);
                }
            });
        }
    }

    private void firePostDialWait(final String str) {
        for (CallbackRecord<Callback> callbackRecord : this.mCallbackRecords) {
            final Callback callback = callbackRecord.getCallback();
            callbackRecord.getHandler().post(new Runnable() {
                @Override
                public void run() {
                    callback.onPostDialWait(this, str);
                }
            });
        }
    }

    private void fireCallDestroyed() {
        if (this.mCallbackRecords.isEmpty()) {
            this.mPhone.internalRemoveCall(this);
        }
        for (final CallbackRecord<Callback> callbackRecord : this.mCallbackRecords) {
            final Callback callback = callbackRecord.getCallback();
            callbackRecord.getHandler().post(new Runnable() {
                @Override
                public void run() {
                    boolean z;
                    try {
                        callback.onCallDestroyed(this);
                        e = null;
                    } catch (RuntimeException e) {
                        e = e;
                    }
                    synchronized (Call.this) {
                        Call.this.mCallbackRecords.remove(callbackRecord);
                        if (Call.this.mCallbackRecords.isEmpty()) {
                            z = true;
                        } else {
                            z = false;
                        }
                    }
                    if (z) {
                        Call.this.mPhone.internalRemoveCall(this);
                    }
                    if (e != null) {
                        throw e;
                    }
                }
            });
        }
    }

    private void fireConferenceableCallsChanged() {
        for (CallbackRecord<Callback> callbackRecord : this.mCallbackRecords) {
            final Callback callback = callbackRecord.getCallback();
            callbackRecord.getHandler().post(new Runnable() {
                @Override
                public void run() {
                    callback.onConferenceableCallsChanged(this, Call.this.mUnmodifiableConferenceableCalls);
                }
            });
        }
    }

    private void fireOnConnectionEvent(final String str, final Bundle bundle) {
        for (CallbackRecord<Callback> callbackRecord : this.mCallbackRecords) {
            final Callback callback = callbackRecord.getCallback();
            callbackRecord.getHandler().post(new Runnable() {
                @Override
                public void run() {
                    callback.onConnectionEvent(this, str, bundle);
                }
            });
        }
    }

    private void fireOnIsRttChanged(final boolean z, final RttCall rttCall) {
        for (CallbackRecord<Callback> callbackRecord : this.mCallbackRecords) {
            final Callback callback = callbackRecord.getCallback();
            callbackRecord.getHandler().post(new Runnable() {
                @Override
                public final void run() {
                    callback.onRttStatusChanged(this, z, rttCall);
                }
            });
        }
    }

    private void fireOnRttModeChanged(final int i) {
        for (CallbackRecord<Callback> callbackRecord : this.mCallbackRecords) {
            final Callback callback = callbackRecord.getCallback();
            callbackRecord.getHandler().post(new Runnable() {
                @Override
                public final void run() {
                    callback.onRttModeChanged(this, i);
                }
            });
        }
    }

    private static boolean areBundlesEqual(Bundle bundle, Bundle bundle2) {
        if (bundle == null || bundle2 == null) {
            return bundle == bundle2;
        }
        if (bundle.size() != bundle2.size()) {
            return false;
        }
        for (String str : bundle.keySet()) {
            if (str != null && !Objects.equals(bundle.get(str), bundle2.get(str))) {
                return false;
            }
        }
        return true;
    }
}
