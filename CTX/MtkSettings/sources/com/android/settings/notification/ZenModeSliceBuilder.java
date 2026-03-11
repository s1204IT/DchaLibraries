package com.android.settings.notification;

import android.app.NotificationManager;
import android.app.PendingIntent;
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
import com.android.settings.Utils;
import com.android.settings.search.DatabaseIndexingUtils;
import com.android.settings.slices.SliceBroadcastReceiver;

public class ZenModeSliceBuilder {
    public static final Uri ZEN_MODE_URI = new Uri.Builder().scheme("content").authority("com.android.settings.slices").appendPath("action").appendPath("zen_mode").build();
    public static final IntentFilter INTENT_FILTER = new IntentFilter();

    static {
        INTENT_FILTER.addAction("android.app.action.NOTIFICATION_POLICY_CHANGED");
        INTENT_FILTER.addAction("android.app.action.INTERRUPTION_FILTER_CHANGED");
        INTENT_FILTER.addAction("android.app.action.INTERRUPTION_FILTER_CHANGED_INTERNAL");
    }

    public static Slice getSlice(Context context) {
        boolean zIsZenModeEnabled = isZenModeEnabled(context);
        final CharSequence text = context.getText(R.string.zen_mode_settings_title);
        int colorAccent = Utils.getColorAccent(context);
        PendingIntent broadcastIntent = getBroadcastIntent(context);
        final SliceAction sliceAction = new SliceAction(getPrimaryAction(context), (IconCompat) null, text);
        final SliceAction sliceAction2 = new SliceAction(broadcastIntent, (CharSequence) null, zIsZenModeEnabled);
        return new ListBuilder(context, ZEN_MODE_URI, -1L).setAccentColor(colorAccent).addRow(new Consumer() {
            @Override
            public final void accept(Object obj) {
                CharSequence charSequence = text;
                ((ListBuilder.RowBuilder) obj).setTitle(charSequence).addEndItem(sliceAction2).setPrimaryAction(sliceAction);
            }
        }).build();
    }

    public static void handleUriChange(Context context, Intent intent) {
        NotificationManager.from(context).setZenMode(intent.getBooleanExtra("android.app.slice.extra.TOGGLE_STATE", false) ? 1 : 0, null, "ZenModeSliceBuilder");
    }

    public static Intent getIntent(Context context) {
        return DatabaseIndexingUtils.buildSearchResultPageIntent(context, ZenModeSettings.class.getName(), "zen_mode", context.getText(R.string.zen_mode_settings_title).toString(), 76).setClassName(context.getPackageName(), SubSettings.class.getName()).setData(new Uri.Builder().appendPath("zen_mode").build());
    }

    private static boolean isZenModeEnabled(Context context) {
        switch (((NotificationManager) context.getSystemService(NotificationManager.class)).getZenMode()) {
            case 1:
            case 2:
            case 3:
                return true;
            default:
                return false;
        }
    }

    private static PendingIntent getPrimaryAction(Context context) {
        return PendingIntent.getActivity(context, 0, getIntent(context), 0);
    }

    private static PendingIntent getBroadcastIntent(Context context) {
        return PendingIntent.getBroadcast(context, 0, new Intent("com.android.settings.notification.ZEN_MODE_CHANGED").setClass(context, SliceBroadcastReceiver.class), 268435456);
    }
}
