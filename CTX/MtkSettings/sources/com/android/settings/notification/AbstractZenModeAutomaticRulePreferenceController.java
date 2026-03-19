package com.android.settings.notification;

import android.app.AutomaticZenRule;
import android.app.Fragment;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.service.notification.ZenModeConfig;
import android.support.v7.preference.Preference;
import android.util.Pair;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.notification.ZenRuleNameDialog;
import com.android.settingslib.core.lifecycle.Lifecycle;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class AbstractZenModeAutomaticRulePreferenceController extends AbstractZenModePreferenceController implements PreferenceControllerMixin {
    private static final Comparator<Map.Entry<String, AutomaticZenRule>> RULE_COMPARATOR = new Comparator<Map.Entry<String, AutomaticZenRule>>() {
        @Override
        public int compare(Map.Entry<String, AutomaticZenRule> entry, Map.Entry<String, AutomaticZenRule> entry2) {
            boolean zContains = AbstractZenModeAutomaticRulePreferenceController.getDefaultRuleIds().contains(entry.getKey());
            if (zContains != AbstractZenModeAutomaticRulePreferenceController.getDefaultRuleIds().contains(entry2.getKey())) {
                return zContains ? -1 : 1;
            }
            int iCompare = Long.compare(entry.getValue().getCreationTime(), entry2.getValue().getCreationTime());
            if (iCompare != 0) {
                return iCompare;
            }
            return key(entry.getValue()).compareTo(key(entry2.getValue()));
        }

        private String key(AutomaticZenRule automaticZenRule) {
            int i;
            if (ZenModeConfig.isValidScheduleConditionId(automaticZenRule.getConditionId())) {
                i = 1;
            } else {
                i = ZenModeConfig.isValidEventConditionId(automaticZenRule.getConditionId()) ? 2 : 3;
            }
            return i + automaticZenRule.getName().toString();
        }
    };
    private static List<String> mDefaultRuleIds;
    protected ZenModeBackend mBackend;
    protected Fragment mParent;
    protected PackageManager mPm;
    protected Set<Map.Entry<String, AutomaticZenRule>> mRules;

    public AbstractZenModeAutomaticRulePreferenceController(Context context, String str, Fragment fragment, Lifecycle lifecycle) {
        super(context, str, lifecycle);
        this.mBackend = ZenModeBackend.getInstance(context);
        this.mPm = this.mContext.getPackageManager();
        this.mParent = fragment;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        this.mRules = getZenModeRules();
    }

    private static List<String> getDefaultRuleIds() {
        if (mDefaultRuleIds == null) {
            mDefaultRuleIds = ZenModeConfig.DEFAULT_RULE_IDS;
        }
        return mDefaultRuleIds;
    }

    private Set<Map.Entry<String, AutomaticZenRule>> getZenModeRules() {
        return NotificationManager.from(this.mContext).getAutomaticZenRules().entrySet();
    }

    protected void showNameRuleDialog(ZenRuleInfo zenRuleInfo, Fragment fragment) {
        ZenRuleNameDialog.show(fragment, null, zenRuleInfo.defaultConditionId, new RuleNameChangeListener(zenRuleInfo));
    }

    protected Map.Entry<String, AutomaticZenRule>[] sortedRules() {
        if (this.mRules == null) {
            this.mRules = getZenModeRules();
        }
        Map.Entry<String, AutomaticZenRule>[] entryArr = (Map.Entry[]) this.mRules.toArray(new Map.Entry[this.mRules.size()]);
        Arrays.sort(entryArr, RULE_COMPARATOR);
        return entryArr;
    }

    protected static Intent getRuleIntent(String str, ComponentName componentName, String str2) {
        Intent intentPutExtra = new Intent().addFlags(67108864).putExtra("android.service.notification.extra.RULE_ID", str2);
        if (componentName != null) {
            intentPutExtra.setComponent(componentName);
        } else {
            intentPutExtra.setAction(str);
        }
        return intentPutExtra;
    }

    public static ZenRuleInfo getRuleInfo(PackageManager packageManager, ServiceInfo serviceInfo) {
        if (serviceInfo == null || serviceInfo.metaData == null) {
            return null;
        }
        String string = serviceInfo.metaData.getString("android.service.zen.automatic.ruleType");
        ComponentName settingsActivity = getSettingsActivity(serviceInfo);
        if (string == null || string.trim().isEmpty() || settingsActivity == null) {
            return null;
        }
        ZenRuleInfo zenRuleInfo = new ZenRuleInfo();
        zenRuleInfo.serviceComponent = new ComponentName(serviceInfo.packageName, serviceInfo.name);
        zenRuleInfo.settingsAction = "android.settings.ZEN_MODE_EXTERNAL_RULE_SETTINGS";
        zenRuleInfo.title = string;
        zenRuleInfo.packageName = serviceInfo.packageName;
        zenRuleInfo.configurationActivity = getSettingsActivity(serviceInfo);
        zenRuleInfo.packageLabel = serviceInfo.applicationInfo.loadLabel(packageManager);
        zenRuleInfo.ruleInstanceLimit = serviceInfo.metaData.getInt("android.service.zen.automatic.ruleInstanceLimit", -1);
        return zenRuleInfo;
    }

    protected static ComponentName getSettingsActivity(ServiceInfo serviceInfo) {
        String string;
        if (serviceInfo == null || serviceInfo.metaData == null || (string = serviceInfo.metaData.getString("android.service.zen.automatic.configurationActivity")) == null) {
            return null;
        }
        return ComponentName.unflattenFromString(string);
    }

    public class RuleNameChangeListener implements ZenRuleNameDialog.PositiveClickListener {
        ZenRuleInfo mRuleInfo;

        public RuleNameChangeListener(ZenRuleInfo zenRuleInfo) {
            this.mRuleInfo = zenRuleInfo;
        }

        @Override
        public void onOk(String str, Fragment fragment) {
            AbstractZenModeAutomaticRulePreferenceController.this.mMetricsFeatureProvider.action(AbstractZenModeAutomaticRulePreferenceController.this.mContext, 1267, new Pair[0]);
            String strAddZenRule = AbstractZenModeAutomaticRulePreferenceController.this.mBackend.addZenRule(new AutomaticZenRule(str, this.mRuleInfo.serviceComponent, this.mRuleInfo.defaultConditionId, 2, true));
            if (strAddZenRule != null) {
                fragment.startActivity(AbstractZenModeAutomaticRulePreferenceController.getRuleIntent(this.mRuleInfo.settingsAction, null, strAddZenRule));
            }
        }
    }
}
