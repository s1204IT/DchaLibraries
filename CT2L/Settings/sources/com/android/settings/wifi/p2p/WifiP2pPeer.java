package com.android.settings.wifi.p2p;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.preference.Preference;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import com.android.settings.R;

public class WifiP2pPeer extends Preference {
    private static final int[] STATE_SECURED = {R.attr.state_encrypted};
    public WifiP2pDevice device;
    private final int mRssi;
    private ImageView mSignal;

    public WifiP2pPeer(Context context, WifiP2pDevice dev) {
        super(context);
        this.device = dev;
        setWidgetLayoutResource(R.layout.preference_widget_wifi_signal);
        this.mRssi = 60;
    }

    @Override
    protected void onBindView(View view) {
        if (TextUtils.isEmpty(this.device.deviceName)) {
            setTitle(this.device.deviceAddress);
        } else {
            setTitle(this.device.deviceName);
        }
        this.mSignal = (ImageView) view.findViewById(R.id.signal);
        if (this.mRssi == Integer.MAX_VALUE) {
            this.mSignal.setImageDrawable(null);
        } else {
            this.mSignal.setImageResource(R.drawable.wifi_signal_dark);
            this.mSignal.setImageState(STATE_SECURED, true);
        }
        refresh();
        super.onBindView(view);
    }

    @Override
    public int compareTo(Preference preference) {
        if (!(preference instanceof WifiP2pPeer)) {
            return 1;
        }
        WifiP2pPeer other = (WifiP2pPeer) preference;
        if (this.device.status != other.device.status) {
            return this.device.status < other.device.status ? -1 : 1;
        }
        if (this.device.deviceName != null) {
            return this.device.deviceName.compareToIgnoreCase(other.device.deviceName);
        }
        return this.device.deviceAddress.compareToIgnoreCase(other.device.deviceAddress);
    }

    int getLevel() {
        if (this.mRssi == Integer.MAX_VALUE) {
            return -1;
        }
        return WifiManager.calculateSignalLevel(this.mRssi, 4);
    }

    private void refresh() {
        if (this.mSignal != null) {
            Context context = getContext();
            this.mSignal.setImageLevel(getLevel());
            String[] statusArray = context.getResources().getStringArray(R.array.wifi_p2p_status);
            setSummary(statusArray[this.device.status]);
        }
    }
}
