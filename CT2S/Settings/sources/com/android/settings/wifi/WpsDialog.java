package com.android.settings.wifi;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.android.settings.R;
import java.util.Timer;
import java.util.TimerTask;

public class WpsDialog extends AlertDialog {
    private Button mButton;
    private Context mContext;
    DialogState mDialogState;
    private final IntentFilter mFilter;
    private Handler mHandler;
    private String mMsgString;
    private ProgressBar mProgressBar;
    private BroadcastReceiver mReceiver;
    private TextView mTextView;
    private ProgressBar mTimeoutBar;
    private Timer mTimer;
    private View mView;
    private WifiManager mWifiManager;
    private WifiManager.WpsCallback mWpsListener;
    private int mWpsSetup;

    private enum DialogState {
        WPS_INIT,
        WPS_START,
        WPS_COMPLETE,
        CONNECTED,
        WPS_FAILED
    }

    public WpsDialog(Context context, int wpsSetup) {
        super(context);
        this.mHandler = new Handler();
        this.mMsgString = "";
        this.mDialogState = DialogState.WPS_INIT;
        this.mContext = context;
        this.mWpsSetup = wpsSetup;
        this.mWpsListener = new WifiManager.WpsCallback() {
            @Override
            public void onStarted(String pin) {
                if (pin != null) {
                    WpsDialog.this.updateDialog(DialogState.WPS_START, String.format(WpsDialog.this.mContext.getString(R.string.wifi_wps_onstart_pin), pin));
                } else {
                    WpsDialog.this.updateDialog(DialogState.WPS_START, WpsDialog.this.mContext.getString(R.string.wifi_wps_onstart_pbc));
                }
            }

            @Override
            public void onSucceeded() {
                WpsDialog.this.updateDialog(DialogState.WPS_COMPLETE, WpsDialog.this.mContext.getString(R.string.wifi_wps_complete));
            }

            @Override
            public void onFailed(int reason) {
                String msg;
                switch (reason) {
                    case 1:
                        msg = WpsDialog.this.mContext.getString(R.string.wifi_wps_in_progress);
                        break;
                    case 2:
                    default:
                        msg = WpsDialog.this.mContext.getString(R.string.wifi_wps_failed_generic);
                        break;
                    case 3:
                        msg = WpsDialog.this.mContext.getString(R.string.wifi_wps_failed_overlap);
                        break;
                    case 4:
                        msg = WpsDialog.this.mContext.getString(R.string.wifi_wps_failed_wep);
                        break;
                    case 5:
                        msg = WpsDialog.this.mContext.getString(R.string.wifi_wps_failed_tkip);
                        break;
                }
                WpsDialog.this.updateDialog(DialogState.WPS_FAILED, msg);
            }
        };
        this.mFilter = new IntentFilter();
        this.mFilter.addAction("android.net.wifi.STATE_CHANGE");
        this.mFilter.addAction("android.net.wifi.CONFIGURED_NETWORKS_CHANGE");
        this.mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                WpsDialog.this.handleEvent(context2, intent);
            }
        };
        setCanceledOnTouchOutside(false);
    }

    @Override
    public Bundle onSaveInstanceState() {
        Bundle bundle = super.onSaveInstanceState();
        bundle.putString("android:dialogState", this.mDialogState.toString());
        bundle.putString("android:dialogMsg", this.mMsgString.toString());
        return bundle;
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            super.onRestoreInstanceState(savedInstanceState);
            DialogState dialogState = this.mDialogState;
            DialogState dialogState2 = DialogState.valueOf(savedInstanceState.getString("android:dialogState"));
            String msg = savedInstanceState.getString("android:dialogMsg");
            updateDialog(dialogState2, msg);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        this.mView = getLayoutInflater().inflate(R.layout.wifi_wps_dialog, (ViewGroup) null);
        this.mTextView = (TextView) this.mView.findViewById(R.id.wps_dialog_txt);
        this.mTextView.setText(R.string.wifi_wps_setup_msg);
        this.mTimeoutBar = (ProgressBar) this.mView.findViewById(R.id.wps_timeout_bar);
        this.mTimeoutBar.setMax(120);
        this.mTimeoutBar.setProgress(0);
        this.mProgressBar = (ProgressBar) this.mView.findViewById(R.id.wps_progress_bar);
        this.mProgressBar.setVisibility(8);
        this.mButton = (Button) this.mView.findViewById(R.id.wps_dialog_btn);
        this.mButton.setText(R.string.wifi_cancel);
        this.mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WpsDialog.this.dismiss();
            }
        });
        this.mWifiManager = (WifiManager) this.mContext.getSystemService("wifi");
        setView(this.mView);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onStart() {
        this.mTimer = new Timer(false);
        this.mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                WpsDialog.this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        WpsDialog.this.mTimeoutBar.incrementProgressBy(1);
                    }
                });
            }
        }, 1000L, 1000L);
        this.mContext.registerReceiver(this.mReceiver, this.mFilter);
        WpsInfo wpsConfig = new WpsInfo();
        wpsConfig.setup = this.mWpsSetup;
        this.mWifiManager.startWps(wpsConfig, this.mWpsListener);
    }

    @Override
    protected void onStop() {
        if (this.mDialogState != DialogState.WPS_COMPLETE) {
            this.mWifiManager.cancelWps(null);
        }
        if (this.mReceiver != null) {
            this.mContext.unregisterReceiver(this.mReceiver);
            this.mReceiver = null;
        }
        if (this.mTimer != null) {
            this.mTimer.cancel();
        }
    }

    private void updateDialog(final DialogState state, final String msg) {
        if (this.mDialogState.ordinal() < state.ordinal()) {
            this.mDialogState = state;
            this.mMsgString = msg;
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    switch (AnonymousClass5.$SwitchMap$com$android$settings$wifi$WpsDialog$DialogState[state.ordinal()]) {
                        case 1:
                            WpsDialog.this.mTimeoutBar.setVisibility(8);
                            WpsDialog.this.mProgressBar.setVisibility(0);
                            break;
                        case 2:
                        case 3:
                            WpsDialog.this.mButton.setText(WpsDialog.this.mContext.getString(R.string.dlg_ok));
                            WpsDialog.this.mTimeoutBar.setVisibility(8);
                            WpsDialog.this.mProgressBar.setVisibility(8);
                            if (WpsDialog.this.mReceiver != null) {
                                WpsDialog.this.mContext.unregisterReceiver(WpsDialog.this.mReceiver);
                                WpsDialog.this.mReceiver = null;
                            }
                            break;
                    }
                    WpsDialog.this.mTextView.setText(msg);
                }
            });
        }
    }

    static class AnonymousClass5 {
        static final int[] $SwitchMap$com$android$settings$wifi$WpsDialog$DialogState = new int[DialogState.values().length];

        static {
            try {
                $SwitchMap$com$android$settings$wifi$WpsDialog$DialogState[DialogState.WPS_COMPLETE.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$settings$wifi$WpsDialog$DialogState[DialogState.CONNECTED.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$android$settings$wifi$WpsDialog$DialogState[DialogState.WPS_FAILED.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
        }
    }

    private void handleEvent(Context context, Intent intent) {
        android.net.wifi.WifiInfo wifiInfo;
        String action = intent.getAction();
        if ("android.net.wifi.STATE_CHANGE".equals(action)) {
            NetworkInfo info = (NetworkInfo) intent.getParcelableExtra("networkInfo");
            NetworkInfo.DetailedState state = info.getDetailedState();
            if (state == NetworkInfo.DetailedState.CONNECTED && this.mDialogState == DialogState.WPS_COMPLETE && (wifiInfo = this.mWifiManager.getConnectionInfo()) != null) {
                String msg = String.format(this.mContext.getString(R.string.wifi_wps_connected), wifiInfo.getSSID());
                updateDialog(DialogState.CONNECTED, msg);
                return;
            }
            return;
        }
        if ("android.net.wifi.CONFIGURED_NETWORKS_CHANGE".equals(action)) {
            WifiConfiguration network = (WifiConfiguration) intent.getParcelableExtra("wifiConfiguration");
            int reason = intent.getIntExtra("changeReason", -1);
            if (reason == 2 && network != null && network.status == 1 && network.disableReason == 3) {
                Log.w("WpsDialog", "WPS completed but failed to connect! SSID: " + network.SSID);
                Intent newIntent = new Intent("com.android.settings.wifi.WpsDialog.WpaReconnect");
                newIntent.addFlags(1073741824);
                newIntent.putExtra("wifiConfiguration", network);
                this.mContext.sendStickyBroadcast(newIntent);
                Toast.makeText(this.mContext, String.format(this.mContext.getString(R.string.wifi_wps_fail_to_connect), network.SSID), 1).show();
                dismiss();
            }
        }
    }
}
