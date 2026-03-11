package com.android.settings.fingerprint;

import android.hardware.fingerprint.FingerprintManager;
import android.os.CancellationSignal;
import com.android.settings.core.InstrumentedFragment;

public class FingerprintAuthenticateSidecar extends InstrumentedFragment {
    private FingerprintManager.AuthenticationCallback mAuthenticationCallback = new FingerprintManager.AuthenticationCallback() {
        @Override
        public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult authenticationResult) {
            FingerprintAuthenticateSidecar.this.mCancellationSignal = null;
            if (FingerprintAuthenticateSidecar.this.mListener != null) {
                FingerprintAuthenticateSidecar.this.mListener.onAuthenticationSucceeded(authenticationResult);
            } else {
                FingerprintAuthenticateSidecar.this.mAuthenticationResult = authenticationResult;
                FingerprintAuthenticateSidecar.this.mAuthenticationError = null;
            }
        }

        @Override
        public void onAuthenticationFailed() {
            if (FingerprintAuthenticateSidecar.this.mListener != null) {
                FingerprintAuthenticateSidecar.this.mListener.onAuthenticationFailed();
            }
        }

        @Override
        public void onAuthenticationError(int i, CharSequence charSequence) {
            FingerprintAuthenticateSidecar.this.mCancellationSignal = null;
            if (FingerprintAuthenticateSidecar.this.mListener != null) {
                FingerprintAuthenticateSidecar.this.mListener.onAuthenticationError(i, charSequence);
                return;
            }
            FingerprintAuthenticateSidecar.this.mAuthenticationError = FingerprintAuthenticateSidecar.this.new AuthenticationError(i, charSequence);
            FingerprintAuthenticateSidecar.this.mAuthenticationResult = null;
        }

        @Override
        public void onAuthenticationHelp(int i, CharSequence charSequence) {
            if (FingerprintAuthenticateSidecar.this.mListener != null) {
                FingerprintAuthenticateSidecar.this.mListener.onAuthenticationHelp(i, charSequence);
            }
        }
    };
    private AuthenticationError mAuthenticationError;
    private FingerprintManager.AuthenticationResult mAuthenticationResult;
    private CancellationSignal mCancellationSignal;
    private FingerprintManager mFingerprintManager;
    private Listener mListener;

    public interface Listener {
        void onAuthenticationError(int i, CharSequence charSequence);

        void onAuthenticationFailed();

        void onAuthenticationHelp(int i, CharSequence charSequence);

        void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult authenticationResult);
    }

    private class AuthenticationError {
        int error;
        CharSequence errorString;

        public AuthenticationError(int i, CharSequence charSequence) {
            this.error = i;
            this.errorString = charSequence;
        }
    }

    @Override
    public int getMetricsCategory() {
        return 1221;
    }

    public void setFingerprintManager(FingerprintManager fingerprintManager) {
        this.mFingerprintManager = fingerprintManager;
    }

    public void startAuthentication(int i) {
        this.mCancellationSignal = new CancellationSignal();
        this.mFingerprintManager.authenticate(null, this.mCancellationSignal, 0, this.mAuthenticationCallback, null, i);
    }

    public void stopAuthentication() {
        if (this.mCancellationSignal != null && !this.mCancellationSignal.isCanceled()) {
            this.mCancellationSignal.cancel();
        }
        this.mCancellationSignal = null;
    }

    public void setListener(Listener listener) {
        if (this.mListener == null && listener != null) {
            if (this.mAuthenticationResult != null) {
                listener.onAuthenticationSucceeded(this.mAuthenticationResult);
                this.mAuthenticationResult = null;
            }
            if (this.mAuthenticationError != null && this.mAuthenticationError.error != 5) {
                listener.onAuthenticationError(this.mAuthenticationError.error, this.mAuthenticationError.errorString);
                this.mAuthenticationError = null;
            }
        }
        this.mListener = listener;
    }
}
