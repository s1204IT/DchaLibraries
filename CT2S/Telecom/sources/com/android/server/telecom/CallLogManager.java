package com.android.server.telecom;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.CallLog;
import android.telecom.PhoneAccountHandle;
import android.telephony.PhoneNumberUtils;
import com.android.internal.telephony.CallerInfo;

final class CallLogManager extends CallsManagerListenerBase {
    private static final String TAG = CallLogManager.class.getSimpleName();
    private final Context mContext;

    private static class AddCallArgs {
        public final PhoneAccountHandle accountHandle;
        public final int callType;
        public final CallerInfo callerInfo;
        public final Context context;
        public final Long dataUsage;
        public final int durationInSec;
        public final int features;
        public final String number;
        public final int presentation;
        public final long timestamp;

        public AddCallArgs(Context context, CallerInfo callerInfo, String str, int i, int i2, int i3, PhoneAccountHandle phoneAccountHandle, long j, long j2, Long l) {
            this.context = context;
            this.callerInfo = callerInfo;
            this.number = str;
            this.presentation = i;
            this.callType = i2;
            this.features = i3;
            this.accountHandle = phoneAccountHandle;
            this.timestamp = j;
            this.durationInSec = (int) (j2 / 1000);
            this.dataUsage = l;
        }
    }

    public CallLogManager(Context context) {
        this.mContext = context;
    }

    @Override
    public void onCallStateChanged(Call call, int i, int i2) {
        int i3 = 2;
        boolean z = false;
        int code = call.getDisconnectCause().getCode();
        boolean z2 = i2 == 7 || i2 == 8;
        if (z2 && code == 4) {
            z = true;
        }
        if (z2 && i != 2 && !call.isConference() && !z) {
            if (call.isIncoming()) {
                i3 = code == 5 ? 3 : 1;
            }
            logCall(call, i3);
        }
    }

    void logCall(Call call, int i) {
        long creationTimeMillis = call.getCreationTimeMillis();
        long ageMillis = call.getAgeMillis();
        String logNumber = getLogNumber(call);
        Log.d(TAG, "logNumber set to: %s", Log.pii(logNumber));
        logCall(call.getCallerInfo(), logNumber, call.getHandlePresentation(), i, getCallFeatures(call.getVideoStateHistory()), call.getTargetPhoneAccount(), creationTimeMillis, ageMillis, null);
    }

    private void logCall(CallerInfo callerInfo, String str, int i, int i2, int i3, PhoneAccountHandle phoneAccountHandle, long j, long j2, Long l) {
        boolean z = !PhoneNumberUtils.isLocalEmergencyNumber(this.mContext, str) || this.mContext.getResources().getBoolean(R.bool.allow_emergency_numbers_in_call_log);
        sendAddCallBroadcast(i2, j2);
        if (z) {
            Log.d(TAG, "Logging Calllog entry: " + callerInfo + ", " + Log.pii(str) + "," + i + ", " + i2 + ", " + j + ", " + j2, new Object[0]);
            logCallAsync(new AddCallArgs(this.mContext, callerInfo, str, i, i2, i3, phoneAccountHandle, j, j2, l));
        } else {
            Log.d(TAG, "Not adding emergency call to call log.", new Object[0]);
        }
    }

    private static int getCallFeatures(int i) {
        return (i & 1) == 1 ? 1 : 0;
    }

    private String getLogNumber(Call call) {
        Uri originalHandle = call.getOriginalHandle();
        if (originalHandle == null) {
            return null;
        }
        String schemeSpecificPart = originalHandle.getSchemeSpecificPart();
        if (!PhoneNumberUtils.isUriNumber(schemeSpecificPart)) {
            return PhoneNumberUtils.stripSeparators(schemeSpecificPart);
        }
        return schemeSpecificPart;
    }

    public AsyncTask<AddCallArgs, Void, Uri[]> logCallAsync(AddCallArgs addCallArgs) {
        return new LogCallAsyncTask().execute(addCallArgs);
    }

    private class LogCallAsyncTask extends AsyncTask<AddCallArgs, Void, Uri[]> {
        private LogCallAsyncTask() {
        }

        @Override
        protected Uri[] doInBackground(AddCallArgs... addCallArgsArr) {
            int length = addCallArgsArr.length;
            Uri[] uriArr = new Uri[length];
            int i = 0;
            while (true) {
                int i2 = i;
                if (i2 >= length) {
                    return uriArr;
                }
                AddCallArgs addCallArgs = addCallArgsArr[i2];
                try {
                    uriArr[i2] = CallLog.Calls.addCall(addCallArgs.callerInfo, addCallArgs.context, addCallArgs.number, addCallArgs.presentation, addCallArgs.callType, addCallArgs.features, addCallArgs.accountHandle, addCallArgs.timestamp, addCallArgs.durationInSec, addCallArgs.dataUsage, true);
                } catch (Exception e) {
                    Log.e(CallLogManager.TAG, (Throwable) e, "Exception raised during adding CallLog entry.", new Object[0]);
                    uriArr[i2] = null;
                }
                i = i2 + 1;
            }
        }

        @Override
        protected void onPostExecute(Uri[] uriArr) {
            for (Uri uri : uriArr) {
                if (uri == null) {
                    Log.w(CallLogManager.TAG, "Failed to write call to the log.", new Object[0]);
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
}
