package com.android.server.pm;

import android.content.pm.PackageParser;
import android.util.ArraySet;
import android.util.Slog;
import java.util.ArrayList;

public class IntentFilterVerificationState {
    public static final int STATE_UNDEFINED = 0;
    public static final int STATE_VERIFICATION_FAILURE = 3;
    public static final int STATE_VERIFICATION_PENDING = 1;
    public static final int STATE_VERIFICATION_SUCCESS = 2;
    static final String TAG = IntentFilterVerificationState.class.getName();
    private String mPackageName;
    private int mRequiredVerifierUid;
    private int mUserId;
    private ArrayList<PackageParser.ActivityIntentInfo> mFilters = new ArrayList<>();
    private ArraySet<String> mHosts = new ArraySet<>();
    private int mState = 0;
    private boolean mVerificationComplete = false;

    public IntentFilterVerificationState(int i, int i2, String str) {
        this.mRequiredVerifierUid = 0;
        this.mRequiredVerifierUid = i;
        this.mUserId = i2;
        this.mPackageName = str;
    }

    public void setState(int i) {
        if (i > 3 || i < 0) {
            this.mState = 0;
        } else {
            this.mState = i;
        }
    }

    public int getState() {
        return this.mState;
    }

    public void setPendingState() {
        setState(1);
    }

    public ArrayList<PackageParser.ActivityIntentInfo> getFilters() {
        return this.mFilters;
    }

    public boolean isVerificationComplete() {
        return this.mVerificationComplete;
    }

    public boolean isVerified() {
        return this.mVerificationComplete && this.mState == 2;
    }

    public int getUserId() {
        return this.mUserId;
    }

    public String getPackageName() {
        return this.mPackageName;
    }

    public String getHostsString() {
        StringBuilder sb = new StringBuilder();
        int size = this.mHosts.size();
        for (int i = 0; i < size; i++) {
            if (i > 0) {
                sb.append(" ");
            }
            String strValueAt = this.mHosts.valueAt(i);
            if (strValueAt.startsWith("*.")) {
                strValueAt = strValueAt.substring(2);
            }
            sb.append(strValueAt);
        }
        return sb.toString();
    }

    public boolean setVerifierResponse(int i, int i2) {
        int i3 = 0;
        if (this.mRequiredVerifierUid == i) {
            if (i2 == 1) {
                i3 = 2;
            } else if (i2 == -1) {
                i3 = 3;
            }
            this.mVerificationComplete = true;
            setState(i3);
            return true;
        }
        Slog.d(TAG, "Cannot set verifier response with callerUid:" + i + " and code:" + i2 + " as required verifierUid is:" + this.mRequiredVerifierUid);
        return false;
    }

    public void addFilter(PackageParser.ActivityIntentInfo activityIntentInfo) {
        this.mFilters.add(activityIntentInfo);
        this.mHosts.addAll(activityIntentInfo.getHostsList());
    }
}
