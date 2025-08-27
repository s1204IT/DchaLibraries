package com.android.settings.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;
import com.android.internal.app.AlertActivity;
import com.android.settings.R;

/* loaded from: classes.dex */
public class RequestToggleWiFiActivity extends AlertActivity implements DialogInterface.OnClickListener {
    private CharSequence mAppLabel;
    private WifiManager mWiFiManager;
    private final StateChangeReceiver mReceiver = new StateChangeReceiver();
    private final Runnable mTimeoutCommand = new Runnable() { // from class: com.android.settings.wifi.-$$Lambda$RequestToggleWiFiActivity$PwZgoHTFFBr3iYEQbWj0vZPfHpw
        @Override // java.lang.Runnable
        public final void run() {
            RequestToggleWiFiActivity.lambda$new$0(this.f$0);
        }
    };
    private int mState = -1;
    private int mLastUpdateState = -1;

    public static /* synthetic */ void lambda$new$0(RequestToggleWiFiActivity requestToggleWiFiActivity) {
        if (!requestToggleWiFiActivity.isFinishing() && !requestToggleWiFiActivity.isDestroyed()) {
            requestToggleWiFiActivity.finish();
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:18:0x0064  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mWiFiManager = (WifiManager) getSystemService(WifiManager.class);
        boolean z = false;
        setResult(0);
        String stringExtra = getIntent().getStringExtra("android.intent.extra.PACKAGE_NAME");
        if (TextUtils.isEmpty(stringExtra)) {
            finish();
        }
        try {
            this.mAppLabel = getPackageManager().getApplicationInfo(stringExtra, 0).loadSafeLabel(getPackageManager());
            String action = getIntent().getAction();
            int iHashCode = action.hashCode();
            if (iHashCode != -2035256254) {
                z = (iHashCode == 317500393 && action.equals("android.net.wifi.action.REQUEST_DISABLE")) ? true : -1;
            } else if (!action.equals("android.net.wifi.action.REQUEST_ENABLE")) {
            }
            switch (z) {
                case false:
                    this.mState = 1;
                    break;
                case true:
                    this.mState = 3;
                    break;
                default:
                    finish();
                    break;
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("RequestToggleWiFiActivity", "Couldn't find app with package name " + stringExtra);
            finish();
        }
    }

    @Override // android.content.DialogInterface.OnClickListener
    public void onClick(DialogInterface dialogInterface, int i) {
        switch (i) {
            case -2:
                finish();
                break;
            case -1:
                int i2 = this.mState;
                if (i2 == 1) {
                    this.mWiFiManager.setWifiEnabled(true);
                    this.mState = 2;
                    scheduleToggleTimeout();
                    updateUi();
                    break;
                } else if (i2 == 3) {
                    this.mWiFiManager.setWifiEnabled(false);
                    this.mState = 4;
                    scheduleToggleTimeout();
                    updateUi();
                    break;
                }
                break;
        }
    }

    protected void onStart() {
        super.onStart();
        this.mReceiver.register();
        int wifiState = this.mWiFiManager.getWifiState();
        switch (this.mState) {
            case 1:
                switch (wifiState) {
                    case 2:
                        this.mState = 2;
                        scheduleToggleTimeout();
                        break;
                    case 3:
                        setResult(-1);
                        finish();
                        return;
                }
            case 2:
                switch (wifiState) {
                    case 0:
                    case 1:
                        this.mState = 1;
                        break;
                    case 2:
                        scheduleToggleTimeout();
                        break;
                    case 3:
                        setResult(-1);
                        finish();
                        return;
                }
            case 3:
                switch (wifiState) {
                    case 1:
                        setResult(-1);
                        finish();
                        return;
                    case 2:
                        this.mState = 4;
                        scheduleToggleTimeout();
                        break;
                }
            case 4:
                switch (wifiState) {
                    case 0:
                        scheduleToggleTimeout();
                        break;
                    case 1:
                        setResult(-1);
                        finish();
                        return;
                    case 2:
                    case 3:
                        this.mState = 3;
                        break;
                }
        }
        updateUi();
    }

    protected void onStop() {
        this.mReceiver.unregister();
        unscheduleToggleTimeout();
        super.onStop();
    }

    private void updateUi() {
        if (this.mLastUpdateState == this.mState) {
            return;
        }
        this.mLastUpdateState = this.mState;
        switch (this.mState) {
            case 1:
                this.mAlertParams.mPositiveButtonText = getString(R.string.allow);
                this.mAlertParams.mPositiveButtonListener = this;
                this.mAlertParams.mNegativeButtonText = getString(R.string.deny);
                this.mAlertParams.mNegativeButtonListener = this;
                this.mAlertParams.mMessage = getString(R.string.wifi_ask_enable, new Object[]{this.mAppLabel});
                break;
            case 2:
                this.mAlert.setButton(-1, (CharSequence) null, (DialogInterface.OnClickListener) null, (Message) null);
                this.mAlert.setButton(-2, (CharSequence) null, (DialogInterface.OnClickListener) null, (Message) null);
                this.mAlertParams.mPositiveButtonText = null;
                this.mAlertParams.mPositiveButtonListener = null;
                this.mAlertParams.mNegativeButtonText = null;
                this.mAlertParams.mNegativeButtonListener = null;
                this.mAlertParams.mMessage = getString(R.string.wifi_starting);
                break;
            case 3:
                this.mAlertParams.mPositiveButtonText = getString(R.string.allow);
                this.mAlertParams.mPositiveButtonListener = this;
                this.mAlertParams.mNegativeButtonText = getString(R.string.deny);
                this.mAlertParams.mNegativeButtonListener = this;
                this.mAlertParams.mMessage = getString(R.string.wifi_ask_disable, new Object[]{this.mAppLabel});
                break;
            case 4:
                this.mAlert.setButton(-1, (CharSequence) null, (DialogInterface.OnClickListener) null, (Message) null);
                this.mAlert.setButton(-2, (CharSequence) null, (DialogInterface.OnClickListener) null, (Message) null);
                this.mAlertParams.mPositiveButtonText = null;
                this.mAlertParams.mPositiveButtonListener = null;
                this.mAlertParams.mNegativeButtonText = null;
                this.mAlertParams.mNegativeButtonListener = null;
                this.mAlertParams.mMessage = getString(R.string.wifi_stopping);
                break;
        }
        setupAlert();
    }

    public void dismiss() {
    }

    private void scheduleToggleTimeout() {
        getWindow().getDecorView().postDelayed(this.mTimeoutCommand, 10000L);
    }

    private void unscheduleToggleTimeout() {
        getWindow().getDecorView().removeCallbacks(this.mTimeoutCommand);
    }

    private final class StateChangeReceiver extends BroadcastReceiver {
        private final IntentFilter mFilter;

        private StateChangeReceiver() {
            this.mFilter = new IntentFilter("android.net.wifi.WIFI_STATE_CHANGED");
        }

        public void register() {
            RequestToggleWiFiActivity.this.registerReceiver(this, this.mFilter);
        }

        public void unregister() {
            RequestToggleWiFiActivity.this.unregisterReceiver(this);
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            AlertActivity alertActivity = RequestToggleWiFiActivity.this;
            if (!alertActivity.isFinishing() && !alertActivity.isDestroyed()) {
                int wifiState = RequestToggleWiFiActivity.this.mWiFiManager.getWifiState();
                Log.d("RequestToggleWiFiActivity", " currentState=" + wifiState);
                if (wifiState != 1) {
                    switch (wifiState) {
                        case 4:
                            Toast.makeText((Context) alertActivity, R.string.wifi_error, 0).show();
                            RequestToggleWiFiActivity.this.finish();
                            break;
                    }
                }
                if (RequestToggleWiFiActivity.this.mState == 2 || RequestToggleWiFiActivity.this.mState == 4) {
                    RequestToggleWiFiActivity.this.setResult(-1);
                    RequestToggleWiFiActivity.this.finish();
                }
            }
        }
    }
}
