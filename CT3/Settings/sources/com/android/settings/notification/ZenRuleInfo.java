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

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ZenRuleInfo that = (ZenRuleInfo) o;
        if (this.isSystem != that.isSystem || this.ruleInstanceLimit != that.ruleInstanceLimit) {
            return false;
        }
        if (this.packageName == null ? that.packageName != null : !this.packageName.equals(that.packageName)) {
            return false;
        }
        if (this.title == null ? that.title != null : !this.title.equals(that.title)) {
            return false;
        }
        if (this.settingsAction == null ? that.settingsAction != null : !this.settingsAction.equals(that.settingsAction)) {
            return false;
        }
        if (this.configurationActivity == null ? that.configurationActivity != null : !this.configurationActivity.equals(that.configurationActivity)) {
            return false;
        }
        if (this.defaultConditionId == null ? that.defaultConditionId != null : !this.defaultConditionId.equals(that.defaultConditionId)) {
            return false;
        }
        if (this.serviceComponent == null ? that.serviceComponent != null : !this.serviceComponent.equals(that.serviceComponent)) {
            return false;
        }
        if (this.packageLabel != null) {
            return this.packageLabel.equals(that.packageLabel);
        }
        return that.packageLabel == null;
    }
}
