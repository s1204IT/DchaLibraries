package com.android.documentsui.inspector.actions;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import com.android.documentsui.R;
import com.android.documentsui.base.DocumentInfo;

public final class ClearDefaultAppAction extends Action {
    public ClearDefaultAppAction(Context context, PackageManager packageManager, DocumentInfo documentInfo) {
        super(context, packageManager, documentInfo);
    }

    @Override
    public String getHeader() {
        return this.mContext.getString(R.string.handler_app_file_opens_with);
    }

    @Override
    public int getButtonIcon() {
        return R.drawable.ic_action_clear;
    }

    @Override
    public boolean canPerformAction() {
        String packageName;
        return (this.mPm.queryIntentActivities(new Intent("android.intent.action.VIEW", this.mDoc.derivedUri), 65536).size() <= 1 || (packageName = getPackageName()) == null || "android".equals(packageName)) ? false : true;
    }

    @Override
    public String getPackageName() {
        ResolveInfo resolveInfoResolveActivity = this.mPm.resolveActivity(new Intent("android.intent.action.VIEW", this.mDoc.derivedUri), 0);
        if (resolveInfoResolveActivity != null && resolveInfoResolveActivity.activityInfo != null) {
            return resolveInfoResolveActivity.activityInfo.packageName;
        }
        return null;
    }

    @Override
    public int getButtonLabel() {
        return R.string.button_clear;
    }
}
