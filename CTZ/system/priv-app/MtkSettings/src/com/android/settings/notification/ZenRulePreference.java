package com.android.settings.notification;

import android.app.AutomaticZenRule;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.service.notification.ZenModeConfig;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.Pair;
import android.view.View;
import com.android.settings.R;
import com.android.settings.notification.ZenDeleteRuleDialog;
import com.android.settings.utils.ManagedServiceSettings;
import com.android.settings.utils.ZenServiceListing;
import com.android.settingslib.TwoTargetPreference;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import java.util.Map;

/* loaded from: classes.dex */
public class ZenRulePreference extends TwoTargetPreference {
    private static final ManagedServiceSettings.Config CONFIG = ZenModeAutomationSettings.getConditionProviderConfig();
    boolean appExists;
    final ZenModeBackend mBackend;
    final Context mContext;
    private final View.OnClickListener mDeleteListener;
    final String mId;
    final MetricsFeatureProvider mMetricsFeatureProvider;
    final CharSequence mName;
    final Fragment mParent;
    final PackageManager mPm;
    final Preference mPref;
    final ZenServiceListing mServiceListing;

    public ZenRulePreference(Context context, Map.Entry<String, AutomaticZenRule> entry, Fragment fragment, MetricsFeatureProvider metricsFeatureProvider) {
        super(context);
        this.mDeleteListener = new View.OnClickListener() { // from class: com.android.settings.notification.ZenRulePreference.1
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                ZenRulePreference.this.showDeleteRuleDialog(ZenRulePreference.this.mParent, ZenRulePreference.this.mId, ZenRulePreference.this.mName.toString());
            }
        };
        this.mBackend = ZenModeBackend.getInstance(context);
        this.mContext = context;
        AutomaticZenRule value = entry.getValue();
        this.mName = value.getName();
        this.mId = entry.getKey();
        this.mParent = fragment;
        this.mPm = this.mContext.getPackageManager();
        this.mServiceListing = new ZenServiceListing(this.mContext, CONFIG);
        this.mServiceListing.reloadApprovedServices();
        this.mPref = this;
        this.mMetricsFeatureProvider = metricsFeatureProvider;
        setAttributes(value);
    }

    @Override // com.android.settingslib.TwoTargetPreference
    protected int getSecondTargetResId() {
        if (this.mId != null && ZenModeConfig.DEFAULT_RULE_IDS.contains(this.mId)) {
            return 0;
        }
        return R.layout.zen_rule_widget;
    }

    @Override // com.android.settingslib.TwoTargetPreference, android.support.v7.preference.Preference
    public void onBindViewHolder(PreferenceViewHolder preferenceViewHolder) {
        super.onBindViewHolder(preferenceViewHolder);
        View viewFindViewById = preferenceViewHolder.findViewById(R.id.delete_zen_rule);
        if (viewFindViewById != null) {
            viewFindViewById.setOnClickListener(this.mDeleteListener);
        }
    }

    private void showDeleteRuleDialog(Fragment fragment, String str, String str2) {
        ZenDeleteRuleDialog.show(fragment, str2, str, new ZenDeleteRuleDialog.PositiveClickListener() { // from class: com.android.settings.notification.ZenRulePreference.2
            @Override // com.android.settings.notification.ZenDeleteRuleDialog.PositiveClickListener
            public void onOk(String str3) {
                ZenRulePreference.this.mMetricsFeatureProvider.action(ZenRulePreference.this.mContext, 175, new Pair[0]);
                ZenRulePreference.this.mBackend.removeZenRule(str3);
            }
        });
    }

    protected void setAttributes(AutomaticZenRule automaticZenRule) {
        String str;
        boolean zIsValidScheduleConditionId = ZenModeConfig.isValidScheduleConditionId(automaticZenRule.getConditionId());
        boolean zIsValidEventConditionId = ZenModeConfig.isValidEventConditionId(automaticZenRule.getConditionId());
        boolean z = true;
        boolean z2 = zIsValidScheduleConditionId || zIsValidEventConditionId;
        try {
            setSummary(computeRuleSummary(automaticZenRule, z2, this.mPm.getApplicationInfo(automaticZenRule.getOwner().getPackageName(), 0).loadLabel(this.mPm)));
            this.appExists = true;
            setTitle(automaticZenRule.getName());
            setPersistent(false);
            if (zIsValidScheduleConditionId) {
                str = "android.settings.ZEN_MODE_SCHEDULE_RULE_SETTINGS";
            } else {
                str = zIsValidEventConditionId ? "android.settings.ZEN_MODE_EVENT_RULE_SETTINGS" : "";
            }
            ComponentName settingsActivity = AbstractZenModeAutomaticRulePreferenceController.getSettingsActivity(this.mServiceListing.findService(automaticZenRule.getOwner()));
            setIntent(AbstractZenModeAutomaticRulePreferenceController.getRuleIntent(str, settingsActivity, this.mId));
            if (settingsActivity == null && !z2) {
                z = false;
            }
            setSelectable(z);
            setKey(this.mId);
        } catch (PackageManager.NameNotFoundException e) {
            this.appExists = false;
        }
    }

    private String computeRuleSummary(AutomaticZenRule automaticZenRule, boolean z, CharSequence charSequence) {
        if (automaticZenRule == null || !automaticZenRule.isEnabled()) {
            return this.mContext.getResources().getString(R.string.switch_off_text);
        }
        return this.mContext.getResources().getString(R.string.switch_on_text);
    }
}
