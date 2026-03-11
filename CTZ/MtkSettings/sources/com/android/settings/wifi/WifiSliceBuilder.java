package com.android.settings.wifi;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.support.v4.graphics.drawable.IconCompat;
import android.support.v4.util.Consumer;
import android.text.TextUtils;
import androidx.slice.Slice;
import androidx.slice.builders.ListBuilder;
import androidx.slice.builders.SliceAction;
import com.android.settings.R;
import com.android.settings.SubSettings;
import com.android.settings.Utils;
import com.android.settings.search.DatabaseIndexingUtils;
import com.android.settings.slices.SliceBroadcastReceiver;

public class WifiSliceBuilder {
    public static final Uri WIFI_URI = new Uri.Builder().scheme("content").authority("android.settings.slices").appendPath("action").appendPath("wifi").build();
    public static final IntentFilter INTENT_FILTER = new IntentFilter();

    static {
        INTENT_FILTER.addAction("android.net.wifi.WIFI_STATE_CHANGED");
        INTENT_FILTER.addAction("android.net.wifi.STATE_CHANGE");
    }

    public static Slice getSlice(Context context) {
        boolean zIsWifiEnabled = isWifiEnabled(context);
        IconCompat iconCompatCreateWithResource = IconCompat.createWithResource(context, R.drawable.ic_settings_wireless);
        final String string = context.getString(R.string.wifi_settings);
        final CharSequence summary = getSummary(context);
        int colorAccent = Utils.getColorAccent(context);
        PendingIntent broadcastIntent = getBroadcastIntent(context);
        final SliceAction sliceAction = new SliceAction(getPrimaryAction(context), iconCompatCreateWithResource, string);
        final SliceAction sliceAction2 = new SliceAction(broadcastIntent, (CharSequence) null, zIsWifiEnabled);
        return new ListBuilder(context, WIFI_URI, -1L).setAccentColor(colorAccent).addRow(new Consumer() {
            @Override
            public final void accept(Object obj) {
                String str = string;
                CharSequence charSequence = summary;
                ((ListBuilder.RowBuilder) obj).setTitle(str).setSubtitle(charSequence).addEndItem(sliceAction2).setPrimaryAction(sliceAction);
            }
        }).build();
    }

    public static void handleUriChange(Context context, Intent intent) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(WifiManager.class);
        wifiManager.setWifiEnabled(intent.getBooleanExtra("android.app.slice.extra.TOGGLE_STATE", wifiManager.isWifiEnabled()));
    }

    public static Intent getIntent(Context context) {
        String string = context.getText(R.string.wifi_settings).toString();
        return DatabaseIndexingUtils.buildSearchResultPageIntent(context, WifiSettings.class.getName(), "wifi", string, 603).setClassName(context.getPackageName(), SubSettings.class.getName()).setData(new Uri.Builder().appendPath("wifi").build());
    }

    private static boolean isWifiEnabled(Context context) {
        switch (((WifiManager) context.getSystemService(WifiManager.class)).getWifiState()) {
            case 2:
            case 3:
                return true;
            default:
                return false;
        }
    }

    private static CharSequence getSummary(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(WifiManager.class);
        switch (wifiManager.getWifiState()) {
            case 0:
            case 1:
                return context.getText(R.string.switch_off_text);
            case 2:
                return context.getText(R.string.disconnected);
            case 3:
                String strRemoveDoubleQuotes = android.net.wifi.WifiInfo.removeDoubleQuotes(wifiManager.getConnectionInfo().getSSID());
                if (TextUtils.equals(strRemoveDoubleQuotes, "<unknown ssid>")) {
                    return context.getText(R.string.disconnected);
                }
                return strRemoveDoubleQuotes;
            default:
                return "";
        }
    }

    private static PendingIntent getPrimaryAction(Context context) {
        return PendingIntent.getActivity(context, 0, getIntent(context), 0);
    }

    private static PendingIntent getBroadcastIntent(Context context) {
        Intent intent = new Intent("com.android.settings.wifi.action.WIFI_CHANGED");
        intent.setClass(context, SliceBroadcastReceiver.class);
        return PendingIntent.getBroadcast(context, 0, intent, 268435456);
    }
}
