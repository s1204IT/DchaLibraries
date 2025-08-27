package com.android.settings.nfc;

import android.content.Context;
import android.nfc.NfcAdapter;
import android.support.v7.preference.PreferenceScreen;
import android.util.Log;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.RestrictedPreference;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;
import com.mediatek.settings.FeatureOption;
import java.util.List;

/* loaded from: classes.dex */
public class AndroidBeamPreferenceController extends BasePreferenceController implements LifecycleObserver, OnPause, OnResume {
    public static final String KEY_ANDROID_BEAM_SETTINGS = "android_beam_settings";
    private static final String KEY_MTK_TOGGLE_NFC = "toggle_mtk_nfc";
    private static final String TAG = "NfcPreferenceController";
    private NfcAirplaneModeObserver mAirplaneModeObserver;
    private AndroidBeamEnabler mAndroidBeamEnabler;
    private final NfcAdapter mNfcAdapter;

    public AndroidBeamPreferenceController(Context context, String str) {
        super(context, str);
        this.mNfcAdapter = NfcAdapter.getDefaultAdapter(context);
    }

    @Override // com.android.settings.core.BasePreferenceController, com.android.settingslib.core.AbstractPreferenceController
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        if (!isAvailable()) {
            setVisible(preferenceScreen, KEY_MTK_TOGGLE_NFC, false);
            this.mAndroidBeamEnabler = null;
            return;
        }
        RestrictedPreference restrictedPreference = (RestrictedPreference) preferenceScreen.findPreference(getPreferenceKey());
        this.mAndroidBeamEnabler = new AndroidBeamEnabler(this.mContext, restrictedPreference);
        if (FeatureOption.MTK_NFC_ADDON_SUPPORT) {
            Log.d(TAG, "MTK NFC support");
            setVisible(preferenceScreen, KEY_ANDROID_BEAM_SETTINGS, false);
        } else {
            Log.d(TAG, "MTK NFC not support");
            setVisible(preferenceScreen, KEY_MTK_TOGGLE_NFC, false);
        }
        if (!NfcPreferenceController.isToggleableInAirplaneMode(this.mContext)) {
            this.mAirplaneModeObserver = new NfcAirplaneModeObserver(this.mContext, this.mNfcAdapter, restrictedPreference);
        }
    }

    @Override // com.android.settings.core.BasePreferenceController
    public int getAvailabilityStatus() {
        if (this.mNfcAdapter != null) {
            return 0;
        }
        return 2;
    }

    @Override // com.android.settingslib.core.lifecycle.events.OnResume
    public void onResume() {
        if (this.mAirplaneModeObserver != null) {
            this.mAirplaneModeObserver.register();
        }
        if (this.mAndroidBeamEnabler != null) {
            this.mAndroidBeamEnabler.resume();
        }
    }

    @Override // com.android.settingslib.core.lifecycle.events.OnPause
    public void onPause() {
        if (this.mAirplaneModeObserver != null) {
            this.mAirplaneModeObserver.unregister();
        }
        if (this.mAndroidBeamEnabler != null) {
            this.mAndroidBeamEnabler.pause();
        }
    }

    @Override // com.android.settings.core.BasePreferenceController
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
