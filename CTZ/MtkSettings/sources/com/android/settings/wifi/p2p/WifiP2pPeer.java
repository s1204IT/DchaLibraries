package com.android.settings.wifi.p2p;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.text.TextUtils;
import android.widget.ImageView;
import com.android.settings.R;

public class WifiP2pPeer extends Preference {
    private static final int[] STATE_SECURED = {R.attr.state_encrypted};
    public WifiP2pDevice device;
    private final int mRssi;
    private ImageView mSignal;

    public WifiP2pPeer(Context context, WifiP2pDevice wifiP2pDevice) {
        super(context);
        this.device = wifiP2pDevice;
        setWidgetLayoutResource(R.layout.preference_widget_wifi_signal);
        this.mRssi = 60;
        if (TextUtils.isEmpty(this.device.deviceName)) {
            setTitle(this.device.deviceAddress);
        } else {
            setTitle(this.device.deviceName);
        }
        setSummary(context.getResources().getStringArray(R.array.wifi_p2p_status)[this.device.status]);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder preferenceViewHolder) {
        super.onBindViewHolder(preferenceViewHolder);
        this.mSignal = (ImageView) preferenceViewHolder.findViewById(R.id.signal);
        if (this.mRssi == Integer.MAX_VALUE) {
            this.mSignal.setImageDrawable(null);
        } else {
            this.mSignal.setImageResource(R.drawable.wifi_signal_dark);
            this.mSignal.setImageState(STATE_SECURED, true);
        }
        this.mSignal.setImageLevel(getLevel());
    }

    @Override
    public int compareTo(Preference preference) {
        if (!(preference instanceof WifiP2pPeer)) {
            return 1;
        }
        WifiP2pPeer wifiP2pPeer = (WifiP2pPeer) preference;
        if (this.device.status != wifiP2pPeer.device.status) {
            return this.device.status < wifiP2pPeer.device.status ? -1 : 1;
        }
        if (this.device.deviceName != null) {
            return this.device.deviceName.compareToIgnoreCase(wifiP2pPeer.device.deviceName);
        }
        return this.device.deviceAddress.compareToIgnoreCase(wifiP2pPeer.device.deviceAddress);
    }

    int getLevel() {
        if (this.mRssi == Integer.MAX_VALUE) {
            return -1;
        }
        return WifiManager.calculateSignalLevel(this.mRssi, 4);
    }
}
