package com.android.settings.gestures;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;
import com.android.internal.hardware.AmbientDisplayConfiguration;
import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.AbstractPreferenceController;
import java.util.ArrayList;
import java.util.List;

/* loaded from: classes.dex */
public class GesturesSettingPreferenceController extends BasePreferenceController {
    private static final String FAKE_PREF_KEY = "fake_key_only_for_get_available";
    private static final String KEY_GESTURES_SETTINGS = "gesture_settings";
    private final AssistGestureFeatureProvider mFeatureProvider;
    private List<AbstractPreferenceController> mGestureControllers;

    public GesturesSettingPreferenceController(Context context) {
        super(context, KEY_GESTURES_SETTINGS);
        this.mFeatureProvider = FeatureFactory.getFactory(context).getAssistGestureFeatureProvider();
    }

    @Override // com.android.settings.core.BasePreferenceController
    public int getAvailabilityStatus() {
        if (this.mGestureControllers == null) {
            this.mGestureControllers = buildAllPreferenceControllers(this.mContext);
        }
        boolean z = false;
        for (AbstractPreferenceController abstractPreferenceController : this.mGestureControllers) {
            if (z || abstractPreferenceController.isAvailable()) {
                z = true;
            } else {
                z = false;
            }
        }
        return z ? 0 : 2;
    }

    private static List<AbstractPreferenceController> buildAllPreferenceControllers(Context context) {
        AmbientDisplayConfiguration ambientDisplayConfiguration = new AmbientDisplayConfiguration(context);
        ArrayList arrayList = new ArrayList();
        arrayList.add(new AssistGestureSettingsPreferenceController(context, FAKE_PREF_KEY).setAssistOnly(false));
        arrayList.add(new SwipeToNotificationPreferenceController(context, FAKE_PREF_KEY));
        arrayList.add(new DoubleTwistPreferenceController(context, FAKE_PREF_KEY));
        arrayList.add(new DoubleTapPowerPreferenceController(context, FAKE_PREF_KEY));
        arrayList.add(new PickupGesturePreferenceController(context, FAKE_PREF_KEY).setConfig(ambientDisplayConfiguration));
        arrayList.add(new DoubleTapScreenPreferenceController(context, FAKE_PREF_KEY).setConfig(ambientDisplayConfiguration));
        arrayList.add(new PreventRingingPreferenceController(context, FAKE_PREF_KEY));
        return arrayList;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public CharSequence getSummary() {
        if (!this.mFeatureProvider.isSensorAvailable(this.mContext)) {
            return "";
        }
        ContentResolver contentResolver = this.mContext.getContentResolver();
        boolean z = Settings.Secure.getInt(contentResolver, "assist_gesture_enabled", 1) != 0;
        boolean z2 = Settings.Secure.getInt(contentResolver, "assist_gesture_silence_alerts_enabled", 1) != 0;
        if (this.mFeatureProvider.isSupported(this.mContext) && z) {
            return this.mContext.getText(R.string.language_input_gesture_summary_on_with_assist);
        }
        if (z2) {
            return this.mContext.getText(R.string.language_input_gesture_summary_on_non_assist);
        }
        return this.mContext.getText(R.string.language_input_gesture_summary_off);
    }
}
