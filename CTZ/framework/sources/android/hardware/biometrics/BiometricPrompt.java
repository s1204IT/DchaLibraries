package android.hardware.biometrics;

import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.hardware.biometrics.BiometricAuthenticator;
import android.hardware.biometrics.BiometricPrompt;
import android.hardware.biometrics.IBiometricPromptReceiver;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.text.TextUtils;
import java.security.Signature;
import java.util.concurrent.Executor;
import javax.crypto.Cipher;
import javax.crypto.Mac;

public class BiometricPrompt implements BiometricAuthenticator, BiometricConstants {
    public static final int DISMISSED_REASON_NEGATIVE = 2;
    public static final int DISMISSED_REASON_POSITIVE = 1;
    public static final int DISMISSED_REASON_USER_CANCEL = 3;
    public static final int HIDE_DIALOG_DELAY = 2000;
    public static final String KEY_DESCRIPTION = "description";
    public static final String KEY_NEGATIVE_TEXT = "negative_text";
    public static final String KEY_POSITIVE_TEXT = "positive_text";
    public static final String KEY_SUBTITLE = "subtitle";
    public static final String KEY_TITLE = "title";
    private Bundle mBundle;
    IBiometricPromptReceiver mDialogReceiver;
    private FingerprintManager mFingerprintManager;
    private ButtonInfo mNegativeButtonInfo;
    private PackageManager mPackageManager;
    private ButtonInfo mPositiveButtonInfo;

    BiometricPrompt(Context context, Bundle bundle, ButtonInfo buttonInfo, ButtonInfo buttonInfo2, AnonymousClass1 anonymousClass1) {
        this(context, bundle, buttonInfo, buttonInfo2);
    }

    private static class ButtonInfo {
        Executor executor;
        DialogInterface.OnClickListener listener;

        ButtonInfo(Executor executor, DialogInterface.OnClickListener onClickListener) {
            this.executor = executor;
            this.listener = onClickListener;
        }
    }

    public static class Builder {
        private final Bundle mBundle = new Bundle();
        private Context mContext;
        private ButtonInfo mNegativeButtonInfo;
        private ButtonInfo mPositiveButtonInfo;

        public Builder(Context context) {
            this.mContext = context;
        }

        public Builder setTitle(CharSequence charSequence) {
            this.mBundle.putCharSequence("title", charSequence);
            return this;
        }

        public Builder setSubtitle(CharSequence charSequence) {
            this.mBundle.putCharSequence(BiometricPrompt.KEY_SUBTITLE, charSequence);
            return this;
        }

        public Builder setDescription(CharSequence charSequence) {
            this.mBundle.putCharSequence("description", charSequence);
            return this;
        }

        public Builder setPositiveButton(CharSequence charSequence, Executor executor, DialogInterface.OnClickListener onClickListener) {
            if (TextUtils.isEmpty(charSequence)) {
                throw new IllegalArgumentException("Text must be set and non-empty");
            }
            if (executor == null) {
                throw new IllegalArgumentException("Executor must not be null");
            }
            if (onClickListener == null) {
                throw new IllegalArgumentException("Listener must not be null");
            }
            this.mBundle.putCharSequence(BiometricPrompt.KEY_POSITIVE_TEXT, charSequence);
            this.mPositiveButtonInfo = new ButtonInfo(executor, onClickListener);
            return this;
        }

        public Builder setNegativeButton(CharSequence charSequence, Executor executor, DialogInterface.OnClickListener onClickListener) {
            if (TextUtils.isEmpty(charSequence)) {
                throw new IllegalArgumentException("Text must be set and non-empty");
            }
            if (executor == null) {
                throw new IllegalArgumentException("Executor must not be null");
            }
            if (onClickListener == null) {
                throw new IllegalArgumentException("Listener must not be null");
            }
            this.mBundle.putCharSequence(BiometricPrompt.KEY_NEGATIVE_TEXT, charSequence);
            this.mNegativeButtonInfo = new ButtonInfo(executor, onClickListener);
            return this;
        }

        public BiometricPrompt build() {
            CharSequence charSequence = this.mBundle.getCharSequence("title");
            CharSequence charSequence2 = this.mBundle.getCharSequence(BiometricPrompt.KEY_NEGATIVE_TEXT);
            if (TextUtils.isEmpty(charSequence)) {
                throw new IllegalArgumentException("Title must be set and non-empty");
            }
            if (TextUtils.isEmpty(charSequence2)) {
                throw new IllegalArgumentException("Negative text must be set and non-empty");
            }
            return new BiometricPrompt(this.mContext, this.mBundle, this.mPositiveButtonInfo, this.mNegativeButtonInfo, null);
        }
    }

    class AnonymousClass1 extends IBiometricPromptReceiver.Stub {
        AnonymousClass1() {
        }

        @Override
        public void onDialogDismissed(int i) {
            if (i == 1) {
                BiometricPrompt.this.mPositiveButtonInfo.executor.execute(new Runnable() {
                    @Override
                    public final void run() {
                        BiometricPrompt.this.mPositiveButtonInfo.listener.onClick(null, -1);
                    }
                });
            } else if (i == 2) {
                BiometricPrompt.this.mNegativeButtonInfo.executor.execute(new Runnable() {
                    @Override
                    public final void run() {
                        BiometricPrompt.this.mNegativeButtonInfo.listener.onClick(null, -2);
                    }
                });
            }
        }
    }

