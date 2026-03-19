package android.content.pm;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;

public class LauncherActivityInfo {
    private static final String TAG = "LauncherActivityInfo";
    private ActivityInfo mActivityInfo;
    private ComponentName mComponentName;
    private final PackageManager mPm;
    private UserHandle mUser;

    LauncherActivityInfo(Context context, ActivityInfo activityInfo, UserHandle userHandle) {
        this(context);
        this.mActivityInfo = activityInfo;
        this.mComponentName = new ComponentName(activityInfo.packageName, activityInfo.name);
        this.mUser = userHandle;
    }

    LauncherActivityInfo(Context context) {
        this.mPm = context.getPackageManager();
    }

    public ComponentName getComponentName() {
        return this.mComponentName;
    }

    public UserHandle getUser() {
        return this.mUser;
    }

    public CharSequence getLabel() {
        return this.mActivityInfo.loadLabel(this.mPm);
    }

    public Drawable getIcon(int i) {
        Drawable drawableForDensity;
        int iconResource = this.mActivityInfo.getIconResource();
        if (i != 0 && iconResource != 0) {
            try {
                drawableForDensity = this.mPm.getResourcesForApplication(this.mActivityInfo.applicationInfo).getDrawableForDensity(iconResource, i);
            } catch (PackageManager.NameNotFoundException | Resources.NotFoundException e) {
                drawableForDensity = null;
            }
        } else {
            drawableForDensity = null;
        }
        if (drawableForDensity == null) {
            return this.mActivityInfo.loadIcon(this.mPm);
        }
        return drawableForDensity;
    }

    public int getApplicationFlags() {
        return this.mActivityInfo.applicationInfo.flags;
    }

    public ApplicationInfo getApplicationInfo() {
        return this.mActivityInfo.applicationInfo;
    }

    public long getFirstInstallTime() {
        try {
            return this.mPm.getPackageInfo(this.mActivityInfo.packageName, 8192).firstInstallTime;
        } catch (PackageManager.NameNotFoundException e) {
            return 0L;
        }
    }

    public String getName() {
        return this.mActivityInfo.name;
    }

    public Drawable getBadgedIcon(int i) {
        return this.mPm.getUserBadgedIcon(getIcon(i), this.mUser);
    }
}
