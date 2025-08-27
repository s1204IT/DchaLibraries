package com.android.settings.slices;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import com.android.settings.bluetooth.BluetoothSliceBuilder;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.SliderPreferenceController;
import com.android.settings.core.TogglePreferenceController;
import com.android.settings.notification.ZenModeSliceBuilder;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.wifi.WifiSliceBuilder;

/* loaded from: classes.dex */
public class SliceBroadcastReceiver extends BroadcastReceiver {
    private static String TAG = "SettSliceBroadcastRec";

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Removed duplicated region for block: B:23:0x0056  */
    @Override // android.content.BroadcastReceiver
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void onReceive(Context context, Intent intent) {
        String stringExtra;
        boolean booleanExtra;
        String action = intent.getAction();
        stringExtra = intent.getStringExtra("com.android.settings.slice.extra.key");
        booleanExtra = intent.getBooleanExtra("com.android.settings.slice.extra.platform", false);
        switch (action) {
            case "com.android.settings.slice.action.TOGGLE_CHANGED":
                handleToggleAction(context, stringExtra, intent.getBooleanExtra("android.app.slice.extra.TOGGLE_STATE", false), booleanExtra);
                break;
            case "com.android.settings.slice.action.SLIDER_CHANGED":
                handleSliderAction(context, stringExtra, intent.getIntExtra("android.app.slice.extra.RANGE_VALUE", -1), booleanExtra);
                break;
            case "com.android.settings.bluetooth.action.BLUETOOTH_MODE_CHANGED":
                BluetoothSliceBuilder.handleUriChange(context, intent);
                break;
            case "com.android.settings.wifi.action.WIFI_CHANGED":
                WifiSliceBuilder.handleUriChange(context, intent);
                break;
            case "com.android.settings.wifi.calling.action.WIFI_CALLING_CHANGED":
                FeatureFactory.getFactory(context).getSlicesFeatureProvider().getNewWifiCallingSliceHelper(context).handleWifiCallingChanged(intent);
                break;
            case "com.android.settings.notification.ZEN_MODE_CHANGED":
                ZenModeSliceBuilder.handleUriChange(context, intent);
                break;
        }
    }

    private void handleToggleAction(Context context, String str, boolean z, boolean z2) {
        if (TextUtils.isEmpty(str)) {
            throw new IllegalStateException("No key passed to Intent for toggle controller");
        }
        BasePreferenceController preferenceController = getPreferenceController(context, str);
        if (!(preferenceController instanceof TogglePreferenceController)) {
            throw new IllegalStateException("Toggle action passed for a non-toggle key: " + str);
        }
        if (!preferenceController.isAvailable()) {
            Log.w(TAG, "Can't update " + str + " since the setting is unavailable");
            if (!preferenceController.hasAsyncUpdate()) {
                updateUri(context, str, z2);
                return;
            }
            return;
        }
        ((TogglePreferenceController) preferenceController).setChecked(z);
        logSliceValueChange(context, str, z ? 1 : 0);
        if (!preferenceController.hasAsyncUpdate()) {
            updateUri(context, str, z2);
        }
    }

    private void handleSliderAction(Context context, String str, int i, boolean z) {
        if (TextUtils.isEmpty(str)) {
            throw new IllegalArgumentException("No key passed to Intent for slider controller. Use extra: com.android.settings.slice.extra.key");
        }
        if (i == -1) {
            throw new IllegalArgumentException("Invalid position passed to Slider controller");
        }
        BasePreferenceController preferenceController = getPreferenceController(context, str);
        if (!(preferenceController instanceof SliderPreferenceController)) {
            throw new IllegalArgumentException("Slider action passed for a non-slider key: " + str);
        }
        if (!preferenceController.isAvailable()) {
            Log.w(TAG, "Can't update " + str + " since the setting is unavailable");
            updateUri(context, str, z);
            return;
        }
        SliderPreferenceController sliderPreferenceController = (SliderPreferenceController) preferenceController;
        int maxSteps = sliderPreferenceController.getMaxSteps();
        if (i < 0 || i > maxSteps) {
            throw new IllegalArgumentException("Invalid position passed to Slider controller. Expected between 0 and " + maxSteps + " but found " + i);
        }
        sliderPreferenceController.setSliderPosition(i);
        logSliceValueChange(context, str, i);
        updateUri(context, str, z);
    }

    private void logSliceValueChange(Context context, String str, int i) {
        FeatureFactory.getFactory(context).getMetricsFeatureProvider().action(context, 1372, Pair.create(854, str), Pair.create(1089, Integer.valueOf(i)));
    }

    private BasePreferenceController getPreferenceController(Context context, String str) {
        return SliceBuilderUtils.getPreferenceController(context, new SlicesDatabaseAccessor(context).getSliceDataFromKey(str));
    }

    private void updateUri(Context context, String str, boolean z) {
        String str2;
        if (z) {
            str2 = "android.settings.slices";
        } else {
            str2 = "com.android.settings.slices";
        }
        context.getContentResolver().notifyChange(new Uri.Builder().scheme("content").authority(str2).appendPath("action").appendPath(str).build(), null);
    }
}
