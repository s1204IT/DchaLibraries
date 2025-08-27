package com.android.settings.bluetooth;

import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.support.v4.graphics.drawable.IconCompat;
import android.support.v4.util.Consumer;
import androidx.slice.Slice;
import androidx.slice.builders.ListBuilder;
import androidx.slice.builders.SliceAction;
import com.android.settings.R;
import com.android.settings.SubSettings;
import com.android.settings.connecteddevice.BluetoothDashboardFragment;
import com.android.settings.search.DatabaseIndexingUtils;
import com.android.settings.slices.SliceBroadcastReceiver;
import com.android.settingslib.bluetooth.LocalBluetoothManager;

/* loaded from: classes.dex */
public class BluetoothSliceBuilder {
    public static final Uri BLUETOOTH_URI = new Uri.Builder().scheme("content").authority("android.settings.slices").appendPath("action").appendPath("bluetooth").build();
    public static final IntentFilter INTENT_FILTER = new IntentFilter();

    static {
        INTENT_FILTER.addAction("android.bluetooth.adapter.action.CONNECTION_STATE_CHANGED");
        INTENT_FILTER.addAction("android.bluetooth.adapter.action.STATE_CHANGED");
    }

    public static Slice getSlice(Context context) {
        boolean zIsBluetoothEnabled = isBluetoothEnabled();
        final CharSequence text = context.getText(R.string.bluetooth_settings);
        IconCompat iconCompatCreateWithResource = IconCompat.createWithResource(context, R.drawable.ic_settings_bluetooth);
        int colorAccent = com.android.settings.Utils.getColorAccent(context);
        PendingIntent broadcastIntent = getBroadcastIntent(context);
        final SliceAction sliceAction = new SliceAction(getPrimaryAction(context), iconCompatCreateWithResource, text);
        final SliceAction sliceAction2 = new SliceAction(broadcastIntent, (CharSequence) null, zIsBluetoothEnabled);
        return new ListBuilder(context, BLUETOOTH_URI, -1L).setAccentColor(colorAccent).addRow(new Consumer() { // from class: com.android.settings.bluetooth.-$$Lambda$BluetoothSliceBuilder$t1meuX4HmFYfZCMXndFcRlc9eII
            @Override // android.support.v4.util.Consumer
            public final void accept(Object obj) {
                CharSequence charSequence = text;
                ((ListBuilder.RowBuilder) obj).setTitle(charSequence).addEndItem(sliceAction2).setPrimaryAction(sliceAction);
            }
        }).build();
    }

    public static Intent getIntent(Context context) {
        String string = context.getText(R.string.bluetooth_settings_title).toString();
        return DatabaseIndexingUtils.buildSearchResultPageIntent(context, BluetoothDashboardFragment.class.getName(), null, string, 747).setClassName(context.getPackageName(), SubSettings.class.getName()).setData(new Uri.Builder().appendPath("bluetooth").build());
    }

    public static void handleUriChange(Context context, Intent intent) {
        LocalBluetoothManager.getInstance(context, null).getBluetoothAdapter().setBluetoothEnabled(intent.getBooleanExtra("android.app.slice.extra.TOGGLE_STATE", false));
    }

    private static boolean isBluetoothEnabled() {
        return BluetoothAdapter.getDefaultAdapter().isEnabled();
    }

    private static PendingIntent getPrimaryAction(Context context) {
        return PendingIntent.getActivity(context, 0, getIntent(context), 0);
    }

    private static PendingIntent getBroadcastIntent(Context context) {
        return PendingIntent.getBroadcast(context, 0, new Intent("com.android.settings.bluetooth.action.BLUETOOTH_MODE_CHANGED").setClass(context, SliceBroadcastReceiver.class), 268435456);
    }
}
