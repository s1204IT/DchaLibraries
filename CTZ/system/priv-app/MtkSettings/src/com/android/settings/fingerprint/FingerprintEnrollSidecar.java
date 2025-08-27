package com.android.settings.fingerprint;

import android.app.Activity;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import com.android.settings.Utils;
import com.android.settings.core.InstrumentedFragment;
import java.util.ArrayList;

/* loaded from: classes.dex */
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
    private FingerprintManager.EnrollmentCallback mEnrollmentCallback = new FingerprintManager.EnrollmentCallback() { // from class: com.android.settings.fingerprint.FingerprintEnrollSidecar.1
        public void onEnrollmentProgress(int i) {
            if (FingerprintEnrollSidecar.this.mEnrollmentSteps == -1) {
                FingerprintEnrollSidecar.this.mEnrollmentSteps = i;
            }
            FingerprintEnrollSidecar.this.mEnrollmentRemaining = i;
            FingerprintEnrollSidecar.this.mDone = i == 0;
            if (FingerprintEnrollSidecar.this.mListener != null) {
                FingerprintEnrollSidecar.this.mListener.onEnrollmentProgressChange(FingerprintEnrollSidecar.this.mEnrollmentSteps, i);
            } else {
                FingerprintEnrollSidecar.this.mQueuedEvents.add(FingerprintEnrollSidecar.this.new QueuedEnrollmentProgress(FingerprintEnrollSidecar.this.mEnrollmentSteps, i));
            }
        }

        public void onEnrollmentHelp(int i, CharSequence charSequence) {
            if (FingerprintEnrollSidecar.this.mListener != null) {
                FingerprintEnrollSidecar.this.mListener.onEnrollmentHelp(charSequence);
            } else {
                FingerprintEnrollSidecar.this.mQueuedEvents.add(FingerprintEnrollSidecar.this.new QueuedEnrollmentHelp(i, charSequence));
            }
        }

        public void onEnrollmentError(int i, CharSequence charSequence) {
            if (FingerprintEnrollSidecar.this.mListener != null) {
                FingerprintEnrollSidecar.this.mListener.onEnrollmentError(i, charSequence);
            } else {
                FingerprintEnrollSidecar.this.mQueuedEvents.add(FingerprintEnrollSidecar.this.new QueuedEnrollmentError(i, charSequence));
            }
            FingerprintEnrollSidecar.this.mEnrolling = false;
        }
    };
    private final Runnable mTimeoutRunnable = new Runnable() { // from class: com.android.settings.fingerprint.FingerprintEnrollSidecar.2
        @Override // java.lang.Runnable
        public void run() {
            FingerprintEnrollSidecar.this.cancelEnrollment();
        }
    };
    private ArrayList<QueuedEvent> mQueuedEvents = new ArrayList<>();

    public interface Listener {
        void onEnrollmentError(int i, CharSequence charSequence);

        void onEnrollmentHelp(CharSequence charSequence);

        void onEnrollmentProgressChange(int i, int i2);
    }

    private abstract class QueuedEvent {
        public abstract void send(Listener listener);

        private QueuedEvent() {
        }
    }

    private class QueuedEnrollmentProgress extends QueuedEvent {
        int enrollmentSteps;
        int remaining;

        public QueuedEnrollmentProgress(int i, int i2) {
            super();
            this.enrollmentSteps = i;
            this.remaining = i2;
        }

        @Override // com.android.settings.fingerprint.FingerprintEnrollSidecar.QueuedEvent
        public void send(Listener listener) {
            listener.onEnrollmentProgressChange(this.enrollmentSteps, this.remaining);
        }
    }

    private class QueuedEnrollmentHelp extends QueuedEvent {
        int helpMsgId;
        CharSequence helpString;

        public QueuedEnrollmentHelp(int i, CharSequence charSequence) {
            super();
            this.helpMsgId = i;
            this.helpString = charSequence;
        }

        @Override // com.android.settings.fingerprint.FingerprintEnrollSidecar.QueuedEvent
        public void send(Listener listener) {
            listener.onEnrollmentHelp(this.helpString);
        }
    }

    private class QueuedEnrollmentError extends QueuedEvent {
        int errMsgId;
        CharSequence errString;

        public QueuedEnrollmentError(int i, CharSequence charSequence) {
            super();
            this.errMsgId = i;
            this.errString = charSequence;
        }

        @Override // com.android.settings.fingerprint.FingerprintEnrollSidecar.QueuedEvent
        public void send(Listener listener) {
            listener.onEnrollmentError(this.errMsgId, this.errString);
        }
    }

    @Override // com.android.settingslib.core.lifecycle.ObservableFragment, android.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setRetainInstance(true);
    }

    @Override // android.app.Fragment
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.mFingerprintManager = Utils.getFingerprintManagerOrNull(activity);
        this.mToken = activity.getIntent().getByteArrayExtra("hw_auth_token");
        this.mUserId = activity.getIntent().getIntExtra("android.intent.extra.USER_ID", -10000);
    }

    @Override // com.android.settingslib.core.lifecycle.ObservableFragment, android.app.Fragment
    public void onStart() {
        super.onStart();
        if (!this.mEnrolling) {
            startEnrollment();
        }
    }

    @Override // com.android.settingslib.core.lifecycle.ObservableFragment, android.app.Fragment
    public void onStop() {
        super.onStop();
        if (!getActivity().isChangingConfigurations()) {
            cancelEnrollment();
        }
    }

    private void startEnrollment() {
        this.mHandler.removeCallbacks(this.mTimeoutRunnable);
        this.mEnrollmentSteps = -1;
        this.mEnrollmentCancel = new CancellationSignal();
        if (this.mUserId != -10000) {
            this.mFingerprintManager.setActiveUser(this.mUserId);
        }
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
        if (this.mListener != null) {
            for (int i = 0; i < this.mQueuedEvents.size(); i++) {
                this.mQueuedEvents.get(i).send(this.mListener);
            }
            this.mQueuedEvents.clear();
        }
    }

    public int getEnrollmentSteps() {
        return this.mEnrollmentSteps;
    }

    public int getEnrollmentRemaining() {
        return this.mEnrollmentRemaining;
    }

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 245;
    }
}
