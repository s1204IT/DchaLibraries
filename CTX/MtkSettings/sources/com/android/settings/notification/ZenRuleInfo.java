package com.android.settings.notification;

import android.content.ComponentName;
import android.net.Uri;

public class ZenRuleInfo {
    public ComponentName configurationActivity;
    public Uri defaultConditionId;
    public boolean isSystem;
    public CharSequence packageLabel;
    public String packageName;
    public int ruleInstanceLimit = -1;
    public ComponentName serviceComponent;
    public String settingsAction;
    public String title;

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ZenRuleInfo zenRuleInfo = (ZenRuleInfo) obj;
        if (this.isSystem != zenRuleInfo.isSystem || this.ruleInstanceLimit != zenRuleInfo.ruleInstanceLimit) {
            return false;
        }
        if (this.packageName == null ? zenRuleInfo.packageName != null : !this.packageName.equals(zenRuleInfo.packageName)) {
            return false;
        }
        if (this.title == null ? zenRuleInfo.title != null : !this.title.equals(zenRuleInfo.title)) {
            return false;
        }
        if (this.settingsAction == null ? zenRuleInfo.settingsAction != null : !this.settingsAction.equals(zenRuleInfo.settingsAction)) {
            return false;
        }
        if (this.configurationActivity == null ? zenRuleInfo.configurationActivity != null : !this.configurationActivity.equals(zenRuleInfo.configurationActivity)) {
            return false;
        }
        if (this.defaultConditionId == null ? zenRuleInfo.defaultConditionId != null : !this.defaultConditionId.equals(zenRuleInfo.defaultConditionId)) {
            return false;
        }
        if (this.serviceComponent == null ? zenRuleInfo.serviceComponent != null : !this.serviceComponent.equals(zenRuleInfo.serviceComponent)) {
            return false;
        }
        if (this.packageLabel != null) {
            return this.packageLabel.equals(zenRuleInfo.packageLabel);
        }
        if (zenRuleInfo.packageLabel == null) {
            return true;
        }
        return false;
    }
}
