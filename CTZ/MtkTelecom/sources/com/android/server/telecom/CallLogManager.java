package com.android.server.telecom;

import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.location.Country;
import android.location.CountryDetector;
import android.location.CountryListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.PersistableBundle;
import android.os.UserHandle;
import android.telecom.Log;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.VideoProfile;
import android.telephony.CarrierConfigManager;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.widget.Toast;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.CallerInfo;
import com.android.server.telecom.MissedCallNotifier;
import com.mediatek.provider.MtkCallLog;
import com.mediatek.server.telecom.ext.ExtensionManager;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;

@VisibleForTesting
public final class CallLogManager extends CallsManagerListenerBase {
    private static final String TAG = CallLogManager.class.getSimpleName();
    private final Context mContext;
    private String mCurrentCountryIso;
    private final MissedCallNotifier mMissedCallNotifier;
    private final PhoneAccountRegistrar mPhoneAccountRegistrar;
    private int mCurrentUserId = 0;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private HashMap<Long, Long> mConferenceCallLogIdMap = new HashMap<>();
    private HashMap<Long, HashMap<String, Uri>> mSrvccConferenceCallLogs = new HashMap<>();
    private BroadcastReceiver mUserSwitchReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int intExtra = intent.getIntExtra("android.intent.extra.user_handle", -1);
            Log.i(CallLogManager.TAG, "Monitor a user switch event. New user id = " + intExtra, new Object[0]);
            if (intExtra != -1) {
                Log.i(CallLogManager.TAG, "user switch from " + CallLogManager.this.mCurrentUserId + " to " + intExtra, new Object[0]);
                CallLogManager.this.mCurrentUserId = intExtra;
            }
        }
    };
    private Object mLock = new Object();

    public interface LogCallCompletedListener {
        void onLogCompleted(Uri uri);
    }

    private static class AddCallArgs {
        public final PhoneAccountHandle accountHandle;
        public final int callType;
        public final CallerInfo callerInfo;
        public final long conferenceCallLogId;
        public final int conferenceDurationInSec;
        public final Context context;
        public final Long dataUsage;
        public final int durationInSec;
        public final int features;
        public final UserHandle initiatingUser;
        public final boolean isRead;
        public final LogCallCompletedListener logCallCompletedListener;
        public final String number;
        public final String postDialDigits;
        public final int presentation;
        public final long timestamp;
        public final String viaNumber;

        public AddCallArgs(Context context, CallerInfo callerInfo, String str, String str2, String str3, int i, int i2, int i3, PhoneAccountHandle phoneAccountHandle, long j, long j2, Long l, UserHandle userHandle, boolean z, LogCallCompletedListener logCallCompletedListener, long j3, long j4) {
            this.context = context;
            this.callerInfo = callerInfo;
            this.number = str;
            this.postDialDigits = str2;
            this.viaNumber = str3;
            this.presentation = i;
            this.callType = i2;
            this.features = i3;
            this.accountHandle = phoneAccountHandle;
            this.timestamp = j;
            this.durationInSec = (int) (j2 / 1000);
            this.dataUsage = l;
            this.initiatingUser = userHandle;
            this.isRead = z;
            this.logCallCompletedListener = logCallCompletedListener;
            this.conferenceCallLogId = j3;
            this.conferenceDurationInSec = (int) (j4 / 1000);
        }
    }

    public CallLogManager(Context context, PhoneAccountRegistrar phoneAccountRegistrar, MissedCallNotifier missedCallNotifier) {
        this.mContext = context;
        this.mPhoneAccountRegistrar = phoneAccountRegistrar;
        this.mMissedCallNotifier = missedCallNotifier;
        registerUserSwitchReceiver();
    }

    @Override
    public void onCallStateChanged(Call call, int i, int i2) {
        int code = call.getDisconnectCause().getCode();
        boolean z = i2 == 7 || i2 == 8;
        boolean z2 = z && code == 4;
        PersistableBundle config = ((CarrierConfigManager) this.mContext.getSystemService("carrier_config")).getConfig();
        if (call.getConferenceCallLogId() > 0) {
            z2 = false;
        }
        String str = TAG;
        StringBuilder sb = new StringBuilder();
        sb.append("onCallStateChanged [");
        sb.append(call.getId());
        sb.append(", ");
        sb.append(Log.piiHandle(call.getOriginalHandle()));
        sb.append("] isNewlyDisconnected:");
        sb.append(z);
        sb.append(", disconnectCause:");
        sb.append(code);
        sb.append(", oldState:");
        sb.append(CallState.toString(i));
        sb.append(", newState:");
        sb.append(CallState.toString(i2));
        sb.append(", call.isConference():");
        sb.append(call.isConference());
        sb.append(", isCallCanceled:");
        sb.append(z2);
        sb.append(", hasParent:");
        sb.append(call.getParentCall() != null);
        sb.append(", ConferenceCallLogId:");
        sb.append(call.getConferenceCallLogId());
        Log.d(str, sb.toString(), new Object[0]);
        boolean z3 = call.isConference() && call.isIncoming() && call.getParentCall() == null;
        if (z) {
            if (i != 2 && ((!call.isConference() || z3) && !z2 && !call.isExternalCall() && (!call.isSelfManaged() || (call.isLoggedSelfManaged() && (call.getHandoverState() == 1 || call.getHandoverState() == 5))))) {
                int i3 = 11;
                int i4 = call.isIncoming() ? code == 5 ? 3 : code == 11 ? 7 : code == 6 ? 5 : 1 : 2;
                boolean z4 = !call.isSelfManaged();
                if (config == null || !config.getBoolean("mtk_dialer_call_pull_bool")) {
                    i3 = i4;
                    String reason = call.getDisconnectCause().getReason() != null ? "" : call.getDisconnectCause().getReason();
                    if (i != 3 && i != 4 && call.getConferenceCallLogId() <= 0 && !reason.contains("IMS_MERGED_SUCCESSFULLY")) {
                        showCallDuration(call);
                    }
                    logCall(call, i3, z4);
                } else {
                    Log.d(TAG, "Call Pull supported", new Object[0]);
                    String reason2 = call.getDisconnectCause().getReason();
                    if (reason2 != null) {
                        if (!call.isIncoming() && reason2.equalsIgnoreCase("Call Has Been Pulled by Another Device")) {
                            Log.d(TAG, "Call type is OUTGOING_PULLED_AWAY", new Object[0]);
                        } else if (reason2.equalsIgnoreCase("Another device sent All Devices Busy response") || reason2.equalsIgnoreCase("INCOMING_REJECTED")) {
                            i3 = 12;
                            Log.d(TAG, "Call type is Declined remotely", new Object[0]);
                        } else if (reason2.equalsIgnoreCase("Call Has Been Pulled by Another Device")) {
                            i3 = 10;
                            Log.d(TAG, "Call type is INCOMING_PULLED_AWAY", new Object[0]);
                        }
                        if (call.getDisconnectCause().getReason() != null) {
                        }
                        if (i != 3) {
                            showCallDuration(call);
                        }
                        logCall(call, i3, z4);
                    }
                }
            }
        }
        if (z && call.isConferenceInvitation() && call.isConference() && !z2) {
            logConferenceUnconnectedParticipants(call);
        }
        if (!z || !call.isConference() || z2 || call.isIncoming()) {
            return;
        }
        insertOrUpdateConferenceDuration(call);
    }

    private void insertOrUpdateConferenceDuration(Call call) {
        Log.d(TAG, "insertOrUpdateConferenceDuration", new Object[0]);
        long conferenceCallLogId = call.getConferenceCallLogId();
        int ageMillis = (int) (call.getAgeMillis() / 1000);
        long jCreateOrGetConferenceId = createOrGetConferenceId(conferenceCallLogId, ageMillis);
        Log.d(TAG, "Mapped id " + jCreateOrGetConferenceId + ", duration " + ageMillis, new Object[0]);
        MtkCallLog.ConferenceCalls.updateConferenceDurationIfNeeded(this.mContext, UserHandle.of(this.mCurrentUserId), jCreateOrGetConferenceId, ageMillis);
    }

    void logConferenceUnconnectedParticipants(Call call) {
        long conferenceCallLogId = call.getConferenceCallLogId();
        if (conferenceCallLogId <= 0) {
            conferenceCallLogId = call.getCreationTimeMillis();
            call.setConferenceCallLogId(conferenceCallLogId);
        }
        boolean z = false;
        Log.d(TAG, "logConferenceUnconnectedParticipants confCallLogId=" + conferenceCallLogId, new Object[0]);
        Iterator<String> it = call.getUnconnectedConferenceInvitationNumbers().iterator();
        while (it.hasNext()) {
            String next = it.next();
            String strExtractPostDialPortion = next != null ? PhoneNumberUtils.extractPostDialPortion(next) : "";
            if (!TextUtils.isEmpty(strExtractPostDialPortion) && strExtractPostDialPortion.equals(PhoneNumberUtils.extractPostDialPortion(next))) {
                next = PhoneNumberUtils.extractNetworkPortionAlt(next);
            }
            String str = next;
            PhoneAccountHandle targetPhoneAccount = call.getTargetPhoneAccount();
            logCall(null, str, strExtractPostDialPortion, "", 1, 2, getCallFeatures(call, targetPhoneAccount, z), targetPhoneAccount, call.getCreationTimeMillis(), 0L, null, PhoneNumberUtils.isEmergencyNumber(str), call.getInitiatingUser(), call.isSelfManaged(), null, conferenceCallLogId, -1L);
            it = it;
            z = z;
        }
    }

    void logCall(final Call call, int i, boolean z) {
        if (i == 3 && z) {
            logCall(call, 3, new LogCallCompletedListener() {
                @Override
                public void onLogCompleted(Uri uri) {
                    CallLogManager.this.mMissedCallNotifier.showMissedCallNotification(new MissedCallNotifier.CallInfo(call));
                }
            });
        } else {
            logCall(call, i, (LogCallCompletedListener) null);
        }
    }

    void logCall(Call call, int i, LogCallCompletedListener logCallCompletedListener) {
        Long lValueOf;
        String str;
        long ageMillis;
        long creationTimeMillis = call.getCreationTimeMillis();
        long ageMillis2 = call.getAgeMillis();
        String logNumber = getLogNumber(call);
        if ("voicemail".equals(getLogScheme(call)) && TextUtils.isEmpty(logNumber)) {
            Log.d(TAG, "Empty voice mail logNumber", new Object[0]);
            return;
        }
        if (handleConferenceSrvccCallLog(call, logNumber)) {
            return;
        }
        Log.d(TAG, "logNumber set to: %s", new Object[]{Log.pii(logNumber)});
        PhoneAccountHandle accountHandle = TelephonyUtil.getDefaultEmergencyPhoneAccount().getAccountHandle();
        String number = PhoneNumberUtils.formatNumber(call.getViaNumber(), getCountryIso());
        if (number == null) {
            number = call.getViaNumber();
        }
        PhoneAccountHandle targetPhoneAccount = call.getTargetPhoneAccount();
        PhoneAccountHandle phoneAccountHandle = accountHandle.equals(targetPhoneAccount) ? null : targetPhoneAccount;
        if (call.getCallDataUsage() != -1) {
            lValueOf = Long.valueOf(call.getCallDataUsage());
        } else {
            lValueOf = null;
        }
        int callFeatures = ExtensionManager.getCallMgrExt().getCallFeatures(call, getCallFeatures(call.getVideoStateHistory(), call.getDisconnectCause().getCode() == 12, shouldSaveHdInfo(call, phoneAccountHandle), (call.getConnectionProperties() & 512) == 512, call.wasEverRttCall()));
        if (call.getParentCall() != null) {
            str = number;
            ageMillis = call.getParentCall().getAgeMillis();
        } else {
            str = number;
            ageMillis = -1;
        }
        Log.d(TAG, "conferenceDuration: " + ageMillis, new Object[0]);
        logCall(call.getCallerInfo(), logNumber, call.getPostDialDigits(), str, call.getHandlePresentation(), i, callFeatures, phoneAccountHandle, creationTimeMillis, ageMillis2, lValueOf, call.isEmergencyCall(), call.getInitiatingUser(), call.isSelfManaged(), logCallCompletedListener, call.getConferenceCallLogId(), ageMillis);
    }

    private void logCall(CallerInfo callerInfo, String str, String str2, String str3, int i, int i2, int i3, PhoneAccountHandle phoneAccountHandle, long j, long j2, Long l, boolean z, UserHandle userHandle, boolean z2, LogCallCompletedListener logCallCompletedListener, long j3, long j4) {
        boolean z3;
        boolean z4;
        PersistableBundle configForSubId = ((CarrierConfigManager) this.mContext.getSystemService("carrier_config")).getConfigForSubId(this.mPhoneAccountRegistrar.getSubscriptionIdForPhoneAccount(phoneAccountHandle));
        if (configForSubId != null) {
            z3 = configForSubId.getBoolean("allow_emergency_numbers_in_call_log_bool");
        } else {
            z3 = false;
        }
        boolean z5 = !z || z3;
        sendAddCallBroadcast(i2, j2);
        if (!z5) {
            Log.d(TAG, "Not adding emergency call to call log.", new Object[0]);
            return;
        }
        Log.d(TAG, "Logging Call log entry: " + callerInfo + ", " + Log.pii(str) + "," + i + ", " + i2 + ", " + j + ", " + j2, new Object[0]);
        if (!z2) {
            z4 = false;
        } else {
            z4 = true;
        }
        logCallAsync(new AddCallArgs(this.mContext, callerInfo, str, str2, str3, i, i2, i3, phoneAccountHandle, j, j2, l, userHandle, z4, logCallCompletedListener, j3, j4));
    }

    private static int getCallFeatures(int i, boolean z, boolean z2, boolean z3, boolean z4) {
        int i2;
        if (VideoProfile.isVideo(i)) {
            i2 = 1;
        } else {
            i2 = 0;
        }
        if (z) {
            i2 |= 2;
        }
        if (z2) {
            i2 |= 4;
        }
        if (z3) {
            i2 |= 16;
        }
        if (z4) {
            return i2 | 32;
        }
        return i2;
    }

    private int getCallFeatures(Call call, PhoneAccountHandle phoneAccountHandle, boolean z) {
        return getCallFeatures(call.getVideoStateHistory(), z, shouldSaveHdInfo(call, phoneAccountHandle), (call.getConnectionProperties() & 512) == 512, call.wasEverRttCall());
    }

    private boolean shouldSaveHdInfo(Call call, PhoneAccountHandle phoneAccountHandle) {
        PersistableBundle configForSubId;
        CarrierConfigManager carrierConfigManager = (CarrierConfigManager) this.mContext.getSystemService("carrier_config");
        if (carrierConfigManager != null) {
            configForSubId = carrierConfigManager.getConfigForSubId(this.mPhoneAccountRegistrar.getSubscriptionIdForPhoneAccount(phoneAccountHandle));
        } else {
            configForSubId = null;
        }
        if (configForSubId != null && configForSubId.getBoolean("identify_high_definition_calls_in_call_log_bool") && call.wasHighDefAudio()) {
            return true;
        }
        return false;
    }

    private String getLogNumber(Call call) {
        Uri originalHandle = call.getOriginalHandle();
        if (originalHandle == null) {
            return null;
        }
        String schemeSpecificPart = originalHandle.getSchemeSpecificPart();
        if (!PhoneNumberUtils.isUriNumber(schemeSpecificPart)) {
            schemeSpecificPart = PhoneNumberUtils.stripSeparators(schemeSpecificPart);
        }
        String postDialDigits = call.getPostDialDigits();
        if (!TextUtils.isEmpty(postDialDigits) && postDialDigits.equals(PhoneNumberUtils.extractPostDialPortion(schemeSpecificPart))) {
            String strExtractNetworkPortionAlt = PhoneNumberUtils.extractNetworkPortionAlt(schemeSpecificPart);
            Log.d(TAG, "Remove duplicate post dial digits: " + postDialDigits, new Object[0]);
            return strExtractNetworkPortionAlt;
        }
        return schemeSpecificPart;
    }

    public AsyncTask<AddCallArgs, Void, Uri[]> logCallAsync(AddCallArgs addCallArgs) {
        return new LogCallAsyncTask().execute(addCallArgs);
    }

    private class LogCallAsyncTask extends AsyncTask<AddCallArgs, Void, Uri[]> {
        private AddCallArgs[] mAddCallArgs;
        private LogCallCompletedListener[] mListeners;

        private LogCallAsyncTask() {
            this.mAddCallArgs = null;
        }

        @Override
        protected Uri[] doInBackground(AddCallArgs... addCallArgsArr) {
            this.mAddCallArgs = addCallArgsArr;
            int length = addCallArgsArr.length;
            Uri[] uriArr = new Uri[length];
            this.mListeners = new LogCallCompletedListener[length];
            for (int i = 0; i < length; i++) {
                AddCallArgs addCallArgs = addCallArgsArr[i];
                this.mListeners[i] = addCallArgs.logCallCompletedListener;
                try {
                    uriArr[i] = addCall(addCallArgs);
                } catch (Exception e) {
                    Log.e(CallLogManager.TAG, e, "Exception raised during adding CallLog entry.", new Object[0]);
                    uriArr[i] = null;
                }
            }
            return uriArr;
        }

        private Uri addCall(AddCallArgs addCallArgs) {
            PhoneAccount phoneAccountUnchecked = CallLogManager.this.mPhoneAccountRegistrar.getPhoneAccountUnchecked(addCallArgs.accountHandle);
            if (phoneAccountUnchecked == null || !phoneAccountUnchecked.hasCapabilities(32)) {
                return addCall(addCallArgs, addCallArgs.accountHandle != null ? addCallArgs.accountHandle.getUserHandle() : null);
            }
            Log.i(CallLogManager.TAG, "addCall, c.initiatingUser=" + addCallArgs.initiatingUser, new Object[0]);
            if (addCallArgs.initiatingUser != null && UserUtil.isManagedProfile(CallLogManager.this.mContext, addCallArgs.initiatingUser)) {
                return addCall(addCallArgs, addCallArgs.initiatingUser);
            }
            return addCall(addCallArgs, null);
        }

        private Uri addCall(AddCallArgs addCallArgs, UserHandle userHandle) {
            UserHandle userHandleOf;
            Log.i(CallLogManager.TAG, "addCall, userToBeInserted=" + userHandle, new Object[0]);
            long jCreateOrGetConferenceId = CallLogManager.this.createOrGetConferenceId(addCallArgs.conferenceCallLogId, addCallArgs.conferenceDurationInSec);
            if (jCreateOrGetConferenceId > 0) {
                Log.i(CallLogManager.TAG, "addCall, change to current user " + CallLogManager.this.mCurrentUserId, new Object[0]);
                userHandleOf = UserHandle.of(CallLogManager.this.mCurrentUserId);
            } else {
                userHandleOf = userHandle;
            }
            return MtkCallLog.Calls.addCall(addCallArgs.callerInfo, addCallArgs.context, addCallArgs.number, addCallArgs.postDialDigits, addCallArgs.viaNumber, addCallArgs.presentation, addCallArgs.callType, addCallArgs.features, addCallArgs.accountHandle, addCallArgs.timestamp, addCallArgs.durationInSec, addCallArgs.dataUsage, userHandleOf == null, userHandleOf, addCallArgs.isRead, jCreateOrGetConferenceId, addCallArgs.conferenceDurationInSec);
        }

        @Override
        protected void onPostExecute(Uri[] uriArr) {
            for (int i = 0; i < uriArr.length; i++) {
                Uri uri = uriArr[i];
                if (uri == null) {
                    Log.w(CallLogManager.TAG, "Failed to write call to the log.", new Object[0]);
                }
                if (this.mListeners[i] != null) {
                    this.mListeners[i].onLogCompleted(uri);
                }
            }
            for (int i2 = 0; i2 < this.mAddCallArgs.length; i2++) {
                AddCallArgs addCallArgs = this.mAddCallArgs[i2];
                if (addCallArgs.conferenceCallLogId > 0 && uriArr[i2] != null) {
                    CallLogManager.this.updateSrvccConferenceCallLogs(addCallArgs.conferenceCallLogId, addCallArgs.number, uriArr[i2]);
                }
            }
        }
    }

    private void sendAddCallBroadcast(int i, long j) {
        Intent intent = new Intent("com.android.server.telecom.intent.action.CALLS_ADD_ENTRY");
        intent.putExtra("callType", i);
        intent.putExtra("duration", j);
        this.mContext.sendBroadcast(intent, "android.permission.PROCESS_CALLLOG_INFO");
    }

    private String getCountryIsoFromCountry(Country country) {
        if (country == null) {
            Log.w(TAG, "Value for country was null. Falling back to Locale.", new Object[0]);
            return Locale.getDefault().getCountry();
        }
        return country.getCountryIso();
    }

    public String getCountryIso() {
        String str;
        synchronized (this.mLock) {
            if (this.mCurrentCountryIso == null) {
                Log.i(TAG, "Country cache is null. Detecting Country and Setting Cache...", new Object[0]);
                CountryDetector countryDetector = (CountryDetector) this.mContext.getSystemService("country_detector");
                Country countryDetectCountry = null;
                if (countryDetector != null) {
                    countryDetectCountry = countryDetector.detectCountry();
                    countryDetector.addCountryListener(new CountryListener() {
                        public final void onCountryDetected(Country country) {
                            CallLogManager.lambda$getCountryIso$0(this.f$0, country);
                        }
                    }, Looper.getMainLooper());
                }
                this.mCurrentCountryIso = getCountryIsoFromCountry(countryDetectCountry);
            }
            str = this.mCurrentCountryIso;
        }
        return str;
    }

    public static void lambda$getCountryIso$0(CallLogManager callLogManager, Country country) {
        Log.startSession("CLM.oCD");
        try {
            synchronized (callLogManager.mLock) {
                Log.i(TAG, "Country ISO changed. Retrieving new ISO...", new Object[0]);
                callLogManager.mCurrentCountryIso = callLogManager.getCountryIsoFromCountry(country);
            }
        } finally {
            Log.endSession();
        }
    }

    private String getLogScheme(Call call) {
        Uri originalHandle = call.getOriginalHandle();
        if (originalHandle == null) {
            return null;
        }
        return originalHandle.getScheme();
    }

    private void showCallDuration(Call call) {
        final long ageMillis = call.getAgeMillis();
        Log.d(TAG, "showCallDuration: " + ageMillis, new Object[0]);
        if (ageMillis / 1000 != 0) {
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(CallLogManager.this.mContext, CallLogManager.this.getFormateDuration((int) (ageMillis / 1000)), 0).show();
                }
            });
        }
    }

    private String getFormateDuration(long j) {
        long j2;
        long j3 = 0;
        if (j >= 3600) {
            j3 = j / 3600;
            long j4 = j - (3600 * j3);
            j2 = j4 / 60;
            j = j4 - (60 * j2);
        } else if (j >= 60) {
            j2 = j / 60;
            j -= 60 * j2;
        } else {
            j2 = 0;
        }
        return this.mContext.getResources().getString(R.string.call_duration_title) + " (" + this.mContext.getResources().getString(R.string.call_duration_format, Long.valueOf(j3), Long.valueOf(j2), Long.valueOf(j)) + ")";
    }

    private synchronized long createOrGetConferenceId(long j, int i) {
        Log.i(TAG, "createOrGetConferenceId id " + j + " duration " + i, new Object[0]);
        if (j <= 0) {
            return 0L;
        }
        Long lValueOf = this.mConferenceCallLogIdMap.get(Long.valueOf(j));
        if (lValueOf == null || lValueOf.longValue() <= 0) {
            Uri uriAddConferenceCall = MtkCallLog.ConferenceCalls.addConferenceCall(this.mContext, UserHandle.of(this.mCurrentUserId), j, i);
            try {
                lValueOf = Long.valueOf(ContentUris.parseId(uriAddConferenceCall));
                if (lValueOf != null && lValueOf.longValue() > 0) {
                    this.mConferenceCallLogIdMap.put(Long.valueOf(j), lValueOf);
                    Log.d(TAG, "Temp conference Id: " + j + " Map to " + lValueOf, new Object[0]);
                }
                Log.d(TAG, "Invalid saved conference Id: " + lValueOf, new Object[0]);
                return 0L;
            } catch (Exception e) {
                Log.e(TAG, e, "Failed to parse conference saved uri:" + uriAddConferenceCall, new Object[0]);
                return 0L;
            }
        }
        return lValueOf.longValue();
    }

    @Override
    public void onIsConferencedChanged(Call call) {
        Log.d(TAG, "onIsConferencedChanged Call: " + call, new Object[0]);
        Call parentCall = call.getParentCall();
        if (parentCall != null) {
            long conferenceCallLogId = parentCall.getConferenceCallLogId();
            if (conferenceCallLogId <= 0) {
                conferenceCallLogId = parentCall.getCreationTimeMillis();
                parentCall.setConferenceCallLogId(conferenceCallLogId);
                Log.d(TAG, "New conference, set a new temp id: " + conferenceCallLogId, new Object[0]);
                Log.d(TAG, "Clear mSrvccConferenceCallLogs", new Object[0]);
                synchronized (this.mSrvccConferenceCallLogs) {
                    this.mSrvccConferenceCallLogs.clear();
                }
            }
            Log.d(TAG, "Set temp conference Id:" + conferenceCallLogId + " to Call:" + call.getId(), new Object[0]);
            call.setConferenceCallLogId(conferenceCallLogId);
        }
        Log.d(TAG, "ConferenceCallLogId: " + call.getConferenceCallLogId(), new Object[0]);
        if (!call.isConference() && call.getParentCall() == null) {
            Log.i(TAG, "onIsConferencedChanged Log this child call", new Object[0]);
            int code = call.getDisconnectCause().getCode();
            int i = 5;
            if (!call.isIncoming()) {
                i = 2;
            } else if (code == 5) {
                i = 3;
            } else if (code == 11) {
                i = 7;
            } else if (code != 6) {
                i = 1;
            }
            logCall(call, i, false);
            return;
        }
        Log.i(TAG, "onIsConferencedChanged No need to handle", new Object[0]);
    }

    private void addSrvccConferenceCallLogs(long j, String str, Uri uri) {
        Log.d(TAG, "addSrvccConferenceCallLogs confId: " + j + ", logNumber:" + str + ", callLogUri:" + uri, new Object[0]);
        if (TextUtils.isEmpty(str)) {
            return;
        }
        synchronized (this.mSrvccConferenceCallLogs) {
            HashMap<String, Uri> map = this.mSrvccConferenceCallLogs.get(Long.valueOf(j));
            if (map == null) {
                map = new HashMap<>();
                this.mSrvccConferenceCallLogs.put(Long.valueOf(j), map);
            }
            map.put(str, uri);
        }
    }

    private void updateSrvccConferenceCallLogs(long j, String str, Uri uri) {
        Log.d(TAG, "updateSrvccConferenceCallLogs confId: " + j + ", logNumber:" + str + ", callLogUri:" + uri, new Object[0]);
        if (TextUtils.isEmpty(str)) {
            return;
        }
        synchronized (this.mSrvccConferenceCallLogs) {
            HashMap<String, Uri> map = this.mSrvccConferenceCallLogs.get(Long.valueOf(j));
            if (map != null) {
                map.put(str, uri);
            }
        }
    }

    private Uri removeSrvccConferenceCallLogs(long j, String str) {
        String next;
        Log.d(TAG, "removeSrvccConferenceCallLogs confId: " + j + ", logNumber:" + str, new Object[0]);
        synchronized (this.mSrvccConferenceCallLogs) {
            HashMap<String, Uri> map = this.mSrvccConferenceCallLogs.get(Long.valueOf(j));
            if (map != null && !TextUtils.isEmpty(str)) {
                Iterator<String> it = map.keySet().iterator();
                while (it.hasNext()) {
                    next = it.next();
                    if (!str.equals(next) && !str.equals(PhoneNumberUtils.getUsernameFromUriNumber(next))) {
                    }
                }
                next = null;
                if (next == null) {
                    return null;
                }
                Log.d(TAG, "removeSrvccConferenceCallLogs removedCallLogNumber:" + next, new Object[0]);
                return map.remove(next);
            }
            return null;
        }
    }

    private boolean handleConferenceSrvccCallLog(Call call, String str) {
        Uri uriRemoveSrvccConferenceCallLogs;
        if (call.getConferenceCallLogId() <= 0) {
            return false;
        }
        if (call.hasProperty(32768)) {
            addSrvccConferenceCallLogs(call.getConferenceCallLogId(), str, null);
            return false;
        }
        if (call.hasProperty(32768) || (uriRemoveSrvccConferenceCallLogs = removeSrvccConferenceCallLogs(call.getConferenceCallLogId(), str)) == null) {
            return false;
        }
        new UpdateConferenceCallLogAsyncTask(call.getContext(), call, uriRemoveSrvccConferenceCallLogs).execute(new Void[0]);
        return true;
    }

    private class UpdateConferenceCallLogAsyncTask extends AsyncTask<Void, Void, Void> {
        private final Uri mCallLogUri;
        private final Call mChildCall;
        private final Context mContext;

        UpdateConferenceCallLogAsyncTask(Context context, Call call, Uri uri) {
            this.mContext = context;
            this.mChildCall = call;
            this.mCallLogUri = uri;
        }

        @Override
        protected Void doInBackground(Void... voidArr) throws Throwable {
            Cursor cursorQuery;
            try {
                try {
                    cursorQuery = this.mContext.getContentResolver().query(this.mCallLogUri, new String[]{"duration"}, null, null, null);
                    try {
                        cursorQuery.moveToFirst();
                        long j = cursorQuery.getLong(0) + (this.mChildCall.getAgeMillis() / 1000);
                        ContentValues contentValues = new ContentValues();
                        contentValues.put("duration", Long.valueOf(j));
                        Log.d(CallLogManager.TAG, "Update " + this.mCallLogUri + " with duration=" + j, new Object[0]);
                        this.mContext.getContentResolver().update(this.mCallLogUri, contentValues, null, null);
                    } catch (Exception e) {
                        e = e;
                        Log.e(CallLogManager.TAG, e, "Exception raised during update conference CallLog.", new Object[0]);
                        if (cursorQuery != null) {
                        }
                        return null;
                    }
                } catch (Throwable th) {
                    th = th;
                    if (cursorQuery != null) {
                        cursorQuery.close();
                    }
                    throw th;
                }
            } catch (Exception e2) {
                e = e2;
                cursorQuery = null;
            } catch (Throwable th2) {
                th = th2;
                cursorQuery = null;
                if (cursorQuery != null) {
                }
                throw th;
            }
            if (cursorQuery != null) {
                cursorQuery.close();
            }
            return null;
        }
    }

    private void registerUserSwitchReceiver() {
        Log.i(TAG, "registerUserSwitchReceiver", new Object[0]);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.USER_SWITCHED");
        this.mContext.registerReceiver(this.mUserSwitchReceiver, intentFilter);
    }
}
