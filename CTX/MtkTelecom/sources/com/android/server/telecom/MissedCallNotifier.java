package com.android.server.telecom;

import android.net.Uri;
import android.os.UserHandle;
import android.telecom.PhoneAccountHandle;
import com.android.internal.telephony.CallerInfo;
import com.android.server.telecom.CallsManager;

public interface MissedCallNotifier extends CallsManager.CallsManagerListener {
    void clearMissedCalls(UserHandle userHandle);

    void reloadAfterBootComplete(CallerInfoLookupHelper callerInfoLookupHelper, CallInfoFactory callInfoFactory);

    void reloadFromDatabase(CallerInfoLookupHelper callerInfoLookupHelper, CallInfoFactory callInfoFactory, UserHandle userHandle);

    void setCurrentUserHandle(UserHandle userHandle);

    void showMissedCallNotification(CallInfo callInfo);

    public static class CallInfoFactory {
        public CallInfo makeCallInfo(CallerInfo callerInfo, PhoneAccountHandle phoneAccountHandle, Uri uri, long j) {
            return new CallInfo(callerInfo, phoneAccountHandle, uri, j);
        }
    }

    public static class CallInfo {
        private CallerInfo mCallerInfo;
        private long mCreationTimeMillis;
        private Uri mHandle;
        private PhoneAccountHandle mPhoneAccountHandle;

        public CallInfo(CallerInfo callerInfo, PhoneAccountHandle phoneAccountHandle, Uri uri, long j) {
            this.mCallerInfo = callerInfo;
            this.mPhoneAccountHandle = phoneAccountHandle;
            this.mHandle = uri;
            this.mCreationTimeMillis = j;
        }

        public CallInfo(Call call) {
            this.mCallerInfo = call.getCallerInfo();
            this.mPhoneAccountHandle = call.getTargetPhoneAccount();
            this.mHandle = call.getHandle();
            this.mCreationTimeMillis = call.getCreationTimeMillis();
        }

        public CallerInfo getCallerInfo() {
            return this.mCallerInfo;
        }

        public PhoneAccountHandle getPhoneAccountHandle() {
            return this.mPhoneAccountHandle;
        }

        public Uri getHandle() {
            return this.mHandle;
        }

        public String getHandleSchemeSpecificPart() {
            if (this.mHandle == null) {
                return null;
            }
            return this.mHandle.getSchemeSpecificPart();
        }

        public long getCreationTimeMillis() {
            return this.mCreationTimeMillis;
        }

        public String getPhoneNumber() {
            if (this.mCallerInfo == null) {
                return null;
            }
            return this.mCallerInfo.phoneNumber;
        }

        public String getName() {
            if (this.mCallerInfo == null) {
                return null;
            }
            return this.mCallerInfo.name;
        }
    }
}
