package com.android.documentsui.inspector.actions;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import com.android.documentsui.base.DocumentInfo;

public abstract class Action {
    static final boolean $assertionsDisabled = false;
    protected Context mContext;
    protected DocumentInfo mDoc;
    protected PackageManager mPm;

    public abstract boolean canPerformAction();

    public abstract int getButtonIcon();

    public abstract int getButtonLabel();

    public abstract String getHeader();

    public abstract String getPackageName();

    public Action(Context context, PackageManager packageManager, DocumentInfo documentInfo) {
        this.mContext = context;
        this.mPm = packageManager;
        this.mDoc = documentInfo;
    }

    public Drawable getAppIcon() {
        String packageName = getPackageName();
        if (packageName == null || "android".equals(packageName)) {
            return null;
        }
        try {
            return this.mPm.getApplicationIcon(packageName);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    public String getAppName() {
        String packageName = getPackageName();
        if (packageName == null) {
            return "unknown";
        }
        if ("android".equals(packageName)) {
            return "android";
        }
        try {
            return (String) this.mPm.getApplicationLabel(this.mPm.getApplicationInfo(packageName, 0));
        } catch (PackageManager.NameNotFoundException e) {
            return "unknown";
        }
    }
}
