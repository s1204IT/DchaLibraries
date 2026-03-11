package com.android.settings.fingerprint;

import android.hardware.fingerprint.FingerprintManager;
import android.os.CancellationSignal;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.settings.R;
import com.android.settings.Utils;

public class FingerprintUiHelper extends FingerprintManager.AuthenticationCallback {
    private Callback mCallback;
    private CancellationSignal mCancellationSignal;
    private TextView mErrorTextView;
    private FingerprintManager mFingerprintManager;
    private ImageView mIcon;
    private Runnable mResetErrorTextRunnable = new Runnable() {
        @Override
        public void run() {
            FingerprintUiHelper.this.mErrorTextView.setText("");
            FingerprintUiHelper.this.mIcon.setImageResource(R.drawable.ic_fingerprint);
        }
    };
    private int mUserId;

    public interface Callback {
        void onAuthenticated();

        void onFingerprintIconVisibilityChanged(boolean z);
    }

    public FingerprintUiHelper(ImageView imageView, TextView textView, Callback callback, int i) {
        this.mFingerprintManager = Utils.getFingerprintManagerOrNull(imageView.getContext());
        this.mIcon = imageView;
        this.mErrorTextView = textView;
        this.mCallback = callback;
        this.mUserId = i;
    }

    public void startListening() {
        if (this.mFingerprintManager != null && this.mFingerprintManager.isHardwareDetected() && this.mFingerprintManager.getEnrolledFingerprints(this.mUserId).size() > 0) {
            this.mCancellationSignal = new CancellationSignal();
            this.mFingerprintManager.setActiveUser(this.mUserId);
            this.mFingerprintManager.authenticate(null, this.mCancellationSignal, 0, this, null, this.mUserId);
            setFingerprintIconVisibility(true);
            this.mIcon.setImageResource(R.drawable.ic_fingerprint);
        }
    }

    public void stopListening() {
        if (this.mCancellationSignal != null) {
            this.mCancellationSignal.cancel();
            this.mCancellationSignal = null;
        }
    }

    public boolean isListening() {
        return (this.mCancellationSignal == null || this.mCancellationSignal.isCanceled()) ? false : true;
    }

    private void setFingerprintIconVisibility(boolean z) {
        this.mIcon.setVisibility(z ? 0 : 8);
        this.mCallback.onFingerprintIconVisibilityChanged(z);
    }

    @Override
    public void onAuthenticationError(int i, CharSequence charSequence) {
        if (i == 5) {
            return;
        }
        showError(charSequence);
        setFingerprintIconVisibility(false);
    }

    @Override
    public void onAuthenticationHelp(int i, CharSequence charSequence) {
        showError(charSequence);
    }

    @Override
    public void onAuthenticationFailed() {
        showError(this.mIcon.getResources().getString(R.string.fingerprint_not_recognized));
    }

    @Override
    public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult authenticationResult) {
        this.mIcon.setImageResource(R.drawable.ic_fingerprint_success);
        this.mCallback.onAuthenticated();
    }

    private void showError(CharSequence charSequence) {
        if (!isListening()) {
            return;
        }
        this.mIcon.setImageResource(R.drawable.ic_fingerprint_error);
        this.mErrorTextView.setText(charSequence);
        this.mErrorTextView.removeCallbacks(this.mResetErrorTextRunnable);
        this.mErrorTextView.postDelayed(this.mResetErrorTextRunnable, 1300L);
    }
}
