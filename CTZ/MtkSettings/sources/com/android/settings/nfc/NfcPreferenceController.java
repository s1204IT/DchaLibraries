package com.android.settings.nfc;

import android.content.Context;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.Log;
import com.android.settings.core.TogglePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;
import com.mediatek.settings.FeatureOption;
import java.util.List;

public class NfcPreferenceController extends TogglePreferenceController implements LifecycleObserver, OnPause, OnResume {
    private static final String KEY_MTK_TOGGLE_NFC = "toggle_mtk_nfc";
    public static final String KEY_TOGGLE_NFC = "toggle_nfc";
    private static final String TAG = "NfcPreferenceController";
    private NfcAirplaneModeObserver mAirplaneModeObserver;
    private final NfcAdapter mNfcAdapter;
    private NfcEnabler mNfcEnabler;

    public NfcPreferenceController(Context context, String str) {
        super(context, str);
        this.mNfcAdapter = NfcAdapter.getDefaultAdapter(context);
    }

    @Override
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        if (!isAvailable()) {
            setVisible(preferenceScreen, KEY_MTK_TOGGLE_NFC, false);
            this.mNfcEnabler = null;
            return;
        }
        SwitchPreference switchPreference = (SwitchPreference) preferenceScreen.findPreference(getPreferenceKey());
        this.mNfcEnabler = new NfcEnabler(this.mContext, switchPreference);
        if (FeatureOption.MTK_NFC_ADDON_SUPPORT) {
            Log.d(TAG, "MTK NFC support");
            setVisible(preferenceScreen, KEY_TOGGLE_NFC, false);
        } else {
            Log.d(TAG, "MTK NFC not support");
            setVisible(preferenceScreen, KEY_MTK_TOGGLE_NFC, false);
        }
        if (!isToggleableInAirplaneMode(this.mContext)) {
            this.mAirplaneModeObserver = new NfcAirplaneModeObserver(this.mContext, this.mNfcAdapter, switchPreference);
        }
    }

    @Override
    public boolean isChecked() {
        return this.mNfcAdapter.isEnabled();
    }

    @Override
    public boolean setChecked(boolean z) {
        if (z) {
            this.mNfcAdapter.enable();
            return true;
        }
        this.mNfcAdapter.disable();
        return true;
    }

    @Override
    public int getAvailabilityStatus() {
        if (this.mNfcAdapter != null) {
            return 0;
        }
        return 2;
    }

    @Override
    public IntentFilter getIntentFilter() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.nfc.action.ADAPTER_STATE_CHANGED");
        intentFilter.addAction("android.nfc.extra.ADAPTER_STATE");
        return intentFilter;
    }

    @Override
    public boolean hasAsyncUpdate() {
        return true;
    }

    @Override
    public boolean isSliceable() {
        return TextUtils.equals(getPreferenceKey(), KEY_TOGGLE_NFC);
    }

    @Override
    public void onResume() {
        if (this.mAirplaneModeObserver != null) {
            this.mAirplaneModeObserver.register();
        }
        if (this.mNfcEnabler != null) {
            this.mNfcEnabler.resume();
        }
    }

    @Override
    public void onPause() {
        if (this.mAirplaneModeObserver != null) {
            this.mAirplaneModeObserver.unregister();
        }
        if (this.mNfcEnabler != null) {
            this.mNfcEnabler.pause();
        }
    }

    public static boolean isToggleableInAirplaneMode(Context context) {
        String string = Settings.Global.getString(context.getContentResolver(), "airplane_mode_toggleable_radios");
        return string != null && string.contains("nfc");
    }

    @Override
    public void updateNonIndexableKeys(List<String> list) {
        if (isAvailable()) {
            if (FeatureOption.MTK_NFC_ADDON_SUPPORT) {
                list.add(getPreferenceKey());
            } else {
                list.add(KEY_MTK_TOGGLE_NFC);
            }
        }
    }
}
