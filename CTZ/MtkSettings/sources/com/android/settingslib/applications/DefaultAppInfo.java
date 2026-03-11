package com.android.settingslib.applications;

import android.app.AppGlobals;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.IconDrawableFactory;
import com.android.settingslib.widget.CandidateInfo;
import com.android.settingslib.wrapper.PackageManagerWrapper;

public class DefaultAppInfo extends CandidateInfo {
    public final ComponentName componentName;
    private final Context mContext;
    protected final PackageManagerWrapper mPm;
    public final PackageItemInfo packageItemInfo;
    public final String summary;
    public final int userId;

    public DefaultAppInfo(Context context, PackageManagerWrapper packageManagerWrapper, int i, ComponentName componentName) {
        this(context, packageManagerWrapper, i, componentName, null, true);
    }

    public DefaultAppInfo(Context context, PackageManagerWrapper packageManagerWrapper, PackageItemInfo packageItemInfo) {
        this(context, packageManagerWrapper, packageItemInfo, null, true);
    }

    public DefaultAppInfo(Context context, PackageManagerWrapper packageManagerWrapper, int i, ComponentName componentName, String str, boolean z) {
        super(z);
        this.mContext = context;
        this.mPm = packageManagerWrapper;
        this.packageItemInfo = null;
        this.userId = i;
        this.componentName = componentName;
        this.summary = str;
    }

    public DefaultAppInfo(Context context, PackageManagerWrapper packageManagerWrapper, PackageItemInfo packageItemInfo, String str, boolean z) {
        super(z);
        this.mContext = context;
        this.mPm = packageManagerWrapper;
        this.userId = UserHandle.myUserId();
        this.packageItemInfo = packageItemInfo;
        this.componentName = null;
        this.summary = str;
    }

    @Override
    public CharSequence loadLabel() {
        if (this.componentName != null) {
            try {
                ComponentInfo componentInfo = getComponentInfo();
                if (componentInfo != null) {
                    return componentInfo.loadLabel(this.mPm.getPackageManager());
                }
                return this.mPm.getApplicationInfoAsUser(this.componentName.getPackageName(), 0, this.userId).loadLabel(this.mPm.getPackageManager());
            } catch (PackageManager.NameNotFoundException e) {
                return null;
            }
        }
        if (this.packageItemInfo != null) {
            return this.packageItemInfo.loadLabel(this.mPm.getPackageManager());
        }
        return null;
    }

    @Override
    public Drawable loadIcon() {
        IconDrawableFactory iconDrawableFactoryNewInstance = IconDrawableFactory.newInstance(this.mContext);
        if (this.componentName != null) {
            try {
                ComponentInfo componentInfo = getComponentInfo();
                ApplicationInfo applicationInfoAsUser = this.mPm.getApplicationInfoAsUser(this.componentName.getPackageName(), 0, this.userId);
                if (componentInfo != null) {
                    return iconDrawableFactoryNewInstance.getBadgedIcon(componentInfo, applicationInfoAsUser, this.userId);
                }
                return iconDrawableFactoryNewInstance.getBadgedIcon(applicationInfoAsUser);
            } catch (PackageManager.NameNotFoundException e) {
                return null;
            }
        }
        if (this.packageItemInfo == null) {
            return null;
        }
        try {
            return iconDrawableFactoryNewInstance.getBadgedIcon(this.packageItemInfo, this.mPm.getApplicationInfoAsUser(this.packageItemInfo.packageName, 0, this.userId), this.userId);
        } catch (PackageManager.NameNotFoundException e2) {
            return null;
        }
    }

    @Override
    public String getKey() {
        if (this.componentName != null) {
            return this.componentName.flattenToString();
        }
        if (this.packageItemInfo != null) {
            return this.packageItemInfo.packageName;
        }
        return null;
    }

    private ComponentInfo getComponentInfo() {
        try {
            ActivityInfo activityInfo = AppGlobals.getPackageManager().getActivityInfo(this.componentName, 0, this.userId);
            if (activityInfo == null) {
                return AppGlobals.getPackageManager().getServiceInfo(this.componentName, 0, this.userId);
            }
            return activityInfo;
        } catch (RemoteException e) {
            return null;
        }
    }
}
