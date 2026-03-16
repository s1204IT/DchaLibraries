package com.android.vpndialogs;

import android.content.DialogInterface;
import android.net.IConnectivityManager;
import android.os.Handler;
import android.os.Message;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import com.android.internal.app.AlertActivity;
import com.android.internal.net.VpnConfig;
import java.io.DataInputStream;
import java.io.FileInputStream;

public class ManageDialog extends AlertActivity implements DialogInterface.OnClickListener, Handler.Callback {
    private VpnConfig mConfig;
    private TextView mDataReceived;
    private boolean mDataRowsHidden;
    private TextView mDataTransmitted;
    private TextView mDuration;
    private Handler mHandler;
    private IConnectivityManager mService;

    protected void onResume() {
        super.onResume();
        if (getCallingPackage() != null) {
            Log.e("VpnManage", getCallingPackage() + " cannot start this activity");
            finish();
            return;
        }
        try {
            this.mService = IConnectivityManager.Stub.asInterface(ServiceManager.getService("connectivity"));
            this.mConfig = this.mService.getVpnConfig();
            if (this.mConfig == null) {
                finish();
                return;
            }
            View view = View.inflate(this, R.layout.manage, null);
            if (this.mConfig.session != null) {
                ((TextView) view.findViewById(R.id.session)).setText(this.mConfig.session);
            }
            this.mDuration = (TextView) view.findViewById(R.id.duration);
            this.mDataTransmitted = (TextView) view.findViewById(R.id.data_transmitted);
            this.mDataReceived = (TextView) view.findViewById(R.id.data_received);
            this.mDataRowsHidden = true;
            if (this.mConfig.legacy) {
                this.mAlertParams.mTitle = getText(R.string.legacy_title);
            } else {
                this.mAlertParams.mTitle = VpnConfig.getVpnLabel(this, this.mConfig.user);
            }
            if (this.mConfig.configureIntent != null) {
                this.mAlertParams.mPositiveButtonText = getText(R.string.configure);
                this.mAlertParams.mPositiveButtonListener = this;
            }
            this.mAlertParams.mNeutralButtonText = getText(R.string.disconnect);
            this.mAlertParams.mNeutralButtonListener = this;
            this.mAlertParams.mNegativeButtonText = getText(android.R.string.cancel);
            this.mAlertParams.mNegativeButtonListener = this;
            this.mAlertParams.mView = view;
            setupAlert();
            if (this.mHandler == null) {
                this.mHandler = new Handler(this);
            }
            this.mHandler.sendEmptyMessage(0);
        } catch (Exception e) {
            Log.e("VpnManage", "onResume", e);
            finish();
        }
    }

    protected void onPause() {
        super.onPause();
        if (!isFinishing()) {
            finish();
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        try {
            if (which == -1) {
                this.mConfig.configureIntent.send();
            } else if (which == -3) {
                if (this.mConfig.legacy) {
                    this.mService.prepareVpn("[Legacy VPN]", "[Legacy VPN]");
                } else {
                    this.mService.prepareVpn(this.mConfig.user, "[Legacy VPN]");
                }
            }
        } catch (Exception e) {
            Log.e("VpnManage", "onClick", e);
            finish();
        }
    }

    @Override
    public boolean handleMessage(Message message) throws Throwable {
        this.mHandler.removeMessages(0);
        if (!isFinishing()) {
            if (this.mConfig.startTime != -1) {
                long seconds = (SystemClock.elapsedRealtime() - this.mConfig.startTime) / 1000;
                this.mDuration.setText(String.format("%02d:%02d:%02d", Long.valueOf(seconds / 3600), Long.valueOf((seconds / 60) % 60), Long.valueOf(seconds % 60)));
            }
            String[] numbers = getNumbers();
            if (numbers != null) {
                if (this.mDataRowsHidden) {
                    findViewById(R.id.data_transmitted_row).setVisibility(0);
                    findViewById(R.id.data_received_row).setVisibility(0);
                    this.mDataRowsHidden = false;
                }
                this.mDataReceived.setText(getString(R.string.data_value_format, new Object[]{numbers[1], numbers[2]}));
                this.mDataTransmitted.setText(getString(R.string.data_value_format, new Object[]{numbers[9], numbers[10]}));
            }
            this.mHandler.sendEmptyMessageDelayed(0, 1000L);
        }
        return true;
    }

    private String[] getNumbers() throws Throwable {
        String line;
        DataInputStream in = null;
        try {
            DataInputStream in2 = new DataInputStream(new FileInputStream("/proc/net/dev"));
            try {
                String prefix = this.mConfig.interfaze + ':';
                do {
                    line = in2.readLine().trim();
                } while (!line.startsWith(prefix));
                String[] numbers = line.substring(prefix.length()).split(" +");
                for (int i = 1; i < 17; i++) {
                    if (!numbers[i].equals("0")) {
                        try {
                            in2.close();
                        } catch (Exception e) {
                        }
                        return numbers;
                    }
                }
                try {
                    in2.close();
                } catch (Exception e2) {
                }
            } catch (Exception e3) {
                in = in2;
                try {
                    in.close();
                } catch (Exception e4) {
                }
            } catch (Throwable th) {
                th = th;
                in = in2;
                try {
                    in.close();
                } catch (Exception e5) {
                }
                throw th;
            }
        } catch (Exception e6) {
        } catch (Throwable th2) {
            th = th2;
        }
        return null;
    }
}
