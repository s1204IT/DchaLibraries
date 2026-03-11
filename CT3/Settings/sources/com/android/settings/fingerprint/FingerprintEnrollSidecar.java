package com.android.settings.fingerprint;

import android.app.Activity;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.util.Log;
import com.android.settings.InstrumentedFragment;

public class FingerprintEnrollSidecar extends InstrumentedFragment {
    private boolean mDone;
    private boolean mEnrolling;
    private CancellationSignal mEnrollmentCancel;
    private FingerprintManager mFingerprintManager;
    private Listener mListener;
    private byte[] mToken;
    private int mUserId;
    private int mEnrollmentSteps = -1;
    private int mEnrollmentRemaining = 0;
    private Handler mHandler = new Handler();
    private FingerprintManager.EnrollmentCallback mEnrollmentCallback = new FingerprintManager.EnrollmentCallback() {
        public void onEnrollmentProgress(int remaining) {
            if (FingerprintEnrollSidecar.this.mEnrollmentSteps == -1) {
                FingerprintEnrollSidecar.this.mEnrollmentSteps = remaining;
            }
            FingerprintEnrollSidecar.this.mEnrollmentRemaining = remaining;
            FingerprintEnrollSidecar.this.mDone = remaining == 0;
            if (FingerprintEnrollSidecar.this.mListener == null) {
                return;
            }
            FingerprintEnrollSidecar.this.mListener.onEnrollmentProgressChange(FingerprintEnrollSidecar.this.mEnrollmentSteps, remaining);
        }

        public void onEnrollmentHelp(int helpMsgId, CharSequence helpString) {
            if (FingerprintEnrollSidecar.this.mListener == null) {
                return;
            }
            FingerprintEnrollSidecar.this.mListener.onEnrollmentHelp(helpString);
        }

        public void onEnrollmentError(int errMsgId, CharSequence errString) {
            if (FingerprintEnrollSidecar.this.mListener != null) {
                FingerprintEnrollSidecar.this.mListener.onEnrollmentError(errMsgId, errString);
            }
            FingerprintEnrollSidecar.this.mEnrolling = false;
        }
    };
    private final Runnable mTimeoutRunnable = new Runnable() {
        @Override
        public void run() {
            FingerprintEnrollSidecar.this.cancelEnrollment();
        }
    };

    public interface Listener {
        void onEnrollmentError(int i, CharSequence charSequence);

        void onEnrollmentHelp(CharSequence charSequence);

        void onEnrollmentProgressChange(int i, int i2);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.mFingerprintManager = (FingerprintManager) activity.getSystemService(FingerprintManager.class);
        this.mToken = activity.getIntent().getByteArrayExtra("hw_auth_token");
        this.mUserId = activity.getIntent().getIntExtra("android.intent.extra.USER_ID", -10000);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (this.mEnrolling) {
            return;
        }
        startEnrollment();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (getActivity().isChangingConfigurations()) {
            return;
        }
        cancelEnrollment();
    }

    private void startEnrollment() {
        this.mHandler.removeCallbacks(this.mTimeoutRunnable);
        this.mEnrollmentSteps = -1;
        this.mEnrollmentCancel = new CancellationSignal();
        if (this.mUserId != -10000) {
            this.mFingerprintManager.setActiveUser(this.mUserId);
        }
        Log.d("FingerprintEnrollSidecar", "startEnrollment: mToken: " + this.mToken);
        this.mFingerprintManager.enroll(this.mToken, this.mEnrollmentCancel, 0, this.mUserId, this.mEnrollmentCallback);
        this.mEnrolling = true;
    }

    boolean cancelEnrollment() {
        this.mHandler.removeCallbacks(this.mTimeoutRunnable);
        if (!this.mEnrolling) {
            return false;
        }
        this.mEnrollmentCancel.cancel();
        this.mEnrolling = false;
        this.mEnrollmentSteps = -1;
        return true;
    }

    public void setListener(Listener listener) {
        this.mListener = listener;
    }

    public int getEnrollmentSteps() {
        return this.mEnrollmentSteps;
    }

    public int getEnrollmentRemaining() {
        return this.mEnrollmentRemaining;
    }

    @Override
    protected int getMetricsCategory() {
        return 245;
    }
}
