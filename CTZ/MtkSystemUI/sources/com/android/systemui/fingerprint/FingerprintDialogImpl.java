package com.android.systemui.fingerprint;

import android.R;
import android.hardware.biometrics.IBiometricPromptReceiver;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.view.WindowManager;
import com.android.internal.os.SomeArgs;
import com.android.systemui.SystemUI;
import com.android.systemui.statusbar.CommandQueue;

public class FingerprintDialogImpl extends SystemUI implements CommandQueue.Callbacks {
    private boolean mDialogShowing;
    private FingerprintDialogView mDialogView;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    FingerprintDialogImpl.this.handleShowDialog((SomeArgs) message.obj);
                    break;
                case 2:
                    FingerprintDialogImpl.this.handleFingerprintAuthenticated();
                    break;
                case 3:
                    FingerprintDialogImpl.this.handleFingerprintHelp((String) message.obj);
                    break;
                case 4:
                    FingerprintDialogImpl.this.handleFingerprintError((String) message.obj);
                    break;
                case 5:
                    FingerprintDialogImpl.this.handleHideDialog(((Boolean) message.obj).booleanValue());
                    break;
                case 6:
                    FingerprintDialogImpl.this.handleButtonNegative();
                    break;
                case 7:
                    FingerprintDialogImpl.this.handleUserCanceled();
                    break;
                case 8:
                    FingerprintDialogImpl.this.handleButtonPositive();
                    break;
                case 9:
                    FingerprintDialogImpl.this.handleClearMessage();
                    break;
            }
        }
    };
    private IBiometricPromptReceiver mReceiver;
    private WindowManager mWindowManager;

    @Override
    public void start() {
        if (!this.mContext.getPackageManager().hasSystemFeature("android.hardware.fingerprint")) {
            return;
        }
        ((CommandQueue) getComponent(CommandQueue.class)).addCallbacks(this);
        this.mWindowManager = (WindowManager) this.mContext.getSystemService("window");
        this.mDialogView = new FingerprintDialogView(this.mContext, this.mHandler);
    }

    @Override
    public void showFingerprintDialog(Bundle bundle, IBiometricPromptReceiver iBiometricPromptReceiver) {
        Log.d("FingerprintDialogImpl", "showFingerprintDialog");
        this.mHandler.removeMessages(4);
        this.mHandler.removeMessages(3);
        this.mHandler.removeMessages(2);
        SomeArgs someArgsObtain = SomeArgs.obtain();
        someArgsObtain.arg1 = bundle;
        someArgsObtain.arg2 = iBiometricPromptReceiver;
        this.mHandler.obtainMessage(1, someArgsObtain).sendToTarget();
    }

    @Override
    public void onFingerprintAuthenticated() {
        Log.d("FingerprintDialogImpl", "onFingerprintAuthenticated");
        this.mHandler.obtainMessage(2).sendToTarget();
    }

    @Override
    public void onFingerprintHelp(String str) {
        Log.d("FingerprintDialogImpl", "onFingerprintHelp: " + str);
        this.mHandler.obtainMessage(3, str).sendToTarget();
    }

    @Override
    public void onFingerprintError(String str) {
        Log.d("FingerprintDialogImpl", "onFingerprintError: " + str);
        this.mHandler.obtainMessage(4, str).sendToTarget();
    }

    @Override
    public void hideFingerprintDialog() {
        Log.d("FingerprintDialogImpl", "hideFingerprintDialog");
        this.mHandler.obtainMessage(5, false).sendToTarget();
    }

    private void handleShowDialog(SomeArgs someArgs) {
        Log.d("FingerprintDialogImpl", "handleShowDialog, isAnimatingAway: " + this.mDialogView.isAnimatingAway());
        if (this.mDialogView.isAnimatingAway()) {
            this.mDialogView.forceRemove();
        } else if (this.mDialogShowing) {
            Log.w("FingerprintDialogImpl", "Dialog already showing");
            return;
        }
        this.mReceiver = (IBiometricPromptReceiver) someArgs.arg2;
        this.mDialogView.setBundle((Bundle) someArgs.arg1);
        this.mWindowManager.addView(this.mDialogView, this.mDialogView.getLayoutParams());
        this.mDialogShowing = true;
    }

    private void handleFingerprintAuthenticated() {
        Log.d("FingerprintDialogImpl", "handleFingerprintAuthenticated");
        this.mDialogView.announceForAccessibility(this.mContext.getResources().getText(R.string.config_customAdbWifiNetworkConfirmationComponent));
        handleHideDialog(false);
    }

    private void handleFingerprintHelp(String str) {
        Log.d("FingerprintDialogImpl", "handleFingerprintHelp: " + str);
        this.mDialogView.showHelpMessage(str);
    }

    private void handleFingerprintError(String str) {
        Log.d("FingerprintDialogImpl", "handleFingerprintError: " + str);
        if (!this.mDialogShowing) {
            Log.d("FingerprintDialogImpl", "Dialog already dismissed");
        } else {
            this.mDialogView.showErrorMessage(str);
        }
    }

    private void handleHideDialog(boolean z) {
        Log.d("FingerprintDialogImpl", "handleHideDialog, userCanceled: " + z);
        if (!this.mDialogShowing) {
            Log.w("FingerprintDialogImpl", "Dialog already dismissed, userCanceled: " + z);
            return;
        }
        if (z) {
            try {
                this.mReceiver.onDialogDismissed(3);
            } catch (RemoteException e) {
                Log.e("FingerprintDialogImpl", "RemoteException when hiding dialog", e);
            }
        }
        this.mReceiver = null;
        this.mDialogShowing = false;
        this.mDialogView.startDismiss();
    }

    private void handleButtonNegative() {
        if (this.mReceiver == null) {
            Log.e("FingerprintDialogImpl", "Receiver is null");
            return;
        }
        try {
            this.mReceiver.onDialogDismissed(2);
        } catch (RemoteException e) {
            Log.e("FingerprintDialogImpl", "Remote exception when handling negative button", e);
        }
        handleHideDialog(false);
    }

    private void handleButtonPositive() {
        if (this.mReceiver == null) {
            Log.e("FingerprintDialogImpl", "Receiver is null");
            return;
        }
        try {
            this.mReceiver.onDialogDismissed(1);
        } catch (RemoteException e) {
            Log.e("FingerprintDialogImpl", "Remote exception when handling positive button", e);
        }
        handleHideDialog(false);
    }

    private void handleClearMessage() {
        this.mDialogView.resetMessage();
    }

    private void handleUserCanceled() {
        handleHideDialog(true);
    }
}
