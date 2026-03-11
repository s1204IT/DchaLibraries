package com.android.settings;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;

public class CredentialCheckResultTracker extends Fragment {
    private boolean mHasResult = false;
    private Listener mListener;
    private Intent mResultData;
    private int mResultEffectiveUserId;
    private boolean mResultMatched;
    private int mResultTimeoutMs;

    interface Listener {
        void onCredentialChecked(boolean z, Intent intent, int i, int i2, boolean z2);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    public void setListener(Listener listener) {
        if (this.mListener == listener) {
            return;
        }
        this.mListener = listener;
        if (this.mListener == null || !this.mHasResult) {
            return;
        }
        this.mListener.onCredentialChecked(this.mResultMatched, this.mResultData, this.mResultTimeoutMs, this.mResultEffectiveUserId, false);
    }

    public void setResult(boolean matched, Intent intent, int timeoutMs, int effectiveUserId) {
        this.mResultMatched = matched;
        this.mResultData = intent;
        this.mResultTimeoutMs = timeoutMs;
        this.mResultEffectiveUserId = effectiveUserId;
        this.mHasResult = true;
        if (this.mListener == null) {
            return;
        }
        this.mListener.onCredentialChecked(this.mResultMatched, this.mResultData, this.mResultTimeoutMs, this.mResultEffectiveUserId, true);
        this.mHasResult = false;
    }

    public void clearResult() {
        this.mHasResult = false;
        this.mResultMatched = false;
        this.mResultData = null;
        this.mResultTimeoutMs = 0;
        this.mResultEffectiveUserId = 0;
    }
}
