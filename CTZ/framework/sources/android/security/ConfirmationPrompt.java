package android.security;

import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.Settings;
import android.security.IConfirmationPromptCallback;
import android.text.TextUtils;
import android.util.Log;
import java.util.Locale;
import java.util.concurrent.Executor;

public class ConfirmationPrompt {
    private static final String TAG = "ConfirmationPrompt";
    private static final int UI_OPTION_ACCESSIBILITY_INVERTED_FLAG = 1;
    private static final int UI_OPTION_ACCESSIBILITY_MAGNIFIED_FLAG = 2;
    private ConfirmationCallback mCallback;
    private final IBinder mCallbackBinder;
    private Context mContext;
    private Executor mExecutor;
    private byte[] mExtraData;
    private final KeyStore mKeyStore;
    private CharSequence mPromptText;

    private void doCallback(int i, byte[] bArr, ConfirmationCallback confirmationCallback) {
        if (i != 5) {
            switch (i) {
                case 0:
                    confirmationCallback.onConfirmed(bArr);
                    break;
                case 1:
                    confirmationCallback.onDismissed();
                    break;
                case 2:
                    confirmationCallback.onCanceled();
                    break;
                default:
                    confirmationCallback.onError(new Exception("Unexpected responseCode=" + i + " from onConfirmtionPromptCompleted() callback."));
                    break;
            }
        }
        confirmationCallback.onError(new Exception("System error returned by ConfirmationUI."));
    }

    public static final class Builder {
        private Context mContext;
        private byte[] mExtraData;
        private CharSequence mPromptText;

        public Builder(Context context) {
            this.mContext = context;
        }

        public Builder setPromptText(CharSequence charSequence) {
            this.mPromptText = charSequence;
            return this;
        }

        public Builder setExtraData(byte[] bArr) {
            this.mExtraData = bArr;
            return this;
        }

        public ConfirmationPrompt build() {
            if (TextUtils.isEmpty(this.mPromptText)) {
                throw new IllegalArgumentException("prompt text must be set and non-empty");
            }
            if (this.mExtraData == null) {
                throw new IllegalArgumentException("extraData must be set");
            }
            return new ConfirmationPrompt(this.mContext, this.mPromptText, this.mExtraData);
        }
    }

    private ConfirmationPrompt(Context context, CharSequence charSequence, byte[] bArr) {
        this.mKeyStore = KeyStore.getInstance();
        this.mCallbackBinder = new IConfirmationPromptCallback.Stub() {
            @Override
            public void onConfirmationPromptCompleted(final int i, final byte[] bArr2) throws RemoteException {
                if (ConfirmationPrompt.this.mCallback != null) {
                    final ConfirmationCallback confirmationCallback = ConfirmationPrompt.this.mCallback;
                    Executor executor = ConfirmationPrompt.this.mExecutor;
                    ConfirmationPrompt.this.mCallback = null;
                    ConfirmationPrompt.this.mExecutor = null;
                    if (executor == null) {
                        ConfirmationPrompt.this.doCallback(i, bArr2, confirmationCallback);
                    } else {
                        executor.execute(new Runnable() {
                            @Override
                            public void run() {
                                ConfirmationPrompt.this.doCallback(i, bArr2, confirmationCallback);
                            }
                        });
                    }
                }
            }
        };
        this.mContext = context;
        this.mPromptText = charSequence;
        this.mExtraData = bArr;
    }

    private int getUiOptionsAsFlags() {
        int i = 0;
        try {
            if (Settings.Secure.getInt(this.mContext.getContentResolver(), Settings.Secure.ACCESSIBILITY_DISPLAY_INVERSION_ENABLED) == 1) {
                i = 1;
            }
            if (Settings.System.getFloat(r1, Settings.System.FONT_SCALE) > 1.0d) {
                return i | 2;
            }
            return i;
        } catch (Settings.SettingNotFoundException e) {
            Log.w(TAG, "Unexpected SettingNotFoundException");
            return i;
        }
    }

    private static boolean isAccessibilityServiceRunning(Context context) {
        try {
            if (Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.ACCESSIBILITY_ENABLED) != 1) {
                return false;
            }
            return true;
        } catch (Settings.SettingNotFoundException e) {
            Log.w(TAG, "Unexpected SettingNotFoundException");
            e.printStackTrace();
            return false;
        }
    }

    public void presentPrompt(Executor executor, ConfirmationCallback confirmationCallback) throws ConfirmationNotAvailableException, ConfirmationAlreadyPresentingException {
        if (this.mCallback != null) {
            throw new ConfirmationAlreadyPresentingException();
        }
        if (isAccessibilityServiceRunning(this.mContext)) {
            throw new ConfirmationNotAvailableException();
        }
        this.mCallback = confirmationCallback;
        this.mExecutor = executor;
        int iPresentConfirmationPrompt = this.mKeyStore.presentConfirmationPrompt(this.mCallbackBinder, this.mPromptText.toString(), this.mExtraData, Locale.getDefault().toLanguageTag(), getUiOptionsAsFlags());
        if (iPresentConfirmationPrompt != 0) {
            if (iPresentConfirmationPrompt == 3) {
                throw new ConfirmationAlreadyPresentingException();
            }
            if (iPresentConfirmationPrompt == 6) {
                throw new ConfirmationNotAvailableException();
            }
            if (iPresentConfirmationPrompt == 65536) {
                throw new IllegalArgumentException();
            }
            Log.w(TAG, "Unexpected responseCode=" + iPresentConfirmationPrompt + " from presentConfirmationPrompt() call.");
            throw new IllegalArgumentException();
        }
    }

    public void cancelPrompt() {
        int iCancelConfirmationPrompt = this.mKeyStore.cancelConfirmationPrompt(this.mCallbackBinder);
        if (iCancelConfirmationPrompt == 0) {
            return;
        }
        if (iCancelConfirmationPrompt == 3) {
            throw new IllegalStateException();
        }
        Log.w(TAG, "Unexpected responseCode=" + iCancelConfirmationPrompt + " from cancelConfirmationPrompt() call.");
        throw new IllegalStateException();
    }

    public static boolean isSupported(Context context) {
        if (isAccessibilityServiceRunning(context)) {
            return false;
        }
        return KeyStore.getInstance().isConfirmationPromptSupported();
    }
}
