package com.android.settings.wifi;

import android.content.Context;
import android.net.NetworkInfo;
import com.android.settings.R;

class Summary {
    static String get(Context context, String ssid, NetworkInfo.DetailedState state, boolean isEphemeral) {
        if (state == NetworkInfo.DetailedState.CONNECTED && isEphemeral && ssid == null) {
            return context.getString(R.string.connected_via_wfa);
        }
        String[] formats = context.getResources().getStringArray(ssid == null ? R.array.wifi_status : R.array.wifi_status_with_ssid);
        int index = state.ordinal();
        if (index >= formats.length || formats[index].length() == 0) {
            return null;
        }
        return String.format(formats[index], ssid);
    }

    static String get(Context context, NetworkInfo.DetailedState state, boolean isEphemeral) {
        return get(context, null, state, isEphemeral);
    }
}
