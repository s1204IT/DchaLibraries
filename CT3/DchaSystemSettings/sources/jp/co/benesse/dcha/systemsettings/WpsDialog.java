package jp.co.benesse.dcha.systemsettings;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import java.util.Timer;
import java.util.TimerTask;
import jp.co.benesse.dcha.util.Logger;

public class WpsDialog extends AlertDialog implements View.OnClickListener {
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
        WPS_FAILED;

        public static DialogState[] valuesCustom() {
            return values();
        }
    }

    public WpsDialog(Context context, int wpsSetup) {
        super(context);
        this.mHandler = new Handler();
        this.mMsgString = "";
        this.mDialogState = DialogState.WPS_INIT;
        Logger.d("WpsDialog", "WpsDialog 0001");
        this.mContext = context;
        this.mWpsSetup = wpsSetup;
        this.mWpsListener = new WifiManager.WpsCallback() {
            @Override
            public void onStarted(String pin) {
                Logger.d("WpsDialog", "onStartSuccess 0001");
                if (pin != null) {
                    Logger.d("WpsDialog", "onStartSuccess 0002");
                    WpsDialog.this.updateDialog(DialogState.WPS_START, String.format(WpsDialog.this.mContext.getString(R.string.wifi_wps_onstart_pin), pin));
                } else {
                    Logger.d("WpsDialog", "onStartSuccess 0003");
                    WpsDialog.this.updateDialog(DialogState.WPS_START, WpsDialog.this.mContext.getString(R.string.wifi_wps_onstart_pbc));
                }
                Logger.d("WpsDialog", "onStartSuccess 0004");
            }

            @Override
            public void onSucceeded() {
                Logger.d("WpsDialog", "onCompletion 0001");
                WpsDialog.this.updateDialog(DialogState.WPS_COMPLETE, WpsDialog.this.mContext.getString(R.string.wifi_wps_complete));
                Logger.d("WpsDialog", "onCompletion 0002");
            }

            @Override
            public void onFailed(int reason) {
                String msg;
                Logger.d("WpsDialog", "onFailure 0001");
                switch (reason) {
                    case 1:
                        Logger.d("WpsDialog", "onFailure 0005");
                        msg = WpsDialog.this.mContext.getString(R.string.wifi_wps_in_progress);
                        break;
                    case 2:
                    default:
                        Logger.d("WpsDialog", "onFailure 0006");
                        msg = WpsDialog.this.mContext.getString(R.string.wifi_wps_failed_generic);
                        break;
                    case 3:
                        Logger.d("WpsDialog", "onFailure 0002");
                        msg = WpsDialog.this.mContext.getString(R.string.wifi_wps_failed_overlap);
                        break;
                    case 4:
                        Logger.d("WpsDialog", "onFailure 0003");
                        msg = WpsDialog.this.mContext.getString(R.string.wifi_wps_failed_wep);
                        break;
                    case 5:
                        Logger.d("WpsDialog", "onFailure 0004");
                        msg = WpsDialog.this.mContext.getString(R.string.wifi_wps_failed_tkip);
                        break;
                }
                WpsDialog.this.updateDialog(DialogState.WPS_FAILED, msg);
                Logger.d("WpsDialog", "onFailure 0007");
            }
        };
        this.mFilter = new IntentFilter();
        this.mFilter.addAction("android.net.wifi.STATE_CHANGE");
        this.mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                Logger.d("WpsDialog", "onReceive 0001");
                WpsDialog.this.handleEvent(context2, intent);
                Logger.d("WpsDialog", "onReceive 0002");
            }
        };
        setCanceledOnTouchOutside(false);
        Logger.d("WpsDialog", "WpsDialog 0002");
    }

    @Override
    public Bundle onSaveInstanceState() {
        Logger.d("WpsDialog", "onSaveInstanceState 0001");
        Bundle bundle = super.onSaveInstanceState();
        bundle.putString("android:dialogState", this.mDialogState.toString());
        bundle.putString("android:dialogMsg", this.mMsgString);
        Logger.d("WpsDialog", "onSaveInstanceState 0002");
        return bundle;
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        Logger.d("WpsDialog", "onRestoreInstanceState 0001");
        if (savedInstanceState != null) {
            Logger.d("WpsDialog", "onRestoreInstanceState 0002");
            super.onRestoreInstanceState(savedInstanceState);
            DialogState dialogState = DialogState.valueOf(savedInstanceState.getString("android:dialogState"));
            String msg = savedInstanceState.getString("android:dialogMsg");
            updateDialog(dialogState, msg);
        }
        Logger.d("WpsDialog", "onRestoreInstanceState 0003");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Logger.d("WpsDialog", "onCreate 0001");
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
        setView(this.mView);
        this.mButton.setOnClickListener(this);
        this.mWifiManager = (WifiManager) this.mContext.getSystemService("wifi");
        if (savedInstanceState == null) {
            Logger.d("WpsDialog", "onCreate 0002");
            WpsInfo wpsConfig = new WpsInfo();
            wpsConfig.setup = this.mWpsSetup;
            this.mWifiManager.startWps(wpsConfig, this.mWpsListener);
        }
        super.onCreate(savedInstanceState);
        Logger.d("WpsDialog", "onCreate 0003");
    }

    @Override
    protected void onStart() {
        Logger.d("WpsDialog", "onStart 0001");
        this.mTimer = new Timer(false);
        this.mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                Logger.d("WpsDialog", "run 0001");
                WpsDialog.this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Logger.d("WpsDialog", "run 0002");
                        WpsDialog.this.mTimeoutBar.incrementProgressBy(1);
                        Logger.d("WpsDialog", "run 0003");
                    }
                });
                Logger.d("WpsDialog", "run 0004");
            }
        }, 1000L, 1000L);
        this.mContext.registerReceiver(this.mReceiver, this.mFilter);
        Logger.d("WpsDialog", "onStart 0002");
    }

    @Override
    protected void onStop() {
        Logger.d("WpsDialog", "onStop 0001");
        if (this.mDialogState != DialogState.WPS_COMPLETE) {
            Logger.d("WpsDialog", "onStop 0002");
            this.mWifiManager.cancelWps(null);
        }
        if (this.mReceiver != null) {
            Logger.d("WpsDialog", "onStop 0003");
            this.mContext.unregisterReceiver(this.mReceiver);
            this.mReceiver = null;
        }
        if (this.mTimer != null) {
            Logger.d("WpsDialog", "onStop 0004");
            this.mTimer.cancel();
        }
        Logger.d("WpsDialog", "onStop 0005");
    }

    public void updateDialog(final DialogState state, final String msg) {
        Logger.d("WpsDialog", "updateDialog 0001");
        if (this.mDialogState.ordinal() >= state.ordinal()) {
            Logger.d("WpsDialog", "updateDialog 0002");
            return;
        }
        this.mDialogState = state;
        this.mMsgString = msg;
        this.mHandler.post(new Runnable() {

            private static final int[] f0x31dcdfdc = null;

            private static int[] m44x17bc580() {
                if (f0x31dcdfdc != null) {
                    return f0x31dcdfdc;
                }
                int[] iArr = new int[DialogState.valuesCustom().length];
                try {
                    iArr[DialogState.CONNECTED.ordinal()] = 1;
                } catch (NoSuchFieldError e) {
                }
                try {
                    iArr[DialogState.WPS_COMPLETE.ordinal()] = 2;
                } catch (NoSuchFieldError e2) {
                }
                try {
                    iArr[DialogState.WPS_FAILED.ordinal()] = 3;
                } catch (NoSuchFieldError e3) {
                }
                try {
                    iArr[DialogState.WPS_INIT.ordinal()] = 4;
                } catch (NoSuchFieldError e4) {
                }
                try {
                    iArr[DialogState.WPS_START.ordinal()] = 5;
                } catch (NoSuchFieldError e5) {
                }
                f0x31dcdfdc = iArr;
                return iArr;
            }

            @Override
            public void run() {
                Logger.d("WpsDialog", "run 0005");
                switch (m44x17bc580()[state.ordinal()]) {
                    case 1:
                        Logger.d("WpsDialog", "run 0007");
                        Logger.d("WpsDialog", "run 0008");
                        WpsDialog.this.mButton.setText(WpsDialog.this.mContext.getString(R.string.dlg_ok));
                        WpsDialog.this.mTimeoutBar.setVisibility(8);
                        WpsDialog.this.mProgressBar.setVisibility(8);
                        if (WpsDialog.this.mReceiver != null) {
                            Logger.d("WpsDialog", "run 0009");
                            WpsDialog.this.mContext.unregisterReceiver(WpsDialog.this.mReceiver);
                            WpsDialog.this.mReceiver = null;
                        }
                        break;
                    case 2:
                        Logger.d("WpsDialog", "run 0006");
                        WpsDialog.this.mTimeoutBar.setVisibility(8);
                        WpsDialog.this.mProgressBar.setVisibility(0);
                        break;
                    case 3:
                        Logger.d("WpsDialog", "run 0008");
                        WpsDialog.this.mButton.setText(WpsDialog.this.mContext.getString(R.string.dlg_ok));
                        WpsDialog.this.mTimeoutBar.setVisibility(8);
                        WpsDialog.this.mProgressBar.setVisibility(8);
                        if (WpsDialog.this.mReceiver != null) {
                        }
                        break;
                }
                WpsDialog.this.mTextView.setText(msg);
                Logger.d("WpsDialog", "run 0010");
            }
        });
        Logger.d("WpsDialog", "updateDialog 0003");
    }

    public void handleEvent(Context context, Intent intent) {
        Logger.d("WpsDialog", "handleEvent 0001");
        String action = intent.getAction();
        if ("android.net.wifi.STATE_CHANGE".equals(action)) {
            Logger.d("WpsDialog", "handleEvent 0002");
            NetworkInfo info = (NetworkInfo) intent.getParcelableExtra("networkInfo");
            NetworkInfo.DetailedState state = info.getDetailedState();
            if (state == NetworkInfo.DetailedState.CONNECTED && this.mDialogState == DialogState.WPS_COMPLETE) {
                Logger.d("WpsDialog", "handleEvent 0003");
                WifiInfo wifiInfo = this.mWifiManager.getConnectionInfo();
                if (wifiInfo != null) {
                    Logger.d("WpsDialog", "handleEvent 0004");
                    String msg = String.format(this.mContext.getString(R.string.wifi_wps_connected), wifiInfo.getSSID());
                    updateDialog(DialogState.CONNECTED, msg);
                }
            }
        }
        Logger.d("WpsDialog", "handleEvent 0005");
    }

    @Override
    public void onClick(View v) {
        Logger.d("WpsDialog", "onClick 0001");
        if (v.getId() == this.mButton.getId()) {
            Logger.d("WpsDialog", "onClick 0002");
            dismiss();
        }
        Logger.d("WpsDialog", "onClick 0003");
    }
}
