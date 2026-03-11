package jp.co.benesse.dcha.systemsettings;

import android.content.Context;
import android.net.NetworkInfo;
import jp.co.benesse.dcha.util.Logger;

class Summary {
    static String get(Context context, String ssid, NetworkInfo.DetailedState state) {
        Logger.d("Summary", "get 0001");
        String[] formats = context.getResources().getStringArray(ssid == null ? R.array.wifi_status : R.array.wifi_status_with_ssid);
        int index = state.ordinal();
        if (index >= formats.length || formats[index].length() == 0) {
            Logger.d("Summary", "get 0002");
            return null;
        }
        Logger.d("Summary", "get 0003");
        return String.format(formats[index], ssid);
    }

    static String get(Context context, NetworkInfo.DetailedState state) {
        Logger.d("Summary", "get 0004");
        return get(context, null, state);
    }
}