    private BiometricPrompt(Context context, Bundle bundle, ButtonInfo buttonInfo, ButtonInfo buttonInfo2) {
        this.mDialogReceiver = new AnonymousClass1();
        this.mBundle = bundle;
        this.mPositiveButtonInfo = buttonInfo;
        this.mNegativeButtonInfo = buttonInfo2;
        this.mFingerprintManager = (FingerprintManager) context.getSystemService(FingerprintManager.class);
        this.mPackageManager = context.getPackageManager();
    }

    public static final class CryptoObject extends android.hardware.biometrics.CryptoObject {
        public CryptoObject(Signature signature) {
            super(signature);
        }

        public CryptoObject(Cipher cipher) {
            super(cipher);
        }

        public CryptoObject(Mac mac) {
            super(mac);
        }

        @Override
        public Signature getSignature() {
            return super.getSignature();
        }

        @Override
        public Cipher getCipher() {
            return super.getCipher();
        }

        @Override
        public Mac getMac() {
            return super.getMac();
        }
    }

    public static class AuthenticationResult extends BiometricAuthenticator.AuthenticationResult {
        public AuthenticationResult(CryptoObject cryptoObject, BiometricAuthenticator.BiometricIdentifier biometricIdentifier, int i) {
            super(cryptoObject, biometricIdentifier, i);
        }

        @Override
        public CryptoObject getCryptoObject() {
            return (CryptoObject) super.getCryptoObject();
        }
    }

    public static abstract class AuthenticationCallback extends BiometricAuthenticator.AuthenticationCallback {
        @Override
        public void onAuthenticationError(int i, CharSequence charSequence) {
        }

        @Override
        public void onAuthenticationHelp(int i, CharSequence charSequence) {
        }

        public void onAuthenticationSucceeded(AuthenticationResult authenticationResult) {
        }

        @Override
        public void onAuthenticationFailed() {
        }

        @Override
        public void onAuthenticationAcquired(int i) {
        }

        @Override
        public void onAuthenticationSucceeded(BiometricAuthenticator.AuthenticationResult authenticationResult) {
            onAuthenticationSucceeded(new AuthenticationResult((CryptoObject) authenticationResult.getCryptoObject(), authenticationResult.getId(), authenticationResult.getUserId()));
        }
    }

    @Override
    public void authenticate(android.hardware.biometrics.CryptoObject cryptoObject, CancellationSignal cancellationSignal, Executor executor, BiometricAuthenticator.AuthenticationCallback authenticationCallback) {
        if (!(authenticationCallback instanceof AuthenticationCallback)) {
            throw new IllegalArgumentException("Callback cannot be casted");
        }
        authenticate(cryptoObject, cancellationSignal, executor, (AuthenticationCallback) authenticationCallback);
    }

    @Override
    public void authenticate(CancellationSignal cancellationSignal, Executor executor, BiometricAuthenticator.AuthenticationCallback authenticationCallback) {
        if (!(authenticationCallback instanceof AuthenticationCallback)) {
            throw new IllegalArgumentException("Callback cannot be casted");
        }
        authenticate(cancellationSignal, executor, (AuthenticationCallback) authenticationCallback);
    }

    public void authenticate(CryptoObject cryptoObject, CancellationSignal cancellationSignal, Executor executor, AuthenticationCallback authenticationCallback) {
        if (handlePreAuthenticationErrors(authenticationCallback, executor)) {
            return;
        }
        this.mFingerprintManager.authenticate(cryptoObject, cancellationSignal, this.mBundle, executor, this.mDialogReceiver, authenticationCallback);
    }

    public void authenticate(CancellationSignal cancellationSignal, Executor executor, AuthenticationCallback authenticationCallback) {
        if (handlePreAuthenticationErrors(authenticationCallback, executor)) {
            return;
        }
        this.mFingerprintManager.authenticate(cancellationSignal, this.mBundle, executor, this.mDialogReceiver, authenticationCallback);
    }

    private boolean handlePreAuthenticationErrors(AuthenticationCallback authenticationCallback, Executor executor) {
        if (!this.mPackageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)) {
            sendError(12, authenticationCallback, executor);
            return true;
        }
        if (!this.mFingerprintManager.isHardwareDetected()) {
            sendError(1, authenticationCallback, executor);
            return true;
        }
        if (!this.mFingerprintManager.hasEnrolledFingerprints()) {
            sendError(11, authenticationCallback, executor);
            return true;
        }
        return false;
    }

    private void sendError(final int i, final AuthenticationCallback authenticationCallback, Executor executor) {
        executor.execute(new Runnable() {
            @Override
            public final void run() {
                BiometricPrompt biometricPrompt = this.f$0;
                BiometricPrompt.AuthenticationCallback authenticationCallback2 = authenticationCallback;
                int i2 = i;
                authenticationCallback2.onAuthenticationError(i2, biometricPrompt.mFingerprintManager.getErrorString(i2, 0));
            }
        });
    }
}
