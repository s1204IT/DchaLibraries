package com.android.settings.notification;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.RadioButton;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.logging.MetricsLogger;
import com.android.settings.R;
import com.android.settings.overlay.FeatureFactory;

/* loaded from: classes.dex */
public class ZenOnboardingActivity extends Activity {

    @VisibleForTesting
    static final long ALWAYS_SHOW_THRESHOLD = 1209600000;

    @VisibleForTesting
    static final String PREF_KEY_SUGGESTION_FIRST_DISPLAY_TIME = "pref_zen_suggestion_first_display_time_ms";
    View mKeepCurrentSetting;
    RadioButton mKeepCurrentSettingButton;
    private MetricsLogger mMetrics;
    View mNewSetting;
    RadioButton mNewSettingButton;
    private NotificationManager mNm;

    @Override // android.app.Activity
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setNotificationManager((NotificationManager) getSystemService(NotificationManager.class));
        setMetricsLogger(new MetricsLogger());
        Settings.Global.putInt(getApplicationContext().getContentResolver(), "zen_settings_suggestion_viewed", 1);
        setupUI();
    }

    @VisibleForTesting
    protected void setupUI() {
        setContentView(R.layout.zen_onboarding);
        this.mNewSetting = findViewById(R.id.zen_onboarding_new_setting);
        this.mKeepCurrentSetting = findViewById(R.id.zen_onboarding_current_setting);
        this.mNewSettingButton = (RadioButton) findViewById(R.id.zen_onboarding_new_setting_button);
        this.mKeepCurrentSettingButton = (RadioButton) findViewById(R.id.zen_onboarding_current_setting_button);
        View.OnClickListener onClickListener = new View.OnClickListener() { // from class: com.android.settings.notification.ZenOnboardingActivity.1
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                ZenOnboardingActivity.this.mKeepCurrentSettingButton.setChecked(false);
                ZenOnboardingActivity.this.mNewSettingButton.setChecked(true);
            }
        };
        View.OnClickListener onClickListener2 = new View.OnClickListener() { // from class: com.android.settings.notification.ZenOnboardingActivity.2
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                ZenOnboardingActivity.this.mKeepCurrentSettingButton.setChecked(true);
                ZenOnboardingActivity.this.mNewSettingButton.setChecked(false);
            }
        };
        this.mNewSetting.setOnClickListener(onClickListener);
        this.mNewSettingButton.setOnClickListener(onClickListener);
        this.mKeepCurrentSetting.setOnClickListener(onClickListener2);
        this.mKeepCurrentSettingButton.setOnClickListener(onClickListener2);
        this.mKeepCurrentSettingButton.setChecked(true);
        this.mMetrics.visible(1380);
    }

    @VisibleForTesting
    protected void setNotificationManager(NotificationManager notificationManager) {
        this.mNm = notificationManager;
    }

    @VisibleForTesting
    protected void setMetricsLogger(MetricsLogger metricsLogger) {
        this.mMetrics = metricsLogger;
    }

    public void launchSettings(View view) {
        this.mMetrics.action(1379);
        Intent intent = new Intent("android.settings.ZEN_MODE_SETTINGS");
        intent.addFlags(268468224);
        startActivity(intent);
    }

    public void save(View view) {
        NotificationManager.Policy notificationPolicy = this.mNm.getNotificationPolicy();
        if (this.mNewSettingButton.isChecked()) {
            this.mNm.setNotificationPolicy(new NotificationManager.Policy(16 | notificationPolicy.priorityCategories, 2, notificationPolicy.priorityMessageSenders, NotificationManager.Policy.getAllSuppressedVisualEffects()));
            this.mMetrics.action(1378);
        } else {
            this.mMetrics.action(1406);
        }
        Settings.Global.putInt(getApplicationContext().getContentResolver(), "zen_settings_updated", 1);
        finishAndRemoveTask();
    }

    public static boolean isSuggestionComplete(Context context) {
        if (wasZenUpdated(context)) {
            return true;
        }
        return (showSuggestion(context) || withinShowTimeThreshold(context)) ? false : true;
    }

    private static boolean wasZenUpdated(Context context) {
        if (NotificationManager.Policy.areAllVisualEffectsSuppressed(((NotificationManager) context.getSystemService(NotificationManager.class)).getNotificationPolicy().suppressedVisualEffects)) {
            Settings.Global.putInt(context.getContentResolver(), "zen_settings_updated", 1);
        }
        return Settings.Global.getInt(context.getContentResolver(), "zen_settings_updated", 0) != 0;
    }

    private static boolean showSuggestion(Context context) {
        return Settings.Global.getInt(context.getContentResolver(), "show_zen_settings_suggestion", 0) != 0;
    }

    private static boolean withinShowTimeThreshold(Context context) {
        long j;
        SharedPreferences sharedPrefs = FeatureFactory.getFactory(context).getSuggestionFeatureProvider(context).getSharedPrefs(context);
        long jCurrentTimeMillis = System.currentTimeMillis();
        if (!sharedPrefs.contains(PREF_KEY_SUGGESTION_FIRST_DISPLAY_TIME)) {
            sharedPrefs.edit().putLong(PREF_KEY_SUGGESTION_FIRST_DISPLAY_TIME, jCurrentTimeMillis).commit();
            j = jCurrentTimeMillis;
        } else {
            j = sharedPrefs.getLong(PREF_KEY_SUGGESTION_FIRST_DISPLAY_TIME, -1L);
        }
        boolean z = jCurrentTimeMillis < j + ALWAYS_SHOW_THRESHOLD;
        Log.d("ZenOnboardingActivity", "still show zen suggestion based on time: " + z);
        return z;
    }
}
