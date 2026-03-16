package android.content.pm;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import android.util.Log;

public class LauncherActivityInfo {
    private static final String TAG = "LauncherActivityInfo";
    private ActivityInfo mActivityInfo;
    private ComponentName mComponentName;
    private long mFirstInstallTime;
    private final PackageManager mPm;
    private UserHandle mUser;

    LauncherActivityInfo(Context context, ResolveInfo info, UserHandle user, long firstInstallTime) {
        this(context);
        this.mActivityInfo = info.activityInfo;
        this.mComponentName = LauncherApps.getComponentName(info);
        this.mUser = user;
        this.mFirstInstallTime = firstInstallTime;
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

    public Drawable getIcon(int density) {
        return this.mActivityInfo.loadIcon(this.mPm);
    }

    public int getApplicationFlags() {
        return this.mActivityInfo.applicationInfo.flags;
    }

    public ApplicationInfo getApplicationInfo() {
        return this.mActivityInfo.applicationInfo;
    }

    public long getFirstInstallTime() {
        return this.mFirstInstallTime;
    }

    public String getName() {
        return this.mActivityInfo.name;
    }

    public Drawable getBadgedIcon(int density) {
        int iconRes = this.mActivityInfo.getIconResource();
        Drawable originalIcon = null;
        try {
            Resources resources = this.mPm.getResourcesForApplication(this.mActivityInfo.applicationInfo);
            if (density != 0) {
                try {
                    originalIcon = resources.getDrawableForDensity(iconRes, density);
                } catch (Resources.NotFoundException e) {
                }
            }
        } catch (PackageManager.NameNotFoundException e2) {
        }
        if (originalIcon == null) {
            originalIcon = this.mActivityInfo.loadIcon(this.mPm);
        }
        if (originalIcon instanceof BitmapDrawable) {
            return this.mPm.getUserBadgedIcon(originalIcon, this.mUser);
        }
        Log.e(TAG, "Unable to create badged icon for " + this.mActivityInfo);
        return originalIcon;
    }
}
