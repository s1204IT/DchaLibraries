package android.appwidget;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.ResourceId;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.UserHandle;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class AppWidgetProviderInfo implements Parcelable {
    public static final Parcelable.Creator<AppWidgetProviderInfo> CREATOR = new Parcelable.Creator<AppWidgetProviderInfo>() {
        @Override
        public AppWidgetProviderInfo createFromParcel(Parcel parcel) {
            return new AppWidgetProviderInfo(parcel);
        }

        @Override
        public AppWidgetProviderInfo[] newArray(int i) {
            return new AppWidgetProviderInfo[i];
        }
    };
    public static final int RESIZE_BOTH = 3;
    public static final int RESIZE_HORIZONTAL = 1;
    public static final int RESIZE_NONE = 0;
    public static final int RESIZE_VERTICAL = 2;
    public static final int WIDGET_CATEGORY_HOME_SCREEN = 1;
    public static final int WIDGET_CATEGORY_KEYGUARD = 2;
    public static final int WIDGET_CATEGORY_SEARCHBOX = 4;
    public static final int WIDGET_FEATURE_HIDE_FROM_PICKER = 2;
    public static final int WIDGET_FEATURE_RECONFIGURABLE = 1;
    public int autoAdvanceViewId;
    public ComponentName configure;
    public int icon;
    public int initialKeyguardLayout;
    public int initialLayout;

    @Deprecated
    public String label;
    public int minHeight;
    public int minResizeHeight;
    public int minResizeWidth;
    public int minWidth;
    public int previewImage;
    public ComponentName provider;
    public ActivityInfo providerInfo;
    public int resizeMode;
    public int updatePeriodMillis;
    public int widgetCategory;
    public int widgetFeatures;

    @Retention(RetentionPolicy.SOURCE)
    public @interface CategoryFlags {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface FeatureFlags {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface ResizeModeFlags {
    }

    public AppWidgetProviderInfo() {
    }

    public AppWidgetProviderInfo(Parcel parcel) {
        this.provider = (ComponentName) parcel.readTypedObject(ComponentName.CREATOR);
        this.minWidth = parcel.readInt();
        this.minHeight = parcel.readInt();
        this.minResizeWidth = parcel.readInt();
        this.minResizeHeight = parcel.readInt();
        this.updatePeriodMillis = parcel.readInt();
        this.initialLayout = parcel.readInt();
        this.initialKeyguardLayout = parcel.readInt();
        this.configure = (ComponentName) parcel.readTypedObject(ComponentName.CREATOR);
        this.label = parcel.readString();
        this.icon = parcel.readInt();
        this.previewImage = parcel.readInt();
        this.autoAdvanceViewId = parcel.readInt();
        this.resizeMode = parcel.readInt();
        this.widgetCategory = parcel.readInt();
        this.providerInfo = (ActivityInfo) parcel.readTypedObject(ActivityInfo.CREATOR);
        this.widgetFeatures = parcel.readInt();
    }

    public final String loadLabel(PackageManager packageManager) {
        CharSequence charSequenceLoadLabel = this.providerInfo.loadLabel(packageManager);
        if (charSequenceLoadLabel != null) {
            return charSequenceLoadLabel.toString().trim();
        }
        return null;
    }

    public final Drawable loadIcon(Context context, int i) {
        return loadDrawable(context, i, this.providerInfo.getIconResource(), true);
    }

    public final Drawable loadPreviewImage(Context context, int i) {
        return loadDrawable(context, i, this.previewImage, false);
    }

    public final UserHandle getProfile() {
        return new UserHandle(UserHandle.getUserId(this.providerInfo.applicationInfo.uid));
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeTypedObject(this.provider, i);
        parcel.writeInt(this.minWidth);
        parcel.writeInt(this.minHeight);
        parcel.writeInt(this.minResizeWidth);
        parcel.writeInt(this.minResizeHeight);
        parcel.writeInt(this.updatePeriodMillis);
        parcel.writeInt(this.initialLayout);
        parcel.writeInt(this.initialKeyguardLayout);
        parcel.writeTypedObject(this.configure, i);
        parcel.writeString(this.label);
        parcel.writeInt(this.icon);
        parcel.writeInt(this.previewImage);
        parcel.writeInt(this.autoAdvanceViewId);
        parcel.writeInt(this.resizeMode);
        parcel.writeInt(this.widgetCategory);
        parcel.writeTypedObject(this.providerInfo, i);
        parcel.writeInt(this.widgetFeatures);
    }

    public AppWidgetProviderInfo m15clone() {
        AppWidgetProviderInfo appWidgetProviderInfo = new AppWidgetProviderInfo();
        appWidgetProviderInfo.provider = this.provider == null ? null : this.provider.m17clone();
        appWidgetProviderInfo.minWidth = this.minWidth;
        appWidgetProviderInfo.minHeight = this.minHeight;
        appWidgetProviderInfo.minResizeWidth = this.minResizeHeight;
        appWidgetProviderInfo.minResizeHeight = this.minResizeHeight;
        appWidgetProviderInfo.updatePeriodMillis = this.updatePeriodMillis;
        appWidgetProviderInfo.initialLayout = this.initialLayout;
        appWidgetProviderInfo.initialKeyguardLayout = this.initialKeyguardLayout;
        appWidgetProviderInfo.configure = this.configure == null ? null : this.configure.m17clone();
        appWidgetProviderInfo.label = this.label != null ? this.label.substring(0) : null;
        appWidgetProviderInfo.icon = this.icon;
        appWidgetProviderInfo.previewImage = this.previewImage;
        appWidgetProviderInfo.autoAdvanceViewId = this.autoAdvanceViewId;
        appWidgetProviderInfo.resizeMode = this.resizeMode;
        appWidgetProviderInfo.widgetCategory = this.widgetCategory;
        appWidgetProviderInfo.providerInfo = this.providerInfo;
        appWidgetProviderInfo.widgetFeatures = this.widgetFeatures;
        return appWidgetProviderInfo;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    private Drawable loadDrawable(Context context, int i, int i2, boolean z) {
        try {
            Resources resourcesForApplication = context.getPackageManager().getResourcesForApplication(this.providerInfo.applicationInfo);
            if (ResourceId.isValid(i2)) {
                if (i < 0) {
                    i = 0;
                }
                return resourcesForApplication.getDrawableForDensity(i2, i, null);
            }
        } catch (PackageManager.NameNotFoundException | Resources.NotFoundException e) {
        }
        if (z) {
            return this.providerInfo.loadIcon(context.getPackageManager());
        }
        return null;
    }

    public void updateDimensions(DisplayMetrics displayMetrics) {
        this.minWidth = TypedValue.complexToDimensionPixelSize(this.minWidth, displayMetrics);
        this.minHeight = TypedValue.complexToDimensionPixelSize(this.minHeight, displayMetrics);
        this.minResizeWidth = TypedValue.complexToDimensionPixelSize(this.minResizeWidth, displayMetrics);
        this.minResizeHeight = TypedValue.complexToDimensionPixelSize(this.minResizeHeight, displayMetrics);
    }

    public String toString() {
        return "AppWidgetProviderInfo(" + getProfile() + '/' + this.provider + ')';
    }
}
