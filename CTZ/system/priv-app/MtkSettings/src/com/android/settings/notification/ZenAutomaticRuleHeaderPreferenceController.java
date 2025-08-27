package com.android.settings.notification;

import android.app.AutomaticZenRule;
import android.app.Fragment;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.service.notification.ZenModeConfig;
import android.support.v14.preference.PreferenceFragment;
import android.support.v7.preference.Preference;
import android.util.Pair;
import android.util.Slog;
import android.view.View;
import com.android.settings.R;
import com.android.settings.applications.LayoutPreference;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.notification.ZenRuleNameDialog;
import com.android.settings.widget.EntityHeaderController;
import com.android.settingslib.core.lifecycle.Lifecycle;

/* loaded from: classes.dex */
public class ZenAutomaticRuleHeaderPreferenceController extends AbstractZenModePreferenceController implements PreferenceControllerMixin {
    private final String KEY;
    private EntityHeaderController mController;
    private final PreferenceFragment mFragment;
    private String mId;
    private AutomaticZenRule mRule;

    public ZenAutomaticRuleHeaderPreferenceController(Context context, PreferenceFragment preferenceFragment, Lifecycle lifecycle) {
        super(context, "pref_app_header", lifecycle);
        this.KEY = "pref_app_header";
        this.mFragment = preferenceFragment;
    }

    @Override // com.android.settings.notification.AbstractZenModePreferenceController, com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "pref_app_header";
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return this.mRule != null;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) {
        if (this.mRule != null && this.mFragment != null) {
            LayoutPreference layoutPreference = (LayoutPreference) preference;
            if (this.mController == null) {
                this.mController = EntityHeaderController.newInstance(this.mFragment.getActivity(), this.mFragment, layoutPreference.findViewById(R.id.entity_header));
                this.mController.setEditZenRuleNameListener(new View.OnClickListener() { // from class: com.android.settings.notification.ZenAutomaticRuleHeaderPreferenceController.1
                    @Override // android.view.View.OnClickListener
                    public void onClick(View view) {
                        ZenRuleNameDialog.show(ZenAutomaticRuleHeaderPreferenceController.this.mFragment, ZenAutomaticRuleHeaderPreferenceController.this.mRule.getName(), null, ZenAutomaticRuleHeaderPreferenceController.this.new RuleNameChangeListener());
                    }
                });
            }
            this.mController.setIcon(getIcon()).setLabel(this.mRule.getName()).setPackageName(this.mRule.getOwner().getPackageName()).setUid(this.mContext.getUserId()).setHasAppInfoLink(false).setButtonActions(2, 0).done(this.mFragment.getActivity(), this.mContext).findViewById(R.id.entity_header).setVisibility(0);
        }
    }

    private Drawable getIcon() throws PackageManager.NameNotFoundException {
        try {
            PackageManager packageManager = this.mContext.getPackageManager();
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(this.mRule.getOwner().getPackageName(), 0);
            if (applicationInfo.isSystemApp()) {
                if (ZenModeConfig.isValidScheduleConditionId(this.mRule.getConditionId())) {
                    return this.mContext.getDrawable(R.drawable.ic_timelapse);
                }
                if (ZenModeConfig.isValidEventConditionId(this.mRule.getConditionId())) {
                    return this.mContext.getDrawable(R.drawable.ic_event);
                }
            }
            return applicationInfo.loadIcon(packageManager);
        } catch (PackageManager.NameNotFoundException e) {
            Slog.w("PrefControllerMixin", "Unable to load icon - PackageManager.NameNotFoundException");
            return null;
        }
    }

    protected void onResume(AutomaticZenRule automaticZenRule, String str) {
        this.mRule = automaticZenRule;
        this.mId = str;
    }

    public class RuleNameChangeListener implements ZenRuleNameDialog.PositiveClickListener {
        public RuleNameChangeListener() {
        }

        @Override // com.android.settings.notification.ZenRuleNameDialog.PositiveClickListener
        public void onOk(String str, Fragment fragment) {
            ZenAutomaticRuleHeaderPreferenceController.this.mMetricsFeatureProvider.action(ZenAutomaticRuleHeaderPreferenceController.this.mContext, 1267, new Pair[0]);
            ZenAutomaticRuleHeaderPreferenceController.this.mRule.setName(str);
            ZenAutomaticRuleHeaderPreferenceController.this.mBackend.setZenRule(ZenAutomaticRuleHeaderPreferenceController.this.mId, ZenAutomaticRuleHeaderPreferenceController.this.mRule);
        }
    }
}
